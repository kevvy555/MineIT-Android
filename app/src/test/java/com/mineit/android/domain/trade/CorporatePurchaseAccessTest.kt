package com.mineit.android.domain.trade

import com.mineit.android.domain.colony.ColonyNetworkService
import com.mineit.android.domain.colony.HeadquartersIdentityState
import com.mineit.android.domain.model.GameState
import com.mineit.android.domain.world.DevelopmentKind
import com.mineit.android.domain.world.SectorCoordinate
import com.mineit.android.domain.world.TileDevelopment
import com.mineit.android.testing.EstablishedColonyFixture
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CorporatePurchaseAccessTest {
    private val networks = ColonyNetworkService()

    @Test
    fun `docked player ship provides corporate purchase computer link without Spaceport power`() {
        val state = EstablishedColonyFixture.contract01()
        val access = CorporatePurchaseAccess.evaluate(state, networks.calculate(state))

        assertTrue(access.available)
        assertEquals(CorporatePurchaseAccessSource.DOCKED_PLAYER_SHIP, access.source)
    }

    @Test
    fun `powered staffed Headquarters provides purchase link after player ship is absent`() {
        val hq = SectorCoordinate(1, 0)
        val power = SectorCoordinate(2, 0)
        var state = EstablishedColonyFixture.contract01().copy(
            fleet = EstablishedColonyFixture.contract01().fleet.copy(ships = emptyList(), selectedShipId = null),
        )
        state = state.withActiveColony { colony ->
            colony.copy(
                headquarters = HeadquartersIdentityState(primary = hq, primaryEverAssigned = true),
                world = colony.world.copy(
                    tiles = colony.world.tiles.map { tile ->
                        when (tile.coordinate) {
                            hq -> tile.copy(revealed = true, development = TileDevelopment(DevelopmentKind.HEADQUARTERS, level = 1))
                            power -> tile.copy(revealed = true, development = TileDevelopment(DevelopmentKind.POWER, level = 1))
                            else -> tile
                        }
                    },
                ),
            )
        }

        val network = networks.calculate(state)
        val access = CorporatePurchaseAccess.evaluate(state, network)

        assertTrue(network.headquarters.rows.single { it.coordinate == hq }.staffed)
        assertTrue(network.headquarters.rows.single { it.coordinate == hq }.powered)
        assertTrue(access.available)
        assertEquals(CorporatePurchaseAccessSource.HEADQUARTERS, access.source)
    }

    @Test
    fun `purchase link is unavailable with neither docked ship nor operational Headquarters`() {
        val initial = EstablishedColonyFixture.contract01()
        val state = initial.copy(fleet = initial.fleet.copy(ships = emptyList(), selectedShipId = null))
        val access = CorporatePurchaseAccess.evaluate(state, networks.calculate(state))

        assertFalse(access.available)
        assertEquals(CorporatePurchaseAccessSource.NONE, access.source)
    }

    private fun GameState.withActiveColony(
        transform: (com.mineit.android.domain.model.ColonyState) -> com.mineit.android.domain.model.ColonyState,
    ): GameState {
        val updated = transform(activeColony)
        return copy(colonies = colonies.map { if (it.id == updated.id) updated else it })
    }
}
