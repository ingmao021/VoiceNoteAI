package com.voicenoteai.wear.data.device

import android.content.Context
import android.os.BatteryManager
import com.voicenoteai.wear.domain.device.BatteryMonitor

class AndroidBatteryMonitor(context: Context) : BatteryMonitor {
    private val batteryManager = context.getSystemService(BatteryManager::class.java)

    /** FA-18. `getIntProperty` devuelve un valor negativo si el dispositivo no lo informa. */
    override fun isBatteryLow(): Boolean {
        val level = batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: return false
        return level in 0 until MIN_BATTERY_PERCENT
    }

    private companion object {
        const val MIN_BATTERY_PERCENT = 5
    }
}
