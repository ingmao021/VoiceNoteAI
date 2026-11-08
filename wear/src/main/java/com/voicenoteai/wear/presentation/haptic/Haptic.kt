package com.voicenoteai.wear.presentation.haptic

/** Patrones hápticos distintos para usar el reloj sin mirarlo (PRD §7.6). */
enum class Haptic {
    /** Comienza una captura. */
    START,

    /** La nota se guardó. */
    SUCCESS,

    /** Algo falló. */
    ERROR,

    /** Cambio menor, como marcar una tarea del historial. */
    TOGGLE,
}
