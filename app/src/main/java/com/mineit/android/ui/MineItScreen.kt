package com.mineit.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
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
import com.mineit.android.domain.GameState
import com.mineit.android.domain.Sector
import com.mineit.android.domain.SectorCoordinate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MineItScreen(
    state: GameState,
    selectedSector: Sector?,
    onSelectSector: (SectorCoordinate) -> Unit,
    onAdvanceDay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("MINEIT", fontWeight = FontWeight.Black)
                        Text(
                            text = "Native POC 0.1  •  Y${state.year} D${state.day}",
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

            Text(
                text = "SECTOR SURVEY",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )

            SectorGrid(
                sectors = state.sectors,
                selectedSector = selectedSector,
                onSelectSector = onSelectSector,
                modifier = Modifier.weight(1f),
            )

            SectorDetails(selectedSector)

            Button(
                onClick = onAdvanceDay,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
            ) {
                Text("ADVANCE DAY")
            }
        }
    }
}

@Composable
private fun ResourceStrip(state: GameState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        ResourceTile("FOOD", state.resources.food.toString(), Modifier.weight(1f))
        ResourceTile("WATER", state.resources.water.toString(), Modifier.weight(1f))
        ResourceTile("ORE", state.resources.ore.toString(), Modifier.weight(1f))
        ResourceTile("CREDITS", state.resources.credits.toString(), Modifier.weight(1f))
    }
}

@Composable
private fun ResourceTile(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 7.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = label,
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.secondary,
                maxLines = 1,
            )
            Text(
                text = value,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun ColonyStatus(state: GameState) {
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
                Text(
                    text = state.colony.name,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Founding colony • Population ${state.colony.population}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                )
            }
            Text(
                text = "⚡ ${state.colony.powerAvailable}/${state.colony.powerDemand}",
                color = if (state.colony.powerAvailable >= state.colony.powerDemand) {
                    MaterialTheme.colorScheme.secondary
                } else {
                    MaterialTheme.colorScheme.error
                },
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun SectorGrid(
    sectors: List<Sector>,
    selectedSector: Sector?,
    onSelectSector: (SectorCoordinate) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(6),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface),
        contentPadding = PaddingValues(6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items(
            items = sectors,
            key = { "${it.coordinate.x}:${it.coordinate.y}" },
        ) { sector ->
            SectorCell(
                sector = sector,
                selected = sector.coordinate == selectedSector?.coordinate,
                onClick = { onSelectSector(sector.coordinate) },
            )
        }
    }
}

@Composable
private fun SectorCell(
    sector: Sector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val containerColor = when {
        selected -> MaterialTheme.colorScheme.primary
        sector.surveyed -> MaterialTheme.colorScheme.surfaceVariant
        else -> Color(0xFF0D1119)
    }
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Surface(
        onClick = onClick,
        modifier = Modifier.aspectRatio(1f),
        shape = RoundedCornerShape(7.dp),
        color = containerColor,
        contentColor = contentColor,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${sector.coordinate.x},${sector.coordinate.y}",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = if (sector.surveyed) "R${sector.richness}" else "?",
                    fontSize = 11.sp,
                    color = if (selected) contentColor else MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun SectorDetails(selectedSector: Sector?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            if (selectedSector == null) {
                Text("Tap a sector to inspect it", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(2.dp))
                Text(
                    "This is native Compose interaction — no WebView.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
            } else {
                Text(
                    text = "Sector ${selectedSector.coordinate.x},${selectedSector.coordinate.y}",
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = if (selectedSector.surveyed) {
                        "Surveyed • Resource richness ${selectedSector.richness}/9"
                    } else {
                        "Unsurveyed • Geological reading unknown"
                    },
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}
