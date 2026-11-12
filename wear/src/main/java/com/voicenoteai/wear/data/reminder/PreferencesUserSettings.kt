package com.voicenoteai.wear.data.reminder

import android.content.Context
import com.voicenoteai.wear.domain.repository.AlertMode
import com.voicenoteai.wear.domain.repository.UserSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** [UserSettings] guardados en SharedPreferences. */
class PreferencesUserSettings(context: Context) : UserSettings {
    private val prefs = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    private val _haptics = MutableStateFlow(prefs.getBoolean(KEY_HAPTICS, true))
    override val hapticsEnabled: StateFlow<Boolean> = _haptics.asStateFlow()

    private val _confirmDelete = MutableStateFlow(prefs.getBoolean(KEY_CONFIRM_DELETE, false))
    override val confirmDelete: StateFlow<Boolean> = _confirmDelete.asStateFlow()

    private val _alertMode = MutableStateFlow(readAlertMode(prefs.getString(KEY_ALERT_MODE, null)))
    override val alertMode: StateFlow<AlertMode> = _alertMode.asStateFlow()

    override fun setHapticsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_HAPTICS, enabled).apply()
        _haptics.value = enabled
    }

    override fun setConfirmDelete(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_CONFIRM_DELETE, enabled).apply()
        _confirmDelete.value = enabled
    }

    override fun setAlertMode(mode: AlertMode) {
        prefs.edit().putString(KEY_ALERT_MODE, mode.name).apply()
        _alertMode.value = mode
    }

    companion object {
        private const val FILE = "user_settings"
        private const val KEY_HAPTICS = "haptics"
        private const val KEY_CONFIRM_DELETE = "confirm_delete"
        private const val KEY_ALERT_MODE = "alert_mode"

        private fun readAlertMode(name: String?): AlertMode =
            AlertMode.entries.firstOrNull { it.name == name } ?: AlertMode.SOUND_AND_VIBRATE

        /** Lectura puntual para el receptor de la alarma, que no tiene acceso al contenedor. */
        fun currentAlertMode(context: Context): AlertMode = readAlertMode(
            context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
                .getString(KEY_ALERT_MODE, null),
        )
    }
}
