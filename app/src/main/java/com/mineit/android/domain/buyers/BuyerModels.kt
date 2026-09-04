package com.mineit.android.domain.buyers

import com.mineit.android.domain.model.ColonyId
import com.mineit.android.domain.model.ResourceId
import com.mineit.android.domain.resources.QualityBand
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BuyerOffer(
    val id: String,
    val buyerId: String,
    val buyerName: String,
    val companyName: String,
    val resourceId: ResourceId,
    val minimumQuality: QualityBand,
    val quantity: Double,
    val unitRate: Double,
    val intervalDays: Int,
    val minimumReputation: Double,
) {
    val fullValue: Double get() = quantity * unitRate
}

@Serializable
enum class BuyerContractStatus {
    @SerialName("active") ACTIVE,
    @SerialName("cancelled") CANCELLED,
    @SerialName("terminated") TERMINATED,
}

@Serializable
enum class BuyerShipStatus {
    @SerialName("idle") IDLE,
    @SerialName("orbital-holding") ORBITAL_HOLDING,
    @SerialName("docked") DOCKED,
}

@Serializable
data class BuyerShipState(
    val status: BuyerShipStatus = BuyerShipStatus.IDLE,
    val dueAbsoluteDay: Int? = null,
    val attemptIndex: Int = 0,
    val nextEventAbsoluteDay: Int? = null,
    val eventPending: Boolean = false,
)

@Serializable
data class BuyerContract(
    val id: String,
    val offerId: String,
    val buyerId: String,
    val colonyId: ColonyId,
    val resourceId: ResourceId,
    val minimumQuality: QualityBand,
    val quantity: Double,
    val unitRate: Double,
    val intervalDays: Int,
    val nextDueAbsoluteDay: Int,
    val status: BuyerContractStatus = BuyerContractStatus.ACTIVE,
    val ship: BuyerShipState = BuyerShipState(),
)

@Serializable
data class BuyerRelationship(
    val buyerId: String,
    val happiness: Int = 75,
    val consecutiveRed: Int = 0,
    val missedShipments: Int = 0,
    val fulfilledShipments: Int = 0,
    val lifetimeRevenue: Double = 0.0,
    val cooldownUntilAbsoluteDay: Int? = null,
)

@Serializable
data class BuyerMarketState(
    val offers: List<BuyerOffer> = emptyList(),
    val contracts: List<BuyerContract> = emptyList(),
    val relationships: List<BuyerRelationship> = emptyList(),
)

data class BuyerCollectionProjection(
    val contract: BuyerContract,
    val qualifyingStock: Double,
    val transferableQuantity: Double,
    val completionRatio: Double,
    val daysLate: Int,
    val happinessChange: Int?,
    val canTransfer: Boolean,
)

data class BuyerActionResult(
    val state: com.mineit.android.domain.model.GameState,
    val ok: Boolean,
    val message: String,
    val revenue: Double = 0.0,
    val terminated: Boolean = false,
)
