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
    selectedSector: WorldTile?,
    selectedCoordinate: SectorCoordinate?,
    selectedSurveyDays: Int?,
    statusMessage: String?,
    onSelectLandingSite: (Int) -> Unit,
    onSelectSector: (SectorCoordinate) -> Unit,
    onSurveySelectedSector: () -> Unit,
    onAdvanceSurveyDay: () -> Unit,
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
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ResourceStrip(state)
            ColonyStatus(state)

            statusMessage?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
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
                    selectedSector = selectedSector,
                    selectedCoordinate = selectedCoordinate,
                    selectedSurveyDays = selectedSurveyDays,
                    onSelectSector = onSelectSector,
                    onSurveySelectedSector = onSurveySelectedSector,
                    onAdvanceSurveyDay = onAdvanceSurveyDay,
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
            Text(
                text = label,
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.secondary,
                maxLines = 1,
            )
            Text(
                text = value.roundToLong().toString(),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
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
                    text = "${colony.contract?.name ?: "Contract 01"} • Population ${colony.population}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                    maxLines = 1,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "£${state.company.cash}",
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = colony.status.name.replace('_', ' '),
                    style = MaterialTheme.typography.labelSmall,
                )
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
            text = "Real Contract 01 • choose one of 8 deterministic 8×8 terrain candidates",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
            modifier = Modifier.padding(bottom = 6.dp),
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp)),
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
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("SITE ${candidate.index + 1}", fontWeight = FontWeight.Bold)
            Text(
                text = "Plain ${candidate.counts[TerrainType.PLAIN] ?: 0} • Hill ${candidate.counts[TerrainType.HILL] ?: 0}",
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text = "Mountain ${candidate.counts[TerrainType.MOUNTAIN] ?: 0} • Lake ${candidate.counts[TerrainType.LAKE] ?: 0}",
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text = "Seed ${candidate.seed}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.58f),
            )
        }
    }
}

@Composable
private fun SettledWorld(
    state: GameState,
    selectedSector: WorldTile?,
    selectedCoordinate: SectorCoordinate?,
    selectedSurveyDays: Int?,
    onSelectSector: (SectorCoordinate) -> Unit,
    onSurveySelectedSector: () -> Unit,
    onAdvanceSurveyDay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val world = state.activeColony.world
    val hasSurveyWork = world.activeSurveys.isNotEmpty() || world.surveyQueue.isNotEmpty()

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "SECTOR SURVEY • 8×8",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "${world.activeSurveys.size} active • ${world.surveyQueue.size} queued",
                style = MaterialTheme.typography.labelSmall,
            )
        }

        Text(
            text = "Phase 2 validation: terrain, discovery and surveying are real; daily economy/survival begins in Phase 3.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
        )

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
            activeDaysRemaining = selectedSector?.coordinate?.let { coordinate ->
                world.activeSurveys.firstOrNull { it.coordinate == coordinate }?.daysRemaining
            },
            queued = selectedSector?.coordinate?.let(world.surveyQueue::contains) == true,
        )

        Button(
            onClick = onSurveySelectedSector,
            enabled = selectedSurveyDays != null,
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
        ) {
            Text(
                if (selectedSurveyDays != null) {
                    "SURVEY SELECTED • $selectedSurveyDays DAYS"
                } else {
                    "SELECT A SURVEYABLE SECTOR"
                },
            )
        }

        Button(
            onClick = onAdvanceSurveyDay,
            enabled = hasSurveyWork,
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
        ) {
            Text("ADVANCE SURVEY DAY")
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
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)),
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
                Text(
                    text = "${tile.coordinate.x},${tile.coordinate.y}",
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
                Text(
                    text = terrainCode(tile.terrain),
                    fontSize = 7.sp,
                    color = if (selected) contentColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                )
                Text(
                    text = status,
                    fontSize = 8.sp,
                    color = if (selected) contentColor else MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun SectorDetails(
    tile: WorldTile?,
    activeDaysRemaining: Double?,
    queued: Boolean,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            if (tile == null) {
                Text("Tap a sector to inspect it", fontWeight = FontWeight.Bold)
                Text(
                    "Terrain is known from landing survey; geological resources are hidden until scanned.",
                    style = MaterialTheme.typography.bodySmall,
                )
                return@Column
            }

            Text(
                text = "Sector ${tile.coordinate.x},${tile.coordinate.y} • ${terrainName(tile.terrain)}",
                fontWeight = FontWeight.Bold,
            )
            val detail = when {
                tile.coordinate.x == 0 && tile.coordinate.y == 0 -> "Founding ship tile • not surveyable"
                activeDaysRemaining != null -> "Surveying • ${ceil(activeDaysRemaining).toInt()} days remaining"
                queued -> "Queued for survey"
                !tile.revealed -> "Unsurveyed • geological reading unknown"
                tile.deposit == null -> "Surveyed L${tile.lastScannedAtLevel} • clear reading"
                else -> {
                    val deposit = tile.deposit
                    val scale = deposit.abundanceLabel ?: deposit.depositScale ?: "Deposit"
                    val reserve = deposit.reserve?.let { " • Reserve $it" }.orEmpty()
                    "${deposit.name} • ${deposit.category.name} • Quality ${deposit.quality} • $scale$reserve"
                }
            }
            Text(detail, style = MaterialTheme.typography.bodySmall)
        }
    }
}

private fun terrainCode(terrain: TerrainType): String = when (terrain) {
    TerrainType.PLAIN -> "PLN"
    TerrainType.HILL -> "HIL"
    TerrainType.MOUNTAIN -> "MTN"
    TerrainType.LAKE -> "LAK"
}

private fun terrainName(terrain: TerrainType): String = when (terrain) {
    TerrainType.PLAIN -> "Plain"
    TerrainType.HILL -> "Hill"
    TerrainType.MOUNTAIN -> "Mountain"
    TerrainType.LAKE -> "Lake"
}
