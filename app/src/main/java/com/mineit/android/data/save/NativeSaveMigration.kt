package com.mineit.android.data.save

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive

interface NativeSaveMigration {
    val fromVersion: Int
    val toVersion: Int
    fun migrate(input: JsonObject): JsonObject
}

object NativeSaveV1ToV2 : NativeSaveMigration {
    override val fromVersion = 1
    override val toVersion = 2
    override fun migrate(input: JsonObject) = bump(input, toVersion)
}

object NativeSaveV2ToV3 : NativeSaveMigration {
    override val fromVersion = 2
    override val toVersion = 3
    override fun migrate(input: JsonObject) = bump(input, toVersion)
}

object NativeSaveV3ToV4 : NativeSaveMigration {
    override val fromVersion = 3
    override val toVersion = 4
    override fun migrate(input: JsonObject) = bump(input, toVersion)
}

/** Phase 6 adds trade/event/log state and contract commercial totals with semantic defaults. */
object NativeSaveV4ToV5 : NativeSaveMigration {
    override val fromVersion = 4
    override val toVersion = 5
    override fun migrate(input: JsonObject) = bump(input, toVersion)
}

/**
 * N05 replaces the temporary per-colony foundingShipDocked flag with durable fleet ownership.
 * Existing native saves remain developed/ashore: their stock and residents are never moved back
 * aboard. A compatible docked command/Industry ship is synthesized only where the v5 flag said
 * one was physically present.
 */
object NativeSaveV5ToV6 : NativeSaveMigration {
    override val fromVersion = 5
    override val toVersion = 6

    override fun migrate(input: JsonObject): JsonObject {
        val state = input["state"] as? JsonObject ?: error("Native v5 save is missing state.")
        val colonies = state["colonies"] as? JsonArray ?: error("Native v5 save is missing colonies.")
        val ships = mutableListOf<JsonObject>()
        val migratedColonies = colonies.map { element ->
            val colony = element as? JsonObject ?: error("Native v5 colony must be an object.")
            val colonyId = colony["id"]?.jsonPrimitive?.content ?: error("Native v5 colony is missing id.")
            val population = colony["population"]?.jsonPrimitive?.doubleOrNull ?: 0.0
            val shipDocked = colony["foundingShipDocked"]?.jsonPrimitive?.booleanOrNull ?: false
            val shipId = if (shipDocked) "legacy-founding-$colonyId" else null

            if (shipId != null) {
                ships += JsonObject(
                    mapOf(
                        "id" to JsonPrimitive(shipId),
                        "name" to JsonPrimitive("Legacy Founding Ship"),
                        "dockedColonyId" to JsonPrimitive(colonyId),
                        "crew" to JsonPrimitive(10),
                        "industrySupport" to JsonPrimitive(50.0),
                        "commandCapable" to JsonPrimitive(true),
                    ),
                )
            }

            val next = colony.toMutableMap()
            next.remove("foundingShipDocked")
            if (shipId != null) next["foundingShipId"] = JsonPrimitive(shipId)
            next["shipResidentAssignments"] = JsonArray(emptyList())
            next["planetaryAccommodationResidents"] = JsonPrimitive(population)
            next["establishmentAcknowledged"] = JsonPrimitive(true)
            next["initialManifestProvisioned"] = JsonPrimitive(true)
            JsonObject(next)
        }

        val nextState = state.toMutableMap()
        nextState["colonies"] = JsonArray(migratedColonies)
        nextState["fleet"] = JsonObject(
            buildMap {
                put("ships", JsonArray(ships))
                ships.firstOrNull()?.get("id")?.let { put("selectedShipId", it) }
            },
        )
        return bump(JsonObject(input + ("state" to JsonObject(nextState))), toVersion)
    }
}

private fun bump(input: JsonObject, version: Int): JsonObject = JsonObject(input + ("formatVersion" to JsonPrimitive(version)))

class NativeSaveMigrationChain(
    private val currentVersion: Int = NativeSaveFormat.CURRENT_VERSION,
    migrations: List<NativeSaveMigration> = defaultMigrations(currentVersion),
) {
    private val migrationsBySource = migrations.associateBy { it.fromVersion }

    init {
        require(currentVersion >= 1) { "Current native save version must be at least 1." }
        require(migrationsBySource.size == migrations.size) { "Only one native save migration may start from each version." }
        migrations.forEach { require(it.toVersion == it.fromVersion + 1) { "Native save migrations must advance exactly one version at a time." } }
    }

    fun migrateToCurrent(input: JsonObject): JsonObject {
        var current = input
        var version = readVersion(current)
        require(version <= currentVersion) { "Native save version $version is newer than supported version $currentVersion." }
        while (version < currentVersion) {
            val migration = migrationsBySource[version] ?: error("No native save migration is registered from version $version.")
            current = migration.migrate(current)
            val migratedVersion = readVersion(current)
            require(migratedVersion == migration.toVersion) { "Native save migration ${migration.fromVersion}->${migration.toVersion} produced version $migratedVersion." }
            version = migratedVersion
        }
        return current
    }

    private fun readVersion(root: JsonObject): Int {
        val raw = root["formatVersion"]?.jsonPrimitive?.content ?: error("Native save is missing formatVersion.")
        return raw.toIntOrNull() ?: error("Native save formatVersion must be an integer.")
    }

    companion object {
        private fun defaultMigrations(currentVersion: Int): List<NativeSaveMigration> = buildList {
            if (currentVersion >= 2) add(NativeSaveV1ToV2)
            if (currentVersion >= 3) add(NativeSaveV2ToV3)
            if (currentVersion >= 4) add(NativeSaveV3ToV4)
            if (currentVersion >= 5) add(NativeSaveV4ToV5)
            if (currentVersion >= 6) add(NativeSaveV5ToV6)
        }
    }
}
