package com.mineit.android.domain.colony

import com.mineit.android.domain.model.GameState
import com.mineit.android.domain.world.DevelopmentKind
import com.mineit.android.domain.world.SectorCoordinate
import com.mineit.android.domain.world.TileDevelopment
import com.mineit.android.domain.world.WorldDiscovery
import com.mineit.android.testing.EstablishedColonyFixture
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExtractionOperationServiceTest {
    private val service = ExtractionOperationService()

    @Test
    fun `renewable harvest intensity changes in twenty five percent steps and clamps to source bounds`() {
        var state = renewableSite()
        val coordinate = SectorCoordinate(-2, -4)

        val pushed = service.adjustHarvestIntensity(state, coordinate, 25)
        assertTrue(pushed.ok)
        assertEquals(125, pushed.afterPercent)
        state = pushed.state
        assertEquals(1.25, state.activeColony.world.tileAt(coordinate)!!.deposit!!.harvestIntensity, 0.0)

        val maximum = service.adjustHarvestIntensity(state, coordinate, 500)
        assertTrue(maximum.ok)
        assertEquals(200, maximum.afterPercent)
        assertEquals(2.0, maximum.state.activeColony.world.tileAt(coordinate)!!.deposit!!.harvestIntensity, 0.0)

        val alreadyMaximum = service.adjustHarvestIntensity(maximum.state, coordinate, 25)
        assertFalse(alreadyMaximum.ok)
    }

    @Test
    fun `finite extraction cannot use renewable harvest controls`() {
        val state = renewableSite()
        val coordinate = SectorCoordinate(-2, -4)
        val tile = state.activeColony.world.tileAt(coordinate)!!
        val finite = tile.copy(deposit = tile.deposit!!.copy(sustainability = com.mineit.android.domain.world.Sustainability.FINITE, reserve = 1000, initialReserve = 1000))
        val updated = state.withTile(finite)

        val result = service.adjustHarvestIntensity(updated, coordinate, 25)

        assertFalse(result.ok)
        assertEquals("Renewable harvesting is unavailable.", result.message)
    }

    private fun renewableSite(): GameState {
        var state = EstablishedColonyFixture.contract01()
        val coordinate = SectorCoordinate(-2, -4)
        val colony = state.activeColony
        val revealed = WorldDiscovery().reveal(
            colonySeed = colony.seed,
            contract = requireNotNull(colony.contract),
            tile = requireNotNull(colony.world.tileAt(coordinate)),
            scanningLevel = 1,
        ).copy(development = TileDevelopment(DevelopmentKind.EXTRACT, level = 1))
        state = state.withTile(revealed)
        return state
    }

    private fun GameState.withTile(tile: com.mineit.android.domain.world.WorldTile): GameState {
        val colony = activeColony.copy(
            world = activeColony.world.copy(
                tiles = activeColony.world.tiles.map { if (it.coordinate == tile.coordinate) tile else it },
            ),
        )
        return copy(colonies = colonies.map { if (it.id == colony.id) colony else it })
    }
}
