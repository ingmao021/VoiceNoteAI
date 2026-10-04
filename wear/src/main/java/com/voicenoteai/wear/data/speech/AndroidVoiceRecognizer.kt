package com.voicenoteai.wear.data.speech

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.speech.RecognitionListener
import android.speech.RecognitionService
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import com.voicenoteai.wear.domain.model.CaptureError
import com.voicenoteai.wear.domain.model.SpeechEvent
import com.voicenoteai.wear.domain.speech.VoiceRecognizer
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * Adaptador de [VoiceRecognizer] sobre `SpeechRecognizer` (PRD §3.3).
 *
 * Todas las llamadas al reconocedor se hacen en el hilo principal (RNF-36): si se invoca desde
 * otro hilo, la llamada se publica en el looper principal. El reconocedor se crea en el primer
 * [start] y se libera con [release], que debe llamarse desde `onDestroy`.
 */
class AndroidVoiceRecognizer(context: Context) : VoiceRecognizer {
    private val context = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private val channel = Channel<SpeechEvent>(Channel.UNLIMITED)
    private val meter = SessionMeter { SystemClock.elapsedRealtime() }
    private var recognizer: SpeechRecognizer? = null

    override val events: Flow<SpeechEvent> = channel.receiveAsFlow()

    override val isAvailable: Boolean
        get() = SpeechRecognizer.isRecognitionAvailable(context)

    override fun start() = onMainThread {
        val r = recognizer ?: create()?.also { recognizer = it }
        if (r == null) {
            logDiagnostics()
            channel.trySend(SpeechEvent.Error(CaptureError.RECOGNIZER_UNAVAILABLE))
            return@onMainThread
        }
        meter.start()
        r.startListening(recognitionIntent())
    }

    override fun stop() = onMainThread { recognizer?.stopListening() }

    override fun cancel() = onMainThread {
        recognizer?.cancel()
        meter.cancel()?.let { Log.i(TAG_MEASUREMENT, it) }
    }

    override fun release() = onMainThread {
        recognizer?.destroy()
        recognizer = null
    }

    /**
     * Solo el reconocedor estándar, que ya usa modelos locales cuando el dispositivo los tiene.
     * `createOnDeviceSpeechRecognizer` queda fuera a propósito: sus modelos pueden no incluir
     * es-CO (PRD §3.3).
     */
    private fun create(): SpeechRecognizer? {
        if (!isAvailable) return null
        Log.i(TAG_CONFIG, "Standard recognizer (createSpeechRecognizer)")
        return SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(listener)
        }
    }

    /** Deja en el log lo que falta en el dispositivo, para no tener que investigarlo con adb. */
    private fun logDiagnostics() {
        val pm = context.packageManager
        val services = pm.queryIntentServices(Intent(RecognitionService.SERVICE_INTERFACE), 0)
            .map { it.serviceInfo.packageName }
        val activities = pm.queryIntentActivities(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH), 0)
            .map { it.activityInfo.packageName }
        Log.e(TAG_CONFIG, "No speech recognition service on this device. " +
            "API ${Build.VERSION.SDK_INT}, ${services.size} recognition services $services, " +
            "dictation activities $activities. " +
            "On the emulator, use a recent Wear OS image with Google services.")
    }

    private fun recognitionIntent() = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, LANGUAGE)
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
    }

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) = emit(SpeechEvent.Ready)
        override fun onBeginningOfSpeech() = emit(SpeechEvent.SpeechStarted)
        override fun onRmsChanged(rmsdB: Float) = emit(SpeechEvent.Level(rmsdB))
        override fun onBufferReceived(buffer: ByteArray?) = Unit
        override fun onEndOfSpeech() = emit(SpeechEvent.SpeechEnded)
        override fun onError(error: Int) = emit(SpeechEvent.Error(SpeechErrorMapper.fromCode(error), error))

        override fun onResults(results: Bundle?) {
            val text = results
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
                .orEmpty()
            emit(SpeechEvent.Result(text))
        }

        override fun onPartialResults(partialResults: Bundle?) = meter.partial()
        override fun onEvent(eventType: Int, params: Bundle?) = Unit
    }

    private fun emit(event: SpeechEvent) {
        meter.record(event)?.let { Log.i(TAG_MEASUREMENT, it) }
        channel.trySend(event)
    }

    private fun onMainThread(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) block() else mainHandler.post(block)
    }

    companion object {
        const val LANGUAGE = "es-CO"
        const val TAG_MEASUREMENT = "VoiceNoteAI.Measurement"
        const val TAG_CONFIG = "VoiceNoteAI.Config"
    }
}
