package com.mineit.android.ui.commercial

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.mineit.android.domain.contracts.ContractMedal
import com.mineit.android.domain.contracts.ContractScore
import com.mineit.android.domain.model.GameState
import com.mineit.android.domain.simulation.ColonyMetrics
import com.mineit.android.ui.CommercialPanel
import com.mineit.android.ui.design.MineItDestructiveButton
import com.mineit.android.ui.design.MineItPalette
import com.mineit.android.ui.design.MineItPanel
import com.mineit.android.ui.design.MineItPrimaryButton
import com.mineit.android.ui.design.MineItSecondaryButton
import com.mineit.android.ui.design.MineItSectionHeader
import com.mineit.android.ui.design.MineItSpacing
import com.mineit.android.ui.design.MineItStat
import com.mineit.android.ui.design.MineItStatusBadge
import com.mineit.android.ui.game.format
import com.mineit.android.ui.game.formatMoney

/**
 * Web-derived Contract presentation. It deliberately keeps contract lifecycle rules in
 * ContractService and only restores the recognisable information hierarchy in Compose.
 */
@Composable
fun ContractCommercialPanelScreen(
    state: GameState,
    metrics: ColonyMetrics,
    score: ContractScore?,
    onSelectPanel: (CommercialPanel) -> Unit,
    onClose: () -> Unit,
    onRenewContract: () -> Unit,
    onEndLiability: () -> Unit,
) {
    val colony = state.activeColony
    val contract = colony.contract

    Surface(modifier = Modifier.fillMaxSize(), color = MineItPalette.Background) {
        Column(
            modifier = Modifier.fillMaxSize().padding(MineItSpacing.Sm),
            verticalArrangement = Arrangement.spacedBy(MineItSpacing.Sm),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MineItSpacing.Sm),
            ) {
                Text(
                    "COMMERCIAL OPERATIONS",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    color = MineItPalette.Accent,
                    fontWeight = FontWeight.Bold,
                )
                MineItSecondaryButton("CLOSE", onClose)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(MineItSpacing.Xs)) {
                CommercialPanel.entries.forEach { panel ->
                    MineItSecondaryButton(
                        text = panel.name,
                        onClick = { onSelectPanel(panel) },
                        selected = panel == CommercialPanel.CONTRACT,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(MineItSpacing.Sm),
            ) {
                if (contract == null) {
                    item {
                        MineItPanel {
                            MineItSectionHeader("NO ACTIVE CONTRACT")
                            Text("This colony has no contract record.", color = MineItPalette.Muted)
                        }
                    }
                    return@LazyColumn
                }

                item {
                    MineItPanel {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(MineItSpacing.Sm),
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "${contract.colonyName} • T${contract.colonyTier}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MineItPalette.Accent,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(contract.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            }
                            MineItStatusBadge(
                                colony.status.name.replace('_', ' '),
                                when {
                                    colony.status.name == "PLAYING" -> MineItPalette.Success
                                    colony.status.name == "HOLDOVER" -> MineItPalette.Warning
                                    colony.status.name == "LIABILITY" -> MineItPalette.Critical
                                    else -> MineItPalette.Muted
                                },
                            )
                        }
                        Text(contract.environment, style = MaterialTheme.typography.bodySmall)
                        Text(contract.hazard, style = MaterialTheme.typography.bodySmall, color = MineItPalette.Muted)
                        Text(
                            "${contract.supportSystem} • environmental support ×${"%.2f".format(contract.supportLoad)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MineItPalette.Muted,
                        )
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(MineItSpacing.Xs)) {
                            MineItStat("TERM", "${contract.years + contract.extensionYears}Y", Modifier.weight(1f))
                            MineItStat("EXTENSIONS", "${contract.extensionsUsed}/3", Modifier.weight(1f))
                            MineItStat("RENEWALS", contract.renewals.toString(), Modifier.weight(1f))
                        }
                    }
                }

                item {
                    MineItPanel {
                        MineItSectionHeader(
                            "CONTRACT OBJECTIVES",
                            score?.medal?.displayName() ?: "UNSCORED",
                        )
                        MetricPair(
                            leftLabel = "FOOD PRODUCTION",
                            leftValue = "${format(metrics.foodProduction)} / ${format(contract.goals.food.toDouble())}",
                            leftGood = (score?.foodRatio ?: 0.0) >= 1.0,
                            rightLabel = "INDUSTRY CAPABILITY",
                            rightValue = "${format(metrics.industry)} / ${format(contract.goals.industry.toDouble())}",
                            rightGood = (score?.industryRatio ?: 0.0) >= 1.0,
                        )
                        MetricPair(
                            leftLabel = "POPULATION",
                            leftValue = "${format(colony.population)} / ${format(contract.goals.population.toDouble())}",
                            leftGood = (score?.populationRatio ?: 0.0) >= 1.0,
                            rightLabel = "CONTRACT PROFIT",
                            rightValue = "£${formatMoney(score?.profit ?: (contract.localRevenue - contract.localCosts))}",
                            rightGood = score?.passed == true,
                        )
                        MetricPair(
                            leftLabel = "LOCAL REVENUE",
                            leftValue = "£${formatMoney(contract.localRevenue)}",
                            leftGood = true,
                            rightLabel = "COLONY COSTS",
                            rightValue = "£${formatMoney(contract.localCosts)}",
                            rightGood = true,
                        )
                        Text(
                            if (score?.passed == true) "All Bronze objectives are currently satisfied."
                            else "Bronze requires Food, Industry and Population objectives to all reach target.",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (score?.passed == true) MineItPalette.Success else MineItPalette.Warning,
                        )
                    }
                }

                item {
                    MineItPanel {
                        MineItSectionHeader("PERFORMANCE BANDS")
                        PerformanceBandRow("BRONZE", "Pass all objectives", score?.passed == true)
                        PerformanceBandRow(
                            "SILVER",
                            "£${formatMoney(contract.bands.silver.toDouble())} profit",
                            score?.medal?.atLeast(ContractMedal.SILVER) == true,
                        )
                        PerformanceBandRow(
                            "GOLD",
                            "£${formatMoney(contract.bands.gold.toDouble())} + 120% objectives",
                            score?.medal?.atLeast(ContractMedal.GOLD) == true,
                        )
                        PerformanceBandRow(
                            "PLATINUM",
                            "£${formatMoney(contract.bands.platinum.toDouble())} + 150% objectives",
                            score?.medal == ContractMedal.PLATINUM,
                        )
                    }
                }

                if (contract.pendingDecision != null) {
                    item {
                        MineItPanel {
                            MineItSectionHeader("CONTRACT ACTION REQUIRED", contract.pendingDecision.replace('-', ' ').uppercase())
                            Text(
                                "The pending contract decision is handled through the blocking corporate event so it survives save/load and cannot be bypassed by closing this screen.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MineItPalette.Warning,
                            )
                        }
                    }
                }

                if (contract.completed && !contract.ended) {
                    item {
                        MineItPanel {
                            MineItSectionHeader("COLONY REMAINS ACTIVE", "HOLDOVER")
                            Text(
                                "The initial charter is complete. Renew it for another 5 years, keep operating on holdover, or end the charter and retain the colony as a corporate liability.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MineItPalette.Muted,
                            )
                            MineItPrimaryButton("RENEW 5 YEARS", onRenewContract, Modifier.fillMaxWidth())
                            Text(
                                "Closing this panel leaves the colony on holdover until you choose another contract action.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MineItPalette.Muted,
                            )
                            MineItDestructiveButton(
                                "END — RETAIN AS LIABILITY",
                                onEndLiability,
                                Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }

                if (contract.ended) {
                    item {
                        MineItPanel {
                            MineItSectionHeader("CONTRACT ENDED", colony.status.name.replace('_', ' '))
                            Text(
                                contract.failureReason ?: "This mining charter is no longer active.",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (contract.failedByContract) MineItPalette.Critical else MineItPalette.Muted,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricPair(
    leftLabel: String,
    leftValue: String,
    leftGood: Boolean,
    rightLabel: String,
    rightValue: String,
    rightGood: Boolean,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(MineItSpacing.Xs)) {
        MineItStat(
            leftLabel,
            leftValue,
            Modifier.weight(1f),
            if (leftGood) MineItPalette.Success else MineItPalette.Warning,
        )
        MineItStat(
            rightLabel,
            rightValue,
            Modifier.weight(1f),
            if (rightGood) MineItPalette.Success else MineItPalette.Warning,
        )
    }
}

@Composable
private fun PerformanceBandRow(label: String, requirement: String, achieved: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MineItSpacing.Sm),
    ) {
        MineItStatusBadge(
            label,
            if (achieved) MineItPalette.Success else MineItPalette.Muted,
        )
        Text(
            requirement,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
            color = if (achieved) MineItPalette.Text else MineItPalette.Muted,
        )
    }
}

private fun ContractMedal.displayName(): String = when (this) {
    ContractMedal.NONE -> "NO MEDAL"
    else -> name
}

private fun ContractMedal.atLeast(target: ContractMedal): Boolean = ordinal >= target.ordinal
