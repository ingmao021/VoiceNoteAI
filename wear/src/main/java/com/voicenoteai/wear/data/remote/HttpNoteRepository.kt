package com.voicenoteai.wear.data.remote

import android.util.Log
import com.voicenoteai.wear.data.remote.dto.CreateNoteRequest
import com.voicenoteai.wear.data.remote.dto.CreatedNoteResponse
import com.voicenoteai.wear.domain.model.SaveNoteResult
import com.voicenoteai.wear.domain.model.SubmissionError
import com.voicenoteai.wear.domain.repository.NoteRepository
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.IOException

/** [NoteRepository] sobre el backend: traduce códigos HTTP y fallos de red a [SaveNoteResult]. */
class HttpNoteRepository(
    private val api: NoteApi,
    private val scope: CoroutineScope,
) : NoteRepository {

    override suspend fun save(text: String): SaveNoteResult = try {
        val response = api.createNote(CreateNoteRequest(text))
        when (response.status) {
            HttpStatusCode.Created -> response.body<CreatedNoteResponse>().let {
                SaveNoteResult.Saved(it.title, it.structured)
            }
            // 401: token de la app inválido. 424: acceso a Google revocado (FA-13).
            HttpStatusCode.Unauthorized,
            HttpStatusCode.FailedDependency -> SaveNoteResult.Failed(SubmissionError.RELINK_ACCOUNT)
            else -> {
                Log.w(TAG, "Unexpected backend response: ${response.status}")
                SaveNoteResult.Failed(SubmissionError.NOT_SAVED)
            }
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: HttpRequestTimeoutException) {
        SaveNoteResult.Failed(SubmissionError.TIMED_OUT)
    } catch (e: IOException) {
        Log.w(TAG, "Backend unreachable: ${e.javaClass.simpleName}")
        SaveNoteResult.Failed(SubmissionError.NO_CONNECTION)
    } catch (e: Exception) {
        Log.e(TAG, "Unexpected failure while sending the note", e)
        SaveNoteResult.Failed(SubmissionError.NOT_SAVED)
    }

    override fun warmUp() {
        scope.launch {
            runCatching { api.health() }
                .onFailure { if (it is CancellationException) throw it }
        }
    }

    companion object {
        const val TAG = "VoiceNoteAI.Submission"
    }
}
