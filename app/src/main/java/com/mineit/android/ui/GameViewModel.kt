package com.mineit.android.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mineit.android.app.AppComposition
import com.mineit.android.app.SimulationClock
import com.mineit.android.app.persistence.PersistenceLoadResult
import com.mineit.android.app.persistence.PersistenceSaveResult
import com.mineit.android.domain.buyers.BuyerCollectionProjection
import com.mineit.android.domain.buyers.BuyerContract
import com.mineit.android.domain.buyers.BuyerOffer
import com.mineit.android.domain.colony.ColonyNetworkSnapshot
import com.mineit.android.domain.colony.DevelopmentActionResult
import com.mineit.android.domain.colony.DevelopmentPreview
import com.mineit.android.domain.colony.HeadquartersDepartureGate
import com.mineit.android.domain.colony.PopulationSupportCapacity
import com.mineit.android.domain.colony.SpaceportStatus
import com.mineit.android.domain.contracts.ContractScore
import com.mineit.android.domain.events.CorporateEvent
import com.mineit.android.domain.events.CorporateEventType
import com.mineit.android.domain.model.ColonyStatus
import com.mineit.android.domain.model.GameState
import com.mineit.android.domain.model.ResourceId
import com.mineit.android.domain.resources.ResourceCategory
import com.mineit.android.domain.simulation.ColonyMetrics
import com.mineit.android.domain.trade.ColonistTransferProjection
import com.mineit.android.domain.trade.TradeQuote
import com.mineit.android.domain.world.DevelopmentKind
import com.mineit.android.domain.world.SectorCoordinate
import com.mineit.android.ui.map.MapFocus
import com.mineit.android.ui.map.MapStateFilter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.max

enum class GameScreen { MAIN_MENU, GAME }
enum class CommercialPanel { TRADE, CONTRACT, BUYERS, LOG }

class GameViewModel(application: Application) : AndroidViewModel(application) {
    private val composition = AppComposition(application)
    private val session = composition.gameSession
    private val newGameFactory = composition.newGameFactory
    private val surveyGameService = composition.surveyGameService
    private val dailySimulationEngine = composition.dailySimulationEngine
    private val commercialDayService = composition.commercialDayService
    private val developmentService = composition.colonyDevelopmentService
    private val networkService = composition.colonyNetworkService
    private val headquartersService = composition.headquartersService
    private val populationSupportService = composition.populationSupportService
    private val spaceportService = composition.spaceportService
    private val tradeService = composition.corporateTradeService
    private val contractService = composition.contractService
    private val buyerService = composition.buyerService
    private val corporateEventService = composition.corporateEventService

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
    private val _selectedSectors = MutableStateFlow<Set<SectorCoordinate>>(emptySet())
    val selectedSectors: StateFlow<Set<SectorCoordinate>> = _selectedSectors.asStateFlow()
    private val _mapFocus = MutableStateFlow(MapFocus.ALL)
    val mapFocus: StateFlow<MapFocus> = _mapFocus.asStateFlow()
    private val _mapFilters = MutableStateFlow<Set<MapStateFilter>>(emptySet())
    val mapFilters: StateFlow<Set<MapStateFilter>> = _mapFilters.asStateFlow()
    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()
    private val _commercialPanel = MutableStateFlow<CommercialPanel?>(null)
    val commercialPanel: StateFlow<CommercialPanel?> = _commercialPanel.asStateFlow()
    private val _currentCorporateEvent = MutableStateFlow<CorporateEvent?>(null)
    val currentCorporateEvent: StateFlow<CorporateEvent?> = _currentCorporateEvent.asStateFlow()

    private val simulationClock = SimulationClock(scope = viewModelScope, advanceDay = { advanceSimulationDay(fromClock = true) })
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
                    val runLost = result.state.activeColony.status in setOf(ColonyStatus.DEAD, ColonyStatus.CONTRACT_FAILED) || result.state.company.gameOver
                    _canContinue.value = !runLost
                    _statusMessage.value = when {
                        runLost -> "Previous company run has ended. Start a new game to begin again."
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
            val result = session.reset("new-game", composition.createNewGame())
            resetMapUi(); _commercialPanel.value = null
            refreshDerived(result.state)
            _canContinue.value = result.persistence is PersistenceSaveResult.Success
            _screen.value = GameScreen.GAME
            _statusMessage.value = if (result.persistence is PersistenceSaveResult.Success) "New Contract 01 game started. Choose a landing site." else "New game started, but the native save could not be written."
        }
    }

    fun continueGame() {
        if (!_entryReady.value || !_canContinue.value || state.value.company.gameOver) return
        simulationClock.setSpeed(0); resetMapUi(); refreshDerived(state.value)
        _screen.value = GameScreen.GAME; _statusMessage.value = null
    }

    fun returnToMainMenu() {
        simulationClock.setSpeed(0); clearSelection(); _commercialPanel.value = null
        _canContinue.value = state.value.activeColony.status !in setOf(ColonyStatus.DEAD, ColonyStatus.CONTRACT_FAILED) && !state.value.company.gameOver
        _screen.value = GameScreen.MAIN_MENU; _statusMessage.value = null
    }

    fun handleBack() {
        when {
            _commercialPanel.value != null -> _commercialPanel.value = null
            _selectedSectors.value.isNotEmpty() -> clearSelection()
            else -> returnToMainMenu()
        }
    }

    fun selectLandingSite(index: Int) {
        viewModelScope.launch {
            val result = session.commit("select-landing-site") { current -> newGameFactory.settleLandingSite(current, index) }
            clearSelection(); refreshDerived(result.state)
            _statusMessage.value = if (result.persistence is PersistenceSaveResult.Success) "Landing Site ${index + 1} selected. Simulation remains paused until you start it." else "Landing site selected, but the native save could not be written."
        }
    }

    fun selectSector(coordinate: SectorCoordinate) { _selectedSector.value = coordinate; _selectedSectors.value = setOf(coordinate); _statusMessage.value = null }
    fun beginMultiSelect(coordinate: SectorCoordinate) { _selectedSector.value = coordinate; _selectedSectors.value = setOf(coordinate); _statusMessage.value = "Drag across sectors to build a survey selection." }
    fun addMultiSelect(coordinate: SectorCoordinate) { _selectedSectors.update { it + coordinate } }
    fun clearSelection() { _selectedSector.value = null; _selectedSectors.value = emptySet(); _statusMessage.value = null }
    fun setMapFocus(focus: MapFocus) { _mapFocus.value = focus }
    fun toggleMapFilter(filter: MapStateFilter) { _mapFilters.update { if (filter in it) it - filter else it + filter } }
    fun clearMapFilters() { _mapFilters.value = emptySet(); _mapFocus.value = MapFocus.ALL }

    fun surveyDays(coordinate: SectorCoordinate): Int? = surveyGameService.surveyDays(state.value, coordinate, commandEfficiency = _network.value.headquarters.efficiency)
    fun surveyableSelectedCount(): Int = _selectedSectors.value.count { surveyDays(it) != null }

    fun surveySelectedSector() {
        val coordinate = singleActionCoordinate() ?: return
        val days = surveyDays(coordinate) ?: return
        viewModelScope.launch {
            val result = session.commit("enqueue-survey") { current -> surveyGameService.enqueue(current, coordinate, networkService.calculate(current).headquarters.efficiency) }
            refreshDerived(result.state)
            _statusMessage.value = if (result.persistence is PersistenceSaveResult.Success) "Sector ${coordinate.x},${coordinate.y} queued for survey ($days days at current capability)." else "Survey queued, but the native save could not be written."
        }
    }

    fun surveySelectedSectors() {
        val coordinates = _selectedSectors.value.sortedWith(compareBy<SectorCoordinate> { it.y }.thenBy { it.x })
        if (coordinates.isEmpty()) return
        viewModelScope.launch {
            var queuedCount = 0
            val result = session.commit("enqueue-survey-selection") { current ->
                var next = current
                coordinates.forEach { coordinate ->
                    val before = next.activeColony.world.activeSurveys.size + next.activeColony.world.surveyQueue.size
                    next = surveyGameService.enqueue(next, coordinate, networkService.calculate(next).headquarters.efficiency)
                    if (next.activeColony.world.activeSurveys.size + next.activeColony.world.surveyQueue.size > before) queuedCount++
                }
                next
            }
            refreshDerived(result.state)
            _statusMessage.value = when {
                result.persistence is PersistenceSaveResult.Failure -> "$queuedCount sectors queued, but the native save could not be written."
                queuedCount > 0 -> "$queuedCount sectors added to the survey plan."
                else -> "No selected sectors are currently surveyable."
            }
        }
    }

    fun buildingPreview(kind: DevelopmentKind): DevelopmentPreview? = singleActionCoordinate()?.let { developmentService.buildingPreview(state.value, it, kind) }
    fun extractionPreview(): DevelopmentPreview? = singleActionCoordinate()?.let { developmentService.extractionPreview(state.value, it) }
    fun upgradePreview(): DevelopmentPreview? = singleActionCoordinate()?.let { developmentService.buildingUpgradePreview(state.value, it) }
    fun buildSelected(kind: DevelopmentKind) = commitDevelopment("build-${kind.name.lowercase()}") { current, coordinate -> developmentService.placeBuilding(current, coordinate, kind) }
    fun developSelectedResource() = commitDevelopment("develop-extraction") { current, coordinate -> developmentService.developExtraction(current, coordinate) }
    fun upgradeSelected() = commitDevelopment("upgrade-development") { current, coordinate -> developmentService.upgrade(current, coordinate) }
    fun demolishSelected() = commitDevelopment("demolish-development") { current, coordinate -> developmentService.demolish(current, coordinate) }

    fun setSelectedAsPrimaryHeadquarters() {
        val coordinate = singleActionCoordinate() ?: return
        viewModelScope.launch {
            val preview = headquartersService.setPrimary(state.value, coordinate)
            if (!preview.ok) { _statusMessage.value = preview.message; return@launch }
            val result = session.commit("set-primary-headquarters") { current -> headquartersService.setPrimary(current, coordinate).state }
            refreshDerived(result.state)
            _statusMessage.value = if (result.persistence is PersistenceSaveResult.Success) preview.message else "Primary changed, but the native save could not be written."
        }
    }

    fun departureGate(): HeadquartersDepartureGate = headquartersService.departureGate(state.value)
    fun advanceDay() { viewModelScope.launch { advanceSimulationDay(fromClock = false) } }
    fun setSimulationSpeed(speed: Int) {
        if (_currentCorporateEvent.value != null && speed > 0) { _statusMessage.value = "Resolve the corporate event before resuming simulation."; return }
        simulationClock.setSpeed(speed); _statusMessage.value = if (speed == 0) "Simulation paused." else "Simulation running at ${speed}×."
    }

    fun openCommercialPanel(panel: CommercialPanel) {
        simulationClock.setSpeed(0)
        if (panel == CommercialPanel.BUYERS && state.value.company.buyers.offers.isEmpty()) {
            viewModelScope.launch {
                val result = session.commit("initialise-buyer-market") { buyerService.ensureMarket(it) }
                refreshDerived(result.state); _commercialPanel.value = panel
            }
        } else _commercialPanel.value = panel
    }
    fun closeCommercialPanel() { _commercialPanel.value = null }

    fun contractScore(): ContractScore? = contractService.score(state.value, _metrics.value.foodProduction, _metrics.value.industry)
    fun populationSupport(): PopulationSupportCapacity = populationSupportService.capacity(state.value, _network.value)
    fun tradeDaysUntilArrival(): Int = tradeService.daysUntilArrival(state.value)
    fun tradeCargoCapacity(): Double = tradeService.cargoCapacity(state.value)
    fun tradeCargoRemaining(): Double = tradeService.cargoRemaining(state.value)
    fun tradeExportCapacity(): Double = tradeService.exportCapacity(state.value)
    fun tradeExportRemaining(): Double = tradeService.exportRemaining(state.value)
    fun tradePassengerRemaining(): Int = tradeService.passengerRemaining(state.value)
    fun tradeSellableAmount(resourceId: ResourceId): Double = tradeService.sellableAmount(state.value, resourceId)
    fun tradeSellQuote(resourceId: ResourceId, amount: Double): TradeQuote = tradeService.quoteSell(state.value, resourceId, amount)
    fun tradeBuyPrice(resourceId: ResourceId): Double = tradeService.buyPrice(resourceId)
    fun tradeColonistProjection(): ColonistTransferProjection {
        val support = populationSupport()
        val surplus = max(0.0, _metrics.value.foodProduction - _metrics.value.foodDemand)
        return tradeService.colonistProjection(state.value, support.supportedPopulationCapacity, surplus)
    }
    fun buyerOffers(): List<BuyerOffer> = buyerService.availableOffers(state.value).take(25)
    fun buyerContracts(): List<BuyerContract> = buyerService.activeContracts(state.value)
    fun buyerProjection(contractId: String): BuyerCollectionProjection? = buyerService.projection(state.value, contractId)

    fun setTradeReserve(amount: Double) = commitCommercial("set-trade-reserve") { tradeService.setColonyTradeReserve(it, amount) }
    fun sellResource(resourceId: ResourceId, amount: Double) = commitCommercialAction("corporate-sell") { tradeService.sell(it, resourceId, amount, _spaceport.value.tradeAllowed) }
    fun sellTradeCategory(category: ResourceCategory) = commitCommercialAction("corporate-sell-category") { tradeService.sellCategory(it, category, _spaceport.value.tradeAllowed) }
    fun sellAllTrade() = commitCommercialAction("corporate-sell-all") { tradeService.sellAll(it, _spaceport.value.tradeAllowed) }
    fun buyResource(resourceId: ResourceId, amount: Double) = commitCommercialAction("corporate-buy") { tradeService.buy(it, resourceId, amount, _spaceport.value.tradeAllowed) }
    fun departCorporateShip() = commitCommercialAction("corporate-depart") { tradeService.depart(it) }

    fun transferColonists(amount: Int) {
        val support = populationSupport()
        val surplus = max(0.0, _metrics.value.foodProduction - _metrics.value.foodDemand)
        commitCommercialAction("transfer-colonists") {
            tradeService.transferColonists(it, amount, _spaceport.value.transfersAllowed, support.supportedPopulationCapacity, surplus)
        }
    }

    fun acceptBuyerOffer(offerId: String) = commitBuyerAction("accept-buyer") { buyerService.enterContract(it, offerId, _network.value.continuity.networkAvailable) }
    fun transferBuyerShipment(contractId: String) = commitBuyerAction("buyer-transfer") { buyerService.transfer(it, contractId) }
    fun continueBuyerWaiting(contractId: String) = commitBuyerAction("buyer-wait") { buyerService.continueWaiting(it, contractId) }
    fun resolveBuyerMiss(contractId: String) = commitBuyerAction("buyer-miss") { buyerService.resolveMiss(it, contractId) }
    fun cancelBuyerContract(contractId: String) = commitBuyerAction("buyer-cancel") { buyerService.cancelContract(it, contractId) }

    fun resolveCurrentEventPrimary() {
        val event = _currentCorporateEvent.value ?: return
        when (event.type) {
            CorporateEventType.SHIP -> clearCorporateEvent(event, "Corporate ship available for trade.")
            CorporateEventType.BUYER -> event.contractId?.let(::transferBuyerShipment)
            CorporateEventType.CONTRACT -> resolveContractDecision(event)
            CorporateEventType.EMERGENCY_FOOD -> clearCorporateEvent(event, "Emergency event acknowledged.")
        }
    }

    fun resolveCurrentEventSecondary() {
        val event = _currentCorporateEvent.value ?: return
        when (event.type) {
            CorporateEventType.BUYER -> event.contractId?.let { contractId ->
                val projection = buyerService.projection(state.value, contractId)
                if ((projection?.daysLate ?: 0) >= 15) resolveBuyerMiss(contractId) else continueBuyerWaiting(contractId)
            }
            CorporateEventType.CONTRACT -> {
                if (event.kind == "renewal-ended") renewContract() else if (event.kind == "complete") clearCorporateEvent(event, "Contract completion recorded; choose renewal later from Contract.") else failCurrentContract("Contract goals were not met at the final deadline.")
            }
            else -> clearCorporateEvent(event, "Event dismissed.")
        }
    }

    fun renewContract() = commitContractAction("renew-contract") { contractService.renew(it) }
    fun endContractAsLiability() = commitContractAction("end-contract-liability") { contractService.endAsLiability(it) }

    private fun resolveContractDecision(event: CorporateEvent) {
        when (event.kind) {
            "complete" -> commitContractAction("contract-complete") { contractService.awardCompletion(it) }
            "extension" -> commitContractAction("contract-extension") { contractService.extend(it) }
            "corporation-failed" -> failCurrentContract("Corporation cannot fund the required contract extension.")
            "failed" -> failCurrentContract("Contract goals were not met after all extensions.")
            "renewal-ended" -> endContractAsLiability()
            else -> clearCorporateEvent(event, "Contract event acknowledged.")
        }
    }

    private fun failCurrentContract(reason: String) = commitContractAction("contract-failed") { contractService.failCorporation(it, reason) }

    private fun clearCorporateEvent(event: CorporateEvent, message: String) {
        commitCommercial("resolve-corporate-event") { corporateEventService.remove(it, event) }
        _statusMessage.value = message
    }

    private fun commitCommercial(reason: String, transition: (GameState) -> GameState) {
        viewModelScope.launch {
            val result = session.commit(reason, transition)
            refreshDerived(result.state)
            if (result.persistence is PersistenceSaveResult.Failure) _statusMessage.value = "Action applied, but the native save could not be written."
        }
    }

    private fun commitCommercialAction(reason: String, action: (GameState) -> com.mineit.android.domain.trade.TradeActionResult) {
        viewModelScope.launch {
            var message = ""
            var ok = false
            val result = session.commit(reason) { current -> action(current).also { message = it.message; ok = it.ok }.state }
            refreshDerived(result.state)
            _statusMessage.value = if (result.persistence is PersistenceSaveResult.Failure && ok) "$message Save write failed." else message
        }
    }

    private fun commitBuyerAction(reason: String, action: (GameState) -> com.mineit.android.domain.buyers.BuyerActionResult) {
        viewModelScope.launch {
            var message = ""
            var ok = false
            val eventBefore = _currentCorporateEvent.value
            val result = session.commit(reason) { current ->
                val actionResult = action(current)
                message = actionResult.message; ok = actionResult.ok
                if (ok && eventBefore?.type == CorporateEventType.BUYER && eventBefore.contractId != null) corporateEventService.remove(actionResult.state, eventBefore) else actionResult.state
            }
            refreshDerived(result.state); _statusMessage.value = if (result.persistence is PersistenceSaveResult.Failure && ok) "$message Save write failed." else message
        }
    }

    private fun commitContractAction(reason: String, action: (GameState) -> com.mineit.android.domain.contracts.ContractActionResult) {
        viewModelScope.launch {
            var message = ""
            var ok = false
            val eventBefore = _currentCorporateEvent.value
            val result = session.commit(reason) { current ->
                val actionResult = action(current)
                message = actionResult.message; ok = actionResult.ok
                if (ok && eventBefore?.type == CorporateEventType.CONTRACT) corporateEventService.remove(actionResult.state, eventBefore) else actionResult.state
            }
            refreshDerived(result.state); _statusMessage.value = if (result.persistence is PersistenceSaveResult.Failure && ok) "$message Save write failed." else message
            if (result.state.company.gameOver) {
                simulationClock.setSpeed(0); _canContinue.value = false; _screen.value = GameScreen.MAIN_MENU
            }
        }
    }

    private fun singleActionCoordinate(): SectorCoordinate? = _selectedSectors.value.singleOrNull()
    private fun commitDevelopment(reason: String, action: (GameState, SectorCoordinate) -> DevelopmentActionResult) {
        val coordinate = singleActionCoordinate() ?: return
        val preview = action(state.value, coordinate)
        if (!preview.ok) { _statusMessage.value = preview.message; return }
        viewModelScope.launch {
            var applied: DevelopmentActionResult? = null
            val result = session.commit(reason) { current -> action(current, coordinate).also { applied = it }.state }
            refreshDerived(result.state)
            val message = requireNotNull(applied).message
            _statusMessage.value = if (result.persistence is PersistenceSaveResult.Success) message else "$message Save write failed."
        }
    }

    private suspend fun advanceSimulationDay(fromClock: Boolean) {
        if (!state.value.activeColony.world.settled || _currentCorporateEvent.value != null) return
        var dayResult: com.mineit.android.domain.commercial.CommercialDayResult? = null
        val commit = session.commit(if (fromClock) "clock-day" else "advance-day") { current ->
            val currentNetwork = networkService.calculate(current)
            val services = spaceportService.status(current, currentNetwork)
            commercialDayService.advanceDay(current, spaceportServicesAvailable = services.operational).also { dayResult = it }.state
        }
        val result = requireNotNull(dayResult)
        refreshDerived(commit.state, completedDayMetrics = result.metrics)
        if (result.simulation.colonyDied) {
            simulationClock.setSpeed(0); clearSelection(); _canContinue.value = false; _screen.value = GameScreen.MAIN_MENU
            _statusMessage.value = if (commit.persistence is PersistenceSaveResult.Success) "Colony lost. Start a new game to begin again." else "Colony lost. The final save write failed; start a new game to begin again."
            return
        }
        if (result.shouldPause) simulationClock.setSpeed(0)
        if (commit.persistence is PersistenceSaveResult.Failure) { _statusMessage.value = "Day advanced, but the native save could not be written."; return }
        _statusMessage.value = when {
            result.shouldPause -> "Corporate event requires attention."
            result.simulation.deaths > .0001 -> "Life-support shortage caused ${formatPopulation(result.simulation.deaths)} deaths this day."
            result.simulation.completedSurveys.isNotEmpty() -> "Survey completed for ${result.simulation.completedSurveys.joinToString { "${it.x},${it.y}" }}."
            !fromClock -> "Advanced one complete MineIT simulation day."
            else -> null
        }
    }

    private fun refreshDerived(gameState: GameState, completedDayMetrics: ColonyMetrics? = null) {
        val nextNetwork = networkService.calculate(gameState)
        _network.value = nextNetwork
        _spaceport.value = spaceportService.status(gameState, nextNetwork)
        _metrics.value = completedDayMetrics ?: dailySimulationEngine.recalculate(gameState)
        _currentCorporateEvent.value = gameState.corporateEvents.pending.firstOrNull()
    }

    private fun resetMapUi() { _selectedSector.value = null; _selectedSectors.value = emptySet(); _mapFocus.value = MapFocus.ALL; _mapFilters.value = emptySet() }
    private fun formatPopulation(value: Double): String = if (value % 1.0 == 0.0) value.toLong().toString() else "%.2f".format(value)
    override fun onCleared() { simulationClock.stop(); super.onCleared() }
}