package com.mineit.android.data.save

import com.mineit.android.app.persistence.GameStatePersistence
import com.mineit.android.app.persistence.PersistenceLoadResult
import com.mineit.android.app.persistence.PersistenceMetadata
import com.mineit.android.app.persistence.PersistenceSaveResult
import com.mineit.android.app.persistence.SaveSource
import com.mineit.android.domain.model.GameState
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FileGameStatePersistence(
    private val directory: File,
    private val gameVersion: String,
    private val universeContentVersion: String? = null,
    private val universeSourceCommit: String? = null,
    private val nowEpochMillis: () -> Long = System::currentTimeMillis,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val codec: SaveCodec = SaveCodec(),
) : GameStatePersistence {
    init {
        require(gameVersion.isNotBlank()) { "Game version must not be blank." }
    }

    private val activeFile: File get() = File(directory, ACTIVE_FILE_NAME)
    private val backupFile: File get() = File(directory, BACKUP_FILE_NAME)
    private val saveTempFile: File get() = File(directory, SAVE_TEMP_FILE_NAME)
    private val backupTempFile: File get() = File(directory, BACKUP_TEMP_FILE_NAME)

    override suspend fun load(): PersistenceLoadResult = withContext(dispatcher) {
        if (!activeFile.exists() && !backupFile.exists()) {
            return@withContext PersistenceLoadResult.NotFound
        }

        val activeAttempt = decodeFile(activeFile)
        activeAttempt.envelope?.let { envelope ->
            return@withContext envelope.toLoadResult(
                source = SaveSource.ACTIVE,
                recoveredFromBackup = false,
            )
        }

        val backupAttempt = decodeFile(backupFile)
        backupAttempt.envelope?.let { envelope ->
            return@withContext envelope.toLoadResult(
                source = SaveSource.BACKUP,
                recoveredFromBackup = true,
            )
        }

        PersistenceLoadResult.Failure(
            activeError = activeAttempt.error,
            backupError = backupAttempt.error,
        )
    }

    override suspend fun save(state: GameState): PersistenceSaveResult = persist(
        state = state,
        preservePreviousAsBackup = true,
    )

    override suspend fun reset(state: GameState): PersistenceSaveResult = persist(
        state = state,
        preservePreviousAsBackup = false,
    )

    private suspend fun persist(
        state: GameState,
        preservePreviousAsBackup: Boolean,
    ): PersistenceSaveResult = withContext(dispatcher) {
        try {
            ensureDirectory()
            cleanTempFiles()

            val envelope = SaveEnvelope(
                formatVersion = NativeSaveFormat.CURRENT_VERSION,
                gameVersion = gameVersion,
                universeContentVersion = universeContentVersion,
                universeSourceCommit = universeSourceCommit,
                savedAtEpochMillis = nowEpochMillis(),
                state = state,
            )
            val raw = codec.encode(envelope)

            writeSynced(saveTempFile, raw)
            codec.decode(saveTempFile.readText(StandardCharsets.UTF_8))

            if (preservePreviousAsBackup && activeFile.exists()) {
                val currentActiveRaw = activeFile.readText(StandardCharsets.UTF_8)
                val currentActiveIsValid = runCatching { codec.decode(currentActiveRaw) }.isSuccess
                if (currentActiveIsValid) {
                    writeSynced(backupTempFile, currentActiveRaw)
                    codec.decode(backupTempFile.readText(StandardCharsets.UTF_8))
                    moveReplace(backupTempFile, backupFile)
                }
            } else if (!preservePreviousAsBackup) {
                require(!backupFile.exists() || backupFile.delete()) {
                    "Unable to remove previous-game native save backup."
                }
            }

            moveReplace(saveTempFile, activeFile)
            PersistenceSaveResult.Success(envelope.metadata())
        } catch (error: Exception) {
            cleanTempFiles()
            PersistenceSaveResult.Failure(safeError(error))
        }
    }

    private fun ensureDirectory() {
        if (directory.exists()) {
            require(directory.isDirectory) { "Native save path is not a directory." }
            return
        }
        require(directory.mkdirs()) { "Unable to create native save directory." }
    }

    private fun decodeFile(file: File): DecodeAttempt {
        if (!file.exists()) return DecodeAttempt(error = "${file.name} not found.")
        return try {
            DecodeAttempt(envelope = codec.decode(file.readText(StandardCharsets.UTF_8)))
        } catch (error: Exception) {
            DecodeAttempt(error = "${file.name}: ${safeError(error)}")
        }
    }

    private fun writeSynced(file: File, content: String) {
        val bytes = content.toByteArray(StandardCharsets.UTF_8)
        FileOutputStream(file, false).use { output ->
            output.write(bytes)
            output.flush()
            output.fd.sync()
        }
    }

    private fun moveReplace(source: File, target: File) {
        try {
            Files.move(
                source.toPath(),
                target.toPath(),
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING,
            )
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(
                source.toPath(),
                target.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
            )
        }
    }

    private fun cleanTempFiles() {
        saveTempFile.delete()
        backupTempFile.delete()
    }

    private fun SaveEnvelope.toLoadResult(
        source: SaveSource,
        recoveredFromBackup: Boolean,
    ) = PersistenceLoadResult.Loaded(
        state = state,
        metadata = metadata(),
        source = source,
        recoveredFromBackup = recoveredFromBackup,
    )

    private fun SaveEnvelope.metadata() = PersistenceMetadata(
        formatVersion = formatVersion,
        gameVersion = gameVersion,
        universeContentVersion = universeContentVersion,
        universeSourceCommit = universeSourceCommit,
        savedAtEpochMillis = savedAtEpochMillis,
    )

    private fun safeError(error: Throwable): String {
        val type = error::class.simpleName ?: "SaveError"
        val message = error.message?.take(240)?.trim().orEmpty()
        return if (message.isBlank()) type else "$type: $message"
    }

    private data class DecodeAttempt(
        val envelope: SaveEnvelope? = null,
        val error: String? = null,
    )

    companion object {
        const val ACTIVE_FILE_NAME = "mineit-save.json"
        const val BACKUP_FILE_NAME = "mineit-save.previous.json"
        private const val SAVE_TEMP_FILE_NAME = "mineit-save.tmp"
        private const val BACKUP_TEMP_FILE_NAME = "mineit-save.previous.tmp"
    }
}
