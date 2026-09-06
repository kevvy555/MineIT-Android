package com.mineit.android.ui.game

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mineit.android.domain.colony.DevelopmentPreview
import com.mineit.android.domain.model.ResourceId
import com.mineit.android.domain.resources.ResourceCatalogue
import com.mineit.android.domain.world.DevelopmentKind
import com.mineit.android.domain.world.SectorCoordinate
import com.mineit.android.domain.world.WorldTile
import com.mineit.android.ui.design.MineItDestructiveButton
import com.mineit.android.ui.design.MineItPalette
import com.mineit.android.ui.design.MineItPanel
import com.mineit.android.ui.design.MineItPrimaryButton
import com.mineit.android.ui.design.MineItSecondaryButton
import com.mineit.android.ui.design.MineItSpacing
import com.mineit.android.ui.design.MineItStatusBadge

private val SectorContextHeight = 88.dp

@Composable
fun SectorContextBar(
    selectedTiles: List<WorldTile>,
    surveyDays: Int?,
    surveyableSelectedCount: Int,
    primaryHeadquarters: SectorCoordinate?,
    powerPreview: DevelopmentPreview?,
    housingPreview: DevelopmentPreview?,
    industryPreview: DevelopmentPreview?,
    headquartersPreview: DevelopmentPreview?,
    extractionPreview: DevelopmentPreview?,
    upgradePreview: DevelopmentPreview?,
    onSurveyOne: () -> Unit,
    onSurveyMany: () -> Unit,
    onBuild: (DevelopmentKind) -> Unit,
    onDevelop: () -> Unit,
    onUpgrade: () -> Unit,
    onDemolish: () -> Unit,
    onSetPrimary: () -> Unit,
    onClearSelection: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // This slot is deliberately constant-height. Selection detail scrolls inside it so the map
    // viewport never changes size when selection state or available actions change.
    Box(modifier = modifier.fillMaxWidth().height(SectorContextHeight)) {
        MineItPanel(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            raised = true,
            contentPadding = 5.dp,
            contentSpacing = 3.dp,
        ) {
            when {
                selectedTiles.isEmpty() -> EmptyContext()
                selectedTiles.size > 1 -> MultiSelectionContext(
                    selectedCount = selectedTiles.size,
                    surveyableCount = surveyableSelectedCount,
                    onSurvey = onSurveyMany,
                    onClear = onClearSelection,
                )
                else -> SingleSectorContext(
                    tile = selectedTiles.single(),
                    surveyDays = surveyDays,
                    primaryHeadquarters = primaryHeadquarters,
                    powerPreview = powerPreview,
                    housingPreview = housingPreview,
                    industryPreview = industryPreview,
                    headquartersPreview = headquartersPreview,
                    extractionPreview = extractionPreview,
                    upgradePreview = upgradePreview,
                    onSurvey = onSurveyOne,
                    onBuild = onBuild,
                    onDevelop = onDevelop,
                    onUpgrade = onUpgrade,
                    onDemolish = onDemolish,
                    onSetPrimary = onSetPrimary,
                    onClear = onClearSelection,
                )
            }
        }
    }
}

@Composable
private fun EmptyContext() {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("SELECT A SECTOR", style = MaterialTheme.typography.titleSmall, color = MineItPalette.Muted)
            Text(
                "Tap to inspect • drag across surveyable sectors to queue several surveys",
                style = MaterialTheme.typography.labelSmall,
                color = MineItPalette.Muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun MultiSelectionContext(
    selectedCount: Int,
    surveyableCount: Int,
    onSurvey: () -> Unit,
    onClear: () -> Unit,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("$selectedCount SECTORS SELECTED", style = MaterialTheme.typography.titleSmall)
            Text(
                if (surveyableCount > 0) "$surveyableCount can be added to the survey plan" else "No selected sectors are currently surveyable",
                style = MaterialTheme.typography.labelSmall,
                color = if (surveyableCount > 0) MineItPalette.Survey else MineItPalette.Muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        MineItSecondaryButton("CLEAR", onClear, compact = true)
    }
    MineItPrimaryButton(
        text = if (surveyableCount > 0) "QUEUE $surveyableCount SURVEYS" else "NO SURVEYABLE SECTORS",
        onClick = onSurvey,
        enabled = surveyableCount > 0,
        modifier = Modifier.fillMaxWidth(),
        compact = true,
    )
}

@Composable
private fun SingleSectorContext(
    tile: WorldTile,
    surveyDays: Int?,
    primaryHeadquarters: SectorCoordinate?,
    powerPreview: DevelopmentPreview?,
    housingPreview: DevelopmentPreview?,
    industryPreview: DevelopmentPreview?,
    headquartersPreview: DevelopmentPreview?,
    extractionPreview: DevelopmentPreview?,
    upgradePreview: DevelopmentPreview?,
    onSurvey: () -> Unit,
    onBuild: (DevelopmentKind) -> Unit,
    onDevelop: () -> Unit,
    onUpgrade: () -> Unit,
    onDemolish: () -> Unit,
    onSetPrimary: () -> Unit,
    onClear: () -> Unit,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f)) {
            Text(
                "SECTOR ${tile.coordinate.x},${tile.coordinate.y} • ${tile.terrain.name}",
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                sectorSummary(tile),
                style = MaterialTheme.typography.labelSmall,
                color = MineItPalette.Muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        MineItSecondaryButton("CLOSE", onClear, compact = true)
    }

    tile.deposit?.let { deposit ->
        Row(horizontalArrangement = Arrangement.spacedBy(MineItSpacing.Sm)) {
            MineItStatusBadge(deposit.category.name, MineItPalette.resource(deposit.category))
            MineItStatusBadge("Q${deposit.quality}", MineItPalette.Accent)
            val stock = deposit.abundanceLabel ?: deposit.reserve?.let { format(it.toDouble()) }
            stock?.let { MineItStatusBadge(it, MineItPalette.Muted) }
        }
    }

    val development = tile.development
    if (development != null) {
        val actions = rememberScrollState()
        Row(
            Modifier.fillMaxWidth().horizontalScroll(actions),
            horizontalArrangement = Arrangement.spacedBy(MineItSpacing.Xs),
        ) {
            MineItPrimaryButton(
                text = if (upgradePreview?.nextLevel ?: 0 > 0) "UPGRADE → L${upgradePreview?.nextLevel}" else "UPGRADE",
                onClick = onUpgrade,
                enabled = upgradePreview?.ok == true,
                compact = true,
            )
            if (development.kind == DevelopmentKind.HEADQUARTERS && tile.coordinate != primaryHeadquarters) {
                MineItSecondaryButton("SET PRIMARY HQ", onSetPrimary, compact = true)
            }
            MineItDestructiveButton("DEMOLISH", onDemolish, compact = true)
        }
        upgradePreview?.takeIf { !it.ok }?.reason?.let { RequirementText(it) }
        return
    }

    if (!tile.revealed) {
        MineItPrimaryButton(
            text = surveyDays?.let { "SURVEY • $it DAYS" } ?: "SURVEY UNAVAILABLE",
            onClick = onSurvey,
            enabled = surveyDays != null,
            modifier = Modifier.fillMaxWidth(),
            compact = true,
        )
        return
    }

    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(MineItSpacing.Xs),
    ) {
        MineItSecondaryButton("POWER", { onBuild(DevelopmentKind.POWER) }, enabled = powerPreview?.ok == true, compact = true)
        MineItSecondaryButton("HOUSING", { onBuild(DevelopmentKind.HOUSING) }, enabled = housingPreview?.ok == true, compact = true)
        MineItSecondaryButton("INDUSTRY", { onBuild(DevelopmentKind.INDUSTRY) }, enabled = industryPreview?.ok == true, compact = true)
        MineItSecondaryButton("HQ", { onBuild(DevelopmentKind.HEADQUARTERS) }, enabled = headquartersPreview?.ok == true, compact = true)
        if (tile.deposit != null && !tile.resourceCovered && !tile.resourceExhausted) {
            MineItPrimaryButton("DEVELOP", onDevelop, enabled = extractionPreview?.ok == true, compact = true)
        }
    }

    val requirement = extractionPreview?.takeIf { tile.deposit != null && !it.ok }?.reason
        ?: listOfNotNull(powerPreview, housingPreview, industryPreview, headquartersPreview).firstOrNull { !it.ok }?.reason
    requirement?.let { RequirementText(it) }
}

@Composable
private fun RequirementText(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(top = MineItSpacing.Xs),
        style = MaterialTheme.typography.labelSmall,
        color = MineItPalette.Warning,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
}

private fun sectorSummary(tile: WorldTile): String {
    val dev = tile.development
    if (dev != null) return "${dev.kind.name.lowercase().replaceFirstChar { it.uppercase() }} level ${dev.level}"
    if (!tile.revealed) return "Unsurveyed • geological reading unknown"
    if (tile.resourceExhausted) return "Resource exhausted"
    val deposit = tile.deposit ?: return "Surveyed • clear reading"
    val name = ResourceCatalogue.get(ResourceId(deposit.resourceId.value))?.name ?: deposit.name
    val quantity = deposit.abundanceLabel ?: deposit.reserve?.let { "${format(it.toDouble())} remaining" } ?: "Deposit"
    return "$name • ${deposit.rarity} • $quantity"
}
