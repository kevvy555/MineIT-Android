package com.mineit.android.app

import com.mineit.android.domain.colony.ColonyEstablishmentAssessment
import com.mineit.android.domain.colony.EstablishmentPhase
import com.mineit.android.domain.colony.EstablishmentResourceSplit
import com.mineit.android.domain.resources.ResourceCategory
import com.mineit.android.domain.simulation.ColonyMetrics
import com.mineit.android.testing.TestGameStates
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ColonyAttentionPolicyTest {
    private val policy = ColonyAttentionPolicy()

    @Test
    fun `housing warning counts only planetary residents under N05`() {
        val state = TestGameStates.foundationState(population = 120.0)
        val shipSupported = assessment(housing = 100.0, shipResidents = 120.0, planetaryResidents = 0.0)

        val stable = policy.current(
            state = state,
            metrics = ColonyMetrics(planetaryResidents = 0.0, shipResidents = 120.0),
            establishment = shipSupported,
        )

        assertEquals(ColonyAttentionTarget.PROBLEMS, stable.target)
        assertEquals(ColonyAttentionSeverity.GOOD, stable.severity)

        val nearCapacity = policy.current(
            state = state,
            metrics = ColonyMetrics(planetaryResidents = 91.0, shipResidents = 29.0),
            establishment = assessment(housing = 100.0, shipResidents = 29.0, planetaryResidents = 91.0),
        )

        assertEquals(ColonyAttentionTarget.HOUSING, nearCapacity.target)
        assertEquals("HOUSING NEAR CAPACITY", nearCapacity.title)
        assertTrue(nearCapacity.detail.startsWith("9 spaces remain"))
    }

    @Test
    fun `attention priority keeps ship food ahead of colony food`() {
        val state = TestGameStates.foundationState()
        val attention = policy.current(
            state = state,
            metrics = ColonyMetrics(foodDays = 5.0, foodSupply = .4, shipResidents = 120.0, shipFoodShortestDays = 8.0),
            establishment = assessment(housing = 0.0, shipResidents = 120.0, planetaryResidents = 0.0, shipFoodDays = 8.0),
        )

        assertEquals(ColonyAttentionTarget.PLAYER_SHIP, attention.target)
        assertEquals(ColonyAttentionSeverity.CRITICAL, attention.severity)
        assertTrue(attention.title.startsWith("SHIP FOOD CRITICAL"))
    }

    @Test
    fun `critical warnings fire once per episode and reset after recovery`() {
        val tracker = CriticalResourceEpisodeTracker()
        val critical = CriticalResourceSnapshot(foodDays = 10.0, fuelDays = 20.0, shipFoodDays = 9.9, shipName = "Prospector")

        val first = tracker.next(critical)
        assertTrue(first?.summary?.contains("FOOD") == true)
        assertTrue(first?.summary?.contains("SHIP FOOD") == true)
        assertNull(tracker.next(critical))

        assertNull(tracker.next(CriticalResourceSnapshot(foodDays = 11.0, fuelDays = 20.0, shipFoodDays = 10.0, shipName = "Prospector")))
        val reentered = tracker.next(critical)
        assertTrue(reentered?.summary?.contains("FOOD") == true)
        assertTrue(reentered?.summary?.contains("SHIP FOOD") == true)
    }

    @Test
    fun `ship food threshold is strictly below ten days while colony threshold includes ten`() {
        val tracker = CriticalResourceEpisodeTracker()
        val alert = tracker.next(CriticalResourceSnapshot(foodDays = 10.0, fuelDays = 10.0, shipFoodDays = 10.0, shipName = "Prospector"))

        assertTrue(alert?.summary?.contains("FOOD") == true)
        assertTrue(alert?.summary?.contains("FUEL") == true)
        assertTrue(alert?.summary?.contains("SHIP FOOD") == false)
    }

    private fun assessment(
        housing: Double,
        shipResidents: Double,
        planetaryResidents: Double,
        shipFoodDays: Double? = null,
    ) = ColonyEstablishmentAssessment(
        required = false,
        acknowledged = true,
        phase = EstablishmentPhase.INDEPENDENT,
        foundingShipId = null,
        foundingShipName = "Prospector",
        shipResidents = shipResidents,
        planetaryResidents = planetaryResidents,
        planetaryAccommodationResidents = planetaryResidents,
        shipAccommodationCapacity = 290,
        shipCrew = 10,
        shipMinimumCrew = 10,
        shipFoodAvailable = 0.0,
        shipFoodDaysRemaining = shipFoodDays,
        housingCapacity = housing,
        resourceSplit = ResourceCategory.entries.associateWith { EstablishmentResourceSplit(0.0, 0.0) },
        support = emptyMap(),
    )
}
