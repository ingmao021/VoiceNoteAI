package com.voicenoteai.wear.data.speech

import app.cash.turbine.test
import com.voicenoteai.wear.domain.audio.RmsNormalizer
import com.voicenoteai.wear.domain.model.CaptureError
import com.voicenoteai.wear.domain.model.SpeechEvent
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeVoiceRecognizerTest {
    private fun TestScope.fake() = FakeVoiceRecognizer(backgroundScope, phrases = listOf("uno", "dos"))

    @Test
    fun `follows the RecognitionListener sequence and alternates phrases`() = runTest {
        val r = fake()
        r.events.filterNot { it is SpeechEvent.Level }.test {
            r.start()
            assertEquals(SpeechEvent.Ready, awaitItem())
            assertEquals(SpeechEvent.SpeechStarted, awaitItem())
            assertEquals(SpeechEvent.SpeechEnded, awaitItem())
            assertEquals(SpeechEvent.Result("uno"), awaitItem())

            r.start()
            assertEquals(SpeechEvent.Ready, awaitItem())
            assertEquals(SpeechEvent.SpeechStarted, awaitItem())
            assertEquals(SpeechEvent.SpeechEnded, awaitItem())
            assertEquals(SpeechEvent.Result("dos"), awaitItem())
        }
    }

    @Test
    fun `levels stay within the normalizer range`() = runTest {
        val r = fake()
        r.events.test {
            r.start()
            val levels = mutableListOf<Float>()
            while (true) {
                when (val e = awaitItem()) {
                    is SpeechEvent.Level -> levels += e.rmsdB
                    is SpeechEvent.Result -> break
                    else -> Unit
                }
            }
            assertTrue(levels.isNotEmpty())
            assertTrue(levels.all { it in RmsNormalizer.MIN_DB..RmsNormalizer.MAX_DB })
        }
    }

    @Test
    fun `stopping while speaking brings the result forward`() = runTest {
        val r = fake()
        r.events.filterNot { it is SpeechEvent.Level }.test {
            r.start()
            assertEquals(SpeechEvent.Ready, awaitItem())
            assertEquals(SpeechEvent.SpeechStarted, awaitItem())
            advanceTimeBy(500)
            r.stop()
            assertEquals(SpeechEvent.SpeechEnded, awaitItem())
            assertEquals(SpeechEvent.Result("uno"), awaitItem())
            assertTrue(this@runTest.testScheduler.currentTime < FakeVoiceRecognizer.SPEECH_DURATION_MS)
        }
    }

    @Test
    fun `stopping before speech is nothing heard`() = runTest {
        val r = fake()
        r.events.test {
            r.start()
            assertEquals(SpeechEvent.Ready, awaitItem())
            r.stop()
            assertEquals(SpeechEvent.Error(CaptureError.NOTHING_HEARD), awaitItem())
        }
    }

    @Test
    fun `cancelling emits nothing more`() = runTest {
        val r = fake()
        r.events.test {
            r.start()
            runCurrent()
            r.cancel()
            advanceUntilIdle()
            expectNoEvents()
        }
    }
}
