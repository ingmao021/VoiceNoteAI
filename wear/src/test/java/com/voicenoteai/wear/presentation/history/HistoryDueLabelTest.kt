package com.voicenoteai.wear.presentation.history

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class HistoryDueLabelTest {
    private val today = LocalDate.of(2026, 11, 7)

    @Test
    fun `no due date has its own label`() {
        assertEquals(DueLabel.None, dueLabel(null, today))
    }

    @Test
    fun `today and tomorrow use the backend day`() {
        assertEquals(DueLabel.Today, dueLabel(today, today))
        assertEquals(DueLabel.Tomorrow, dueLabel(today.plusDays(1), today))
    }

    @Test
    fun `a later date is formatted in Spanish`() {
        val label = dueLabel(LocalDate.of(2026, 11, 14), today)
        assertTrue(label is DueLabel.Date)
        assertTrue((label as DueLabel.Date).text.contains("14"))
        assertTrue(label.text.contains("nov"))
    }
}
