package com.mineit.android.ui.game

import com.mineit.android.domain.colony.ExtractionOverdriveRules

/** Presentation vocabulary only; canonical values remain owned by ExtractionOverdriveRules.Profile. */
internal val ExtractionOverdriveRules.Profile.risk: String
    get() = riskLabel

internal val ExtractionOverdriveRules.Profile.output: Double
    get() = outputMultiplier

internal val ExtractionOverdriveRules.Profile.workforce: Double
    get() = workforceMultiplier
