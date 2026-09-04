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
import com.mineit.android.domain.colony.SpaceportStatus
import com.mineit.android.domain.model.ColonyStatus
import com.mineit.android.domain.model.GameState
import com.mineit.android.domain.resources.ResourceCategory
import com.mineit.android.domain.simulation.ColonyMetrics
import com.mineit.android.domain.world.DevelopmentKind
import com.mineit.android.ui.design.MineItPalette
import com.mineit.android.ui.design.MineItRadius
import com.mineit.android.ui.design.MineItSpacing
import kotlin.math.max
import kotlin.math.roundToLong

@Composable
fun GameHeader(
    state: GameState,
    metrics: ColonyMetrics,
    network: ColonyNetworkSnapshot,
    spaceport: SpaceportStatus,
    onOpenColonyDetail: () -> Unit,
    modifier: Modifier = Modifier,
    establishment: ColonyEstablishmentAssessment = hudAssessment(state, metrics, network),
    onOpenEstablishment: (() -> Unit)? = null,
) {
    val colony = state.activeColony
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(MineItSpacing.Xs)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = MineItSpacing.Sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MineItSpacing.Md),
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

        OperationalHud(metrics, network, establishment)
        ResourceHud(metrics, establishment)

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(MineItSpacing.Xs)) {
            if (establishment.required && colony.status != ColonyStatus.SITE_SELECTION && onOpenEstablishment != null) {
                HeaderAction(
                    label = "HANDOVER",
                    value = establishment.phase.name,
                    onClick = onOpenEstablishment,
                    color = if (establishment.acknowledged) MineItPalette.Warning else MineItPalette.Accent,
                    modifier = Modifier.weight(1f),
                )
            }
            HeaderAction(
                label = "COLONY",
                value = if (spaceport.operational) "ONLINE" else "DETAILS",
                onClick = onOpenColonyDetail,
                color = if (spaceport.operational) MineItPalette.Success else MineItPalette.Warning,
                modifier = Modifier.weight(1f),
            )
        }
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
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(MineItSpacing.Xs)) {
        SplitCard(
            label = "HOUSING",
            ship = "${format(establishment.shipResidents)}/${format(establishment.shipAccommodationCapacity.toDouble())}",
            colony = "${format(establishment.planetaryAccommodationResidents)}/${format(establishment.housingCapacity)}",
            modifier = Modifier.weight(1f),
        )
        SplitCard(
            label = "POWER",
            ship = if (establishment.foundingShipId != null) "SELF" else "—",
            colony = "${format(network.fuelLimitedGeneration)}/${format(network.powerDemand)}",
            colonyColor = if (metrics.powerFactor >= .999) MineItPalette.Success else MineItPalette.Critical,
            modifier = Modifier.weight(1f),
        )
        SplitCard(
            label = "INDUSTRY",
            ship = "+${format(network.shipIndustry)}",
            colony = "${format(planetaryIndustry)}/${format(network.builtIndustry)}",
            colonyColor = if (network.industryPowerFactor >= .999) MineItPalette.Success else MineItPalette.Warning,
            modifier = Modifier.weight(1f),
        )
        SplitCard(
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
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(MineItSpacing.Xs)) {
        ResourceSplitCard("FOOD", ResourceCategory.FOOD, establishment, supplyLabel(metrics.foodDays, metrics.foodProduction, metrics.foodDemand), establishment.shipFoodDaysRemaining?.let(::daysLabel) ?: "stable", MineItPalette.Food, Modifier.weight(1f))
        ResourceSplitCard("BUILD", ResourceCategory.BUILD, establishment, "+${format(metrics.buildProduction)}/d", "aboard", MineItPalette.Build, Modifier.weight(1f))
        ResourceSplitCard("FUEL", ResourceCategory.FUEL, establishment, supplyLabel(metrics.fuelDays, metrics.fuelProduction, metrics.fuelDemand), "aboard", MineItPalette.Fuel, Modifier.weight(1f))
        ResourceSplitCard("ORE", ResourceCategory.ORE, establishment, supplyLabel(metrics.oreDays, metrics.oreProduction, metrics.oreDemand), "aboard", MineItPalette.Ore, Modifier.weight(1f))
    }
}

@Composable
private fun SplitCard(label: String, ship: String, colony: String, modifier: Modifier = Modifier, shipColor: Color = MineItPalette.Text, colonyColor: Color = MineItPalette.Text) {
    Surface(color = MineItPalette.Control, shape = RoundedCornerShape(MineItRadius.Small), border = BorderStroke(1.dp, MineItPalette.Line), modifier = modifier) {
        Column(Modifier.padding(horizontal = 4.dp, vertical = 3.dp), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MineItPalette.Muted, maxLines = 1)
            SplitRow("S", ship, shipColor)
            SplitRow("C", colony, colonyColor)
        }
    }
}

@Composable
private fun ResourceSplitCard(label: String, category: ResourceCategory, establishment: ColonyEstablishmentAssessment, colonyDetail: String, shipDetail: String, accent: Color, modifier: Modifier = Modifier) {
    val split = establishment.resourceSplit.getValue(category)
    Surface(color = accent.copy(alpha = .10f), shape = RoundedCornerShape(MineItRadius.Small), border = BorderStroke(1.dp, accent.copy(alpha = .28f)), modifier = modifier) {
        Column(Modifier.padding(horizontal = 4.dp, vertical = 3.dp), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = accent, maxLines = 1)
            SplitRow("S", format(split.ship), MineItPalette.Text, shipDetail)
            SplitRow("C", format(split.colony), MineItPalette.Text, colonyDetail)
        }
    }
}

@Composable
private fun SplitRow(prefix: String, value: String, color: Color, detail: String? = null) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(prefix, style = MaterialTheme.typography.labelSmall, color = MineItPalette.Muted)
        Column(horizontalAlignment = Alignment.End) {
            Text(value, style = MaterialTheme.typography.labelMedium, color = color, maxLines = 1)
            detail?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = MineItPalette.Muted, maxLines = 1) }
        }
    }
}

@Composable
private fun HeaderAction(label: String, value: String, onClick: () -> Unit, color: Color, modifier: Modifier = Modifier) {
    Surface(onClick = onClick, color = MineItPalette.Control, shape = RoundedCornerShape(MineItRadius.Small), border = BorderStroke(1.dp, MineItPalette.Line), modifier = modifier) {
        Row(Modifier.padding(horizontal = 7.dp, vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MineItPalette.Muted)
            Text(value, style = MaterialTheme.typography.labelMedium, color = color)
        }
    }
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
