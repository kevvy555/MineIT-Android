package com.mineit.android.domain.colony

import com.mineit.android.domain.resources.ExtractionFamily
import com.mineit.android.domain.world.DevelopmentKind
import com.mineit.android.domain.world.TileDevelopment

/**
 * Current physical infrastructure curves from the pinned MineIT 5.13.15 baseline.
 * Phase 3 uses these for simulation; Phase 4 reuses the same owner for build/upgrade rules.
 */
object InfrastructureRules {
    const val FOUNDING_SHIP_INDUSTRY = 50.0
    const val BASIC_SPACEPORT_POWER = 10.0
    const val INDUSTRY_VARIABLE_POWER_PER_CAPACITY = .25

    private val housingCapacity = listOf(160.0, 360.0, 650.0, 1_050.0, 1_600.0)
    private val powerGeneration = listOf(75.0, 165.0, 300.0, 500.0, 800.0)
    private val industryCapacity = listOf(100.0, 230.0, 420.0, 700.0, 1_100.0)
    private val housingFixedPower = listOf(1.0, 2.0, 4.0, 7.0, 11.0)
    private val industryIdlePower = listOf(3.0, 7.0, 14.0, 24.0, 38.0)

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

    fun capacity(development: TileDevelopment): Double = when (development.kind) {
        DevelopmentKind.HOUSING -> tier(housingCapacity, development.level)
        DevelopmentKind.POWER -> tier(powerGeneration, development.level)
        DevelopmentKind.INDUSTRY -> tier(industryCapacity, development.level)
        DevelopmentKind.EXTRACT,
        DevelopmentKind.HEADQUARTERS,
        -> 0.0
    }

    fun housingFixedPower(level: Int): Double = tier(housingFixedPower, level)

    fun industryIdlePower(level: Int): Double = tier(industryIdlePower, level)

    fun facilityPower(family: ExtractionFamily, level: Int): Double =
        tier(facilityPower.getValue(family), level)

    private fun tier(values: List<Double>, level: Int): Double = values[level.coerceIn(1, 5) - 1]
}
