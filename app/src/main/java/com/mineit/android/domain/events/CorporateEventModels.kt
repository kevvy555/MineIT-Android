package com.mineit.android.domain.events

import com.mineit.android.domain.model.ColonyId
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class CorporateEventType {
    @SerialName("emergency-food") EMERGENCY_FOOD,
    @SerialName("buyer") BUYER,
    @SerialName("contract") CONTRACT,
    @SerialName("ship") SHIP,
}

@Serializable
data class CorporateEvent(
    val type: CorporateEventType,
    val colonyId: ColonyId,
    val colonyName: String,
    val sequence: Int = 0,
    val kind: String? = null,
    val contractId: String? = null,
    val shipId: String? = null,
    val shipName: String? = null,
    val amount: Double = 0.0,
    val recovered: Boolean = false,
    val attemptIndex: Int? = null,
    val dueAbsoluteDay: Int? = null,
)

@Serializable
data class CorporateEventQueueState(
    val nextSequence: Int = 1,
    val pending: List<CorporateEvent> = emptyList(),
)
