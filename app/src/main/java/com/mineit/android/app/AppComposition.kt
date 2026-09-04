package com.mineit.android.app

import android.app.Application
import com.mineit.android.BuildConfig
import com.mineit.android.data.save.FileGameStatePersistence
import com.mineit.android.domain.colony.ColonyDevelopmentService
import com.mineit.android.domain.colony.ColonyNetworkService
import com.mineit.android.domain.colony.HeadquartersService
import com.mineit.android.domain.colony.SpaceportService
import com.mineit.android.domain.model.ColonyId
import com.mineit.android.domain.model.NewGameFactory
import com.mineit.android.domain.simulation.DailySimulationEngine
import com.mineit.android.domain.world.SurveyGameService
import java.io.File

/** Android application composition root for the native migration build. */
class AppComposition(application: Application) {
    val newGameFactory = NewGameFactory()
    val surveyGameService = SurveyGameService()
    val headquartersService = HeadquartersService()
    val colonyNetworkService = ColonyNetworkService(headquartersService)
    val colonyDevelopmentService = ColonyDevelopmentService(headquartersService)
    val spaceportService = SpaceportService()
    val dailySimulationEngine = DailySimulationEngine(
        surveyGameService = surveyGameService,
        networkService = colonyNetworkService,
        headquartersService = headquartersService,
    )

    private val initialState = newGameFactory.contract01(
        colonySeed = VALIDATION_SEED,
        colonyId = ColonyId("intro-$VALIDATION_SEED"),
        colonyName = "Colony 01",
    )

    val gameSession = GameSession(
        initialState = initialState,
        persistence = FileGameStatePersistence(
            directory = File(application.filesDir, "game-state"),
            gameVersion = BuildConfig.VERSION_NAME,
        ),
    )

    companion object {
        const val VALIDATION_SEED = 123456789L
    }
}
