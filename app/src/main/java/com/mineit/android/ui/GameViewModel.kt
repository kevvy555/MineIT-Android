package com.mineit.android.ui

import androidx.lifecycle.ViewModel
import com.mineit.android.domain.DemoGame
import com.mineit.android.domain.GameSimulation
import com.mineit.android.domain.GameState
import com.mineit.android.domain.SectorCoordinate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class GameViewModel : ViewModel() {
    private val _state = MutableStateFlow(DemoGame.initialState())
    val state: StateFlow<GameState> = _state.asStateFlow()

    private val _selectedSector = MutableStateFlow<SectorCoordinate?>(null)
    val selectedSector: StateFlow<SectorCoordinate?> = _selectedSector.asStateFlow()

    fun advanceDay() {
        _state.update(GameSimulation::advanceDay)
    }

    fun selectSector(coordinate: SectorCoordinate) {
        _selectedSector.value = coordinate
    }
}
