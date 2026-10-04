package com.voicenoteai.wear.presentation.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * Los tres iconos de la app, con los trazados de Material Symbols. Se definen a mano para no
 * arrastrar `material-icons-extended` (varios MB) por tres iconos (RNF-11).
 */
object AppIcons {
    val Microphone = icon(
        "Microphone",
        "M12,14c1.66,0 2.99,-1.34 2.99,-3L15,5c0,-1.66 -1.34,-3 -3,-3S9,3.34 9,5v6c0,1.66 1.34,3 3,3z" +
            "M17.3,11c0,3 -2.54,5.1 -5.3,5.1S6.7,14 6.7,11L5,11c0,3.41 2.72,6.23 6,6.72L11,21h2v-3.28" +
            "c3.28,-0.48 6,-3.3 6,-6.72h-1.7z",
    )

    val Stop = icon("Stop", "M7,7h10v10H7z")

    val Warning = icon(
        "Warning",
        "M1,21h22L12,2 1,21zM13,18h-2v-2h2v2zM13,14h-2v-4h2v4z",
    )

    private fun icon(name: String, pathData: String) =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f)
            .addPath(PathParser().parsePathString(pathData).toNodes(), fill = SolidColor(Color.White))
            .build()
}
