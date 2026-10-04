package com.voicenoteai.wear.data.speech

import android.speech.SpeechRecognizer
import com.voicenoteai.wear.domain.model.CaptureError

/** Traduce los códigos `SpeechRecognizer.ERROR_*` a errores de dominio (PRD §5). */
object SpeechErrorMapper {
    fun fromCode(code: Int): CaptureError = when (code) {
        SpeechRecognizer.ERROR_AUDIO -> CaptureError.MICROPHONE_BUSY
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT,
        SpeechRecognizer.ERROR_NO_MATCH -> CaptureError.NOTHING_HEARD
        SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED,
        SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE -> CaptureError.LANGUAGE_UNAVAILABLE
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> CaptureError.PERMISSION_DENIED
        SpeechRecognizer.ERROR_NETWORK,
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT,
        SpeechRecognizer.ERROR_SERVER_DISCONNECTED -> CaptureError.NO_CONNECTION
        else -> CaptureError.NOT_UNDERSTOOD
    }
}
