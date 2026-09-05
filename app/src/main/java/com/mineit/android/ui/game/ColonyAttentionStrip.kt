package com.mineit.android.ui.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mineit.android.app.ColonyAttention
import com.mineit.android.app.ColonyAttentionSeverity
import com.mineit.android.ui.design.MineItPalette
import com.mineit.android.ui.design.MineItRadius
import com.mineit.android.ui.design.MineItSpacing

@Composable
fun ColonyAttentionStrip(
    attention: ColonyAttention,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = attentionColor(attention.severity)
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        color = accent.copy(alpha = .12f),
        shape = RoundedCornerShape(MineItRadius.Small),
        border = BorderStroke(1.dp, accent.copy(alpha = .42f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = MineItSpacing.Sm, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MineItSpacing.Sm),
        ) {
            Text("◆", color = accent, style = MaterialTheme.typography.labelLarge)
            Column(Modifier.weight(1f)) {
                Text(
                    attention.title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MineItPalette.Text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    attention.detail,
                    style = MaterialTheme.typography.labelSmall,
                    color = MineItPalette.Muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                attention.actionLabel,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Black,
                color = accent,
                maxLines = 1,
            )
        }
    }
}

private fun attentionColor(severity: ColonyAttentionSeverity): Color = when (severity) {
    ColonyAttentionSeverity.GOOD -> MineItPalette.Success
    ColonyAttentionSeverity.WARNING -> MineItPalette.Warning
    ColonyAttentionSeverity.CRITICAL -> MineItPalette.Critical
}
