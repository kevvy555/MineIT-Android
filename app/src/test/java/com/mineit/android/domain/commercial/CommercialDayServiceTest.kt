package com.mineit.android.domain.commercial

import com.mineit.android.domain.buyers.BuyerContract
import com.mineit.android.domain.buyers.BuyerMarketState
import com.mineit.android.domain.buyers.BuyerOffer
import com.mineit.android.domain.events.CorporateEventType
import com.mineit.android.domain.model.AbsoluteDay
import com.mineit.android.domain.model.GameDate
import com.mineit.android.domain.model.GameState
import com.mineit.android.domain.model.NewGameFactory
import com.mineit.android.domain.model.ResourceId
import com.mineit.android.domain.resources.QualityBand
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CommercialDayServiceTest {
    private val service = CommercialDayService()
    private val factory = NewGameFactory()

    private fun settled(seed: Long = 123L): GameState = factory.settleLandingSite(factory.contract01(seed), 0)

    @Test
    fun corporateShipArrivesWhenDayAdvanceReaches181AndRecoveryDoesNotDuplicateEvent() {
        val state = settled().copy(date = GameDate.fromAbsoluteDay(AbsoluteDay(180)))

        val result = service.advanceDay(state)

        assertEquals(181, result.state.date.toAbsoluteDay().value)
        assertTrue(result.state.activeColony.trade.active)
        assertTrue(result.shouldPause)
        assertEquals(1, result.state.corporateEvents.pending.count { it.type == CorporateEventType.SHIP })
        assertTrue(result.state.gameLog.events.any { it.type == "corporate-ship-arrival" })

        val recovered = service.recoverBlockingEvents(result.state, result.metrics)
        assertEquals(1, recovered.corporateEvents.pending.count { it.type == CorporateEventType.SHIP })
    }

    @Test
    fun missedContractDeadlineQueuesDurableExtensionDecisionAfterSimulationDay() {
        val state = settled().copy(date = GameDate.fromAbsoluteDay(AbsoluteDay(3_600)))

        val result = service.advanceDay(state)

        val event = result.state.corporateEvents.pending.first { it.type == CorporateEventType.CONTRACT }
        assertEquals(3_601, result.state.date.toAbsoluteDay().value)
        assertEquals("extension", event.kind)
        assertEquals("extension", result.state.activeColony.contract!!.pendingDecision)
        assertTrue(result.shouldPause)

        val recovered = service.recoverBlockingEvents(result.state, result.metrics)
        assertEquals(1, recovered.corporateEvents.pending.count { it.type == CorporateEventType.CONTRACT && it.kind == "extension" })
    }

    @Test
    fun buyerDueDateIsProcessedBySameDailyAdvanceAndQueuesCollectionEvent() {
        val offer = BuyerOffer(
            id = "offer-test",
            buyerId = "buyer-test",
            buyerName = "Buyer Test",
            companyName = "Buyer Test Ltd",
            resourceId = ResourceId("iron"),
            minimumQuality = QualityBand.GOOD,
            quantity = 100.0,
            unitRate = 2.0,
            intervalDays = 45,
            minimumReputation = 0.0,
        )
        var state = settled().copy(date = GameDate.fromAbsoluteDay(AbsoluteDay(1)))
        val contract = BuyerContract(
            id = "buyer-contract-1",
            offerId = offer.id,
            buyerId = offer.buyerId,
            colonyId = state.activeColonyId,
            resourceId = offer.resourceId,
            minimumQuality = offer.minimumQuality,
            quantity = offer.quantity,
            unitRate = offer.unitRate,
            intervalDays = offer.intervalDays,
            nextDueAbsoluteDay = 2,
        )
        state = state.copy(company = state.company.copy(buyers = BuyerMarketState(
            offers = listOf(offer),
            contracts = listOf(contract),
        )))

        val result = service.advanceDay(state)

        val event = result.state.corporateEvents.pending.first { it.type == CorporateEventType.BUYER }
        assertEquals(contract.id, event.contractId)
        assertEquals(0, event.attemptIndex)
        assertEquals(2, event.dueAbsoluteDay)
        assertTrue(result.shouldPause)
    }
}
