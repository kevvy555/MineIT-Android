package com.mineit.android.domain.colony

import com.mineit.android.domain.model.GameState
import com.mineit.android.domain.resources.ExtractionCompatibility
import com.mineit.android.domain.resources.ExtractionFamily
import com.mineit.android.domain.resources.ResourceCategory
import com.mineit.android.domain.world.DeterministicHash
import com.mineit.android.domain.world.ExtractionAccidentOutcome
import com.mineit.android.domain.world.ExtractionAccidentRecord
import com.mineit.android.domain.world.ExtractionOperatingMode
import com.mineit.android.domain.world.Sustainability
import com.mineit.android.domain.world.WorldTile
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max

/** Canonical immutable port of the maintained web extraction-overdrive rules. */
object ExtractionOverdriveRules {
    const val RISK_PERIOD = 30.0
    const val ACCIDENT_CHANCE = .25
    const val ACCIDENT_SHUTDOWN_DAYS = 3

    data class Profile(
        val mode: ExtractionOperatingMode,
        val label: String,
        val workforceMultiplier: Double,
        val outputMultiplier: Double,
        val exposureDelta: Double,
        val riskLabel: String,
    )

    val profiles = listOf(
        Profile(ExtractionOperatingMode.NORMAL, "NORMAL", 1.0, 1.0, -1.0, "NONE"),
        Profile(ExtractionOperatingMode.PUSHED, "PUSHED", 1.25, 1.15, .3, "LOW"),
        Profile(ExtractionOperatingMode.HARD, "HARD", 1.50, 1.30, 1.0, "HIGH"),
    )

    fun supports(tile: WorldTile): Boolean {
        val deposit = tile.deposit ?: return false
        if (tile.development?.kind != com.mineit.android.domain.world.DevelopmentKind.EXTRACT) return false
        if (deposit.sustainability == Sustainability.RENEWABLE || deposit.category == ResourceCategory.FOOD) return false
        return ExtractionCompatibility.familyFor(deposit.resourceId) in accidentFamilies.keys
    }

    fun mode(tile: WorldTile): ExtractionOperatingMode =
        if (supports(tile)) tile.development?.operatingMode ?: ExtractionOperatingMode.NORMAL else ExtractionOperatingMode.NORMAL

    fun profile(tile: WorldTile, override: ExtractionOperatingMode? = null): Profile {
        val selected = if (override != null && supports(tile)) override else mode(tile)
        return profiles.first { it.mode == selected }
    }

    fun outputMultiplier(tile: WorldTile): Double = profile(tile).outputMultiplier
    fun workforceMultiplier(tile: WorldTile, override: ExtractionOperatingMode? = null): Double = profile(tile, override).workforceMultiplier
    fun riskExposure(tile: WorldTile): Double = max(0.0, tile.development?.overdriveExposure ?: 0.0)
    fun isShutdown(tile: WorldTile): Boolean = max(0, tile.development?.accidentShutdownDays ?: 0) > 0

    fun setMode(tile: WorldTile, requested: ExtractionOperatingMode): OverdriveModeResult {
        if (!supports(tile)) return OverdriveModeResult(false, tile, "This site does not use industrial overdrive controls.")
        if (isShutdown(tile)) {
            val days = max(0, tile.development?.accidentShutdownDays ?: 0)
            return OverdriveModeResult(false, tile, "Facility is closed for $days more day${if (days == 1) "" else "s"}.")
        }
        val dev = requireNotNull(tile.development)
        val next = tile.copy(development = dev.copy(operatingMode = requested, overdriveExposure = riskExposure(tile)))
        return OverdriveModeResult(true, next, "${profile(next).label} operating mode selected.")
    }

    fun advanceRisk(
        state: GameState,
        tile: WorldTile,
        random: (() -> Double)? = null,
    ): OverdriveAdvanceResult {
        if (!supports(tile) || isShutdown(tile)) return OverdriveAdvanceResult(tile)
        val dev = requireNotNull(tile.development)
        val selected = mode(tile)
        val profile = profile(tile)
        val before = riskExposure(tile)
        if (selected == ExtractionOperatingMode.NORMAL) {
            return OverdriveAdvanceResult(
                tile.copy(development = dev.copy(overdriveExposure = max(0.0, before + profile.exposureDelta))),
            )
        }

        var exposure = before + profile.exposureDelta
        if (exposure < RISK_PERIOD) {
            return OverdriveAdvanceResult(tile.copy(development = dev.copy(overdriveExposure = exposure)) )
        }
        exposure = max(0.0, exposure - RISK_PERIOD)
        val riskCheck = max(0, dev.overdriveRiskChecks) + 1
        var rollIndex = 0
        fun roll(): Double = if (random != null) {
            (random.invoke().takeIf { it.isFinite() } ?: 0.0).coerceIn(0.0, .999999)
        } else {
            deterministicUnit(state, tile, "$riskCheck:${rollIndex++}")
        }

        var nextDev = dev.copy(overdriveExposure = exposure, overdriveRiskChecks = riskCheck)
        if (roll() >= ACCIDENT_CHANCE) return OverdriveAdvanceResult(tile.copy(development = nextDev))

        val family = ExtractionCompatibility.familyFor(requireNotNull(tile.deposit).resourceId)
        val details = requireNotNull(accidentFamilies[family])
        val fatal = roll() < .25
        val rawDeaths = if (fatal) 1 + floor(roll() * details.fatalMax).toInt() else 0
        val event = ExtractionAccidentRecord(
            name = details.name,
            family = familyKey(family),
            outcome = if (fatal) ExtractionAccidentOutcome.FATALITIES else ExtractionAccidentOutcome.MACHINERY,
            deaths = rawDeaths,
            shutdownDays = ACCIDENT_SHUTDOWN_DAYS,
            mode = selected,
            year = state.date.year,
            day = state.date.day,
        )
        nextDev = nextDev.copy(
            operatingMode = ExtractionOperatingMode.NORMAL,
            overdriveExposure = 0.0,
            accidentShutdownDays = ACCIDENT_SHUTDOWN_DAYS,
            productionStopped = true,
            lastAccident = event,
        )
        return OverdriveAdvanceResult(tile.copy(development = nextDev), event)
    }

    fun advanceShutdownDay(tile: WorldTile): WorldTile {
        if (!isShutdown(tile)) return tile
        val dev = requireNotNull(tile.development)
        val days = max(0, ceil(dev.accidentShutdownDays.toDouble()).toInt() - 1)
        return tile.copy(development = dev.copy(accidentShutdownDays = days, productionStopped = days > 0))
    }

    private fun deterministicUnit(state: GameState, tile: WorldTile, salt: String): Double {
        val seed = state.activeColony.seed
        val hash = DeterministicHash.hashString("$seed|${tile.coordinate.x}|${tile.coordinate.y}|${state.date.year}|${state.date.day}|$salt")
        return (hash % 1_000_000u).toDouble() / 1_000_000.0
    }

    private fun familyKey(family: ExtractionFamily): String = when (family) {
        ExtractionFamily.QUARRY -> "quarry"
        ExtractionFamily.MINE -> "mine"
        ExtractionFamily.DEEP_MINE -> "deep-mine"
        ExtractionFamily.RIG -> "rig"
        ExtractionFamily.FARM -> "farm"
        ExtractionFamily.RANCH -> "ranch"
        ExtractionFamily.BIO -> "bio-harvester"
        ExtractionFamily.ALGAE -> "algae-facility"
    }

    private data class AccidentFamily(val name: String, val fatalMax: Int)

    private val accidentFamilies = mapOf(
        ExtractionFamily.QUARRY to AccidentFamily("Quarry Face Collapse", 3),
        ExtractionFamily.MINE to AccidentFamily("Tunnel Collapse", 3),
        ExtractionFamily.DEEP_MINE to AccidentFamily("Rockburst & Shaft Collapse", 4),
        ExtractionFamily.RIG to AccidentFamily("Over-pressure Blowout", 3),
    )
}

data class OverdriveModeResult(val ok: Boolean, val tile: WorldTile, val message: String)
data class OverdriveAdvanceResult(val tile: WorldTile, val accident: ExtractionAccidentRecord? = null)
