package com.voicenoteai.wear.presentation.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * Los iconos vectoriales de la app, con los trazados de Material Symbols. Se definen a mano para no
 * arrastrar `material-icons-extended` (varios MB) por dos iconos (RNF-11). El micrófono de INICIAL
 * es la imagen `patch_microphone`.
 */
object AppIcons {
    val Stop = icon("Stop", "M7,7h10v10H7z")

    val Warning = icon(
        "Warning",
        "M1,21h22L12,2 1,21zM13,18h-2v-2h2v2zM13,14h-2v-4h2v4z",
    )

    val Check = icon("Check", "M9,16.17L4.83,12l-1.42,1.41L9,19 21,7l-1.41,-1.41z")

    val ChevronUp = icon("ChevronUp", "M7.41,15.41L12,10.83l4.59,4.58L18,14l-6,-6 -6,6z")

    val ChevronDown = icon("ChevronDown", "M7.41,8.59L12,13.17l4.59,-4.58L18,10l-6,6 -6,-6 1.41,-1.41z")

    private fun icon(name: String, pathData: String) =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f)
            .addPath(PathParser().parsePathString(pathData).toNodes(), fill = SolidColor(Color.White))
            .build()
}
