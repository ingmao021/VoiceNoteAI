package com.voicenoteai.wear.presentation.note

import android.view.HapticFeedbackConstants
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Icon
import com.voicenoteai.wear.presentation.theme.FurPalette

/**
 * Parche de felpa central (skill `wear-ui-ux`, §1 y §5): la imagen del estado con su icono bordado encima.
 * Con [onClick] es el único objetivo táctil del estado: escala a 0,92 al presionar y vibra.
 */
@Composable
fun FurPatch(
    @DrawableRes image: Int,
    diameter: Dp,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    description: String? = null,
    enabled: Boolean = true,
    content: @Composable () -> Unit = {},
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.92f else 1f, label = "scale")
    val view = LocalView.current
    val touch = if (onClick == null) {
        Modifier
    } else {
        Modifier
            .clip(CircleShape)
            .clickable(interactionSource = interaction, indication = null, enabled = enabled) {
                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                onClick()
            }
            .semantics {
                role = Role.Button
                description?.let { contentDescription = it }
            }
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(diameter)
            .scale(scale)
            .then(touch),
    ) {
        Image(painterResource(image), contentDescription = null, modifier = Modifier.fillMaxSize())
        content()
    }
}

/** Icono "bordado" en hilo crema, con una sombra leve que lo levanta de la felpa. */
@Composable
fun ThreadIcon(icon: ImageVector, size: Dp) {
    Box {
        Icon(
            icon,
            contentDescription = null,
            tint = Color.Black.copy(alpha = 0.35f),
            modifier = Modifier.size(size).offset(x = 1.dp, y = 2.dp),
        )
        Icon(icon, contentDescription = null, tint = FurPalette.Thread, modifier = Modifier.size(size))
    }
}
