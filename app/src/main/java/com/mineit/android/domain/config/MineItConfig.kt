package com.mineit.android.domain.config

/**
 * Typed subset of the pinned MineIT web configuration needed by migrated native domains.
 * Add constants here only when their owning native subsystem is migrated.
 */
object MineItConfig {
    const val DAYS_PER_YEAR = 360
    const val DAY_MS = 360L
    const val GRID_SIZE = 8
    const val START_CASH = 32_000L
    const val START_POPULATION = 120.0
    const val START_HOUSING = 180
    const val START_INDUSTRY_LEVEL = 1
    const val START_FOOD = 1_300.0
    const val START_BUILD = 520.0
    const val START_FUEL = 420.0
    const val START_ORE = 260.0

    const val FOOD_PER_COLONIST = .12
    const val LIFE_SUPPORT_POWER_PER_COLONIST = .07
    const val ORE_PER_INDUSTRY_LEVEL = 2.0
    const val INDUSTRY_SITE_LOAD_BASE = 20.0
    const val INDUSTRY_SITE_LOAD_GROWTH = 1.45
    const val WORKFORCE_SHARE = .50
    const val SITE_WORKFORCE_BASE = 12.0
    const val SITE_WORKFORCE_GROWTH = 1.60
    const val RENEWABLE_OVERHARVEST_RATE = .004
    const val RENEWABLE_RECOVERY_RATE = .003
    const val EMERGENCY_LIFE_SUPPORT_MULTIPLIER = .72
    const val CRITICAL_MORTALITY_MIN = .0005
    const val CRITICAL_MORTALITY_MAX = .002
    const val COLLAPSE_MORTALITY_MAX = .03
    const val COLONY_DEATH_REPUTATION_PENALTY = 2

    val SITE_OUTPUT_LEVELS = listOf(10.0, 18.0, 30.0, 48.0, 72.0, 105.0, 145.0, 195.0, 255.0, 330.0)
}
