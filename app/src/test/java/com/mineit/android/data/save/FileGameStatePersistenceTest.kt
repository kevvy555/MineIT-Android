package com.mineit.android.data.save

import com.mineit.android.app.persistence.PersistenceLoadResult
import com.mineit.android.app.persistence.PersistenceSaveResult
import com.mineit.android.app.persistence.SaveSource
import com.mineit.android.domain.model.GameDate
import com.mineit.android.testing.TestGameStates
import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FileGameStatePersistenceTest {
    @Test
    fun `native save round trips canonical state and metadata`() = withTempDirectory { directory ->
        val repository = repository(directory)
        val state = TestGameStates.foundationState()

        val saved = runBlocking { repository.save(state) }
        val loaded = runBlocking { repository.load() }

        assertTrue(saved is PersistenceSaveResult.Success)
        loaded as PersistenceLoadResult.Loaded
        assertEquals(state, loaded.state)
        assertEquals(SaveSource.ACTIVE, loaded.source)
        assertEquals(false, loaded.recoveredFromBackup)
        assertEquals(1, loaded.metadata.formatVersion)
        assertEquals("0.2.0-migration", loaded.metadata.gameVersion)
        assertEquals("fixture-universe", loaded.metadata.universeContentVersion)
        assertEquals("universe-commit", loaded.metadata.universeSourceCommit)
        assertEquals(1_000L, loaded.metadata.savedAtEpochMillis)
        assertEquals(425.0, loaded.state.activeColony.inventory.amountFor(com.mineit.android.domain.model.ResourceId("fungal")), 0.0001)
    }

    @Test
    fun `second save preserves the previous valid state as backup`() = withTempDirectory { directory ->
        var now = 1_000L
        val repository = repository(directory) { now }
        val first = TestGameStates.foundationState(cash = 100)
        val second = TestGameStates.foundationState(cash = 200, date = GameDate(1, 2))

        runBlocking { repository.save(first) }
        now = 2_000L
        runBlocking { repository.save(second) }

        File(directory, FileGameStatePersistence.ACTIVE_FILE_NAME).writeText("{corrupt")
        val recovered = runBlocking { repository.load() } as PersistenceLoadResult.Loaded

        assertEquals(SaveSource.BACKUP, recovered.source)
        assertTrue(recovered.recoveredFromBackup)
        assertEquals(first, recovered.state)
        assertEquals(1_000L, recovered.metadata.savedAtEpochMillis)
    }

    @Test
    fun `corrupt active save never replaces an existing healthy backup`() = withTempDirectory { directory ->
        val repository = repository(directory)
        val first = TestGameStates.foundationState(cash = 100)
        val second = TestGameStates.foundationState(cash = 200)
        val third = TestGameStates.foundationState(cash = 300)

        runBlocking { repository.save(first) }
        runBlocking { repository.save(second) }
        File(directory, FileGameStatePersistence.ACTIVE_FILE_NAME).writeText("broken-active")
        runBlocking { repository.save(third) }
        File(directory, FileGameStatePersistence.ACTIVE_FILE_NAME).writeText("broken-again")

        val recovered = runBlocking { repository.load() } as PersistenceLoadResult.Loaded
        assertEquals(first, recovered.state)
    }

    @Test
    fun `invalid active save with no healthy backup produces explicit failure`() = withTempDirectory { directory ->
        val repository = repository(directory)
        runBlocking { repository.save(TestGameStates.foundationState()) }
        File(directory, FileGameStatePersistence.ACTIVE_FILE_NAME).writeText("not-json")

        val result = runBlocking { repository.load() }

        assertTrue(result is PersistenceLoadResult.Failure)
        result as PersistenceLoadResult.Failure
        assertTrue(result.activeError?.contains(FileGameStatePersistence.ACTIVE_FILE_NAME) == true)
    }

    private fun repository(
        directory: File,
        now: () -> Long = { 1_000L },
    ) = FileGameStatePersistence(
        directory = directory,
        gameVersion = "0.2.0-migration",
        universeContentVersion = "fixture-universe",
        universeSourceCommit = "universe-commit",
        nowEpochMillis = now,
    )

    private fun withTempDirectory(block: (File) -> Unit) {
        val directory = Files.createTempDirectory("mineit-save-test").toFile()
        try {
            block(directory)
        } finally {
            directory.deleteRecursively()
        }
    }
}
