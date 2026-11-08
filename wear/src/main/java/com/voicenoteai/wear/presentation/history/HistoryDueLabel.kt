package com.voicenoteai.wear.presentation.history

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Etiqueta del vencimiento de una tarea del historial.
 *
 * Se calcula con el [today] que devuelve el backend, no con el reloj del reloj: así HOY y MAÑANA
 * coinciden con el filtro que oculta las tareas vencidas, aunque el reloj esté en otra zona.
 */
sealed interface DueLabel {
    data object None : DueLabel

    data object Today : DueLabel

    data object Tomorrow : DueLabel

    data class Date(val text: String) : DueLabel
}

private val dayMonth: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMM", Locale.forLanguageTag("es"))

fun dueLabel(dueDate: LocalDate?, today: LocalDate): DueLabel = when (dueDate) {
    null -> DueLabel.None
    today -> DueLabel.Today
    today.plusDays(1) -> DueLabel.Tomorrow
    else -> DueLabel.Date(dayMonth.format(dueDate))
}
