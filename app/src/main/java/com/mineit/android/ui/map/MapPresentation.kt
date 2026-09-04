package com.mineit.android.ui.map

import com.mineit.android.domain.colony.ColonyNetworkSnapshot
import com.mineit.android.domain.resources.ResourceCategory
import com.mineit.android.domain.world.DevelopmentKind
import com.mineit.android.domain.world.SectorCoordinate
import com.mineit.android.domain.world.WorldTile

enum class MapFocus(val label: String) {
    ALL("ALL"),
    PROBLEMS("PROBLEMS"),
    BUILDINGS("BUILDINGS"),
    FOOD("FOOD"),
    BUILD("BUILD"),
    FUEL("FUEL"),
    ORE("ORE"),
}

enum class MapStateFilter(val label: String) {
    UNSURVEYED("UNSURVEYED"),
    SURVEYED("SURVEYED"),
    DEVELOPED("DEVELOPED"),
    QUEUED("QUEUED"),
}

object MapPresentation {
    fun matches(
        tile: WorldTile,
        focus: MapFocus,
        stateFilters: Set<MapStateFilter>,
        queued: Set<SectorCoordinate>,
        active: Set<SectorCoordinate>,
        network: ColonyNetworkSnapshot,
    ): Boolean {
        val focusMatches = when (focus) {
            MapFocus.ALL -> true
            MapFocus.PROBLEMS -> isProblem(tile, network)
            MapFocus.BUILDINGS -> tile.development != null || tile.coordinate == SectorCoordinate(0, 0)
            MapFocus.FOOD -> tile.deposit?.category == ResourceCategory.FOOD
            MapFocus.BUILD -> tile.deposit?.category == ResourceCategory.BUILD
            MapFocus.FUEL -> tile.deposit?.category == ResourceCategory.FUEL
            MapFocus.ORE -> tile.deposit?.category == ResourceCategory.ORE
        }
        if (!focusMatches) return false
        if (stateFilters.isEmpty()) return true
        val states = buildSet {
            if (!tile.revealed) add(MapStateFilter.UNSURVEYED)
            if (tile.revealed) add(MapStateFilter.SURVEYED)
            if (tile.development != null || tile.coordinate == SectorCoordinate(0, 0)) add(MapStateFilter.DEVELOPED)
            if (tile.coordinate in queued || tile.coordinate in active) add(MapStateFilter.QUEUED)
        }
        return states.any(stateFilters::contains)
    }

    fun isProblem(tile: WorldTile, network: ColonyNetworkSnapshot): Boolean {
        if (tile.resourceExhausted || tile.deposit?.renewableWiped == true) return true
        val development = tile.development ?: return false
        if (development.productionStopped || !development.constructionComplete) return true
        if (development.kind == DevelopmentKind.EXTRACT) {
            return (network.sitePowerFactors[siteId(tile.coordinate)] ?: 1.0) < .999
        }
        if (development.kind == DevelopmentKind.HEADQUARTERS) {
            return tile.coordinate !in network.poweredHeadquarters
        }
        return false
    }

    fun siteId(coordinate: SectorCoordinate): String = "site:${coordinate.x},${coordinate.y}"
}
