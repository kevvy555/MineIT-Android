package com.mineit.android.domain.trade

import com.mineit.android.domain.config.MineItConfig
import com.mineit.android.domain.model.ColonyState
import com.mineit.android.domain.model.ColonyStatus
import com.mineit.android.domain.model.GameState
import com.mineit.android.domain.model.ResourceId
import com.mineit.android.domain.resources.QualityBand
import com.mineit.android.domain.resources.ResourceCatalogue
import com.mineit.android.domain.resources.ResourceQuality
import com.mineit.android.domain.resources.ResourceStock
import kotlin.math.floor
import kotlin.math.min

/** Canonical native corporate-ship trade owner for the pinned MineIT 5.13.15 rules. */
class CorporateTradeService {
    fun cargoCapacity(state: GameState): Double = state.activeColony.trade.visitCargoCapacity
        ?: min(MineItConfig.TRADE_MAX_CARGO, MineItConfig.TRADE_BASE_CARGO + state.company.reputation.coerceAtLeast(0) * MineItConfig.TRADE_CARGO_PER_REP)

    fun cargoRemaining(state: GameState): Double = (cargoCapacity(state) - state.activeColony.trade.cargoUsed).coerceAtLeast(0.0)

    fun exportCapacity(state: GameState): Double = state.activeColony.trade.visitExportCapacity
        ?: min(MineItConfig.TRADE_MAX_EXPORT_CARGO, MineItConfig.TRADE_BASE_EXPORT_CARGO + state.company.reputation.coerceAtLeast(0) * MineItConfig.TRADE_EXPORT_PER_REP)

    fun exportRemaining(state: GameState): Double = (exportCapacity(state) - state.activeColony.trade.exportUsed).coerceAtLeast(0.0)

    fun passengerRemaining(state: GameState): Int = (MineItConfig.TRADE_PASSENGER_CAPACITY - state.activeColony.trade.passengersUsed).coerceAtLeast(0)

    fun daysUntilArrival(state: GameState): Int = if (state.activeColony.trade.active || state.activeColony.trade.orbitalHolding) 0
        else (state.activeColony.trade.nextArrivalAbsoluteDay - state.date.toAbsoluteDay().value).coerceAtLeast(0)

    fun shouldArrive(state: GameState): Boolean {
        val colony = state.activeColony
        return !colony.trade.active && colony.status != ColonyStatus.DEAD && state.date.toAbsoluteDay().value >= colony.trade.nextArrivalAbsoluteDay
    }

    fun arrive(state: GameState): TradeActionResult {
        val colony = state.activeColony
        if (colony.trade.active) return TradeActionResult(state, false, "Corporate ship is already docked.")
        if (colony.status == ColonyStatus.DEAD) return TradeActionResult(state, false, "This colony cannot receive the corporate ship.")
        val arrivedAt = state.date.toAbsoluteDay().value
        var nextArrival = colony.trade.nextArrivalAbsoluteDay
        do nextArrival += MineItConfig.TRADE_INTERVAL_DAYS while (nextArrival <= arrivedAt)
        val nextTrade = colony.trade.copy(
            active = true,
            nextArrivalAbsoluteDay = nextArrival,
            visits = colony.trade.visits + 1,
            arrivedAtAbsoluteDay = arrivedAt,
            cargoUsed = 0.0,
            exportUsed = 0.0,
            passengersUsed = 0,
            visitCargoCapacity = min(MineItConfig.TRADE_MAX_CARGO, MineItConfig.TRADE_BASE_CARGO + state.company.reputation.coerceAtLeast(0) * MineItConfig.TRADE_CARGO_PER_REP),
            visitExportCapacity = min(MineItConfig.TRADE_MAX_EXPORT_CARGO, MineItConfig.TRADE_BASE_EXPORT_CARGO + state.company.reputation.coerceAtLeast(0) * MineItConfig.TRADE_EXPORT_PER_REP),
            exportReputationAwarded = false,
            orbitalHolding = false,
            orbitalSinceAbsoluteDay = null,
        )
        return TradeActionResult(updateColony(state, colony.copy(trade = nextTrade)), true, "Corporate trade ship docked.")
    }

    fun depart(state: GameState): TradeActionResult {
        val colony = state.activeColony
        if (!colony.trade.active) return TradeActionResult(state, false, "No corporate ship is docked.")
        return TradeActionResult(
            updateColony(state, colony.copy(trade = colony.trade.copy(active = false, arrivedAtAbsoluteDay = null, visitCargoCapacity = null, visitExportCapacity = null))),
            true,
            "Corporate trade ship departed.",
        )
    }

    fun setColonyTradeReserve(state: GameState, amount: Double): GameState = updateColony(
        state,
        state.activeColony.copy(tradeReserve = floor(amount.takeIf { it.isFinite() }?.coerceAtLeast(0.0) ?: 0.0)),
    )

    fun sellableAmount(state: GameState, resourceId: ResourceId): Double =
        (state.activeColony.inventory.amountFor(resourceId) - state.activeColony.tradeReserve).coerceAtLeast(0.0)

    fun sellPrice(resourceId: ResourceId, band: QualityBand, processingBonus: Double = 0.0): Double {
        val definition = ResourceCatalogue.require(resourceId)
        val processing = 1.0 + processingBonus.coerceIn(0.0, MineItConfig.INDUSTRY_PROCESSING_MAX_BONUS)
        return definition.sellPrice * MineItConfig.RESOURCE_VALUE_SCALE * ResourceQuality.forBand(band).valueMultiplier * processing
    }

    fun buyPrice(resourceId: ResourceId): Double =
        ResourceCatalogue.require(resourceId).sellPrice * MineItConfig.RESOURCE_VALUE_SCALE * MineItConfig.CORPORATE_BUY_MARKUP

    fun quoteSell(state: GameState, resourceId: ResourceId, amount: Double, processingBonus: Double = 0.0): TradeQuote {
        val stock = state.activeColony.inventory.find(resourceId) ?: return TradeQuote(0.0, 0.0)
        var remaining = minOf(amount.coerceAtLeast(0.0), sellableAmount(state, resourceId), exportRemaining(state))
        var quantity = 0.0
        var revenue = 0.0
        lotsDescending(stock, processingBonus).forEach { (_, available, unitPrice) ->
            if (remaining > .0001) {
                val take = min(remaining, available)
                remaining -= take
                quantity += take
                revenue += take * unitPrice
            }
        }
        return TradeQuote(quantity, revenue)
    }

    fun sell(state: GameState, resourceId: ResourceId, amount: Double, spaceportServicesAvailable: Boolean, processingBonus: Double = 0.0): TradeActionResult {
        val colony = state.activeColony
        if (!colony.trade.active) return TradeActionResult(state, false, "No corporate ship is docked.")
        if (!spaceportServicesAvailable) return TradeActionResult(state, false, SPACEPORT_OFFLINE)
        val stock = colony.inventory.find(resourceId) ?: return TradeActionResult(state, false, "No stock available.")
        val quote = quoteSell(state, resourceId, amount, processingBonus)
        if (quote.quantity <= .0001) {
            val message = if (sellableAmount(state, resourceId) <= .0001) "This stock is protected by the colony trade reserve." else "Ship export capacity is exhausted for this visit."
            return TradeActionResult(state, false, message)
        }
        var remaining = quote.quantity
        var nextBands = stock.qualityBands
        lotsDescending(stock, processingBonus).forEach { (band, available, _) ->
            if (remaining > .0001) {
                val take = min(remaining, available)
                remaining -= take
                nextBands = nextBands + (band to (available - take).coerceAtLeast(0.0))
            }
        }
        val nextStock = stock.copy(qualityBands = nextBands)
        val nextInventory = colony.inventory.copy(resources = colony.inventory.resources.map { if (it.resourceId == resourceId) nextStock else it })
        val nextTrade = colony.trade.copy(exportUsed = colony.trade.exportUsed + quote.quantity, exportReputationAwarded = true)
        val nextContract = colony.contract?.copy(localRevenue = colony.contract.localRevenue + quote.value)
        val nextColony = colony.copy(inventory = nextInventory, trade = nextTrade, contract = nextContract)
        val nextCompany = state.company.copy(cash = state.company.cash + quote.value, earnedRevenue = state.company.earnedRevenue + quote.value)
        val next = updateColony(state.copy(company = nextCompany), nextColony)
        return TradeActionResult(next, true, "Sold ${quote.quantity} units for £${"%.2f".format(quote.value)}.", quote.quantity, quote.value)
    }

    fun buy(state: GameState, resourceId: ResourceId, amount: Double, spaceportServicesAvailable: Boolean): TradeActionResult {
        val colony = state.activeColony
        if (!colony.trade.active) return TradeActionResult(state, false, "No corporate ship is docked.")
        if (!spaceportServicesAvailable) return TradeActionResult(state, false, SPACEPORT_OFFLINE)
        val definition = ResourceCatalogue.get(resourceId) ?: return TradeActionResult(state, false, "Unknown resource.")
        val requested = floor(amount.takeIf { it.isFinite() }?.coerceAtLeast(0.0) ?: 0.0)
        if (requested <= 0.0) return TradeActionResult(state, false, "Nothing selected.")
        val cargo = cargoRemaining(state)
        if (cargo <= 0.0) return TradeActionResult(state, false, "Ship import cargo capacity is exhausted for this visit.")
        val unitPrice = buyPrice(resourceId)
        val affordable = floor(state.company.cash.coerceAtLeast(0.0) / unitPrice)
        val quantity = minOf(requested, affordable, cargo)
        if (quantity <= 0.0) return TradeActionResult(state, false, "Insufficient cash.")
        val cost = quantity * unitPrice
        val nextInventory = colony.inventory.store(resourceId, definition.category, quantity, quality = 201)
        val nextContract = colony.contract?.copy(localCosts = colony.contract.localCosts + cost)
        val nextColony = colony.copy(inventory = nextInventory, trade = colony.trade.copy(cargoUsed = colony.trade.cargoUsed + quantity), contract = nextContract)
        val next = updateColony(state.copy(company = state.company.copy(cash = state.company.cash - cost)), nextColony)
        return TradeActionResult(next, true, "Bought $quantity units for £${"%.2f".format(cost)}.", quantity, cost)
    }

    private fun lotsDescending(stock: ResourceStock, processingBonus: Double): List<Triple<QualityBand, Double, Double>> =
        stock.qualityBands.map { (band, available) -> Triple(band, available, sellPrice(stock.resourceId, band, processingBonus)) }.sortedByDescending { it.third }

    private fun updateColony(state: GameState, colony: ColonyState): GameState =
        state.copy(colonies = state.colonies.map { if (it.id == colony.id) colony else it })

    companion object {
        const val SPACEPORT_OFFLINE = "Basic Spaceport services are offline: provide its full 10 Power to enable trade, cargo, passenger and Engineering services."
    }
}
