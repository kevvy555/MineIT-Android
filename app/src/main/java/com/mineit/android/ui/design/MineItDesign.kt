package com.mineit.android.ui.design

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mineit.android.domain.resources.ResourceCategory

/** Production MineIT design tokens, aligned with the current web map-first visual language. */
object MineItPalette {
    val Background = Color(0xFF050708)
    val Header = Color(0xFF081015)
    val Panel = Color(0xFF0D1318)
    val RaisedPanel = Color(0xFF131C23)
    val Control = Color(0xFF0B1116)
    val Line = Color(0xFF25333D)
    val Text = Color(0xFFEEF5F7)
    val Muted = Color(0xFF91A3AD)
    val Accent = Color(0xFF76C6FF)
    val Survey = Color(0xFF62B8FF)
    val Food = Color(0xFF6BD986)
    val Build = Color(0xFF8EC5D9)
    val Fuel = Color(0xFFFF9F5F)
    val Ore = Color(0xFFC7A0FF)
    val Success = Color(0xFF8FDCA3)
    val Warning = Color(0xFFFFD166)
    val Critical = Color(0xFFFF7777)
    val Selection = Color(0xFF76C6FF)
    val MultiSelection = Color(0xFFFFD166)
    val Disabled = Color(0xFF59656C)

    fun resource(category: ResourceCategory): Color = when (category) {
        ResourceCategory.FOOD -> Food
        ResourceCategory.BUILD -> Build
        ResourceCategory.FUEL -> Fuel
        ResourceCategory.ORE -> Ore
    }
}

object MineItSpacing {
    val Xs = 3.dp
    val Sm = 5.dp
    val Md = 8.dp
    val Lg = 12.dp
    val Xl = 16.dp
}

object MineItRadius {
    val Small = 6.dp
    val Medium = 8.dp
    val Large = 12.dp
}

object MineItTouch {
    val Minimum = 44.dp
}
