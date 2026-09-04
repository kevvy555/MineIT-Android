package com.mineit.android.domain.poc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PocScenarioTest {
    @Test
    fun `POC scenario creates a stable six by six sector grid`() {
        val state = PocScenario.initialState()

        assertEquals(36, state.sectors.size)
        assertEquals(36, state.sectors.map { it.coordinate }.toSet().size)
        assertTrue(state.sectors.any { it.surveyed })
        assertTrue(state.sectors.any { !it.surveyed })
    }
}
