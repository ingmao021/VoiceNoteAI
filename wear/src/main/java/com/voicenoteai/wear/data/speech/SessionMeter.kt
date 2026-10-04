package com.voicenoteai.wear.data.speech

import com.voicenoteai.wear.domain.model.SpeechEvent
import java.util.Locale

/**
 * Mide cada sesión de reconocimiento para resolver los pendientes del anexo del PRD:
 * punto 2 / R2 (cuánto tolera el reconocedor en habla continua), punto 6 / R7 (rango útil de
 * `rmsdB`) y RNF-02 (latencia del toque a `onReadyForSpeech`).
 *
 * Nunca registra el texto reconocido, solo su número de palabras.
 *
 * Cada resumen se autovalida: si el `rmsdB` máximo nunca superó [audioThresholdDb], el micrófono no
 * pasó audio (p. ej. el emulador sin entrada del host) y el reconocedor cerró por silencio. Esa
 * sesión se marca `valid=NO reason=no_audio` para no confundir su `speech=` con el límite real de R2.
 */
class SessionMeter(
    private val audioThresholdDb: Float = AUDIO_THRESHOLD_DB,
    private val clock: () -> Long,
) {
    private var tStart = -1L
    private var tReady = -1L
    private var tSpeechStart = -1L
    private var tSpeechEnd = -1L
    private var tLastPartial = -1L
    private var partials = 0
    private var rmsMin = Float.POSITIVE_INFINITY
    private var rmsMax = Float.NEGATIVE_INFINITY
    private var rmsSum = 0.0
    private var rmsSamples = 0

    fun start() {
        tStart = clock()
        tReady = -1L
        tSpeechStart = -1L
        tSpeechEnd = -1L
        tLastPartial = -1L
        partials = 0
        rmsMin = Float.POSITIVE_INFINITY
        rmsMax = Float.NEGATIVE_INFINITY
        rmsSum = 0.0
        rmsSamples = 0
    }

    fun partial() {
        if (tStart < 0) return
        partials++
        tLastPartial = clock()
    }

    /** Devuelve el resumen de la sesión cuando [event] la termina; `null` en otro caso. */
    fun record(event: SpeechEvent): String? {
        if (tStart < 0) return null
        val now = clock()
        when (event) {
            SpeechEvent.Ready -> tReady = now
            SpeechEvent.SpeechStarted -> tSpeechStart = now
            SpeechEvent.SpeechEnded -> tSpeechEnd = now
            is SpeechEvent.Level -> {
                rmsMin = minOf(rmsMin, event.rmsdB)
                rmsMax = maxOf(rmsMax, event.rmsdB)
                rmsSum += event.rmsdB
                rmsSamples++
            }
            is SpeechEvent.Result -> return summary(now, "Result", wordCount(event.text))
            is SpeechEvent.Error -> return summary(now, "Error(${event.error}, code=${event.code})", 0)
        }
        return null
    }

    /** La sesión se abortó con `cancel()`: no habrá más callbacks. */
    fun cancel(): String? {
        if (tStart < 0) return null
        return summary(clock(), "Cancelled", 0)
    }

    private fun summary(now: Long, end: String, words: Int): String {
        val text = String.format(
            Locale.ROOT,
            "end=%s | %s | tap→ready=%s | speech=%s | session=%d ms | lastPartial=%s | " +
                "partials=%d | words=%d | rmsdB min=%s max=%s mean=%s (%d samples)",
            end,
            validity(),
            ms(tStart, tReady),
            ms(tSpeechStart, tSpeechEnd),
            now - tStart,
            if (tLastPartial < 0) "—" else "+${tLastPartial - tStart} ms",
            partials,
            words,
            db(rmsMin.takeIf { rmsSamples > 0 }),
            db(rmsMax.takeIf { rmsSamples > 0 }),
            db(if (rmsSamples > 0) (rmsSum / rmsSamples).toFloat() else null),
            rmsSamples,
        )
        tStart = -1L
        return text
    }

    private fun validity() =
        if (rmsSamples > 0 && rmsMax > audioThresholdDb) "valid=YES" else "valid=NO reason=no_audio"

    private fun ms(from: Long, to: Long) =
        if (from < 0 || to < 0) "—" else "${to - from} ms"

    private fun db(value: Float?) = value?.let { String.format(Locale.ROOT, "%.1f", it) } ?: "—"

    private fun wordCount(text: String) = text.split(Regex("\\s+")).count { it.isNotBlank() }

    companion object {
        /**
         * Provisional: una voz real con el micrófono abierto supera holgadamente este valor, y el
         * silencio digital del emulador se queda por debajo. Ajustar con las primeras mediciones.
         */
        const val AUDIO_THRESHOLD_DB = 3f
    }
}
