package com.voicenoteai.wear.domain.usecase

import com.voicenoteai.wear.domain.device.BatteryMonitor
import com.voicenoteai.wear.domain.model.CaptureError
import com.voicenoteai.wear.domain.model.CaptureMode

/** Lo que impide capturar antes de tocar el micrófono: sin vía de captura (FA-21) o batería baja (FA-18). */
class CheckCapturePreconditionsUseCase(
    private val mode: CaptureMode,
    private val battery: BatteryMonitor,
) {
    /** `null` si se puede capturar. */
    operator fun invoke(): CaptureError? = when {
        mode == CaptureMode.NONE -> CaptureError.RECOGNIZER_UNAVAILABLE
        battery.isBatteryLow() -> CaptureError.LOW_BATTERY
        else -> null
    }
}
