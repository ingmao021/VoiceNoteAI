package com.voicenoteai.wear.presentation.note

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/** Patrones hápticos de la sección 7.4: corto al iniciar, doble en éxito, largo en error. */
class HapticPlayer(context: Context) {
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Vibrator::class.java)
    }

    fun play(haptic: Haptic) {
        val effect = when (haptic) {
            Haptic.START -> VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE)
            Haptic.SUCCESS -> VibrationEffect.createWaveform(longArrayOf(0, 40, 90, 40), -1)
            Haptic.ERROR -> VibrationEffect.createOneShot(350, VibrationEffect.DEFAULT_AMPLITUDE)
        }
        vibrator?.vibrate(effect)
    }
}
