package com.voicenoteai.wear.presentation.settings

import androidx.lifecycle.ViewModel
import com.voicenoteai.wear.domain.repository.AlertMode
import com.voicenoteai.wear.domain.repository.ReminderSettings
import com.voicenoteai.wear.domain.repository.UserSettings
import kotlinx.coroutines.flow.StateFlow

/** Estado de Ajustes: vibración, confirmar borrado, modo del aviso y los tiempos del selector. */
class SettingsViewModel(
    private val settings: ReminderSettings,
    private val user: UserSettings,
) : ViewModel() {
    val reminderMinutes: StateFlow<Long> = settings.minutes
    val hapticsEnabled: StateFlow<Boolean> = user.hapticsEnabled
    val confirmDelete: StateFlow<Boolean> = user.confirmDelete
    val alertMode: StateFlow<AlertMode> = user.alertMode

    fun step(delta: Int) {
        settings.setMinutes(ReminderSettings.step(settings.minutes.value, delta))
    }

    fun toggleHaptics() = user.setHapticsEnabled(!user.hapticsEnabled.value)

    fun toggleConfirmDelete() = user.setConfirmDelete(!user.confirmDelete.value)

    fun cycleAlertMode() = user.setAlertMode(user.alertMode.value.next())
}
