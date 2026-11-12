package com.voicenoteai.wear.presentation.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * Los iconos vectoriales de la app, con los trazados de Material Icons (un único set). Se definen a
 * mano para no arrastrar `material-icons-extended` (varios MB) por unos pocos iconos (RNF-11).
 */
object AppIcons {
    val Mic = icon(
        "Mic",
        "M12,14c1.66,0 3,-1.34 3,-3V5c0,-1.66 -1.34,-3 -3,-3S9,3.34 9,5v6c0,1.66 1.34,3 3,3zM17.91,11c-0.49,0 -0.9,0.36 -0.98,0.85C16.52,14.2 14.47,16 12,16s-4.52,-1.8 -4.93,-4.15c-0.08,-0.49 -0.49,-0.85 -0.98,-0.85 -0.61,0 -1.09,0.54 -1,1.14 0.49,3 2.89,5.35 5.91,5.78V20c0,0.55 0.45,1 1,1s1,-0.45 1,-1v-2.08c3.02,-0.43 5.42,-2.78 5.91,-5.78 0.1,-0.6 -0.39,-1.14 -1,-1.14z",
    )

    val Stop = icon("Stop", "M6,6h12v12H6z")

    val Warning = icon(
        "Warning",
        "M1,21h22L12,2 1,21zM13,18h-2v-2h2v2zM13,14h-2v-4h2v4z",
    )

    val Check = icon("Check", "M9,16.17L4.83,12l-1.42,1.41L9,19 21,7l-1.41,-1.41z")

    val ChevronUp = icon("ChevronUp", "M7.41,15.41L12,10.83l4.59,4.58L18,14l-6,-6 -6,6z")

    val ChevronDown = icon("ChevronDown", "M7.41,8.59L12,13.17l4.59,-4.58L18,10l-6,6 -6,-6 1.41,-1.41z")

    /** Chevron hacia abajo de trazo redondeado: más ligero y suave que el macizo de Material. */
    val ChevronDownRounded: ImageVector = ImageVector.Builder("ChevronDownRounded", 24.dp, 24.dp, 24f, 24f)
        .addPath(
            PathParser().parsePathString("M6.5,9L12,14.5L17.5,9").toNodes(),
            stroke = SolidColor(Color.White),
            strokeLineWidth = 2.8f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        )
        .build()

    val History = icon(
        "History",
        "M13,3c-4.97,0 -9,4.03 -9,9H1l3.89,3.89 0.07,0.14L9,12H6c0,-3.87 3.13,-7 7,-7s7,3.13 7,7 -3.13,7 -7,7c-1.93,0 -3.68,-0.79 -4.94,-2.06l-1.42,1.42C8.27,19.99 10.51,21 13,21c4.97,0 9,-4.03 9,-9s-4.03,-9 -9,-9zM12,8v5l4.28,2.54 0.72,-1.21 -3.5,-2.08V8H12z",
    )

    val Refresh = icon(
        "Refresh",
        "M17.65,6.35C16.2,4.9 14.21,4 12,4c-4.42,0 -7.99,3.58 -7.99,8s3.57,8 7.99,8c3.73,0 6.84,-2.55 7.73,-6h-2.08c-0.82,2.33 -3.04,4 -5.65,4 -3.31,0 -6,-2.69 -6,-6s2.69,-6 6,-6c1.66,0 3.14,0.69 4.22,1.78L13,11h7V4l-2.35,2.35z",
    )

    private fun icon(name: String, pathData: String) =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f)
            .addPath(PathParser().parsePathString(pathData).toNodes(), fill = SolidColor(Color.White))
            .build()
}
