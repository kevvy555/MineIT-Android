package com.mineit.android.app

import com.mineit.android.domain.colony.ColonyEstablishmentAssessment
import com.mineit.android.domain.model.ColonyStatus
import com.mineit.android.domain.model.GameState
import com.mineit.android.domain.simulation.ColonyMetrics
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToLong

enum class ColonyAttentionSeverity { GOOD, WARNING, CRITICAL }

enum class ColonyAttentionTarget {
    COLONY,
    LANDING_SITE,
    CORPORATE_SHIP,
    PLAYER_SHIP,
    ESTABLISHMENT,
    FOOD,
    POWER,
    FUEL,
    INDUSTRY,
    HOUSING,
    ORE,
    PROBLEMS,
}

data class ColonyAttention(
    val severity: ColonyAttentionSeverity,
    val title: String,
    val detail: String,
    val actionLabel: String,
    val target: ColonyAttentionTarget,
)

data class CriticalResourceSnapshot(
    val foodDays: Double?,
    val fuelDays: Double?,
    val shipFoodDays: Double?,
    val shipName: String?,
)

data class CriticalResourceAlert(
    val summary: String,
    val foodDays: Double?,
    val fuelDays: Double?,
    val shipFoodDays: Double?,
    val shipName: String?,
)

/**
 * Source-compatible single-colony attention policy.
 *
 * This is application-derived state: it owns warning priority/threshold semantics that can pause
 * the simulation, while Compose only renders the result and dispatches the typed target.
 */
class ColonyAttentionPolicy {
    fun current(
        state: GameState,
        metrics: ColonyMetrics,
        establishment: ColonyEstablishmentAssessment,
    ): ColonyAttention {
        val colony = state.activeColony
        if (colony.status == ColonyStatus.DEAD) {
            return ColonyAttention(
                ColonyAttentionSeverity.CRITICAL,
                "COLONY LOST",
                "Population has reached zero.",
                "OPEN COLONY",
                ColonyAttentionTarget.COLONY,
            )
        }
        if (colony.status == ColonyStatus.SITE_SELECTION) {
            return ColonyAttention(
                ColonyAttentionSeverity.WARNING,
                "CHOOSE A LANDING SITE",
                "Select one of the surveyed terrain candidates before starting colony operations.",
                "SELECT SITE ›",
                ColonyAttentionTarget.LANDING_SITE,
            )
        }
        if (colony.trade.active) {
            return ColonyAttention(
                ColonyAttentionSeverity.WARNING,
                "CORPORATE SHIP DOCKED",
                "Resolve trade before corporation time can continue.",
                "OPEN SHIP ›",
                ColonyAttentionTarget.CORPORATE_SHIP,
            )
        }

        val shipDays = establishment.shipFoodDaysRemaining
        if (establishment.shipResidents > .0001 && shipDays != null && shipDays <= 30.0) {
            val ship = establishment.foundingShipId?.let { id -> state.fleet.ships.firstOrNull { it.id == id } }
            val days = max(0, shipDays.toInt())
            if (days <= 0) {
                val grace = max(0, 30 - (ship?.residentFoodStarvationDays ?: 0))
                return ColonyAttention(
                    ColonyAttentionSeverity.CRITICAL,
                    if (grace > 0) "SHIP RESIDENTS STARVING — DEATHS IN ${grace}d" else "SHIP RESIDENT DEATHS ACTIVE",
                    "${establishment.foundingShipName ?: "Founding ship"} has no Food for ${formatNumber(establishment.shipResidents)} residents.",
                    "OPEN SHIP ›",
                    ColonyAttentionTarget.PLAYER_SHIP,
                )
            }
            val critical = shipDays < 10.0
            return ColonyAttention(
                if (critical) ColonyAttentionSeverity.CRITICAL else ColonyAttentionSeverity.WARNING,
                "SHIP FOOD ${if (critical) "CRITICAL" else "LOW"} — ${days}d",
                "${establishment.foundingShipName ?: "Founding ship"} has the shortest occupied-ship Food runway. Colony Food is separate.",
                "OPEN SHIP ›",
                ColonyAttentionTarget.PLAYER_SHIP,
            )
        }

        if (metrics.foodSupply < .9 || metrics.foodDays?.let { it <= 30.0 } == true) {
            val critical = metrics.foodSupply < .5 || metrics.foodDays?.let { it <= 10.0 } == true
            return ColonyAttention(
                if (critical) ColonyAttentionSeverity.CRITICAL else ColonyAttentionSeverity.WARNING,
                "FOOD LOW — ${daysText(metrics.foodDays)}",
                "Food demand is consuming reserves faster than production.",
                "SHOW FOOD ›",
                ColonyAttentionTarget.FOOD,
            )
        }

        if (metrics.powerFactor < .95 || metrics.powerDemand > metrics.powerCapacity + .0001) {
            return ColonyAttention(
                if (metrics.powerFactor < .6) ColonyAttentionSeverity.CRITICAL else ColonyAttentionSeverity.WARNING,
                "POWER SHORTAGE",
                "Demand ${formatNumber(metrics.powerDemand)} • delivered ${formatNumber(metrics.powerDemand * metrics.powerFactor)} • Fuel-limited ${formatNumber(metrics.powerFuelLimitedGeneration)}.",
                "SHOW POWER ›",
                ColonyAttentionTarget.POWER,
            )
        }

        if (metrics.fuelSupply < .9 || metrics.fuelDays?.let { it <= 30.0 } == true) {
            val critical = metrics.fuelSupply < .5 || metrics.fuelDays?.let { it <= 10.0 } == true
            return ColonyAttention(
                if (critical) ColonyAttentionSeverity.CRITICAL else ColonyAttentionSeverity.WARNING,
                "FUEL LOW — ${daysText(metrics.fuelDays)}",
                "Power demand is drawing Fuel reserves down.",
                "SHOW FUEL ›",
                ColonyAttentionTarget.FUEL,
            )
        }

        if (metrics.workforceRequired > metrics.workforceAvailable + .0001) {
            return ColonyAttention(
                ColonyAttentionSeverity.WARNING,
                "WORKFORCE SHORTAGE",
                "${formatNumber(metrics.workforceRequired)} required • ${formatNumber(metrics.workforceAvailable)} available.",
                "COLONY ›",
                ColonyAttentionTarget.COLONY,
            )
        }

        if (metrics.industryCommercialFactor < .999) {
            return ColonyAttention(
                ColonyAttentionSeverity.WARNING,
                "INDUSTRY OVERLOAD",
                "Build/Ore operations are running at ${percent(metrics.industryCommercialFactor)}.",
                "SHOW INDUSTRY ›",
                ColonyAttentionTarget.INDUSTRY,
            )
        }

        // N05 correction: only residents living ashore consume planetary Housing.
        val housing = establishment.housingCapacity
        val planetaryResidents = metrics.planetaryResidents
        if (housing > .0001 && planetaryResidents / housing > .9) {
            return ColonyAttention(
                ColonyAttentionSeverity.WARNING,
                "HOUSING NEAR CAPACITY",
                "${formatNumber(max(0.0, housing - planetaryResidents))} spaces remain.",
                "SHOW HOUSING ›",
                ColonyAttentionTarget.HOUSING,
            )
        }

        if (metrics.oreDays?.let { it <= 20.0 } == true) {
            return ColonyAttention(
                ColonyAttentionSeverity.WARNING,
                "ORE LOW — ${daysText(metrics.oreDays)}",
                "Industry is consuming Ore reserves faster than replacement.",
                "SHOW ORE ›",
                ColonyAttentionTarget.ORE,
            )
        }

        if (establishment.required && !establishment.acknowledged) {
            return ColonyAttention(
                ColonyAttentionSeverity.WARNING,
                "BEGIN COLONY ESTABLISHMENT",
                "Review the founding handover before starting time.",
                "OPEN CHECKLIST ›",
                ColonyAttentionTarget.ESTABLISHMENT,
            )
        }

        return ColonyAttention(
            ColonyAttentionSeverity.GOOD,
            "COLONY STABLE",
            "No immediate operational constraint needs attention.",
            "PROBLEMS ›",
            ColonyAttentionTarget.PROBLEMS,
        )
    }

    fun criticalSnapshot(
        metrics: ColonyMetrics,
        establishment: ColonyEstablishmentAssessment,
    ): CriticalResourceSnapshot = CriticalResourceSnapshot(
        foodDays = metrics.foodDays,
        fuelDays = metrics.fuelDays,
        shipFoodDays = establishment.shipFoodDaysRemaining.takeIf { establishment.shipResidents > .0001 },
        shipName = establishment.foundingShipName,
    )

    private fun daysText(days: Double?): String = when {
        days == null -> "SURPLUS"
        days <= 0.0 -> "EMPTY"
        else -> "${max(1.0, ceil(days)).toInt()}d"
    }

    private fun percent(value: Double): String = "${(value.coerceIn(0.0, 1.0) * 100.0).roundToLong()}%"

    private fun formatNumber(value: Double): String = when {
        value >= 1_000.0 -> "%.1fk".format(value / 1_000.0)
        value % 1.0 == 0.0 -> value.roundToLong().toString()
        else -> "%.1f".format(value)
    }
}

/** One alert per continuous critical episode, matching the maintained web warning behaviour. */
class CriticalResourceEpisodeTracker {
    private var foodCritical = false
    private var fuelCritical = false
    private var shipFoodCritical = false

    fun next(snapshot: CriticalResourceSnapshot): CriticalResourceAlert? {
        val nextFood = snapshot.foodDays?.let { it <= 10.0 } == true
        val nextFuel = snapshot.fuelDays?.let { it <= 10.0 } == true
        val nextShipFood = snapshot.shipFoodDays?.let { it < 10.0 } == true

        val entered = buildList {
            if (nextFood && !foodCritical) add("FOOD has 10 days or less remaining")
            if (nextFuel && !fuelCritical) add("FUEL has 10 days or less remaining")
            if (nextShipFood && !shipFoodCritical) add("SHIP FOOD has less than 10 days remaining")
        }

        foodCritical = nextFood
        fuelCritical = nextFuel
        shipFoodCritical = nextShipFood

        if (entered.isEmpty()) return null
        return CriticalResourceAlert(
            summary = entered.joinToString(" • "),
            foodDays = snapshot.foodDays,
            fuelDays = snapshot.fuelDays,
            shipFoodDays = snapshot.shipFoodDays,
            shipName = snapshot.shipName,
        )
    }

    fun reset() {
        foodCritical = false
        fuelCritical = false
        shipFoodCritical = false
    }
}
