package com.mineit.android.domain

object DemoGame {
    fun initialState(): GameState = GameState(
        year = 1,
        day = 1,
        resources = ResourceStockpile(
            food = 120,
            water = 180,
            ore = 40,
            credits = 2_500,
        ),
        colony = Colony(
            name = "Koplin Prospect",
            population = 24,
            powerAvailable = 32,
            powerDemand = 24,
        ),
        sectors = buildSectors(),
    )

    private fun buildSectors(): List<Sector> = buildList {
        repeat(GRID_SIZE) { y ->
            repeat(GRID_SIZE) { x ->
                add(
                    Sector(
                        coordinate = SectorCoordinate(x = x - GRID_OFFSET, y = y - GRID_OFFSET),
                        richness = ((x * 17 + y * 31) % 9) + 1,
                        surveyed = (x + y) % 4 == 0,
                    ),
                )
            }
        }
    }

    private const val GRID_SIZE = 6
    private const val GRID_OFFSET = 3
}
