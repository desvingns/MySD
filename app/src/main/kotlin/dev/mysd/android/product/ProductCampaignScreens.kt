package dev.mysd.android.product

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.mysd.android.R
import dev.mysd.android.ui.theme.ProductColors
import dev.mysd.android.ui.theme.ProductMetrics

@Composable
internal fun ProductLaunchScreen(
    state: ProductSurfaceUi.Launch,
    onAction: (ProductUiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        ProductColors.Void,
                        ProductColors.DeepOrbit,
                        ProductColors.SignalPanelRaised,
                    ),
                ),
            )
            .testTag("product-launch"),
    ) {
        ProductLaunchBackdrop(modifier = Modifier.matchParentSize())
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(ProductMetrics.screenInset),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(ProductColors.RelayAmber, CircleShape),
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    text = stringResource(R.string.campaign_launch_kicker),
                    style = MaterialTheme.typography.labelLarge,
                    color = ProductColors.TextSecondary,
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = stringResource(R.string.campaign_launch_title),
                style = MaterialTheme.typography.displayMedium,
                color = ProductColors.RelayAmber,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.product_launch_tagline),
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.headlineSmall,
                color = ProductColors.TextPrimary,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.campaign_launch_body),
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = ProductColors.AuroraTeal,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.product_launch_detail),
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = ProductColors.TextSecondary,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.weight(1f))
            ProductPrimaryButton(
                label = stringResource(R.string.campaign_enter_action),
                onClick = { onAction(ProductUiAction.EnterCampaign) },
                enabled = state.canEnter,
                modifier = Modifier.fillMaxWidth(),
                testTag = "product-enter-campaign",
            )
        }
    }
}

@Composable
private fun ProductLaunchBackdrop(modifier: Modifier = Modifier) {
    val description = stringResource(R.string.product_launch_tagline)
    Canvas(
        modifier = modifier.semantics {
            contentDescription = description
        },
    ) {
        val center = Offset(size.width * 0.5f, size.height * 0.42f)
        val radius = size.minDimension * 0.22f
        drawCircle(ProductColors.AuroraTeal.copy(alpha = 0.08f), radius * 1.7f, center)
        drawCircle(ProductColors.AuroraTeal.copy(alpha = 0.16f), radius, center)
        drawCircle(ProductColors.RelayAmber.copy(alpha = 0.9f), radius * 0.24f, center)
        drawCircle(
            color = ProductColors.TextPrimary.copy(alpha = 0.7f),
            radius = radius,
            center = center,
            style = Stroke(width = 2.dp.toPx()),
        )
        repeat(7) { index ->
            val x = size.width * (0.1f + index * 0.13f)
            val y = size.height * (0.18f + (index % 3) * 0.09f)
            drawCircle(
                color = if (index % 2 == 0) ProductColors.RelayAmber else ProductColors.AuroraTeal,
                radius = (1.5f + index % 3) * density,
                center = Offset(x, y),
            )
        }
    }
}

@Composable
internal fun ProductCampaignMapScreen(
    shell: ProductShellUi,
    state: ProductSurfaceUi.CampaignMap,
    onAction: (ProductUiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    ProductScreenFrame(shell = shell, onAction = onAction, modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(ProductMetrics.screenInset)
                .testTag("product-campaign-map"),
            verticalArrangement = Arrangement.spacedBy(ProductMetrics.sectionGap),
        ) {
            ProductSectionHeader(
                title = stringResource(R.string.campaign_selection_title),
                body = stringResource(R.string.product_campaign_body),
            )
            Text(
                text = stringResource(R.string.product_campaign_title),
                style = MaterialTheme.typography.titleLarge,
                color = ProductColors.AuroraTeal,
            )
            state.stages.forEach { stage ->
                ProductStageCard(stage = stage, onAction = onAction)
            }
        }
    }
}

@Composable
private fun ProductStageCard(
    stage: CampaignStageUi,
    onAction: (ProductUiAction) -> Unit,
) {
    val unlocked = stage.status != StageStatusUi.LOCKED
    val statusLabel = stringResource(
        when (stage.status) {
            StageStatusUi.LOCKED -> R.string.product_stage_locked
            StageStatusUi.AVAILABLE -> R.string.product_stage_available
            StageStatusUi.COMPLETED -> R.string.product_stage_completed
        },
    )
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val stacked = maxWidth < 360.dp || LocalDensity.current.fontScale >= 1.5f
        ProductCard(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("product-stage-${stage.id}")
                .semantics {
                    stateDescription = statusLabel
                },
            highlighted = stage.status == StageStatusUi.AVAILABLE,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (stacked) {
                    ProductStageHeading(stage = stage, modifier = Modifier.fillMaxWidth())
                    ProductStageStatus(stage = stage, statusLabel = statusLabel)
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        ProductStageHeading(stage = stage, modifier = Modifier.weight(1f))
                        ProductStageStatus(stage = stage, statusLabel = statusLabel)
                    }
                }
                if (stacked) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        ProductStageMetadata(stage = stage, modifier = Modifier.fillMaxWidth())
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ProductStageMetadata(stage = stage)
                    }
                }
                ProductPrimaryButton(
                    label = stringResource(R.string.product_stage_open),
                    onClick = { onAction(ProductUiAction.SelectStage(stage.id)) },
                    enabled = unlocked,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "product-stage-open-${stage.id}",
                )
            }
        }
    }
}

@Composable
private fun ProductStageHeading(stage: CampaignStageUi, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = stage.title,
            modifier = Modifier.fillMaxWidth().testTag("product-stage-title-${stage.id}"),
            style = MaterialTheme.typography.titleLarge,
            color = ProductColors.TextPrimary,
        )
        Text(
            text = stage.subtitle,
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodyMedium,
            color = ProductColors.TextSecondary,
        )
    }
}

@Composable
private fun ProductStageStatus(stage: CampaignStageUi, statusLabel: String) {
    Surface(
        color = when (stage.status) {
            StageStatusUi.LOCKED -> ProductColors.SignalPanelRaised
            StageStatusUi.AVAILABLE -> ProductColors.AuroraTeal.copy(alpha = 0.16f)
            StageStatusUi.COMPLETED -> ProductColors.SuccessMint.copy(alpha = 0.16f)
        },
        shape = MaterialTheme.shapes.small,
    ) {
        Text(
            text = statusLabel,
            modifier = Modifier
                .padding(horizontal = 10.dp, vertical = 6.dp)
                .testTag("product-stage-status-${stage.id}"),
            style = MaterialTheme.typography.labelMedium,
            color = if (stage.status != StageStatusUi.LOCKED) {
                ProductColors.AuroraTeal
            } else {
                ProductColors.TextMuted
            },
        )
    }
}

@Composable
private fun ProductStageMetadata(stage: CampaignStageUi, modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.product_stage_stars, stage.stars),
        modifier = modifier.testTag("product-stage-stars-${stage.id}"),
        color = ProductColors.RelayAmber,
        style = MaterialTheme.typography.labelLarge,
    )
    Text(
        text = stringResource(R.string.product_stage_waves, stage.waveCount),
        modifier = modifier.testTag("product-stage-waves-${stage.id}"),
        color = ProductColors.TextSecondary,
        style = MaterialTheme.typography.labelLarge,
    )
    Text(
        text = stringResource(R.string.product_stage_energy_cost, stage.energyCost),
        modifier = modifier.testTag("product-stage-energy-${stage.id}"),
        color = ProductColors.TextSecondary,
        style = MaterialTheme.typography.labelLarge,
    )
}

@Composable
internal fun ProductSetupScreen(
    shell: ProductShellUi,
    state: ProductSurfaceUi.Setup,
    onAction: (ProductUiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    ProductScreenFrame(shell = shell, onAction = onAction, modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(ProductMetrics.screenInset)
                .testTag("product-stage-setup"),
            verticalArrangement = Arrangement.spacedBy(ProductMetrics.sectionGap),
        ) {
            ProductSectionHeader(
                title = stringResource(R.string.battle_setup_title),
                body = stringResource(R.string.product_setup_body),
            )
            ProductCard(modifier = Modifier.fillMaxWidth(), highlighted = true) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = state.title,
                        style = MaterialTheme.typography.headlineSmall,
                        color = ProductColors.TextPrimary,
                    )
                    Text(
                        text = stringResource(R.string.product_setup_threat, state.threatLabel),
                        color = ProductColors.WarningCoral,
                    )
                    Text(
                        text = stringResource(R.string.product_setup_wave_count, state.waveCount),
                        color = ProductColors.TextSecondary,
                    )
                    Text(
                        text = stringResource(R.string.product_setup_hero, state.heroName),
                        color = ProductColors.AuroraTeal,
                    )
                }
            }
            ProductSectionHeader(
                title = stringResource(R.string.product_setup_hero_loadout),
                body = stringResource(R.string.product_setup_hero_body),
            )
            if (state.heroChoices.isEmpty()) {
                Text(
                    text = stringResource(R.string.product_setup_hero_empty),
                    color = ProductColors.TextSecondary,
                )
            }
            state.heroChoices.forEach { choice ->
                val status = stringResource(
                    if (choice.selected) R.string.product_setup_hero_selected
                    else R.string.product_setup_hero_unselected,
                )
                ProductCard(modifier = Modifier.fillMaxWidth(), highlighted = choice.selected) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = ProductMetrics.minTouchTarget)
                            .testTag("product-setup-hero-${choice.id}")
                            .semantics { stateDescription = status }
                            .toggleable(value = choice.selected, role = Role.Checkbox) {
                                onAction(ProductUiAction.ToggleHeroSkill(choice.id))
                            },
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = choice.title,
                            style = MaterialTheme.typography.titleLarge,
                            color = ProductColors.TextPrimary,
                        )
                        Text(text = choice.description, color = ProductColors.TextSecondary)
                        Text(
                            text = status,
                            style = MaterialTheme.typography.labelLarge,
                            color = ProductColors.AuroraTeal,
                        )
                    }
                }
            }
            Text(
                text = stringResource(
                    R.string.product_setup_loadout_capacity,
                    state.loadout.count { it.contentId != null },
                    7,
                ),
                style = MaterialTheme.typography.titleLarge,
                color = ProductColors.TextPrimary,
            )
            state.loadout.forEach { slot ->
                ProductCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("product-loadout-slot-${slot.index}"),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = slot.label?.let {
                                stringResource(
                                    R.string.product_setup_slot_selected,
                                    slot.index + 1,
                                    it,
                                )
                            } ?: stringResource(
                                R.string.product_setup_slot_empty,
                                slot.index + 1,
                            ),
                            color = ProductColors.TextPrimary,
                        )
                        val compatibleChoices = state.availableLoadout.filter { option ->
                            slot.kind == null || option.kind == slot.kind
                        }
                        compatibleChoices.groupBy(SetupChoiceUi::kind).forEach { (kind, choices) ->
                            Text(
                                text = stringResource(
                                    if (kind == LoadoutKindUi.TOWER) {
                                        R.string.product_setup_kind_tower
                                    } else {
                                        R.string.product_setup_kind_ally
                                    },
                                ),
                                color = ProductColors.TextSecondary,
                                style = MaterialTheme.typography.labelMedium,
                            )
                            choices.forEach { option ->
                                TextButton(
                                    onClick = {
                                        onAction(
                                            ProductUiAction.SetLoadoutSlot(
                                                index = slot.index,
                                                contentId = option.id,
                                            ),
                                        )
                                    },
                                    enabled = option.id != slot.contentId,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = ProductMetrics.minTouchTarget)
                                        .testTag("product-loadout-${slot.index}-${option.id}"),
                                ) {
                                    Text(
                                        text = option.title,
                                        modifier = Modifier.fillMaxWidth(),
                                        color = if (option.id == slot.contentId) {
                                            ProductColors.TextMuted
                                        } else {
                                            ProductColors.AuroraTeal
                                        },
                                        textAlign = TextAlign.Center,
                                    )
                                }
                            }
                        }
                    }
                }
            }
            if (!state.energyAvailable) {
                Text(
                    text = stringResource(R.string.product_setup_energy_guard, state.energyCost),
                    color = ProductColors.WarningCoral,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            ProductPrimaryButton(
                label = stringResource(R.string.product_setup_start),
                onClick = { onAction(ProductUiAction.StartBattle) },
                enabled = state.canStart && state.energyAvailable,
                modifier = Modifier.fillMaxWidth(),
                testTag = "product-start-battle",
            )
            ProductPrimaryButton(
                label = stringResource(R.string.product_setup_sweep),
                onClick = { onAction(ProductUiAction.SweepStage) },
                enabled = state.sweepAvailable && state.energyAvailable,
                modifier = Modifier.fillMaxWidth(),
                testTag = "product-sweep-stage",
            )
            if (!state.sweepAvailable) {
                Text(
                    text = stringResource(R.string.product_setup_sweep_guard),
                    style = MaterialTheme.typography.bodyMedium,
                    color = ProductColors.TextMuted,
                )
            }
            TextButton(
                onClick = { onAction(ProductUiAction.BackToCampaign) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = ProductMetrics.minTouchTarget),
            ) {
                Text(
                    text = stringResource(R.string.product_setup_back),
                    color = ProductColors.AuroraTeal,
                )
            }
        }
    }
}
