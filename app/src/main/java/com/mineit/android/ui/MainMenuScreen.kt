package com.mineit.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mineit.android.BuildConfig

@Composable
fun MainMenuScreen(
    ready: Boolean,
    canContinue: Boolean,
    statusMessage: String?,
    onContinue: () -> Unit,
    onNewGame: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "MINEIT",
                fontSize = 38.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "Android Migration ${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = .72f),
            )
            Spacer(Modifier.height(36.dp))

            Column(
                modifier = Modifier.widthIn(max = 320.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (canContinue) {
                    Button(
                        onClick = onContinue,
                        enabled = ready,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                    ) {
                        Text("CONTINUE", fontWeight = FontWeight.Bold)
                    }
                }
                OutlinedButton(
                    onClick = onNewGame,
                    enabled = ready,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                ) {
                    Text("NEW GAME", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(22.dp))
            Text(
                text = when {
                    !ready -> "Loading native save…"
                    statusMessage != null -> statusMessage
                    canContinue -> "Continue the saved colony or begin a fresh Contract 01 game."
                    else -> "Start a new Contract 01 game."
                },
                modifier = Modifier.widthIn(max = 360.dp),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = if (statusMessage?.contains("lost", ignoreCase = true) == true) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = .72f)
                },
            )
        }
    }
}
