package com.voicenoteai.wear.presentation.note

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.voicenoteai.wear.presentation.theme.AppIcons
import com.voicenoteai.wear.presentation.theme.BottomTab
import com.voicenoteai.wear.presentation.theme.EdgeAction
import com.voicenoteai.wear.presentation.theme.EdgeIcon
import com.voicenoteai.wear.presentation.theme.EdgeLabel
import com.voicenoteai.wear.presentation.theme.EdgeStyle
import com.voicenoteai.wear.presentation.theme.StateBadge
import com.voicenoteai.wear.presentation.theme.VoltActionButton
import com.voicenoteai.wear.presentation.theme.VoltPalette
import com.voicenoteai.wear.presentation.theme.VoiceNoteAITheme

/**
 * Composición común a los estados, como fracción del lado de la pantalla (skill `smartwatch-app-design`,
 * §4): el elemento principal siempre en el mismo sitio y el texto debajo, dentro del cuadrado inscrito.
 */
private class Layout(val side: Dp, fontScale: Float) {
    val button: Dp = side * 0.43f
    val buttonTop: Dp = side * 0.115f
    val textTop: Dp = side * 0.59f
    /** Con la fuente ampliada el texto necesita algo más de ancho para no acabar en puntos suspensivos. */
    private val compact = side < 225.dp && fontScale >= 1.25f
    val textWidth: Dp = side * 0.72f
    /** Solo para textos de una línea o anclados arriba: más ancho sin rozar la circunferencia. */
    val wideTextWidth: Dp = if (compact) side * 0.84f else textWidth

    /** Hueco real del botón del borde (48 dp + margen inferior + aire): nada de texto lo invade. */
    val edgeReserve: Dp = 55.dp

    /** El mensaje de error usa una línea más con la fuente ampliada. */
    val errorLines: Int = if (compact) 3 else 2

    /** El badge puede encogerse más cuando el mensaje necesita tres líneas. */
    val badgeMinFactor: Float = if (compact) 0.45f else 0.6f

    /** Breakpoint de 225 dp (PRD §7.7). */
    private val large = side >= 225.dp
    val primarySp: TextUnit = if (large) 16.sp else 15.sp
    val secondarySp: TextUnit = if (large) 12.sp else 11.sp
}

/**
 * La pantalla de la nota (PRD §7.5). Sin estado propio: lo recibe del [NoteViewModel].
 *
 * [onMainButton] es el toque del botón principal en INICIAL (grabar) y GRABANDO (detener);
 * [onErrorAction] es el toque del botón del borde en ERROR; [onOpenHistory] abre el historial.
 */
@Composable
fun NoteScreen(
    state: NoteUiState,
    onMainButton: () -> Unit,
    onErrorAction: (ErrorAction) -> Unit,
    modifier: Modifier = Modifier,
    onOpenHistory: () -> Unit = {},
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val layout = Layout(min(maxWidth, maxHeight), LocalDensity.current.fontScale)
        AnimatedContent(
            targetState = state,
            // Solo cambiar de estado anima; los cambios dentro de un estado (nivel, etapa) no.
            contentKey = { it::class },
            transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(150)) },
            modifier = Modifier.fillMaxSize(),
            label = "state",
        ) { current ->
            when (current) {
                is NoteUiState.Idle -> IdleContent(layout, current.starting, onMainButton, onOpenHistory)
                is NoteUiState.Recording -> RecordingContent(layout, current, onMainButton)
                is NoteUiState.Processing -> ProcessingContent(layout, current.stage)
                is NoteUiState.Success -> SuccessContent(layout, current.title)
                is NoteUiState.Error -> ErrorContent(layout, current, onErrorAction)
            }
        }
    }
}

/** Coloca el elemento principal y el bloque de texto en sus posiciones fijas; [behind] queda debajo. */
@Composable
private fun StateLayout(
    layout: Layout,
    main: @Composable () -> Unit,
    text: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier,
    textTop: Dp = layout.textTop,
    textBottom: Dp = layout.side - 4.dp,
    textWidth: Dp = layout.textWidth,
    behind: @Composable BoxScope.() -> Unit = {},
    edge: @Composable BoxScope.() -> Unit = {},
) {
    Box(modifier.fillMaxSize()) {
        behind()
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .padding(top = layout.buttonTop)
                .testTag("noteMain"),
        ) { main() }
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = textTop)
                .fillMaxWidth()
                .height((textBottom - textTop).coerceAtLeast(0.dp)),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier
                    .widthIn(max = textWidth)
                    .testTag("noteText"),
                content = text,
            )
        }
        edge()
    }
}

@Composable
private fun IdleContent(layout: Layout, starting: Boolean, onMainButton: () -> Unit, onOpenHistory: () -> Unit) {
    StateLayout(
        layout,
        textWidth = layout.wideTextWidth,
        edge = {
            if (!starting) {
                BottomTab(
                    onClick = onOpenHistory,
                    description = stringResource(R.string.history_swipe_description),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .testTag("edgeAction"),
                ) {
                    EdgeIcon(AppIcons.ChevronUp, size = 18.dp)
                    EdgeIcon(AppIcons.History, size = 22.dp)
                }
            }
        },
        main = {
            VoltActionButton(
                diameter = layout.button,
                onClick = onMainButton,
                description = stringResource(R.string.idle_record_description),
                enabled = !starting,
            ) {
                Icon(AppIcons.Mic, contentDescription = null, tint = VoltPalette.OnPrimary, modifier = Modifier.size(layout.button * 0.46f))
            }
        },
        text = { StatusText(stringResource(R.string.idle_hint), layout.primarySp) },
    )
}

@Composable
private fun RecordingContent(layout: Layout, state: NoteUiState.Recording, onMainButton: () -> Unit) {
    // Arco del borde: se completa en los 60 s de la guarda. Arranca al entrar en GRABANDO, que es
    // cuando el ViewModel arma la guarda (onReadyForSpeech).
    val progress = rememberCountdownProgress(NoteViewModel.MAX_DURATION_MS.toInt())
    // El RmsNormalizer ya suaviza; el muelle quita el escalón entre muestras (~10 Hz) sin retrasar.
    val level by animateFloatAsState(
        targetValue = if (state.speaking) state.level else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "level",
    )
    val stopDescription = stringResource(R.string.recording_stop_description)

    // Composición del diseño de ESCUCHANDO: anillo a pantalla completa, texto con tracking y
    // ecualizador en el centro, y el botón de parar abrazando el borde inferior (hueco del arco).
    Box(Modifier.fillMaxSize()) {
        ListeningRing(
            progress = { progress.value },
            level = { level },
            modifier = Modifier.fillMaxSize(),
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(layout.side * 0.05f, Alignment.CenterVertically),
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = layout.side * 0.12f),
        ) {
            Text(
                text = stringResource(R.string.recording_listening).uppercase(),
                fontSize = (layout.secondarySp.value + 1).sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                color = VoltPalette.OnSurface,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .widthIn(max = layout.textWidth)
                    .testTag("noteText")
                    .semantics { liveRegion = LiveRegionMode.Polite },
            )
            VoiceWaveform(
                level = { level },
                speaking = { state.speaking },
                modifier = Modifier
                    .size(width = layout.side * 0.42f, height = layout.side * 0.17f)
                    .testTag("noteMain"),
            )
        }
        EdgeAction(
            onClick = onMainButton,
            description = stopDescription,
            style = EdgeStyle.Primary,
            width = layout.side * 0.34f,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .testTag("noteStop"),
        ) {
            EdgeIcon(AppIcons.Stop, EdgeStyle.Primary, size = 26.dp)
        }
    }
}

@Composable
private fun ProcessingContent(layout: Layout, stage: ProcessingStage) {
    val description = stringResource(R.string.processing_description)
    StateLayout(
        layout,
        main = {
            // Sin verde: no hay nada que pulsar. Anillo indeterminado azul con el micrófono dentro.
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(layout.button)
                    .semantics { contentDescription = description },
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.fillMaxSize(),
                    colors = ProgressIndicatorDefaults.colors(
                        indicatorColor = VoltPalette.Secondary,
                        trackColor = VoltPalette.SurfaceContainerHigh,
                    ),
                    strokeWidth = 8.dp,
                )
                Icon(AppIcons.Mic, contentDescription = null, tint = VoltPalette.Secondary, modifier = Modifier.size(layout.button * 0.38f))
            }
        },
        text = { StatusText(stringResource(stage.textRes), layout.primarySp, maxLines = 2) },
    )
}

@Composable
private fun SuccessContent(layout: Layout, title: String) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val fontScale = density.fontScale
        // El bloque (título + píldora) se ancla abajo y crece hacia arriba: la píldora nunca se sale
        // del círculo por los lados y el título nunca pisa el badge, tampoco con fuente grande.
        val blockBottom = layout.side * 0.87f
        val titleMax = with(density) { (layout.primarySp * 1.2f * 2).toDp() } * fontScale
        val pillHeight = maxOf(16.dp, with(density) { (layout.secondarySp * 1.2f).toDp() }) + 10.dp
        val badge = (blockBottom - titleMax - 6.dp - pillHeight - 6.dp - layout.buttonTop)
            .coerceIn(layout.button * 0.6f, layout.button)

        Box(Modifier.fillMaxSize()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = layout.buttonTop)
                    .size(badge)
                    .clip(CircleShape)
                    .background(VoltPalette.Primary)
                    .testTag("noteMain"),
            ) {
                AnimatedCheck(Modifier.size(badge * 0.46f))
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = layout.side - blockBottom)
                    .widthIn(max = layout.wideTextWidth)
                    .testTag("noteText"),
            ) {
                // El título es la única prueba de que el sistema entendió (§7.4): es lo que se anuncia.
                StatusText(
                    text = title,
                    fontSize = layout.primarySp,
                    maxLines = 2,
                    modifier = Modifier.testTag("noteTitle"),
                )
                SavedBadge(layout)
            }
        }
    }
}

/** Píldora «Guardado», con el icono y el texto centrados en el mismo eje. */
@Composable
private fun SavedBadge(layout: Layout) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(VoltPalette.SurfaceContainer)
            .testTag("notePill")
            .padding(horizontal = 14.dp, vertical = 5.dp),
    ) {
        Icon(AppIcons.Check, contentDescription = null, tint = VoltPalette.Primary, modifier = Modifier.size(16.dp))
        Text(
            text = stringResource(R.string.success_saved),
            fontSize = layout.secondarySp,
            fontWeight = FontWeight.SemiBold,
            color = VoltPalette.Primary,
            maxLines = 1,
        )
    }
}

/** Check que se traza en ~300 ms, en el color del texto sobre el verde. */
@Composable
private fun AnimatedCheck(modifier: Modifier = Modifier) {
    val stroke = remember { Animatable(0f) }
    LaunchedEffect(Unit) { stroke.animateTo(1f, tween(300)) }
    val description = stringResource(R.string.success_saved_description)
    Canvas(modifier.semantics { contentDescription = description }) {
        val check = Path().apply {
            moveTo(size.width * 0.12f, size.height * 0.52f)
            lineTo(size.width * 0.40f, size.height * 0.78f)
            lineTo(size.width * 0.90f, size.height * 0.24f)
        }
        val measure = PathMeasure().apply { setPath(check, false) }
        val partial = Path()
        measure.getSegment(0f, measure.length * stroke.value, partial, true)
        val style = Stroke(size.minDimension * 0.16f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        drawPath(partial, VoltPalette.OnPrimary, style = style)
    }
}

@Composable
private fun ErrorContent(layout: Layout, state: NoteUiState.Error, onAction: (ErrorAction) -> Unit) {
    val retry = state.action == ErrorAction.RESEND || state.action == ErrorAction.SPEAK_AGAIN
    val density = LocalDensity.current
    val fontScale = density.fontScale
    // El mensaje puede ocupar tres líneas: el badge se encoge lo justo para que quepa entero por
    // encima del botón del borde, también con la fuente del sistema al 130 %. `toDp` no aplica la
    // escala de fuente, por eso se multiplica a mano.
    val messageMax = with(density) { (layout.primarySp * 1.2f * layout.errorLines).toDp() } * fontScale + 2.dp
    val textBottom = layout.side - layout.edgeReserve
    val badge = (textBottom - messageMax - 6.dp - layout.buttonTop)
        .coerceIn(layout.button * layout.badgeMinFactor, layout.button)

    StateLayout(
        layout,
        textTop = layout.buttonTop + badge + 6.dp,
        textBottom = textBottom,
        edge = {
            EdgeAction(
                onClick = { onAction(state.action) },
                description = stringResource(state.action.labelRes),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .testTag("edgeAction"),
                style = EdgeStyle.Error,
                width = 128.dp,
            ) {
                if (retry) EdgeIcon(AppIcons.Refresh, EdgeStyle.Error, size = 20.dp)
                EdgeLabel(stringResource(state.action.labelRes), EdgeStyle.Error)
            }
        },
        main = {
            StateBadge(badge) {
                Icon(AppIcons.Warning, contentDescription = null, tint = VoltPalette.Error, modifier = Modifier.size(badge * 0.46f))
            }
        },
        text = { StatusText(stringResource(state.messageRes), layout.primarySp, maxLines = layout.errorLines) },
    )
}

/**
 * Texto de estado: semibold, centrado y anunciado por TalkBack cuando cambia (§7.6). Las cadenas
 * siguen en su caso natural; no se pasan a mayúsculas.
 */
@Composable
private fun StatusText(
    text: String,
    fontSize: TextUnit,
    maxLines: Int = 1,
    color: Color = VoltPalette.OnSurface,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        fontSize = fontSize,
        fontWeight = FontWeight.SemiBold,
        lineHeight = fontSize * 1.15f,
        color = color,
        textAlign = TextAlign.Center,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier.semantics { liveRegion = LiveRegionMode.Polite },
    )
}

// ——— Vistas previas: los cinco estados en pantalla pequeña y grande, y con fuente al 130 % ———

@Composable
private fun Preview(state: NoteUiState) = VoiceNoteAITheme {
    Box(Modifier.background(VoltPalette.Background)) {
        NoteScreen(state, {}, {})
    }
}

@WearPreviewDevices
@Composable
private fun IdlePreview() = Preview(NoteUiState.Idle())

@WearPreviewDevices
@Composable
private fun RecordingPreview() = Preview(NoteUiState.Recording(level = 0.6f, speaking = true))

@WearPreviewDevices
@Composable
private fun ProcessingPreview() = Preview(NoteUiState.Processing(ProcessingStage.WAKING_SERVER))

@WearPreviewDevices
@WearPreviewFontScales
@Composable
private fun SuccessPreview() = Preview(NoteUiState.Success("Comprar café mañana en la tarde"))

@WearPreviewDevices
@WearPreviewFontScales
@Composable
private fun ErrorPreview() = Preview(NoteUiState.Error(R.string.error_submission_relink_account, ErrorAction.GO_BACK))
