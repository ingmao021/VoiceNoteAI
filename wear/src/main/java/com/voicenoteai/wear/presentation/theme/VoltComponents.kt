package com.voicenoteai.wear.presentation.theme

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlinx.coroutines.delay
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.Text

/**
 * Botón de acción principal de una pantalla (skill `smartwatch-app-design`, §4 y §7): se comprime al
 * presionar, vibra y su forma se «morfa» con [cornerPercent] (50 = círculo; menos = estado activo).
 */
@Composable
fun VoltActionButton(
    diameter: Dp,
    onClick: () -> Unit,
    description: String,
    modifier: Modifier = Modifier,
    container: Color = VoltPalette.Primary,
    cornerPercent: Int = 50,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.95f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "scale",
    )
    val percent by animateFloatAsState(
        targetValue = if (pressed) (cornerPercent - 8).coerceAtLeast(20).toFloat() else cornerPercent.toFloat(),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "corner",
    )
    val view = LocalView.current
    val shape = RoundedCornerShape(percent = percent.toInt())
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(diameter)
            .scale(scale)
            .clip(shape)
            // Degradado sutil (más claro arriba) y un aro fino interior: relieve sin sombras ni bordes duros.
            .background(
                if (enabled) {
                    Brush.verticalGradient(listOf(androidx.compose.ui.graphics.lerp(container, Color.White, 0.30f), container))
                } else {
                    SolidColor(VoltPalette.SurfaceContainerHigh)
                },
            )
            .drawBehind {
                if (enabled) {
                    drawCircle(
                        VoltPalette.OnPrimary.copy(alpha = 0.12f),
                        radius = size.minDimension / 2f - 7.dp.toPx(),
                        style = Stroke(1.5.dp.toPx()),
                    )
                }
            }
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

/** Colores de [EdgeAction]: tonal azul (por defecto) o apagado con texto de error. */
enum class EdgeStyle(val container: Color, val content: Color) {
    Tonal(VoltPalette.SecondaryContainer, VoltPalette.OnSecondaryContainer),
    Error(VoltPalette.SurfaceContainerHigh, VoltPalette.Error),

    /** Asas de navegación: casi invisibles, para que no compitan con la acción principal. */
    Quiet(VoltPalette.SurfaceContainerHigh, VoltPalette.OnSurfaceVariant),

    /** Acción principal de la pantalla (p. ej. parar de escuchar): verde con texto/icono oscuro. */
    Primary(VoltPalette.Primary, VoltPalette.OnPrimary),
}

/**
 * Botón que abraza el borde inferior del reloj redondo (skill, §4). Máximo uno por pantalla. Parte
 * superior poco redondeada y parte inferior que sigue la curva del bisel. Alto 44 dp + 4 dp de margen
 * inferior: el área táctil real supera los 48 dp.
 */
@Composable
fun EdgeAction(
    onClick: () -> Unit,
    description: String,
    modifier: Modifier = Modifier,
    style: EdgeStyle = EdgeStyle.Tonal,
    width: Dp = 104.dp,
    content: @Composable RowScope.() -> Unit,
) {
    val shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 46.dp, bottomEnd = 46.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
        modifier = modifier
            .padding(bottom = 3.dp)
            .width(width)
            .defaultMinSize(minHeight = 44.dp)
            .clip(shape)
            .background(style.container)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description }
            .padding(bottom = 4.dp),
    ) {
        content()
    }
}

/**
 * Lazo que cuelga del borde superior: una cuerdecita y, de ella, una pestaña pequeña (64 × 34 dp),
 * plana arriba y redondeada abajo. Cuando [swing] pasa a `true` (se llega a la página) el lazo se
 * balancea como un péndulo, con el pivote en lo alto de la cuerda, y se va amortiguando hasta
 * quedar quieto. El área táctil mide 48 dp de alto (skill, §4) aunque lo visible sea menor.
 */
@Composable
fun HangingTab(
    onClick: () -> Unit,
    description: String,
    modifier: Modifier = Modifier,
    swing: Boolean = true,
    content: @Composable () -> Unit,
) {
    val angle = remember { Animatable(0f) }
    LaunchedEffect(swing) {
        if (swing) {
            // Sale despedido hacia un lado y vuelve oscilando: muelle de poco amortiguamiento.
            angle.snapTo(SwingStartDegrees)
            angle.animateTo(
                0f,
                spring(dampingRatio = 0.16f, stiffness = 45f, visibilityThreshold = 0.05f),
            )
        } else {
            angle.snapTo(0f)
        }
    }

    Box(
        modifier = modifier
            .width(80.dp)
            .height(HangingTabTouchHeight)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.graphicsLayer {
                rotationZ = angle.value
                transformOrigin = TransformOrigin(0.5f, 0f)
            },
        ) {
            Box(
                Modifier
                    .size(width = 2.dp, height = 6.dp)
                    .background(EdgeStyle.Quiet.content.copy(alpha = 0.5f)),
            )
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(width = 52.dp, height = 26.dp)
                    .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 16.dp, bottomEnd = 16.dp))
                    .background(EdgeStyle.Quiet.container),
            ) {
                content()
            }
        }
    }
}

private const val SwingStartDegrees = 16f

/**
 * Gemela de [HangingTab] para el borde inferior, sin cuerda ni movimiento: la misma pestaña pequeña
 * (64 × 34 dp) con la forma invertida —redondeada arriba, casi plana abajo— y con el contenido que
 * se le pase (en «Toca para hablar», la flecha y el icono de historial). Área táctil de 48 dp.
 */
@Composable
fun BottomTab(
    onClick: () -> Unit,
    description: String,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .width(80.dp)
            .height(HangingTabTouchHeight)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.BottomCenter,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterHorizontally),
            modifier = Modifier
                .padding(bottom = 6.dp)
                .size(width = 56.dp, height = 28.dp)
                .clip(RoundedCornerShape(percent = 50))
                .background(EdgeStyle.Quiet.container),
        ) {
            content()
        }
    }
}

private val HangingTabTouchHeight = 48.dp

@Composable
fun EdgeIcon(icon: ImageVector, style: EdgeStyle = EdgeStyle.Quiet, size: Dp = 24.dp) {
    Icon(icon, contentDescription = null, tint = style.content, modifier = Modifier.size(size))
}

@Composable
fun EdgeLabel(text: String, style: EdgeStyle = EdgeStyle.Tonal) {
    Text(text = text, color = style.content, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
}

/** Círculo tonal con un icono dentro: el bloque de los estados de error, vacío y cargando. */
@Composable
fun StateBadge(
    diameter: Dp,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(diameter)
            .clip(CircleShape)
            .background(VoltPalette.SurfaceContainer),
    ) {
        content()
    }
}

/**
 * Ondas que salen del botón principal en reposo: dos anillos verdes que se expanden y se desvanecen,
 * desfasados, invitando a tocar. Se dibujan en el `Canvas`, sin recomponer. Con «reducir movimiento»
 * queda un único aro estático.
 */
@Composable
fun IdleHalo(modifier: Modifier = Modifier, color: Color = VoltPalette.Primary) {
    val still = reduceMotion()
    val transition = rememberInfiniteTransition(label = "halo")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2800, easing = LinearOutSlowInEasing)),
        label = "haloT",
    )
    Canvas(modifier) {
        val base = size.minDimension / 2f
        val stroke = 2.dp.toPx()
        if (still) {
            drawCircle(color.copy(alpha = 0.22f), base * 0.88f, style = Stroke(stroke))
            return@Canvas
        }
        for (phase in listOf(0f, 0.5f)) {
            val p = (t + phase) % 1f
            drawCircle(
                color = color.copy(alpha = 0.38f * (1f - p)),
                radius = base * (0.78f + 0.22f * p),
                style = Stroke(stroke * (1.5f - p)),
            )
        }
    }
}

/** Aparece escalando desde [from] con un muelle suave y fundiéndose; [delayMs] escalona varios bloques. */
@Composable
fun Modifier.popIn(delayMs: Int = 0, from: Float = 0.82f): Modifier {
    if (reduceMotion()) return this
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(delayMs.toLong())
        progress.animateTo(1f, spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessMediumLow))
    }
    return graphicsLayer {
        val p = progress.value
        val s = from + (1f - from) * p
        scaleX = s
        scaleY = s
        alpha = p.coerceIn(0f, 1f)
    }
}

/**
 * Interruptor de píldora: la perilla se desliza con muelle y la pista pasa de apagado a verde. La
 * posición va además en texto («Activada»/«Apagada») para que el color no sea el único indicador.
 */
@Composable
fun VoltSwitch(checked: Boolean, modifier: Modifier = Modifier) {
    val progress by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMedium),
        label = "switch",
    )
    val track = androidx.compose.ui.graphics.lerp(VoltPalette.SurfaceContainerHigh, VoltPalette.Primary, progress)
    val knob = androidx.compose.ui.graphics.lerp(VoltPalette.OnSurfaceVariant, VoltPalette.OnPrimary, progress)
    Box(
        modifier = modifier
            .size(width = 44.dp, height = 26.dp)
            .clip(CircleShape)
            .background(track)
            .padding(3.dp),
    ) {
        Box(
            Modifier
                .offset(x = 18.dp * progress)
                .size(20.dp)
                .clip(CircleShape)
                .background(knob),
        )
    }
}
