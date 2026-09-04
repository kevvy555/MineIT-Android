package com.mineit.android.domain.events

import com.mineit.android.domain.model.GameState

/** Global durable corporate-event queue. Presentation never owns event ordering or deduplication. */
class CorporateEventService {
    fun enqueue(state: GameState, event: CorporateEvent): GameState {
        val queue = state.corporateEvents
        val key = key(event)
        val existing = queue.pending.firstOrNull { key(it) == key }
        val nextPending = if (existing != null) {
            queue.pending.map { current ->
                if (key(current) != key) current else current.copy(
                    recovered = current.recovered || event.recovered,
                    attemptIndex = event.attemptIndex ?: current.attemptIndex,
                    dueAbsoluteDay = event.dueAbsoluteDay ?: current.dueAbsoluteDay,
                )
            }
        } else {
            queue.pending + event.copy(sequence = queue.nextSequence)
        }.sortedWith(compareBy<CorporateEvent> { priority(it) }.thenBy { it.sequence })

        return state.copy(
            corporateEvents = queue.copy(
                nextSequence = if (existing == null) queue.nextSequence + 1 else queue.nextSequence,
                pending = nextPending,
            ),
        )
    }

    fun remove(state: GameState, event: CorporateEvent): GameState {
        val target = key(event)
        return state.copy(corporateEvents = state.corporateEvents.copy(pending = state.corporateEvents.pending.filterNot { key(it) == target }))
    }

    fun queueCorporateShip(state: GameState, recovered: Boolean = false): GameState {
        val colony = state.activeColony
        return enqueue(
            state,
            CorporateEvent(
                type = CorporateEventType.SHIP,
                colonyId = colony.id,
                colonyName = colony.name,
                recovered = recovered,
            ),
        )
    }

    internal fun priority(event: CorporateEvent): Int = when {
        event.type == CorporateEventType.EMERGENCY_FOOD -> 0
        event.recovered && event.type in setOf(CorporateEventType.SHIP, CorporateEventType.BUYER) -> 1
        event.type == CorporateEventType.BUYER -> 2
        event.type == CorporateEventType.CONTRACT -> 3
        event.type == CorporateEventType.SHIP -> 4
        else -> 9
    }

    internal fun key(event: CorporateEvent): String = when (event.type) {
        CorporateEventType.BUYER -> "buyer:${event.colonyId.value}:${event.contractId ?: "none"}"
        CorporateEventType.EMERGENCY_FOOD -> "emergency-food:${event.colonyId.value}:${event.shipId ?: "none"}"
        CorporateEventType.CONTRACT -> "contract:${event.colonyId.value}:${event.kind ?: "unknown"}"
        CorporateEventType.SHIP -> "ship:${event.colonyId.value}:"
    }
}
