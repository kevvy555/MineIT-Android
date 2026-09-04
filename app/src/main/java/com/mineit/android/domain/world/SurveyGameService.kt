package com.mineit.android.domain.world

import com.mineit.android.domain.model.GameState
import com.mineit.android.domain.technology.ScanningTechnology

/**
 * Canonical aggregate-level survey owner. It applies SurveyService rules to the active colony
 * without exposing root-state mutation to the UI.
 */
class SurveyGameService(
    private val surveyService: SurveyService = SurveyService(),
) {
    fun canSurvey(state: GameState, coordinate: SectorCoordinate): Boolean {
        val colony = state.activeColony
        val contract = colony.contract ?: return false
        return surveyService.surveyable(
            world = colony.world,
            contract = contract,
            coordinate = coordinate,
            scanningLevel = colony.technology.scanning,
        )
    }

    fun surveyDays(state: GameState, coordinate: SectorCoordinate): Int? {
        val colony = state.activeColony
        val contract = colony.contract ?: return null
        if (!surveyService.surveyable(colony.world, contract, coordinate, colony.technology.scanning)) return null
        val capability = ScanningTechnology.forLevel(colony.technology.scanning)
        return surveyService.days(
            contract = contract,
            coordinate = coordinate,
            resurvey = surveyService.isResurveyable(colony.world, coordinate, colony.technology.scanning),
            surveyFactor = capability.scanTimeFactor,
        )
    }

    fun enqueue(state: GameState, coordinate: SectorCoordinate): GameState {
        val colony = state.activeColony
        val contract = colony.contract ?: return state
        val capability = ScanningTechnology.forLevel(colony.technology.scanning)
        val nextWorld = surveyService.enqueue(
            world = colony.world,
            contract = contract,
            coordinate = coordinate,
            scanningLevel = colony.technology.scanning,
            slots = capability.surveySlots,
            surveyFactor = capability.scanTimeFactor,
        )
        return state.withActiveColony(colony.copy(world = nextWorld))
    }

    fun processSurveys(state: GameState): SurveyGameProcessResult {
        val colony = state.activeColony
        val contract = colony.contract ?: return SurveyGameProcessResult(state, emptyList())
        val capability = ScanningTechnology.forLevel(colony.technology.scanning)
        val result = surveyService.tick(
            world = colony.world,
            colonySeed = colony.seed,
            contract = contract,
            currentScanningLevel = colony.technology.scanning,
            slots = capability.surveySlots,
            surveyFactor = capability.scanTimeFactor,
        )
        return SurveyGameProcessResult(
            state = state.withActiveColony(colony.copy(world = result.world)),
            completed = result.completed,
        )
    }

    private fun GameState.withActiveColony(updated: com.mineit.android.domain.model.ColonyState): GameState {
        require(updated.id == activeColonyId) { "Updated colony must be the active colony." }
        return copy(colonies = colonies.map { if (it.id == updated.id) updated else it })
    }
}

data class SurveyGameProcessResult(
    val state: GameState,
    val completed: List<WorldTile>,
)
