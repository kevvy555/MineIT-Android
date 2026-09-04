package com.mineit.android.ui

import androidx.lifecycle.ViewModel
import com.mineit.android.domain.poc.PocGameState
import com.mineit.android.domain.poc.PocScenario
import com.mineit.android.domain.poc.PocSectorCoordinate
import com.mineit.android.domain.poc.PocSimulation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class GameViewModel : ViewModel() {
    private val _state = MutableStateFlow(PocScenario.initialState())
    val state: StateFlow<PocGameState> = _state.asStateFlow()

    private val _selectedSector = MutableStateFlow<PocSectorCoordinate?>(null)
    val selectedSector: StateFlow<PocSectorCoordinate?> = _selectedSector.asStateFlow()

    fun advanceDay() {
        _state.update(PocSimulation::advanceDay)
    }

    fun selectSector(coordinate: PocSectorCoordinate) {
        _selectedSector.value = coordinate
    }
}
