package com.voicenoteai.wear.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.MaterialTheme
import com.voicenoteai.wear.R

/**
 * Paleta de felpa de docs/propuesta_diseño.png (skill `wear-ui-ux`, §3): fondo crema, un parche de color
 * por estado y bordado crema encima. Los colores de estado deben coincidir con `STATE_COLORS` de
 * `.claude/skills/wear-ui-ux/scripts/build_assets.py`, que tiñe las imágenes de los parches.
 */
object FurPalette {
    val Base = Color(0xFFD8CCB8)
    val Ink = Color(0xFF1B1A17)
    val InkSoft = Color(0xFF4A453C)
    val Thread = Color(0xFFF1E8D8)

    val Idle = Color(0xFF2158A2)
    val Recording = Color(0xFFD63A3A)
    val Processing = Color(0xFFE09A14)
    val Success = Color(0xFF2E8B57)
    val Error = Color(0xFFC2451C)
}

/** Lilita One: la tipografía pesada y redondeada del diseño. */
val DisplayFont = FontFamily(Font(R.font.lilita_one))

private val colorScheme = ColorScheme(
    primary = FurPalette.Idle,
    onPrimary = FurPalette.Thread,
    error = FurPalette.Error,
    onError = FurPalette.Thread,
    background = FurPalette.Base,
    onBackground = FurPalette.Ink,
    onSurface = FurPalette.Ink,
    onSurfaceVariant = FurPalette.InkSoft,
)

@Composable
fun VoiceNoteAITheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colorScheme, content = content)
}
