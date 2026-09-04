package com.mineit.android.domain.colony

import com.mineit.android.domain.config.MineItConfig
import com.mineit.android.domain.model.GameState
import com.mineit.android.domain.world.DevelopmentKind
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/** Canonical Housing + priority-Power population support used by colonist transfer gates. */
class PopulationSupportService {
    fun capacity(state: GameState, network: ColonyNetworkSnapshot): PopulationSupportCapacity {
        val colony = state.activeColony
        val housingDevelopments = colony.world.tiles.mapNotNull { it.development }.filter {
            it.kind == DevelopmentKind.HOUSING && it.constructionComplete && !it.productionStopped
        }
        val builtHousing = housingDevelopments.sumOf(InfrastructureRules::capacity)
        val housingCapacity = max(MineItConfig.START_HOUSING.toDouble(), builtHousing)
        val housingFixedPower = housingDevelopments.sumOf { InfrastructureRules.housingFixedPower(it.level) }
        val headquartersPower = colony.world.tiles.filter { tile ->
            tile.coordinate in network.poweredHeadquarters && tile.development?.kind == DevelopmentKind.HEADQUARTERS
        }.sumOf { tile -> InfrastructureRules.headquartersPower(requireNotNull(tile.development).level) }
        val powerForPeople = max(0.0, network.fuelLimitedGeneration - headquartersPower - housingFixedPower)
        val perPerson = MineItConfig.LIFE_SUPPORT_POWER_PER_COLONIST * (colony.contract?.supportLoad ?: 1.0)
        val powerCapacity = if (perPerson > 0.0) floor(powerForPeople / perPerson) else housingCapacity
        val supported = min(housingCapacity, powerCapacity).coerceAtLeast(0.0)
        return PopulationSupportCapacity(
            housingCapacity = floor(housingCapacity).toInt(),
            powerCapacity = floor(powerCapacity).toInt().coerceAtLeast(0),
            supportedPopulationCapacity = floor(supported).toInt(),
        )
    }
}

data class PopulationSupportCapacity(
    val housingCapacity: Int,
    val powerCapacity: Int,
    val supportedPopulationCapacity: Int,
)
