package com.voicenoteai.wear.domain.model

/**
 * Errores de captura (PRD §5). El mensaje en pantalla lo decide la presentación; la traducción de
 * los códigos de `SpeechRecognizer` vive en la capa de datos.
 */
enum class CaptureError {
    /** FA-02. También `ERROR_INSUFFICIENT_PERMISSIONS` del reconocedor. */
    PERMISSION_DENIED,

    /** FA-03. */
    MICROPHONE_BUSY,

    /** FA-05 y FA-06. */
    NOTHING_HEARD,

    /** FA-07, y cualquier código no reconocido. */
    NOT_UNDERSTOOD,

    /** FA-08. */
    LANGUAGE_UNAVAILABLE,

    /** El reconocedor necesitó red y no la tuvo (relevante para R4). */
    NO_CONNECTION,

    /**
     * El dispositivo no tiene servicio de reconocimiento: típicamente una imagen de emulador sin
     * servicios de Google. Es un fallo de configuración, no de captura; no debe confundirse con
     * [NOT_UNDERSTOOD].
     */
    RECOGNIZER_UNAVAILABLE,

    /** FA-18: por debajo del 5 % no se inicia la captura. */
    LOW_BATTERY,
}
