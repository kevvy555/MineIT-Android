package com.mineit.android.domain.ships

import com.mineit.android.domain.config.MineItConfig
import com.mineit.android.domain.model.ColonyId
import com.mineit.android.domain.model.ShipId
import com.mineit.android.domain.resources.Inventory
import kotlinx.serialization.Serializable

/** Permanent corporation-owned fleet state. Later ship-market/travel slices extend this owner. */
@Serializable
data class FleetState(
    val ships: List<PlayerShipState> = emptyList(),
    val selectedShipId: ShipId? = null,
) {
    init {
        require(ships.map { it.id }.distinct().size == ships.size) { "Fleet ship IDs must be unique." }
        require(selectedShipId == null || ships.any { it.id == selectedShipId }) {
            "Selected ship must reference an existing fleet ship."
        }
    }
}

@Serializable
data class PlayerShipState(
    val id: ShipId,
    val name: String,
    val dockedColonyId: ColonyId? = null,
    val inventory: Inventory = Inventory(),
    val crew: Int = 0,
    val minimumCrew: Int = MineItConfig.FOUNDING_SHIP_CREW,
    val maximumCrew: Int = 40,
    val passengerManifest: Int = 0,
    val passengerCapacity: Int = 250,
    val accommodationCapacity: Int = 290,
    val cargoCapacity: Double = 8_000.0,
    val foodCapacity: Double = 2_000.0,
    val fuelCapacity: Double = 2_000.0,
    val industrySupport: Double = 0.0,
    val commandCapable: Boolean = true,
    val residentFoodStarvationDays: Int = 0,
) {
    init {
        require(name.isNotBlank()) { "Ship name must not be blank." }
        require(crew >= 0) { "Ship crew must not be negative." }
        require(minimumCrew >= 0) { "Ship minimum crew must not be negative." }
        require(maximumCrew >= minimumCrew) { "Ship maximum crew must not be below its minimum crew." }
        require(passengerManifest >= 0) { "Ship passenger manifest must not be negative." }
        require(passengerCapacity >= 0) { "Ship passenger capacity must not be negative." }
        require(accommodationCapacity >= 0) { "Ship accommodation capacity must not be negative." }
        require(cargoCapacity.isFinite() && cargoCapacity >= 0.0) { "Ship cargo capacity must be finite and non-negative." }
        require(foodCapacity.isFinite() && foodCapacity >= 0.0) { "Ship Food capacity must be finite and non-negative." }
        require(fuelCapacity.isFinite() && fuelCapacity >= 0.0) { "Ship Fuel capacity must be finite and non-negative." }
        require(industrySupport.isFinite() && industrySupport >= 0.0) { "Ship Industry support must be finite and non-negative." }
        require(residentFoodStarvationDays >= 0) { "Ship-resident starvation days must not be negative." }
    }
}

@Serializable
data class ShipResidentAssignment(
    val shipId: ShipId,
    val residents: Double,
) {
    init {
        require(residents.isFinite() && residents >= 0.0) { "Ship resident assignment must be finite and non-negative." }
    }
}
