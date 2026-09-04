package com.mineit.android.migration

import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ParityFixtureLoaderTest {
    @Test
    fun `loads representative MineIT web v16 fixture`() {
        val fixture = ParityFixtureLoader.load(
            "parity/web-v16/representative-multi-colony.json",
        )

        assertEquals("representative-multi-colony-v16", fixture.name)
        assertEquals("kevvy555/MineIT", fixture.sourceRepository)
        assertEquals("9e58983adaa7a15cd525451266ce9df3c17ae886", fixture.sourceCommit)
        assertEquals("5.13.15", fixture.sourceGameVersion)
        assertEquals(16, fixture.sourceSaveVersion)
        assertEquals(16, fixture.state.getValue("version").jsonPrimitive.content.toInt())

        val portfolio = fixture.state.getValue("portfolio").jsonObject
        assertEquals(2, portfolio.getValue("colonies").jsonArray.size)

        val ships = fixture.state
            .getValue("company").jsonObject
            .getValue("expansion").jsonObject
            .getValue("ships").jsonArray
        assertEquals(1, ships.size)

        val inventory = fixture.state.getValue("inventory").jsonObject
        val food = inventory.getValue("food:fungal").jsonObject
        assertEquals(425, food.getValue("amount").jsonPrimitive.content.toInt())
        assertEquals(2, food.getValue("qualityBands").jsonObject.size)

        val tiles = fixture.state.getValue("tiles").jsonObject
        assertTrue(tiles.getValue("2,2").jsonObject.getValue("developed").jsonPrimitive.content.toBoolean())

        assertEquals(
            fixture.expectedSummary.getValue("activeColonyId").jsonPrimitive.content,
            portfolio.getValue("activeColonyId").jsonPrimitive.content,
        )
    }
}
