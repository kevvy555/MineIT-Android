package com.mineit.android.domain.colony

import com.mineit.android.domain.world.SectorCoordinate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class HeadquartersContinuityPhase {
    @SerialName("online") ONLINE,
    @SerialName("outage") OUTAGE,
    @SerialName("recovery") RECOVERY,
}

@Serializable
data class HeadquartersOutageState(
    val phase: HeadquartersContinuityPhase = HeadquartersContinuityPhase.ONLINE,
    val penalty: Double = 0.0,
    val offlineDays: Int = 0,
    val outageStartedAbsoluteDay: Int? = null,
    val outageStartPenalty: Double = 0.0,
    val recoveryStartedAbsoluteDay: Int? = null,
    val recoveryInitialPenalty: Double = 0.0,
    val recoveryDaysElapsed: Int = 0,
    val recoveryDaysRemaining: Int = 0,
    val lastOutageDays: Int = 0,
)

@Serializable
data class HeadquartersIdentityState(
    val primary: SectorCoordinate? = null,
    val primaryEverAssigned: Boolean = false,
    val commandHandoverComplete: Boolean = false,
    val outage: HeadquartersOutageState = HeadquartersOutageState(),
)

enum class CommandSourceType { HEADQUARTERS, SHIP }

data class HeadquartersRow(
    val coordinate: SectorCoordinate,
    val level: Int,
    val capacity: Double,
    val requiredStaff: Double,
    val requiredPower: Double,
    val constructed: Boolean,
    val staffed: Boolean,
    val powered: Boolean,
    val primary: Boolean,
)

data class HeadquartersNetwork(
    val rows: List<HeadquartersRow>,
    val sourceType: CommandSourceType?,
    val sourceCoordinate: SectorCoordinate?,
    val primaryOperational: Boolean,
    val reservedStaff: Double,
    val capacity: Double,
    val load: Double,
    val overloadPenalty: Double,
    val bonus: Double,
    val efficiency: Double,
)

data class HeadquartersContinuity(
    val phase: HeadquartersContinuityPhase,
    val established: Boolean,
    val primaryOperational: Boolean,
    val networkAvailable: Boolean,
    val penalty: Double,
    val efficiencyFactor: Double,
    val effectiveCommandEfficiency: Double,
    val offlineDays: Int,
    val recoveryDaysRemaining: Int,
    val downTools: Boolean,
    val reason: String,
    val network: HeadquartersNetwork,
    val persisted: HeadquartersOutageState,
)
