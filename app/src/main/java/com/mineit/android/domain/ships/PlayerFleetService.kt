package com.mineit.android.domain.ships

import com.mineit.android.domain.colony.ColonyNetworkSnapshot
import com.mineit.android.domain.config.MineItConfig
import com.mineit.android.domain.model.ColonyId
import com.mineit.android.domain.model.ColonyState
import com.mineit.android.domain.model.GameState
import com.mineit.android.domain.model.ResourceId
import com.mineit.android.domain.model.ShipId
import com.mineit.android.domain.resources.ResourceCategory
import kotlin.math.floor
import kotlin.math.max

/**
 * Canonical native owner for corporation fleet facts needed by colony establishment and the
 * docked single-colony ship surface. Ship procurement and travel extend this owner later.
 */
class PlayerFleetService {
    fun dockedShips(state: GameState, colonyId: ColonyId = state.activeColonyId): List<PlayerShipState> =
        state.fleet.ships.filter { it.dockedColonyId == colonyId }

    fun foundingShip(state: GameState, colony: ColonyState = state.activeColony): PlayerShipState? =
        colony.foundingShipId?.let { id -> state.fleet.ships.firstOrNull { it.id == id } }

    fun shipResidentCount(colony: ColonyState, shipId: ShipId): Double =
        colony.shipResidentAssignments.firstOrNull { it.shipId == shipId }?.residents ?: 0.0

    fun totalShipResidents(colony: ColonyState): Double = colony.shipResidentAssignments.sumOf { it.residents }

    fun planetaryResidentCount(state: GameState, colonyId: ColonyId = state.activeColonyId): Double {
        val colony = state.colonies.first { it.id == colonyId }
        return max(0.0, colony.population - totalShipResidents(colony))
    }

    fun homelessResidentCount(state: GameState, colonyId: ColonyId = state.activeColonyId): Double {
        val colony = state.colonies.first { it.id == colonyId }
        return max(0.0, planetaryResidentCount(state, colonyId) - colony.planetaryAccommodationResidents)
    }

    fun hasCommandShip(state: GameState, colonyId: ColonyId = state.activeColonyId): Boolean =
        dockedShips(state, colonyId).any { it.commandCapable }

    fun industrySupport(state: GameState, colonyId: ColonyId = state.activeColonyId): Double =
        dockedShips(state, colonyId).sumOf { it.industrySupport }

    fun generalCargoLoad(ship: PlayerShipState): Double =
        ship.inventory.amountFor(ResourceCategory.BUILD) + ship.inventory.amountFor(ResourceCategory.ORE)

    fun foodLoad(ship: PlayerShipState): Double = ship.inventory.amountFor(ResourceCategory.FOOD)
    fun fuelLoad(ship: PlayerShipState): Double = ship.inventory.amountFor(ResourceCategory.FUEL)
    fun totalPhysicalLoad(ship: PlayerShipState): Double = generalCargoLoad(ship) + foodLoad(ship) + fuelLoad(ship)
    fun totalPhysicalCapacity(ship: PlayerShipState): Double = ship.cargoCapacity + ship.foodCapacity + ship.fuelCapacity

    fun loadForCategory(ship: PlayerShipState, category: ResourceCategory): Double = when (category) {
        ResourceCategory.FOOD -> foodLoad(ship)
        ResourceCategory.FUEL -> fuelLoad(ship)
        ResourceCategory.BUILD, ResourceCategory.ORE -> generalCargoLoad(ship)
    }

    fun capacityForCategory(ship: PlayerShipState, category: ResourceCategory): Double = when (category) {
        ResourceCategory.FOOD -> ship.foodCapacity
        ResourceCategory.FUEL -> ship.fuelCapacity
        ResourceCategory.BUILD, ResourceCategory.ORE -> ship.cargoCapacity
    }

    fun remainingCapacityForCategory(ship: PlayerShipState, category: ResourceCategory): Double =
        max(0.0, capacityForCategory(ship, category) - loadForCategory(ship, category))

    fun residentFoodStatus(state: GameState): ShipResidentFoodStatus {
        val colony = state.activeColony
        val rows = colony.shipResidentAssignments.mapNotNull { assignment ->
            val ship = state.fleet.ships.firstOrNull { it.id == assignment.shipId } ?: return@mapNotNull null
            residentFoodRow(ship, assignment.residents, consumed = 0.0)
        }
        return summarizeFood(rows)
    }

    fun consumeResidentFood(state: GameState): ShipResidentFoodConsumption {
        val colony = state.activeColony
        var ships = state.fleet.ships
        val rows = mutableListOf<ShipResidentFoodRow>()
        for (assignment in colony.shipResidentAssignments) {
            val index = ships.indexOfFirst { it.id == assignment.shipId }
            if (index < 0 || assignment.residents <= .0001) continue
            val ship = ships[index]
            val requested = assignment.residents * MineItConfig.FOOD_PER_COLONIST
            val use = ship.inventory.consumeCategory(ResourceCategory.FOOD, requested)
            val noFood = requested > 0.0 && use.consumed <= .0001
            val starvationDays = if (noFood) ship.residentFoodStarvationDays + 1 else 0
            val updated = ship.copy(
                inventory = use.inventory,
                residentFoodStarvationDays = starvationDays,
            )
            ships = ships.toMutableList().also { it[index] = updated }
            rows += residentFoodRow(updated, assignment.residents, consumed = use.consumed, requestedOverride = requested)
        }
        val updatedState = state.copy(fleet = state.fleet.copy(ships = ships))
        val summary = summarizeFood(rows)
        return ShipResidentFoodConsumption(
            state = updatedState,
            requested = summary.requested,
            consumed = rows.sumOf { it.consumed },
            ratio = if (summary.requested > 0.0) (rows.sumOf { it.consumed } / summary.requested).coerceIn(0.0, 1.0) else 1.0,
            rows = rows,
            shortestDays = summary.shortestDays,
        )
    }

    fun applyResidentDeaths(state: GameState, losses: Map<ShipId, Double>): GameState {
        if (losses.isEmpty()) return state
        val colony = state.activeColony
        var totalDeaths = 0.0
        val assignments = colony.shipResidentAssignments.mapNotNull { assignment ->
            val deaths = (losses[assignment.shipId] ?: 0.0).coerceIn(0.0, assignment.residents)
            totalDeaths += deaths
            val remaining = max(0.0, assignment.residents - deaths)
            if (remaining > .0001) assignment.copy(residents = remaining) else null
        }
        val updatedColony = colony.copy(
            population = max(0.0, colony.population - totalDeaths),
            shipResidentAssignments = assignments,
        )
        return state.copy(colonies = state.colonies.map { if (it.id == colony.id) updatedColony else it })
    }

    fun moveResidentsAshore(
        state: GameState,
        shipId: ShipId,
        requested: Double,
        housingCapacity: Double,
        network: ColonyNetworkSnapshot,
        spaceportServicesAvailable: Boolean,
        confirmed: Boolean = false,
    ): FleetActionResult {
        val colony = state.activeColony
        val ship = state.fleet.ships.firstOrNull { it.id == shipId }
            ?: return FleetActionResult(false, state, "Ship not found.", 0.0)
        if (ship.dockedColonyId != colony.id) {
            return FleetActionResult(false, state, "The selected player ship is not docked at this colony.", 0.0)
        }
        val assignment = colony.shipResidentAssignments.firstOrNull { it.shipId == shipId }
            ?: return FleetActionResult(false, state, "No colony residents are accommodated aboard that ship.", 0.0)
        val freeHousing = max(0.0, housingCapacity - colony.planetaryAccommodationResidents)
        val normalized = requested.takeIf { it.isFinite() }?.coerceAtLeast(0.0) ?: 0.0
        val moved = minOf(assignment.residents, freeHousing, floor(normalized))
        if (moved <= .0001) {
            val reason = if (freeHousing <= .0001) "No planetary accommodation is available." else "Resident transfer amount must be positive."
            return FleetActionResult(false, state, reason, 0.0)
        }
        if (!spaceportServicesAvailable) {
            return FleetActionResult(false, state, "Powered Spaceport services are required to move residents ashore.", 0.0)
        }
        if (network.powerCapacity <= .0001) {
            return FleetActionResult(false, state, "Build and power a colony Power Plant before moving residents into habitats.", 0.0)
        }
        val supportLoad = max(.5, colony.contract?.supportLoad ?: 1.0)
        val incrementalSupport = moved * MineItConfig.LIFE_SUPPORT_POWER_PER_COLONIST * supportLoad
        val projectedDemand = network.powerDemand + incrementalSupport
        val availableGeneration = network.fuelLimitedGeneration
        val shortage = max(0.0, projectedDemand - availableGeneration)
        if (shortage > .0001 && !confirmed) {
            return FleetActionResult(
                ok = false,
                state = state,
                message = "Power shortage warning: projected demand is %.1f against %.1f available generation (%.1f short). Transfer anyway?".format(projectedDemand, availableGeneration, shortage),
                amount = moved,
                requiresConfirmation = true,
                projectedDemand = projectedDemand,
                availableGeneration = availableGeneration,
            )
        }
        val updatedAssignments = colony.shipResidentAssignments.mapNotNull {
            if (it.shipId != shipId) it else {
                val remaining = it.residents - moved
                if (remaining > .0001) it.copy(residents = remaining) else null
            }
        }
        val updatedColony = colony.copy(
            shipResidentAssignments = updatedAssignments,
            planetaryAccommodationResidents = colony.planetaryAccommodationResidents + moved,
        )
        return FleetActionResult(
            ok = true,
            state = state.copy(colonies = state.colonies.map { if (it.id == colony.id) updatedColony else it }),
            message = "Moved ${moved.toInt()} residents ashore${if (shortage > .0001) " with a projected Power shortage" else ""}.",
            amount = moved,
            projectedDemand = projectedDemand,
            availableGeneration = availableGeneration,
        )
    }

    /** Web-parity inverse of [moveResidentsAshore]: homeless residents are boarded first. */
    fun moveResidentsAboard(
        state: GameState,
        shipId: ShipId,
        requested: Double,
        spaceportServicesAvailable: Boolean,
    ): FleetActionResult {
        val colony = state.activeColony
        val ship = state.fleet.ships.firstOrNull { it.id == shipId }
            ?: return FleetActionResult(false, state, "Ship not found.", 0.0)
        if (ship.dockedColonyId != colony.id) {
            return FleetActionResult(false, state, "The selected player ship is not docked at this colony.", 0.0)
        }
        if (!spaceportServicesAvailable) {
            return FleetActionResult(false, state, "Powered Spaceport services are required to move residents aboard.", 0.0)
        }
        val aboard = shipResidentCount(colony, shipId)
        val room = max(0.0, ship.accommodationCapacity - aboard)
        val ashore = planetaryResidentCount(state, colony.id)
        val normalized = requested.takeIf { it.isFinite() }?.coerceAtLeast(0.0) ?: 0.0
        val moved = minOf(room, ashore, floor(normalized))
        if (moved <= .0001) {
            val reason = when {
                room <= .0001 -> "Ship accommodation is full."
                ashore <= .0001 -> "No colony residents can be moved aboard."
                else -> "Resident transfer amount must be positive."
            }
            return FleetActionResult(false, state, reason, 0.0)
        }

        val homelessBefore = homelessResidentCount(state, colony.id)
        val movedFromPlanetaryHousing = max(0.0, moved - minOf(moved, homelessBefore))
        val nextAssignments = colony.shipResidentAssignments
            .filterNot { it.shipId == shipId }
            .plus(ShipResidentAssignment(shipId, aboard + moved))
        val updatedColony = colony.copy(
            shipResidentAssignments = nextAssignments,
            planetaryAccommodationResidents = max(0.0, colony.planetaryAccommodationResidents - movedFromPlanetaryHousing),
        )
        return FleetActionResult(
            ok = true,
            state = state.copy(colonies = state.colonies.map { if (it.id == colony.id) updatedColony else it }),
            message = "Moved ${moved.toInt()} residents aboard ${ship.name}.",
            amount = moved,
        )
    }

    fun transferFromShipToColony(
        state: GameState,
        shipId: ShipId,
        resourceId: ResourceId,
        requested: Double,
        spaceportServicesAvailable: Boolean,
    ): FleetActionResult {
        val colony = state.activeColony
        val ship = state.fleet.ships.firstOrNull { it.id == shipId }
            ?: return FleetActionResult(false, state, "Ship not found.", 0.0)
        if (ship.dockedColonyId != colony.id) return FleetActionResult(false, state, "Ship is not docked at this colony.", 0.0)
        val bootstrap = colony.foundingShipId == shipId && !colony.headquarters.commandHandoverComplete
        if (!bootstrap && !spaceportServicesAvailable) {
            return FleetActionResult(false, state, "Powered Spaceport services are required for this transfer.", 0.0)
        }
        val withdrawal = ship.inventory.withdraw(resourceId, requested)
        val moved = withdrawal.stock?.amount ?: 0.0
        if (moved <= .0001) return FleetActionResult(false, state, "No requested stock is available aboard the ship.", 0.0)
        val updatedShip = ship.copy(inventory = withdrawal.inventory)
        val updatedColony = colony.copy(inventory = colony.inventory.store(requireNotNull(withdrawal.stock)))
        return FleetActionResult(
            ok = true,
            state = state.copy(
                fleet = state.fleet.copy(ships = state.fleet.ships.map { if (it.id == shipId) updatedShip else it }),
                colonies = state.colonies.map { if (it.id == colony.id) updatedColony else it },
            ),
            message = if (bootstrap) "Founding-ship bootstrap unload complete." else "Ship unload complete.",
            amount = moved,
        )
    }

    fun transferFromColonyToShip(
        state: GameState,
        shipId: ShipId,
        resourceId: ResourceId,
        requested: Double,
        spaceportServicesAvailable: Boolean,
    ): FleetActionResult {
        val colony = state.activeColony
        val ship = state.fleet.ships.firstOrNull { it.id == shipId }
            ?: return FleetActionResult(false, state, "Ship not found.", 0.0)
        if (ship.dockedColonyId != colony.id) return FleetActionResult(false, state, "Ship is not docked at this colony.", 0.0)
        if (!spaceportServicesAvailable) {
            return FleetActionResult(false, state, "Powered Spaceport services are required for loading.", 0.0)
        }
        val source = colony.inventory.find(resourceId)
            ?: return FleetActionResult(false, state, "No requested colony stock is available.", 0.0)
        val normalized = requested.takeIf { it.isFinite() }?.coerceAtLeast(0.0) ?: 0.0
        if (normalized <= .0001) return FleetActionResult(false, state, "Ship loading amount must be positive.", 0.0)
        val remainingCapacity = remainingCapacityForCategory(ship, source.category)
        if (remainingCapacity <= .0001) {
            val label = when (source.category) {
                ResourceCategory.FOOD -> "Food store"
                ResourceCategory.FUEL -> "Fuel tank"
                ResourceCategory.BUILD, ResourceCategory.ORE -> "general cargo hold"
            }
            return FleetActionResult(false, state, "Ship $label is full.", 0.0)
        }
        val allowed = minOf(normalized, source.amount, remainingCapacity)
        val withdrawal = colony.inventory.withdraw(resourceId, allowed)
        val moved = withdrawal.stock?.amount ?: 0.0
        if (moved <= .0001) return FleetActionResult(false, state, "No requested colony stock is available.", 0.0)
        val updatedShip = ship.copy(inventory = ship.inventory.store(requireNotNull(withdrawal.stock)))
        val updatedColony = colony.copy(inventory = withdrawal.inventory)
        val clippedByCapacity = moved + .0001 < minOf(normalized, source.amount)
        return FleetActionResult(
            ok = true,
            state = state.copy(
                fleet = state.fleet.copy(ships = state.fleet.ships.map { if (it.id == shipId) updatedShip else it }),
                colonies = state.colonies.map { if (it.id == colony.id) updatedColony else it },
            ),
            message = if (clippedByCapacity) "Loaded ${moved.toInt()} units; ship storage capacity reached." else "Ship loading complete.",
            amount = moved,
        )
    }

    private fun residentFoodRow(
        ship: PlayerShipState,
        residents: Double,
        consumed: Double,
        requestedOverride: Double? = null,
    ): ShipResidentFoodRow {
        val requested = requestedOverride ?: residents * MineItConfig.FOOD_PER_COLONIST
        val available = ship.inventory.amountFor(ResourceCategory.FOOD)
        val ratio = if (requested > 0.0) (if (consumed > 0.0) consumed / requested else minOf(1.0, available / requested)) else 1.0
        val days = if (requested > .0001) available / requested else null
        return ShipResidentFoodRow(
            shipId = ship.id,
            residents = residents,
            requested = requested,
            consumed = consumed,
            available = available,
            ratio = ratio.coerceIn(0.0, 1.0),
            starvationDays = ship.residentFoodStarvationDays,
            daysRemaining = days,
        )
    }

    private fun summarizeFood(rows: List<ShipResidentFoodRow>): ShipResidentFoodStatus = ShipResidentFoodStatus(
        requested = rows.sumOf { it.requested },
        available = rows.sumOf { it.available },
        ratio = if (rows.sumOf { it.requested } > 0.0) {
            rows.sumOf { minOf(it.available, it.requested) } / rows.sumOf { it.requested }
        } else 1.0,
        rows = rows,
        shortestDays = rows.mapNotNull { it.daysRemaining }.minOrNull(),
    )
}

data class ShipResidentFoodRow(
    val shipId: ShipId,
    val residents: Double,
    val requested: Double,
    val consumed: Double,
    val available: Double,
    val ratio: Double,
    val starvationDays: Int,
    val daysRemaining: Double?,
)

data class ShipResidentFoodStatus(
    val requested: Double,
    val available: Double,
    val ratio: Double,
    val rows: List<ShipResidentFoodRow>,
    val shortestDays: Double?,
)

data class ShipResidentFoodConsumption(
    val state: GameState,
    val requested: Double,
    val consumed: Double,
    val ratio: Double,
    val rows: List<ShipResidentFoodRow>,
    val shortestDays: Double?,
)

data class FleetActionResult(
    val ok: Boolean,
    val state: GameState,
    val message: String,
    val amount: Double,
    val requiresConfirmation: Boolean = false,
    val projectedDemand: Double? = null,
    val availableGeneration: Double? = null,
)
