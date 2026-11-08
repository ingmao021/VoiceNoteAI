package com.voicenoteai.wear.data.remote

import com.voicenoteai.wear.domain.model.HistoryError
import com.voicenoteai.wear.domain.model.HistoryTask
import com.voicenoteai.wear.domain.model.LoadTasksResult
import com.voicenoteai.wear.domain.model.SetTaskCompletedResult
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.ConnectException
import java.time.LocalDate

class HttpTaskHistoryRepositoryTest {
    private val requests = mutableListOf<HttpRequestData>()

    private fun repository(
        handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
    ) = HttpTaskHistoryRepository(
        NoteApi(
            baseUrl = "http://backend",
            token = "secret",
            engine = MockEngine { requests += it; handler(it) },
        ),
    )

    private fun MockRequestHandleScope.json(status: HttpStatusCode, body: String) =
        respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))

    @Test
    fun `recent asks with the limit and maps the backend response`() = runTest {
        val r = repository {
            json(
                HttpStatusCode.OK,
                """
                {
                  "today": "2026-11-07",
                  "tasks": [
                    {"id":"1","title":"Comprar café","dueDate":"2026-11-08","completed":false},
                    {"id":"2","title":"Sin fecha","dueDate":null,"completed":true}
                  ]
                }
                """.trimIndent(),
            )
        }

        val result = r.recent(limit = 20)

        assertEquals(
            LoadTasksResult.Loaded(
                tasks = listOf(
                    HistoryTask("1", "Comprar café", LocalDate.of(2026, 11, 8), completed = false),
                    HistoryTask("2", "Sin fecha", null, completed = true),
                ),
                today = LocalDate.of(2026, 11, 7),
            ),
            result,
        )
        val request = requests.single()
        assertEquals("http://backend/v1/tasks?limit=20", request.url.toString())
        assertEquals("Bearer secret", request.headers[HttpHeaders.Authorization])
    }

    @Test
    fun `a malformed date or an empty task is ignored`() = runTest {
        val r = repository {
            json(
                HttpStatusCode.OK,
                """
                {
                  "today": "2026-11-07",
                  "tasks": [
                    {"id":"1","title":"Válida","dueDate":"no-es-fecha","completed":false},
                    {"id":"","title":"Sin id","dueDate":null,"completed":false},
                    {"id":"3","title":"","dueDate":null,"completed":false}
                  ]
                }
                """.trimIndent(),
            )
        }

        val result = r.recent(limit = 20) as LoadTasksResult.Loaded

        assertEquals(listOf(HistoryTask("1", "Válida", null, completed = false)), result.tasks)
    }

    @Test
    fun `401 and 424 ask to relink`() = runTest {
        val unauthorized = repository { json(HttpStatusCode.Unauthorized, "{}") }
        assertEquals(
            LoadTasksResult.Failed(HistoryError.RELINK_ACCOUNT),
            unauthorized.recent(limit = 20),
        )

        val revoked = repository { json(HttpStatusCode.FailedDependency, "{}") }
        assertEquals(
            LoadTasksResult.Failed(HistoryError.RELINK_ACCOUNT),
            revoked.recent(limit = 20),
        )
    }

    @Test
    fun `no network is no connection and a timeout is timed out`() = runTest {
        val offline = repository { throw ConnectException("Connection refused") }
        assertEquals(
            LoadTasksResult.Failed(HistoryError.NO_CONNECTION),
            offline.recent(limit = 20),
        )

        val slow = repository { throw HttpRequestTimeoutException(it.url.toString(), NoteApi.HISTORY_TIMEOUT_MS) }
        assertEquals(
            LoadTasksResult.Failed(HistoryError.TIMED_OUT),
            slow.recent(limit = 20),
        )
    }

    @Test
    fun `updating a task PATCHes the new state`() = runTest {
        val r = repository { json(HttpStatusCode.OK, """{"id":"task-1","completed":true}""") }

        assertEquals(SetTaskCompletedResult.Updated, r.setCompleted("task-1", true))

        val request = requests.single()
        assertEquals(HttpMethod.Patch, request.method)
        assertEquals("http://backend/v1/tasks/task-1", request.url.toString())
        assertEquals("Bearer secret", request.headers[HttpHeaders.Authorization])
        assertEquals("""{"completed":true}""", (request.body as TextContent).text)
    }

    @Test
    fun `a task deleted elsewhere is not found`() = runTest {
        val r = repository { json(HttpStatusCode.NotFound, """{"error":"task_not_found"}""") }

        assertEquals(
            SetTaskCompletedResult.Failed(HistoryError.TASK_NOT_FOUND),
            r.setCompleted("gone", true),
        )
    }

    @Test
    fun `an update failure keeps the task and reports the error`() = runTest {
        val offline = repository { throw ConnectException("Connection refused") }
        assertEquals(
            SetTaskCompletedResult.Failed(HistoryError.NO_CONNECTION),
            offline.setCompleted("task-1", false),
        )

        val slow = repository { throw HttpRequestTimeoutException(it.url.toString(), NoteApi.HISTORY_TIMEOUT_MS) }
        assertEquals(
            SetTaskCompletedResult.Failed(HistoryError.TIMED_OUT),
            slow.setCompleted("task-1", false),
        )

        val server = repository { json(HttpStatusCode.BadGateway, """{"error":"tasks_provider_error"}""") }
        assertEquals(
            SetTaskCompletedResult.Failed(HistoryError.NOT_LOADED),
            server.setCompleted("task-1", false),
        )
    }

    @Test
    fun `the backend day is optional but tasks still load`() = runTest {
        val r = repository {
            json(
                HttpStatusCode.OK,
                """{"tasks":[{"id":"1","title":"Comprar café","dueDate":null,"completed":false}]}""",
            )
        }

        val result = r.recent(limit = 5)

        assertTrue(result is LoadTasksResult.Loaded)
        assertEquals(1, (result as LoadTasksResult.Loaded).tasks.size)
    }
}
