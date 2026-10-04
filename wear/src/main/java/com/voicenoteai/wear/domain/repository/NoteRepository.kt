package com.voicenoteai.wear.domain.repository

import com.voicenoteai.wear.domain.model.SaveNoteResult

/** Contrato con el backend (`POST /v1/notes`, `GET /v1/health`). Abstracto para usar falsos en pruebas. */
interface NoteRepository {
    /** Nunca lanza: todo fallo llega como [SaveNoteResult.Failed]. */
    suspend fun save(text: String): SaveNoteResult

    /** Ping de calentamiento al entrar en INICIAL, sin esperar respuesta (PRD §3.5). */
    fun warmUp()
}
