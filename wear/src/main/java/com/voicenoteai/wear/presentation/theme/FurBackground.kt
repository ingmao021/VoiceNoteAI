package com.voicenoteai.wear.presentation.theme

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.voicenoteai.wear.R

/**
 * Fondo de felpa compartido por las dos páginas. Vive fuera del paginador para que la textura no
 * se mueva ni se duplique al deslizar.
 */
@Composable
fun FurBackground(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            // En pantallas cuadradas las esquinas quedan fuera del fondo redondo.
            .background(FurPalette.Base),
    ) {
        Image(
            painterResource(R.drawable.fur_background),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
