package com.mineit.android.domain.config

/**
 * Typed subset of the pinned MineIT web configuration needed by migrated native domains.
 * Add constants here only when their owning native subsystem is migrated.
 */
object MineItConfig {
    const val DAYS_PER_YEAR = 360
    const val DAY_MS = 360L
    const val GRID_SIZE = 8
    const val START_CASH = 32_000.0
    const val START_POPULATION = 120.0
    const val START_HOUSING = 180
    const val START_INDUSTRY_LEVEL = 1
    const val START_FOOD = 1_300.0
    const val START_BUILD = 520.0
    const val START_FUEL = 420.0
    const val START_ORE = 260.0

    const val MAX_CONTRACT_EXTENSIONS = 3
    const val RENEWAL_YEARS = 5
    const val HOLDOVER_COST_MULTIPLIER = 1.35
    const val LIABILITY_COST_MULTIPLIER = 1.90
    const val RETURN_MIN_RESOURCE_RATIO = .12

    const val TRADE_INTERVAL_DAYS = 180
    const val FIRST_TRADE_DAY = 181
    const val CORPORATE_BUY_MARKUP = 1.5
    const val RESOURCE_VALUE_SCALE = 5.0
    const val TRADE_BASE_CARGO = 4_000.0
    const val TRADE_CARGO_PER_REP = 250.0
    const val TRADE_MAX_CARGO = 12_000.0
    const val TRADE_BASE_EXPORT_CARGO = 100_000.0
    const val TRADE_EXPORT_PER_REP = 10_000.0
    const val TRADE_MAX_EXPORT_CARGO = 500_000.0
    const val TRADE_PASSENGER_CAPACITY = 250
    const val COLONIST_TRANSFER_COST = 250.0
    const val INDUSTRY_PROCESSING_MAX_BONUS = .50
    const val LOG_MAX_EVENTS = 10_000

    const val BUYER_SHIPMENT_REPUTATION = .01
    const val CORPORATE_EXPORT_REPUTATION = .01
    const val CONTRACT_COMPLETION_REPUTATION = .10
    val BUYER_COLLECTION_ATTEMPT_OFFSETS = listOf(0, 5, 10, 15)

    const val FOOD_PER_COLONIST = .12
    const val LIFE_SUPPORT_POWER_PER_COLONIST = .07
    const val ORE_PER_INDUSTRY_LEVEL = 2.0
    const val INDUSTRY_SITE_LOAD_BASE = 20.0
    const val INDUSTRY_SITE_LOAD_GROWTH = 1.45
    const val WORKFORCE_SHARE = .50
    const val SITE_WORKFORCE_BASE = 12.0
    const val SITE_WORKFORCE_GROWTH = 1.60
    const val SITE_DEVELOP_BASE_BUILD = 45.0
    const val RENEWABLE_OVERHARVEST_RATE = .004
    const val RENEWABLE_RECOVERY_RATE = .003
    const val EMERGENCY_LIFE_SUPPORT_MULTIPLIER = .72
    const val CRITICAL_MORTALITY_MIN = .0005
    const val CRITICAL_MORTALITY_MAX = .002
    const val COLLAPSE_MORTALITY_MAX = .03
    const val COLONY_DEATH_REPUTATION_PENALTY = 2.0

    val SITE_OUTPUT_LEVELS = listOf(10.0, 18.0, 30.0, 48.0, 72.0, 105.0, 145.0, 195.0, 255.0, 330.0)
    val SITE_COMPLEXITY_COSTS = listOf(1.0, 1.3, 1.7, 2.3, 3.0, 4.0, 5.5, 7.0, 9.0, 12.0)
}
