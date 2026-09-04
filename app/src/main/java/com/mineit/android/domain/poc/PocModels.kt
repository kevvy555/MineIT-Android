package com.mineit.android.domain.poc

/**
 * Temporary proof-of-concept models retained only until the production migration state exists.
 * Names are deliberately POC-scoped so they cannot become accidental production contracts.
 */
data class PocGameState(
    val year: Int,
    val day: Int,
    val resources: PocResourceStockpile,
    val colony: PocColony,
    val sectors: List<PocSector>,
)

data class PocResourceStockpile(
    val food: Int,
    val water: Int,
    val ore: Int,
    val credits: Int,
)

data class PocColony(
    val name: String,
    val population: Int,
    val powerAvailable: Int,
    val powerDemand: Int,
)

data class PocSector(
    val coordinate: PocSectorCoordinate,
    val richness: Int,
    val surveyed: Boolean,
)

data class PocSectorCoordinate(
    val x: Int,
    val y: Int,
)
