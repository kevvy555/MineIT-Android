package com.mineit.android.domain.colony

import com.mineit.android.domain.config.MineItConfig
import com.mineit.android.domain.model.ColonyState
import com.mineit.android.domain.model.ColonyStatus
import com.mineit.android.domain.model.GameDate
import com.mineit.android.domain.model.GameState
import com.mineit.android.domain.resources.ResourceCategory
import com.mineit.android.domain.world.DevelopmentKind
import com.mineit.android.domain.world.SectorCoordinate
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/** Canonical native owner for A08a/A08b Headquarters command and continuity semantics. */
class HeadquartersService {
    fun baseWorkforceAvailable(colony: ColonyState): Double = if (
        colony.status == ColonyStatus.DEAD || colony.foodStarvationDays > 0
    ) 0.0 else floor(colony.population * MineItConfig.WORKFORCE_SHARE)

    fun synchronizePrimary(state: GameState): GameState {
        val colony = state.activeColony
        val coords = headquartersTiles(colony).map { it.coordinate }.toSet()
        var identity = colony.headquarters
        if (identity.primary != null && identity.primary !in coords) {
            identity = identity.copy(primary = null, primaryEverAssigned = true)
        }
        if (identity.primary == null && !identity.primaryEverAssigned) {
            val provisional = staffing(colony.copy(headquarters = identity), null)
                .firstOrNull { it.staffed && it.constructed }
            if (provisional != null) {
                identity = identity.copy(primary = provisional.coordinate, primaryEverAssigned = true)
            }
        }
        return state.withActiveColony(colony.copy(headquarters = identity))
    }

    fun setPrimary(state: GameState, coordinate: SectorCoordinate): HeadquartersActionResult {
        val synced = synchronizePrimary(state)
        val colony = synced.activeColony
        val row = staffing(colony, colony.headquarters.primary).firstOrNull { it.coordinate == coordinate }
        if (row == null || !row.constructed || !row.staffed) {
            return HeadquartersActionResult(false, state, "Primary Headquarters must be fully constructed and staffed.")
        }
        val updated = colony.copy(
            headquarters = colony.headquarters.copy(primary = coordinate, primaryEverAssigned = true),
        )
        return HeadquartersActionResult(true, synced.withActiveColony(updated), "Primary Headquarters changed.")
    }

    fun network(
        state: GameState,
        poweredHeadquarters: Set<SectorCoordinate>,
    ): HeadquartersNetwork {
        val colony = state.activeColony
        val primary = colony.headquarters.primary
        var rows = staffing(colony, primary).map { row ->
            row.copy(
                powered = row.coordinate in poweredHeadquarters,
                primary = row.coordinate == primary,
            )
        }
        val primaryRow = rows.firstOrNull { it.coordinate == primary }
        val primaryOperational = primaryRow?.let { it.constructed && it.staffed && it.powered } == true
        val shipAvailable = colony.foundingShipDocked
        val sourceType = when {
            primaryOperational -> CommandSourceType.HEADQUARTERS
            shipAvailable -> CommandSourceType.SHIP
            else -> null
        }
        if (sourceType == null) {
            rows = rows.map { row -> if (row.primary) row else row.copy(staffed = false) }
        }
        val active = if (sourceType == null) emptyList() else rows.filter { it.staffed && it.powered }
        val activeForCapacity = when (sourceType) {
            CommandSourceType.HEADQUARTERS -> active
            CommandSourceType.SHIP -> active.filterNot { it.primary }
            null -> emptyList()
        }
        val capacity = when (sourceType) {
            CommandSourceType.HEADQUARTERS -> activeForCapacity.sumOf { it.capacity }
            CommandSourceType.SHIP -> InfrastructureRules.TEMPORARY_SHIP_COMMAND_CAPACITY + activeForCapacity.sumOf { it.capacity }
            null -> 0.0
        }
        val bonus = headquartersBonus(activeForCapacity)
        val load = commandLoad(colony)
        val overloadRatio = if (capacity > 0.0) max(0.0, load / capacity - 1.0) else if (load > 0.0) 1.0 else 0.0
        val overloadPenalty = min(
            InfrastructureRules.HEADQUARTERS_OVERLOAD_PENALTY_CAP,
            overloadRatio * InfrastructureRules.HEADQUARTERS_OVERLOAD_PENALTY_PER_RATIO,
        )
        val efficiency = (1.0 + bonus - overloadPenalty).coerceIn(0.0, 1.15)
        return HeadquartersNetwork(
            rows = rows,
            sourceType = sourceType,
            sourceCoordinate = if (sourceType == CommandSourceType.HEADQUARTERS) primary else null,
            primaryOperational = primaryOperational,
            reservedStaff = rows.filter { it.staffed }.sumOf { it.requiredStaff },
            capacity = capacity,
            load = load,
            overloadPenalty = overloadPenalty,
            bonus = bonus,
            efficiency = efficiency,
        )
    }

    fun continuity(
        state: GameState,
        network: HeadquartersNetwork,
    ): HeadquartersContinuity {
        val colony = state.activeColony
        val established = colony.headquarters.commandHandoverComplete
        val current = colony.headquarters.outage
        val now = state.date.absoluteDay
        var next = current

        when (current.phase) {
            HeadquartersContinuityPhase.OUTAGE -> {
                val started = current.outageStartedAbsoluteDay ?: now
                val elapsed = max(0, now - started)
                next = current.copy(
                    outageStartedAbsoluteDay = started,
                    offlineDays = elapsed,
                    penalty = ((if (current.outageStartPenalty > 0) current.outageStartPenalty else .10) + elapsed * .01)
                        .coerceIn(0.0, 1.0),
                )
            }
            HeadquartersContinuityPhase.RECOVERY -> {
                val started = current.recoveryStartedAbsoluteDay ?: now
                val elapsed = (now - started).coerceIn(0, RECOVERY_DAYS)
                val initial = (if (current.recoveryInitialPenalty > 0) current.recoveryInitialPenalty else current.penalty)
                    .coerceIn(0.0, 1.0)
                next = if (elapsed >= RECOVERY_DAYS) {
                    current.copy(
                        phase = HeadquartersContinuityPhase.ONLINE,
                        penalty = 0.0,
                        recoveryStartedAbsoluteDay = null,
                        recoveryInitialPenalty = 0.0,
                        recoveryDaysElapsed = 0,
                        recoveryDaysRemaining = 0,
                    )
                } else {
                    current.copy(
                        recoveryStartedAbsoluteDay = started,
                        recoveryInitialPenalty = initial,
                        recoveryDaysElapsed = elapsed,
                        recoveryDaysRemaining = RECOVERY_DAYS - elapsed,
                        penalty = (initial * (1.0 - elapsed.toDouble() / RECOVERY_DAYS)).coerceIn(0.0, 1.0),
                    )
                }
            }
            HeadquartersContinuityPhase.ONLINE -> Unit
        }

        next = when {
            !established -> HeadquartersOutageState()
            !network.primaryOperational && next.phase != HeadquartersContinuityPhase.OUTAGE -> next.copy(
                phase = HeadquartersContinuityPhase.OUTAGE,
                penalty = max(.10, next.penalty),
                offlineDays = 0,
                outageStartedAbsoluteDay = now,
                outageStartPenalty = max(.10, next.penalty),
                recoveryStartedAbsoluteDay = null,
                recoveryInitialPenalty = 0.0,
                recoveryDaysElapsed = 0,
                recoveryDaysRemaining = 0,
            )
            network.primaryOperational && next.phase == HeadquartersContinuityPhase.OUTAGE -> {
                val loss = next.penalty
                next.copy(
                    phase = if (loss > 0) HeadquartersContinuityPhase.RECOVERY else HeadquartersContinuityPhase.ONLINE,
                    lastOutageDays = next.offlineDays,
                    recoveryStartedAbsoluteDay = if (loss > 0) now else null,
                    recoveryInitialPenalty = loss,
                    recoveryDaysElapsed = 0,
                    recoveryDaysRemaining = if (loss > 0) RECOVERY_DAYS else 0,
                )
            }
            else -> next
        }

        val factor = (1.0 - next.penalty).coerceIn(0.0, 1.0)
        val networkAvailable = !established || network.primaryOperational
        val reason = when {
            networkAvailable && next.phase == HeadquartersContinuityPhase.RECOVERY -> "Headquarters restored; operational efficiency is recovering."
            networkAvailable -> "Headquarters network online."
            network.sourceType == CommandSourceType.SHIP -> "Primary Headquarters offline; emergency ship command cannot access the conglomerate network."
            else -> "Primary Headquarters offline; conglomerate network unavailable."
        }
        return HeadquartersContinuity(
            phase = next.phase,
            established = established,
            primaryOperational = network.primaryOperational,
            networkAvailable = networkAvailable,
            penalty = next.penalty,
            efficiencyFactor = factor,
            effectiveCommandEfficiency = (network.efficiency * factor).coerceIn(0.0, 1.15),
            offlineDays = next.offlineDays,
            recoveryDaysRemaining = next.recoveryDaysRemaining,
            downTools = factor <= .000001,
            reason = reason,
            network = network,
            persisted = next,
        )
    }

    fun persistContinuity(state: GameState, continuity: HeadquartersContinuity): GameState {
        val colony = state.activeColony
        return state.withActiveColony(
            colony.copy(headquarters = colony.headquarters.copy(outage = continuity.persisted)),
        )
    }

    fun departureGate(state: GameState): HeadquartersDepartureGate {
        val colony = state.activeColony
        if (colony.headquarters.commandHandoverComplete) return HeadquartersDepartureGate(true, emptyList())
        val primary = colony.headquarters.primary
        val row = staffing(colony, primary).firstOrNull { it.coordinate == primary }
        val failures = buildList {
            if (primary == null || row == null) add("Primary Headquarters is required.")
            else {
                if (!row.constructed) add("Primary Headquarters must be fully constructed.")
                if (!row.staffed) add("Primary Headquarters must be fully staffed (${row.requiredStaff.toInt()} required).")
            }
        }
        return HeadquartersDepartureGate(failures.isEmpty(), failures)
    }

    fun completeCommandHandover(state: GameState): GameState {
        val gate = departureGate(state)
        require(gate.ok) { gate.failures.joinToString(" ") }
        val colony = state.activeColony
        return state.withActiveColony(
            colony.copy(headquarters = colony.headquarters.copy(commandHandoverComplete = true)),
        )
    }

    private fun staffing(colony: ColonyState, primary: SectorCoordinate?): List<HeadquartersRow> {
        val raw = headquartersTiles(colony).map { tile ->
            val level = tile.development!!.level
            HeadquartersRow(
                coordinate = tile.coordinate,
                level = level,
                capacity = InfrastructureRules.headquartersCapacity(level),
                requiredStaff = InfrastructureRules.headquartersMinimumStaff(level),
                requiredPower = InfrastructureRules.headquartersPower(level),
                constructed = tile.development.constructionComplete,
                staffed = false,
                powered = false,
                primary = tile.coordinate == primary,
            )
        }
        val primaryRow = raw.firstOrNull { it.coordinate == primary }
        val ordered = buildList {
            primaryRow?.let(::add)
            addAll(raw.filterNot { it.coordinate == primary }.sortedWith(compareByDescending<HeadquartersRow> { it.level }.thenBy { it.coordinate.x }.thenBy { it.coordinate.y }))
        }
        var remaining = baseWorkforceAvailable(colony)
        val staffed = mutableSetOf<SectorCoordinate>()
        for (row in ordered) {
            if (!row.constructed || remaining < row.requiredStaff) continue
            staffed += row.coordinate
            remaining -= row.requiredStaff
        }
        return raw.map { it.copy(staffed = it.coordinate in staffed) }
    }

    private fun headquartersTiles(colony: ColonyState) = colony.world.tiles.filter {
        it.development?.kind == DevelopmentKind.HEADQUARTERS
    }

    private fun commandLoad(colony: ColonyState): Double = colony.world.tiles.sumOf { tile ->
        val dev = tile.development ?: return@sumOf 0.0
        if (dev.kind == DevelopmentKind.HEADQUARTERS || tile.resourceExhausted) return@sumOf 0.0
        InfrastructureRules.commandWeight(dev.kind, tile.deposit?.category) * dev.level
    }

    private fun headquartersBonus(rows: List<HeadquartersRow>): Double {
        val ordered = rows.sortedWith(compareByDescending<HeadquartersRow> { it.level }.thenBy { it.coordinate.x }.thenBy { it.coordinate.y })
        var result = 0.0
        ordered.forEachIndexed { index, row ->
            val diminishing = when (index) {
                0 -> 1.0
                1 -> .5
                2 -> .25
                else -> .125
            }
            result += diminishing * row.level * InfrastructureRules.HEADQUARTERS_BONUS_PER_LEVEL
        }
        return min(InfrastructureRules.HEADQUARTERS_BONUS_CAP, result)
    }

    private fun GameState.withActiveColony(updated: ColonyState): GameState =
        copy(colonies = colonies.map { if (it.id == updated.id) updated else it })

    companion object {
        private const val RECOVERY_DAYS = 10
    }
}

data class HeadquartersActionResult(val ok: Boolean, val state: GameState, val message: String)
data class HeadquartersDepartureGate(val ok: Boolean, val failures: List<String>)
