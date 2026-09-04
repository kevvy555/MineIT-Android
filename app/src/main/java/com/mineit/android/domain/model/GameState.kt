package com.mineit.android.domain.model

import com.mineit.android.domain.resources.Inventory
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
data class CompanyState(
    val cash: Long,
    val reputation: Int,
) {
    init {
        require(reputation >= 0) { "Company reputation must not be negative." }
    }
}

@Serializable
data class ColonyState(
    val id: ColonyId,
    val name: String,
    val population: Int,
    val seed: Long,
    val inventory: Inventory = Inventory(),
) {
    init {
        require(name.isNotBlank()) { "Colony name must not be blank." }
        require(population >= 0) { "Colony population must not be negative." }
    }
}
