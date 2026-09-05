package com.mineit.android.ui

import com.mineit.android.app.DevelopmentDetail
import com.mineit.android.app.DevelopmentDetailPolicy
import com.mineit.android.domain.world.SectorCoordinate

private val developmentDetailPolicy = DevelopmentDetailPolicy()

/** ViewModel-facing adapter for derived adaptive-building details. */
fun GameViewModel.developmentDetail(coordinate: SectorCoordinate): DevelopmentDetail? =
    developmentDetailPolicy.detail(
        state = state.value,
        coordinate = coordinate,
        network = network.value,
        metrics = metrics.value,
    )
