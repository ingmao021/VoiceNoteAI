package com.voicenoteai.wear.domain.model

sealed interface SaveNoteResult {
    /** [structured] es `false` si el LLM falló y se guardó el texto crudo (FA-11): para el usuario sigue siendo un éxito. */
    data class Saved(val title: String, val structured: Boolean) : SaveNoteResult

    data class Failed(val error: SubmissionError) : SaveNoteResult
}
