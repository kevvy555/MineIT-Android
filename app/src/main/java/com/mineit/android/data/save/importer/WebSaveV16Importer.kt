package com.mineit.android.data.save.importer

import com.mineit.android.domain.model.ColonyId
import com.mineit.android.domain.model.GameDate
import com.mineit.android.domain.model.ResourceId
import com.mineit.android.domain.resources.Inventory
import com.mineit.android.domain.resources.QualityBand
import com.mineit.android.domain.resources.ResourceCategory
import com.mineit.android.domain.resources.ResourceStock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

/**
 * Boundary parser for the current web save schema. It deliberately returns an import preview rather than
 * pretending Phase 1 can already map every web subsystem into the still-growing native GameState.
 */
class WebSaveV16Importer {
    private val json = Json { ignoreUnknownKeys = true }

    fun parse(raw: String): WebSaveV16ImportPreview {
        require(raw.isNotBlank()) { "Web save is empty." }
        val root = json.parseToJsonElement(raw).jsonObject
        val version = requireInt(root, "version")
        require(version == SUPPORTED_VERSION) {
            "Expected MineIT web save version $SUPPORTED_VERSION but found $version."
        }

        val portfolio = requireObject(root, "portfolio")
        val activeColonyId = ColonyId(requireString(portfolio, "activeColonyId"))
        val rootColonyId = ColonyId(requireString(root, "colonyId"))
        require(rootColonyId == activeColonyId) {
            "Web save root colony does not match portfolio active colony."
        }

        val colonies = requireArray(portfolio, "colonies")
        require(colonies.isNotEmpty()) { "Web save portfolio must contain at least one colony." }
        require(colonies.any { entry ->
            entry is JsonObject && entry["id"]?.jsonPrimitive?.content == activeColonyId.value
        }) { "Web save active colony is missing from portfolio." }

        val company = requireObject(root, "company")
        val inventory = parseInventory(requireObject(root, "inventory"))
        val colonyName = findActiveColonyName(colonies, activeColonyId)
            ?: root["contract"]?.jsonObject?.get("colonyName")?.jsonPrimitive?.content
            ?: "Imported Colony"

        return WebSaveV16ImportPreview(
            sourceVersion = version,
            date = GameDate(
                year = requireInt(root, "year"),
                day = requireInt(root, "day"),
            ),
            activeColonyId = activeColonyId,
            activeColonyName = colonyName,
            activePopulation = requireInt(root, "pop").also {
                require(it >= 0) { "Web save population must not be negative." }
            },
            activeColonySeed = requireLong(root, "seed"),
            companyCash = requireLong(company, "cash"),
            companyReputation = optionalInt(company, "rep") ?: 0,
            colonyCount = colonies.size,
            activeInventory = inventory,
        )
    }

    private fun parseInventory(source: JsonObject): Inventory {
        val resources = source.values.mapNotNull { element ->
            val entry = element as? JsonObject ?: return@mapNotNull null
            val rawResourceId = entry["resourceId"]?.jsonPrimitive?.content?.trim().orEmpty()
            if (rawResourceId.isBlank()) return@mapNotNull null

            val category = parseCategory(requireString(entry, "type"))
            val bandsElement = entry["qualityBands"]
            val bands = if (bandsElement is JsonObject) {
                bandsElement.mapNotNull { (rawKey, rawValue) ->
                    val bandObject = rawValue as? JsonObject ?: return@mapNotNull null
                    val amount = bandObject["amount"]?.jsonPrimitive?.doubleOrNull ?: 0.0
                    require(amount.isFinite() && amount >= 0.0) {
                        "Web save quality-band amount must be finite and non-negative."
                    }
                    parseQualityBand(rawKey) to amount
                }.toMap()
            } else {
                val legacyAmount = entry["amount"]?.jsonPrimitive?.doubleOrNull ?: 0.0
                require(legacyAmount.isFinite() && legacyAmount >= 0.0) {
                    "Web save resource amount must be finite and non-negative."
                }
                if (legacyAmount > 0.0) mapOf(QualityBand.EXCELLENT to legacyAmount) else emptyMap()
            }

            ResourceStock(
                resourceId = ResourceId(rawResourceId),
                category = category,
                qualityBands = bands,
            )
        }
        return Inventory(resources)
    }

    private fun findActiveColonyName(colonies: JsonArray, activeId: ColonyId): String? = colonies
        .asSequence()
        .mapNotNull { it as? JsonObject }
        .firstOrNull { it["id"]?.jsonPrimitive?.content == activeId.value }
        ?.get("name")
        ?.jsonPrimitive
        ?.content

    private fun parseCategory(raw: String): ResourceCategory = when (raw.lowercase()) {
        "food" -> ResourceCategory.FOOD
        "build" -> ResourceCategory.BUILD
        "fuel" -> ResourceCategory.FUEL
        "ore" -> ResourceCategory.ORE
        else -> error("Unsupported web resource category '$raw'.")
    }

    private fun parseQualityBand(raw: String): QualityBand = when (raw.lowercase()) {
        "common" -> QualityBand.COMMON
        "good" -> QualityBand.GOOD
        "excellent" -> QualityBand.EXCELLENT
        "exceptional" -> QualityBand.EXCEPTIONAL
        "rare" -> QualityBand.RARE
        "extraordinary" -> QualityBand.EXTRAORDINARY
        else -> QualityBand.EXCELLENT
    }

    private fun requireObject(parent: JsonObject, key: String): JsonObject =
        requireNotNull(parent[key]) { "Web save is missing '$key'." }.jsonObject

    private fun requireArray(parent: JsonObject, key: String): JsonArray =
        requireNotNull(parent[key]) { "Web save is missing '$key'." }.jsonArray

    private fun requireString(parent: JsonObject, key: String): String =
        requireNotNull(parent[key]) { "Web save is missing '$key'." }.jsonPrimitive.content

    private fun requireInt(parent: JsonObject, key: String): Int =
        requireNotNull(parent[key]) { "Web save is missing '$key'." }.jsonPrimitive.intOrNull
            ?: error("Web save '$key' must be an integer.")

    private fun optionalInt(parent: JsonObject, key: String): Int? = parent[key]?.jsonPrimitive?.intOrNull

    private fun requireLong(parent: JsonObject, key: String): Long =
        requireNotNull(parent[key]) { "Web save is missing '$key'." }.jsonPrimitive.longOrNull
            ?: error("Web save '$key' must be an integer.")

    companion object {
        const val SUPPORTED_VERSION = 16
    }
}

data class WebSaveV16ImportPreview(
    val sourceVersion: Int,
    val date: GameDate,
    val activeColonyId: ColonyId,
    val activeColonyName: String,
    val activePopulation: Int,
    val activeColonySeed: Long,
    val companyCash: Long,
    val companyReputation: Int,
    val colonyCount: Int,
    val activeInventory: Inventory,
)
