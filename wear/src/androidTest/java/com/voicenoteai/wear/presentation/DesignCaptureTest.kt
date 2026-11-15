package com.voicenoteai.wear.presentation

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.voicenoteai.wear.domain.repository.AlertMode
import com.voicenoteai.wear.presentation.note.NoteScreen
import com.voicenoteai.wear.presentation.note.NoteUiState
import com.voicenoteai.wear.presentation.settings.SettingsScreen
import com.voicenoteai.wear.presentation.theme.VoiceNoteAITheme
import com.voicenoteai.wear.presentation.theme.VoltPalette
import java.io.File
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Solo guarda capturas (inicio con las ondas a medio camino y Ajustes) para revisar el diseño a ojo. */
@RunWith(AndroidJUnit4::class)
class DesignCaptureTest {
    @get:Rule
    val rule = createComposeRule()

    private fun shot(name: String) {
        val bitmap = rule.onRoot().captureToImage().asAndroidBitmap()
        val dir = InstrumentationRegistry.getInstrumentation().targetContext
            .getExternalFilesDir("capturas")?.resolve("design") ?: return
        dir.mkdirs()
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test
    fun capture() {
        rule.mainClock.autoAdvance = false
        rule.setContent {
            VoiceNoteAITheme {
                Box(Modifier.fillMaxSize().background(VoltPalette.Background)) {
                    NoteScreen(state = NoteUiState.Idle(), onMainButton = {}, onErrorAction = {})
                }
            }
        }
        rule.mainClock.advanceTimeBy(1700)
        shot("inicio")
    }

    @Test
    fun captureSettings() {
        rule.setContent {
            VoiceNoteAITheme {
                SettingsScreen(
                    reminderMinutes = 10, hapticsEnabled = true, confirmDelete = false,
                    alertMode = AlertMode.SOUND_AND_VIBRATE,
                    onStep = {}, onToggleHaptics = {}, onToggleConfirmDelete = {}, onCycleAlertMode = {}, onClose = {},
                )
            }
        }
        rule.mainClock.advanceTimeBy(900)
        shot("ajustes")
    }
}
