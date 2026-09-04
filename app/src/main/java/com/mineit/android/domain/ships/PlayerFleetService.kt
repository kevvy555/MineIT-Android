package com.mineit.android.domain.ships

import com.mineit.android.domain.config.MineItConfig
import com.mineit.android.domain.model.ColonyId
import com.mineit.android.domain.model.ColonyState
import com.mineit.android.domain.model.GameState
import com.mineit.android.domain.model.ResourceId
import com.mineit.android.domain.model.ShipId
import com.mineit.android.domain.resources.ResourceCategory
import kotlin.math.max

/**
 * Canonical native owner for corporation fleet facts needed by colony establishment.
 * Ship procurement, travel and market behaviour extend this owner in their later slices.
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

    fun hasCommandShip(state: GameState, colonyId: ColonyId = state.activeColonyId): Boolean =
        dockedShips(state, colonyId).any { it.commandCapable }

    fun industrySupport(state: GameState, colonyId: ColonyId = state.activeColonyId): Double =
        dockedShips(state, colonyId).sumOf { it.industrySupport }

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
        val assignments = colony.shipResidentAssignments.map { assignment ->
            val deaths = (losses[assignment.shipId] ?: 0.0).coerceIn(0.0, assignment.residents)
            totalDeaths += deaths
            assignment.copy(residents = max(0.0, assignment.residents - deaths))
        }
        val updatedColony = colony.copy(
            population = max(0.0, colony.population - totalDeaths),
            shipResidentAssignments = assignments,
        )
        return state.copy(colonies = state.colonies.map { if (it.id == colony.id) updatedColony else it })
    }

    fun moveResidentsAshore(state: GameState, shipId: ShipId, requested: Double): FleetActionResult {
        val colony = state.activeColony
        val assignment = colony.shipResidentAssignments.firstOrNull { it.shipId == shipId }
            ?: return FleetActionResult(false, state, "No colony residents are accommodated aboard that ship.", 0.0)
        val moved = requested.takeIf { it.isFinite() }?.coerceIn(0.0, assignment.residents) ?: 0.0
        if (moved <= .0001) return FleetActionResult(false, state, "Resident transfer amount must be positive.", 0.0)
        val updatedAssignments = colony.shipResidentAssignments.map {
            if (it.shipId == shipId) it.copy(residents = it.residents - moved) else it
        }
        val updatedColony = colony.copy(
            shipResidentAssignments = updatedAssignments,
            planetaryAccommodationResidents = colony.planetaryAccommodationResidents + moved,
        )
        return FleetActionResult(
            ok = true,
            state = state.copy(colonies = state.colonies.map { if (it.id == colony.id) updatedColony else it }),
            message = "Moved ${moved.toInt()} residents ashore.",
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
        val withdrawal = colony.inventory.withdraw(resourceId, requested)
        val moved = withdrawal.stock?.amount ?: 0.0
        if (moved <= .0001) return FleetActionResult(false, state, "No requested colony stock is available.", 0.0)
        val updatedShip = ship.copy(inventory = ship.inventory.store(requireNotNull(withdrawal.stock)))
        val updatedColony = colony.copy(inventory = withdrawal.inventory)
        return FleetActionResult(
            ok = true,
            state = state.copy(
                fleet = state.fleet.copy(ships = state.fleet.ships.map { if (it.id == shipId) updatedShip else it }),
                colonies = state.colonies.map { if (it.id == colony.id) updatedColony else it },
            ),
            message = "Ship loading complete.",
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
)
