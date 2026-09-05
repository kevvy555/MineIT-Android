package com.mineit.android.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mineit.android.app.ColonyAttentionTarget
import com.mineit.android.domain.events.CorporateEventType
import com.mineit.android.domain.model.ColonyStatus
import com.mineit.android.domain.technology.ScanningTechnology
import com.mineit.android.domain.world.DevelopmentKind
import com.mineit.android.domain.world.SectorCoordinate
import com.mineit.android.ui.commercial.CommercialPanelScreen
import com.mineit.android.ui.commercial.ContractCommercialPanelScreen
import com.mineit.android.ui.commercial.CorporateEventDialog
import com.mineit.android.ui.commercial.TradeCommercialPanelScreen
import com.mineit.android.ui.design.MineItSecondaryButton
import com.mineit.android.ui.game.ColonyEstablishmentDialog
import com.mineit.android.ui.game.CriticalResourceWarningDialog
import com.mineit.android.ui.game.DevelopmentDetailDialog
import com.mineit.android.ui.map.MapFocus
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
    val attention by viewModel.attention.collectAsStateWithLifecycle()
    val criticalResourceAlert by viewModel.criticalResourceAlert.collectAsStateWithLifecycle()
    val simulationSpeed by viewModel.simulationSpeed.collectAsStateWithLifecycle()
    val establishmentPrompt by viewModel.establishmentPrompt.collectAsStateWithLifecycle()
    val selectedCoordinates by viewModel.selectedSectors.collectAsStateWithLifecycle()
    val mapFocus by viewModel.mapFocus.collectAsStateWithLifecycle()
    val mapFilters by viewModel.mapFilters.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()
    val commercialPanel by viewModel.commercialPanel.collectAsStateWithLifecycle()
    val corporateEvent by viewModel.currentCorporateEvent.collectAsStateWithLifecycle()
    var showEstablishment by remember { mutableStateOf(false) }
    var developmentDetailCoordinate by remember { mutableStateOf<SectorCoordinate?>(null) }
    val establishment = viewModel.establishmentAssessment()
    val scanning = ScanningTechnology.forLevel(state.activeColony.technology.scanning)
    val surveyableCoordinates = state.activeColony.world.tiles
        .map { it.coordinate }
        .filterTo(linkedSetOf()) { viewModel.surveyDays(it) != null }
    val developmentDetailTile = developmentDetailCoordinate?.let(state.activeColony.world::tileAt)
    val developmentDetail = developmentDetailCoordinate?.let(viewModel::developmentDetail)

    LaunchedEffect(establishmentPrompt) {
        if (establishmentPrompt > 0L) showEstablishment = true
    }
    LaunchedEffect(state.activeColony.id, state.activeColony.status, establishment.acknowledged) {
        developmentDetailCoordinate = null
        if (state.activeColony.status != ColonyStatus.SITE_SELECTION && establishment.required && !establishment.acknowledged) {
            showEstablishment = true
        }
    }
    LaunchedEffect(developmentDetailCoordinate, developmentDetail) {
        if (developmentDetailCoordinate != null && developmentDetail == null) developmentDetailCoordinate = null
    }

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

            Box(Modifier.fillMaxSize()) {
                MineItScreen(
                    state = state,
                    metrics = metrics,
                    network = network,
                    spaceport = spaceport,
                    attention = attention,
                    simulationSpeed = simulationSpeed,
                    selectedTiles = selectedTiles,
                    selectedSurveyDays = singleCoordinate?.let(viewModel::surveyDays),
                    surveyableSelectedCount = viewModel.surveyableSelectedCount(),
                    surveyableCoordinates = surveyableCoordinates,
                    scanningLevel = scanning.level,
                    surveySlots = scanning.surveySlots,
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
                    onSelectSector = { coordinate ->
                        if (coordinate in surveyableCoordinates) {
                            developmentDetailCoordinate = null
                            viewModel.selectSector(coordinate)
                            viewModel.surveySelectedSector()
                        } else {
                            viewModel.selectSector(coordinate)
                            val kind = state.activeColony.world.tileAt(coordinate)?.development?.kind
                            developmentDetailCoordinate = if (kind != null && kind != DevelopmentKind.HEADQUARTERS) coordinate else null
                        }
                    },
                    onBeginMultiSelect = { coordinate ->
                        developmentDetailCoordinate = null
                        viewModel.beginMultiSelect(coordinate)
                    },
                    onAddMultiSelect = viewModel::addMultiSelect,
                    onEndMultiSelect = {
                        viewModel.surveySelectedSectors()
                        viewModel.clearSelection()
                    },
                    onSurveySelectedSector = viewModel::surveySelectedSector,
                    onSurveySelectedSectors = {
                        viewModel.surveySelectedSectors()
                        viewModel.clearSelection()
                    },
                    onClearSelection = {
                        developmentDetailCoordinate = null
                        viewModel.clearSelection()
                    },
                    onSetMapFocus = viewModel::setMapFocus,
                    onToggleMapFilter = viewModel::toggleMapFilter,
                    onClearMapFilters = viewModel::clearMapFilters,
                    onBuild = viewModel::buildSelected,
                    onDevelopExtraction = viewModel::developSelectedResource,
                    onUpgrade = viewModel::upgradeSelected,
                    onDemolish = viewModel::demolishSelected,
                    onSetPrimaryHeadquarters = viewModel::setSelectedAsPrimaryHeadquarters,
                    onPreviewShipResidentsAshore = viewModel::shipResidentTransferPreview,
                    onMoveShipResidentsAshore = viewModel::moveShipResidentsAshore,
                    onMoveShipResidentsAboard = viewModel::moveShipResidentsAboard,
                    onUnloadShipResource = viewModel::transferShipToColony,
                    onLoadShipResource = viewModel::transferColonyToShip,
                    onAdvanceDay = viewModel::advanceDay,
                    onSetSimulationSpeed = viewModel::setSimulationSpeed,
                    onOpenCommercial = { viewModel.openCommercialPanel(CommercialPanel.TRADE) },
                    onOpenAttention = {
                        developmentDetailCoordinate = null
                        when (attention.target) {
                            ColonyAttentionTarget.LANDING_SITE -> Unit
                            ColonyAttentionTarget.CORPORATE_SHIP -> viewModel.openCommercialPanel(CommercialPanel.TRADE)
                            ColonyAttentionTarget.PLAYER_SHIP -> {
                                showEstablishment = false
                                val hasDockedPlayerShip = state.fleet.ships.any { it.dockedColonyId == state.activeColony.id }
                                if (hasDockedPlayerShip) viewModel.selectSector(SectorCoordinate(0, 0)) else showEstablishment = true
                            }
                            ColonyAttentionTarget.ESTABLISHMENT -> showEstablishment = true
                            ColonyAttentionTarget.FOOD -> viewModel.setMapFocus(MapFocus.FOOD)
                            ColonyAttentionTarget.POWER -> viewModel.setMapFocus(MapFocus.POWER)
                            ColonyAttentionTarget.FUEL -> viewModel.setMapFocus(MapFocus.FUEL)
                            ColonyAttentionTarget.INDUSTRY -> viewModel.setMapFocus(MapFocus.INDUSTRY)
                            ColonyAttentionTarget.HOUSING -> viewModel.setMapFocus(MapFocus.HOUSING)
                            ColonyAttentionTarget.ORE -> viewModel.setMapFocus(MapFocus.ORE)
                            ColonyAttentionTarget.PROBLEMS -> viewModel.setMapFocus(MapFocus.PROBLEMS)
                            ColonyAttentionTarget.COLONY -> Unit
                        }
                    },
                    onMainMenu = viewModel::returnToMainMenu,
                )

                if (developmentDetail != null && developmentDetailTile != null) {
                    DevelopmentDetailDialog(
                        tile = developmentDetailTile,
                        detail = developmentDetail,
                        statusMessage = statusMessage,
                        onUpgrade = viewModel::upgradeSelected,
                        onAdjustHarvest = viewModel::adjustSelectedHarvestIntensity,
                        onSetOperatingMode = viewModel::setSelectedExtractionOperatingMode,
                        onDemolish = {
                            viewModel.demolishSelected()
                            developmentDetailCoordinate = null
                        },
                        onDismiss = { developmentDetailCoordinate = null },
                    )
                }

                if (establishment.required && state.activeColony.status != ColonyStatus.SITE_SELECTION && !showEstablishment) {
                    MineItSecondaryButton(
                        text = "HANDOVER • ${establishment.phase.name}",
                        onClick = {
                            developmentDetailCoordinate = null
                            showEstablishment = true
                        },
                        selected = !establishment.acknowledged,
                        modifier = Modifier.align(Alignment.BottomEnd).padding(end = 10.dp, bottom = 62.dp),
                    )
                }

                commercialPanel?.let { panel ->
                    when (panel) {
                        CommercialPanel.TRADE -> TradeCommercialPanelScreen(
                            state = state,
                            spaceport = spaceport,
                            daysUntilArrival = viewModel.tradeDaysUntilArrival(),
                            cargoCapacity = viewModel.tradeCargoCapacity(),
                            cargoRemaining = viewModel.tradeCargoRemaining(),
                            exportCapacity = viewModel.tradeExportCapacity(),
                            exportRemaining = viewModel.tradeExportRemaining(),
                            passengerRemaining = viewModel.tradePassengerRemaining(),
                            colonistProjection = viewModel.tradeColonistProjection(),
                            sellableAmount = viewModel::tradeSellableAmount,
                            sellQuote = viewModel::tradeSellQuote,
                            buyPrice = viewModel::tradeBuyPrice,
                            onSelectPanel = viewModel::openCommercialPanel,
                            onClose = viewModel::closeCommercialPanel,
                            onSetReserve = viewModel::setTradeReserve,
                            onSellResource = viewModel::sellResource,
                            onSellCategory = viewModel::sellTradeCategory,
                            onSellAll = viewModel::sellAllTrade,
                            onBuyResource = viewModel::buyResource,
                            onTransferColonists = viewModel::transferColonists,
                            onDepartCorporateShip = viewModel::departCorporateShip,
                        )

                        CommercialPanel.CONTRACT -> ContractCommercialPanelScreen(
                            state = state,
                            metrics = metrics,
                            score = viewModel.contractScore(),
                            onSelectPanel = viewModel::openCommercialPanel,
                            onClose = viewModel::closeCommercialPanel,
                            onRenewContract = viewModel::renewContract,
                            onEndLiability = viewModel::endContractAsLiability,
                        )

                        CommercialPanel.BUYERS, CommercialPanel.LOG -> {
                            val contracts = viewModel.buyerContracts()
                            CommercialPanelScreen(
                                panel = panel,
                                state = state,
                                network = network,
                                buyerOffers = if (panel == CommercialPanel.BUYERS) state.company.buyers.offers.take(25) else emptyList(),
                                buyerContracts = contracts,
                                buyerProjections = contracts.mapNotNull { contract -> viewModel.buyerProjection(contract.id)?.let { contract.id to it } }.toMap(),
                                onSelectPanel = viewModel::openCommercialPanel,
                                onClose = viewModel::closeCommercialPanel,
                                onAcceptBuyerOffer = viewModel::acceptBuyerOffer,
                                onTransferBuyer = viewModel::transferBuyerShipment,
                                onWaitBuyer = viewModel::continueBuyerWaiting,
                                onMissBuyer = viewModel::resolveBuyerMiss,
                                onCancelBuyer = viewModel::cancelBuyerContract,
                            )
                        }
                    }
                }

                corporateEvent?.let { event ->
                    CorporateEventDialog(
                        event = event,
                        onPrimary = {
                            viewModel.resolveCurrentEventPrimary()
                            if (event.type == CorporateEventType.SHIP) viewModel.openCommercialPanel(CommercialPanel.TRADE)
                        },
                        onSecondary = viewModel::resolveCurrentEventSecondary,
                    )
                }

                if (showEstablishment && establishment.required && state.activeColony.status != ColonyStatus.SITE_SELECTION) {
                    ColonyEstablishmentDialog(
                        colonyName = state.activeColony.name,
                        assessment = establishment,
                        statusMessage = statusMessage,
                        transferPreview = viewModel::establishmentResidentTransferPreview,
                        onUnloadCategory = viewModel::unloadFoundingCategory,
                        onMoveResidentsAshore = viewModel::moveFoundingResidentsAshore,
                        onBeginOperations = {
                            viewModel.beginEstablishmentOperations()
                            showEstablishment = false
                        },
                        onDismiss = { showEstablishment = false },
                    )
                }

                criticalResourceAlert?.let { alert ->
                    CriticalResourceWarningDialog(
                        alert = alert,
                        onDismiss = viewModel::dismissCriticalResourceAlert,
                    )
                }
            }
        }
    }
}
