package com.mineit.android.ui.map

import com.mineit.android.domain.colony.ColonyNetworkSnapshot
import com.mineit.android.domain.model.ResourceId
import com.mineit.android.domain.resources.ResourceCategory
import com.mineit.android.domain.world.DevelopmentKind
import com.mineit.android.domain.world.ResourceDeposit
import com.mineit.android.domain.world.SectorCoordinate
import com.mineit.android.domain.world.Sustainability
import com.mineit.android.domain.world.TerrainType
import com.mineit.android.domain.world.TileDevelopment
import com.mineit.android.domain.world.WorldTile
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MapPresentationTest {
    @Test
    fun `resource focus shows matching resource category`() {
        val food = resourceTile(ResourceCategory.FOOD)
        val queued = emptySet<SectorCoordinate>()

        assertTrue(MapPresentation.matches(food, MapFocus.FOOD, emptySet(), queued, emptySet(), ColonyNetworkSnapshot.empty()))
        assertFalse(MapPresentation.matches(food, MapFocus.ORE, emptySet(), queued, emptySet(), ColonyNetworkSnapshot.empty()))
    }

    @Test
    fun `buildings focus includes developments and fixed spaceport`() {
        val development = plainTile(SectorCoordinate(-1, 0)).copy(development = TileDevelopment(DevelopmentKind.POWER))
        val port = plainTile(SectorCoordinate(0, 0))
        val empty = plainTile(SectorCoordinate(1, 0))
        val network = ColonyNetworkSnapshot.empty()

        assertTrue(MapPresentation.matches(development, MapFocus.BUILDINGS, emptySet(), emptySet(), emptySet(), network))
        assertTrue(MapPresentation.matches(port, MapFocus.BUILDINGS, emptySet(), emptySet(), emptySet(), network))
        assertFalse(MapPresentation.matches(empty, MapFocus.BUILDINGS, emptySet(), emptySet(), emptySet(), network))
    }

    @Test
    fun `problem focus includes exhausted and underpowered extraction sectors`() {
        val exhausted = resourceTile(ResourceCategory.ORE).copy(resourceExhausted = true)
        val extraction = resourceTile(ResourceCategory.ORE).copy(development = TileDevelopment(DevelopmentKind.EXTRACT))
        val network = ColonyNetworkSnapshot.empty().copy(
            sitePowerFactors = mapOf(MapPresentation.siteId(extraction.coordinate) to .4),
        )

        assertTrue(MapPresentation.isProblem(exhausted, network))
        assertTrue(MapPresentation.isProblem(extraction, network))
        assertTrue(MapPresentation.matches(extraction, MapFocus.PROBLEMS, emptySet(), emptySet(), emptySet(), network))
    }

    @Test
    fun `state filters compose with focus`() {
        val unsurveyed = plainTile(SectorCoordinate(-2, 1))
        val surveyed = unsurveyed.copy(revealed = true)
        val queued = setOf(unsurveyed.coordinate)
        val network = ColonyNetworkSnapshot.empty()

        assertTrue(MapPresentation.matches(unsurveyed, MapFocus.ALL, setOf(MapStateFilter.UNSURVEYED), emptySet(), emptySet(), network))
        assertFalse(MapPresentation.matches(surveyed, MapFocus.ALL, setOf(MapStateFilter.UNSURVEYED), emptySet(), emptySet(), network))
        assertTrue(MapPresentation.matches(unsurveyed, MapFocus.ALL, setOf(MapStateFilter.QUEUED), queued, emptySet(), network))
    }

    private fun plainTile(coordinate: SectorCoordinate) = WorldTile(
        coordinate = coordinate,
        terrain = TerrainType.PLAIN,
        terrainVariant = 1,
    )

    private fun resourceTile(category: ResourceCategory): WorldTile = WorldTile(
        coordinate = SectorCoordinate(-3, -3),
        terrain = TerrainType.PLAIN,
        terrainVariant = 1,
        revealed = true,
        deposit = ResourceDeposit(
            resourceId = ResourceId("fixture-${category.name.lowercase()}"),
            category = category,
            name = "Fixture ${category.name}",
            rarity = "Common",
            multiplier = 1.0,
            quality = 500,
            requiredScanningLevel = 1,
            requiredMiningLevel = 1,
            requiredMiningTech = "Mining I",
            terrainYieldFactor = 1.0,
            sustainability = Sustainability.FINITE,
            depositScale = "Modest",
            reserve = 1_000,
            initialReserve = 1_000,
        ),
    )
}
