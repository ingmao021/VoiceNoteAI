package com.voicenoteai.wear.presentation.note

import com.voicenoteai.wear.R
import com.voicenoteai.wear.data.speech.FakeVoiceRecognizer
import com.voicenoteai.wear.domain.model.CaptureMode
import com.voicenoteai.wear.domain.model.SaveNoteResult
import com.voicenoteai.wear.domain.model.SubmissionError
import com.voicenoteai.wear.domain.repository.NoteRepository
import com.voicenoteai.wear.domain.usecase.CheckCapturePreconditionsUseCase
import com.voicenoteai.wear.domain.usecase.SaveNoteUseCase
import com.voicenoteai.wear.domain.usecase.WarmUpBackendUseCase
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
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

@OptIn(ExperimentalCoroutinesApi::class)
class NoteViewModelTest {
    private class FakeNoteRepository(
        var responses: ArrayDeque<SaveNoteResult> = ArrayDeque(),
        var gate: CompletableDeferred<Unit>? = null,
    ) : NoteRepository {
        val sent = mutableListOf<String>()
        var pings = 0

        override suspend fun save(text: String): SaveNoteResult {
            sent += text
            gate?.await()
            return responses.removeFirstOrNull() ?: SaveNoteResult.Saved("Título", structured = true)
        }

        override fun warmUp() {
            pings++
        }
    }

    @Before
    fun setUp() = Dispatchers.setMain(StandardTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.viewModel(
        repository: NoteRepository = FakeNoteRepository(),
        mode: CaptureMode = CaptureMode.RECOGNIZER,
        lowBattery: Boolean = false,
        speechDurationMs: Long = 1_000,
    ): NoteViewModel {
        val recognizer = FakeVoiceRecognizer(backgroundScope, listOf("comprar café mañana"), speechDurationMs)
        return NoteViewModel(
            recognizer = recognizer,
            saveNote = SaveNoteUseCase(repository),
            warmUpBackend = WarmUpBackendUseCase(repository),
            checkCapturePreconditions = CheckCapturePreconditionsUseCase(mode) { lowBattery },
            captureMode = mode,
        ).also { runCurrent() }
    }

    private fun NoteViewModel.capture() {
        assertTrue(prepareCapture())
        startCapture()
    }

    @Test
    fun `main flow reaches SUCCESS and returns to IDLE after 3 s`() = runTest {
        val repository = FakeNoteRepository()
        val vm = viewModel(repository)
        val haptics = mutableListOf<Haptic>()
        backgroundScope.launch { vm.haptics.toList(haptics) }
        assertEquals(1, repository.pings)

        vm.capture()
        assertEquals(NoteUiState.Idle(starting = true), vm.state.value)
        advanceTimeBy(FakeVoiceRecognizer.READY_DELAY_MS + 1)
        assertTrue(vm.state.value is NoteUiState.Recording)

        advanceTimeBy(FakeVoiceRecognizer.SPEECH_DELAY_MS + 1_000)
        assertEquals(NoteUiState.Processing(), vm.state.value)

        advanceTimeBy(FakeVoiceRecognizer.RESULT_DELAY_MS + 1)
        assertEquals(NoteUiState.Success("Título"), vm.state.value)
        assertEquals(listOf("comprar café mañana"), repository.sent)
        assertEquals(listOf(Haptic.START, Haptic.SUCCESS), haptics)

        advanceTimeBy(NoteViewModel.SUCCESS_DISPLAY_MS + 1)
        assertEquals(NoteUiState.Idle(), vm.state.value)
        assertEquals("cada entrada en INICIAL calienta el servidor", 2, repository.pings)
    }

    @Test
    fun `the ring grows with the voice while recording`() = runTest {
        val vm = viewModel()
        vm.capture()
        advanceTimeBy(FakeVoiceRecognizer.READY_DELAY_MS + FakeVoiceRecognizer.SPEECH_DELAY_MS + 500)
        val state = vm.state.value as NoteUiState.Recording
        assertTrue(state.speaking)
        assertTrue(state.level in 0.01f..1f)
    }

    @Test
    fun `the 60 s guard produces a note, not an error`() = runTest {
        val repository = FakeNoteRepository()
        val vm = viewModel(repository, speechDurationMs = 120_000)
        vm.capture()
        advanceTimeBy(FakeVoiceRecognizer.READY_DELAY_MS + NoteViewModel.MAX_DURATION_MS + 1)
        advanceTimeBy(FakeVoiceRecognizer.RESULT_DELAY_MS + 1)
        assertEquals(NoteUiState.Success("Título"), vm.state.value)
        assertEquals(1, repository.sent.size)
    }

    @Test
    fun `stopping delivers what was heard`() = runTest {
        val repository = FakeNoteRepository()
        val vm = viewModel(repository, speechDurationMs = 30_000)
        vm.capture()
        advanceTimeBy(FakeVoiceRecognizer.READY_DELAY_MS + FakeVoiceRecognizer.SPEECH_DELAY_MS + 500)
        vm.stopCapture()
        advanceUntilIdle()
        assertEquals(listOf("comprar café mañana"), repository.sent)
    }

    @Test
    fun `PROCESSING staggers its text at 5 and 10 s`() = runTest {
        val repository = FakeNoteRepository(gate = CompletableDeferred())
        val vm = viewModel(repository)
        vm.onDictationResult("nota")
        runCurrent()
        assertEquals(NoteUiState.Processing(ProcessingStage.PROCESSING), vm.state.value)
        advanceTimeBy(5_001)
        assertEquals(NoteUiState.Processing(ProcessingStage.ALMOST_DONE), vm.state.value)
        advanceTimeBy(5_000)
        assertEquals(NoteUiState.Processing(ProcessingStage.WAKING_SERVER), vm.state.value)
        repository.gate!!.complete(Unit)
        runCurrent()
        assertEquals(NoteUiState.Success("Título"), vm.state.value)
    }

    @Test
    fun `a submission error keeps the text and retrying does not ask to speak again`() = runTest {
        val repository = FakeNoteRepository(ArrayDeque(listOf(SaveNoteResult.Failed(SubmissionError.NO_CONNECTION))))
        val vm = viewModel(repository)
        vm.onDictationResult("idea importante")
        runCurrent()
        assertEquals(
            NoteUiState.Error(R.string.error_submission_no_connection, ErrorAction.RESEND),
            vm.state.value,
        )

        vm.resend()
        runCurrent()
        assertEquals(NoteUiState.Success("Título"), vm.state.value)
        assertEquals(listOf("idea importante", "idea importante"), repository.sent)
    }

    @Test
    fun `revoked access offers no retry`() = runTest {
        val repository = FakeNoteRepository(ArrayDeque(listOf(SaveNoteResult.Failed(SubmissionError.RELINK_ACCOUNT))))
        val vm = viewModel(repository)
        vm.onDictationResult("nota")
        runCurrent()
        assertEquals(
            NoteUiState.Error(R.string.error_submission_relink_account, ErrorAction.GO_BACK),
            vm.state.value,
        )
    }

    @Test
    fun `empty dictation is nothing heard and cancelled returns to IDLE`() = runTest {
        val vm = viewModel()
        vm.onDictationResult("  ")
        assertEquals(NoteUiState.Error(R.string.error_capture_nothing_heard, ErrorAction.SPEAK_AGAIN), vm.state.value)
        vm.onDictationResult(null)
        assertEquals(NoteUiState.Idle(), vm.state.value)
    }

    @Test
    fun `leaving during capture discards it`() = runTest {
        val repository = FakeNoteRepository()
        val vm = viewModel(repository)
        vm.capture()
        advanceTimeBy(FakeVoiceRecognizer.READY_DELAY_MS + 1)
        assertTrue(vm.cancelOnExit())
        advanceUntilIdle()
        assertEquals(NoteUiState.Idle(), vm.state.value)
        assertTrue(repository.sent.isEmpty())
        assertFalse("fuera de la captura no cierra la app", vm.cancelOnExit())
    }

    @Test
    fun `low battery and missing recognizer block the capture`() = runTest {
        val lowBattery = viewModel(lowBattery = true)
        assertFalse(lowBattery.prepareCapture())
        assertEquals(NoteUiState.Error(R.string.error_capture_low_battery, ErrorAction.GO_BACK), lowBattery.state.value)

        val noRecognizer = viewModel(mode = CaptureMode.NONE)
        assertFalse(noRecognizer.prepareCapture())
        assertEquals(
            NoteUiState.Error(R.string.error_capture_recognizer_unavailable, ErrorAction.GO_BACK),
            noRecognizer.state.value,
        )
    }

    @Test
    fun `permanently denied permission leads to settings`() = runTest {
        val vm = viewModel()
        vm.onPermissionDenied(permanent = true)
        assertEquals(ErrorAction.OPEN_SETTINGS, (vm.state.value as NoteUiState.Error).action)
        vm.onPermissionDenied(permanent = false)
        assertEquals(ErrorAction.SPEAK_AGAIN, (vm.state.value as NoteUiState.Error).action)
    }
}
