package com.mineit.android.domain.colony

import com.mineit.android.domain.config.MineItConfig
import com.mineit.android.domain.model.ColonyStatus
import com.mineit.android.domain.model.GameState
import com.mineit.android.domain.ships.PlayerFleetService
import com.mineit.android.domain.world.ExtractionAccidentOutcome
import com.mineit.android.domain.world.ExtractionAccidentRecord
import com.mineit.android.domain.world.SectorCoordinate
import kotlin.math.max
import kotlin.math.min

/**
 * Advances industrial extraction overdrive once per completed simulation day.
 *
 * Production/workforce multipliers are already consumed by the network and production rules. This
 * coordinator owns only exposure/recovery, source-compatible accident checks, shutdown recovery,
 * and accident fatalities. Existing shutdowns are snapshotted from the beginning of the day so a
 * newly-created three-day shutdown is never immediately shortened to two days.
 */
class ExtractionOverdriveDayService(
    private val fleetService: PlayerFleetService = PlayerFleetService(),
    private val networkService: ColonyNetworkService = ColonyNetworkService(fleetService),
    private val random: (() -> Double)? = null,
) {
    fun advanceDay(before: GameState, afterSimulation: GameState): ExtractionOverdriveDayResult {
        val beforeColony = before.activeColony
        if (
            beforeColony.status == ColonyStatus.SITE_SELECTION ||
            beforeColony.status == ColonyStatus.DEAD ||
            beforeColony.contract?.ended == true
        ) {
            return ExtractionOverdriveDayResult(afterSimulation)
        }

        val network = networkService.calculate(before)
        val existingShutdowns = beforeColony.world.tiles
            .filter(ExtractionOverdriveRules::isShutdown)
            .map { it.coordinate }
            .toSet()
        val producingOverdriveSites = network.activeSites
            .filter(ExtractionOverdriveRules::supports)
            .filter { SiteProductionRules.rate(beforeColony, it, network) > .0001 }
            .map { it.coordinate }

        var working = afterSimulation
        var accidentDeaths = 0.0
        var colonyDied = false
        val accidents = mutableListOf<ExtractionOverdriveAccident>()

        if (existingShutdowns.isNotEmpty()) {
            working = updateActiveColonyTiles(working) { tile ->
                if (tile.coordinate in existingShutdowns) ExtractionOverdriveRules.advanceShutdownDay(tile) else tile
            }
        }

        for (coordinate in producingOverdriveSites) {
            val tile = working.activeColony.world.tileAt(coordinate) ?: continue
            if (tile.resourceExhausted || tile.deposit?.renewableWiped == true) continue
            val advance = ExtractionOverdriveRules.advanceRisk(before, tile, random)
            var nextTile = advance.tile
            var record = advance.accident
            if (record != null) {
                var appliedDeaths = 0
                if (record.outcome == ExtractionAccidentOutcome.FATALITIES && record.deaths > 0) {
                    val availablePlanetary = fleetService.planetaryResidentCount(working, working.activeColonyId)
                    appliedDeaths = min(record.deaths.toDouble(), availablePlanetary).toInt()
                    if (appliedDeaths > 0) {
                        working = applyPlanetaryDeaths(working, appliedDeaths.toDouble())
                        accidentDeaths += appliedDeaths
                    }
                }
                if (appliedDeaths != record.deaths) {
                    record = record.copy(deaths = appliedDeaths)
                    nextTile = nextTile.copy(
                        development = requireNotNull(nextTile.development).copy(lastAccident = record),
                    )
                }
                accidents += ExtractionOverdriveAccident(coordinate, record)
            }
            working = replaceTile(working, nextTile)
        }

        if (working.activeColony.population <= 0.0 && working.activeColony.status != ColonyStatus.DEAD) {
            val colony = working.activeColony.copy(
                population = 0.0,
                planetaryAccommodationResidents = 0.0,
                status = ColonyStatus.DEAD,
                contract = working.activeColony.contract?.copy(ended = true),
                emergencyMode = false,
            )
            working = working.copy(
                company = working.company.copy(
                    reputation = max(0.0, working.company.reputation - MineItConfig.COLONY_DEATH_REPUTATION_PENALTY),
                ),
                colonies = working.colonies.map { if (it.id == colony.id) colony else it },
            )
            colonyDied = true
        }

        return ExtractionOverdriveDayResult(
            state = working,
            accidents = accidents,
            deaths = accidentDeaths,
            planetaryResidents = fleetService.planetaryResidentCount(working, working.activeColonyId),
            colonyDied = colonyDied,
        )
    }

    private fun applyPlanetaryDeaths(state: GameState, deaths: Double): GameState {
        val colony = state.activeColony
        val nextPopulation = max(0.0, colony.population - deaths)
        val shipResidents = colony.shipResidentAssignments.sumOf { it.residents }
        val nextPlanetary = min(
            colony.planetaryAccommodationResidents,
            max(0.0, nextPopulation - shipResidents),
        )
        val nextColony = colony.copy(
            population = nextPopulation,
            planetaryAccommodationResidents = nextPlanetary,
        )
        return state.copy(colonies = state.colonies.map { if (it.id == colony.id) nextColony else it })
    }

    private fun replaceTile(state: GameState, tile: com.mineit.android.domain.world.WorldTile): GameState =
        updateActiveColonyTiles(state) { current -> if (current.coordinate == tile.coordinate) tile else current }

    private fun updateActiveColonyTiles(
        state: GameState,
        transform: (com.mineit.android.domain.world.WorldTile) -> com.mineit.android.domain.world.WorldTile,
    ): GameState {
        val colony = state.activeColony
        val next = colony.copy(world = colony.world.copy(tiles = colony.world.tiles.map(transform)))
        return state.copy(colonies = state.colonies.map { if (it.id == colony.id) next else it })
    }
}

data class ExtractionOverdriveAccident(
    val coordinate: SectorCoordinate,
    val record: ExtractionAccidentRecord,
)

data class ExtractionOverdriveDayResult(
    val state: GameState,
    val accidents: List<ExtractionOverdriveAccident> = emptyList(),
    val deaths: Double = 0.0,
    val planetaryResidents: Double = 0.0,
    val colonyDied: Boolean = false,
)
