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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
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
import com.mineit.android.ui.game.SectorContextBar
import com.mineit.android.ui.map.ColonyMap
import com.mineit.android.ui.map.MapFocus
import com.mineit.android.ui.map.MapStateFilter

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
    onSelectLandingSite: (Int) -> Unit,
    onSelectSector: (SectorCoordinate) -> Unit,
    onBeginMultiSelect: (SectorCoordinate) -> Unit,
    onAddMultiSelect: (SectorCoordinate) -> Unit,
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
    onAdvanceDay: () -> Unit,
    onSetSimulationSpeed: (Int) -> Unit,
    onOpenCommercial: () -> Unit,
    onOpenAttention: () -> Unit,
    onMainMenu: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showColonyDetail by remember { mutableStateOf(false) }
    val colony = state.activeColony

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MineItPalette.Background,
    ) { scaffoldPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(scaffoldPadding)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = MineItSpacing.Sm, vertical = MineItSpacing.Xs),
            verticalArrangement = Arrangement.spacedBy(MineItSpacing.Xs),
        ) {
            GameHeader(
                state = state,
                metrics = metrics,
                network = network,
                spaceport = spaceport,
                onOpenColonyDetail = { showColonyDetail = true },
            )

            ColonyAttentionStrip(
                attention = attention,
                onClick = {
                    if (attention.target == ColonyAttentionTarget.COLONY) showColonyDetail = true
                    else onOpenAttention()
                },
            )
            statusMessage?.let { StatusStrip(it) }

            if (colony.status == ColonyStatus.SITE_SELECTION) {
                LandingSiteSelection(
                    candidates = colony.world.landingCandidates,
                    onSelect = onSelectLandingSite,
                    modifier = Modifier.weight(1f),
                )
                FooterControls(
                    speed = simulationSpeed,
                    enabled = false,
                    onSetSpeed = onSetSimulationSpeed,
                    onAdvanceDay = onAdvanceDay,
                    onMainMenu = onMainMenu,
                )
            } else {
                MineItSecondaryButton(
                    text = if (colony.trade.active) "COMMERCIAL • TRADE SHIP DOCKED" else "COMMERCIAL • TRADE / CONTRACTS / BUYERS / LOG",
                    onClick = onOpenCommercial,
                    modifier = Modifier.fillMaxWidth(),
                    selected = colony.trade.active,
                    accent = if (colony.trade.active) MineItPalette.Success else MineItPalette.Accent,
                )

                MapToolbar(
                    focus = mapFocus,
                    filters = mapFilters,
                    onFocus = onSetMapFocus,
                    onToggleFilter = onToggleMapFilter,
                    onReset = onClearMapFilters,
                )

                ColonyMap(
                    tiles = colony.world.tiles,
                    activeSurveys = colony.world.activeSurveys,
                    queued = colony.world.surveyQueue.toSet(),
                    selected = selectedTiles.mapTo(linkedSetOf()) { it.coordinate },
                    focus = mapFocus,
                    stateFilters = mapFilters,
                    network = network,
                    onTap = onSelectSector,
                    onBeginMultiSelect = onBeginMultiSelect,
                    onAddMultiSelect = onAddMultiSelect,
                    modifier = Modifier.fillMaxWidth().weight(1f),
                )

                SectorContextBar(
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
                )

                FooterControls(
                    speed = simulationSpeed,
                    enabled = colony.status != ColonyStatus.DEAD,
                    onSetSpeed = onSetSimulationSpeed,
                    onAdvanceDay = onAdvanceDay,
                    onMainMenu = onMainMenu,
                )
            }
        }
    }

    if (showColonyDetail) {
        ColonyDetailSheet(
            state = state,
            metrics = metrics,
            network = network,
            spaceport = spaceport,
            departureGate = departureGate,
            onDismiss = { showColonyDetail = false },
        )
    }
}

@Composable
private fun StatusStrip(message: String) {
    val critical = message.contains("lost", true) || message.contains("failed", true) || message.contains("death", true)
    val color = if (critical) MineItPalette.Critical else MineItPalette.Accent
    Surface(
        color = color.copy(alpha = .10f),
        shape = RoundedCornerShape(5.dp),
        modifier = Modifier.fillMaxWidth(),
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
) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(MineItSpacing.Xs),
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
            )
        }
        MapStateFilter.entries.forEach { filter ->
            MineItSecondaryButton(
                text = filter.label,
                onClick = { onToggleFilter(filter) },
                selected = filter in filters,
                accent = MineItPalette.Survey,
            )
        }
        MineItSecondaryButton("RESET", onReset, enabled = focus != MapFocus.ALL || filters.isNotEmpty())
    }
}

@Composable
private fun FooterControls(
    speed: Int,
    enabled: Boolean,
    onSetSpeed: (Int) -> Unit,
    onAdvanceDay: () -> Unit,
    onMainMenu: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MineItSpacing.Xs),
    ) {
        listOf(0 to "PAUSE", 1 to "1×", 2 to "2×", 4 to "4×").forEach { (value, label) ->
            MineItSecondaryButton(
                text = label,
                onClick = { onSetSpeed(value) },
                enabled = enabled,
                selected = speed == value,
                modifier = Modifier.weight(1f),
            )
        }
        MineItSecondaryButton(
            text = "+1 DAY",
            onClick = onAdvanceDay,
            enabled = enabled && speed == 0,
            selected = false,
            accent = MineItPalette.Warning,
            modifier = Modifier.weight(1.25f),
        )
        MineItSecondaryButton(
            text = "MENU",
            onClick = onMainMenu,
            modifier = Modifier.weight(1f),
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
