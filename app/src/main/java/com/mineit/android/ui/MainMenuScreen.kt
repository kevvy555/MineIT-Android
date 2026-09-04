package com.mineit.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mineit.android.BuildConfig
import com.mineit.android.ui.design.MineItPalette
import com.mineit.android.ui.design.MineItPanel
import com.mineit.android.ui.design.MineItPrimaryButton
import com.mineit.android.ui.design.MineItSecondaryButton
import com.mineit.android.ui.design.MineItSpacing

@Composable
fun MainMenuScreen(
    ready: Boolean,
    canContinue: Boolean,
    statusMessage: String?,
    onContinue: () -> Unit,
    onNewGame: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize(), color = MineItPalette.Background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MineItPalette.Background)
                .padding(horizontal = 24.dp, vertical = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "MINEIT",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = MineItPalette.Text,
            )
            Text(
                text = "KOPLIN CONTRACT MINING",
                style = MaterialTheme.typography.labelMedium,
                color = MineItPalette.Accent,
            )
            Text(
                text = BuildConfig.VERSION_NAME,
                style = MaterialTheme.typography.labelSmall,
                color = MineItPalette.Muted,
            )

            Spacer(Modifier.height(28.dp))

            MineItPanel(
                modifier = Modifier.widthIn(max = 340.dp).fillMaxWidth(),
                raised = true,
            ) {
                Text("CONTRACT OPERATIONS", style = MaterialTheme.typography.titleSmall, color = MineItPalette.Accent)
                Text(
                    "Establish a colony, survey the terrain and build a sustainable mining operation.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MineItPalette.Muted,
                )
                if (canContinue) {
                    MineItPrimaryButton(
                        text = "CONTINUE",
                        onClick = onContinue,
                        enabled = ready,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                MineItSecondaryButton(
                    text = "NEW GAME",
                    onClick = onNewGame,
                    enabled = ready,
                    modifier = Modifier.fillMaxWidth(),
                    accent = MineItPalette.Warning,
                )
            }

            Spacer(Modifier.height(MineItSpacing.Lg))
            Text(
                text = when {
                    !ready -> "Loading native save…"
                    statusMessage != null -> statusMessage
                    canContinue -> "A saved colony is ready to continue."
                    else -> "Start a new Contract 01 operation."
                },
                modifier = Modifier.widthIn(max = 360.dp),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = if (statusMessage?.contains("lost", ignoreCase = true) == true || statusMessage?.contains("failed", ignoreCase = true) == true) {
                    MineItPalette.Critical
                } else {
                    MineItPalette.Muted
                },
            )
        }
    }
}
