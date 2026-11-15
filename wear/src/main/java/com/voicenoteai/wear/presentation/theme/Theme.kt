package com.voicenoteai.wear.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.wear.compose.foundation.LocalReduceMotion
import androidx.wear.compose.material3.LocalTextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Typography
import com.voicenoteai.wear.R

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

/** Fuente de la app: Poppins (OFL), incluida en `res/font` para que funcione sin conexión. */
val AppFontFamily = FontFamily(
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_medium, FontWeight.Medium),
    Font(R.font.poppins_semibold, FontWeight.SemiBold),
    Font(R.font.poppins_bold, FontWeight.Bold),
)

/**
 * Los estilos de M3 traen su propia familia, así que `defaultFontFamily` no llega a aplicarse: se
 * reescribe cada estilo con Poppins. Y como los `Text` de la app fijan solo tamaño y peso, el estilo
 * local también lleva la familia para que ninguno caiga a la fuente del sistema.
 */
private fun appTypography(): Typography {
    val t = Typography()
    fun TextStyle.poppins() = copy(fontFamily = AppFontFamily)
    return t.copy(
        displayLarge = t.displayLarge.poppins(), displayMedium = t.displayMedium.poppins(),
        displaySmall = t.displaySmall.poppins(), titleLarge = t.titleLarge.poppins(),
        titleMedium = t.titleMedium.poppins(), titleSmall = t.titleSmall.poppins(),
        labelLarge = t.labelLarge.poppins(), labelMedium = t.labelMedium.poppins(),
        labelSmall = t.labelSmall.poppins(), bodyLarge = t.bodyLarge.poppins(),
        bodyMedium = t.bodyMedium.poppins(), bodySmall = t.bodySmall.poppins(),
        bodyExtraSmall = t.bodyExtraSmall.poppins(), numeralLarge = t.numeralLarge.poppins(),
        numeralMedium = t.numeralMedium.poppins(), numeralSmall = t.numeralSmall.poppins(),
    )
}

@Composable
fun VoiceNoteAITheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colorScheme, typography = appTypography()) {
        CompositionLocalProvider(
            LocalTextStyle provides LocalTextStyle.current.merge(TextStyle(fontFamily = AppFontFamily)),
            content = content,
        )
    }
}

/** `true` si el usuario pidió reducir el movimiento: las animaciones decorativas se omiten. */
@Composable
fun reduceMotion(): Boolean = LocalReduceMotion.current
