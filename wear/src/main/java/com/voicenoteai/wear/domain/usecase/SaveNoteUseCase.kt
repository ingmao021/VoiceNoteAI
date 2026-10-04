package com.voicenoteai.wear.domain.usecase

import com.voicenoteai.wear.domain.model.SaveNoteResult
import com.voicenoteai.wear.domain.repository.NoteRepository

/** Envía el dictado al backend. Nunca lanza (ver [NoteRepository.save]). */
class SaveNoteUseCase(private val repository: NoteRepository) {
    suspend operator fun invoke(text: String): SaveNoteResult = repository.save(text)
}
