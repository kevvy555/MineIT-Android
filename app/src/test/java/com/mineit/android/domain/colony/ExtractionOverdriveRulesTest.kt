package com.mineit.android.domain.colony

import com.mineit.android.domain.model.GameState
import com.mineit.android.domain.model.ResourceId
import com.mineit.android.domain.resources.ResourceCategory
import com.mineit.android.domain.world.DevelopmentKind
import com.mineit.android.domain.world.ExtractionAccidentOutcome
import com.mineit.android.domain.world.ExtractionOperatingMode
import com.mineit.android.domain.world.ResourceDeposit
import com.mineit.android.domain.world.SectorCoordinate
import com.mineit.android.domain.world.Sustainability
import com.mineit.android.domain.world.TerrainType
import com.mineit.android.domain.world.TileDevelopment
import com.mineit.android.domain.world.WorldTile
import com.mineit.android.testing.EstablishedColonyFixture
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.ceil

class ExtractionOverdriveRulesTest {
    @Test
    fun `profiles and supported families match maintained web rules`() {
        val state = EstablishedColonyFixture.contract01()
        val mine = oreSite(state, "iron")
        val renewable = mine.copy(deposit = requireNotNull(mine.deposit).copy(sustainability = Sustainability.RENEWABLE))
        val food = mine.copy(deposit = requireNotNull(mine.deposit).copy(category = ResourceCategory.FOOD))

        assertEquals(1.25, ExtractionOverdriveRules.profiles.first { it.mode == ExtractionOperatingMode.PUSHED }.workforceMultiplier, 0.0)
        assertEquals(1.15, ExtractionOverdriveRules.profiles.first { it.mode == ExtractionOperatingMode.PUSHED }.outputMultiplier, 0.0)
        assertEquals(1.50, ExtractionOverdriveRules.profiles.first { it.mode == ExtractionOperatingMode.HARD }.workforceMultiplier, 0.0)
        assertEquals(1.30, ExtractionOverdriveRules.profiles.first { it.mode == ExtractionOperatingMode.HARD }.outputMultiplier, 0.0)
        assertTrue(ExtractionOverdriveRules.supports(mine))
        assertFalse(ExtractionOverdriveRules.supports(renewable))
        assertFalse(ExtractionOverdriveRules.supports(food))
    }

    @Test
    fun `hard mode increases output and workforce using canonical shared rules`() {
        val state = EstablishedColonyFixture.contract01()
        val normal = oreSite(state, "iron")
        val hard = normal.copy(development = requireNotNull(normal.development).copy(operatingMode = ExtractionOperatingMode.HARD))
        val normalOutput = SiteProductionRules.potential(state.activeColony, normal)
        val normalWorkers = SiteOperationRules.workforceRequirement(state.activeColony, normal)

        assertEquals(normalOutput * 1.30, SiteProductionRules.potential(state.activeColony, hard), .0001)
        assertEquals(ceil(normalWorkers * 1.50), SiteOperationRules.workforceRequirement(state.activeColony, hard), .0001)
    }

    @Test
    fun `risk threshold can cause machinery accident and three day shutdown`() {
        val state = EstablishedColonyFixture.contract01()
        var tile = oreSite(state, "iron").copy(
            development = TileDevelopment(
                kind = DevelopmentKind.EXTRACT,
                operatingMode = ExtractionOperatingMode.HARD,
                overdriveExposure = 29.0,
            ),
        )
        val rolls = ArrayDeque(listOf(0.0, .9))

        val result = ExtractionOverdriveRules.advanceRisk(state, tile) { rolls.removeFirst() }
        tile = result.tile

        assertEquals(ExtractionAccidentOutcome.MACHINERY, result.accident?.outcome)
        assertEquals("Tunnel Collapse", result.accident?.name)
        assertEquals(3, tile.development?.accidentShutdownDays)
        assertEquals(ExtractionOperatingMode.NORMAL, tile.development?.operatingMode)
        assertEquals(0.0, ExtractionOverdriveRules.riskExposure(tile), 0.0)
        assertTrue(tile.development?.productionStopped == true)

        tile = ExtractionOverdriveRules.advanceShutdownDay(tile)
        assertEquals(2, tile.development?.accidentShutdownDays)
        tile = ExtractionOverdriveRules.advanceShutdownDay(tile)
        tile = ExtractionOverdriveRules.advanceShutdownDay(tile)
        assertEquals(0, tile.development?.accidentShutdownDays)
        assertFalse(tile.development?.productionStopped == true)
    }

    @Test
    fun `deep mine fatal accident preserves source maximum fatality roll`() {
        val state = EstablishedColonyFixture.contract01()
        val tile = oreSite(state, "diamond").copy(
            development = TileDevelopment(
                kind = DevelopmentKind.EXTRACT,
                operatingMode = ExtractionOperatingMode.HARD,
                overdriveExposure = 29.0,
            ),
        )
        val rolls = ArrayDeque(listOf(0.0, 0.0, .999))

        val result = ExtractionOverdriveRules.advanceRisk(state, tile) { rolls.removeFirst() }

        assertEquals(ExtractionAccidentOutcome.FATALITIES, result.accident?.outcome)
        assertEquals("Rockburst & Shaft Collapse", result.accident?.name)
        assertEquals(4, result.accident?.deaths)
    }

    private fun oreSite(state: GameState, resourceId: String): WorldTile = WorldTile(
        coordinate = SectorCoordinate(2, 3),
        terrain = TerrainType.MOUNTAIN,
        terrainVariant = 1,
        revealed = true,
        deposit = ResourceDeposit(
            resourceId = ResourceId(resourceId),
            category = ResourceCategory.ORE,
            name = resourceId,
            rarity = "Common",
            multiplier = 1.0,
            quality = 500,
            requiredScanningLevel = 1,
            requiredMiningLevel = 1,
            requiredMiningTech = "Mining L1",
            terrainYieldFactor = 1.0,
            sustainability = Sustainability.FINITE,
            depositScale = "Large",
            reserve = 10_000,
            initialReserve = 10_000,
        ),
        development = TileDevelopment(DevelopmentKind.EXTRACT),
    )
}
