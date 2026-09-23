package dev.mysd.android.product

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
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
internal fun ProductRosterScreen(
    shell: ProductShellUi,
    state: ProductSurfaceUi.Roster,
    onAction: (ProductUiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    ProductScreenFrame(shell = shell, onAction = onAction, modifier = modifier) {
        ProductScrollableRoute(
            tag = "product-roster",
            title = stringResource(R.string.product_roster_title),
            body = stringResource(R.string.product_roster_body),
        ) {
            state.entries.forEach { entry ->
                val equipped = entry.id in state.equippedIds
                ProductCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("product-roster-${entry.id}")
                        .semantics {
                            stateDescription = when {
                                !entry.unlocked -> "Заблокировано"
                                equipped -> "В составе"
                                else -> "Доступно"
                            }
                        },
                    highlighted = equipped,
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        ProductRosterEmblem(
                            title = entry.title,
                            modifier = Modifier.size(64.dp),
                        )
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = entry.title,
                                modifier = Modifier.fillMaxWidth().testTag("product-roster-title-${entry.id}"),
                                style = MaterialTheme.typography.titleLarge,
                                color = ProductColors.TextPrimary,
                            )
                            Text(
                                text = entry.role,
                                modifier = Modifier.fillMaxWidth().testTag("product-roster-role-${entry.id}"),
                                color = ProductColors.AuroraTeal,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Text(
                                text = stringResource(R.string.product_roster_level, entry.level),
                                color = ProductColors.TextSecondary,
                            )
                            if (entry.unlocked) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    ProductPrimaryButton(
                                        label = stringResource(
                                            if (equipped) {
                                                R.string.product_roster_remove
                                            } else {
                                                R.string.product_roster_equip
                                            },
                                        ),
                                        onClick = {
                                            onAction(
                                                if (entry.category == RosterCategoryUi.HERO) {
                                                    ProductUiAction.ToggleHeroSkill(entry.id)
                                                } else {
                                                    ProductUiAction.ToggleRosterEntry(entry.id)
                                                },
                                            )
                                        },
                                        enabled = entry.category == RosterCategoryUi.HERO ||
                                            !equipped || state.entries.count {
                                                it.category == entry.category &&
                                                    it.id in state.equippedIds
                                            } > 1,
                                        modifier = Modifier.fillMaxWidth(),
                                        testTag = "product-roster-toggle-${entry.id}",
                                    )
                                    ProductPrimaryButton(
                                        label = if (entry.maximumLevel) {
                                            stringResource(R.string.product_roster_maximum)
                                        } else {
                                            stringResource(
                                                R.string.product_roster_upgrade,
                                                entry.upgradeCost,
                                            )
                                        },
                                        onClick = {
                                            onAction(ProductUiAction.UpgradeRosterEntry(entry.id))
                                        },
                                        enabled = !entry.maximumLevel &&
                                            entry.affordable && entry.upgradeCost > 0,
                                        modifier = Modifier.fillMaxWidth(),
                                        testTag = "product-roster-upgrade-${entry.id}",
                                    )
                                }
                            } else {
                                val lockedLabel = if (entry.unlockStage != null) {
                                    stringResource(
                                        R.string.product_roster_locked,
                                        entry.unlockStage,
                                    )
                                } else {
                                    stringResource(R.string.product_roster_locked_progress)
                                }
                                Text(
                                    text = lockedLabel,
                                    color = ProductColors.TextMuted,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductRosterEmblem(
    title: String,
    modifier: Modifier = Modifier,
) {
    val description = "$title — знак боевой роли"
    Canvas(
        modifier = modifier.semantics { contentDescription = description },
    ) {
        val center = Offset(size.width / 2, size.height / 2)
        drawCircle(ProductColors.AuroraTeal.copy(alpha = 0.14f), size.minDimension * 0.48f, center)
        drawCircle(ProductColors.RelayAmber, size.minDimension * 0.27f, center)
        drawCircle(ProductColors.Void, size.minDimension * 0.12f, center)
        drawLine(
            color = ProductColors.TextPrimary,
            start = Offset(size.width * 0.2f, size.height * 0.8f),
            end = Offset(size.width * 0.8f, size.height * 0.8f),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round,
        )
    }
}

@Composable
internal fun ProductTechScreen(
    shell: ProductShellUi,
    state: ProductSurfaceUi.Tech,
    onAction: (ProductUiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    ProductScreenFrame(shell = shell, onAction = onAction, modifier = modifier) {
        ProductScrollableRoute(
            tag = "product-tech",
            title = stringResource(R.string.product_tech_title),
            body = stringResource(R.string.product_tech_body),
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                // Preserve prerequisite-row order, but never force three tiny cards onto a phone.
                // Enlarged type needs wider cards, not truncated labels or smaller text.
                val minimumCardWidth = 160.dp * LocalDensity.current.fontScale.coerceAtLeast(1f)
                val columns = ((maxWidth + 8.dp) / (minimumCardWidth + 8.dp)).toInt().coerceIn(1, 3)
                Column(verticalArrangement = Arrangement.spacedBy(ProductMetrics.cardGap)) {
                    state.nodes.groupBy(TechNodeUi::row).toSortedMap().forEach { (_, rowNodes) ->
                        rowNodes.sortedBy(TechNodeUi::column).chunked(columns).forEach { nodes ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.Top,
                            ) {
                                nodes.forEach { node ->
                                    ProductTechNode(
                                        node = node,
                                        onAction = onAction,
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                                repeat(columns - nodes.size) {
                                    Box(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductTechNode(
    node: TechNodeUi,
    onAction: (ProductUiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val statusLabel = when (node.status) {
        TechNodeStatusUi.LOCKED -> stringResource(
            R.string.product_tech_locked,
            node.prerequisiteLabel.orEmpty(),
        )
        TechNodeStatusUi.AVAILABLE -> stringResource(R.string.product_tech_available)
        TechNodeStatusUi.UNLOCKED -> stringResource(R.string.product_tech_unlocked)
    }
    ProductCard(
        modifier = modifier
            .testTag("product-tech-${node.id}")
            .semantics { stateDescription = statusLabel },
        highlighted = node.status == TechNodeStatusUi.AVAILABLE,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        when (node.status) {
                            TechNodeStatusUi.LOCKED -> ProductColors.TextMuted
                            TechNodeStatusUi.AVAILABLE -> ProductColors.RelayAmber
                            TechNodeStatusUi.UNLOCKED -> ProductColors.SuccessMint
                        },
                        MaterialTheme.shapes.small,
                    ),
            )
            Text(
                text = node.title,
                modifier = Modifier.fillMaxWidth().testTag("product-tech-title-${node.id}"),
                style = MaterialTheme.typography.titleSmall,
                color = ProductColors.TextPrimary,
                textAlign = TextAlign.Center,
            )
            Text(
                text = statusLabel,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.labelSmall,
                color = ProductColors.TextSecondary,
                textAlign = TextAlign.Center,
            )
            if (node.status == TechNodeStatusUi.AVAILABLE) {
                ProductPrimaryButton(
                    label = stringResource(R.string.product_tech_unlock, node.cost),
                    onClick = { onAction(ProductUiAction.UnlockTech(node.id)) },
                    enabled = node.affordable,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "product-tech-unlock-${node.id}",
                )
            }
        }
    }
}

@Composable
internal fun ProductShopScreen(
    shell: ProductShellUi,
    state: ProductSurfaceUi.Shop,
    onAction: (ProductUiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    ProductScreenFrame(shell = shell, onAction = onAction, modifier = modifier) {
        ProductScrollableRoute(
            tag = "product-shop",
            title = stringResource(R.string.product_shop_title),
            body = stringResource(R.string.product_shop_body),
        ) {
            state.products.forEach { product ->
                ProductCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("product-shop-${product.id}"),
                    highlighted = product.kind != ShopProductKindUi.SOFT_CURRENCY,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = product.title,
                                modifier = Modifier.fillMaxWidth().testTag("product-shop-title-${product.id}"),
                                style = MaterialTheme.typography.titleLarge,
                                color = ProductColors.TextPrimary,
                            )
                            if (product.kind != ShopProductKindUi.SOFT_CURRENCY) {
                                Text(
                                    text = stringResource(R.string.product_shop_iap_badge),
                                    modifier = Modifier.fillMaxWidth().testTag("product-shop-badge-${product.id}"),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ProductColors.AuroraTeal,
                                )
                            }
                        }
                        Text(
                            text = product.description,
                            color = ProductColors.TextSecondary,
                        )
                        if (product.softReward > 0) {
                            Text(
                                text = stringResource(
                                    R.string.product_shop_soft_reward,
                                    product.softReward,
                                ),
                                color = ProductColors.RelayAmber,
                            )
                        }
                        if (product.energyReward > 0) {
                            Text(
                                text = stringResource(
                                    R.string.product_shop_energy_reward,
                                    product.energyReward,
                                ),
                                color = ProductColors.AuroraTeal,
                            )
                        }
                        if (product.purchased > 0) {
                            Text(
                                text = stringResource(
                                    R.string.product_shop_purchased,
                                    product.purchased,
                                ),
                                color = ProductColors.TextMuted,
                            )
                        }
                        ProductPrimaryButton(
                            label = when {
                                product.owned -> stringResource(R.string.product_shop_owned)
                                product.kind == ShopProductKindUi.SOFT_CURRENCY -> {
                                    stringResource(R.string.product_shop_buy, product.cost)
                                }
                                product.kind == ShopProductKindUi.REWARDED_STUB -> {
                                    stringResource(R.string.product_result_multiplier)
                                }
                                else -> stringResource(R.string.product_shop_iap)
                            },
                            onClick = {
                                onAction(
                                    when (product.kind) {
                                        ShopProductKindUi.SOFT_CURRENCY -> {
                                            ProductUiAction.BuyShopProduct(product.id)
                                        }
                                        ShopProductKindUi.REWARDED_STUB -> {
                                            ProductUiAction.RequestRewardedStub(product.id)
                                        }
                                        ShopProductKindUi.PURCHASE_STUB -> {
                                            ProductUiAction.RequestPurchaseStub(product.id)
                                        }
                                    },
                                )
                            },
                            enabled = !product.owned && (
                                product.kind != ShopProductKindUi.SOFT_CURRENCY ||
                                    product.affordable
                                ),
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "product-shop-action-${product.id}",
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun ProductRewardTrackScreen(
    shell: ProductShellUi,
    state: ProductSurfaceUi.RewardTrack,
    onAction: (ProductUiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    ProductScreenFrame(shell = shell, onAction = onAction, modifier = modifier) {
        ProductScrollableRoute(
            tag = "product-reward-track",
            title = stringResource(R.string.product_track_title),
            body = stringResource(R.string.product_track_body),
        ) {
            state.tiers.forEach { tier ->
                ProductCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("product-track-tier-${tier.tier}")
                        .semantics {
                            stateDescription = when (tier.status) {
                                RewardTierStatusUi.LOCKED -> "Недоступно"
                                RewardTierStatusUi.AVAILABLE -> "Можно получить"
                                RewardTierStatusUi.CLAIMED -> "Получено"
                            }
                        },
                    highlighted = tier.status == RewardTierStatusUi.AVAILABLE,
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Surface(
                                color = when (tier.status) {
                                    RewardTierStatusUi.LOCKED -> ProductColors.SignalPanelRaised
                                    RewardTierStatusUi.AVAILABLE -> ProductColors.RelayAmber
                                    RewardTierStatusUi.CLAIMED -> ProductColors.SuccessMint
                                },
                                shape = MaterialTheme.shapes.medium,
                            ) {
                                Text(
                                    text = tier.tier.toString(),
                                    modifier = Modifier.padding(14.dp),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = ProductColors.Void,
                                )
                            }
                            Text(
                                text = stringResource(R.string.product_track_tier, tier.tier),
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.titleMedium,
                                color = ProductColors.TextPrimary,
                            )
                        }
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = tier.rewardLabel,
                                modifier = Modifier.fillMaxWidth().testTag("product-track-copy-${tier.tier}"),
                                color = ProductColors.TextSecondary,
                            )
                            Text(
                                text = rewardTierContents(tier),
                                color = ProductColors.RelayAmber,
                                style = MaterialTheme.typography.labelMedium,
                            )
                            Text(
                                text = stringResource(
                                    R.string.product_track_progress,
                                    state.experience.coerceAtMost(tier.requiredExperience),
                                    tier.requiredExperience,
                                ),
                                color = ProductColors.AuroraTeal,
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                        ProductPrimaryButton(
                            label = stringResource(
                                when (tier.status) {
                                    RewardTierStatusUi.LOCKED -> R.string.product_track_locked
                                    RewardTierStatusUi.AVAILABLE -> R.string.product_track_claim
                                    RewardTierStatusUi.CLAIMED -> R.string.product_track_claimed
                                },
                            ),
                            onClick = { onAction(ProductUiAction.ClaimRewardTier(tier.id)) },
                            enabled = tier.status == RewardTierStatusUi.AVAILABLE,
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "product-track-claim-${tier.tier}",
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun rewardTierContents(tier: RewardTierUi): String {
    val parts = mutableListOf<String>()
    if (tier.softReward > 0) {
        parts += stringResource(R.string.product_track_soft_reward, tier.softReward)
    }
    if (tier.crystalReward > 0) {
        parts += stringResource(R.string.product_track_crystal_reward, tier.crystalReward)
    }
    if (tier.energyReward > 0) {
        parts += stringResource(R.string.product_track_energy_reward, tier.energyReward)
    }
    val contents = parts.joinToString(" · ")
    return if (contents.isBlank()) {
        stringResource(R.string.product_track_no_reward)
    } else {
        contents
    }
}

@Composable
internal fun ProductSettingsScreen(
    shell: ProductShellUi,
    state: ProductSurfaceUi.Settings,
    onAction: (ProductUiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    ProductScreenFrame(shell = shell, onAction = onAction, modifier = modifier) {
        ProductScrollableRoute(
            tag = "product-settings",
            title = stringResource(R.string.product_settings_title),
            body = stringResource(R.string.product_settings_body),
        ) {
            ProductSettingUi.entries.forEach { setting ->
                val label = stringResource(
                    when (setting) {
                        ProductSettingUi.SOUND -> R.string.product_settings_audio
                        ProductSettingUi.MUSIC -> R.string.product_settings_music
                        ProductSettingUi.HAPTICS -> R.string.product_settings_haptics
                        ProductSettingUi.REDUCED_MOTION -> R.string.product_settings_reduced_motion
                    },
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = ProductMetrics.actionHeight)
                        .toggleable(
                            value = state.values[setting] == true,
                            role = Role.Switch,
                            onValueChange = { onAction(ProductUiAction.ToggleSetting(setting)) },
                        )
                        .testTag("product-setting-${setting.name.lowercase()}")
                        .semantics {
                            role = Role.Switch
                            stateDescription = if (state.values[setting] == true) {
                                "Включено"
                            } else {
                                "Выключено"
                            }
                        },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = label,
                        modifier = Modifier.weight(1f).padding(end = 12.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        color = ProductColors.TextPrimary,
                    )
                    Switch(
                        checked = state.values[setting] == true,
                        onCheckedChange = null,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = ProductColors.Void,
                            checkedTrackColor = ProductColors.RelayAmber,
                            uncheckedThumbColor = ProductColors.TextSecondary,
                            uncheckedTrackColor = ProductColors.SignalPanelRaised,
                        ),
                    )
                }
            }
            ProductPrimaryButton(
                label = stringResource(R.string.product_settings_close),
                onClick = { onAction(ProductUiAction.Navigate(ProductDestinationUi.CAMPAIGN)) },
                modifier = Modifier.fillMaxWidth(),
                testTag = "product-settings-close",
            )
        }
    }
}

@Composable
private fun ProductScrollableRoute(
    tag: String,
    title: String,
    body: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(ProductMetrics.screenInset)
            .testTag(tag),
        verticalArrangement = Arrangement.spacedBy(ProductMetrics.cardGap),
    ) {
        ProductSectionHeader(title = title, body = body)
        content()
    }
}
