package com.mineit.android.app.persistence

import com.mineit.android.domain.model.GameState

data class PersistenceMetadata(
    val formatVersion: Int,
    val gameVersion: String,
    val universeContentVersion: String?,
    val universeSourceCommit: String?,
    val savedAtEpochMillis: Long,
)

enum class SaveSource {
    ACTIVE,
    BACKUP,
}

sealed interface PersistenceLoadResult {
    data object NotFound : PersistenceLoadResult

    data class Loaded(
        val state: GameState,
        val metadata: PersistenceMetadata,
        val source: SaveSource,
        val recoveredFromBackup: Boolean,
    ) : PersistenceLoadResult

    data class Failure(
        val activeError: String?,
        val backupError: String?,
    ) : PersistenceLoadResult
}

sealed interface PersistenceSaveResult {
    data class Success(val metadata: PersistenceMetadata) : PersistenceSaveResult
    data class Failure(val message: String) : PersistenceSaveResult
}

interface GameStatePersistence {
    suspend fun load(): PersistenceLoadResult
    suspend fun save(state: GameState): PersistenceSaveResult

    /**
     * Writes a deliberate fresh game and discards recovery history from the previous run.
     * Implementations must not leave a previous-game backup available for later recovery.
     */
    suspend fun reset(state: GameState): PersistenceSaveResult
}
