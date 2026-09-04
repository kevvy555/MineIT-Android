package com.mineit.android.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DemoGameTest {
    @Test
    fun `POC scenario creates a stable six by six sector grid`() {
        val state = DemoGame.initialState()

        assertEquals(36, state.sectors.size)
        assertEquals(36, state.sectors.map { it.coordinate }.toSet().size)
        assertTrue(state.sectors.any { it.surveyed })
        assertTrue(state.sectors.any { !it.surveyed })
    }
}
