package com.mineit.android.domain.colony

import com.mineit.android.domain.model.ColonyStatus
import com.mineit.android.domain.model.GameState

/** Current Basic Spaceport operational rules from the pinned web baseline. */
class SpaceportService {
    fun status(state: GameState, network: ColonyNetworkSnapshot): SpaceportStatus {
        val colony = state.activeColony
        val colonyOperational = colony.status !in setOf(ColonyStatus.DEAD, ColonyStatus.SITE_SELECTION)
        val powered = network.spaceportPowerFactor >= .999
        val operational = colonyOperational && powered
        val reason = when {
            !colonyOperational -> "Spaceport unavailable while the colony is not operational."
            !powered -> "Spaceport is unpowered. Trade, loading, transfers, engineering, market actions and normal departures are disabled."
            else -> "Spaceport online."
        }
        return SpaceportStatus(
            operational = operational,
            powerFactor = network.spaceportPowerFactor,
            berths = 2,
            serviceSlots = 2,
            cargoPerDay = 7,
            passengersPerDay = 25,
            arrivalsAllowed = true,
            emergencyDepartureAllowed = true,
            tradeAllowed = operational,
            loadingAllowed = operational,
            transfersAllowed = operational,
            engineeringAllowed = operational,
            shipMarketAllowed = operational,
            normalDepartureAllowed = operational,
            reason = reason,
        )
    }
}

data class SpaceportStatus(
    val operational: Boolean,
    val powerFactor: Double,
    val berths: Int,
    val serviceSlots: Int,
    val cargoPerDay: Int,
    val passengersPerDay: Int,
    val arrivalsAllowed: Boolean,
    val emergencyDepartureAllowed: Boolean,
    val tradeAllowed: Boolean,
    val loadingAllowed: Boolean,
    val transfersAllowed: Boolean,
    val engineeringAllowed: Boolean,
    val shipMarketAllowed: Boolean,
    val normalDepartureAllowed: Boolean,
    val reason: String,
)
