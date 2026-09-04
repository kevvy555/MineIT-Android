package com.mineit.android.app

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SimulationClockTest {
    @Test
    fun `clock advances at configured cadence and pause stops future day requests`() = runTest {
        var advances = 0
        val clock = SimulationClock(
            scope = this,
            advanceDay = { advances += 1 },
            baseDayMillis = 100,
        )

        clock.setSpeed(2)
        clock.start()
        runCurrent()

        advanceTimeBy(49)
        runCurrent()
        assertEquals(0, advances)

        advanceTimeBy(1)
        runCurrent()
        assertEquals(1, advances)

        clock.pause()
        advanceTimeBy(250)
        runCurrent()
        assertEquals(1, advances)

        clock.stop()
    }

    @Test
    fun `clock rejects unsupported speed`() {
        val clock = SimulationClock(
            scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Unconfined),
            advanceDay = {},
        )

        assertThrows(IllegalArgumentException::class.java) {
            clock.setSpeed(3)
        }
    }
}
