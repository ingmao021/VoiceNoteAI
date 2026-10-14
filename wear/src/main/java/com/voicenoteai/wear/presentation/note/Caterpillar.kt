package com.voicenoteai.wear.presentation.note

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.voicenoteai.wear.presentation.theme.FurPalette
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private const val SEGMENTS = 7

/** Separación angular entre segmentos, en radianes. */
private const val SPACING = 0.36f

private const val LAP_MS = 2_400
private const val CRAWL_MS = 600

/**
 * Indicador de PROCESANDO: un gusanito bordado en hilo crema que da vueltas dentro del parche. El cuerpo
 * ondula hacia dentro y hacia fuera, como si avanzara reptando.
 */
@Composable
fun CaterpillarSpinner(description: String, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "caterpillar")
    val lap by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(LAP_MS, easing = LinearEasing)),
        label = "lap",
    )
    val crawl by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(CRAWL_MS, easing = LinearEasing), RepeatMode.Restart),
        label = "crawl",
    )

    Canvas(modifier.semantics { contentDescription = description }) {
        val orbit = size.minDimension * 0.30f
        val unit = size.minDimension * 0.075f
        val shadow = Offset(unit * 0.15f, unit * 0.3f)

        // Del último segmento a la cabeza, para que cada uno quede encima del anterior.
        val segments = (SEGMENTS - 1 downTo 0).map { i ->
            val angle = lap - i * SPACING
            val wave = sin(crawl - i * 0.9f) * unit * 0.35f
            val radius = if (i == 0) unit * 1.15f else unit * (1f - i * 0.06f)
            Triple(polar(orbit + wave, angle), radius, angle)
        }
        segments.forEach { (c, r, _) -> drawCircle(Color.Black.copy(alpha = 0.3f), r, c + shadow) }
        segments.forEach { (c, r, _) ->
            drawCircle(FurPalette.Thread, r, c)
            // Costura del segmento: un aro más oscuro que lo separa del vecino.
            drawCircle(FurPalette.Processing.copy(alpha = 0.55f), r * 0.82f, c, style = Stroke(r * 0.12f))
        }
        val (head, headRadius, headAngle) = segments.last()
        drawFace(head, headRadius, headAngle)
    }
}

private fun DrawScope.polar(radius: Float, angle: Float) =
    Offset(center.x + radius * cos(angle), center.y + radius * sin(angle))

/** Ojos y antenas mirando hacia donde avanza (tangente de la órbita, sentido horario). */
private fun DrawScope.drawFace(head: Offset, radius: Float, angle: Float) {
    val forward = Offset(-sin(angle), cos(angle))
    val outward = Offset(cos(angle), sin(angle))
    val eyeBase = head + forward * (radius * 0.35f)
    listOf(-1f, 1f).forEach { side ->
        val eye = eyeBase + outward * (radius * 0.42f * side)
        drawCircle(FurPalette.Ink, radius * 0.2f, eye)
        drawCircle(Color.White, radius * 0.07f, eye + forward * (radius * 0.06f))
        val root = head + forward * (radius * 0.7f) + outward * (radius * 0.45f * side)
        val tip = root + forward * (radius * 0.75f) + outward * (radius * 0.4f * side)
        drawLine(FurPalette.Thread, root, tip, strokeWidth = radius * 0.16f, cap = StrokeCap.Round)
        drawCircle(FurPalette.Thread, radius * 0.17f, tip)
    }
}
