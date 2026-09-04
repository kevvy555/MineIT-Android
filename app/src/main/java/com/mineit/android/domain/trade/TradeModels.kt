package com.mineit.android.domain.trade

import kotlinx.serialization.Serializable

@Serializable
data class TradeState(
    val active: Boolean = false,
    val nextArrivalAbsoluteDay: Int = 181,
    val visits: Int = 0,
    val arrivedAtAbsoluteDay: Int? = null,
    val cargoUsed: Double = 0.0,
    val exportUsed: Double = 0.0,
    val passengersUsed: Int = 0,
    val visitCargoCapacity: Double? = null,
    val visitExportCapacity: Double? = null,
    val exportReputationAwarded: Boolean = false,
    val orbitalHolding: Boolean = false,
    val orbitalSinceAbsoluteDay: Int? = null,
)

data class TradeQuote(
    val quantity: Double,
    val value: Double,
)

data class ColonistTransferProjection(
    val supportedPopulationCapacity: Int,
    val housingPowerRemaining: Int,
    val passengerRemaining: Int,
    val foodSupportedAdditional: Int,
    val maxTransfer: Int,
    val maxSafeTransfer: Int,
    val unitCost: Double,
)

data class TradeActionResult(
    val state: com.mineit.android.domain.model.GameState,
    val ok: Boolean,
    val message: String,
    val quantity: Double = 0.0,
    val value: Double = 0.0,
)
