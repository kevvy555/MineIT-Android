package com.mineit.android.ui.commercial

import com.mineit.android.domain.model.GameState
import com.mineit.android.domain.model.ResourceId
import com.mineit.android.domain.resources.ResourceCatalogue
import com.mineit.android.domain.resources.ResourceCategory
import com.mineit.android.domain.resources.ResourceDefinition
import com.mineit.android.domain.trade.ColonistTransferProjection

/** Pure presentation policy for the source-compatible compact Corporate Ship workflow. */
object CorporateTradePresentation {
    const val PAGE_SIZE = 4
    const val MAX_TRADE_AMOUNT = 100_000.0

    val buyCategories: List<ResourceCategory> = listOf(
        ResourceCategory.FUEL,
        ResourceCategory.FOOD,
        ResourceCategory.ORE,
        ResourceCategory.BUILD,
    )

    fun colonistsAvailable(state: GameState): Boolean =
        state.activeColony.contract?.ended != true && state.activeColony.status.name != "LIABILITY" && state.activeColony.status.name != "DEAD"

    fun defaultColonistAmount(projection: ColonistTransferProjection): Int =
        projection.maxSafeTransfer.coerceIn(0, projection.maxTransfer)

    fun sellStocks(
        state: GameState,
        sellableAmount: (ResourceId) -> Double,
    ) = state.activeColony.inventory.resources
        .filter { it.amount > .0001 && sellableAmount(it.resourceId) > .0001 }
        .sortedWith(compareBy({ it.category.ordinal }, { ResourceCatalogue.get(it.resourceId)?.name ?: it.resourceId.value }))

    fun buyRows(state: GameState, category: ResourceCategory): List<CorporateTradeBuyRow> =
        ResourceCatalogue.byCategory(category)
            .map { definition ->
                val stock = state.activeColony.inventory.amountFor(definition.id)
                val reserve = state.activeColony.tradeReserve
                CorporateTradeBuyRow(
                    definition = definition,
                    stock = stock,
                    reserve = reserve,
                    reserveShortfall = (reserve - stock).coerceAtLeast(0.0),
                )
            }
            .sortedWith(
                compareByDescending<CorporateTradeBuyRow> { it.reserveShortfall > .0001 }
                    .thenByDescending { it.reserveShortfall }
                    .thenBy { it.definition.name },
            )
}

data class CorporateTradeBuyRow(
    val definition: ResourceDefinition,
    val stock: Double,
    val reserve: Double,
    val reserveShortfall: Double,
)
