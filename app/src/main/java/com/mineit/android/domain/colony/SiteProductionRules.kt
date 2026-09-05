package com.mineit.android.domain.colony

import com.mineit.android.domain.config.MineItConfig
import com.mineit.android.domain.model.ColonyState
import com.mineit.android.domain.resources.ResourceCategory
import com.mineit.android.domain.world.Sustainability
import com.mineit.android.domain.world.WorldTile
import kotlin.math.max
import kotlin.math.pow

/**
 * Canonical extraction-throughput rules shared by simulation and player-facing projections.
 * Keeping the rate calculation here prevents detail panels from becoming a second gameplay owner.
 */
object SiteProductionRules {
    fun potential(
        colony: ColonyState,
        tile: WorldTile,
        levelOverride: Int? = null,
    ): Double {
        val deposit = requireNotNull(tile.deposit) { "Extraction production requires a resource deposit." }
        val level = levelOverride ?: tile.development?.level ?: 1
        val base = if (deposit.sustainability == Sustainability.RENEWABLE) {
            siteOutput(level) *
                renewableRateFactor(deposit.abundanceLabel) *
                deposit.terrainYieldFactor *
                deposit.harvestIntensity.coerceIn(.25, 2.0)
        } else {
            siteOutput(level) * finiteRateFactor(deposit.depositScale) * deposit.terrainYieldFactor
        }
        val technologyAdjusted = if (deposit.category == ResourceCategory.FOOD) {
            base * TechnologyCapabilities.foodProductionMultiplier(colony.technology)
        } else {
            base
        }
        return technologyAdjusted * ExtractionOverdriveRules.outputMultiplier(tile)
    }

    fun rate(
        colony: ColonyState,
        tile: WorldTile,
        network: ColonyNetworkSnapshot,
    ): Double {
        if (ExtractionOverdriveRules.isShutdown(tile)) return 0.0
        val deposit = requireNotNull(tile.deposit) { "Extraction production requires a resource deposit." }
        val workforce = if (deposit.category in survivalCategories) {
            network.workforceSurvivalFactor
        } else {
            network.workforceCommercialFactor
        }
        val industry = if (deposit.category in survivalCategories) {
            network.industrySurvivalFactor
        } else {
            network.industryCommercialFactor
        }
        val power = network.sitePowerFactors[siteId(tile)] ?: 1.0
        return max(
            0.0,
            potential(colony, tile) *
                workforce *
                industry *
                power *
                network.continuity.effectiveCommandEfficiency,
        )
    }

    private fun siteOutput(level: Int): Double {
        val normalized = max(1, level)
        if (normalized <= MineItConfig.SITE_OUTPUT_LEVELS.size) {
            return MineItConfig.SITE_OUTPUT_LEVELS[normalized - 1]
        }
        return MineItConfig.SITE_OUTPUT_LEVELS.last() *
            1.24.pow(normalized - MineItConfig.SITE_OUTPUT_LEVELS.size)
    }

    private fun finiteRateFactor(label: String?): Double = when (label?.lowercase()) {
        "small" -> .75
        "modest" -> .90
        "large" -> 1.05
        "huge" -> 1.20
        "colossal" -> 1.35
        else -> 1.0
    }

    private fun renewableRateFactor(label: String?): Double = when (label?.lowercase()) {
        "limited" -> .65
        "large" -> 1.45
        "vast" -> 2.10
        else -> 1.0
    }

    private fun siteId(tile: WorldTile): String = "site:${tile.coordinate.x},${tile.coordinate.y}"

    private val survivalCategories = setOf(ResourceCategory.FOOD, ResourceCategory.FUEL)
}
