package com.mineit.android.domain.contracts

import com.mineit.android.domain.model.AbsoluteDay
import com.mineit.android.domain.model.ColonyStatus
import com.mineit.android.domain.model.GameDate
import com.mineit.android.domain.model.GameState
import com.mineit.android.domain.model.NewGameFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContractServiceTest {
    private val service = ContractService()
    private val factory = NewGameFactory()

    private fun settled(seed: Long = 123L): GameState = factory.settleLandingSite(factory.contract01(seed), 0)

    private fun withColony(state: GameState, transform: (com.mineit.android.domain.model.ColonyState) -> com.mineit.android.domain.model.ColonyState): GameState {
        val updated = transform(state.activeColony)
        return state.copy(colonies = state.colonies.map { if (it.id == updated.id) updated else it })
    }

    @Test
    fun scoreUsesCurrentGoalsProfitAndMedalThresholds() {
        var state = settled()
        state = withColony(state) { colony ->
            colony.copy(
                population = 1_575.0,
                contract = colony.contract!!.copy(localRevenue = 1_100_000.0, localCosts = 0.0),
            )
        }
        val gold = service.score(state, foodMetric = 180.0, industryMetric = 780.0)!!
        assertTrue(gold.passed)
        assertEquals(ContractMedal.GOLD, gold.medal)

        state = withColony(state) { colony ->
            colony.copy(contract = colony.contract!!.copy(localRevenue = 2_300_000.0))
        }
        val platinum = service.score(state, foodMetric = 180.0, industryMetric = 780.0)!!
        assertEquals(ContractMedal.PLATINUM, platinum.medal)

        val failed = service.score(state, foodMetric = 119.0, industryMetric = 780.0)!!
        assertFalse(failed.passed)
        assertEquals(ContractMedal.NONE, failed.medal)
    }

    @Test
    fun missedDeadlineOffersExtensionThenFailsAfterThirdExtension() {
        var state = settled().copy(
            date = GameDate.fromAbsoluteDay(AbsoluteDay(3_601)),
            company = settled().company.copy(cash = 100_000.0),
        )
        assertEquals(ContractDeadlineState.EXTENSION, service.deadlineState(state, 0.0, 0.0))

        val extended = service.extend(state)
        assertTrue(extended.ok)
        assertEquals(25_000.0, extended.cost, .0001)
        assertEquals(1, extended.state.activeColony.contract!!.extensionsUsed)
        assertEquals(1, extended.state.activeColony.contract!!.extensionYears)
        assertEquals(75_000.0, extended.state.company.cash, .0001)

        state = withColony(state) { colony ->
            colony.copy(contract = colony.contract!!.copy(extensionsUsed = 3, extensionYears = 3))
        }.copy(date = GameDate.fromAbsoluteDay(AbsoluteDay(4_681)))
        assertEquals(ContractDeadlineState.FAILED, service.deadlineState(state, 0.0, 0.0))
    }

    @Test
    fun soleColonyUnableToFundMandatoryExtensionFailsCorporation() {
        var state = settled().copy(
            date = GameDate.fromAbsoluteDay(AbsoluteDay(3_601)),
            company = settled().company.copy(cash = 0.0),
        )
        assertEquals(ContractDeadlineState.CORPORATION_FAILED, service.deadlineState(state, 0.0, 0.0))

        val failed = service.failCorporation(state, "Unable to fund mandatory extension.")
        assertTrue(failed.ok)
        assertTrue(failed.state.company.gameOver)
        assertEquals(ColonyStatus.CONTRACT_FAILED, failed.state.activeColony.status)
        assertTrue(failed.state.activeColony.contract!!.ended)
        assertTrue(failed.state.activeColony.contract!!.failedByContract)
    }

    @Test
    fun completionAwardsWinAndReputationOnceThenRenewalAddsFiveYears() {
        var state = settled()
        state = withColony(state) { colony ->
            colony.copy(
                population = 1_100.0,
                contract = colony.contract!!.copy(completed = true),
                status = ColonyStatus.HOLDOVER,
            )
        }.copy(company = state.company.copy(cash = 500_000.0))

        val award = service.awardCompletion(state)
        assertTrue(award.ok)
        assertEquals(1, award.state.company.wins)
        assertEquals(.10, award.state.company.reputation, .0001)
        val repeated = service.awardCompletion(award.state)
        assertEquals(1, repeated.state.company.wins)
        assertEquals(.10, repeated.state.company.reputation, .0001)

        val renewed = service.renew(repeated.state)
        assertTrue(renewed.ok)
        assertEquals(ColonyStatus.PLAYING, renewed.state.activeColony.status)
        assertEquals(1, renewed.state.activeColony.contract!!.renewals)
        assertEquals(5, renewed.state.activeColony.contract!!.extensionYears)
        assertEquals(400_000.0, renewed.state.company.cash, .0001)
    }

    @Test
    fun completedColonyCanEndAsLiability() {
        var state = settled()
        state = withColony(state) { colony -> colony.copy(contract = colony.contract!!.copy(completed = true), status = ColonyStatus.HOLDOVER) }
        val result = service.endAsLiability(state)
        assertTrue(result.ok)
        assertEquals(ColonyStatus.LIABILITY, result.state.activeColony.status)
        assertTrue(result.state.activeColony.contract!!.ended)
    }
}
