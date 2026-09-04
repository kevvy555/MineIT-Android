package com.mineit.android.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class GameDate(
    val year: Int,
    val day: Int,
) {
    init {
        require(year >= 1) { "Game year must be at least 1." }
        require(day in 1..DAYS_PER_YEAR) { "Game day must be between 1 and $DAYS_PER_YEAR." }
    }

    fun toAbsoluteDay(): AbsoluteDay = AbsoluteDay((year - 1) * DAYS_PER_YEAR + day)

    companion object {
        /** Canonical MineIT calendar from the web behavioural baseline. */
        const val DAYS_PER_YEAR = 360

        fun fromAbsoluteDay(absoluteDay: AbsoluteDay): GameDate {
            val zeroBased = absoluteDay.value - 1
            return GameDate(
                year = zeroBased / DAYS_PER_YEAR + 1,
                day = zeroBased % DAYS_PER_YEAR + 1,
            )
        }
    }
}
