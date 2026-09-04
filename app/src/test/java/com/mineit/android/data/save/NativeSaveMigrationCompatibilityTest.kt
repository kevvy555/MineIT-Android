package com.mineit.android.data.save

import com.mineit.android.domain.buyers.BuyerContract
import com.mineit.android.domain.buyers.BuyerMarketState
import com.mineit.android.domain.buyers.BuyerOffer
import com.mineit.android.domain.buyers.BuyerRelationship
import com.mineit.android.domain.buyers.BuyerShipState
import com.mineit.android.domain.buyers.BuyerShipStatus
import com.mineit.android.domain.colony.HeadquartersContinuityPhase
import com.mineit.android.domain.colony.HeadquartersIdentityState
import com.mineit.android.domain.colony.HeadquartersOutageState
import com.mineit.android.domain.events.CorporateEvent
import com.mineit.android.domain.events.CorporateEventQueueState
import com.mineit.android.domain.events.CorporateEventType
import com.mineit.android.domain.logging.GameLogEvent
import com.mineit.android.domain.logging.GameLogState
import com.mineit.android.domain.model.ColonyId
import com.mineit.android.domain.model.ColonyStatus
import com.mineit.android.domain.model.NewGameFactory
import com.mineit.android.domain.model.ResourceId
import com.mineit.android.domain.resources.QualityBand
import com.mineit.android.domain.trade.TradeState
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
    fun `phase 1 native save migrates through current Phase 6 defaults without losing state`() {
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
        assertEquals(500_000.0, migrated.state.company.cash, 0.0)
        assertEquals(7.0, migrated.state.company.reputation, 0.0)
        assertEquals(0, migrated.state.company.wins)
        assertFalse(migrated.state.company.gameOver)
        assertTrue(migrated.state.company.buyers.offers.isEmpty())
        assertEquals(120.0, migrated.state.activeColony.population, 0.0)
        assertEquals(425.0, migrated.state.activeColony.inventory.resources.single().amount, 0.0)
        assertEquals(ColonyStatus.PLAYING, migrated.state.activeColony.status)
        assertEquals(TradeState(), migrated.state.activeColony.trade)
        assertTrue(migrated.state.corporateEvents.pending.isEmpty())
        assertTrue(migrated.state.gameLog.events.isEmpty())
        assertEquals(HeadquartersIdentityState(), migrated.state.activeColony.headquarters)
    }

    @Test
    fun `phase 3 save migrates through Phase 6 with infrastructure and commercial defaults`() {
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

        assertEquals(NativeSaveFormat.CURRENT_VERSION, migrated.formatVersion)
        assertEquals(HeadquartersIdentityState(), colony.headquarters)
        assertNull(tile.exhaustedResourceId)
        assertFalse(tile.resourceCovered)
        assertEquals(0.0, tile.development?.investedBuild ?: -1.0, 0.0)
        assertEquals(0.0, tile.development?.investedOre ?: -1.0, 0.0)
        assertEquals(DevelopmentKind.POWER, tile.development?.kind)
        assertEquals(TradeState(), colony.trade)
        assertEquals(0.0, colony.tradeReserve, 0.0)
        assertTrue(migrated.state.company.buyers.contracts.isEmpty())
    }

    @Test
    fun `phase 4 save migrates through v6 with semantic commercial and fleet defaults`() {
        val phase4Save = """
            {
              "formatVersion":4,
              "gameVersion":"0.5.1-migration",
              "universeContentVersion":null,
              "universeSourceCommit":null,
              "savedAtEpochMillis":777777,
              "state":{
                "date":{"year":1,"day":180},
                "company":{"cash":32000.0,"reputation":2.5,"earnedRevenue":125.0},
                "colonies":[{
                  "id":"phase4-colony",
                  "name":"Phase 4 Colony",
                  "population":120.0,
                  "seed":123456789,
                  "status":"playing"
                }],
                "activeColonyId":"phase4-colony"
              }
            }
        """.trimIndent()

        val migrated = SaveCodec().decode(phase4Save)

        assertEquals(NativeSaveFormat.CURRENT_VERSION, migrated.formatVersion)
        assertEquals(2.5, migrated.state.company.reputation, 0.0)
        assertEquals(0, migrated.state.company.wins)
        assertFalse(migrated.state.company.gameOver)
        assertEquals(BuyerMarketState(), migrated.state.company.buyers)
        assertEquals(TradeState(), migrated.state.activeColony.trade)
        assertEquals(0.0, migrated.state.activeColony.tradeReserve, 0.0)
        assertEquals(CorporateEventQueueState(), migrated.state.corporateEvents)
        assertEquals(GameLogState(), migrated.state.gameLog)
        assertTrue(migrated.state.fleet.ships.isEmpty())
        assertEquals(120.0, migrated.state.activeColony.planetaryAccommodationResidents, 0.0)
    }

    @Test
    fun `phase 6 commercial fleet state and Phase 4 infrastructure round trip exactly in v6`() {
        val factory = NewGameFactory()
        var state = factory.settleLandingSite(
            factory.contract01(
                colonySeed = 123456789L,
                colonyId = ColonyId("phase6-roundtrip"),
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
        val offer = BuyerOffer(
            id = "offer-test-buyer",
            buyerId = "test-buyer",
            buyerName = "Test Buyer",
            companyName = "Test Industries",
            resourceId = ResourceId("iron"),
            minimumQuality = QualityBand.GOOD,
            quantity = 750.0,
            unitRate = 1.25,
            intervalDays = 45,
            minimumReputation = 1.5,
        )
        val buyerContract = BuyerContract(
            id = "buyer-contract-1",
            offerId = offer.id,
            buyerId = offer.buyerId,
            colonyId = colony.id,
            resourceId = offer.resourceId,
            minimumQuality = QualityBand.GOOD,
            quantity = offer.quantity,
            unitRate = offer.unitRate,
            intervalDays = offer.intervalDays,
            nextDueAbsoluteDay = 225,
            ship = BuyerShipState(
                status = BuyerShipStatus.DOCKED,
                dueAbsoluteDay = 225,
                attemptIndex = 1,
                nextEventAbsoluteDay = 230,
                eventPending = true,
            ),
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
            trade = TradeState(
                active = true,
                nextArrivalAbsoluteDay = 361,
                visits = 1,
                arrivedAtAbsoluteDay = 181,
                cargoUsed = 125.0,
                exportUsed = 250.0,
                passengersUsed = 10,
                visitCargoCapacity = 4_625.0,
                visitExportCapacity = 125_000.0,
                exportReputationAwarded = true,
            ),
            tradeReserve = 300.0,
            contract = colony.contract?.copy(localRevenue = 12_345.5, localCosts = 2_345.25),
        )
        state = state.copy(
            company = state.company.copy(
                cash = 44_321.75,
                reputation = 2.51,
                earnedRevenue = 12_345.5,
                wins = 1,
                buyers = BuyerMarketState(
                    offers = listOf(offer),
                    contracts = listOf(buyerContract),
                    relationships = listOf(BuyerRelationship("test-buyer", happiness = 68, fulfilledShipments = 2, lifetimeRevenue = 4_200.0)),
                ),
            ),
            colonies = state.colonies.map { if (it.id == updatedColony.id) updatedColony else it },
            corporateEvents = CorporateEventQueueState(
                nextSequence = 3,
                pending = listOf(
                    CorporateEvent(
                        type = CorporateEventType.BUYER,
                        colonyId = colony.id,
                        colonyName = colony.name,
                        sequence = 2,
                        contractId = buyerContract.id,
                        attemptIndex = 1,
                        dueAbsoluteDay = 225,
                    ),
                ),
            ),
            gameLog = GameLogState(
                nextId = 2,
                events = listOf(
                    GameLogEvent(
                        id = 1,
                        absoluteDay = 181,
                        year = 1,
                        day = 181,
                        type = "corporate-ship-arrival",
                        message = "Corporate trade ship arrived.",
                        colonyId = colony.id,
                        colonyName = colony.name,
                    ),
                ),
            ),
        )
        val envelope = SaveEnvelope(
            formatVersion = NativeSaveFormat.CURRENT_VERSION,
            gameVersion = "0.7.1-migration",
            savedAtEpochMillis = 999999,
            state = state,
        )
        val codec = SaveCodec()

        val decoded = codec.decode(codec.encode(envelope))

        assertEquals(envelope, decoded)
    }
}
