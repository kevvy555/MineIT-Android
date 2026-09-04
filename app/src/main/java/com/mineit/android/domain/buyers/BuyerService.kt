package com.mineit.android.domain.buyers

import com.mineit.android.domain.config.MineItConfig
import com.mineit.android.domain.events.CorporateEvent
import com.mineit.android.domain.events.CorporateEventType
import com.mineit.android.domain.model.GameState
import com.mineit.android.domain.reputation.ReputationService
import com.mineit.android.domain.resources.QualityBand
import com.mineit.android.domain.resources.ResourceCatalogue
import com.mineit.android.domain.resources.ResourceQuality
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.round

/**
 * Buyer-market lifecycle owner. Offers are deterministic for the game seed; collection, waiting,
 * fulfilment, happiness and reputation rules mirror the current web BuyerService.
 */
class BuyerService(
    private val reputationService: ReputationService = ReputationService(),
) {
    fun ensureMarket(state: GameState): GameState {
        if (state.company.buyers.offers.size == OFFER_COUNT) return state
        val market = state.company.buyers.copy(offers = generateOffers(state.activeColony.seed))
        return state.copy(company = state.company.copy(buyers = market))
    }

    fun catalog(state: GameState): List<BuyerOffer> = ensureMarket(state).company.buyers.offers

    fun availableOffers(state: GameState): List<BuyerOffer> {
        val ready = ensureMarket(state)
        val now = ready.date.toAbsoluteDay().value
        return ready.company.buyers.offers.filter { offer ->
            ready.company.reputation + .0001 >= offer.minimumReputation &&
                ready.company.buyers.contracts.none { it.offerId == offer.id && it.status == BuyerContractStatus.ACTIVE } &&
                relationship(ready, offer.buyerId).let { rel ->
                    (rel.cooldownUntilAbsoluteDay ?: 0) <= now && rel.consecutiveRed < 2
                }
        }
    }

    fun canEnter(state: GameState, offerId: String, networkAvailable: Boolean = true): Pair<Boolean, String> {
        val ready = ensureMarket(state)
        val offer = ready.company.buyers.offers.firstOrNull { it.id == offerId } ?: return false to "Unknown buyer offer."
        if (!networkAvailable) return false to "Conglomerate network offline: restore the Primary Headquarters before creating a buyer commitment."
        if (ready.company.buyers.contracts.any { it.offerId == offerId && it.status == BuyerContractStatus.ACTIVE }) return false to "That buyer already has an active colony contract."
        val relationship = relationship(ready, offer.buyerId)
        val now = ready.date.toAbsoluteDay().value
        if (relationship.consecutiveRed >= 2) return false to "This buyer terminated the commercial relationship."
        if ((relationship.cooldownUntilAbsoluteDay ?: 0) > now) return false to "This buyer is cooling down."
        if (ready.company.reputation + .0001 < offer.minimumReputation) return false to "Requires ${"%.2f".format(offer.minimumReputation)} reputation."
        return true to "Available."
    }

    fun enterContract(state: GameState, offerId: String, networkAvailable: Boolean = true): BuyerActionResult {
        var ready = ensureMarket(state)
        val check = canEnter(ready, offerId, networkAvailable)
        if (!check.first) return BuyerActionResult(ready, false, check.second)
        val market = ready.company.buyers
        val offer = market.offers.first { it.id == offerId }
        val sequence = market.contracts.size + 1
        val now = ready.date.toAbsoluteDay().value
        val contract = BuyerContract(
            id = "buyer-contract-$sequence",
            offerId = offer.id,
            buyerId = offer.buyerId,
            colonyId = ready.activeColonyId,
            resourceId = offer.resourceId,
            minimumQuality = offer.minimumQuality,
            quantity = offer.quantity,
            unitRate = offer.unitRate,
            intervalDays = offer.intervalDays,
            nextDueAbsoluteDay = now + offer.intervalDays,
        )
        val relationships = upsertRelationship(market.relationships, relationship(ready, offer.buyerId))
        ready = ready.copy(company = ready.company.copy(buyers = market.copy(
            contracts = market.contracts + contract,
            relationships = relationships,
        )))
        return BuyerActionResult(ready, true, "Buyer contract accepted with ${offer.companyName}.")
    }

    fun activeContracts(state: GameState) = state.company.buyers.contracts.filter {
        it.status == BuyerContractStatus.ACTIVE && it.colonyId == state.activeColonyId
    }

    fun processDay(state: GameState, berthAvailable: Boolean = true): BuyerProcessResult {
        var ready = ensureMarket(state)
        val now = ready.date.toAbsoluteDay().value
        val events = mutableListOf<CorporateEvent>()
        val contracts = ready.company.buyers.contracts.map { original ->
            if (original.status != BuyerContractStatus.ACTIVE || original.colonyId != ready.activeColonyId) return@map original
            var contract = original
            if (contract.ship.status == BuyerShipStatus.IDLE && now >= contract.nextDueAbsoluteDay) {
                contract = contract.copy(ship = BuyerShipState(
                    status = if (berthAvailable) BuyerShipStatus.DOCKED else BuyerShipStatus.ORBITAL_HOLDING,
                    dueAbsoluteDay = contract.nextDueAbsoluteDay,
                    attemptIndex = 0,
                    nextEventAbsoluteDay = contract.nextDueAbsoluteDay,
                    eventPending = false,
                ))
            } else if (contract.ship.status == BuyerShipStatus.ORBITAL_HOLDING && berthAvailable) {
                contract = contract.copy(ship = contract.ship.copy(status = BuyerShipStatus.DOCKED))
            }
            val nextEvent = contract.ship.nextEventAbsoluteDay
            if (contract.ship.status in setOf(BuyerShipStatus.DOCKED, BuyerShipStatus.ORBITAL_HOLDING) &&
                !contract.ship.eventPending && nextEvent != null && now >= nextEvent
            ) {
                contract = contract.copy(ship = contract.ship.copy(eventPending = true))
                events += eventFor(ready, contract)
            }
            contract
        }
        ready = ready.copy(company = ready.company.copy(buyers = ready.company.buyers.copy(contracts = contracts)))
        return BuyerProcessResult(ready, events)
    }

    fun recoverEvents(state: GameState, berthAvailable: Boolean = true): BuyerProcessResult {
        var processed = processDay(state, berthAvailable)
        val now = processed.state.date.toAbsoluteDay().value
        val extra = processed.state.company.buyers.contracts.filter { contract ->
            contract.status == BuyerContractStatus.ACTIVE &&
                contract.colonyId == processed.state.activeColonyId &&
                contract.ship.status in setOf(BuyerShipStatus.DOCKED, BuyerShipStatus.ORBITAL_HOLDING) &&
                (contract.ship.nextEventAbsoluteDay ?: contract.ship.dueAbsoluteDay ?: Int.MAX_VALUE) <= now
        }.map { eventFor(processed.state, it) }
        return processed.copy(events = (processed.events + extra).distinctBy { it.contractId })
    }

    fun projection(state: GameState, contractId: String): BuyerCollectionProjection? {
        val contract = state.company.buyers.contracts.firstOrNull { it.id == contractId } ?: return null
        val stock = qualifyingStock(state, contract)
        val transfer = min(contract.quantity, stock)
        val ratio = if (contract.quantity > 0.0) transfer / contract.quantity else 0.0
        val due = contract.ship.dueAbsoluteDay ?: contract.nextDueAbsoluteDay
        val late = max(0, state.date.toAbsoluteDay().value - due)
        val happiness = resolvedHappinessChange(late, ratio)
        return BuyerCollectionProjection(
            contract = contract,
            qualifyingStock = stock,
            transferableQuantity = transfer,
            completionRatio = ratio,
            daysLate = late,
            happinessChange = happiness,
            canTransfer = contract.ship.status == BuyerShipStatus.DOCKED && ratio >= .5,
        )
    }

    fun transfer(state: GameState, contractId: String): BuyerActionResult {
        val projection = projection(state, contractId) ?: return BuyerActionResult(state, false, "Unknown buyer contract.")
        if (!projection.canTransfer) return BuyerActionResult(state, false, "At least 50% of the shipment must be ready and the buyer ship must be docked.")
        val contract = projection.contract
        val happinessChange = projection.happinessChange ?: return BuyerActionResult(state, false, "Shipment is below the minimum accepted fulfilment.")
        val removed = removeQualifyingLowestFirst(state, contract, projection.transferableQuantity)
        val revenue = removed.second * contract.unitRate
        var next = removed.first
        var relationship = relationship(next, contract.buyerId)
        val actualHappinessChange = happinessChange.coerceIn(1 - relationship.happiness, 100 - relationship.happiness)
        relationship = relationship.copy(
            happiness = (relationship.happiness + actualHappinessChange).coerceIn(1, 100),
            missedShipments = 0,
            fulfilledShipments = relationship.fulfilledShipments + 1,
            lifetimeRevenue = relationship.lifetimeRevenue + revenue,
        )
        if (actualHappinessChange < 0) next = reputationService.applyBuyerLoss(next, actualHappinessChange.toDouble()).state
        next = reputationService.awardBuyerShipment(next).state
        val consecutiveRed = if (relationship.happiness <= 33) relationship.consecutiveRed + 1 else 0
        relationship = relationship.copy(consecutiveRed = consecutiveRed)
        val terminated = consecutiveRed >= 2
        val now = next.date.toAbsoluteDay().value
        val updatedContract = contract.copy(
            status = if (terminated) BuyerContractStatus.TERMINATED else BuyerContractStatus.ACTIVE,
            nextDueAbsoluteDay = now + contract.intervalDays,
            ship = BuyerShipState(),
        )
        val colony = next.activeColony
        val localContract = colony.contract?.copy(localRevenue = colony.contract.localRevenue + revenue)
        val nextColony = colony.copy(contract = localContract)
        next = next.copy(
            company = next.company.copy(
                cash = next.company.cash + revenue,
                earnedRevenue = next.company.earnedRevenue + revenue,
                buyers = next.company.buyers.copy(
                    contracts = replaceContract(next.company.buyers.contracts, updatedContract),
                    relationships = upsertRelationship(next.company.buyers.relationships, relationship),
                ),
            ),
            colonies = next.colonies.map { if (it.id == nextColony.id) nextColony else it },
        )
        val result = fulfilmentLabel(projection.completionRatio)
        val message = if (terminated) "Buyer shipment $result resolved; relationship terminated after consecutive Red cycles." else "Buyer shipment $result resolved for £${"%.2f".format(revenue)}."
        return BuyerActionResult(next, true, message, revenue, terminated)
    }

    fun continueWaiting(state: GameState, contractId: String): BuyerActionResult {
        val contract = state.company.buyers.contracts.firstOrNull { it.id == contractId } ?: return BuyerActionResult(state, false, "Unknown buyer contract.")
        if (contract.ship.status !in setOf(BuyerShipStatus.DOCKED, BuyerShipStatus.ORBITAL_HOLDING)) return BuyerActionResult(state, false, "Buyer ship is not waiting.")
        val attempt = contract.ship.attemptIndex
        if (attempt >= 3) return BuyerActionResult(state, false, "This is the final collection attempt; resolve the shipment or miss.")
        val due = contract.ship.dueAbsoluteDay ?: contract.nextDueAbsoluteDay
        val nextAttempt = attempt + 1
        val nextDay = due + MineItConfig.BUYER_COLLECTION_ATTEMPT_OFFSETS[nextAttempt]
        val updated = contract.copy(ship = contract.ship.copy(
            attemptIndex = nextAttempt,
            nextEventAbsoluteDay = nextDay,
            eventPending = false,
        ))
        val next = state.copy(company = state.company.copy(buyers = state.company.buyers.copy(
            contracts = replaceContract(state.company.buyers.contracts, updated),
        )))
        return BuyerActionResult(next, true, "Buyer will wait until absolute day $nextDay.")
    }

    fun resolveMiss(state: GameState, contractId: String): BuyerActionResult {
        val contract = state.company.buyers.contracts.firstOrNull { it.id == contractId } ?: return BuyerActionResult(state, false, "Unknown buyer contract.")
        if (contract.ship.attemptIndex < 3) return BuyerActionResult(state, false, "A missed shipment can only be resolved at the final collection attempt.")
        var relationship = relationship(state, contract.buyerId)
        val missNumber = relationship.missedShipments + 1
        val loss = when (missNumber) { 1 -> -10; 2 -> -20; else -> -30 }
        val actualLoss = loss.coerceAtLeast(1 - relationship.happiness)
        relationship = relationship.copy(
            happiness = (relationship.happiness + actualLoss).coerceIn(1, 100),
            missedShipments = missNumber,
        )
        var next = reputationService.applyBuyerLoss(state, actualLoss.toDouble()).state
        val red = if (relationship.happiness <= 33) relationship.consecutiveRed + 1 else 0
        val terminated = missNumber >= 3 || red >= 2
        relationship = relationship.copy(consecutiveRed = red)
        val now = next.date.toAbsoluteDay().value
        val updated = contract.copy(
            status = if (terminated) BuyerContractStatus.TERMINATED else BuyerContractStatus.ACTIVE,
            nextDueAbsoluteDay = now + contract.intervalDays,
            ship = BuyerShipState(),
        )
        next = next.copy(company = next.company.copy(buyers = next.company.buyers.copy(
            contracts = replaceContract(next.company.buyers.contracts, updated),
            relationships = upsertRelationship(next.company.buyers.relationships, relationship),
        )))
        val reason = when {
            missNumber >= 3 -> "Third missed shipment; buyer terminated the relationship."
            red >= 2 -> "Buyer terminated the relationship after consecutive resolved Red shipment cycles."
            else -> "Missed shipment resolved with $actualLoss buyer happiness."
        }
        return BuyerActionResult(next, true, reason, terminated = terminated)
    }

    fun cancelContract(state: GameState, contractId: String): BuyerActionResult {
        val contract = state.company.buyers.contracts.firstOrNull { it.id == contractId } ?: return BuyerActionResult(state, false, "Unknown buyer contract.")
        if (contract.ship.status in setOf(BuyerShipStatus.DOCKED, BuyerShipStatus.ORBITAL_HOLDING)) return BuyerActionResult(state, false, "Cannot cancel while the buyer ship is waiting.")
        var relationship = relationship(state, contract.buyerId)
        val actualLoss = (-5).coerceAtLeast(1 - relationship.happiness)
        relationship = relationship.copy(
            happiness = (relationship.happiness + actualLoss).coerceIn(1, 100),
            cooldownUntilAbsoluteDay = state.date.toAbsoluteDay().value + contract.intervalDays,
        )
        var next = reputationService.applyBuyerLoss(state, actualLoss.toDouble()).state
        val updated = contract.copy(status = BuyerContractStatus.CANCELLED)
        next = next.copy(company = next.company.copy(buyers = next.company.buyers.copy(
            contracts = replaceContract(next.company.buyers.contracts, updated),
            relationships = upsertRelationship(next.company.buyers.relationships, relationship),
        )))
        return BuyerActionResult(next, true, "Buyer contract cancelled; relationship happiness reduced by ${-actualLoss}.")
    }

    fun timingChange(daysLate: Int): Int = when {
        daysLate <= 0 -> 1
        daysLate <= 5 -> -1
        daysLate <= 10 -> -2
        else -> -3
    }

    fun partialChange(ratio: Double): Int? = when {
        ratio >= .999999 -> 0
        ratio >= .75 -> -1
        ratio >= .5 -> -2
        else -> null
    }

    fun resolvedHappinessChange(daysLate: Int, ratio: Double): Int? {
        val partial = partialChange(ratio) ?: return null
        if (daysLate <= 0 && ratio < .999999) return partial
        return timingChange(daysLate) + partial
    }

    fun fulfilmentLabel(ratio: Double): String = when {
        ratio >= .999999 -> "full"
        ratio >= .75 -> "minor-partial"
        ratio >= .5 -> "major-partial"
        else -> "not-accepted"
    }

    private fun relationship(state: GameState, buyerId: String): BuyerRelationship =
        state.company.buyers.relationships.firstOrNull { it.buyerId == buyerId } ?: BuyerRelationship(buyerId)

    private fun qualifyingStock(state: GameState, contract: BuyerContract): Double {
        val stock = state.activeColony.inventory.find(contract.resourceId) ?: return 0.0
        val minRank = QUALITY_ORDER.indexOf(contract.minimumQuality)
        return stock.qualityBands.filterKeys { QUALITY_ORDER.indexOf(it) >= minRank }.values.sum()
    }

    private fun removeQualifyingLowestFirst(state: GameState, contract: BuyerContract, requested: Double): Pair<GameState, Double> {
        val colony = state.activeColony
        val stock = colony.inventory.find(contract.resourceId) ?: return state to 0.0
        val minRank = QUALITY_ORDER.indexOf(contract.minimumQuality)
        var remaining = requested.coerceAtLeast(0.0)
        var removed = 0.0
        var bands = stock.qualityBands
        QUALITY_ORDER.drop(minRank).forEach { band ->
            if (remaining > .0001) {
                val available = bands[band] ?: 0.0
                val take = min(available, remaining)
                if (take > 0.0) {
                    bands = bands + (band to (available - take).coerceAtLeast(0.0))
                    remaining -= take
                    removed += take
                }
            }
        }
        val updatedStock = stock.copy(qualityBands = bands)
        val inventory = colony.inventory.copy(resources = colony.inventory.resources.map { if (it.resourceId == stock.resourceId) updatedStock else it })
        val updatedColony = colony.copy(inventory = inventory)
        return state.copy(colonies = state.colonies.map { if (it.id == colony.id) updatedColony else it }) to removed
    }

    private fun eventFor(state: GameState, contract: BuyerContract): CorporateEvent = CorporateEvent(
        type = CorporateEventType.BUYER,
        colonyId = contract.colonyId,
        colonyName = state.colonies.firstOrNull { it.id == contract.colonyId }?.name ?: "Colony",
        contractId = contract.id,
        attemptIndex = contract.ship.attemptIndex,
        dueAbsoluteDay = contract.ship.dueAbsoluteDay,
    )

    private fun replaceContract(all: List<BuyerContract>, updated: BuyerContract) = all.map { if (it.id == updated.id) updated else it }
    private fun upsertRelationship(all: List<BuyerRelationship>, updated: BuyerRelationship) = all.filterNot { it.buyerId == updated.buyerId } + updated

    private fun generateOffers(seed: Long): List<BuyerOffer> {
        val resources = ResourceCatalogue.all.filterNot { it.manufactured }
        return List(OFFER_COUNT) { index ->
            val number = index + 1
            val minRep = round2(100.0 * (index.toDouble() / (OFFER_COUNT - 1)).pow(1.35))
            val tier = tierFor(minRep)
            val random = JsRandom(fnv1a32("$seed|buyer-offer-v1|buyer-${number.toString().padStart(4, '0')}"))
            val phase = when { minRep < 20 -> 0; minRep < 60 -> 1; else -> 2 }
            val eligible = resources.filter { definition ->
                when (phase) {
                    0 -> definition.miningLevel <= 4
                    1 -> definition.miningLevel <= 7
                    else -> true
                }
            }
            val resource = eligible[(random.next() * eligible.size).toInt().coerceIn(0, eligible.lastIndex)]
            val minimumQuality = when (tier.name) {
                "Entry" -> if (random.next() < .5) QualityBand.COMMON else QualityBand.GOOD
                "Regional" -> if (random.next() < .67) QualityBand.GOOD else QualityBand.EXCELLENT
                else -> if (random.next() < .55) QualityBand.EXCELLENT else QualityBand.EXCEPTIONAL
            }
            val shipCapacity = SHIP_CAPACITIES[(tier.shipMin - 1 + (random.next() * (tier.shipMax - tier.shipMin + 1)).toInt()).coerceIn(0, SHIP_CAPACITIES.lastIndex)]
            val directRate = resource.sellPrice * MineItConfig.RESOURCE_VALUE_SCALE * ResourceQuality.forBand(minimumQuality).valueMultiplier
            val envelope = when (phase) { 0 -> .65 to .88; 1 -> .60 to .90; else -> .55 to .92 }
            val priceFactor = envelope.first + random.next() * (envelope.second - envelope.first)
            val unitRate = round4(directRate * min(.999, priceFactor))
            val quantityFloor = max(50.0, min(shipCapacity * .12, 1_000.0))
            val quantityCeiling = max(quantityFloor, shipCapacity * .9)
            val quantity = round(quantityFloor + random.next() * (quantityCeiling - quantityFloor))
            val interval = round(tier.intervalMin + random.next() * (tier.intervalMax - tier.intervalMin)).toInt()
            BuyerOffer(
                id = "offer-buyer-${number.toString().padStart(4, '0')}",
                buyerId = "buyer-${number.toString().padStart(4, '0')}",
                buyerName = "Buyer ${number.toString().padStart(4, '0')}",
                companyName = "Commercial Partner ${number.toString().padStart(4, '0')}",
                resourceId = resource.id,
                minimumQuality = minimumQuality,
                quantity = min(quantity, shipCapacity),
                unitRate = unitRate,
                intervalDays = interval,
                minimumReputation = minRep,
            )
        }
    }

    private fun tierFor(rep: Double): Tier = when {
        rep < 10 -> Tier("Entry", 1, 8, 45, 90)
        rep < 25 -> Tier("Regional", 4, 12, 35, 75)
        rep < 50 -> Tier("Major", 8, 18, 25, 60)
        rep < 75 -> Tier("Strategic", 13, 24, 20, 45)
        else -> Tier("Premier", 18, 30, 15, 40)
    }

    private fun round2(value: Double) = round(value * 100.0) / 100.0
    private fun round4(value: Double) = round(value * 10_000.0) / 10_000.0

    private data class Tier(val name: String, val shipMin: Int, val shipMax: Int, val intervalMin: Int, val intervalMax: Int)

    private class JsRandom(private var seed: Int) {
        fun next(): Double {
            seed += 0x6D2B79F5.toInt()
            var t = seed
            t = (t xor (t ushr 15)) * (t or 1)
            t = t xor (t + (t xor (t ushr 7)) * (t or 61))
            val result = (t xor (t ushr 14)).toUInt().toLong()
            return result / 4294967296.0
        }
    }

    private fun fnv1a32(value: String): Int {
        var hash = 0x811C9DC5u.toInt()
        value.forEach { char ->
            hash = hash xor char.code
            hash *= 16777619
        }
        return hash
    }

    companion object {
        const val OFFER_COUNT = 1_000
        private val QUALITY_ORDER = listOf(
            QualityBand.COMMON,
            QualityBand.GOOD,
            QualityBand.EXCELLENT,
            QualityBand.EXCEPTIONAL,
            QualityBand.RARE,
            QualityBand.EXTRAORDINARY,
        )
        private val SHIP_CAPACITIES = listOf(
            2_500.0, 4_000.0, 6_000.0, 8_000.0, 12_000.0, 18_000.0, 25_000.0, 35_000.0, 50_000.0, 70_000.0,
            90_000.0, 120_000.0, 160_000.0, 210_000.0, 270_000.0, 350_000.0, 450_000.0, 575_000.0, 725_000.0, 900_000.0,
            1_100_000.0, 1_350_000.0, 1_600_000.0, 1_900_000.0, 2_200_000.0, 2_500_000.0, 2_800_000.0, 3_200_000.0, 3_600_000.0, 4_000_000.0,
        )
    }
}

data class BuyerProcessResult(
    val state: GameState,
    val events: List<CorporateEvent>,
)
