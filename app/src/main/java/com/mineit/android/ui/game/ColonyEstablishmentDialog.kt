package com.mineit.android.ui.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mineit.android.domain.colony.ColonyEstablishmentAssessment
import com.mineit.android.domain.colony.EstablishmentStep
import com.mineit.android.domain.colony.EstablishmentSupportStatus
import com.mineit.android.domain.resources.ResourceCategory
import com.mineit.android.domain.ships.FleetActionResult
import com.mineit.android.ui.design.MineItActionRow
import com.mineit.android.ui.design.MineItPalette
import com.mineit.android.ui.design.MineItPrimaryButton
import com.mineit.android.ui.design.MineItRadius
import com.mineit.android.ui.design.MineItSecondaryButton
import com.mineit.android.ui.design.MineItSpacing

@Composable
fun ColonyEstablishmentDialog(
    colonyName: String,
    assessment: ColonyEstablishmentAssessment,
    statusMessage: String?,
    transferPreview: (Double) -> FleetActionResult,
    onUnloadCategory: (ResourceCategory) -> Unit,
    onMoveResidentsAshore: (Double, Boolean) -> Unit,
    onBeginOperations: () -> Unit,
    onDismiss: () -> Unit,
) {
    var showTransfers by remember { mutableStateOf(false) }
    var localMessage by remember { mutableStateOf<String?>(null) }
    var confirmation by remember { mutableStateOf<Pair<Double, String>?>(null) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            color = MineItPalette.Background,
            shape = RoundedCornerShape(MineItRadius.Medium),
            border = BorderStroke(1.dp, MineItPalette.Line),
            modifier = Modifier.fillMaxSize().padding(10.dp),
        ) {
            Column(
                Modifier.fillMaxSize().padding(MineItSpacing.Md).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(MineItSpacing.Sm),
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("FOUNDING HANDOVER", style = MaterialTheme.typography.labelSmall, color = MineItPalette.Accent)
                        Text("ESTABLISH $colonyName", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                    }
                    StatusPill(assessment.phase.name, if (assessment.acknowledged) MineItPalette.Warning else MineItPalette.Accent)
                }
                Text(
                    "The colony begins aboard its founding ship. Move each system ashore deliberately; ship and colony supplies are separate.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MineItPalette.Muted,
                )

                Text("SUPPLIES • SHIP / COLONY", style = MaterialTheme.typography.labelMedium, color = MineItPalette.Muted)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(MineItSpacing.Xs)) {
                    ResourceCategory.entries.forEach { category ->
                        val split = assessment.resourceSplit.getValue(category)
                        EstablishmentResource(
                            label = category.name,
                            ship = split.ship,
                            colony = split.colony,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                Text("ESTABLISHMENT CHECKLIST", style = MaterialTheme.typography.labelMedium, color = MineItPalette.Muted)
                ChecklistRow("1", "DEPLOY BUILD + FUEL", "Bootstrap unload is allowed before Spaceport Power.", assessment.support.getValue(EstablishmentStep.SUPPLIES))
                ChecklistRow("2", "SURVEY LAND", "Resources remain hidden until sectors are surveyed.", assessment.support.getValue(EstablishmentStep.SURVEY))
                ChecklistRow("3", "POWER + HOUSING", "Build planetary Power and real Housing before moving residents ashore.", assessment.support.getValue(EstablishmentStep.HOUSING))
                ChecklistRow("4", "MOVE RESIDENTS ASHORE", "Manual transfer; residents aboard consume ship Food.", assessment.support.getValue(EstablishmentStep.RESIDENTS))
                ChecklistRow("5", "ESTABLISH FOOD", "Colony Food production supports planetary residents only.", assessment.support.getValue(EstablishmentStep.FOOD))
                ChecklistRow("6", "ESTABLISH FUEL", "Planetary Power consumes colony Fuel.", assessment.support.getValue(EstablishmentStep.FUEL))
                ChecklistRow("7", "REPLACE SHIP INDUSTRY", "Docked founding ship contributes 50 self-powered Industry.", assessment.support.getValue(EstablishmentStep.INDUSTRY))
                ChecklistRow("8", "ESTABLISH HEADQUARTERS", "A fully staffed Primary Headquarters completes command handover readiness.", assessment.support.getValue(EstablishmentStep.HEADQUARTERS))

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(MineItSpacing.Xs)) {
                    SummaryCard("SHIP RESIDENTS", format(assessment.shipResidents), Modifier.weight(1f))
                    SummaryCard("PLANET RESIDENTS", format(assessment.planetaryResidents), Modifier.weight(1f))
                    SummaryCard("SHIP FOOD", assessment.shipFoodDaysRemaining?.let { "${format(assessment.shipFoodAvailable)} • ${if (it > 999) "999+" else format(it)}d" } ?: format(assessment.shipFoodAvailable), Modifier.weight(1f))
                }

                MineItSecondaryButton(
                    text = if (showTransfers) "HIDE SHIP TRANSFERS" else "OPEN SHIP TRANSFERS",
                    onClick = { showTransfers = !showTransfers; localMessage = null },
                    modifier = Modifier.fillMaxWidth(),
                    selected = showTransfers,
                )

                if (showTransfers) {
                    TransferControls(
                        assessment = assessment,
                        onUnload = { category -> localMessage = null; onUnloadCategory(category) },
                        onMove = { amount ->
                            val preview = transferPreview(amount)
                            when {
                                preview.requiresConfirmation -> confirmation = amount to preview.message
                                preview.ok -> { localMessage = null; onMoveResidentsAshore(amount, false) }
                                else -> localMessage = preview.message
                            }
                        },
                    )
                }

                (localMessage ?: statusMessage)?.let { message ->
                    Text(message, style = MaterialTheme.typography.bodySmall, color = if (message.contains("warning", true) || message.contains("required", true)) MineItPalette.Warning else MineItPalette.Muted)
                }

                MineItActionRow {
                    MineItSecondaryButton("CLOSE", onDismiss, modifier = Modifier.weight(1f))
                    MineItPrimaryButton(
                        text = "BEGIN OPERATIONS • 1×",
                        onClick = onBeginOperations,
                        enabled = !assessment.acknowledged,
                        modifier = Modifier.weight(1.6f),
                    )
                }
            }
        }
    }

    confirmation?.let { (amount, reason) ->
        AlertDialog(
            onDismissRequest = { confirmation = null },
            title = { Text("Power shortage warning") },
            text = { Text(reason) },
            confirmButton = {
                MineItPrimaryButton(
                    text = "TRANSFER ANYWAY",
                    onClick = { confirmation = null; onMoveResidentsAshore(amount, true) },
                )
            },
            dismissButton = { MineItSecondaryButton("CANCEL", { confirmation = null }) },
        )
    }
}

@Composable
private fun TransferControls(
    assessment: ColonyEstablishmentAssessment,
    onUnload: (ResourceCategory) -> Unit,
    onMove: (Double) -> Unit,
) {
    Surface(
        color = MineItPalette.Panel,
        shape = RoundedCornerShape(MineItRadius.Small),
        border = BorderStroke(1.dp, MineItPalette.Line),
    ) {
        Column(Modifier.fillMaxWidth().padding(MineItSpacing.Sm), verticalArrangement = Arrangement.spacedBy(MineItSpacing.Xs)) {
            Text("FOUNDING SHIP → COLONY", style = MaterialTheme.typography.labelMedium, color = MineItPalette.Accent)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(MineItSpacing.Xs)) {
                ResourceCategory.entries.forEach { category ->
                    MineItSecondaryButton(
                        text = "UNLOAD ${category.name}",
                        onClick = { onUnload(category) },
                        enabled = assessment.resourceSplit.getValue(category).ship > .0001,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Text(
                "Residents: ${format(assessment.shipResidents)} aboard • ${format(assessment.planetaryAccommodationResidents)}/${format(assessment.housingCapacity)} planetary accommodation",
                style = MaterialTheme.typography.bodySmall,
                color = MineItPalette.Muted,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(MineItSpacing.Xs)) {
                MineItSecondaryButton("MOVE 10 ASHORE", { onMove(10.0) }, enabled = assessment.shipResidents > .0001, modifier = Modifier.weight(1f))
                MineItSecondaryButton("MOVE MAX ASHORE", { onMove(assessment.shipResidents) }, enabled = assessment.shipResidents > .0001, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ChecklistRow(number: String, title: String, detail: String, status: EstablishmentSupportStatus) {
    Surface(
        color = MineItPalette.Control,
        shape = RoundedCornerShape(MineItRadius.Small),
        border = BorderStroke(1.dp, MineItPalette.Line),
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 7.dp, vertical = 5.dp), horizontalArrangement = Arrangement.spacedBy(MineItSpacing.Sm), verticalAlignment = Alignment.CenterVertically) {
            Text(number, style = MaterialTheme.typography.labelLarge, color = MineItPalette.Accent)
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.labelMedium, color = MineItPalette.Text, fontWeight = FontWeight.Bold)
                Text(detail, style = MaterialTheme.typography.labelSmall, color = MineItPalette.Muted)
            }
            StatusPill(status.name, statusColor(status))
        }
    }
}

@Composable
private fun EstablishmentResource(label: String, ship: Double, colony: Double, modifier: Modifier = Modifier) {
    Surface(color = MineItPalette.Control, shape = RoundedCornerShape(MineItRadius.Small), border = BorderStroke(1.dp, MineItPalette.Line), modifier = modifier) {
        Column(Modifier.padding(5.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MineItPalette.Muted)
            Text("S ${format(ship)}", style = MaterialTheme.typography.labelMedium, color = MineItPalette.Text)
            Text("C ${format(colony)}", style = MaterialTheme.typography.labelMedium, color = MineItPalette.Accent)
        }
    }
}

@Composable
private fun SummaryCard(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(color = MineItPalette.Control, shape = RoundedCornerShape(MineItRadius.Small), border = BorderStroke(1.dp, MineItPalette.Line), modifier = modifier) {
        Column(Modifier.padding(5.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MineItPalette.Muted)
            Text(value, style = MaterialTheme.typography.labelMedium, color = MineItPalette.Text)
        }
    }
}

@Composable
private fun StatusPill(text: String, color: Color) {
    Surface(color = color.copy(alpha = .12f), shape = RoundedCornerShape(999.dp), border = BorderStroke(1.dp, color.copy(alpha = .5f))) {
        Text(text, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.Bold)
    }
}

private fun statusColor(status: EstablishmentSupportStatus): Color = when (status) {
    EstablishmentSupportStatus.READY -> MineItPalette.Success
    EstablishmentSupportStatus.HYBRID -> MineItPalette.Warning
    EstablishmentSupportStatus.SHIP -> MineItPalette.Accent
    EstablishmentSupportStatus.COLONY -> MineItPalette.Muted
}
