package com.mineit.android.migration

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

internal data class ParityFixture(
    val name: String,
    val sourceRepository: String,
    val sourceCommit: String,
    val sourceGameVersion: String,
    val sourceSaveVersion: Int,
    val state: JsonObject,
    val expectedSummary: JsonObject,
)

internal object ParityFixtureLoader {
    private val json = Json { ignoreUnknownKeys = false }

    fun load(resourcePath: String): ParityFixture {
        val stream = requireNotNull(javaClass.classLoader?.getResourceAsStream(resourcePath)) {
            "Parity fixture not found: $resourcePath"
        }
        val root = stream.bufferedReader().use { reader ->
            json.parseToJsonElement(reader.readText()).jsonObject
        }
        val metadata = requireObject(root, "metadata")
        val source = requireObject(metadata, "source")
        val state = requireObject(root, "state")
        val expected = requireObject(root, "expectedSummary")

        return ParityFixture(
            name = requireString(metadata, "name"),
            sourceRepository = requireString(source, "repository"),
            sourceCommit = requireString(source, "commit"),
            sourceGameVersion = requireString(source, "gameVersion"),
            sourceSaveVersion = requireInt(source, "saveVersion"),
            state = state,
            expectedSummary = expected,
        )
    }

    private fun requireObject(parent: JsonObject, key: String): JsonObject =
        requireNotNull(parent[key]) { "Parity fixture is missing '$key'." }.jsonObject

    private fun requireString(parent: JsonObject, key: String): String =
        requireNotNull(parent[key]) { "Parity fixture is missing '$key'." }.jsonPrimitive.content

    private fun requireInt(parent: JsonObject, key: String): Int =
        requireString(parent, key).toIntOrNull()
            ?: error("Parity fixture '$key' must be an integer.")
}
