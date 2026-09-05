package com.mineit.android.ui.game

import com.mineit.android.domain.colony.ColonyNetworkService
import com.mineit.android.domain.colony.CommandSourceType
import com.mineit.android.domain.colony.HeadquartersService
import com.mineit.android.domain.colony.SpaceportService
import com.mineit.android.domain.model.NewGameFactory
import com.mineit.android.domain.resources.ResourceCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerShipControlPresentationTest {
    private val factory = NewGameFactory()

    @Test
    fun `fresh founding ship preserves web Ship Control hierarchy without inventing travel actions`() {
        val state = factory.settleLandingSite(factory.contract01(colonySeed = 61L), 0)
        val ship = state.fleet.ships.single()
        val network = ColonyNetworkService().calculate(state)
        val spaceport = SpaceportService().status(state, network)
        val gate = HeadquartersService().departureGate(state)

        val model = PlayerShipControlPresentation.build(state, network, spaceport, gate, ship)

        assertEquals("FOUNDING COLONY SHIP", model.kicker)
        assertEquals(
            listOf("CARGO", "FUEL", "FOOD", "CREW"),
            model.topMetrics.map { it.label },
        )
        assertEquals(
            listOf("TOTAL LOAD", "GENERAL HOLD", "CREW RANGE", "PASSENGERS", "SHIP INDUSTRY", "COMMAND"),
            model.statusMetrics.map { it.label },
        )
        assertTrue(model.commandLinked)
        assertEquals(120.0, model.shipResidents, 0.0)
        assertEquals(0.0, model.planetaryResidents, 0.0)
        assertFalse(model.canMoveAshore)
        assertFalse(model.canMoveAboard)
        assertTrue(model.foodRunwayDays!! > 80.0)
        assertEquals(
            setOf(ResourceCategory.FOOD, ResourceCategory.BUILD, ResourceCategory.FUEL, ResourceCategory.ORE),
            model.manifest.map { it.category }.toSet(),
        )
        assertTrue(model.manifest.all { it.canUnload })
        assertTrue(model.manifest.all { it.bootstrapUnload })
        assertTrue(model.manifest.none { it.canLoad })
        assertTrue(model.departureNotices.any { it.title.startsWith("COMMAND HANDOVER • BLOCKED") })
        assertTrue(model.departureNotices.any { it.title == "RESIDENTS STILL ABOARD" })
        assertTrue(model.departureNotices.any { it.title == "COLONY INDUSTRY DEPENDENCY" })
    }

    @Test
    fun `CSM is offline when Headquarters is the active command source`() {
        val state = factory.settleLandingSite(factory.contract01(colonySeed = 62L), 0)
        val ship = state.fleet.ships.single()
        val calculated = ColonyNetworkService().calculate(state)
        val network = calculated.copy(
            headquarters = calculated.headquarters.copy(sourceType = CommandSourceType.HEADQUARTERS),
        )
        val spaceport = SpaceportService().status(state, network)

        val model = PlayerShipControlPresentation.build(
            state,
            network,
            spaceport,
            HeadquartersService().departureGate(state),
            ship,
        )

        assertFalse(model.commandLinked)
        assertEquals("CAPABLE", model.statusMetrics.first { it.label == "COMMAND" }.value)
        assertEquals("Not current command source", model.statusMetrics.first { it.label == "COMMAND" }.detail)
    }

    @Test
    fun `manifest exposes separate store headroom for each resource category`() {
        val state = factory.settleLandingSite(factory.contract01(colonySeed = 63L), 0)
        val ship = state.fleet.ships.single()
        val network = ColonyNetworkService().calculate(state)
        val model = PlayerShipControlPresentation.build(
            state,
            network,
            SpaceportService().status(state, network),
            HeadquartersService().departureGate(state),
            ship,
        )

        val food = model.manifest.first { it.category == ResourceCategory.FOOD }
        val fuel = model.manifest.first { it.category == ResourceCategory.FUEL }
        val build = model.manifest.first { it.category == ResourceCategory.BUILD }
        val ore = model.manifest.first { it.category == ResourceCategory.ORE }

        assertEquals(ship.foodCapacity - food.shipAmount, food.remainingShipCapacity, 0.0)
        assertEquals(ship.fuelCapacity - fuel.shipAmount, fuel.remainingShipCapacity, 0.0)
        assertEquals(ship.cargoCapacity - build.shipAmount - ore.shipAmount, build.remainingShipCapacity, 0.0)
        assertEquals(build.remainingShipCapacity, ore.remainingShipCapacity, 0.0)
        assertTrue(food.qualitySummary.isNotBlank())
    }
}
