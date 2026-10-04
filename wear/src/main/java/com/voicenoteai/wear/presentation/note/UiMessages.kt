package com.voicenoteai.wear.presentation.note

import androidx.annotation.StringRes
import com.voicenoteai.wear.R
import com.voicenoteai.wear.domain.model.CaptureError
import com.voicenoteai.wear.domain.model.SubmissionError

/** Mensaje en pantalla de cada error de captura (PRD §5). Máximo tres palabras por mensaje (§7.4). */
@get:StringRes
val CaptureError.messageRes: Int
    get() = when (this) {
        CaptureError.PERMISSION_DENIED -> R.string.error_capture_permission_denied
        CaptureError.MICROPHONE_BUSY -> R.string.error_capture_microphone_busy
        CaptureError.NOTHING_HEARD -> R.string.error_capture_nothing_heard
        CaptureError.NOT_UNDERSTOOD -> R.string.error_capture_not_understood
        CaptureError.LANGUAGE_UNAVAILABLE -> R.string.error_capture_language_unavailable
        CaptureError.NO_CONNECTION -> R.string.error_capture_no_connection
        CaptureError.RECOGNIZER_UNAVAILABLE -> R.string.error_capture_recognizer_unavailable
        CaptureError.LOW_BATTERY -> R.string.error_capture_low_battery
    }

/** Mensaje en pantalla de cada error de envío (PRD §5). */
@get:StringRes
val SubmissionError.messageRes: Int
    get() = when (this) {
        SubmissionError.NO_CONNECTION -> R.string.error_submission_no_connection
        SubmissionError.RELINK_ACCOUNT -> R.string.error_submission_relink_account
        SubmissionError.NOT_SAVED -> R.string.error_submission_not_saved
        SubmissionError.TIMED_OUT -> R.string.error_submission_timed_out
    }
