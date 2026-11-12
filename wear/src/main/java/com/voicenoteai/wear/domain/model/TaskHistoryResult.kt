package com.voicenoteai.wear.domain.model

import java.time.LocalDate

/** Resultado de pedir el historial: el backend ya ocultó las tareas vencidas. */
sealed interface LoadTasksResult {
    data class Loaded(val tasks: List<HistoryTask>, val today: LocalDate) : LoadTasksResult

    data class Failed(val error: HistoryError) : LoadTasksResult
}

/** Resultado de marcar o desmarcar una tarea. */
sealed interface SetTaskCompletedResult {
    data object Updated : SetTaskCompletedResult

    data class Failed(val error: HistoryError) : SetTaskCompletedResult
}

/** Resultado de eliminar una tarea. */
sealed interface DeleteTaskResult {
    data object Deleted : DeleteTaskResult

    data class Failed(val error: HistoryError) : DeleteTaskResult
}

/**
 * Errores del historial, con el mismo criterio que [SubmissionError] para los reintentos.
 * [retryable] indica si tiene sentido ofrecer «Toca para reintentar».
 */
enum class HistoryError(val retryable: Boolean = true) {
    /** Sin red o backend inalcanzable. */
    NO_CONNECTION,

    /** El backend no respondió dentro del timeout. */
    TIMED_OUT,

    /** Token de la app inválido o acceso a Google revocado: hay que volver a vincular la cuenta. */
    RELINK_ACCOUNT(retryable = false),

    /** La tarea se borró desde otro cliente. */
    TASK_NOT_FOUND(retryable = false),

    /** Fallo del backend o de Google Tasks. */
    NOT_LOADED,

    /** La app no tiene token configurado: el historial no está disponible. */
    NOT_CONFIGURED(retryable = false),
}
