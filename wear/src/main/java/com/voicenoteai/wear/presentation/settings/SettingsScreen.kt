package com.voicenoteai.wear.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
    options: List<Long>,
    onStep: (index: Int, delta: Int) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize().background(VoltPalette.Background)) {
        val side = min(maxWidth, maxHeight)
        Box(Modifier.size(side).align(Alignment.Center)) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = side * 0.13f, vertical = side * 0.16f)
                    .testTag("settingsPage"),
            ) {
                Text(
                    text = stringResource(R.string.settings_title),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = VoltPalette.OnSurface,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
                Text(
                    text = stringResource(R.string.settings_reminder_times),
                    fontSize = 11.sp,
                    color = VoltPalette.OnSurface,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
                options.forEachIndexed { index, minutes ->
                    OptionRow(
                        minutes = minutes,
                        onMinus = { onStep(index, -1) },
                        onPlus = { onStep(index, +1) },
                        index = index,
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

@Composable
private fun OptionRow(minutes: Long, onMinus: () -> Unit, onPlus: () -> Unit, index: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(percent = 50))
            .background(VoltPalette.SecondaryContainer)
            .padding(horizontal = 4.dp, vertical = 3.dp),
    ) {
        StepButton("−", stringResource(R.string.settings_decrease), onMinus, Modifier.testTag("optionMinus$index"))
        Text(
            text = durationLabel(minutes),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = VoltPalette.OnSurface,
            maxLines = 1,
            modifier = Modifier.testTag("optionValue$index"),
        )
        StepButton("+", stringResource(R.string.settings_increase), onPlus, Modifier.testTag("optionPlus$index"))
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
