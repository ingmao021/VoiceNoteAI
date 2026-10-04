package com.voicenoteai.wear.domain.device

fun interface BatteryMonitor {
    /** FA-18: por debajo del mínimo no se inicia la captura. */
    fun isBatteryLow(): Boolean
}
