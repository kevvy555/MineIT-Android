package com.mineit.android.domain.config

/**
 * Typed subset of the pinned MineIT web configuration needed by the Phase 2 domains.
 * Add constants here only when their owning native subsystem is migrated.
 */
object MineItConfig {
    const val DAYS_PER_YEAR = 360
    const val GRID_SIZE = 8
    const val START_CASH = 32_000L
    const val START_POPULATION = 120
    const val START_HOUSING = 180
    const val START_INDUSTRY_LEVEL = 1
    const val START_FOOD = 1_300.0
    const val START_BUILD = 520.0
    const val START_FUEL = 420.0
    const val START_ORE = 260.0

    val SITE_OUTPUT_LEVELS = listOf(10.0, 18.0, 30.0, 48.0, 72.0, 105.0, 145.0, 195.0, 255.0, 330.0)
}
