package com.mineit.android.domain.commercial

import com.mineit.android.domain.buyers.BuyerProcessResult
import com.mineit.android.domain.buyers.BuyerService
import com.mineit.android.domain.colony.ExtractionOverdriveAccident
import com.mineit.android.domain.colony.ExtractionOverdriveDayService
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
import com.mineit.android.domain.world.ExtractionAccidentOutcome

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
    private val overdriveDayService: ExtractionOverdriveDayService = ExtractionOverdriveDayService(),
) {
    fun recalculate(state: GameState): ColonyMetrics = dailySimulationEngine.recalculate(state)

    fun advanceDay(state: GameState, spaceportServicesAvailable: Boolean = true): CommercialDayResult {
        val baseSimulation = dailySimulationEngine.advanceDay(state)
        if (baseSimulation.colonyDied) return CommercialDayResult(baseSimulation.state, baseSimulation.metrics, baseSimulation, true)

        val overdrive = overdriveDayService.advanceDay(state, baseSimulation.state)
        val adjustedMetrics = baseSimulation.metrics.copy(
            lastDeaths = baseSimulation.metrics.lastDeaths + overdrive.deaths,
            planetaryResidents = overdrive.planetaryResidents,
        )
        val simulation = baseSimulation.copy(
            state = overdrive.state,
            metrics = adjustedMetrics,
            deaths = baseSimulation.deaths + overdrive.deaths,
            colonyDied = overdrive.colonyDied,
        )
        if (simulation.colonyDied) {
            return CommercialDayResult(
                state = simulation.state,
                metrics = adjustedMetrics,
                simulation = simulation,
                shouldPause = true,
                extractionAccidents = overdrive.accidents,
            )
        }

        var working = simulation.state
        overdrive.accidents.forEach { accident ->
            val record = accident.record
            val message = if (record.outcome == ExtractionAccidentOutcome.FATALITIES) {
                "${record.name} at sector ${accident.coordinate.x},${accident.coordinate.y}: ${record.deaths} fatalities; facility closed for ${record.shutdownDays} days."
            } else {
                "${record.name} at sector ${accident.coordinate.x},${accident.coordinate.y}: machinery damage; facility closed for ${record.shutdownDays} days."
            }
            working = gameLogService.event(working, "extraction-accident", message).state
        }

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

        val finalSimulation = simulation.copy(state = working)
        return CommercialDayResult(
            state = working,
            metrics = adjustedMetrics,
            simulation = finalSimulation,
            shouldPause = working.corporateEvents.pending.isNotEmpty(),
            extractionAccidents = overdrive.accidents,
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
    val extractionAccidents: List<ExtractionOverdriveAccident> = emptyList(),
)
