package com.voicenoteai.wear.presentation.note

import androidx.annotation.StringRes
import com.voicenoteai.wear.R

/** Los cinco estados de la única pantalla (PRD §7.4, §7.5). */
sealed interface NoteUiState {
    /** [starting]: se tocó el botón y se espera `onReadyForSpeech`; ignora toques repetidos. */
    data class Idle(val starting: Boolean = false) : NoteUiState

    /** [level] es el `rmsdB` ya normalizado a 0..1 (R7). */
    data class Recording(val level: Float = 0f, val speaking: Boolean = false) : NoteUiState

    data class Processing(val stage: ProcessingStage = ProcessingStage.PROCESSING) : NoteUiState

    data class Success(val title: String) : NoteUiState

    data class Error(@param:StringRes val messageRes: Int, val action: ErrorAction) : NoteUiState
}

/** Texto escalonado por tiempo transcurrido en PROCESANDO (PRD §3.5). */
enum class ProcessingStage(@param:StringRes val textRes: Int, val fromMs: Long) {
    PROCESSING(R.string.processing_stage_processing, 0),
    ALMOST_DONE(R.string.processing_stage_almost_done, 5_000),
    WAKING_SERVER(R.string.processing_stage_waking_server, 10_000),
}

/** Qué hace el botón de ERROR. */
enum class ErrorAction(@param:StringRes val labelRes: Int) {
    /** Reenvía el texto conservado: el usuario no vuelve a hablar (regla transversal de §5). */
    RESEND(R.string.action_retry),

    /** Error de captura: no hay texto que conservar, así que se vuelve a escuchar. */
    SPEAK_AGAIN(R.string.action_retry),

    /** FA-02: permiso denegado permanentemente. */
    OPEN_SETTINGS(R.string.action_settings),

    /** Errores sin reintento: vuelve a INICIAL. */
    GO_BACK(R.string.action_back),
}

/** Patrones hápticos distintos para usar el reloj sin mirarlo (PRD §7.6). */
enum class Haptic { START, SUCCESS, ERROR }
