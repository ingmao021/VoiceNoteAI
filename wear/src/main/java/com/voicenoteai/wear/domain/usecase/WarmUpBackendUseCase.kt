package com.voicenoteai.wear.domain.usecase

import com.voicenoteai.wear.domain.repository.NoteRepository

/** Ping de calentamiento del servidor en cada entrada en INICIAL (§3.5). */
class WarmUpBackendUseCase(private val repository: NoteRepository) {
    operator fun invoke() = repository.warmUp()
}
