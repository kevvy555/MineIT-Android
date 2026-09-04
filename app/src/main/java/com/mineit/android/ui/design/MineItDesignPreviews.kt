package com.mineit.android.ui.design

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mineit.android.ui.theme.MineItTheme

@Preview(name = "MineIT component language", widthDp = 390, heightDp = 260, showBackground = true)
@Composable
private fun MineItComponentsPreview() {
    MineItTheme {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MineItSectionHeader("COLONY OPERATIONS", "Y1 D1")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                MineItResourceCard("FOOD", "1.3k", "90d", MineItPalette.Food, Modifier.weight(1f))
                MineItResourceCard("BUILD", "520", "+0/d", MineItPalette.Build, Modifier.weight(1f))
                MineItResourceCard("FUEL", "420", "18d", MineItPalette.Fuel, Modifier.weight(1f))
                MineItResourceCard("ORE", "260", "stable", MineItPalette.Ore, Modifier.weight(1f))
            }
            MineItPanel(raised = true) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    MineItStat("POWER", "75/48", Modifier.weight(1f), MineItPalette.Success)
                    MineItStat("LIFE", "100%", Modifier.weight(1f), MineItPalette.Success)
                    MineItStat("COMMAND", "6/16", Modifier.weight(1f), MineItPalette.Accent)
                }
                MineItActionRow {
                    MineItPrimaryButton("SURVEY", {}, Modifier.weight(1f))
                    MineItSecondaryButton("BUILD", {}, Modifier.weight(1f))
                }
            }
        }
    }
}
