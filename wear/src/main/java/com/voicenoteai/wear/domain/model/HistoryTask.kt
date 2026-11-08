package com.voicenoteai.wear.domain.model

import java.time.LocalDate

/** Una tarea reciente que el backend devuelve para el historial. */
data class HistoryTask(
    val id: String,
    val title: String,
    val dueDate: LocalDate?,
    val completed: Boolean,
)
