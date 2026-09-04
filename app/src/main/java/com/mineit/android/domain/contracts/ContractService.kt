package com.mineit.android.domain.contracts

import com.mineit.android.domain.config.MineItConfig
import com.mineit.android.domain.model.ColonyState
import com.mineit.android.domain.model.ColonyStatus
import com.mineit.android.domain.model.GameState
import com.mineit.android.domain.reputation.ReputationService
import com.mineit.android.domain.world.Sustainability
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.round

/** Canonical native owner for colony-contract deadlines, scoring and post-contract decisions. */
class ContractService(
    private val reputationService: ReputationService = ReputationService(),
) {
    fun ageDays(state: GameState, colony: ColonyState = state.activeColony): Int {
        val contract = colony.contract ?: return 0
        return max(1, state.date.toAbsoluteDay().value - contract.startAbsoluteDay + 1)
    }

    fun contractDate(state: GameState, colony: ColonyState = state.activeColony): Pair<Int, Int> {
        val age = ageDays(state, colony)
        return ((age - 1) / MineItConfig.DAYS_PER_YEAR + 1) to ((age - 1) % MineItConfig.DAYS_PER_YEAR + 1)
    }

    fun deadlineDays(contract: ContractState): Int = (contract.years + contract.extensionYears) * MineItConfig.DAYS_PER_YEAR

    fun score(
        state: GameState,
        foodMetric: Double,
        industryMetric: Double,
        colony: ColonyState = state.activeColony,
    ): ContractScore? {
        val contract = colony.contract ?: return null
        val profit = contract.localRevenue - contract.localCosts
        val foodRatio = if (contract.goals.food > 0) foodMetric / contract.goals.food else 1.0
        val industryRatio = if (contract.goals.industry > 0) industryMetric / contract.goals.industry else 1.0
        val populationRatio = if (contract.goals.population > 0) colony.population / contract.goals.population else 1.0
        val minimumRatio = minOf(foodRatio, industryRatio, populationRatio)
        val passed = colony.status != ColonyStatus.DEAD && foodRatio >= 1.0 && industryRatio >= 1.0 && populationRatio >= 1.0
        val medal = when {
            passed && profit >= contract.bands.platinum && minimumRatio >= 1.5 -> ContractMedal.PLATINUM
            passed && profit >= contract.bands.gold && minimumRatio >= 1.2 -> ContractMedal.GOLD
            passed && profit >= contract.bands.silver -> ContractMedal.SILVER
            passed -> ContractMedal.BRONZE
            else -> ContractMedal.NONE
        }
        return ContractScore(passed, medal, profit, foodRatio, industryRatio, populationRatio, minimumRatio)
    }

    fun deadlineState(
        state: GameState,
        foodMetric: Double,
        industryMetric: Double,
        operatingColonyCount: Int = state.colonies.count(::isOperating),
    ): ContractDeadlineState? {
        val colony = state.activeColony
        val contract = colony.contract ?: return null
        if (colony.status == ColonyStatus.DEAD || colony.status != ColonyStatus.PLAYING) return null
        if (ageDays(state, colony) <= deadlineDays(contract)) return null
        if (contract.completed) return ContractDeadlineState.RENEWAL_ENDED
        if (score(state, foodMetric, industryMetric, colony)?.passed == true) return ContractDeadlineState.COMPLETE
        if (contract.extensionsUsed < MineItConfig.MAX_CONTRACT_EXTENSIONS) {
            val fee = extensionFee(contract)
            return if (operatingColonyCount <= 1 && state.company.cash + .0001 < fee) {
                ContractDeadlineState.CORPORATION_FAILED
            } else ContractDeadlineState.EXTENSION
        }
        return ContractDeadlineState.FAILED
    }

    fun extensionFee(contract: ContractState): Double = 25_000.0 * (contract.extensionsUsed + 1) * contract.tier

    fun renewalFee(contract: ContractState): Double = round(
        100_000.0 * contract.tier * (1.0 + .35 * (contract.colonyTier - 1)) * 2.1.pow(contract.renewals),
    )

    fun awardCompletion(state: GameState): ContractActionResult {
        val contract = state.activeColony.contract ?: return failed(state, "No active colony contract.")
        if (contract.completed && contract.completionAwarded) return ContractActionResult(state, true, "Contract completion already awarded.")
        var next = state
        var completion = contract.copy(completed = true)
        if (!completion.completionAwarded) {
            val reputation = reputationService.awardColonyContract(next)
            next = reputation.state.copy(company = reputation.state.company.copy(wins = state.company.wins + 1))
            completion = completion.copy(completionAwarded = true)
        }
        next = replaceActive(next, next.activeColony.copy(contract = completion, status = ColonyStatus.HOLDOVER))
        return ContractActionResult(next, true, "Contract complete. Colony entered holdover.")
    }

    fun extend(state: GameState): ContractActionResult {
        val colony = state.activeColony
        val contract = colony.contract ?: return failed(state, "No active colony contract.")
        if (contract.extensionsUsed >= MineItConfig.MAX_CONTRACT_EXTENSIONS) return failed(state, "No contract extensions remain.")
        val fee = extensionFee(contract)
        if (state.company.cash + .0001 < fee) return failed(state, "Insufficient corporate cash for extension.")
        val updated = contract.copy(
            extensionYears = contract.extensionYears + 1,
            extensionsUsed = contract.extensionsUsed + 1,
            localCosts = contract.localCosts + fee,
            pendingDecision = null,
            pendingDecisionPreviousStatus = null,
        )
        val next = replaceActive(
            state.copy(company = state.company.copy(cash = state.company.cash - fee)),
            colony.copy(contract = updated, status = ColonyStatus.PLAYING),
        )
        return ContractActionResult(next, true, "Contract extended by one year.", fee)
    }

    fun renew(state: GameState): ContractActionResult {
        val colony = state.activeColony
        val contract = colony.contract ?: return failed(state, "No colony contract.")
        if (!contract.completed || contract.ended || colony.status == ColonyStatus.DEAD) return failed(state, "Contract is not eligible for renewal.")
        val fee = renewalFee(contract)
        if (state.company.cash + .0001 < fee) return failed(state, "Insufficient corporate cash for renewal.")
        val updated = contract.copy(
            renewals = contract.renewals + 1,
            extensionYears = contract.extensionYears + MineItConfig.RENEWAL_YEARS,
            localCosts = contract.localCosts + fee,
            pendingDecision = null,
            pendingDecisionPreviousStatus = null,
        )
        val next = replaceActive(
            state.copy(company = state.company.copy(cash = state.company.cash - fee)),
            colony.copy(contract = updated, status = ColonyStatus.PLAYING),
        )
        return ContractActionResult(next, true, "Contract renewed for ${MineItConfig.RENEWAL_YEARS} years.", fee)
    }

    fun failCorporation(state: GameState, reason: String): ContractActionResult {
        val colony = state.activeColony
        val contract = colony.contract ?: return failed(state, "No active colony contract.")
        val (year, day) = contractDate(state, colony)
        val updated = contract.copy(
            ended = true,
            failedByContract = true,
            failureReason = reason,
            failureYear = year,
            failureDay = day,
            pendingDecision = null,
        )
        val nextColony = colony.copy(
            contract = updated,
            status = ColonyStatus.CONTRACT_FAILED,
            trade = colony.trade.copy(active = false),
        )
        return ContractActionResult(
            replaceActive(state.copy(company = state.company.copy(gameOver = true)), nextColony),
            true,
            reason,
        )
    }

    fun endAsLiability(state: GameState): ContractActionResult {
        val colony = state.activeColony
        val contract = colony.contract ?: return failed(state, "No colony contract.")
        val updated = contract.copy(ended = true, pendingDecision = null, pendingDecisionPreviousStatus = null)
        return ContractActionResult(
            replaceActive(state, colony.copy(contract = updated, status = ColonyStatus.LIABILITY, trade = colony.trade.copy(active = false))),
            true,
            "Contract ended; colony retained as a corporate liability.",
        )
    }

    fun resourceHealth(colony: ColonyState): ResourceHealth {
        val deposits = colony.world.tiles.filter { it.revealed }.mapNotNull { it.deposit }
        if (deposits.isEmpty()) return ResourceHealth(true, 0, 0, 1.0)
        var renewable = 0
        var viableFinite = 0
        var minimumFiniteRatio = 1.0
        deposits.forEach { deposit ->
            if (deposit.sustainability == Sustainability.RENEWABLE) {
                renewable++
            } else {
                val initial = (deposit.initialReserve ?: 0L).toDouble()
                val remaining = (deposit.reserve ?: 0L).toDouble()
                val ratio = if (initial > 0.0) (remaining / initial).coerceIn(0.0, 1.0) else 0.0
                minimumFiniteRatio = min(minimumFiniteRatio, ratio)
                if (ratio >= MineItConfig.RETURN_MIN_RESOURCE_RATIO) viableFinite++
            }
        }
        return ResourceHealth(renewable > 0 || viableFinite > 0, renewable, viableFinite, minimumFiniteRatio)
    }

    fun isOperating(colony: ColonyState): Boolean = colony.status in setOf(ColonyStatus.PLAYING, ColonyStatus.HOLDOVER, ColonyStatus.LIABILITY)

    private fun replaceActive(state: GameState, colony: ColonyState): GameState = state.copy(
        colonies = state.colonies.map { if (it.id == colony.id) colony else it },
    )

    private fun failed(state: GameState, message: String) = ContractActionResult(state, false, message)
}

enum class ContractMedal { NONE, BRONZE, SILVER, GOLD, PLATINUM }
enum class ContractDeadlineState { COMPLETE, EXTENSION, CORPORATION_FAILED, FAILED, RENEWAL_ENDED }

data class ContractScore(
    val passed: Boolean,
    val medal: ContractMedal,
    val profit: Double,
    val foodRatio: Double,
    val industryRatio: Double,
    val populationRatio: Double,
    val minimumRatio: Double,
)

data class ResourceHealth(
    val acceptableForReturn: Boolean,
    val renewableDeposits: Int,
    val viableFiniteDeposits: Int,
    val minimumFiniteRatio: Double,
)

data class ContractActionResult(
    val state: GameState,
    val ok: Boolean,
    val message: String,
    val cost: Double = 0.0,
)
