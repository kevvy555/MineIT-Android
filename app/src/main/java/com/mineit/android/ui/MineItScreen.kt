package com.mineit.android.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mineit.android.app.ColonyAttention
import com.mineit.android.app.ColonyAttentionTarget
import com.mineit.android.domain.colony.ColonyNetworkSnapshot
import com.mineit.android.domain.colony.DevelopmentPreview
import com.mineit.android.domain.colony.HeadquartersDepartureGate
import com.mineit.android.domain.colony.SpaceportStatus
import com.mineit.android.domain.model.ColonyStatus
import com.mineit.android.domain.model.GameState
import com.mineit.android.domain.model.ResourceId
import com.mineit.android.domain.model.ShipId
import com.mineit.android.domain.ships.FleetActionResult
import com.mineit.android.domain.simulation.ColonyMetrics
import com.mineit.android.domain.world.DevelopmentKind
import com.mineit.android.domain.world.LandingSiteCandidate
import com.mineit.android.domain.world.SectorCoordinate
import com.mineit.android.domain.world.TerrainType
import com.mineit.android.domain.world.WorldTile
import com.mineit.android.ui.design.MineItPalette
import com.mineit.android.ui.design.MineItPanel
import com.mineit.android.ui.design.MineItSecondaryButton
import com.mineit.android.ui.design.MineItSpacing
import com.mineit.android.ui.game.ColonyAttentionStrip
import com.mineit.android.ui.game.ColonyDetailSheet
import com.mineit.android.ui.game.GameHeader
import com.mineit.android.ui.game.HeadquartersControlReadiness
import com.mineit.android.ui.game.HeadquartersControlSheet
import com.mineit.android.ui.game.PlayerShipControlSheet
import com.mineit.android.ui.game.SectorActionBar
import com.mineit.android.ui.game.SectorContextBar
import com.mineit.android.ui.map.ColonyMap
import com.mineit.android.ui.map.MapFocus
import com.mineit.android.ui.map.MapStateFilter

private val GameplayRailHeight = 32.dp
private val GameplayFooterHeight = 34.dp

@Composable
fun MineItScreen(
    state: GameState,
    metrics: ColonyMetrics,
    network: ColonyNetworkSnapshot,
    spaceport: SpaceportStatus,
    attention: ColonyAttention,
    simulationSpeed: Int,
    selectedTiles: List<WorldTile>,
    selectedSurveyDays: Int?,
    surveyableSelectedCount: Int,
    surveyableCoordinates: Set<SectorCoordinate>,
    scanningLevel: Int,
    surveySlots: Int,
    statusMessage: String?,
    mapFocus: MapFocus,
    mapFilters: Set<MapStateFilter>,
    powerPreview: DevelopmentPreview?,
    housingPreview: DevelopmentPreview?,
    industryPreview: DevelopmentPreview?,
    headquartersPreview: DevelopmentPreview?,
    extractionPreview: DevelopmentPreview?,
    upgradePreview: DevelopmentPreview?,
    departureGate: HeadquartersDepartureGate,
    handoverAvailable: Boolean,
    onSelectLandingSite: (Int) -> Unit,
    onSelectSector: (SectorCoordinate) -> Unit,
    onBeginMultiSelect: (SectorCoordinate) -> Unit,
    onAddMultiSelect: (SectorCoordinate) -> Unit,
    onEndMultiSelect: () -> Unit,
    onSurveySelectedSector: () -> Unit,
    onSurveySelectedSectors: () -> Unit,
    onClearSelection: () -> Unit,
    onSetMapFocus: (MapFocus) -> Unit,
    onToggleMapFilter: (MapStateFilter) -> Unit,
    onClearMapFilters: () -> Unit,
    onBuild: (DevelopmentKind) -> Unit,
    onDevelopExtraction: () -> Unit,
    onUpgrade: () -> Unit,
    onDemolish: () -> Unit,
    onSetPrimaryHeadquarters: () -> Unit,
    onPreviewShipResidentsAshore: (ShipId, Double) -> FleetActionResult,
    onMoveShipResidentsAshore: (ShipId, Double, Boolean) -> Unit,
    onMoveShipResidentsAboard: (ShipId, Double) -> Unit,
    onUnloadShipResource: (ShipId, ResourceId, Double) -> Unit,
    onLoadShipResource: (ShipId, ResourceId, Double) -> Unit,
    onAdvanceDay: () -> Unit,
    onSetSimulationSpeed: (Int) -> Unit,
    onOpenCommercial: () -> Unit,
    onOpenHandover: () -> Unit,
    onOpenAttention: () -> Unit,
    onMainMenu: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showColonyDetail by remember { mutableStateOf(false) }
    val colony = state.activeColony
    val selectedTile = selectedTiles.singleOrNull()
    val selectedHeadquarters = selectedTile?.takeIf {
        it.development?.kind == DevelopmentKind.HEADQUARTERS && HeadquartersControlReadiness.isReady(it, network)
    }
    val primaryHeadquarters = colony.headquarters.primary
        ?.let(colony.world::tileAt)
        ?.takeIf { it.development?.kind == DevelopmentKind.HEADQUARTERS && HeadquartersControlReadiness.isReady(it, network) }
    val headquartersSheetTile = selectedHeadquarters ?: if (showColonyDetail) primaryHeadquarters else null
    val selectedPlayerShip = selectedTile
        ?.takeIf { it.coordinate.x == 0 && it.coordinate.y == 0 }
        ?.let {
            val docked = state.fleet.ships.filter { ship -> ship.dockedColonyId == colony.id }
            state.fleet.selectedShipId
                ?.let { selectedId -> docked.firstOrNull { ship -> ship.id == selectedId } }
                ?: docked.firstOrNull()
        }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MineItPalette.Background,
    ) { scaffoldPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(scaffoldPadding)
                .padding(horizontal = 2.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            GameHeader(
                state = state,
                metrics = metrics,
                network = network,
                notificationContent = {
                    ColonyAttentionStrip(
                        attention = attention,
                        onClick = {
                            if (attention.target == ColonyAttentionTarget.COLONY) showColonyDetail = true
                            else onOpenAttention()
                        },
                    )
                },
            )

            if (colony.status == ColonyStatus.SITE_SELECTION) {
                statusMessage?.let { StatusStrip(it) }
                LandingSiteSelection(
                    candidates = colony.world.landingCandidates,
                    onSelect = onSelectLandingSite,
                    modifier = Modifier.weight(1f),
                )
                FooterControls(
                    speed = simulationSpeed,
                    enabled = false,
                    commercialActive = false,
                    showCommercial = false,
                    handoverAvailable = false,
                    onSetSpeed = onSetSimulationSpeed,
                    onAdvanceDay = onAdvanceDay,
                    onOpenCommercial = onOpenCommercial,
                    onOpenHandover = onOpenHandover,
                    onMainMenu = onMainMenu,
                    modifier = Modifier.height(GameplayFooterHeight),
                )
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(1.dp),
                ) {
                    MapToolbar(
                        focus = mapFocus,
                        filters = mapFilters,
                        onFocus = onSetMapFocus,
                        onToggleFilter = onToggleMapFilter,
                        onReset = onClearMapFilters,
                        modifier = Modifier.height(GameplayRailHeight),
                    )

                    Box(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        ColonyMap(
                            tiles = colony.world.tiles,
                            activeSurveys = colony.world.activeSurveys,
                            queued = colony.world.surveyQueue.toSet(),
                            selected = selectedTiles.mapTo(linkedSetOf()) { it.coordinate },
                            surveyable = surveyableCoordinates,
                            scanningLevel = scanningLevel,
                            surveySlots = surveySlots,
                            focus = mapFocus,
                            stateFilters = mapFilters,
                            network = network,
                            onTap = onSelectSector,
                            onBeginMultiSelect = onBeginMultiSelect,
                            onAddMultiSelect = onAddMultiSelect,
                            onEndMultiSelect = onEndMultiSelect,
                            modifier = Modifier.fillMaxSize(),
                        )
                        statusMessage?.let {
                            StatusStrip(
                                message = it,
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(horizontal = 18.dp, vertical = 2.dp),
                            )
                        }
                    }

                    SectorContextBar(
                        selectedTiles = selectedTiles,
                        surveyDays = selectedSurveyDays,
                        surveyableSelectedCount = surveyableSelectedCount,
                        powerPreview = powerPreview,
                        housingPreview = housingPreview,
                        industryPreview = industryPreview,
                        headquartersPreview = headquartersPreview,
                        extractionPreview = extractionPreview,
                        upgradePreview = upgradePreview,
                    )

                    if (selectedTiles.isEmpty()) {
                        FooterControls(
                            speed = simulationSpeed,
                            enabled = colony.status != ColonyStatus.DEAD,
                            commercialActive = colony.trade.active,
                            showCommercial = true,
                            handoverAvailable = handoverAvailable,
                            onSetSpeed = onSetSimulationSpeed,
                            onAdvanceDay = onAdvanceDay,
                            onOpenCommercial = onOpenCommercial,
                            onOpenHandover = onOpenHandover,
                            onMainMenu = onMainMenu,
                            modifier = Modifier.height(GameplayFooterHeight),
                        )
                    } else {
                        SectorActionBar(
                            selectedTiles = selectedTiles,
                            surveyDays = selectedSurveyDays,
                            surveyableSelectedCount = surveyableSelectedCount,
                            primaryHeadquarters = colony.headquarters.primary,
                            powerPreview = powerPreview,
                            housingPreview = housingPreview,
                            industryPreview = industryPreview,
                            headquartersPreview = headquartersPreview,
                            extractionPreview = extractionPreview,
                            upgradePreview = upgradePreview,
                            onSurveyOne = onSurveySelectedSector,
                            onSurveyMany = onSurveySelectedSectors,
                            onBuild = onBuild,
                            onDevelop = onDevelopExtraction,
                            onUpgrade = onUpgrade,
                            onDemolish = onDemolish,
                            onSetPrimary = onSetPrimaryHeadquarters,
                            onClearSelection = onClearSelection,
                            modifier = Modifier.height(GameplayFooterHeight),
                        )
                    }
                }
            }
        }
    }

    if (headquartersSheetTile != null) {
        HeadquartersControlSheet(
            state = state,
            metrics = metrics,
            network = network,
            spaceport = spaceport,
            departureGate = departureGate,
            tile = headquartersSheetTile,
            upgradePreview = upgradePreview,
            statusMessage = statusMessage,
            onSetPrimary = onSetPrimaryHeadquarters,
            onUpgrade = onUpgrade,
            onDemolish = {
                onDemolish()
                showColonyDetail = false
                onClearSelection()
            },
            onDismiss = {
                if (showColonyDetail) showColonyDetail = false else onClearSelection()
            },
        )
    } else if (showColonyDetail) {
        // Before a Primary HQ exists, retain the early-establishment fallback. Once the HQ exists,
        // Colony Control is represented by the HQ sheet rather than a separate duplicate surface.
        ColonyDetailSheet(
            state = state,
            metrics = metrics,
            network = network,
            spaceport = spaceport,
            departureGate = departureGate,
            onDismiss = { showColonyDetail = false },
        )
    } else if (selectedPlayerShip != null) {
        PlayerShipControlSheet(
            state = state,
            network = network,
            spaceport = spaceport,
            departureGate = departureGate,
            ship = selectedPlayerShip,
            statusMessage = statusMessage,
            onOpenColonyControl = { showColonyDetail = true },
            onPreviewResidentsAshore = { amount -> onPreviewShipResidentsAshore(selectedPlayerShip.id, amount) },
            onMoveResidentsAshore = { amount, confirmed -> onMoveShipResidentsAshore(selectedPlayerShip.id, amount, confirmed) },
            onMoveResidentsAboard = { amount -> onMoveShipResidentsAboard(selectedPlayerShip.id, amount) },
            onUnload = { resourceId, amount -> onUnloadShipResource(selectedPlayerShip.id, resourceId, amount) },
            onLoad = { resourceId, amount -> onLoadShipResource(selectedPlayerShip.id, resourceId, amount) },
            onDismiss = onClearSelection,
        )
    }
}

@Composable
private fun StatusStrip(message: String, modifier: Modifier = Modifier) {
    val critical = message.contains("lost", true) || message.contains("failed", true) || message.contains("death", true)
    val color = if (critical) MineItPalette.Critical else MineItPalette.Accent
    Surface(
        color = MineItPalette.Panel.copy(alpha = .94f),
        shape = RoundedCornerShape(5.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = .45f)),
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun MapToolbar(
    focus: MapFocus,
    filters: Set<MapStateFilter>,
    onFocus: (MapFocus) -> Unit,
    onToggleFilter: (MapStateFilter) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(1.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MapFocus.entries.forEach { option ->
            val accent = when (option) {
                MapFocus.FOOD -> MineItPalette.Food
                MapFocus.BUILD -> MineItPalette.Build
                MapFocus.FUEL -> MineItPalette.Fuel
                MapFocus.ORE -> MineItPalette.Ore
                MapFocus.PROBLEMS -> MineItPalette.Critical
                else -> MineItPalette.Accent
            }
            MineItSecondaryButton(
                text = option.label,
                onClick = { onFocus(option) },
                selected = focus == option,
                accent = accent,
                compact = true,
            )
        }
        MapStateFilter.entries.forEach { filter ->
            MineItSecondaryButton(
                text = filter.label,
                onClick = { onToggleFilter(filter) },
                selected = filter in filters,
                accent = MineItPalette.Survey,
                compact = true,
            )
        }
        MineItSecondaryButton(
            "RESET",
            onReset,
            enabled = focus != MapFocus.ALL || filters.isNotEmpty(),
            compact = true,
        )
    }
}

@Composable
private fun FooterControls(
    speed: Int,
    enabled: Boolean,
    commercialActive: Boolean,
    showCommercial: Boolean,
    handoverAvailable: Boolean,
    onSetSpeed: (Int) -> Unit,
    onAdvanceDay: () -> Unit,
    onOpenCommercial: () -> Unit,
    onOpenHandover: () -> Unit,
    onMainMenu: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(1.dp)) {
        MineItSecondaryButton(
            text = "PAUSE",
            onClick = { onSetSpeed(0) },
            enabled = enabled,
            selected = speed == 0,
            modifier = Modifier.weight(1.0f),
            compact = true,
        )
        listOf(1 to "1×", 2 to "2×", 4 to "4×").forEach { (value, label) ->
            MineItSecondaryButton(
                text = label,
                onClick = { onSetSpeed(value) },
                enabled = enabled,
                selected = speed == value,
                modifier = Modifier.weight(.72f),
                compact = true,
            )
        }
        MineItSecondaryButton(
            text = "+1D",
            onClick = onAdvanceDay,
            enabled = enabled && speed == 0,
            accent = MineItPalette.Warning,
            modifier = Modifier.weight(.88f),
            compact = true,
        )
        if (showCommercial) {
            MineItSecondaryButton(
                text = "COMM",
                onClick = onOpenCommercial,
                selected = commercialActive,
                accent = if (commercialActive) MineItPalette.Success else MineItPalette.Accent,
                modifier = Modifier.weight(.98f),
                compact = true,
            )
        }
        if (handoverAvailable) {
            MineItSecondaryButton(
                text = "HANDOVER",
                onClick = onOpenHandover,
                enabled = enabled,
                selected = true,
                accent = MineItPalette.Warning,
                modifier = Modifier.weight(1.28f),
                compact = true,
            )
        }
        MineItSecondaryButton(
            "MENU",
            onMainMenu,
            modifier = Modifier.weight(.9f),
            compact = true,
        )
    }
}

@Composable
private fun LandingSiteSelection(
    candidates: List<LandingSiteCandidate>,
    onSelect: (Int) -> Unit,
    modifier: Modifier,
) {
    MineItPanel(modifier = modifier.fillMaxWidth()) {
        Text("CHOOSE COLONY LANDING SITE", style = MaterialTheme.typography.titleMedium, color = MineItPalette.Accent)
        Text(
            "Only terrain is known before settlement. Resources remain hidden until each sector is surveyed.",
            style = MaterialTheme.typography.bodySmall,
            color = MineItPalette.Muted,
        )
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(MineItSpacing.Sm)) {
            candidates.chunked(2).forEach { pair ->
                Row(
                    Modifier.weight(1f).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MineItSpacing.Sm),
                ) {
                    pair.forEach { candidate ->
                        LandingCandidateCard(
                            candidate = candidate,
                            onClick = { onSelect(candidate.index) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (pair.size == 1) Box(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun LandingCandidateCard(
    candidate: LandingSiteCandidate,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxSize(),
        color = MineItPalette.Control,
        shape = RoundedCornerShape(7.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MineItPalette.Line),
    ) {
        Column(
            Modifier.fillMaxSize().padding(MineItSpacing.Sm),
            verticalArrangement = Arrangement.spacedBy(MineItSpacing.Xs),
        ) {
            Text("LOCATION ${candidate.index + 1}", style = MaterialTheme.typography.labelLarge, color = MineItPalette.Text)
            TerrainPreview(candidate, Modifier.weight(1f).fillMaxWidth())
            Text(
                "Plain ${candidate.counts[TerrainType.PLAIN] ?: 0} • Hill ${candidate.counts[TerrainType.HILL] ?: 0} • Mountain ${candidate.counts[TerrainType.MOUNTAIN] ?: 0} • Lake ${candidate.counts[TerrainType.LAKE] ?: 0}",
                style = MaterialTheme.typography.labelSmall,
                color = MineItPalette.Muted,
                maxLines = 2,
            )
        }
    }
}

@Composable
private fun TerrainPreview(candidate: LandingSiteCandidate, modifier: Modifier = Modifier) {
    Canvas(modifier.background(Color.Black, RoundedCornerShape(3.dp))) {
        val cellWidth = size.width / 8f
        val cellHeight = size.height / 8f
        candidate.cells.forEach { cell ->
            val column = cell.coordinate.x + 4
            val row = cell.coordinate.y + 4
            val base = when (cell.terrain) {
                TerrainType.PLAIN -> Color(0xFF334C32)
                TerrainType.HILL -> Color(0xFF5A5337)
                TerrainType.MOUNTAIN -> Color(0xFF565C62)
                TerrainType.LAKE -> Color(0xFF214F68)
            }
            drawRect(
                color = base,
                topLeft = Offset(column * cellWidth, row * cellHeight),
                size = Size(cellWidth + .5f, cellHeight + .5f),
            )
        }
    }
}
