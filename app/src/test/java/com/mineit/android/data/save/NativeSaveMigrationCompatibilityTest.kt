package com.mineit.android.data.save

import com.mineit.android.domain.colony.HeadquartersContinuityPhase
import com.mineit.android.domain.colony.HeadquartersIdentityState
import com.mineit.android.domain.colony.HeadquartersOutageState
import com.mineit.android.domain.model.ColonyId
import com.mineit.android.domain.model.ColonyStatus
import com.mineit.android.domain.model.NewGameFactory
import com.mineit.android.domain.world.DevelopmentKind
import com.mineit.android.domain.world.SectorCoordinate
import com.mineit.android.domain.world.TileDevelopment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NativeSaveMigrationCompatibilityTest {
    @Test
    fun `phase 1 native save migrates through current Phase 4 defaults without losing state`() {
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
        assertEquals(HeadquartersIdentityState(), migrated.state.activeColony.headquarters)
    }

    @Test
    fun `phase 3 save without Phase 4 fields migrates with semantic defaults`() {
        val phase3Save = """
            {
              "formatVersion":3,
              "gameVersion":"0.4.0-migration",
              "universeContentVersion":null,
              "universeSourceCommit":null,
              "savedAtEpochMillis":654321,
              "state":{
                "date":{"year":2,"day":45},
                "company":{"cash":32000,"reputation":0},
                "colonies":[{
                  "id":"phase3-colony",
                  "name":"Phase 3 Colony",
                  "population":120.0,
                  "seed":123456789,
                  "world":{
                    "landingCandidates":[],
                    "selectedLandingSiteIndex":0,
                    "tiles":[{
                      "coordinate":{"x":1,"y":0},
                      "terrain":"plain",
                      "terrainVariant":0,
                      "revealed":true,
                      "lastScannedAtLevel":1,
                      "resourceExhausted":false,
                      "deposit":null,
                      "development":{
                        "kind":"power",
                        "level":1,
                        "productionStopped":false,
                        "constructionComplete":true
                      }
                    }],
                    "activeSurveys":[],
                    "surveyQueue":[]
                  }
                }],
                "activeColonyId":"phase3-colony"
              }
            }
        """.trimIndent()

        val migrated = SaveCodec().decode(phase3Save)
        val colony = migrated.state.activeColony
        val tile = colony.world.tiles.single()

        assertEquals(4, migrated.formatVersion)
        assertEquals(HeadquartersIdentityState(), colony.headquarters)
        assertNull(tile.exhaustedResourceId)
        assertFalse(tile.resourceCovered)
        assertEquals(0.0, tile.development?.investedBuild ?: -1.0, 0.0)
        assertEquals(0.0, tile.development?.investedOre ?: -1.0, 0.0)
        assertEquals(DevelopmentKind.POWER, tile.development?.kind)
    }

    @Test
    fun `Phase 4 development and Headquarters state round trips exactly`() {
        val factory = NewGameFactory()
        var state = factory.settleLandingSite(
            factory.contract01(
                colonySeed = 123456789L,
                colonyId = ColonyId("phase4-roundtrip"),
            ),
            0,
        )
        val hq = SectorCoordinate(1, 0)
        val colony = state.activeColony
        val world = colony.world.copy(
            tiles = colony.world.tiles.map { tile ->
                if (tile.coordinate == hq) {
                    tile.copy(
                        revealed = true,
                        resourceCovered = true,
                        development = TileDevelopment(
                            kind = DevelopmentKind.HEADQUARTERS,
                            level = 2,
                            investedBuild = 260.0,
                            investedOre = 25.0,
                        ),
                    )
                } else tile
            },
        )
        val updatedColony = colony.copy(
            world = world,
            headquarters = HeadquartersIdentityState(
                primary = hq,
                primaryEverAssigned = true,
                commandHandoverComplete = true,
                outage = HeadquartersOutageState(
                    phase = HeadquartersContinuityPhase.RECOVERY,
                    penalty = .08,
                    offlineDays = 3,
                    outageStartedAbsoluteDay = 10,
                    outageStartPenalty = .10,
                    recoveryStartedAbsoluteDay = 13,
                    recoveryInitialPenalty = .10,
                    recoveryDaysElapsed = 2,
                    recoveryDaysRemaining = 8,
                    lastOutageDays = 3,
                ),
            ),
        )
        state = state.copy(colonies = state.colonies.map { if (it.id == updatedColony.id) updatedColony else it })
        val envelope = SaveEnvelope(
            formatVersion = NativeSaveFormat.CURRENT_VERSION,
            gameVersion = "0.5.0-migration",
            savedAtEpochMillis = 999999,
            state = state,
        )
        val codec = SaveCodec()

        val decoded = codec.decode(codec.encode(envelope))

        assertEquals(envelope, decoded)
    }
}
