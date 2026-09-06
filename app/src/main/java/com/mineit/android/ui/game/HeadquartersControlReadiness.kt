package com.mineit.android.ui.game

import com.mineit.android.domain.colony.ColonyNetworkSnapshot
import com.mineit.android.domain.world.DevelopmentKind
import com.mineit.android.domain.world.WorldTile

/**
 * Prevents the HQ sheet from rendering during the brief state/network handoff after an HQ build.
 * GameSession publishes the new authoritative state before derived network flows are recalculated;
 * the previous snapshot therefore may not contain the just-built HQ row for one composition frame.
 */
object HeadquartersControlReadiness {
    fun isReady(tile: WorldTile?, network: ColonyNetworkSnapshot): Boolean {
        if (tile?.development?.kind != DevelopmentKind.HEADQUARTERS) return false
        return network.headquarters.rows.any { it.coordinate == tile.coordinate }
    }
}
