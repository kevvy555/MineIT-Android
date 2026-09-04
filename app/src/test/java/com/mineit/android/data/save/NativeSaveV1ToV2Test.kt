package com.mineit.android.data.save

import com.mineit.android.domain.model.ColonyStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NativeSaveV1ToV2Test {
    @Test
    fun `phase 1 native save migrates into phase 2 defaults without losing state`() {
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

        assertEquals(2, migrated.formatVersion)
        assertEquals("0.2.0-migration", migrated.gameVersion)
        assertEquals(500_000L, migrated.state.company.cash)
        assertEquals(7, migrated.state.company.reputation)
        assertEquals(120, migrated.state.activeColony.population)
        assertEquals(425.0, migrated.state.activeColony.inventory.resources.single().amount, 0.0)
        assertEquals(ColonyStatus.PLAYING, migrated.state.activeColony.status)
        assertTrue(migrated.state.activeColony.world.tiles.isEmpty())
        assertTrue(migrated.state.activeColony.world.landingCandidates.isEmpty())
    }
}
