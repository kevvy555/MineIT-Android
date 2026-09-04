package com.mineit.android.domain.poc

import org.junit.Assert.assertEquals
import org.junit.Test

class PocSimulationTest {
    @Test
    fun `advance day consumes colony supplies and produces ore`() {
        val initial = PocScenario.initialState()

        val result = PocSimulation.advanceDay(initial)

        assertEquals(2, result.day)
        assertEquals(108, result.resources.food)
        assertEquals(170, result.resources.water)
        assertEquals(48, result.resources.ore)
        assertEquals(initial.resources.credits, result.resources.credits)
    }

    @Test
    fun `power shortage reduces ore output`() {
        val initial = PocScenario.initialState().copy(
            colony = PocScenario.initialState().colony.copy(
                powerAvailable = 10,
                powerDemand = 24,
            ),
        )

        val result = PocSimulation.advanceDay(initial)

        assertEquals(44, result.resources.ore)
    }

    @Test
    fun `day 365 advances to a new year`() {
        val initial = PocScenario.initialState().copy(year = 3, day = 365)

        val result = PocSimulation.advanceDay(initial)

        assertEquals(4, result.year)
        assertEquals(1, result.day)
    }
}
