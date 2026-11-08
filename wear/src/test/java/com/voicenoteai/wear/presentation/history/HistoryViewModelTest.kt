package com.voicenoteai.wear.presentation.history

import com.voicenoteai.wear.R
import com.voicenoteai.wear.domain.model.HistoryError
import com.voicenoteai.wear.domain.model.HistoryTask
import com.voicenoteai.wear.domain.model.LoadTasksResult
import com.voicenoteai.wear.domain.model.SetTaskCompletedResult
import com.voicenoteai.wear.domain.repository.TaskHistoryRepository
import com.voicenoteai.wear.presentation.haptic.Haptic
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {
    private val today = LocalDate.of(2026, 11, 7)
    private val task = HistoryTask("1", "Comprar café", today.plusDays(1), completed = false)

    private class FakeHistoryRepository(
        var loadResult: LoadTasksResult = LoadTasksResult.Loaded(emptyList(), LocalDate.of(2026, 11, 7)),
        var updateResult: SetTaskCompletedResult = SetTaskCompletedResult.Updated,
    ) : TaskHistoryRepository {
        var loads = 0
        var loadGate: CompletableDeferred<Unit>? = null
        var updateGate: CompletableDeferred<Unit>? = null
        val updates = mutableListOf<Pair<String, Boolean>>()

        override suspend fun recent(limit: Int): LoadTasksResult {
            loads++
            loadGate?.await()
            return loadResult
        }

        override suspend fun setCompleted(id: String, completed: Boolean): SetTaskCompletedResult {
            updates += id to completed
            updateGate?.await()
            return updateResult
        }
    }

    @Before
    fun setUp() = Dispatchers.setMain(StandardTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.viewModel(repository: FakeHistoryRepository) = HistoryViewModel(repository)

    @Test
    fun `refresh publishes the tasks and the day sent by the backend`() = runTest {
        val repository = FakeHistoryRepository(LoadTasksResult.Loaded(listOf(task), today))
        val vm = viewModel(repository)

        vm.refresh()
        advanceUntilIdle()

        assertEquals(HistoryUiState.Content(listOf(task), today), vm.state.value)
        assertEquals(1, repository.loads)
    }

    @Test
    fun `refreshing keeps the previous list until the new one arrives`() = runTest {
        val repository = FakeHistoryRepository(LoadTasksResult.Loaded(listOf(task), today))
        val vm = viewModel(repository)
        vm.refresh()
        advanceUntilIdle()

        repository.loadGate = CompletableDeferred()
        vm.refresh()
        runCurrent()

        assertEquals(HistoryUiState.Content(listOf(task), today), vm.state.value)

        repository.loadGate!!.complete(Unit)
        advanceUntilIdle()
        assertEquals(HistoryUiState.Content(listOf(task), today), vm.state.value)
    }

    @Test
    fun `an empty history has its own state`() = runTest {
        val repository = FakeHistoryRepository(LoadTasksResult.Loaded(emptyList(), today))
        val vm = viewModel(repository)

        vm.refresh()
        advanceUntilIdle()

        assertEquals(HistoryUiState.Empty, vm.state.value)
    }

    @Test
    fun `a load failure keeps its message and vibrates`() = runTest {
        val repository = FakeHistoryRepository(
            LoadTasksResult.Failed(HistoryError.NO_CONNECTION),
        )
        val vm = viewModel(repository)
        val haptics = mutableListOf<Haptic>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.haptics.toList(haptics) }
        runCurrent()

        vm.refresh()
        advanceUntilIdle()

        assertEquals(
            HistoryUiState.Error(R.string.error_history_no_connection, retryable = true),
            vm.state.value,
        )
        assertEquals(listOf(Haptic.ERROR), haptics)
    }

    @Test
    fun `a missing token offers no retry`() = runTest {
        val repository = FakeHistoryRepository(
            LoadTasksResult.Failed(HistoryError.NOT_CONFIGURED),
        )
        val vm = viewModel(repository)

        vm.refresh()
        advanceUntilIdle()

        assertEquals(
            HistoryUiState.Error(R.string.error_history_not_configured, retryable = false),
            vm.state.value,
        )
    }

    @Test
    fun `toggling marks the task immediately and confirms with a haptic`() = runTest {
        val repository = FakeHistoryRepository(LoadTasksResult.Loaded(listOf(task), today))
        val vm = viewModel(repository)
        vm.refresh()
        advanceUntilIdle()
        val haptics = mutableListOf<Haptic>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.haptics.toList(haptics) }
        runCurrent()

        repository.updateGate = CompletableDeferred()
        vm.setCompleted(task.id, true)
        runCurrent()

        // Optimista: la casilla ya está marcada aunque el backend no haya respondido.
        val pending = vm.state.value as HistoryUiState.Content
        assertTrue(pending.tasks.single().completed)
        assertEquals(setOf(task.id), pending.updatingIds)

        repository.updateGate!!.complete(Unit)
        advanceUntilIdle()

        val settled = vm.state.value as HistoryUiState.Content
        assertTrue(settled.tasks.single().completed)
        assertTrue(settled.updatingIds.isEmpty())
        assertEquals(listOf(task.id to true), repository.updates)
        assertEquals(listOf(Haptic.TOGGLE), haptics)
    }

    @Test
    fun `a failed toggle rolls back and explains why`() = runTest {
        val repository = FakeHistoryRepository(
            LoadTasksResult.Loaded(listOf(task), today),
            updateResult = SetTaskCompletedResult.Failed(HistoryError.NO_CONNECTION),
        )
        val vm = viewModel(repository)
        vm.refresh()
        advanceUntilIdle()
        val haptics = mutableListOf<Haptic>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.haptics.toList(haptics) }
        runCurrent()

        vm.setCompleted(task.id, true)
        advanceUntilIdle()

        val content = vm.state.value as HistoryUiState.Content
        assertFalse(content.tasks.single().completed)
        assertEquals(R.string.history_update_failed, content.messageRes)
        assertTrue(content.updatingIds.isEmpty())
        assertEquals(listOf(Haptic.ERROR), haptics)
    }

    @Test
    fun `a task deleted elsewhere disappears from the history`() = runTest {
        val repository = FakeHistoryRepository(
            LoadTasksResult.Loaded(listOf(task), today),
            updateResult = SetTaskCompletedResult.Failed(HistoryError.TASK_NOT_FOUND),
        )
        val vm = viewModel(repository)
        vm.refresh()
        advanceUntilIdle()

        vm.setCompleted(task.id, true)
        advanceUntilIdle()

        assertEquals(HistoryUiState.Empty, vm.state.value)
    }

    @Test
    fun `a second tap while the first update is in flight is ignored`() = runTest {
        val repository = FakeHistoryRepository(LoadTasksResult.Loaded(listOf(task), today))
        val vm = viewModel(repository)
        vm.refresh()
        advanceUntilIdle()

        repository.updateGate = CompletableDeferred()
        vm.setCompleted(task.id, true)
        runCurrent()
        vm.setCompleted(task.id, false)
        runCurrent()

        repository.updateGate!!.complete(Unit)
        advanceUntilIdle()

        assertEquals(listOf(task.id to true), repository.updates)
    }
}
