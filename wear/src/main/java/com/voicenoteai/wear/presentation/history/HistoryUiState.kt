package com.voicenoteai.wear.presentation.history

import androidx.annotation.StringRes
import com.voicenoteai.wear.R
import com.voicenoteai.wear.domain.model.HistoryError
import com.voicenoteai.wear.domain.model.HistoryTask
import java.time.LocalDate

/** Estados de la segunda página: el historial de tareas recientes. */
sealed interface HistoryUiState {
    data object Loading : HistoryUiState

    data object Empty : HistoryUiState

    /** [retryable] evita ofrecer un reintento que volvería a fallar (p. ej. sin token). */
    data class Error(
        @param:StringRes val messageRes: Int,
        val retryable: Boolean = true,
    ) : HistoryUiState

    /**
     * [today] viene del backend para que HOY/MAÑANA coincidan con el filtro de vencidas.
     * [updatingIds] evita que un doble toque dispare dos peticiones para la misma tarea.
     */
    data class Content(
        val tasks: List<HistoryTask>,
        val today: LocalDate,
        val updatingIds: Set<String> = emptySet(),
        @param:StringRes val messageRes: Int? = null,
    ) : HistoryUiState
}

/** Mensaje de error al cargar el historial. */
@StringRes
fun HistoryError.loadMessageRes(): Int = when (this) {
    HistoryError.NO_CONNECTION -> R.string.error_history_no_connection
    HistoryError.TIMED_OUT -> R.string.error_history_timed_out
    HistoryError.RELINK_ACCOUNT -> R.string.error_submission_relink_account
    HistoryError.TASK_NOT_FOUND -> R.string.history_task_gone
    HistoryError.NOT_LOADED -> R.string.error_history_not_loaded
    HistoryError.NOT_CONFIGURED -> R.string.error_history_not_configured
}

/** Mensaje breve cuando falla el marcado de una tarea. */
@StringRes
fun HistoryError.updateMessageRes(): Int = when (this) {
    HistoryError.TASK_NOT_FOUND -> R.string.history_task_gone
    else -> R.string.history_update_failed
}
