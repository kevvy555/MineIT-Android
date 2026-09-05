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

@Serializable
enum class ExtractionOperatingMode {
    @SerialName("normal") NORMAL,
    @SerialName("pushed") PUSHED,
    @SerialName("hard") HARD,
}

@Serializable
enum class ExtractionAccidentOutcome {
    @SerialName("machinery") MACHINERY,
    @SerialName("fatalities") FATALITIES,
}

@Serializable
data class ExtractionAccidentRecord(
    val name: String,
    val family: String,
    val outcome: ExtractionAccidentOutcome,
    val deaths: Int,
    val shutdownDays: Int,
    val mode: ExtractionOperatingMode,
    val year: Int,
    val day: Int,
)

/** Permanent physical development state shared by construction and simulation. */
@Serializable
data class TileDevelopment(
    val kind: DevelopmentKind,
    val level: Int = 1,
    val productionStopped: Boolean = false,
    val constructionComplete: Boolean = true,
    val investedBuild: Double = 0.0,
    val investedOre: Double = 0.0,
    val operatingMode: ExtractionOperatingMode = ExtractionOperatingMode.NORMAL,
    val overdriveExposure: Double = 0.0,
    val overdriveRiskChecks: Int = 0,
    val accidentShutdownDays: Int = 0,
    val lastAccident: ExtractionAccidentRecord? = null,
) {
    init {
        require(level in 1..5) { "Tile development level must be between 1 and 5." }
        require(investedBuild.isFinite() && investedBuild >= 0.0) { "Invested Build must be non-negative." }
        require(investedOre.isFinite() && investedOre >= 0.0) { "Invested Ore must be non-negative." }
        require(overdriveExposure.isFinite() && overdriveExposure >= 0.0) { "Overdrive exposure must be finite and non-negative." }
        require(overdriveRiskChecks >= 0) { "Overdrive risk checks must not be negative." }
        require(accidentShutdownDays >= 0) { "Accident shutdown days must not be negative." }
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
    val exhaustedResourceId: ResourceId? = null,
    val resourceCovered: Boolean = false,
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
