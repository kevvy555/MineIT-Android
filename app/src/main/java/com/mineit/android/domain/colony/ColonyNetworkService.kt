package com.mineit.android.domain.colony

import com.mineit.android.domain.config.MineItConfig
import com.mineit.android.domain.model.ColonyState
import com.mineit.android.domain.model.ColonyStatus
import com.mineit.android.domain.model.GameState
import com.mineit.android.domain.resources.ExtractionCompatibility
import com.mineit.android.domain.resources.ResourceCategory
import com.mineit.android.domain.world.DevelopmentKind
import com.mineit.android.domain.world.SectorCoordinate
import com.mineit.android.domain.world.WorldTile
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round

/**
 * Canonical derived colony network owner. Phase 4 moves shared workforce/Industry/Power/HQ
 * calculations here so simulation, construction gates and UI consume one result.
 */
class ColonyNetworkService(
    private val headquartersService: HeadquartersService = HeadquartersService(),
) {
    fun calculate(state: GameState, fuelStock: Double = state.activeColony.inventory.amountFor(ResourceCategory.FUEL)): ColonyNetworkSnapshot {
        val colony = state.activeColony
        if (colony.status == ColonyStatus.SITE_SELECTION) return ColonyNetworkSnapshot.empty()
        val sites = activeSites(colony)
        val hqStaffing = headquartersService.network(state, emptySet())
        val workforce = workforce(colony, sites, hqStaffing.reservedStaff)
        val industry = industry(colony, sites)
        val power = power(state, sites, workforce, industry, fuelStock)
        val hq = headquartersService.network(state, power.poweredHeadquarters)
        val continuity = headquartersService.continuity(state, hq)
        return ColonyNetworkSnapshot(
            activeSites = sites,
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
            lifeSupportPowerFactor = power.lifeSupportPowerFactor,
            industryPowerFactor = power.industryPowerFactor,
            spaceportPowerFactor = power.spaceportPowerFactor,
            sitePowerFactors = power.siteFactors,
            poweredHeadquarters = power.poweredHeadquarters,
            headquarters = hq,
            continuity = continuity,
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
                !tile.resourceCovered &&
                deposit != null &&
                !deposit.renewableWiped &&
                colony.technology.mining >= deposit.requiredMiningLevel &&
                (!colony.emergencyMode || deposit.category == ResourceCategory.FOOD || deposit.category == ResourceCategory.FUEL)
        }
    }

    private fun workforce(colony: ColonyState, sites: List<WorldTile>, hqReserved: Double): Workforce {
        val base = if (colony.status == ColonyStatus.DEAD || colony.foodStarvationDays > 0) 0.0 else floor(colony.population * MineItConfig.WORKFORCE_SHARE)
        val available = max(0.0, base - hqReserved)
        var survivalRequired = 0.0
        var commercialRequired = 0.0
        sites.forEach { tile ->
            val required = SiteOperationRules.workforceRequirement(colony, tile)
            if (tile.deposit?.category in setOf(ResourceCategory.FOOD, ResourceCategory.FUEL)) survivalRequired += required else commercialRequired += required
        }
        val survivalFactor = if (survivalRequired > 0) (available / survivalRequired).coerceIn(0.0, 1.0) else 1.0
        val remaining = max(0.0, available - survivalRequired)
        val commercialFactor = if (colony.emergencyMode) 0.0 else if (commercialRequired > 0) (remaining / commercialRequired).coerceIn(0.0, 1.0) else 1.0
        return Workforce(available, survivalRequired + commercialRequired, survivalFactor, commercialFactor)
    }

    private fun industry(colony: ColonyState, sites: List<WorldTile>): Industry {
        val buildingCapacity = colony.world.tiles.filter {
            it.development?.kind == DevelopmentKind.INDUSTRY && it.development.constructionComplete && !it.development.productionStopped
        }.sumOf { InfrastructureRules.capacity(requireNotNull(it.development)) }
        val installed = buildingCapacity + if (colony.foundingShipDocked) InfrastructureRules.FOUNDING_SHIP_INDUSTRY else 0.0
        val populationRequired = max(40.0, round(installed * .8 * TechnologyCapabilities.industryWorkforceEfficiency(colony.technology)))
        val staffFactor = (colony.population / max(1.0, populationRequired)).coerceIn(0.0, 1.0)
        val capacity = if (colony.status == ColonyStatus.DEAD || colony.emergencyMode) 0.0 else installed * staffFactor
        var survivalLoad = 0.0
        var commercialLoad = 0.0
        sites.forEach { tile ->
            val load = SiteOperationRules.industryLoad(tile.development?.level ?: 1)
            if (tile.deposit?.category in setOf(ResourceCategory.FOOD, ResourceCategory.FUEL)) survivalLoad += load else commercialLoad += load
        }
        val survivalFactor = if (survivalLoad > 0) (capacity / survivalLoad).coerceIn(0.0, 1.0) else 1.0
        val remaining = max(0.0, capacity - survivalLoad)
        val commercialFactor = if (colony.emergencyMode) 0.0 else if (commercialLoad > 0) (remaining / commercialLoad).coerceIn(0.0, 1.0) else 1.0
        return Industry(installed, capacity, survivalLoad + commercialLoad, survivalLoad, commercialLoad, survivalFactor, commercialFactor, staffFactor, populationRequired)
    }

    private fun power(
        state: GameState,
        sites: List<WorldTile>,
        workforce: Workforce,
        industry: Industry,
        fuelStock: Double,
    ): Power {
        val colony = state.activeColony
        val powerBuildings = colony.world.tiles.filter {
            it.development?.kind == DevelopmentKind.POWER && it.development.constructionComplete && !it.development.productionStopped
        }
        val capacity = powerBuildings.sumOf { InfrastructureRules.capacity(requireNotNull(it.development)) }
        val fullFuelBurn = capacity * TechnologyCapabilities.fuelIntensity(colony.technology)
        val fuelRatio = if (fullFuelBurn > 0) (fuelStock / fullFuelBurn).coerceIn(0.0, 1.0) else 0.0
        val available = capacity * fuelRatio

        val preliminaryHq = headquartersService.network(state, emptySet())
        val primary = colony.headquarters.primary
        val orderedHq = preliminaryHq.rows.sortedWith(
            compareByDescending<HeadquartersRow> { it.coordinate == primary }
                .thenByDescending { it.level }
                .thenBy { it.coordinate.x }
                .thenBy { it.coordinate.y },
        )
        var remaining = available
        val poweredHq = mutableSetOf<SectorCoordinate>()
        var commandConnected = colony.foundingShipDocked
        for (row in orderedHq) {
            if (!row.constructed || !row.staffed) continue
            if (row.coordinate != primary && !commandConnected) continue
            if (remaining + .0001 >= row.requiredPower) {
                remaining -= row.requiredPower
                poweredHq += row.coordinate
                if (row.coordinate == primary) commandConnected = true
            }
        }
        val hqRequested = orderedHq.filter { it.constructed && it.staffed }.sumOf { it.requiredPower }
        val hqDelivered = orderedHq.filter { it.coordinate in poweredHq }.sumOf { it.requiredPower }

        val housingPower = colony.world.tiles.filter {
            it.development?.kind == DevelopmentKind.HOUSING && it.development.constructionComplete && !it.development.productionStopped
        }.sumOf { InfrastructureRules.housingFixedPower(requireNotNull(it.development).level) }
        val lifeSupport = colony.population * MineItConfig.LIFE_SUPPORT_POWER_PER_COLONIST *
            (colony.contract?.supportLoad ?: 1.0) *
            (if (colony.emergencyMode) MineItConfig.EMERGENCY_LIFE_SUPPORT_MULTIPLIER else 1.0)
        val lifeRequested = housingPower + lifeSupport
        val lifeDelivered = min(remaining, lifeRequested)
        val lifeFactor = if (lifeRequested > 0) lifeDelivered / lifeRequested else 1.0
        remaining -= lifeDelivered

        val industryRows = mutableListOf<Pair<Double, Boolean>>()
        colony.world.tiles.filter {
            it.development?.kind == DevelopmentKind.INDUSTRY && it.development.constructionComplete && !it.development.productionStopped
        }.forEach { tile ->
            val dev = requireNotNull(tile.development)
            val installedCapacity = InfrastructureRules.capacity(dev)
            val idle = InfrastructureRules.industryIdlePower(dev.level)
            val variable = if (colony.emergencyMode) 0.0 else installedCapacity * industry.staffFactor * InfrastructureRules.INDUSTRY_VARIABLE_POWER_PER_CAPACITY
            industryRows += (idle + variable) to false
        }
        if (colony.foundingShipDocked && !colony.emergencyMode) {
            industryRows += (InfrastructureRules.FOUNDING_SHIP_INDUSTRY * industry.staffFactor * InfrastructureRules.INDUSTRY_VARIABLE_POWER_PER_CAPACITY) to false
        }
        val industryTotal = industryRows.sumOf { it.first }
        val operationalIndustry = max(0.0, industry.capacity)
        val survivalIndustry = min(operationalIndustry, industry.survivalLoad)
        val survivalShare = if (operationalIndustry > 0) survivalIndustry / operationalIndustry else 0.0
        val industrySurvivalDemand = industryTotal * survivalShare
        val industryCommercialDemand = industryTotal * (1.0 - survivalShare)

        val survivalSiteRows = sites.filter { it.deposit?.category in setOf(ResourceCategory.FOOD, ResourceCategory.FUEL) }
        val commercialSiteRows = sites.filterNot { it.deposit?.category in setOf(ResourceCategory.FOOD, ResourceCategory.FUEL) }
        val survivalSiteDemand = survivalSiteRows.sumOf { InfrastructureRules.facilityPower(ExtractionCompatibility.familyFor(requireNotNull(it.deposit).resourceId), requireNotNull(it.development).level) }
        val survivalDemand = survivalSiteDemand + industrySurvivalDemand
        val survivalDelivered = min(remaining, survivalDemand)
        val survivalFactor = if (survivalDemand > 0) survivalDelivered / survivalDemand else 1.0
        remaining -= survivalDelivered

        val spaceportDelivered = if (remaining + .0001 >= InfrastructureRules.BASIC_SPACEPORT_POWER) InfrastructureRules.BASIC_SPACEPORT_POWER else 0.0
        val spaceportFactor = if (spaceportDelivered > 0) 1.0 else 0.0
        remaining -= spaceportDelivered

        val commercialSiteDemand = commercialSiteRows.sumOf { InfrastructureRules.facilityPower(ExtractionCompatibility.familyFor(requireNotNull(it.deposit).resourceId), requireNotNull(it.development).level) }
        val commercialDemand = commercialSiteDemand + industryCommercialDemand
        val commercialDelivered = min(max(0.0, remaining), commercialDemand)
        val commercialFactor = if (commercialDemand > 0) commercialDelivered / commercialDemand else 1.0
        remaining -= commercialDelivered

        val siteFactors = buildMap {
            survivalSiteRows.forEach { put(siteId(it), survivalFactor) }
            commercialSiteRows.forEach { put(siteId(it), commercialFactor) }
        }
        val industryRequested = industrySurvivalDemand + industryCommercialDemand
        val industryDelivered = industrySurvivalDemand * survivalFactor + industryCommercialDemand * commercialFactor
        val industryPowerFactor = if (industryRequested > 0) industryDelivered / industryRequested else 1.0
        val demand = hqRequested + lifeRequested + survivalDemand + InfrastructureRules.BASIC_SPACEPORT_POWER + commercialDemand
        val delivered = hqDelivered + lifeDelivered + survivalDelivered + spaceportDelivered + commercialDelivered
        return Power(
            demand = demand,
            capacity = capacity,
            fullFuelBurn = fullFuelBurn,
            fuelLimitedGeneration = available,
            powerFactor = if (demand > 0) (delivered / demand).coerceIn(0.0, 1.0) else 1.0,
            lifeSupportPowerFactor = lifeFactor,
            industryPowerFactor = industryPowerFactor,
            spaceportPowerFactor = spaceportFactor,
            siteFactors = siteFactors,
            poweredHeadquarters = poweredHq,
        )
    }

    private fun siteId(tile: WorldTile) = "site:${tile.coordinate.x},${tile.coordinate.y}"

    private data class Workforce(val available: Double, val required: Double, val survivalFactor: Double, val commercialFactor: Double)
    private data class Industry(
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
    private data class Power(
        val demand: Double,
        val capacity: Double,
        val fullFuelBurn: Double,
        val fuelLimitedGeneration: Double,
        val powerFactor: Double,
        val lifeSupportPowerFactor: Double,
        val industryPowerFactor: Double,
        val spaceportPowerFactor: Double,
        val siteFactors: Map<String, Double>,
        val poweredHeadquarters: Set<SectorCoordinate>,
    )
}

data class ColonyNetworkSnapshot(
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
    val spaceportPowerFactor: Double,
    val sitePowerFactors: Map<String, Double>,
    val poweredHeadquarters: Set<SectorCoordinate>,
    val headquarters: HeadquartersNetwork,
    val continuity: HeadquartersContinuity,
) {
    companion object {
        fun empty(): ColonyNetworkSnapshot {
            val hq = HeadquartersNetwork(emptyList(), null, null, false, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0)
            val continuity = HeadquartersContinuity(HeadquartersContinuityPhase.ONLINE, false, false, true, 0.0, 1.0, 1.0, 0, 0, false, "Headquarters network online.", hq, HeadquartersOutageState())
            return ColonyNetworkSnapshot(emptyList(), 0.0, 0.0, 1.0, 1.0, 0.0, 0.0, 0.0, 1.0, 1.0, 1.0, 0.0, 0.0, 0.0, 1.0, 1.0, 1.0, 0.0, emptyMap(), emptySet(), hq, continuity)
        }
    }
}
