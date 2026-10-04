package com.voicenoteai.wear.data.speech

import com.voicenoteai.wear.domain.model.CaptureError
import com.voicenoteai.wear.domain.model.SpeechEvent
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionMeterTest {
    private var now = 0L
    private val meter = SessionMeter { now }

    @Test
    fun `summarizes timings, partials, words and rms range`() {
        meter.start()
        now = 250; meter.record(SpeechEvent.Ready)
        now = 1_000; meter.record(SpeechEvent.SpeechStarted)
        meter.record(SpeechEvent.Level(-1f))
        meter.record(SpeechEvent.Level(9f))
        now = 5_000; meter.partial()
        now = 13_000; meter.record(SpeechEvent.SpeechEnded)
        now = 14_000
        val r = meter.record(SpeechEvent.Result("comprar café mañana cuatro de octubre en la tarde"))!!

        listOf(
            "end=Result", "tap→ready=250 ms", "speech=12000 ms", "session=14000 ms",
            "lastPartial=+5000 ms", "partials=1", "words=9",
            "min=-1.0", "max=9.0", "mean=4.0", "(2 samples)",
        ).forEach { assertTrue("missing «$it» in: $r", it in r) }
    }

    @Test
    fun `a session with voice above the threshold is valid`() {
        meter.start()
        meter.record(SpeechEvent.Level(-2f))
        meter.record(SpeechEvent.Level(7f))
        val r = meter.record(SpeechEvent.Result("hola"))!!
        assertTrue(r, "valid=YES" in r)
    }

    @Test
    fun `a silent session is invalid even with short speech`() {
        // Emulador sin audio del host: silencio digital y cierre rápido por el reconocedor.
        meter.start()
        now = 300; meter.record(SpeechEvent.SpeechStarted)
        repeat(20) { meter.record(SpeechEvent.Level(-2.12f)) }
        meter.record(SpeechEvent.Level(SessionMeter.AUDIO_THRESHOLD_DB)) // igualar el umbral no basta
        now = 1_500; meter.record(SpeechEvent.SpeechEnded)
        val r = meter.record(SpeechEvent.Result(""))!!
        assertTrue(r, "valid=NO reason=no_audio" in r)
    }

    @Test
    fun `a session without level samples is invalid`() {
        meter.start()
        val r = meter.record(SpeechEvent.Error(CaptureError.NOTHING_HEARD, 6))!!
        assertTrue(r, "valid=NO reason=no_audio" in r)
    }

    @Test
    fun `never logs the recognized text`() {
        meter.start()
        val r = meter.record(SpeechEvent.Result("idea secreta"))!!
        assertTrue("secreta" !in r)
    }

    @Test
    fun `an error closes the session with its code`() {
        meter.start()
        val r = meter.record(SpeechEvent.Error(CaptureError.NOTHING_HEARD, 6))!!
        assertTrue(r, "end=Error(NOTHING_HEARD, code=6)" in r)
        assertTrue(r, "speech=—" in r)
    }

    @Test
    fun `no summary without a started session`() {
        assertNull(meter.record(SpeechEvent.Error(CaptureError.RECOGNIZER_UNAVAILABLE)))
        meter.start()
        meter.record(SpeechEvent.Result("hola"))
        assertNull("la sesión ya terminó", meter.record(SpeechEvent.Result("hola")))
    }
}
