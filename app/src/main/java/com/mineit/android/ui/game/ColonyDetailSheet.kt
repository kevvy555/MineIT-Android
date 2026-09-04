package com.mineit.android.ui.game

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.mineit.android.domain.colony.ColonyNetworkSnapshot
import com.mineit.android.domain.colony.HeadquartersDepartureGate
import com.mineit.android.domain.colony.SpaceportStatus
import com.mineit.android.domain.model.GameState
import com.mineit.android.domain.simulation.ColonyMetrics
import com.mineit.android.ui.design.MineItPalette
import com.mineit.android.ui.design.MineItPanel
import com.mineit.android.ui.design.MineItSpacing
import com.mineit.android.ui.design.MineItStatusBadge

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColonyDetailSheet(
    state: GameState,
    metrics: ColonyMetrics,
    network: ColonyNetworkSnapshot,
    spaceport: SpaceportStatus,
    departureGate: HeadquartersDepartureGate,
    onDismiss: () -> Unit,
) {
    val colony = state.activeColony
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MineItPalette.Background,
        contentColor = MineItPalette.Text,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = MineItSpacing.Lg, vertical = MineItSpacing.Md),
            verticalArrangement = Arrangement.spacedBy(MineItSpacing.Md),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text(colony.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${colony.contract?.name ?: "Contract 01"} • Population ${format(colony.population)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MineItPalette.Muted,
                    )
                }
                MineItStatusBadge(
                    text = colony.status.name.replace('_', ' '),
                    color = if (colony.status.name == "PLAYING") MineItPalette.Success else MineItPalette.Warning,
                )
            }

            DetailSection("POWER") {
                DetailRow("Installed generation", format(metrics.powerCapacity))
                DetailRow("Fuel-limited generation", format(metrics.powerFuelLimitedGeneration), powerColor(metrics.powerFactor))
                DetailRow("Total demand", format(metrics.powerDemand))
                DetailRow("Life support delivered", percent(metrics.lifeSupportPowerFactor), powerColor(metrics.lifeSupportPowerFactor))
                DetailRow("Industry delivered", percent(metrics.industryPowerFactor), powerColor(metrics.industryPowerFactor))
                DetailRow("Generator Fuel burn", "${format(metrics.powerFuelBurn)}/day")
            }

            DetailSection("WORKFORCE & INDUSTRY") {
                DetailRow("Workforce", "${format(network.workforceAvailable)} / ${format(network.workforceRequired)}")
                DetailRow("Survival workforce", percent(network.workforceSurvivalFactor), factorColor(network.workforceSurvivalFactor))
                DetailRow("Commercial workforce", percent(network.workforceCommercialFactor), factorColor(network.workforceCommercialFactor))
                DetailRow("Industry installed", format(network.industryInstalled))
                DetailRow("Industry available", format(network.industryCapacity))
                DetailRow("Industry load", format(network.industryLoad))
                DetailRow("Industry staffing", percent(network.industryStaffFactor), factorColor(network.industryStaffFactor))
            }

            DetailSection("HEADQUARTERS") {
                DetailRow("Command source", network.headquarters.sourceType?.name ?: "NONE")
                DetailRow("Command load", "${format(network.headquarters.load)} / ${format(network.headquarters.capacity)}")
                DetailRow("Command efficiency", percent(network.continuity.effectiveCommandEfficiency), factorColor(network.continuity.effectiveCommandEfficiency))
                DetailRow("Network", if (network.continuity.networkAvailable) "ONLINE" else "OFFLINE", if (network.continuity.networkAvailable) MineItPalette.Success else MineItPalette.Critical)
                DetailRow("Continuity", network.continuity.phase.name.replace('_', ' '))
                DetailRow("Founding-ship handover", if (departureGate.ok) "READY" else "BLOCKED", if (departureGate.ok) MineItPalette.Success else MineItPalette.Warning)
                if (!departureGate.ok) {
                    Text(
                        departureGate.failures.joinToString(" • "),
                        style = MaterialTheme.typography.labelSmall,
                        color = MineItPalette.Warning,
                    )
                }
            }

            DetailSection("SPACEPORT") {
                DetailRow("Status", if (spaceport.operational) "ONLINE" else "OFFLINE", if (spaceport.operational) MineItPalette.Success else MineItPalette.Critical)
                DetailRow("Power", percent(spaceport.powerFactor), powerColor(spaceport.powerFactor))
                DetailRow("Berths / service slots", "${spaceport.berths} / ${spaceport.serviceSlots}")
                DetailRow("Cargo / passengers per day", "${spaceport.cargoPerDay} / ${spaceport.passengersPerDay}")
                Text(spaceport.reason, style = MaterialTheme.typography.labelSmall, color = MineItPalette.Muted)
            }
        }
    }
}

@Composable
private fun DetailSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    MineItPanel(raised = true) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = MineItPalette.Accent)
        HorizontalDivider(color = MineItPalette.Line)
        content()
    }
}

@Composable
private fun DetailRow(label: String, value: String, valueColor: Color = MineItPalette.Text) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MineItPalette.Muted, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodySmall, color = valueColor)
    }
}

private fun factorColor(value: Double): Color = when {
    value >= .999 -> MineItPalette.Success
    value >= .7 -> MineItPalette.Warning
    else -> MineItPalette.Critical
}

private fun powerColor(value: Double): Color = factorColor(value)
