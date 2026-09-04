package com.mineit.android.domain.model

import com.mineit.android.domain.colony.HeadquartersIdentityState
import com.mineit.android.domain.contracts.ContractState
import com.mineit.android.domain.events.CorporateEventQueueState
import com.mineit.android.domain.logging.GameLogState
import com.mineit.android.domain.resources.Inventory
import com.mineit.android.domain.trade.TradeState
import com.mineit.android.domain.world.WorldState
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GameState(
    val date: GameDate,
    val company: CompanyState,
    val colonies: List<ColonyState>,
    val activeColonyId: ColonyId,
    val corporateEvents: CorporateEventQueueState = CorporateEventQueueState(),
    val gameLog: GameLogState = GameLogState(),
) {
    init {
        require(colonies.isNotEmpty()) { "GameState must contain at least one colony." }
        require(colonies.map { it.id }.distinct().size == colonies.size) { "GameState colony IDs must be unique." }
        require(colonies.any { it.id == activeColonyId }) { "GameState activeColonyId must reference an existing colony." }
    }

    val activeColony: ColonyState
        get() = colonies.first { it.id == activeColonyId }
}

@Serializable
data class TechnologyLevels(
    val housing: Int = 1,
    val power: Int = 1,
    val food: Int = 1,
    val industry: Int = 1,
    val mining: Int = 1,
    val scanning: Int = 1,
) {
    init {
        require(listOf(housing, power, food, industry, mining, scanning).all { it >= 1 }) { "Technology levels must be at least 1." }
    }
}

@Serializable
data class CompanyState(
    val cash: Double,
    val reputation: Double,
    val earnedRevenue: Double = 0.0,
    val wins: Int = 0,
    val gameOver: Boolean = false,
    val technology: TechnologyLevels = TechnologyLevels(),
) {
    init {
        require(cash.isFinite()) { "Company cash must be finite." }
        require(earnedRevenue.isFinite()) { "Company earned revenue must be finite." }
        require(reputation.isFinite() && reputation in -100.0..100.0) { "Company reputation must be between -100 and 100." }
        require(wins >= 0) { "Company wins must not be negative." }
    }
}

@Serializable
enum class ColonyStatus {
    @SerialName("site-selection") SITE_SELECTION,
    @SerialName("playing") PLAYING,
    @SerialName("holdover") HOLDOVER,
    @SerialName("liability") LIABILITY,
    @SerialName("contract-failed") CONTRACT_FAILED,
    @SerialName("dead") DEAD,
}

@Serializable
data class ColonyState(
    val id: ColonyId,
    val name: String,
    val population: Double,
    val seed: Long,
    val inventory: Inventory = Inventory(),
    val contract: ContractState? = null,
    val status: ColonyStatus = ColonyStatus.PLAYING,
    val technology: TechnologyLevels = TechnologyLevels(),
    val world: WorldState = WorldState(),
    val emergencyMode: Boolean = false,
    val foodStarvationDays: Int = 0,
    val headquarters: HeadquartersIdentityState = HeadquartersIdentityState(),
    val trade: TradeState = TradeState(),
    val tradeReserve: Double = 0.0,
    /**
     * Durable staged ownership until the full ship domain is migrated. It represents the
     * canonical founding ship being physically docked at this colony and therefore able
     * to provide its current 50 Industry support and temporary/emergency command capability.
     * Phase 8 will migrate this fact into the full fleet model.
     */
    val foundingShipDocked: Boolean = true,
) {
    init {
        require(name.isNotBlank()) { "Colony name must not be blank." }
        require(population.isFinite() && population >= 0.0) { "Colony population must not be negative." }
        require(foodStarvationDays >= 0) { "Food starvation days must not be negative." }
        require(tradeReserve.isFinite() && tradeReserve >= 0.0) { "Trade reserve must be finite and non-negative." }
        require(contract == null || contract.uid == id.value) { "Colony contract uid must match the colony id." }
    }
}
