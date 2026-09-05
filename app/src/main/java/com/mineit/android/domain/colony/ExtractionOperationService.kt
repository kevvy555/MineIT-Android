package com.mineit.android.domain.colony

import com.mineit.android.domain.model.ColonyStatus
import com.mineit.android.domain.model.GameState
import com.mineit.android.domain.world.DevelopmentKind
import com.mineit.android.domain.world.ExtractionOperatingMode
import com.mineit.android.domain.world.SectorCoordinate
import com.mineit.android.domain.world.Sustainability
import kotlin.math.roundToInt

/** Canonical player controls for an existing extraction site. */
class ExtractionOperationService {
    fun adjustHarvestIntensity(
        state: GameState,
        coordinate: SectorCoordinate,
        deltaPercent: Int,
    ): ExtractionOperationResult {
        val colony = state.activeColony
        commonBlock(state)?.let { return ExtractionOperationResult(false, state, it) }
        val tile = colony.world.tileAt(coordinate) ?: return ExtractionOperationResult(false, state, "Sector not found.")
        if (tile.development?.kind != DevelopmentKind.EXTRACT) return ExtractionOperationResult(false, state, "This sector is not an extraction site.")
        val deposit = tile.deposit ?: return ExtractionOperationResult(false, state, "This extraction site has no active resource.")
        if (deposit.sustainability != Sustainability.RENEWABLE || deposit.renewableWiped || tile.resourceExhausted) {
            return ExtractionOperationResult(false, state, "Renewable harvesting is unavailable.")
        }
        val before = (deposit.harvestIntensity.coerceIn(.25, 2.0) * 100.0).roundToInt()
        val after = (before + deltaPercent).coerceIn(25, 200)
        if (after == before) return ExtractionOperationResult(false, state, "Harvest intensity is already at ${after}%.")
        val nextTile = tile.copy(deposit = deposit.copy(harvestIntensity = after / 100.0))
        return ExtractionOperationResult(
            ok = true,
            state = replaceTile(state, nextTile),
            message = "${deposit.name} harvest changed from $before% to $after%.",
            beforePercent = before,
            afterPercent = after,
        )
    }

    fun setOperatingMode(
        state: GameState,
        coordinate: SectorCoordinate,
        mode: ExtractionOperatingMode,
    ): ExtractionOperationResult {
        commonBlock(state)?.let { return ExtractionOperationResult(false, state, it) }
        val tile = state.activeColony.world.tileAt(coordinate) ?: return ExtractionOperationResult(false, state, "Sector not found.")
        val result = ExtractionOverdriveRules.setMode(tile, mode)
        if (!result.ok) return ExtractionOperationResult(false, state, result.message)
        return ExtractionOperationResult(
            ok = true,
            state = replaceTile(state, result.tile),
            message = result.message,
            operatingMode = mode,
        )
    }

    private fun commonBlock(state: GameState): String? = when {
        state.activeColony.status == ColonyStatus.DEAD -> "This colony has been lost."
        state.activeColony.contract?.ended == true -> "The mining contract has ended."
        else -> null
    }

    private fun replaceTile(state: GameState, nextTile: com.mineit.android.domain.world.WorldTile): GameState {
        val colony = state.activeColony
        val nextColony = colony.copy(
            world = colony.world.copy(
                tiles = colony.world.tiles.map { if (it.coordinate == nextTile.coordinate) nextTile else it },
            ),
        )
        return state.copy(colonies = state.colonies.map { if (it.id == colony.id) nextColony else it })
    }
}

data class ExtractionOperationResult(
    val ok: Boolean,
    val state: GameState,
    val message: String,
    val beforePercent: Int? = null,
    val afterPercent: Int? = null,
    val operatingMode: ExtractionOperatingMode? = null,
)
