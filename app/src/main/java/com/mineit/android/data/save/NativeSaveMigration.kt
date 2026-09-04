package com.mineit.android.data.save

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
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
        }
    }
}
