package com.mineit.android.domain.events

import com.mineit.android.domain.model.NewGameFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CorporateEventServiceTest {
    @Test
    fun queueDeduplicatesAndPreservesPinnedPriority() {
        val service = CorporateEventService()
        var state = NewGameFactory().contract01(4L)
        val colony = state.activeColony
        state = service.enqueue(state, CorporateEvent(CorporateEventType.SHIP, colony.id, colony.name))
        state = service.enqueue(state, CorporateEvent(CorporateEventType.CONTRACT, colony.id, colony.name, kind = "extension"))
        state = service.enqueue(state, CorporateEvent(CorporateEventType.SHIP, colony.id, colony.name, recovered = true))
        assertEquals(2, state.corporateEvents.pending.size)
        assertEquals(CorporateEventType.SHIP, state.corporateEvents.pending.first().type)
        assertTrue(state.corporateEvents.pending.first().recovered)
        assertEquals(3, state.corporateEvents.nextSequence)
    }
}
