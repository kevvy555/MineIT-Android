package com.mineit.android.ui.commercial

import com.mineit.android.domain.model.ResourceId
import com.mineit.android.domain.resources.QualityBand
import com.mineit.android.domain.resources.ResourceCategory
import com.mineit.android.domain.resources.ResourceStock
import com.mineit.android.domain.trade.ColonistTransferProjection
import com.mineit.android.testing.EstablishedColonyFixture
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CorporateTradePresentationTest {
    @Test
    fun `sell surface hides stock fully protected by colony reserve`() {
        var state = EstablishedColonyFixture.contract01()
        state = state.withActiveColony { colony ->
            colony.copy(
                tradeReserve = 600.0,
                inventory = colony.inventory.copy(
                    resources = colony.inventory.resources + ResourceStock(
                        ResourceId("gold"),
                        ResourceCategory.ORE,
                        mapOf(QualityBand.EXCELLENT to 25.0),
                    ),
                ),
            )
        }

        val visible = CorporateTradePresentation.sellStocks(state) { id ->
            (state.activeColony.inventory.amountFor(id) - state.activeColony.tradeReserve).coerceAtLeast(0.0)
        }

        assertTrue(visible.any { it.resourceId == ResourceId("fungal") })
        assertFalse(visible.any { it.resourceId == ResourceId("gold") })
    }

    @Test
    fun `buy surface uses web category order and prioritises reserve shortfalls`() {
        var state = EstablishedColonyFixture.contract01()
        state = state.withActiveColony { colony -> colony.copy(tradeReserve = 500.0) }

        assertEquals(
            listOf(ResourceCategory.FUEL, ResourceCategory.FOOD, ResourceCategory.ORE, ResourceCategory.BUILD),
            CorporateTradePresentation.buyCategories,
        )

        val rows = CorporateTradePresentation.buyRows(state, ResourceCategory.FUEL)
        assertTrue(rows.first().reserveShortfall >= rows.last().reserveShortfall)
        assertTrue(rows.takeWhile { it.reserveShortfall > .0001 }.isNotEmpty())
    }

    @Test
    fun `colonists default to safe maximum and become unavailable when contract has ended`() {
        val projection = ColonistTransferProjection(
            supportedPopulationCapacity = 500,
            housingPowerRemaining = 200,
            passengerRemaining = 250,
            foodSupportedAdditional = 73,
            maxTransfer = 200,
            maxSafeTransfer = 73,
            unitCost = 100.0,
        )
        assertEquals(73, CorporateTradePresentation.defaultColonistAmount(projection))

        val active = EstablishedColonyFixture.contract01()
        assertTrue(CorporateTradePresentation.colonistsAvailable(active))
        val ended = active.withActiveColony { colony ->
            colony.copy(contract = requireNotNull(colony.contract).copy(ended = true))
        }
        assertFalse(CorporateTradePresentation.colonistsAvailable(ended))
    }

    private fun com.mineit.android.domain.model.GameState.withActiveColony(
        transform: (com.mineit.android.domain.model.ColonyState) -> com.mineit.android.domain.model.ColonyState,
    ): com.mineit.android.domain.model.GameState {
        val updated = transform(activeColony)
        return copy(colonies = colonies.map { if (it.id == updated.id) updated else it })
    }
}
