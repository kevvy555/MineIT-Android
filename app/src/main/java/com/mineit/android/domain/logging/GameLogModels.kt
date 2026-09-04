package com.mineit.android.domain.logging

import com.mineit.android.domain.model.ColonyId
import kotlinx.serialization.Serializable

@Serializable
data class GameLogEvent(
    val id: Int,
    val absoluteDay: Int,
    val year: Int,
    val day: Int,
    val type: String,
    val message: String,
    val colonyId: ColonyId? = null,
    val colonyName: String? = null,
    val data: Map<String, String> = emptyMap(),
)

@Serializable
data class GameLogState(
    val nextId: Int = 1,
    val events: List<GameLogEvent> = emptyList(),
)

data class GameLogResult(
    val state: com.mineit.android.domain.model.GameState,
    val event: GameLogEvent,
)
