package com.voicenoteai.wear.data.remote.dto

import kotlinx.serialization.Serializable

/** Modelos escritos a mano contra el OpenAPI del backend (`/openapi.json`, PRD §2.3). */
@Serializable
data class CreateNoteRequest(val text: String)

@Serializable
data class CreatedNoteResponse(
    val id: String,
    val title: String,
    val structured: Boolean,
    val dueDate: String? = null,
)
