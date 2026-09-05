package com.mineit.android.ui.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import com.mineit.android.domain.colony.ColonyNetworkSnapshot
import com.mineit.android.domain.colony.CommandSourceType
import com.mineit.android.domain.colony.HeadquartersDepartureGate
import com.mineit.android.domain.colony.InfrastructureRules
import com.mineit.android.domain.colony.SpaceportStatus
import com.mineit.android.domain.config.MineItConfig
import com.mineit.android.domain.model.GameState
import com.mineit.android.domain.model.ResourceId
import com.mineit.android.domain.resources.QualityBand
import com.mineit.android.domain.resources.ResourceCatalogue
import com.mineit.android.domain.resources.ResourceCategory
import com.mineit.android.domain.ships.FleetActionResult
import com.mineit.android.domain.ships.PlayerFleetService
import com.mineit.android.domain.ships.PlayerShipState
import com.mineit.android.domain.world.DevelopmentKind
import com.mineit.android.ui.design.MineItPalette
import com.mineit.android.ui.design.MineItPanel
import com.mineit.android.ui.design.MineItPrimaryButton
import com.mineit.android.ui.design.MineItRadius
import com.mineit.android.ui.design.MineItSecondaryButton
import com.mineit.android.ui.design.MineItSectionHeader
import com.mineit.android.ui.design.MineItSpacing
import com.mineit.android.ui.design.MineItStatusBadge
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerShipControlSheet(
    state: GameState,
    network: ColonyNetworkSnapshot,
    spaceport: SpaceportStatus,
    departureGate: HeadquartersDepartureGate,
    ship: PlayerShipState,
    statusMessage: String?,
    onOpenColonyControl: () -> Unit,
    onPreviewResidentsAshore: (Double) -> FleetActionResult,
    onMoveResidentsAshore: (Double, Boolean) -> Unit,
    onMoveResidentsAboard: (Double) -> Unit,
    onUnload: (ResourceId, Double) -> Unit,
    onLoad: (ResourceId, Double) -> Unit,
    onDismiss: () -> Unit,
) {
    val model = PlayerShipControlPresentation.build(state, network, spaceport, departureGate, ship)
    var pendingAshore by remember(ship.id) { mutableStateOf<Double?>(null) }
    var pendingWarning by remember(ship.id) { mutableStateOf<String?>(null) }

    fun requestAshore(amount: Double) {
        val preview = onPreviewResidentsAshore(amount)
        if (preview.requiresConfirmation) {
            pendingAshore = amount
            pendingWarning = preview.message
        } else {
            onMoveResidentsAshore(amount, false)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MineItPalette.Background,
        contentColor = MineItPalette.Text,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MineItSpacing.Lg, vertical = MineItSpacing.Md),
            verticalArrangement = Arrangement.spacedBy(MineItSpacing.Sm),
        ) {
            ShipHero(model)
            ShipTopMetrics(model.topMetrics)

            statusMessage?.takeIf { it.isNotBlank() }?.let {
                ShipNotice("SHIP SYSTEM", it, if (it.contains("failed", true) || it.contains("full", true) || it.contains("required", true)) ColonyDetailTone.WARNING else ColonyDetailTone.NORMAL)
            }

            ColonySupportModule(model, onOpenColonyControl)

            MineItSectionHeader("SHIP STATUS")
            ShipMetricGrid(model.statusMetrics)

            MineItSectionHeader("ACCOMMODATION")
            ShipMetricGrid(model.accommodationMetrics)
            if (model.shipResidents > 0.0) {
                ShipNotice(
                    "SHIP RESIDENTS",
                    "${shipFormat(model.shipResidents)} colony residents still depend on ship accommodation and consume Food from the ship inventory, not the colony pantry.",
                    if ((model.foodRunwayDays ?: Double.POSITIVE_INFINITY) < 10.0) ColonyDetailTone.CRITICAL else ColonyDetailTone.WARNING,
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(MineItSpacing.Xs)) {
                MineItSecondaryButton(
                    "10 ASHORE",
                    { requestAshore(10.0) },
                    enabled = model.canMoveAshore && model.shipResidents > 0.0,
                    modifier = Modifier.weight(1f),
                )
                MineItSecondaryButton(
                    "MAX ASHORE",
                    { requestAshore(model.moveAshoreMax) },
                    enabled = model.canMoveAshore && model.moveAshoreMax > 0.0,
                    modifier = Modifier.weight(1f),
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(MineItSpacing.Xs)) {
                MineItSecondaryButton(
                    "10 ABOARD",
                    { onMoveResidentsAboard(10.0) },
                    enabled = model.canMoveAboard && model.planetaryResidents > 0.0,
                    modifier = Modifier.weight(1f),
                )
                MineItSecondaryButton(
                    "MAX ABOARD",
                    { onMoveResidentsAboard(model.moveAboardMax) },
                    enabled = model.canMoveAboard && model.moveAboardMax > 0.0,
                    modifier = Modifier.weight(1f),
                )
            }
            if (!spaceport.transfersAllowed) {
                Text("Powered Spaceport services are required for normal resident and cargo loading.", style = MaterialTheme.typography.labelSmall, color = MineItPalette.Warning)
            }

            MineItSectionHeader("CARGO & STORES")
            ShipCapacityStrip(model.capacityMetrics)
            if (model.manifest.isEmpty()) {
                ShipNotice("NO STORED RESOURCES", "No colony or ship inventory is available for transfer.", ColonyDetailTone.NORMAL)
            } else {
                model.manifest.forEach { row ->
                    ShipManifestRow(
                        row = row,
                        onUnload = { amount -> onUnload(row.resourceId, amount) },
                        onLoad = { amount -> onLoad(row.resourceId, amount) },
                    )
                }
            }

            MineItSectionHeader("DEPARTURE READINESS")
            model.departureNotices.forEach { ShipNotice(it.title, it.text, it.tone) }
            Text(
                "Interstellar route selection and launch remain in the later travel/fleet phase. This panel shows the single-colony conditions that will matter when departure is enabled.",
                style = MaterialTheme.typography.labelSmall,
                color = MineItPalette.Muted,
            )

            MineItSecondaryButton("CLOSE", onDismiss, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.padding(bottom = MineItSpacing.Sm))
        }
    }

    if (pendingAshore != null) {
        AlertDialog(
            onDismissRequest = { pendingAshore = null; pendingWarning = null },
            title = { Text("MOVE RESIDENTS ASHORE WITH POWER SHORTAGE?") },
            text = { Text(pendingWarning ?: "The transfer will increase colony life-support demand beyond available generation.") },
            confirmButton = {
                MineItPrimaryButton(
                    "TRANSFER ANYWAY",
                    onClick = {
                        val amount = pendingAshore ?: return@MineItPrimaryButton
                        pendingAshore = null
                        pendingWarning = null
                        onMoveResidentsAshore(amount, true)
                    },
                )
            },
            dismissButton = { MineItSecondaryButton("CANCEL", { pendingAshore = null; pendingWarning = null }) },
        )
    }
}

@Composable
private fun ShipHero(model: PlayerShipControlModel) {
    MineItPanel(raised = true) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MineItSpacing.Md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(model.kicker, style = MaterialTheme.typography.labelSmall, color = MineItPalette.Accent, fontWeight = FontWeight.Bold)
                Text(model.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                Text(model.location, style = MaterialTheme.typography.labelSmall, color = MineItPalette.Muted)
            }
            MineItStatusBadge("DOCKED", MineItPalette.Success)
        }
    }
}

@Composable
private fun ShipTopMetrics(metrics: List<ShipMetric>) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(MineItSpacing.Xs)) {
        metrics.forEach { metric -> ShipMetricCard(metric, Modifier.weight(1f), compact = true) }
    }
}

@Composable
private fun ShipMetricGrid(metrics: List<ShipMetric>) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(MineItSpacing.Xs)) {
        metrics.chunked(2).forEach { pair ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(MineItSpacing.Xs)) {
                pair.forEach { ShipMetricCard(it, Modifier.weight(1f)) }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ShipMetricCard(metric: ShipMetric, modifier: Modifier = Modifier, compact: Boolean = false) {
    Surface(
        modifier = modifier,
        color = MineItPalette.Control,
        border = BorderStroke(1.dp, MineItPalette.Line),
        shape = RoundedCornerShape(MineItRadius.Small),
    ) {
        Column(Modifier.padding(if (compact) 6.dp else MineItSpacing.Sm), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(metric.label, style = MaterialTheme.typography.labelSmall, color = MineItPalette.Muted, maxLines = 1)
            Text(metric.value, style = if (compact) MaterialTheme.typography.labelLarge else MaterialTheme.typography.titleSmall, color = shipToneColor(metric.tone), fontWeight = FontWeight.Black, maxLines = 1)
            if (!compact) metric.detail?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = MineItPalette.Muted) }
        }
    }
}

@Composable
private fun ColonySupportModule(model: PlayerShipControlModel, onOpenColonyControl: () -> Unit) {
    MineItPanel {
        MineItSectionHeader(
            "AUX BAY • COLONY SUPPORT MODULE",
            trailing = if (model.commandLinked) "LINKED" else "OFFLINE",
            color = if (model.commandLinked) MineItPalette.Success else MineItPalette.Muted,
        )
        Text("CSM-01 Colony Support Module", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black)
        Text(
            if (model.commandLinked) {
                "Hard-mounted colony command interface. This ship currently provides emergency colony command and can open the same Colony Control surface used at Headquarters."
            } else {
                "No active command link from this vessel. Colony command is currently provided by Headquarters or is unavailable."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MineItPalette.Muted,
        )
        MineItSecondaryButton(
            if (model.commandLinked) "OPEN COLONY CONTROL" else "COLONY CONTROL UNAVAILABLE",
            onOpenColonyControl,
            enabled = model.commandLinked,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ShipCapacityStrip(metrics: List<ShipMetric>) {
    ShipMetricGrid(metrics)
}

@Composable
private fun ShipManifestRow(row: ShipManifestRowModel, onUnload: (Double) -> Unit, onLoad: (Double) -> Unit) {
    Surface(
        color = MineItPalette.Control,
        border = BorderStroke(1.dp, MineItPalette.Line),
        shape = RoundedCornerShape(MineItRadius.Small),
    ) {
        Column(Modifier.fillMaxWidth().padding(MineItSpacing.Sm), verticalArrangement = Arrangement.spacedBy(MineItSpacing.Xs)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(MineItSpacing.Sm), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(row.name, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black)
                    Text("${row.category.name} • ${row.qualitySummary}", style = MaterialTheme.typography.labelSmall, color = MineItPalette.Muted)
                }
                MineItStatusBadge("S ${shipFormat(row.shipAmount)}", MineItPalette.Accent)
                MineItStatusBadge("C ${shipFormat(row.colonyAmount)}", MineItPalette.Text)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(MineItSpacing.Xs)) {
                MineItSecondaryButton("UNLOAD 10", { onUnload(min(10.0, row.shipAmount)) }, enabled = row.canUnload && row.shipAmount > 0.0, modifier = Modifier.weight(1f))
                MineItSecondaryButton("UNLOAD MAX", { onUnload(row.shipAmount) }, enabled = row.canUnload && row.shipAmount > 0.0, modifier = Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(MineItSpacing.Xs)) {
                MineItSecondaryButton("LOAD 10", { onLoad(min(10.0, row.colonyAmount)) }, enabled = row.canLoad && row.colonyAmount > 0.0 && row.remainingShipCapacity > 0.0, modifier = Modifier.weight(1f))
                MineItSecondaryButton("LOAD MAX", { onLoad(min(row.colonyAmount, row.remainingShipCapacity)) }, enabled = row.canLoad && row.colonyAmount > 0.0 && row.remainingShipCapacity > 0.0, modifier = Modifier.weight(1f))
            }
            if (row.bootstrapUnload && !row.normalTransfersAvailable) {
                Text("Founding bootstrap unload is permitted before command handover even while normal Spaceport transfer services are unavailable.", style = MaterialTheme.typography.labelSmall, color = MineItPalette.Warning)
            }
        }
    }
}

@Composable
private fun ShipNotice(title: String, text: String, tone: ColonyDetailTone) {
    val color = shipToneColor(tone)
    Surface(
        color = color.copy(alpha = .08f),
        border = BorderStroke(1.dp, color.copy(alpha = .4f)),
        shape = RoundedCornerShape(MineItRadius.Small),
    ) {
        Column(Modifier.fillMaxWidth().padding(MineItSpacing.Sm), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = color, fontWeight = FontWeight.Black)
            Text(text, style = MaterialTheme.typography.labelSmall, color = MineItPalette.Muted)
        }
    }
}

data class ShipMetric(
    val label: String,
    val value: String,
    val detail: String? = null,
    val tone: ColonyDetailTone = ColonyDetailTone.NORMAL,
)

data class ShipManifestRowModel(
    val resourceId: ResourceId,
    val name: String,
    val category: ResourceCategory,
    val qualitySummary: String,
    val shipAmount: Double,
    val colonyAmount: Double,
    val remainingShipCapacity: Double,
    val canUnload: Boolean,
    val canLoad: Boolean,
    val bootstrapUnload: Boolean,
    val normalTransfersAvailable: Boolean,
)

data class ShipNoticeModel(val title: String, val text: String, val tone: ColonyDetailTone)

data class PlayerShipControlModel(
    val name: String,
    val kicker: String,
    val location: String,
    val commandLinked: Boolean,
    val topMetrics: List<ShipMetric>,
    val statusMetrics: List<ShipMetric>,
    val accommodationMetrics: List<ShipMetric>,
    val capacityMetrics: List<ShipMetric>,
    val manifest: List<ShipManifestRowModel>,
    val shipResidents: Double,
    val planetaryResidents: Double,
    val moveAshoreMax: Double,
    val moveAboardMax: Double,
    val canMoveAshore: Boolean,
    val canMoveAboard: Boolean,
    val foodRunwayDays: Double?,
    val departureNotices: List<ShipNoticeModel>,
)

/** Pure presentation mapping for the maintained web Ship Control hierarchy within Phase 7 scope. */
object PlayerShipControlPresentation {
    private val fleet = PlayerFleetService()

    fun build(
        state: GameState,
        network: ColonyNetworkSnapshot,
        spaceport: SpaceportStatus,
        departureGate: HeadquartersDepartureGate,
        ship: PlayerShipState,
    ): PlayerShipControlModel {
        val colony = state.activeColony
        val founding = colony.foundingShipId == ship.id
        val shipResidents = fleet.shipResidentCount(colony, ship.id)
        val planetaryResidents = fleet.planetaryResidentCount(state, colony.id)
        val housingCapacity = colony.world.tiles
            .filter { it.development?.kind == DevelopmentKind.HOUSING && it.development.constructionComplete && !it.development.productionStopped }
            .sumOf { InfrastructureRules.capacity(requireNotNull(it.development)) }
        val freeHousing = max(0.0, housingCapacity - colony.planetaryAccommodationResidents)
        val accommodationRoom = max(0.0, ship.accommodationCapacity - shipResidents)
        val food = fleet.foodLoad(ship)
        val foodUse = shipResidents * MineItConfig.FOOD_PER_COLONIST
        val runway = if (foodUse > .0001) food / foodUse else null
        val commandLinked = ship.commandCapable && ship.dockedColonyId == colony.id && network.headquarters.sourceType == CommandSourceType.SHIP
        val general = fleet.generalCargoLoad(ship)
        val fuel = fleet.fuelLoad(ship)
        val total = fleet.totalPhysicalLoad(ship)
        val totalCapacity = fleet.totalPhysicalCapacity(ship)

        val topMetrics = listOf(
            ShipMetric("CARGO", "${shipFormat(general)}/${shipFormat(ship.cargoCapacity)}", tone = capacityTone(general, ship.cargoCapacity)),
            ShipMetric("FUEL", "${shipFormat(fuel)}/${shipFormat(ship.fuelCapacity)}", tone = capacityTone(fuel, ship.fuelCapacity)),
            ShipMetric("FOOD", "${shipFormat(food)}/${shipFormat(ship.foodCapacity)}", tone = foodTone(runway)),
            ShipMetric("CREW", "${ship.crew}/${ship.minimumCrew}", tone = if (ship.crew >= ship.minimumCrew) ColonyDetailTone.GOOD else ColonyDetailTone.CRITICAL),
        )

        val statusMetrics = listOf(
            ShipMetric("TOTAL LOAD", "${shipFormat(total)} / ${shipFormat(totalCapacity)}", "Cargo + Food + Fuel"),
            ShipMetric("GENERAL HOLD", "${shipFormat(general)} / ${shipFormat(ship.cargoCapacity)}", "Build and Ore share this hold", capacityTone(general, ship.cargoCapacity)),
            ShipMetric("CREW RANGE", "${ship.crew} / ${ship.minimumCrew} MIN", "Maximum ${ship.maximumCrew}", if (ship.crew >= ship.minimumCrew) ColonyDetailTone.GOOD else ColonyDetailTone.CRITICAL),
            ShipMetric("PASSENGERS", "${ship.passengerManifest} / ${ship.passengerCapacity}", "Travel manifest; colony residents are tracked separately"),
            ShipMetric("SHIP INDUSTRY", shipFormat(ship.industrySupport), if (ship.industrySupport > 0.0) "Self-powered while docked" else "No colony Industry contribution", if (ship.industrySupport > 0.0) ColonyDetailTone.GOOD else ColonyDetailTone.NORMAL),
            ShipMetric("COMMAND", if (ship.commandCapable) "CAPABLE" else "NONE", if (commandLinked) "Currently managing colony" else "Not current command source", if (commandLinked) ColonyDetailTone.GOOD else ColonyDetailTone.NORMAL),
        )

        val accommodationMetrics = listOf(
            ShipMetric("SHIP RESIDENTS", "${shipFormat(shipResidents)} / ${ship.accommodationCapacity}", "Colony residents assigned aboard", if (shipResidents > 0.0) ColonyDetailTone.WARNING else ColonyDetailTone.GOOD),
            ShipMetric("PLANET RESIDENTS", shipFormat(planetaryResidents), "${shipFormat(colony.planetaryAccommodationResidents)} housed"),
            ShipMetric("PLANET HOUSING", "${shipFormat(colony.planetaryAccommodationResidents)} / ${shipFormat(housingCapacity)}", "${shipFormat(freeHousing)} free"),
            ShipMetric("SHIP FOOD RUNWAY", runway?.let { "${shipFormat(it)} DAYS" } ?: "NO RESIDENT USE", "Food is consumed from ship stores", foodTone(runway)),
        )

        val capacityMetrics = listOf(
            ShipMetric("GENERAL CARGO", "${shipFormat(general)} / ${shipFormat(ship.cargoCapacity)}", "Build + Ore", capacityTone(general, ship.cargoCapacity)),
            ShipMetric("FOOD STORE", "${shipFormat(food)} / ${shipFormat(ship.foodCapacity)}", runway?.let { "${shipFormat(it)} resident-days runway" }, capacityTone(food, ship.foodCapacity)),
            ShipMetric("FUEL TANK", "${shipFormat(fuel)} / ${shipFormat(ship.fuelCapacity)}", null, capacityTone(fuel, ship.fuelCapacity)),
            ShipMetric("TOTAL PHYSICAL", "${shipFormat(total)} / ${shipFormat(totalCapacity)}", "Separate stores; not one interchangeable hold", capacityTone(total, totalCapacity)),
        )

        val resourceIds = (ship.inventory.resources.map { it.resourceId } + colony.inventory.resources.map { it.resourceId }).distinct()
        val bootstrap = founding && !colony.headquarters.commandHandoverComplete
        val manifest = resourceIds.mapNotNull { resourceId ->
            val aboard = ship.inventory.find(resourceId)
            val ashore = colony.inventory.find(resourceId)
            val sample = aboard ?: ashore ?: return@mapNotNull null
            val bands = ((aboard?.qualityBands?.keys ?: emptySet()) + (ashore?.qualityBands?.keys ?: emptySet())).distinct()
            ShipManifestRowModel(
                resourceId = resourceId,
                name = ResourceCatalogue.get(resourceId)?.name ?: resourceId.value,
                category = sample.category,
                qualitySummary = qualitySummary(bands),
                shipAmount = aboard?.amount ?: 0.0,
                colonyAmount = ashore?.amount ?: 0.0,
                remainingShipCapacity = fleet.remainingCapacityForCategory(ship, sample.category),
                canUnload = bootstrap || spaceport.transfersAllowed,
                canLoad = spaceport.transfersAllowed,
                bootstrapUnload = bootstrap,
                normalTransfersAvailable = spaceport.transfersAllowed,
            )
        }.sortedWith(compareBy<ShipManifestRowModel> { it.category.ordinal }.thenBy { it.name })

        val handoverComplete = colony.headquarters.commandHandoverComplete
        val departureNotices = buildList {
            add(
                ShipNoticeModel(
                    "COMMAND HANDOVER • ${if (handoverComplete) "COMPLETE" else if (departureGate.ok) "READY" else "BLOCKED"}",
                    when {
                        handoverComplete -> "The first-departure Headquarters handover has been completed."
                        departureGate.ok -> "Primary Headquarters is constructed and fully staffed; first departure may complete command handover."
                        else -> departureGate.failures.joinToString(" • ")
                    },
                    if (handoverComplete || departureGate.ok) ColonyDetailTone.GOOD else ColonyDetailTone.WARNING,
                ),
            )
            if (shipResidents > 0.0) add(
                ShipNoticeModel(
                    "RESIDENTS STILL ABOARD",
                    "${shipFormat(shipResidents)} colony residents still depend on this ship's accommodation. A later launch workflow must warn before leaving them without ship accommodation.",
                    ColonyDetailTone.WARNING,
                ),
            )
            if (ship.industrySupport > 0.0) add(
                ShipNoticeModel(
                    "COLONY INDUSTRY DEPENDENCY",
                    "This ship currently supplies ${shipFormat(ship.industrySupport)} self-powered Industry while docked. Departure will remove that contribution immediately.",
                    ColonyDetailTone.WARNING,
                ),
            )
            if (ship.crew < ship.minimumCrew) add(
                ShipNoticeModel(
                    "CREW BELOW MINIMUM",
                    "${ship.minimumCrew} crew are required; ${ship.crew} are currently assigned.",
                    ColonyDetailTone.CRITICAL,
                ),
            )
        }

        return PlayerShipControlModel(
            name = ship.name,
            kicker = if (founding) "FOUNDING COLONY SHIP" else "PLAYER SHIP",
            location = "${colony.name} • BASIC SPACEPORT",
            commandLinked = commandLinked,
            topMetrics = topMetrics,
            statusMetrics = statusMetrics,
            accommodationMetrics = accommodationMetrics,
            capacityMetrics = capacityMetrics,
            manifest = manifest,
            shipResidents = shipResidents,
            planetaryResidents = planetaryResidents,
            moveAshoreMax = min(shipResidents, freeHousing),
            moveAboardMax = min(planetaryResidents, accommodationRoom),
            canMoveAshore = spaceport.transfersAllowed && shipResidents > 0.0 && freeHousing > 0.0,
            canMoveAboard = spaceport.transfersAllowed && planetaryResidents > 0.0 && accommodationRoom > 0.0,
            foodRunwayDays = runway,
            departureNotices = departureNotices,
        )
    }

    private fun qualitySummary(bands: List<QualityBand>): String = when {
        bands.isEmpty() -> "NO QUALITY STOCK"
        bands.size == 1 -> qualityName(bands.single())
        else -> bands.sortedBy { it.ordinal }.joinToString(" + ") { qualityName(it) }
    }

    private fun qualityName(band: QualityBand): String = band.name.lowercase().replaceFirstChar { it.titlecase() }

    private fun capacityTone(value: Double, capacity: Double): ColonyDetailTone = when {
        capacity <= .0001 -> ColonyDetailTone.NORMAL
        value >= capacity - .0001 -> ColonyDetailTone.WARNING
        else -> ColonyDetailTone.GOOD
    }

    private fun foodTone(days: Double?): ColonyDetailTone = when {
        days == null -> ColonyDetailTone.NORMAL
        days < 10.0 -> ColonyDetailTone.CRITICAL
        days < 30.0 -> ColonyDetailTone.WARNING
        else -> ColonyDetailTone.GOOD
    }
}

private fun shipToneColor(tone: ColonyDetailTone): Color = when (tone) {
    ColonyDetailTone.NORMAL -> MineItPalette.Text
    ColonyDetailTone.GOOD -> MineItPalette.Success
    ColonyDetailTone.WARNING -> MineItPalette.Warning
    ColonyDetailTone.CRITICAL -> MineItPalette.Critical
}

private fun shipFormat(value: Double): String = if (value % 1.0 == 0.0) floor(value).toLong().toString() else "%.1f".format(value)
