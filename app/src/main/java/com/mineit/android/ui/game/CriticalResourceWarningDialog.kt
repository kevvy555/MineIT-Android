package com.mineit.android.ui.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mineit.android.app.CriticalResourceAlert
import com.mineit.android.ui.design.MineItPalette
import com.mineit.android.ui.design.MineItPrimaryButton
import com.mineit.android.ui.design.MineItRadius
import com.mineit.android.ui.design.MineItSpacing
import kotlin.math.floor

@Composable
fun CriticalResourceWarningDialog(
    alert: CriticalResourceAlert,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MineItPalette.Background,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(MineItSpacing.Xs)) {
                Text(
                    "⚠ CRITICAL SURVIVAL RESERVE",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Black,
                    color = MineItPalette.Critical,
                )
                Text(alert.summary, style = MaterialTheme.typography.bodySmall, color = MineItPalette.Text)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(MineItSpacing.Sm)) {
                alert.shipFoodDays?.let { days ->
                    WarningMetric(
                        label = "SHIP FOOD • ${alert.shipName ?: "FOUNDING SHIP"}",
                        value = daysLabel(days),
                        critical = days < 10.0,
                    )
                }
                WarningMetric(
                    label = "FOOD REMAINING",
                    value = daysLabel(alert.foodDays),
                    critical = alert.foodDays?.let { it <= 10.0 } == true,
                )
                WarningMetric(
                    label = "FUEL REMAINING",
                    value = daysLabel(alert.fuelDays),
                    critical = alert.fuelDays?.let { it <= 10.0 } == true,
                )
                Surface(
                    color = MineItPalette.Warning.copy(alpha = .10f),
                    border = BorderStroke(1.dp, MineItPalette.Warning.copy(alpha = .35f)),
                    shape = RoundedCornerShape(MineItRadius.Small),
                ) {
                    Text(
                        "Ship and colony inventories are separate. Transfer supply or reduce consumption immediately.",
                        modifier = Modifier.padding(MineItSpacing.Sm),
                        style = MaterialTheme.typography.labelSmall,
                        color = MineItPalette.Warning,
                    )
                }
            }
        },
        confirmButton = {
            MineItPrimaryButton(text = "ACKNOWLEDGE", onClick = onDismiss)
        },
    )
}

@Composable
private fun WarningMetric(label: String, value: String, critical: Boolean) {
    val accent: Color = if (critical) MineItPalette.Critical else MineItPalette.Success
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MineItPalette.Control,
        border = BorderStroke(1.dp, MineItPalette.Line),
        shape = RoundedCornerShape(MineItRadius.Small),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(MineItSpacing.Sm),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MineItPalette.Muted)
            Text(value, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = accent)
        }
    }
}

private fun daysLabel(days: Double?): String = when {
    days == null -> "SAFE"
    days <= 0.0 -> "0 DAYS"
    else -> "${floor(days).toInt()} DAYS"
}
