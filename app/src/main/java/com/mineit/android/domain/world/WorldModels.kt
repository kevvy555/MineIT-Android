package com.mineit.android.domain.world

import com.mineit.android.domain.model.ResourceId
import com.mineit.android.domain.resources.ResourceCategory
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class TerrainType {
    @SerialName("plain") PLAIN,
    @SerialName("hill") HILL,
    @SerialName("mountain") MOUNTAIN,
    @SerialName("lake") LAKE,
}

@Serializable
data class SectorCoordinate(val x: Int, val y: Int)

@Serializable
data class TerrainCell(
    val coordinate: SectorCoordinate,
    val terrain: TerrainType,
    val variant: Int,
)

@Serializable
data class LandingSiteCandidate(
    val id: String,
    val index: Int,
    val seed: Long,
    val cells: List<TerrainCell>,
    val counts: Map<TerrainType, Int>,
)

@Serializable
enum class Sustainability {
    @SerialName("renewable") RENEWABLE,
    @SerialName("finite") FINITE,
}

@Serializable
enum class DevelopmentKind {
    @SerialName("extract") EXTRACT,
    @SerialName("housing") HOUSING,
    @SerialName("power") POWER,
    @SerialName("industry") INDUSTRY,
    @SerialName("headquarters") HEADQUARTERS,
}

/**
 * Durable physical development attached to a world tile.
 *
 * Phase 3 consumes this model for simulation. Phase 4 adds the player-facing
 * construction/upgrade/demolition actions; it must extend this owner rather than
 * introduce a second building/site representation.
 */
@Serializable
data class TileDevelopment(
    val kind: DevelopmentKind,
    val level: Int = 1,
    val productionStopped: Boolean = false,
    val constructionComplete: Boolean = true,
) {
    init {
        require(level in 1..5) { "Tile development level must be between 1 and 5." }
    }
}

@Serializable
data class ResourceDeposit(
    val resourceId: ResourceId,
    val category: ResourceCategory,
    val name: String,
    val rarity: String,
    val multiplier: Double,
    val quality: Int,
    val requiredScanningLevel: Int,
    val requiredMiningLevel: Int,
    val requiredMiningTech: String,
    val terrainYieldFactor: Double,
    val sustainability: Sustainability,
    val abundance: Double? = null,
    val abundanceLabel: String? = null,
    val depositScale: String? = null,
    val reserve: Long? = null,
    val initialReserve: Long? = null,
    val renewableOriginalRank: Int? = null,
    val renewableHealth: Double? = null,
    val harvestIntensity: Double = 1.0,
    val renewableWiped: Boolean = false,
) {
    init {
        require(quality in 1..10_000) { "Resource quality must be between 1 and 10000." }
        require(harvestIntensity in .25..2.0) { "Harvest intensity must be between 0.25 and 2.0." }
        require(reserve == null || reserve >= 0) { "Resource reserve must not be negative." }
    }
}

@Serializable
data class WorldTile(
    val coordinate: SectorCoordinate,
    val terrain: TerrainType,
    val terrainVariant: Int,
    val revealed: Boolean = false,
    val lastScannedAtLevel: Int = 0,
    val resourceExhausted: Boolean = false,
    val deposit: ResourceDeposit? = null,
    val development: TileDevelopment? = null,
)

@Serializable
data class SurveyTask(
    val coordinate: SectorCoordinate,
    val totalDays: Int,
    val daysRemaining: Double,
    val scanningLevel: Int,
    val resurvey: Boolean,
)

@Serializable
data class WorldState(
    val landingCandidates: List<LandingSiteCandidate> = emptyList(),
    val selectedLandingSiteIndex: Int? = null,
    val tiles: List<WorldTile> = emptyList(),
    val activeSurveys: List<SurveyTask> = emptyList(),
    val surveyQueue: List<SectorCoordinate> = emptyList(),
) {
    val settled: Boolean get() = selectedLandingSiteIndex != null && tiles.isNotEmpty()

    fun tileAt(coordinate: SectorCoordinate): WorldTile? = tiles.firstOrNull { it.coordinate == coordinate }
}
