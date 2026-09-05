package com.mineit.android.domain.colony

import com.mineit.android.domain.config.MineItConfig
import com.mineit.android.domain.model.ColonyState
import com.mineit.android.domain.resources.ResourceCategory
import com.mineit.android.domain.world.ExtractionOperatingMode
import com.mineit.android.domain.world.Sustainability
import com.mineit.android.domain.world.WorldTile
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.round
import kotlin.math.pow

/** Shared canonical operational requirements for extraction sites. */
object SiteOperationRules {
    fun workforceRequirement(
        colony: ColonyState,
        tile: WorldTile,
        levelOverride: Int? = null,
        modeOverride: ExtractionOperatingMode? = null,
    ): Double {
        val deposit = requireNotNull(tile.deposit) { "Extraction site requires a resource deposit." }
        val level = (levelOverride ?: tile.development?.level ?: 1).coerceAtLeast(1)
        val complexity = 1.0 + .18 * (deposit.requiredMiningLevel - 1)
        val efficiency = if (deposit.category == ResourceCategory.FOOD) {
            TechnologyCapabilities.foodWorkforceEfficiency(colony.technology)
        } else {
            TechnologyCapabilities.miningWorkforceEfficiency(colony.technology)
        }
        val intensity = if (
            deposit.sustainability == Sustainability.RENEWABLE ||
            deposit.category == ResourceCategory.FOOD ||
            deposit.resourceId.value == "biomass" ||
            deposit.resourceId.value == "fiber"
        ) deposit.harvestIntensity.coerceIn(.25, 2.0) else 1.0
        val intensityFactor = .5 + .5 * intensity
        return max(
            1.0,
            ceil(
                MineItConfig.SITE_WORKFORCE_BASE *
                    MineItConfig.SITE_WORKFORCE_GROWTH.pow(level - 1) *
                    complexity * efficiency * intensityFactor *
                    ExtractionOverdriveRules.workforceMultiplier(tile, modeOverride),
            ),
        )
    }

    fun industryLoad(level: Int): Double = round(
        MineItConfig.INDUSTRY_SITE_LOAD_BASE * MineItConfig.INDUSTRY_SITE_LOAD_GROWTH.pow(level.coerceAtLeast(1) - 1),
    )
}
