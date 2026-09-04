package com.mineit.android.domain.trade

import com.mineit.android.domain.model.AbsoluteDay
import com.mineit.android.domain.model.GameDate
import com.mineit.android.domain.model.GameState
import com.mineit.android.domain.model.NewGameFactory
import com.mineit.android.domain.model.ResourceId
import com.mineit.android.domain.resources.QualityBand
import com.mineit.android.domain.resources.ResourceCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CorporateTradeServiceTest {
    private val service = CorporateTradeService()
    private val factory = NewGameFactory()

    private fun settled(seed: Long): GameState = factory.settleLandingSite(factory.contract01(seed), 0)

    @Test
    fun arrivalUsesPinnedScheduleAndVisitCapacities() {
        val fresh = settled(42L)
        assertEquals(180, service.daysUntilArrival(fresh))
        val due = fresh.copy(date = GameDate.fromAbsoluteDay(AbsoluteDay(181)))
        assertTrue(service.shouldArrive(due))
        val arrived = service.arrive(due)
        assertTrue(arrived.ok)
        assertTrue(arrived.state.activeColony.trade.active)
        assertEquals(1, arrived.state.activeColony.trade.visits)
        assertEquals(4_000.0, service.cargoCapacity(arrived.state), .0001)
        assertEquals(100_000.0, service.exportCapacity(arrived.state), .0001)
        assertEquals(361, arrived.state.activeColony.trade.nextArrivalAbsoluteDay)
    }

    @Test
    fun reserveProtectsEveryResourceAndHighestQualitySellsFirst() {
        var state = settled(7L).copy(date = GameDate.fromAbsoluteDay(AbsoluteDay(181)))
        state = service.arrive(state).state
        val colony = state.activeColony
        var inventory = colony.inventory.store(ResourceId("gold"), ResourceCategory.ORE, 100.0, quality = 30)
        inventory = inventory.store(ResourceId("gold"), ResourceCategory.ORE, 20.0, quality = 9_000)
        state = state.copy(colonies = state.colonies.map { if (it.id == colony.id) colony.copy(inventory = inventory) else it })
        state = service.setColonyTradeReserve(state, 20.0)

        assertEquals(100.0, service.sellableAmount(state, ResourceId("gold")), .0001)
        assertEquals(18.75, service.sellPrice(ResourceId("gold"), QualityBand.COMMON), .0001)
        assertEquals(75.0, service.sellPrice(ResourceId("gold"), QualityBand.EXTRAORDINARY), .0001)
        val result = service.sell(state, ResourceId("gold"), 30.0, spaceportServicesAvailable = true)
        assertTrue(result.ok)
        assertEquals(30.0, result.quantity, .0001)
        assertEquals(90.0, result.state.activeColony.inventory.find(ResourceId("gold"))!!.qualityBands[QualityBand.COMMON]!!, .0001)
        assertEquals(0.0, result.state.activeColony.inventory.find(ResourceId("gold"))!!.qualityBands[QualityBand.EXTRAORDINARY]!!, .0001)
        assertEquals(1_687.5, result.value, .0001)
        assertEquals(33_687.5, result.state.company.cash, .0001)
        assertEquals(.01, result.state.company.reputation, .0001)

        val second = service.sell(result.state, ResourceId("gold"), 10.0, spaceportServicesAvailable = true)
        assertTrue(second.ok)
        assertEquals(.01, second.state.company.reputation, .0001)
    }

    @Test
    fun buyRespectsCargoCashAndSpaceportGate() {
        var state = settled(9L).copy(date = GameDate.fromAbsoluteDay(AbsoluteDay(181)))
        state = service.arrive(state).state
        val blocked = service.buy(state, ResourceId("coal"), 100.0, spaceportServicesAvailable = false)
        assertFalse(blocked.ok)
        assertEquals(state.company.cash, blocked.state.company.cash, .0001)

        val bought = service.buy(state, ResourceId("coal"), 100.0, spaceportServicesAvailable = true)
        assertTrue(bought.ok)
        assertEquals(100.0, bought.quantity, .0001)
        assertEquals(210.0, bought.value, .0001)
        assertEquals(31_790.0, bought.state.company.cash, .0001)
        assertEquals(100.0, bought.state.activeColony.inventory.amountFor(ResourceId("coal")), .0001)
        assertEquals(100.0, bought.state.activeColony.trade.cargoUsed, .0001)
    }

    @Test
    fun colonistTransferUsesHousingPowerPassengersAndCashAsHardGatesButNotFood() {
        var state = settled(11L).copy(date = GameDate.fromAbsoluteDay(AbsoluteDay(181)))
        state = service.arrive(state).state
        val projection = service.colonistProjection(
            state = state,
            supportedPopulationCapacity = 200,
            foodDailySurplus = 0.0,
        )
        assertEquals(80, projection.maxTransfer)
        assertEquals(0, projection.maxSafeTransfer)

        val transferred = service.transferColonists(
            state = state,
            amount = 10,
            spaceportServicesAvailable = true,
            supportedPopulationCapacity = 200,
            foodDailySurplus = 0.0,
        )
        assertTrue(transferred.ok)
        assertEquals(130.0, transferred.state.activeColony.population, .0001)
        assertEquals(10, transferred.state.activeColony.trade.passengersUsed)
        assertEquals(29_500.0, transferred.state.company.cash, .0001)
        assertTrue(transferred.message.contains("Food production does not currently support"))

        val blocked = service.transferColonists(
            state = state,
            amount = 81,
            spaceportServicesAvailable = true,
            supportedPopulationCapacity = 200,
            foodDailySurplus = 10_000.0,
        )
        assertFalse(blocked.ok)
    }
}
