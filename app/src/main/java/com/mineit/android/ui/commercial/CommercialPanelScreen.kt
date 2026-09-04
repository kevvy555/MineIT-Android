package com.mineit.android.ui.commercial

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.mineit.android.domain.buyers.BuyerCollectionProjection
import com.mineit.android.domain.buyers.BuyerContract
import com.mineit.android.domain.buyers.BuyerContractStatus
import com.mineit.android.domain.buyers.BuyerOffer
import com.mineit.android.domain.buyers.BuyerRelationship
import com.mineit.android.domain.colony.ColonyNetworkSnapshot
import com.mineit.android.domain.model.GameState
import com.mineit.android.domain.reputation.ReputationService
import com.mineit.android.domain.resources.ResourceCatalogue
import com.mineit.android.ui.CommercialPanel
import com.mineit.android.ui.design.MineItActionRow
import com.mineit.android.ui.design.MineItDestructiveButton
import com.mineit.android.ui.design.MineItPalette
import com.mineit.android.ui.design.MineItPanel
import com.mineit.android.ui.design.MineItPrimaryButton
import com.mineit.android.ui.design.MineItRadius
import com.mineit.android.ui.design.MineItSecondaryButton
import com.mineit.android.ui.design.MineItSectionHeader
import com.mineit.android.ui.design.MineItSpacing
import com.mineit.android.ui.design.MineItStat
import com.mineit.android.ui.design.MineItStatusBadge
import com.mineit.android.ui.game.format
import com.mineit.android.ui.game.formatMoney

enum class BuyerDirectoryFilter { ALL, ELIGIBLE, LOCKED }

/**
 * The generic commercial container now owns only Buyers and Game Log. Trade and Contract have
 * dedicated web-referenced screens, avoiding parallel production presentations for those features.
 */
@Composable
fun CommercialPanelScreen(
    panel: CommercialPanel,
    state: GameState,
    network: ColonyNetworkSnapshot,
    buyerOffers: List<BuyerOffer>,
    buyerContracts: List<BuyerContract>,
    buyerProjections: Map<String, BuyerCollectionProjection>,
    onSelectPanel: (CommercialPanel) -> Unit,
    onClose: () -> Unit,
    onAcceptBuyerOffer: (String) -> Unit,
    onTransferBuyer: (String) -> Unit,
    onWaitBuyer: (String) -> Unit,
    onMissBuyer: (String) -> Unit,
    onCancelBuyer: (String) -> Unit,
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
                    if (panel == CommercialPanel.BUYERS) "CONGLOMERATE BUYERS SERVICE" else "GAME LOG",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    color = MineItPalette.Accent,
                    fontWeight = FontWeight.Bold,
                )
                MineItSecondaryButton("CLOSE", onClose)
            }
            CommercialTabs(panel, onSelectPanel)
            when (panel) {
                CommercialPanel.BUYERS -> BuyersPanel(
                    state = state,
                    network = network,
                    offers = buyerOffers,
                    contracts = buyerContracts,
                    projections = buyerProjections,
                    onAccept = onAcceptBuyerOffer,
                    onTransfer = onTransferBuyer,
                    onWait = onWaitBuyer,
                    onMiss = onMissBuyer,
                    onCancel = onCancelBuyer,
                )
                CommercialPanel.LOG -> GameLogPanel(state)
                else -> Unit
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
private fun BuyersPanel(
    state: GameState,
    network: ColonyNetworkSnapshot,
    offers: List<BuyerOffer>,
    contracts: List<BuyerContract>,
    projections: Map<String, BuyerCollectionProjection>,
    onAccept: (String) -> Unit,
    onTransfer: (String) -> Unit,
    onWait: (String) -> Unit,
    onMiss: (String) -> Unit,
    onCancel: (String) -> Unit,
) {
    var filterName by rememberSaveable { mutableStateOf(BuyerDirectoryFilter.ALL.name) }
    var selectedOfferId by rememberSaveable { mutableStateOf<String?>(null) }
    val filter = BuyerDirectoryFilter.valueOf(filterName)
    val networkOnline = network.continuity.networkAvailable
    val activeOfferIds = contracts.filter { it.status == BuyerContractStatus.ACTIVE }.mapTo(hashSetOf()) { it.offerId }
    val filteredOffers = offers
        .filter { offer ->
            val eligible = state.company.reputation + .0001 >= offer.minimumReputation && offer.id !in activeOfferIds
            when (filter) {
                BuyerDirectoryFilter.ALL -> true
                BuyerDirectoryFilter.ELIGIBLE -> eligible
                BuyerDirectoryFilter.LOCKED -> !eligible && offer.id !in activeOfferIds
            }
        }
        .sortedWith(compareBy<BuyerOffer> { it.minimumReputation }.thenBy { it.companyName }.thenBy { it.buyerName })
    val selectedOffer = offers.firstOrNull { it.id == selectedOfferId }
    val repLevel = ReputationService().level(state.company.reputation)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(MineItSpacing.Sm),
    ) {
        item {
            BuyersTerminal(
                online = networkOnline,
                recoveryDays = network.continuity.recoveryDaysRemaining,
                efficiency = network.continuity.effectiveCommandEfficiency,
                reputation = state.company.reputation,
                reputationLevel = repLevel.name,
            )
        }

        if (contracts.isNotEmpty()) {
            item { MineItSectionHeader("CURRENT CONTRACTS", "${contracts.size} ACTIVE") }
            items(contracts, key = { it.id }) { contract ->
                val projection = projections[contract.id]
                val offer = offers.firstOrNull { it.id == contract.offerId }
                val relationship = state.company.buyers.relationships.firstOrNull { it.buyerId == contract.buyerId }
                CurrentBuyerContract(
                    contract = contract,
                    offer = offer,
                    relationship = relationship,
                    projection = projection,
                    onTransfer = onTransfer,
                    onWait = onWait,
                    onMiss = onMiss,
                    onCancel = onCancel,
                )
            }
        }

        item {
            MineItSectionHeader("BUYER DIRECTORY", "${filteredOffers.size} / ${offers.size}")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(MineItSpacing.Xs)) {
                BuyerDirectoryFilter.entries.forEach { candidate ->
                    MineItSecondaryButton(
                        text = candidate.name,
                        onClick = { filterName = candidate.name },
                        selected = filter == candidate,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        selectedOffer?.let { offer ->
            item {
                BuyerProfile(
                    state = state,
                    networkOnline = networkOnline,
                    offer = offer,
                    current = offer.id in activeOfferIds,
                    onClose = { selectedOfferId = null },
                    onAccept = onAccept,
                )
            }
        }

        items(filteredOffers, key = { it.id }) { offer ->
            BuyerDirectoryRow(
                state = state,
                networkOnline = networkOnline,
                offer = offer,
                current = offer.id in activeOfferIds,
                onView = { selectedOfferId = offer.id },
            )
        }

        if (offers.isEmpty()) {
            item { Text("No buyer catalogue is available yet.", color = MineItPalette.Muted) }
        }
    }
}

@Composable
private fun BuyersTerminal(
    online: Boolean,
    recoveryDays: Int,
    efficiency: Double,
    reputation: Double,
    reputationLevel: String,
) {
    MineItPanel(raised = true) {
        MineItSectionHeader(
            title = if (online) "NETWORK ONLINE" else "NETWORK OFFLINE",
            trailing = if (online) "NODE KPL-CN08" else "PRIMARY HQ LINK LOST",
            color = if (online) MineItPalette.Success else MineItPalette.Critical,
        )
        Text(
            when {
                !online -> "Existing commitments continue. New buyer contacts are blocked until the Primary Headquarters link is restored."
                recoveryDays > 0 -> "RECOVERY $recoveryDays DAYS • ${(efficiency * 100).toInt()}% EFFICIENCY"
                else -> "SECURE COMMERCIAL INDEX • KOPLIN DEEP REACH CORPORATION"
            },
            style = MaterialTheme.typography.labelSmall,
            color = if (online) MineItPalette.Muted else MineItPalette.Critical,
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(MineItSpacing.Xs)) {
            MineItStat("REPUTATION", "%.2f".format(reputation), Modifier.weight(1f))
            MineItStat("LEVEL", reputationLevel.uppercase(), Modifier.weight(1f), if (reputation >= 0.0) MineItPalette.Success else MineItPalette.Warning)
        }
    }
}

@Composable
private fun CurrentBuyerContract(
    contract: BuyerContract,
    offer: BuyerOffer?,
    relationship: BuyerRelationship?,
    projection: BuyerCollectionProjection?,
    onTransfer: (String) -> Unit,
    onWait: (String) -> Unit,
    onMiss: (String) -> Unit,
    onCancel: (String) -> Unit,
) {
    val resource = ResourceCatalogue.get(contract.resourceId)?.name ?: contract.resourceId.value
    val happiness = relationship?.happiness ?: 75
    val band = happinessBand(happiness)
    MineItPanel {
        MineItSectionHeader(offer?.companyName ?: resource, contract.ship.status.name.replace('_', ' '))
        Text(
            "${offer?.buyerName ?: contract.buyerId} • $resource • ${format(contract.quantity)} @ £${formatMoney(contract.unitRate)}",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(MineItSpacing.Xs)) {
            MineItStat("HAPPINESS", "$happiness • ${band.first}", Modifier.weight(1f), band.second)
            MineItStat("NEXT DUE", absoluteDate(contract.nextDueAbsoluteDay), Modifier.weight(1f))
        }
        projection?.let {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(MineItSpacing.Xs)) {
                MineItStat("READY", "${format(it.transferableQuantity)}/${format(contract.quantity)}", Modifier.weight(1f), if (it.canTransfer) MineItPalette.Success else MineItPalette.Warning)
                MineItStat("LATE", "${it.daysLate}d", Modifier.weight(1f), if (it.daysLate > 0) MineItPalette.Warning else MineItPalette.Text)
            }
        }
        relationship?.let {
            Text(
                "${it.fulfilledShipments} fulfilled • ${it.missedShipments} missed • £${formatMoney(it.lifetimeRevenue)} lifetime revenue",
                style = MaterialTheme.typography.labelSmall,
                color = MineItPalette.Muted,
            )
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

@Composable
private fun BuyerDirectoryRow(
    state: GameState,
    networkOnline: Boolean,
    offer: BuyerOffer,
    current: Boolean,
    onView: () -> Unit,
) {
    val resource = ResourceCatalogue.get(offer.resourceId)?.name ?: offer.resourceId.value
    val eligible = state.company.reputation + .0001 >= offer.minimumReputation
    Surface(
        color = MineItPalette.Control,
        shape = RoundedCornerShape(MineItRadius.Small),
        border = BorderStroke(1.dp, if (current) MineItPalette.Success.copy(alpha = .5f) else MineItPalette.Line),
    ) {
        Column(Modifier.fillMaxWidth().padding(MineItSpacing.Sm), verticalArrangement = Arrangement.spacedBy(MineItSpacing.Xs)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(MineItSpacing.Sm)) {
                Column(Modifier.weight(1f)) {
                    Text(offer.buyerName, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    Text(offer.companyName, style = MaterialTheme.typography.labelSmall, color = MineItPalette.Muted)
                }
                MineItStatusBadge(
                    when {
                        current -> "CURRENT"
                        !eligible -> "REP LOCKED"
                        !networkOnline -> "NETWORK OFFLINE"
                        else -> "AVAILABLE"
                    },
                    when {
                        current -> MineItPalette.Success
                        !eligible -> MineItPalette.Muted
                        !networkOnline -> MineItPalette.Critical
                        else -> MineItPalette.Accent
                    },
                )
            }
            Text(
                "$resource • ${offer.minimumQuality.name.lowercase()}+ • ${format(offer.quantity)} units • £${formatMoney(offer.unitRate)}/u • every ${offer.intervalDays}d • REP ${"%.2f".format(offer.minimumReputation)}",
                style = MaterialTheme.typography.labelSmall,
                color = MineItPalette.Muted,
            )
            MineItSecondaryButton("VIEW", onView, Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun BuyerProfile(
    state: GameState,
    networkOnline: Boolean,
    offer: BuyerOffer,
    current: Boolean,
    onClose: () -> Unit,
    onAccept: (String) -> Unit,
) {
    val resource = ResourceCatalogue.get(offer.resourceId)?.name ?: offer.resourceId.value
    val eligible = state.company.reputation + .0001 >= offer.minimumReputation
    MineItPanel(raised = true) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(MineItSpacing.Sm)) {
            Column(Modifier.weight(1f)) {
                Text(offer.buyerName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(offer.companyName, style = MaterialTheme.typography.bodySmall, color = MineItPalette.Accent)
            }
            MineItSecondaryButton("CLOSE", onClose)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(MineItSpacing.Xs)) {
            MineItStat("RESOURCE", resource.uppercase(), Modifier.weight(1f))
            MineItStat("QUALITY", offer.minimumQuality.name, Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(MineItSpacing.Xs)) {
            MineItStat("LOAD", format(offer.quantity), Modifier.weight(1f))
            MineItStat("PRICE", "£${formatMoney(offer.unitRate)}", Modifier.weight(1f))
            MineItStat("VALUE", "£${formatMoney(offer.fullValue)}", Modifier.weight(1f))
        }
        Text(
            "Collection every ${offer.intervalDays} days • minimum reputation ${"%.2f".format(offer.minimumReputation)}.",
            style = MaterialTheme.typography.bodySmall,
            color = MineItPalette.Muted,
        )
        MineItPrimaryButton(
            text = when {
                current -> "CURRENT CONTRACT"
                !networkOnline -> "NETWORK OFFLINE"
                !eligible -> "REP LOCKED"
                else -> "ENTER CONTRACT"
            },
            onClick = { onAccept(offer.id) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !current && networkOnline && eligible,
        )
    }
}

@Composable
private fun GameLogPanel(state: GameState) {
    LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(MineItSpacing.Xs)) {
        item { MineItSectionHeader("GAME LOG", "${state.gameLog.events.size} EVENTS") }
        items(state.gameLog.events.asReversed().take(100), key = { it.id }) { event ->
            Surface(
                color = MineItPalette.Control,
                shape = RoundedCornerShape(MineItRadius.Small),
                border = BorderStroke(1.dp, MineItPalette.Line),
            ) {
                Column(Modifier.fillMaxWidth().padding(horizontal = MineItSpacing.Sm, vertical = MineItSpacing.Xs)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(MineItSpacing.Sm)) {
                        Text("Y${event.year} D${event.day}", style = MaterialTheme.typography.labelSmall, color = MineItPalette.Accent)
                        Text(event.type.uppercase(), modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = MineItPalette.Muted)
                        Text("#${event.id}", style = MaterialTheme.typography.labelSmall, color = MineItPalette.Muted)
                    }
                    Text(event.message, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        if (state.gameLog.events.isEmpty()) item { Text("No logged commercial events yet.", color = MineItPalette.Muted) }
    }
}

private fun happinessBand(value: Int): Pair<String, androidx.compose.ui.graphics.Color> = when {
    value <= 33 -> "RED" to MineItPalette.Critical
    value <= 66 -> "AMBER" to MineItPalette.Warning
    else -> "GREEN" to MineItPalette.Success
}

private fun absoluteDate(value: Int): String {
    val day = value.coerceAtLeast(1)
    return "Y${(day - 1) / 360 + 1} D${(day - 1) % 360 + 1}"
}
