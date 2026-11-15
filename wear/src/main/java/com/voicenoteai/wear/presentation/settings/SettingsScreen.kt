package com.voicenoteai.wear.presentation.settings

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.scale
import com.voicenoteai.wear.presentation.theme.VoltSwitch
import com.voicenoteai.wear.presentation.theme.popIn
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
import androidx.compose.foundation.layout.height
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
                contentPadding = PaddingValues(top = side * 0.14f, bottom = side * 0.32f),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = side * 0.10f)
                    .testTag("settingsPage"),
            ) {
                item {
                    Text(
                        text = stringResource(R.string.settings_title),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = VoltPalette.OnSurface,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        modifier = Modifier.popIn(from = 0.9f),
                    )
                }
                item { SectionTitle(R.string.settings_section_feedback) }
                item {
                    ToggleRow(
                        label = stringResource(R.string.settings_haptics),
                        value = stringResource(if (hapticsEnabled) R.string.settings_on else R.string.settings_off),
                        checked = hapticsEnabled,
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
                        checked = confirmDelete,
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
            // Las filas se desvanecen hacia el negro bajo el asa, en vez de cortarse contra ella.
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(side * 0.26f)
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            listOf(androidx.compose.ui.graphics.Color.Transparent, VoltPalette.Background),
                        ),
                    ),
            )
            BottomTab(
                onClick = onClose,
                description = stringResource(R.string.settings_back),
                modifier = Modifier.align(Alignment.BottomCenter).testTag("edgeAction"),
            ) {
                EdgeIcon(AppIcons.ChevronDownRounded, size = 20.dp)
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
        text = stringResource(textRes).uppercase(),
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.2.sp,
        color = VoltPalette.Secondary,
        textAlign = TextAlign.Center,
        maxLines = 1,
        modifier = Modifier.padding(top = 8.dp, bottom = 0.dp),
    )
}

/** Cambio de valor con deslizamiento vertical corto: se nota que cambió sin distraer. */
private fun valueSwap() =
    (slideInVertically(tween(220)) { it / 2 } + fadeIn(tween(220))) togetherWith
        (slideOutVertically(tween(160)) { -it / 2 } + fadeOut(tween(120)))

/** Se comprime al pulsar: la misma respuesta táctil que el botón principal. */
@Composable
private fun pressScale(source: MutableInteractionSource): Float {
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.95f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "press",
    )
    return scale
}

/**
 * Fila de una opción que se alterna o cicla al tocarla: etiqueta y valor actual a la izquierda y, si
 * es un interruptor ([checked] no nulo), la perilla animada a la derecha. Alto mínimo de 52 dp.
 */
@Composable
private fun ToggleRow(label: String, value: String, onClick: () -> Unit, tag: String, checked: Boolean? = null) {
    val source = remember { MutableInteractionSource() }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .scale(pressScale(source))
            .clip(RoundedCornerShape(24.dp))
            .background(VoltPalette.SurfaceContainer)
            .clickable(interactionSource = source, indication = null, onClick = onClick)
            .padding(start = 16.dp, end = 12.dp, top = 6.dp, bottom = 6.dp)
            .testTag(tag),
    ) {
        Column(Modifier.weight(1f)) {
            Text(text = label, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = VoltPalette.OnSurface, maxLines = 1)
            AnimatedContent(targetState = value, transitionSpec = { valueSwap() }, label = "toggleValue") { v ->
                Text(
                    text = v,
                    fontSize = 12.sp,
                    color = VoltPalette.OnSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
        if (checked != null) VoltSwitch(checked)
    }
}

@Composable
private fun OptionRow(minutes: Long, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(VoltPalette.SurfaceContainer)
            .padding(horizontal = 6.dp, vertical = 6.dp),
    ) {
        StepButton(AppIcons.Minus, stringResource(R.string.settings_decrease), onMinus, Modifier.testTag("optionMinus"))
        AnimatedContent(
            targetState = minutes,
            // Sube al aumentar y baja al disminuir.
            transitionSpec = {
                val up = targetState > initialState
                (slideInVertically(tween(200)) { if (up) it else -it } + fadeIn(tween(200))) togetherWith
                    (slideOutVertically(tween(160)) { if (up) -it else it } + fadeOut(tween(120)))
            },
            label = "minutes",
        ) { m ->
            Text(
                text = durationLabel(m),
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = VoltPalette.Primary,
                maxLines = 1,
                modifier = Modifier.testTag("optionValue"),
            )
        }
        StepButton(AppIcons.Plus, stringResource(R.string.settings_increase), onPlus, Modifier.testTag("optionPlus"))
    }
}

@Composable
private fun StepButton(icon: androidx.compose.ui.graphics.vector.ImageVector, description: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val source = remember { MutableInteractionSource() }
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(40.dp)
            .scale(pressScale(source))
            .clip(CircleShape)
            .background(VoltPalette.SecondaryContainer)
            .clickable(interactionSource = source, indication = null, onClick = onClick)
            .semantics { contentDescription = description },
    ) {
        androidx.wear.compose.material3.Icon(icon, contentDescription = null, tint = VoltPalette.OnSecondaryContainer, modifier = Modifier.size(18.dp))
    }
}
