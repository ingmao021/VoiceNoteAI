package com.voicenoteai.wear.data.remote

import com.voicenoteai.wear.data.remote.dto.CreateNoteRequest
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * Cliente HTTP del backend sobre Ktor: solo transporte, sin interpretar respuestas.
 *
 * El token es el fijo de la etapa 2 (PRD §12); en la etapa 4 sale del Keystore (RNF-14).
 */
class NoteApi(
    private val baseUrl: String,
    private val token: String,
    engine: HttpClientEngine = Android.create(),
) {
    private val client = HttpClient(engine) {
        expectSuccess = false
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        install(HttpTimeout) { requestTimeoutMillis = TIMEOUT_MS }
    }

    suspend fun createNote(request: CreateNoteRequest): HttpResponse =
        client.post("$baseUrl/v1/notes") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(request)
        }

    suspend fun health(): HttpResponse = client.get("$baseUrl/v1/health")

    companion object {
        /** RNF-05: un único timeout que cubre el arranque en frío de Render (PRD §3.5). */
        const val TIMEOUT_MS = 90_000L
    }
}
