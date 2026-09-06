package com.mineit.android.ui.game

import com.mineit.android.domain.colony.ColonyNetworkService
import com.mineit.android.domain.model.GameState
import com.mineit.android.domain.world.DevelopmentKind
import com.mineit.android.domain.world.SectorCoordinate
import com.mineit.android.domain.world.TileDevelopment
import com.mineit.android.testing.EstablishedColonyFixture
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HeadquartersControlReadinessTest {
    private val networks = ColonyNetworkService()

    @Test
    fun `new Headquarters waits for matching derived network row before sheet renders`() {
        val before = EstablishedColonyFixture.contract01()
        val coordinate = SectorCoordinate(1, 0)
        val staleNetwork = networks.calculate(before)
        val afterBuild = before.withActiveColony { colony ->
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
            )
        }
        val headquartersTile = requireNotNull(afterBuild.activeColony.world.tileAt(coordinate))

        assertFalse(HeadquartersControlReadiness.isReady(headquartersTile, staleNetwork))
        assertTrue(HeadquartersControlReadiness.isReady(headquartersTile, networks.calculate(afterBuild)))
    }

    private fun GameState.withActiveColony(
        transform: (com.mineit.android.domain.model.ColonyState) -> com.mineit.android.domain.model.ColonyState,
    ): GameState {
        val updated = transform(activeColony)
        return copy(colonies = colonies.map { if (it.id == updated.id) updated else it })
    }
}
