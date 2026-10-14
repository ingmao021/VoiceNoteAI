package com.voicenoteai.wear.presentation.note

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.ProgressIndicatorDefaults
import androidx.wear.compose.material3.Text
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices
import androidx.wear.compose.ui.tooling.preview.WearPreviewFontScales
import com.voicenoteai.wear.R
import com.voicenoteai.wear.presentation.theme.AppIcons
import com.voicenoteai.wear.presentation.theme.DisplayFont
import com.voicenoteai.wear.presentation.theme.FurPalette
import com.voicenoteai.wear.presentation.theme.VoiceNoteAITheme

/**
 * Composición común a los cinco estados, como fracción del lado de la pantalla (skill `wear-ui-ux`, §2):
 * el parche siempre en el mismo sitio y el texto debajo, dentro del cuadrado inscrito.
 */
private class Layout(side: Dp) {
    // Parche y texto suben respecto al diseño original para que el texto secundario (TOCA PARA …) no
    // caiga en la zona donde la curva de la pantalla lo estrecha.
    val patch: Dp = side * 0.45f
    val patchTop: Dp = side * 0.13f
    val textTop: Dp = side * 0.62f
    val textWidth: Dp = side * 0.72f

    /** Breakpoint de 225 dp (PRD §7.7). */
    private val large = side >= 225.dp
    val primarySp: TextUnit = if (large) 16.sp else 14.sp
    val secondarySp: TextUnit = if (large) 12.sp else 11.sp
}

/**
 * La única pantalla de la app (PRD §7.5). Sin estado propio: lo recibe del [NoteViewModel].
 *
 * [onMainButton] es el toque del parche en INICIAL (grabar) y GRABANDO (detener); [onErrorAction] es
 * el toque del parche en ERROR.
 */
@Composable
fun NoteScreen(
    state: NoteUiState,
    onMainButton: () -> Unit,
    onErrorAction: (ErrorAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            // En pantallas cuadradas las esquinas quedan fuera del fondo redondo.
            .background(FurPalette.Base),
    ) {
        val layout = Layout(min(maxWidth, maxHeight))
        Image(
            painterResource(R.drawable.fur_background),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        AnimatedContent(
            targetState = state,
            // Solo cambiar de estado anima; los cambios dentro de un estado (nivel, etapa) no.
            contentKey = { it::class },
            transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(150)) },
            modifier = Modifier.fillMaxSize(),
            label = "state",
        ) { current ->
            when (current) {
                is NoteUiState.Idle -> IdleContent(layout, current.starting, onMainButton)
                is NoteUiState.Recording -> RecordingContent(layout, current, onMainButton)
                is NoteUiState.Processing -> ProcessingContent(layout, current.stage)
                is NoteUiState.Success -> SuccessContent(layout, current.title)
                is NoteUiState.Error -> ErrorContent(layout, current, onErrorAction)
            }
        }
    }
}

/** Coloca el parche y el bloque de texto en sus posiciones fijas; [behind] queda debajo de ambos. */
@Composable
private fun StateLayout(
    layout: Layout,
    patch: @Composable () -> Unit,
    text: @Composable ColumnScope.() -> Unit,
    behind: @Composable BoxScope.() -> Unit = {},
) {
    Box(Modifier.fillMaxSize()) {
        behind()
        Box(Modifier.align(Alignment.TopCenter).padding(top = layout.patchTop)) { patch() }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = layout.textTop)
                .widthIn(max = layout.textWidth),
            content = text,
        )
    }
}

@Composable
private fun IdleContent(layout: Layout, starting: Boolean, onMainButton: () -> Unit) {
    StateLayout(
        layout,
        patch = {
            // El micrófono bordado viene en la imagen, tal cual el diseño.
            FurPatch(
                R.drawable.patch_idle,
                layout.patch,
                onClick = onMainButton,
                description = stringResource(R.string.idle_record_description),
                enabled = !starting,
            )
        },
        text = { StatusText(stringResource(R.string.idle_hint), layout.primarySp) },
    )
}

@Composable
private fun RecordingContent(layout: Layout, state: NoteUiState.Recording, onMainButton: () -> Unit) {
    // Arco del borde: se completa en los 60 s de la guarda. Arranca al entrar en GRABANDO, que es
    // cuando el ViewModel arma la guarda (onReadyForSpeech).
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(NoteViewModel.MAX_DURATION_MS.toInt(), easing = LinearEasing))
    }
    // El RmsNormalizer ya suaviza; el muelle quita el escalón entre muestras (~10 Hz) sin retrasar.
    val level by animateFloatAsState(
        targetValue = if (state.speaking) state.level else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "level",
    )
    val ringMax = layout.patch * 1.22f

    StateLayout(
        layout,
        behind = {
            CircularProgressIndicator(
                progress = { progress.value },
                modifier = Modifier.fillMaxSize(),
                colors = ProgressIndicatorDefaults.colors(
                    indicatorColor = FurPalette.Recording,
                    trackColor = FurPalette.Ink.copy(alpha = 0.1f),
                ),
                strokeWidth = 4.dp,
            )
            VoiceRing(
                level,
                min = layout.patch,
                max = ringMax,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = layout.patchTop - (ringMax - layout.patch) / 2),
            )
        },
        patch = {
            FurPatch(
                R.drawable.patch_recording,
                layout.patch,
                onClick = onMainButton,
                description = stringResource(R.string.recording_stop_description),
            ) {
                ThreadIcon(AppIcons.Stop, layout.patch * 0.4f)
            }
        },
        text = { StatusText(stringResource(R.string.recording_listening), layout.primarySp) },
    )
}

/** Halo rojo tras el parche cuyo diámetro sigue la amplitud real de la voz (PRD §7.4, CA-02). */
@Composable
private fun VoiceRing(level: Float, min: Dp, max: Dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(max)) {
        val radius = lerp(min, max, level).toPx() / 2
        drawCircle(FurPalette.Recording.copy(alpha = 0.15f + 0.2f * level), radius)
        drawCircle(FurPalette.Recording.copy(alpha = 0.5f), radius, style = Stroke(2.dp.toPx()))
    }
}

@Composable
private fun ProcessingContent(layout: Layout, stage: ProcessingStage) {
    val description = stringResource(R.string.processing_description)
    StateLayout(
        layout,
        patch = {
            FurPatch(R.drawable.patch_processing, layout.patch) {
                CaterpillarSpinner(description, Modifier.size(layout.patch))
            }
        },
        text = { StatusText(stringResource(stage.textRes), layout.primarySp, maxLines = 2) },
    )
}

@Composable
private fun SuccessContent(layout: Layout, title: String) {
    StateLayout(
        layout,
        patch = {
            FurPatch(R.drawable.patch_success, layout.patch) {
                AnimatedCheck(Modifier.size(layout.patch * 0.42f))
            }
        },
        text = {
            // El título es la única prueba de que el sistema entendió (§7.4): es lo que se anuncia.
            // Es contenido del usuario, así que no se pasa a mayúsculas.
            StatusText(title, layout.primarySp, maxLines = 2, uppercase = false)
            StatusText(stringResource(R.string.success_saved), layout.secondarySp, color = FurPalette.InkSoft)
        },
    )
}

/** Check bordado que se traza en ~300 ms. */
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
        translate(1.dp.toPx(), 2.dp.toPx()) { drawPath(partial, Color.Black.copy(alpha = 0.35f), style = style) }
        drawPath(partial, FurPalette.Thread, style = style)
    }
}

@Composable
private fun ErrorContent(layout: Layout, state: NoteUiState.Error, onAction: (ErrorAction) -> Unit) {
    StateLayout(
        layout,
        patch = {
            FurPatch(
                R.drawable.patch_error,
                layout.patch,
                onClick = { onAction(state.action) },
                description = stringResource(state.action.labelRes),
            ) {
                ThreadIcon(AppIcons.Warning, layout.patch * 0.42f)
            }
        },
        text = {
            StatusText(stringResource(state.messageRes), layout.primarySp, maxLines = 2)
            StatusText(stringResource(state.action.hintRes), layout.secondarySp, color = FurPalette.InkSoft)
        },
    )
}

/**
 * Texto de estado con la tipografía del diseño (Lilita One, mayúsculas, tinta sobre crema). Centrado y
 * anunciado por TalkBack cuando cambia (§7.6). Las cadenas siguen en minúscula en strings.xml.
 */
@Composable
private fun StatusText(
    text: String,
    fontSize: TextUnit,
    maxLines: Int = 1,
    color: Color = FurPalette.Ink,
    uppercase: Boolean = true,
) {
    Text(
        text = if (uppercase) text.uppercase() else text,
        fontSize = fontSize,
        fontFamily = DisplayFont,
        lineHeight = fontSize * 1.1f,
        color = color,
        textAlign = TextAlign.Center,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
    )
}

// ——— Vistas previas: los cinco estados en pantalla pequeña y grande, y con fuente al 130 % ———

@Composable
private fun Preview(state: NoteUiState) = VoiceNoteAITheme { NoteScreen(state, {}, {}) }

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
