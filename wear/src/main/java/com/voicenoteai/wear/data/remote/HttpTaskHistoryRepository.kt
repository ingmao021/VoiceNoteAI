package com.voicenoteai.wear.data.remote

import android.util.Log
import com.voicenoteai.wear.data.remote.dto.TaskListResponse
import com.voicenoteai.wear.domain.model.HistoryError
import com.voicenoteai.wear.domain.model.HistoryTask
import com.voicenoteai.wear.domain.model.LoadTasksResult
import com.voicenoteai.wear.domain.model.SetTaskCompletedResult
import com.voicenoteai.wear.domain.repository.TaskHistoryRepository
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CancellationException
import java.io.IOException
import java.time.LocalDate

/** [TaskHistoryRepository] sobre el backend: traduce códigos HTTP y fallos de red. */
class HttpTaskHistoryRepository(private val api: NoteApi) : TaskHistoryRepository {

    override suspend fun recent(limit: Int): LoadTasksResult = try {
        val response = api.tasks(limit)
        when (response.status) {
            HttpStatusCode.OK -> {
                val body = response.body<TaskListResponse>()
                val today = body.today?.let(::parseDate) ?: LocalDate.now()
                LoadTasksResult.Loaded(
                    tasks = body.tasks.mapNotNull { dto ->
                        val id = dto.id.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                        val title = dto.title.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                        HistoryTask(id, title, parseDateOrNull(dto.dueDate), dto.completed)
                    },
                    today = today,
                )
            }
            // 401: token de la app inválido. 424: acceso a Google revocado (FA-13).
            HttpStatusCode.Unauthorized,
            HttpStatusCode.FailedDependency -> LoadTasksResult.Failed(HistoryError.RELINK_ACCOUNT)
            else -> {
                Log.w(TAG, "Unexpected task list response: ${response.status}")
                LoadTasksResult.Failed(HistoryError.NOT_LOADED)
            }
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: HttpRequestTimeoutException) {
        LoadTasksResult.Failed(HistoryError.TIMED_OUT)
    } catch (e: IOException) {
        Log.w(TAG, "Backend unreachable: ${e.javaClass.simpleName}")
        LoadTasksResult.Failed(HistoryError.NO_CONNECTION)
    } catch (e: Exception) {
        Log.e(TAG, "Unexpected failure while loading the task history", e)
        LoadTasksResult.Failed(HistoryError.NOT_LOADED)
    }

    override suspend fun setCompleted(id: String, completed: Boolean): SetTaskCompletedResult = try {
        val response = api.updateTask(id, completed)
        when (response.status) {
            HttpStatusCode.OK -> SetTaskCompletedResult.Updated
            HttpStatusCode.Unauthorized,
            HttpStatusCode.FailedDependency -> SetTaskCompletedResult.Failed(HistoryError.RELINK_ACCOUNT)
            HttpStatusCode.NotFound -> SetTaskCompletedResult.Failed(HistoryError.TASK_NOT_FOUND)
            else -> {
                Log.w(TAG, "Unexpected task update response: ${response.status}")
                SetTaskCompletedResult.Failed(HistoryError.NOT_LOADED)
            }
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: HttpRequestTimeoutException) {
        SetTaskCompletedResult.Failed(HistoryError.TIMED_OUT)
    } catch (e: IOException) {
        Log.w(TAG, "Backend unreachable: ${e.javaClass.simpleName}")
        SetTaskCompletedResult.Failed(HistoryError.NO_CONNECTION)
    } catch (e: Exception) {
        Log.e(TAG, "Unexpected failure while updating the task", e)
        SetTaskCompletedResult.Failed(HistoryError.NOT_LOADED)
    }

    private fun parseDate(value: String): LocalDate? = parseDateOrNull(value)

    private fun parseDateOrNull(value: String?): LocalDate? =
        value?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

    companion object {
        const val TAG = "VoiceNoteAI.History"
    }
}
