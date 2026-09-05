package com.mineit.android.data.save

import com.mineit.android.domain.resources.ResourceCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NativeSaveV5ToV6Test {
    @Test
    fun `v5 staged founding ship migrates without reallocating developed colony state`() {
        val raw = """
            {
              "formatVersion":5,
              "gameVersion":"0.7.0-migration",
              "savedAtEpochMillis":123456,
              "state":{
                "date":{"year":1,"day":40},
                "company":{"cash":32000.0,"reputation":0.0},
                "colonies":[{
                  "id":"legacy-colony",
                  "name":"Legacy Colony",
                  "population":117.5,
                  "seed":99,
                  "inventory":{"resources":[{
                    "resourceId":"fungal",
                    "category":"food",
                    "qualityBands":{"excellent":444.0}
                  }]},
                  "foundingShipDocked":true
                }],
                "activeColonyId":"legacy-colony"
              }
            }
        """.trimIndent()

        val migrated = SaveCodec().decode(raw)
        val colony = migrated.state.activeColony
        val ship = migrated.state.fleet.ships.single()

        assertEquals(NativeSaveFormat.CURRENT_VERSION, migrated.formatVersion)
        assertEquals(117.5, colony.population, 0.0)
        assertEquals(444.0, colony.inventory.amountFor(ResourceCategory.FOOD), 0.0)
        assertTrue(colony.shipResidentAssignments.isEmpty())
        assertEquals(117.5, colony.planetaryAccommodationResidents, 0.0)
        assertTrue(colony.establishmentAcknowledged)
        assertTrue(colony.initialManifestProvisioned)
        assertNotNull(colony.foundingShipId)
        assertEquals(colony.id, ship.dockedColonyId)
        assertEquals(10, ship.crew)
        assertEquals(50.0, ship.industrySupport, 0.0)
        assertTrue(ship.commandCapable)
        assertTrue(ship.inventory.resources.isEmpty())
        assertFalse(migrated.state.fleet.ships.isEmpty())
    }
}
