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
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()
    val selectedSector = state.activeColony.world.tileAt(selectedCoordinate ?: return@let null)

    MineItTheme {
        MineItScreen(
            state = state,
            selectedSector = selectedSector,
            selectedCoordinate = selectedCoordinate,
            statusMessage = statusMessage,
            onSelectLandingSite = viewModel::selectLandingSite,
            onSelectSector = viewModel::selectSector,
            onSurveySelectedSector = viewModel::surveySelectedSector,
            onAdvanceSurveyDay = viewModel::advanceSurveyDay,
        )
    }
}
