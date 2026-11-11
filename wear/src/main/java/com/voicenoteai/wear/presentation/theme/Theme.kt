package com.voicenoteai.wear.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.MaterialTheme

/**
 * Paleta «Volt» (skill `smartwatch-app-design`, §2): fondo negro OLED, verde #cbfe00 como único
 * elemento dominante por pantalla y azul #009bfe para lo secundario. Los pares `On*` garantizan
 * contraste: nunca texto blanco sobre el verde ni sobre el azul.
 */
object VoltPalette {
    val Background = Color(0xFF000000)
    val OnBackground = Color(0xFFE4E6D8)

    val Primary = Color(0xFFCBFE00)
    val OnPrimary = Color(0xFF0E1400)
    val PrimaryContainer = Color(0xFF2B3600)
    val OnPrimaryContainer = Color(0xFFE6FF80)

    val Secondary = Color(0xFF009BFE)
    val OnSecondary = Color(0xFF00131F)
    val SecondaryContainer = Color(0xFF00344F)
    val OnSecondaryContainer = Color(0xFF9ED6FF)

    val SurfaceContainer = Color(0xFF1B1D17)
    val SurfaceContainerHigh = Color(0xFF26291F)
    val OnSurface = Color(0xFFE4E6D8)
    val OnSurfaceVariant = Color(0xFFC5C8B8)
    val Outline = Color(0xFF8F9284)

    val Error = Color(0xFFFFB4AB)
    val OnError = Color(0xFF690005)

    /** Texto principal sobre `SecondaryContainer` (más claro que `OnSecondaryContainer`). */
    val OnSecondaryContainerStrong = Color(0xFFE6F4FF)
}

private val colorScheme = ColorScheme(
    primary = VoltPalette.Primary,
    onPrimary = VoltPalette.OnPrimary,
    primaryContainer = VoltPalette.PrimaryContainer,
    onPrimaryContainer = VoltPalette.OnPrimaryContainer,
    secondary = VoltPalette.Secondary,
    onSecondary = VoltPalette.OnSecondary,
    secondaryContainer = VoltPalette.SecondaryContainer,
    onSecondaryContainer = VoltPalette.OnSecondaryContainer,
    surfaceContainer = VoltPalette.SurfaceContainer,
    surfaceContainerHigh = VoltPalette.SurfaceContainerHigh,
    onSurface = VoltPalette.OnSurface,
    onSurfaceVariant = VoltPalette.OnSurfaceVariant,
    outline = VoltPalette.Outline,
    background = VoltPalette.Background,
    onBackground = VoltPalette.OnBackground,
    error = VoltPalette.Error,
    onError = VoltPalette.OnError,
)

@Composable
fun VoiceNoteAITheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colorScheme, content = content)
}
