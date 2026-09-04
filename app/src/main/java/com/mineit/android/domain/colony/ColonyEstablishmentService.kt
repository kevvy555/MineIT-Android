package com.mineit.android.domain.colony

import com.mineit.android.domain.model.GameState
import com.mineit.android.domain.model.ShipId
import com.mineit.android.domain.resources.ResourceCategory
import com.mineit.android.domain.ships.FleetActionResult
import com.mineit.android.domain.ships.PlayerFleetService
import com.mineit.android.domain.world.DevelopmentKind

/** Canonical native owner for the one-time N05 founding-ship-to-colony handover. */
class ColonyEstablishmentService(
    private val fleetService: PlayerFleetService = PlayerFleetService(),
    private val headquartersService: HeadquartersService = HeadquartersService(fleetService),
) {
    fun assessment(
        state: GameState,
        network: ColonyNetworkSnapshot,
        foodProduction: Double = 0.0,
        fuelProduction: Double = 0.0,
    ): ColonyEstablishmentAssessment {
        val colony = state.activeColony
        val ship = fleetService.foundingShip(state, colony)
        val shipResidents = ship?.let { fleetService.shipResidentCount(colony, it.id) } ?: 0.0
        val planetaryResidents = fleetService.planetaryResidentCount(state)
        val shipFood = fleetService.residentFoodStatus(state).rows.firstOrNull { it.shipId == ship?.id }
        val housingCapacity = builtHousingCapacity(state)
        val resourceSplit = ResourceCategory.entries.associateWith { category ->
            EstablishmentResourceSplit(
                ship = ship?.inventory?.amountFor(category) ?: 0.0,
                colony = colony.inventory.amountFor(category),
            )
        }
        val headquartersReady = headquartersService.departureGate(state).ok
        val support = mapOf(
            EstablishmentStep.SUPPLIES to when {
                resourceSplit.getValue(ResourceCategory.BUILD).colony > .0001 || resourceSplit.getValue(ResourceCategory.FUEL).colony > .0001 -> EstablishmentSupportStatus.HYBRID
                else -> EstablishmentSupportStatus.SHIP
            },
            EstablishmentStep.SURVEY to if (colony.world.tiles.any { it.revealed }) EstablishmentSupportStatus.READY else EstablishmentSupportStatus.SHIP,
            EstablishmentStep.HOUSING to when {
                network.powerCapacity > .0001 && housingCapacity > .0001 -> EstablishmentSupportStatus.READY
                network.powerCapacity > .0001 || housingCapacity > .0001 -> EstablishmentSupportStatus.HYBRID
                else -> EstablishmentSupportStatus.SHIP
            },
            EstablishmentStep.RESIDENTS to when {
                planetaryResidents <= .0001 -> EstablishmentSupportStatus.SHIP
                shipResidents > .0001 -> EstablishmentSupportStatus.HYBRID
                else -> EstablishmentSupportStatus.COLONY
            },
            EstablishmentStep.FOOD to when {
                foodProduction > .0001 -> EstablishmentSupportStatus.READY
                resourceSplit.getValue(ResourceCategory.FOOD).colony > .0001 -> EstablishmentSupportStatus.HYBRID
                else -> EstablishmentSupportStatus.SHIP
            },
            EstablishmentStep.FUEL to when {
                fuelProduction > .0001 -> EstablishmentSupportStatus.READY
                resourceSplit.getValue(ResourceCategory.FUEL).colony > .0001 -> EstablishmentSupportStatus.HYBRID
                else -> EstablishmentSupportStatus.SHIP
            },
            EstablishmentStep.INDUSTRY to when {
                network.builtIndustry > .0001 -> EstablishmentSupportStatus.READY
                network.shipIndustry > .0001 -> EstablishmentSupportStatus.SHIP
                else -> EstablishmentSupportStatus.COLONY
            },
            EstablishmentStep.HEADQUARTERS to if (headquartersReady) EstablishmentSupportStatus.READY else EstablishmentSupportStatus.SHIP,
        )
        val required = colony.foundingShipId != null && !colony.headquarters.commandHandoverComplete
        val phase = when {
            !required -> EstablishmentPhase.INDEPENDENT
            planetaryResidents <= .0001 -> EstablishmentPhase.SHIP
            shipResidents > .0001 -> EstablishmentPhase.HYBRID
            headquartersReady -> EstablishmentPhase.READY
            else -> EstablishmentPhase.COLONY
        }
        return ColonyEstablishmentAssessment(
            required = required,
            acknowledged = colony.establishmentAcknowledged,
            phase = phase,
            foundingShipId = ship?.id,
            foundingShipName = ship?.name,
            shipResidents = shipResidents,
            planetaryResidents = planetaryResidents,
            planetaryAccommodationResidents = colony.planetaryAccommodationResidents,
            shipAccommodationCapacity = ship?.accommodationCapacity ?: 0,
            shipCrew = ship?.crew ?: 0,
            shipMinimumCrew = ship?.minimumCrew ?: 0,
            shipFoodAvailable = shipFood?.available ?: 0.0,
            shipFoodDaysRemaining = shipFood?.daysRemaining,
            housingCapacity = housingCapacity,
            resourceSplit = resourceSplit,
            support = support,
        )
    }

    fun acknowledge(state: GameState): EstablishmentActionResult {
        val colony = state.activeColony
        if (colony.foundingShipId == null || colony.headquarters.commandHandoverComplete) {
            return EstablishmentActionResult(false, state, "This colony is not awaiting a founding handover.")
        }
        val updated = colony.copy(establishmentAcknowledged = true)
        return EstablishmentActionResult(
            ok = true,
            state = state.copy(colonies = state.colonies.map { if (it.id == colony.id) updated else it }),
            message = "Colony operations started at 1×. Follow the establishment checklist as systems move ashore.",
        )
    }

    fun unloadCategory(
        state: GameState,
        category: ResourceCategory,
        spaceportServicesAvailable: Boolean,
    ): FleetActionResult {
        val ship = fleetService.foundingShip(state)
            ?: return FleetActionResult(false, state, "Founding ship not found.", 0.0)
        val resources = ship.inventory.resources.filter { it.category == category && it.amount > .0001 }
        if (resources.isEmpty()) return FleetActionResult(false, state, "No ${category.name.lowercase()} stock remains aboard the founding ship.", 0.0)
        var working = state
        var moved = 0.0
        for (stock in resources) {
            val result = fleetService.transferFromShipToColony(
                state = working,
                shipId = ship.id,
                resourceId = stock.resourceId,
                requested = stock.amount,
                spaceportServicesAvailable = spaceportServicesAvailable,
            )
            if (!result.ok) return if (moved > .0001) FleetActionResult(true, working, "Partially unloaded ${category.name.lowercase()} stock.", moved) else result
            working = result.state
            moved += result.amount
        }
        return FleetActionResult(true, working, "Unloaded ${moved.toInt()} ${category.name} from the founding ship.", moved)
    }

    fun residentTransferPreview(
        state: GameState,
        network: ColonyNetworkSnapshot,
        requested: Double,
        spaceportServicesAvailable: Boolean,
        confirmed: Boolean = false,
    ): FleetActionResult {
        val ship = fleetService.foundingShip(state)
            ?: return FleetActionResult(false, state, "Founding ship not found.", 0.0)
        return fleetService.moveResidentsAshore(
            state = state,
            shipId = ship.id,
            requested = requested,
            housingCapacity = builtHousingCapacity(state),
            network = network,
            spaceportServicesAvailable = spaceportServicesAvailable,
            confirmed = confirmed,
        )
    }

    fun builtHousingCapacity(state: GameState): Double = state.activeColony.world.tiles
        .mapNotNull { it.development }
        .filter { it.kind == DevelopmentKind.HOUSING && it.constructionComplete && !it.productionStopped }
        .sumOf(InfrastructureRules::capacity)
}

enum class EstablishmentPhase { SHIP, HYBRID, COLONY, READY, INDEPENDENT }
enum class EstablishmentSupportStatus { SHIP, HYBRID, COLONY, READY }
enum class EstablishmentStep { SUPPLIES, SURVEY, HOUSING, RESIDENTS, FOOD, FUEL, INDUSTRY, HEADQUARTERS }

data class EstablishmentResourceSplit(val ship: Double, val colony: Double)

data class ColonyEstablishmentAssessment(
    val required: Boolean,
    val acknowledged: Boolean,
    val phase: EstablishmentPhase,
    val foundingShipId: ShipId?,
    val foundingShipName: String?,
    val shipResidents: Double,
    val planetaryResidents: Double,
    val planetaryAccommodationResidents: Double,
    val shipAccommodationCapacity: Int,
    val shipCrew: Int,
    val shipMinimumCrew: Int,
    val shipFoodAvailable: Double,
    val shipFoodDaysRemaining: Double?,
    val housingCapacity: Double,
    val resourceSplit: Map<ResourceCategory, EstablishmentResourceSplit>,
    val support: Map<EstablishmentStep, EstablishmentSupportStatus>,
)

data class EstablishmentActionResult(val ok: Boolean, val state: GameState, val message: String)
