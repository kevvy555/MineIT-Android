package com.mineit.android.domain.technology

/** Current scanning capability values from the pinned MineIT web technology tree. */
data class ScanningCapability(
    val level: Int,
    val surveySlots: Int,
    val scanTimeFactor: Double,
    val hintTier: Int,
)

object ScanningTechnology {
    private val capabilities = listOf(
        ScanningCapability(level = 1, surveySlots = 1, scanTimeFactor = 1.000, hintTier = 0),
        ScanningCapability(level = 2, surveySlots = 1, scanTimeFactor = 0.975, hintTier = 0),
        ScanningCapability(level = 3, surveySlots = 2, scanTimeFactor = 0.950, hintTier = 0),
        ScanningCapability(level = 4, surveySlots = 2, scanTimeFactor = 0.925, hintTier = 1),
        ScanningCapability(level = 5, surveySlots = 3, scanTimeFactor = 0.900, hintTier = 1),
        ScanningCapability(level = 6, surveySlots = 3, scanTimeFactor = 0.875, hintTier = 1),
        ScanningCapability(level = 7, surveySlots = 4, scanTimeFactor = 0.850, hintTier = 2),
        ScanningCapability(level = 8, surveySlots = 4, scanTimeFactor = 0.825, hintTier = 2),
        ScanningCapability(level = 9, surveySlots = 5, scanTimeFactor = 0.800, hintTier = 2),
        ScanningCapability(level = 10, surveySlots = 5, scanTimeFactor = 0.775, hintTier = 3),
    )

    fun forLevel(level: Int): ScanningCapability = capabilities[(level.coerceIn(1, capabilities.size)) - 1]
}
