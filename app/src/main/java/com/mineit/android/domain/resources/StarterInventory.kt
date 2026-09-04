package com.mineit.android.domain.resources

import com.mineit.android.domain.config.MineItConfig
import com.mineit.android.domain.model.ResourceId

object StarterInventory {
    fun contract01(): Inventory = Inventory(
        resources = listOf(
            stock("fungal", ResourceCategory.FOOD, MineItConfig.START_FOOD),
            stock("fiber", ResourceCategory.BUILD, MineItConfig.START_BUILD),
            stock("biomass", ResourceCategory.FUEL, MineItConfig.START_FUEL),
            stock("surface-iron", ResourceCategory.ORE, MineItConfig.START_ORE),
        ),
    )

    private fun stock(id: String, category: ResourceCategory, amount: Double) = ResourceStock(
        resourceId = ResourceId(id),
        category = category,
        qualityBands = mapOf(QualityBand.EXCELLENT to amount),
    )
}
