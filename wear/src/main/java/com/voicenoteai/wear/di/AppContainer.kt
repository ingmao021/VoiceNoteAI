package com.voicenoteai.wear.di

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.voicenoteai.wear.BuildConfig
import com.voicenoteai.wear.data.device.AndroidBatteryMonitor
import com.voicenoteai.wear.data.local.OfflineNoteRepository
import com.voicenoteai.wear.data.remote.HttpNoteRepository
import com.voicenoteai.wear.data.remote.NoteApi
import com.voicenoteai.wear.data.speech.AndroidVoiceRecognizer
import com.voicenoteai.wear.data.speech.SystemDictationContract
import com.voicenoteai.wear.domain.device.BatteryMonitor
import com.voicenoteai.wear.domain.model.CaptureMode
import com.voicenoteai.wear.domain.repository.NoteRepository
import com.voicenoteai.wear.domain.speech.VoiceRecognizer
import com.voicenoteai.wear.domain.usecase.CheckCapturePreconditionsUseCase
import com.voicenoteai.wear.domain.usecase.SaveNoteUseCase
import com.voicenoteai.wear.domain.usecase.WarmUpBackendUseCase
import com.voicenoteai.wear.presentation.note.NoteViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Raíz de composición (PRD §2.1: contenedor manual, sin framework de inyección). Es el único sitio
 * que conoce las implementaciones concretas; el resto depende de las interfaces del dominio.
 */
class AppContainer(context: Context) {
    private val context = context.applicationContext

    // El ping de calentamiento no debe morir con la actividad: alcance propio, del tamaño de la app.
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val noteRepository: NoteRepository by lazy { createNoteRepository() }
    private val batteryMonitor: BatteryMonitor by lazy { AndroidBatteryMonitor(this.context) }

    /** El ViewModel es dueño del reconocedor y lo libera en `onCleared`. */
    val noteViewModelFactory: ViewModelProvider.Factory = viewModelFactory {
        initializer {
            // Siempre el reconocedor real, también en debug: el simulado solo existe en las pruebas.
            val recognizer = AndroidVoiceRecognizer(context)
            val mode = captureMode(recognizer)
            NoteViewModel(
                recognizer = recognizer,
                saveNote = SaveNoteUseCase(noteRepository),
                warmUpBackend = WarmUpBackendUseCase(noteRepository),
                checkCapturePreconditions = CheckCapturePreconditionsUseCase(mode, batteryMonitor),
                captureMode = mode,
            )
        }
    }

    /** Sin `RecognitionService` se recurre al dictado del sistema; sin ninguno de los dos, FA-21. */
    private fun captureMode(recognizer: VoiceRecognizer) = when {
        recognizer.isAvailable -> CaptureMode.RECOGNIZER
        SystemDictationContract.isAvailable(context) -> CaptureMode.SYSTEM_DICTATION
        else -> {
            Log.e(AndroidVoiceRecognizer.TAG_CONFIG, "Neither RecognitionService nor system dictation: FA-21")
            CaptureMode.NONE
        }
    }

    /** Sin token configurado la app no envía nada y muestra el texto como título, como en la etapa 1. */
    private fun createNoteRepository(): NoteRepository {
        if (BuildConfig.DEV_API_TOKEN.isBlank()) {
            Log.w(HttpNoteRepository.TAG, "No voicenoteai.devApiToken in local.properties: notes are not sent")
            return OfflineNoteRepository()
        }
        Log.i(HttpNoteRepository.TAG, "Backend: ${BuildConfig.BACKEND_URL}")
        return HttpNoteRepository(NoteApi(BuildConfig.BACKEND_URL, BuildConfig.DEV_API_TOKEN), appScope)
    }
}
