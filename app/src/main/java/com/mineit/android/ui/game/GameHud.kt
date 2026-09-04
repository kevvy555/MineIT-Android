package com.mineit.android.ui.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mineit.android.BuildConfig
import com.mineit.android.domain.colony.ColonyNetworkSnapshot
import com.mineit.android.domain.colony.SpaceportStatus
import com.mineit.android.domain.model.GameState
import com.mineit.android.domain.resources.ResourceCategory
import com.mineit.android.domain.simulation.ColonyMetrics
import com.mineit.android.ui.design.MineItPalette
import com.mineit.android.ui.design.MineItRadius
import com.mineit.android.ui.design.MineItResourceCard
import com.mineit.android.ui.design.MineItSpacing
import com.mineit.android.ui.design.MineItStat
import kotlin.math.roundToLong

@Composable
fun GameHeader(state: GameState, metrics: ColonyMetrics, network: ColonyNetworkSnapshot, spaceport: SpaceportStatus, onOpenColonyDetail: () -> Unit, modifier: Modifier = Modifier) {
    val colony = state.activeColony
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(MineItSpacing.Sm)) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = MineItSpacing.Sm), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(MineItSpacing.Md)) {
            Column(Modifier.weight(1f)) {
                Text("MINEIT", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                Text("${colony.contract?.name ?: "Contract 01"} • ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.labelSmall, color = MineItPalette.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("£${formatMoney(state.company.cash)}", style = MaterialTheme.typography.titleSmall, color = MineItPalette.Success)
                Text("Y${state.date.year} D${state.date.day}", style = MaterialTheme.typography.labelMedium, color = MineItPalette.Muted)
            }
        }
        ResourceHud(state, metrics)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(MineItSpacing.Xs)) {
            MineItStat("POWER", "${format(metrics.powerFuelLimitedGeneration)}/${format(metrics.powerDemand)}", if (metrics.powerFactor >= .999) MineItPalette.Success else MineItPalette.Critical, Modifier.weight(1f))
            MineItStat("LIFE", percent(metrics.lifeSupportPowerFactor), if (metrics.lifeSupportPowerFactor >= .999) MineItPalette.Success else MineItPalette.Critical, Modifier.weight(1f))
            MineItStat("INDUSTRY", "${format(metrics.industry)}/${format(metrics.industryInstalled)}", if (metrics.industryPowerFactor >= .999) MineItPalette.Success else MineItPalette.Warning, Modifier.weight(1f))
            MineItStat("COMMAND", "${format(network.headquarters.load)}/${format(network.headquarters.capacity)}", if (network.continuity.networkAvailable) MineItPalette.Success else MineItPalette.Critical, Modifier.weight(1f))
            Surface(onClick = onOpenColonyDetail, color = MineItPalette.Control, shape = RoundedCornerShape(MineItRadius.Small), border = BorderStroke(1.dp, MineItPalette.Line), modifier = Modifier.weight(1f)) {
                Column(Modifier.padding(horizontal = 4.dp, vertical = 3.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("COLONY", style = MaterialTheme.typography.labelSmall, color = MineItPalette.Muted)
                    Text(if (spaceport.operational) "ONLINE" else "DETAILS", style = MaterialTheme.typography.labelMedium, color = if (spaceport.operational) MineItPalette.Accent else MineItPalette.Warning)
                }
            }
        }
    }
}

@Composable
private fun ResourceHud(state: GameState, metrics: ColonyMetrics) {
    val inventory = state.activeColony.inventory
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(MineItSpacing.Xs)) {
        MineItResourceCard("FOOD", format(inventory.amountFor(ResourceCategory.FOOD)), supplyLabel(metrics.foodDays, metrics.foodProduction, metrics.foodDemand), MineItPalette.Food, Modifier.weight(1f))
        MineItResourceCard("BUILD", format(inventory.amountFor(ResourceCategory.BUILD)), "+${format(metrics.buildProduction)}/d", MineItPalette.Build, Modifier.weight(1f))
        MineItResourceCard("FUEL", format(inventory.amountFor(ResourceCategory.FUEL)), supplyLabel(metrics.fuelDays, metrics.fuelProduction, metrics.fuelDemand), MineItPalette.Fuel, Modifier.weight(1f))
        MineItResourceCard("ORE", format(inventory.amountFor(ResourceCategory.ORE)), supplyLabel(metrics.oreDays, metrics.oreProduction, metrics.oreDemand), MineItPalette.Ore, Modifier.weight(1f))
    }
}

private fun supplyLabel(days: Double?, production: Double, demand: Double): String = when {
    demand <= .0001 -> "stable"
    days == null -> "+${format(production)}/d"
    days < 1.0 -> "<1d"
    days > 999.0 -> "999+d"
    else -> "${days.roundToLong()}d"
}

fun format(value: Double): String = when {
    value >= 1_000 -> "%.1fk".format(value / 1000.0)
    value % 1.0 == 0.0 -> value.roundToLong().toString()
    else -> "%.1f".format(value)
}

fun formatMoney(value: Double): String = if (value % 1.0 == 0.0) value.roundToLong().toString() else "%.2f".format(value)
fun percent(value: Double): String = "${(value.coerceIn(0.0, 1.0) * 100).roundToLong()}%"
