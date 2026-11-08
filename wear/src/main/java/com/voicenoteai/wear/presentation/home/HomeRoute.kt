package com.voicenoteai.wear.presentation.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.pager.VerticalPager
import androidx.wear.compose.foundation.pager.rememberPagerState
import com.voicenoteai.wear.presentation.history.HistoryRoute
import com.voicenoteai.wear.presentation.history.HistoryViewModel
import com.voicenoteai.wear.presentation.note.NoteRoute
import com.voicenoteai.wear.presentation.note.NoteUiState
import com.voicenoteai.wear.presentation.note.NoteViewModel
import com.voicenoteai.wear.presentation.theme.FurBackground
import kotlinx.coroutines.launch

private const val NOTE_PAGE = 0
private const val HISTORY_PAGE = 1

/**
 * Las dos páginas de la app: la nota (0) y el historial (1).
 *
 * El gesto de subir solo se bloquea mientras la nota está ocupada (grabando o procesando), para que
 * un deslizamiento no interrumpa la captura. En el historial el gesto sigue activo para poder volver.
 */
@Composable
fun HomeRoute(
    noteViewModel: NoteViewModel,
    historyViewModel: HistoryViewModel,
    onRecordingChanged: (Boolean) -> Unit,
) {
    val noteState by noteViewModel.state.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState(pageCount = { 2 })
    val scope = rememberCoroutineScope()

    val onNotePage = pagerState.currentPage == NOTE_PAGE
    val busy = when (val current = noteState) {
        is NoteUiState.Recording, is NoteUiState.Processing -> true
        is NoteUiState.Idle -> current.starting
        else -> false
    }

    fun openHistory() {
        scope.launch { pagerState.animateScrollToPage(HISTORY_PAGE) }
    }

    fun closeHistory() {
        scope.launch { pagerState.animateScrollToPage(NOTE_PAGE) }
    }

    // «Atrás» en el reloj vuelve a la nota en vez de cerrar la app.
    BackHandler(enabled = !onNotePage) { closeHistory() }

    // La primera carga la dispara la página al quedar visible, no el ViewModel en `init`.
    LaunchedEffect(pagerState.settledPage) {
        if (pagerState.settledPage == HISTORY_PAGE) historyViewModel.refresh()
    }

    Box(Modifier.fillMaxSize()) {
        FurBackground()
        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            // Bloqueado solo mientras se graba/procesa y la página visible es la nota.
            userScrollEnabled = !busy || !onNotePage,
        ) { page ->
            when (page) {
                NOTE_PAGE -> NoteRoute(
                    viewModel = noteViewModel,
                    onRecordingChanged = onRecordingChanged,
                    onOpenHistory = ::openHistory,
                )
                HISTORY_PAGE -> HistoryRoute(viewModel = historyViewModel, onClose = ::closeHistory)
            }
        }
    }
}
