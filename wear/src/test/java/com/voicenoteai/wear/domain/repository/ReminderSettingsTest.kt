package com.voicenoteai.wear.domain.repository

import org.junit.Assert.assertEquals
import org.junit.Test

class ReminderSettingsTest {
    @Test
    fun `step moves one minute at a time up to ten then by five`() {
        assertEquals(2L, ReminderSettings.step(1, +1))
        assertEquals(10L, ReminderSettings.step(9, +1))
        assertEquals(15L, ReminderSettings.step(10, +1))
        assertEquals(10L, ReminderSettings.step(15, -1))
        assertEquals(55L, ReminderSettings.step(60, -1))
    }

    @Test
    fun `step snaps custom values and stays within 1 to 60`() {
        assertEquals(55L, ReminderSettings.step(52, +1))
        assertEquals(1L, ReminderSettings.step(1, -1))
        assertEquals(60L, ReminderSettings.step(60, +1))
    }
}
