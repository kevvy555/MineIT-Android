package com.mineit.android.ui.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import com.mineit.android.ui.design.MineItPrimaryButton
import com.mineit.android.ui.design.MineItRadius
import com.mineit.android.ui.design.MineItSecondaryButton

val SectorInfoHeight = 26.dp

/**
 * A deliberately tiny, fixed-height information rail. It is always present below the map so
 * selecting or clearing a sector never changes the map viewport dimensions.
 */
@Composable
fun SectorContextBar(
    selectedTiles: List<WorldTile>,
    surveyDays: Int?,
    surveyableSelectedCount: Int,
    powerPreview: DevelopmentPreview?,
    housingPreview: DevelopmentPreview?,
    industryPreview: DevelopmentPreview?,
    headquartersPreview: DevelopmentPreview?,
    extractionPreview: DevelopmentPreview?,
    upgradePreview: DevelopmentPreview?,
    modifier: Modifier = Modifier,
) {
    val text = when {
        selectedTiles.isEmpty() -> "TAP A SECTOR • DRAG ACROSS SURVEYABLE TILES TO QUEUE"
        selectedTiles.size > 1 -> "${selectedTiles.size} SECTORS SELECTED • $surveyableSelectedCount SURVEYABLE"
        else -> {
            val tile = selectedTiles.single()
            val requirement = selectedRequirement(
                tile = tile,
                powerPreview = powerPreview,
                housingPreview = housingPreview,
                industryPreview = industryPreview,
                headquartersPreview = headquartersPreview,
                extractionPreview = extractionPreview,
                upgradePreview = upgradePreview,
            )
            buildString {
                append("${tile.coordinate.x},${tile.coordinate.y} • ${tile.terrain.name} • ")
                append(requirement ?: sectorSummary(tile, surveyDays))
            }
        }
    }
    val warning = selectedTiles.singleOrNull()?.let {
        selectedRequirement(it, powerPreview, housingPreview, industryPreview, headquartersPreview, extractionPreview, upgradePreview)
    } != null

    Surface(
        modifier = modifier.fillMaxWidth().height(SectorInfoHeight),
        color = MineItPalette.Panel,
        border = BorderStroke(1.dp, MineItPalette.Line),
        shape = RoundedCornerShape(MineItRadius.Small),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = if (warning) MineItPalette.Warning else MineItPalette.Muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * Context actions replace the normal simulation footer while a sector selection exists. This keeps
 * buttons out of the map and gives the player one stable interaction rail at the bottom of the screen.
 */
@Composable
fun SectorActionBar(
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
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(1.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when {
            selectedTiles.isEmpty() -> Unit
            selectedTiles.size > 1 -> {
                MineItPrimaryButton(
                    text = if (surveyableSelectedCount > 0) "QUEUE $surveyableSelectedCount" else "NO SURVEY",
                    onClick = onSurveyMany,
                    enabled = surveyableSelectedCount > 0,
                    compact = true,
                    modifier = Modifier.weight(2f),
                )
                MineItSecondaryButton("CLOSE", onClearSelection, compact = true, modifier = Modifier.weight(1f))
            }
            else -> {
                val tile = selectedTiles.single()
                val development = tile.development
                when {
                    development != null -> {
                        MineItPrimaryButton(
                            text = if (upgradePreview?.nextLevel ?: 0 > 0) "UP L${upgradePreview?.nextLevel}" else "UPGRADE",
                            onClick = onUpgrade,
                            enabled = upgradePreview?.ok == true,
                            compact = true,
                            modifier = Modifier.weight(1f),
                        )
                        if (development.kind == DevelopmentKind.HEADQUARTERS && tile.coordinate != primaryHeadquarters) {
                            MineItSecondaryButton("PRIMARY", onSetPrimary, compact = true, modifier = Modifier.weight(1f))
                        }
                        MineItDestructiveButton("DEMOLISH", onDemolish, compact = true, modifier = Modifier.weight(1f))
                        MineItSecondaryButton("CLOSE", onClearSelection, compact = true, modifier = Modifier.weight(1f))
                    }
                    !tile.revealed -> {
                        MineItPrimaryButton(
                            text = surveyDays?.let { "SURVEY ${it}D" } ?: "NO SURVEY",
                            onClick = onSurveyOne,
                            enabled = surveyDays != null,
                            compact = true,
                            modifier = Modifier.weight(2f),
                        )
                        MineItSecondaryButton("CLOSE", onClearSelection, compact = true, modifier = Modifier.weight(1f))
                    }
                    else -> {
                        MineItSecondaryButton(
                            "PWR",
                            { onBuild(DevelopmentKind.POWER) },
                            enabled = powerPreview?.ok == true,
                            compact = true,
                            modifier = Modifier.weight(1f),
                        )
                        MineItSecondaryButton(
                            "HOUSE",
                            { onBuild(DevelopmentKind.HOUSING) },
                            enabled = housingPreview?.ok == true,
                            compact = true,
                            modifier = Modifier.weight(1f),
                        )
                        MineItSecondaryButton(
                            "IND",
                            { onBuild(DevelopmentKind.INDUSTRY) },
                            enabled = industryPreview?.ok == true,
                            compact = true,
                            modifier = Modifier.weight(1f),
                        )
                        MineItSecondaryButton(
                            "HQ",
                            { onBuild(DevelopmentKind.HEADQUARTERS) },
                            enabled = headquartersPreview?.ok == true,
                            compact = true,
                            modifier = Modifier.weight(.8f),
                        )
                        if (tile.deposit != null && !tile.resourceCovered && !tile.resourceExhausted) {
                            MineItPrimaryButton(
                                "DEVELOP",
                                onDevelop,
                                enabled = extractionPreview?.ok == true,
                                compact = true,
                                modifier = Modifier.weight(1.15f),
                            )
                        }
                        MineItSecondaryButton("CLOSE", onClearSelection, compact = true, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

private fun selectedRequirement(
    tile: WorldTile,
    powerPreview: DevelopmentPreview?,
    housingPreview: DevelopmentPreview?,
    industryPreview: DevelopmentPreview?,
    headquartersPreview: DevelopmentPreview?,
    extractionPreview: DevelopmentPreview?,
    upgradePreview: DevelopmentPreview?,
): String? = when {
    tile.development != null -> upgradePreview?.takeIf { !it.ok }?.reason
    tile.deposit != null && !tile.resourceCovered && !tile.resourceExhausted -> extractionPreview?.takeIf { !it.ok }?.reason
    !tile.revealed -> null
    else -> listOfNotNull(powerPreview, housingPreview, industryPreview, headquartersPreview).firstOrNull { !it.ok }?.reason
}

private fun sectorSummary(tile: WorldTile, surveyDays: Int?): String {
    val dev = tile.development
    if (dev != null) return "${dev.kind.name.lowercase().replaceFirstChar { it.uppercase() }} L${dev.level}"
    if (!tile.revealed) return surveyDays?.let { "Unsurveyed • ${it}d survey" } ?: "Unsurveyed"
    if (tile.resourceExhausted) return "Resource exhausted"
    val deposit = tile.deposit ?: return "Surveyed • clear reading"
    val name = ResourceCatalogue.get(ResourceId(deposit.resourceId.value))?.name ?: deposit.name
    val quantity = deposit.abundanceLabel ?: deposit.reserve?.let { "${format(it.toDouble())} remaining" } ?: "Deposit"
    return "$name • Q${deposit.quality} • ${deposit.rarity} • $quantity"
}
