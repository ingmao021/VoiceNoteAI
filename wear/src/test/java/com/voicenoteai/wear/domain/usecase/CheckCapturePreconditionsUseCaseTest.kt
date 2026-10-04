package com.voicenoteai.wear.domain.usecase

import com.voicenoteai.wear.domain.model.CaptureError
import com.voicenoteai.wear.domain.model.CaptureMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CheckCapturePreconditionsUseCaseTest {
    @Test
    fun `allows capture with a capture mode and enough battery`() {
        assertNull(CheckCapturePreconditionsUseCase(CaptureMode.RECOGNIZER) { false }())
        assertNull(CheckCapturePreconditionsUseCase(CaptureMode.SYSTEM_DICTATION) { false }())
    }

    @Test
    fun `no capture mode wins over low battery (FA-21)`() {
        assertEquals(CaptureError.RECOGNIZER_UNAVAILABLE, CheckCapturePreconditionsUseCase(CaptureMode.NONE) { true }())
    }

    @Test
    fun `low battery blocks the capture (FA-18)`() {
        assertEquals(CaptureError.LOW_BATTERY, CheckCapturePreconditionsUseCase(CaptureMode.RECOGNIZER) { true }())
    }
}
