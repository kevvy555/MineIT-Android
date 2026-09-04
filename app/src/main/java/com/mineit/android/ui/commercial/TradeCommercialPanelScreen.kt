package com.mineit.android.ui.commercial

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.mineit.android.domain.colony.SpaceportStatus
import com.mineit.android.domain.model.GameState
import com.mineit.android.domain.model.ResourceId
import com.mineit.android.domain.resources.ResourceCatalogue
import com.mineit.android.domain.resources.ResourceCategory
import com.mineit.android.domain.trade.ColonistTransferProjection
import com.mineit.android.domain.trade.TradeQuote
import com.mineit.android.ui.CommercialPanel
import com.mineit.android.ui.design.MineItActionRow
import com.mineit.android.ui.design.MineItDestructiveButton
import com.mineit.android.ui.design.MineItPalette
import com.mineit.android.ui.design.MineItPanel
import com.mineit.android.ui.design.MineItPrimaryButton
import com.mineit.android.ui.design.MineItSecondaryButton
import com.mineit.android.ui.design.MineItSectionHeader
import com.mineit.android.ui.design.MineItSpacing
import com.mineit.android.ui.design.MineItStat
import com.mineit.android.ui.game.format
import com.mineit.android.ui.game.formatMoney
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.round

private enum class TradeMode { SELL, BUY, COLONISTS }

/**
 * Corporate trade screen intentionally mirrors the pinned web workflow rather than the generic
 * Android dashboard: visit summary -> Sell/Buy/Colonists modes -> explicit ship departure.
 * Compose owns only transient selection/filter/page state; all trade rules come from domain queries.
 */
@Composable
fun TradeCommercialPanelScreen(
    state: GameState,
    spaceport: SpaceportStatus,
    daysUntilArrival: Int,
    cargoCapacity: Double,
    cargoRemaining: Double,
    exportCapacity: Double,
    exportRemaining: Double,
    passengerRemaining: Int,
    colonistProjection: ColonistTransferProjection,
    sellableAmount: (ResourceId) -> Double,
    sellQuote: (ResourceId, Double) -> TradeQuote,
    buyPrice: (ResourceId) -> Double,
    onSelectPanel: (CommercialPanel) -> Unit,
    onClose: () -> Unit,
    onSetReserve: (Double) -> Unit,
    onSellResource: (ResourceId, Double) -> Unit,
    onSellCategory: (ResourceCategory) -> Unit,
    onSellAll: () -> Unit,
    onBuyResource: (ResourceId, Double) -> Unit,
    onTransferColonists: (Int) -> Unit,
    onDepartCorporateShip: () -> Unit,
) {
    val colony = state.activeColony
    val trade = colony.trade
    var modeName by rememberSaveable { mutableStateOf(TradeMode.SELL.name) }
    var amount by rememberSaveable { mutableStateOf(100.0) }
    var buyCategoryName by rememberSaveable { mutableStateOf("ALL") }
    var buyPage by rememberSaveable { mutableStateOf(0) }
    var sellPage by rememberSaveable { mutableStateOf(0) }
    var colonists by rememberSaveable { mutableStateOf(1) }
    val mode = TradeMode.valueOf(modeName)

    Surface(modifier = Modifier.fillMaxSize(), color = MineItPalette.Background) {
        Column(
            modifier = Modifier.fillMaxSize().padding(MineItSpacing.Sm),
            verticalArrangement = Arrangement.spacedBy(MineItSpacing.Sm),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MineItSpacing.Sm),
            ) {
                Text(
                    "COMMERCIAL OPERATIONS",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    color = MineItPalette.Accent,
                    fontWeight = FontWeight.Bold,
                )
                MineItSecondaryButton("CLOSE", onClose)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(MineItSpacing.Xs)) {
                CommercialPanel.entries.forEach { panel ->
                    MineItSecondaryButton(
                        text = panel.name,
                        onClick = { onSelectPanel(panel) },
                        selected = panel == CommercialPanel.TRADE,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(MineItSpacing.Sm),
            ) {
                item {
                    MineItPanel {
                        MineItSectionHeader(
                            "CONGLOMERATE TRADE SHIP",
                            if (trade.active) "DOCKED" else "${daysUntilArrival}d",
                        )
                        if (trade.active) {
                            TradeSummaryGrid(
                                cash = state.company.cash,
                                cargoRemaining = cargoRemaining,
                                cargoCapacity = cargoCapacity,
                                exportRemaining = exportRemaining,
                                exportCapacity = exportCapacity,
                                passengerRemaining = passengerRemaining,
                            )
                        } else {
                            Text(
                                "Next scheduled visit in $daysUntilArrival day(s). Trade controls become available when the corporate ship docks.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MineItPalette.Muted,
                            )
                        }
                        Text(
                            spaceport.reason,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (spaceport.operational) MineItPalette.Success else MineItPalette.Warning,
                        )
                    }
                }

                if (trade.active) {
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(MineItSpacing.Xs)) {
                            TradeMode.entries.forEach { candidate ->
                                MineItSecondaryButton(
                                    text = candidate.name,
                                    onClick = { modeName = candidate.name },
                                    selected = candidate == mode,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }

                    item {
                        when (mode) {
                            TradeMode.SELL -> SellTradeContent(
                                state = state,
                                amount = amount,
                                exportRemaining = exportRemaining,
                                page = sellPage,
                                onPage = { sellPage = it },
                                onAmount = { amount = it.coerceAtLeast(0.0) },
                                sellableAmount = sellableAmount,
                                sellQuote = sellQuote,
                                onSetReserve = onSetReserve,
                                onSellResource = onSellResource,
                                onSellCategory = onSellCategory,
                                onSellAll = onSellAll,
                                enabled = spaceport.tradeAllowed,
                            )

                            TradeMode.BUY -> BuyTradeContent(
                                state = state,
                                amount = amount,
                                cargoRemaining = cargoRemaining,
                                categoryName = buyCategoryName,
                                page = buyPage,
                                onCategory = { buyCategoryName = it; buyPage = 0 },
                                onPage = { buyPage = it },
                                onAmount = { amount = it.coerceAtLeast(0.0) },
                                buyPrice = buyPrice,
                                onBuyResource = onBuyResource,
                                enabled = spaceport.tradeAllowed,
                            )

                            TradeMode.COLONISTS -> ColonistTradeContent(
                                state = state,
                                selected = colonists,
                                projection = colonistProjection,
                                enabled = spaceport.transfersAllowed,
                                onSelected = { colonists = it.coerceIn(0, colonistProjection.maxTransfer) },
                                onTransfer = onTransferColonists,
                            )
                        }
                    }

                    item {
                        MineItDestructiveButton(
                            "SHIP DEPARTS",
                            onDepartCorporateShip,
                            Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TradeSummaryGrid(
    cash: Double,
    cargoRemaining: Double,
    cargoCapacity: Double,
    exportRemaining: Double,
    exportCapacity: Double,
    passengerRemaining: Int,
) {
    Column(verticalArrangement = Arrangement.spacedBy(MineItSpacing.Xs)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(MineItSpacing.Xs)) {
            MineItStat("CASH", "£${formatMoney(cash)}", Modifier.weight(1f))
            MineItStat("IMPORT", "${format(cargoRemaining)}/${format(cargoCapacity)}", Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(MineItSpacing.Xs)) {
            MineItStat("EXPORT", "${format(exportRemaining)}/${format(exportCapacity)}", Modifier.weight(1f))
            MineItStat("PAX", passengerRemaining.toString(), Modifier.weight(1f))
        }
    }
}

@Composable
private fun SellTradeContent(
    state: GameState,
    amount: Double,
    exportRemaining: Double,
    page: Int,
    onPage: (Int) -> Unit,
    onAmount: (Double) -> Unit,
    sellableAmount: (ResourceId) -> Double,
    sellQuote: (ResourceId, Double) -> TradeQuote,
    onSetReserve: (Double) -> Unit,
    onSellResource: (ResourceId, Double) -> Unit,
    onSellCategory: (ResourceCategory) -> Unit,
    onSellAll: () -> Unit,
    enabled: Boolean,
) {
    val colony = state.activeColony
    val stocks = colony.inventory.resources
        .filter { it.amount > .0001 }
        .sortedWith(compareBy({ it.category.ordinal }, { ResourceCatalogue.get(it.resourceId)?.name ?: it.resourceId.value }))
    val pageSize = 4
    val pageCount = max(1, ceil(stocks.size / pageSize.toDouble()).toInt())
    val safePage = page.coerceIn(0, pageCount - 1)
    val visible = stocks.drop(safePage * pageSize).take(pageSize)

    Column(verticalArrangement = Arrangement.spacedBy(MineItSpacing.Sm)) {
        MineItPanel {
            MineItSectionHeader("SELL COLONY STOCK", "EXPORT ${format(exportRemaining)}")
            Text(
                "Highest-value quality lots sell first. The colony reserve is protected independently for every resource.",
                style = MaterialTheme.typography.bodySmall,
                color = MineItPalette.Muted,
            )
            TradeAmountSelector(amount, exportRemaining, onAmount)
        }

        MineItPanel {
            MineItSectionHeader("COLONY TRADE RESERVE", format(colony.tradeReserve))
            MineItActionRow {
                MineItSecondaryButton("0", { onSetReserve(0.0) }, Modifier.weight(1f))
                MineItSecondaryButton("-100", { onSetReserve((colony.tradeReserve - 100.0).coerceAtLeast(0.0)) }, Modifier.weight(1f))
                MineItSecondaryButton("+100", { onSetReserve(colony.tradeReserve + 100.0) }, Modifier.weight(1f))
            }
        }

        MineItPanel {
            MineItSectionHeader("QUICK SELL", "BY CATEGORY")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(MineItSpacing.Xs)) {
                ResourceCategory.entries.forEach { category ->
                    MineItSecondaryButton(
                        category.name,
                        { onSellCategory(category) },
                        Modifier.weight(1f),
                        enabled = enabled && exportRemaining > .0001,
                    )
                }
            }
            MineItPrimaryButton(
                "SELL ALL AVAILABLE STOCK",
                onSellAll,
                Modifier.fillMaxWidth(),
                enabled = enabled && exportRemaining > .0001 && stocks.any { sellableAmount(it.resourceId) > .0001 },
            )
        }

        if (visible.isEmpty()) {
            MineItPanel { Text("No colony stock is currently available to sell.", color = MineItPalette.Muted) }
        } else {
            visible.forEach { stock ->
                val definition = ResourceCatalogue.get(stock.resourceId)
                val sellable = sellableAmount(stock.resourceId)
                val quote = sellQuote(stock.resourceId, amount)
                MineItPanel {
                    MineItSectionHeader(definition?.name ?: stock.resourceId.value, stock.category.name)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(MineItSpacing.Xs)) {
                        MineItStat("STOCK", format(stock.amount), Modifier.weight(1f))
                        MineItStat("RESERVE", format(colony.tradeReserve), Modifier.weight(1f))
                        MineItStat("SELLABLE", format(sellable), Modifier.weight(1f))
                    }
                    Text(
                        "Selected ${format(quote.quantity)} • Revenue £${formatMoney(quote.value)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MineItPalette.Muted,
                    )
                    MineItPrimaryButton(
                        "SELL ${format(amount)}",
                        { onSellResource(stock.resourceId, amount) },
                        Modifier.fillMaxWidth(),
                        enabled = enabled && quote.quantity > .0001,
                    )
                }
            }
        }
        Pager(safePage, pageCount, onPage)
    }
}

@Composable
private fun BuyTradeContent(
    state: GameState,
    amount: Double,
    cargoRemaining: Double,
    categoryName: String,
    page: Int,
    onCategory: (String) -> Unit,
    onPage: (Int) -> Unit,
    onAmount: (Double) -> Unit,
    buyPrice: (ResourceId) -> Double,
    onBuyResource: (ResourceId, Double) -> Unit,
    enabled: Boolean,
) {
    val colony = state.activeColony
    val category = ResourceCategory.entries.firstOrNull { it.name == categoryName }
    val resources = ResourceCatalogue.all.filter { category == null || it.category == category }
    val pageSize = 4
    val pageCount = max(1, ceil(resources.size / pageSize.toDouble()).toInt())
    val safePage = page.coerceIn(0, pageCount - 1)
    val visible = resources.drop(safePage * pageSize).take(pageSize)

    Column(verticalArrangement = Arrangement.spacedBy(MineItSpacing.Sm)) {
        MineItPanel {
            MineItSectionHeader("BUY FROM CORPORATION", "IMPORT ${format(cargoRemaining)}")
            TradeAmountSelector(amount, cargoRemaining, onAmount)
            Text(
                "The corporate ship limits purchases by remaining cargo and available company cash.",
                style = MaterialTheme.typography.bodySmall,
                color = MineItPalette.Muted,
            )
        }

        MineItPanel {
            MineItSectionHeader("RESOURCE CATEGORY", categoryName)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(MineItSpacing.Xs)) {
                listOf("ALL", "FOOD", "BUILD").forEach { label ->
                    MineItSecondaryButton(label, { onCategory(label) }, Modifier.weight(1f), selected = categoryName == label)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(MineItSpacing.Xs)) {
                listOf("FUEL", "ORE").forEach { label ->
                    MineItSecondaryButton(label, { onCategory(label) }, Modifier.weight(1f), selected = categoryName == label)
                }
            }
        }

        visible.forEach { definition ->
            val stock = colony.inventory.amountFor(definition.id)
            val unitPrice = buyPrice(definition.id)
            val reserveShortfall = (colony.tradeReserve - stock).coerceAtLeast(0.0)
            val canBuyAny = enabled && cargoRemaining >= 1.0 && state.company.cash + .0001 >= unitPrice
            MineItPanel {
                MineItSectionHeader(definition.name, definition.category.name)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(MineItSpacing.Xs)) {
                    MineItStat("STOCK", format(stock), Modifier.weight(1f))
                    MineItStat("RESERVE", format(colony.tradeReserve), Modifier.weight(1f))
                    MineItStat("PRICE", "£${formatMoney(unitPrice)}", Modifier.weight(1f))
                }
                Text(
                    "Request ${format(amount)} • up to £${formatMoney(amount * unitPrice)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MineItPalette.Muted,
                )
                MineItActionRow {
                    MineItPrimaryButton(
                        "BUY ${format(amount)}",
                        { onBuyResource(definition.id, amount) },
                        Modifier.weight(1f),
                        enabled = canBuyAny && amount >= 1.0,
                    )
                    MineItSecondaryButton(
                        "TO RESERVE",
                        { onBuyResource(definition.id, reserveShortfall) },
                        Modifier.weight(1f),
                        enabled = canBuyAny && reserveShortfall >= 1.0,
                    )
                }
            }
        }
        Pager(safePage, pageCount, onPage)
    }
}

@Composable
private fun ColonistTradeContent(
    state: GameState,
    selected: Int,
    projection: ColonistTransferProjection,
    enabled: Boolean,
    onSelected: (Int) -> Unit,
    onTransfer: (Int) -> Unit,
) {
    val effective = selected.coerceIn(0, projection.maxTransfer)
    val cost = round(effective * projection.unitCost)
    val afterPopulation = state.activeColony.population + effective
    val foodSafe = effective <= projection.maxSafeTransfer

    Column(verticalArrangement = Arrangement.spacedBy(MineItSpacing.Sm)) {
        MineItPanel {
            MineItSectionHeader("TRANSFER COLONISTS", "PAX ${projection.passengerRemaining}")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(MineItSpacing.Xs)) {
                MineItSecondaryButton("-10", { onSelected(effective - 10) }, Modifier.weight(1f), enabled = effective > 0)
                MineItSecondaryButton("-1", { onSelected(effective - 1) }, Modifier.weight(1f), enabled = effective > 0)
                MineItStat("SELECTED", effective.toString(), Modifier.weight(1f))
                MineItSecondaryButton("+1", { onSelected(effective + 1) }, Modifier.weight(1f), enabled = effective < projection.maxTransfer)
                MineItSecondaryButton("+10", { onSelected(effective + 10) }, Modifier.weight(1f), enabled = effective < projection.maxTransfer)
            }
            MineItSecondaryButton(
                "MAX SAFE ${projection.maxSafeTransfer}",
                { onSelected(projection.maxSafeTransfer) },
                Modifier.fillMaxWidth(),
                enabled = projection.maxSafeTransfer > 0,
            )
        }

        MineItPanel {
            MineItSectionHeader("TRANSFER PROJECTION")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(MineItSpacing.Xs)) {
                MineItStat("COST", "£${formatMoney(cost)}", Modifier.weight(1f))
                MineItStat("POP AFTER", format(afterPopulation), Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(MineItSpacing.Xs)) {
                MineItStat("MAX SAFE", projection.maxSafeTransfer.toString(), Modifier.weight(1f), if (foodSafe) MineItPalette.Success else MineItPalette.Warning)
                MineItStat("HARD MAX", projection.maxTransfer.toString(), Modifier.weight(1f))
            }
            Text(
                "Housing/Power room ${projection.housingPowerRemaining} • Food-supported additional ${projection.foodSupportedAdditional} • Supported population ${projection.supportedPopulationCapacity}",
                style = MaterialTheme.typography.bodySmall,
                color = if (foodSafe) MineItPalette.Muted else MineItPalette.Warning,
            )
            Text(
                "Food is a MAX SAFE guide; Housing/Power, passenger capacity, company cash and Spaceport services are hard limits.",
                style = MaterialTheme.typography.bodySmall,
                color = MineItPalette.Muted,
            )
            MineItPrimaryButton(
                "TRANSFER $effective COLONISTS",
                { onTransfer(effective) },
                Modifier.fillMaxWidth(),
                enabled = enabled && effective > 0 && effective <= projection.maxTransfer && state.company.cash + .0001 >= cost,
            )
        }
    }
}

@Composable
private fun TradeAmountSelector(amount: Double, maxAmount: Double, onAmount: (Double) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(MineItSpacing.Xs)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(MineItSpacing.Xs)) {
            MineItSecondaryButton("-1K", { onAmount((amount - 1_000.0).coerceAtLeast(0.0)) }, Modifier.weight(1f), enabled = amount > 0.0)
            MineItStat("AMOUNT", format(amount), Modifier.weight(1.2f))
            MineItSecondaryButton("+1K", { onAmount(amount + 1_000.0) }, Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(MineItSpacing.Xs)) {
            listOf(100.0 to "100", 1_000.0 to "1K", 10_000.0 to "10K").forEach { (value, label) ->
                MineItSecondaryButton(label, { onAmount(value) }, Modifier.weight(1f), selected = amount == value)
            }
            MineItSecondaryButton("MAX", { onAmount(maxAmount.coerceAtLeast(0.0)) }, Modifier.weight(1f))
        }
    }
}

@Composable
private fun Pager(page: Int, pageCount: Int, onPage: (Int) -> Unit) {
    if (pageCount <= 1) return
    MineItActionRow {
        MineItSecondaryButton("PREV", { onPage(page - 1) }, Modifier.weight(1f), enabled = page > 0)
        MineItStat("PAGE", "${page + 1}/$pageCount", Modifier.weight(1f))
        MineItSecondaryButton("NEXT", { onPage(page + 1) }, Modifier.weight(1f), enabled = page + 1 < pageCount)
    }
}
