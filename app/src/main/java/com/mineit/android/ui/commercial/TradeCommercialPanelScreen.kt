package com.mineit.android.ui.commercial

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
 * Full-screen Corporate Ship surface following the maintained web quick-trade hierarchy:
 * fixed visit summary -> fixed Sell/Buy/Colonists tabs -> four-row work area -> fixed departure.
 * Compose owns transient selection/paging only; authoritative trade rules remain in domain services.
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
    val colonistsAvailable = CorporateTradePresentation.colonistsAvailable(state)
    var modeName by rememberSaveable { mutableStateOf(TradeMode.SELL.name) }
    var sellAmount by rememberSaveable { mutableStateOf(10_000.0) }
    var buyAmount by rememberSaveable { mutableStateOf(10_000.0) }
    var buyCategoryName by rememberSaveable { mutableStateOf(ResourceCategory.FUEL.name) }
    var buyPage by rememberSaveable { mutableIntStateOf(0) }
    var sellPage by rememberSaveable { mutableIntStateOf(0) }
    var colonists by rememberSaveable { mutableIntStateOf(0) }
    var colonistVisit by rememberSaveable { mutableIntStateOf(-1) }
    var mode = TradeMode.valueOf(modeName)

    if (!colonistsAvailable && mode == TradeMode.COLONISTS) {
        modeName = TradeMode.SELL.name
        mode = TradeMode.SELL
    }

    LaunchedEffect(trade.visits, colonistProjection.maxSafeTransfer, colonistProjection.maxTransfer) {
        if (trade.visits != colonistVisit) {
            colonistVisit = trade.visits
            colonists = CorporateTradePresentation.defaultColonistAmount(colonistProjection)
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MineItPalette.Background) {
        Column(
            modifier = Modifier.fillMaxSize().padding(MineItSpacing.Sm),
            verticalArrangement = Arrangement.spacedBy(MineItSpacing.Xs),
        ) {
            CorporateShipHeader(
                colonyName = colony.name,
                active = trade.active,
                daysUntilArrival = daysUntilArrival,
                onClose = onClose,
            )

            if (!trade.active) {
                MineItPanel(modifier = Modifier.fillMaxWidth()) {
                    MineItSectionHeader("CONGLOMERATE TRADE SHIP", "${daysUntilArrival}d")
                    Text(
                        "Next scheduled visit in $daysUntilArrival day(s). Trade controls become available when the corporate ship docks.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MineItPalette.Muted,
                    )
                    Text(
                        spaceport.reason,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (spaceport.operational) MineItPalette.Success else MineItPalette.Warning,
                    )
                }
                return@Column
            }

            TradeSummaryGrid(
                cash = state.company.cash,
                cargoRemaining = cargoRemaining,
                cargoCapacity = cargoCapacity,
                exportRemaining = exportRemaining,
                exportCapacity = exportCapacity,
                passengerRemaining = if (colonistsAvailable) passengerRemaining else null,
            )

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(MineItSpacing.Xs)) {
                TradeMode.entries.forEach { candidate ->
                    val enabled = candidate != TradeMode.COLONISTS || colonistsAvailable
                    MineItSecondaryButton(
                        text = candidate.name,
                        onClick = { if (enabled) modeName = candidate.name },
                        selected = candidate == mode,
                        modifier = Modifier.weight(1f),
                        enabled = enabled,
                    )
                }
            }

            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                when (mode) {
                    TradeMode.SELL -> SellTradeContent(
                        state = state,
                        amount = sellAmount,
                        exportRemaining = exportRemaining,
                        page = sellPage,
                        onPage = { sellPage = it },
                        onAmount = { sellAmount = it.coerceIn(1.0, CorporateTradePresentation.MAX_TRADE_AMOUNT) },
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
                        amount = buyAmount,
                        cargoRemaining = cargoRemaining,
                        categoryName = buyCategoryName,
                        page = buyPage,
                        onCategory = { buyCategoryName = it; buyPage = 0 },
                        onPage = { buyPage = it },
                        onAmount = { buyAmount = it.coerceIn(1.0, CorporateTradePresentation.MAX_TRADE_AMOUNT) },
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

            MineItDestructiveButton(
                "SHIP DEPARTS",
                onClick = {
                    onDepartCorporateShip()
                    onClose()
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun CorporateShipHeader(
    colonyName: String,
    active: Boolean,
    daysUntilArrival: Int,
    onClose: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MineItSpacing.Sm),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "CORPORATE TRADE SHIP",
                style = MaterialTheme.typography.titleMedium,
                color = MineItPalette.Accent,
                fontWeight = FontWeight.Bold,
            )
            Text(
                if (active) "KOPLIN DEEP REACH • LOGISTICS VESSEL • DOCKED AT $colonyName"
                else "KOPLIN DEEP REACH • NEXT SERVICE IN ${daysUntilArrival}d",
                style = MaterialTheme.typography.bodySmall,
                color = MineItPalette.Muted,
            )
        }
        MineItSecondaryButton("CLOSE", onClose)
    }
}

@Composable
private fun TradeSummaryGrid(
    cash: Double,
    cargoRemaining: Double,
    cargoCapacity: Double,
    exportRemaining: Double,
    exportCapacity: Double,
    passengerRemaining: Int?,
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        if (maxWidth.value < 370f) {
            Column(verticalArrangement = Arrangement.spacedBy(MineItSpacing.Xs)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(MineItSpacing.Xs)) {
                    MineItStat("CASH", "£${formatMoney(cash)}", Modifier.weight(1f))
                    MineItStat("IMPORT", "${format(cargoRemaining)}/${format(cargoCapacity)}", Modifier.weight(1f))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(MineItSpacing.Xs)) {
                    MineItStat("EXPORT", "${format(exportRemaining)}/${format(exportCapacity)}", Modifier.weight(1f))
                    MineItStat("PAX", passengerRemaining?.toString() ?: "—", Modifier.weight(1f))
                }
            }
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(MineItSpacing.Xs)) {
                MineItStat("CASH", "£${formatMoney(cash)}", Modifier.weight(1f))
                MineItStat("IMPORT", "${format(cargoRemaining)}/${format(cargoCapacity)}", Modifier.weight(1f))
                MineItStat("EXPORT", "${format(exportRemaining)}/${format(exportCapacity)}", Modifier.weight(1f))
                MineItStat("PAX", passengerRemaining?.toString() ?: "—", Modifier.weight(1f))
            }
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
    val stocks = CorporateTradePresentation.sellStocks(state, sellableAmount)
    val pageCount = max(1, ceil(stocks.size / CorporateTradePresentation.PAGE_SIZE.toDouble()).toInt())
    val safePage = page.coerceIn(0, pageCount - 1)
    val visible = stocks.drop(safePage * CorporateTradePresentation.PAGE_SIZE).take(CorporateTradePresentation.PAGE_SIZE)

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(MineItSpacing.Xs)) {
        MineItSectionHeader("SELL COLONY STOCK", "EXPORT ${format(exportRemaining)}")
        Text(
            "Highest-value quality lots sell first. One colony-wide reserve amount is protected for every individual resource.",
            style = MaterialTheme.typography.bodySmall,
            color = MineItPalette.Muted,
        )
        TradeAmountSelector(amount, onAmount)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(MineItSpacing.Xs)) {
            ResourceCategory.entries.forEach { category ->
                val available = stocks.any { it.category == category }
                MineItSecondaryButton(
                    "ALL ${category.name}",
                    { onSellCategory(category) },
                    Modifier.weight(1f),
                    enabled = enabled && exportRemaining > .0001 && available,
                )
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(MineItSpacing.Xs)) {
            Text("RESERVE ${format(colony.tradeReserve)} / RESOURCE", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = MineItPalette.Muted)
            MineItSecondaryButton("0", { onSetReserve(0.0) })
            MineItSecondaryButton("-100", { onSetReserve((colony.tradeReserve - 100.0).coerceAtLeast(0.0)) })
            MineItSecondaryButton("+100", { onSetReserve(colony.tradeReserve + 100.0) })
        }

        if (visible.isEmpty()) {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text("No stock is available above the colony reserve.", color = MineItPalette.Muted)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(MineItSpacing.Xs),
            ) {
                items(visible.size) { index ->
                    val stock = visible[index]
                    val definition = ResourceCatalogue.get(stock.resourceId)
                    val sellable = sellableAmount(stock.resourceId)
                    val quote = sellQuote(stock.resourceId, amount)
                    CompactTradeRow(
                        title = definition?.name ?: stock.resourceId.value,
                        detail = "${format(stock.amount)} stock • ${format(colony.tradeReserve)} reserve • ${format(sellable)} sellable",
                        action = if (quote.quantity > .0001) "SELL ${format(quote.quantity)}\n£${formatMoney(quote.value)}" else "SELL",
                        enabled = enabled && quote.quantity > .0001,
                        onAction = { onSellResource(stock.resourceId, amount) },
                    )
                }
            }
        }

        MineItActionRow {
            MineItPrimaryButton(
                "SELL ALL AVAILABLE STOCK",
                onSellAll,
                Modifier.weight(1f),
                enabled = enabled && exportRemaining > .0001 && stocks.isNotEmpty(),
            )
            Pager(safePage, pageCount, onPage)
        }
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
    val category = CorporateTradePresentation.buyCategories.firstOrNull { it.name == categoryName } ?: ResourceCategory.FUEL
    val rows = CorporateTradePresentation.buyRows(state, category)
    val pageCount = max(1, ceil(rows.size / CorporateTradePresentation.PAGE_SIZE.toDouble()).toInt())
    val safePage = page.coerceIn(0, pageCount - 1)
    val visible = rows.drop(safePage * CorporateTradePresentation.PAGE_SIZE).take(CorporateTradePresentation.PAGE_SIZE)

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(MineItSpacing.Xs)) {
        MineItSectionHeader("BUY FROM CORPORATION", "${format(cargoRemaining)} CARGO")
        Text("Choose an amount once, then tap resources to buy.", style = MaterialTheme.typography.bodySmall, color = MineItPalette.Muted)
        TradeAmountSelector(amount, onAmount)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(MineItSpacing.Xs)) {
            CorporateTradePresentation.buyCategories.forEach { candidate ->
                MineItSecondaryButton(
                    candidate.name,
                    { onCategory(candidate.name) },
                    Modifier.weight(1f),
                    selected = candidate == category,
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(MineItSpacing.Xs),
        ) {
            items(visible.size) { index ->
                val row = visible[index]
                val unitPrice = buyPrice(row.definition.id)
                val affordable = (state.company.cash / unitPrice).coerceAtLeast(0.0)
                val quantity = minOf(amount, cargoRemaining, affordable, CorporateTradePresentation.MAX_TRADE_AMOUNT).toInt().coerceAtLeast(0)
                CompactTradeRow(
                    title = row.definition.name,
                    detail = buildString {
                        append("${format(row.stock)} stock")
                        if (row.reserve > 0) append(" • ${format(row.reserve)} reserve")
                        if (row.reserveShortfall > .0001) append(" • SHORT ${format(row.reserveShortfall)}")
                        append(" • £${formatMoney(unitPrice)}/u")
                    },
                    action = if (quantity > 0) "BUY ${format(quantity.toDouble())}\n£${formatMoney(quantity * unitPrice)}" else "BUY",
                    enabled = enabled && quantity > 0,
                    onAction = { onBuyResource(row.definition.id, amount) },
                    secondaryAction = if (row.reserveShortfall >= 1.0) "TO RESERVE" else null,
                    onSecondary = { onBuyResource(row.definition.id, row.reserveShortfall) },
                )
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

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(MineItSpacing.Xs)) {
        MineItSectionHeader("TRANSFER COLONISTS", "PAX ${projection.passengerRemaining}")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(MineItSpacing.Xs), verticalAlignment = Alignment.CenterVertically) {
            MineItSecondaryButton("-10", { onSelected(effective - 10) }, Modifier.weight(1f), enabled = effective > 0)
            MineItSecondaryButton("-1", { onSelected(effective - 1) }, Modifier.weight(1f), enabled = effective > 0)
            MineItStat("SELECTED", effective.toString(), Modifier.weight(1.3f))
            MineItSecondaryButton("+1", { onSelected(effective + 1) }, Modifier.weight(1f), enabled = effective < projection.maxTransfer)
            MineItSecondaryButton("+10", { onSelected(effective + 10) }, Modifier.weight(1f), enabled = effective < projection.maxTransfer)
        }
        MineItSecondaryButton(
            "MAX SAFE ${projection.maxSafeTransfer}",
            { onSelected(projection.maxSafeTransfer) },
            Modifier.fillMaxWidth(),
            enabled = projection.maxSafeTransfer > 0,
        )
        MineItPanel(modifier = Modifier.fillMaxWidth().weight(1f)) {
            MineItSectionHeader("TRANSFER PROJECTION")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(MineItSpacing.Xs)) {
                MineItStat("COST", "£${formatMoney(cost)}", Modifier.weight(1f))
                MineItStat("POP AFTER", format(afterPopulation), Modifier.weight(1f))
                MineItStat("MAX SAFE", projection.maxSafeTransfer.toString(), Modifier.weight(1f), if (foodSafe) MineItPalette.Success else MineItPalette.Warning)
                MineItStat("HARD MAX", projection.maxTransfer.toString(), Modifier.weight(1f))
            }
            Text(
                "Housing/Power room ${projection.housingPowerRemaining} • Food-supported additional ${projection.foodSupportedAdditional} • Supported population ${projection.supportedPopulationCapacity}",
                style = MaterialTheme.typography.bodySmall,
                color = if (foodSafe) MineItPalette.Muted else MineItPalette.Warning,
            )
            Text(
                "Food is a MAX SAFE guide. Housing/Power, passenger capacity, company cash and powered Spaceport services remain hard limits.",
                style = MaterialTheme.typography.bodySmall,
                color = MineItPalette.Muted,
            )
        }
        MineItPrimaryButton(
            "TRANSFER $effective COLONISTS",
            { onTransfer(effective) },
            Modifier.fillMaxWidth(),
            enabled = enabled && effective > 0 && effective <= projection.maxTransfer && state.company.cash + .0001 >= cost,
        )
    }
}

@Composable
private fun CompactTradeRow(
    title: String,
    detail: String,
    action: String,
    enabled: Boolean,
    onAction: () -> Unit,
    secondaryAction: String? = null,
    onSecondary: () -> Unit = {},
) {
    MineItPanel(modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(MineItSpacing.Xs)) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Text(detail, style = MaterialTheme.typography.bodySmall, color = MineItPalette.Muted)
            }
            if (secondaryAction != null) MineItSecondaryButton(secondaryAction, onSecondary, enabled = enabled)
            MineItPrimaryButton(action, onAction, enabled = enabled)
        }
    }
}

@Composable
private fun TradeAmountSelector(amount: Double, onAmount: (Double) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(MineItSpacing.Xs)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(MineItSpacing.Xs)) {
            MineItSecondaryButton("−", { onAmount((amount - 1_000.0).coerceAtLeast(1.0)) }, Modifier.weight(1f), enabled = amount > 1.0)
            MineItStat("AMOUNT", format(amount), Modifier.weight(2.2f))
            MineItSecondaryButton("+", { onAmount((amount + 1_000.0).coerceAtMost(CorporateTradePresentation.MAX_TRADE_AMOUNT)) }, Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(MineItSpacing.Xs)) {
            listOf(100.0 to "100", 1_000.0 to "1K", 10_000.0 to "10K", CorporateTradePresentation.MAX_TRADE_AMOUNT to "MAX").forEach { (value, label) ->
                MineItSecondaryButton(label, { onAmount(value) }, Modifier.weight(1f), selected = amount == value)
            }
        }
    }
}

@Composable
private fun Pager(page: Int, pageCount: Int, onPage: (Int) -> Unit) {
    if (pageCount <= 1) {
        Text("1 / 1", style = MaterialTheme.typography.bodySmall, color = MineItPalette.Muted)
        return
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(MineItSpacing.Xs)) {
        MineItSecondaryButton("‹", { onPage(page - 1) }, enabled = page > 0)
        Text("${page + 1} / $pageCount", style = MaterialTheme.typography.bodySmall, color = MineItPalette.Muted)
        MineItSecondaryButton("›", { onPage(page + 1) }, enabled = page + 1 < pageCount)
    }
}
