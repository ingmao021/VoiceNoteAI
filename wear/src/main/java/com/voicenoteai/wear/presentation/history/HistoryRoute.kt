package com.voicenoteai.wear.presentation.history

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.voicenoteai.wear.domain.repository.ReminderSettings
import com.voicenoteai.wear.presentation.haptic.HapticPlayer

/** Une el [HistoryViewModel] con la vibración y la pantalla pura. */
@Composable
fun HistoryRoute(
    viewModel: HistoryViewModel,
    onClose: () -> Unit,
    active: Boolean = true,
    reminderOptions: List<Long> = ReminderSettings.DEFAULTS,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val hapticPlayer = remember { HapticPlayer(context) }

    var pendingRemind by remember { mutableStateOf<Pair<String, Long>?>(null) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        pendingRemind?.let { (id, minutes) -> viewModel.remind(id, minutes) }
        pendingRemind = null
    }

    LaunchedEffect(viewModel) { viewModel.haptics.collect(hapticPlayer::play) }

    HistoryScreen(
        state = state,
        onToggle = viewModel::setCompleted,
        onDelete = viewModel::delete,
        onRemind = { id, minutes ->
            // El permiso de notificaciones (API 33+) se pide la primera vez; luego se programa el aviso.
            if (needsPermission(context)) {
                pendingRemind = id to minutes
                permission.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                viewModel.remind(id, minutes)
            }
        },
        onRetry = viewModel::refresh,
        onClose = onClose,
        active = active,
        reminderOptions = reminderOptions,
    )
}

private fun needsPermission(context: Context): Boolean =
    Build.VERSION.SDK_INT >= 33 &&
        context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
