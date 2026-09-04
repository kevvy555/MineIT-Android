package com.mineit.android.app

import com.mineit.android.domain.config.MineItConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Application-layer scheduler for native game days.
 *
 * It owns cadence only. Daily gameplay remains in the pure domain simulation engine and can be
 * invoked identically by this clock, a manual debug action or a JVM test.
 */
class SimulationClock(
    private val scope: CoroutineScope,
    private val advanceDay: suspend () -> Unit,
    private val baseDayMillis: Long = MineItConfig.DAY_MS,
) {
    private val mutableSpeed = MutableStateFlow(0)
    val speed: StateFlow<Int> = mutableSpeed.asStateFlow()

    private var job: Job? = null

    init {
        require(baseDayMillis > 0) { "Simulation day duration must be positive." }
    }

    fun start() {
        if (job?.isActive == true) return
        job = scope.launch {
            while (isActive) {
                val currentSpeed = mutableSpeed.value
                if (currentSpeed <= 0) {
                    delay(PAUSED_POLL_MILLIS)
                    continue
                }
                delay((baseDayMillis / currentSpeed).coerceAtLeast(1L))
                if (mutableSpeed.value > 0) advanceDay()
            }
        }
    }

    fun setSpeed(multiplier: Int) {
        require(multiplier in SUPPORTED_SPEEDS) {
            "Simulation speed must be one of ${SUPPORTED_SPEEDS.joinToString()}."
        }
        mutableSpeed.value = multiplier
    }

    fun pause() = setSpeed(0)

    fun stop() {
        job?.cancel()
        job = null
        mutableSpeed.value = 0
    }

    companion object {
        val SUPPORTED_SPEEDS = setOf(0, 1, 2, 4)
        private const val PAUSED_POLL_MILLIS = 50L
    }
}
