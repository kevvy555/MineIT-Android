package com.mineit.android.ui.game

import com.mineit.android.domain.colony.ColonyNetworkSnapshot
import com.mineit.android.domain.colony.HeadquartersDepartureGate
import com.mineit.android.domain.colony.SpaceportStatus
import com.mineit.android.domain.simulation.ColonyMetrics
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ColonyDetailPresentationTest {
    @Test
    fun `healthy colony keeps compact web-derived information hierarchy`() {
        val model = ColonyDetailPresentation.build(
            population = 120.0,
            metrics = ColonyMetrics(
                powerDemand = 100.0,
                powerCapacity = 140.0,
                powerFuelLimitedGeneration = 120.0,
                powerFuelBurn = 8.0,
                powerFactor = 1.0,
                lifeSupportPowerFactor = 1.0,
                industryPowerFactor = 1.0,
            ),
            network = ColonyNetworkSnapshot.empty().copy(
                workforceAvailable = 80.0,
                workforceRequired = 60.0,
                workforceSurvivalFactor = 1.0,
                workforceCommercialFactor = 1.0,
                industryInstalled = 100.0,
                industryCapacity = 90.0,
                industryLoad = 50.0,
                industrySurvivalFactor = 1.0,
                industryCommercialFactor = 1.0,
                industryStaffFactor = 1.0,
                industryPowerFactor = 1.0,
            ),
            spaceport = spaceport(operational = true, powerFactor = 1.0),
            departureGate = HeadquartersDepartureGate(ok = true, failures = emptyList()),
        )

        assertEquals(
            listOf("POPULATION", "POWER", "WORKFORCE", "INDUSTRY", "COMMAND", "SPACEPORT"),
            model.summary.map { it.label },
        )
        assertEquals(
            listOf("COMMAND", "POWER", "WORKFORCE", "INDUSTRY", "SPACEPORT"),
            model.operations.map { it.title },
        )
        assertEquals(
            listOf("POWER NETWORK", "OPERATIONAL WORKFORCE", "INDUSTRIAL CAPACITY", "COMMAND NETWORK", "SPACEPORT"),
            model.sections.map { it.title },
        )
        assertEquals("COLONY CONTROL", model.kicker)
        assertEquals("NO COMMAND", model.controlStatus)
        assertEquals(listOf("COLONY SERVICES", "UNLINKED"), model.badges)
        assertEquals("20", model.section("OPERATIONAL WORKFORCE").metric("FREE").value)
        assertEquals("AVAILABLE", model.section("POWER NETWORK").status)
        assertEquals("AVAILABLE", model.section("INDUSTRIAL CAPACITY").status)
        assertTrue(model.alerts.isEmpty())
    }

    @Test
    fun `operational shortages and departure gate failures remain prominent`() {
        val base = ColonyNetworkSnapshot.empty()
        val model = ColonyDetailPresentation.build(
            population = 90.0,
            metrics = ColonyMetrics(
                powerDemand = 100.0,
                powerCapacity = 80.0,
                powerFuelLimitedGeneration = 60.0,
                powerFuelBurn = 7.0,
                powerFactor = .6,
                lifeSupportPowerFactor = .9,
                industryPowerFactor = .5,
            ),
            network = base.copy(
                workforceAvailable = 40.0,
                workforceRequired = 80.0,
                workforceSurvivalFactor = .8,
                workforceCommercialFactor = .2,
                industryInstalled = 100.0,
                industryCapacity = 50.0,
                industryLoad = 75.0,
                industrySurvivalFactor = .9,
                industryCommercialFactor = .4,
                industryStaffFactor = .8,
                industryPowerFactor = .5,
                continuity = base.continuity.copy(
                    networkAvailable = false,
                    effectiveCommandEfficiency = .5,
                    reason = "Primary Headquarters offline.",
                ),
            ),
            spaceport = spaceport(operational = false, powerFactor = .2),
            departureGate = HeadquartersDepartureGate(
                ok = false,
                failures = listOf("Primary Headquarters must be fully staffed."),
            ),
        )

        assertEquals("LIMITED", model.section("POWER NETWORK").status)
        assertEquals("SHORTFALL", model.section("OPERATIONAL WORKFORCE").status)
        assertEquals("OVERLOADED", model.section("INDUSTRIAL CAPACITY").status)
        assertEquals("OFFLINE", model.section("COMMAND NETWORK").status)
        assertEquals("OFFLINE", model.section("SPACEPORT").status)
        assertEquals("LINK OFFLINE", model.controlStatus)
        assertEquals(ColonyDetailTone.CRITICAL, model.controlTone)
        assertEquals(ColonyDetailTone.CRITICAL, model.summary.first { it.label == "WORKFORCE" }.tone)
        assertEquals(1, model.alerts.size)
        assertTrue(model.alerts.single().contains("Primary Headquarters must be fully staffed."))
    }

    private fun ColonyDetailModel.section(title: String) = sections.first { it.title == title }
    private fun ColonyDetailSection.metric(label: String) = metrics.first { it.label == label }

    private fun spaceport(operational: Boolean, powerFactor: Double) = SpaceportStatus(
        operational = operational,
        powerFactor = powerFactor,
        berths = 2,
        serviceSlots = 2,
        cargoPerDay = 7,
        passengersPerDay = 25,
        arrivalsAllowed = true,
        emergencyDepartureAllowed = true,
        tradeAllowed = operational,
        loadingAllowed = operational,
        transfersAllowed = operational,
        engineeringAllowed = operational,
        shipMarketAllowed = operational,
        normalDepartureAllowed = operational,
        reason = if (operational) "Spaceport online." else "Spaceport is unpowered.",
    )
}
