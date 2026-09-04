package com.mineit.android.domain.model

import com.mineit.android.domain.resources.Inventory
import com.mineit.android.domain.resources.QualityBand
import com.mineit.android.domain.resources.ResourceCategory
import com.mineit.android.domain.resources.ResourceStock
import com.mineit.android.testing.TestGameStates
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class FoundationModelTest {
    @Test
    fun `game date uses canonical 360 day MineIT year`() {
        val date = GameDate(year = 4, day = 360)

        assertEquals(date, GameDate.fromAbsoluteDay(date.toAbsoluteDay()))
        assertEquals(1440, date.toAbsoluteDay().value)
        assertEquals(GameDate(year = 2, day = 1), GameDate.fromAbsoluteDay(AbsoluteDay(361)))
        assertEquals(GameDate(year = 5, day = 1), date.nextDay())
    }

    @Test
    fun `resource amount is derived from quality bands`() {
        val stock = TestGameStates.foundationState().activeColony.inventory
            .find(ResourceId("fungal"))!!

        assertEquals(425.0, stock.amount, 0.0001)
    }

    @Test
    fun `inventory rejects duplicate resource identity`() {
        val first = ResourceStock(
            resourceId = ResourceId("surface-iron"),
            category = ResourceCategory.ORE,
            qualityBands = mapOf(QualityBand.EXCELLENT to 10.0),
        )
        val second = first.copy(qualityBands = mapOf(QualityBand.RARE to 5.0))

        assertThrows(IllegalArgumentException::class.java) {
            Inventory(listOf(first, second))
        }
    }

    @Test
    fun `game state requires its active colony to exist`() {
        val state = TestGameStates.foundationState()

        assertThrows(IllegalArgumentException::class.java) {
            state.copy(activeColonyId = ColonyId("missing-colony"))
        }
    }
}
