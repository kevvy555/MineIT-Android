package com.mineit.android.domain.ships

import com.mineit.android.domain.colony.ColonyNetworkService
import com.mineit.android.domain.config.MineItConfig
import com.mineit.android.domain.model.ColonyState
import com.mineit.android.domain.model.ColonyStatus
import com.mineit.android.domain.model.GameState
import com.mineit.android.domain.model.NewGameFactory
import com.mineit.android.domain.model.ResourceId
import com.mineit.android.domain.resources.ResourceCategory
import com.mineit.android.domain.simulation.DailySimulationEngine
import com.mineit.android.testing.EstablishedColonyFixture
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShipEstablishmentParityTest {
    private val factory = NewGameFactory()
    private val fleetService = PlayerFleetService()

    @Test
    fun `new Contract 01 starts with the N05 founding manifest aboard ship`() {
        val state = factory.contract01(colonySeed = 42L)
        val colony = state.activeColony
        val ship = state.fleet.ships.single()

        assertEquals(0.0, colony.inventory.amountFor(ResourceCategory.FOOD), 0.0)
        assertEquals(0.0, colony.inventory.amountFor(ResourceCategory.FUEL), 0.0)
        assertEquals(0.0, colony.inventory.amountFor(ResourceCategory.BUILD), 0.0)
        assertEquals(0.0, colony.inventory.amountFor(ResourceCategory.ORE), 0.0)
        assertEquals(MineItConfig.START_FOOD, ship.inventory.amountFor(ResourceCategory.FOOD), 0.0)
        assertEquals(675.0, ship.inventory.amountFor(ResourceCategory.FUEL), 0.0)
        assertEquals(520.0, ship.inventory.amountFor(ResourceCategory.BUILD), 0.0)
        assertEquals(260.0, ship.inventory.amountFor(ResourceCategory.ORE), 0.0)
        assertEquals(10, ship.crew)
        assertEquals(0, ship.passengerManifest)
        assertEquals(120.0, colony.shipResidentAssignments.single().residents, 0.0)
        assertEquals(0.0, colony.planetaryAccommodationResidents, 0.0)
        assertFalse(colony.establishmentAcknowledged)
    }

    @Test
    fun `occupied founding ship protects the first 30 days from the old colony pantry starvation bug`() {
        var state = factory.settleLandingSite(factory.contract01(colonySeed = 43L), 0)
        val engine = DailySimulationEngine()

        repeat(31) { state = engine.advanceDay(state).state }

        val ship = state.fleet.ships.single()
        assertEquals(120.0, state.activeColony.population, 0.0)
        assertEquals(ColonyStatus.PLAYING, state.activeColony.status)
        assertEquals(0, state.activeColony.foodStarvationDays)
        assertEquals(31, state.date.toAbsoluteDay().value - 1)
        assertEquals(1_300.0 - (31 * 120.0 * MineItConfig.FOOD_PER_COLONIST), ship.inventory.amountFor(ResourceCategory.FOOD), .0001)
        assertEquals(0, ship.residentFoodStarvationDays)
    }

    @Test
    fun `founding manifest alone keeps an all-aboard colony alive for ninety days`() {
        var state = factory.settleLandingSite(factory.contract01(colonySeed = 44L), 0)
        val engine = DailySimulationEngine()

        repeat(90) { state = engine.advanceDay(state).state }

        assertEquals(120.0, state.activeColony.population, 0.0)
        assertEquals(ColonyStatus.PLAYING, state.activeColony.status)
        assertTrue(state.fleet.ships.single().inventory.amountFor(ResourceCategory.FOOD) > 0.0)
    }

    @Test
    fun `ship residents are outside planetary workforce and life support while ship Industry is self powered`() {
        val state = factory.settleLandingSite(factory.contract01(colonySeed = 45L), 0)
        val network = ColonyNetworkService().calculate(state)
        val withoutShip = state.copy(fleet = FleetState())
        val withoutShipNetwork = ColonyNetworkService().calculate(withoutShip)

        assertEquals(0.0, network.workforceAvailable, 0.0)
        assertEquals(50.0, network.shipIndustry, 0.0)
        assertEquals(50.0, network.industryInstalled, 0.0)
        assertEquals(0.0, withoutShipNetwork.shipIndustry, 0.0)
        assertEquals(withoutShipNetwork.powerDemand, network.powerDemand, .0001)
    }

    @Test
    fun `bootstrap unload conserves quality stock and reverse loading remains Spaceport gated`() {
        val state = factory.settleLandingSite(factory.contract01(colonySeed = 46L), 0)
        val shipId = state.fleet.ships.single().id
        val resourceId = ResourceId("fungal")
        val before = state.activeColony.inventory.amountFor(resourceId) + state.fleet.ships.single().inventory.amountFor(resourceId)

        val unloaded = fleetService.transferFromShipToColony(
            state = state,
            shipId = shipId,
            resourceId = resourceId,
            requested = 100.0,
            spaceportServicesAvailable = false,
        )

        assertTrue(unloaded.ok)
        assertEquals(100.0, unloaded.state.activeColony.inventory.amountFor(resourceId), 0.0)
        assertEquals(before, unloaded.state.activeColony.inventory.amountFor(resourceId) + unloaded.state.fleet.ships.single().inventory.amountFor(resourceId), .0001)

        val reverse = fleetService.transferFromColonyToShip(
            state = unloaded.state,
            shipId = shipId,
            resourceId = resourceId,
            requested = 50.0,
            spaceportServicesAvailable = false,
        )
        assertFalse(reverse.ok)
        assertEquals(unloaded.state, reverse.state)
    }

    @Test
    fun `moving residents back aboard uses homeless residents before vacating planetary Housing`() {
        var state = EstablishedColonyFixture.contract01(colonySeed = 47L)
        val ship = state.fleet.ships.single()
        state = state.withActiveColony { colony ->
            colony.copy(planetaryAccommodationResidents = 60.0)
        }

        val moved = fleetService.moveResidentsAboard(
            state = state,
            shipId = ship.id,
            requested = 80.0,
            spaceportServicesAvailable = true,
        )

        assertTrue(moved.message, moved.ok)
        assertEquals(80.0, moved.amount, 0.0)
        assertEquals(80.0, fleetService.shipResidentCount(moved.state.activeColony, ship.id), 0.0)
        assertEquals(40.0, fleetService.planetaryResidentCount(moved.state), 0.0)
        assertEquals(40.0, moved.state.activeColony.planetaryAccommodationResidents, 0.0)
        assertEquals(0.0, fleetService.homelessResidentCount(moved.state), 0.0)
    }

    @Test
    fun `moving residents aboard is Spaceport gated and bounded by ship accommodation`() {
        var state = EstablishedColonyFixture.contract01(colonySeed = 48L)
        val ship = state.fleet.ships.single().copy(accommodationCapacity = 50)
        state = state.copy(fleet = state.fleet.copy(ships = listOf(ship)))

        val blocked = fleetService.moveResidentsAboard(
            state = state,
            shipId = ship.id,
            requested = 100.0,
            spaceportServicesAvailable = false,
        )
        assertFalse(blocked.ok)
        assertTrue(blocked.message.contains("Spaceport"))

        val moved = fleetService.moveResidentsAboard(
            state = state,
            shipId = ship.id,
            requested = 100.0,
            spaceportServicesAvailable = true,
        )
        assertTrue(moved.ok)
        assertEquals(50.0, moved.amount, 0.0)
        assertEquals(50.0, fleetService.shipResidentCount(moved.state.activeColony, ship.id), 0.0)
    }

    @Test
    fun `loading enforces separate general cargo Food and Fuel capacities`() {
        var state = EstablishedColonyFixture.contract01(colonySeed = 49L)
        val ship = state.fleet.ships.single()
        state = state.withActiveColony { colony ->
            colony.copy(
                inventory = colony.inventory
                    .store(ResourceId("fiber"), ResourceCategory.BUILD, 10_000.0, 500)
                    .store(ResourceId("fungal"), ResourceCategory.FOOD, 3_000.0, 500)
                    .store(ResourceId("biomass"), ResourceCategory.FUEL, 3_000.0, 500),
            )
        }

        val cargo = fleetService.transferFromColonyToShip(
            state = state,
            shipId = ship.id,
            resourceId = ResourceId("fiber"),
            requested = 10_000.0,
            spaceportServicesAvailable = true,
        )
        assertTrue(cargo.ok)
        assertEquals(ship.cargoCapacity, cargo.amount, 0.0)
        assertEquals(ship.cargoCapacity, fleetService.generalCargoLoad(cargo.state.fleet.ships.single()), 0.0)
        assertTrue(cargo.message.contains("capacity"))

        val cargoFull = fleetService.transferFromColonyToShip(
            state = cargo.state,
            shipId = ship.id,
            resourceId = ResourceId("surface-iron"),
            requested = 1.0,
            spaceportServicesAvailable = true,
        )
        assertFalse(cargoFull.ok)
        assertTrue(cargoFull.message.contains("cargo hold"))

        val food = fleetService.transferFromColonyToShip(
            state = cargo.state,
            shipId = ship.id,
            resourceId = ResourceId("fungal"),
            requested = 3_000.0,
            spaceportServicesAvailable = true,
        )
        assertTrue(food.ok)
        assertEquals(ship.foodCapacity, food.amount, 0.0)
        assertEquals(ship.foodCapacity, fleetService.foodLoad(food.state.fleet.ships.single()), 0.0)

        val fuel = fleetService.transferFromColonyToShip(
            state = food.state,
            shipId = ship.id,
            resourceId = ResourceId("biomass"),
            requested = 3_000.0,
            spaceportServicesAvailable = true,
        )
        assertTrue(fuel.ok)
        assertEquals(ship.fuelCapacity, fuel.amount, 0.0)
        assertEquals(ship.fuelCapacity, fleetService.fuelLoad(fuel.state.fleet.ships.single()), 0.0)
        assertEquals(ship.cargoCapacity + ship.foodCapacity + ship.fuelCapacity, fleetService.totalPhysicalLoad(fuel.state.fleet.ships.single()), 0.0)
    }

    private fun GameState.withActiveColony(transform: (ColonyState) -> ColonyState): GameState {
        val updated = transform(activeColony)
        return copy(colonies = colonies.map { if (it.id == updated.id) updated else it })
    }
}
