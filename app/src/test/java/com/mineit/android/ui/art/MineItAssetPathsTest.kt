package com.mineit.android.ui.art

import com.mineit.android.domain.model.ResourceId
import com.mineit.android.domain.resources.ResourceCategory
import com.mineit.android.domain.world.DevelopmentKind
import com.mineit.android.domain.world.ResourceDeposit
import com.mineit.android.domain.world.SectorCoordinate
import com.mineit.android.domain.world.Sustainability
import com.mineit.android.domain.world.TerrainType
import com.mineit.android.domain.world.TileDevelopment
import com.mineit.android.domain.world.WorldTile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MineItAssetPathsTest {
    @Test
    fun `terrain paths preserve canonical source families and four variants`() {
        assertEquals("art/terrain/plains/plains-01.webp", MineItAssetPaths.terrain(TerrainType.PLAIN, 1))
        assertEquals("art/terrain/hills/hills-02.webp", MineItAssetPaths.terrain(TerrainType.HILL, 2))
        assertEquals("art/terrain/mountains/mountains-03.webp", MineItAssetPaths.terrain(TerrainType.MOUNTAIN, 3))
        assertEquals("art/terrain/lakes/lakes-04.webp", MineItAssetPaths.terrain(TerrainType.LAKE, 4))
        assertEquals("art/terrain/plains/plains-01.webp", MineItAssetPaths.terrain(TerrainType.PLAIN, 5))
    }

    @Test
    fun `resource frame lookup matches pinned atlas manifest order`() {
        assertEquals(AtlasFrame(0, 0, 256, 256), MineItAssetPaths.resourceFrame("fungal"))
        assertEquals(AtlasFrame(0, 256, 256, 256), MineItAssetPaths.resourceFrame("stone"))
        assertEquals(AtlasFrame(1792, 1024, 256, 256), MineItAssetPaths.resourceFrame("crystal"))
        assertNull(MineItAssetPaths.resourceFrame("not-a-resource"))
    }

    @Test
    fun `development atlas maps current physical development families`() {
        val base = tile().copy(development = TileDevelopment(DevelopmentKind.HOUSING))
        assertEquals("art/development/housing/housing-levels-256.webp", MineItAssetPaths.developmentAtlas(base))

        val extraction = tile().copy(
            development = TileDevelopment(DevelopmentKind.EXTRACT),
            deposit = ResourceDeposit(
                resourceId = ResourceId("surface-iron"),
                category = ResourceCategory.ORE,
                name = "Surface Iron Nodules",
                rarity = "Common",
                multiplier = 1.0,
                quality = 500,
                requiredScanningLevel = 1,
                requiredMiningLevel = 1,
                requiredMiningTech = "Surface Recovery",
                terrainYieldFactor = 1.0,
                sustainability = Sustainability.FINITE,
                reserve = 1_000,
                initialReserve = 1_000,
            ),
        )
        assertEquals("art/development/mine/mine-levels-256.webp", MineItAssetPaths.developmentAtlas(extraction))

        assertNull(MineItAssetPaths.developmentAtlas(tile().copy(development = TileDevelopment(DevelopmentKind.POWER))))
        assertNull(MineItAssetPaths.developmentAtlas(tile().copy(development = TileDevelopment(DevelopmentKind.HEADQUARTERS))))
    }

    private fun tile() = WorldTile(
        coordinate = SectorCoordinate(-1, 1),
        terrain = TerrainType.PLAIN,
        terrainVariant = 1,
        revealed = true,
    )
}
