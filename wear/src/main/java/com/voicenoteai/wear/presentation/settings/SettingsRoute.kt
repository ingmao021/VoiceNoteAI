package com.voicenoteai.wear.presentation.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun SettingsRoute(viewModel: SettingsViewModel, onClose: () -> Unit) {
    val options by viewModel.options.collectAsStateWithLifecycle()
    SettingsScreen(options = options, onStep = viewModel::step, onClose = onClose)
}
