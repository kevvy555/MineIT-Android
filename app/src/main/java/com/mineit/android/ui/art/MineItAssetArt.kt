package com.mineit.android.ui.art

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import com.mineit.android.domain.resources.ExtractionCompatibility
import com.mineit.android.domain.resources.ExtractionFamily
import com.mineit.android.domain.world.DevelopmentKind
import com.mineit.android.domain.world.TerrainType
import com.mineit.android.domain.world.WorldTile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Presentation-only paths into the pinned MineIT web-art snapshot bundled under src/main/assets.
 * Gameplay code never depends on these paths.
 */
object MineItAssetPaths {
    const val RESOURCE_ATLAS = "art/resources/resource-atlas-256.webp"
    const val RESOURCE_FRAME_SIZE = 256
    const val RESOURCE_ATLAS_COLUMNS = 8

    fun terrain(terrain: TerrainType, variant: Int): String {
        val normalized = ((variant - 1).mod(4)) + 1
        val number = normalized.toString().padStart(2, '0')
        return when (terrain) {
            TerrainType.PLAIN -> "art/terrain/plains/plains-$number.webp"
            TerrainType.HILL -> "art/terrain/hills/hills-$number.webp"
            TerrainType.MOUNTAIN -> "art/terrain/mountains/mountains-$number.webp"
            TerrainType.LAKE -> "art/terrain/lakes/lakes-$number.webp"
        }
    }

    fun developmentAtlas(tile: WorldTile): String? {
        val development = tile.development ?: return null
        val folder = when (development.kind) {
            DevelopmentKind.HOUSING -> "housing"
            DevelopmentKind.INDUSTRY -> "industry"
            DevelopmentKind.EXTRACT -> extractionFolder(tile)
            DevelopmentKind.POWER,
            DevelopmentKind.HEADQUARTERS,
            -> null
        } ?: return null
        return "art/development/$folder/$folder-levels-256.webp"
    }

    private fun extractionFolder(tile: WorldTile): String? {
        val resourceId = tile.deposit?.resourceId ?: return null
        return when (ExtractionCompatibility.familyFor(resourceId)) {
            ExtractionFamily.FARM -> "farm"
            ExtractionFamily.RANCH -> "ranch"
            ExtractionFamily.BIO -> "bio-harvester"
            ExtractionFamily.ALGAE -> "algae-facility"
            ExtractionFamily.QUARRY -> "quarry"
            ExtractionFamily.RIG -> "rig"
            ExtractionFamily.MINE -> "mine"
            ExtractionFamily.DEEP_MINE -> "deep-mine"
        }
    }

    /** Atlas order is the canonical order recorded by resource-atlas-256.json. */
    private val resourceFrames = listOf(
        "fungal", "flora", "herd", "nutrient", "protein", "thermal", "synthetic", "fiber",
        "stone", "clay", "silica", "limestone", "structural", "ceramic", "biomass", "peat",
        "coal", "oil", "gas", "fissile", "brine", "exotic-fuel", "surface-iron", "iron",
        "copper", "reactive", "conductive", "magnetic", "exotic", "advanced", "silver", "gold",
        "gems", "platinum", "palladium", "sapphire", "ruby", "emerald", "diamond", "crystal",
    )
    private val resourceFrameIndex = resourceFrames.withIndex().associate { it.value to it.index }

    fun resourceFrame(resourceId: String): AtlasFrame? {
        val index = resourceFrameIndex[resourceId] ?: return null
        return AtlasFrame(
            x = (index % RESOURCE_ATLAS_COLUMNS) * RESOURCE_FRAME_SIZE,
            y = (index / RESOURCE_ATLAS_COLUMNS) * RESOURCE_FRAME_SIZE,
            width = RESOURCE_FRAME_SIZE,
            height = RESOURCE_FRAME_SIZE,
        )
    }
}

data class AtlasFrame(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
)

/**
 * Small process-local bitmap cache for immutable bundled art. Asset decoding is done off the main
 * thread and the cache prevents the 8×8 map from decoding the same terrain/atlas repeatedly.
 */
private object AssetBitmapCache {
    private const val MAX_KIB = 32 * 1024
    private val cache = object : LruCache<String, Bitmap>(MAX_KIB) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.allocationByteCount / 1024
    }

    fun load(context: Context, path: String): Bitmap? = synchronized(cache) {
        cache.get(path)?.let { return it }
        val bitmap = runCatching {
            context.assets.open(path).use(BitmapFactory::decodeStream)
        }.getOrNull() ?: return null
        cache.put(path, bitmap)
        bitmap
    }
}

@Composable
fun rememberMineItAssetBitmap(path: String?): ImageBitmap? {
    if (path == null) return null
    val context = LocalContext.current.applicationContext
    val image by produceState<ImageBitmap?>(initialValue = null, path) {
        value = withContext(Dispatchers.IO) {
            AssetBitmapCache.load(context, path)?.asImageBitmap()
        }
    }
    return image
}
