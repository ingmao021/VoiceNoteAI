package com.voicenoteai.wear.presentation.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ProgressIndicatorDefaults
import androidx.wear.compose.material3.Text
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices
import androidx.wear.compose.ui.tooling.preview.WearPreviewFontScales
import com.voicenoteai.wear.R
import com.voicenoteai.wear.domain.model.HistoryTask
import com.voicenoteai.wear.presentation.theme.AppIcons
import com.voicenoteai.wear.presentation.theme.DisplayFont
import com.voicenoteai.wear.presentation.theme.FurBackground
import com.voicenoteai.wear.presentation.theme.FurPalette
import com.voicenoteai.wear.presentation.theme.VoiceNoteAITheme
import java.time.LocalDate

/** Medidas de la página de historial, como fracción del lado de la pantalla (skill `wear-ui-ux`, §2). */
private class HistoryLayout(side: Dp) {
    private val large = side >= 225.dp

    /** Las filas quedan dentro del cuadrado inscrito incluso al acercarse al borde inferior. */
    val horizontal: Dp = side * 0.13f
    val textWidth: Dp = side * 0.72f
    val headerTop: Dp = side * 0.07f
    val bottom: Dp = side * 0.16f

    val touch: Dp = 48.dp
    val backIcon: Dp = if (large) 22.dp else 20.dp
    val titleSp: TextUnit = if (large) 16.sp else 14.sp
    val taskSp: TextUnit = if (large) 13.sp else 12.sp
    val dueSp: TextUnit = if (large) 11.sp else 10.sp

    val badge: Dp = side * 0.26f
    val check: Dp = if (large) 26.dp else 22.dp
    val checkGap: Dp = if (large) 10.dp else 8.dp
    val rowPadding: Dp = if (large) 10.dp else 8.dp
    val rowVertical: Dp = if (large) 7.dp else 6.dp
}

/**
 * Segunda página: historial de tareas recientes de Google Tasks.
 *
 * [onToggle] recibe el id y el nuevo estado de la casilla; [onRetry] recarga el historial; [onClose]
 * vuelve a la nota (la cabecera y el gesto de bajar).
 */
@Composable
fun HistoryScreen(
    state: HistoryUiState,
    onToggle: (String, Boolean) -> Unit,
    onRetry: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val layout = HistoryLayout(min(maxWidth, maxHeight))

        Column(Modifier.fillMaxSize()) {
            Header(layout, onClose)
            when (state) {
                is HistoryUiState.Loading -> LoadingContent(layout)
                is HistoryUiState.Empty -> EmptyContent(layout, onClose)
                is HistoryUiState.Error -> ErrorContent(layout, state, onRetry, onClose)
                is HistoryUiState.Content -> HistoryList(layout, state, onToggle)
            }
        }
    }
}

@Composable
private fun Header(layout: HistoryLayout, onClose: () -> Unit) {
    val back = stringResource(R.string.history_back)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = layout.horizontal, end = layout.horizontal, top = layout.headerTop),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(layout.touch)
                .clip(CircleShape)
                .clickable(role = Role.Button, onClick = onClose)
                .semantics { contentDescription = back },
        ) {
            Icon(
                AppIcons.ChevronDown,
                contentDescription = null,
                tint = FurPalette.Ink,
                modifier = Modifier.size(layout.backIcon),
            )
        }
        Spacer(Modifier.weight(1f))
        Text(
            text = stringResource(R.string.history_title).uppercase(),
            fontSize = layout.titleSp,
            fontFamily = DisplayFont,
            color = FurPalette.Ink,
            maxLines = 1,
        )
        Spacer(Modifier.weight(1f))
        // Mantiene el título centrado aunque solo haya un botón a la izquierda.
        Spacer(Modifier.size(layout.touch))
    }
}

@Composable
private fun ColumnScope.HistoryList(
    layout: HistoryLayout,
    state: HistoryUiState.Content,
    onToggle: (String, Boolean) -> Unit,
) {
    if (state.messageRes != null) {
        Text(
            text = stringResource(state.messageRes).uppercase(),
            fontSize = layout.dueSp,
            fontFamily = DisplayFont,
            // Texto siempre en tinta: el color de estado se reserva a iconos y formas.
            color = FurPalette.Ink,
            textAlign = TextAlign.Center,
            maxLines = 2,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = layout.horizontal, vertical = 2.dp)
                .semantics { liveRegion = LiveRegionMode.Polite },
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
        contentPadding = PaddingValues(top = 2.dp, bottom = layout.bottom),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items(items = state.tasks, key = { it.id }) { task ->
            TaskRow(
                task = task,
                today = state.today,
                updating = task.id in state.updatingIds,
                onToggle = onToggle,
                layout = layout,
                modifier = Modifier.padding(horizontal = layout.horizontal),
            )
        }
    }
}

/** Una tarea: tocar la fila entera marca o desmarca, con una casilla visible a la izquierda. */
@Composable
private fun TaskRow(
    task: HistoryTask,
    today: LocalDate,
    updating: Boolean,
    onToggle: (String, Boolean) -> Unit,
    layout: HistoryLayout,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(percent = 50)
    val surface = if (task.completed) {
        FurPalette.Thread.copy(alpha = 0.40f)
    } else {
        FurPalette.Thread.copy(alpha = 0.65f)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(surface)
            .border(1.dp, FurPalette.InkSoft.copy(alpha = 0.18f), shape)
            .toggleable(
                value = task.completed,
                enabled = !updating,
                role = Role.Checkbox,
                onValueChange = { onToggle(task.id, it) },
            )
            .padding(horizontal = layout.rowPadding, vertical = layout.rowVertical),
    ) {
        CheckIndicator(completed = task.completed, updating = updating, layout = layout)
        Spacer(Modifier.width(layout.checkGap))
        Column(Modifier.weight(1f)) {
            Text(
                text = task.title,
                fontSize = layout.taskSp,
                lineHeight = layout.taskSp * 1.15f,
                // El título es contenido del usuario: no se pasa a mayúsculas (skill `wear-ui-ux`, §4).
                fontFamily = DisplayFont,
                color = if (task.completed) FurPalette.InkSoft else FurPalette.Ink,
                textDecoration = if (task.completed) TextDecoration.LineThrough else null,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = dueLabelText(dueLabel(task.dueDate, today)),
                fontSize = layout.dueSp,
                fontFamily = DisplayFont,
                color = FurPalette.InkSoft,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun CheckIndicator(completed: Boolean, updating: Boolean, layout: HistoryLayout) {
    val shape = CircleShape
    val base = Modifier
        .size(layout.check)
        .clip(shape)
        .alpha(if (updating) 0.5f else 1f)
    Box(
        contentAlignment = Alignment.Center,
        modifier = if (completed) {
            base.background(FurPalette.Success)
        } else {
            base.border(1.5.dp, FurPalette.InkSoft.copy(alpha = 0.55f), shape)
        },
    ) {
        if (completed) {
            Icon(
                AppIcons.Check,
                contentDescription = null,
                tint = FurPalette.Thread,
                modifier = Modifier.size(layout.check * 0.68f),
            )
        }
    }
}

@Composable
private fun ColumnScope.LoadingContent(layout: HistoryLayout) {
    CenteredState(layout) {
        Badge(layout) {
            CircularProgressIndicator(
                modifier = Modifier.size(layout.badge * 0.52f),
                colors = ProgressIndicatorDefaults.colors(
                    indicatorColor = FurPalette.Idle,
                    trackColor = FurPalette.Ink.copy(alpha = 0.15f),
                ),
            )
        }
        StateText(stringResource(R.string.history_loading), layout.titleSp)
    }
}

@Composable
private fun ColumnScope.EmptyContent(layout: HistoryLayout, onClose: () -> Unit) {
    CenteredState(
        layout,
        modifier = Modifier
            .clickable(role = Role.Button, onClick = onClose)
            .semantics(mergeDescendants = true) {},
    ) {
        Badge(layout) {
            Icon(
                AppIcons.Check,
                contentDescription = null,
                tint = FurPalette.Idle,
                modifier = Modifier.size(layout.badge * 0.52f),
            )
        }
        StateText(stringResource(R.string.history_empty), layout.titleSp)
        StateText(
            stringResource(R.string.history_empty_hint),
            layout.dueSp,
            color = FurPalette.InkSoft,
        )
    }
}

@Composable
private fun ColumnScope.ErrorContent(
    layout: HistoryLayout,
    state: HistoryUiState.Error,
    onRetry: () -> Unit,
    onClose: () -> Unit,
) {
    val action = if (state.retryable) onRetry else onClose
    val hint = if (state.retryable) R.string.history_retry else R.string.history_empty_hint
    CenteredState(
        layout,
        modifier = Modifier
            .clickable(role = Role.Button, onClick = action)
            .semantics(mergeDescendants = true) {},
    ) {
        Badge(layout) {
            Icon(
                AppIcons.Warning,
                contentDescription = null,
                tint = FurPalette.Error,
                modifier = Modifier.size(layout.badge * 0.52f),
            )
        }
        StateText(stringResource(state.messageRes), layout.titleSp)
        StateText(stringResource(hint), layout.dueSp, color = FurPalette.InkSoft)
    }
}

/** Bloque centrado de los estados sin lista, dentro del ancho seguro de la pantalla redonda. */
@Composable
private fun ColumnScope.CenteredState(
    layout: HistoryLayout,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        modifier = modifier
            .align(Alignment.CenterHorizontally)
            .widthIn(max = layout.textWidth)
            .fillMaxWidth()
            .weight(1f)
            .padding(vertical = layout.horizontal),
        content = content,
    )
}

@Composable
private fun Badge(layout: HistoryLayout, content: @Composable () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(layout.badge)
            .clip(CircleShape)
            .background(FurPalette.Thread.copy(alpha = 0.60f))
            .border(1.dp, FurPalette.InkSoft.copy(alpha = 0.20f), CircleShape),
    ) {
        content()
    }
}

@Composable
private fun StateText(
    text: String,
    fontSize: TextUnit,
    modifier: Modifier = Modifier,
    color: Color = FurPalette.Ink,
) {
    Text(
        text = text.uppercase(),
        fontSize = fontSize,
        fontFamily = DisplayFont,
        color = color,
        textAlign = TextAlign.Center,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier.semantics { liveRegion = LiveRegionMode.Polite },
    )
}

@Composable
private fun dueLabelText(label: DueLabel): String = when (label) {
    DueLabel.None -> stringResource(R.string.history_due_none)
    DueLabel.Today -> stringResource(R.string.history_due_today)
    DueLabel.Tomorrow -> stringResource(R.string.history_due_tomorrow)
    is DueLabel.Date -> label.text
}

// ——— Vistas previas ———

private val previewToday = LocalDate.of(2026, 11, 7)

private val previewTasks = listOf(
    HistoryTask("1", "Comprar café", previewToday, completed = false),
    HistoryTask("2", "Llamar a Juan mañana", previewToday.plusDays(1), completed = false),
    HistoryTask("3", "Enviar el informe", previewToday.plusDays(7), completed = true),
    HistoryTask("4", "Revisar la propuesta de diseño", null, completed = false),
)

@Composable
private fun Preview(state: HistoryUiState) = VoiceNoteAITheme {
    Box {
        FurBackground()
        HistoryScreen(state, onToggle = { _, _ -> }, onRetry = {}, onClose = {})
    }
}

@WearPreviewDevices
@WearPreviewFontScales
@Composable
private fun ContentPreview() = Preview(
    HistoryUiState.Content(previewTasks, previewToday, updatingIds = setOf("1")),
)

@WearPreviewDevices
@Composable
private fun EmptyPreview() = Preview(HistoryUiState.Empty)

@WearPreviewDevices
@Composable
private fun LoadingPreview() = Preview(HistoryUiState.Loading)

@WearPreviewDevices
@Composable
private fun ErrorPreview() = Preview(
    HistoryUiState.Error(R.string.error_history_no_connection, retryable = true),
)
