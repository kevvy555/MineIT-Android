package com.mineit.android.domain.poc

object PocSimulation {
    fun advanceDay(state: PocGameState): PocGameState {
        val nextDay = if (state.day >= DAYS_PER_YEAR) 1 else state.day + 1
        val nextYear = if (state.day >= DAYS_PER_YEAR) state.year + 1 else state.year
        val productionFactor = if (state.colony.powerAvailable >= state.colony.powerDemand) 1.0 else 0.5

        val updatedResources = state.resources.copy(
            food = (state.resources.food - DAILY_FOOD_USE).coerceAtLeast(0),
            water = (state.resources.water - DAILY_WATER_USE).coerceAtLeast(0),
            ore = state.resources.ore + (DAILY_ORE_OUTPUT * productionFactor).toInt(),
        )

        return state.copy(
            year = nextYear,
            day = nextDay,
            resources = updatedResources,
        )
    }

    private const val DAYS_PER_YEAR = 365
    private const val DAILY_FOOD_USE = 12
    private const val DAILY_WATER_USE = 10
    private const val DAILY_ORE_OUTPUT = 8
}
