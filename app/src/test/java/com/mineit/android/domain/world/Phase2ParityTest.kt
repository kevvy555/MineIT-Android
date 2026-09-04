package com.mineit.android.domain.world

import com.mineit.android.domain.contracts.Contract01
import com.mineit.android.domain.model.ColonyId
import com.mineit.android.domain.model.ColonyStatus
import com.mineit.android.domain.model.NewGameFactory
import com.mineit.android.domain.model.ResourceId
import com.mineit.android.domain.resources.ExtractionCompatibility
import com.mineit.android.domain.resources.ExtractionFamily
import com.mineit.android.domain.resources.ResourceCatalogue
import com.mineit.android.domain.resources.ResourceCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase2ParityTest {
    @Test
    fun `JavaScript hash and seeded random primitives match pinned web baseline`() {
        val seed = DeterministicHash.hashString("hello")
        assertEquals(1_335_831_723u, seed)

        val random = DeterministicRandom(seed)
        assertEquals(0.6311965801287442, random.nextDouble(), 0.0)
        assertEquals(0.7983490515034646, random.nextDouble(), 0.0)
        assertEquals(0.30862852558493614, random.nextDouble(), 0.0)
    }

    @Test
    fun `Contract 01 uses current web starter data and generic inventory`() {
        val state = NewGameFactory().contract01(
            colonySeed = 123456789,
            colonyId = ColonyId("intro-123456789"),
        )
        val colony = state.activeColony
        val contract = requireNotNull(colony.contract)

        assertEquals(32_000L, state.company.cash)
        assertEquals(120, colony.population)
        assertEquals(ColonyStatus.SITE_SELECTION, colony.status)
        assertEquals("Koplin Mining Charter — Contract 01", contract.name)
        assertEquals(10, contract.years)
        assertEquals(120, contract.goals.food)
        assertEquals(520, contract.goals.industry)
        assertEquals(1_050, contract.goals.population)
        assertEquals(1_300.0, colony.inventory.amountFor(ResourceCategory.FOOD), 0.0)
        assertEquals(520.0, colony.inventory.amountFor(ResourceCategory.BUILD), 0.0)
        assertEquals(420.0, colony.inventory.amountFor(ResourceCategory.FUEL), 0.0)
        assertEquals(260.0, colony.inventory.amountFor(ResourceCategory.ORE), 0.0)
        assertEquals(8, colony.world.landingCandidates.size)
    }

    @Test
    fun `current resource catalogue and extraction mapping are canonical`() {
        assertEquals(40, ResourceCatalogue.all.size)
        assertEquals("Thermal Algae", ResourceCatalogue.require(ResourceId("thermal")).name)
        assertEquals(2, ResourceCatalogue.require(ResourceId("thermal")).scanningLevel)
        assertTrue(ResourceCatalogue.require(ResourceId("synthetic")).manufactured)
        assertEquals(ExtractionFamily.RANCH, ExtractionCompatibility.familyFor(ResourceId("herd")))
        assertEquals(ExtractionFamily.RIG, ExtractionCompatibility.familyFor(ResourceId("gas")))
        assertEquals(ExtractionFamily.DEEP_MINE, ExtractionCompatibility.familyFor(ResourceId("diamond")))
        assertEquals(ExtractionFamily.QUARRY, ExtractionCompatibility.familyFor(ResourceId("stone")))
    }

    @Test
    fun `temperate landing candidate matches pinned JavaScript terrain fixture`() {
        val state = NewGameFactory().contract01(
            colonySeed = 123456789,
            colonyId = ColonyId("intro-123456789"),
        )
        val candidate = state.activeColony.world.landingCandidates.first()

        assertEquals(730_361_737L, candidate.seed)
        assertEquals(64, candidate.cells.size)
        assertEquals(38, candidate.counts.getValue(TerrainType.PLAIN))
        assertEquals(6, candidate.counts.getValue(TerrainType.HILL))
        assertEquals(14, candidate.counts.getValue(TerrainType.MOUNTAIN))
        assertEquals(6, candidate.counts.getValue(TerrainType.LAKE))
        assertEquals(TerrainType.PLAIN, candidate.cells.first().terrain)
        assertEquals(1, candidate.cells.first().variant)
        assertEquals(TerrainType.PLAIN, candidate.cells.first { it.coordinate == SectorCoordinate(0, 0) }.terrain)
    }

    @Test
    fun `surface discovery and higher level resurvey match pinned JavaScript fixture`() {
        val factory = NewGameFactory()
        val state = factory.contract01(
            colonySeed = 123456789,
            colonyId = ColonyId("intro-123456789"),
        )
        val settled = factory.settleLandingSite(state, 0)
        val contract = requireNotNull(settled.activeColony.contract)
        val discovery = WorldDiscovery()

        val nutrientTile = settled.activeColony.world.tileAt(SectorCoordinate(-2, -4))!!
        val nutrient = discovery.reveal(settled.activeColony.seed, contract, nutrientTile, 1)
        assertEquals(ResourceId("nutrient"), nutrient.deposit?.resourceId)
        assertEquals(49, nutrient.deposit?.quality)
        assertEquals("Established", nutrient.deposit?.abundanceLabel)
        assertEquals(1.10, nutrient.deposit?.terrainYieldFactor ?: 0.0, 0.0)

        val deepGasTile = settled.activeColony.world.tileAt(SectorCoordinate(-3, -4))!!
        val levelOne = discovery.reveal(settled.activeColony.seed, contract, deepGasTile, 1)
        assertTrue(levelOne.revealed)
        assertNull(levelOne.deposit)
        assertEquals(1, levelOne.lastScannedAtLevel)

        val levelFive = discovery.reveal(settled.activeColony.seed, contract, levelOne, 5)
        assertEquals(ResourceId("gas"), levelFive.deposit?.resourceId)
        assertEquals(449, levelFive.deposit?.quality)
        assertEquals("Large", levelFive.deposit?.depositScale)
        assertEquals(126_126L, levelFive.deposit?.reserve)
    }

    @Test
    fun `survey timing queue and resurvey rules match web semantics`() {
        val factory = NewGameFactory()
        val state = factory.contract01(
            colonySeed = 123456789,
            colonyId = ColonyId("intro-123456789"),
        )
        val settled = factory.settleLandingSite(state, 0)
        val colony = settled.activeColony
        val contract = requireNotNull(colony.contract)
        val survey = SurveyService()
        val coordinate = SectorCoordinate(-2, -4)

        assertEquals(9, survey.baseDays(contract, coordinate))
        var world = survey.enqueue(colony.world, contract, coordinate, scanningLevel = 1)
        assertEquals(1, world.activeSurveys.size)
        assertEquals(9, world.activeSurveys.single().totalDays)
        assertFalse(world.surveyQueue.contains(coordinate))

        repeat(9) {
            world = survey.tick(
                world = world,
                colonySeed = colony.seed,
                contract = contract,
                currentScanningLevel = 1,
            ).world
        }
        val revealed = world.tileAt(coordinate)!!
        assertTrue(revealed.revealed)
        assertNotNull(revealed.deposit)
        assertTrue(survey.isResurveyable(world, coordinate, scanningLevel = 2))
        assertEquals(5, survey.days(contract, coordinate, resurvey = true))
        assertFalse(survey.surveyable(world, contract, SectorCoordinate(0, 0), scanningLevel = 2))
    }
}
