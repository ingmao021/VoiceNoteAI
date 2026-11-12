package com.voicenoteai.wear.presentation.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun SettingsRoute(viewModel: SettingsViewModel, onClose: () -> Unit) {
    val minutes by viewModel.reminderMinutes.collectAsStateWithLifecycle()
    val haptics by viewModel.hapticsEnabled.collectAsStateWithLifecycle()
    val confirmDelete by viewModel.confirmDelete.collectAsStateWithLifecycle()
    val alertMode by viewModel.alertMode.collectAsStateWithLifecycle()
    SettingsScreen(
        reminderMinutes = minutes,
        hapticsEnabled = haptics,
        confirmDelete = confirmDelete,
        alertMode = alertMode,
        onStep = viewModel::step,
        onToggleHaptics = viewModel::toggleHaptics,
        onToggleConfirmDelete = viewModel::toggleConfirmDelete,
        onCycleAlertMode = viewModel::cycleAlertMode,
        onClose = onClose,
    )
}
