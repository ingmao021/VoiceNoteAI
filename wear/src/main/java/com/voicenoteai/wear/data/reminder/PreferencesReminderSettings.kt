package com.voicenoteai.wear.data.reminder

import android.content.Context
import com.voicenoteai.wear.domain.repository.ReminderSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** [ReminderSettings] guardado en SharedPreferences. */
class PreferencesReminderSettings(context: Context) : ReminderSettings {
    private val prefs = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
    private val _minutes = MutableStateFlow(
        prefs.getLong(KEY, ReminderSettings.DEFAULT_MINUTES)
            .coerceIn(ReminderSettings.MIN_MINUTES, ReminderSettings.MAX_MINUTES),
    )
    override val minutes: StateFlow<Long> = _minutes.asStateFlow()

    override fun setMinutes(minutes: Long) {
        val value = minutes.coerceIn(ReminderSettings.MIN_MINUTES, ReminderSettings.MAX_MINUTES)
        prefs.edit().putLong(KEY, value).apply()
        _minutes.value = value
    }

    private companion object {
        const val FILE = "reminder_settings"
        const val KEY = "minutes"
    }
}
