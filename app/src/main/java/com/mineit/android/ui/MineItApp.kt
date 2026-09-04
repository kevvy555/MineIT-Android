package com.mineit.android.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mineit.android.domain.events.CorporateEventType
import com.mineit.android.domain.world.DevelopmentKind
import com.mineit.android.ui.commercial.CommercialPanelScreen
import com.mineit.android.ui.commercial.ContractCommercialPanelScreen
import com.mineit.android.ui.commercial.CorporateEventDialog
import com.mineit.android.ui.commercial.TradeCommercialPanelScreen
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
    val commercialPanel by viewModel.commercialPanel.collectAsStateWithLifecycle()
    val corporateEvent by viewModel.currentCorporateEvent.collectAsStateWithLifecycle()

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
                    onOpenCommercial = { viewModel.openCommercialPanel(CommercialPanel.TRADE) },
                    onMainMenu = viewModel::returnToMainMenu,
                )

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
                                metrics = metrics,
                                spaceport = spaceport,
                                contractScore = viewModel.contractScore(),
                                populationSupport = viewModel.populationSupport(),
                                tradeDaysUntilArrival = viewModel.tradeDaysUntilArrival(),
                                buyerOffers = if (panel == CommercialPanel.BUYERS) viewModel.buyerOffers() else emptyList(),
                                buyerContracts = contracts,
                                buyerProjections = contracts.mapNotNull { contract -> viewModel.buyerProjection(contract.id)?.let { contract.id to it } }.toMap(),
                                onSelectPanel = viewModel::openCommercialPanel,
                                onClose = viewModel::closeCommercialPanel,
                                onSetReserve = viewModel::setTradeReserve,
                                onSellResource = viewModel::sellResource,
                                onBuyResource = viewModel::buyResource,
                                onTransferColonists = viewModel::transferColonists,
                                onDepartCorporateShip = viewModel::departCorporateShip,
                                onAcceptBuyerOffer = viewModel::acceptBuyerOffer,
                                onTransferBuyer = viewModel::transferBuyerShipment,
                                onWaitBuyer = viewModel::continueBuyerWaiting,
                                onMissBuyer = viewModel::resolveBuyerMiss,
                                onCancelBuyer = viewModel::cancelBuyerContract,
                                onRenewContract = viewModel::renewContract,
                                onEndLiability = viewModel::endContractAsLiability,
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
            }
        }
    }
}
