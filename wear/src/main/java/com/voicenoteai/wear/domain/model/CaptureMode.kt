package com.voicenoteai.wear.domain.model

/** Cómo obtiene el texto este dispositivo (PRD §3.3). */
enum class CaptureMode {
    /** `SpeechRecognizer` con anillo de nivel real. */
    RECOGNIZER,

    /** Respaldo: pantalla de dictado del sistema (`RecognizerIntent`), sin anillo ni parciales. */
    SYSTEM_DICTATION,

    /** Ninguna de las dos vías: FA-21. */
    NONE,
}
