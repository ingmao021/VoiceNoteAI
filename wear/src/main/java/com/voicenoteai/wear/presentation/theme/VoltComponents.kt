package com.voicenoteai.wear.presentation.theme

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
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
            .background(if (enabled) container else VoltPalette.SurfaceContainerHigh)
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
    val shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp, bottomStart = 54.dp, bottomEnd = 54.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
        modifier = modifier
            .padding(bottom = 3.dp)
            .width(width)
            .defaultMinSize(minHeight = 48.dp)
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
                    .size(width = 3.dp, height = 7.dp)
                    .background(EdgeStyle.Tonal.content.copy(alpha = 0.55f)),
            )
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(width = 64.dp, height = 34.dp)
                    .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp, bottomStart = 22.dp, bottomEnd = 22.dp))
                    .background(EdgeStyle.Tonal.container),
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
                .padding(bottom = 3.dp)
                .size(width = 64.dp, height = 34.dp)
                .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp, bottomStart = 10.dp, bottomEnd = 10.dp))
                .background(EdgeStyle.Tonal.container),
        ) {
            content()
        }
    }
}

private val HangingTabTouchHeight = 48.dp

@Composable
fun EdgeIcon(icon: ImageVector, style: EdgeStyle = EdgeStyle.Tonal, size: Dp = 24.dp) {
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
