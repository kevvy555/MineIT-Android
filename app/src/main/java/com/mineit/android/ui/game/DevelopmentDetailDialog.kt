package com.mineit.android.ui.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mineit.android.app.DevelopmentDetail
import com.mineit.android.app.DevelopmentDetailAlert
import com.mineit.android.app.DevelopmentDetailCard
import com.mineit.android.app.DevelopmentDetailTone
import com.mineit.android.app.DevelopmentRequirement
import com.mineit.android.domain.colony.ExtractionOverdriveRules
import com.mineit.android.domain.world.ExtractionOperatingMode
import com.mineit.android.domain.world.Sustainability
import com.mineit.android.domain.world.WorldTile
import com.mineit.android.ui.art.MineItAssetPaths
import com.mineit.android.ui.art.rememberMineItAssetBitmap
import com.mineit.android.ui.design.MineItDestructiveButton
import com.mineit.android.ui.design.MineItPalette
import com.mineit.android.ui.design.MineItPrimaryButton
import com.mineit.android.ui.design.MineItRadius
import com.mineit.android.ui.design.MineItSecondaryButton
import com.mineit.android.ui.design.MineItSpacing
import com.mineit.android.ui.design.MineItStatusBadge

/** Native adaptive-building surface preserving the maintained web information hierarchy. */
@Composable
fun DevelopmentDetailDialog(
    tile: WorldTile,
    detail: DevelopmentDetail,
    statusMessage: String?,
    onUpgrade: () -> Unit,
    onAdjustHarvest: (Int) -> Unit,
    onSetOperatingMode: (ExtractionOperatingMode) -> Unit,
    onDemolish: () -> Unit,
    onDismiss: () -> Unit,
) {
    var confirmDemolition by remember(detail.coordinate) { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = true),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            color = MineItPalette.Background,
            border = BorderStroke(1.dp, MineItPalette.Line),
            shape = RoundedCornerShape(MineItRadius.Large),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(MineItSpacing.Md)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(MineItSpacing.Sm),
            ) {
                DevelopmentHero(tile, detail)
                detail.alert?.let { DevelopmentAlert(it) }
                statusMessage?.takeIf { it.isNotBlank() }?.let {
                    Text(it, style = MaterialTheme.typography.labelSmall, color = MineItPalette.Muted)
                }

                DetailSection("OVERVIEW") {
                    MetricGrid(detail.overview)
                }

                val renewable = tile.deposit?.takeIf {
                    it.sustainability == Sustainability.RENEWABLE && !it.renewableWiped && !tile.resourceExhausted
                }
                if (renewable != null) {
                    HarvestControl(
                        percent = (renewable.harvestIntensity.coerceIn(.25, 2.0) * 100.0).toInt(),
                        onAdjustHarvest = onAdjustHarvest,
                    )
                }

                if (ExtractionOverdriveRules.supports(tile)) {
                    OverdriveControl(tile = tile, onSetOperatingMode = onSetOperatingMode)
                }

                if (detail.operations.isNotEmpty()) {
                    DetailSection("OPERATIONS") {
                        MetricGrid(detail.operations)
                    }
                }

                DetailSection("UPGRADE TO NEXT LEVEL") {
                    val upgrade = detail.upgrade
                    if (upgrade.max) {
                        DetailNotice("MAX LEVEL", upgrade.improvement, DevelopmentDetailTone.READY)
                    } else {
                        MetricGrid(
                            listOf(
                                DevelopmentDetailCard("NEXT LEVEL", "${detail.name.uppercase()} L${upgrade.nextLevel}"),
                                DevelopmentDetailCard("IMPROVEMENT", upgrade.improvement, tone = if (upgrade.ready) DevelopmentDetailTone.READY else DevelopmentDetailTone.NEUTRAL),
                            ),
                        )
                    }
                }

                DetailSection("REQUIREMENTS") {
                    if (detail.upgrade.max) {
                        DetailNotice("NO FURTHER REQUIREMENTS", "Maximum building level reached.", DevelopmentDetailTone.NEUTRAL)
                    } else {
                        RequirementGrid(detail.upgrade.requirements)
                        DetailNotice(
                            if (detail.upgrade.ready) "READY TO UPGRADE" else "UPGRADE BLOCKED",
                            if (detail.upgrade.ready) {
                                "All currently required gates are satisfied."
                            } else {
                                detail.upgrade.reason ?: "One or more upgrade requirements are not satisfied."
                            },
                            if (detail.upgrade.ready) DevelopmentDetailTone.READY else DevelopmentDetailTone.WARN,
                        )
                    }
                }

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MineItSpacing.Sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MineItPrimaryButton(
                        text = when {
                            detail.upgrade.max -> "MAX LEVEL"
                            detail.upgrade.ready -> "UPGRADE TO L${detail.upgrade.nextLevel}"
                            else -> "UPGRADE BLOCKED"
                        },
                        onClick = onUpgrade,
                        enabled = detail.upgrade.ready && !detail.upgrade.max,
                        modifier = Modifier.weight(1f),
                    )
                    MineItDestructiveButton(
                        text = "DEMOLISH",
                        onClick = { confirmDemolition = true },
                        modifier = Modifier.weight(1f),
                    )
                }
                MineItSecondaryButton("CLOSE", onDismiss, modifier = Modifier.fillMaxWidth())
            }
        }
    }

    if (confirmDemolition) {
        AlertDialog(
            onDismissRequest = { confirmDemolition = false },
            title = { Text("DEMOLISH ${detail.name.uppercase()} L${detail.level}?") },
            text = {
                Text(
                    buildString {
                        append("The development will be removed and only normal demolition recovery will be returned. ")
                        if (tile.resourceCovered && tile.deposit != null) {
                            append("The known ${tile.deposit.name} beneath it will remain and become available for development.")
                        } else {
                            append("This action cannot be undone.")
                        }
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
private fun HarvestControl(percent: Int, onAdjustHarvest: (Int) -> Unit) {
    Surface(
        color = MineItPalette.Control,
        border = BorderStroke(1.dp, MineItPalette.Line),
        shape = RoundedCornerShape(MineItRadius.Small),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(MineItSpacing.Sm),
            horizontalArrangement = Arrangement.spacedBy(MineItSpacing.Sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("HARVEST INTENSITY", style = MaterialTheme.typography.labelSmall, color = MineItPalette.Muted)
                Text("$percent%", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                Text(
                    if (percent > 100) "Above 100% increases output but degrades renewable condition." else if (percent < 100) "Below 100% allows renewable condition to recover." else "Sustainable baseline.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MineItPalette.Muted,
                )
            }
            MineItSecondaryButton("−25%", { onAdjustHarvest(-25) }, enabled = percent > 25)
            MineItSecondaryButton("+25%", { onAdjustHarvest(25) }, enabled = percent < 200)
        }
    }
}

@Composable
private fun OverdriveControl(tile: WorldTile, onSetOperatingMode: (ExtractionOperatingMode) -> Unit) {
    val development = requireNotNull(tile.development)
    val mode = ExtractionOverdriveRules.mode(tile)
    val profile = ExtractionOverdriveRules.profile(tile)
    val shutdown = ExtractionOverdriveRules.isShutdown(tile)
    val exposure = ExtractionOverdriveRules.riskExposure(tile)

    Surface(
        color = MineItPalette.Control,
        border = BorderStroke(1.dp, if (shutdown) MineItPalette.Critical else MineItPalette.Line),
        shape = RoundedCornerShape(MineItRadius.Small),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(MineItSpacing.Sm),
            verticalArrangement = Arrangement.spacedBy(MineItSpacing.Xs),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(MineItSpacing.Sm)) {
                Column(Modifier.weight(1f)) {
                    Text("OPERATING MODE", style = MaterialTheme.typography.labelSmall, color = MineItPalette.Muted)
                    Text(profile.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                }
                MineItStatusBadge("RISK ${profile.risk}", if (profile.risk == "NONE") MineItPalette.Success else if (profile.risk == "LOW") MineItPalette.Warning else MineItPalette.Critical)
            }
            MetricGrid(
                listOf(
                    DevelopmentDetailCard("OUTPUT", "${(profile.output * 100).toInt()}%", "Operating multiplier"),
                    DevelopmentDetailCard("WORKFORCE", "${(profile.workforce * 100).toInt()}%", "Required staffing multiplier"),
                    DevelopmentDetailCard("EXPOSURE", "${"%.1f".format(exposure)} / 30", "Risk check threshold"),
                    DevelopmentDetailCard("CHECKS", development.overdriveRiskChecks.toString(), "Completed exposure checks"),
                ),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(MineItSpacing.Xs)) {
                ExtractionOperatingMode.entries.forEach { candidate ->
                    MineItSecondaryButton(
                        text = candidate.name,
                        onClick = { onSetOperatingMode(candidate) },
                        enabled = !shutdown && candidate != mode,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            if (shutdown) {
                DetailNotice(
                    "ACCIDENT SHUTDOWN",
                    "Facility is closed for ${development.accidentShutdownDays} more day${if (development.accidentShutdownDays == 1) "" else "s"}. Operating mode returns to Normal during recovery.",
                    DevelopmentDetailTone.BLOCKED,
                )
            } else {
                Text(
                    when (mode) {
                        ExtractionOperatingMode.NORMAL -> "Normal operation reduces accumulated exposure by 1 point per productive day."
                        ExtractionOperatingMode.PUSHED -> "Pushed operation adds 0.3 exposure per productive day for +15% output and +25% staffing demand."
                        ExtractionOperatingMode.HARD -> "Hard operation adds 1 exposure per productive day for +30% output and +50% staffing demand. Every 30 exposure triggers a 25% accident check."
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MineItPalette.Muted,
                )
            }
            development.lastAccident?.let { accident ->
                DetailNotice(
                    "LAST INCIDENT — Y${accident.year} D${accident.day}",
                    buildString {
                        append(accident.name)
                        if (accident.deaths > 0) append(" • ${accident.deaths} fatalities") else append(" • Machinery damage")
                        append(" • ${accident.shutdownDays}-day shutdown")
                    },
                    DevelopmentDetailTone.WARN,
                )
            }
        }
    }
}

@Composable
private fun DevelopmentHero(tile: WorldTile, detail: DevelopmentDetail) {
    Surface(
        color = MineItPalette.RaisedPanel,
        border = BorderStroke(1.dp, MineItPalette.Line),
        shape = RoundedCornerShape(MineItRadius.Medium),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(MineItSpacing.Sm),
            horizontalArrangement = Arrangement.spacedBy(MineItSpacing.Md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DevelopmentArtwork(tile, detail.level)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(MineItSpacing.Xs)) {
                Text(detail.kicker, style = MaterialTheme.typography.labelSmall, color = MineItPalette.Muted)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(MineItSpacing.Sm)) {
                    Text(
                        "${detail.name} L${detail.level}",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    MineItStatusBadge(detail.status, statusColor(detail.status))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(MineItSpacing.Xs)) {
                    detail.badges.take(3).forEach { MineItStatusBadge(it, MineItPalette.Muted) }
                }
                if (detail.badges.size > 3) {
                    Text(detail.badges.drop(3).joinToString(" • "), style = MaterialTheme.typography.labelSmall, color = MineItPalette.Muted, maxLines = 2)
                }
            }
        }
    }
}

@Composable
private fun DevelopmentArtwork(tile: WorldTile, level: Int) {
    val path = MineItAssetPaths.developmentAtlas(tile)
    val bitmap = rememberMineItAssetBitmap(path)
    Box(
        Modifier
            .size(112.dp)
            .background(MineItPalette.Control, RoundedCornerShape(MineItRadius.Medium))
            .border(1.dp, MineItPalette.Accent.copy(alpha = .2f), RoundedCornerShape(MineItRadius.Medium)),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Canvas(Modifier.fillMaxSize().padding(3.dp)) {
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
            Text(
                when (tile.development?.kind?.name) {
                    "POWER" -> "⚡"
                    else -> "BUILDING"
                },
                style = MaterialTheme.typography.titleLarge,
                color = MineItPalette.Accent,
                fontWeight = FontWeight.Black,
            )
        }
    }
}

@Composable
private fun DevelopmentAlert(alert: DevelopmentDetailAlert) {
    val color = toneColor(alert.tone)
    Column(
        Modifier
            .fillMaxWidth()
            .background(color.copy(alpha = .10f), RoundedCornerShape(MineItRadius.Small))
            .border(1.dp, color.copy(alpha = .5f), RoundedCornerShape(MineItRadius.Small))
            .padding(MineItSpacing.Sm),
        verticalArrangement = Arrangement.spacedBy(MineItSpacing.Xs),
    ) {
        Text(alert.title, style = MaterialTheme.typography.labelMedium, color = color, fontWeight = FontWeight.Black)
        Text(alert.text, style = MaterialTheme.typography.bodySmall, color = MineItPalette.Text)
    }
}

@Composable
private fun DetailSection(title: String, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(MineItSpacing.Xs)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black)
            Box(Modifier.padding(start = MineItSpacing.Sm).weight(1f).background(MineItPalette.Accent.copy(alpha = .7f)).padding(top = 1.dp))
        }
        content()
    }
}

@Composable
private fun MetricGrid(cards: List<DevelopmentDetailCard>) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(MineItSpacing.Xs)) {
        cards.chunked(2).forEach { pair ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(MineItSpacing.Xs)) {
                pair.forEach { DetailCard(it, Modifier.weight(1f)) }
                if (pair.size == 1) Box(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun DetailCard(card: DevelopmentDetailCard, modifier: Modifier = Modifier) {
    val color = toneColor(card.tone)
    Column(
        modifier
            .background(if (card.tone == DevelopmentDetailTone.NEUTRAL) MineItPalette.Control else color.copy(alpha = .08f), RoundedCornerShape(MineItRadius.Small))
            .border(1.dp, if (card.tone == DevelopmentDetailTone.NEUTRAL) MineItPalette.Line else color.copy(alpha = .45f), RoundedCornerShape(MineItRadius.Small))
            .padding(MineItSpacing.Sm),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(card.label, style = MaterialTheme.typography.labelSmall, color = MineItPalette.Muted, maxLines = 1)
        Text(card.value, style = MaterialTheme.typography.titleSmall, color = if (card.tone == DevelopmentDetailTone.NEUTRAL) MineItPalette.Text else color, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (card.detail.isNotBlank()) Text(card.detail, style = MaterialTheme.typography.labelSmall, color = MineItPalette.Muted, maxLines = 2)
    }
}

@Composable
private fun RequirementGrid(requirements: List<DevelopmentRequirement>) {
    if (requirements.isEmpty()) return
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(MineItSpacing.Xs)) {
        requirements.chunked(2).forEach { pair ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(MineItSpacing.Xs)) {
                pair.forEach { RequirementCard(it, Modifier.weight(1f)) }
                if (pair.size == 1) Box(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun RequirementCard(requirement: DevelopmentRequirement, modifier: Modifier = Modifier) {
    val color = if (requirement.ready) MineItPalette.Success else MineItPalette.Warning
    Column(
        modifier
            .background(color.copy(alpha = .07f), RoundedCornerShape(MineItRadius.Small))
            .border(1.dp, color.copy(alpha = .4f), RoundedCornerShape(MineItRadius.Small))
            .padding(MineItSpacing.Sm),
    ) {
        Text(requirement.label, style = MaterialTheme.typography.labelSmall, color = MineItPalette.Muted, maxLines = 1)
        Text(requirement.required, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black, color = MineItPalette.Text, maxLines = 1)
        Text(requirement.current, style = MaterialTheme.typography.labelSmall, color = color, maxLines = 2)
    }
}

@Composable
private fun DetailNotice(title: String, text: String, tone: DevelopmentDetailTone) {
    val color = toneColor(tone)
    Column(
        Modifier
            .fillMaxWidth()
            .background(color.copy(alpha = .07f), RoundedCornerShape(MineItRadius.Small))
            .border(1.dp, color.copy(alpha = .4f), RoundedCornerShape(MineItRadius.Small))
            .padding(MineItSpacing.Sm),
    ) {
        Text(title, style = MaterialTheme.typography.labelMedium, color = color, fontWeight = FontWeight.Black)
        Text(text, style = MaterialTheme.typography.labelSmall, color = MineItPalette.Muted)
    }
}

private fun toneColor(tone: DevelopmentDetailTone): Color = when (tone) {
    DevelopmentDetailTone.NEUTRAL -> MineItPalette.Text
    DevelopmentDetailTone.READY -> MineItPalette.Success
    DevelopmentDetailTone.WARN -> MineItPalette.Warning
    DevelopmentDetailTone.BLOCKED -> MineItPalette.Critical
}

private fun statusColor(status: String): Color = when (status) {
    "ACTIVE" -> MineItPalette.Success
    "STOPPED", "EMERGENCY PAUSE" -> MineItPalette.Warning
    else -> MineItPalette.Critical
}
