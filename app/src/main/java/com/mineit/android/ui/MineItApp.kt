package com.mineit.android.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mineit.android.ui.theme.MineItTheme

@Composable
fun MineItApp(viewModel: GameViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val metrics by viewModel.metrics.collectAsStateWithLifecycle()
    val simulationSpeed by viewModel.simulationSpeed.collectAsStateWithLifecycle()
    val selectedCoordinate by viewModel.selectedSector.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()
    val selectedSector = selectedCoordinate?.let { state.activeColony.world.tileAt(it) }
    val selectedSurveyDays = selectedCoordinate?.let(viewModel::surveyDays)

    MineItTheme {
        MineItScreen(
            state = state,
            metrics = metrics,
            simulationSpeed = simulationSpeed,
            selectedSector = selectedSector,
            selectedCoordinate = selectedCoordinate,
            selectedSurveyDays = selectedSurveyDays,
            statusMessage = statusMessage,
            onSelectLandingSite = viewModel::selectLandingSite,
            onSelectSector = viewModel::selectSector,
            onSurveySelectedSector = viewModel::surveySelectedSector,
            onAdvanceDay = viewModel::advanceDay,
            onSetSimulationSpeed = viewModel::setSimulationSpeed,
        )
    }
}
