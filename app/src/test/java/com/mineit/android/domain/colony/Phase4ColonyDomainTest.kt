package com.mineit.android.domain.colony

import com.mineit.android.domain.model.AbsoluteDay
import com.mineit.android.domain.model.GameDate
import com.mineit.android.domain.model.GameState
import com.mineit.android.domain.model.ResourceId
import com.mineit.android.domain.resources.Inventory
import com.mineit.android.domain.resources.QualityBand
import com.mineit.android.domain.resources.ResourceCategory
import com.mineit.android.domain.resources.ResourceStock
import com.mineit.android.domain.world.DevelopmentKind
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
import kotlin.math.floor

class Phase4ColonyDomainTest {
    private val development = ColonyDevelopmentService()
    private val headquarters = HeadquartersService()
    private val networks = ColonyNetworkService()

    @Test
    fun `building placement covers a resource and demolition restores it with canonical recovery`() {
        val coordinate = SectorCoordinate(1, 0)
        val initial = updateTile(settledState(), coordinate) { tile ->
            tile.copy(
                terrain = TerrainType.PLAIN,
                revealed = true,
                lastScannedAtLevel = 1,
                deposit = finiteOreDeposit(),
            )
        }
        val buildBefore = initial.activeColony.inventory.amountFor(ResourceCategory.BUILD)

        val placed = development.placeBuilding(initial, coordinate, DevelopmentKind.HOUSING)

        assertTrue(placed.ok)
        assertEquals(55.0, placed.cost?.build ?: -1.0, 0.0)
        val occupied = requireNotNull(placed.state.activeColony.world.tileAt(coordinate))
        assertEquals(DevelopmentKind.HOUSING, occupied.development?.kind)
        assertEquals(55.0, occupied.development?.investedBuild ?: -1.0, 0.0)
        assertTrue(occupied.resourceCovered)
        assertFalse(development.extractionPreview(placed.state, coordinate).ok)
        assertEquals(buildBefore - 55.0, placed.state.activeColony.inventory.amountFor(ResourceCategory.BUILD), 0.0)

        val demolished = development.demolish(placed.state, coordinate)

        assertTrue(demolished.ok)
        assertEquals(floor(55.0 * InfrastructureRules.DEMOLITION_RECOVERY), demolished.recoveredBuild, 0.0)
        val restored = requireNotNull(demolished.state.activeColony.world.tileAt(coordinate))
        assertEquals(null, restored.development)
        assertFalse(restored.resourceCovered)
        assertEquals(ResourceId("iron"), restored.deposit?.resourceId)
        assertEquals(buildBefore - 55.0 + 13.0, demolished.state.activeColony.inventory.amountFor(ResourceCategory.BUILD), 0.0)
    }

    @Test
    fun `industrial extraction upgrade enforces installed infrastructure then consumes Build and Ore`() {
        val coordinate = SectorCoordinate(1, 0)
        var state = updateTile(settledState(), coordinate) { tile ->
            tile.copy(
                terrain = TerrainType.HILL,
                revealed = true,
                lastScannedAtLevel = 1,
                deposit = finiteOreDeposit(),
            )
        }

        val developed = development.developExtraction(state, coordinate)
        assertTrue(developed.ok)
        state = developed.state

        val blocked = development.extractionUpgradePreview(state, coordinate)
        assertFalse(blocked.ok)
        assertTrue(blocked.reason.orEmpty().contains("installed Industry"))

        state = updateTile(state, SectorCoordinate(2, 0)) { tile ->
            tile.copy(
                terrain = TerrainType.PLAIN,
                revealed = true,
                development = TileDevelopment(DevelopmentKind.INDUSTRY, level = 5),
            )
        }
        state = updateTile(state, SectorCoordinate(3, 0)) { tile ->
            tile.copy(
                terrain = TerrainType.PLAIN,
                revealed = true,
                development = TileDevelopment(DevelopmentKind.POWER, level = 5),
            )
        }
        state = withExtraResources(state, build = 10_000.0, ore = 10_000.0)

        val preview = development.extractionUpgradePreview(state, coordinate)
        assertTrue(preview.reason.orEmpty(), preview.ok)
        assertEquals(2, preview.nextLevel)
        assertEquals(10.0, preview.cost?.ore ?: -1.0, 0.0)

        val buildBefore = state.activeColony.inventory.amountFor(ResourceCategory.BUILD)
        val oreBefore = state.activeColony.inventory.amountFor(ResourceCategory.ORE)
        val upgraded = development.upgradeExtraction(state, coordinate)

        assertTrue(upgraded.ok)
        val site = requireNotNull(upgraded.state.activeColony.world.tileAt(coordinate)).development
        assertEquals(2, site?.level)
        assertEquals(10.0, site?.investedOre ?: -1.0, 0.0)
        assertEquals(buildBefore - requireNotNull(upgraded.cost).build, upgraded.state.activeColony.inventory.amountFor(ResourceCategory.BUILD), 1e-9)
        assertEquals(oreBefore - 10.0, upgraded.state.activeColony.inventory.amountFor(ResourceCategory.ORE), 1e-9)
    }

    @Test
    fun `Power priority keeps Headquarters and life support ahead of Spaceport and commercial Industry`() {
        val headquartersCoordinate = SectorCoordinate(1, 0)
        var state = updateTile(settledState(), headquartersCoordinate) { tile ->
            tile.copy(
                terrain = TerrainType.PLAIN,
                revealed = true,
                development = TileDevelopment(DevelopmentKind.HEADQUARTERS, level = 1),
            )
        }
        state = updateTile(state, SectorCoordinate(2, 0)) { tile ->
            tile.copy(
                terrain = TerrainType.PLAIN,
                revealed = true,
                development = TileDevelopment(DevelopmentKind.POWER, level = 1),
            )
        }
        state = updateTile(state, SectorCoordinate(3, 0)) { tile ->
            tile.copy(
                terrain = TerrainType.PLAIN,
                revealed = true,
                development = TileDevelopment(DevelopmentKind.INDUSTRY, level = 1),
            )
        }
        state = state.withActiveColony { colony ->
            colony.copy(
                headquarters = HeadquartersIdentityState(
                    primary = headquartersCoordinate,
                    primaryEverAssigned = true,
                ),
                inventory = replaceCategoryAmount(
                    colony.inventory,
                    ResourceCategory.FUEL,
                    ResourceId("biomass"),
                    2.0,
                ),
            )
        }

        val network = networks.calculate(state)

        assertEquals(75.0, network.powerCapacity, 0.0)
        assertEquals(20.0, network.fuelLimitedGeneration, 1e-9)
        assertTrue(headquartersCoordinate in network.poweredHeadquarters)
        assertEquals(1.0, network.lifeSupportPowerFactor, 1e-9)
        assertEquals(1.0, network.spaceportPowerFactor, 1e-9)
        assertTrue("Commercial built Industry should receive only the generation left after higher bands.", network.industryPowerFactor in 0.0..0.1)
    }

    @Test
    fun `first founding ship departure requires staffed Headquarters but not Headquarters Power`() {
        val coordinate = SectorCoordinate(1, 0)
        val withoutHeadquarters = settledState()
        val missing = headquarters.departureGate(withoutHeadquarters)
        assertFalse(missing.ok)
        assertTrue(missing.failures.any { it.contains("Primary Headquarters") })

        var state = updateTile(withoutHeadquarters, coordinate) { tile ->
            tile.copy(
                terrain = TerrainType.PLAIN,
                revealed = true,
                development = TileDevelopment(DevelopmentKind.HEADQUARTERS, level = 1),
            )
        }
        state = state.withActiveColony { colony ->
            colony.copy(
                headquarters = HeadquartersIdentityState(
                    primary = coordinate,
                    primaryEverAssigned = true,
                ),
            )
        }

        assertEquals(0.0, networks.calculate(state).powerCapacity, 0.0)
        assertFalse(networks.calculate(state).headquarters.primaryOperational)
        val gate = headquarters.departureGate(state)
        assertTrue(gate.failures.joinToString(), gate.ok)

        val handedOver = headquarters.completeCommandHandover(state)
        assertTrue(handedOver.activeColony.headquarters.commandHandoverComplete)
    }

    @Test
    fun `Headquarters overload uses command weights and positive network bonus`() {
        val hq = SectorCoordinate(1, 0)
        var state = settledState()
        state = updateTile(state, hq) { it.copy(revealed = true, development = TileDevelopment(DevelopmentKind.HEADQUARTERS, 1)) }
        state = updateTile(state, SectorCoordinate(2, 0)) { it.copy(revealed = true, development = TileDevelopment(DevelopmentKind.HOUSING, 5)) }
        state = updateTile(state, SectorCoordinate(3, 0)) { it.copy(revealed = true, development = TileDevelopment(DevelopmentKind.POWER, 5)) }
        state = updateTile(state, SectorCoordinate(1, 1)) { it.copy(revealed = true, development = TileDevelopment(DevelopmentKind.INDUSTRY, 5)) }
        state = state.withActiveColony { colony ->
            colony.copy(headquarters = HeadquartersIdentityState(primary = hq, primaryEverAssigned = true))
        }

        val network = headquarters.network(state, setOf(hq))

        assertEquals(16.0, network.capacity, 0.0)
        assertEquals(30.0, network.load, 0.0)
        assertEquals(.02, network.bonus, 1e-9)
        assertEquals(.4375, network.overloadPenalty, 1e-9)
        assertEquals(.5825, network.efficiency, 1e-9)
    }

    @Test
    fun `Headquarters outage degrades daily and recovers linearly over ten days while ship command remains emergency only`() {
        val hq = SectorCoordinate(1, 0)
        var state = updateTile(settledState(), hq) { tile ->
            tile.copy(revealed = true, development = TileDevelopment(DevelopmentKind.HEADQUARTERS, 1))
        }
        state = state.withActiveColony { colony ->
            colony.copy(
                headquarters = HeadquartersIdentityState(
                    primary = hq,
                    primaryEverAssigned = true,
                    commandHandoverComplete = true,
                ),
            )
        }

        var unpoweredNetwork = headquarters.network(state, emptySet())
        assertEquals(CommandSourceType.SHIP, unpoweredNetwork.sourceType)
        assertEquals(16.0, unpoweredNetwork.capacity, 0.0)
        var continuity = headquarters.continuity(state, unpoweredNetwork)
        assertEquals(HeadquartersContinuityPhase.OUTAGE, continuity.phase)
        assertEquals(.10, continuity.penalty, 1e-9)
        assertFalse(continuity.networkAvailable)
        state = headquarters.persistContinuity(state, continuity)

        state = state.copy(date = GameDate.fromAbsoluteDay(AbsoluteDay(6)))
        unpoweredNetwork = headquarters.network(state, emptySet())
        continuity = headquarters.continuity(state, unpoweredNetwork)
        assertEquals(.15, continuity.penalty, 1e-9)
        assertEquals(5, continuity.offlineDays)
        state = headquarters.persistContinuity(state, continuity)

        val restoredNetwork = headquarters.network(state, setOf(hq))
        continuity = headquarters.continuity(state, restoredNetwork)
        assertEquals(HeadquartersContinuityPhase.RECOVERY, continuity.phase)
        assertEquals(.15, continuity.penalty, 1e-9)
        assertEquals(10, continuity.recoveryDaysRemaining)
        state = headquarters.persistContinuity(state, continuity)

        state = state.copy(date = GameDate.fromAbsoluteDay(AbsoluteDay(11)))
        continuity = headquarters.continuity(state, headquarters.network(state, setOf(hq)))
        assertEquals(HeadquartersContinuityPhase.RECOVERY, continuity.phase)
        assertEquals(.075, continuity.penalty, 1e-9)
        assertEquals(5, continuity.recoveryDaysRemaining)
        state = headquarters.persistContinuity(state, continuity)

        state = state.copy(date = GameDate.fromAbsoluteDay(AbsoluteDay(16)))
        continuity = headquarters.continuity(state, headquarters.network(state, setOf(hq)))
        assertEquals(HeadquartersContinuityPhase.ONLINE, continuity.phase)
        assertEquals(0.0, continuity.penalty, 0.0)
        assertEquals(0, continuity.recoveryDaysRemaining)
        assertTrue(continuity.networkAvailable)
    }

    private fun settledState(): GameState = EstablishedColonyFixture.contract01()

    private fun finiteOreDeposit(): ResourceDeposit = ResourceDeposit(
        resourceId = ResourceId("iron"),
        category = ResourceCategory.ORE,
        name = "Iron Ore",
        rarity = "Common",
        multiplier = 1.0,
        quality = 500,
        requiredScanningLevel = 1,
        requiredMiningLevel = 1,
        requiredMiningTech = "Surface Recovery",
        terrainYieldFactor = 1.0,
        sustainability = Sustainability.FINITE,
        depositScale = "Large",
        reserve = 100_000,
        initialReserve = 100_000,
    )

    private fun withExtraResources(state: GameState, build: Double, ore: Double): GameState = state.withActiveColony { colony ->
        colony.copy(
            inventory = colony.inventory
                .store(ResourceId("fiber"), ResourceCategory.BUILD, build, 500)
                .store(ResourceId("surface-iron"), ResourceCategory.ORE, ore, 500),
        )
    }

    private fun replaceCategoryAmount(
        inventory: Inventory,
        category: ResourceCategory,
        resourceId: ResourceId,
        amount: Double,
    ): Inventory = Inventory(
        inventory.resources.filterNot { it.category == category } +
            ResourceStock(resourceId, category, mapOf(QualityBand.EXCELLENT to amount)),
    )

    private fun updateTile(
        state: GameState,
        coordinate: SectorCoordinate,
        transform: (WorldTile) -> WorldTile,
    ): GameState = state.withActiveColony { colony ->
        colony.copy(
            world = colony.world.copy(
                tiles = colony.world.tiles.map { tile -> if (tile.coordinate == coordinate) transform(tile) else tile },
            ),
        )
    }

    private fun GameState.withActiveColony(transform: (com.mineit.android.domain.model.ColonyState) -> com.mineit.android.domain.model.ColonyState): GameState {
        val updated = transform(activeColony)
        return copy(colonies = colonies.map { if (it.id == updated.id) updated else it })
    }
}
