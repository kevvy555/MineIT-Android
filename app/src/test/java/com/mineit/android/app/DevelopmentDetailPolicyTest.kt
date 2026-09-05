package com.mineit.android.app

import com.mineit.android.domain.colony.ColonyNetworkService
import com.mineit.android.domain.colony.SiteProductionRules
import com.mineit.android.domain.model.GameState
import com.mineit.android.domain.simulation.DailySimulationEngine
import com.mineit.android.domain.world.DevelopmentKind
import com.mineit.android.domain.world.SectorCoordinate
import com.mineit.android.domain.world.TileDevelopment
import com.mineit.android.domain.world.WorldDiscovery
import com.mineit.android.testing.EstablishedColonyFixture
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DevelopmentDetailPolicyTest {
    private val policy = DevelopmentDetailPolicy()
    private val networkService = ColonyNetworkService()
    private val simulation = DailySimulationEngine()

    @Test
    fun `housing detail uses planetary residents and exposes capacity and power`() {
        var state = EstablishedColonyFixture.contract01()
        val coordinate = SectorCoordinate(-4, -4)
        state = state.withTile(
            requireNotNull(state.activeColony.world.tileAt(coordinate)).copy(
                revealed = true,
                development = TileDevelopment(DevelopmentKind.HOUSING, level = 1),
            ),
        )
        val network = networkService.calculate(state)
        val detail = requireNotNull(policy.detail(state, coordinate, network, simulation.recalculate(state)))

        assertEquals("Housing", detail.name)
        assertEquals("120", detail.overview.first { it.label == "OCCUPIED" }.value)
        assertEquals("40", detail.overview.first { it.label == "FREE SPACES" }.value)
        assertTrue(detail.overview.any { it.label == "POWER" })
        assertEquals(2, detail.upgrade.nextLevel)
    }

    @Test
    fun `extraction detail shares canonical site throughput and exposes operating constraints`() {
        var state = EstablishedColonyFixture.contract01()
        val resourceCoordinate = SectorCoordinate(-2, -4)
        val powerCoordinate = SectorCoordinate(-4, -4)
        val colony = state.activeColony
        val contract = requireNotNull(colony.contract)
        val revealed = WorldDiscovery().reveal(
            colonySeed = colony.seed,
            contract = contract,
            tile = requireNotNull(colony.world.tileAt(resourceCoordinate)),
            scanningLevel = 1,
        ).copy(development = TileDevelopment(DevelopmentKind.EXTRACT, level = 1))
        state = state.withTile(revealed)
        state = state.withTile(
            requireNotNull(state.activeColony.world.tileAt(powerCoordinate)).copy(
                revealed = true,
                development = TileDevelopment(DevelopmentKind.POWER, level = 1),
            ),
        )

        val network = networkService.calculate(state)
        val rate = SiteProductionRules.rate(state.activeColony, requireNotNull(state.activeColony.world.tileAt(resourceCoordinate)), network)
        val detail = requireNotNull(policy.detail(state, resourceCoordinate, network, simulation.recalculate(state)))

        assertEquals(11.0, rate, .001)
        assertEquals("Farm", detail.name)
        assertEquals("+11 / DAY", detail.overview.first { it.label == "OUTPUT" }.value)
        assertEquals("Q49", detail.operations.first { it.label == "QUALITY" }.value)
        assertTrue(detail.overview.any { it.label == "STAFF" })
        assertTrue(detail.overview.any { it.label == "POWER" })
        assertNotNull(detail.upgrade)
        assertTrue(detail.upgrade.requirements.any { it.label == "POWER CAPACITY" })
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
