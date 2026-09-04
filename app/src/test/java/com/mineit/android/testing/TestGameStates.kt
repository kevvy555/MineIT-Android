package com.mineit.android.testing

import com.mineit.android.domain.model.ColonyId
import com.mineit.android.domain.model.ColonyState
import com.mineit.android.domain.model.CompanyState
import com.mineit.android.domain.model.GameDate
import com.mineit.android.domain.model.GameState
import com.mineit.android.domain.model.ResourceId
import com.mineit.android.domain.resources.Inventory
import com.mineit.android.domain.resources.QualityBand
import com.mineit.android.domain.resources.ResourceCategory
import com.mineit.android.domain.resources.ResourceStock

internal object TestGameStates {
    val colonyId = ColonyId("test-colony-1")

    fun foundationState(
        cash: Double = 500_000.0,
        population: Double = 120.0,
        date: GameDate = GameDate(1, 1),
    ): GameState = GameState(
        date = date,
        company = CompanyState(
            cash = cash,
            reputation = 0,
        ),
        colonies = listOf(
            ColonyState(
                id = colonyId,
                name = "Koplin Prospect",
                population = population,
                seed = 123_456_789,
                inventory = Inventory(
                    listOf(
                        ResourceStock(
                            resourceId = ResourceId("fungal"),
                            category = ResourceCategory.FOOD,
                            qualityBands = mapOf(
                                QualityBand.EXCELLENT to 350.0,
                                QualityBand.RARE to 75.0,
                            ),
                        ),
                        ResourceStock(
                            resourceId = ResourceId("biomass"),
                            category = ResourceCategory.FUEL,
                            qualityBands = mapOf(QualityBand.EXCELLENT to 500.0),
                        ),
                    ),
                ),
            ),
        ),
        activeColonyId = colonyId,
    )
}
