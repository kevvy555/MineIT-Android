package com.mineit.android.app

import com.mineit.android.app.persistence.GameStatePersistence
import com.mineit.android.app.persistence.PersistenceLoadResult
import com.mineit.android.app.persistence.PersistenceMetadata
import com.mineit.android.app.persistence.PersistenceSaveResult
import com.mineit.android.app.persistence.SaveSource
import com.mineit.android.domain.model.GameState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class GameSessionDiagnostics(
    val revision: Long = 0,
    val lastAction: String = "initial-state",
    val persistenceState: PersistenceState = PersistenceState.NOT_ATTEMPTED,
    val persistenceMessage: String? = null,
    val loadedFrom: SaveSource? = null,
    val recoveredFromBackup: Boolean = false,
    val saveMetadata: PersistenceMetadata? = null,
)

enum class PersistenceState {
    NOT_ATTEMPTED,
    SAVED,
    LOADED,
    NOT_FOUND,
    FAILED,
}

data class GameSessionCommitResult(
    val state: GameState,
    val persistence: PersistenceSaveResult,
)

class GameSession(
    initialState: GameState,
    private val persistence: GameStatePersistence,
) {
    private val mutex = Mutex()
    private val mutableState = MutableStateFlow(initialState)
    private val mutableDiagnostics = MutableStateFlow(GameSessionDiagnostics())

    val state: StateFlow<GameState> = mutableState.asStateFlow()
    val diagnostics: StateFlow<GameSessionDiagnostics> = mutableDiagnostics.asStateFlow()

    suspend fun commit(
        label: String,
        transition: (GameState) -> GameState,
    ): GameSessionCommitResult = mutex.withLock {
        require(label.isNotBlank()) { "GameSession commit label must not be blank." }

        val nextState = transition(mutableState.value)
        mutableState.value = nextState
        val nextRevision = mutableDiagnostics.value.revision + 1
        val saveResult = persistence.save(nextState)

        mutableDiagnostics.value = when (saveResult) {
            is PersistenceSaveResult.Success -> GameSessionDiagnostics(
                revision = nextRevision,
                lastAction = label,
                persistenceState = PersistenceState.SAVED,
                loadedFrom = mutableDiagnostics.value.loadedFrom,
                recoveredFromBackup = mutableDiagnostics.value.recoveredFromBackup,
                saveMetadata = saveResult.metadata,
            )
            is PersistenceSaveResult.Failure -> GameSessionDiagnostics(
                revision = nextRevision,
                lastAction = label,
                persistenceState = PersistenceState.FAILED,
                persistenceMessage = saveResult.message,
                loadedFrom = mutableDiagnostics.value.loadedFrom,
                recoveredFromBackup = mutableDiagnostics.value.recoveredFromBackup,
                saveMetadata = mutableDiagnostics.value.saveMetadata,
            )
        }

        GameSessionCommitResult(nextState, saveResult)
    }

    suspend fun persistCurrentState(): PersistenceSaveResult = mutex.withLock {
        val saveResult = persistence.save(mutableState.value)
        mutableDiagnostics.value = when (saveResult) {
            is PersistenceSaveResult.Success -> mutableDiagnostics.value.copy(
                persistenceState = PersistenceState.SAVED,
                persistenceMessage = null,
                saveMetadata = saveResult.metadata,
            )
            is PersistenceSaveResult.Failure -> mutableDiagnostics.value.copy(
                persistenceState = PersistenceState.FAILED,
                persistenceMessage = saveResult.message,
            )
        }
        saveResult
    }

    suspend fun restoreFromPersistence(): PersistenceLoadResult = mutex.withLock {
        when (val loadResult = persistence.load()) {
            PersistenceLoadResult.NotFound -> {
                mutableDiagnostics.value = mutableDiagnostics.value.copy(
                    lastAction = "restore",
                    persistenceState = PersistenceState.NOT_FOUND,
                    persistenceMessage = null,
                    loadedFrom = null,
                    recoveredFromBackup = false,
                )
                loadResult
            }
            is PersistenceLoadResult.Loaded -> {
                mutableState.value = loadResult.state
                mutableDiagnostics.value = GameSessionDiagnostics(
                    revision = mutableDiagnostics.value.revision + 1,
                    lastAction = "restore",
                    persistenceState = PersistenceState.LOADED,
                    loadedFrom = loadResult.source,
                    recoveredFromBackup = loadResult.recoveredFromBackup,
                    saveMetadata = loadResult.metadata,
                )
                loadResult
            }
            is PersistenceLoadResult.Failure -> {
                val message = listOfNotNull(loadResult.activeError, loadResult.backupError)
                    .joinToString("; ")
                    .ifBlank { "Unable to load native save." }
                mutableDiagnostics.value = mutableDiagnostics.value.copy(
                    lastAction = "restore",
                    persistenceState = PersistenceState.FAILED,
                    persistenceMessage = message,
                )
                loadResult
            }
        }
    }
}
