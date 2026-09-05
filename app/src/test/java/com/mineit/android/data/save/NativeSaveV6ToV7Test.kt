package com.mineit.android.data.save

import com.mineit.android.domain.world.DevelopmentKind
import com.mineit.android.domain.world.ExtractionOperatingMode
import com.mineit.android.domain.world.SectorCoordinate
import com.mineit.android.domain.world.TileDevelopment
import com.mineit.android.testing.EstablishedColonyFixture
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.encodeToJsonElement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class NativeSaveV6ToV7Test {
    @Test
    fun `v6 save migrates with safe default extraction overdrive state`() {
        val coordinate = SectorCoordinate(-4, -4)
        var state = EstablishedColonyFixture.contract01()
        val colony = state.activeColony
        val tile = requireNotNull(colony.world.tileAt(coordinate)).copy(
            revealed = true,
            development = TileDevelopment(DevelopmentKind.INDUSTRY, level = 2),
        )
        val nextColony = colony.copy(world = colony.world.copy(tiles = colony.world.tiles.map { if (it.coordinate == coordinate) tile else it }))
        state = state.copy(colonies = state.colonies.map { if (it.id == nextColony.id) nextColony else it })

        val json = Json { encodeDefaults = false }
        val raw = JsonObject(
            mapOf(
                "formatVersion" to JsonPrimitive(6),
                "gameVersion" to JsonPrimitive("0.7.3-migration"),
                "savedAtEpochMillis" to JsonPrimitive(1L),
                "state" to json.encodeToJsonElement(state),
            ),
        ).toString()

        val decoded = SaveCodec().decode(raw)
        val development = decoded.state.activeColony.world.tileAt(coordinate)?.development

        assertEquals(7, decoded.formatVersion)
        assertNotNull(development)
        assertEquals(ExtractionOperatingMode.NORMAL, development?.operatingMode)
        assertEquals(0.0, development?.overdriveExposure ?: -1.0, 0.0)
        assertEquals(0, development?.accidentShutdownDays)
    }
}
