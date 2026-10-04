package com.voicenoteai.wear.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.MaterialTheme

/** Paleta de la sección 7.3 del PRD: un color por estado sobre negro puro (OLED, bisel). */
object AppColors {
    val Idle = Color(0xFF8AB4F8)
    val Recording = Color(0xFFF28B82)
    val Processing = Color(0xFFFDD663)
    val Success = Color(0xFF81C995)
    val Error = Color(0xFFF6AEA9)
    val Background = Color(0xFF000000)
    val TextPrimary = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xFF9AA0A6)
}

private val colorScheme = ColorScheme(
    primary = AppColors.Idle,
    onPrimary = AppColors.Background,
    error = AppColors.Error,
    onError = AppColors.Background,
    background = AppColors.Background,
    onBackground = AppColors.TextPrimary,
    onSurface = AppColors.TextPrimary,
    onSurfaceVariant = AppColors.TextSecondary,
)

@Composable
fun VoiceNoteAITheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colorScheme, content = content)
}
