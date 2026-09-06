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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mineit.android.BuildConfig
import com.mineit.android.domain.colony.ColonyEstablishmentAssessment
import com.mineit.android.domain.colony.ColonyNetworkSnapshot
import com.mineit.android.domain.colony.EstablishmentPhase
import com.mineit.android.domain.colony.EstablishmentResourceSplit
import com.mineit.android.domain.colony.InfrastructureRules
import com.mineit.android.domain.model.GameState
import com.mineit.android.domain.resources.ResourceCategory
import com.mineit.android.domain.simulation.ColonyMetrics
import com.mineit.android.domain.world.DevelopmentKind
import com.mineit.android.ui.design.MineItPalette
import com.mineit.android.ui.design.MineItRadius
import kotlin.math.max
import kotlin.math.roundToLong

@Composable
fun GameHeader(
    state: GameState,
    metrics: ColonyMetrics,
    network: ColonyNetworkSnapshot,
    modifier: Modifier = Modifier,
    establishment: ColonyEstablishmentAssessment = hudAssessment(state, metrics, network),
    notificationContent: (@Composable () -> Unit)? = null,
) {
    val colony = state.activeColony
    Column(
        modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 5.dp, vertical = 1.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text("MINEIT", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                Text(
                    "${colony.contract?.name ?: "Contract 01"} • ${BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MineItPalette.Muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("£${formatMoney(state.company.cash)}", style = MaterialTheme.typography.titleSmall, color = MineItPalette.Success)
                Text("Y${state.date.year} D${state.date.day}", style = MaterialTheme.typography.labelMedium, color = MineItPalette.Muted)
            }
        }

        notificationContent?.invoke()
        OperationalHud(metrics, network, establishment)
        ResourceHud(metrics, establishment)
    }
}

@Composable
private fun OperationalHud(
    metrics: ColonyMetrics,
    network: ColonyNetworkSnapshot,
    establishment: ColonyEstablishmentAssessment,
) {
    val planetaryIndustry = max(0.0, metrics.industry - network.shipIndustry)
    val freeWorkforce = max(0.0, network.workforceAvailable - network.workforceRequired)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        CompactSplitCard(
            label = "HOUSING",
            ship = "${format(establishment.shipResidents)}/${format(establishment.shipAccommodationCapacity.toDouble())}",
            colony = "${format(establishment.planetaryAccommodationResidents)}/${format(establishment.housingCapacity)}",
            modifier = Modifier.weight(1f),
        )
        CompactSplitCard(
            label = "POWER",
            ship = if (establishment.foundingShipId != null) "SELF" else "—",
            colony = "${format(network.fuelLimitedGeneration)}/${format(network.powerDemand)}",
            colonyColor = if (metrics.powerFactor >= .999) MineItPalette.Success else MineItPalette.Critical,
            modifier = Modifier.weight(1f),
        )
        CompactSplitCard(
            label = "INDUSTRY",
            ship = "+${format(network.shipIndustry)}",
            colony = "${format(planetaryIndustry)}/${format(network.builtIndustry)}",
            colonyColor = if (network.industryPowerFactor >= .999) MineItPalette.Success else MineItPalette.Warning,
            modifier = Modifier.weight(1f),
        )
        CompactSplitCard(
            label = "WORKFORCE",
            ship = "${establishment.shipCrew}/${establishment.shipMinimumCrew} MIN",
            colony = "${format(freeWorkforce)} FREE",
            colonyColor = if (network.workforceCommercialFactor >= .999) MineItPalette.Success else MineItPalette.Warning,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ResourceHud(metrics: ColonyMetrics, establishment: ColonyEstablishmentAssessment) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        CompactResourceCard(
            label = "FOOD",
            category = ResourceCategory.FOOD,
            establishment = establishment,
            colonyDetail = supplyLabel(metrics.foodDays, metrics.foodProduction, metrics.foodDemand),
            shipDetail = establishment.shipFoodDaysRemaining?.let(::daysLabel),
            accent = MineItPalette.Food,
            modifier = Modifier.weight(1f),
        )
        CompactResourceCard(
            label = "BUILD",
            category = ResourceCategory.BUILD,
            establishment = establishment,
            colonyDetail = "+${format(metrics.buildProduction)}/d",
            shipDetail = null,
            accent = MineItPalette.Build,
            modifier = Modifier.weight(1f),
        )
        CompactResourceCard(
            label = "FUEL",
            category = ResourceCategory.FUEL,
            establishment = establishment,
            colonyDetail = supplyLabel(metrics.fuelDays, metrics.fuelProduction, metrics.fuelDemand),
            shipDetail = null,
            accent = MineItPalette.Fuel,
            modifier = Modifier.weight(1f),
        )
        CompactResourceCard(
            label = "ORE",
            category = ResourceCategory.ORE,
            establishment = establishment,
            colonyDetail = supplyLabel(metrics.oreDays, metrics.oreProduction, metrics.oreDemand),
            shipDetail = null,
            accent = MineItPalette.Ore,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun CompactSplitCard(
    label: String,
    ship: String,
    colony: String,
    modifier: Modifier = Modifier,
    shipColor: Color = MineItPalette.Text,
    colonyColor: Color = MineItPalette.Text,
) {
    Surface(
        color = MineItPalette.Control,
        shape = RoundedCornerShape(MineItRadius.Small),
        border = BorderStroke(1.dp, MineItPalette.Line),
        modifier = modifier,
    ) {
        Column(
            Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MineItPalette.Muted, maxLines = 1)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                CompactValue("S", ship, shipColor, Modifier.weight(1f))
                CompactValue("C", colony, colonyColor, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun CompactResourceCard(
    label: String,
    category: ResourceCategory,
    establishment: ColonyEstablishmentAssessment,
    colonyDetail: String,
    shipDetail: String?,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    val split = establishment.resourceSplit.getValue(category)
    Surface(
        color = accent.copy(alpha = .10f),
        shape = RoundedCornerShape(MineItRadius.Small),
        border = BorderStroke(1.dp, accent.copy(alpha = .28f)),
        modifier = modifier,
    ) {
        Column(
            Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = accent, maxLines = 1)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                CompactValue(
                    prefix = "S",
                    value = buildString {
                        append(format(split.ship))
                        shipDetail?.let { append(" · ").append(it) }
                    },
                    color = MineItPalette.Text,
                    modifier = Modifier.weight(1f),
                )
                CompactValue(
                    prefix = "C",
                    value = "${format(split.colony)} · $colonyDetail",
                    color = MineItPalette.Text,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun CompactValue(prefix: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Text(
        text = "$prefix $value",
        modifier = modifier,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

private fun hudAssessment(state: GameState, metrics: ColonyMetrics, network: ColonyNetworkSnapshot): ColonyEstablishmentAssessment {
    val colony = state.activeColony
    val ship = colony.foundingShipId?.let { id -> state.fleet.ships.firstOrNull { it.id == id } }
    val housing = colony.world.tiles.mapNotNull { it.development }
        .filter { it.kind == DevelopmentKind.HOUSING && it.constructionComplete && !it.productionStopped }
        .sumOf(InfrastructureRules::capacity)
    val split = ResourceCategory.entries.associateWith { category ->
        EstablishmentResourceSplit(ship?.inventory?.amountFor(category) ?: 0.0, colony.inventory.amountFor(category))
    }
    return ColonyEstablishmentAssessment(
        required = colony.foundingShipId != null && !colony.headquarters.commandHandoverComplete,
        acknowledged = colony.establishmentAcknowledged,
        phase = if (metrics.planetaryResidents <= .0001) EstablishmentPhase.SHIP else if (metrics.shipResidents > .0001) EstablishmentPhase.HYBRID else EstablishmentPhase.COLONY,
        foundingShipId = ship?.id,
        foundingShipName = ship?.name,
        shipResidents = metrics.shipResidents,
        planetaryResidents = metrics.planetaryResidents,
        planetaryAccommodationResidents = colony.planetaryAccommodationResidents,
        shipAccommodationCapacity = ship?.accommodationCapacity ?: 0,
        shipCrew = ship?.crew ?: 0,
        shipMinimumCrew = ship?.minimumCrew ?: 0,
        shipFoodAvailable = metrics.shipFoodAvailable,
        shipFoodDaysRemaining = metrics.shipFoodShortestDays,
        housingCapacity = housing,
        resourceSplit = split,
        support = emptyMap(),
    )
}

private fun supplyLabel(days: Double?, production: Double, demand: Double): String = when {
    demand <= .0001 -> "stable"
    days == null -> "+${format(production)}/d"
    else -> daysLabel(days)
}

private fun daysLabel(days: Double): String = when {
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
