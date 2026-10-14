package com.voicenoteai.wear.presentation.note

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.voicenoteai.wear.data.speech.SystemDictationContract
import com.voicenoteai.wear.domain.model.CaptureMode

private const val TAG = "VoiceNoteAI.UI"

/**
 * Une el [NoteViewModel] con lo que solo puede hacer una actividad: pedir el micrófono (FA-01,
 * FA-02), lanzar el dictado del sistema (respaldo de §3.3), abrir ajustes y vibrar.
 *
 * [onRecordingChanged] activa `FLAG_KEEP_SCREEN_ON` mientras se escucha (§7.4).
 */
@Composable
fun NoteRoute(viewModel: NoteViewModel, onRecordingChanged: (Boolean) -> Unit) {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val hapticPlayer = remember { HapticPlayer(context) }

    val requestPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            viewModel.startCapture()
        } else {
            // Si el sistema ya no mostraría el diálogo, la denegación es permanente: solo quedan los ajustes.
            val permanent = activity != null &&
                !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.RECORD_AUDIO)
            viewModel.onPermissionDenied(permanent)
        }
    }

    val dictate = rememberLauncherForActivityResult(SystemDictationContract(), viewModel::onDictationResult)

    fun capture() {
        if (!viewModel.prepareCapture()) return
        when {
            // El dictado del sistema pide el micrófono por su cuenta; la app no necesita el permiso.
            viewModel.captureMode == CaptureMode.SYSTEM_DICTATION -> dictate.launch(Unit)
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED -> viewModel.startCapture()
            else -> requestPermission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    LaunchedEffect(viewModel) { viewModel.haptics.collect(hapticPlayer::play) }

    val recording = state is NoteUiState.Recording
    LaunchedEffect(recording) { onRecordingChanged(recording) }

    NoteScreen(
        state = state,
        onMainButton = { if (state is NoteUiState.Recording) viewModel.stopCapture() else capture() },
        onErrorAction = { action ->
            when (action) {
                ErrorAction.RESEND -> viewModel.resend()
                ErrorAction.SPEAK_AGAIN -> capture()
                ErrorAction.OPEN_SETTINGS -> {
                    openAppSettings(context)
                    viewModel.goBack()
                }
                ErrorAction.GO_BACK -> viewModel.goBack()
            }
        },
    )
}

/** [context] es la actividad (LocalContext), así que no hace falta `FLAG_ACTIVITY_NEW_TASK` (WearRecents). */
private fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
    runCatching { context.startActivity(intent) }
        .onFailure { Log.w(TAG, "Could not open the app settings", it) }
}
