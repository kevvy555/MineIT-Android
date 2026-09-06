package com.mineit.android.ui.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
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
import com.mineit.android.domain.colony.DevelopmentPreview
import com.mineit.android.domain.colony.HeadquartersDepartureGate
import com.mineit.android.domain.colony.SpaceportStatus
import com.mineit.android.domain.model.GameState
import com.mineit.android.domain.simulation.ColonyMetrics
import com.mineit.android.domain.world.WorldTile
import com.mineit.android.ui.design.MineItDestructiveButton
import com.mineit.android.ui.design.MineItPalette
import com.mineit.android.ui.design.MineItPanel
import com.mineit.android.ui.design.MineItPrimaryButton
import com.mineit.android.ui.design.MineItRadius
import com.mineit.android.ui.design.MineItSecondaryButton
import com.mineit.android.ui.design.MineItSectionHeader
import com.mineit.android.ui.design.MineItSpacing
import com.mineit.android.ui.design.MineItStatusBadge

/**
 * Consolidated Colony Control surface. Once a Primary Headquarters exists, colony-wide status lives
 * here rather than behind a separate COLONY / DETAILS strip on the map screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeadquartersColonyControlSheet(
    state: GameState,
    metrics: ColonyMetrics,
    network: ColonyNetworkSnapshot,
    spaceport: SpaceportStatus,
    departureGate: HeadquartersDepartureGate,
    tile: WorldTile,
    upgradePreview: DevelopmentPreview?,
    statusMessage: String?,
    onSetPrimary: () -> Unit,
    onUpgrade: () -> Unit,
    onDemolish: () -> Unit,
    onDismiss: () -> Unit,
) {
    val headquarters = HeadquartersControlPresentation.build(state, network, departureGate, tile, upgradePreview)
    val colony = ColonyDetailPresentation.build(
        population = state.activeColony.population,
        metrics = metrics,
        network = network,
        spaceport = spaceport,
        departureGate = departureGate,
    )
    var confirmDemolition by remember(tile.coordinate) { mutableStateOf(false) }

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
            CombinedHero(
                colonyName = state.activeColony.name,
                contractName = state.activeColony.contract?.name ?: "Contract 01",
                headquarters = headquarters,
                colony = colony,
            )

            colony.alerts.forEach { alert ->
                CombinedNotice(
                    title = if (alert.contains("OFFLINE")) "COLONY ALERT" else "ATTENTION",
                    text = alert,
                    tone = if (alert.contains("OFFLINE")) ColonyDetailTone.CRITICAL else ColonyDetailTone.WARNING,
                )
            }
            headquarters.alert?.let { CombinedNotice(it.title, it.text, it.tone) }
            statusMessage?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.labelSmall, color = MineItPalette.Muted)
            }

            MineItSectionHeader("COLONY OVERVIEW")
            CombinedMetricGrid(
                colony.summary.map { CombinedMetric(it.label, it.value, null, it.tone) },
                columns = 3,
            )

            MineItSectionHeader("COLONY OPERATIONS")
            CombinedMetricGrid(
                colony.operations.map {
                    CombinedMetric(
                        label = it.title,
                        value = it.value,
                        detail = "${it.status} • ${it.detail}",
                        tone = it.tone,
                    )
                },
                columns = 2,
            )

            MineItSectionHeader("HEADQUARTERS")
            CombinedMetricGrid(
                headquarters.overview.map { CombinedMetric(it.label, it.value, it.detail, it.tone) },
                columns = 2,
            )

            MineItSectionHeader("COMMAND NETWORK")
            CombinedMetricGrid(
                headquarters.operations.map { CombinedMetric(it.label, it.value, it.detail, it.tone) },
                columns = 2,
            )

            CombinedNotice(
                title = "FOUNDING-SHIP HANDOVER • ${headquarters.handoverStatus}",
                text = headquarters.handoverDetail,
                tone = headquarters.handoverTone,
            )
            if (headquarters.primary && headquarters.handoverStatus != "COMPLETE") {
                MineItPrimaryButton(
                    text = if (departureGate.ok) "COMPLETE COMMAND HANDOVER" else "HANDOVER BLOCKED",
                    onClick = onSetPrimary,
                    enabled = departureGate.ok,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            MineItPanel {
                MineItSectionHeader(
                    title = "KOPLIN DEEP REACH CORPORATION",
                    trailing = if (network.continuity.networkAvailable) "LINK ONLINE" else "LINK OFFLINE",
                    color = if (network.continuity.networkAvailable) MineItPalette.Success else MineItPalette.Critical,
                )
                Text(
                    "External charter node KPL-CN08 • corporate services use the operational colony command link.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MineItPalette.Muted,
                )
                if (!network.continuity.networkAvailable) {
                    Text(network.continuity.reason, style = MaterialTheme.typography.labelSmall, color = MineItPalette.Critical)
                }
            }

            MineItSectionHeader("HEADQUARTERS UPGRADE")
            if (headquarters.maxLevel) {
                CombinedNotice("MAX LEVEL", "Headquarters has reached L5.", ColonyDetailTone.GOOD)
            } else {
                CombinedMetricGrid(
                    listOf(
                        CombinedMetric("NEXT LEVEL", "HQ L${headquarters.nextLevel}"),
                        CombinedMetric("COMMAND GAIN", "+${format(headquarters.capacityGain)}"),
                    ),
                    columns = 2,
                )
                MineItSectionHeader("REQUIREMENTS")
                CombinedMetricGrid(
                    headquarters.requirements.map { CombinedMetric(it.label, it.value, it.detail, it.tone) },
                    columns = 2,
                )
                CombinedNotice(
                    title = if (headquarters.upgradeReady) "READY TO UPGRADE" else "UPGRADE BLOCKED",
                    text = if (headquarters.upgradeReady) "All Headquarters upgrade requirements are satisfied." else headquarters.upgradeReason,
                    tone = if (headquarters.upgradeReady) ColonyDetailTone.GOOD else ColonyDetailTone.WARNING,
                )
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(MineItSpacing.Sm)) {
                if (!headquarters.primary) {
                    MineItSecondaryButton(
                        text = if (headquarters.primaryEligible) "SET PRIMARY" else "PRIMARY BLOCKED",
                        onClick = onSetPrimary,
                        enabled = headquarters.primaryEligible,
                        modifier = Modifier.weight(1f),
                    )
                }
                MineItPrimaryButton(
                    text = when {
                        headquarters.maxLevel -> "MAX LEVEL"
                        headquarters.upgradeReady -> "UPGRADE TO L${headquarters.nextLevel}"
                        else -> "UPGRADE BLOCKED"
                    },
                    onClick = onUpgrade,
                    enabled = headquarters.upgradeReady && !headquarters.maxLevel,
                    modifier = Modifier.weight(1f),
                )
            }

            MineItDestructiveButton(
                text = "DEMOLISH HEADQUARTERS",
                onClick = { confirmDemolition = true },
                modifier = Modifier.fillMaxWidth(),
            )
            MineItSecondaryButton("CLOSE", onDismiss, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.padding(bottom = MineItSpacing.Sm))
        }
    }

    if (confirmDemolition) {
        AlertDialog(
            onDismissRequest = { confirmDemolition = false },
            title = { Text(if (headquarters.primary) "DEMOLISH PRIMARY HEADQUARTERS?" else "DEMOLISH HEADQUARTERS?") },
            text = {
                Text(
                    when {
                        !headquarters.primary -> "This expansion Headquarters will stop contributing command capacity and bonus immediately."
                        headquarters.commandShipFallback -> "The docked command-capable ship can take emergency command, but the corporate network will be offline until a staffed Primary Headquarters is restored."
                        else -> "Command capacity will fall to zero and the corporate network will be offline until a staffed Primary Headquarters is restored."
                    },
                )
            },
            confirmButton = {
                MineItDestructiveButton(
                    "DEMOLISH",
                    onClick = {
                        confirmDemolition = false
                        onDemolish()
                    },
                )
            },
            dismissButton = { MineItSecondaryButton("CANCEL", { confirmDemolition = false }) },
        )
    }
}

@Composable
private fun CombinedHero(
    colonyName: String,
    contractName: String,
    headquarters: HeadquartersControlModel,
    colony: ColonyDetailModel,
) {
    MineItPanel(raised = true) {
        Text(
            "COLONY CONTROL • HEADQUARTERS",
            style = MaterialTheme.typography.labelSmall,
            color = MineItPalette.Accent,
            fontWeight = FontWeight.Bold,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MineItSpacing.Sm),
        ) {
            Column(Modifier.weight(1f)) {
                Text(colonyName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                Text(
                    "$contractName • Headquarters L${headquarters.level}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MineItPalette.Muted,
                )
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                MineItStatusBadge(colony.controlStatus, combinedToneColor(colony.controlTone))
                MineItStatusBadge(if (headquarters.primary) "PRIMARY" else "EXPANSION", MineItPalette.Accent)
            }
        }
    }
}

private data class CombinedMetric(
    val label: String,
    val value: String,
    val detail: String? = null,
    val tone: ColonyDetailTone = ColonyDetailTone.NORMAL,
)

@Composable
private fun CombinedMetricGrid(metrics: List<CombinedMetric>, columns: Int) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(MineItSpacing.Xs)) {
        metrics.chunked(columns).forEach { rowMetrics ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(MineItSpacing.Xs)) {
                rowMetrics.forEach { metric ->
                    Surface(
                        modifier = Modifier.weight(1f),
                        color = MineItPalette.Control,
                        border = BorderStroke(1.dp, MineItPalette.Line),
                        shape = RoundedCornerShape(MineItRadius.Small),
                    ) {
                        Column(Modifier.padding(horizontal = 6.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                            Text(metric.label, style = MaterialTheme.typography.labelSmall, color = MineItPalette.Muted)
                            Text(metric.value, style = MaterialTheme.typography.titleSmall, color = combinedToneColor(metric.tone), fontWeight = FontWeight.Black)
                            metric.detail?.let {
                                Text(it, style = MaterialTheme.typography.labelSmall, color = MineItPalette.Muted, maxLines = 2)
                            }
                        }
                    }
                }
                repeat(columns - rowMetrics.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun CombinedNotice(title: String, text: String, tone: ColonyDetailTone) {
    val color = combinedToneColor(tone)
    Surface(
        color = color.copy(alpha = .08f),
        border = BorderStroke(1.dp, color.copy(alpha = .45f)),
        shape = RoundedCornerShape(MineItRadius.Small),
    ) {
        Column(Modifier.fillMaxWidth().padding(MineItSpacing.Sm), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = color, fontWeight = FontWeight.Black)
            Text(text, style = MaterialTheme.typography.labelSmall, color = MineItPalette.Muted)
        }
    }
}

private fun combinedToneColor(tone: ColonyDetailTone): Color = when (tone) {
    ColonyDetailTone.NORMAL -> MineItPalette.Text
    ColonyDetailTone.GOOD -> MineItPalette.Success
    ColonyDetailTone.WARNING -> MineItPalette.Warning
    ColonyDetailTone.CRITICAL -> MineItPalette.Critical
}
