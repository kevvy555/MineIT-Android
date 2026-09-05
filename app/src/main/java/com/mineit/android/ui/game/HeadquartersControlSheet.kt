package com.mineit.android.ui.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.mineit.android.domain.colony.ColonyNetworkSnapshot
import com.mineit.android.domain.colony.CommandSourceType
import com.mineit.android.domain.colony.DevelopmentPreview
import com.mineit.android.domain.colony.HeadquartersContinuityPhase
import com.mineit.android.domain.colony.HeadquartersDepartureGate
import com.mineit.android.domain.colony.InfrastructureRules
import com.mineit.android.domain.model.GameState
import com.mineit.android.domain.resources.ResourceCategory
import com.mineit.android.domain.world.DevelopmentKind
import com.mineit.android.domain.world.WorldTile
import com.mineit.android.ui.art.MineItAssetPaths
import com.mineit.android.ui.art.rememberMineItAssetBitmap
import com.mineit.android.ui.design.MineItDestructiveButton
import com.mineit.android.ui.design.MineItPalette
import com.mineit.android.ui.design.MineItPanel
import com.mineit.android.ui.design.MineItPrimaryButton
import com.mineit.android.ui.design.MineItRadius
import com.mineit.android.ui.design.MineItSecondaryButton
import com.mineit.android.ui.design.MineItSectionHeader
import com.mineit.android.ui.design.MineItSpacing
import com.mineit.android.ui.design.MineItStatusBadge
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeadquartersControlSheet(
    state: GameState,
    network: ColonyNetworkSnapshot,
    departureGate: HeadquartersDepartureGate,
    tile: WorldTile,
    upgradePreview: DevelopmentPreview?,
    statusMessage: String?,
    onSetPrimary: () -> Unit,
    onUpgrade: () -> Unit,
    onDemolish: () -> Unit,
    onDismiss: () -> Unit,
) {
    val model = HeadquartersControlPresentation.build(state, network, departureGate, tile, upgradePreview)
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
            HeadquartersHero(tile, model)

            model.alert?.let { HeadquartersNotice(it.title, it.text, it.tone) }
            statusMessage?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.labelSmall, color = MineItPalette.Muted)
            }

            MineItSectionHeader("OVERVIEW")
            HeadquartersMetricGrid(model.overview)

            MineItSectionHeader("OPERATIONS")
            HeadquartersMetricGrid(model.operations)

            HeadquartersNotice(
                title = "FOUNDING-SHIP HANDOVER • ${model.handoverStatus}",
                text = model.handoverDetail,
                tone = model.handoverTone,
            )

            MineItPanel {
                MineItSectionHeader(
                    title = "KOPLIN DEEP REACH CORPORATION",
                    trailing = if (network.continuity.networkAvailable) "LINK ONLINE" else "LINK OFFLINE",
                    color = if (network.continuity.networkAvailable) MineItPalette.Success else MineItPalette.Critical,
                )
                Text(
                    "External charter node KPL-CN08 • new conglomerate services require the Primary Headquarters command link. Existing commitments continue during an outage.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MineItPalette.Muted,
                )
                if (!network.continuity.networkAvailable) {
                    Text(network.continuity.reason, style = MaterialTheme.typography.labelSmall, color = MineItPalette.Critical)
                }
            }

            MineItSectionHeader("UPGRADE TO NEXT LEVEL")
            if (model.maxLevel) {
                HeadquartersNotice("MAX LEVEL", "Headquarters has reached L5.", ColonyDetailTone.GOOD)
            } else {
                HeadquartersMetricGrid(
                    listOf(
                        HeadquartersMetric("NEXT LEVEL", "HEADQUARTERS L${model.nextLevel}"),
                        HeadquartersMetric("IMPROVEMENT", "+${hqFormatNumber(model.capacityGain)} COMMAND"),
                    ),
                )
                MineItSectionHeader("REQUIREMENTS")
                HeadquartersMetricGrid(model.requirements)
                HeadquartersNotice(
                    title = if (model.upgradeReady) "READY TO UPGRADE" else "UPGRADE BLOCKED",
                    text = if (model.upgradeReady) "All current Headquarters upgrade requirements are satisfied." else model.upgradeReason,
                    tone = if (model.upgradeReady) ColonyDetailTone.GOOD else ColonyDetailTone.WARNING,
                )
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(MineItSpacing.Sm)) {
                if (!model.primary) {
                    MineItSecondaryButton(
                        text = if (model.primaryEligible) "SET PRIMARY" else "PRIMARY BLOCKED",
                        onClick = onSetPrimary,
                        enabled = model.primaryEligible,
                        modifier = Modifier.weight(1f),
                    )
                }
                MineItPrimaryButton(
                    text = when {
                        model.maxLevel -> "MAX LEVEL"
                        model.upgradeReady -> "UPGRADE TO L${model.nextLevel}"
                        else -> "UPGRADE BLOCKED"
                    },
                    onClick = onUpgrade,
                    enabled = model.upgradeReady && !model.maxLevel,
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
            title = { Text(if (model.primary) "DEMOLISH PRIMARY HEADQUARTERS?" else "DEMOLISH HEADQUARTERS?") },
            text = {
                Text(
                    when {
                        !model.primary -> "This expansion Headquarters will stop contributing command capacity and bonus immediately. Normal demolition recovery applies."
                        model.commandShipFallback -> "This removes the selected Primary Headquarters. The docked command-capable ship can take emergency command, but the conglomerate network will be offline until a staffed Primary Headquarters is restored. Normal demolition recovery applies."
                        else -> "This removes the selected Primary Headquarters. No docked command-capable ship is available, so command capacity will fall to zero and the conglomerate network will be offline until a staffed Primary Headquarters is restored. Normal demolition recovery applies."
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
private fun HeadquartersHero(tile: WorldTile, model: HeadquartersControlModel) {
    MineItPanel(raised = true) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MineItSpacing.Md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HeadquartersArtwork(tile, model.level)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(MineItSpacing.Xs)) {
                Text("COLONY CONTROL • HEADQUARTERS", style = MaterialTheme.typography.labelSmall, color = MineItPalette.Accent, fontWeight = FontWeight.Bold)
                Text("Headquarters L${model.level}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                Row(horizontalArrangement = Arrangement.spacedBy(MineItSpacing.Xs)) {
                    MineItStatusBadge(model.status, hqToneColor(model.statusTone))
                    MineItStatusBadge(if (model.primary) "PRIMARY" else "EXPANSION", MineItPalette.Accent)
                }
                Text("Sector ${tile.coordinate.x},${tile.coordinate.y}", style = MaterialTheme.typography.labelSmall, color = MineItPalette.Muted)
            }
        }
    }
}

@Composable
private fun HeadquartersArtwork(tile: WorldTile, level: Int) {
    val bitmap = rememberMineItAssetBitmap(MineItAssetPaths.developmentAtlas(tile))
    Box(
        Modifier
            .size(104.dp)
            .background(MineItPalette.Control, RoundedCornerShape(MineItRadius.Medium))
            .border(1.dp, MineItPalette.Accent.copy(alpha = .25f), RoundedCornerShape(MineItRadius.Medium)),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Canvas(Modifier.size(98.dp)) {
                val frameWidth = bitmap.width / 5
                val frame = (level.coerceIn(1, 5) - 1) * frameWidth
                drawImage(
                    image = bitmap,
                    srcOffset = IntOffset(frame, 0),
                    srcSize = IntSize(frameWidth, bitmap.height),
                    dstSize = IntSize(size.width.toInt().coerceAtLeast(1), size.height.toInt().coerceAtLeast(1)),
                )
            }
        } else {
            Text("HQ", style = MaterialTheme.typography.headlineMedium, color = MineItPalette.Accent, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun HeadquartersMetricGrid(metrics: List<HeadquartersMetric>) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(MineItSpacing.Sm)) {
        metrics.chunked(2).forEach { pair ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(MineItSpacing.Sm)) {
                pair.forEach { metric ->
                    Surface(
                        modifier = Modifier.weight(1f),
                        color = MineItPalette.Control,
                        border = BorderStroke(1.dp, MineItPalette.Line),
                        shape = RoundedCornerShape(MineItRadius.Small),
                    ) {
                        Column(Modifier.padding(MineItSpacing.Sm), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(metric.label, style = MaterialTheme.typography.labelSmall, color = MineItPalette.Muted)
                            Text(metric.value, style = MaterialTheme.typography.titleSmall, color = hqToneColor(metric.tone), fontWeight = FontWeight.Black)
                            metric.detail?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = MineItPalette.Muted) }
                        }
                    }
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun HeadquartersNotice(title: String, text: String, tone: ColonyDetailTone) {
    val color = hqToneColor(tone)
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

data class HeadquartersMetric(
    val label: String,
    val value: String,
    val detail: String? = null,
    val tone: ColonyDetailTone = ColonyDetailTone.NORMAL,
)

data class HeadquartersControlAlert(
    val title: String,
    val text: String,
    val tone: ColonyDetailTone,
)

data class HeadquartersControlModel(
    val level: Int,
    val primary: Boolean,
    val primaryEligible: Boolean,
    val status: String,
    val statusTone: ColonyDetailTone,
    val overview: List<HeadquartersMetric>,
    val operations: List<HeadquartersMetric>,
    val alert: HeadquartersControlAlert?,
    val handoverStatus: String,
    val handoverDetail: String,
    val handoverTone: ColonyDetailTone,
    val nextLevel: Int,
    val capacityGain: Double,
    val maxLevel: Boolean,
    val upgradeReady: Boolean,
    val upgradeReason: String,
    val requirements: List<HeadquartersMetric>,
    val commandShipFallback: Boolean,
)

/** Pure HQ-context presentation mapping of the maintained web Colony Control hierarchy. */
object HeadquartersControlPresentation {
    fun build(
        state: GameState,
        network: ColonyNetworkSnapshot,
        departureGate: HeadquartersDepartureGate,
        tile: WorldTile,
        upgradePreview: DevelopmentPreview?,
    ): HeadquartersControlModel {
        val development = requireNotNull(tile.development) { "Headquarters Control requires a developed tile." }
        require(development.kind == DevelopmentKind.HEADQUARTERS) { "Headquarters Control requires a Headquarters tile." }
        val row = requireNotNull(network.headquarters.rows.firstOrNull { it.coordinate == tile.coordinate }) {
            "Headquarters network row not found."
        }
        val primary = row.primary
        val continuity = network.continuity
        val status = when {
            primary && continuity.phase == HeadquartersContinuityPhase.OUTAGE -> "HQ OUTAGE"
            primary && continuity.phase == HeadquartersContinuityPhase.RECOVERY -> "RECOVERING"
            !row.constructed -> "INCOMPLETE"
            !row.staffed -> "UNSTAFFED"
            !row.powered -> "UNPOWERED"
            else -> "ACTIVE"
        }
        val statusTone = when (status) {
            "ACTIVE" -> ColonyDetailTone.GOOD
            "RECOVERING", "UNSTAFFED", "UNPOWERED" -> ColonyDetailTone.WARNING
            else -> ColonyDetailTone.CRITICAL
        }

        val overview = listOf(
            HeadquartersMetric("COMMAND CAPACITY", hqFormatNumber(row.capacity)),
            HeadquartersMetric(
                "STAFF",
                "${hqFormatNumber(if (row.staffed) row.requiredStaff else 0.0)} / ${hqFormatNumber(row.requiredStaff)}",
                if (row.staffed) "Fully staffed" else "Not operational",
                if (row.staffed) ColonyDetailTone.GOOD else ColonyDetailTone.CRITICAL,
            ),
            HeadquartersMetric(
                "POWER",
                "${hqFormatNumber(if (row.powered) row.requiredPower else 0.0)} / ${hqFormatNumber(row.requiredPower)}",
                if (row.powered) "Operational" else "No command contribution",
                if (row.powered) ColonyDetailTone.GOOD else ColonyDetailTone.CRITICAL,
            ),
            HeadquartersMetric(
                "ROLE",
                if (primary) "PRIMARY" else "EXPANSION",
                if (primary) "Selected command centre" else if (row.staffed) "Eligible for Primary" else "Staffing required for Primary",
                if (primary || row.staffed) ColonyDetailTone.GOOD else ColonyDetailTone.WARNING,
            ),
        )

        val source = when (network.headquarters.sourceType) {
            CommandSourceType.HEADQUARTERS -> "HEADQUARTERS"
            CommandSourceType.SHIP -> "SHIP"
            null -> "NONE"
        }
        val operations = listOf(
            HeadquartersMetric("NETWORK SOURCE", source),
            HeadquartersMetric(
                "CONGLOMERATE LINK",
                if (continuity.networkAvailable) "ONLINE" else "OFFLINE",
                if (continuity.phase == HeadquartersContinuityPhase.RECOVERY) "${continuity.recoveryDaysRemaining} recovery days remain" else continuity.reason,
                if (continuity.networkAvailable) ColonyDetailTone.GOOD else ColonyDetailTone.CRITICAL,
            ),
            HeadquartersMetric("TOTAL CAPACITY", hqFormatNumber(network.headquarters.capacity)),
            HeadquartersMetric(
                "COMMAND LOAD",
                hqFormatNumber(network.headquarters.load),
                if (network.headquarters.load > network.headquarters.capacity) "Capacity exceeded" else "Within capacity",
                if (network.headquarters.load > network.headquarters.capacity) ColonyDetailTone.WARNING else ColonyDetailTone.GOOD,
            ),
            HeadquartersMetric(
                "COMMAND EFFICIENCY",
                hqPercent(network.headquarters.efficiency),
                "${hqPercent(network.headquarters.bonus)} bonus • ${hqPercent(network.headquarters.overloadPenalty)} penalty",
                hqFactorTone(network.headquarters.efficiency),
            ),
            HeadquartersMetric(
                "OUTAGE CONTINUITY",
                hqPercent(continuity.efficiencyFactor),
                when (continuity.phase) {
                    HeadquartersContinuityPhase.OUTAGE -> "${continuity.offlineDays} full offline days"
                    HeadquartersContinuityPhase.RECOVERY -> "${continuity.recoveryDaysRemaining} days to full recovery"
                    HeadquartersContinuityPhase.ONLINE -> "No continuity penalty"
                },
                hqFactorTone(continuity.efficiencyFactor),
            ),
            HeadquartersMetric(
                "EFFECTIVE OUTPUT",
                hqPercent(continuity.effectiveCommandEfficiency),
                "Command × outage continuity",
                hqFactorTone(continuity.effectiveCommandEfficiency),
            ),
        )

        val alert = when (continuity.phase) {
            HeadquartersContinuityPhase.OUTAGE -> HeadquartersControlAlert(
                "CONGLOMERATE NETWORK OFFLINE",
                "New conglomerate orders are blocked. Existing commitments continue. Colony work is operating at ${hqPercent(continuity.efficiencyFactor)}; continuity degrades while the Primary Headquarters remains offline.",
                ColonyDetailTone.CRITICAL,
            )
            HeadquartersContinuityPhase.RECOVERY -> HeadquartersControlAlert(
                "HEADQUARTERS RECOVERY",
                "Conglomerate access is restored. Colony work is operating at ${hqPercent(continuity.efficiencyFactor)} and will recover over ${continuity.recoveryDaysRemaining} more day${if (continuity.recoveryDaysRemaining == 1) "" else "s"}.",
                ColonyDetailTone.WARNING,
            )
            HeadquartersContinuityPhase.ONLINE -> null
        }

        val handoverComplete = state.activeColony.headquarters.commandHandoverComplete
        val handoverStatus = when {
            handoverComplete -> "COMPLETE"
            departureGate.ok -> "READY"
            else -> "BLOCKED"
        }
        val handoverDetail = when {
            handoverComplete -> "Founding command has already been handed over to colony Headquarters."
            departureGate.ok -> "Primary Headquarters is constructed and fully staffed; first ship departure may complete command handover."
            else -> departureGate.failures.joinToString(" • ")
        }

        val maxLevel = development.level >= InfrastructureRules.MAX_LEVEL || upgradePreview?.max == true
        val nextLevel = if (maxLevel) development.level else upgradePreview?.nextLevel?.takeIf { it > development.level } ?: development.level + 1
        val capacityGain = max(0.0, InfrastructureRules.headquartersCapacity(nextLevel) - row.capacity)
        val upgradeReady = !maxLevel && upgradePreview?.ok == true
        val upgradeReason = upgradePreview?.reason ?: if (maxLevel) "Maximum Headquarters level reached." else "Upgrade requirements are not satisfied."
        val buildStock = state.activeColony.inventory.amountFor(ResourceCategory.BUILD)
        val oreStock = state.activeColony.inventory.amountFor(ResourceCategory.ORE)
        val requirements = if (maxLevel) emptyList() else buildList {
            val buildRequired = upgradePreview?.cost?.build ?: 0.0
            add(
                HeadquartersMetric(
                    "BUILD",
                    hqFormatNumber(buildRequired),
                    "${hqFormatNumber(buildStock)} available",
                    if (buildStock + .0001 >= buildRequired) ColonyDetailTone.GOOD else ColonyDetailTone.WARNING,
                ),
            )
            val oreRequired = upgradePreview?.cost?.ore ?: 0.0
            if (oreRequired > .0001) {
                add(
                    HeadquartersMetric(
                        "ORE",
                        hqFormatNumber(oreRequired),
                        "${hqFormatNumber(oreStock)} available",
                        if (oreStock + .0001 >= oreRequired) ColonyDetailTone.GOOD else ColonyDetailTone.WARNING,
                    ),
                )
            }
        }
        val commandShipFallback = state.fleet.ships.any { it.dockedColonyId == state.activeColonyId && it.commandCapable }

        return HeadquartersControlModel(
            level = development.level,
            primary = primary,
            primaryEligible = !primary && row.constructed && row.staffed,
            status = status,
            statusTone = statusTone,
            overview = overview,
            operations = operations,
            alert = alert,
            handoverStatus = handoverStatus,
            handoverDetail = handoverDetail,
            handoverTone = if (handoverComplete || departureGate.ok) ColonyDetailTone.GOOD else ColonyDetailTone.WARNING,
            nextLevel = nextLevel,
            capacityGain = capacityGain,
            maxLevel = maxLevel,
            upgradeReady = upgradeReady,
            upgradeReason = upgradeReason,
            requirements = requirements,
            commandShipFallback = commandShipFallback,
        )
    }
}

private fun hqFactorTone(value: Double): ColonyDetailTone = when {
    value >= .999 -> ColonyDetailTone.GOOD
    value >= .7 -> ColonyDetailTone.WARNING
    else -> ColonyDetailTone.CRITICAL
}

private fun hqToneColor(tone: ColonyDetailTone): Color = when (tone) {
    ColonyDetailTone.NORMAL -> MineItPalette.Text
    ColonyDetailTone.GOOD -> MineItPalette.Success
    ColonyDetailTone.WARNING -> MineItPalette.Warning
    ColonyDetailTone.CRITICAL -> MineItPalette.Critical
}

private fun hqPercent(value: Double): String = "${(value.coerceAtLeast(0.0) * 100.0).toInt()}%"
private fun hqFormatNumber(value: Double): String = if (value % 1.0 == 0.0) value.toLong().toString() else "%.1f".format(value)
