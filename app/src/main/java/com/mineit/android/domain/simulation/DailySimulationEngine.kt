package com.mineit.android.domain.simulation

import com.mineit.android.domain.colony.ColonyNetworkService
import com.mineit.android.domain.colony.ColonyNetworkSnapshot
import com.mineit.android.domain.colony.HeadquartersContinuityPhase
import com.mineit.android.domain.colony.HeadquartersService
import com.mineit.android.domain.colony.SiteProductionRules
import com.mineit.android.domain.colony.TechnologyCapabilities
import com.mineit.android.domain.config.MineItConfig
import com.mineit.android.domain.model.ColonyState
import com.mineit.android.domain.model.ColonyStatus
import com.mineit.android.domain.model.GameState
import com.mineit.android.domain.model.ShipId
import com.mineit.android.domain.resources.ResourceCategory
import com.mineit.android.domain.ships.PlayerFleetService
import com.mineit.android.domain.world.SectorCoordinate
import com.mineit.android.domain.world.Sustainability
import com.mineit.android.domain.world.SurveyGameProcessResult
import com.mineit.android.domain.world.SurveyGameService
import com.mineit.android.domain.world.WorldTile
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/** Canonical pure-Kotlin owner for one complete MineIT colony day. */
class DailySimulationEngine(
    private val surveyGameService: SurveyGameService = SurveyGameService(),
    private val fleetService: PlayerFleetService = PlayerFleetService(),
    private val networkService: ColonyNetworkService = ColonyNetworkService(fleetService),
    private val headquartersService: HeadquartersService = HeadquartersService(fleetService),
) {
    fun recalculate(state: GameState): ColonyMetrics = calculateMetrics(state, networkService.calculate(state))

    fun advanceDay(state: GameState): DailySimulationResult {
        val initialColony = state.activeColony
        if (initialColony.status == ColonyStatus.SITE_SELECTION) {
            return DailySimulationResult(state, recalculate(state), 0.0, false, emptyList(), emptyList(), emptyList())
        }
        if (initialColony.status == ColonyStatus.DEAD) {
            val next = state.copy(date = state.date.nextDay())
            return DailySimulationResult(next, recalculate(next), 0.0, false, emptyList(), emptyList(), emptyList())
        }

        // Power and all priority allocations are frozen from beginning-of-day Fuel. Fuel collected
        // later in this method is stored for tomorrow and never retroactively increases today.
        val fuelStockAtStart = initialColony.inventory.amountFor(ResourceCategory.FUEL)
        val startNetwork = networkService.calculate(state, fuelStockAtStart)
        var workingState = headquartersService.persistContinuity(state, startNetwork.continuity)
        var workingColony = workingState.activeColony

        val collection = collect(workingColony, startNetwork)
        workingColony = collection.colony
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

        workingColony = workingColony.copy(inventory = inventory)
        workingState = workingState.withActiveColony(workingColony)

        // N05: residents accommodated aboard ships consume ship Food before planetary demand is
        // processed. Their starvation counter is owned by the ship, never by the colony pantry.
        val shipFoodUse = fleetService.consumeResidentFood(workingState)
        workingState = shipFoodUse.state
        workingColony = workingState.activeColony
        inventory = workingColony.inventory

        val demand = demand(workingState, workingColony, startNetwork)
        val foodUse = inventory.consumeCategory(ResourceCategory.FOOD, demand.foodDemand)
        inventory = foodUse.inventory
        val fuelUse = inventory.consumeCategory(ResourceCategory.FUEL, min(demand.fuelDemand, fuelStockAtStart))
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
        var planetaryDeaths = 0.0
        val planetaryBefore = fleetService.planetaryResidentCount(workingState)
        if (stable < .7 && planetaryBefore > 0.0) {
            planetaryDeaths = min(planetaryBefore, max(.05, planetaryBefore * mortalityRate(stable)))
            population = max(0.0, population - planetaryDeaths)
        }
        if (population < .5) population = 0.0

        val nextPlanetaryAccommodation = min(
            workingColony.planetaryAccommodationResidents,
            max(0.0, population - workingColony.shipResidentAssignments.sumOf { it.residents }),
        )
        workingColony = workingColony.copy(
            population = population,
            inventory = inventory,
            foodStarvationDays = starvationDays,
            planetaryAccommodationResidents = nextPlanetaryAccommodation,
        )
        workingState = workingState.withActiveColony(workingColony)

        val shipLosses = buildMap<ShipId, Double> {
            shipFoodUse.rows.forEach { row ->
                val shipStable = if (row.consumed <= .0001 && row.starvationDays <= STARVATION_GRACE_DAYS) 1.0 else row.ratio
                if (shipStable < .7 && row.residents > 0.0) {
                    put(row.shipId, min(row.residents, max(.05, row.residents * mortalityRate(shipStable))))
                }
            }
        }
        val beforeShipDeaths = workingState.activeColony.population
        workingState = fleetService.applyResidentDeaths(workingState, shipLosses)
        val shipDeaths = max(0.0, beforeShipDeaths - workingState.activeColony.population)
        var deaths = planetaryDeaths + shipDeaths
        workingColony = workingState.activeColony
        population = workingColony.population
        if (population < .5 && population > 0.0) {
            deaths += population
            population = 0.0
            workingColony = workingColony.copy(population = 0.0)
            workingState = workingState.withActiveColony(workingColony)
        }

        var status = workingColony.status
        var contract = workingColony.contract
        var reputation = workingState.company.reputation
        val colonyDied = population <= 0.0 && status != ColonyStatus.DEAD
        if (colonyDied) {
            status = ColonyStatus.DEAD
            contract = contract?.copy(ended = true)
            reputation = max(0.0, reputation - MineItConfig.COLONY_DEATH_REPUTATION_PENALTY)
        }

        workingColony = workingColony.copy(
            population = population,
            status = status,
            contract = contract,
            emergencyMode = if (colonyDied) false else workingColony.emergencyMode,
        )
        workingState = workingState.copy(
            company = workingState.company.copy(reputation = reputation),
            colonies = workingState.colonies.map { if (it.id == workingColony.id) workingColony else it },
        )

        val surveyResult = if (colonyDied) {
            SurveyGameProcessResult(workingState, emptyList())
        } else {
            surveyGameService.processSurveys(
                state = workingState,
                commandEfficiency = startNetwork.headquarters.efficiency,
                headquartersContinuityFactor = startNetwork.continuity.efficiencyFactor,
            )
        }
        workingState = surveyResult.state.copy(date = state.date.nextDay())

        // End-of-day stocks are current, while network/delivery fields describe the network that
        // actually governed the completed day rather than a recalculation using newly produced Fuel.
        val endMetrics = calculateMetrics(workingState, networkService.calculate(workingState))
        val fuelSupplyForDay = if (demand.fuelDemand > 0.0) (fuelUse.consumed / demand.fuelDemand).coerceIn(0.0, 1.0) else 1.0
        val oreSupplyForDay = if (demand.oreDemand > 0.0) oreUse.ratio else 1.0
        val poweredIndustryForDay = startNetwork.shipIndustry +
            startNetwork.builtIndustry * startNetwork.industryStaffFactor * startNetwork.industryPowerFactor
        val industryForDay = if (colonyDied || workingColony.emergencyMode) 0.0 else
            poweredIndustryForDay * oreSupplyForDay * startNetwork.continuity.efficiencyFactor
        val shipFoodStatus = fleetService.residentFoodStatus(workingState)
        val finalMetrics = endMetrics.copy(
            foodProduction = collection.production.food + syntheticFood,
            buildProduction = collection.production.build,
            fuelProduction = collection.production.fuel,
            oreProduction = collection.production.ore,
            foodDemand = demand.foodDemand,
            fuelDemand = demand.fuelDemand,
            oreDemand = demand.oreDemand,
            foodSupply = foodUse.ratio,
            fuelSupply = fuelSupplyForDay,
            oreSupply = oreSupplyForDay,
            powerDemand = startNetwork.powerDemand,
            powerCapacity = startNetwork.powerCapacity,
            powerFuelLimitedGeneration = startNetwork.fuelLimitedGeneration,
            powerFuelBurn = startNetwork.fullFuelBurn,
            powerFuelConsumed = fuelUse.consumed,
            powerFactor = if (colonyDied) 0.0 else startNetwork.powerFactor,
            lifeSupportPowerFactor = if (colonyDied) 0.0 else startNetwork.lifeSupportPowerFactor,
            industryPowerFactor = if (colonyDied) 0.0 else startNetwork.industryPowerFactor,
            industryInstalled = startNetwork.industryInstalled,
            industry = industryForDay,
            industryLoad = startNetwork.industryLoad,
            industrySurvivalFactor = startNetwork.industrySurvivalFactor,
            industryCommercialFactor = startNetwork.industryCommercialFactor,
            industryStaffFactor = startNetwork.industryStaffFactor,
            industryPopulationRequired = startNetwork.industryPopulationRequired,
            workforceAvailable = startNetwork.workforceAvailable,
            workforceRequired = startNetwork.workforceRequired,
            workforceSurvivalFactor = startNetwork.workforceSurvivalFactor,
            workforceCommercialFactor = startNetwork.workforceCommercialFactor,
            spaceportPowerFactor = if (colonyDied) 0.0 else startNetwork.spaceportPowerFactor,
            commandCapacity = startNetwork.headquarters.capacity,
            commandLoad = startNetwork.headquarters.load,
            commandEfficiency = startNetwork.headquarters.efficiency,
            headquartersContinuityFactor = startNetwork.continuity.efficiencyFactor,
            headquartersPhase = startNetwork.continuity.phase,
            primaryHeadquartersOperational = startNetwork.headquarters.primaryOperational,
            headquartersNetworkAvailable = startNetwork.continuity.networkAvailable,
            headquartersRecoveryDaysRemaining = startNetwork.continuity.recoveryDaysRemaining,
            lastDeaths = deaths,
            foodStarvationDays = starvationDays,
            survivalSupply = if (colonyDied) 0.0 else min(foodUse.ratio, startNetwork.lifeSupportPowerFactor),
            planetaryResidents = fleetService.planetaryResidentCount(workingState),
            shipResidents = workingState.activeColony.shipResidentAssignments.sumOf { it.residents },
            shipFoodDemand = shipFoodUse.requested,
            shipFoodConsumed = shipFoodUse.consumed,
            shipFoodAvailable = shipFoodStatus.available,
            shipFoodSupply = shipFoodUse.ratio,
            shipFoodShortestDays = shipFoodStatus.shortestDays,
            shipFoodWarning = shipFoodStatus.shortestDays?.let { it <= MineItConfig.SHIP_FOOD_WARNING_DAYS } == true,
            shipFoodCritical = shipFoodStatus.shortestDays?.let { it < MineItConfig.SHIP_FOOD_CRITICAL_DAYS } == true,
            shipFoodDeaths = shipDeaths,
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

    private fun calculateMetrics(state: GameState, network: ColonyNetworkSnapshot): ColonyMetrics {
        val colony = state.activeColony
        val shipFood = fleetService.residentFoodStatus(state)
        val planetaryResidents = fleetService.planetaryResidentCount(state)
        val shipResidents = colony.shipResidentAssignments.sumOf { it.residents }
        if (colony.status == ColonyStatus.SITE_SELECTION) {
            return ColonyMetrics(
                foodStock = colony.inventory.amountFor(ResourceCategory.FOOD),
                buildStock = colony.inventory.amountFor(ResourceCategory.BUILD),
                fuelStock = colony.inventory.amountFor(ResourceCategory.FUEL),
                oreStock = colony.inventory.amountFor(ResourceCategory.ORE),
                foodStarvationDays = colony.foodStarvationDays,
                planetaryResidents = planetaryResidents,
                shipResidents = shipResidents,
                shipFoodDemand = shipFood.requested,
                shipFoodAvailable = shipFood.available,
                shipFoodSupply = shipFood.ratio,
                shipFoodShortestDays = shipFood.shortestDays,
                shipFoodWarning = shipFood.shortestDays?.let { it <= MineItConfig.SHIP_FOOD_WARNING_DAYS } == true,
                shipFoodCritical = shipFood.shortestDays?.let { it < MineItConfig.SHIP_FOOD_CRITICAL_DAYS } == true,
            )
        }
        val production = forecastProduction(colony, network)
        val currentDemand = demand(state, colony, network)
        val foodStock = colony.inventory.amountFor(ResourceCategory.FOOD)
        val buildStock = colony.inventory.amountFor(ResourceCategory.BUILD)
        val fuelStock = colony.inventory.amountFor(ResourceCategory.FUEL)
        val oreStock = colony.inventory.amountFor(ResourceCategory.ORE)
        val foodSupply = availableRatio(foodStock, production.food, currentDemand.foodDemand)
        val fuelSupply = if (currentDemand.fuelDemand > 0.0) min(1.0, fuelStock / currentDemand.fuelDemand) else 1.0
        val oreSupply = availableRatio(oreStock, production.ore, currentDemand.oreDemand)
        val poweredIndustry = network.shipIndustry + network.builtIndustry * network.industryStaffFactor * network.industryPowerFactor
        val effectiveIndustry = if (colony.status == ColonyStatus.DEAD || colony.emergencyMode) 0.0 else
            poweredIndustry * oreSupply * network.continuity.efficiencyFactor
        return ColonyMetrics(
            foodProduction = production.food + syntheticFoodRate(colony),
            buildProduction = production.build,
            fuelProduction = production.fuel,
            oreProduction = production.ore,
            foodStock = foodStock,
            buildStock = buildStock,
            fuelStock = fuelStock,
            oreStock = oreStock,
            foodDemand = currentDemand.foodDemand,
            fuelDemand = currentDemand.fuelDemand,
            oreDemand = currentDemand.oreDemand,
            foodSupply = foodSupply,
            fuelSupply = fuelSupply,
            oreSupply = oreSupply,
            foodDays = supplyDays(foodStock, production.food, currentDemand.foodDemand),
            fuelDays = supplyDays(fuelStock, production.fuel, currentDemand.fuelDemand),
            oreDays = if (currentDemand.oreDemand > 0.0) supplyDays(oreStock, production.ore, currentDemand.oreDemand) else null,
            powerDemand = network.powerDemand,
            powerCapacity = network.powerCapacity,
            powerFuelLimitedGeneration = network.fuelLimitedGeneration,
            powerFuelBurn = network.fullFuelBurn,
            powerFactor = if (colony.status == ColonyStatus.DEAD) 0.0 else network.powerFactor,
            lifeSupportPowerFactor = if (colony.status == ColonyStatus.DEAD) 0.0 else network.lifeSupportPowerFactor,
            industryPowerFactor = if (colony.status == ColonyStatus.DEAD) 0.0 else network.industryPowerFactor,
            industryInstalled = network.industryInstalled,
            industry = effectiveIndustry,
            industryLoad = network.industryLoad,
            industrySurvivalFactor = network.industrySurvivalFactor,
            industryCommercialFactor = network.industryCommercialFactor,
            industryStaffFactor = network.industryStaffFactor,
            industryPopulationRequired = network.industryPopulationRequired,
            workforceAvailable = network.workforceAvailable,
            workforceRequired = network.workforceRequired,
            workforceSurvivalFactor = network.workforceSurvivalFactor,
            workforceCommercialFactor = network.workforceCommercialFactor,
            survivalSupply = if (colony.status == ColonyStatus.DEAD) 0.0 else min(foodSupply, network.lifeSupportPowerFactor),
            foodStarvationDays = colony.foodStarvationDays,
            spaceportPowerFactor = if (colony.status == ColonyStatus.DEAD) 0.0 else network.spaceportPowerFactor,
            commandCapacity = network.headquarters.capacity,
            commandLoad = network.headquarters.load,
            commandEfficiency = network.headquarters.efficiency,
            headquartersContinuityFactor = network.continuity.efficiencyFactor,
            headquartersPhase = network.continuity.phase,
            primaryHeadquartersOperational = network.headquarters.primaryOperational,
            headquartersNetworkAvailable = network.continuity.networkAvailable,
            headquartersRecoveryDaysRemaining = network.continuity.recoveryDaysRemaining,
            planetaryResidents = planetaryResidents,
            shipResidents = shipResidents,
            shipFoodDemand = shipFood.requested,
            shipFoodAvailable = shipFood.available,
            shipFoodSupply = shipFood.ratio,
            shipFoodShortestDays = shipFood.shortestDays,
            shipFoodWarning = shipFood.shortestDays?.let { it <= MineItConfig.SHIP_FOOD_WARNING_DAYS } == true,
            shipFoodCritical = shipFood.shortestDays?.let { it < MineItConfig.SHIP_FOOD_CRITICAL_DAYS } == true,
        )
    }

    private fun collect(colony: ColonyState, network: ColonyNetworkSnapshot): CollectionResult {
        var inventory = colony.inventory
        var tiles = colony.world.tiles
        var food = 0.0
        var build = 0.0
        var fuel = 0.0
        var ore = 0.0
        val depleted = mutableListOf<SectorCoordinate>()
        val renewableEvents = mutableListOf<RenewableEvent>()

        for (site in network.activeSites) {
            val current = tiles.first { it.coordinate == site.coordinate }
            val deposit = requireNotNull(current.deposit)
            val rate = collectionRate(colony, current, network)
            if (rate <= 0.0) continue

            val collected: Double
            val nextTile: WorldTile
            if (deposit.sustainability == Sustainability.FINITE) {
                val reserve = (deposit.reserve ?: 0L).toDouble()
                collected = min(rate, reserve)
                val nextReserve = max(0.0, reserve - collected)
                val exhausted = nextReserve <= .0001
                nextTile = current.copy(
                    deposit = deposit.copy(reserve = jsRoundToLong(nextReserve)),
                    resourceExhausted = exhausted,
                    exhaustedResourceId = if (exhausted) deposit.resourceId else current.exhaustedResourceId,
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
                inventory = inventory.store(deposit.resourceId, deposit.category, collected, deposit.quality)
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
            colony.copy(inventory = inventory, world = colony.world.copy(tiles = tiles)),
            Production(food, build, fuel, ore),
            depleted,
            renewableEvents,
        )
    }

    private fun forecastProduction(colony: ColonyState, network: ColonyNetworkSnapshot): Production {
        var food = 0.0
        var build = 0.0
        var fuel = 0.0
        var ore = 0.0
        network.activeSites.forEach { tile ->
            val rate = collectionRate(colony, tile, network)
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

    private fun collectionRate(colony: ColonyState, tile: WorldTile, network: ColonyNetworkSnapshot): Double =
        SiteProductionRules.rate(colony, tile, network)

    private fun updateRenewable(tile: WorldTile): RenewableUpdate {
        val deposit = requireNotNull(tile.deposit)
        val originalRank = deposit.renewableOriginalRank ?: renewableRank(deposit.abundanceLabel)
        var health = deposit.renewableHealth ?: (originalRank + 1.0)
        val intensity = deposit.harvestIntensity.coerceIn(.25, 2.0)
        val beforeLabel = deposit.abundanceLabel ?: "Established"
        val maxScore = originalRank + 1.0
        if (intensity > 1.0) health = max(0.0, health - MineItConfig.RENEWABLE_OVERHARVEST_RATE * (intensity - 1.0))
        else if (intensity < 1.0) health = min(maxScore, health + MineItConfig.RENEWABLE_RECOVERY_RATE * (1.0 - intensity))
        if (health <= 0.0) {
            val next = tile.copy(
                resourceExhausted = true,
                exhaustedResourceId = deposit.resourceId,
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
        val next = tile.copy(deposit = deposit.copy(
            renewableOriginalRank = originalRank,
            renewableHealth = health,
            abundanceLabel = afterLabel,
            abundance = renewableRateFactor(afterLabel),
        ))
        return RenewableUpdate(next, if (afterLabel != beforeLabel) RenewableEvent(tile.coordinate, beforeLabel, afterLabel) else null)
    }

    private fun demand(state: GameState, colony: ColonyState, network: ColonyNetworkSnapshot): Demand {
        val planetaryResidents = fleetService.planetaryResidentCount(state, colony.id)
        val foodDemand = if (colony.status == ColonyStatus.DEAD || planetaryResidents <= 0.0) 0.0 else max(.1, planetaryResidents * MineItConfig.FOOD_PER_COLONIST)
        val staffedIndustry = if (colony.emergencyMode) 0.0 else network.industryCapacity
        val oreDemand = if (colony.emergencyMode) 0.0 else max(
            .05,
            staffedIndustry * (MineItConfig.ORE_PER_INDUSTRY_LEVEL / 100.0) * TechnologyCapabilities.industryOreEfficiency(colony.technology),
        )
        return Demand(foodDemand, network.fullFuelBurn, oreDemand)
    }

    private fun syntheticFoodRate(colony: ColonyState): Double {
        val base = TechnologyCapabilities.syntheticFood(colony.technology)
        return if (colony.contract?.naturalFood == false) base else base * .35
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
            return MineItConfig.CRITICAL_MORTALITY_MIN + t * (MineItConfig.CRITICAL_MORTALITY_MAX - MineItConfig.CRITICAL_MORTALITY_MIN)
        }
        val t = (.4 - max(0.0, stable)) / .4
        return MineItConfig.CRITICAL_MORTALITY_MAX + t * (MineItConfig.COLLAPSE_MORTALITY_MAX - MineItConfig.CRITICAL_MORTALITY_MAX)
    }

    private fun renewableRank(label: String?): Int = when (label?.lowercase()) { "limited" -> 0; "large" -> 2; "vast" -> 3; else -> 1 }
    private fun renewableLabel(rank: Int): String = listOf("Limited", "Established", "Large", "Vast")[rank.coerceIn(0, 3)]
    private fun renewableRateFactor(label: String?): Double = when (label?.lowercase()) { "limited" -> .65; "large" -> 1.45; "vast" -> 2.10; else -> 1.0 }
    private fun jsRoundToLong(value: Double) = floor(value + .5).toLong()

    private fun GameState.withActiveColony(updated: ColonyState): GameState =
        copy(colonies = colonies.map { if (it.id == updated.id) updated else it })

    private data class Demand(val foodDemand: Double, val fuelDemand: Double, val oreDemand: Double)
    private data class Production(val food: Double, val build: Double, val fuel: Double, val ore: Double)
    private data class CollectionResult(val colony: ColonyState, val production: Production, val depletedSites: List<SectorCoordinate>, val renewableEvents: List<RenewableEvent>)
    private data class RenewableUpdate(val tile: WorldTile, val event: RenewableEvent?)

    companion object { private const val STARVATION_GRACE_DAYS = 30 }
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
    val spaceportPowerFactor: Double = 0.0,
    val commandCapacity: Double = 0.0,
    val commandLoad: Double = 0.0,
    val commandEfficiency: Double = 1.0,
    val headquartersContinuityFactor: Double = 1.0,
    val headquartersPhase: HeadquartersContinuityPhase = HeadquartersContinuityPhase.ONLINE,
    val primaryHeadquartersOperational: Boolean = false,
    val headquartersNetworkAvailable: Boolean = true,
    val headquartersRecoveryDaysRemaining: Int = 0,
    val planetaryResidents: Double = 0.0,
    val shipResidents: Double = 0.0,
    val shipFoodDemand: Double = 0.0,
    val shipFoodConsumed: Double = 0.0,
    val shipFoodAvailable: Double = 0.0,
    val shipFoodSupply: Double = 1.0,
    val shipFoodShortestDays: Double? = null,
    val shipFoodWarning: Boolean = false,
    val shipFoodCritical: Boolean = false,
    val shipFoodDeaths: Double = 0.0,
)

data class RenewableEvent(val coordinate: SectorCoordinate, val from: String, val to: String)
data class DailySimulationResult(
    val state: GameState,
    val metrics: ColonyMetrics,
    val deaths: Double,
    val colonyDied: Boolean,
    val completedSurveys: List<SectorCoordinate>,
    val depletedSites: List<SectorCoordinate>,
    val renewableEvents: List<RenewableEvent>,
)
