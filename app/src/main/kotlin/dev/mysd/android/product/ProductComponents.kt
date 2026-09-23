package dev.mysd.android.product

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.mysd.android.R
import dev.mysd.android.ui.WholeWordText
import dev.mysd.android.ui.theme.ProductColors
import dev.mysd.android.ui.theme.ProductMetrics

@Composable
internal fun ProductScreenFrame(
    shell: ProductShellUi,
    onAction: (ProductUiAction) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(ProductColors.Void, ProductColors.DeepOrbit),
                ),
            ),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            ProductHeader(
                shell = shell,
                onOpenRewardTrack = { onAction(ProductUiAction.OpenRewardTrack) },
                onOpenSettings = { onAction(ProductUiAction.OpenSettings) },
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .then(
                        if (shell.showMetaNavigation) Modifier
                        else Modifier.navigationBarsPadding(),
                    ),
            ) {
                content()
            }
            if (shell.showMetaNavigation) {
                ProductBottomNavigation(
                    selected = shell.selectedDestination,
                    onSelect = { onAction(ProductUiAction.Navigate(it)) },
                )
            }
        }
    }
}

@Composable
private fun ProductHeader(
    shell: ProductShellUi,
    onOpenRewardTrack: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .testTag("product-header"),
        color = ProductColors.SignalPanel,
        contentColor = ProductColors.TextPrimary,
    ) {
        BoxWithConstraints {
            val compactHeader = maxWidth < 360.dp || LocalDensity.current.fontScale >= 1.5f
            Column(
                modifier = Modifier.padding(
                    horizontal = ProductMetrics.screenInset,
                    vertical = ProductMetrics.hudInset,
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (compactHeader) {
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        ResourceChip(
                            text = stringResource(
                                R.string.product_resource_energy,
                                shell.energy,
                                shell.energyCap,
                            ),
                            testTag = "product-resource-energy",
                        )
                        ResourceChip(
                            text = stringResource(
                                R.string.product_resource_credits,
                                shell.credits,
                            ),
                            testTag = "product-resource-credits",
                        )
                        ResourceChip(
                            text = stringResource(
                                R.string.product_resource_crystals,
                                shell.crystals,
                            ),
                            testTag = "product-resource-crystals",
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ResourceChip(
                            text = stringResource(
                                R.string.product_resource_energy,
                                shell.energy,
                                shell.energyCap,
                            ),
                            modifier = Modifier.weight(1f),
                            testTag = "product-resource-energy",
                        )
                        ResourceChip(
                            text = stringResource(
                                R.string.product_resource_credits,
                                shell.credits,
                            ),
                            modifier = Modifier.weight(1f),
                            testTag = "product-resource-credits",
                        )
                        ResourceChip(
                            text = stringResource(
                                R.string.product_resource_crystals,
                                shell.crystals,
                            ),
                            modifier = Modifier.weight(1f),
                            testTag = "product-resource-crystals",
                        )
                    }
                }
                if (compactHeader) {
                    ProductHeaderActions(
                        shell = shell,
                        onOpenRewardTrack = onOpenRewardTrack,
                        onOpenSettings = onOpenSettings,
                        stacked = true,
                    )
                } else {
                    ProductHeaderActions(
                        shell = shell,
                        onOpenRewardTrack = onOpenRewardTrack,
                        onOpenSettings = onOpenSettings,
                        stacked = false,
                    )
                }
            }
        }
    }
}

@Composable
private fun ProductHeaderActions(
    shell: ProductShellUi,
    onOpenRewardTrack: () -> Unit,
    onOpenSettings: () -> Unit,
    stacked: Boolean,
) {
    if (stacked) {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ProductRewardTrackButton(
                shell = shell,
                onClick = onOpenRewardTrack,
            )
            ProductSettingsButton(
                onClick = onOpenSettings,
            )
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ProductRewardTrackButton(
                shell = shell,
                onClick = onOpenRewardTrack,
                modifier = Modifier.weight(1f),
            )
            ProductSettingsButton(onClick = onOpenSettings)
        }
    }
}

@Composable
private fun ProductRewardTrackButton(
    shell: ProductShellUi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TextButton(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = ProductMetrics.minTouchTarget)
            .testTag("open-reward-track"),
    ) {
        Text(
            text = stringResource(
                R.string.product_reward_progress,
                shell.rewardTrackExperience,
                shell.rewardTrackTarget,
            ),
            color = ProductColors.AuroraTeal,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ProductSettingsButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TextButton(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = ProductMetrics.minTouchTarget)
            .testTag("open-settings"),
    ) {
        Text(
            text = stringResource(R.string.product_settings_action),
            color = ProductColors.RelayAmber,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ResourceChip(
    text: String,
    modifier: Modifier = Modifier,
    testTag: String,
) {
    Surface(
        modifier = modifier.testTag(testTag),
        shape = MaterialTheme.shapes.small,
        color = ProductColors.DeepOrbit,
        border = BorderStroke(1.dp, ProductColors.Grid),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            color = ProductColors.TextPrimary,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ProductBottomNavigation(
    selected: ProductDestinationUi?,
    onSelect: (ProductDestinationUi) -> Unit,
) {
    val destinations = listOf(
        ProductDestinationUi.CAMPAIGN to R.string.product_nav_campaign,
        ProductDestinationUi.ROSTER to R.string.product_nav_roster,
        ProductDestinationUi.TECH to R.string.product_nav_tech,
        ProductDestinationUi.SHOP to R.string.product_nav_shop,
        ProductDestinationUi.ARENA to R.string.product_nav_arena,
    )
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .testTag("product-bottom-navigation"),
        color = ProductColors.SignalPanel,
        contentColor = ProductColors.TextPrimary,
    ) {
        BoxWithConstraints {
            val scrollable = maxWidth < 360.dp || LocalDensity.current.fontScale >= 1.5f
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (scrollable) Modifier.horizontalScroll(rememberScrollState()) else Modifier)
                    .padding(ProductMetrics.bottomBarInset),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                destinations.forEach { (destination, labelId) ->
                    val label = stringResource(labelId)
                    val isSelected = destination == selected
                    Button(
                        onClick = { onSelect(destination) },
                        modifier = Modifier
                            .then(if (scrollable) Modifier.widthIn(min = 88.dp) else Modifier.weight(1f))
                            .heightIn(min = ProductMetrics.minTouchTarget)
                            .testTag("product-nav-${destination.name.lowercase()}")
                            .semantics {
                                role = Role.Tab
                                this.selected = isSelected
                                if (isSelected) {
                                    stateDescription = "Выбрано"
                                }
                            },
                        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) {
                                ProductColors.RelayAmber.copy(alpha = 0.2f)
                            } else {
                                ProductColors.DeepOrbit
                            },
                            contentColor = if (isSelected) {
                                ProductColors.RelayAmber
                            } else {
                                ProductColors.TextSecondary
                            },
                        ),
                        shape = MaterialTheme.shapes.small,
                    ) {
                        Text(
                            text = label,
                            modifier = Modifier.testTag("product-nav-label-${destination.name.lowercase()}"),
                            style = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            softWrap = false,
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun ProductSectionHeader(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = ProductColors.TextPrimary,
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyLarge,
            color = ProductColors.TextSecondary,
        )
    }
}

@Composable
internal fun ProductCard(
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = if (highlighted) {
            ProductColors.SignalPanelRaised
        } else {
            ProductColors.SignalPanel
        },
        contentColor = ProductColors.TextPrimary,
        border = BorderStroke(
            1.dp,
            if (highlighted) ProductColors.AuroraTeal else ProductColors.Grid,
        ),
    ) {
        Box(modifier = Modifier.padding(ProductMetrics.cardPadding)) {
            content()
        }
    }
}

/** Dialog heading that keeps whole words at the system font scale of its own window. */
@Composable
internal fun ProductDialogTitle(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.headlineSmall,
    color: Color = ProductColors.TextPrimary,
    textAlign: TextAlign? = null,
) {
    val typography = MaterialTheme.typography
    WholeWordText(
        text = text,
        style = style,
        fallbackStyles = listOf(typography.titleLarge, typography.titleMedium, typography.titleSmall)
            .filter { style.fontSize.isSp && it.fontSize.isSp && it.fontSize < style.fontSize },
        modifier = modifier,
        color = color,
        textAlign = textAlign,
    )
}

/** Dialog copy that keeps whole words at the system font scale of its own window. */
@Composable
internal fun ProductDialogBody(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
    color: Color = Color.Unspecified,
    textAlign: TextAlign? = null,
) {
    val typography = MaterialTheme.typography
    WholeWordText(
        text = text,
        style = style,
        fallbackStyles = listOf(typography.bodyMedium, typography.bodySmall, typography.labelSmall)
            .filter { style.fontSize.isSp && it.fontSize.isSp && it.fontSize < style.fontSize },
        modifier = modifier,
        color = color,
        textAlign = textAlign,
    )
}

@Composable
internal fun ProductPrimaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    testTag: String? = null,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .heightIn(min = ProductMetrics.actionHeight)
            .widthIn(min = ProductMetrics.minTouchTarget)
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
        colors = ButtonDefaults.buttonColors(
            containerColor = ProductColors.RelayAmber,
            contentColor = ProductColors.Void,
            disabledContainerColor = ProductColors.SignalPanelRaised,
            disabledContentColor = ProductColors.TextMuted,
        ),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center,
        )
    }
}
