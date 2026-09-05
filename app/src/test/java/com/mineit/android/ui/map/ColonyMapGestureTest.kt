package com.mineit.android.ui.map

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import com.mineit.android.domain.world.SectorCoordinate
import com.mineit.android.domain.world.TerrainType
import com.mineit.android.domain.world.WorldTile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ColonyMapGestureTest {
    @Test
    fun `pixel coordinates map to the canonical eight by eight sector grid`() {
        val size = IntSize(800, 800)

        assertEquals(SectorCoordinate(-4, -4), coordinateAt(Offset(0f, 0f), size))
        assertEquals(SectorCoordinate(0, 0), coordinateAt(Offset(450f, 450f), size))
        assertEquals(SectorCoordinate(3, 3), coordinateAt(Offset(799f, 799f), size))
    }

    @Test
    fun `coordinates outside the map do not enter multi selection`() {
        val size = IntSize(800, 800)

        assertNull(coordinateAt(Offset(-1f, 10f), size))
        assertNull(coordinateAt(Offset(10f, -1f), size))
        assertNull(coordinateAt(Offset(800f, 10f), size))
        assertNull(coordinateAt(Offset(10f, 800f), size))
    }

    @Test
    fun `yellow resurvey opportunity is derived from last scan level not hidden resource truth`() {
        val coordinate = SectorCoordinate(-2, 1)
        val clearOldScan = WorldTile(
            coordinate = coordinate,
            terrain = TerrainType.PLAIN,
            terrainVariant = 1,
            revealed = true,
            lastScannedAtLevel = 2,
            deposit = null,
        )

        assertTrue(isResurveyAvailable(clearOldScan, scanningLevel = 3))
        assertFalse(isResurveyAvailable(clearOldScan, scanningLevel = 2))
        assertFalse(isResurveyAvailable(clearOldScan.copy(lastScannedAtLevel = 0), scanningLevel = 3))
        assertFalse(isResurveyAvailable(clearOldScan.copy(coordinate = SectorCoordinate(0, 0)), scanningLevel = 3))
    }
}
