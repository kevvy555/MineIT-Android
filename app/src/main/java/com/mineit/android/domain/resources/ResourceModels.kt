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

object ResourceQuality {
    data class Details(
        val band: QualityBand,
        val minimum: Int,
        val maximum: Int,
        val valueMultiplier: Double,
    )

    val bands: List<Details> = listOf(
        Details(QualityBand.COMMON, 1, 50, .75),
        Details(QualityBand.GOOD, 51, 200, .90),
        Details(QualityBand.EXCELLENT, 201, 750, 1.00),
        Details(QualityBand.EXCEPTIONAL, 751, 2_500, 1.25),
        Details(QualityBand.RARE, 2_501, 7_500, 1.75),
        Details(QualityBand.EXTRAORDINARY, 7_501, 10_000, 3.00),
    )

    fun forQuality(quality: Int): Details {
        val normalized = quality.coerceIn(1, 10_000)
        return bands.first { normalized <= it.maximum }
    }

    fun forBand(band: QualityBand): Details = bands.first { it.band == band }
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

data class ResourceConsumption(
    val requested: Double,
    val consumed: Double,
    val inventory: Inventory,
) {
    val ratio: Double
        get() = if (requested > 0.0) (consumed / requested).coerceIn(0.0, 1.0) else 1.0
}

data class ResourceWithdrawal(
    val requested: Double,
    val stock: ResourceStock?,
    val inventory: Inventory,
)

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

    fun store(
        resourceId: ResourceId,
        category: ResourceCategory,
        amount: Double,
        quality: Int,
    ): Inventory {
        if (!amount.isFinite() || amount <= 0.0) return this
        return store(ResourceStock(resourceId, category, mapOf(ResourceQuality.forQuality(quality).band to amount)))
    }

    /** Merge an exact stock lot, preserving every quality-band quantity. */
    fun store(stock: ResourceStock): Inventory {
        if (stock.amount <= .0001) return this
        val existing = find(stock.resourceId)
        require(existing == null || existing.category == stock.category) {
            "Resource ${stock.resourceId.value} cannot change inventory category."
        }
        val updated = if (existing == null) {
            stock
        } else {
            existing.copy(
                qualityBands = (existing.qualityBands.keys + stock.qualityBands.keys).associateWith { band ->
                    (existing.qualityBands[band] ?: 0.0) + (stock.qualityBands[band] ?: 0.0)
                },
            )
        }
        return copy(resources = resources.filterNot { it.resourceId == stock.resourceId } + updated)
    }

    /**
     * Remove one resource while preserving the exact quality bands moved to another inventory.
     * Lowest quality moves first, matching the conservative stock-use ordering used elsewhere.
     */
    fun withdraw(resourceId: ResourceId, requested: Double): ResourceWithdrawal {
        val normalized = requested.takeIf { it.isFinite() }?.coerceAtLeast(0.0) ?: 0.0
        val existing = find(resourceId)
        if (normalized <= 0.0 || existing == null || existing.amount <= .0001) {
            return ResourceWithdrawal(normalized, null, this)
        }
        var remaining = minOf(normalized, existing.amount)
        val sourceBands = existing.qualityBands.toMutableMap()
        val movedBands = linkedMapOf<QualityBand, Double>()
        existing.qualityBands.keys
            .sortedBy { ResourceQuality.forBand(it).minimum }
            .forEach { band ->
                if (remaining <= .0001) return@forEach
                val available = sourceBands[band] ?: 0.0
                val moved = minOf(available, remaining)
                if (moved > .0001) {
                    sourceBands[band] = (available - moved).coerceAtLeast(0.0)
                    movedBands[band] = moved
                    remaining -= moved
                }
            }
        val updated = existing.copy(qualityBands = sourceBands)
        return ResourceWithdrawal(
            requested = normalized,
            stock = ResourceStock(existing.resourceId, existing.category, movedBands),
            inventory = copy(resources = resources.map { if (it.resourceId == resourceId) updated else it }),
        )
    }

    /**
     * Canonical category consumption: consume the lowest-value available lots first,
     * matching the web InventoryService ordering by sale value then quality band.
     */
    fun consumeCategory(category: ResourceCategory, requested: Double): ResourceConsumption {
        val normalized = requested.takeIf { it.isFinite() }?.coerceAtLeast(0.0) ?: 0.0
        if (normalized <= 0.0) return ResourceConsumption(normalized, 0.0, this)

        data class Lot(
            val resourceId: ResourceId,
            val band: QualityBand,
            val unitValue: Double,
            val bandMinimum: Int,
        )

        val lots = resources
            .filter { it.category == category }
            .flatMap { stock ->
                val definition = ResourceCatalogue.get(stock.resourceId)
                stock.qualityBands
                    .filterValues { it > .0001 }
                    .map { (band, _) ->
                        val quality = ResourceQuality.forBand(band)
                        Lot(
                            resourceId = stock.resourceId,
                            band = band,
                            unitValue = (definition?.sellPrice ?: fallbackSellPrice(category)) *
                                RESOURCE_VALUE_SCALE * quality.valueMultiplier,
                            bandMinimum = quality.minimum,
                        )
                    }
            }
            .sortedWith(compareBy<Lot> { it.unitValue }.thenBy { it.bandMinimum })

        var remaining = normalized
        var consumed = 0.0
        val mutable = resources.associateBy { it.resourceId }.toMutableMap()
        for (lot in lots) {
            if (remaining <= .0001) break
            val stock = mutable[lot.resourceId] ?: continue
            val available = stock.qualityBands[lot.band] ?: 0.0
            val take = minOf(available, remaining)
            if (take <= 0.0) continue
            val nextBands = stock.qualityBands + (lot.band to (available - take).coerceAtLeast(0.0))
            mutable[lot.resourceId] = stock.copy(qualityBands = nextBands)
            consumed += take
            remaining -= take
        }

        return ResourceConsumption(
            requested = normalized,
            consumed = consumed,
            inventory = copy(resources = resources.map { mutable.getValue(it.resourceId) }),
        )
    }

    private fun fallbackSellPrice(category: ResourceCategory): Double = when (category) {
        ResourceCategory.FOOD -> .08
        ResourceCategory.BUILD -> .12
        ResourceCategory.FUEL -> .20
        ResourceCategory.ORE -> .40
    }

    companion object {
        private const val RESOURCE_VALUE_SCALE = 5.0
    }
}
