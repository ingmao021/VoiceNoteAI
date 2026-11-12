package com.voicenoteai.wear.presentation.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import com.voicenoteai.wear.presentation.settings.SettingsRoute
import com.voicenoteai.wear.presentation.settings.SettingsViewModel
import com.voicenoteai.wear.presentation.theme.VoltPalette
import kotlinx.coroutines.launch

private const val SETTINGS_PAGE = 0
private const val NOTE_PAGE = 1
private const val HISTORY_PAGE = 2

/**
 * Las tres páginas de la app: ajustes (0, arriba), la nota (1, la de inicio) y el historial (2, abajo).
 *
 * El gesto de subir solo se bloquea mientras la nota está ocupada (grabando o procesando), para que
 * un deslizamiento no interrumpa la captura. En el historial el gesto sigue activo para poder volver.
 */
@Composable
fun HomeRoute(
    noteViewModel: NoteViewModel,
    historyViewModel: HistoryViewModel,
    settingsViewModel: SettingsViewModel,
    onRecordingChanged: (Boolean) -> Unit,
) {
    val reminderMinutes by settingsViewModel.reminderMinutes.collectAsStateWithLifecycle()
    val hapticsEnabled by settingsViewModel.hapticsEnabled.collectAsStateWithLifecycle()
    val confirmDelete by settingsViewModel.confirmDelete.collectAsStateWithLifecycle()
    val noteState by noteViewModel.state.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState(initialPage = NOTE_PAGE, pageCount = { 3 })
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

    Box(Modifier.fillMaxSize().background(VoltPalette.Background)) {
        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            // Bloqueado solo mientras se graba/procesa y la página visible es la nota.
            userScrollEnabled = !busy || !onNotePage,
        ) { page ->
            when (page) {
                SETTINGS_PAGE -> SettingsRoute(viewModel = settingsViewModel, onClose = ::closeHistory)
                NOTE_PAGE -> NoteRoute(
                    viewModel = noteViewModel,
                    onRecordingChanged = onRecordingChanged,
                    onOpenHistory = ::openHistory,
                    hapticsEnabled = hapticsEnabled,
                )
                HISTORY_PAGE -> HistoryRoute(
                    viewModel = historyViewModel,
                    onClose = ::closeHistory,
                    active = pagerState.currentPage == HISTORY_PAGE,
                    reminderMinutes = reminderMinutes,
                    hapticsEnabled = hapticsEnabled,
                    confirmDelete = confirmDelete,
                )
            }
        }
    }
}
