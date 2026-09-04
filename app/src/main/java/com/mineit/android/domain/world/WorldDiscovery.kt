package com.mineit.android.domain.world

import com.mineit.android.domain.config.MineItConfig
import com.mineit.android.domain.contracts.ContractState
import com.mineit.android.domain.model.ResourceId
import com.mineit.android.domain.resources.ResourceCatalogue
import com.mineit.android.domain.resources.ResourceCategory
import com.mineit.android.domain.resources.ResourceDefinition
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

/** Deterministic port of MineIT web surface/deep resource discovery. */
class WorldDiscovery(
    private val landGenerator: LandGenerator = LandGenerator(),
) {
    fun reveal(
        colonySeed: Long,
        contract: ContractState,
        tile: WorldTile,
        scanningLevel: Int,
    ): WorldTile {
        val level = max(1, scanningLevel)
        if (tile.resourceExhausted) return clear(tile, level)
        if (tile.revealed && tile.lastScannedAtLevel >= level) return tile
        if (tile.revealed && tile.deposit != null) {
            return tile.copy(lastScannedAtLevel = level)
        }

        val truth = resourceTruth(colonySeed, contract, tile)
        return if (truth == null || level < truth.requiredScanningLevel) {
            clear(tile, level)
        } else {
            tile.copy(
                revealed = true,
                lastScannedAtLevel = level,
                deposit = truth,
            )
        }
    }

    fun resourceTruth(colonySeed: Long, contract: ContractState, tile: WorldTile): ResourceDeposit? =
        surfaceTruth(colonySeed, contract, tile) ?: deepTruth(colonySeed, contract, tile)

    private fun surfaceTruth(colonySeed: Long, contract: ContractState, tile: WorldTile): ResourceDeposit? {
        val random = DeterministicRandom(seed(colonySeed, contract.uid, tile.coordinate, "surface"))
        val chance = surfaceChance.getValue(tile.terrain)
        if (random.nextDouble() > chance) return null
        val picked = pickSurface(contract, tile, random) ?: return null
        return truthFromDefinition(contract, tile, picked.first, picked.second, random, 1.0)
    }

    private fun deepTruth(colonySeed: Long, contract: ContractState, tile: WorldTile): ResourceDeposit? {
        val random = DeterministicRandom(seed(colonySeed, contract.uid, tile.coordinate, "deep"))
        if (random.nextDouble() > .12) return null
        val eligible = deepPool.mapNotNull { item ->
            ResourceCatalogue.get(ResourceId(item.id))?.let { def -> item.copy(definition = def) }
        }
        val total = eligible.sumOf { it.weight }
        var roll = random.nextDouble() * total
        var picked = eligible.last()
        for (item in eligible) {
            roll -= item.weight
            if (roll <= 0) {
                picked = item
                break
            }
        }
        return truthFromDefinition(contract, tile, picked.category, requireNotNull(picked.definition), random, 1.08)
    }

    private fun pickSurface(
        contract: ContractState,
        tile: WorldTile,
        random: DeterministicRandom,
    ): Pair<ResourceCategory, ResourceDefinition>? {
        val weights = familyWeights(contract, tile.terrain)
        val families = ResourceCategory.entries.mapNotNull { category ->
            val ids = surfaceIds[tile.terrain]?.get(category).orEmpty()
            val definitions = ResourceCatalogue.byCategory(category).filter { definition ->
                definition.id.value in ids && definition.weight > 0 && !definition.manufactured
            }
            if (definitions.isEmpty()) null else SurfaceFamily(category, weights.getValue(category), definitions)
        }
        val total = families.sumOf { max(0.0, it.weight) }
        if (total <= 0) return null
        var roll = random.nextDouble() * total
        var family = families.last()
        for (candidate in families) {
            roll -= max(0.0, candidate.weight)
            if (roll <= 0) {
                family = candidate
                break
            }
        }
        return family.category to pickDefinition(family.definitions, random)
    }

    private fun familyWeights(contract: ContractState, terrain: TerrainType): Map<ResourceCategory, Double> {
        val terrainWeights = terrainWeights.getValue(terrain)
        return mapOf(
            ResourceCategory.FOOD to contract.resourceWeights.food * terrainWeights.getValue(ResourceCategory.FOOD),
            ResourceCategory.BUILD to contract.resourceWeights.build * terrainWeights.getValue(ResourceCategory.BUILD),
            ResourceCategory.FUEL to contract.resourceWeights.fuel * terrainWeights.getValue(ResourceCategory.FUEL),
            ResourceCategory.ORE to contract.resourceWeights.ore * terrainWeights.getValue(ResourceCategory.ORE),
        )
    }

    private fun pickDefinition(definitions: List<ResourceDefinition>, random: DeterministicRandom): ResourceDefinition {
        val eligible = definitions.filter { it.weight > 0 }
        val total = eligible.sumOf { it.weight }
        var roll = random.nextDouble() * total
        for (definition in eligible) {
            roll -= definition.weight
            if (roll <= 0) return definition
        }
        return eligible.last()
    }

    private fun truthFromDefinition(
        contract: ContractState,
        tile: WorldTile,
        category: ResourceCategory,
        definition: ResourceDefinition,
        random: DeterministicRandom,
        rareMultiplier: Double,
    ): ResourceDeposit {
        val quality = quality(random, definition.qualityBias, contract.rareMultiplier * rareMultiplier)
        val terrainYield = landGenerator.terrainYieldFactor(tile.terrain, category)
        val base = ResourceDeposit(
            resourceId = definition.id,
            category = category,
            name = definition.name,
            rarity = definition.rarity,
            multiplier = definition.multiplier,
            quality = quality,
            requiredScanningLevel = max(1, definition.scanningLevel),
            requiredMiningLevel = definition.miningLevel,
            requiredMiningTech = definition.unlock,
            terrainYieldFactor = terrainYield,
            sustainability = if (definition.renewable) Sustainability.RENEWABLE else Sustainability.FINITE,
        )

        if (definition.renewable) {
            val abundance = renewableAbundance(random)
            val rank = renewableRank(abundance.label)
            return base.copy(
                abundance = abundance.factor,
                abundanceLabel = abundance.label,
                renewableOriginalRank = rank,
                renewableHealth = rank + 1.0,
                harvestIntensity = 1.0,
            )
        }

        val deposit = finiteDeposit(random, contract.reserveMultiplier, category)
        val baseline = MineItConfig.SITE_OUTPUT_LEVELS.first() * finiteRateFactor(deposit.label) * terrainYield
        val reserve = jsRoundToLong(baseline * MineItConfig.DAYS_PER_YEAR * deposit.years).coerceAtLeast(1)
        return base.copy(
            depositScale = deposit.label,
            reserve = reserve,
            initialReserve = reserve,
        )
    }

    private fun quality(random: DeterministicRandom, bias: Double, contractRare: Double): Int {
        val raw = random.nextDouble()
        val adjustedBias = clamp(bias * sqrt(contractRare), .55, 2.4)
        val z = 1 - (1 - raw).pow(adjustedBias)
        val quality = when {
            z < .60 -> 1 + floor(random.nextDouble() * 100).toInt()
            z < .85 -> 101 + floor(random.nextDouble() * 400).toInt()
            z < .95 -> 501 + floor(random.nextDouble() * 1_500).toInt()
            z < .99 -> 2_001 + floor(random.nextDouble() * 3_000).toInt()
            z < .999 -> 5_001 + floor(random.nextDouble() * 4_000).toInt()
            else -> 9_001 + floor(random.nextDouble() * 1_000).toInt()
        }
        return quality.coerceIn(1, 10_000)
    }

    private fun renewableAbundance(random: DeterministicRandom): Abundance {
        val roll = random.nextDouble()
        return when {
            roll < .52 -> Abundance("Limited", .65)
            roll < .82 -> Abundance("Established", 1.0)
            roll < .96 -> Abundance("Large", 1.45)
            else -> Abundance("Vast", 2.10)
        }
    }

    private fun renewableRank(label: String): Int = when (label.lowercase()) {
        "limited" -> 0
        "large" -> 2
        "vast" -> 3
        else -> 1
    }

    private fun finiteDeposit(
        random: DeterministicRandom,
        reserveBias: Double,
        category: ResourceCategory,
    ): FiniteDeposit {
        val roll = random.nextDouble()
        val (label, baseYears) = when {
            roll < .16 -> "Small" to (1.5 + random.nextDouble() * 5.5)
            roll < .52 -> "Modest" to (7 + random.nextDouble() * 20)
            roll < .80 -> "Large" to (27 + random.nextDouble() * 55)
            roll < .95 -> "Huge" to (82 + random.nextDouble() * 120)
            else -> "Colossal" to (202 + random.nextDouble() * 350)
        }
        val oreFactor = if (category == ResourceCategory.ORE) .92 else 1.0
        return FiniteDeposit(label, max(.5, baseYears * reserveBias * oreFactor))
    }

    private fun finiteRateFactor(label: String): Double = when (label.lowercase()) {
        "small" -> .75
        "modest" -> .90
        "large" -> 1.05
        "huge" -> 1.20
        "colossal" -> 1.35
        else -> 1.0
    }

    private fun clear(tile: WorldTile, level: Int): WorldTile = tile.copy(
        revealed = true,
        lastScannedAtLevel = max(1, level),
        deposit = null,
    )

    private fun seed(seed: Long, contractUid: String, coordinate: SectorCoordinate, suffix: String): UInt =
        DeterministicHash.hashString("$seed|$contractUid|${coordinate.x}|${coordinate.y}|$suffix")

    private fun clamp(value: Double, minimum: Double, maximum: Double): Double = min(maximum, max(minimum, value))

    private fun jsRoundToLong(value: Double): Long = floor(value + .5).toLong()

    private data class SurfaceFamily(
        val category: ResourceCategory,
        val weight: Double,
        val definitions: List<ResourceDefinition>,
    )

    private data class Abundance(val label: String, val factor: Double)
    private data class FiniteDeposit(val label: String, val years: Double)
    private data class DeepPoolItem(
        val category: ResourceCategory,
        val id: String,
        val weight: Double,
        val definition: ResourceDefinition? = null,
    )

    companion object {
        private val surfaceChance = mapOf(
            TerrainType.PLAIN to .44,
            TerrainType.HILL to .60,
            TerrainType.MOUNTAIN to .72,
            TerrainType.LAKE to .52,
        )

        private fun weights(food: Double, build: Double, fuel: Double, ore: Double) = mapOf(
            ResourceCategory.FOOD to food,
            ResourceCategory.BUILD to build,
            ResourceCategory.FUEL to fuel,
            ResourceCategory.ORE to ore,
        )

        private val terrainWeights = mapOf(
            TerrainType.PLAIN to weights(1.75, .85, .70, .38),
            TerrainType.HILL to weights(.58, 1.75, .72, 1.05),
            TerrainType.MOUNTAIN to weights(.12, .86, .82, 2.35),
            TerrainType.LAKE to weights(2.25, .42, .62, .10),
        )

        private val surfaceIds = mapOf(
            TerrainType.PLAIN to mapOf(
                ResourceCategory.FOOD to setOf("fungal", "flora", "herd", "nutrient", "protein"),
                ResourceCategory.BUILD to setOf("fiber", "stone", "clay", "silica", "limestone"),
                ResourceCategory.FUEL to setOf("biomass", "peat"),
                ResourceCategory.ORE to setOf("surface-iron"),
            ),
            TerrainType.HILL to mapOf(
                ResourceCategory.FOOD to setOf("fungal", "flora", "herd", "protein"),
                ResourceCategory.BUILD to setOf("fiber", "stone", "clay", "silica", "limestone", "structural", "ceramic"),
                ResourceCategory.FUEL to setOf("biomass", "peat", "coal"),
                ResourceCategory.ORE to setOf("surface-iron", "iron", "copper", "reactive", "conductive", "silver"),
            ),
            TerrainType.MOUNTAIN to mapOf(
                ResourceCategory.FOOD to setOf("fungal"),
                ResourceCategory.BUILD to setOf("stone", "silica", "limestone", "structural", "ceramic"),
                ResourceCategory.FUEL to setOf("coal", "fissile"),
                ResourceCategory.ORE to setOf("surface-iron", "iron", "copper", "reactive", "conductive", "silver", "gold", "gems", "platinum", "palladium", "sapphire", "ruby", "emerald", "magnetic"),
            ),
            TerrainType.LAKE to mapOf(
                ResourceCategory.FOOD to setOf("flora", "protein", "thermal"),
                ResourceCategory.BUILD to setOf("clay", "silica"),
                ResourceCategory.FUEL to setOf("biomass", "peat"),
                ResourceCategory.ORE to setOf("surface-iron"),
            ),
        )

        private val deepPool = listOf(
            DeepPoolItem(ResourceCategory.FUEL, "oil", 18.0),
            DeepPoolItem(ResourceCategory.FUEL, "gas", 18.0),
            DeepPoolItem(ResourceCategory.FUEL, "brine", 8.0),
            DeepPoolItem(ResourceCategory.FUEL, "exotic-fuel", 2.0),
            DeepPoolItem(ResourceCategory.ORE, "iron", 14.0),
            DeepPoolItem(ResourceCategory.ORE, "copper", 11.0),
            DeepPoolItem(ResourceCategory.ORE, "reactive", 9.0),
            DeepPoolItem(ResourceCategory.ORE, "conductive", 7.0),
            DeepPoolItem(ResourceCategory.ORE, "gold", 5.0),
            DeepPoolItem(ResourceCategory.ORE, "magnetic", 6.0),
            DeepPoolItem(ResourceCategory.ORE, "diamond", 4.0),
            DeepPoolItem(ResourceCategory.ORE, "exotic", 2.0),
            DeepPoolItem(ResourceCategory.ORE, "crystal", 1.0),
            DeepPoolItem(ResourceCategory.ORE, "advanced", .5),
        )
    }
}
