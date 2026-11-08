package com.voicenoteai.wear.domain.repository

import com.voicenoteai.wear.domain.model.LoadTasksResult
import com.voicenoteai.wear.domain.model.SetTaskCompletedResult

/**
 * Contrato con el backend para el historial (`GET /v1/tasks`, `PATCH /v1/tasks/{id}`).
 * Nunca lanza: todo fallo llega como un resultado fallido.
 */
interface TaskHistoryRepository {
    suspend fun recent(limit: Int): LoadTasksResult

    suspend fun setCompleted(id: String, completed: Boolean): SetTaskCompletedResult
}
