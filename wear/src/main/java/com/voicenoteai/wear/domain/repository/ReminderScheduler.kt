package com.voicenoteai.wear.domain.repository

import com.voicenoteai.wear.domain.model.HistoryTask

/** Programa un aviso en el reloj para una tarea (deslizar a la derecha en el historial). */
interface ReminderScheduler {
    /** Avisará pasados [minutes]. Reprogramar la misma tarea sustituye el aviso anterior. */
    fun schedule(task: HistoryTask, minutes: Long)
}
