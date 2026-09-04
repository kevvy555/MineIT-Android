package com.mineit.android.data.save

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class NativeSaveMigrationChainTest {
    @Test
    fun `test migration can advance an older envelope one explicit version`() {
        val migration = object : NativeSaveMigration {
            override val fromVersion = 0
            override val toVersion = 1

            override fun migrate(input: JsonObject): JsonObject = JsonObject(
                input.toMutableMap().apply {
                    put("formatVersion", JsonPrimitive(1))
                },
            )
        }
        val chain = NativeSaveMigrationChain(
            currentVersion = 1,
            migrations = listOf(migration),
        )

        val migrated = chain.migrateToCurrent(
            JsonObject(mapOf("formatVersion" to JsonPrimitive(0))),
        )

        assertEquals("1", migrated.getValue("formatVersion").toString())
    }

    @Test
    fun `future native save version is rejected`() {
        val chain = NativeSaveMigrationChain(currentVersion = 1)

        assertThrows(IllegalArgumentException::class.java) {
            chain.migrateToCurrent(JsonObject(mapOf("formatVersion" to JsonPrimitive(2))))
        }
    }

    @Test
    fun `missing migration is rejected rather than silently normalised`() {
        val chain = NativeSaveMigrationChain(currentVersion = 1)

        assertThrows(IllegalStateException::class.java) {
            chain.migrateToCurrent(JsonObject(mapOf("formatVersion" to JsonPrimitive(0))))
        }
    }
}
