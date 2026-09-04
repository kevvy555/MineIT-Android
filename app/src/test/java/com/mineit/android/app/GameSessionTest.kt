package com.mineit.android.app

import com.mineit.android.app.persistence.PersistenceLoadResult
import com.mineit.android.app.persistence.PersistenceSaveResult
import com.mineit.android.app.persistence.SaveSource
import com.mineit.android.data.save.FileGameStatePersistence
import com.mineit.android.data.save.NativeSaveFormat
import com.mineit.android.testing.TestGameStates
import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GameSessionTest {
    @Test
    fun `commit owns immutable root transition and persistence diagnostics`() = withTempDirectory { directory ->
        val session = GameSession(
            initialState = TestGameStates.foundationState(cash = 500),
            persistence = repository(directory),
        )

        val result = runBlocking {
            session.commit("test-cash-change") { state ->
                state.copy(company = state.company.copy(cash = 475))
            }
        }

        assertEquals(475L, session.state.value.company.cash)
        assertEquals(1L, session.diagnostics.value.revision)
        assertEquals("test-cash-change", session.diagnostics.value.lastAction)
        assertEquals(PersistenceState.SAVED, session.diagnostics.value.persistenceState)
        assertTrue(result.persistence is PersistenceSaveResult.Success)
        assertEquals(NativeSaveFormat.CURRENT_VERSION, session.diagnostics.value.saveMetadata?.formatVersion)
    }

    @Test
    fun `fresh session restores the exact previously saved canonical state`() = withTempDirectory { directory ->
        val first = GameSession(
            initialState = TestGameStates.foundationState(cash = 500),
            persistence = repository(directory),
        )
        val expected = runBlocking {
            first.commit("population-update") { state ->
                val updatedColony = state.activeColony.copy(population = 144.0)
                state.copy(
                    colonies = state.colonies.map { colony ->
                        if (colony.id == updatedColony.id) updatedColony else colony
                    },
                )
            }.state
        }

        val restarted = GameSession(
            initialState = TestGameStates.foundationState(cash = 1),
            persistence = repository(directory),
        )
        val loadResult = runBlocking { restarted.restoreFromPersistence() }

        assertTrue(loadResult is PersistenceLoadResult.Loaded)
        assertEquals(expected, restarted.state.value)
        assertEquals(PersistenceState.LOADED, restarted.diagnostics.value.persistenceState)
        assertEquals(SaveSource.ACTIVE, restarted.diagnostics.value.loadedFrom)
        assertEquals(false, restarted.diagnostics.value.recoveredFromBackup)
        assertEquals("0.2.0-migration", restarted.diagnostics.value.saveMetadata?.gameVersion)
    }

    private fun repository(directory: File) = FileGameStatePersistence(
        directory = directory,
        gameVersion = "0.2.0-migration",
        universeContentVersion = "phase1-fixture",
        universeSourceCommit = "not-yet-pinned-runtime",
        nowEpochMillis = { 10_000L },
    )

    private fun withTempDirectory(block: (File) -> Unit) {
        val directory = Files.createTempDirectory("mineit-session-test").toFile()
        try {
            block(directory)
        } finally {
            directory.deleteRecursively()
        }
    }
}
