package com.mineit.android.domain.reputation

import com.mineit.android.domain.config.MineItConfig
import com.mineit.android.domain.model.GameState
import kotlin.math.abs
import kotlin.math.round

/** Source-compatible fractional corporate reputation owner. */
class ReputationService {
    fun normalize(value: Double): Double = round(value.coerceIn(-100.0, 100.0) * 100.0) / 100.0

    fun level(value: Double): ReputationLevel {
        val rep = normalize(value)
        return LEVELS.first { rep >= it.minimum && rep <= it.maximum }
    }

    fun change(state: GameState, delta: Double): ReputationChange {
        val before = normalize(state.company.reputation)
        val after = normalize(before + delta)
        val next = state.copy(company = state.company.copy(reputation = after))
        return ReputationChange(next, before, after, normalize(after - before), level(after))
    }

    fun awardBuyerShipment(state: GameState) = change(state, MineItConfig.BUYER_SHIPMENT_REPUTATION)
    fun awardCorporateExportVisit(state: GameState) = change(state, MineItConfig.CORPORATE_EXPORT_REPUTATION)
    fun awardColonyContract(state: GameState) = change(state, MineItConfig.CONTRACT_COMPLETION_REPUTATION)
    fun applyBuyerLoss(state: GameState, happinessLoss: Double) = change(state, -abs(happinessLoss) * .10)

    companion object {
        val LEVELS = listOf(
            ReputationLevel(1, "Disgraced", -100.0, -25.0),
            ReputationLevel(2, "Distrusted", -24.99, -10.0),
            ReputationLevel(3, "Questionable", -9.99, -.01),
            ReputationLevel(4, "Unknown", 0.0, 4.99),
            ReputationLevel(5, "Emerging", 5.0, 14.99),
            ReputationLevel(6, "Recognised", 15.0, 29.99),
            ReputationLevel(7, "Established", 30.0, 49.99),
            ReputationLevel(8, "Trusted", 50.0, 69.99),
            ReputationLevel(9, "Preferred", 70.0, 89.99),
            ReputationLevel(10, "Elite", 90.0, 100.0),
        )
    }
}

data class ReputationLevel(
    val level: Int,
    val name: String,
    val minimum: Double,
    val maximum: Double,
)

data class ReputationChange(
    val state: GameState,
    val before: Double,
    val after: Double,
    val delta: Double,
    val level: ReputationLevel,
)
