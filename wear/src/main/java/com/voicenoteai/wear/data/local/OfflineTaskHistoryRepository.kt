package com.voicenoteai.wear.data.local

import com.voicenoteai.wear.domain.model.HistoryError
import com.voicenoteai.wear.domain.model.LoadTasksResult
import com.voicenoteai.wear.domain.model.SetTaskCompletedResult
import com.voicenoteai.wear.domain.repository.TaskHistoryRepository

/**
 * Objeto nulo de [TaskHistoryRepository] para cuando no hay `voicenoteai.devApiToken`: la app no
 * puede leer ni modificar Google Tasks, así que el historial informa que no está disponible.
 */
class OfflineTaskHistoryRepository : TaskHistoryRepository {
    override suspend fun recent(limit: Int) = LoadTasksResult.Failed(HistoryError.NOT_CONFIGURED)

    override suspend fun setCompleted(id: String, completed: Boolean) =
        SetTaskCompletedResult.Failed(HistoryError.NOT_CONFIGURED)
}
