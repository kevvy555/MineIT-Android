package com.mineit.android.domain.world

import com.mineit.android.domain.contracts.ContractState
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max

/** Immutable native port of the current MineIT survey queue/timing rules. */
class SurveyService(
    private val discovery: WorldDiscovery = WorldDiscovery(),
) {
    fun isResurveyable(world: WorldState, coordinate: SectorCoordinate, scanningLevel: Int): Boolean {
        if (isShipTile(coordinate)) return false
        val tile = world.tileAt(coordinate) ?: return false
        return tile.revealed && tile.lastScannedAtLevel > 0 && tile.lastScannedAtLevel < max(1, scanningLevel)
    }

    fun baseDays(
        contract: ContractState,
        coordinate: SectorCoordinate,
        surveyFactor: Double = 1.0,
        commandEfficiency: Double = 1.0,
    ): Int {
        val raw = (8 + hypot(coordinate.x.toDouble(), coordinate.y.toDouble()) * .22) *
            contract.scanMultiplier * surveyFactor / max(.5, commandEfficiency)
        return max(2, jsRound(raw))
    }

    fun days(
        contract: ContractState,
        coordinate: SectorCoordinate,
        resurvey: Boolean,
        surveyFactor: Double = 1.0,
        commandEfficiency: Double = 1.0,
    ): Int {
        val base = baseDays(contract, coordinate, surveyFactor, commandEfficiency)
        return if (resurvey) max(1, jsRound(base * RESURVEY_TIME_FACTOR)) else base
    }

    fun enqueue(
        world: WorldState,
        contract: ContractState,
        coordinate: SectorCoordinate,
        scanningLevel: Int,
        slots: Int = 1,
        surveyFactor: Double = 1.0,
        commandEfficiency: Double = 1.0,
    ): WorldState {
        if (!surveyable(world, contract, coordinate, scanningLevel)) return world
        return fill(
            world.copy(surveyQueue = world.surveyQueue + coordinate),
            contract,
            scanningLevel,
            slots,
            surveyFactor,
            commandEfficiency,
        )
    }

    fun fill(
        world: WorldState,
        contract: ContractState,
        scanningLevel: Int,
        slots: Int = 1,
        surveyFactor: Double = 1.0,
        commandEfficiency: Double = 1.0,
    ): WorldState {
        if (contract.ended) return world
        val maxSlots = slots.coerceIn(1, 5)
        val active = world.activeSurveys.toMutableList()
        val queue = world.surveyQueue.toMutableList()
        var working = world

        while (active.size < maxSlots && queue.isNotEmpty()) {
            val coordinate = queue.removeAt(0)
            working = working.copy(activeSurveys = active.toList(), surveyQueue = queue.toList())
            if (!surveyable(working, contract, coordinate, scanningLevel)) continue
            val resurvey = isResurveyable(working, coordinate, scanningLevel)
            val total = days(contract, coordinate, resurvey, surveyFactor, commandEfficiency)
            active += SurveyTask(
                coordinate = coordinate,
                totalDays = total,
                daysRemaining = total.toDouble(),
                scanningLevel = max(1, scanningLevel),
                resurvey = resurvey,
            )
        }

        return world.copy(activeSurveys = active, surveyQueue = queue)
    }

    fun tick(
        world: WorldState,
        colonySeed: Long,
        contract: ContractState,
        currentScanningLevel: Int,
        slots: Int = 1,
        surveyFactor: Double = 1.0,
        commandEfficiency: Double = 1.0,
        headquartersContinuityFactor: Double = 1.0,
    ): SurveyTickResult {
        if (contract.ended) return SurveyTickResult(world, emptyList())
        var working = fill(world, contract, currentScanningLevel, slots, surveyFactor, commandEfficiency)
        val continuity = headquartersContinuityFactor.coerceIn(0.0, 1.0)
        val decremented = working.activeSurveys.map { it.copy(daysRemaining = it.daysRemaining - continuity) }
        var tiles = working.tiles
        val completed = mutableListOf<WorldTile>()

        for (scan in decremented.filter { it.daysRemaining <= 0 }) {
            val before = tiles.firstOrNull { it.coordinate == scan.coordinate } ?: continue
            val previousResource = before.deposit?.resourceId
            val revealed = discovery.reveal(colonySeed, contract, before, scan.scanningLevel)
            tiles = tiles.map { if (it.coordinate == scan.coordinate) revealed else it }
            val foundNewResource = revealed.deposit?.resourceId != null && revealed.deposit.resourceId != previousResource
            if (!scan.resurvey || foundNewResource) completed += revealed
        }

        working = working.copy(
            tiles = tiles,
            activeSurveys = decremented.filter { it.daysRemaining > 0 },
        )
        working = fill(working, contract, currentScanningLevel, slots, surveyFactor, commandEfficiency)
        return SurveyTickResult(working, completed)
    }

    fun surveyable(
        world: WorldState,
        contract: ContractState,
        coordinate: SectorCoordinate,
        scanningLevel: Int,
    ): Boolean {
        if (contract.ended || isShipTile(coordinate)) return false
        if (world.activeSurveys.any { it.coordinate == coordinate }) return false
        if (world.surveyQueue.contains(coordinate)) return false
        val tile = world.tileAt(coordinate) ?: return false
        return !tile.revealed || isResurveyable(world, coordinate, scanningLevel)
    }

    private fun isShipTile(coordinate: SectorCoordinate): Boolean = coordinate.x == 0 && coordinate.y == 0

    private fun jsRound(value: Double): Int = floor(value + .5).toInt()

    companion object {
        private const val RESURVEY_TIME_FACTOR = .5
    }
}

data class SurveyTickResult(
    val world: WorldState,
    val completed: List<WorldTile>,
)
