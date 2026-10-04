package com.voicenoteai.wear.domain.model

/** Traducción uno a uno de los callbacks de `RecognitionListener` (PRD §3.3). */
sealed interface SpeechEvent {
    data object Ready : SpeechEvent
    data object SpeechStarted : SpeechEvent
    data class Level(val rmsdB: Float) : SpeechEvent
    data object SpeechEnded : SpeechEvent
    data class Result(val text: String) : SpeechEvent

    /** [code] es el `SpeechRecognizer.ERROR_*` original; `null` si el error no viene del reconocedor. */
    data class Error(val error: CaptureError, val code: Int? = null) : SpeechEvent
}
