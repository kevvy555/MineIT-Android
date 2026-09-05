package com.mineit.android.ui.map

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextOverflow
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
    surveyable: Set<SectorCoordinate>,
    scanningLevel: Int,
    surveySlots: Int,
    focus: MapFocus,
    stateFilters: Set<MapStateFilter>,
    network: ColonyNetworkSnapshot,
    onTap: (SectorCoordinate) -> Unit,
    onBeginMultiSelect: (SectorCoordinate) -> Unit,
    onAddMultiSelect: (SectorCoordinate) -> Unit,
    onEndMultiSelect: () -> Unit,
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
                .pointerSurveySelect(
                    gridPixels = gridPixels,
                    surveyable = surveyable,
                    onStart = { coordinate ->
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onBeginMultiSelect(coordinate)
                    },
                    onDrag = onAddMultiSelect,
                    onEnd = onEndMultiSelect,
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
                                    resurveyAvailable = isResurveyAvailable(tile, scanningLevel),
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
            if (activeSurveys.isNotEmpty() || queued.isNotEmpty()) {
                SurveyHud(
                    scanningLevel = scanningLevel,
                    slots = surveySlots,
                    active = activeSurveys,
                    queuedCount = queued.size,
                    modifier = Modifier.align(Alignment.TopEnd).padding(4.dp),
                )
            }
        }
    }
}

private fun Modifier.pointerSurveySelect(
    gridPixels: IntSize,
    surveyable: Set<SectorCoordinate>,
    onStart: (SectorCoordinate) -> Unit,
    onDrag: (SectorCoordinate) -> Unit,
    onEnd: () -> Unit,
): Modifier = this.then(
    Modifier.pointerInput(gridPixels, surveyable) {
        var selecting = false
        detectDragGestures(
            onDragStart = { offset ->
                val coordinate = coordinateAt(offset, gridPixels)
                selecting = coordinate != null && coordinate in surveyable
                if (selecting) onStart(requireNotNull(coordinate))
            },
            onDrag = { change, _ ->
                if (selecting) {
                    coordinateAt(change.position, gridPixels)
                        ?.takeIf { it in surveyable }
                        ?.let(onDrag)
                    change.consume()
                }
            },
            onDragEnd = {
                if (selecting) onEnd()
                selecting = false
            },
            onDragCancel = { selecting = false },
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

internal fun isResurveyAvailable(tile: WorldTile, scanningLevel: Int): Boolean =
    tile.coordinate != SectorCoordinate(0, 0) &&
        tile.revealed &&
        tile.lastScannedAtLevel > 0 &&
        tile.lastScannedAtLevel < scanningLevel

@Composable
private fun ColonyMapTile(
    tile: WorldTile,
    activeSurvey: SurveyTask?,
    queued: Boolean,
    selected: Boolean,
    multiSelected: Boolean,
    resurveyAvailable: Boolean,
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
                contentDescription = tileDescription(tile, activeSurvey, queued, resurveyAvailable)
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
        if (resurveyAvailable) {
            val activeResurvey = activeSurvey?.resurvey == true
            Text(
                text = when {
                    activeResurvey -> "?\nRESCAN"
                    queued -> "?\nQUEUED"
                    else -> "?"
                },
                modifier = Modifier.align(Alignment.BottomStart).padding(2.dp),
                color = Color(0xFFFFD166),
                fontSize = if (activeResurvey || queued) 4.7.sp else 10.sp,
                lineHeight = 5.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
            )
        }
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
private fun SurveyHud(
    scanningLevel: Int,
    slots: Int,
    active: List<SurveyTask>,
    queuedCount: Int,
    modifier: Modifier = Modifier,
) {
    val lead = active.firstOrNull()
    val progress = lead?.let { task ->
        if (task.totalDays <= 0) 1f else (1.0 - task.daysRemaining / task.totalDays.toDouble()).coerceIn(0.0, 1.0).toFloat()
    } ?: 0f
    Surface(
        modifier = modifier.widthIn(min = 104.dp, max = 150.dp),
        color = MineItPalette.Control.copy(alpha = .94f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MineItPalette.Survey.copy(alpha = .48f)),
        shape = RoundedCornerShape(5.dp),
    ) {
        Column(Modifier.padding(horizontal = 6.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("SCANNING • L$scanningLevel", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = MineItPalette.Survey)
            Text(
                "${active.size}/${slots.coerceAtLeast(1)} ACTIVE • $queuedCount QUEUED",
                style = MaterialTheme.typography.labelSmall,
                color = MineItPalette.Text,
                maxLines = 1,
            )
            lead?.let { task ->
                Text(
                    "${task.coordinate.x},${task.coordinate.y} • ${if (task.resurvey) "RESCAN" else "SURVEY"} • ${ceil(task.daysRemaining).toInt()}d",
                    style = MaterialTheme.typography.labelSmall,
                    color = MineItPalette.Muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Box(Modifier.fillMaxWidth().height(3.dp).background(MineItPalette.Line, RoundedCornerShape(99.dp))) {
                    Box(
                        Modifier
                            .fillMaxWidth(progress)
                            .height(3.dp)
                            .background(MineItPalette.Survey, RoundedCornerShape(99.dp)),
                    )
                }
            }
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
    if (!tile.revealed) {
        if (active != null) return "SCANNING\n${ceil(active.daysRemaining).toInt()}d" to MineItPalette.Survey
        if (queued) return "QUEUED" to MineItPalette.Survey
    }
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

private fun tileDescription(tile: WorldTile, active: SurveyTask?, queued: Boolean, resurveyAvailable: Boolean): String = buildString {
    append("Sector ${tile.coordinate.x}, ${tile.coordinate.y}. ${tile.terrain.name.lowercase()} terrain. ")
    when {
        active != null -> append("${if (active.resurvey) "Resurvey" else "Survey"} active, ${ceil(active.daysRemaining).toInt()} days remaining.")
        queued && resurveyAvailable -> append("Queued for resurvey.")
        queued -> append("Queued for survey.")
        resurveyAvailable -> append("Previously surveyed with older scanning technology; resurvey available.")
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
