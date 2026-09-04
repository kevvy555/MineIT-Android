package com.mineit.android.domain.colony

import com.mineit.android.domain.resources.ExtractionFamily
import com.mineit.android.domain.world.DevelopmentKind
import com.mineit.android.domain.world.TerrainType
import com.mineit.android.domain.world.TileDevelopment
import kotlin.math.roundToInt

/** Canonical physical infrastructure curves from MineIT 5.13.15. */
object InfrastructureRules {
    const val FOUNDING_SHIP_INDUSTRY = 50.0
    const val BASIC_SPACEPORT_POWER = 10.0
    const val INDUSTRY_VARIABLE_POWER_PER_CAPACITY = .25
    const val DEMOLITION_RECOVERY = .25
    const val MAX_LEVEL = 5

    const val HEADQUARTERS_BONUS_PER_LEVEL = .02
    const val HEADQUARTERS_BONUS_CAP = .15
    const val HEADQUARTERS_OVERLOAD_PENALTY_CAP = .50
    const val HEADQUARTERS_OVERLOAD_PENALTY_PER_RATIO = .50
    const val TEMPORARY_SHIP_COMMAND_CAPACITY = 16.0

    private val housingCapacity = listOf(160.0, 360.0, 650.0, 1_050.0, 1_600.0)
    private val powerGeneration = listOf(75.0, 165.0, 300.0, 500.0, 800.0)
    private val industryCapacity = listOf(100.0, 230.0, 420.0, 700.0, 1_100.0)
    private val headquartersCapacity = listOf(16.0, 36.0, 64.0, 100.0, 150.0)

    private val housingFixedPower = listOf(1.0, 2.0, 4.0, 7.0, 11.0)
    private val headquartersPower = listOf(1.0, 2.0, 4.0, 7.0, 11.0)
    private val industryIdlePower = listOf(3.0, 7.0, 14.0, 24.0, 38.0)
    private val headquartersStaff = listOf(5.0, 10.0, 18.0, 28.0, 40.0)

    private val buildingBuildCost = mapOf(
        DevelopmentKind.HOUSING to listOf(55.0, 95.0, 165.0, 285.0, 480.0),
        DevelopmentKind.POWER to listOf(70.0, 125.0, 220.0, 390.0, 680.0),
        DevelopmentKind.INDUSTRY to listOf(80.0, 145.0, 255.0, 435.0, 720.0),
        DevelopmentKind.HEADQUARTERS to listOf(90.0, 170.0, 300.0, 510.0, 850.0),
    )
    private val buildingOreCost = mapOf(
        DevelopmentKind.HOUSING to listOf(0.0, 10.0, 25.0, 50.0, 90.0),
        DevelopmentKind.POWER to listOf(0.0, 15.0, 45.0, 90.0, 170.0),
        DevelopmentKind.INDUSTRY to listOf(0.0, 20.0, 55.0, 110.0, 200.0),
        DevelopmentKind.HEADQUARTERS to listOf(0.0, 25.0, 65.0, 130.0, 240.0),
    )

    private val facilityPower = mapOf(
        ExtractionFamily.FARM to listOf(2.0, 5.0, 10.0, 18.0, 30.0),
        ExtractionFamily.RANCH to listOf(2.0, 5.0, 9.0, 16.0, 26.0),
        ExtractionFamily.BIO to listOf(3.0, 7.0, 13.0, 22.0, 35.0),
        ExtractionFamily.ALGAE to listOf(3.0, 7.0, 13.0, 22.0, 35.0),
        ExtractionFamily.QUARRY to listOf(4.0, 9.0, 17.0, 29.0, 46.0),
        ExtractionFamily.RIG to listOf(4.0, 10.0, 19.0, 33.0, 52.0),
        ExtractionFamily.MINE to listOf(5.0, 12.0, 23.0, 40.0, 64.0),
        ExtractionFamily.DEEP_MINE to listOf(7.0, 16.0, 31.0, 54.0, 86.0),
    )
    private val facilityPowerUpgradeGate = mapOf(
        ExtractionFamily.FARM to listOf(0.0, 90.0, 190.0, 340.0, 550.0),
        ExtractionFamily.RANCH to listOf(0.0, 90.0, 190.0, 340.0, 550.0),
        ExtractionFamily.BIO to listOf(0.0, 100.0, 210.0, 370.0, 600.0),
        ExtractionFamily.ALGAE to listOf(0.0, 100.0, 210.0, 370.0, 600.0),
        ExtractionFamily.QUARRY to listOf(0.0, 110.0, 225.0, 400.0, 650.0),
        ExtractionFamily.RIG to listOf(0.0, 115.0, 235.0, 420.0, 680.0),
        ExtractionFamily.MINE to listOf(0.0, 125.0, 250.0, 440.0, 710.0),
        ExtractionFamily.DEEP_MINE to listOf(0.0, 140.0, 280.0, 470.0, 750.0),
    )

    fun capacity(development: TileDevelopment): Double = when (development.kind) {
        DevelopmentKind.HOUSING -> tier(housingCapacity, development.level)
        DevelopmentKind.POWER -> tier(powerGeneration, development.level)
        DevelopmentKind.INDUSTRY -> tier(industryCapacity, development.level)
        DevelopmentKind.HEADQUARTERS -> tier(headquartersCapacity, development.level)
        DevelopmentKind.EXTRACT -> 0.0
    }

    fun headquartersCapacity(level: Int): Double = tier(headquartersCapacity, level)
    fun headquartersMinimumStaff(level: Int): Double = tier(headquartersStaff, level)
    fun headquartersPower(level: Int): Double = tier(headquartersPower, level)
    fun housingFixedPower(level: Int): Double = tier(housingFixedPower, level)
    fun industryIdlePower(level: Int): Double = tier(industryIdlePower, level)
    fun facilityPower(family: ExtractionFamily, level: Int): Double = tier(facilityPower.getValue(family), level)
    fun facilityUpgradePowerGate(family: ExtractionFamily, level: Int): Double = tier(facilityPowerUpgradeGate.getValue(family), level)

    fun buildingCost(kind: DevelopmentKind, level: Int, terrain: TerrainType): InfrastructureCost {
        require(kind != DevelopmentKind.EXTRACT) { "Extraction sites use extraction-site costs." }
        val multiplier = terrainMultiplier(kind, terrain)
        require(multiplier.isFinite()) { "Standard buildings cannot be placed on lakes." }
        return InfrastructureCost(
            build = (tier(buildingBuildCost.getValue(kind), level) * multiplier).roundToInt().toDouble(),
            ore = (tier(buildingOreCost.getValue(kind), level) * multiplier).roundToInt().toDouble(),
        )
    }

    fun terrainMultiplier(kind: DevelopmentKind, terrain: TerrainType): Double {
        if (terrain == TerrainType.LAKE) return Double.POSITIVE_INFINITY
        return when (kind) {
            DevelopmentKind.HOUSING -> when (terrain) {
                TerrainType.HILL -> 1.20
                TerrainType.MOUNTAIN -> 1.65
                else -> 1.0
            }
            DevelopmentKind.INDUSTRY -> when (terrain) {
                TerrainType.HILL -> 1.15
                TerrainType.MOUNTAIN -> 1.45
                else -> 1.0
            }
            DevelopmentKind.POWER -> when (terrain) {
                TerrainType.HILL -> .95
                TerrainType.MOUNTAIN -> 1.15
                else -> 1.0
            }
            DevelopmentKind.HEADQUARTERS -> 1.0
            DevelopmentKind.EXTRACT -> 1.0
        }
    }

    fun commandWeight(kind: DevelopmentKind, category: com.mineit.android.domain.resources.ResourceCategory? = null): Double = when (kind) {
        DevelopmentKind.HOUSING -> 1.0
        DevelopmentKind.POWER -> 2.0
        DevelopmentKind.INDUSTRY -> 3.0
        DevelopmentKind.HEADQUARTERS -> 0.0
        DevelopmentKind.EXTRACT -> if (category == com.mineit.android.domain.resources.ResourceCategory.ORE) 3.0 else 2.0
    }

    private fun tier(values: List<Double>, level: Int): Double = values[level.coerceIn(1, MAX_LEVEL) - 1]
}

data class InfrastructureCost(val build: Double, val ore: Double)
