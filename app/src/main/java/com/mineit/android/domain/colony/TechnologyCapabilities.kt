package com.mineit.android.domain.colony

import com.mineit.android.domain.model.TechnologyLevels

/** Current deployed technology effects required by the native daily simulation. */
object TechnologyCapabilities {
    private val powerFuelIntensity = listOf(.10, .085, .070, .050, .035)
    private val foodProduction = listOf(1.0, 1.12, 1.28, 1.48, 1.72)
    private val syntheticFood = listOf(0.0, 0.0, 15.0, 30.0, 55.0)
    private val industryWorkforce = listOf(1.0, .96, .91, .85, .78)
    private val industryOre = listOf(1.0, .96, .90, .83, .75)
    private val miningWorkforce = listOf(1.0, .96, .92, .88, .84, .80, .76, .72, .68, .65)

    fun fuelIntensity(levels: TechnologyLevels): Double = tier(powerFuelIntensity, levels.power)
    fun foodProductionMultiplier(levels: TechnologyLevels): Double = tier(foodProduction, levels.food)
    fun syntheticFood(levels: TechnologyLevels): Double = tier(syntheticFood, levels.food)
    fun industryWorkforceEfficiency(levels: TechnologyLevels): Double = tier(industryWorkforce, levels.industry)
    fun industryOreEfficiency(levels: TechnologyLevels): Double = tier(industryOre, levels.industry)
    fun miningWorkforceEfficiency(levels: TechnologyLevels): Double = tier(miningWorkforce, levels.mining)

    /** Food production currently uses the same level-one workforce efficiency path as the web baseline. */
    fun foodWorkforceEfficiency(levels: TechnologyLevels): Double = when (levels.food.coerceIn(1, 5)) {
        else -> 1.0
    }

    private fun tier(values: List<Double>, level: Int): Double = values[level.coerceIn(1, values.size) - 1]
}
