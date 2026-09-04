package com.mineit.android.data.save

import com.mineit.android.domain.model.ColonyStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NativeSaveMigrationCompatibilityTest {
    @Test
    fun `phase 1 native save migrates through current Phase 3 defaults without losing state`() {
        val phase1Save = """
            {
              "formatVersion":1,
              "gameVersion":"0.2.0-migration",
              "universeContentVersion":null,
              "universeSourceCommit":null,
              "savedAtEpochMillis":123456,
              "state":{
                "date":{"year":1,"day":1},
                "company":{"cash":500000,"reputation":7},
                "colonies":[{
                  "id":"test-colony-1",
                  "name":"Koplin Prospect",
                  "population":120,
                  "seed":123456789,
                  "inventory":{"resources":[
                    {"resourceId":"fungal","category":"food","qualityBands":{"excellent":350.0,"rare":75.0}}
                  ]}
                }],
                "activeColonyId":"test-colony-1"
              }
            }
        """.trimIndent()

        val migrated = SaveCodec().decode(phase1Save)

        assertEquals(NativeSaveFormat.CURRENT_VERSION, migrated.formatVersion)
        assertEquals("0.2.0-migration", migrated.gameVersion)
        assertEquals(500_000L, migrated.state.company.cash)
        assertEquals(7, migrated.state.company.reputation)
        assertEquals(120.0, migrated.state.activeColony.population, 0.0)
        assertEquals(425.0, migrated.state.activeColony.inventory.resources.single().amount, 0.0)
        assertEquals(ColonyStatus.PLAYING, migrated.state.activeColony.status)
        assertEquals(0, migrated.state.activeColony.foodStarvationDays)
        assertFalse(migrated.state.activeColony.emergencyMode)
        assertTrue(migrated.state.activeColony.foundingShipDocked)
        assertTrue(migrated.state.activeColony.world.tiles.isEmpty())
        assertTrue(migrated.state.activeColony.world.landingCandidates.isEmpty())
    }
}
