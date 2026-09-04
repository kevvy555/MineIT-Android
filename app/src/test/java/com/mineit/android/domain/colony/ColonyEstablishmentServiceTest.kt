package com.mineit.android.domain.colony

import com.mineit.android.domain.model.ColonyId
import com.mineit.android.domain.model.GameState
import com.mineit.android.domain.model.NewGameFactory
import com.mineit.android.domain.resources.ResourceCategory
import com.mineit.android.domain.ships.PlayerFleetService
import com.mineit.android.domain.world.DevelopmentKind
import com.mineit.android.domain.world.SectorCoordinate
import com.mineit.android.domain.world.TileDevelopment
import com.mineit.android.domain.world.WorldTile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ColonyEstablishmentServiceTest {
    private val fleet = PlayerFleetService()
    private val headquarters = HeadquartersService(fleet)
    private val networks = ColonyNetworkService(fleet, headquarters)
    private val service = ColonyEstablishmentService(fleet, headquarters)
    private val factory = NewGameFactory()

    @Test
    fun `fresh settled colony reports ship phase and source-compatible checklist`() {
        val state = settledState()
        val assessment = service.assessment(state, networks.calculate(state))

        assertTrue(assessment.required)
        assertFalse(assessment.acknowledged)
        assertEquals(EstablishmentPhase.SHIP, assessment.phase)
        assertEquals(120.0, assessment.shipResidents, 0.0)
        assertEquals(0.0, assessment.planetaryResidents, 0.0)
        assertEquals(290, assessment.shipAccommodationCapacity)
        assertEquals(10, assessment.shipCrew)
        assertEquals(10, assessment.shipMinimumCrew)
        assertEquals(EstablishmentSupportStatus.SHIP, assessment.support[EstablishmentStep.SUPPLIES])
        assertEquals(EstablishmentSupportStatus.SHIP, assessment.support[EstablishmentStep.HOUSING])
        assertEquals(EstablishmentSupportStatus.SHIP, assessment.support[EstablishmentStep.INDUSTRY])
        assertEquals(520.0, assessment.resourceSplit.getValue(ResourceCategory.BUILD).ship, 0.0)
        assertEquals(0.0, assessment.resourceSplit.getValue(ResourceCategory.BUILD).colony, 0.0)
    }

    @Test
    fun `founding bootstrap unload creates a hybrid split before Spaceport Power`() {
        val state = settledState()
        val result = service.unloadCategory(state, ResourceCategory.BUILD, spaceportServicesAvailable = false)
        val assessment = service.assessment(result.state, networks.calculate(result.state))

        assertTrue(result.ok)
        assertEquals(520.0, result.amount, 0.0)
        assertEquals(0.0, assessment.resourceSplit.getValue(ResourceCategory.BUILD).ship, 0.0)
        assertEquals(520.0, assessment.resourceSplit.getValue(ResourceCategory.BUILD).colony, 0.0)
        assertEquals(EstablishmentSupportStatus.HYBRID, assessment.support[EstablishmentStep.SUPPLIES])
    }

    @Test
    fun `residents cannot move ashore until real Housing and powered Spaceport exist`() {
        var state = settledState()
        val noHousing = service.residentTransferPreview(
            state = state,
            network = networks.calculate(state),
            requested = 10.0,
            spaceportServicesAvailable = false,
        )
        assertFalse(noHousing.ok)
        assertTrue(noHousing.message.contains("accommodation", ignoreCase = true))

        state = service.unloadCategory(state, ResourceCategory.FUEL, spaceportServicesAvailable = false).state
        state = updateTile(state, SectorCoordinate(1, 0)) { tile ->
            tile.copy(revealed = true, development = TileDevelopment(DevelopmentKind.POWER, level = 1))
        }
        state = updateTile(state, SectorCoordinate(2, 0)) { tile ->
            tile.copy(revealed = true, development = TileDevelopment(DevelopmentKind.HOUSING, level = 1))
        }
        val network = networks.calculate(state)
        val moved = service.residentTransferPreview(
            state = state,
            network = network,
            requested = 10.0,
            spaceportServicesAvailable = SpaceportService().status(state, network).transfersAllowed,
        )

        assertTrue(moved.message, moved.ok)
        assertEquals(110.0, moved.state.activeColony.shipResidentAssignments.single().residents, 0.0)
        assertEquals(10.0, moved.state.activeColony.planetaryAccommodationResidents, 0.0)
        assertEquals(EstablishmentPhase.HYBRID, service.assessment(moved.state, networks.calculate(moved.state)).phase)
    }

    @Test
    fun `begin operations only acknowledges handover and leaves command completion independent`() {
        val state = settledState()
        val result = service.acknowledge(state)

        assertTrue(result.ok)
        assertTrue(result.state.activeColony.establishmentAcknowledged)
        assertFalse(result.state.activeColony.headquarters.commandHandoverComplete)
        assertTrue(service.assessment(result.state, networks.calculate(result.state)).required)
    }

    private fun settledState(): GameState = factory.settleLandingSite(
        factory.contract01(colonySeed = 123456789L, colonyId = ColonyId("n05-establishment")),
        0,
    )

    private fun updateTile(
        state: GameState,
        coordinate: SectorCoordinate,
        transform: (WorldTile) -> WorldTile,
    ): GameState {
        val colony = state.activeColony
        val updated = colony.copy(
            world = colony.world.copy(
                tiles = colony.world.tiles.map { tile -> if (tile.coordinate == coordinate) transform(tile) else tile },
            ),
        )
        return state.copy(colonies = state.colonies.map { if (it.id == updated.id) updated else it })
    }
}
