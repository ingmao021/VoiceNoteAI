package com.voicenoteai.wear.presentation

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.voicenoteai.wear.R
import com.voicenoteai.wear.domain.model.HistoryTask
import com.voicenoteai.wear.presentation.history.HistoryScreen
import com.voicenoteai.wear.presentation.history.HistoryUiState
import com.voicenoteai.wear.presentation.note.ErrorAction
import com.voicenoteai.wear.presentation.note.NoteScreen
import com.voicenoteai.wear.presentation.note.NoteUiState
import com.voicenoteai.wear.presentation.note.ProcessingStage
import com.voicenoteai.wear.presentation.theme.VoiceNoteAITheme
import com.voicenoteai.wear.presentation.theme.VoltPalette
import java.io.File
import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.hypot
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Revisión visual automatizada en el emulador (reloj redondo de 192 dp): cada estado debe quedar
 * centrado, entero dentro del círculo y sin que el botón del borde tape el texto. Se ejecuta dos
 * veces, con la fuente del sistema al 100 % y al 130 %, y guarda una captura de cada caso.
 */
@RunWith(AndroidJUnit4::class)
class LayoutSymmetryTest {

    @get:Rule
    val rule = createComposeRule()

    private val baseDensity: Float =
        InstrumentationRegistry.getInstrumentation().targetContext.resources.displayMetrics.density

    private val fontScales = listOf(1f, 1.3f)

    private data class NoteCase(val name: String, val state: NoteUiState, val hasEdge: Boolean)

    private data class HistoryCase(val name: String, val state: HistoryUiState)

    @Test
    fun noteScreen_isSymmetricAndNothingIsCovered() {
        val cases = listOf(
            NoteCase("inicial", NoteUiState.Idle(), hasEdge = true),
            NoteCase("grabando", NoteUiState.Recording(level = 0.8f, speaking = true), hasEdge = false),
            NoteCase("procesando", NoteUiState.Processing(ProcessingStage.WAKING_SERVER), hasEdge = false),
            NoteCase("guardado", NoteUiState.Success("Comprar café mañana en la tarde"), hasEdge = false),
            NoteCase(
                "error",
                NoteUiState.Error(R.string.error_submission_relink_account, ErrorAction.GO_BACK),
                hasEdge = true,
            ),
        )
        var state by mutableStateOf(cases.first().state)
        var scale by mutableFloatStateOf(1f)

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(baseDensity, scale)) {
                VoiceNoteAITheme {
                    Box(Modifier.fillMaxSize().background(VoltPalette.Background)) {
                        NoteScreen(state = state, onMainButton = {}, onErrorAction = {})
                    }
                }
            }
        }

        for (fontScale in fontScales) {
            scale = fontScale
            for (case in cases) {
                state = case.state
                settle()
                verifyNote(case, fontScale)
            }
        }
    }

    @Test
    fun historyScreen_isSymmetricAndNothingIsCovered() {
        val today = LocalDate.of(2026, 11, 11)
        val tasks = listOf(
            HistoryTask("1", "Comprar café", today, completed = false),
            HistoryTask("2", "Llamar a Juan mañana", today.plusDays(1), completed = false),
        )
        val cases = listOf(
            HistoryCase("cargando", HistoryUiState.Loading),
            HistoryCase("vacio", HistoryUiState.Empty),
            HistoryCase("error", HistoryUiState.Error(R.string.error_submission_relink_account)),
            HistoryCase("lista", HistoryUiState.Content(tasks, today)),
        )
        var state by mutableStateOf(cases.first().state)
        var scale by mutableFloatStateOf(1f)

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(baseDensity, scale)) {
                VoiceNoteAITheme {
                    Box(Modifier.fillMaxSize().background(VoltPalette.Background)) {
                        HistoryScreen(
                            state = state,
                            onToggle = { _, _ -> },
                            onDelete = {},
                            onRemind = { _, _ -> },
                            onRetry = {},
                            onClose = {},
                        )
                    }
                }
            }
        }

        for (fontScale in fontScales) {
            scale = fontScale
            for (case in cases) {
                state = case.state
                settle()
                verifyHistory(case, fontScale)
            }
        }
    }

    private fun verifyNote(case: NoteCase, scale: Float) {
        val label = "nota/${case.name}/fuente$scale"
        val side = side()

        val main = bounds("noteMain")
        assertCentered("$label icono", main, side)
        assertInsideCircle("$label icono", main, side)

        val text = bounds("noteText")
        assertCentered("$label texto", text, side)
        if (case.state is NoteUiState.Success) {
            // En GUARDADO el bloque mezcla un título ancho con una píldora estrecha: se revisan
            // por separado para no medir la esquina del rectángulo que los envuelve a los dos.
            val title = bounds("noteTitle")
            val pill = bounds("notePill")
            assertCentered("$label título", title, side)
            assertInsideCircle("$label título", title, side)
            assertCentered("$label píldora", pill, side)
            assertInsideCircle("$label píldora", pill, side)
        } else {
            assertInsideCircle("$label texto", text, side)
        }

        if (case.hasEdge) {
            val edge = bounds("edgeAction")
            assertCentered("$label borde", edge, side)
            assertNotCovered("$label texto", text, edge)
        } else {
            assertTrue("$label no debe mostrar botón de borde", !exists("edgeAction"))
        }
        screenshot("nota_${case.name}_fuente${scale.toString().replace('.', '_')}")
    }

    private fun verifyHistory(case: HistoryCase, scale: Float) {
        val label = "historial/${case.name}/fuente$scale"
        val side = side()

        val edge = bounds("edgeAction")
        assertCentered("$label borde", edge, side)

        if (case.state is HistoryUiState.Content) {
            val header = bounds("historyHeader")
            assertCentered("$label título", header, side)
            assertBelow("$label título", header, edge)

            val list = listBounds()
            assertBelow("$label lista", list, edge)

            rows().forEachIndexed { index, row ->
                // Solo las filas enteras dentro del viewport: las demás están recortadas por el scroll.
                if (row.bottom.value <= list.bottom.value + 0.5f) {
                    assertCentered("$label fila $index", row, side)
                    // Las filas son píldoras: la esquina del rectángulo que las envuelve sobresale del
                    // círculo cerca del borde inferior aunque lo visible quepa; ahí solo se exige
                    // que la fila esté por encima de la franja de holgura inferior.
                    if (row.bottom.value <= side.value * 0.78f) assertInsideCircle("$label fila $index", row, side)
                    assertBelow("$label fila $index", row, edge)
                }
            }
        } else {
            assertTrue("$label no debe mostrar el título de la lista", !exists("historyHeader"))
            val block = bounds("historyState")
            assertCentered("$label bloque", block, side)
            // Cada pieza por separado: el rectángulo que une badge + título + pista es más grande
            // que el contenido real y daría falsos positivos contra el círculo.
            listOf("stateBadge", "stateTitle", "stateHint").forEach { tag ->
                if (exists(tag)) {
                    val piece = bounds(tag)
                    assertCentered("$label $tag", piece, side)
                    assertInsideCircle("$label $tag", piece, side)
                }
            }
            assertBelow("$label bloque", block, edge)
        }
        screenshot("historial_${case.name}_fuente${scale.toString().replace('.', '_')}")
    }

    private fun settle() {
        rule.mainClock.advanceTimeBy(700)
        rule.waitForIdle()
    }

    private fun side(): Dp {
        val root = rule.onRoot().getUnclippedBoundsInRoot()
        return root.right - root.left
    }

    private fun bounds(tag: String): DpRect =
        rule.onNodeWithTag(tag, useUnmergedTree = true).getUnclippedBoundsInRoot()

    private fun listBounds(): DpRect = bounds("historyList")

    private fun rows(): List<DpRect> = rule
        .onAllNodesWithTag("historyRow", useUnmergedTree = true)
        .fetchSemanticsNodes()
        .indices
        .map { rule.onAllNodesWithTag("historyRow", useUnmergedTree = true)[it].getUnclippedBoundsInRoot() }

    private fun exists(tag: String): Boolean =
        rule.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()

    private fun assertCentered(label: String, area: DpRect, side: Dp) {
        val center = (area.left.value + area.right.value) / 2f
        val expected = side.value / 2f
        assertTrue(
            "$label descentrado: centro x=${center}dp, esperado ${expected}dp (bounds=$area)",
            abs(center - expected) <= 1f,
        )
    }

    /** Ninguna esquina del bloque puede salir del círculo de la pantalla. */
    private fun assertInsideCircle(label: String, area: DpRect, side: Dp) {
        val center = side.value / 2f
        val radius = side.value / 2f - 1f
        for (x in listOf(area.left.value, area.right.value)) {
            for (y in listOf(area.top.value, area.bottom.value)) {
                val distance = hypot(x - center, y - center)
                assertTrue(
                    "$label se sale del círculo: esquina (${x}dp, ${y}dp) a ${"%.1f".format(distance)}dp " +
                        "del centro, radio ${radius}dp (bounds=$area)",
                    distance <= radius,
                )
            }
        }
    }

    /**
     * La pestaña superior queda por encima: el contenido empieza donde ella termina. Su área táctil
     * mide 48 dp pero lo visible solo 37 dp (3 + 34), así que se tolera ese solape de 11 dp.
     */
    private fun assertBelow(label: String, content: DpRect, edge: DpRect) {
        assertTrue(
            "$label queda debajo del botón superior: empieza en ${content.top}dp y el botón " +
                "termina en ${edge.bottom}dp (content=$content, edge=$edge)",
            content.top.value >= edge.bottom.value - 11f,
        )
    }

    private fun assertNotCovered(label: String, content: DpRect, edge: DpRect) {
        assertTrue(
            "$label queda debajo del botón del borde: termina en ${content.bottom}dp y el botón " +
                "empieza en ${edge.top}dp (content=$content, edge=$edge)",
            content.bottom.value <= edge.top.value + 0.5f,
        )
    }

    private fun screenshot(name: String) {
        val bitmap = rule.onRoot().captureToImage().asAndroidBitmap()
        val dir = InstrumentationRegistry.getInstrumentation().targetContext
            .getExternalFilesDir("capturas")
            ?.resolve("layout")
            ?: return
        dir.mkdirs()
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
