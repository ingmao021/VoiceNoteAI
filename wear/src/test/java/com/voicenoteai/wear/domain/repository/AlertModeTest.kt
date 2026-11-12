package com.voicenoteai.wear.domain.repository

import org.junit.Assert.assertEquals
import org.junit.Test

class AlertModeTest {
    @Test
    fun `next cycles through all modes and wraps around`() {
        assertEquals(AlertMode.VIBRATE, AlertMode.SOUND_AND_VIBRATE.next())
        assertEquals(AlertMode.SILENT, AlertMode.VIBRATE.next())
        assertEquals(AlertMode.SOUND_AND_VIBRATE, AlertMode.SILENT.next())
    }
}
