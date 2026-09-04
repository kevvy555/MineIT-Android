package com.mineit.android.domain.commercial

import com.mineit.android.domain.buyers.BuyerProcessResult
import com.mineit.android.domain.buyers.BuyerService
import com.mineit.android.domain.contracts.ContractDeadlineState
import com.mineit.android.domain.contracts.ContractService
import com.mineit.android.domain.events.CorporateEvent
import com.mineit.android.domain.events.CorporateEventService
import com.mineit.android.domain.events.CorporateEventType
import com.mineit.android.domain.logging.GameLogService
import com.mineit.android.domain.model.GameState
import com.mineit.android.domain.simulation.ColonyMetrics
import com.mineit.android.domain.simulation.DailySimulationEngine
import com.mineit.android.domain.simulation.DailySimulationResult
import com.mineit.android.domain.trade.CorporateTradeService

/**
 * Cross-domain day coordinator. It preserves the pure daily engine and applies commercial events
 * after the canonical day transition, matching the web application's orchestration responsibility
 * without recreating app.js inside the Activity/ViewModel.
 */
class CommercialDayService(
    private val dailySimulationEngine: DailySimulationEngine = DailySimulationEngine(),
    private val tradeService: CorporateTradeService = CorporateTradeService(),
    private val buyerService: BuyerService = BuyerService(),
    private val contractService: ContractService = ContractService(),
    private val eventService: CorporateEventService = CorporateEventService(),
    private val gameLogService: GameLogService = GameLogService(),
) {
    fun recalculate(state: GameState): ColonyMetrics = dailySimulationEngine.recalculate(state)

    fun advanceDay(state: GameState, spaceportServicesAvailable: Boolean = true): CommercialDayResult {
        val simulation = dailySimulationEngine.advanceDay(state)
        if (simulation.colonyDied) return CommercialDayResult(simulation.state, simulation.metrics, simulation, true)
        var working = simulation.state

        if (tradeService.shouldArrive(working)) {
            val arrived = tradeService.arrive(working)
            if (arrived.ok) {
                working = eventService.queueCorporateShip(arrived.state)
                working = gameLogService.event(working, "corporate-ship-arrival", "Corporate trade ship arrived at ${working.activeColony.name}.").state
            }
        }

        val buyers = buyerService.processDay(working, berthAvailable = spaceportServicesAvailable)
        working = buyers.state
        buyers.events.forEach { event -> working = eventService.enqueue(working, event) }

        val deadline = contractService.deadlineState(
            state = working,
            foodMetric = simulation.metrics.foodProduction,
            industryMetric = simulation.metrics.industry,
        )
        if (deadline != null) {
            val colony = working.activeColony
            val kind = deadline.name.lowercase().replace('_', '-')
            val pending = colony.contract?.copy(
                pendingDecision = kind,
                pendingDecisionPreviousStatus = colony.status.name.lowercase(),
            )
            working = working.copy(colonies = working.colonies.map { if (it.id == colony.id) colony.copy(contract = pending) else it })
            working = eventService.enqueue(
                working,
                CorporateEvent(
                    type = CorporateEventType.CONTRACT,
                    colonyId = colony.id,
                    colonyName = colony.name,
                    kind = kind,
                ),
            )
        }

        return CommercialDayResult(
            state = working,
            metrics = simulation.metrics,
            simulation = simulation.copy(state = working),
            shouldPause = working.corporateEvents.pending.isNotEmpty(),
        )
    }

    /** Reconstruct blocking presentation events after process death/load from durable domain state. */
    fun recoverBlockingEvents(state: GameState, metrics: ColonyMetrics, spaceportServicesAvailable: Boolean = true): GameState {
        var working = state
        if (working.activeColony.trade.active) working = eventService.queueCorporateShip(working, recovered = true)
        val buyers: BuyerProcessResult = buyerService.recoverEvents(working, berthAvailable = spaceportServicesAvailable)
        working = buyers.state
        buyers.events.forEach { event -> working = eventService.enqueue(working, event.copy(recovered = true)) }
        val pendingKind = working.activeColony.contract?.pendingDecision
        if (!pendingKind.isNullOrBlank()) {
            working = eventService.enqueue(
                working,
                CorporateEvent(
                    type = CorporateEventType.CONTRACT,
                    colonyId = working.activeColonyId,
                    colonyName = working.activeColony.name,
                    kind = pendingKind,
                    recovered = true,
                ),
            )
        } else {
            val deadline = contractService.deadlineState(working, metrics.foodProduction, metrics.industry)
            if (deadline != null) {
                working = eventService.enqueue(
                    working,
                    CorporateEvent(
                        type = CorporateEventType.CONTRACT,
                        colonyId = working.activeColonyId,
                        colonyName = working.activeColony.name,
                        kind = deadline.name.lowercase().replace('_', '-'),
                        recovered = true,
                    ),
                )
            }
        }
        return working
    }
}

data class CommercialDayResult(
    val state: GameState,
    val metrics: ColonyMetrics,
    val simulation: DailySimulationResult,
    val shouldPause: Boolean,
)
