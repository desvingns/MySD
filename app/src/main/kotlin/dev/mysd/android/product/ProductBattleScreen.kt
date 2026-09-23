package dev.mysd.android.product

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import dev.mysd.android.R
import dev.mysd.android.ui.theme.ProductColors
import dev.mysd.android.ui.theme.ProductMetrics

@Composable
internal fun ProductBattleScreen(
    state: ProductSurfaceUi.Battle,
    onAction: (ProductUiAction) -> Unit,
    modifier: Modifier = Modifier,
    arenaLabel: String? = null,
    reduceMotion: Boolean = false,
) {
    val scene = state.scene
    val battleInputEnabled = scene.phase == BattlePhaseUi.ACTIVE || scene.phase == BattlePhaseUi.WAVE_INTRO
    val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsState()
    val motion = rememberProductBattleMotion(
        scene = scene,
        reduceMotion = reduceMotion,
        resumed = lifecycleState.isAtLeast(Lifecycle.State.RESUMED),
    )
    // A slot choice belongs to one run's input-enabled phase. A wave-end choice, pause, terminal or
    // a new run (retry) replaces the holder, so a stale choice never re-opens the build dialog when
    // input returns, including after a process restore made while such a phase was showing.
    var selectedSlotId by rememberSaveable(scene.runId, battleInputEnabled) {
        mutableStateOf<String?>(null)
    }
    val selectedSlot = scene.slots.firstOrNull { it.id == selectedSlotId }
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ProductColors.Void)
            .testTag("product-battle"),
    ) {
        Column(
            Modifier.fillMaxSize().then(
                if (battleInputEnabled) Modifier else Modifier
                    .clearAndSetSemantics { }
                    .pointerInput(Unit) {
                        // The in-Activity overlay is a sibling, not a modal platform window.
                        // Block the covered battle before child click/scroll handlers see input.
                        awaitPointerEventScope {
                            while (true) {
                                awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
                            }
                        }
                    },
            ),
        ) {
            ProductBattleHud(
                scene = scene,
                arenaLabel = arenaLabel,
                onAction = onAction,
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(ProductMetrics.hudInset),
            )
            ProductBattleField(
                scene = scene,
                motion = motion,
                selectedSlotId = selectedSlotId,
                onSelectSlot = { selectedSlotId = it },
                modifier = Modifier.weight(1f).fillMaxWidth(),
            )
            ProductBattleActionTray(
                scene = scene,
                onAction = onAction,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(ProductMetrics.hudInset),
            )
        }
        when (scene.phase) {
            BattlePhaseUi.PAUSED -> ProductPauseOverlay(onAction = onAction)
            BattlePhaseUi.INTER_WAVE_CHOICE -> ProductEnhancementOverlay(
                scene = scene,
                onAction = onAction,
            )
            BattlePhaseUi.VICTORY,
            BattlePhaseUi.DEFEAT,
            -> ProductTerminalOverlay(scene = scene, onAction = onAction)

            BattlePhaseUi.WAVE_INTRO,
            BattlePhaseUi.ACTIVE,
            -> Unit
        }
        if (
            selectedSlot != null &&
            scene.phase in setOf(BattlePhaseUi.WAVE_INTRO, BattlePhaseUi.ACTIVE)
        ) {
            ProductBattleSlotDialog(
                slot = selectedSlot,
                towerIds = scene.availableTowerIds,
                towerBuildCosts = scene.towerBuildCosts,
                resource = scene.resource,
                onAction = { action ->
                    onAction(action)
                    selectedSlotId = null
                },
                onDismiss = { selectedSlotId = null },
            )
        }
    }
}

@Composable
private fun ProductBattleHud(
    scene: BattleSceneUi,
    arenaLabel: String?,
    onAction: (ProductUiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val speedDescription = stringResource(R.string.product_battle_speed, scene.speedMultiplier)
    val enlargedType = LocalDensity.current.fontScale >= 1.5f
    val inputEnabled = scene.phase == BattlePhaseUi.ACTIVE || scene.phase == BattlePhaseUi.WAVE_INTRO
    Surface(
        modifier = modifier.testTag("product-battle-hud"),
        color = ProductColors.SignalPanel,
        contentColor = ProductColors.TextPrimary,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, ProductColors.Grid),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = if (enlargedType) 8.dp else 12.dp),
            verticalArrangement = Arrangement.spacedBy(if (enlargedType) 4.dp else 6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = arenaLabel ?: scene.stageTitle,
                    modifier = Modifier.padding(end = 12.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = ProductColors.TextSecondary,
                )
                Text(
                    text = stringResource(
                        R.string.product_battle_wave,
                        scene.wave,
                        scene.totalWaves,
                    ),
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    style = MaterialTheme.typography.labelLarge,
                    color = ProductColors.RelayAmber,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BattleHudChip(
                    label = stringResource(
                        R.string.product_battle_base,
                        scene.baseHealth,
                        scene.baseMaxHealth,
                    ),
                    color = if (scene.baseHealth * 3 <= scene.baseMaxHealth) {
                        ProductColors.WarningCoral
                    } else {
                        ProductColors.AuroraTeal
                    },
                )
                BattleHudChip(
                    label = stringResource(
                        R.string.product_battle_resource,
                        scene.resource,
                        scene.resourceCap,
                    ),
                    color = ProductColors.RelayAmber,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TextButton(
                    onClick = { onAction(ProductUiAction.ChangeSpeed) },
                    enabled = inputEnabled,
                    modifier = Modifier
                        .weight(1f)
                        .widthIn(min = ProductMetrics.minTouchTarget)
                        .heightIn(min = ProductMetrics.minTouchTarget)
                        .semantics { contentDescription = speedDescription }
                        .testTag("product-battle-speed"),
                ) {
                    Text(
                        text = stringResource(
                            R.string.product_battle_speed_value,
                            scene.speedMultiplier,
                        ),
                        color = ProductColors.IonBlue,
                    )
                }
                TextButton(
                    onClick = { onAction(ProductUiAction.PauseOrResume) },
                    enabled = inputEnabled,
                    modifier = Modifier
                        .weight(1f)
                        .widthIn(min = ProductMetrics.minTouchTarget)
                        .heightIn(min = ProductMetrics.minTouchTarget)
                        .testTag("product-battle-pause"),
                ) {
                    Text(
                        text = stringResource(R.string.product_battle_pause),
                        color = ProductColors.TextPrimary,
                    )
                }
            }
        }
    }
}

@Composable
private fun BattleHudChip(
    label: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.14f),
        contentColor = color,
        shape = MaterialTheme.shapes.small,
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

@Composable
private fun ProductBattleField(
    scene: BattleSceneUi,
    motion: ProductBattleMotion,
    selectedSlotId: String?,
    onSelectSlot: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val fieldDescription = stringResource(
        R.string.product_battle_field_description,
        scene.baseHealth,
        scene.baseMaxHealth,
        scene.enemies.size,
        scene.allies.size,
        scene.slots.size,
    )
    BoxWithConstraints(
        modifier = modifier.semantics {
            contentDescription = fieldDescription
        },
    ) {
        ProductBattleCanvas(
            scene = scene,
            motion = motion,
            selectedSlotId = selectedSlotId,
            modifier = Modifier.matchParentSize(),
        )
        scene.slots.forEach { slot ->
            val label = if (slot.towerName == null) {
                stringResource(R.string.product_battle_slot_empty, slot.index + 1)
            } else {
                stringResource(
                    R.string.product_battle_slot_tower,
                    slot.index + 1,
                    slot.towerName,
                    slot.level,
                )
            }
            Box(
                modifier = Modifier
                    .offset(
                        x = maxWidth * slot.xFraction - ProductMetrics.minTouchTarget / 2,
                        y = maxHeight * slot.yFraction - ProductMetrics.minTouchTarget / 2,
                    )
                    .size(ProductMetrics.minTouchTarget)
                    .testTag("product-battle-slot-${slot.id}")
                    .semantics {
                        contentDescription = label
                        role = Role.Button
                    }
                    .clickable(
                        enabled = scene.phase in setOf(
                            BattlePhaseUi.WAVE_INTRO,
                            BattlePhaseUi.ACTIVE,
                        ),
                    ) {
                        onSelectSlot(slot.id)
                    },
            )
        }
        scene.enemies.forEach { enemy ->
            val description = stringResource(
                R.string.product_battle_enemy_description,
                enemy.label,
                enemy.health,
                enemy.maxHealth,
            )
            Box(
                modifier = Modifier
                    .offset(
                        x = maxWidth * enemy.xFraction - 18.dp,
                        y = maxHeight * enemy.yFraction - 18.dp,
                    )
                    .size(36.dp)
                    .testTag("product-enemy-${enemy.id}")
                    .semantics { contentDescription = description },
            )
        }
        scene.allies.forEach { ally ->
            val description = stringResource(
                R.string.product_battle_ally_description,
                ally.label,
                ally.health,
                ally.maxHealth,
            )
            Box(
                modifier = Modifier
                    .offset(
                        x = maxWidth * ally.xFraction - 18.dp,
                        y = maxHeight * ally.yFraction - 18.dp,
                    )
                    .size(36.dp)
                    .testTag("product-ally-${ally.id}")
                    .semantics { contentDescription = description },
            )
        }
    }
}

@Composable
private fun ProductBattleCanvas(
    scene: BattleSceneUi,
    motion: ProductBattleMotion,
    selectedSlotId: String?,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.testTag("product-battle-canvas")) {
        drawRect(
            brush = Brush.verticalGradient(
                listOf(
                    ProductColors.Void,
                    ProductColors.DeepOrbit,
                    ProductColors.SignalPanelRaised,
                ),
            ),
            size = size,
        )
        val path = Path().apply {
            moveTo(size.width * 0.22f, size.height * 0.22f)
            cubicTo(
                size.width * 0.82f,
                size.height * 0.35f,
                size.width * 0.18f,
                size.height * 0.62f,
                size.width * 0.5f,
                size.height * 0.86f,
            )
        }
        drawPath(
            path = path,
            color = ProductColors.AuroraTeal.copy(alpha = 0.28f),
            style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round),
        )
        repeat(7) { index ->
            val y = size.height * (0.22f + index * 0.1f)
            drawLine(
                color = ProductColors.Grid,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1.dp.toPx(),
            )
        }
        scene.slots.forEach { slot ->
            val center = Offset(size.width * slot.xFraction, size.height * slot.yFraction)
            val tile = 48.dp.toPx()
            drawRoundRect(
                color = if (slot.id == selectedSlotId) {
                    ProductColors.RelayAmber.copy(alpha = 0.4f)
                } else {
                    ProductColors.SignalPanelRaised
                },
                topLeft = Offset(center.x - tile / 2, center.y - tile / 2),
                size = Size(tile, tile),
            )
            drawRoundRect(
                color = ProductColors.AuroraTeal,
                topLeft = Offset(center.x - tile / 2, center.y - tile / 2),
                size = Size(tile, tile),
                style = Stroke(width = 2.dp.toPx()),
            )
            if (slot.towerName != null) {
                drawCircle(
                    color = ProductColors.RelayAmber,
                    radius = tile * 0.22f,
                    center = center,
                )
                repeat(slot.level.coerceAtMost(4)) { level ->
                    drawCircle(
                        color = ProductColors.TextPrimary,
                        radius = 2.dp.toPx(),
                        center = Offset(center.x - 9.dp.toPx() + level * 6.dp.toPx(), center.y + 16.dp.toPx()),
                    )
                }
            }
        }
        scene.enemies.forEach { entity ->
            val position = motion.enemies.getValue(entity.id).value
            val center = Offset(size.width * position.x, size.height * position.y)
            val radius = if (entity.role == BattleEntityRoleUi.ENEMY_BOSS) 18.dp.toPx() else 11.dp.toPx()
            drawCircle(ProductColors.WarningCoral.copy(alpha = 0.2f), radius * 1.6f, center)
            drawCircle(ProductColors.WarningCoral, radius, center)
            drawHealthBar(center, radius, entity.health, entity.maxHealth, ProductColors.WarningCoral)
        }
        scene.allies.forEach { entity ->
            val position = motion.allies.getValue(entity.id).value
            val center = Offset(size.width * position.x, size.height * position.y)
            val radius = 10.dp.toPx()
            drawCircle(ProductColors.IonBlue.copy(alpha = 0.2f), radius * 1.6f, center)
            drawCircle(ProductColors.IonBlue, radius, center)
            drawHealthBar(center, radius, entity.health, entity.maxHealth, ProductColors.SuccessMint)
        }
        val base = Offset(size.width * 0.5f, size.height * 0.88f)
        drawCircle(ProductColors.AuroraTeal.copy(alpha = 0.16f), 52.dp.toPx(), base)
        drawCircle(ProductColors.AuroraTeal, 30.dp.toPx(), base)
        drawCircle(
            ProductColors.TextPrimary,
            30.dp.toPx(),
            base,
            style = Stroke(width = 3.dp.toPx()),
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawHealthBar(
    center: Offset,
    radius: Float,
    health: Int,
    maximum: Int,
    color: androidx.compose.ui.graphics.Color,
) {
    val width = radius * 2f
    val ratio = if (maximum > 0) (health.toFloat() / maximum).coerceIn(0f, 1f) else 0f
    drawRect(
        color = ProductColors.Void,
        topLeft = Offset(center.x - width / 2, center.y - radius * 1.8f),
        size = Size(width, 4.dp.toPx()),
    )
    drawRect(
        color = color,
        topLeft = Offset(center.x - width / 2, center.y - radius * 1.8f),
        size = Size(width * ratio, 4.dp.toPx()),
    )
}

@Composable
private fun ProductBattleActionTray(
    scene: BattleSceneUi,
    onAction: (ProductUiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (scene.phase !in setOf(BattlePhaseUi.WAVE_INTRO, BattlePhaseUi.ACTIVE)) return
    Surface(
        modifier = modifier.testTag("product-battle-actions"),
        color = ProductColors.SignalPanel,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, ProductColors.Grid),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (scene.availableAllyIds.isNotEmpty()) {
                    scene.availableAllyIds.take(3).forEach { allyId ->
                        val deployCost = scene.allyDeployCosts[allyId] ?: 0
                        Button(
                            onClick = { onAction(ProductUiAction.DeployAlly(allyId)) },
                            enabled = scene.resource >= deployCost,
                            modifier = Modifier
                                .widthIn(min = 136.dp, max = 200.dp)
                                .heightIn(min = ProductMetrics.minTouchTarget)
                                .testTag("product-deploy-$allyId"),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ProductColors.IonBlue.copy(alpha = 0.2f),
                                contentColor = ProductColors.IonBlue,
                            ),
                        ) {
                            Text(
                                text = "${productTitle(allyId)} · $deployCost",
                                style = MaterialTheme.typography.labelSmall,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
            }
                scene.abilities.forEach { ability ->
                    Button(
                        onClick = { onAction(ProductUiAction.UseAbility(ability.id)) },
                        enabled = ability.ready,
                        modifier = Modifier
                            .widthIn(min = 136.dp, max = 200.dp)
                            .heightIn(min = ProductMetrics.compactTouchTarget)
                            .testTag("product-ability-${ability.id}"),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ProductColors.AuroraTeal.copy(alpha = 0.2f),
                            contentColor = ProductColors.AuroraTeal,
                            disabledContainerColor = ProductColors.SignalPanelRaised,
                            disabledContentColor = ProductColors.TextMuted,
                        ),
                    ) {
                        Text(
                            text = if (ability.ready) {
                                stringResource(R.string.product_battle_ability_ready, ability.title)
                            } else {
                                stringResource(
                                    R.string.product_battle_ability_cooldown,
                                    ability.title,
                                    ability.cooldownSeconds,
                                )
                            },
                            style = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
        }
    }
}

@Composable
private fun ProductBattleSlotDialog(
    slot: BattleSlotUi,
    towerIds: List<String>,
    towerBuildCosts: Map<String, Int>,
    resource: Int,
    onAction: (ProductUiAction) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        ProductBattleSlotOverlay(slot, towerIds, towerBuildCosts, resource, onAction, onDismiss)
    }
}

// Separate from its Dialog window, which takes the system font scale, so compact large-font
// layout of the build/upgrade choices can be verified directly.
@Composable
internal fun ProductBattleSlotOverlay(
    slot: BattleSlotUi,
    towerIds: List<String>,
    towerBuildCosts: Map<String, Int>,
    resource: Int,
    onAction: (ProductUiAction) -> Unit,
    onDismiss: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ProductColors.Scrim),
        contentAlignment = Alignment.Center,
    ) {
        ProductCard(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = ProductMetrics.panelMaxWidth)
                .heightIn(max = ProductMetrics.panelMaxHeight)
                .padding(ProductMetrics.screenInset)
                .testTag("product-build-dialog"),
            highlighted = true,
        ) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                ProductDialogTitle(
                    text = stringResource(
                        if (slot.towerName == null) {
                            R.string.active_battle_build_popup_title
                        } else {
                            R.string.active_battle_upgrade_popup_title
                        },
                    ),
                )
                if (slot.towerName == null) {
                    towerIds.take(4).forEach { towerId ->
                        val cost = towerBuildCosts[towerId] ?: 0
                        ProductPrimaryButton(
                            label = "${stringResource(R.string.product_battle_build)} · ${productTitle(towerId)} · $cost",
                            onClick = {
                                onAction(ProductUiAction.BuildTower(slot.id, towerId))
                            },
                            enabled = resource >= cost,
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "product-build-${slot.id}-$towerId",
                        )
                    }
                } else {
                    ProductDialogBody(
                        text = stringResource(
                            R.string.product_roster_level,
                            slot.level,
                        ),
                        color = ProductColors.TextSecondary,
                    )
                    ProductPrimaryButton(
                        label = stringResource(R.string.product_battle_upgrade),
                        onClick = { onAction(ProductUiAction.UpgradeTower(slot.id)) },
                        enabled = !slot.maxLevel && slot.canAfford,
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "product-upgrade-${slot.id}",
                    )
                }
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = ProductMetrics.minTouchTarget),
                ) {
                    Text(
                        text = stringResource(R.string.active_battle_popup_close),
                        color = ProductColors.AuroraTeal,
                    )
                }
            }
        }
    }
}

@Composable
private fun ProductEnhancementOverlay(
    scene: BattleSceneUi,
    onAction: (ProductUiAction) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ProductColors.Void)
            .systemBarsPadding()
            .testTag("product-enhancement-overlay"),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(ProductMetrics.screenInset),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.product_enhancement_title),
                style = MaterialTheme.typography.headlineMedium,
                color = ProductColors.RelayAmber,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.product_enhancement_wave_complete, scene.wave),
                style = MaterialTheme.typography.titleMedium,
                color = ProductColors.AuroraTeal,
            )
            Text(
                text = stringResource(R.string.product_enhancement_body),
                color = ProductColors.TextSecondary,
                textAlign = TextAlign.Center,
            )
            scene.enhancementChoices.take(3).forEach { choice ->
                ProductCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("product-enhancement-${choice.id}"),
                    highlighted = true,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = choice.title,
                            style = MaterialTheme.typography.titleLarge,
                            color = ProductColors.TextPrimary,
                        )
                        Text(
                            text = choice.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = ProductColors.TextSecondary,
                        )
                        ProductPrimaryButton(
                            label = stringResource(R.string.product_enhancement_choose),
                            onClick = {
                                onAction(ProductUiAction.SelectEnhancement(choice.id))
                            },
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "product-enhancement-choose-${choice.id}",
                        )
                    }
                }
            }
            ProductPrimaryButton(
                label = stringResource(
                    R.string.product_enhancement_reroll,
                    scene.enhancementRerollsRemaining,
                ),
                onClick = { onAction(ProductUiAction.RerollEnhancements) },
                enabled = scene.enhancementRerollsRemaining > 0,
                modifier = Modifier.fillMaxWidth(),
                testTag = "product-enhancement-reroll",
            )
        }
    }
}

@Composable
private fun ProductPauseOverlay(onAction: (ProductUiAction) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ProductColors.Scrim)
            .systemBarsPadding()
            .testTag("product-pause-overlay"),
        contentAlignment = Alignment.Center,
    ) {
        ProductCard(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = ProductMetrics.panelMaxWidth)
                .heightIn(max = ProductMetrics.panelMaxHeight)
                .padding(ProductMetrics.screenInset),
            highlighted = true,
        ) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.product_battle_paused_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = ProductColors.TextPrimary,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(R.string.product_battle_paused_body),
                    color = ProductColors.TextSecondary,
                    textAlign = TextAlign.Center,
                )
                ProductPrimaryButton(
                    label = stringResource(R.string.active_battle_resume_action),
                    onClick = { onAction(ProductUiAction.PauseOrResume) },
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "product-battle-resume",
                )
                TextButton(
                    onClick = { onAction(ProductUiAction.RequestExitBattle) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = ProductMetrics.minTouchTarget)
                        .testTag("product-battle-exit"),
                ) {
                    Text(
                        text = stringResource(R.string.product_battle_exit),
                        color = ProductColors.WarningCoral,
                    )
                }
            }
        }
    }
}

@Composable
private fun ProductTerminalOverlay(
    scene: BattleSceneUi,
    onAction: (ProductUiAction) -> Unit,
) {
    val result = scene.result ?: BattleResultUi(
        victory = scene.phase == BattlePhaseUi.VICTORY,
        completedWaves = scene.wave,
        defeatedEnemies = 0,
        credits = 0,
        experience = 0,
        claimed = false,
        multiplierAvailable = false,
    )
    val title = stringResource(
        if (result.victory) {
            R.string.active_battle_terminal_victory_title
        } else {
            R.string.active_battle_terminal_defeat_title
        },
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ProductColors.Scrim)
            .systemBarsPadding()
            .testTag("product-terminal-overlay")
            .semantics {
                liveRegion = LiveRegionMode.Assertive
                contentDescription = title
            },
        contentAlignment = Alignment.Center,
    ) {
        ProductCard(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = ProductMetrics.panelMaxWidth)
                .heightIn(max = ProductMetrics.panelMaxHeight)
                .padding(ProductMetrics.screenInset),
            highlighted = true,
        ) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.displaySmall,
                    color = if (result.victory) {
                        ProductColors.SuccessMint
                    } else {
                        ProductColors.WarningCoral
                    },
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(
                        if (result.victory) {
                            R.string.active_battle_terminal_victory_body
                        } else {
                            R.string.active_battle_terminal_defeat_body
                        },
                    ),
                    color = ProductColors.TextPrimary,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(
                        R.string.product_result_wave,
                        result.completedWaves,
                        scene.totalWaves,
                    ),
                    color = ProductColors.TextSecondary,
                )
                Text(
                    text = stringResource(
                        R.string.product_result_reward,
                        result.credits,
                        result.experience,
                    ),
                    color = ProductColors.RelayAmber,
                )
                if (!result.claimed) {
                    ProductPrimaryButton(
                        label = stringResource(R.string.product_result_claim),
                        onClick = { onAction(ProductUiAction.ClaimBattleReward) },
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "product-claim-battle-reward",
                    )
                    if (result.victory) {
                        ProductPrimaryButton(
                            label = stringResource(R.string.product_result_multiplier),
                            onClick = { onAction(ProductUiAction.ClaimBattleRewardMultiplier) },
                            enabled = result.multiplierAvailable,
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "product-rewarded-multiplier",
                        )
                    }
                } else if (result.claimed) {
                    Text(
                        text = stringResource(R.string.product_result_claimed),
                        color = ProductColors.SuccessMint,
                    )
                }
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ProductPrimaryButton(
                        label = stringResource(R.string.product_result_retry),
                        onClick = { onAction(ProductUiAction.RetryBattle) },
                        enabled = result.claimed,
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "product-retry-battle",
                    )
                    ProductPrimaryButton(
                        label = stringResource(R.string.product_result_map),
                        onClick = { onAction(ProductUiAction.BackToCampaign) },
                        enabled = result.claimed,
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "product-return-map",
                    )
                }
            }
        }
    }
}
