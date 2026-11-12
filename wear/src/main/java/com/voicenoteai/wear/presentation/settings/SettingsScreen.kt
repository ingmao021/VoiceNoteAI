package com.voicenoteai.wear.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.Text
import com.voicenoteai.wear.R
import com.voicenoteai.wear.domain.repository.AlertMode
import com.voicenoteai.wear.presentation.theme.AppIcons
import com.voicenoteai.wear.presentation.theme.BottomTab
import com.voicenoteai.wear.presentation.theme.EdgeIcon
import com.voicenoteai.wear.presentation.theme.VoltPalette

/**
 * Página de Ajustes (encima de «Toca para hablar»): los tiempos que ofrece el aviso del historial.
 * Cada fila tiene − y + que recorren los escalones de minutos y horas.
 */
@Composable
fun SettingsScreen(
    reminderMinutes: Long,
    hapticsEnabled: Boolean,
    confirmDelete: Boolean,
    alertMode: AlertMode,
    onStep: (delta: Int) -> Unit,
    onToggleHaptics: () -> Unit,
    onToggleConfirmDelete: () -> Unit,
    onCycleAlertMode: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize().background(VoltPalette.Background)) {
        val side = min(maxWidth, maxHeight)
        Box(Modifier.size(side).align(Alignment.Center)) {
            // Lista desplazable con secciones; el relleno inferior deja la última fila libre del botón.
            LazyColumn(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(top = side * 0.14f, bottom = side * 0.30f),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = side * 0.13f)
                    .testTag("settingsPage"),
            ) {
                item {
                    Text(
                        text = stringResource(R.string.settings_title),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = VoltPalette.OnSurface,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                    )
                }
                item { SectionTitle(R.string.settings_section_feedback) }
                item {
                    ToggleRow(
                        label = stringResource(R.string.settings_haptics),
                        value = stringResource(if (hapticsEnabled) R.string.settings_on else R.string.settings_off),
                        onClick = onToggleHaptics,
                        tag = "hapticsRow",
                    )
                }
                item {
                    ToggleRow(
                        label = stringResource(R.string.settings_alert),
                        value = stringResource(alertMode.labelRes()),
                        onClick = onCycleAlertMode,
                        tag = "alertModeRow",
                    )
                }
                item { SectionTitle(R.string.settings_section_history) }
                item {
                    ToggleRow(
                        label = stringResource(R.string.settings_confirm_delete),
                        value = stringResource(if (confirmDelete) R.string.settings_on else R.string.settings_off),
                        onClick = onToggleConfirmDelete,
                        tag = "confirmDeleteRow",
                    )
                }
                item { SectionTitle(R.string.settings_reminder_times) }
                item {
                    OptionRow(
                        minutes = reminderMinutes,
                        onMinus = { onStep(-1) },
                        onPlus = { onStep(+1) },
                    )
                }
            }
            BottomTab(
                onClick = onClose,
                description = stringResource(R.string.settings_back),
                modifier = Modifier.align(Alignment.BottomCenter).testTag("edgeAction"),
            ) {
                EdgeIcon(AppIcons.ChevronDown, size = 24.dp)
            }
        }
    }
}

private fun AlertMode.labelRes(): Int = when (this) {
    AlertMode.SOUND_AND_VIBRATE -> R.string.settings_alert_sound_vibrate
    AlertMode.VIBRATE -> R.string.settings_alert_vibrate
    AlertMode.SILENT -> R.string.settings_alert_silent
}

@Composable
private fun SectionTitle(textRes: Int) {
    Text(
        text = stringResource(textRes),
        fontSize = 11.sp,
        color = VoltPalette.Secondary,
        textAlign = TextAlign.Center,
        maxLines = 1,
        modifier = Modifier.padding(top = 4.dp),
    )
}

/** Fila de una opción que se alterna o cicla al tocarla: etiqueta arriba, valor actual debajo. */
@Composable
private fun ToggleRow(label: String, value: String, onClick: () -> Unit, tag: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .clip(RoundedCornerShape(percent = 50))
            .background(VoltPalette.SecondaryContainer)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 5.dp)
            .testTag(tag),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = label, fontSize = 11.sp, color = VoltPalette.OnSurface, maxLines = 1)
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = VoltPalette.OnSurface,
            maxLines = 1,
        )
    }
}

@Composable
private fun OptionRow(minutes: Long, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(percent = 50))
            .background(VoltPalette.SecondaryContainer)
            .padding(horizontal = 4.dp, vertical = 3.dp),
    ) {
        StepButton("−", stringResource(R.string.settings_decrease), onMinus, Modifier.testTag("optionMinus"))
        Text(
            text = durationLabel(minutes),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = VoltPalette.OnSurface,
            maxLines = 1,
            modifier = Modifier.testTag("optionValue"),
        )
        StepButton("+", stringResource(R.string.settings_increase), onPlus, Modifier.testTag("optionPlus"))
    }
}

@Composable
private fun StepButton(symbol: String, description: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(VoltPalette.Background.copy(alpha = 0.5f))
            .clickable(onClick = onClick)
            .semantics { contentDescription = description },
    ) {
        Text(text = symbol, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = VoltPalette.OnSurface)
    }
}
