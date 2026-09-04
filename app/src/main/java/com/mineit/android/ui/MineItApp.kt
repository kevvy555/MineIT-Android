package com.mineit.android.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mineit.android.domain.world.DevelopmentKind
import com.mineit.android.ui.theme.MineItTheme

@Composable
fun MineItApp(viewModel: GameViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val screen by viewModel.screen.collectAsStateWithLifecycle()
    val entryReady by viewModel.entryReady.collectAsStateWithLifecycle()
    val canContinue by viewModel.canContinue.collectAsStateWithLifecycle()
    val metrics by viewModel.metrics.collectAsStateWithLifecycle()
    val network by viewModel.network.collectAsStateWithLifecycle()
    val spaceport by viewModel.spaceport.collectAsStateWithLifecycle()
    val simulationSpeed by viewModel.simulationSpeed.collectAsStateWithLifecycle()
    val selectedCoordinates by viewModel.selectedSectors.collectAsStateWithLifecycle()
    val mapFocus by viewModel.mapFocus.collectAsStateWithLifecycle()
    val mapFilters by viewModel.mapFilters.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()

    MineItTheme {
        if (screen == GameScreen.MAIN_MENU) {
            MainMenuScreen(
                ready = entryReady,
                canContinue = canContinue,
                statusMessage = statusMessage,
                onContinue = viewModel::continueGame,
                onNewGame = viewModel::startNewGame,
            )
        } else {
            BackHandler { viewModel.handleBack() }
            val selectedTiles = selectedCoordinates
                .mapNotNull { state.activeColony.world.tileAt(it) }
                .sortedWith(compareBy({ it.coordinate.y }, { it.coordinate.x }))
            val singleCoordinate = selectedCoordinates.singleOrNull()
            MineItScreen(
                state = state,
                metrics = metrics,
                network = network,
                spaceport = spaceport,
                simulationSpeed = simulationSpeed,
                selectedTiles = selectedTiles,
                selectedSurveyDays = singleCoordinate?.let(viewModel::surveyDays),
                surveyableSelectedCount = viewModel.surveyableSelectedCount(),
                statusMessage = statusMessage,
                mapFocus = mapFocus,
                mapFilters = mapFilters,
                powerPreview = viewModel.buildingPreview(DevelopmentKind.POWER),
                housingPreview = viewModel.buildingPreview(DevelopmentKind.HOUSING),
                industryPreview = viewModel.buildingPreview(DevelopmentKind.INDUSTRY),
                headquartersPreview = viewModel.buildingPreview(DevelopmentKind.HEADQUARTERS),
                extractionPreview = viewModel.extractionPreview(),
                upgradePreview = viewModel.upgradePreview(),
                departureGate = viewModel.departureGate(),
                onSelectLandingSite = viewModel::selectLandingSite,
                onSelectSector = viewModel::selectSector,
                onBeginMultiSelect = viewModel::beginMultiSelect,
                onAddMultiSelect = viewModel::addMultiSelect,
                onSurveySelectedSector = viewModel::surveySelectedSector,
                onSurveySelectedSectors = viewModel::surveySelectedSectors,
                onClearSelection = viewModel::clearSelection,
                onSetMapFocus = viewModel::setMapFocus,
                onToggleMapFilter = viewModel::toggleMapFilter,
                onClearMapFilters = viewModel::clearMapFilters,
                onBuild = viewModel::buildSelected,
                onDevelopExtraction = viewModel::developSelectedResource,
                onUpgrade = viewModel::upgradeSelected,
                onDemolish = viewModel::demolishSelected,
                onSetPrimaryHeadquarters = viewModel::setSelectedAsPrimaryHeadquarters,
                onAdvanceDay = viewModel::advanceDay,
                onSetSimulationSpeed = viewModel::setSimulationSpeed,
                onMainMenu = viewModel::returnToMainMenu,
            )
        }
    }
}
