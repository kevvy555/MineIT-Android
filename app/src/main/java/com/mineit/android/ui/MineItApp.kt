package com.mineit.android.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mineit.android.domain.world.DevelopmentKind
import com.mineit.android.ui.theme.MineItTheme

@Composable
fun MineItApp(viewModel: GameViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val metrics by viewModel.metrics.collectAsStateWithLifecycle()
    val network by viewModel.network.collectAsStateWithLifecycle()
    val spaceport by viewModel.spaceport.collectAsStateWithLifecycle()
    val simulationSpeed by viewModel.simulationSpeed.collectAsStateWithLifecycle()
    val selectedCoordinate by viewModel.selectedSector.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()
    val selectedSector = selectedCoordinate?.let { state.activeColony.world.tileAt(it) }
    val selectedSurveyDays = selectedCoordinate?.let(viewModel::surveyDays)

    MineItTheme {
        MineItScreen(
            state = state,
            metrics = metrics,
            network = network,
            spaceport = spaceport,
            simulationSpeed = simulationSpeed,
            selectedSector = selectedSector,
            selectedCoordinate = selectedCoordinate,
            selectedSurveyDays = selectedSurveyDays,
            statusMessage = statusMessage,
            powerPreview = viewModel.buildingPreview(DevelopmentKind.POWER),
            housingPreview = viewModel.buildingPreview(DevelopmentKind.HOUSING),
            industryPreview = viewModel.buildingPreview(DevelopmentKind.INDUSTRY),
            headquartersPreview = viewModel.buildingPreview(DevelopmentKind.HEADQUARTERS),
            extractionPreview = viewModel.extractionPreview(),
            upgradePreview = viewModel.upgradePreview(),
            departureGate = viewModel.departureGate(),
            onSelectLandingSite = viewModel::selectLandingSite,
            onSelectSector = viewModel::selectSector,
            onSurveySelectedSector = viewModel::surveySelectedSector,
            onBuild = viewModel::buildSelected,
            onDevelopExtraction = viewModel::developSelectedResource,
            onUpgrade = viewModel::upgradeSelected,
            onDemolish = viewModel::demolishSelected,
            onSetPrimaryHeadquarters = viewModel::setSelectedAsPrimaryHeadquarters,
            onAdvanceDay = viewModel::advanceDay,
            onSetSimulationSpeed = viewModel::setSimulationSpeed,
        )
    }
}
