package com.mineit.android.domain.colony

import com.mineit.android.domain.config.MineItConfig
import com.mineit.android.domain.model.ColonyState
import com.mineit.android.domain.model.ColonyStatus
import com.mineit.android.domain.model.GameState
import com.mineit.android.domain.model.ResourceId
import com.mineit.android.domain.resources.ExtractionCompatibility
import com.mineit.android.domain.resources.Inventory
import com.mineit.android.domain.resources.QualityBand
import com.mineit.android.domain.resources.ResourceCategory
import com.mineit.android.domain.resources.ResourceQuality
import com.mineit.android.domain.ships.PlayerFleetService
import com.mineit.android.domain.world.DevelopmentKind
import com.mineit.android.domain.world.ResourceDeposit
import com.mineit.android.domain.world.SectorCoordinate
import com.mineit.android.domain.world.Sustainability
import com.mineit.android.domain.world.TerrainType
import com.mineit.android.domain.world.TileDevelopment
import com.mineit.android.domain.world.WorldTile
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.pow

/** Canonical immutable owner for local building/extraction construction, upgrades and demolition. */
class ColonyDevelopmentService(
    private val headquartersService: HeadquartersService = HeadquartersService(),
    private val fleetService: PlayerFleetService = PlayerFleetService(),
) {
    fun buildingPreview(state: GameState, coordinate: SectorCoordinate, kind: DevelopmentKind): DevelopmentPreview {
        if (kind == DevelopmentKind.EXTRACT) return DevelopmentPreview(false, "Use extraction development for resources.")
        val colony = state.activeColony
        val tile = colony.world.tileAt(coordinate) ?: return DevelopmentPreview(false, "Sector not found.")
        commonBuildBlock(colony, tile)?.let { return DevelopmentPreview(false, it) }
        if (tile.terrain == TerrainType.LAKE) return DevelopmentPreview(false, "Standard ${label(kind)} cannot be built on lakes.")
        if (tile.development != null) return DevelopmentPreview(false, "Demolish the existing development first.")
        val cost = InfrastructureRules.buildingCost(kind, 1, tile.terrain)
        resourceShortage(colony.inventory, cost.build, cost.ore)?.let { return DevelopmentPreview(false, it, cost = cost) }
        return DevelopmentPreview(true, cost = cost, nextLevel = 1, coversResource = tile.deposit != null)
    }

    fun placeBuilding(state: GameState, coordinate: SectorCoordinate, kind: DevelopmentKind): DevelopmentActionResult {
        val preview = buildingPreview(state, coordinate, kind)
        if (!preview.ok) return DevelopmentActionResult(false, state, preview.reason ?: "Building unavailable.")
        val colony = state.activeColony
        val tile = requireNotNull(colony.world.tileAt(coordinate))
        val cost = requireNotNull(preview.cost)
        val nextTile = tile.copy(
            resourceCovered = tile.deposit != null,
            development = TileDevelopment(kind, 1, investedBuild = cost.build, investedOre = cost.ore),
        )
        var next = state.withActiveColony(colony.copy(
            inventory = spend(colony.inventory, cost.build, cost.ore),
            world = colony.world.copy(tiles = replaceTile(colony.world.tiles, nextTile)),
        ))
        if (kind == DevelopmentKind.HEADQUARTERS) next = headquartersService.synchronizePrimary(next)
        return DevelopmentActionResult(true, next, "${label(kind)} L1 constructed.", cost = cost)
    }

    fun buildingUpgradePreview(state: GameState, coordinate: SectorCoordinate): DevelopmentPreview {
        val colony = state.activeColony
        val tile = colony.world.tileAt(coordinate) ?: return DevelopmentPreview(false, "Sector not found.")
        val dev = tile.development ?: return DevelopmentPreview(false, "Nothing has been constructed on this tile.")
        if (dev.kind == DevelopmentKind.EXTRACT) return extractionUpgradePreview(state, coordinate)
        if (colony.status == ColonyStatus.DEAD) return DevelopmentPreview(false, "This colony has been lost.")
        if (colony.contract?.ended == true) return DevelopmentPreview(false, "The mining contract has ended.")
        if (dev.level >= InfrastructureRules.MAX_LEVEL) return DevelopmentPreview(false, "${label(dev.kind)} is already at L5.", max = true)
        val nextLevel = dev.level + 1
        val tech = localTechnologyLevel(colony, dev.kind)
        if (dev.kind != DevelopmentKind.HEADQUARTERS && tech < nextLevel) {
            return DevelopmentPreview(false, "Requires ${label(dev.kind)} Tech L$nextLevel; this colony has deployed L$tech.", nextLevel = nextLevel)
        }
        val cost = InfrastructureRules.buildingCost(dev.kind, nextLevel, tile.terrain)
        resourceShortage(colony.inventory, cost.build, cost.ore)?.let {
            return DevelopmentPreview(false, it, cost = cost, nextLevel = nextLevel)
        }
        return DevelopmentPreview(true, cost = cost, nextLevel = nextLevel)
    }

    fun upgrade(state: GameState, coordinate: SectorCoordinate): DevelopmentActionResult {
        val colony = state.activeColony
        val tile = colony.world.tileAt(coordinate) ?: return DevelopmentActionResult(false, state, "Sector not found.")
        if (tile.development?.kind == DevelopmentKind.EXTRACT) return upgradeExtraction(state, coordinate)
        val preview = buildingUpgradePreview(state, coordinate)
        if (!preview.ok) return DevelopmentActionResult(false, state, preview.reason ?: "Upgrade unavailable.")
        val cost = requireNotNull(preview.cost)
        val dev = requireNotNull(tile.development)
        val nextTile = tile.copy(development = dev.copy(
            level = preview.nextLevel,
            investedBuild = dev.investedBuild + cost.build,
            investedOre = dev.investedOre + cost.ore,
        ))
        var next = state.withActiveColony(colony.copy(
            inventory = spend(colony.inventory, cost.build, cost.ore),
            world = colony.world.copy(tiles = replaceTile(colony.world.tiles, nextTile)),
        ))
        if (dev.kind == DevelopmentKind.HEADQUARTERS) next = headquartersService.synchronizePrimary(next)
        return DevelopmentActionResult(true, next, "${label(dev.kind)} upgraded to L${preview.nextLevel}.", cost = cost)
    }

    fun extractionPreview(state: GameState, coordinate: SectorCoordinate): DevelopmentPreview {
        val colony = state.activeColony
        val tile = colony.world.tileAt(coordinate) ?: return DevelopmentPreview(false, "Sector not found.")
        val deposit = tile.deposit ?: return DevelopmentPreview(false, "No exploitable resource on this tile.")
        commonBuildBlock(colony, tile)?.let { return DevelopmentPreview(false, it) }
        if (tile.development != null || tile.resourceExhausted || deposit.renewableWiped) return DevelopmentPreview(false, "Site unavailable.")
        if (tile.resourceCovered) return DevelopmentPreview(false, "The resource is covered by another development.")
        if (colony.technology.mining < deposit.requiredMiningLevel) {
            return DevelopmentPreview(false, "Requires Mining L${deposit.requiredMiningLevel}: ${deposit.requiredMiningTech}.")
        }
        val cost = InfrastructureCost(developBuildCost(colony, tile, deposit), 0.0)
        val workers = SiteOperationRules.workforceRequirement(colony, tile, 1)
        val free = freeWorkforce(state)
        if (free < workers) return DevelopmentPreview(false, "Need ${workers.toInt()} free operational workers; only ${floor(free).toInt()} are available.", cost = cost, workforce = workers)
        if (colony.inventory.amountFor(ResourceCategory.BUILD) + .0001 < cost.build) {
            return DevelopmentPreview(false, "Need ${cost.build.toInt()} Build materials.", cost = cost, workforce = workers)
        }
        return DevelopmentPreview(true, cost = cost, nextLevel = 1, workforce = workers)
    }

    fun developExtraction(state: GameState, coordinate: SectorCoordinate): DevelopmentActionResult {
        val preview = extractionPreview(state, coordinate)
        if (!preview.ok) return DevelopmentActionResult(false, state, preview.reason ?: "Extraction site unavailable.")
        val colony = state.activeColony
        val tile = requireNotNull(colony.world.tileAt(coordinate))
        val cost = requireNotNull(preview.cost)
        val nextTile = tile.copy(development = TileDevelopment(DevelopmentKind.EXTRACT, 1, investedBuild = cost.build))
        return DevelopmentActionResult(
            true,
            state.withActiveColony(colony.copy(
                inventory = spend(colony.inventory, cost.build, 0.0),
                world = colony.world.copy(tiles = replaceTile(colony.world.tiles, nextTile)),
            )),
            "${ExtractionCompatibility.familyFor(requireNotNull(tile.deposit).resourceId).displayName} L1 developed.",
            cost = cost,
        )
    }

    fun extractionUpgradePreview(state: GameState, coordinate: SectorCoordinate): DevelopmentPreview {
        val colony = state.activeColony
        val tile = colony.world.tileAt(coordinate) ?: return DevelopmentPreview(false, "Sector not found.")
        val dev = tile.development ?: return DevelopmentPreview(false, "Site unavailable.")
        val deposit = tile.deposit ?: return DevelopmentPreview(false, "Site unavailable.")
        if (dev.kind != DevelopmentKind.EXTRACT || tile.resourceExhausted || deposit.renewableWiped) return DevelopmentPreview(false, "Site unavailable.")
        if (dev.level >= InfrastructureRules.MAX_LEVEL) return DevelopmentPreview(false, "Extraction site is already at L5.", max = true)
        if (colony.contract?.ended == true) return DevelopmentPreview(false, "Mining contract ended. This colony is support-only.")
        if (colony.technology.mining < deposit.requiredMiningLevel) return DevelopmentPreview(false, "Requires Mining L${deposit.requiredMiningLevel}: ${deposit.requiredMiningTech}.")
        val nextLevel = dev.level + 1
        if (deposit.category == ResourceCategory.FOOD && colony.technology.food < nextLevel) {
            return DevelopmentPreview(false, "Food Production Tech L$nextLevel is required to upgrade this facility to L$nextLevel.", nextLevel = nextLevel)
        }
        val family = ExtractionCompatibility.familyFor(deposit.resourceId)
        val industryRequired = industryUpgradeRequirement(deposit.category, nextLevel)
        val powerRequired = InfrastructureRules.facilityUpgradePowerGate(family, nextLevel)
        val installedIndustry = installedIndustry(state)
        val installedPower = installedPower(colony)
        if (installedIndustry < industryRequired) return DevelopmentPreview(false, "Site L$nextLevel needs ${industryRequired.toInt()} installed Industry; colony has ${installedIndustry.toInt()}.", nextLevel = nextLevel)
        if (installedPower < powerRequired) return DevelopmentPreview(false, "${family.displayName} L$nextLevel needs ${powerRequired.toInt()} installed Power; colony has ${installedPower.toInt()}.", nextLevel = nextLevel)
        val currentWorkers = SiteOperationRules.workforceRequirement(colony, tile, dev.level)
        val nextWorkers = SiteOperationRules.workforceRequirement(colony, tile, nextLevel)
        val extraWorkers = max(0.0, nextWorkers - currentWorkers)
        val free = freeWorkforce(state)
        if (free < extraWorkers) return DevelopmentPreview(false, "Upgrade needs ${extraWorkers.toInt()} additional operational workers; only ${floor(free).toInt()} are free.", nextLevel = nextLevel, workforce = extraWorkers)
        val build = jsRound(developBuildCost(colony, tile, deposit) * 1.60.pow(dev.level))
        val ore = extractionUpgradeOre(deposit.category, nextLevel)
        val cost = InfrastructureCost(build, ore)
        resourceShortage(colony.inventory, build, ore)?.let {
            return DevelopmentPreview(false, it, cost = cost, nextLevel = nextLevel, workforce = extraWorkers)
        }
        return DevelopmentPreview(true, cost = cost, nextLevel = nextLevel, workforce = extraWorkers)
    }

    fun upgradeExtraction(state: GameState, coordinate: SectorCoordinate): DevelopmentActionResult {
        val preview = extractionUpgradePreview(state, coordinate)
        if (!preview.ok) return DevelopmentActionResult(false, state, preview.reason ?: "Extraction upgrade unavailable.")
        val colony = state.activeColony
        val tile = requireNotNull(colony.world.tileAt(coordinate))
        val dev = requireNotNull(tile.development)
        val cost = requireNotNull(preview.cost)
        val nextTile = tile.copy(development = dev.copy(
            level = preview.nextLevel,
            investedBuild = dev.investedBuild + cost.build,
            investedOre = dev.investedOre + cost.ore,
        ))
        return DevelopmentActionResult(
            true,
            state.withActiveColony(colony.copy(
                inventory = spend(colony.inventory, cost.build, cost.ore),
                world = colony.world.copy(tiles = replaceTile(colony.world.tiles, nextTile)),
            )),
            "Extraction site upgraded to L${preview.nextLevel}.",
            cost = cost,
        )
    }

    fun demolish(state: GameState, coordinate: SectorCoordinate): DevelopmentActionResult {
        val colony = state.activeColony
        val tile = colony.world.tileAt(coordinate) ?: return DevelopmentActionResult(false, state, "Sector not found.")
        val dev = tile.development ?: return DevelopmentActionResult(false, state, "Nothing has been constructed on this tile.")
        val recoverBuild = floor(dev.investedBuild * InfrastructureRules.DEMOLITION_RECOVERY)
        val recoverOre = floor(dev.investedOre * InfrastructureRules.DEMOLITION_RECOVERY)
        var inventory = colony.inventory
        val excellentQuality = ResourceQuality.forBand(QualityBand.EXCELLENT).minimum
        if (recoverBuild > 0) inventory = inventory.store(ResourceId("fiber"), ResourceCategory.BUILD, recoverBuild, excellentQuality)
        if (recoverOre > 0) inventory = inventory.store(ResourceId("surface-iron"), ResourceCategory.ORE, recoverOre, excellentQuality)

        val depletedExtractor = dev.kind == DevelopmentKind.EXTRACT && (
            tile.resourceExhausted || tile.deposit?.renewableWiped == true || (tile.deposit?.reserve ?: 1L) <= 0L
        )
        val nextTile = if (depletedExtractor) {
            tile.copy(
                resourceExhausted = true,
                exhaustedResourceId = tile.deposit?.resourceId ?: tile.exhaustedResourceId,
                resourceCovered = false,
                deposit = null,
                development = null,
            )
        } else {
            tile.copy(resourceCovered = false, development = null)
        }
        var identity = colony.headquarters
        if (dev.kind == DevelopmentKind.HEADQUARTERS && identity.primary == coordinate) {
            identity = identity.copy(primary = null, primaryEverAssigned = true)
        }
        var next = state.withActiveColony(colony.copy(
            inventory = inventory,
            headquarters = identity,
            world = colony.world.copy(tiles = replaceTile(colony.world.tiles, nextTile)),
        ))
        next = headquartersService.synchronizePrimary(next)
        return DevelopmentActionResult(
            true,
            next,
            "Development demolished; recovered ${recoverBuild.toInt()} Build and ${recoverOre.toInt()} Ore.",
            recoveredBuild = recoverBuild,
            recoveredOre = recoverOre,
        )
    }

    fun installedHousing(colony: ColonyState): Double = colony.world.tiles
        .filter { it.development?.kind == DevelopmentKind.HOUSING }
        .sumOf { InfrastructureRules.capacity(requireNotNull(it.development)) }

    fun installedPower(colony: ColonyState): Double = colony.world.tiles
        .filter { it.development?.kind == DevelopmentKind.POWER && it.development.constructionComplete && !it.development.productionStopped }
        .sumOf { InfrastructureRules.capacity(requireNotNull(it.development)) }

    fun installedIndustry(state: GameState): Double =
        fleetService.industrySupport(state) +
            state.activeColony.world.tiles
                .filter { it.development?.kind == DevelopmentKind.INDUSTRY && it.development.constructionComplete && !it.development.productionStopped }
                .sumOf { InfrastructureRules.capacity(requireNotNull(it.development)) }

    private fun commonBuildBlock(colony: ColonyState, tile: WorldTile): String? = when {
        colony.status == ColonyStatus.DEAD -> "This colony has been lost."
        colony.contract?.ended == true -> "The mining contract has ended."
        !tile.revealed -> "Survey this tile before construction."
        tile.coordinate.x == 0 && tile.coordinate.y == 0 -> "The Spaceport occupies this tile."
        else -> null
    }

    private fun localTechnologyLevel(colony: ColonyState, kind: DevelopmentKind): Int = when (kind) {
        DevelopmentKind.HOUSING -> colony.technology.housing
        DevelopmentKind.POWER -> colony.technology.power
        DevelopmentKind.INDUSTRY -> colony.technology.industry
        DevelopmentKind.HEADQUARTERS -> Int.MAX_VALUE
        DevelopmentKind.EXTRACT -> colony.technology.mining
    }

    private fun freeWorkforce(state: GameState): Double {
        val colony = state.activeColony
        val hqReserved = headquartersService.network(state, emptySet()).reservedStaff
        val siteWorkers = colony.world.tiles
            .filter { it.development?.kind == DevelopmentKind.EXTRACT && !it.resourceExhausted && it.deposit?.renewableWiped != true && !it.development.productionStopped }
            .sumOf { SiteOperationRules.workforceRequirement(colony, it) }
        return max(0.0, headquartersService.baseWorkforceAvailable(state) - hqReserved - siteWorkers)
    }

    private fun developBuildCost(colony: ColonyState, tile: WorldTile, deposit: ResourceDeposit): Double {
        val contractCost = colony.contract?.costMultiplier ?: 1.0
        val distance = 1.0 + hypot(tile.coordinate.x.toDouble(), tile.coordinate.y.toDouble()) * .012
        val complexity = .70 + .30 * MineItConfig.SITE_COMPLEXITY_COSTS[
            deposit.requiredMiningLevel.coerceIn(1, MineItConfig.SITE_COMPLEXITY_COSTS.size) - 1
        ]
        val size = if (deposit.sustainability == Sustainability.RENEWABLE) {
            when (deposit.abundanceLabel?.lowercase()) {
                "limited" -> 1.12
                "large" -> .90
                "vast" -> .80
                else -> 1.0
            }
        } else {
            when (deposit.depositScale?.lowercase()) {
                "small" -> 1.15
                "modest" -> 1.05
                "large" -> .95
                "huge" -> .85
                "colossal" -> .75
                else -> 1.0
            }
        }
        val terrain = when {
            tile.terrain == TerrainType.MOUNTAIN && deposit.category == ResourceCategory.ORE -> .90
            tile.terrain == TerrainType.HILL && deposit.category == ResourceCategory.BUILD -> .90
            tile.terrain == TerrainType.PLAIN && deposit.category == ResourceCategory.FOOD -> .94
            tile.terrain == TerrainType.LAKE && deposit.category == ResourceCategory.FOOD -> .92
            else -> 1.0
        }
        return jsRound(MineItConfig.SITE_DEVELOP_BASE_BUILD * distance * contractCost * complexity * size * terrain)
    }

    private fun industryUpgradeRequirement(category: ResourceCategory, level: Int): Double {
        val table = if (category == ResourceCategory.FOOD) {
            listOf(0.0, 0.0, 100.0, 230.0, 420.0, 700.0)
        } else {
            listOf(0.0, 0.0, 150.0, 300.0, 550.0, 900.0)
        }
        return table[level.coerceIn(1, 5)]
    }

    private fun extractionUpgradeOre(category: ResourceCategory, level: Int): Double {
        val table = if (category == ResourceCategory.FOOD) {
            listOf(0.0, 0.0, 5.0, 15.0, 35.0, 65.0)
        } else {
            listOf(0.0, 0.0, 10.0, 30.0, 70.0, 130.0)
        }
        return table[level.coerceIn(1, 5)]
    }

    private fun resourceShortage(inventory: Inventory, build: Double, ore: Double): String? = when {
        inventory.amountFor(ResourceCategory.BUILD) + .0001 < build -> "Need ${build.toInt()} Build materials."
        inventory.amountFor(ResourceCategory.ORE) + .0001 < ore -> "Need ${ore.toInt()} Ore."
        else -> null
    }

    private fun spend(inventory: Inventory, build: Double, ore: Double): Inventory {
        var result = inventory
        if (build > 0) result = result.consumeCategory(ResourceCategory.BUILD, build).inventory
        if (ore > 0) result = result.consumeCategory(ResourceCategory.ORE, ore).inventory
        return result
    }

    private fun label(kind: DevelopmentKind): String = when (kind) {
        DevelopmentKind.HOUSING -> "Housing"
        DevelopmentKind.POWER -> "Power Plant"
        DevelopmentKind.INDUSTRY -> "Industry"
        DevelopmentKind.HEADQUARTERS -> "Headquarters"
        DevelopmentKind.EXTRACT -> "Extraction Site"
    }

    private fun replaceTile(tiles: List<WorldTile>, updated: WorldTile): List<WorldTile> =
        tiles.map { if (it.coordinate == updated.coordinate) updated else it }

    private fun jsRound(value: Double): Double = floor(value + .5)

    private fun GameState.withActiveColony(updated: ColonyState): GameState =
        copy(colonies = colonies.map { if (it.id == updated.id) updated else it })
}

data class DevelopmentPreview(
    val ok: Boolean,
    val reason: String? = null,
    val cost: InfrastructureCost? = null,
    val nextLevel: Int = 0,
    val workforce: Double = 0.0,
    val coversResource: Boolean = false,
    val max: Boolean = false,
)

data class DevelopmentActionResult(
    val ok: Boolean,
    val state: GameState,
    val message: String,
    val cost: InfrastructureCost? = null,
    val recoveredBuild: Double = 0.0,
    val recoveredOre: Double = 0.0,
)
