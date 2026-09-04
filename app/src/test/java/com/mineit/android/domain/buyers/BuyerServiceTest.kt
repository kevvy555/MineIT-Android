package com.mineit.android.domain.buyers

import com.mineit.android.domain.model.AbsoluteDay
import com.mineit.android.domain.model.GameDate
import com.mineit.android.domain.model.GameState
import com.mineit.android.domain.model.NewGameFactory
import com.mineit.android.domain.model.ResourceId
import com.mineit.android.domain.resources.QualityBand
import com.mineit.android.domain.resources.ResourceCategory
import com.mineit.android.domain.trade.TradeState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BuyerServiceTest {
    private val service = BuyerService()
    private val factory = NewGameFactory()

    private fun settled(seed: Long = 123L): GameState = factory.settleLandingSite(factory.contract01(seed), 0)

    @Test
    fun deterministicOffersAndNetworkReputationGatesMatchCommercialContractRules() {
        val first = service.ensureMarket(settled(123L))
        val second = service.ensureMarket(settled(123L))
        assertEquals(first.company.buyers.offers, second.company.buyers.offers)
        assertTrue(first.company.buyers.offers.isNotEmpty())

        val offer = first.company.buyers.offers.first()
        val networkBlocked = service.canEnter(first, offer.id, networkAvailable = false)
        assertFalse(networkBlocked.first)

        val enoughReputation = first.copy(company = first.company.copy(reputation = offer.minimumReputation))
        val eligible = service.canEnter(enoughReputation, offer.id, networkAvailable = true)
        assertTrue(eligible.first)
        val entered = service.enterContract(enoughReputation, offer.id, networkAvailable = true)
        assertTrue(entered.ok)
        assertEquals(1, entered.state.company.buyers.contracts.size)
    }

    @Test
    fun dueCollectionUsesZeroFiveTenFifteenDayAttempts() {
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
        val base = settled().copy(date = GameDate.fromAbsoluteDay(AbsoluteDay(100)))
        val contract = BuyerContract(
            id = "buyer-contract-1",
            offerId = offer.id,
            buyerId = offer.buyerId,
            colonyId = base.activeColonyId,
            resourceId = offer.resourceId,
            minimumQuality = offer.minimumQuality,
            quantity = offer.quantity,
            unitRate = offer.unitRate,
            intervalDays = offer.intervalDays,
            nextDueAbsoluteDay = 100,
        )
        val state = base.copy(company = base.company.copy(buyers = BuyerMarketState(
            offers = listOf(offer),
            contracts = listOf(contract),
        )))

        val due = service.processDay(state, berthAvailable = true)
        val waiting = due.state.company.buyers.contracts.single()
        assertEquals(BuyerShipStatus.DOCKED, waiting.ship.status)
        assertEquals(0, waiting.ship.attemptIndex)
        assertEquals(100, waiting.ship.nextEventAbsoluteDay)
        assertEquals(1, due.events.size)

        val wait1 = service.continueWaiting(due.state, contract.id)
        assertTrue(wait1.ok)
        assertEquals(1, wait1.state.company.buyers.contracts.single().ship.attemptIndex)
        assertEquals(105, wait1.state.company.buyers.contracts.single().ship.nextEventAbsoluteDay)

        val wait2State = wait1.state.copy(date = GameDate.fromAbsoluteDay(AbsoluteDay(105)))
        val wait2 = service.continueWaiting(wait2State, contract.id)
        assertEquals(2, wait2.state.company.buyers.contracts.single().ship.attemptIndex)
        assertEquals(110, wait2.state.company.buyers.contracts.single().ship.nextEventAbsoluteDay)

        val wait3State = wait2.state.copy(date = GameDate.fromAbsoluteDay(AbsoluteDay(110)))
        val wait3 = service.continueWaiting(wait3State, contract.id)
        assertEquals(3, wait3.state.company.buyers.contracts.single().ship.attemptIndex)
        assertEquals(115, wait3.state.company.buyers.contracts.single().ship.nextEventAbsoluteDay)
        assertFalse(service.continueWaiting(wait3.state, contract.id).ok)
    }

    @Test
    fun buyerShipmentRequiresHalfQualifyingStockAndDoesNotConsumeCorporateExportCapacity() {
        val resource = ResourceId("iron")
        val offer = BuyerOffer(
            id = "offer-test",
            buyerId = "buyer-test",
            buyerName = "Buyer Test",
            companyName = "Buyer Test Ltd",
            resourceId = resource,
            minimumQuality = QualityBand.GOOD,
            quantity = 100.0,
            unitRate = 2.0,
            intervalDays = 45,
            minimumReputation = 0.0,
        )
        var state = settled()
        val colony = state.activeColony
        val inventory = colony.inventory.store(resource, ResourceCategory.ORE, 75.0, quality = 201)
        val contract = BuyerContract(
            id = "buyer-contract-1",
            offerId = offer.id,
            buyerId = offer.buyerId,
            colonyId = colony.id,
            resourceId = resource,
            minimumQuality = QualityBand.GOOD,
            quantity = 100.0,
            unitRate = 2.0,
            intervalDays = 45,
            nextDueAbsoluteDay = 46,
            ship = BuyerShipState(
                status = BuyerShipStatus.DOCKED,
                dueAbsoluteDay = 1,
                attemptIndex = 0,
                nextEventAbsoluteDay = 1,
                eventPending = true,
            ),
        )
        state = state.copy(
            company = state.company.copy(buyers = BuyerMarketState(
                offers = listOf(offer),
                contracts = listOf(contract),
                relationships = listOf(BuyerRelationship(offer.buyerId)),
            )),
            colonies = state.colonies.map {
                if (it.id == colony.id) it.copy(
                    inventory = inventory,
                    trade = TradeState(active = true, exportUsed = 123.0, visitExportCapacity = 100_000.0),
                ) else it
            },
        )

        val projection = service.projection(state, contract.id)!!
        assertEquals(.75, projection.completionRatio, .0001)
        assertTrue(projection.canTransfer)
        val transferred = service.transfer(state, contract.id)
        assertTrue(transferred.ok)
        assertEquals(150.0, transferred.revenue, .0001)
        assertEquals(123.0, transferred.state.activeColony.trade.exportUsed, .0001)
        assertEquals(0.0, transferred.state.activeColony.inventory.amountFor(resource), .0001)
        assertEquals(32_150.0, transferred.state.company.cash, .0001)
        // On-time 75% fulfilment is -1 happiness => -0.10 reputation, then +0.01 shipment award.
        assertEquals(-.09, transferred.state.company.reputation, .0001)

        val underHalfState = state.copy(colonies = state.colonies.map {
            if (it.id == colony.id) it.copy(inventory = colony.inventory.store(resource, ResourceCategory.ORE, 49.0, quality = 201)) else it
        })
        assertFalse(service.projection(underHalfState, contract.id)!!.canTransfer)
    }
}
