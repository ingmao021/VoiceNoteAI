package com.voicenoteai.wear.presentation.history

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.voicenoteai.wear.presentation.haptic.HapticPlayer

/** Une el [HistoryViewModel] con la vibración y la pantalla pura. */
@Composable
fun HistoryRoute(
    viewModel: HistoryViewModel,
    onClose: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val hapticPlayer = remember { HapticPlayer(context) }

    LaunchedEffect(viewModel) { viewModel.haptics.collect(hapticPlayer::play) }

    HistoryScreen(
        state = state,
        onToggle = viewModel::setCompleted,
        onRetry = viewModel::refresh,
        onClose = onClose,
    )
}
