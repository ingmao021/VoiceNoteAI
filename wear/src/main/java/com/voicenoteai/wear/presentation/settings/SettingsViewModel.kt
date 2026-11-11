package com.voicenoteai.wear.presentation.settings

import androidx.lifecycle.ViewModel
import com.voicenoteai.wear.domain.repository.ReminderSettings
import kotlinx.coroutines.flow.StateFlow

/** Estado de Ajustes: los tiempos de aviso y los botones − / + de cada uno. */
class SettingsViewModel(private val settings: ReminderSettings) : ViewModel() {
    val options: StateFlow<List<Long>> = settings.options

    fun step(index: Int, delta: Int) {
        val current = settings.options.value.getOrNull(index) ?: return
        settings.setOption(index, ReminderSettings.step(current, delta))
    }
}
