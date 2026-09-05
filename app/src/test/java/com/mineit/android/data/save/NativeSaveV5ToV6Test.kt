package com.mineit.android.data.save

import com.mineit.android.domain.model.GameState
import com.mineit.android.testing.EstablishedColonyFixture
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NativeSaveV5ToV6Test {
    private val json = Json { encodeDefaults = true }

    @Test
    fun `v5 developed colony remains ashore and gains compatible docked fleet`() {
        val original = EstablishedColonyFixture.contract01()
        val colony = original.activeColony
        val legacyColony = json.encodeToJsonElement(colony).jsonObject.toMutableMap().apply {
            put("foundingShipDocked", JsonPrimitive(true))
            remove("foundingShipId")
            remove("shipResidentAssignments")
            remove("planetaryAccommodationResidents")
            remove("establishmentAcknowledged")
            remove("initialManifestProvisioned")
        }
        val legacyState = json.encodeToJsonElement(original).jsonObject.toMutableMap().apply {
            put("colonies", kotlinx.serialization.json.JsonArray(listOf(JsonObject(legacyColony))))
            remove("fleet")
        }
        val raw = JsonObject(
            mapOf(
                "formatVersion" to JsonPrimitive(5),
                "gameVersion" to JsonPrimitive("0.6.0-migration"),
                "savedAtEpochMillis" to JsonPrimitive(1),
                "state" to JsonObject(legacyState),
            ),
        ).toString()

        val migrated = SaveCodec().decode(raw)
        val migratedColony = migrated.state.activeColony

        assertEquals(NativeSaveFormat.CURRENT_VERSION, migrated.formatVersion)
        assertEquals(colony.population, migratedColony.planetaryAccommodationResidents, 0.0)
        assertTrue(migratedColony.shipResidentAssignments.isEmpty())
        assertTrue(migratedColony.establishmentAcknowledged)
        assertTrue(migratedColony.initialManifestProvisioned)
        assertNotNull(migratedColony.foundingShipId)
        assertTrue(migrated.state.fleet.ships.any { it.id == migratedColony.foundingShipId })
        assertFalse(migrated.state.fleet.ships.any { it.residentPassengers > 0.0 })
        assertEquals(colony.inventory.totalFor(com.mineit.android.domain.resources.ResourceCategory.FOOD), migratedColony.inventory.totalFor(com.mineit.android.domain.resources.ResourceCategory.FOOD), 0.0)
    }
}
