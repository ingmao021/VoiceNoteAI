package com.voicenoteai.wear.domain.repository

import kotlinx.coroutines.flow.StateFlow

/** Minutos de antelación del aviso al deslizar una tarea a la derecha; el usuario lo define en Ajustes. */
interface ReminderSettings {
    val minutes: StateFlow<Long>

    /** Cambia el tiempo a [minutes], acotado a [MIN_MINUTES]..[MAX_MINUTES]. */
    fun setMinutes(minutes: Long)

    companion object {
        const val DEFAULT_MINUTES = 10L
        const val MIN_MINUTES = 1L
        const val MAX_MINUTES = 60L

        /** Escalones de − y +: de minuto en minuto hasta 10 y de 5 en 5 hasta 60. */
        val STEPS = (1L..10L).toList() + (15L..60L step 5L).toList()

        /** Siguiente valor de [STEPS] en la dirección de [delta] (+1 / −1); se queda en el extremo. */
        fun step(current: Long, delta: Int): Long {
            val index = if (delta > 0) {
                STEPS.indexOfFirst { it > current }.let { if (it < 0) STEPS.lastIndex else it }
            } else {
                STEPS.indexOfLast { it < current }.let { if (it < 0) 0 else it }
            }
            return STEPS[index]
        }
    }
}
