package com.mineit.android.ui.game

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.mineit.android.domain.colony.ColonyNetworkSnapshot
import com.mineit.android.domain.colony.HeadquartersDepartureGate
import com.mineit.android.domain.colony.SpaceportStatus
import com.mineit.android.domain.model.GameState
import com.mineit.android.domain.simulation.ColonyMetrics
import com.mineit.android.ui.design.MineItPalette
import com.mineit.android.ui.design.MineItPanel
import com.mineit.android.ui.design.MineItSpacing
import com.mineit.android.ui.design.MineItStat
import com.mineit.android.ui.design.MineItStatusBadge
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColonyDetailSheet(
    state: GameState,
    metrics: ColonyMetrics,
    network: ColonyNetworkSnapshot,
    spaceport: SpaceportStatus,
    departureGate: HeadquartersDepartureGate,
    onDismiss: () -> Unit,
) {
    val colony = state.activeColony
    val presentation = ColonyDetailPresentation.build(
        population = colony.population,
        metrics = metrics,
        network = network,
        spaceport = spaceport,
        departureGate = departureGate,
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MineItPalette.Background,
        contentColor = MineItPalette.Text,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MineItSpacing.Lg, vertical = MineItSpacing.Md),
            verticalArrangement = Arrangement.spacedBy(MineItSpacing.Sm),
        ) {
            MineItPanel {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(MineItSpacing.Md),
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(colony.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${colony.contract?.name ?: "Contract 01"} • Y${state.date.year} D${state.date.day}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MineItPalette.Muted,
                        )
                    }
                    MineItStatusBadge(
                        text = colony.status.name.replace('_', ' '),
                        color = if (colony.status.name == "PLAYING") MineItPalette.Success else MineItPalette.Warning,
                    )
                }
            }

            MetricGrid(presentation.summary)

            presentation.alerts.forEach { alert ->
                MineItPanel {
                    Text(
                        alert,
                        style = MaterialTheme.typography.labelSmall,
                        color = MineItPalette.Warning,
                    )
                }
            }

            presentation.sections.forEach { section ->
                OperationalCard(section)
            }
        }
    }
}

@Composable
private fun OperationalCard(section: ColonyDetailSection) {
    MineItPanel {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MineItSpacing.Sm),
        ) {
            Text(
                section.title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleSmall,
                color = MineItPalette.Accent,
            )
            MineItStatusBadge(section.status, toneColor(section.tone))
        }
        MetricGrid(section.metrics)
        section.note?.let {
            Text(it, style = MaterialTheme.typography.labelSmall, color = MineItPalette.Muted)
        }
    }
}

@Composable
private fun MetricGrid(metrics: List<ColonyDetailMetric>) {
    Column(verticalArrangement = Arrangement.spacedBy(MineItSpacing.Sm)) {
        metrics.chunked(2).forEach { rowMetrics ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MineItSpacing.Sm),
            ) {
                rowMetrics.forEach { metric ->
                    MineItStat(
                        label = metric.label,
                        value = metric.value,
                        modifier = Modifier.weight(1f),
                        stateColor = toneColor(metric.tone),
                    )
                }
                if (rowMetrics.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

private fun toneColor(tone: ColonyDetailTone): Color = when (tone) {
    ColonyDetailTone.NORMAL -> MineItPalette.Text
    ColonyDetailTone.GOOD -> MineItPalette.Success
    ColonyDetailTone.WARNING -> MineItPalette.Warning
    ColonyDetailTone.CRITICAL -> MineItPalette.Critical
}

enum class ColonyDetailTone { NORMAL, GOOD, WARNING, CRITICAL }

data class ColonyDetailMetric(
    val label: String,
    val value: String,
    val tone: ColonyDetailTone = ColonyDetailTone.NORMAL,
)

data class ColonyDetailSection(
    val title: String,
    val status: String,
    val tone: ColonyDetailTone,
    val metrics: List<ColonyDetailMetric>,
    val note: String? = null,
)

data class ColonyDetailModel(
    val summary: List<ColonyDetailMetric>,
    val sections: List<ColonyDetailSection>,
    val alerts: List<String>,
)

/** Pure presentation mapping so the web-derived Colony Details hierarchy stays regression-testable. */
object ColonyDetailPresentation {
    fun build(
        population: Double,
        metrics: ColonyMetrics,
        network: ColonyNetworkSnapshot,
        spaceport: SpaceportStatus,
        departureGate: HeadquartersDepartureGate,
    ): ColonyDetailModel {
        val workforceFactor = minOf(network.workforceSurvivalFactor, network.workforceCommercialFactor)
        val industryFactor = minOf(
            network.industryCommercialFactor,
            network.industryStaffFactor,
            network.industryPowerFactor,
        )
        val commandFactor = network.continuity.effectiveCommandEfficiency
        val workforceShortfall = network.workforceAvailable + .001 < network.workforceRequired
        val industryOverloaded = network.industryLoad > network.industryCapacity + .001
        val powerLimited = metrics.powerFactor < .999

        val summary = listOf(
            ColonyDetailMetric("POPULATION", format(population)),
            ColonyDetailMetric(
                "POWER",
                "${format(metrics.powerFuelLimitedGeneration)}/${format(metrics.powerDemand)}",
                factorTone(metrics.powerFactor),
            ),
            ColonyDetailMetric(
                "WORKFORCE",
                "${format(network.workforceAvailable)}/${format(network.workforceRequired)}",
                factorTone(workforceFactor),
            ),
            ColonyDetailMetric(
                "INDUSTRY",
                "${format(network.industryCapacity)}/${format(network.industryInstalled)}",
                factorTone(industryFactor),
            ),
            ColonyDetailMetric(
                "COMMAND",
                "${format(network.headquarters.load)}/${format(network.headquarters.capacity)}",
                if (network.continuity.networkAvailable) factorTone(commandFactor) else ColonyDetailTone.CRITICAL,
            ),
            ColonyDetailMetric(
                "SPACEPORT",
                if (spaceport.operational) "ONLINE" else "OFFLINE",
                if (spaceport.operational) ColonyDetailTone.GOOD else ColonyDetailTone.CRITICAL,
            ),
        )

        val sections = listOf(
            ColonyDetailSection(
                title = "POWER NETWORK",
                status = if (powerLimited) "LIMITED" else "AVAILABLE",
                tone = if (powerLimited) factorTone(metrics.powerFactor) else ColonyDetailTone.GOOD,
                metrics = listOf(
                    ColonyDetailMetric("INSTALLED", format(metrics.powerCapacity)),
                    ColonyDetailMetric("FUEL-LIMIT", format(metrics.powerFuelLimitedGeneration), factorTone(metrics.powerFactor)),
                    ColonyDetailMetric("DEMAND", format(metrics.powerDemand)),
                    ColonyDetailMetric("FUEL BURN", "${format(metrics.powerFuelBurn)}/day"),
                    ColonyDetailMetric("LIFE SUPPORT", percent(metrics.lifeSupportPowerFactor), factorTone(metrics.lifeSupportPowerFactor)),
                    ColonyDetailMetric("INDUSTRY", percent(metrics.industryPowerFactor), factorTone(metrics.industryPowerFactor)),
                ),
                note = if (powerLimited) {
                    "Power is allocated by priority. Life Support is protected before commercial Industry."
                } else {
                    "Generation currently covers colony demand; generator Fuel burn is charged from beginning-of-day stock."
                },
            ),
            ColonyDetailSection(
                title = "OPERATIONAL WORKFORCE",
                status = if (workforceShortfall) "SHORTFALL" else "AVAILABLE",
                tone = if (workforceShortfall) factorTone(workforceFactor) else ColonyDetailTone.GOOD,
                metrics = listOf(
                    ColonyDetailMetric("AVAILABLE", format(network.workforceAvailable)),
                    ColonyDetailMetric("REQUIRED", format(network.workforceRequired)),
                    ColonyDetailMetric("FREE", format(max(0.0, network.workforceAvailable - network.workforceRequired))),
                    ColonyDetailMetric("COMMERCIAL", percent(network.workforceCommercialFactor), factorTone(network.workforceCommercialFactor)),
                    ColonyDetailMetric("SURVIVAL", percent(network.workforceSurvivalFactor), factorTone(network.workforceSurvivalFactor)),
                    ColonyDetailMetric("IND STAFF", percent(network.industryStaffFactor), factorTone(network.industryStaffFactor)),
                ),
                note = "Food and Fuel receive worker priority. Build and Ore are automatically throttled when workers become scarce.",
            ),
            ColonyDetailSection(
                title = "INDUSTRIAL CAPACITY",
                status = when {
                    industryOverloaded -> "OVERLOADED"
                    industryFactor < .999 -> "THROTTLED"
                    else -> "AVAILABLE"
                },
                tone = if (industryOverloaded || industryFactor < .999) factorTone(industryFactor) else ColonyDetailTone.GOOD,
                metrics = listOf(
                    ColonyDetailMetric("LOAD / CAP", "${format(network.industryLoad)} / ${format(network.industryCapacity)}"),
                    ColonyDetailMetric("HEADROOM", format(max(0.0, network.industryCapacity - network.industryLoad))),
                    ColonyDetailMetric("INSTALLED", format(network.industryInstalled)),
                    ColonyDetailMetric("STAFFING", percent(network.industryStaffFactor), factorTone(network.industryStaffFactor)),
                    ColonyDetailMetric("SURVIVAL", percent(network.industrySurvivalFactor), factorTone(network.industrySurvivalFactor)),
                    ColonyDetailMetric("BUILD / ORE", percent(network.industryCommercialFactor), factorTone(network.industryCommercialFactor)),
                ),
                note = if (industryOverloaded || industryFactor < .999) {
                    "Food and Fuel keep priority. Build and Ore are reduced until Industrial Capacity, staffing or Power improves."
                } else {
                    "Developed sites consume Industrial Capacity; spare capacity runs commercial Build and Ore operations."
                },
            ),
            ColonyDetailSection(
                title = "COMMAND NETWORK",
                status = if (network.continuity.networkAvailable) "ONLINE" else "OFFLINE",
                tone = if (network.continuity.networkAvailable) factorTone(commandFactor) else ColonyDetailTone.CRITICAL,
                metrics = listOf(
                    ColonyDetailMetric("SOURCE", network.headquarters.sourceType?.name ?: "NONE"),
                    ColonyDetailMetric("LOAD / CAP", "${format(network.headquarters.load)} / ${format(network.headquarters.capacity)}"),
                    ColonyDetailMetric("EFFICIENCY", percent(commandFactor), factorTone(commandFactor)),
                    ColonyDetailMetric(
                        "NETWORK",
                        if (network.continuity.networkAvailable) "ONLINE" else "OFFLINE",
                        if (network.continuity.networkAvailable) ColonyDetailTone.GOOD else ColonyDetailTone.CRITICAL,
                    ),
                    ColonyDetailMetric("CONTINUITY", network.continuity.phase.name.replace('_', ' ')),
                    ColonyDetailMetric(
                        "HANDOVER",
                        if (departureGate.ok) "READY" else "BLOCKED",
                        if (departureGate.ok) ColonyDetailTone.GOOD else ColonyDetailTone.WARNING,
                    ),
                ),
                note = network.continuity.reason,
            ),
            ColonyDetailSection(
                title = "SPACEPORT",
                status = if (spaceport.operational) "ONLINE" else "OFFLINE",
                tone = if (spaceport.operational) ColonyDetailTone.GOOD else ColonyDetailTone.CRITICAL,
                metrics = listOf(
                    ColonyDetailMetric("POWER", percent(spaceport.powerFactor), factorTone(spaceport.powerFactor)),
                    ColonyDetailMetric("BERTHS / SLOTS", "${spaceport.berths} / ${spaceport.serviceSlots}"),
                    ColonyDetailMetric("CARGO / DAY", spaceport.cargoPerDay.toString()),
                    ColonyDetailMetric("PEOPLE / DAY", spaceport.passengersPerDay.toString()),
                ),
                note = spaceport.reason,
            ),
        )

        val alerts = buildList {
            if (!departureGate.ok) {
                add("FOUNDING-SHIP HANDOVER BLOCKED — ${departureGate.failures.joinToString(" • ")}")
            }
        }

        return ColonyDetailModel(summary = summary, sections = sections, alerts = alerts)
    }

    private fun factorTone(value: Double): ColonyDetailTone = when {
        value >= .999 -> ColonyDetailTone.GOOD
        value >= .7 -> ColonyDetailTone.WARNING
        else -> ColonyDetailTone.CRITICAL
    }
}
