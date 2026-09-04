package com.mineit.android.domain.resources

import com.mineit.android.domain.model.ResourceId

enum class ExtractionFamily {
    FARM,
    RANCH,
    BIO,
    ALGAE,
    QUARRY,
    RIG,
    MINE,
    DEEP_MINE,
}

/**
 * Canonical native owner for the current resource-to-extractor mapping.
 * This preserves web behaviour while removing the duplicated mapping from future native services.
 */
object ExtractionCompatibility {
    fun familyFor(definition: ResourceDefinition): ExtractionFamily = when (definition.category) {
        ResourceCategory.FOOD -> when (definition.id.value) {
            "herd" -> ExtractionFamily.RANCH
            "thermal" -> ExtractionFamily.ALGAE
            "fungal", "protein" -> ExtractionFamily.BIO
            else -> ExtractionFamily.FARM
        }

        ResourceCategory.BUILD -> ExtractionFamily.QUARRY
        ResourceCategory.FUEL -> when (definition.id.value) {
            "oil", "gas", "brine" -> ExtractionFamily.RIG
            else -> ExtractionFamily.MINE
        }

        ResourceCategory.ORE -> when (definition.id.value) {
            "diamond", "exotic", "crystal", "advanced" -> ExtractionFamily.DEEP_MINE
            else -> ExtractionFamily.MINE
        }
    }

    fun familyFor(resourceId: ResourceId): ExtractionFamily = familyFor(ResourceCatalogue.require(resourceId))
}
