package com.mineit.android.domain.colony

import com.mineit.android.domain.model.GameState
import com.mineit.android.domain.model.ResourceId
import com.mineit.android.domain.resources.ResourceCategory
import com.mineit.android.domain.world.DevelopmentKind
import com.mineit.android.domain.world.ExtractionAccidentOutcome
import com.mineit.android.domain.world.ExtractionOperatingMode
import com.mineit.android.domain.world.ResourceDeposit
import com.mineit.android.domain.world.Sustainability
import com.mineit.android.domain.world.TileDevelopment
import com.mineit.android.testing.EstablishedColonyFixture
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExtractionOverdriveDayServiceTest {
    @Test
    fun `completed productive hard day can trigger machinery shutdown without shortening new closure`() {
        val state = preparedState(exposure = 29.0, mode = ExtractionOperatingMode.HARD)
        val rolls = ArrayDeque(listOf(0.0, .9))
        val service = ExtractionOverdriveDayService(random = { rolls.removeFirst() })

        val result = service.advanceDay(state, state)
        val site = result.state.activeColony.world.tiles.first { it.deposit?.resourceId == ResourceId("iron") }

        assertEquals(1, result.accidents.size)
        assertEquals(ExtractionAccidentOutcome.MACHINERY, result.accidents.single().record.outcome)
        assertEquals("Tunnel Collapse", result.accidents.single().record.name)
        assertEquals(3, site.development?.accidentShutdownDays)
        assertTrue(site.development?.productionStopped == true)
    }

    @Test
    fun `fatal accident removes planetary residents and records applied deaths`() {
        val state = preparedState(exposure = 29.0, mode = ExtractionOperatingMode.HARD)
        val beforePopulation = state.activeColony.population
        val rolls = ArrayDeque(listOf(0.0, 0.0, .999))
        val service = ExtractionOverdriveDayService(random = { rolls.removeFirst() })

        val result = service.advanceDay(state, state)

        assertEquals(3.0, result.deaths, 0.0)
        assertEquals(beforePopulation - 3.0, result.state.activeColony.population, 0.0)
        assertEquals(beforePopulation - 3.0, result.state.activeColony.planetaryAccommodationResidents, 0.0)
        assertEquals(3, result.accidents.single().record.deaths)
    }

    @Test
    fun `shutdown present at beginning of day recovers by one day and resumes after third day`() {
        var state = preparedState(
            exposure = 0.0,
            mode = ExtractionOperatingMode.NORMAL,
            shutdownDays = 3,
        )
        val service = ExtractionOverdriveDayService()
        val coordinate = state.activeColony.world.tiles.first { it.deposit?.resourceId == ResourceId("iron") }.coordinate

        state = service.advanceDay(state, state).state
        assertEquals(2, state.activeColony.world.tileAt(coordinate)?.development?.accidentShutdownDays)
        state = service.advanceDay(state, state).state
        assertEquals(1, state.activeColony.world.tileAt(coordinate)?.development?.accidentShutdownDays)
        state = service.advanceDay(state, state).state
        val recovered = state.activeColony.world.tileAt(coordinate)!!
        assertEquals(0, recovered.development?.accidentShutdownDays)
        assertFalse(recovered.development?.productionStopped == true)
    }

    private fun preparedState(
        exposure: Double,
        mode: ExtractionOperatingMode,
        shutdownDays: Int = 0,
    ): GameState {
        val state = EstablishedColonyFixture.contract01()
        val colony = state.activeColony
        val siteBase = colony.world.tiles[0]
        val powerBase = colony.world.tiles[1]
        val site = siteBase.copy(
            revealed = true,
            resourceExhausted = false,
            resourceCovered = false,
            deposit = ResourceDeposit(
                resourceId = ResourceId("iron"),
                category = ResourceCategory.ORE,
                name = "Iron",
                rarity = "Common",
                multiplier = 1.0,
                quality = 500,
                requiredScanningLevel = 1,
                requiredMiningLevel = 1,
                requiredMiningTech = "Mining L1",
                terrainYieldFactor = 1.0,
                sustainability = Sustainability.FINITE,
                depositScale = "Large",
                reserve = 10_000,
                initialReserve = 10_000,
            ),
            development = TileDevelopment(
                kind = DevelopmentKind.EXTRACT,
                operatingMode = mode,
                overdriveExposure = exposure,
                accidentShutdownDays = shutdownDays,
                productionStopped = shutdownDays > 0,
            ),
        )
        val power = powerBase.copy(
            revealed = true,
            resourceCovered = false,
            deposit = null,
            development = TileDevelopment(DevelopmentKind.POWER, level = 5),
        )
        val nextColony = colony.copy(
            world = colony.world.copy(
                tiles = colony.world.tiles.mapIndexed { index, tile ->
                    when (index) {
                        0 -> site
                        1 -> power
                        else -> tile
                    }
                },
            ),
        )
        val next = state.copy(colonies = state.colonies.map { if (it.id == colony.id) nextColony else it })
        assertTrue(ColonyNetworkService().calculate(next).activeSites.any { it.coordinate == site.coordinate } || shutdownDays > 0)
        return next
    }
}
