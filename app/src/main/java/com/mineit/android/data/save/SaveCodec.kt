package com.mineit.android.data.save

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject

class SaveCodec(
    private val migrations: NativeSaveMigrationChain = NativeSaveMigrationChain(),
) {
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = false
        prettyPrint = false
    }

    fun encode(envelope: SaveEnvelope): String {
        require(envelope.formatVersion == NativeSaveFormat.CURRENT_VERSION) {
            "Only current native save version ${NativeSaveFormat.CURRENT_VERSION} may be written."
        }
        return json.encodeToString(envelope)
    }

    fun decode(raw: String): SaveEnvelope {
        require(raw.isNotBlank()) { "Native save is empty." }
        val parsed = json.parseToJsonElement(raw)
        val root: JsonObject = parsed.jsonObject
        val migrated = migrations.migrateToCurrent(root)
        val envelope = json.decodeFromJsonElement<SaveEnvelope>(migrated)
        require(envelope.formatVersion == NativeSaveFormat.CURRENT_VERSION) {
            "Native save migration did not produce the current format version."
        }
        return envelope
    }
}
