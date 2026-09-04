package com.mineit.android.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mineit.android.ui.theme.MineItTheme

@Composable
fun MineItApp(viewModel: GameViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val selectedCoordinate by viewModel.selectedSector.collectAsStateWithLifecycle()
    val selectedSector = state.sectors.firstOrNull { it.coordinate == selectedCoordinate }

    MineItTheme {
        MineItScreen(
            state = state,
            selectedSector = selectedSector,
            onSelectSector = viewModel::selectSector,
            onAdvanceDay = viewModel::advanceDay,
        )
    }
}
