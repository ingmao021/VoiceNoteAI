package com.voicenoteai.wear.domain.audio

/**
 * Convierte `rmsdB` en un nivel 0..1 para el anillo de voz (PRD §7.4, R7).
 *
 * El rango real de `rmsdB` varía por dispositivo, así que no se mapea linealmente: se recorta a un
 * rango calibrado y se suaviza con una media móvil exponencial para que el anillo no salte.
 * Arranca en silencio, de modo que la primera muestra no produce un salto.
 */
class RmsNormalizer(
    private val minDb: Float = MIN_DB,
    private val maxDb: Float = MAX_DB,
    private val alpha: Float = ALPHA,
) {
    private var smoothed = minDb

    fun normalize(rmsdB: Float): Float {
        smoothed += alpha * (rmsdB.coerceIn(minDb, maxDb) - smoothed)
        return (smoothed - minDb) / (maxDb - minDb)
    }

    fun reset() {
        smoothed = minDb
    }

    companion object {
        // Valores iniciales; se ajustan con el rango medido en VoiceNoteAI.Measurement (anexo, punto 6).
        const val MIN_DB = -2f
        const val MAX_DB = 10f
        const val ALPHA = 0.3f
    }
}
