package com.mineit.android.ui.game

import com.mineit.android.domain.colony.ColonyNetworkSnapshot
import com.mineit.android.domain.colony.CommandSourceType
import com.mineit.android.domain.colony.DevelopmentPreview
import com.mineit.android.domain.colony.HeadquartersContinuity
import com.mineit.android.domain.colony.HeadquartersContinuityPhase
import com.mineit.android.domain.colony.HeadquartersDepartureGate
import com.mineit.android.domain.colony.HeadquartersIdentityState
import com.mineit.android.domain.colony.HeadquartersNetwork
import com.mineit.android.domain.colony.HeadquartersOutageState
import com.mineit.android.domain.colony.HeadquartersRow
import com.mineit.android.domain.colony.InfrastructureCost
import com.mineit.android.domain.colony.InfrastructureRules
import com.mineit.android.domain.model.GameState
import com.mineit.android.domain.world.DevelopmentKind
import com.mineit.android.domain.world.SectorCoordinate
import com.mineit.android.domain.world.TileDevelopment
import com.mineit.android.domain.world.WorldTile
import com.mineit.android.testing.EstablishedColonyFixture
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HeadquartersControlPresentationTest {
    private val selected = SectorCoordinate(1, 0)

    @Test
    fun `active Primary Headquarters mirrors source Colony Control hierarchy`() {
        val state = stateWithHeadquarters(level = 2, primary = selected)
        val row = row(selected, level = 2, primary = true, staffed = true, powered = true)
        val headquarters = headquarters(
            rows = listOf(row),
            source = CommandSourceType.HEADQUARTERS,
            primaryOperational = true,
            capacity = 36.0,
            load = 12.0,
            bonus = .04,
            efficiency = 1.04,
        )
        val network = snapshot(
            headquarters = headquarters,
            phase = HeadquartersContinuityPhase.ONLINE,
            networkAvailable = true,
            effectiveCommandEfficiency = 1.04,
        )
        val model = HeadquartersControlPresentation.build(
            state = state,
            network = network,
            departureGate = HeadquartersDepartureGate(true, emptyList()),
            tile = requireNotNull(state.activeColony.world.tileAt(selected)),
            upgradePreview = DevelopmentPreview(
                ok = true,
                cost = InfrastructureCost(build = 300.0, ore = 65.0),
                nextLevel = 3,
            ),
        )

        assertEquals(
            listOf("COMMAND CAPACITY", "STAFF", "POWER", "ROLE"),
            model.overview.map { it.label },
        )
        assertEquals(
            listOf(
                "NETWORK SOURCE",
                "CONGLOMERATE LINK",
                "TOTAL CAPACITY",
                "COMMAND LOAD",
                "COMMAND EFFICIENCY",
                "OUTAGE CONTINUITY",
                "EFFECTIVE OUTPUT",
            ),
            model.operations.map { it.label },
        )
        assertTrue(model.primary)
        assertFalse(model.primaryEligible)
        assertEquals("ACTIVE", model.status)
        assertEquals("PRIMARY", model.metric("ROLE").value)
        assertEquals("HEADQUARTERS", model.operation("NETWORK SOURCE").value)
        assertEquals("ONLINE", model.operation("CONGLOMERATE LINK").value)
        assertEquals("104%", model.operation("COMMAND EFFICIENCY").value)
        assertEquals("READY", model.handoverStatus)
        assertTrue(model.upgradeReady)
        assertEquals(3, model.nextLevel)
        assertEquals(28.0, model.capacityGain, 0.0)
        assertEquals(listOf("BUILD", "ORE"), model.requirements.map { it.label })
        assertTrue("The established fixture retains its docked founding command ship.", model.commandShipFallback)
    }

    @Test
    fun `staffed Expansion Headquarters remains selectable while Primary outage is prominent`() {
        val primary = SectorCoordinate(2, 0)
        val state = stateWithHeadquarters(level = 1, primary = primary)
        val selectedRow = row(selected, level = 1, primary = false, staffed = true, powered = true)
        val primaryRow = row(primary, level = 1, primary = true, staffed = true, powered = false)
        val headquarters = headquarters(
            rows = listOf(selectedRow, primaryRow),
            source = CommandSourceType.SHIP,
            primaryOperational = false,
            capacity = 32.0,
            load = 20.0,
            bonus = .02,
            efficiency = 1.02,
        )
        val network = snapshot(
            headquarters = headquarters,
            phase = HeadquartersContinuityPhase.OUTAGE,
            networkAvailable = false,
            efficiencyFactor = .86,
            effectiveCommandEfficiency = .8772,
            offlineDays = 4,
        )
        val model = HeadquartersControlPresentation.build(
            state = state,
            network = network,
            departureGate = HeadquartersDepartureGate(false, listOf("Primary Headquarters must be fully staffed.")),
            tile = requireNotNull(state.activeColony.world.tileAt(selected)),
            upgradePreview = DevelopmentPreview(
                ok = false,
                reason = "Need 170 Build materials.",
                cost = InfrastructureCost(build = 170.0, ore = 25.0),
                nextLevel = 2,
            ),
        )

        assertFalse(model.primary)
        assertTrue(model.primaryEligible)
        assertEquals("EXPANSION", model.metric("ROLE").value)
        assertEquals("SHIP", model.operation("NETWORK SOURCE").value)
        assertEquals("OFFLINE", model.operation("CONGLOMERATE LINK").value)
        assertEquals("86%", model.operation("OUTAGE CONTINUITY").value)
        assertEquals("87%", model.operation("EFFECTIVE OUTPUT").value)
        assertNotNull(model.alert)
        assertEquals("CONGLOMERATE NETWORK OFFLINE", model.alert?.title)
        assertEquals("BLOCKED", model.handoverStatus)
        assertTrue(model.handoverDetail.contains("fully staffed"))
        assertFalse(model.upgradeReady)
        assertEquals("Need 170 Build materials.", model.upgradeReason)
    }

    @Test
    fun `L5 Headquarters reports max level without fabricated requirements`() {
        val state = stateWithHeadquarters(level = 5, primary = selected)
        val row = row(selected, level = 5, primary = true, staffed = true, powered = true)
        val headquarters = headquarters(
            rows = listOf(row),
            source = CommandSourceType.HEADQUARTERS,
            primaryOperational = true,
            capacity = InfrastructureRules.headquartersCapacity(5),
            load = 70.0,
            bonus = .10,
            efficiency = 1.10,
        )
        val network = snapshot(
            headquarters = headquarters,
            phase = HeadquartersContinuityPhase.ONLINE,
            networkAvailable = true,
            effectiveCommandEfficiency = 1.10,
        )
        val model = HeadquartersControlPresentation.build(
            state = state,
            network = network,
            departureGate = HeadquartersDepartureGate(true, emptyList()),
            tile = requireNotNull(state.activeColony.world.tileAt(selected)),
            upgradePreview = DevelopmentPreview(ok = false, reason = "Headquarters is already at L5.", max = true),
        )

        assertTrue(model.maxLevel)
        assertFalse(model.upgradeReady)
        assertEquals(5, model.nextLevel)
        assertEquals(0.0, model.capacityGain, 0.0)
        assertTrue(model.requirements.isEmpty())
    }

    private fun stateWithHeadquarters(level: Int, primary: SectorCoordinate): GameState {
        var state = EstablishedColonyFixture.contract01()
        state = updateTile(state, selected) { tile ->
            tile.copy(revealed = true, development = TileDevelopment(DevelopmentKind.HEADQUARTERS, level))
        }
        if (primary != selected) {
            state = updateTile(state, primary) { tile ->
                tile.copy(revealed = true, development = TileDevelopment(DevelopmentKind.HEADQUARTERS, 1))
            }
        }
        return state.withActiveColony { colony ->
            colony.copy(
                headquarters = HeadquartersIdentityState(
                    primary = primary,
                    primaryEverAssigned = true,
                    commandHandoverComplete = false,
                ),
            )
        }
    }

    private fun row(
        coordinate: SectorCoordinate,
        level: Int,
        primary: Boolean,
        staffed: Boolean,
        powered: Boolean,
    ) = HeadquartersRow(
        coordinate = coordinate,
        level = level,
        capacity = InfrastructureRules.headquartersCapacity(level),
        requiredStaff = InfrastructureRules.headquartersMinimumStaff(level),
        requiredPower = InfrastructureRules.headquartersPower(level),
        constructed = true,
        staffed = staffed,
        powered = powered,
        primary = primary,
    )

    private fun headquarters(
        rows: List<HeadquartersRow>,
        source: CommandSourceType,
        primaryOperational: Boolean,
        capacity: Double,
        load: Double,
        bonus: Double,
        efficiency: Double,
    ) = HeadquartersNetwork(
        rows = rows,
        sourceType = source,
        sourceCoordinate = rows.firstOrNull { it.primary }?.coordinate.takeIf { source == CommandSourceType.HEADQUARTERS },
        primaryOperational = primaryOperational,
        reservedStaff = rows.filter { it.staffed }.sumOf { it.requiredStaff },
        capacity = capacity,
        load = load,
        overloadPenalty = 0.0,
        bonus = bonus,
        efficiency = efficiency,
    )

    private fun snapshot(
        headquarters: HeadquartersNetwork,
        phase: HeadquartersContinuityPhase,
        networkAvailable: Boolean,
        efficiencyFactor: Double = 1.0,
        effectiveCommandEfficiency: Double,
        offlineDays: Int = 0,
    ): ColonyNetworkSnapshot {
        val continuity = HeadquartersContinuity(
            phase = phase,
            established = true,
            primaryOperational = headquarters.primaryOperational,
            networkAvailable = networkAvailable,
            penalty = 1.0 - efficiencyFactor,
            efficiencyFactor = efficiencyFactor,
            effectiveCommandEfficiency = effectiveCommandEfficiency,
            offlineDays = offlineDays,
            recoveryDaysRemaining = if (phase == HeadquartersContinuityPhase.RECOVERY) 5 else 0,
            downTools = !networkAvailable,
            reason = if (networkAvailable) "Headquarters network online." else "Primary Headquarters offline; conglomerate network unavailable.",
            network = headquarters,
            persisted = HeadquartersOutageState(
                phase = phase,
                penalty = 1.0 - efficiencyFactor,
                offlineDays = offlineDays,
            ),
        )
        return ColonyNetworkSnapshot.empty().copy(
            headquarters = headquarters,
            continuity = continuity,
            poweredHeadquarters = headquarters.rows.filter { it.powered }.mapTo(linkedSetOf()) { it.coordinate },
        )
    }

    private fun HeadquartersControlModel.metric(label: String) = overview.first { it.label == label }
    private fun HeadquartersControlModel.operation(label: String) = operations.first { it.label == label }

    private fun updateTile(state: GameState, coordinate: SectorCoordinate, transform: (WorldTile) -> WorldTile): GameState =
        state.withActiveColony { colony ->
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
