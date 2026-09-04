package com.mineit.android.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mineit.android.app.AppComposition
import com.mineit.android.app.SimulationClock
import com.mineit.android.app.persistence.PersistenceLoadResult
import com.mineit.android.app.persistence.PersistenceSaveResult
import com.mineit.android.domain.colony.ColonyNetworkSnapshot
import com.mineit.android.domain.colony.DevelopmentActionResult
import com.mineit.android.domain.colony.DevelopmentPreview
import com.mineit.android.domain.colony.HeadquartersDepartureGate
import com.mineit.android.domain.colony.SpaceportStatus
import com.mineit.android.domain.model.ColonyStatus
import com.mineit.android.domain.model.GameState
import com.mineit.android.domain.simulation.ColonyMetrics
import com.mineit.android.domain.simulation.DailySimulationResult
import com.mineit.android.domain.world.DevelopmentKind
import com.mineit.android.domain.world.SectorCoordinate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class GameScreen {
    MAIN_MENU,
    GAME,
}

class GameViewModel(application: Application) : AndroidViewModel(application) {
    private val composition = AppComposition(application)
    private val session = composition.gameSession
    private val newGameFactory = composition.newGameFactory
    private val surveyGameService = composition.surveyGameService
    private val dailySimulationEngine = composition.dailySimulationEngine
    private val developmentService = composition.colonyDevelopmentService
    private val networkService = composition.colonyNetworkService
    private val headquartersService = composition.headquartersService
    private val spaceportService = composition.spaceportService

    val state: StateFlow<GameState> = session.state

    private val _screen = MutableStateFlow(GameScreen.MAIN_MENU)
    val screen: StateFlow<GameScreen> = _screen.asStateFlow()

    private val _entryReady = MutableStateFlow(false)
    val entryReady: StateFlow<Boolean> = _entryReady.asStateFlow()

    private val _canContinue = MutableStateFlow(false)
    val canContinue: StateFlow<Boolean> = _canContinue.asStateFlow()

    private val _metrics = MutableStateFlow(dailySimulationEngine.recalculate(state.value))
    val metrics: StateFlow<ColonyMetrics> = _metrics.asStateFlow()

    private val _network = MutableStateFlow(networkService.calculate(state.value))
    val network: StateFlow<ColonyNetworkSnapshot> = _network.asStateFlow()

    private val _spaceport = MutableStateFlow(spaceportService.status(state.value, _network.value))
    val spaceport: StateFlow<SpaceportStatus> = _spaceport.asStateFlow()

    private val _selectedSector = MutableStateFlow<SectorCoordinate?>(null)
    val selectedSector: StateFlow<SectorCoordinate?> = _selectedSector.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val simulationClock = SimulationClock(
        scope = viewModelScope,
        advanceDay = { advanceSimulationDay(fromClock = true) },
    )
    val simulationSpeed: StateFlow<Int> = simulationClock.speed

    init {
        simulationClock.start()
        viewModelScope.launch {
            when (val result = session.restoreFromPersistence()) {
                PersistenceLoadResult.NotFound -> {
                    refreshDerived(state.value)
                    _canContinue.value = false
                    _statusMessage.value = null
                }
                is PersistenceLoadResult.Loaded -> {
                    refreshDerived(result.state)
                    val colonyLost = result.state.activeColony.status == ColonyStatus.DEAD
                    _canContinue.value = !colonyLost
                    _statusMessage.value = when {
                        colonyLost -> "Previous colony was lost. Start a new game to begin again."
                        result.recoveredFromBackup -> "Recovered the native save from the previous-good backup."
                        else -> null
                    }
                }
                is PersistenceLoadResult.Failure -> {
                    refreshDerived(state.value)
                    _canContinue.value = false
                    _statusMessage.value = "Save restore failed. Start a new game to replace the unusable save."
                }
            }
            _screen.value = GameScreen.MAIN_MENU
            _entryReady.value = true
        }
    }

    fun startNewGame() {
        if (!_entryReady.value) return
        viewModelScope.launch {
            simulationClock.setSpeed(0)
            val freshState = composition.createNewGame()
            val result = session.reset("new-game", freshState)
            _selectedSector.value = null
            refreshDerived(result.state)
            _canContinue.value = result.persistence is PersistenceSaveResult.Success
            _screen.value = GameScreen.GAME
            _statusMessage.value = if (result.persistence is PersistenceSaveResult.Success) {
                "New Contract 01 game started. Choose a landing site."
            } else {
                "New game started, but the native save could not be written."
            }
        }
    }

    fun continueGame() {
        if (!_entryReady.value || !_canContinue.value || state.value.activeColony.status == ColonyStatus.DEAD) return
        simulationClock.setSpeed(0)
        _selectedSector.value = null
        refreshDerived(state.value)
        _screen.value = GameScreen.GAME
        _statusMessage.value = null
    }

    fun selectLandingSite(index: Int) {
        viewModelScope.launch {
            val result = session.commit("select-landing-site") { current -> newGameFactory.settleLandingSite(current, index) }
            _selectedSector.value = null
            refreshDerived(result.state)
            _statusMessage.value = if (result.persistence is PersistenceSaveResult.Success) {
                "Landing Site ${index + 1} selected. Simulation remains paused until you start it."
            } else "Landing site selected, but the native save could not be written."
        }
    }

    fun selectSector(coordinate: SectorCoordinate) {
        _selectedSector.value = coordinate
        _statusMessage.value = null
    }

    fun surveyDays(coordinate: SectorCoordinate): Int? = surveyGameService.surveyDays(
        state.value,
        coordinate,
        commandEfficiency = _network.value.headquarters.efficiency,
    )

    fun surveySelectedSector() {
        val coordinate = _selectedSector.value ?: return
        val snapshot = state.value
        val commandEfficiency = _network.value.headquarters.efficiency
        val days = surveyGameService.surveyDays(snapshot, coordinate, commandEfficiency) ?: return
        viewModelScope.launch {
            val result = session.commit("enqueue-survey") { current ->
                surveyGameService.enqueue(current, coordinate, networkService.calculate(current).headquarters.efficiency)
            }
            refreshDerived(result.state)
            _statusMessage.value = if (result.persistence is PersistenceSaveResult.Success) {
                "Sector ${coordinate.x},${coordinate.y} queued for survey ($days days at current capability)."
            } else "Survey queued, but the native save could not be written."
        }
    }

    fun buildingPreview(kind: DevelopmentKind): DevelopmentPreview? {
        val coordinate = _selectedSector.value ?: return null
        return developmentService.buildingPreview(state.value, coordinate, kind)
    }

    fun extractionPreview(): DevelopmentPreview? {
        val coordinate = _selectedSector.value ?: return null
        return developmentService.extractionPreview(state.value, coordinate)
    }

    fun upgradePreview(): DevelopmentPreview? {
        val coordinate = _selectedSector.value ?: return null
        return developmentService.buildingUpgradePreview(state.value, coordinate)
    }

    fun buildSelected(kind: DevelopmentKind) = commitDevelopment("build-${kind.name.lowercase()}") { current, coordinate ->
        developmentService.placeBuilding(current, coordinate, kind)
    }

    fun developSelectedResource() = commitDevelopment("develop-extraction") { current, coordinate ->
        developmentService.developExtraction(current, coordinate)
    }

    fun upgradeSelected() = commitDevelopment("upgrade-development") { current, coordinate ->
        developmentService.upgrade(current, coordinate)
    }

    fun demolishSelected() = commitDevelopment("demolish-development") { current, coordinate ->
        developmentService.demolish(current, coordinate)
    }

    fun setSelectedAsPrimaryHeadquarters() {
        val coordinate = _selectedSector.value ?: return
        viewModelScope.launch {
            val preview = headquartersService.setPrimary(state.value, coordinate)
            if (!preview.ok) {
                _statusMessage.value = preview.message
                return@launch
            }
            val result = session.commit("set-primary-headquarters") { current -> headquartersService.setPrimary(current, coordinate).state }
            refreshDerived(result.state)
            _statusMessage.value = if (result.persistence is PersistenceSaveResult.Success) preview.message else "Primary changed, but the native save could not be written."
        }
    }

    fun departureGate(): HeadquartersDepartureGate = headquartersService.departureGate(state.value)

    fun advanceDay() {
        viewModelScope.launch { advanceSimulationDay(fromClock = false) }
    }

    fun setSimulationSpeed(speed: Int) {
        simulationClock.setSpeed(speed)
        _statusMessage.value = if (speed == 0) "Simulation paused." else "Simulation running at ${speed}×."
    }

    private fun commitDevelopment(
        reason: String,
        action: (GameState, SectorCoordinate) -> DevelopmentActionResult,
    ) {
        val coordinate = _selectedSector.value ?: return
        val preview = action(state.value, coordinate)
        if (!preview.ok) {
            _statusMessage.value = preview.message
            return
        }
        viewModelScope.launch {
            var applied: DevelopmentActionResult? = null
            val result = session.commit(reason) { current -> action(current, coordinate).also { applied = it }.state }
            refreshDerived(result.state)
            val message = requireNotNull(applied).message
            _statusMessage.value = if (result.persistence is PersistenceSaveResult.Success) message else "$message Save write failed."
        }
    }

    private suspend fun advanceSimulationDay(fromClock: Boolean) {
        if (!state.value.activeColony.world.settled) return
        var simulationResult: DailySimulationResult? = null
        val commit = session.commit(if (fromClock) "clock-day" else "advance-day") { current ->
            dailySimulationEngine.advanceDay(current).also { simulationResult = it }.state
        }
        val result = requireNotNull(simulationResult)
        refreshDerived(commit.state, completedDayMetrics = result.metrics)

        if (result.colonyDied) {
            simulationClock.setSpeed(0)
            _selectedSector.value = null
            _canContinue.value = false
            _screen.value = GameScreen.MAIN_MENU
            _statusMessage.value = if (commit.persistence is PersistenceSaveResult.Success) {
                "Colony lost. Start a new game to begin again."
            } else {
                "Colony lost. The final save write failed; start a new game to begin again."
            }
            return
        }

        if (commit.persistence is PersistenceSaveResult.Failure) {
            _statusMessage.value = "Day advanced, but the native save could not be written."
            return
        }
        _statusMessage.value = when {
            result.deaths > .0001 -> "Life-support shortage caused ${formatPopulation(result.deaths)} deaths this day."
            result.completedSurveys.isNotEmpty() -> "Survey completed for ${result.completedSurveys.joinToString { "${it.x},${it.y}" }}."
            !fromClock -> "Advanced one complete MineIT simulation day."
            else -> null
        }
    }

    private fun refreshDerived(gameState: GameState, completedDayMetrics: ColonyMetrics? = null) {
        val nextNetwork = networkService.calculate(gameState)
        _network.value = nextNetwork
        _spaceport.value = spaceportService.status(gameState, nextNetwork)
        _metrics.value = completedDayMetrics ?: dailySimulationEngine.recalculate(gameState)
    }

    private fun formatPopulation(value: Double): String = if (value % 1.0 == 0.0) value.toLong().toString() else "%.2f".format(value)

    override fun onCleared() {
        simulationClock.stop()
        super.onCleared()
    }
}
