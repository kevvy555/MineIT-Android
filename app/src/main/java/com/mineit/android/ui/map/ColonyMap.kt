package com.mineit.android.ui.map

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mineit.android.domain.colony.ColonyNetworkSnapshot
import com.mineit.android.domain.resources.ResourceCatalogue
import com.mineit.android.domain.world.DevelopmentKind
import com.mineit.android.domain.world.SectorCoordinate
import com.mineit.android.domain.world.SurveyTask
import com.mineit.android.domain.world.TerrainType
import com.mineit.android.domain.world.WorldTile
import com.mineit.android.ui.art.MineItAssetPaths
import com.mineit.android.ui.art.rememberMineItAssetBitmap
import com.mineit.android.ui.design.MineItPalette
import kotlin.math.ceil
import kotlin.math.roundToInt

@Composable
fun ColonyMap(
    tiles: List<WorldTile>,
    activeSurveys: List<SurveyTask>,
    queued: Set<SectorCoordinate>,
    selected: Set<SectorCoordinate>,
    focus: MapFocus,
    stateFilters: Set<MapStateFilter>,
    network: ColonyNetworkSnapshot,
    onTap: (SectorCoordinate) -> Unit,
    onBeginMultiSelect: (SectorCoordinate) -> Unit,
    onAddMultiSelect: (SectorCoordinate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    val byCoordinate = remember(tiles) { tiles.associateBy { it.coordinate } }
    val activeByCoordinate = remember(activeSurveys) { activeSurveys.associateBy { it.coordinate } }
    var gridPixels by remember { mutableStateOf(IntSize.Zero) }

    BoxWithConstraints(modifier) {
        val gridSize = minOf(maxWidth, maxHeight)
        Box(
            modifier = Modifier
                .size(gridSize)
                .align(Alignment.Center)
                .onSizeChanged { gridPixels = it }
                .pointerSelect(
                    gridPixels = gridPixels,
                    onStart = { coordinate ->
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onBeginMultiSelect(coordinate)
                    },
                    onDrag = onAddMultiSelect,
                ),
        ) {
            Column(Modifier.fillMaxSize()) {
                for (y in -4..3) {
                    Row(Modifier.weight(1f).fillMaxWidth()) {
                        for (x in -4..3) {
                            val coordinate = SectorCoordinate(x, y)
                            val tile = byCoordinate[coordinate]
                            if (tile == null) {
                                Box(Modifier.weight(1f).fillMaxHeight())
                            } else {
                                val matches = MapPresentation.matches(
                                    tile = tile,
                                    focus = focus,
                                    stateFilters = stateFilters,
                                    queued = queued,
                                    active = activeByCoordinate.keys,
                                    network = network,
                                )
                                ColonyMapTile(
                                    tile = tile,
                                    activeSurvey = activeByCoordinate[coordinate],
                                    queued = coordinate in queued,
                                    selected = coordinate in selected,
                                    multiSelected = selected.size > 1 && coordinate in selected,
                                    problem = MapPresentation.isProblem(tile, network),
                                    dimmed = !matches,
                                    onClick = { onTap(coordinate) },
                                    modifier = Modifier.weight(1f).fillMaxHeight(),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun Modifier.pointerSelect(
    gridPixels: IntSize,
    onStart: (SectorCoordinate) -> Unit,
    onDrag: (SectorCoordinate) -> Unit,
): Modifier = this.then(
    Modifier.pointerInput(gridPixels) {
        detectDragGesturesAfterLongPress(
            onDragStart = { offset -> coordinateAt(offset, gridPixels)?.let(onStart) },
            onDrag = { change, _ ->
                coordinateAt(change.position, gridPixels)?.let(onDrag)
                change.consume()
            },
        )
    },
)

internal fun coordinateAt(offset: Offset, size: IntSize): SectorCoordinate? {
    if (size.width <= 0 || size.height <= 0) return null
    if (offset.x < 0f || offset.y < 0f || offset.x >= size.width || offset.y >= size.height) return null
    val column = ((offset.x / size.width) * 8f).toInt().coerceIn(0, 7)
    val row = ((offset.y / size.height) * 8f).toInt().coerceIn(0, 7)
    return SectorCoordinate(column - 4, row - 4)
}

@Composable
private fun ColonyMapTile(
    tile: WorldTile,
    activeSurvey: SurveyTask?,
    queued: Boolean,
    selected: Boolean,
    multiSelected: Boolean,
    problem: Boolean,
    dimmed: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectionColor = if (multiSelected) MineItPalette.MultiSelection else MineItPalette.Selection
    val borderColor = when {
        selected -> selectionColor
        problem -> MineItPalette.Critical.copy(alpha = .85f)
        else -> MineItPalette.Line.copy(alpha = .8f)
    }
    val shape = RoundedCornerShape(3.dp)
    Box(
        modifier = modifier
            .padding(.75.dp)
            .alpha(if (dimmed) .22f else 1f)
            .clip(shape)
            .background(terrainFallback(tile.terrain))
            .border(if (selected) 2.dp else 1.dp, borderColor, shape)
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = tileDescription(tile, activeSurvey, queued)
                this.selected = selected
            },
    ) {
        TileArtwork(tile, Modifier.fillMaxSize())
        Box(
            Modifier
                .fillMaxSize()
                .background(if (tile.revealed) Color.Black.copy(alpha = .18f) else Color.Black.copy(alpha = .46f)),
        )
        Text(
            text = "${tile.coordinate.x},${tile.coordinate.y}",
            modifier = Modifier.align(Alignment.TopStart).padding(2.dp),
            color = MineItPalette.Text.copy(alpha = .92f),
            fontSize = 5.5.sp,
            fontWeight = FontWeight.Bold,
        )
        val (label, color) = tileMarker(tile, activeSurvey, queued)
        Text(
            text = label,
            modifier = Modifier.align(Alignment.Center).padding(horizontal = 1.dp),
            color = color,
            fontSize = if (label.length > 9) 4.8.sp else 5.8.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
            maxLines = 2,
            lineHeight = 6.sp,
        )
        if (problem) {
            Text(
                text = "!",
                modifier = Modifier.align(Alignment.TopEnd).padding(2.dp),
                color = MineItPalette.Critical,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Black,
            )
        }
    }
}

@Composable
private fun TileArtwork(tile: WorldTile, modifier: Modifier = Modifier) {
    val terrain = rememberMineItAssetBitmap(MineItAssetPaths.terrain(tile.terrain, tile.terrainVariant))
    val resourceFrame = tile.deposit
        ?.takeIf { tile.revealed && tile.development == null && !tile.resourceExhausted }
        ?.let { MineItAssetPaths.resourceFrame(it.resourceId.value) }
    val resourceAtlas = rememberMineItAssetBitmap(if (resourceFrame != null) MineItAssetPaths.RESOURCE_ATLAS else null)
    val developmentPath = MineItAssetPaths.developmentAtlas(tile)
    val developmentAtlas = rememberMineItAssetBitmap(developmentPath)

    Canvas(modifier) {
        terrain?.let { image ->
            drawImage(
                image = image,
                dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
            )
        }
        if (resourceAtlas != null && resourceFrame != null) {
            drawImage(
                image = resourceAtlas,
                srcOffset = IntOffset(resourceFrame.x, resourceFrame.y),
                srcSize = IntSize(resourceFrame.width, resourceFrame.height),
                dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
            )
        }
        val development = tile.development
        if (developmentAtlas != null && development != null) {
            val frameWidth = developmentAtlas.width / 5
            val frame = (development.level.coerceIn(1, 5) - 1) * frameWidth
            val insetX = (size.width * .04f).roundToInt()
            val insetY = (size.height * .04f).roundToInt()
            drawImage(
                image = developmentAtlas,
                srcOffset = IntOffset(frame, 0),
                srcSize = IntSize(frameWidth, developmentAtlas.height),
                dstOffset = IntOffset(insetX, insetY),
                dstSize = IntSize(
                    (size.width.roundToInt() - insetX * 2).coerceAtLeast(1),
                    (size.height.roundToInt() - insetY * 2).coerceAtLeast(1),
                ),
            )
        }
    }
}

private fun tileMarker(tile: WorldTile, active: SurveyTask?, queued: Boolean): Pair<String, Color> {
    if (tile.coordinate == SectorCoordinate(0, 0)) return "PORT" to MineItPalette.Accent
    if (active != null) return "${ceil(active.daysRemaining).toInt()}d" to MineItPalette.Survey
    if (queued) return "QUEUE" to MineItPalette.Survey
    tile.development?.let { development ->
        val label = when (development.kind) {
            DevelopmentKind.POWER -> "PWR"
            DevelopmentKind.HOUSING -> "HAB"
            DevelopmentKind.INDUSTRY -> "IND"
            DevelopmentKind.HEADQUARTERS -> "HQ"
            DevelopmentKind.EXTRACT -> "EXT"
        }
        val color = tile.deposit?.category?.let(MineItPalette::resource) ?: MineItPalette.Accent
        return "$label L${development.level}" to color
    }
    if (!tile.revealed) return "?" to MineItPalette.Muted
    tile.deposit?.let { deposit ->
        val definition = ResourceCatalogue.get(deposit.resourceId)
        val short = definition?.name?.split(' ')?.firstOrNull()?.uppercase()?.take(7) ?: deposit.category.name
        return "$short\nQ${deposit.quality}" to MineItPalette.resource(deposit.category)
    }
    if (tile.resourceExhausted) return "DEPLETED" to MineItPalette.Critical
    return "CLEAR" to MineItPalette.Success
}

private fun tileDescription(tile: WorldTile, active: SurveyTask?, queued: Boolean): String = buildString {
    append("Sector ${tile.coordinate.x}, ${tile.coordinate.y}. ${tile.terrain.name.lowercase()} terrain. ")
    when {
        active != null -> append("Survey active, ${ceil(active.daysRemaining).toInt()} days remaining.")
        queued -> append("Queued for survey.")
        !tile.revealed -> append("Unsurveyed.")
        tile.development != null -> append("${tile.development.kind.name.lowercase()} level ${tile.development.level}.")
        tile.deposit != null -> append("${tile.deposit.name}, quality ${tile.deposit.quality}.")
        else -> append("Surveyed, clear reading.")
    }
}

private fun terrainFallback(terrain: TerrainType): Color = when (terrain) {
    TerrainType.PLAIN -> Color(0xFF334C32)
    TerrainType.HILL -> Color(0xFF5A5337)
    TerrainType.MOUNTAIN -> Color(0xFF565C62)
    TerrainType.LAKE -> Color(0xFF214F68)
}
