package com.voicenoteai.wear.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voicenoteai.wear.domain.model.HistoryError
import com.voicenoteai.wear.domain.model.LoadTasksResult
import com.voicenoteai.wear.domain.model.SetTaskCompletedResult
import com.voicenoteai.wear.domain.repository.TaskHistoryRepository
import com.voicenoteai.wear.presentation.haptic.Haptic
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * Estado del historial. No carga en `init`: la primera carga la dispara la página al hacerse
 * visible, para no pedir tareas si el usuario nunca desliza.
 */
class HistoryViewModel(private val repository: TaskHistoryRepository) : ViewModel() {
    private val _state = MutableStateFlow<HistoryUiState>(HistoryUiState.Loading)
    val state: StateFlow<HistoryUiState> = _state.asStateFlow()

    private val _haptics = Channel<Haptic>(Channel.BUFFERED)
    val haptics: Flow<Haptic> = _haptics.receiveAsFlow()

    private var loadJob: Job? = null
    private val updating = mutableSetOf<String>()

    fun refresh() {
        loadJob?.cancel()
        updating.clear()
        loadJob = viewModelScope.launch {
            // Al volver a la página se conserva la lista anterior mientras llega la nueva; solo se
            // muestra el indicador si todavía no había nada que enseñar.
            if (_state.value !is HistoryUiState.Content) _state.value = HistoryUiState.Loading
            when (val result = repository.recent(PAGE_SIZE)) {
                is LoadTasksResult.Loaded -> {
                    _state.value = if (result.tasks.isEmpty()) {
                        HistoryUiState.Empty
                    } else {
                        HistoryUiState.Content(result.tasks, result.today)
                    }
                }
                is LoadTasksResult.Failed -> {
                    _state.value = HistoryUiState.Error(
                        messageRes = result.error.loadMessageRes(),
                        retryable = result.error.retryable,
                    )
                    _haptics.trySend(Haptic.ERROR)
                }
            }
        }
    }

    /** Cambio optimista: el check responde al instante y se revierte si el backend falla. */
    fun setCompleted(id: String, completed: Boolean) {
        val current = _state.value as? HistoryUiState.Content ?: return
        val task = current.tasks.firstOrNull { it.id == id } ?: return
        if (task.completed == completed) return
        if (!updating.add(id)) return

        val previous = task.completed
        val optimistic = current.tasks.map { if (it.id == id) it.copy(completed = completed) else it }
        _state.value = current.copy(
            tasks = optimistic,
            updatingIds = current.updatingIds + id,
            messageRes = null,
        )

        viewModelScope.launch {
            when (val result = repository.setCompleted(id, completed)) {
                SetTaskCompletedResult.Updated -> {
                    updating.remove(id)
                    val latest = _state.value as? HistoryUiState.Content ?: return@launch
                    _state.value = latest.copy(updatingIds = latest.updatingIds - id)
                    _haptics.trySend(Haptic.TOGGLE)
                }
                is SetTaskCompletedResult.Failed -> {
                    updating.remove(id)
                    val latest = _state.value as? HistoryUiState.Content ?: return@launch
                    val remaining = if (result.error == HistoryError.TASK_NOT_FOUND) {
                        latest.tasks.filterNot { it.id == id }
                    } else {
                        latest.tasks.map { if (it.id == id) it.copy(completed = previous) else it }
                    }
                    _state.value = if (remaining.isEmpty()) {
                        HistoryUiState.Empty
                    } else {
                        latest.copy(
                            tasks = remaining,
                            updatingIds = latest.updatingIds - id,
                            messageRes = result.error.updateMessageRes(),
                        )
                    }
                    _haptics.trySend(Haptic.ERROR)
                }
            }
        }
    }

    companion object {
        /** Tareas que caben en un par de pantallas; suficiente para «recientes». */
        const val PAGE_SIZE = 20
    }
}
