package com.voicenoteai.wear.presentation.note

import android.view.HapticFeedbackConstants
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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ProgressIndicatorDefaults
import androidx.wear.compose.material3.Text
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices
import androidx.wear.compose.ui.tooling.preview.WearPreviewFontScales
import com.voicenoteai.wear.R
import com.voicenoteai.wear.presentation.theme.AppColors
import com.voicenoteai.wear.presentation.theme.AppIcons
import com.voicenoteai.wear.presentation.theme.VoiceNoteAITheme

/** Medidas que cambian en el breakpoint de 225 dp (PRD §7.7). */
private data class Dimensions(val large: Boolean) {
    val button: Dp = if (large) 112.dp else 96.dp

    /** El anillo y el botón de detener crecen en la misma proporción que el botón principal. */
    val scale: Float = button / 96.dp
    val titleSp: TextUnit = if (large) 16.sp else 14.sp
    val titleLines: Int = if (large) 3 else 2
}

private const val MARGIN = 0.052f

/** Fracción del ancho que ocupa el texto: el lado del cuadrado inscrito, para que la curvatura no lo corte. */
private const val TEXT_WIDTH_FRACTION = 0.72f

/**
 * La única pantalla de la app (PRD §7.5). Sin estado propio: lo recibe del [NoteViewModel].
 *
 * [onMainButton] es el toque del botón central (iniciar en INICIAL, detener en GRABANDO);
 * [onErrorAction] es el botón de ERROR.
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
            .background(AppColors.Background),
        contentAlignment = Alignment.Center,
    ) {
        val dimensions = Dimensions(large = maxWidth >= 225.dp)
        val textWidth = maxWidth * TEXT_WIDTH_FRACTION
        AnimatedContent(
            targetState = state,
            // Solo cambiar de estado anima; los cambios dentro de un estado (nivel, etapa) no.
            contentKey = { it::class },
            transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(150)) },
            modifier = Modifier
                .fillMaxSize()
                .padding(maxWidth * MARGIN),
            contentAlignment = Alignment.Center,
            label = "state",
        ) { current ->
            when (current) {
                is NoteUiState.Idle -> IdleContent(dimensions, current.starting, onMainButton)
                is NoteUiState.Recording -> RecordingContent(dimensions, current, onMainButton)
                is NoteUiState.Processing -> ProcessingContent(current.stage, textWidth)
                is NoteUiState.Success -> SuccessContent(dimensions, current.title, textWidth)
                is NoteUiState.Error -> ErrorContent(current, textWidth, onErrorAction)
            }
        }
    }
}

@Composable
private fun IdleContent(dimensions: Dimensions, starting: Boolean, onMainButton: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        CircularButton(
            diameter = dimensions.button,
            description = stringResource(R.string.idle_record_description),
            onClick = onMainButton,
            enabled = !starting,
            modifier = Modifier.border(2.dp, AppColors.Idle, CircleShape),
        ) {
            Icon(AppIcons.Microphone, contentDescription = null, tint = AppColors.Idle, modifier = Modifier.size(40.dp))
        }
        Spacer(Modifier.height(16.dp))
        StatusText(stringResource(R.string.idle_hint), 12.sp, AppColors.TextSecondary)
    }
}

@Composable
private fun RecordingContent(dimensions: Dimensions, state: NoteUiState.Recording, onMainButton: () -> Unit) {
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

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            progress = { progress.value },
            modifier = Modifier.fillMaxSize(),
            colors = ProgressIndicatorDefaults.colors(
                indicatorColor = AppColors.Recording,
                trackColor = AppColors.Recording.copy(alpha = 0.15f),
            ),
            strokeWidth = 3.dp,
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            val ringMax = 140.dp * dimensions.scale
            Box(Modifier.size(ringMax), contentAlignment = Alignment.Center) {
                VoiceRing(level, min = 100.dp * dimensions.scale, max = ringMax)
                CircularButton(
                    diameter = 75.dp * dimensions.scale,
                    description = stringResource(R.string.recording_stop_description),
                    onClick = onMainButton,
                    modifier = Modifier.background(AppColors.Recording, CircleShape),
                ) {
                    Icon(AppIcons.Stop, contentDescription = null, tint = AppColors.Background, modifier = Modifier.size(32.dp))
                }
            }
            Spacer(Modifier.height(4.dp))
            StatusText(stringResource(R.string.recording_listening), 12.sp, AppColors.Error)
        }
    }
}

/** Círculo concéntrico cuyo diámetro sigue la amplitud real de la voz (PRD §7.4, CA-02). */
@Composable
private fun VoiceRing(level: Float, min: Dp, max: Dp) {
    Canvas(Modifier.size(max)) {
        val radius = lerp(min, max, level).toPx() / 2
        drawCircle(AppColors.Recording.copy(alpha = 0.18f + 0.22f * level), radius)
        drawCircle(AppColors.Recording.copy(alpha = 0.6f), radius, style = Stroke(2.dp.toPx()))
    }
}

@Composable
private fun ProcessingContent(stage: ProcessingStage, textWidth: Dp) {
    val description = stringResource(R.string.processing_description)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator(
            modifier = Modifier
                .size(72.dp)
                .semantics { contentDescription = description },
            colors = ProgressIndicatorDefaults.colors(
                indicatorColor = AppColors.Processing,
                trackColor = Color.Transparent,
            ),
            strokeWidth = 4.dp,
        )
        Spacer(Modifier.height(12.dp))
        StatusText(stringResource(stage.textRes), 14.sp, AppColors.Processing, Modifier.widthIn(max = textWidth), maxLines = 2)
    }
}

@Composable
private fun SuccessContent(dimensions: Dimensions, title: String, textWidth: Dp) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        AnimatedCheck(Modifier.size(56.dp))
        // El título es la única prueba de que el sistema entendió (§7.4): es lo que se anuncia.
        Text(
            text = title,
            fontSize = dimensions.titleSp,
            color = AppColors.TextPrimary,
            textAlign = TextAlign.Center,
            maxLines = dimensions.titleLines,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .widthIn(max = textWidth)
                .semantics { liveRegion = LiveRegionMode.Polite },
        )
        Text(stringResource(R.string.success_saved), fontSize = 12.sp, color = AppColors.Success)
    }
}

/** Check verde que se dibuja con un trazo de ~300 ms. */
@Composable
private fun AnimatedCheck(modifier: Modifier = Modifier) {
    val stroke = remember { Animatable(0f) }
    LaunchedEffect(Unit) { stroke.animateTo(1f, tween(300)) }
    val description = stringResource(R.string.success_saved_description)
    Canvas(modifier.semantics { contentDescription = description }) {
        val width = 5.dp.toPx()
        drawCircle(AppColors.Success, radius = size.minDimension / 2 - width / 2, style = Stroke(width))
        val check = Path().apply {
            moveTo(size.width * 0.28f, size.height * 0.52f)
            lineTo(size.width * 0.44f, size.height * 0.67f)
            lineTo(size.width * 0.73f, size.height * 0.36f)
        }
        val measure = PathMeasure().apply { setPath(check, false) }
        val partial = Path()
        measure.getSegment(0f, measure.length * stroke.value, partial, true)
        drawPath(partial, AppColors.Success, style = Stroke(width, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
private fun ErrorContent(state: NoteUiState.Error, textWidth: Dp, onAction: (ErrorAction) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(AppIcons.Warning, contentDescription = null, tint = AppColors.Error, modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(4.dp))
        StatusText(stringResource(state.messageRes), 14.sp, AppColors.TextPrimary, Modifier.widthIn(max = textWidth), maxLines = 2)
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = { onAction(state.action) },
            modifier = Modifier.heightIn(min = 48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = AppColors.Error,
                contentColor = AppColors.Background,
            ),
        ) {
            Text(stringResource(state.action.labelRes), fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
    }
}

/** Texto de estado: centrado y anunciado por TalkBack cuando cambia (§7.6). */
@Composable
private fun StatusText(
    text: String,
    fontSize: TextUnit,
    color: Color,
    modifier: Modifier = Modifier,
    maxLines: Int = 1,
) {
    Text(
        text = text,
        fontSize = fontSize,
        color = color,
        textAlign = TextAlign.Center,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier.semantics { liveRegion = LiveRegionMode.Polite },
    )
}

/** Botón redondo propio: el de Material no ofrece la escala a 0,92 al presionar (§7.4). */
@Composable
private fun CircularButton(
    diameter: Dp,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.92f else 1f, label = "scale")
    val view = LocalView.current
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(diameter)
            .scale(scale)
            .clip(CircleShape)
            .then(modifier)
            .clickable(interactionSource = interaction, indication = null, enabled = enabled) {
                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                onClick()
            }
            .semantics {
                role = Role.Button
                contentDescription = description
            },
    ) {
        content()
    }
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
