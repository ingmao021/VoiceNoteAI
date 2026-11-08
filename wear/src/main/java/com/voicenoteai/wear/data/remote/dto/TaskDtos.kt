package com.voicenoteai.wear.data.remote.dto

import kotlinx.serialization.Serializable

/** Respuesta de `GET /v1/tasks` (OpenAPI del backend). */
@Serializable
data class TaskListResponse(
    /** `AAAA-MM-DD`; el reloj lo usa para etiquetar HOY/MAÑANA. */
    val today: String? = null,
    val tasks: List<TaskDto> = emptyList(),
)

@Serializable
data class TaskDto(
    val id: String,
    val title: String,
    val dueDate: String? = null,
    val completed: Boolean = false,
)

/** Cuerpo de `PATCH /v1/tasks/{id}`. */
@Serializable
data class UpdateTaskRequest(val completed: Boolean)
