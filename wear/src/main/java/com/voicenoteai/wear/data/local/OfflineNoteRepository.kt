package com.voicenoteai.wear.data.local

import com.voicenoteai.wear.domain.model.SaveNoteResult
import com.voicenoteai.wear.domain.repository.NoteRepository

/**
 * Objeto nulo de [NoteRepository] para cuando no hay `voicenoteai.devApiToken` configurado: no
 * envía nada y devuelve el propio texto como título, de modo que la UI completa sigue siendo usable
 * (como en la etapa 1, PRD §12).
 */
class OfflineNoteRepository : NoteRepository {
    override suspend fun save(text: String) = SaveNoteResult.Saved(text, structured = false)

    override fun warmUp() = Unit
}
