package com.mineit.android.domain.contracts

import kotlinx.serialization.Serializable

@Serializable
data class RequiredTechnology(
    val power: Int,
    val food: Int,
    val mining: Int,
    val scanning: Int,
)

@Serializable
data class ResourceWeights(
    val food: Double,
    val build: Double,
    val fuel: Double,
    val ore: Double,
)

@Serializable
data class ContractGoals(
    val food: Int,
    val industry: Int,
    val population: Int,
)

@Serializable
data class ContractBands(
    val silver: Long,
    val gold: Long,
    val platinum: Long,
)

@Serializable
data class ContractState(
    val uid: String,
    val tier: Int,
    val colonyTier: Int,
    val name: String,
    val colonyName: String,
    val archetypeId: String,
    val environment: String,
    val hazard: String,
    val supportSystem: String,
    val supportLoad: Double,
    val techAccess: String,
    val requiredTechnology: RequiredTechnology,
    val resourceWeights: ResourceWeights,
    val rareMultiplier: Double,
    val reserveMultiplier: Double,
    val scanMultiplier: Double,
    val costMultiplier: Double,
    val naturalFood: Boolean,
    val years: Int,
    val goals: ContractGoals,
    val bands: ContractBands,
    val startAbsoluteDay: Int = 1,
    val foundedAbsoluteDay: Int = 1,
    val ended: Boolean = false,
    val completed: Boolean = false,
)

/** Current introductory MineIT contract from the pinned web baseline. */
object Contract01 {
    fun create(uid: String, colonyName: String = "Colony 01"): ContractState {
        require(uid.isNotBlank()) { "Contract uid must not be blank." }
        return ContractState(
            uid = uid,
            tier = 1,
            colonyTier = 1,
            name = "Koplin Mining Charter — Contract 01",
            colonyName = colonyName,
            archetypeId = "temperate",
            environment = "Temperate / breathable",
            hazard = "Minimal environmental support required.",
            supportSystem = "Open Habitat",
            supportLoad = 1.0,
            techAccess = "direct",
            requiredTechnology = RequiredTechnology(power = 1, food = 1, mining = 1, scanning = 1),
            resourceWeights = ResourceWeights(food = .35, build = .25, fuel = .18, ore = .22),
            rareMultiplier = 1.0,
            reserveMultiplier = 1.0,
            scanMultiplier = 1.0,
            costMultiplier = 1.0,
            naturalFood = true,
            years = 10,
            goals = ContractGoals(food = 120, industry = 520, population = 1_050),
            bands = ContractBands(silver = 450_000, gold = 1_000_000, platinum = 2_200_000),
        )
    }
}
