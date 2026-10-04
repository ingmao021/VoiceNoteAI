package com.voicenoteai.wear.data.speech

import com.voicenoteai.wear.domain.audio.RmsNormalizer
import com.voicenoteai.wear.domain.model.CaptureError
import com.voicenoteai.wear.domain.model.SpeechEvent
import com.voicenoteai.wear.domain.speech.VoiceRecognizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sin

/**
 * [VoiceRecognizer] simulado para las pruebas. Vive en `src/test`, así que nunca llega al APK:
 * la app siempre usa `AndroidVoiceRecognizer`.
 *
 * Imita la secuencia real de `RecognitionListener`: Ready → SpeechStarted → Level… → SpeechEnded →
 * Result. [stop] adelanta el final, como `stopListening()`; [cancel] lo corta sin resultado.
 * No sirve para medir R2/R7: los tiempos y niveles son inventados.
 */
class FakeVoiceRecognizer(
    private val scope: CoroutineScope,
    private val phrases: List<String> = PHRASES,
    private val speechDurationMs: Long = SPEECH_DURATION_MS,
) : VoiceRecognizer {
    private val channel = Channel<SpeechEvent>(Channel.UNLIMITED)
    private var session: Job? = null
    private var speaking = false
    private var nextPhrase = 0

    override val events: Flow<SpeechEvent> = channel.receiveAsFlow()

    override val isAvailable = true

    override fun start() {
        session?.cancel()
        session = scope.launch {
            delay(READY_DELAY_MS)
            channel.send(SpeechEvent.Ready)
            delay(SPEECH_DELAY_MS)
            speaking = true
            channel.send(SpeechEvent.SpeechStarted)
            var t = 0L
            while (t < speechDurationMs) {
                channel.send(SpeechEvent.Level(simulatedLevel(t)))
                delay(LEVEL_INTERVAL_MS)
                t += LEVEL_INTERVAL_MS
            }
            finish()
        }
    }

    override fun stop() {
        val active = session ?: return
        // Antes de SpeechStarted no hay nada que entregar: el servicio real respondería "no escuché".
        active.cancel()
        session = scope.launch {
            if (speaking) finish() else channel.send(SpeechEvent.Error(CaptureError.NOTHING_HEARD))
        }
    }

    override fun cancel() {
        session?.cancel()
        session = null
        speaking = false
    }

    override fun release() {
        cancel()
        scope.cancel()
    }

    private suspend fun finish() {
        speaking = false
        channel.send(SpeechEvent.SpeechEnded)
        delay(RESULT_DELAY_MS)
        channel.send(SpeechEvent.Result(phrases[nextPhrase % phrases.size]))
        nextPhrase++
        session = null
    }

    /** Onda entre MIN_DB y MAX_DB de [RmsNormalizer], con altibajos como los de una voz. */
    private fun simulatedLevel(t: Long): Float {
        val wave = abs(sin(t / 180.0)) * 0.7 + abs(sin(t / 53.0)) * 0.3
        return (RmsNormalizer.MIN_DB + wave * (RmsNormalizer.MAX_DB - RmsNormalizer.MIN_DB)).toFloat()
    }

    companion object {
        const val READY_DELAY_MS = 300L
        const val SPEECH_DELAY_MS = 400L
        const val LEVEL_INTERVAL_MS = 100L
        const val SPEECH_DURATION_MS = 3_000L
        const val RESULT_DELAY_MS = 500L

        // Frases de ejemplo en español: simulan lo que dictaría el usuario (es-CO).
        val PHRASES = listOf(
            "Comprar leche y pan mañana domingo cuatro de octubre",
            "Llamar al médico el jueves ocho de octubre para pedir cita",
            "Revisar el informe del proyecto antes del viernes",
        )
    }
}
