package com.mineit.android.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mineit.android.app.AppComposition
import com.mineit.android.app.persistence.PersistenceLoadResult
import com.mineit.android.app.persistence.PersistenceSaveResult
import com.mineit.android.domain.model.GameState
import com.mineit.android.domain.world.SectorCoordinate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GameViewModel(application: Application) : AndroidViewModel(application) {
    private val composition = AppComposition(application)
    private val session = composition.gameSession
    private val newGameFactory = composition.newGameFactory
    private val surveyGameService = composition.surveyGameService

    val state: StateFlow<GameState> = session.state

    private val _selectedSector = MutableStateFlow<SectorCoordinate?>(null)
    val selectedSector: StateFlow<SectorCoordinate?> = _selectedSector.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    init {
        viewModelScope.launch {
            when (val result = session.restoreFromPersistence()) {
                PersistenceLoadResult.NotFound -> Unit
                is PersistenceLoadResult.Loaded -> {
                    _statusMessage.value = if (result.recoveredFromBackup) {
                        "Recovered the native save from the previous-good backup."
                    } else {
                        "Loaded native migration save."
                    }
                }
                is PersistenceLoadResult.Failure -> {
                    _statusMessage.value = "Save restore failed; using the fresh Contract 01 validation state."
                }
            }
        }
    }

    fun selectLandingSite(index: Int) {
        viewModelScope.launch {
            val result = session.commit("select-landing-site") { current ->
                newGameFactory.settleLandingSite(current, index)
            }
            _selectedSector.value = null
            _statusMessage.value = if (result.persistence is PersistenceSaveResult.Success) {
                "Landing Site ${index + 1} selected. The 8×8 surface grid is ready for surveying."
            } else {
                "Landing site selected, but the native save could not be written."
            }
        }
    }

    fun selectSector(coordinate: SectorCoordinate) {
        _selectedSector.value = coordinate
        _statusMessage.value = null
    }

    fun surveyDays(coordinate: SectorCoordinate): Int? = surveyGameService.surveyDays(state.value, coordinate)

    fun surveySelectedSector() {
        val coordinate = _selectedSector.value ?: return
        val snapshot = state.value
        val days = surveyGameService.surveyDays(snapshot, coordinate) ?: return

        viewModelScope.launch {
            val result = session.commit("enqueue-survey") { current ->
                surveyGameService.enqueue(current, coordinate)
            }
            _statusMessage.value = if (result.persistence is PersistenceSaveResult.Success) {
                "Sector ${coordinate.x},${coordinate.y} queued for survey ($days days at current capability)."
            } else {
                "Survey queued, but the native save could not be written."
            }
        }
    }

    /**
     * Phase 2 validation control only: advances the canonical date and the real survey subsystem.
     * Phase 3 replaces this partial day action with the complete daily simulation pipeline.
     */
    fun advanceSurveyDay() {
        val snapshot = state.value
        val world = snapshot.activeColony.world
        if (world.activeSurveys.isEmpty() && world.surveyQueue.isEmpty()) return

        viewModelScope.launch {
            val result = session.commit("phase2-survey-day") { current ->
                val processed = surveyGameService.processSurveys(current)
                processed.state.copy(date = current.date.nextDay())
            }
            _statusMessage.value = if (result.persistence is PersistenceSaveResult.Success) {
                "Survey progress advanced one game day. Economy/survival simulation remains intentionally inactive until Phase 3."
            } else {
                "Survey progress advanced, but the native save could not be written."
            }
        }
    }
}
