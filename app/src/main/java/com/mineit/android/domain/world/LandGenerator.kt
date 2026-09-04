package com.mineit.android.domain.world

import com.mineit.android.domain.config.MineItConfig
import kotlin.math.hypot
import kotlin.math.max

private data class TerrainProfile(
    val mountains: Double,
    val hills: Double,
    val lakes: Double,
    val mountainCenters: Int,
    val lakeCenters: Int,
)

private data class TerrainCenter(val x: Double, val y: Double, val radius: Double)

/** Deterministic port of the web LandService candidate generator. */
class LandGenerator {
    val start: Int = -MineItConfig.GRID_SIZE / 2
    val end: Int = start + MineItConfig.GRID_SIZE - 1

    fun generateCandidates(seed: Long, contractUid: String, archetypeId: String): List<LandingSiteCandidate> =
        List(CANDIDATE_COUNT) { index -> generateCandidate(seed, contractUid, archetypeId, index) }

    fun generateCandidate(
        seed: Long,
        contractUid: String,
        archetypeId: String,
        index: Int,
    ): LandingSiteCandidate {
        require(index in 0 until CANDIDATE_COUNT) { "Landing site index must be 0..${CANDIDATE_COUNT - 1}." }
        val candidateSeed = candidateSeed(seed, contractUid, index)
        val random = DeterministicRandom(candidateSeed)
        val profile = profiles[archetypeId] ?: profiles.getValue("temperate")
        val mountainCenters = centers(random, profile.mountainCenters, 1.7, 3.5)
        val lakeCenters = centers(random, profile.lakeCenters, 1.2, 2.7)
        val mountainShift = (random.nextDouble() - .5) * .12
        val lakeShift = (random.nextDouble() - .5) * .14
        val cells = mutableListOf<TerrainCell>()

        for (y in start..end) {
            for (x in start..end) {
                val jitter = (random.nextDouble() - .5) * .18
                val mountain = blobScore(x, y, mountainCenters) + jitter
                val lake = blobScore(x, y, lakeCenters) + (random.nextDouble() - .5) * .12
                var terrain = TerrainType.PLAIN
                if (lake > profile.lakes + lakeShift && mountain < profile.mountains + .12) {
                    terrain = TerrainType.LAKE
                } else if (mountain > profile.mountains + mountainShift) {
                    terrain = TerrainType.MOUNTAIN
                } else if (mountain > profile.hills + mountainShift) {
                    terrain = TerrainType.HILL
                }
                if (isShipTile(x, y)) terrain = TerrainType.PLAIN

                val variantHash = DeterministicHash.hashString(
                    "${candidateSeed.toLong()}|$x|$y|${terrain.serialValue}",
                )
                cells += TerrainCell(
                    coordinate = SectorCoordinate(x, y),
                    terrain = terrain,
                    variant = 1 + (variantHash % 4u).toInt(),
                )
            }
        }

        val smoothed = smooth(cells)
        val counts = TerrainType.entries.associateWith { terrain -> smoothed.count { it.terrain == terrain } }
        return LandingSiteCandidate(
            id = "site-${index + 1}",
            index = index,
            seed = candidateSeed.toLong(),
            cells = smoothed,
            counts = counts,
        )
    }

    fun terrainYieldFactor(terrain: TerrainType, category: com.mineit.android.domain.resources.ResourceCategory): Double = when {
        terrain == TerrainType.MOUNTAIN && category == com.mineit.android.domain.resources.ResourceCategory.ORE -> 1.12
        terrain == TerrainType.HILL && category == com.mineit.android.domain.resources.ResourceCategory.BUILD -> 1.12
        terrain == TerrainType.PLAIN && category == com.mineit.android.domain.resources.ResourceCategory.FOOD -> 1.10
        terrain == TerrainType.LAKE && category == com.mineit.android.domain.resources.ResourceCategory.FOOD -> 1.18
        else -> 1.0
    }

    private fun candidateSeed(seed: Long, contractUid: String, index: Int): UInt =
        DeterministicHash.hashString("$seed|$contractUid|land|$index")

    private fun centers(
        random: DeterministicRandom,
        count: Int,
        radiusMin: Double,
        radiusMax: Double,
    ): List<TerrainCenter> = List(count) {
        TerrainCenter(
            x = start + random.nextDouble() * MineItConfig.GRID_SIZE,
            y = start + random.nextDouble() * MineItConfig.GRID_SIZE,
            radius = radiusMin + random.nextDouble() * (radiusMax - radiusMin),
        )
    }

    private fun blobScore(x: Int, y: Int, centers: List<TerrainCenter>): Double = centers.fold(0.0) { best, center ->
        val distance = hypot(x - center.x, y - center.y) / max(.6, center.radius)
        max(best, 1 - distance)
    }

    private fun smooth(input: List<TerrainCell>): List<TerrainCell> {
        var cells = input
        repeat(2) {
            val byCoordinate = cells.associateBy { it.coordinate }
            val changes = mutableMapOf<SectorCoordinate, TerrainType>()
            for (cell in cells) {
                val coordinate = cell.coordinate
                if (isShipTile(coordinate.x, coordinate.y)) continue
                val neighborTerrains = neighborOffsets.mapNotNull { (dx, dy) ->
                    byCoordinate[SectorCoordinate(coordinate.x + dx, coordinate.y + dy)]?.terrain
                }
                val counts = linkedMapOf<TerrainType, Int>()
                neighborTerrains.forEach { terrain -> counts[terrain] = (counts[terrain] ?: 0) + 1 }
                val dominant = counts.entries.maxByOrNull { it.value }
                if (dominant != null && dominant.value >= 3 && cell.terrain != dominant.key) {
                    changes[coordinate] = dominant.key
                }
            }
            cells = cells.map { cell ->
                val terrain = changes[cell.coordinate] ?: cell.terrain
                cell.copy(terrain = if (isShipTile(cell.coordinate.x, cell.coordinate.y)) TerrainType.PLAIN else terrain)
            }
        }
        return cells.map { cell ->
            if (isShipTile(cell.coordinate.x, cell.coordinate.y)) cell.copy(terrain = TerrainType.PLAIN) else cell
        }
    }

    private fun isShipTile(x: Int, y: Int): Boolean = x == 0 && y == 0

    companion object {
        const val CANDIDATE_COUNT = 8

        private val neighborOffsets = listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1)

        private val profiles = mapOf(
            "temperate" to TerrainProfile(.42, .30, .46, 2, 2),
            "verdant" to TerrainProfile(.50, .33, .39, 1, 2),
            "arid" to TerrainProfile(.43, .28, .61, 2, 1),
            "frozen" to TerrainProfile(.36, .25, .48, 2, 2),
            "barren" to TerrainProfile(.31, .22, .76, 3, 1),
            "volcanic" to TerrainProfile(.27, .20, .81, 3, 1),
            "deep" to TerrainProfile(.30, .22, .84, 3, 1),
        )
    }
}

private val TerrainType.serialValue: String
    get() = when (this) {
        TerrainType.PLAIN -> "plain"
        TerrainType.HILL -> "hill"
        TerrainType.MOUNTAIN -> "mountain"
        TerrainType.LAKE -> "lake"
    }
