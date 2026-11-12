package com.voicenoteai.wear.presentation.note

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.runtime.State
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import com.voicenoteai.wear.presentation.theme.VoltPalette
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

private const val ARC_START = 135f
private const val ARC_SWEEP = 270f

/**
 * Anillo de ESCUCHANDO a pantalla completa (skill `smartwatch-app-design`, patrón C):
 *  - arco de 270° con degradado azul→verde que se completa en [progress] (los 60 s de la guarda), con un
 *    cabezal luminoso en la punta;
 *  - un cometa azul que gira sin parar por dentro, para que se vea que el reloj está atento;
 *  - un resplandor central que respira con la voz ([level], 0..1).
 *
 * Todo lo animado se lee dentro del dibujo: ni el giro ni el resplandor recomponen nada.
 */
@Composable
fun ListeningRing(progress: () -> Float, level: () -> Float, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "ring")
    val spin = transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(2600, easing = LinearEasing)),
        label = "spin",
    )
    val breath = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1800), RepeatMode.Reverse),
        label = "breath",
    )

    Canvas(modifier) {
        val stroke = 7.dp.toPx()
        val inset = stroke / 2 + 3.dp.toPx()
        val arcSize = Size(size.width - 2 * inset, size.height - 2 * inset)
        val arcTopLeft = Offset(inset, inset)
        val radius = arcSize.width / 2
        val c = center
        val voice = level()

        // Resplandor central: el volumen de la voz lo infla; en silencio solo respira.
        val glowRadius = size.width * (0.30f + 0.10f * breath.value + 0.12f * voice)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(VoltPalette.Secondary.copy(alpha = 0.10f + 0.26f * voice), Color.Transparent),
                center = c,
                radius = glowRadius,
            ),
            radius = glowRadius,
            center = c,
        )

        // Pista.
        drawArc(
            color = VoltPalette.SurfaceContainerHigh,
            startAngle = ARC_START,
            sweepAngle = ARC_SWEEP,
            useCenter = false,
            topLeft = arcTopLeft,
            size = arcSize,
            style = Stroke(stroke, cap = StrokeCap.Round),
        )

        // Progreso con degradado: el azul del inicio se funde con el verde de la punta.
        val sweep = (ARC_SWEEP * progress().coerceIn(0f, 1f)).coerceAtLeast(0.5f)
        val end = (sweep / 360f).coerceAtLeast(0.01f)
        rotate(ARC_START, pivot = c) {
            drawArc(
                brush = Brush.sweepGradient(
                    0f to VoltPalette.Secondary,
                    end to VoltPalette.Primary,
                    1f to VoltPalette.Primary,
                    center = c,
                ),
                startAngle = 0f,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
        }

        // Cabezal luminoso en la punta del arco.
        val headAngle = (ARC_START + sweep) * (PI.toFloat() / 180f)
        val head = Offset(c.x + radius * cos(headAngle), c.y + radius * sin(headAngle))
        drawCircle(VoltPalette.Primary.copy(alpha = 0.16f), stroke * 2.1f, head)
        drawCircle(VoltPalette.Primary.copy(alpha = 0.34f), stroke * 1.4f, head)
        drawCircle(VoltPalette.Primary, stroke * 0.62f, head)

        // Cometa que gira por dentro del arco.
        val cometInset = inset + stroke + 7.dp.toPx()
        val cometSweep = 110f
        rotate(spin.value, pivot = c) {
            drawArc(
                brush = Brush.sweepGradient(
                    0f to Color.Transparent,
                    cometSweep / 360f to VoltPalette.Secondary.copy(alpha = 0.55f + 0.45f * voice),
                    (cometSweep + 1f) / 360f to Color.Transparent,
                    1f to Color.Transparent,
                    center = c,
                ),
                startAngle = 0f,
                sweepAngle = cometSweep,
                useCenter = false,
                topLeft = Offset(cometInset, cometInset),
                size = Size(size.width - 2 * cometInset, size.height - 2 * cometInset),
                style = Stroke(3.dp.toPx(), cap = StrokeCap.Round),
            )
        }
    }
}

/** Progreso lineal 0→1 en [durationMs]; arranca al entrar en la composición. */
@Composable
fun rememberCountdownProgress(durationMs: Int): State<Float> {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) { progress.animateTo(1f, tween(durationMs, easing = LinearEasing)) }
    return progress.asState()
}

/**
 * Ecualizador de voz: [BARS] barras redondeadas, más altas en el centro, que bailan con una fase
 * continua y crecen con [level]. En silencio quedan casi planas y respiran: se nota que escucha.
 * Degradado azul en los extremos → verde en el centro.
 */
@Composable
fun VoiceWaveform(level: () -> Float, speaking: () -> Boolean, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "wave")
    val phase = transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(1300, easing = LinearEasing)),
        label = "phase",
    )

    Canvas(modifier) {
        val barWidth = size.width / (BARS * 2f - 1f)
        val gap = barWidth
        val minHeight = barWidth
        val voice = if (speaking()) level().coerceIn(0f, 1f) else 0f
        val mid = (BARS - 1) / 2f
        for (i in 0 until BARS) {
            val fromCenter = abs(i - mid) / mid // 0 centro … 1 extremo
            val envelope = 1f - 0.62f * fromCenter * fromCenter
            // Cada barra con su desfase: la onda recorre el ecualizador en vez de latir toda a la vez.
            val wobble = 0.5f + 0.5f * sin(phase.value + i * 0.85f)
            // Base alta para que en silencio las barras superen su alto mínimo y se vea que respiran.
            val energy = 0.26f + 0.74f * voice
            val h = max(minHeight, size.height * envelope * energy * (0.35f + 0.65f * wobble))
            val x = i * (barWidth + gap) + barWidth / 2
            drawLine(
                color = lerpColor(VoltPalette.Secondary, VoltPalette.Primary, 1f - fromCenter),
                start = Offset(x, size.height / 2 - h / 2),
                end = Offset(x, size.height / 2 + h / 2),
                strokeWidth = barWidth,
                cap = StrokeCap.Round,
            )
        }
    }
}

private const val BARS = 9

private fun lerpColor(a: Color, b: Color, t: Float): Color = androidx.compose.ui.graphics.lerp(a, b, t)
