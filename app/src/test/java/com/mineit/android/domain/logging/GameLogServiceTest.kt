package com.mineit.android.domain.logging

import com.mineit.android.domain.model.NewGameFactory
import org.junit.Assert.assertEquals
import org.junit.Test

class GameLogServiceTest {
    @Test
    fun eventUsesCanonicalClockAndMonotonicIds() {
        val service = GameLogService()
        var state = NewGameFactory().contract01(3L)
        val first = service.event(state, "trade", "Corporate ship arrived")
        state = first.state
        val second = service.event(state, "trade", "Cargo transferred")
        assertEquals(1, first.event.id)
        assertEquals(1, first.event.absoluteDay)
        assertEquals(2, second.event.id)
        assertEquals(2, second.state.gameLog.events.size)
        assertEquals(state.activeColony.id, second.event.colonyId)
    }
}
