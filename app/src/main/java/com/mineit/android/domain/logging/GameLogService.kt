package com.mineit.android.domain.logging

import com.mineit.android.domain.config.MineItConfig
import com.mineit.android.domain.model.GameState

/** Durable player/audit event log. Telemetry snapshots will build on this owner, not a UI-local log. */
class GameLogService {
    fun event(state: GameState, type: String, message: String, data: Map<String, String> = emptyMap()): GameLogResult {
        val log = state.gameLog
        val entry = GameLogEvent(
            id = log.nextId,
            absoluteDay = state.date.toAbsoluteDay().value,
            year = state.date.year,
            day = state.date.day,
            type = type.ifBlank { "event" },
            message = message,
            colonyId = state.activeColonyId,
            colonyName = state.activeColony.name,
            data = data.toMap(),
        )
        val nextEvents = (log.events + entry).takeLast(MineItConfig.LOG_MAX_EVENTS)
        val nextState = state.copy(gameLog = log.copy(nextId = log.nextId + 1, events = nextEvents))
        return GameLogResult(nextState, entry)
    }
}
