package com.voicenoteai.wear.presentation.note

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voicenoteai.wear.domain.audio.RmsNormalizer
import com.voicenoteai.wear.domain.model.CaptureError
import com.voicenoteai.wear.domain.model.CaptureMode
import com.voicenoteai.wear.domain.model.SaveNoteResult
import com.voicenoteai.wear.domain.model.SpeechEvent
import com.voicenoteai.wear.domain.speech.VoiceRecognizer
import com.voicenoteai.wear.domain.usecase.CheckCapturePreconditionsUseCase
import com.voicenoteai.wear.domain.usecase.SaveNoteUseCase
import com.voicenoteai.wear.domain.usecase.WarmUpBackendUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * Máquina de estados de la pantalla única (PRD §7.5).
 *
 * Lo que necesita una actividad —pedir el permiso, lanzar el dictado del sistema, abrir ajustes—
 * lo hace la UI; este ViewModel decide solo los estados y los patrones hápticos.
 */
class NoteViewModel(
    private val recognizer: VoiceRecognizer,
    private val saveNote: SaveNoteUseCase,
    private val warmUpBackend: WarmUpBackendUseCase,
    private val checkCapturePreconditions: CheckCapturePreconditionsUseCase,
    val captureMode: CaptureMode,
) : ViewModel() {
    private val _state = MutableStateFlow<NoteUiState>(NoteUiState.Idle())
    val state: StateFlow<NoteUiState> = _state.asStateFlow()

    private val _haptics = Channel<Haptic>(Channel.BUFFERED)
    val haptics: Flow<Haptic> = _haptics.receiveAsFlow()

    private val normalizer = RmsNormalizer()

    /** Hay una sesión del reconocedor abierta: sus eventos se atienden, los de una sesión ya cerrada no. */
    private var capturing = false

    /** Texto reconocido que aún no se ha guardado: se conserva ante cualquier error de envío (§5). */
    private var pendingText: String? = null

    private var durationGuard: Job? = null
    private var stages: Job? = null
    private var returnToIdle: Job? = null

    init {
        viewModelScope.launch { recognizer.events.collect(::onSpeechEvent) }
        warmUpBackend()
    }

    /**
     * Comprueba lo que no depende de la actividad antes de capturar. Devuelve `false` si no se debe
     * seguir: el toque llegó en un estado que no lo admite, o la captura acabó en ERROR (FA-18, FA-21).
     */
    fun prepareCapture(): Boolean {
        val current = _state.value
        if (!(current is NoteUiState.Idle && !current.starting) && current !is NoteUiState.Error) return false
        val blocker = checkCapturePreconditions() ?: return true
        showError(blocker.messageRes, ErrorAction.GO_BACK)
        return false
    }

    /** Con el permiso ya concedido. Pasa a GRABANDO cuando llegue `onReadyForSpeech`. */
    fun startCapture() {
        pendingText = null
        normalizer.reset()
        capturing = true
        _state.value = NoteUiState.Idle(starting = true)
        recognizer.start()
    }

    /** FA-02: [permanent] si el sistema ya no volverá a mostrar el diálogo. */
    fun onPermissionDenied(permanent: Boolean) {
        showError(
            CaptureError.PERMISSION_DENIED.messageRes,
            if (permanent) ErrorAction.OPEN_SETTINGS else ErrorAction.SPEAK_AGAIN,
        )
    }

    /** Resultado del dictado del sistema: `null` si el usuario lo canceló. */
    fun onDictationResult(text: String?) {
        when {
            text == null -> enterIdle()
            text.isBlank() -> showError(CaptureError.NOTHING_HEARD.messageRes, ErrorAction.SPEAK_AGAIN)
            else -> submit(text)
        }
    }

    /** Final manual: entrega lo oído hasta ahora (§4.1). */
    fun stopCapture() {
        if (_state.value is NoteUiState.Recording) recognizer.stop()
    }

    fun resend() {
        pendingText?.let(::submit) ?: enterIdle()
    }

    fun goBack() = enterIdle()

    /**
     * FA-04: la app sale de primer plano durante la captura. Descarta con `cancel()` y devuelve
     * `true` para que la actividad se cierre. Fuera de la captura no hace nada.
     */
    fun cancelOnExit(): Boolean {
        if (!capturing || _state.value is NoteUiState.Processing) return false
        capturing = false
        durationGuard?.cancel()
        recognizer.cancel()
        enterIdle()
        return true
    }

    override fun onCleared() {
        recognizer.release()
    }

    private fun onSpeechEvent(event: SpeechEvent) {
        if (!capturing) return
        when (event) {
            SpeechEvent.Ready -> {
                _state.value = NoteUiState.Recording()
                _haptics.trySend(Haptic.START)
                // Guarda no destructiva: stop() entrega el texto, no lo descarta (§4.2, RN-11).
                durationGuard = viewModelScope.launch {
                    delay(MAX_DURATION_MS)
                    recognizer.stop()
                }
            }
            SpeechEvent.SpeechStarted -> updateRecording { it.copy(speaking = true) }
            is SpeechEvent.Level -> updateRecording { it.copy(level = normalizer.normalize(event.rmsdB)) }
            SpeechEvent.SpeechEnded -> {
                durationGuard?.cancel()
                enterProcessing()
            }
            is SpeechEvent.Result -> {
                capturing = false
                durationGuard?.cancel()
                if (event.text.isBlank()) {
                    showError(CaptureError.NOTHING_HEARD.messageRes, ErrorAction.SPEAK_AGAIN)
                } else {
                    submit(event.text)
                }
            }
            is SpeechEvent.Error -> {
                capturing = false
                durationGuard?.cancel()
                showError(event.error.messageRes, actionFor(event.error))
            }
        }
    }

    private fun submit(text: String) {
        pendingText = text
        enterProcessing()
        viewModelScope.launch {
            val result = saveNote(text)
            stages?.cancel()
            when (result) {
                is SaveNoteResult.Saved -> {
                    pendingText = null
                    _state.value = NoteUiState.Success(result.title)
                    _haptics.trySend(Haptic.SUCCESS)
                    returnToIdle = viewModelScope.launch {
                        delay(SUCCESS_DISPLAY_MS)
                        enterIdle()
                    }
                }
                is SaveNoteResult.Failed -> {
                    val error = result.error
                    if (!error.retryable) pendingText = null
                    showError(error.messageRes, if (error.retryable) ErrorAction.RESEND else ErrorAction.GO_BACK)
                }
            }
        }
    }

    private fun enterProcessing() {
        // Las etapas cuentan desde el fin del habla: si ya se está procesando, no se reinician.
        if (_state.value is NoteUiState.Processing) return
        _state.value = NoteUiState.Processing()
        stages = viewModelScope.launch {
            var elapsed = 0L
            for (stage in ProcessingStage.entries.drop(1)) {
                delay(stage.fromMs - elapsed)
                elapsed = stage.fromMs
                _state.value = NoteUiState.Processing(stage)
            }
        }
    }

    /** Cada entrada en INICIAL dispara el ping de calentamiento (§3.5). */
    private fun enterIdle() {
        returnToIdle?.cancel()
        _state.value = NoteUiState.Idle()
        warmUpBackend()
    }

    private fun showError(@StringRes messageRes: Int, action: ErrorAction) {
        stages?.cancel()
        _state.value = NoteUiState.Error(messageRes, action)
        _haptics.trySend(Haptic.ERROR)
    }

    private inline fun updateRecording(change: (NoteUiState.Recording) -> NoteUiState.Recording) {
        val current = _state.value
        if (current is NoteUiState.Recording) _state.value = change(current)
    }

    private fun actionFor(error: CaptureError) = when (error) {
        CaptureError.PERMISSION_DENIED -> ErrorAction.OPEN_SETTINGS
        CaptureError.LANGUAGE_UNAVAILABLE,
        CaptureError.RECOGNIZER_UNAVAILABLE,
        CaptureError.LOW_BATTERY -> ErrorAction.GO_BACK
        CaptureError.MICROPHONE_BUSY,
        CaptureError.NOTHING_HEARD,
        CaptureError.NOT_UNDERSTOOD,
        CaptureError.NO_CONNECTION -> ErrorAction.SPEAK_AGAIN
    }

    companion object {
        /** Guarda de duración (§4.3). Una sola constante en un solo lugar. */
        const val MAX_DURATION_MS = 60_000L
        const val SUCCESS_DISPLAY_MS = 3_000L
    }
}
