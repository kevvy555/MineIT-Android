package com.mineit.android.testing

import com.mineit.android.domain.model.ColonyId
import com.mineit.android.domain.model.GameState
import com.mineit.android.domain.model.NewGameFactory
import com.mineit.android.domain.ships.PlayerFleetService

/**
 * Test-only fixture for domain tests whose subject is an already-established planetary colony.
 * N05 new-game parity is covered separately; this helper explicitly completes the initial
 * ship-to-colony stock/resident handover without completing Headquarters command handover.
 */
object EstablishedColonyFixture {
    fun contract01(
        colonySeed: Long = 123456789L,
        colonyId: ColonyId = ColonyId("intro-$colonySeed"),
        landingSiteIndex: Int = 0,
    ): GameState {
        val factory = NewGameFactory()
        val fleet = PlayerFleetService()
        var state = factory.settleLandingSite(
            factory.contract01(colonySeed = colonySeed, colonyId = colonyId),
            landingSiteIndex,
        )
        val ship = state.fleet.ships.single()
        ship.inventory.resources.forEach { stock ->
            val result = fleet.transferFromShipToColony(
                state = state,
                shipId = ship.id,
                resourceId = stock.resourceId,
                requested = stock.amount,
                spaceportServicesAvailable = false,
            )
            check(result.ok) { result.message }
            state = result.state
        }
        val residents = state.activeColony.shipResidentAssignments.single().residents
        val moved = fleet.moveResidentsAshore(state, ship.id, residents)
        check(moved.ok) { moved.message }
        state = moved.state
        val colony = state.activeColony.copy(establishmentAcknowledged = true)
        return state.copy(colonies = state.colonies.map { if (it.id == colony.id) colony else it })
    }
}
