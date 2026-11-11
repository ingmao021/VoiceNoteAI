package com.voicenoteai.wear.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.voicenoteai.wear.presentation.note.NoteScreen
import com.voicenoteai.wear.presentation.note.NoteUiState
import com.voicenoteai.wear.presentation.theme.VoiceNoteAITheme
import com.voicenoteai.wear.presentation.theme.VoltPalette
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Comprueba que la onda de «ESCUCHANDO» se mueve de verdad: en silencio sigue viva pero baja, y con
 * voz crece. Mide, en capturas reales de la barra central, cuántos píxeles de alto ocupa.
 */
@RunWith(AndroidJUnit4::class)
class WaveformReactsToVoiceTest {

    @get:Rule
    val rule = createComposeRule()

    private var state by mutableStateOf<NoteUiState>(NoteUiState.Recording(level = 0f, speaking = false))

    /** Alto máximo y mínimo (en px) de la barra central a lo largo de ~1,4 s de animación. */
    private fun centerBarHeights(): Pair<Int, Int> {
        val heights = mutableListOf<Int>()
        repeat(14) {
            rule.mainClock.advanceTimeBy(100)
            val bitmap = rule.onNodeWithTag("noteMain").captureToImage().asAndroidBitmap()
            val x = bitmap.width / 2
            var top = -1
            var bottom = -1
            for (y in 0 until bitmap.height) {
                val p = bitmap.getPixel(x, y)
                val lit = android.graphics.Color.red(p) + android.graphics.Color.green(p) +
                    android.graphics.Color.blue(p) > 120
                if (lit) {
                    if (top < 0) top = y
                    bottom = y
                }
            }
            heights += if (top < 0) 0 else bottom - top + 1
        }
        android.util.Log.i("WaveformTest", "alturas=$heights")
        return heights.max() to heights.min()
    }

    @Test
    fun waveform_isAliveInSilenceAndGrowsWithVoice() {
        // Reloj manual: así cada advanceTimeBy dibuja de verdad los fotogramas de las animaciones infinitas.
        rule.mainClock.autoAdvance = false
        rule.setContent {
            VoiceNoteAITheme {
                Box(Modifier.fillMaxSize().background(VoltPalette.Background)) {
                    NoteScreen(state = state, onMainButton = {}, onErrorAction = {})
                }
            }
        }

        val (silenceMax, silenceMin) = centerBarHeights()

        state = NoteUiState.Recording(level = 0.9f, speaking = true)
        rule.mainClock.advanceTimeBy(500) // deja asentarse el muelle del nivel
        val (voiceMax, _) = centerBarHeights()

        android.util.Log.i("WaveformTest", "silencio max=$silenceMax min=$silenceMin | voz max=$voiceMax")
        assertTrue("En silencio la onda debe seguir moviéndose (max=$silenceMax, min=$silenceMin)", silenceMax > silenceMin)
        assertTrue("Con voz la onda debe crecer (silencio=$silenceMax, voz=$voiceMax)", voiceMax >= silenceMax * 1.8f)
    }
}
