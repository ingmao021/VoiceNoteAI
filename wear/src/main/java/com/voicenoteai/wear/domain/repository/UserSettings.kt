package com.voicenoteai.wear.domain.repository

import kotlinx.coroutines.flow.StateFlow

/** Cómo avisa el recordatorio de una tarea. En Android el canal no se puede cambiar: hay uno por modo. */
enum class AlertMode {
    SOUND_AND_VIBRATE,
    VIBRATE,
    SILENT;

    /** Siguiente modo al tocar la fila de Ajustes. */
    fun next(): AlertMode = entries[(ordinal + 1) % entries.size]
}

/** Preferencias de uso del reloj (vibración, confirmación de borrado, modo del aviso). */
interface UserSettings {
    /** Vibración de la app (marcar, borrar, errores, programar aviso). */
    val hapticsEnabled: StateFlow<Boolean>

    /** Pedir confirmación antes de borrar una tarea deslizando. */
    val confirmDelete: StateFlow<Boolean>

    val alertMode: StateFlow<AlertMode>

    fun setHapticsEnabled(enabled: Boolean)

    fun setConfirmDelete(enabled: Boolean)

    fun setAlertMode(mode: AlertMode)
}
