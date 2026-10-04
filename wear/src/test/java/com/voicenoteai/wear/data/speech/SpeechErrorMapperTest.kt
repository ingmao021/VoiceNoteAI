package com.voicenoteai.wear.data.speech

import com.voicenoteai.wear.domain.model.CaptureError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Códigos numéricos literales de `SpeechRecognizer.ERROR_*`, para documentarlos. */
class SpeechErrorMapperTest {

    @Test
    fun `maps each code to its PRD case`() {
        val expected = mapOf(
            1 to CaptureError.NO_CONNECTION, // NETWORK_TIMEOUT
            2 to CaptureError.NO_CONNECTION, // NETWORK
            3 to CaptureError.MICROPHONE_BUSY, // AUDIO — FA-03
            4 to CaptureError.NOT_UNDERSTOOD, // SERVER
            5 to CaptureError.NOT_UNDERSTOOD, // CLIENT — FA-07
            6 to CaptureError.NOTHING_HEARD, // SPEECH_TIMEOUT — FA-05
            7 to CaptureError.NOTHING_HEARD, // NO_MATCH — FA-06
            8 to CaptureError.NOT_UNDERSTOOD, // RECOGNIZER_BUSY — FA-07
            9 to CaptureError.PERMISSION_DENIED, // INSUFFICIENT_PERMISSIONS
            10 to CaptureError.NOT_UNDERSTOOD, // TOO_MANY_REQUESTS
            11 to CaptureError.NO_CONNECTION, // SERVER_DISCONNECTED
            12 to CaptureError.LANGUAGE_UNAVAILABLE, // LANGUAGE_NOT_SUPPORTED — FA-08
            13 to CaptureError.LANGUAGE_UNAVAILABLE, // LANGUAGE_UNAVAILABLE — FA-08
            999 to CaptureError.NOT_UNDERSTOOD,
        )
        expected.forEach { (code, error) -> assertEquals("code $code", error, SpeechErrorMapper.fromCode(code)) }
    }

    @Test
    fun `no code maps to recognizer unavailable`() {
        (-5..50).forEach { assertTrue(SpeechErrorMapper.fromCode(it) != CaptureError.RECOGNIZER_UNAVAILABLE) }
    }
}
