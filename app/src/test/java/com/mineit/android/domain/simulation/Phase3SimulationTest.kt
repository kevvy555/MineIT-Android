package com.mineit.android.domain.simulation

import com.mineit.android.domain.model.ResourceId
import com.mineit.android.domain.resources.Inventory
import com.mineit.android.domain.resources.QualityBand
import com.mineit.android.domain.resources.ResourceCategory
import com.mineit.android.domain.resources.ResourceStock
import com.mineit.android.domain.world.DevelopmentKind
import com.mineit.android.domain.world.ResourceDeposit
import com.mineit.android.domain.world.SectorCoordinate
import com.mineit.android.domain.world.Sustainability
import com.mineit.android.domain.world.TileDevelopment
import com.mineit.android.testing.EstablishedColonyFixture
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase3SimulationTest {
    private val engine = DailySimulationEngine()

    @Test
    fun `starter colony with no Power Plant has source-compatible survival demand and immediate life-support mortality`() {
        val state = settledState()
        val before = engine.recalculate(state)

        assertEquals(14.4, before.foodDemand, 1e-9)
        assertEquals(0.0, before.powerCapacity, 1e-9)
        assertEquals(18.4, before.powerDemand, 1e-9)
        assertEquals(50.0, before.industryInstalled, 1e-9)
        assertEquals(1.0, before.oreDemand, 1e-9)
        assertEquals(90.27777777777777, before.foodDays ?: -1.0, 1e-9)

        val result = engine.advanceDay(state)

        assertEquals(2, result.state.date.day)
        assertEquals(1_285.6, result.state.activeColony.inventory.amountFor(ResourceCategory.FOOD), 1e-9)
        assertEquals(259.0, result.state.activeColony.inventory.amountFor(ResourceCategory.ORE), 1e-9)
        assertEquals(116.4, result.state.activeColony.population, 1e-9)
        assertEquals(3.6, result.deaths, 1e-9)
        assertFalse(result.colonyDied)
    }

    @Test
    fun `L1 Power Plant keeps starter colony supplied and burns beginning-of-day Fuel`() {
        val state = withPowerPlant(settledState())
        val before = engine.recalculate(state)

        assertEquals(75.0, before.powerCapacity, 1e-9)
        assertEquals(7.5, before.powerFuelBurn, 1e-9)
        assertEquals(1.0, before.lifeSupportPowerFactor, 1e-9)
        assertEquals(1.0, before.industryPowerFactor, 1e-9)

        val result = engine.advanceDay(state)

        assertEquals(120.0, result.state.activeColony.population, 1e-9)
        assertEquals(0.0, result.deaths, 1e-9)
        assertEquals(667.5, result.state.activeColony.inventory.amountFor(ResourceCategory.FUEL), 1e-9)
        assertEquals(259.0, result.state.activeColony.inventory.amountFor(ResourceCategory.ORE), 1e-9)
        assertEquals(7.5, result.metrics.powerFuelConsumed, 1e-9)
    }

    @Test
    fun `L1 food extractor produces before same-day food consumption`() {
        var state = withPowerPlant(settledState())
        state = addExtractor(
            state = state,
            coordinate = SectorCoordinate(-2, -4),
            deposit = renewableDeposit(
                resourceId = "fungal",
                category = ResourceCategory.FOOD,
                name = "Fungal Shelf",
                requiredMiningLevel = 1,
            ),
        )

        val result = engine.advanceDay(state)

        assertEquals(10.0, result.metrics.foodProduction, 1e-9)
        assertEquals(1_295.6, result.state.activeColony.inventory.amountFor(ResourceCategory.FOOD), 1e-9)
        assertEquals(120.0, result.state.activeColony.population, 1e-9)
    }

    @Test
    fun `new Fuel collected during a day cannot increase that same day's generation`() {
        var state = withPowerPlant(settledState())
        state = replaceCategoryAmount(state, ResourceCategory.FUEL, ResourceId("biomass"), 1.0)
        state = addExtractor(
            state = state,
            coordinate = SectorCoordinate(-2, -4),
            deposit = renewableDeposit(
                resourceId = "biomass",
                category = ResourceCategory.FUEL,
                name = "Biomass",
                requiredMiningLevel = 1,
            ),
        )

        val result = engine.advanceDay(state)

        // L1 generation capacity is 75 and full burn is 7.5, so one unit at day start
        // can provide only 10 generation even though the fuel site collects later in the tick.
        assertEquals(10.0, result.metrics.powerFuelLimitedGeneration, 1e-9)
        assertEquals(1.0, result.metrics.powerFuelConsumed, 1e-9)
        assertTrue(result.metrics.fuelProduction > 0.0)
        assertTrue(result.state.activeColony.inventory.amountFor(ResourceCategory.FUEL) > 0.0)
    }

    @Test
    fun `zero Food removes workforce immediately but food mortality waits for 30 complete starvation days`() {
        var state = withPowerPlant(settledState())
        state = state.withActiveInventory(
            state.activeColony.inventory.copy(
                resources = state.activeColony.inventory.resources.filterNot { it.category == ResourceCategory.FOOD },
            ).store(ResourceId("biomass"), ResourceCategory.FUEL, 10_000.0, 500)
                .store(ResourceId("surface-iron"), ResourceCategory.ORE, 10_000.0, 500),
        )

        repeat(30) { index ->
            val result = engine.advanceDay(state)
            state = result.state
            assertEquals(120.0, state.activeColony.population, 1e-9)
            assertEquals(0.0, result.deaths, 1e-9)
            assertEquals(index + 1, state.activeColony.foodStarvationDays)
            assertEquals(0.0, engine.recalculate(state).workforceAvailable, 1e-9)
        }

        val day31 = engine.advanceDay(state)
        assertTrue(day31.deaths > 0.0)
        assertTrue(day31.state.activeColony.population < 120.0)
    }

    @Test
    fun `stable supplied colony keeps population and finite metrics through 25 year soak`() {
        var state = withPowerPlant(settledState())
        var inventory = state.activeColony.inventory
        inventory = inventory
            .store(ResourceId("fungal"), ResourceCategory.FOOD, 5_000_000.0, 500)
            .store(ResourceId("biomass"), ResourceCategory.FUEL, 5_000_000.0, 500)
            .store(ResourceId("surface-iron"), ResourceCategory.ORE, 5_000_000.0, 500)
            .store(ResourceId("fiber"), ResourceCategory.BUILD, 5_000_000.0, 500)
        state = state.withActiveInventory(inventory)
        val startingPopulation = state.activeColony.population

        repeat(25 * 360) {
            val result = engine.advanceDay(state)
            state = result.state
            assertFalse(result.colonyDied)
        }

        val metrics = engine.recalculate(state)
        assertEquals(startingPopulation, state.activeColony.population, 1e-9)
        assertEquals(26, state.date.year)
        assertEquals(1, state.date.day)
        assertTrue(metrics.foodDemand.isFinite())
        assertTrue(metrics.fuelDemand.isFinite())
        assertTrue(metrics.oreDemand.isFinite())
        assertTrue(metrics.powerFactor.isFinite())
        assertTrue(state.activeColony.inventory.amountFor(ResourceCategory.FOOD) >= 0.0)
        assertTrue(state.activeColony.inventory.amountFor(ResourceCategory.FUEL) >= 0.0)
        assertTrue(state.activeColony.inventory.amountFor(ResourceCategory.ORE) >= 0.0)
    }

    private fun settledState() = EstablishedColonyFixture.contract01()

    private fun withPowerPlant(state: com.mineit.android.domain.model.GameState): com.mineit.android.domain.model.GameState =
        updateTile(state, SectorCoordinate(1, 0)) { tile ->
            tile.copy(
                deposit = null,
                revealed = true,
                development = TileDevelopment(DevelopmentKind.POWER, level = 1),
            )
        }

    private fun addExtractor(
        state: com.mineit.android.domain.model.GameState,
        coordinate: SectorCoordinate,
        deposit: ResourceDeposit,
    ): com.mineit.android.domain.model.GameState = updateTile(state, coordinate) { tile ->
        tile.copy(
            revealed = true,
            lastScannedAtLevel = deposit.requiredScanningLevel,
            deposit = deposit,
            development = TileDevelopment(DevelopmentKind.EXTRACT, level = 1),
        )
    }

    private fun renewableDeposit(
        resourceId: String,
        category: ResourceCategory,
        name: String,
        requiredMiningLevel: Int,
    ) = ResourceDeposit(
        resourceId = ResourceId(resourceId),
        category = category,
        name = name,
        rarity = "Common",
        multiplier = 1.0,
        quality = 500,
        requiredScanningLevel = 1,
        requiredMiningLevel = requiredMiningLevel,
        requiredMiningTech = "Surface Recovery",
        terrainYieldFactor = 1.0,
        sustainability = Sustainability.RENEWABLE,
        abundance = 1.0,
        abundanceLabel = "Established",
        renewableOriginalRank = 1,
        renewableHealth = 2.0,
    )

    private fun replaceCategoryAmount(
        state: com.mineit.android.domain.model.GameState,
        category: ResourceCategory,
        resourceId: ResourceId,
        amount: Double,
    ): com.mineit.android.domain.model.GameState {
        val resources = state.activeColony.inventory.resources.filterNot { it.category == category } +
            ResourceStock(resourceId, category, mapOf(QualityBand.EXCELLENT to amount))
        return state.withActiveInventory(Inventory(resources))
    }

    private fun updateTile(
        state: com.mineit.android.domain.model.GameState,
        coordinate: SectorCoordinate,
        transform: (com.mineit.android.domain.world.WorldTile) -> com.mineit.android.domain.world.WorldTile,
    ): com.mineit.android.domain.model.GameState {
        val colony = state.activeColony
        val world = colony.world.copy(
            tiles = colony.world.tiles.map { tile -> if (tile.coordinate == coordinate) transform(tile) else tile },
        )
        val updated = colony.copy(world = world)
        return state.copy(colonies = state.colonies.map { if (it.id == updated.id) updated else it })
    }

    private fun com.mineit.android.domain.model.GameState.withActiveInventory(inventory: Inventory): com.mineit.android.domain.model.GameState {
        val updated = activeColony.copy(inventory = inventory)
        return copy(colonies = colonies.map { if (it.id == updated.id) updated else it })
    }
}
