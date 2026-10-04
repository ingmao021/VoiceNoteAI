package com.voicenoteai.wear.domain.speech

import com.voicenoteai.wear.domain.model.SpeechEvent
import kotlinx.coroutines.flow.Flow

/** Abstracción del reconocedor para poder sustituirlo por un falso en las pruebas. */
interface VoiceRecognizer {
    val events: Flow<SpeechEvent>

    /** `false` si el dispositivo no tiene servicio de reconocimiento (FA-21): [start] solo emitirá ese error. */
    val isAvailable: Boolean

    fun start()

    /** Deja de escuchar y entrega lo oído hasta ahora: final manual y guarda de 60 s (§4.2). */
    fun stop()

    /** Aborta y descarta. Solo para la cancelación explícita (FA-04). */
    fun cancel()

    fun release()
}
