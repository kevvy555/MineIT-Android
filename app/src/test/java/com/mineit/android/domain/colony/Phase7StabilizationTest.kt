package com.mineit.android.domain.colony

import com.mineit.android.domain.model.GameState
import com.mineit.android.domain.world.DevelopmentKind
import com.mineit.android.domain.world.SectorCoordinate
import com.mineit.android.domain.world.TileDevelopment
import com.mineit.android.testing.EstablishedColonyFixture
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase7StabilizationTest {
    private val headquarters = HeadquartersService()

    @Test
    fun `primary Headquarters action completes founding handover before travel migration`() {
        val coordinate = SectorCoordinate(1, 0)
        var state = EstablishedColonyFixture.contract01()
        state = state.withActiveColony { colony ->
            colony.copy(
                world = colony.world.copy(
                    tiles = colony.world.tiles.map { tile ->
                        if (tile.coordinate == coordinate) {
                            tile.copy(
                                revealed = true,
                                development = TileDevelopment(DevelopmentKind.HEADQUARTERS, level = 1),
                            )
                        } else tile
                    },
                ),
                headquarters = HeadquartersIdentityState(
                    primary = coordinate,
                    primaryEverAssigned = true,
                    commandHandoverComplete = false,
                ),
            )
        }

        val gate = headquarters.departureGate(state)
        assertTrue(gate.failures.joinToString(), gate.ok)
        assertFalse(state.activeColony.headquarters.commandHandoverComplete)

        val action = headquarters.setPrimary(state, coordinate)

        assertTrue(action.message, action.ok)
        assertTrue(action.state.activeColony.headquarters.commandHandoverComplete)
        assertEquals(coordinate, action.state.activeColony.headquarters.primary)
        assertTrue(action.message.contains("Command handover complete"))

        val repeated = headquarters.setPrimary(action.state, coordinate)
        assertTrue(repeated.ok)
        assertTrue(repeated.state.activeColony.headquarters.commandHandoverComplete)
        assertTrue(repeated.message.contains("already owns colony command"))
    }

    private fun GameState.withActiveColony(
        transform: (com.mineit.android.domain.model.ColonyState) -> com.mineit.android.domain.model.ColonyState,
    ): GameState {
        val updated = transform(activeColony)
        return copy(colonies = colonies.map { if (it.id == updated.id) updated else it })
    }
}
