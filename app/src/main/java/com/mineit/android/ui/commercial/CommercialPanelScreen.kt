package com.mineit.android.ui.commercial

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.mineit.android.domain.buyers.BuyerCollectionProjection
import com.mineit.android.domain.buyers.BuyerContract
import com.mineit.android.domain.buyers.BuyerOffer
import com.mineit.android.domain.colony.PopulationSupportCapacity
import com.mineit.android.domain.colony.SpaceportStatus
import com.mineit.android.domain.contracts.ContractScore
import com.mineit.android.domain.model.GameState
import com.mineit.android.domain.model.ResourceId
import com.mineit.android.domain.resources.ResourceCatalogue
import com.mineit.android.domain.simulation.ColonyMetrics
import com.mineit.android.ui.CommercialPanel
import com.mineit.android.ui.design.MineItActionRow
import com.mineit.android.ui.design.MineItDestructiveButton
import com.mineit.android.ui.design.MineItPalette
import com.mineit.android.ui.design.MineItPanel
import com.mineit.android.ui.design.MineItPrimaryButton
import com.mineit.android.ui.design.MineItSecondaryButton
import com.mineit.android.ui.design.MineItSectionHeader
import com.mineit.android.ui.design.MineItSpacing
import com.mineit.android.ui.game.format
import com.mineit.android.ui.game.formatMoney

@Composable
fun CommercialPanelScreen(
    panel: CommercialPanel,
    state: GameState,
    metrics: ColonyMetrics,
    spaceport: SpaceportStatus,
    contractScore: ContractScore?,
    populationSupport: PopulationSupportCapacity,
    tradeDaysUntilArrival: Int,
    buyerOffers: List<BuyerOffer>,
    buyerContracts: List<BuyerContract>,
    buyerProjections: Map<String, BuyerCollectionProjection>,
    onSelectPanel: (CommercialPanel) -> Unit,
    onClose: () -> Unit,
    onSetReserve: (Double) -> Unit,
    onSellResource: (ResourceId, Double) -> Unit,
    onBuyResource: (ResourceId, Double) -> Unit,
    onTransferColonists: (Int) -> Unit,
    onDepartCorporateShip: () -> Unit,
    onAcceptBuyerOffer: (String) -> Unit,
    onTransferBuyer: (String) -> Unit,
    onWaitBuyer: (String) -> Unit,
    onMissBuyer: (String) -> Unit,
    onCancelBuyer: (String) -> Unit,
    onRenewContract: () -> Unit,
    onEndLiability: () -> Unit,
) {
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
            CommercialTabs(panel, onSelectPanel)
            when (panel) {
                CommercialPanel.TRADE -> TradePanel(
                    state = state,
                    metrics = metrics,
                    spaceport = spaceport,
                    populationSupport = populationSupport,
                    daysUntilArrival = tradeDaysUntilArrival,
                    onSetReserve = onSetReserve,
                    onSellResource = onSellResource,
                    onBuyResource = onBuyResource,
                    onTransferColonists = onTransferColonists,
                    onDepartCorporateShip = onDepartCorporateShip,
                )
                CommercialPanel.CONTRACT -> ContractPanel(state, contractScore, onRenewContract, onEndLiability)
                CommercialPanel.BUYERS -> BuyersPanel(
                    buyerOffers,
                    buyerContracts,
                    buyerProjections,
                    onAcceptBuyerOffer,
                    onTransferBuyer,
                    onWaitBuyer,
                    onMissBuyer,
                    onCancelBuyer,
                )
                CommercialPanel.LOG -> GameLogPanel(state)
            }
        }
    }
}

@Composable
private fun CommercialTabs(selected: CommercialPanel, onSelect: (CommercialPanel) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(MineItSpacing.Xs)) {
        CommercialPanel.entries.forEach { panel ->
            MineItSecondaryButton(
                text = panel.name,
                onClick = { onSelect(panel) },
                selected = panel == selected,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun TradePanel(
    state: GameState,
    metrics: ColonyMetrics,
    spaceport: SpaceportStatus,
    populationSupport: PopulationSupportCapacity,
    daysUntilArrival: Int,
    onSetReserve: (Double) -> Unit,
    onSellResource: (ResourceId, Double) -> Unit,
    onBuyResource: (ResourceId, Double) -> Unit,
    onTransferColonists: (Int) -> Unit,
    onDepartCorporateShip: () -> Unit,
) {
    val colony = state.activeColony
    val trade = colony.trade
    LazyColumn(verticalArrangement = Arrangement.spacedBy(MineItSpacing.Sm)) {
        item {
            MineItPanel {
                MineItSectionHeader("CONGLOMERATE TRADE SHIP", if (trade.active) "DOCKED" else "${daysUntilArrival}d")
                Text(
                    if (trade.active) "Import ${format((trade.visitCargoCapacity ?: 0.0) - trade.cargoUsed)} remaining • Export ${format((trade.visitExportCapacity ?: 0.0) - trade.exportUsed)} remaining • Passengers ${250 - trade.passengersUsed}" else "Next scheduled visit in $daysUntilArrival day(s).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MineItPalette.Muted,
                )
                Text(spaceport.reason, style = MaterialTheme.typography.bodySmall, color = if (spaceport.operational) MineItPalette.Success else MineItPalette.Warning)
            }
        }
        item {
            MineItPanel {
                MineItSectionHeader("COLONY TRADE RESERVE", format(colony.tradeReserve))
                Text("The reserve protects this quantity independently on every stocked resource.", style = MaterialTheme.typography.bodySmall, color = MineItPalette.Muted)
                MineItActionRow {
                    MineItSecondaryButton("0", { onSetReserve(0.0) }, Modifier.weight(1f))
                    MineItSecondaryButton("-100", { onSetReserve((colony.tradeReserve - 100).coerceAtLeast(0.0)) }, Modifier.weight(1f))
                    MineItSecondaryButton("+100", { onSetReserve(colony.tradeReserve + 100) }, Modifier.weight(1f))
                }
            }
        }
        item {
            MineItPanel {
                MineItSectionHeader("COLONISTS", "POP ${format(colony.population)}")
                Text(
                    "Housing ${populationSupport.housingCapacity} • Power-supported ${populationSupport.powerCapacity} • Food ${if (metrics.foodProduction >= metrics.foodDemand) "surplus" else "deficit"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MineItPalette.Muted,
                )
                Text("Food is a MAX SAFE guide; Housing/Power, passenger capacity, cash and Spaceport services are hard limits.", style = MaterialTheme.typography.bodySmall, color = MineItPalette.Muted)
                MineItActionRow {
                    MineItSecondaryButton("+10", { onTransferColonists(10) }, Modifier.weight(1f), enabled = trade.active && spaceport.transfersAllowed)
                    MineItSecondaryButton("+25", { onTransferColonists(25) }, Modifier.weight(1f), enabled = trade.active && spaceport.transfersAllowed)
                }
            }
        }
        item { MineItSectionHeader("STOCK & DIRECT TRADE") }
        items(colony.inventory.resources.sortedBy { ResourceCatalogue.get(it.resourceId)?.name ?: it.resourceId.value }, key = { it.resourceId.value }) { stock ->
            val definition = ResourceCatalogue.get(stock.resourceId)
            MineItPanel {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(definition?.name ?: stock.resourceId.value, fontWeight = FontWeight.Bold)
                        Text("${format(stock.amount)} stock", style = MaterialTheme.typography.bodySmall, color = MineItPalette.Muted)
                    }
                    Text("£${formatMoney((definition?.sellPrice ?: 0.0) * 5.0)} base", style = MaterialTheme.typography.labelSmall, color = MineItPalette.Muted)
                }
                MineItActionRow {
                    MineItPrimaryButton("SELL 100", { onSellResource(stock.resourceId, 100.0) }, Modifier.weight(1f), enabled = trade.active && spaceport.tradeAllowed)
                    MineItSecondaryButton("BUY 100", { onBuyResource(stock.resourceId, 100.0) }, Modifier.weight(1f), enabled = trade.active && spaceport.tradeAllowed)
                }
            }
        }
        if (trade.active) {
            item { MineItDestructiveButton("DEPART CORPORATE SHIP", onDepartCorporateShip, Modifier.fillMaxWidth()) }
        }
    }
}

@Composable
private fun ContractPanel(
    state: GameState,
    score: ContractScore?,
    onRenewContract: () -> Unit,
    onEndLiability: () -> Unit,
) {
    val colony = state.activeColony
    val contract = colony.contract
    LazyColumn(verticalArrangement = Arrangement.spacedBy(MineItSpacing.Sm)) {
        item {
            MineItPanel {
                MineItSectionHeader(contract?.name ?: "NO CONTRACT", colony.status.name)
                if (contract != null) {
                    Text("Term ${contract.years + contract.extensionYears} years • extensions ${contract.extensionsUsed}/3 • renewals ${contract.renewals}", color = MineItPalette.Muted, style = MaterialTheme.typography.bodySmall)
                    Text("Local revenue £${formatMoney(contract.localRevenue)} • costs £${formatMoney(contract.localCosts)} • profit £${formatMoney(contract.localRevenue - contract.localCosts)}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        score?.let { current ->
            item {
                MineItPanel {
                    MineItSectionHeader("CONTRACT SCORE", current.medal.name)
                    Text("Food ${percentRatio(current.foodRatio)} • Industry ${percentRatio(current.industryRatio)} • Population ${percentRatio(current.populationRatio)}", style = MaterialTheme.typography.bodySmall)
                    Text(if (current.passed) "Goals currently satisfied." else "One or more contract goals are below target.", color = if (current.passed) MineItPalette.Success else MineItPalette.Warning)
                }
            }
        }
        contract?.let {
            item {
                MineItPanel {
                    MineItSectionHeader("GOALS")
                    Text("Food ${it.goals.food}/d • Industry ${it.goals.industry} • Population ${it.goals.population}", style = MaterialTheme.typography.bodySmall)
                    Text("Silver £${it.bands.silver} • Gold £${it.bands.gold} • Platinum £${it.bands.platinum}", style = MaterialTheme.typography.bodySmall, color = MineItPalette.Muted)
                }
            }
            if (it.completed && !it.ended) {
                item {
                    MineItPanel {
                        MineItSectionHeader("POST-CONTRACT DECISION")
                        MineItPrimaryButton("RENEW +5 YEARS", onRenewContract, Modifier.fillMaxWidth())
                        MineItDestructiveButton("END — RETAIN AS LIABILITY", onEndLiability, Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
}

@Composable
private fun BuyersPanel(
    offers: List<BuyerOffer>,
    contracts: List<BuyerContract>,
    projections: Map<String, BuyerCollectionProjection>,
    onAccept: (String) -> Unit,
    onTransfer: (String) -> Unit,
    onWait: (String) -> Unit,
    onMiss: (String) -> Unit,
    onCancel: (String) -> Unit,
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(MineItSpacing.Sm)) {
        if (contracts.isNotEmpty()) item { MineItSectionHeader("ACTIVE BUYER CONTRACTS") }
        items(contracts, key = { it.id }) { contract ->
            val projection = projections[contract.id]
            MineItPanel {
                val resource = ResourceCatalogue.get(contract.resourceId)?.name ?: contract.resourceId.value
                MineItSectionHeader(resource, contract.ship.status.name)
                Text("${format(contract.quantity)} every ${contract.intervalDays}d • £${formatMoney(contract.unitRate)}/unit", style = MaterialTheme.typography.bodySmall)
                projection?.let {
                    Text("Ready ${format(it.transferableQuantity)}/${format(contract.quantity)} • ${(it.completionRatio * 100).toInt()}% • ${it.daysLate}d late", style = MaterialTheme.typography.bodySmall, color = if (it.canTransfer) MineItPalette.Success else MineItPalette.Warning)
                }
                MineItActionRow {
                    MineItPrimaryButton("TRANSFER", { onTransfer(contract.id) }, Modifier.weight(1f), enabled = projection?.canTransfer == true)
                    MineItSecondaryButton("WAIT", { onWait(contract.id) }, Modifier.weight(1f), enabled = contract.ship.status.name != "IDLE")
                }
                MineItActionRow {
                    MineItSecondaryButton("MISS", { onMiss(contract.id) }, Modifier.weight(1f), enabled = contract.ship.attemptIndex >= 3)
                    MineItDestructiveButton("CANCEL", { onCancel(contract.id) }, Modifier.weight(1f), enabled = contract.ship.status.name == "IDLE")
                }
            }
        }
        item { MineItSectionHeader("AVAILABLE BUYERS", "${offers.size} shown") }
        items(offers, key = { it.id }) { offer ->
            MineItPanel {
                MineItSectionHeader(offer.companyName, "REP ${"%.2f".format(offer.minimumReputation)}")
                Text("${offer.buyerName} • ${ResourceCatalogue.get(offer.resourceId)?.name ?: offer.resourceId.value}", fontWeight = FontWeight.Bold)
                Text("${format(offer.quantity)} @ £${formatMoney(offer.unitRate)} • minimum ${offer.minimumQuality.name.lowercase()} • every ${offer.intervalDays}d", style = MaterialTheme.typography.bodySmall, color = MineItPalette.Muted)
                MineItPrimaryButton("ACCEPT CONTRACT", { onAccept(offer.id) }, Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun GameLogPanel(state: GameState) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(MineItSpacing.Xs)) {
        item { MineItSectionHeader("GAME LOG", "${state.gameLog.events.size} events") }
        items(state.gameLog.events.asReversed().take(100), key = { it.id }) { event ->
            MineItPanel {
                Row(Modifier.fillMaxWidth()) {
                    Text("Y${event.year} D${event.day}", style = MaterialTheme.typography.labelSmall, color = MineItPalette.Accent)
                    Text(event.type.uppercase(), modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = MineItPalette.Muted)
                    Text("#${event.id}", style = MaterialTheme.typography.labelSmall, color = MineItPalette.Muted)
                }
                Text(event.message, style = MaterialTheme.typography.bodySmall)
            }
        }
        if (state.gameLog.events.isEmpty()) item { Text("No logged commercial events yet.", color = MineItPalette.Muted) }
    }
}

private fun percentRatio(value: Double): String = "${(value * 100).toInt()}%"
