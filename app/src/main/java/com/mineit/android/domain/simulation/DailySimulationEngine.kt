package com.mineit.android.domain.simulation

import com.mineit.android.domain.colony.InfrastructureRules
import com.mineit.android.domain.colony.TechnologyCapabilities
import com.mineit.android.domain.config.MineItConfig
import com.mineit.android.domain.model.ColonyState
import com.mineit.android.domain.model.ColonyStatus
import com.mineit.android.domain.model.GameState
import com.mineit.android.domain.resources.ExtractionCompatibility
import com.mineit.android.domain.resources.Inventory
import com.mineit.android.domain.resources.ResourceCategory
import com.mineit.android.domain.world.DevelopmentKind
import com.mineit.android.domain.world.ResourceDeposit
import com.mineit.android.domain.world.SectorCoordinate
import com.mineit.android.domain.world.Sustainability
import com.mineit.android.domain.world.SurveyGameService
import com.mineit.android.domain.world.WorldTile
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.round

/**
 * Canonical native owner for one MineIT colony day.
 *
 * The engine is pure Kotlin: it owns no timers, files, Android lifecycle objects or UI state.
 * Beginning-of-day Fuel semantics intentionally match the pinned web baseline: Power generation
 * and Fuel burn are calculated from Fuel available before the day's extraction is stored.
 */
class DailySimulationEngine(
    private val surveyGameService: SurveyGameService = SurveyGameService(),
) {
    fun recalculate(state: GameState): ColonyMetrics = calculateMetrics(state, state.activeColony)

    fun advanceDay(state: GameState): DailySimulationResult {
        val colony = state.activeColony
        if (colony.status == ColonyStatus.SITE_SELECTION) {
            return DailySimulationResult(
                state = state,
                metrics = recalculate(state),
                deaths = 0.0,
                colonyDied = false,
                completedSurveys = emptyList(),
                depletedSites = emptyList(),
                renewableEvents = emptyList(),
            )
        }
        if (colony.status == ColonyStatus.DEAD) {
            val next = state.copy(date = state.date.nextDay())
            return DailySimulationResult(
                state = next,
                metrics = recalculate(next),
                deaths = 0.0,
                colonyDied = false,
                completedSurveys = emptyList(),
                depletedSites = emptyList(),
                renewableEvents = emptyList(),
            )
        }

        val fuelStockAtStart = colony.inventory.amountFor(ResourceCategory.FUEL)
        val startNetwork = calculateNetworks(colony, fuelStockAtStart)
        val collection = collect(colony, startNetwork)
        var workingColony = collection.colony
        var inventory = workingColony.inventory

        val syntheticFood = syntheticFoodRate(workingColony)
        if (syntheticFood > 0.0) {
            inventory = inventory.store(
                resourceId = com.mineit.android.domain.model.ResourceId("synthetic"),
                category = ResourceCategory.FOOD,
                amount = syntheticFood,
                quality = 201,
            )
        }

        val demand = demand(workingColony, startNetwork)
        val foodUse = inventory.consumeCategory(ResourceCategory.FOOD, demand.foodDemand)
        inventory = foodUse.inventory
        val fuelUse = inventory.consumeCategory(
            ResourceCategory.FUEL,
            min(demand.fuelDemand, fuelStockAtStart),
        )
        inventory = fuelUse.inventory
        val oreUse = if (demand.oreDemand > 0.0) {
            inventory.consumeCategory(ResourceCategory.ORE, demand.oreDemand)
        } else {
            com.mineit.android.domain.resources.ResourceConsumption(0.0, 0.0, inventory)
        }
        inventory = oreUse.inventory

        val noFood = demand.foodDemand > 0.0 && foodUse.consumed <= .0001
        val starvationDays = if (noFood) workingColony.foodStarvationDays + 1 else 0
        val foodMortalityFactor = if (noFood && starvationDays <= STARVATION_GRACE_DAYS) 1.0 else foodUse.ratio
        val stable = min(foodMortalityFactor, startNetwork.lifeSupportPowerFactor)

        var population = workingColony.population
        var deaths = 0.0
        if (stable < .7 && population > 0.0) {
            val rate = mortalityRate(stable)
            deaths = min(population, max(.05, population * rate))
            population = max(0.0, population - deaths)
        }
        if (population < .5) population = 0.0

        var status = workingColony.status
        var contract = workingColony.contract
        var reputation = state.company.reputation
        val colonyDied = population <= 0.0 && status != ColonyStatus.DEAD
        if (colonyDied) {
            status = ColonyStatus.DEAD
            contract = contract?.copy(ended = true)
            reputation = max(0, reputation - MineItConfig.COLONY_DEATH_REPUTATION_PENALTY)
        }

        workingColony = workingColony.copy(
            population = population,
            inventory = inventory,
            foodStarvationDays = starvationDays,
            status = status,
            contract = contract,
            emergencyMode = if (colonyDied) false else workingColony.emergencyMode,
        )

        var workingState = state.copy(
            company = state.company.copy(reputation = reputation),
            colonies = state.colonies.map { if (it.id == workingColony.id) workingColony else it },
        )

        val surveyResult = if (colonyDied) {
            com.mineit.android.domain.world.SurveyGameProcessResult(workingState, emptyList())
        } else {
            surveyGameService.processSurveys(workingState)
        }
        workingState = surveyResult.state.copy(date = state.date.nextDay())

        val finalMetrics = calculateMetrics(workingState, workingState.activeColony).copy(
            foodProduction = collection.production.food + syntheticFood,
            buildProduction = collection.production.build,
            fuelProduction = collection.production.fuel,
            oreProduction = collection.production.ore,
            foodSupply = foodUse.ratio,
            fuelSupply = if (demand.fuelDemand > 0.0) fuelUse.ratio else 1.0,
            oreSupply = oreUse.ratio,
            powerFuelConsumed = fuelUse.consumed,
            lastDeaths = deaths,
            foodStarvationDays = starvationDays,
            survivalSupply = if (colonyDied) 0.0 else min(foodUse.ratio, startNetwork.lifeSupportPowerFactor),
        )

        return DailySimulationResult(
            state = workingState,
            metrics = finalMetrics,
            deaths = deaths,
            colonyDied = colonyDied,
            completedSurveys = surveyResult.completed.map { it.coordinate },
            depletedSites = collection.depletedSites,
            renewableEvents = collection.renewableEvents,
        )
    }

    private fun calculateMetrics(state: GameState, colony: ColonyState): ColonyMetrics {
        if (colony.status == ColonyStatus.SITE_SELECTION) {
            return ColonyMetrics(
                foodStock = colony.inventory.amountFor(ResourceCategory.FOOD),
                buildStock = colony.inventory.amountFor(ResourceCategory.BUILD),
                fuelStock = colony.inventory.amountFor(ResourceCategory.FUEL),
                oreStock = colony.inventory.amountFor(ResourceCategory.ORE),
                foodStarvationDays = colony.foodStarvationDays,
            )
        }

        val fuelStock = colony.inventory.amountFor(ResourceCategory.FUEL)
        val networks = calculateNetworks(colony, fuelStock)
        val production = forecastProduction(colony, networks)
        val demand = demand(colony, networks)
        val foodStock = colony.inventory.amountFor(ResourceCategory.FOOD)
        val buildStock = colony.inventory.amountFor(ResourceCategory.BUILD)
        val oreStock = colony.inventory.amountFor(ResourceCategory.ORE)
        val foodSupply = availableRatio(foodStock, production.food, demand.foodDemand)
        val fuelSupply = if (demand.fuelDemand > 0.0) min(1.0, fuelStock / demand.fuelDemand) else 1.0
        val oreSupply = availableRatio(oreStock, production.ore, demand.oreDemand)
        val industryFactor = if (colony.status == ColonyStatus.DEAD || colony.emergencyMode) {
            0.0
        } else {
            minOf(networks.industryPowerFactor, oreSupply, networks.industryStaffFactor)
        }
        val industry = networks.industryInstalled * industryFactor

        return ColonyMetrics(
            foodProduction = production.food + syntheticFoodRate(colony),
            buildProduction = production.build,
            fuelProduction = production.fuel,
            oreProduction = production.ore,
            foodStock = foodStock,
            buildStock = buildStock,
            fuelStock = fuelStock,
            oreStock = oreStock,
            foodDemand = demand.foodDemand,
            fuelDemand = demand.fuelDemand,
            oreDemand = demand.oreDemand,
            foodSupply = foodSupply,
            fuelSupply = fuelSupply,
            oreSupply = oreSupply,
            foodDays = supplyDays(foodStock, production.food, demand.foodDemand),
            fuelDays = supplyDays(fuelStock, production.fuel, demand.fuelDemand),
            oreDays = if (demand.oreDemand > 0.0) supplyDays(oreStock, production.ore, demand.oreDemand) else null,
            powerDemand = networks.powerDemand,
            powerCapacity = networks.powerCapacity,
            powerFuelLimitedGeneration = networks.fuelLimitedGeneration,
            powerFuelBurn = networks.fullFuelBurn,
            powerFuelConsumed = 0.0,
            powerFactor = if (colony.status == ColonyStatus.DEAD) 0.0 else networks.powerFactor,
            lifeSupportPowerFactor = if (colony.status == ColonyStatus.DEAD) 0.0 else networks.lifeSupportPowerFactor,
            industryPowerFactor = if (colony.status == ColonyStatus.DEAD) 0.0 else networks.industryPowerFactor,
            industryInstalled = networks.industryInstalled,
            industry = if (colony.status == ColonyStatus.DEAD) 0.0 else industry,
            industryLoad = networks.industryLoad,
            industrySurvivalFactor = networks.industrySurvivalFactor,
            industryCommercialFactor = networks.industryCommercialFactor,
            industryStaffFactor = networks.industryStaffFactor,
            industryPopulationRequired = networks.industryPopulationRequired,
            workforceAvailable = networks.workforceAvailable,
            workforceRequired = networks.workforceRequired,
            workforceSurvivalFactor = networks.workforceSurvivalFactor,
            workforceCommercialFactor = networks.workforceCommercialFactor,
            survivalSupply = if (colony.status == ColonyStatus.DEAD) 0.0 else min(foodSupply, networks.lifeSupportPowerFactor),
            foodStarvationDays = colony.foodStarvationDays,
        )
    }

    private fun calculateNetworks(colony: ColonyState, fuelStock: Double): Networks {
        val activeSites = activeSites(colony)
        val workforce = workforceNetwork(colony, activeSites)
        val industry = industryNetwork(colony, activeSites)
        val power = powerNetwork(colony, activeSites, workforce, industry, fuelStock)
        return Networks(
            activeSites = activeSites,
            workforceAvailable = workforce.available,
            workforceRequired = workforce.required,
            workforceSurvivalFactor = workforce.survivalFactor,
            workforceCommercialFactor = workforce.commercialFactor,
            industryInstalled = industry.installed,
            industryCapacity = industry.capacity,
            industryLoad = industry.load,
            industrySurvivalFactor = industry.survivalFactor,
            industryCommercialFactor = industry.commercialFactor,
            industryStaffFactor = industry.staffFactor,
            industryPopulationRequired = industry.populationRequired,
            powerDemand = power.demand,
            powerCapacity = power.capacity,
            fullFuelBurn = power.fullFuelBurn,
            fuelLimitedGeneration = power.fuelLimitedGeneration,
            powerFactor = power.powerFactor,
            lifeSupportPowerFactor = power.lifeSupportFactor,
            industryPowerFactor = power.industryPowerFactor,
            sitePowerFactors = power.siteFactors,
        )
    }

    private fun activeSites(colony: ColonyState): List<WorldTile> {
        if (colony.status == ColonyStatus.DEAD || colony.contract?.ended == true) return emptyList()
        return colony.world.tiles.filter { tile ->
            val development = tile.development
            val deposit = tile.deposit
            development?.kind == DevelopmentKind.EXTRACT &&
                development.constructionComplete &&
                !development.productionStopped &&
                !tile.resourceExhausted &&
                deposit != null &&
                !deposit.renewableWiped &&
                colony.technology.mining >= deposit.requiredMiningLevel &&
                (!colony.emergencyMode || deposit.category == ResourceCategory.FOOD || deposit.category == ResourceCategory.FUEL)
        }
    }

    private fun workforceNetwork(colony: ColonyState, sites: List<WorldTile>): WorkforceNetwork {
        val available = if (colony.status == ColonyStatus.DEAD || colony.foodStarvationDays > 0) {
            0.0
        } else {
            floor(colony.population * MineItConfig.WORKFORCE_SHARE)
        }
        var survivalRequired = 0.0
        var commercialRequired = 0.0
        for (tile in sites) {
            val required = siteWorkforce(colony, tile)
            val survival = tile.deposit?.category in setOf(ResourceCategory.FOOD, ResourceCategory.FUEL)
            if (survival) survivalRequired += required else commercialRequired += required
        }
        val survivalFactor = if (survivalRequired > 0.0) (available / survivalRequired).coerceIn(0.0, 1.0) else 1.0
        val remaining = max(0.0, available - survivalRequired)
        val commercialFactor = if (colony.emergencyMode) 0.0 else if (commercialRequired > 0.0) {
            (remaining / commercialRequired).coerceIn(0.0, 1.0)
        } else 1.0
        return WorkforceNetwork(
            available = available,
            required = survivalRequired + commercialRequired,
            survivalFactor = survivalFactor,
            commercialFactor = commercialFactor,
        )
    }

    private fun siteWorkforce(colony: ColonyState, tile: WorldTile): Double {
        val deposit = requireNotNull(tile.deposit)
        val level = tile.development?.level ?: 1
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
                    complexity * efficiency * intensityFactor,
            ),
        )
    }

    private fun industryNetwork(colony: ColonyState, sites: List<WorldTile>): IndustryNetwork {
        val installedBuildings = colony.world.tiles
            .filter { it.development?.kind == DevelopmentKind.INDUSTRY && it.development.constructionComplete && !it.development.productionStopped }
            .sumOf { InfrastructureRules.capacity(requireNotNull(it.development)) }
        val shipIndustry = if (colony.foundingShipDocked) InfrastructureRules.FOUNDING_SHIP_INDUSTRY else 0.0
        val installed = installedBuildings + shipIndustry
        val efficiency = TechnologyCapabilities.industryWorkforceEfficiency(colony.technology)
        val populationRequired = max(40.0, round(installed * .8 * efficiency))
        val staffFactor = (colony.population / max(1.0, populationRequired)).coerceIn(0.0, 1.0)
        val capacity = if (colony.status == ColonyStatus.DEAD || colony.emergencyMode) 0.0 else installed * staffFactor

        var survivalLoad = 0.0
        var commercialLoad = 0.0
        for (tile in sites) {
            val level = tile.development?.level ?: 1
            val load = round(MineItConfig.INDUSTRY_SITE_LOAD_BASE * MineItConfig.INDUSTRY_SITE_LOAD_GROWTH.pow(level - 1))
            if (tile.deposit?.category in setOf(ResourceCategory.FOOD, ResourceCategory.FUEL)) {
                survivalLoad += load
            } else {
                commercialLoad += load
            }
        }
        val survivalFactor = if (survivalLoad > 0.0) (capacity / survivalLoad).coerceIn(0.0, 1.0) else 1.0
        val remaining = max(0.0, capacity - survivalLoad)
        val commercialFactor = if (colony.emergencyMode) 0.0 else if (commercialLoad > 0.0) {
            (remaining / commercialLoad).coerceIn(0.0, 1.0)
        } else 1.0
        return IndustryNetwork(
            installed = installed,
            capacity = capacity,
            load = survivalLoad + commercialLoad,
            survivalLoad = survivalLoad,
            commercialLoad = commercialLoad,
            survivalFactor = survivalFactor,
            commercialFactor = commercialFactor,
            staffFactor = staffFactor,
            populationRequired = populationRequired,
        )
    }

    private fun powerNetwork(
        colony: ColonyState,
        sites: List<WorldTile>,
        workforce: WorkforceNetwork,
        industry: IndustryNetwork,
        fuelStock: Double,
    ): PowerNetwork {
        val powerBuildings = colony.world.tiles.filter {
            it.development?.kind == DevelopmentKind.POWER &&
                it.development.constructionComplete &&
                !it.development.productionStopped
        }
        val capacity = powerBuildings.sumOf { InfrastructureRules.capacity(requireNotNull(it.development)) }
        val fuelIntensity = TechnologyCapabilities.fuelIntensity(colony.technology)
        val fullFuelBurn = capacity * fuelIntensity
        val fuelRatio = if (fullFuelBurn > 0.0) (fuelStock / fullFuelBurn).coerceIn(0.0, 1.0) else 0.0
        val available = capacity * fuelRatio

        val housingPower = colony.world.tiles
            .filter {
                it.development?.kind == DevelopmentKind.HOUSING &&
                    it.development.constructionComplete &&
                    !it.development.productionStopped
            }
            .sumOf { InfrastructureRules.housingFixedPower(requireNotNull(it.development).level) }
        val lifeSupport = colony.population * MineItConfig.LIFE_SUPPORT_POWER_PER_COLONIST *
            (colony.contract?.supportLoad ?: 1.0) *
            (if (colony.emergencyMode) MineItConfig.EMERGENCY_LIFE_SUPPORT_MULTIPLIER else 1.0)

        val industryRows = mutableListOf<PowerRequest>()
        for (tile in colony.world.tiles) {
            val development = tile.development ?: continue
            if (development.kind != DevelopmentKind.INDUSTRY || !development.constructionComplete || development.productionStopped) continue
            val installedCapacity = InfrastructureRules.capacity(development)
            val idle = InfrastructureRules.industryIdlePower(development.level)
            val variable = if (colony.emergencyMode) 0.0 else installedCapacity * industry.staffFactor * InfrastructureRules.INDUSTRY_VARIABLE_POWER_PER_CAPACITY
            industryRows += PowerRequest("industry:${tile.coordinate.x},${tile.coordinate.y}", idle + variable)
        }
        if (colony.foundingShipDocked && !colony.emergencyMode) {
            industryRows += PowerRequest(
                "founding-ship-industry",
                InfrastructureRules.FOUNDING_SHIP_INDUSTRY * industry.staffFactor * InfrastructureRules.INDUSTRY_VARIABLE_POWER_PER_CAPACITY,
            )
        }

        val operationalIndustry = max(0.0, industry.capacity)
        val survivalIndustry = min(operationalIndustry, industry.survivalLoad)
        val survivalShare = if (operationalIndustry > 0.0) survivalIndustry / operationalIndustry else 0.0
        val industrySurvival = industryRows.map { it.copy(id = "${it.id}:survival", requested = it.requested * survivalShare) }
            .filter { it.requested > 0.0 }
        val industryCommercial = industryRows.map { it.copy(id = "${it.id}:commercial", requested = it.requested * (1.0 - survivalShare)) }
            .filter { it.requested > 0.0 }

        val survivalSites = mutableListOf<PowerRequest>()
        val commercialSites = mutableListOf<PowerRequest>()
        for (tile in sites) {
            val deposit = requireNotNull(tile.deposit)
            val level = tile.development?.level ?: 1
            val family = ExtractionCompatibility.familyFor(deposit.resourceId)
            val row = PowerRequest(
                id = siteId(tile),
                requested = InfrastructureRules.facilityPower(family, level),
            )
            if (deposit.category in setOf(ResourceCategory.FOOD, ResourceCategory.FUEL)) survivalSites += row else commercialSites += row
        }

        var remaining = available
        val siteFactors = mutableMapOf<String, Double>()

        val lifeRequested = lifeSupport + housingPower
        val lifeDelivered = min(remaining, lifeRequested)
        val lifeFactor = if (lifeRequested > 0.0) lifeDelivered / lifeRequested else 1.0
        remaining -= lifeDelivered

        val survivalRows = survivalSites + industrySurvival
        val survivalRequested = survivalRows.sumOf { it.requested }
        val survivalDelivered = min(remaining, survivalRequested)
        val survivalFactor = if (survivalRequested > 0.0) survivalDelivered / survivalRequested else 1.0
        for (row in survivalSites) siteFactors[row.id] = survivalFactor
        remaining -= survivalDelivered

        val spaceportDelivered = if (remaining >= InfrastructureRules.BASIC_SPACEPORT_POWER) InfrastructureRules.BASIC_SPACEPORT_POWER else 0.0
        remaining -= spaceportDelivered

        val commercialRows = commercialSites + industryCommercial
        val commercialRequested = commercialRows.sumOf { it.requested }
        val commercialDelivered = min(remaining, commercialRequested)
        val commercialFactor = if (commercialRequested > 0.0) commercialDelivered / commercialRequested else 1.0
        for (row in commercialSites) siteFactors[row.id] = commercialFactor
        remaining -= commercialDelivered

        val industryRequested = industrySurvival.sumOf { it.requested } + industryCommercial.sumOf { it.requested }
        val industryDelivered = industrySurvival.sumOf { it.requested * survivalFactor } +
            industryCommercial.sumOf { it.requested * commercialFactor }
        val industryPowerFactor = if (industryRequested > 0.0) industryDelivered / industryRequested else 1.0

        val demand = lifeRequested + survivalRequested + InfrastructureRules.BASIC_SPACEPORT_POWER + commercialRequested
        val delivered = available - max(0.0, remaining)
        return PowerNetwork(
            demand = demand,
            capacity = capacity,
            fullFuelBurn = fullFuelBurn,
            fuelLimitedGeneration = available,
            powerFactor = if (demand > 0.0) (delivered / demand).coerceIn(0.0, 1.0) else 1.0,
            lifeSupportFactor = lifeFactor,
            industryPowerFactor = industryPowerFactor,
            siteFactors = siteFactors,
        )
    }

    private fun collect(colony: ColonyState, networks: Networks): CollectionResult {
        var inventory = colony.inventory
        var tiles = colony.world.tiles
        var food = 0.0
        var build = 0.0
        var fuel = 0.0
        var ore = 0.0
        val depleted = mutableListOf<SectorCoordinate>()
        val renewableEvents = mutableListOf<RenewableEvent>()

        for (site in networks.activeSites) {
            val current = tiles.first { it.coordinate == site.coordinate }
            val deposit = requireNotNull(current.deposit)
            val rate = collectionRate(colony, current, networks)
            if (rate <= 0.0) continue

            val collected: Double
            var nextTile = current
            if (deposit.sustainability == Sustainability.FINITE) {
                val reserve = (deposit.reserve ?: 0L).toDouble()
                collected = min(rate, reserve)
                val nextReserve = max(0.0, reserve - collected)
                val exhausted = nextReserve <= .0001
                nextTile = current.copy(
                    deposit = deposit.copy(reserve = jsRoundToLong(nextReserve)),
                    resourceExhausted = exhausted,
                    development = if (exhausted) null else current.development,
                )
                if (exhausted) depleted += current.coordinate
            } else {
                collected = rate
                val renewable = updateRenewable(current)
                nextTile = renewable.tile
                renewable.event?.let(renewableEvents::add)
                if (nextTile.resourceExhausted) depleted += current.coordinate
            }

            if (collected > 0.0) {
                inventory = inventory.store(
                    resourceId = deposit.resourceId,
                    category = deposit.category,
                    amount = collected,
                    quality = deposit.quality,
                )
                when (deposit.category) {
                    ResourceCategory.FOOD -> food += collected
                    ResourceCategory.BUILD -> build += collected
                    ResourceCategory.FUEL -> fuel += collected
                    ResourceCategory.ORE -> ore += collected
                }
            }
            tiles = tiles.map { if (it.coordinate == nextTile.coordinate) nextTile else it }
        }

        return CollectionResult(
            colony = colony.copy(inventory = inventory, world = colony.world.copy(tiles = tiles)),
            production = Production(food, build, fuel, ore),
            depletedSites = depleted,
            renewableEvents = renewableEvents,
        )
    }

    private fun forecastProduction(colony: ColonyState, networks: Networks): Production {
        var food = 0.0
        var build = 0.0
        var fuel = 0.0
        var ore = 0.0
        for (tile in networks.activeSites) {
            val rate = collectionRate(colony, tile, networks)
            when (tile.deposit?.category) {
                ResourceCategory.FOOD -> food += rate
                ResourceCategory.BUILD -> build += rate
                ResourceCategory.FUEL -> fuel += rate
                ResourceCategory.ORE -> ore += rate
                null -> Unit
            }
        }
        return Production(food, build, fuel, ore)
    }

    private fun collectionRate(colony: ColonyState, tile: WorldTile, networks: Networks): Double {
        val deposit = requireNotNull(tile.deposit)
        val level = tile.development?.level ?: 1
        val baseOutput = siteOutput(level)
        val potential = if (deposit.sustainability == Sustainability.RENEWABLE) {
            baseOutput * renewableRateFactor(deposit.abundanceLabel) * deposit.terrainYieldFactor * deposit.harvestIntensity.coerceIn(.25, 2.0)
        } else {
            baseOutput * finiteRateFactor(deposit.depositScale) * deposit.terrainYieldFactor
        }
        val foodAdjusted = if (deposit.category == ResourceCategory.FOOD) {
            potential * TechnologyCapabilities.foodProductionMultiplier(colony.technology)
        } else potential
        val workforce = if (deposit.category in setOf(ResourceCategory.FOOD, ResourceCategory.FUEL)) {
            networks.workforceSurvivalFactor
        } else networks.workforceCommercialFactor
        val industry = if (deposit.category in setOf(ResourceCategory.FOOD, ResourceCategory.FUEL)) {
            networks.industrySurvivalFactor
        } else networks.industryCommercialFactor
        val power = networks.sitePowerFactors[siteId(tile)] ?: 1.0
        return max(0.0, foodAdjusted * workforce * industry * power)
    }

    private fun updateRenewable(tile: WorldTile): RenewableUpdate {
        val deposit = requireNotNull(tile.deposit)
        val originalRank = deposit.renewableOriginalRank ?: renewableRank(deposit.abundanceLabel)
        var health = deposit.renewableHealth ?: (originalRank + 1.0)
        val intensity = deposit.harvestIntensity.coerceIn(.25, 2.0)
        val beforeLabel = deposit.abundanceLabel ?: "Established"
        val maxScore = originalRank + 1.0
        if (intensity > 1.0) {
            health = max(0.0, health - MineItConfig.RENEWABLE_OVERHARVEST_RATE * (intensity - 1.0))
        } else if (intensity < 1.0) {
            health = min(maxScore, health + MineItConfig.RENEWABLE_RECOVERY_RATE * (1.0 - intensity))
        }
        if (health <= 0.0) {
            val next = tile.copy(
                resourceExhausted = true,
                development = null,
                deposit = deposit.copy(
                    renewableOriginalRank = originalRank,
                    renewableHealth = 0.0,
                    renewableWiped = true,
                    abundance = 0.0,
                ),
            )
            return RenewableUpdate(next, RenewableEvent(tile.coordinate, beforeLabel, "Wiped Out"))
        }
        val afterRank = min(originalRank, max(0, ceil(health).toInt() - 1))
        val afterLabel = renewableLabel(afterRank)
        val next = tile.copy(
            deposit = deposit.copy(
                renewableOriginalRank = originalRank,
                renewableHealth = health,
                abundanceLabel = afterLabel,
                abundance = renewableRateFactor(afterLabel),
            ),
        )
        val event = if (afterLabel != beforeLabel) RenewableEvent(tile.coordinate, beforeLabel, afterLabel) else null
        return RenewableUpdate(next, event)
    }

    private fun demand(colony: ColonyState, networks: Networks): Demand {
        val foodDemand = if (colony.status == ColonyStatus.DEAD || colony.population <= 0.0) {
            0.0
        } else {
            max(.1, colony.population * MineItConfig.FOOD_PER_COLONIST)
        }
        val staffedIndustry = if (colony.emergencyMode) 0.0 else networks.industryInstalled * networks.industryStaffFactor
        val oreDemand = if (colony.emergencyMode) 0.0 else max(
            .05,
            staffedIndustry * (MineItConfig.ORE_PER_INDUSTRY_LEVEL / 100.0) *
                TechnologyCapabilities.industryOreEfficiency(colony.technology),
        )
        return Demand(
            foodDemand = foodDemand,
            fuelDemand = networks.fullFuelBurn,
            oreDemand = oreDemand,
        )
    }

    private fun syntheticFoodRate(colony: ColonyState): Double {
        val base = TechnologyCapabilities.syntheticFood(colony.technology)
        return if (colony.contract?.naturalFood == false) base else base * .35
    }

    private fun siteOutput(level: Int): Double {
        val normalized = max(1, level)
        if (normalized <= MineItConfig.SITE_OUTPUT_LEVELS.size) return MineItConfig.SITE_OUTPUT_LEVELS[normalized - 1]
        val last = MineItConfig.SITE_OUTPUT_LEVELS.last()
        return last * 1.24.pow(normalized - MineItConfig.SITE_OUTPUT_LEVELS.size)
    }

    private fun availableRatio(stock: Double, production: Double, demand: Double): Double =
        if (demand > 0.0) ((max(0.0, stock) + max(0.0, production)) / demand).coerceIn(0.0, 1.0) else 1.0

    private fun supplyDays(stock: Double, production: Double, demand: Double): Double? {
        val deficit = max(0.0, demand - production)
        return if (deficit <= .0001) null else max(0.0, stock / deficit)
    }

    fun mortalityRate(stable: Double): Double {
        if (stable >= .7) return 0.0
        if (stable >= .4) {
            val t = (.7 - stable) / .3
            return MineItConfig.CRITICAL_MORTALITY_MIN +
                t * (MineItConfig.CRITICAL_MORTALITY_MAX - MineItConfig.CRITICAL_MORTALITY_MIN)
        }
        val t = (.4 - max(0.0, stable)) / .4
        return MineItConfig.CRITICAL_MORTALITY_MAX +
            t * (MineItConfig.COLLAPSE_MORTALITY_MAX - MineItConfig.CRITICAL_MORTALITY_MAX)
    }

    private fun finiteRateFactor(label: String?): Double = when (label?.lowercase()) {
        "small" -> .75
        "modest" -> .90
        "large" -> 1.05
        "huge" -> 1.20
        "colossal" -> 1.35
        else -> 1.0
    }

    private fun renewableRank(label: String?): Int = when (label?.lowercase()) {
        "limited" -> 0
        "large" -> 2
        "vast" -> 3
        else -> 1
    }

    private fun renewableLabel(rank: Int): String = listOf("Limited", "Established", "Large", "Vast")[rank.coerceIn(0, 3)]

    private fun renewableRateFactor(label: String?): Double = when (label?.lowercase()) {
        "limited" -> .65
        "large" -> 1.45
        "vast" -> 2.10
        else -> 1.0
    }

    private fun siteId(tile: WorldTile): String = "site:${tile.coordinate.x},${tile.coordinate.y}"

    private fun jsRoundToLong(value: Double): Long = floor(value + .5).toLong()

    private data class WorkforceNetwork(
        val available: Double,
        val required: Double,
        val survivalFactor: Double,
        val commercialFactor: Double,
    )

    private data class IndustryNetwork(
        val installed: Double,
        val capacity: Double,
        val load: Double,
        val survivalLoad: Double,
        val commercialLoad: Double,
        val survivalFactor: Double,
        val commercialFactor: Double,
        val staffFactor: Double,
        val populationRequired: Double,
    )

    private data class PowerRequest(val id: String, val requested: Double)

    private data class PowerNetwork(
        val demand: Double,
        val capacity: Double,
        val fullFuelBurn: Double,
        val fuelLimitedGeneration: Double,
        val powerFactor: Double,
        val lifeSupportFactor: Double,
        val industryPowerFactor: Double,
        val siteFactors: Map<String, Double>,
    )

    private data class Networks(
        val activeSites: List<WorldTile>,
        val workforceAvailable: Double,
        val workforceRequired: Double,
        val workforceSurvivalFactor: Double,
        val workforceCommercialFactor: Double,
        val industryInstalled: Double,
        val industryCapacity: Double,
        val industryLoad: Double,
        val industrySurvivalFactor: Double,
        val industryCommercialFactor: Double,
        val industryStaffFactor: Double,
        val industryPopulationRequired: Double,
        val powerDemand: Double,
        val powerCapacity: Double,
        val fullFuelBurn: Double,
        val fuelLimitedGeneration: Double,
        val powerFactor: Double,
        val lifeSupportPowerFactor: Double,
        val industryPowerFactor: Double,
        val sitePowerFactors: Map<String, Double>,
    )

    private data class Demand(
        val foodDemand: Double,
        val fuelDemand: Double,
        val oreDemand: Double,
    )

    private data class Production(val food: Double, val build: Double, val fuel: Double, val ore: Double)

    private data class CollectionResult(
        val colony: ColonyState,
        val production: Production,
        val depletedSites: List<SectorCoordinate>,
        val renewableEvents: List<RenewableEvent>,
    )

    private data class RenewableUpdate(val tile: WorldTile, val event: RenewableEvent?)

    companion object {
        private const val STARVATION_GRACE_DAYS = 30
    }
}

data class ColonyMetrics(
    val foodProduction: Double = 0.0,
    val buildProduction: Double = 0.0,
    val fuelProduction: Double = 0.0,
    val oreProduction: Double = 0.0,
    val foodStock: Double = 0.0,
    val buildStock: Double = 0.0,
    val fuelStock: Double = 0.0,
    val oreStock: Double = 0.0,
    val foodDemand: Double = 0.0,
    val fuelDemand: Double = 0.0,
    val oreDemand: Double = 0.0,
    val foodSupply: Double = 1.0,
    val fuelSupply: Double = 1.0,
    val oreSupply: Double = 1.0,
    val foodDays: Double? = null,
    val fuelDays: Double? = null,
    val oreDays: Double? = null,
    val powerDemand: Double = 0.0,
    val powerCapacity: Double = 0.0,
    val powerFuelLimitedGeneration: Double = 0.0,
    val powerFuelBurn: Double = 0.0,
    val powerFuelConsumed: Double = 0.0,
    val powerFactor: Double = 1.0,
    val lifeSupportPowerFactor: Double = 1.0,
    val industryPowerFactor: Double = 1.0,
    val industryInstalled: Double = 0.0,
    val industry: Double = 0.0,
    val industryLoad: Double = 0.0,
    val industrySurvivalFactor: Double = 1.0,
    val industryCommercialFactor: Double = 1.0,
    val industryStaffFactor: Double = 1.0,
    val industryPopulationRequired: Double = 0.0,
    val workforceAvailable: Double = 0.0,
    val workforceRequired: Double = 0.0,
    val workforceSurvivalFactor: Double = 1.0,
    val workforceCommercialFactor: Double = 1.0,
    val survivalSupply: Double = 1.0,
    val foodStarvationDays: Int = 0,
    val lastDeaths: Double = 0.0,
)

data class RenewableEvent(
    val coordinate: SectorCoordinate,
    val from: String,
    val to: String,
)

data class DailySimulationResult(
    val state: GameState,
    val metrics: ColonyMetrics,
    val deaths: Double,
    val colonyDied: Boolean,
    val completedSurveys: List<SectorCoordinate>,
    val depletedSites: List<SectorCoordinate>,
    val renewableEvents: List<RenewableEvent>,
)
