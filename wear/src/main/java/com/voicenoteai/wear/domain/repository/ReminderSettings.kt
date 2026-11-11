package com.voicenoteai.wear.domain.repository

import kotlinx.coroutines.flow.StateFlow

/** Tiempos (en minutos) que ofrece el selector de aviso del historial; el usuario los define en Ajustes. */
interface ReminderSettings {
    val options: StateFlow<List<Long>>

    /** Cambia la opción [index] a [minutes], acotada a [MIN_MINUTES]..[MAX_MINUTES]. */
    fun setOption(index: Int, minutes: Long)

    companion object {
        val DEFAULTS = listOf(10L, 30L, 60L)
        const val MIN_MINUTES = 5L
        const val MAX_MINUTES = 1_440L

        /** Escalones de los botones − y +: pasos cortos para minutos y más largos para horas. */
        val STEPS = listOf(5L, 10L, 15L, 20L, 30L, 45L, 60L, 90L, 120L, 180L, 240L, 360L, 480L, 720L, 1_440L)

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
