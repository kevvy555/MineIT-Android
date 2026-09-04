package com.mineit.android.domain

data class GameState(
    val year: Int,
    val day: Int,
    val resources: ResourceStockpile,
    val colony: Colony,
    val sectors: List<Sector>,
)

data class ResourceStockpile(
    val food: Int,
    val water: Int,
    val ore: Int,
    val credits: Int,
)

data class Colony(
    val name: String,
    val population: Int,
    val powerAvailable: Int,
    val powerDemand: Int,
)

data class Sector(
    val coordinate: SectorCoordinate,
    val richness: Int,
    val surveyed: Boolean,
)

data class SectorCoordinate(
    val x: Int,
    val y: Int,
)
