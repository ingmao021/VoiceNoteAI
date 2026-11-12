package com.voicenoteai.wear.presentation.history

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
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
import com.voicenoteai.wear.domain.repository.ReminderSettings
import com.voicenoteai.wear.presentation.settings.durationLabel
import com.voicenoteai.wear.presentation.theme.AppIcons
import com.voicenoteai.wear.presentation.theme.HangingTab
import com.voicenoteai.wear.presentation.theme.EdgeIcon
import com.voicenoteai.wear.presentation.theme.StateBadge
import com.voicenoteai.wear.presentation.theme.VoiceNoteAITheme
import com.voicenoteai.wear.presentation.theme.VoltPalette
import java.time.LocalDate

/** Medidas de la página de historial, como fracción del lado de la pantalla (skill `smartwatch-app-design`, §4). */
private class HistoryLayout(side: Dp, fontScale: Float) {
    private val large = side >= 225.dp
    /** Con la fuente ampliada el bloque de estado ocupa más alto: se compensa con un badge menor. */
    private val compact = !large && fontScale >= 1.25f

    /** Las filas quedan dentro del cuadrado inscrito incluso al acercarse al borde inferior. */
    val horizontal: Dp = side * 0.115f
    val textWidth: Dp = if (compact) side * 0.80f else side * 0.72f

    /** Deja libre la pestaña colgante (3 dp de margen + 34 dp) y un respiro: el contenido cuelga debajo. */
    val edge: Dp = side * 0.24f
    val headerTop: Dp = side * 0.01f
    val headerBottom: Dp = side * 0.03f

    /** Holgura al final de la lista: la última fila puede subir por encima de la curva del bisel. */
    val bottom: Dp = side * 0.20f

    val titleSp: TextUnit = if (large) 16.sp else 15.sp
    val taskSp: TextUnit = if (large) 13.sp else 12.sp
    val dueSp: TextUnit = 11.sp

    /** El bloque de estado comparte sitio con el botón del borde: se encoge en pantallas pequeñas. */
    val badge: Dp = when {
        large -> side * 0.34f
        compact -> side * 0.18f
        else -> side * 0.29f
    }
    /** Separación del bloque de estado: se aprieta cuando la fuente ampliada pide más alto. */
    val stateGap: Dp = if (compact) 4.dp else 6.dp
    val check: Dp = if (large) 26.dp else 24.dp
    val rowPadding: Dp = 6.dp
    val rowHeight: Dp = if (large) 42.dp else 38.dp
}

/**
 * Segunda página: historial de tareas recientes de Google Tasks.
 *
 * [onToggle] recibe el id y el nuevo estado de la casilla; [onRetry] recarga el historial; [onClose]
 * vuelve a la nota (el botón del borde y el gesto de bajar).
 */
@Composable
fun HistoryScreen(
    state: HistoryUiState,
    onToggle: (String, Boolean) -> Unit,
    onDelete: (String) -> Unit,
    onRemind: (String, Long) -> Unit,
    onRetry: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    /** `true` mientras la página está a la vista: el lazo se balancea al llegar. */
    active: Boolean = true,
    /** Minutos de antelación del aviso al deslizar a la derecha (se define en Ajustes). */
    reminderMinutes: Long = ReminderSettings.DEFAULT_MINUTES,
    /** `true`: deslizar para borrar pide confirmación antes de eliminar. */
    confirmDelete: Boolean = false,
) {
    var deleteFor by remember { mutableStateOf<String?>(null) }
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val side = min(maxWidth, maxHeight)
        val layout = HistoryLayout(side, LocalDensity.current.fontScale)

        // Todo se compone dentro de un cuadrado centrado: si la ventana llega más alta que ancha
        // (arranque o pantalla rectangular), el contenido y el botón no se separan ni se pisan.
        Box(
            Modifier
                .size(side)
                .align(Alignment.Center),
        ) {
            Box(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().padding(top = layout.edge)) {
                // El título solo acompaña a la lista: en vacío/error/cargando el bloque va solo,
                // como en la maqueta, para que quepa entero por encima del botón del borde.
                if (state is HistoryUiState.Content) Header(layout)
                when (state) {
                    is HistoryUiState.Loading -> LoadingContent(layout)
                    is HistoryUiState.Empty -> EmptyContent(layout, onClose)
                    is HistoryUiState.Error -> ErrorContent(layout, state, onRetry, onClose)
                    is HistoryUiState.Content -> HistoryList(
                        layout,
                        state,
                        onToggle,
                        onDelete = { id -> if (confirmDelete) deleteFor = id else onDelete(id) },
                        confirmDelete = confirmDelete,
                    ) { id -> onRemind(id, reminderMinutes) }
                }
            }
            // Una pestaña que cuelga del borde superior, con la flecha hacia abajo: pulsarla o arrastrarla
            // vuelve a «Toca para hablar» (también sirven el gesto de bajar y «Atrás»).
            val dragThreshold = with(LocalDensity.current) { 24.dp.toPx() }
            HangingTab(
                onClick = onClose,
                description = stringResource(R.string.history_back),
                swing = active,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .pointerInput(onClose) {
                        var dragged = 0f
                        detectDragGestures(
                            onDragStart = { dragged = 0f },
                            onDrag = { change, amount ->
                                change.consume()
                                val before = dragged
                                dragged += amount.getDistance()
                                if (before < dragThreshold && dragged >= dragThreshold) onClose()
                            },
                        )
                    }
                    .testTag("edgeAction"),
            ) {
                EdgeIcon(AppIcons.ChevronDownRounded, size = 24.dp)
            }
            deleteFor?.let { id ->
                DeleteConfirm(
                    layout = layout,
                    onConfirm = { deleteFor = null; onDelete(id) },
                    onCancel = { deleteFor = null },
                )
            }
            }
        }
    }
}

@Composable
private fun Header(layout: HistoryLayout) {
    Text(
        text = stringResource(R.string.history_title),
        fontSize = layout.titleSp,
        fontWeight = FontWeight.SemiBold,
        color = VoltPalette.OnSurface,
        textAlign = TextAlign.Center,
        maxLines = 1,
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = layout.horizontal,
                end = layout.horizontal,
                top = layout.headerTop,
                bottom = layout.headerBottom,
            )
            .testTag("historyHeader"),
    )
}

@Composable
private fun ColumnScope.HistoryList(
    layout: HistoryLayout,
    state: HistoryUiState.Content,
    onToggle: (String, Boolean) -> Unit,
    onDelete: (String) -> Unit,
    confirmDelete: Boolean,
    onRemind: (String) -> Unit,
) {
    if (state.messageRes != null) {
        Text(
            text = stringResource(state.messageRes),
            fontSize = layout.dueSp,
            fontWeight = FontWeight.SemiBold,
            // Texto siempre neutro: el color de estado se reserva a iconos y formas.
            color = VoltPalette.OnSurface,
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
            .weight(1f)
            .testTag("historyList"),
        // La lista llega hasta el borde de la pantalla (nada de franja negra tapando filas); la
        // holgura va en el contenido, así la última fila se puede desplazar hasta quedar visible.
        contentPadding = PaddingValues(top = 2.dp, bottom = layout.bottom),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        items(items = state.tasks, key = { it.id }) { task ->
            SwipeToDeleteRow(
                onDelete = { onDelete(task.id) },
                onRemind = { onRemind(task.id) },
                layout = layout,
                returnAfterDelete = confirmDelete,
                modifier = Modifier.padding(horizontal = layout.horizontal),
            ) {
                TaskRow(
                    task = task,
                    today = state.today,
                    updating = task.id in state.updatingIds,
                    onToggle = onToggle,
                    layout = layout,
                )
            }
        }
    }
}

/**
 * Izquierda: pasado el umbral la fila se borra al soltar. Derecha: pasado el umbral programa un
 * recordatorio (con el tiempo de Ajustes) y la fila vuelve a su sitio.
 */
@Composable
private fun SwipeToDeleteRow(
    onDelete: () -> Unit,
    onRemind: () -> Unit,
    layout: HistoryLayout,
    returnAfterDelete: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val offset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val threshold = with(density) { 72.dp.toPx() }
    val maxRight = threshold * 1.6f

    Box(modifier = modifier.fillMaxWidth()) {
        if (offset.value > 0f) {
            Box(
                contentAlignment = Alignment.CenterStart,
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(percent = 50))
                    .background(VoltPalette.Secondary.copy(alpha = (offset.value / threshold).coerceIn(0.2f, 1f)))
                    .padding(start = 18.dp)
                    .testTag("remindReveal"),
            ) {
                Text(
                    text = stringResource(R.string.history_reminder_action),
                    fontSize = layout.dueSp,
                    fontWeight = FontWeight.SemiBold,
                    color = VoltPalette.Background,
                    maxLines = 1,
                )
            }
        }
        if (offset.value < 0f) {
            Box(
                contentAlignment = Alignment.CenterEnd,
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(percent = 50))
                    .background(VoltPalette.Error.copy(alpha = (abs(offset.value) / threshold).coerceIn(0.2f, 1f)))
                    .padding(end = 18.dp)
                    .testTag("deleteReveal"),
            ) {
                Text(
                    text = stringResource(R.string.history_delete_action),
                    fontSize = layout.dueSp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                )
            }
        }
        Box(
            modifier = Modifier
                .offset { IntOffset(offset.value.roundToInt(), 0) }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            scope.launch {
                                offset.snapTo((offset.value + dragAmount).coerceIn(-threshold * 1.6f, maxRight))
                            }
                        },
                        onDragEnd = {
                            scope.launch {
                                if (offset.value <= -threshold) {
                                    onDelete()
                                    // Con confirmación la fila sigue en la lista: vuelve a su sitio.
                                    if (returnAfterDelete) offset.animateTo(0f)
                                } else if (offset.value >= threshold) {
                                    onRemind()
                                    offset.animateTo(0f)
                                } else {
                                    offset.animateTo(0f)
                                }
                            }
                        },
                        onDragCancel = { scope.launch { offset.animateTo(0f) } },
                    )
                },
        ) { content() }
    }
}

/** Confirmación de borrado: «Eliminar» borra; tocar fuera o «Cancelar» la deja como estaba. */
@Composable
private fun DeleteConfirm(layout: HistoryLayout, onConfirm: () -> Unit, onCancel: () -> Unit) {
    val shape = RoundedCornerShape(percent = 50)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(VoltPalette.Background.copy(alpha = 0.92f))
            .clickable(onClick = onCancel)
            .testTag("deleteConfirm"),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(horizontal = layout.horizontal * 1.6f),
        ) {
            Text(
                text = stringResource(R.string.history_delete_confirm),
                fontSize = layout.dueSp,
                fontWeight = FontWeight.SemiBold,
                color = VoltPalette.OnSurface,
            )
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 40.dp)
                    .clip(shape)
                    .background(VoltPalette.Error)
                    .clickable(onClick = onConfirm)
                    .testTag("deleteConfirmYes"),
            ) {
                Text(
                    text = stringResource(R.string.history_delete_action),
                    fontSize = layout.taskSp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                )
            }
            Text(
                text = stringResource(R.string.history_reminder_cancel),
                fontSize = layout.dueSp,
                color = VoltPalette.OnSurface,
                modifier = Modifier.clickable(onClick = onCancel).padding(vertical = 4.dp),
            )
        }
    }
}

/** Una tarea: chip de píldora; tocar la fila entera marca o desmarca, con una casilla visible a la izquierda. */
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
    // Pendiente = tonal azul; hecha = tonal oscuro y tachada. Ninguna usa verde sólido.
    val surface = if (task.completed) VoltPalette.SurfaceContainer else VoltPalette.SecondaryContainer

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = layout.rowHeight)
            .clip(shape)
            .background(surface)
            .testTag("historyRow")
            .toggleable(
                value = task.completed,
                enabled = !updating,
                role = Role.Checkbox,
                onValueChange = { onToggle(task.id, it) },
            )
            .padding(start = layout.rowPadding, end = 14.dp, top = 3.dp, bottom = 3.dp),
    ) {
        CheckIndicator(completed = task.completed, updating = updating, layout = layout)
        Spacer(Modifier.width(layout.rowPadding))
        Column(Modifier.weight(1f)) {
            Text(
                text = task.title,
                fontSize = layout.taskSp,
                lineHeight = layout.taskSp * 1.1f,
                // El título es contenido del usuario: se muestra tal cual.
                fontWeight = FontWeight.SemiBold,
                color = if (task.completed) VoltPalette.OnSurfaceVariant else VoltPalette.OnSecondaryContainerStrong,
                textDecoration = if (task.completed) TextDecoration.LineThrough else null,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = dueLabelText(dueLabel(task.dueDate, today)),
                fontSize = layout.dueSp,
                lineHeight = layout.dueSp * 1.1f,
                color = if (task.completed) VoltPalette.OnSurfaceVariant else VoltPalette.OnSecondaryContainer,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun CheckIndicator(completed: Boolean, updating: Boolean, layout: HistoryLayout) {
    val base = Modifier
        .size(layout.check)
        .clip(CircleShape)
        .alpha(if (updating) 0.5f else 1f)
    Box(
        contentAlignment = Alignment.Center,
        modifier = if (completed) {
            base.background(VoltPalette.PrimaryContainer)
        } else {
            base.border(2.dp, VoltPalette.Secondary, CircleShape)
        },
    ) {
        if (completed) {
            Icon(
                AppIcons.Check,
                contentDescription = null,
                tint = VoltPalette.Primary,
                modifier = Modifier.size(layout.check * 0.6f),
            )
        }
    }
}

@Composable
private fun ColumnScope.LoadingContent(layout: HistoryLayout) {
    CenteredState(layout) {
        StateBadge(layout.badge, modifier = Modifier.testTag("stateBadge")) {
            CircularProgressIndicator(
                modifier = Modifier.size(layout.badge * 0.6f),
                colors = ProgressIndicatorDefaults.colors(
                    indicatorColor = VoltPalette.Secondary,
                    trackColor = VoltPalette.SurfaceContainerHigh,
                ),
                strokeWidth = 5.dp,
            )
        }
        StateText(
            stringResource(R.string.history_loading),
            layout.titleSp,
            modifier = Modifier.testTag("stateTitle"),
            maxLines = 3,
        )
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
        StateBadge(layout.badge, modifier = Modifier.testTag("stateBadge")) {
            Icon(
                AppIcons.Check,
                contentDescription = null,
                tint = VoltPalette.Primary,
                modifier = Modifier.size(layout.badge * 0.52f),
            )
        }
        StateText(
            stringResource(R.string.history_empty),
            layout.titleSp,
            modifier = Modifier.testTag("stateTitle"),
            maxLines = 3,
        )
        StateText(
            stringResource(R.string.history_empty_hint),
            layout.dueSp,
            modifier = Modifier.testTag("stateHint"),
            color = VoltPalette.OnSurfaceVariant,
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
        StateBadge(layout.badge, modifier = Modifier.testTag("stateBadge")) {
            Icon(
                AppIcons.Warning,
                contentDescription = null,
                tint = VoltPalette.Error,
                modifier = Modifier.size(layout.badge * 0.52f),
            )
        }
        StateText(
            stringResource(state.messageRes),
            layout.titleSp,
            modifier = Modifier.testTag("stateTitle"),
            maxLines = 3,
        )
        StateText(
            stringResource(hint),
            layout.dueSp,
            modifier = Modifier.testTag("stateHint"),
            color = VoltPalette.OnSurfaceVariant,
        )
    }
}

/** Bloque centrado de los estados sin lista, siempre por encima del botón del borde. */
@Composable
private fun ColumnScope.CenteredState(
    layout: HistoryLayout,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            // El botón del borde está arriba y la columna ya lo esquiva: se equilibra con el mismo hueco
            // abajo para que el bloque quede en el centro real del círculo y no roce el bisel.
            .padding(bottom = layout.edge),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(layout.stateGap),
            modifier = modifier
                .widthIn(max = layout.textWidth)
                .testTag("historyState"),
            content = content,
        )
    }
}

@Composable
private fun StateText(
    text: String,
    fontSize: TextUnit,
    modifier: Modifier = Modifier,
    color: Color = VoltPalette.OnSurface,
    maxLines: Int = 2,
) {
    Text(
        text = text,
        fontSize = fontSize,
        // Interlineado proporcional: el alto por defecto (20 sp) se come el bloque con fuente grande.
        lineHeight = fontSize * 1.15f,
        fontWeight = FontWeight.SemiBold,
        color = color,
        textAlign = TextAlign.Center,
        maxLines = maxLines,
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
    Box(Modifier.background(VoltPalette.Background)) {
        HistoryScreen(state, onToggle = { _, _ -> }, onDelete = {}, onRemind = { _, _ -> }, onRetry = {}, onClose = {})
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
