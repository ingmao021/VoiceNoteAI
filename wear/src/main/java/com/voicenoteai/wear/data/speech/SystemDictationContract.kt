package com.voicenoteai.wear.data.speech

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.result.contract.ActivityResultContract

/**
 * Respaldo con la pantalla de dictado del sistema (`RecognizerIntent`) para dispositivos sin
 * `RecognitionService`, como los emuladores de Wear OS (PRD §3.3). Sin nivel de voz ni parciales.
 *
 * Devuelve el texto dictado, `""` si el dictado terminó sin texto, o `null` si el usuario lo canceló.
 */
class SystemDictationContract : ActivityResultContract<Unit, String?>() {
    override fun createIntent(context: Context, input: Unit) = dictationIntent()

    override fun parseResult(resultCode: Int, intent: Intent?): String? {
        android.util.Log.i("VoiceNoteAI.DEBUG", "resultCode=$resultCode extras=${intent?.extras?.keySet()} clip=${intent?.clipData} " +
            "remote=${intent?.let { androidx.core.app.RemoteInput.getResultsFromIntent(it)?.keySet() }}")
        if (resultCode != Activity.RESULT_OK) return null
        return intent
            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()
            .orEmpty()
    }

    companion object {
        fun isAvailable(context: Context) =
            context.packageManager.queryIntentActivities(dictationIntent(), 0).isNotEmpty()

        private fun dictationIntent() = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, AndroidVoiceRecognizer.LANGUAGE)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
    }
}
