package com.voicenoteai.wear.data.remote

import com.voicenoteai.wear.domain.model.SaveNoteResult
import com.voicenoteai.wear.domain.model.SubmissionError
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Test
import java.net.ConnectException

class HttpNoteRepositoryTest {
    private val requests = mutableListOf<HttpRequestData>()

    private fun TestScope.repository(
        handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
    ) = HttpNoteRepository(
        api = NoteApi(
            baseUrl = "http://backend",
            token = "secret",
            engine = MockEngine { requests += it; handler(it) },
        ),
        scope = backgroundScope,
    )

    private fun MockRequestHandleScope.json(status: HttpStatusCode, body: String) =
        respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))

    @Test
    fun `sends the text with the token and returns the title`() = runTest {
        val r = repository {
            json(HttpStatusCode.Created, """{"id":"t1","title":"Compras","structured":true,"dueDate":null}""")
        }

        assertEquals(SaveNoteResult.Saved("Compras", structured = true), r.save("comprar leche"))

        val request = requests.single()
        assertEquals("http://backend/v1/notes", request.url.toString())
        assertEquals("Bearer secret", request.headers[HttpHeaders.Authorization])
        assertEquals("""{"text":"comprar leche"}""", (request.body as TextContent).text)
    }

    @Test
    fun `a note saved without AI is still a success (FA-11)`() = runTest {
        val r = repository { json(HttpStatusCode.Created, """{"id":"t1","title":"Idea","structured":false}""") }
        assertEquals(SaveNoteResult.Saved("Idea", structured = false), r.save("idea"))
    }

    @Test
    fun `401 asks to relink`() = runTest {
        val r = repository { json(HttpStatusCode.Unauthorized, """{"error":"invalid_token","message":"x"}""") }
        assertEquals(SaveNoteResult.Failed(SubmissionError.RELINK_ACCOUNT), r.save("hola"))
    }

    @Test
    fun `424 revoked Google access asks to relink (FA-13)`() = runTest {
        val r = repository { json(HttpStatusCode.FailedDependency, """{"error":"google_access_revoked","message":"x"}""") }
        assertEquals(SaveNoteResult.Failed(SubmissionError.RELINK_ACCOUNT), r.save("hola"))
    }

    @Test
    fun `502 or an unexpected response is not saved (FA-16)`() = runTest {
        val r = repository { json(HttpStatusCode.BadGateway, """{"error":"tasks_provider_error","message":"x"}""") }
        assertEquals(SaveNoteResult.Failed(SubmissionError.NOT_SAVED), r.save("hola"))
    }

    @Test
    fun `no network is no connection (FA-09)`() = runTest {
        val r = repository { throw ConnectException("Connection refused") }
        assertEquals(SaveNoteResult.Failed(SubmissionError.NO_CONNECTION), r.save("hola"))
    }

    @Test
    fun `timeout is timed out (FA-17)`() = runTest {
        val r = repository { throw HttpRequestTimeoutException(it.url.toString(), NoteApi.TIMEOUT_MS) }
        assertEquals(SaveNoteResult.Failed(SubmissionError.TIMED_OUT), r.save("hola"))
    }

    @Test
    fun `warm up GETs health and does not fail without network`() = runTest {
        val arrival = CompletableDeferred<String>()
        val r = repository {
            arrival.complete(it.url.toString())
            throw ConnectException("Connection refused")
        }
        r.warmUp()
        // MockEngine atiende en su propio dispatcher: se espera en tiempo real, no virtual.
        val url = withContext(Dispatchers.Default) { withTimeout(5_000) { arrival.await() } }
        assertEquals("http://backend/v1/health", url)
    }
}
