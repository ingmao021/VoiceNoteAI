package com.voicenoteai.wear.domain.repository

import org.junit.Assert.assertEquals
import org.junit.Test

class ReminderSettingsTest {
    @Test
    fun `step moves to the next and previous rung`() {
        assertEquals(15L, ReminderSettings.step(10, +1))
        assertEquals(5L, ReminderSettings.step(10, -1))
        assertEquals(90L, ReminderSettings.step(60, +1))
        assertEquals(45L, ReminderSettings.step(60, -1))
    }

    @Test
    fun `step snaps custom values and stays within the limits`() {
        assertEquals(60L, ReminderSettings.step(50, +1))
        assertEquals(5L, ReminderSettings.step(5, -1))
        assertEquals(1_440L, ReminderSettings.step(1_440, +1))
    }
}
