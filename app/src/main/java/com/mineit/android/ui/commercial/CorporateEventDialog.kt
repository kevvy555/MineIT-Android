package com.mineit.android.ui.commercial

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.mineit.android.domain.events.CorporateEvent
import com.mineit.android.domain.events.CorporateEventType
import com.mineit.android.ui.design.MineItPrimaryButton
import com.mineit.android.ui.design.MineItSecondaryButton

@Composable
fun CorporateEventDialog(
    event: CorporateEvent,
    onPrimary: () -> Unit,
    onSecondary: () -> Unit,
) {
    val attemptIndex = event.attemptIndex ?: 0
    val title = when (event.type) {
        CorporateEventType.EMERGENCY_FOOD -> "EMERGENCY FOOD"
        CorporateEventType.BUYER -> "BUYER COLLECTION"
        CorporateEventType.CONTRACT -> "CONTRACT DECISION"
        CorporateEventType.SHIP -> "CORPORATE SHIP ARRIVAL"
    }
    val message = when (event.type) {
        CorporateEventType.EMERGENCY_FOOD -> "${event.colonyName} requires emergency attention."
        CorporateEventType.SHIP -> "The Conglomerate corporate ship has arrived at ${event.colonyName}. Trade, cargo and passenger services are available while Spaceport services remain powered."
        CorporateEventType.BUYER -> "A contracted buyer ship is ready at ${event.colonyName}. Attempt ${attemptIndex + 1} of 4. Resolve the shipment or ask the buyer to wait."
        CorporateEventType.CONTRACT -> when (event.kind) {
            "complete" -> "${event.colonyName} has reached its Contract goals at the deadline. Complete the contract and enter holdover."
            "extension" -> "${event.colonyName} missed its Contract goals. Purchase the next one-year extension to continue."
            "corporation-failed" -> "The company cannot fund the mandatory contract extension."
            "failed" -> "The final contract deadline has passed without meeting the required goals."
            "renewal-ended" -> "The current contract term has ended. Renew it or retain the colony as a liability."
            else -> "${event.colonyName} requires a contract decision."
        }
    }
    val primary = when (event.type) {
        CorporateEventType.SHIP -> "OPEN TRADE"
        CorporateEventType.BUYER -> "TRANSFER"
        CorporateEventType.CONTRACT -> when (event.kind) {
            "complete" -> "COMPLETE"
            "extension" -> "BUY EXTENSION"
            "corporation-failed", "failed" -> "ACKNOWLEDGE"
            "renewal-ended" -> "END AS LIABILITY"
            else -> "CONTINUE"
        }
        CorporateEventType.EMERGENCY_FOOD -> "ACKNOWLEDGE"
    }
    val secondary = when (event.type) {
        CorporateEventType.BUYER -> if (attemptIndex >= 3) "MISS SHIPMENT" else "WAIT"
        CorporateEventType.CONTRACT -> when (event.kind) {
            "renewal-ended" -> "RENEW +5 YEARS"
            "complete" -> "LATER"
            else -> "DECLINE"
        }
        else -> null
    }

    AlertDialog(
        onDismissRequest = {},
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = { MineItPrimaryButton(primary, onPrimary) },
        dismissButton = { secondary?.let { MineItSecondaryButton(it, onSecondary) } },
    )
}
