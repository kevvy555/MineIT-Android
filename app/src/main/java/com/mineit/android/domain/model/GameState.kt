package com.mineit.android.domain.model

import com.mineit.android.domain.contracts.ContractState
import com.mineit.android.domain.resources.Inventory
import com.mineit.android.domain.world.WorldState
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GameState(
    val date: GameDate,
    val company: CompanyState,
    val colonies: List<ColonyState>,
    val activeColonyId: ColonyId,
) {
    init {
        require(colonies.isNotEmpty()) { "GameState must contain at least one colony." }
        require(colonies.map { it.id }.distinct().size == colonies.size) {
            "GameState colony IDs must be unique."
        }
        require(colonies.any { it.id == activeColonyId }) {
            "GameState activeColonyId must reference an existing colony."
        }
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
        require(listOf(housing, power, food, industry, mining, scanning).all { it >= 1 }) {
            "Technology levels must be at least 1."
        }
    }
}

@Serializable
data class CompanyState(
    val cash: Long,
    val reputation: Int,
    val technology: TechnologyLevels = TechnologyLevels(),
) {
    init {
        require(reputation >= 0) { "Company reputation must not be negative." }
    }
}

@Serializable
enum class ColonyStatus {
    @SerialName("site-selection") SITE_SELECTION,
    @SerialName("playing") PLAYING,
    @SerialName("holdover") HOLDOVER,
    @SerialName("liability") LIABILITY,
    @SerialName("dead") DEAD,
}

@Serializable
data class ColonyState(
    val id: ColonyId,
    val name: String,
    val population: Int,
    val seed: Long,
    val inventory: Inventory = Inventory(),
    val contract: ContractState? = null,
    val status: ColonyStatus = ColonyStatus.PLAYING,
    val technology: TechnologyLevels = TechnologyLevels(),
    val world: WorldState = WorldState(),
) {
    init {
        require(name.isNotBlank()) { "Colony name must not be blank." }
        require(population >= 0) { "Colony population must not be negative." }
        require(contract == null || contract.uid == id.value) {
            "Colony contract uid must match the colony id."
        }
    }
}
