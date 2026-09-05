package com.mineit.android.app

import com.mineit.android.domain.colony.ColonyDevelopmentService
import com.mineit.android.domain.colony.ColonyNetworkSnapshot
import com.mineit.android.domain.colony.InfrastructureRules
import com.mineit.android.domain.colony.SiteOperationRules
import com.mineit.android.domain.colony.SiteProductionRules
import com.mineit.android.domain.model.ColonyStatus
import com.mineit.android.domain.model.GameState
import com.mineit.android.domain.resources.ExtractionCompatibility
import com.mineit.android.domain.resources.ResourceCategory
import com.mineit.android.domain.ships.PlayerFleetService
import com.mineit.android.domain.simulation.ColonyMetrics
import com.mineit.android.domain.world.DevelopmentKind
import com.mineit.android.domain.world.SectorCoordinate
import com.mineit.android.domain.world.Sustainability
import com.mineit.android.domain.world.TileDevelopment
import com.mineit.android.domain.world.WorldTile
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Application-derived adaptive-building presentation. Gameplay facts stay in domain services;
 * Compose receives a typed, immutable description and only renders/dispatches actions.
 */
class DevelopmentDetailPolicy(
    private val development: ColonyDevelopmentService = ColonyDevelopmentService(),
    private val fleet: PlayerFleetService = PlayerFleetService(),
) {
    fun detail(
        state: GameState,
        coordinate: SectorCoordinate,
        network: ColonyNetworkSnapshot,
        metrics: ColonyMetrics,
    ): DevelopmentDetail? {
        val colony = state.activeColony
        val tile = colony.world.tileAt(coordinate) ?: return null
        val dev = tile.development ?: return null
        // Current web routes Headquarters taps to Colony Control, not adaptive-building details.
        if (dev.kind == DevelopmentKind.HEADQUARTERS) return null
        return if (dev.kind == DevelopmentKind.EXTRACT) {
            extractionDetail(state, tile, network)
        } else {
            localDetail(state, tile, network, metrics)
        }
    }

    private fun localDetail(
        state: GameState,
        tile: WorldTile,
        network: ColonyNetworkSnapshot,
        metrics: ColonyMetrics,
    ): DevelopmentDetail {
        val colony = state.activeColony
        val dev = requireNotNull(tile.development)
        val label = localLabel(dev.kind)
        val capacity = InfrastructureRules.capacity(dev)
        val status = when {
            colony.status == ColonyStatus.DEAD -> "OFFLINE"
            dev.productionStopped -> "STOPPED"
            else -> "ACTIVE"
        }
        val overview = mutableListOf<DevelopmentDetailCard>()
        val operations = mutableListOf<DevelopmentDetailCard>()

        when (dev.kind) {
            DevelopmentKind.HOUSING -> {
                val total = development.installedHousing(colony)
                val occupied = fleet.planetaryResidentCount(state, colony.id)
                val free = max(0.0, total - occupied)
                val requested = InfrastructureRules.housingFixedPower(dev.level)
                val factor = network.lifeSupportPowerFactor.coerceIn(0.0, 1.0)
                overview += card("THIS BUILDING", "+${amount(capacity)}", "Housing capacity")
                overview += card("COLONY HOUSING", amount(total))
                overview += card("OCCUPIED", amount(occupied), "Planetary residents only")
                overview += card(
                    "FREE SPACES",
                    amount(free),
                    if (free <= max(10.0, total * .10)) "Near capacity" else "Available",
                    if (free <= max(10.0, total * .10)) DevelopmentDetailTone.WARN else DevelopmentDetailTone.READY,
                )
                overview += card(
                    "POWER",
                    "${amount(requested * factor)} / ${amount(requested)}",
                    "Life-support priority • ${(factor * 100).roundToInt()}% delivered",
                    if (factor < .999) DevelopmentDetailTone.BLOCKED else DevelopmentDetailTone.READY,
                )
            }

            DevelopmentKind.POWER -> {
                val delivered = network.powerDemand * network.powerFactor
                overview += card("THIS BUILDING", "+${amount(capacity)} POWER")
                overview += card("ONLINE CAPACITY", amount(network.powerCapacity))
                overview += card("FUEL-LIMITED", amount(network.fuelLimitedGeneration))
                overview += card(
                    "DEMAND",
                    amount(network.powerDemand),
                    if (network.powerFactor < .999) "Demand exceeds delivered generation" else "Fully supplied",
                    if (network.powerFactor < .999) DevelopmentDetailTone.BLOCKED else DevelopmentDetailTone.READY,
                )
                operations += card("DELIVERED", amount(delivered))
                operations += card("UNUSED", amount(max(0.0, network.fuelLimitedGeneration - delivered)))
                operations += card("FULL FUEL BURN", amount(network.fullFuelBurn), "Fuel / day")
                operations += card("ACTUAL FUEL USED", amount(metrics.powerFuelConsumed), "Last completed day")
            }

            DevelopmentKind.INDUSTRY -> {
                val requested = InfrastructureRules.industryIdlePower(dev.level) +
                    capacity * network.industryStaffFactor * InfrastructureRules.INDUSTRY_VARIABLE_POWER_PER_CAPACITY
                val factor = network.industryPowerFactor.coerceIn(0.0, 1.0)
                overview += card("THIS BUILDING", "+${amount(capacity)} INDUSTRY")
                overview += card("COLONY INDUSTRY", amount(network.industryInstalled), "Includes docked ship support")
                overview += card("OPERATIONAL", amount(network.industryCapacity))
                overview += card(
                    "STAFFING FACTOR",
                    "${(network.industryStaffFactor * 100).roundToInt()}%",
                    if (network.industryStaffFactor < .999) "Population constrained" else "Fully supported",
                    if (network.industryStaffFactor < .999) DevelopmentDetailTone.BLOCKED else DevelopmentDetailTone.READY,
                )
                overview += card(
                    "POWER",
                    "${amount(requested * factor)} / ${amount(requested)}",
                    "${(factor * 100).roundToInt()}% operating Power",
                    if (factor < .999) DevelopmentDetailTone.BLOCKED else DevelopmentDetailTone.READY,
                )
                operations += card("POPULATION REQUIRED", amount(network.industryPopulationRequired))
                operations += card("INDUSTRY LOAD", amount(network.industryLoad))
                operations += card("BUILT INDUSTRY", amount(network.builtIndustry))
                operations += card("SHIP SUPPORT", amount(network.shipIndustry), "Self-powered")
            }

            else -> Unit
        }

        val covered = tile.resourceCovered && tile.deposit != null
        val alert = when {
            colony.status == ColonyStatus.DEAD -> DevelopmentDetailAlert(
                "COLONY LOST",
                "This building is offline because the colony population has been lost.",
                DevelopmentDetailTone.BLOCKED,
            )
            dev.productionStopped -> DevelopmentDetailAlert(
                "PRODUCTION STOPPED",
                "This building is currently stopped and contributes no active capacity.",
                DevelopmentDetailTone.WARN,
            )
            covered -> DevelopmentDetailAlert(
                "RESOURCE COVERED",
                "${tile.deposit?.name ?: "A known resource"} lies beneath this building and cannot be exploited until the building is demolished.",
                DevelopmentDetailTone.WARN,
            )
            else -> null
        }

        return DevelopmentDetail(
            coordinate = tile.coordinate,
            kicker = "COLONY BUILDING",
            name = label,
            level = dev.level,
            status = status,
            badges = listOf(dev.kind.name, tile.terrain.name) + if (covered) listOf("RESOURCE BELOW") else emptyList(),
            alert = alert,
            overview = overview,
            operations = operations,
            upgrade = localUpgrade(state, tile),
        )
    }

    private fun extractionDetail(
        state: GameState,
        tile: WorldTile,
        network: ColonyNetworkSnapshot,
    ): DevelopmentDetail {
        val colony = state.activeColony
        val dev = requireNotNull(tile.development)
        val deposit = requireNotNull(tile.deposit)
        val family = ExtractionCompatibility.familyFor(deposit.resourceId)
        val requestedPower = InfrastructureRules.facilityPower(family, dev.level)
        val powerFactor = (network.sitePowerFactors[siteId(tile)] ?: 0.0).coerceIn(0.0, 1.0)
        val survival = deposit.category in survivalCategories
        val workforceFactor = if (survival) network.workforceSurvivalFactor else network.workforceCommercialFactor
        val industryFactor = if (survival) network.industrySurvivalFactor else network.industryCommercialFactor
        val currentOutput = if (network.activeSites.any { it.coordinate == tile.coordinate }) {
            SiteProductionRules.rate(colony, tile, network)
        } else {
            0.0
        }
        val nominal = SiteProductionRules.potential(colony, tile)
        val workforce = SiteOperationRules.workforceRequirement(colony, tile)
        val finite = deposit.sustainability == Sustainability.FINITE
        val remaining = if (finite) amount((deposit.reserve ?: 0L).toDouble()) else deposit.abundanceLabel ?: "Renewable"
        val stock = colony.inventory.amountFor(deposit.resourceId)
        val lifeYears = if (finite && currentOutput > .0001) {
            (deposit.reserve ?: 0L).toDouble() / (currentOutput * 360.0)
        } else {
            null
        }
        val status = when {
            colony.status == ColonyStatus.DEAD -> "OFFLINE"
            tile.resourceExhausted || deposit.renewableWiped -> "DEPLETED"
            dev.productionStopped -> "STOPPED"
            colony.emergencyMode && !survival -> "EMERGENCY PAUSE"
            currentOutput <= .0001 -> "BLOCKED"
            else -> "ACTIVE"
        }

        val overview = listOf(
            card(
                "OUTPUT",
                "+${amount(currentOutput)} / DAY",
                if (currentOutput + .0001 < nominal) "Nominal ${amount(nominal)} / day" else "Current production",
                if (currentOutput <= .0001) DevelopmentDetailTone.BLOCKED else DevelopmentDetailTone.READY,
            ),
            card(
                if (finite) "RESOURCE LEFT" else "RESOURCE CONDITION",
                remaining,
                if (finite) "Deposit remaining" else "Renewable resource",
            ),
            card("STAFF", amount(workforce), "Required workers"),
            card(
                "POWER",
                "${amount(requestedPower * powerFactor)} / ${amount(requestedPower)}",
                "${(powerFactor * 100).roundToInt()}% delivered • ${if (survival) "SURVIVAL" else "COMMERCIAL"} priority",
                if (powerFactor < .999) DevelopmentDetailTone.BLOCKED else DevelopmentDetailTone.READY,
            ),
        )
        val operations = mutableListOf(
            card("QUALITY", "Q${deposit.quality}"),
            card("IN STOCK", amount(stock), deposit.name),
            card(
                if (finite) "EST. LIFE" else "HARVEST",
                if (finite) lifeYears?.let(::years) ?: "—" else "${(deposit.harvestIntensity * 100).roundToInt()}%",
                if (finite) "At current delivered output" else "Current harvest intensity",
            ),
            card(
                "WORKFORCE",
                "${(workforceFactor * 100).roundToInt()}%",
                if (workforceFactor < .999) "Workforce constrained" else "Fully staffed",
                if (workforceFactor < .999) DevelopmentDetailTone.BLOCKED else DevelopmentDetailTone.READY,
            ),
            card(
                "INDUSTRY SUPPORT",
                "${(industryFactor * 100).roundToInt()}%",
                if (industryFactor < .999) "Industry constrained" else "Fully supported",
                if (industryFactor < .999) DevelopmentDetailTone.BLOCKED else DevelopmentDetailTone.READY,
            ),
        )

        val alert = when {
            colony.status == ColonyStatus.DEAD -> DevelopmentDetailAlert("COLONY LOST", "This site is offline because the colony population has been lost.", DevelopmentDetailTone.BLOCKED)
            colony.contract?.ended == true -> DevelopmentDetailAlert("CONTRACT ENDED", "Extraction is unavailable after the mining contract ends.", DevelopmentDetailTone.BLOCKED)
            tile.resourceExhausted || deposit.renewableWiped -> DevelopmentDetailAlert("RESOURCE DEPLETED", "This resource can no longer produce output.", DevelopmentDetailTone.BLOCKED)
            dev.productionStopped -> DevelopmentDetailAlert("PRODUCTION STOPPED", "This extraction site has been stopped.", DevelopmentDetailTone.WARN)
            colony.emergencyMode && !survival -> DevelopmentDetailAlert("EMERGENCY PAUSE", "Commercial extraction is paused while the colony is in emergency mode.", DevelopmentDetailTone.WARN)
            powerFactor < .999 -> DevelopmentDetailAlert("POWER CONSTRAINED", "Delivered Power is reducing site output.", DevelopmentDetailTone.WARN)
            workforceFactor < .999 -> DevelopmentDetailAlert("WORKFORCE CONSTRAINED", "Available workers are reducing site output.", DevelopmentDetailTone.WARN)
            industryFactor < .999 -> DevelopmentDetailAlert("INDUSTRY CONSTRAINED", "Available Industry is reducing site output.", DevelopmentDetailTone.WARN)
            else -> null
        }

        return DevelopmentDetail(
            coordinate = tile.coordinate,
            kicker = "${deposit.category.name} EXTRACTION",
            name = family.displayName,
            level = dev.level,
            status = status,
            badges = listOf(deposit.category.name, tile.terrain.name, "Q${deposit.quality}", deposit.name.uppercase()),
            alert = alert,
            overview = overview,
            operations = operations,
            upgrade = extractionUpgrade(state, tile, network),
        )
    }

    private fun localUpgrade(state: GameState, tile: WorldTile): DevelopmentUpgradeDetail {
        val colony = state.activeColony
        val dev = requireNotNull(tile.development)
        if (dev.level >= InfrastructureRules.MAX_LEVEL) {
            return DevelopmentUpgradeDetail(max = true, nextLevel = dev.level, improvement = "Maximum building level reached.")
        }
        val nextLevel = dev.level + 1
        val preview = development.buildingUpgradePreview(state, tile.coordinate)
        val currentCapacity = InfrastructureRules.capacity(dev)
        val nextCapacity = InfrastructureRules.capacity(TileDevelopment(dev.kind, nextLevel))
        val cost = InfrastructureRules.buildingCost(dev.kind, nextLevel, tile.terrain)
        val buildStock = colony.inventory.amountFor(ResourceCategory.BUILD)
        val oreStock = colony.inventory.amountFor(ResourceCategory.ORE)
        val techCurrent = when (dev.kind) {
            DevelopmentKind.HOUSING -> colony.technology.housing
            DevelopmentKind.POWER -> colony.technology.power
            DevelopmentKind.INDUSTRY -> colony.technology.industry
            else -> nextLevel
        }
        return DevelopmentUpgradeDetail(
            max = false,
            nextLevel = nextLevel,
            improvement = "+${amount(max(0.0, nextCapacity - currentCapacity))} ${dev.kind.name}",
            ready = preview.ok,
            reason = preview.reason,
            requirements = listOf(
                requirement("BUILD", amount(cost.build), "${amount(buildStock)} available", buildStock + .0001 >= cost.build),
                requirement("ORE", amount(cost.ore), "${amount(oreStock)} available", oreStock + .0001 >= cost.ore),
                requirement("${dev.kind.name} TECH", "L$nextLevel", "Current L$techCurrent", techCurrent >= nextLevel),
            ).filterNot { it.required == "0" },
        )
    }

    private fun extractionUpgrade(
        state: GameState,
        tile: WorldTile,
        network: ColonyNetworkSnapshot,
    ): DevelopmentUpgradeDetail {
        val colony = state.activeColony
        val dev = requireNotNull(tile.development)
        val deposit = requireNotNull(tile.deposit)
        if (dev.level >= InfrastructureRules.MAX_LEVEL) {
            return DevelopmentUpgradeDetail(max = true, nextLevel = dev.level, improvement = "Maximum extraction level reached.")
        }
        val nextLevel = dev.level + 1
        val preview = development.extractionUpgradePreview(state, tile.coordinate)
        val family = ExtractionCompatibility.familyFor(deposit.resourceId)
        val currentPotential = SiteProductionRules.potential(colony, tile)
        val nextPotential = SiteProductionRules.potential(colony, tile, nextLevel)
        val buildStock = colony.inventory.amountFor(ResourceCategory.BUILD)
        val oreStock = colony.inventory.amountFor(ResourceCategory.ORE)
        val currentWorkers = SiteOperationRules.workforceRequirement(colony, tile)
        val nextWorkers = SiteOperationRules.workforceRequirement(colony, tile, nextLevel)
        val extraWorkers = max(0.0, nextWorkers - currentWorkers)
        val freeWorkers = max(0.0, network.workforceAvailable - network.workforceRequired)
        val powerGate = InfrastructureRules.facilityUpgradePowerGate(family, nextLevel)
        val techLabel = if (deposit.category == ResourceCategory.FOOD) "FOOD TECH" else "MINING TECH"
        val techRequired = if (deposit.category == ResourceCategory.FOOD) nextLevel else deposit.requiredMiningLevel
        val techCurrent = if (deposit.category == ResourceCategory.FOOD) colony.technology.food else colony.technology.mining
        val requirements = mutableListOf<DevelopmentRequirement>()
        preview.cost?.let { cost ->
            requirements += requirement("BUILD", amount(cost.build), "${amount(buildStock)} available", buildStock + .0001 >= cost.build)
            if (cost.ore > .0001) requirements += requirement("ORE", amount(cost.ore), "${amount(oreStock)} available", oreStock + .0001 >= cost.ore)
        }
        requirements += requirement(techLabel, "L$techRequired", "Current L$techCurrent", techCurrent >= techRequired)
        requirements += requirement("POWER CAPACITY", amount(powerGate), "${amount(network.powerCapacity)} installed", network.powerCapacity + .0001 >= powerGate)
        if (extraWorkers > .0001) requirements += requirement("EXTRA WORKFORCE", amount(extraWorkers), "${amount(freeWorkers)} free", freeWorkers + .0001 >= extraWorkers)
        if (!preview.ok && !preview.reason.isNullOrBlank()) {
            requirements += requirement("BLOCKING GATE", "NOT READY", preview.reason, false)
        }
        return DevelopmentUpgradeDetail(
            max = false,
            nextLevel = nextLevel,
            improvement = "+${amount(max(0.0, nextPotential - currentPotential))} / DAY nominal output",
            ready = preview.ok,
            reason = preview.reason,
            requirements = requirements,
        )
    }

    private fun localLabel(kind: DevelopmentKind): String = when (kind) {
        DevelopmentKind.HOUSING -> "Housing"
        DevelopmentKind.POWER -> "Power Plant"
        DevelopmentKind.INDUSTRY -> "Industry"
        DevelopmentKind.HEADQUARTERS -> "Headquarters"
        DevelopmentKind.EXTRACT -> "Extraction Site"
    }

    private fun card(
        label: String,
        value: String,
        detail: String = "",
        tone: DevelopmentDetailTone = DevelopmentDetailTone.NEUTRAL,
    ) = DevelopmentDetailCard(label, value, detail, tone)

    private fun requirement(label: String, required: String, current: String, ready: Boolean) =
        DevelopmentRequirement(label, required, current, ready)

    private fun amount(value: Double): String {
        if (!value.isFinite()) return "—"
        if (kotlin.math.abs(value - value.roundToInt()) < .05) return value.roundToInt().toString()
        return String.format(Locale.ROOT, "%.1f", value)
    }

    private fun years(value: Double): String = when {
        value >= 100.0 -> "${value.roundToInt()} YEARS"
        value >= 1.0 -> String.format(Locale.ROOT, "%.1f YEARS", value)
        else -> String.format(Locale.ROOT, "%.1f MONTHS", value * 12.0)
    }

    private fun siteId(tile: WorldTile): String = "site:${tile.coordinate.x},${tile.coordinate.y}"

    private val survivalCategories = setOf(ResourceCategory.FOOD, ResourceCategory.FUEL)
}

enum class DevelopmentDetailTone { NEUTRAL, READY, WARN, BLOCKED }

data class DevelopmentDetailCard(
    val label: String,
    val value: String,
    val detail: String = "",
    val tone: DevelopmentDetailTone = DevelopmentDetailTone.NEUTRAL,
)

data class DevelopmentDetailAlert(
    val title: String,
    val text: String,
    val tone: DevelopmentDetailTone,
)

data class DevelopmentRequirement(
    val label: String,
    val required: String,
    val current: String,
    val ready: Boolean,
)

data class DevelopmentUpgradeDetail(
    val max: Boolean,
    val nextLevel: Int,
    val improvement: String,
    val ready: Boolean = false,
    val reason: String? = null,
    val requirements: List<DevelopmentRequirement> = emptyList(),
)

data class DevelopmentDetail(
    val coordinate: SectorCoordinate,
    val kicker: String,
    val name: String,
    val level: Int,
    val status: String,
    val badges: List<String>,
    val alert: DevelopmentDetailAlert?,
    val overview: List<DevelopmentDetailCard>,
    val operations: List<DevelopmentDetailCard>,
    val upgrade: DevelopmentUpgradeDetail,
)
