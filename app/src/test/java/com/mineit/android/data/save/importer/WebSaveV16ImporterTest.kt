package com.mineit.android.data.save.importer

import com.mineit.android.domain.model.ResourceId
import com.mineit.android.domain.resources.QualityBand
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Test

class WebSaveV16ImporterTest {
    @Test
    fun `representative web v16 save is parsed through isolated import boundary`() {
        val fixtureRaw = requireNotNull(javaClass.classLoader?.getResourceAsStream(
            "parity/web-v16/representative-multi-colony.json",
        )).bufferedReader().use { it.readText() }
        val fixture = Json.parseToJsonElement(fixtureRaw).jsonObject
        val webState = fixture.getValue("state").jsonObject.toString()

        val preview = WebSaveV16Importer().parse(webState)

        assertEquals(16, preview.sourceVersion)
        assertEquals(3, preview.date.year)
        assertEquals(42, preview.date.day)
        assertEquals("fixture-colony-1", preview.activeColonyId.value)
        assertEquals("Koplin Prospect", preview.activeColonyName)
        assertEquals(120, preview.activePopulation)
        assertEquals(123_456_789L, preview.activeColonySeed)
        assertEquals(987_654L, preview.companyCash)
        assertEquals(24, preview.companyReputation)
        assertEquals(2, preview.colonyCount)
        assertEquals("temperate", preview.contractArchetypeId)
        assertEquals(2, preview.tileCount)
        assertEquals(2, preview.revealedTileCount)
        assertEquals(1, preview.activeSurveyCount)
        assertEquals(1, preview.surveyQueueCount)

        val fungal = preview.activeInventory.find(ResourceId("fungal"))!!
        assertEquals(425.0, fungal.amount, 0.0001)
        assertEquals(350.0, fungal.qualityBands.getValue(QualityBand.EXCELLENT), 0.0001)
        assertEquals(75.0, fungal.qualityBands.getValue(QualityBand.RARE), 0.0001)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `unsupported web version is rejected explicitly`() {
        WebSaveV16Importer().parse(
            """{
                "version":15,
                "year":1,
                "day":1,
                "colonyId":"one",
                "seed":1,
                "pop":1,
                "company":{"cash":1},
                "portfolio":{"activeColonyId":"one","colonies":[{"id":"one","name":"One"}]},
                "inventory":{},
                "contract":{"arch":"temperate"},
                "tiles":{},
                "scans":[],
                "scanQueue":[]
            }""",
        )
    }
}
