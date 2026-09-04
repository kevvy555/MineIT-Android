package com.mineit.android.app

import android.app.Application
import com.mineit.android.BuildConfig
import com.mineit.android.data.save.FileGameStatePersistence
import com.mineit.android.domain.buyers.BuyerService
import com.mineit.android.domain.colony.ColonyDevelopmentService
import com.mineit.android.domain.colony.ColonyNetworkService
import com.mineit.android.domain.colony.HeadquartersService
import com.mineit.android.domain.colony.PopulationSupportService
import com.mineit.android.domain.colony.SpaceportService
import com.mineit.android.domain.commercial.CommercialDayService
import com.mineit.android.domain.contracts.ContractService
import com.mineit.android.domain.events.CorporateEventService
import com.mineit.android.domain.logging.GameLogService
import com.mineit.android.domain.model.ColonyId
import com.mineit.android.domain.model.GameState
import com.mineit.android.domain.model.NewGameFactory
import com.mineit.android.domain.reputation.ReputationService
import com.mineit.android.domain.simulation.DailySimulationEngine
import com.mineit.android.domain.trade.CorporateTradeService
import com.mineit.android.domain.world.SurveyGameService
import java.io.File

/** Android application composition root for the native migration build. */
class AppComposition(application: Application) {
    val newGameFactory = NewGameFactory()
    val surveyGameService = SurveyGameService()
    val headquartersService = HeadquartersService()
    val colonyNetworkService = ColonyNetworkService(headquartersService)
    val colonyDevelopmentService = ColonyDevelopmentService(headquartersService)
    val populationSupportService = PopulationSupportService()
    val spaceportService = SpaceportService()
    val reputationService = ReputationService()
    val corporateTradeService = CorporateTradeService(reputationService)
    val contractService = ContractService(reputationService)
    val buyerService = BuyerService(reputationService)
    val corporateEventService = CorporateEventService()
    val gameLogService = GameLogService()
    val dailySimulationEngine = DailySimulationEngine(
        surveyGameService = surveyGameService,
        networkService = colonyNetworkService,
        headquartersService = headquartersService,
    )
    val commercialDayService = CommercialDayService(
        dailySimulationEngine = dailySimulationEngine,
        tradeService = corporateTradeService,
        buyerService = buyerService,
        contractService = contractService,
        eventService = corporateEventService,
        gameLogService = gameLogService,
    )

    private val initialState = createNewGame()

    val gameSession = GameSession(
        initialState = initialState,
        persistence = FileGameStatePersistence(
            directory = File(application.filesDir, "game-state"),
            gameVersion = BuildConfig.VERSION_NAME,
        ),
    )

    fun createNewGame(): GameState = newGameFactory.contract01(
        colonySeed = VALIDATION_SEED,
        colonyId = ColonyId("intro-$VALIDATION_SEED"),
        colonyName = "Colony 01",
    )

    companion object {
        const val VALIDATION_SEED = 123456789L
    }
}
