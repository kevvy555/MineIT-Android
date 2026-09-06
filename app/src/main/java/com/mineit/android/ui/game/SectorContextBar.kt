package com.mineit.android.ui.game

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

private val SectorContextHeight = 58.dp

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
    MineItPanel(
        modifier = modifier.fillMaxWidth().height(SectorContextHeight),
        raised = true,
        contentPadding = 4.dp,
        contentSpacing = 0.dp,
    ) {
        when {
            selectedTiles.isEmpty() -> Unit
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

@Composable
private fun MultiSelectionContext(
    selectedCount: Int,
    surveyableCount: Int,
    onSurvey: () -> Unit,
    onClear: () -> Unit,
) {
    Row(
        Modifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                "$selectedCount SECTORS • $surveyableCount SURVEYABLE",
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                if (surveyableCount > 0) "Queue selected sectors" else "No selected sectors can be surveyed",
                style = MaterialTheme.typography.labelSmall,
                color = if (surveyableCount > 0) MineItPalette.Survey else MineItPalette.Muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            MineItPrimaryButton(
                text = if (surveyableCount > 0) "QUEUE $surveyableCount" else "NO SURVEY",
                onClick = onSurvey,
                enabled = surveyableCount > 0,
                compact = true,
            )
            MineItSecondaryButton("CLOSE", onClear, compact = true)
        }
    }
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
    val requirement = when {
        tile.development != null -> upgradePreview?.takeIf { !it.ok }?.reason
        tile.deposit != null && !tile.resourceCovered && !tile.resourceExhausted -> extractionPreview?.takeIf { !it.ok }?.reason
        else -> listOfNotNull(powerPreview, housingPreview, industryPreview, headquartersPreview).firstOrNull { !it.ok }?.reason
    }

    Row(
        Modifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Column(Modifier.weight(1.05f)) {
            Text(
                "${tile.coordinate.x},${tile.coordinate.y} • ${tile.terrain.name}",
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                requirement ?: sectorSummary(tile),
                style = MaterialTheme.typography.labelSmall,
                color = if (requirement != null) MineItPalette.Warning else MineItPalette.Muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Row(
            Modifier.weight(1.45f).horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val development = tile.development
            when {
                development != null -> {
                    MineItPrimaryButton(
                        text = if (upgradePreview?.nextLevel ?: 0 > 0) "UP L${upgradePreview?.nextLevel}" else "UPGRADE",
                        onClick = onUpgrade,
                        enabled = upgradePreview?.ok == true,
                        compact = true,
                    )
                    if (development.kind == DevelopmentKind.HEADQUARTERS && tile.coordinate != primaryHeadquarters) {
                        MineItSecondaryButton("PRIMARY", onSetPrimary, compact = true)
                    }
                    MineItDestructiveButton("DEMOLISH", onDemolish, compact = true)
                }
                !tile.revealed -> {
                    MineItPrimaryButton(
                        text = surveyDays?.let { "SURVEY ${it}D" } ?: "NO SURVEY",
                        onClick = onSurvey,
                        enabled = surveyDays != null,
                        compact = true,
                    )
                }
                else -> {
                    MineItSecondaryButton("PWR", { onBuild(DevelopmentKind.POWER) }, enabled = powerPreview?.ok == true, compact = true)
                    MineItSecondaryButton("HOUSE", { onBuild(DevelopmentKind.HOUSING) }, enabled = housingPreview?.ok == true, compact = true)
                    MineItSecondaryButton("IND", { onBuild(DevelopmentKind.INDUSTRY) }, enabled = industryPreview?.ok == true, compact = true)
                    MineItSecondaryButton("HQ", { onBuild(DevelopmentKind.HEADQUARTERS) }, enabled = headquartersPreview?.ok == true, compact = true)
                    if (tile.deposit != null && !tile.resourceCovered && !tile.resourceExhausted) {
                        MineItPrimaryButton("DEVELOP", onDevelop, enabled = extractionPreview?.ok == true, compact = true)
                    }
                }
            }
            MineItSecondaryButton("CLOSE", onClear, compact = true)
        }
    }
}

private fun sectorSummary(tile: WorldTile): String {
    val dev = tile.development
    if (dev != null) return "${dev.kind.name.lowercase().replaceFirstChar { it.uppercase() }} L${dev.level}"
    if (!tile.revealed) return "Unsurveyed • geological reading unknown"
    if (tile.resourceExhausted) return "Resource exhausted"
    val deposit = tile.deposit ?: return "Surveyed • clear reading"
    val name = ResourceCatalogue.get(ResourceId(deposit.resourceId.value))?.name ?: deposit.name
    val quantity = deposit.abundanceLabel ?: deposit.reserve?.let { "${format(it.toDouble())} remaining" } ?: "Deposit"
    return "$name • Q${deposit.quality} • ${deposit.rarity} • $quantity"
}
