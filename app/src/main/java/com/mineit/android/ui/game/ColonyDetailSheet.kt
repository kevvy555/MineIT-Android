package com.mineit.android.ui.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mineit.android.domain.colony.ColonyNetworkSnapshot
import com.mineit.android.domain.colony.CommandSourceType
import com.mineit.android.domain.colony.HeadquartersContinuityPhase
import com.mineit.android.domain.colony.HeadquartersDepartureGate
import com.mineit.android.domain.colony.SpaceportStatus
import com.mineit.android.domain.model.GameState
import com.mineit.android.domain.simulation.ColonyMetrics
import com.mineit.android.ui.design.MineItPalette
import com.mineit.android.ui.design.MineItPanel
import com.mineit.android.ui.design.MineItRadius
import com.mineit.android.ui.design.MineItSecondaryButton
import com.mineit.android.ui.design.MineItSectionHeader
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
    var showSystemDetail by remember { mutableStateOf(false) }

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
            ColonyControlHero(
                colonyName = colony.name,
                contractName = colony.contract?.name ?: "Contract 01",
                date = "Y${state.date.year} D${state.date.day}",
                presentation = presentation,
            )

            presentation.alerts.forEach { alert ->
                MineItPanel {
                    Text(
                        alert,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (alert.startsWith("CONGLOMERATE NETWORK OFFLINE")) MineItPalette.Critical else MineItPalette.Warning,
                    )
                }
            }

            MineItSectionHeader("OVERVIEW")
            AdaptiveMetricGrid(presentation.summary)

            MineItSectionHeader("OPERATIONS")
            OperationGrid(presentation.operations)

            KoplinLinkPanel(
                online = network.continuity.networkAvailable,
                detail = network.continuity.reason,
            )

            MineItSecondaryButton(
                text = if (showSystemDetail) "HIDE SYSTEM DETAIL" else "SYSTEM DETAIL",
                onClick = { showSystemDetail = !showSystemDetail },
                selected = showSystemDetail,
                modifier = Modifier.fillMaxWidth(),
            )

            if (showSystemDetail) {
                presentation.sections.forEach { section ->
                    OperationalCard(section)
                }
            }
        }
    }
}

@Composable
private fun ColonyControlHero(
    colonyName: String,
    contractName: String,
    date: String,
    presentation: ColonyDetailModel,
) {
    MineItPanel(raised = true) {
        Text(
            presentation.kicker,
            style = MaterialTheme.typography.labelSmall,
            color = MineItPalette.Accent,
            fontWeight = FontWeight.Bold,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MineItSpacing.Md),
        ) {
            Column(Modifier.weight(1f)) {
                Text(colonyName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "$contractName • $date",
                    style = MaterialTheme.typography.bodySmall,
                    color = MineItPalette.Muted,
                )
            }
            MineItStatusBadge(presentation.controlStatus, toneColor(presentation.controlTone))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MineItSpacing.Xs),
        ) {
            presentation.badges.take(2).forEach { badge ->
                MineItStatusBadge(badge, MineItPalette.Accent)
            }
        }
    }
}

@Composable
private fun OperationGrid(operations: List<ColonyControlOperation>) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val columns = if (maxWidth < 370.dp) 1 else 2
        Column(verticalArrangement = Arrangement.spacedBy(MineItSpacing.Sm)) {
            operations.chunked(columns).forEach { rowOperations ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MineItSpacing.Sm),
                ) {
                    rowOperations.forEach { operation ->
                        OperationTile(operation, Modifier.weight(1f))
                    }
                    repeat(columns - rowOperations.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun OperationTile(operation: ColonyControlOperation, modifier: Modifier = Modifier) {
    val accent = toneColor(operation.tone)
    Surface(
        modifier = modifier,
        color = MineItPalette.Control,
        shape = RoundedCornerShape(MineItRadius.Small),
        border = BorderStroke(1.dp, MineItPalette.Line),
    ) {
        Column(
            Modifier.padding(horizontal = 6.dp, vertical = 5.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MineItSpacing.Xs),
            ) {
                Text(
                    operation.title,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelSmall,
                    color = MineItPalette.Muted,
                    fontWeight = FontWeight.Bold,
                )
                MineItStatusBadge(operation.status, accent)
            }
            Text(operation.value, style = MaterialTheme.typography.titleSmall, color = accent)
            Text(operation.detail, style = MaterialTheme.typography.labelSmall, color = MineItPalette.Muted)
        }
    }
}

@Composable
private fun KoplinLinkPanel(online: Boolean, detail: String) {
    MineItPanel {
        MineItSectionHeader(
            title = "KOPLIN DEEP REACH CORPORATION",
            trailing = if (online) "LINK ONLINE" else "LINK OFFLINE",
            color = if (online) MineItPalette.Success else MineItPalette.Critical,
        )
        Text(
            "External charter node KPL-CN08 • conglomerate services follow the colony command network.",
            style = MaterialTheme.typography.labelSmall,
            color = MineItPalette.Muted,
        )
        if (!online) {
            Text(detail, style = MaterialTheme.typography.labelSmall, color = MineItPalette.Critical)
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
        DetailMetricGrid(section.metrics)
        section.note?.let {
            Text(it, style = MaterialTheme.typography.labelSmall, color = MineItPalette.Muted)
        }
    }
}

@Composable
private fun AdaptiveMetricGrid(metrics: List<ColonyDetailMetric>) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val columns = if (maxWidth < 370.dp) 2 else 4
        MetricRows(metrics, columns)
    }
}

@Composable
private fun DetailMetricGrid(metrics: List<ColonyDetailMetric>) {
    MetricRows(metrics, 2)
}

@Composable
private fun MetricRows(metrics: List<ColonyDetailMetric>, columns: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(MineItSpacing.Sm)) {
        metrics.chunked(columns).forEach { rowMetrics ->
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
                repeat(columns - rowMetrics.size) { Spacer(Modifier.weight(1f)) }
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

data class ColonyControlOperation(
    val title: String,
    val status: String,
    val value: String,
    val detail: String,
    val tone: ColonyDetailTone,
)

data class ColonyDetailSection(
    val title: String,
    val status: String,
    val tone: ColonyDetailTone,
    val metrics: List<ColonyDetailMetric>,
    val note: String? = null,
)

data class ColonyDetailModel(
    val kicker: String,
    val controlStatus: String,
    val controlTone: ColonyDetailTone,
    val badges: List<String>,
    val summary: List<ColonyDetailMetric>,
    val operations: List<ColonyControlOperation>,
    val sections: List<ColonyDetailSection>,
    val alerts: List<String>,
)

/** Pure presentation mapping for the compact web Colony Control hierarchy. */
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
        val commandSource = network.headquarters.sourceType
        val controlStatus = when {
            !network.continuity.networkAvailable -> "LINK OFFLINE"
            commandSource == CommandSourceType.SHIP -> "SHIP COMMAND"
            commandSource == CommandSourceType.HEADQUARTERS -> "ACTIVE"
            else -> "NO COMMAND"
        }
        val controlTone = when {
            !network.continuity.networkAvailable -> ColonyDetailTone.CRITICAL
            commandSource == null -> ColonyDetailTone.WARNING
            else -> factorTone(commandFactor)
        }

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

        val powerStatus = if (powerLimited) "LIMITED" else "AVAILABLE"
        val workforceStatus = if (workforceShortfall) "SHORTFALL" else "AVAILABLE"
        val industryStatus = when {
            industryOverloaded -> "OVERLOADED"
            industryFactor < .999 -> "THROTTLED"
            else -> "AVAILABLE"
        }
        val operations = listOf(
            ColonyControlOperation(
                title = "COMMAND",
                status = if (network.continuity.networkAvailable) "ONLINE" else "OFFLINE",
                value = "${format(network.headquarters.load)} / ${format(network.headquarters.capacity)}",
                detail = "${commandSource?.name ?: "UNLINKED"} • ${percent(commandFactor)} efficiency",
                tone = if (network.continuity.networkAvailable) factorTone(commandFactor) else ColonyDetailTone.CRITICAL,
            ),
            ColonyControlOperation(
                title = "POWER",
                status = powerStatus,
                value = "${format(metrics.powerFuelLimitedGeneration)} / ${format(metrics.powerDemand)}",
                detail = "${format(metrics.powerCapacity)} installed • ${format(metrics.powerFuelBurn)}/d Fuel",
                tone = if (powerLimited) factorTone(metrics.powerFactor) else ColonyDetailTone.GOOD,
            ),
            ColonyControlOperation(
                title = "WORKFORCE",
                status = workforceStatus,
                value = "${format(network.workforceAvailable)} / ${format(network.workforceRequired)}",
                detail = "${format(max(0.0, network.workforceAvailable - network.workforceRequired))} free",
                tone = if (workforceShortfall) factorTone(workforceFactor) else ColonyDetailTone.GOOD,
            ),
            ColonyControlOperation(
                title = "INDUSTRY",
                status = industryStatus,
                value = "${format(network.industryLoad)} / ${format(network.industryCapacity)}",
                detail = "${format(network.industryInstalled)} installed",
                tone = if (industryOverloaded || industryFactor < .999) factorTone(industryFactor) else ColonyDetailTone.GOOD,
            ),
            ColonyControlOperation(
                title = "SPACEPORT",
                status = if (spaceport.operational) "ONLINE" else "OFFLINE",
                value = "${spaceport.berths} BERTHS • ${spaceport.serviceSlots} SLOTS",
                detail = "${spaceport.cargoPerDay} cargo/d • ${spaceport.passengersPerDay} people/d",
                tone = if (spaceport.operational) ColonyDetailTone.GOOD else ColonyDetailTone.CRITICAL,
            ),
        )

        val sections = listOf(
            ColonyDetailSection(
                title = "POWER NETWORK",
                status = powerStatus,
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
                    "Generation covers colony demand; generator Fuel burn is charged from beginning-of-day stock."
                },
            ),
            ColonyDetailSection(
                title = "OPERATIONAL WORKFORCE",
                status = workforceStatus,
                tone = if (workforceShortfall) factorTone(workforceFactor) else ColonyDetailTone.GOOD,
                metrics = listOf(
                    ColonyDetailMetric("AVAILABLE", format(network.workforceAvailable)),
                    ColonyDetailMetric("REQUIRED", format(network.workforceRequired)),
                    ColonyDetailMetric("FREE", format(max(0.0, network.workforceAvailable - network.workforceRequired))),
                    ColonyDetailMetric("COMMERCIAL", percent(network.workforceCommercialFactor), factorTone(network.workforceCommercialFactor)),
                    ColonyDetailMetric("SURVIVAL", percent(network.workforceSurvivalFactor), factorTone(network.workforceSurvivalFactor)),
                    ColonyDetailMetric("IND STAFF", percent(network.industryStaffFactor), factorTone(network.industryStaffFactor)),
                ),
                note = "Food and Fuel receive worker priority. Build and Ore are throttled when workers become scarce.",
            ),
            ColonyDetailSection(
                title = "INDUSTRIAL CAPACITY",
                status = industryStatus,
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
                    "Food and Fuel keep priority. Build and Ore are reduced until Industry, staffing or Power improves."
                } else {
                    "Developed sites consume Industrial Capacity; spare capacity runs commercial Build and Ore operations."
                },
            ),
            ColonyDetailSection(
                title = "COMMAND NETWORK",
                status = if (network.continuity.networkAvailable) "ONLINE" else "OFFLINE",
                tone = if (network.continuity.networkAvailable) factorTone(commandFactor) else ColonyDetailTone.CRITICAL,
                metrics = listOf(
                    ColonyDetailMetric("SOURCE", commandSource?.name ?: "NONE"),
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
            when (network.continuity.phase) {
                HeadquartersContinuityPhase.OUTAGE -> add("CONGLOMERATE NETWORK OFFLINE — ${network.continuity.reason}")
                HeadquartersContinuityPhase.RECOVERY -> add("HEADQUARTERS RECOVERY — ${network.continuity.reason}")
                HeadquartersContinuityPhase.ONLINE -> Unit
            }
            if (!departureGate.ok) {
                add("FOUNDING-SHIP HANDOVER BLOCKED — ${departureGate.failures.joinToString(" • ")}")
            }
        }

        return ColonyDetailModel(
            kicker = if (commandSource == CommandSourceType.HEADQUARTERS) "COLONY CONTROL • HEADQUARTERS" else "COLONY CONTROL",
            controlStatus = controlStatus,
            controlTone = controlTone,
            badges = listOf("COLONY SERVICES", commandSource?.name ?: "UNLINKED"),
            summary = summary,
            operations = operations,
            sections = sections,
            alerts = alerts,
        )
    }

    private fun factorTone(value: Double): ColonyDetailTone = when {
        value >= .999 -> ColonyDetailTone.GOOD
        value >= .7 -> ColonyDetailTone.WARNING
        else -> ColonyDetailTone.CRITICAL
    }
}
