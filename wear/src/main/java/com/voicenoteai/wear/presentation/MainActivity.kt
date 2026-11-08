package com.voicenoteai.wear.presentation

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.voicenoteai.wear.VoiceNoteApplication
import com.voicenoteai.wear.presentation.history.HistoryViewModel
import com.voicenoteai.wear.presentation.home.HomeRoute
import com.voicenoteai.wear.presentation.note.NoteViewModel
import com.voicenoteai.wear.presentation.theme.VoiceNoteAITheme

class MainActivity : ComponentActivity() {
    private val container by lazy { (application as VoiceNoteApplication).container }

    private val noteViewModel: NoteViewModel by viewModels { container.noteViewModelFactory }
    private val historyViewModel: HistoryViewModel by viewModels { container.historyViewModelFactory }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Sin esta llamada el tema Theme.SplashScreen nunca cede a postSplashScreenTheme.
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContent {
            VoiceNoteAITheme {
                HomeRoute(
                    noteViewModel = noteViewModel,
                    historyViewModel = historyViewModel,
                    onRecordingChanged = ::keepScreenOn,
                )
            }
        }
    }

    /**
     * FA-04: deslizar para cerrar, el botón de encendido o una notificación sacan la app de primer
     * plano. Si se estaba escuchando, la captura se descarta y la app se cierra.
     */
    override fun onStop() {
        super.onStop()
        if (noteViewModel.cancelOnExit()) finish()
    }

    /** Evita que el modo ambiente corte un dictado largo. */
    private fun keepScreenOn(enabled: Boolean) {
        if (enabled) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
}
