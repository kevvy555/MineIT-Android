package com.mineit.android.domain.model

import com.mineit.android.domain.config.MineItConfig
import com.mineit.android.domain.contracts.Contract01
import com.mineit.android.domain.resources.StarterInventory
import com.mineit.android.domain.trade.TradeState
import com.mineit.android.domain.world.LandGenerator
import com.mineit.android.domain.world.WorldState

/** Canonical deterministic native start-state factory for Contract 01. */
class NewGameFactory(
    private val landGenerator: LandGenerator = LandGenerator(),
) {
    fun contract01(
        colonySeed: Long,
        colonyId: ColonyId = ColonyId("intro-$colonySeed"),
        colonyName: String = "Colony 01",
    ): GameState {
        val contract = Contract01.create(uid = colonyId.value, colonyName = colonyName)
        val candidates = landGenerator.generateCandidates(seed = colonySeed, contractUid = contract.uid, archetypeId = contract.archetypeId)
        val colony = ColonyState(
            id = colonyId,
            name = colonyName,
            population = MineItConfig.START_POPULATION,
            seed = colonySeed,
            inventory = StarterInventory.contract01(),
            contract = contract,
            status = ColonyStatus.SITE_SELECTION,
            technology = TechnologyLevels(),
            world = WorldState(landingCandidates = candidates),
            trade = TradeState(nextArrivalAbsoluteDay = contract.startAbsoluteDay + MineItConfig.FIRST_TRADE_DAY - 1),
        )
        return GameState(
            date = GameDate(year = 1, day = 1),
            company = CompanyState(cash = MineItConfig.START_CASH, reputation = 0, technology = TechnologyLevels()),
            colonies = listOf(colony),
            activeColonyId = colonyId,
        )
    }

    fun settleLandingSite(state: GameState, index: Int): GameState {
        val colony = state.activeColony
        val candidate = colony.world.landingCandidates.getOrNull(index) ?: error("Landing site $index does not exist.")
        val settledWorld = colony.world.copy(
            selectedLandingSiteIndex = index,
            tiles = candidate.cells.map { cell ->
                com.mineit.android.domain.world.WorldTile(
                    coordinate = cell.coordinate,
                    terrain = cell.terrain,
                    terrainVariant = cell.variant,
                )
            },
            activeSurveys = emptyList(),
            surveyQueue = emptyList(),
        )
        val updatedColony = colony.copy(status = ColonyStatus.PLAYING, world = settledWorld)
        return state.copy(colonies = state.colonies.map { if (it.id == updatedColony.id) updatedColony else it })
    }
}
