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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mineit.android.BuildConfig
import com.mineit.android.domain.model.ColonyStatus
import com.mineit.android.domain.model.GameState
import com.mineit.android.domain.resources.ResourceCategory
import com.mineit.android.domain.simulation.ColonyMetrics
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
    simulationSpeed: Int,
    selectedSector: WorldTile?,
    selectedCoordinate: SectorCoordinate?,
    selectedSurveyDays: Int?,
    statusMessage: String?,
    onSelectLandingSite: (Int) -> Unit,
    onSelectSector: (SectorCoordinate) -> Unit,
    onSurveySelectedSector: () -> Unit,
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
                        Text(
                            text = "Android Migration ${BuildConfig.VERSION_NAME}  •  Y${state.date.year} D${state.date.day}",
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            ResourceStrip(state)
            ColonyStatus(state)

            statusMessage?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (metrics.lastDeaths > .0001) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }

            if (colony.status == ColonyStatus.SITE_SELECTION) {
                LandingSiteSelection(
                    candidates = colony.world.landingCandidates,
                    onSelectLandingSite = onSelectLandingSite,
                    modifier = Modifier.weight(1f),
                )
            } else {
                SettledWorld(
                    state = state,
                    metrics = metrics,
                    simulationSpeed = simulationSpeed,
                    selectedSector = selectedSector,
                    selectedCoordinate = selectedCoordinate,
                    selectedSurveyDays = selectedSurveyDays,
                    onSelectSector = onSelectSector,
                    onSurveySelectedSector = onSurveySelectedSector,
                    onAdvanceDay = onAdvanceDay,
                    onSetSimulationSpeed = onSetSimulationSpeed,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun ResourceStrip(state: GameState) {
    val inventory = state.activeColony.inventory
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        ResourceTile("FOOD", inventory.amountFor(ResourceCategory.FOOD), Modifier.weight(1f))
        ResourceTile("BUILD", inventory.amountFor(ResourceCategory.BUILD), Modifier.weight(1f))
        ResourceTile("FUEL", inventory.amountFor(ResourceCategory.FUEL), Modifier.weight(1f))
        ResourceTile("ORE", inventory.amountFor(ResourceCategory.ORE), Modifier.weight(1f))
    }
}

@Composable
private fun ResourceTile(label: String, value: Double, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 5.dp, vertical = 7.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(label, fontSize = 9.sp, color = MaterialTheme.colorScheme.secondary, maxLines = 1)
            Text(value.roundToLong().toString(), fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}

@Composable
private fun ColonyStatus(state: GameState) {
    val colony = state.activeColony
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(colony.name, fontWeight = FontWeight.Bold)
                Text(
                    text = "${colony.contract?.name ?: "Contract 01"} • Population ${formatNumber(colony.population)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                    maxLines = 1,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("£${state.company.cash}", color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                Text(colony.status.name.replace('_', ' '), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun LandingSiteSelection(
    candidates: List<LandingSiteCandidate>,
    onSelectLandingSite: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "LANDING SITE SELECTION",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "Contract 01 • choose one of 8 deterministic 8×8 terrain candidates",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
            modifier = Modifier.padding(bottom = 6.dp),
        )
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(12.dp)),
            contentPadding = PaddingValues(4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(items = candidates, key = { it.id }) { candidate ->
                LandingSiteCard(candidate, onClick = { onSelectLandingSite(candidate.index) })
            }
        }
    }
}

@Composable
private fun LandingSiteCard(candidate: LandingSiteCandidate, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("SITE ${candidate.index + 1}", fontWeight = FontWeight.Bold)
            Text("Plain ${candidate.counts[TerrainType.PLAIN] ?: 0} • Hill ${candidate.counts[TerrainType.HILL] ?: 0}", style = MaterialTheme.typography.bodySmall)
            Text("Mountain ${candidate.counts[TerrainType.MOUNTAIN] ?: 0} • Lake ${candidate.counts[TerrainType.LAKE] ?: 0}", style = MaterialTheme.typography.bodySmall)
            Text("Seed ${candidate.seed}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.58f))
        }
    }
}

@Composable
private fun SettledWorld(
    state: GameState,
    metrics: ColonyMetrics,
    simulationSpeed: Int,
    selectedSector: WorldTile?,
    selectedCoordinate: SectorCoordinate?,
    selectedSurveyDays: Int?,
    onSelectSector: (SectorCoordinate) -> Unit,
    onSurveySelectedSector: () -> Unit,
    onAdvanceDay: () -> Unit,
    onSetSimulationSpeed: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colony = state.activeColony
    val world = colony.world

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SimulationStatus(metrics, colony.foodStarvationDays)

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "SECTOR SURVEY • 8×8",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            Text("${world.activeSurveys.size} active • ${world.surveyQueue.size} queued", style = MaterialTheme.typography.labelSmall)
        }

        SectorGrid(
            tiles = world.tiles,
            selectedCoordinate = selectedCoordinate,
            activeSurveys = world.activeSurveys.associateBy { it.coordinate },
            queued = world.surveyQueue.toSet(),
            onSelectSector = onSelectSector,
            modifier = Modifier.weight(1f),
        )

        SectorDetails(
            tile = selectedSector,
            activeDaysRemaining = selectedSector?.coordinate?.let { coordinate -> world.activeSurveys.firstOrNull { it.coordinate == coordinate }?.daysRemaining },
            queued = selectedSector?.coordinate?.let(world.surveyQueue::contains) == true,
        )

        Button(
            onClick = onSurveySelectedSector,
            enabled = selectedSurveyDays != null && colony.status != ColonyStatus.DEAD,
            modifier = Modifier.fillMaxWidth().height(40.dp),
        ) {
            Text(if (selectedSurveyDays != null) "SURVEY SELECTED • $selectedSurveyDays DAYS" else "SELECT A SURVEYABLE SECTOR")
        }

        SimulationControls(
            speed = simulationSpeed,
            enabled = colony.status != ColonyStatus.DEAD,
            onSetSpeed = onSetSimulationSpeed,
            onAdvanceDay = onAdvanceDay,
        )
    }
}

@Composable
private fun SimulationStatus(metrics: ColonyMetrics, starvationDays: Int) {
    val lifePercent = (metrics.lifeSupportPowerFactor * 100).roundToLong()
    val warning = metrics.lifeSupportPowerFactor < .7 || metrics.foodSupply < .7
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "POWER ${formatNumber(metrics.powerFuelLimitedGeneration)}/${formatNumber(metrics.powerDemand)}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (metrics.lifeSupportPowerFactor < .7) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f),
                )
                Text("Life support $lifePercent%", style = MaterialTheme.typography.labelSmall)
            }
            Text(
                "Food ${formatNumber(metrics.foodProduction)}/${formatNumber(metrics.foodDemand)}/day • Fuel burn ${formatNumber(metrics.powerFuelBurn)} • Industry ${formatNumber(metrics.industry)}/${formatNumber(metrics.industryInstalled)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = .72f),
                maxLines = 1,
            )
            Text(
                "Workforce ${formatNumber(metrics.workforceAvailable)}/${formatNumber(metrics.workforceRequired)} • Starvation $starvationDays d${if (warning) " • SURVIVAL SHORTAGE" else ""}",
                style = MaterialTheme.typography.labelSmall,
                color = if (warning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface.copy(alpha = .72f),
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun SimulationControls(
    speed: Int,
    enabled: Boolean,
    onSetSpeed: (Int) -> Unit,
    onAdvanceDay: () -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        listOf(0 to "PAUSE", 1 to "1×", 2 to "2×", 4 to "4×").forEach { (value, label) ->
            OutlinedButton(
                onClick = { onSetSpeed(value) },
                enabled = enabled,
                modifier = Modifier.weight(1f).height(38.dp),
                contentPadding = PaddingValues(horizontal = 2.dp),
            ) {
                Text(if (speed == value) "• $label" else label, fontSize = 10.sp)
            }
        }
        Button(
            onClick = onAdvanceDay,
            enabled = enabled && speed == 0,
            modifier = Modifier.weight(1.35f).height(38.dp),
            contentPadding = PaddingValues(horizontal = 2.dp),
        ) {
            Text("+1 DAY", fontSize = 10.sp)
        }
    }
}

@Composable
private fun SectorGrid(
    tiles: List<WorldTile>,
    selectedCoordinate: SectorCoordinate?,
    activeSurveys: Map<SectorCoordinate, com.mineit.android.domain.world.SurveyTask>,
    queued: Set<SectorCoordinate>,
    onSelectSector: (SectorCoordinate) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(8),
        modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
        contentPadding = PaddingValues(4.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        items(
            items = tiles.sortedWith(compareBy<WorldTile> { it.coordinate.y }.thenBy { it.coordinate.x }),
            key = { "${it.coordinate.x}:${it.coordinate.y}" },
        ) { tile ->
            SectorCell(
                tile = tile,
                selected = tile.coordinate == selectedCoordinate,
                activeDaysRemaining = activeSurveys[tile.coordinate]?.daysRemaining,
                queued = queued.contains(tile.coordinate),
                onClick = { onSelectSector(tile.coordinate) },
            )
        }
    }
}

@Composable
private fun SectorCell(
    tile: WorldTile,
    selected: Boolean,
    activeDaysRemaining: Double?,
    queued: Boolean,
    onClick: () -> Unit,
) {
    val isShip = tile.coordinate.x == 0 && tile.coordinate.y == 0
    val containerColor = when {
        selected -> MaterialTheme.colorScheme.primary
        activeDaysRemaining != null -> MaterialTheme.colorScheme.tertiaryContainer
        tile.revealed -> MaterialTheme.colorScheme.surfaceVariant
        else -> MaterialTheme.colorScheme.surface
    }
    val contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    val status = when {
        isShip -> "SHIP"
        activeDaysRemaining != null -> "${ceil(activeDaysRemaining).toInt()}d"
        queued -> "QUEUE"
        !tile.revealed -> "?"
        tile.deposit != null -> tile.deposit.name.take(4).uppercase()
        else -> "CLEAR"
    }

    Surface(
        onClick = onClick,
        modifier = Modifier.aspectRatio(1f),
        shape = RoundedCornerShape(6.dp),
        color = containerColor,
        contentColor = contentColor,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${tile.coordinate.x},${tile.coordinate.y}", fontSize = 7.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 1)
                Text(status, fontSize = 7.sp, color = if (selected) contentColor else MaterialTheme.colorScheme.secondary, maxLines = 1)
            }
        }
    }
}

@Composable
private fun SectorDetails(tile: WorldTile?, activeDaysRemaining: Double?, queued: Boolean) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 7.dp)) {
            if (tile == null) {
                Text("Tap a sector to inspect it", style = MaterialTheme.typography.bodyMedium)
                return@Column
            }
            Text("Sector ${tile.coordinate.x},${tile.coordinate.y} • ${terrainName(tile.terrain)}", fontWeight = FontWeight.Bold)
            val description = when {
                activeDaysRemaining != null -> "Survey active • ${ceil(activeDaysRemaining).toInt()} days remaining"
                queued -> "Survey queued"
                !tile.revealed -> "Unsurveyed • geological reading unknown"
                tile.deposit != null -> "${tile.deposit.name} • quality ${tile.deposit.quality} • ${tile.deposit.abundanceLabel ?: tile.deposit.depositScale ?: "deposit"}"
                else -> "Surveyed L${tile.lastScannedAtLevel} • clear reading"
            }
            Text(description, style = MaterialTheme.typography.bodySmall)
        }
    }
}

private fun terrainName(terrain: TerrainType): String = when (terrain) {
    TerrainType.PLAIN -> "Plain"
    TerrainType.HILL -> "Hill"
    TerrainType.MOUNTAIN -> "Mountain"
    TerrainType.LAKE -> "Lake"
}

private fun formatNumber(value: Double): String {
    val rounded = value.roundToLong()
    return if (kotlin.math.abs(value - rounded) < .005) rounded.toString() else "%.1f".format(value)
}
