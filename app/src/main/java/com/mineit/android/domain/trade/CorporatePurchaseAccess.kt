package com.mineit.android.domain.trade

import com.mineit.android.domain.colony.ColonyNetworkSnapshot
import com.mineit.android.domain.model.GameState

/**
 * Canonical access rule for purchasing supplies from an already-docked Corporate Ship.
 *
 * A purchase only needs a working corporation computer/communications endpoint: either any
 * player ship docked at the active colony, or a constructed, staffed and powered Headquarters.
 * The Corporate Ship owns unloading of purchased supplies, so Spaceport Power is not a purchase
 * requirement. Selling/loading and passenger transfer retain their separate Spaceport gates.
 */
object CorporatePurchaseAccess {
    fun evaluate(state: GameState, network: ColonyNetworkSnapshot): CorporatePurchaseAccessStatus {
        val colonyId = state.activeColony.id
        val dockedPlayerShip = state.fleet.ships.any { it.dockedColonyId == colonyId }
        val operationalHeadquarters = network.headquarters.rows.any { row ->
            row.constructed && row.staffed && row.powered
        }
        val available = dockedPlayerShip || operationalHeadquarters
        val source = when {
            dockedPlayerShip -> CorporatePurchaseAccessSource.DOCKED_PLAYER_SHIP
            operationalHeadquarters -> CorporatePurchaseAccessSource.HEADQUARTERS
            else -> CorporatePurchaseAccessSource.NONE
        }
        val reason = when (source) {
            CorporatePurchaseAccessSource.DOCKED_PLAYER_SHIP -> "Corporate purchase link available through a docked player ship."
            CorporatePurchaseAccessSource.HEADQUARTERS -> "Corporate purchase link available through a powered and staffed Headquarters."
            CorporatePurchaseAccessSource.NONE -> "Corporate purchase link unavailable: dock a player ship or provide a powered and staffed Headquarters."
        }
        return CorporatePurchaseAccessStatus(
            available = available,
            source = source,
            reason = reason,
        )
    }
}

data class CorporatePurchaseAccessStatus(
    val available: Boolean,
    val source: CorporatePurchaseAccessSource,
    val reason: String,
)

enum class CorporatePurchaseAccessSource {
    DOCKED_PLAYER_SHIP,
    HEADQUARTERS,
    NONE,
}
