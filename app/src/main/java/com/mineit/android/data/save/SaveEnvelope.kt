package com.mineit.android.data.save

import com.mineit.android.domain.model.GameState
import kotlinx.serialization.Serializable

object NativeSaveFormat {
    const val CURRENT_VERSION = 4
}

@Serializable
data class SaveEnvelope(
    val formatVersion: Int,
    val gameVersion: String,
    val universeContentVersion: String? = null,
    val universeSourceCommit: String? = null,
    val savedAtEpochMillis: Long,
    val state: GameState,
) {
    init {
        require(formatVersion >= 1) { "Native save formatVersion must be at least 1." }
        require(gameVersion.isNotBlank()) { "Native save gameVersion must not be blank." }
        require(savedAtEpochMillis >= 0) { "Native save timestamp must not be negative." }
    }
}
