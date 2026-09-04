package com.mineit.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mineit.android.BuildConfig
import com.mineit.android.domain.colony.ColonyNetworkSnapshot
import com.mineit.android.domain.colony.DevelopmentPreview
import com.mineit.android.domain.colony.HeadquartersDepartureGate
import com.mineit.android.domain.colony.SpaceportStatus
import com.mineit.android.domain.model.ColonyStatus
import com.mineit.android.domain.model.GameState
import com.mineit.android.domain.resources.ExtractionCompatibility
import com.mineit.android.domain.resources.ResourceCategory
import com.mineit.android.domain.simulation.ColonyMetrics
import com.mineit.android.domain.world.DevelopmentKind
import com.mineit.android.domain.world.LandingSiteCandidate
import com.mineit.android.domain.world.SectorCoordinate
import com.mineit.android.domain.world.TerrainType
import com.mineit.android.domain.world.WorldTile
import kotlin.math.ceil
import kotlin.math.roundToLong

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MineItScreen(
    state: GameState,
    metrics: ColonyMetrics,
    network: ColonyNetworkSnapshot,
    spaceport: SpaceportStatus,
    simulationSpeed: Int,
    selectedSector: WorldTile?,
    selectedCoordinate: SectorCoordinate?,
    selectedSurveyDays: Int?,
    statusMessage: String?,
    powerPreview: DevelopmentPreview?,
    housingPreview: DevelopmentPreview?,
    industryPreview: DevelopmentPreview?,
    headquartersPreview: DevelopmentPreview?,
    extractionPreview: DevelopmentPreview?,
    upgradePreview: DevelopmentPreview?,
    departureGate: HeadquartersDepartureGate,
    onSelectLandingSite: (Int) -> Unit,
    onSelectSector: (SectorCoordinate) -> Unit,
    onSurveySelectedSector: () -> Unit,
    onBuild: (DevelopmentKind) -> Unit,
    onDevelopExtraction: () -> Unit,
    onUpgrade: () -> Unit,
    onDemolish: () -> Unit,
    onSetPrimaryHeadquarters: () -> Unit,
    onAdvanceDay: () -> Unit,
    onSetSimulationSpeed: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colony = state.activeColony
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("MINEIT", fontWeight = FontWeight.Black)
                        Text("Android Migration ${BuildConfig.VERSION_NAME}  •  Y${state.date.year} D${state.date.day}", style = MaterialTheme.typography.labelSmall)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 10.dp, vertical = 7.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            ResourceStrip(state)
            ColonyStatus(state)
            statusMessage?.let {
                Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(horizontal = 4.dp))
            }
            if (colony.status == ColonyStatus.SITE_SELECTION) {
                LandingSiteSelection(colony.world.landingCandidates, onSelectLandingSite, Modifier.weight(1f))
            } else {
                SettledWorld(
                    state, metrics, network, spaceport, simulationSpeed,
                    selectedSector, selectedCoordinate, selectedSurveyDays,
                    powerPreview, housingPreview, industryPreview, headquartersPreview,
                    extractionPreview, upgradePreview, departureGate,
                    onSelectSector, onSurveySelectedSector, onBuild, onDevelopExtraction,
                    onUpgrade, onDemolish, onSetPrimaryHeadquarters, onAdvanceDay, onSetSimulationSpeed,
                    Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun ResourceStrip(state: GameState) {
    val inventory = state.activeColony.inventory
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        ResourceTile("FOOD", inventory.amountFor(ResourceCategory.FOOD), Modifier.weight(1f))
        ResourceTile("BUILD", inventory.amountFor(ResourceCategory.BUILD), Modifier.weight(1f))
        ResourceTile("FUEL", inventory.amountFor(ResourceCategory.FUEL), Modifier.weight(1f))
        ResourceTile("ORE", inventory.amountFor(ResourceCategory.ORE), Modifier.weight(1f))
    }
}

@Composable
private fun ResourceTile(label: String, value: Double, modifier: Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, fontSize = 9.sp, color = MaterialTheme.colorScheme.secondary)
            Text(value.roundToLong().toString(), fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ColonyStatus(state: GameState) {
    val colony = state.activeColony
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(colony.name, fontWeight = FontWeight.Bold)
                Text("${colony.contract?.name ?: "Contract 01"} • Population ${formatNumber(colony.population)}", style = MaterialTheme.typography.bodySmall, maxLines = 1)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("£${state.company.cash}", color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                Text(colony.status.name.replace('_', ' '), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun LandingSiteSelection(candidates: List<LandingSiteCandidate>, onSelect: (Int) -> Unit, modifier: Modifier) {
    Column(modifier.fillMaxWidth()) {
        Text("LANDING SITE SELECTION", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        Text("Contract 01 • choose one of 8 deterministic 8×8 candidates", style = MaterialTheme.typography.labelSmall)
        LazyVerticalGrid(
            columns = GridCells.Fixed(2), modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(candidates, key = { it.id }) { candidate ->
                Surface(onClick = { onSelect(candidate.index) }, shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                    Column(Modifier.padding(10.dp)) {
                        Text("SITE ${candidate.index + 1}", fontWeight = FontWeight.Bold)
                        Text("Plain ${candidate.counts[TerrainType.PLAIN] ?: 0} • Hill ${candidate.counts[TerrainType.HILL] ?: 0}", style = MaterialTheme.typography.bodySmall)
                        Text("Mountain ${candidate.counts[TerrainType.MOUNTAIN] ?: 0} • Lake ${candidate.counts[TerrainType.LAKE] ?: 0}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun SettledWorld(
    state: GameState,
    metrics: ColonyMetrics,
    network: ColonyNetworkSnapshot,
    spaceport: SpaceportStatus,
    speed: Int,
    selectedSector: WorldTile?,
    selectedCoordinate: SectorCoordinate?,
    surveyDays: Int?,
    powerPreview: DevelopmentPreview?,
    housingPreview: DevelopmentPreview?,
    industryPreview: DevelopmentPreview?,
    headquartersPreview: DevelopmentPreview?,
    extractionPreview: DevelopmentPreview?,
    upgradePreview: DevelopmentPreview?,
    departureGate: HeadquartersDepartureGate,
    onSelectSector: (SectorCoordinate) -> Unit,
    onSurvey: () -> Unit,
    onBuild: (DevelopmentKind) -> Unit,
    onDevelop: () -> Unit,
    onUpgrade: () -> Unit,
    onDemolish: () -> Unit,
    onSetPrimary: () -> Unit,
    onAdvanceDay: () -> Unit,
    onSetSpeed: (Int) -> Unit,
    modifier: Modifier,
) {
    val colony = state.activeColony
    val world = colony.world
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        NetworkStatus(metrics, network, spaceport, departureGate)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("COLONY GRID • 8×8", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text("${world.activeSurveys.size} scan • ${world.surveyQueue.size} queued", style = MaterialTheme.typography.labelSmall)
        }
        SectorGrid(
            world.tiles, selectedCoordinate, world.activeSurveys.associateBy { it.coordinate }, world.surveyQueue.toSet(), onSelectSector,
            Modifier.weight(1f),
        )
        SectorDetails(selectedSector, selectedSector?.coordinate?.let { c -> world.activeSurveys.firstOrNull { it.coordinate == c }?.daysRemaining }, selectedSector?.coordinate?.let(world.surveyQueue::contains) == true)
        DevelopmentActions(
            selectedSector, surveyDays, powerPreview, housingPreview, industryPreview, headquartersPreview,
            extractionPreview, upgradePreview, colony.headquarters.primary,
            onSurvey, onBuild, onDevelop, onUpgrade, onDemolish, onSetPrimary,
        )
        SimulationControls(speed, colony.status != ColonyStatus.DEAD, onSetSpeed, onAdvanceDay)
    }
}

@Composable
private fun NetworkStatus(
    metrics: ColonyMetrics,
    network: ColonyNetworkSnapshot,
    spaceport: SpaceportStatus,
    departureGate: HeadquartersDepartureGate,
) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(horizontal = 9.dp, vertical = 6.dp)) {
            Row(Modifier.fillMaxWidth()) {
                Text("POWER ${formatNumber(metrics.powerFuelLimitedGeneration)}/${formatNumber(metrics.powerDemand)}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text(if (spaceport.operational) "SPACEPORT ONLINE" else "SPACEPORT OFFLINE", style = MaterialTheme.typography.labelSmall, color = if (spaceport.operational) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error)
            }
            Text(
                "Life ${percent(metrics.lifeSupportPowerFactor)} • Food ${formatNumber(metrics.foodProduction)}/${formatNumber(metrics.foodDemand)} • Fuel burn ${formatNumber(metrics.powerFuelBurn)} • Industry ${formatNumber(metrics.industry)}/${formatNumber(metrics.industryInstalled)}",
                style = MaterialTheme.typography.labelSmall, maxLines = 1,
            )
            val source = network.headquarters.sourceType?.name ?: "NONE"
            Text(
                "Command $source ${formatNumber(network.headquarters.load)}/${formatNumber(network.headquarters.capacity)} • Eff ${percent(network.continuity.effectiveCommandEfficiency)} • ${network.continuity.phase.name} • Handover ${if (departureGate.ok) "READY" else "BLOCKED"}",
                style = MaterialTheme.typography.labelSmall,
                color = if (!network.continuity.networkAvailable) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface.copy(alpha = .72f),
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun DevelopmentActions(
    tile: WorldTile?,
    surveyDays: Int?,
    power: DevelopmentPreview?,
    housing: DevelopmentPreview?,
    industry: DevelopmentPreview?,
    headquarters: DevelopmentPreview?,
    extraction: DevelopmentPreview?,
    upgrade: DevelopmentPreview?,
    primary: SectorCoordinate?,
    onSurvey: () -> Unit,
    onBuild: (DevelopmentKind) -> Unit,
    onDevelop: () -> Unit,
    onUpgrade: () -> Unit,
    onDemolish: () -> Unit,
    onSetPrimary: () -> Unit,
) {
    if (tile == null) return
    if (tile.development != null) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Button(onClick = onUpgrade, enabled = upgrade?.ok == true, modifier = Modifier.weight(1f).height(36.dp), contentPadding = PaddingValues(2.dp)) {
                Text(if (upgrade?.nextLevel ?: 0 > 0) "UPGRADE L${upgrade?.nextLevel}" else "UPGRADE", fontSize = 9.sp)
            }
            OutlinedButton(onClick = onDemolish, modifier = Modifier.weight(1f).height(36.dp), contentPadding = PaddingValues(2.dp)) { Text("DEMOLISH", fontSize = 9.sp) }
            if (tile.development.kind == DevelopmentKind.HEADQUARTERS && tile.coordinate != primary) {
                OutlinedButton(onClick = onSetPrimary, modifier = Modifier.weight(1f).height(36.dp), contentPadding = PaddingValues(2.dp)) { Text("SET PRIMARY", fontSize = 9.sp) }
            }
        }
        upgrade?.reason?.takeIf { !upgrade.ok }?.let { Text(it, style = MaterialTheme.typography.labelSmall, maxLines = 1) }
        return
    }
    if (!tile.revealed) {
        Button(onClick = onSurvey, enabled = surveyDays != null, modifier = Modifier.fillMaxWidth().height(36.dp)) {
            Text(if (surveyDays != null) "SURVEY • $surveyDays DAYS" else "SURVEY UNAVAILABLE", fontSize = 10.sp)
        }
        return
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        listOf(
            Triple("POWER", DevelopmentKind.POWER, power),
            Triple("HOUSE", DevelopmentKind.HOUSING, housing),
            Triple("IND", DevelopmentKind.INDUSTRY, industry),
            Triple("HQ", DevelopmentKind.HEADQUARTERS, headquarters),
        ).forEach { (label, kind, preview) ->
            OutlinedButton(onClick = { onBuild(kind) }, enabled = preview?.ok == true, modifier = Modifier.weight(1f).height(34.dp), contentPadding = PaddingValues(1.dp)) {
                Text(label, fontSize = 9.sp)
            }
        }
        if (tile.deposit != null && !tile.resourceCovered && !tile.resourceExhausted) {
            Button(onClick = onDevelop, enabled = extraction?.ok == true, modifier = Modifier.weight(1.2f).height(34.dp), contentPadding = PaddingValues(1.dp)) {
                Text("DEVELOP", fontSize = 9.sp)
            }
        }
    }
    val hint = extraction?.takeIf { tile.deposit != null && !it.ok }?.reason
        ?: listOfNotNull(power, housing, industry, headquarters).firstOrNull { !it.ok }?.reason
    hint?.let { Text(it, style = MaterialTheme.typography.labelSmall, maxLines = 1) }
}

@Composable
private fun SimulationControls(speed: Int, enabled: Boolean, onSetSpeed: (Int) -> Unit, onAdvanceDay: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        listOf(0 to "PAUSE", 1 to "1×", 2 to "2×", 4 to "4×").forEach { (value, label) ->
            OutlinedButton(onClick = { onSetSpeed(value) }, enabled = enabled, modifier = Modifier.weight(1f).height(36.dp), contentPadding = PaddingValues(2.dp)) {
                Text(if (speed == value) "• $label" else label, fontSize = 9.sp)
            }
        }
        Button(onClick = onAdvanceDay, enabled = enabled && speed == 0, modifier = Modifier.weight(1.25f).height(36.dp), contentPadding = PaddingValues(2.dp)) { Text("+1 DAY", fontSize = 9.sp) }
    }
}

@Composable
private fun SectorGrid(
    tiles: List<WorldTile>, selected: SectorCoordinate?,
    active: Map<SectorCoordinate, com.mineit.android.domain.world.SurveyTask>, queued: Set<SectorCoordinate>,
    onSelect: (SectorCoordinate) -> Unit, modifier: Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(8), modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
        contentPadding = PaddingValues(3.dp), horizontalArrangement = Arrangement.spacedBy(3.dp), verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        items(tiles.sortedWith(compareBy<WorldTile> { it.coordinate.y }.thenBy { it.coordinate.x }), key = { "${it.coordinate.x}:${it.coordinate.y}" }) { tile ->
            SectorCell(tile, tile.coordinate == selected, active[tile.coordinate]?.daysRemaining, queued.contains(tile.coordinate)) { onSelect(tile.coordinate) }
        }
    }
}

@Composable
private fun SectorCell(tile: WorldTile, selected: Boolean, activeDays: Double?, queued: Boolean, onClick: () -> Unit) {
    val isShip = tile.coordinate.x == 0 && tile.coordinate.y == 0
    val color = when {
        selected -> MaterialTheme.colorScheme.primary
        activeDays != null -> MaterialTheme.colorScheme.tertiaryContainer
        tile.development != null -> MaterialTheme.colorScheme.secondaryContainer
        tile.revealed -> MaterialTheme.colorScheme.surfaceVariant
        else -> MaterialTheme.colorScheme.surface
    }
    val status = when {
        isShip -> "PORT"
        tile.development != null -> developmentCode(tile.development.kind) + tile.development.level
        activeDays != null -> "${ceil(activeDays).toInt()}d"
        queued -> "QUEUE"
        !tile.revealed -> "?"
        tile.resourceExhausted -> "EMPTY"
        tile.deposit != null -> tile.deposit.name.take(4).uppercase()
        else -> "CLEAR"
    }
    Surface(onClick = onClick, modifier = Modifier.aspectRatio(1f), shape = RoundedCornerShape(6.dp), color = color) {
        Box(contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${tile.coordinate.x},${tile.coordinate.y}", fontSize = 7.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(status, fontSize = 7.sp, color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.secondary, maxLines = 1)
            }
        }
    }
}

@Composable
private fun SectorDetails(tile: WorldTile?, activeDays: Double?, queued: Boolean) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 11.dp, vertical = 6.dp)) {
            if (tile == null) { Text("Tap a sector to inspect it"); return@Column }
            Text("Sector ${tile.coordinate.x},${tile.coordinate.y} • ${terrainName(tile.terrain)}", fontWeight = FontWeight.Bold)
            val dev = tile.development
            val text = when {
                dev != null -> "${developmentName(dev.kind)} L${dev.level}${if (tile.resourceCovered) " • resource covered" else ""}"
                activeDays != null -> "Survey active • ${ceil(activeDays).toInt()} days remaining"
                queued -> "Survey queued"
                !tile.revealed -> "Unsurveyed • geological reading unknown"
                tile.resourceExhausted -> "Resource exhausted • clear after demolition"
                tile.deposit != null -> "${tile.deposit.name} • Q${tile.deposit.quality} • ${ExtractionCompatibility.familyFor(tile.deposit.resourceId).displayName}"
                else -> "Surveyed L${tile.lastScannedAtLevel} • clear reading"
            }
            Text(text, style = MaterialTheme.typography.bodySmall, maxLines = 1)
        }
    }
}

private fun developmentCode(kind: DevelopmentKind) = when (kind) {
    DevelopmentKind.POWER -> "P"
    DevelopmentKind.HOUSING -> "H"
    DevelopmentKind.INDUSTRY -> "I"
    DevelopmentKind.HEADQUARTERS -> "HQ"
    DevelopmentKind.EXTRACT -> "E"
}
private fun developmentName(kind: DevelopmentKind) = when (kind) {
    DevelopmentKind.POWER -> "Power Plant"
    DevelopmentKind.HOUSING -> "Housing"
    DevelopmentKind.INDUSTRY -> "Industry"
    DevelopmentKind.HEADQUARTERS -> "Headquarters"
    DevelopmentKind.EXTRACT -> "Extraction Site"
}
private fun terrainName(terrain: TerrainType) = terrain.name.lowercase().replaceFirstChar { it.uppercase() }
private fun percent(value: Double) = "${(value * 100).roundToLong()}%"
private fun formatNumber(value: Double): String {
    val rounded = value.roundToLong()
    return if (kotlin.math.abs(value - rounded) < .005) rounded.toString() else "%.1f".format(value)
}
