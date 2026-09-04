package com.mineit.android.domain.resources

import com.mineit.android.domain.model.ResourceId
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class ResourceCategory {
    @SerialName("food") FOOD,
    @SerialName("build") BUILD,
    @SerialName("fuel") FUEL,
    @SerialName("ore") ORE,
}

@Serializable
enum class QualityBand {
    @SerialName("common") COMMON,
    @SerialName("good") GOOD,
    @SerialName("excellent") EXCELLENT,
    @SerialName("exceptional") EXCEPTIONAL,
    @SerialName("rare") RARE,
    @SerialName("extraordinary") EXTRAORDINARY,
}

@Serializable
data class ResourceStock(
    val resourceId: ResourceId,
    val category: ResourceCategory,
    val qualityBands: Map<QualityBand, Double>,
) {
    init {
        require(qualityBands.values.all { it.isFinite() && it >= 0.0 }) {
            "Resource quality-band quantities must be finite and non-negative."
        }
    }

    val amount: Double
        get() = qualityBands.values.sum()
}

@Serializable
data class Inventory(
    val resources: List<ResourceStock> = emptyList(),
) {
    init {
        require(resources.map { it.resourceId }.distinct().size == resources.size) {
            "Inventory may contain only one entry for each ResourceId."
        }
    }

    fun find(resourceId: ResourceId): ResourceStock? = resources.firstOrNull { it.resourceId == resourceId }

    fun amountFor(resourceId: ResourceId): Double = find(resourceId)?.amount ?: 0.0

    fun amountFor(category: ResourceCategory): Double = resources
        .asSequence()
        .filter { it.category == category }
        .sumOf { it.amount }
}
