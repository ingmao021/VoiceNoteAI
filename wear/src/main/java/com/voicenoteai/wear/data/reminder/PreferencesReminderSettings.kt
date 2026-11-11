package com.voicenoteai.wear.data.reminder

import android.content.Context
import com.voicenoteai.wear.domain.repository.ReminderSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** [ReminderSettings] guardados en SharedPreferences, como una lista separada por comas. */
class PreferencesReminderSettings(context: Context) : ReminderSettings {
    private val prefs = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
    private val _options = MutableStateFlow(read())
    override val options: StateFlow<List<Long>> = _options.asStateFlow()

    override fun setOption(index: Int, minutes: Long) {
        val current = _options.value
        if (index !in current.indices) return
        val updated = current.toMutableList().also {
            it[index] = minutes.coerceIn(ReminderSettings.MIN_MINUTES, ReminderSettings.MAX_MINUTES)
        }
        prefs.edit().putString(KEY, updated.joinToString(",")).apply()
        _options.value = updated
    }

    private fun read(): List<Long> {
        val parsed = prefs.getString(KEY, null)?.split(",")?.mapNotNull { it.trim().toLongOrNull() }
        return if (parsed != null && parsed.size == ReminderSettings.DEFAULTS.size) parsed else ReminderSettings.DEFAULTS
    }

    private companion object {
        const val FILE = "reminder_settings"
        const val KEY = "options"
    }
}
