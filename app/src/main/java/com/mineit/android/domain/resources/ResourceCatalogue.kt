package com.mineit.android.domain.resources

import com.mineit.android.domain.model.ResourceId

data class ResourceDefinition(
    val id: ResourceId,
    val category: ResourceCategory,
    val name: String,
    val rarity: String,
    val weight: Double,
    val multiplier: Double,
    val qualityBias: Double,
    val renewable: Boolean,
    val miningLevel: Int,
    val scanningLevel: Int,
    val unlock: String,
    val sellPrice: Double,
    val manufactured: Boolean = false,
)

/** Exact current resource catalogue from the pinned MineIT 5.13.15 baseline. */
object ResourceCatalogue {
    val all: List<ResourceDefinition> = listOf(
        resource("fungal", ResourceCategory.FOOD, "Fungal Shelf", "Common", 22.0, 1.00, .98, true, 1, 1, "Surface Recovery", .08),
        resource("flora", ResourceCategory.FOOD, "Edible Flora", "Common", 20.0, 1.05, 1.02, true, 1, 1, "Surface Recovery", .10),
        resource("herd", ResourceCategory.FOOD, "Grazing Herd", "Uncommon", 15.0, 1.16, 1.08, true, 1, 1, "Surface Recovery", .14),
        resource("nutrient", ResourceCategory.FOOD, "Nutrient Crop", "Uncommon", 15.0, 1.23, 1.10, true, 1, 1, "Surface Recovery", .12),
        resource("protein", ResourceCategory.FOOD, "Protein Bloom", "Rare", 10.0, 1.45, 1.20, true, 2, 1, "Quarrying & Field Processing", .20),
        resource("thermal", ResourceCategory.FOOD, "Thermal Algae", "Rare", 6.0, 1.70, 1.28, true, 3, 2, "Shaft Mining", .28),
        resource("synthetic", ResourceCategory.FOOD, "Synthetic Nutrient", "Manufactured", 0.0, 1.0, 1.0, true, 1, 1, "Food Production Tech", .12, manufactured = true),

        resource("fiber", ResourceCategory.BUILD, "Construction Fibre", "Common", 14.0, .90, .95, true, 1, 1, "Surface Recovery", .12),
        resource("stone", ResourceCategory.BUILD, "Stone", "Very Common", 26.0, 1.00, .92, false, 2, 1, "Quarrying", .10),
        resource("clay", ResourceCategory.BUILD, "Clay", "Common", 18.0, 1.05, .98, false, 2, 2, "Quarrying", .11),
        resource("silica", ResourceCategory.BUILD, "Silica", "Common", 16.0, 1.10, 1.00, false, 2, 2, "Quarrying", .14),
        resource("limestone", ResourceCategory.BUILD, "Limestone", "Common", 15.0, 1.08, .98, false, 2, 2, "Quarrying", .12),
        resource("structural", ResourceCategory.BUILD, "Structural Mineral", "Uncommon", 8.0, 1.35, 1.12, false, 3, 3, "Shaft Mining", .20),
        resource("ceramic", ResourceCategory.BUILD, "Advanced Ceramic Feedstock", "Rare", 3.0, 1.70, 1.25, false, 6, 6, "Precision Extraction", .34),

        resource("biomass", ResourceCategory.FUEL, "Biomass", "Common", 22.0, .75, .92, true, 1, 1, "Surface Recovery", .16),
        resource("peat", ResourceCategory.FUEL, "Peat Bed", "Common", 14.0, .95, .95, false, 2, 2, "Quarrying", .20),
        resource("coal", ResourceCategory.FUEL, "Coal Seam", "Common", 22.0, 1.15, 1.00, false, 3, 3, "Shaft Mining", .28),
        resource("oil", ResourceCategory.FUEL, "Crude Oil", "Uncommon", 14.0, 1.45, 1.10, false, 5, 5, "Rotary Drilling", .44),
        resource("gas", ResourceCategory.FUEL, "Natural Gas", "Uncommon", 12.0, 1.55, 1.12, false, 5, 5, "Rotary Drilling", .48),
        resource("fissile", ResourceCategory.FUEL, "Fissile Mineral", "Rare", 8.0, 1.80, 1.25, false, 6, 6, "Precision Extraction", .80),
        resource("brine", ResourceCategory.FUEL, "Hydrogen-rich Brine", "Rare", 6.0, 2.00, 1.30, false, 7, 7, "Pressure & Brine Drilling", .95),
        resource("exotic-fuel", ResourceCategory.FUEL, "Exotic Fuel Crystal", "Exceptional", 2.0, 3.00, 1.60, false, 9, 9, "Exotic Matter Separation", 2.20),

        resource("surface-iron", ResourceCategory.ORE, "Surface Iron Nodules", "Common", 18.0, .75, .90, false, 1, 1, "Surface Recovery", .30),
        resource("iron", ResourceCategory.ORE, "Iron Ore", "Common", 22.0, 1.00, .98, false, 3, 3, "Shaft Mining", .42),
        resource("copper", ResourceCategory.ORE, "Copper Ore", "Uncommon", 15.0, 1.18, 1.06, false, 3, 3, "Shaft Mining", .60),
        resource("reactive", ResourceCategory.ORE, "Reactive Metal Ore", "Uncommon", 9.0, 1.35, 1.14, false, 4, 4, "Deep Mining", .85),
        resource("conductive", ResourceCategory.ORE, "Conductive Ore", "Rare", 7.0, 1.55, 1.20, false, 4, 4, "Deep Mining", 1.10),
        resource("silver", ResourceCategory.ORE, "Silver", "Rare", 7.0, 1.70, 1.20, false, 4, 4, "Deep Mining", 2.50),
        resource("gold", ResourceCategory.ORE, "Gold", "Very Rare", 5.0, 2.20, 1.32, false, 4, 4, "Deep Mining", 5.00),
        resource("gems", ResourceCategory.ORE, "Gemstone Deposit", "Very Rare", 4.0, 2.60, 1.38, false, 4, 4, "Deep Mining", 6.50),
        resource("platinum", ResourceCategory.ORE, "Platinum", "Exceptional", 3.0, 3.00, 1.45, false, 6, 6, "Precision Extraction", 9.00),
        resource("palladium", ResourceCategory.ORE, "Palladium", "Exceptional", 2.5, 3.20, 1.48, false, 6, 6, "Precision Extraction", 10.00),
        resource("sapphire", ResourceCategory.ORE, "Sapphire", "Exceptional", 2.0, 3.30, 1.50, false, 6, 6, "Precision Extraction", 12.00),
        resource("ruby", ResourceCategory.ORE, "Ruby", "Exceptional", 1.7, 3.50, 1.55, false, 6, 6, "Precision Extraction", 14.00),
        resource("emerald", ResourceCategory.ORE, "Emerald", "Exceptional", 1.5, 3.60, 1.58, false, 6, 6, "Precision Extraction", 15.00),
        resource("diamond", ResourceCategory.ORE, "Diamond", "Ultra Rare", 1.2, 4.20, 1.70, false, 7, 7, "Pressure & Brine Drilling", 22.00),
        resource("magnetic", ResourceCategory.ORE, "Magnetic Ore", "Rare", 3.0, 2.10, 1.35, false, 6, 4, "Precision Extraction", 2.40),
        resource("exotic", ResourceCategory.ORE, "Exotic Industrial Mineral", "Ultra Rare", .8, 5.00, 1.75, false, 8, 8, "Deep-Core Extraction", 35.00),
        resource("crystal", ResourceCategory.ORE, "Exotic Crystal", "Ultra Rare", .5, 6.00, 1.90, false, 9, 9, "Exotic Matter Separation", 55.00),
        resource("advanced", ResourceCategory.ORE, "Advanced Element Deposit", "Unique", .3, 8.00, 2.00, false, 10, 10, "Quantum Bore Systems", 90.00),
    )

    private val byId = all.associateBy { it.id }

    fun get(id: ResourceId): ResourceDefinition? = byId[id]

    fun require(id: ResourceId): ResourceDefinition = requireNotNull(get(id)) {
        "Unknown resource id: ${id.value}"
    }

    fun byCategory(category: ResourceCategory): List<ResourceDefinition> = all.filter { it.category == category }

    private fun resource(
        id: String,
        category: ResourceCategory,
        name: String,
        rarity: String,
        weight: Double,
        multiplier: Double,
        qualityBias: Double,
        renewable: Boolean,
        miningLevel: Int,
        scanningLevel: Int,
        unlock: String,
        sellPrice: Double,
        manufactured: Boolean = false,
    ) = ResourceDefinition(
        id = ResourceId(id),
        category = category,
        name = name,
        rarity = rarity,
        weight = weight,
        multiplier = multiplier,
        qualityBias = qualityBias,
        renewable = renewable,
        miningLevel = miningLevel,
        scanningLevel = scanningLevel,
        unlock = unlock,
        sellPrice = sellPrice,
        manufactured = manufactured,
    )
}
