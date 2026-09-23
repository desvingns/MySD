package dev.mysd.android.product

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.mysd.android.R
import dev.mysd.android.ui.theme.ProductColors
import dev.mysd.android.ui.theme.ProductMetrics

@Composable
internal fun ProductSaveFailureBanner(onRetry: () -> Unit) {
    Surface(color = ProductColors.SignalPanelRaised, contentColor = ProductColors.WarningCoral) {
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 12.dp)
                .testTag("product-save-failed"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                stringResource(R.string.product_save_failed),
                Modifier.weight(1f),
                style = MaterialTheme.typography.labelMedium,
            )
            TextButton(
                onClick = onRetry,
                modifier = Modifier.heightIn(min = ProductMetrics.minTouchTarget).testTag("product-save-retry"),
            ) { Text(stringResource(R.string.product_save_retry)) }
        }
    }
}

@Composable
internal fun ProductRecoveryDialog(notice: ProductRecoveryNotice, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        ProductRecoveryPanel(notice, onDismiss)
    }
}

// Dialog windows take the system font scale, not an Activity-local override. Their content is
// separate from the window so compact large-font layout can be verified directly.
@Composable
internal fun ProductRecoveryPanel(notice: ProductRecoveryNotice, onDismiss: () -> Unit) {
    ProductCard(
        Modifier.fillMaxWidth().heightIn(max = ProductMetrics.panelMaxHeight)
            .testTag("product-recovery-notice"),
    ) {
        Column(
            Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ProductDialogTitle(
                stringResource(R.string.product_recovery_title),
                style = MaterialTheme.typography.titleLarge,
            )
            ProductDialogBody(stringResource(if (notice.profileRetained) {
                R.string.product_recovery_run_body
            } else {
                R.string.product_recovery_profile_body
            }))
            ProductDialogBody(stringResource(if (notice.copyArchived) {
                R.string.product_recovery_archived
            } else {
                R.string.product_recovery_archive_failed
            }))
            ProductPrimaryButton(
                stringResource(R.string.product_service_confirm), onDismiss,
                Modifier.fillMaxWidth(), testTag = "product-recovery-confirm",
            )
        }
    }
}

@Composable
fun MySdProductApp(
    state: ProductUiModel,
    onAction: (ProductUiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val exitConfirmationState = rememberSaveable(
        state.surface is ProductSurfaceUi.Battle,
    ) { mutableStateOf(false) }
    // Changing surface replaces the saveable holder. A stable host callback must not retain
    // the old holder from Setup when a new Battle is entered.
    val dispatch = remember(onAction, exitConfirmationState) {
        { action: ProductUiAction ->
            when (action) {
                ProductUiAction.RequestExitBattle -> exitConfirmationState.value = true
                ProductUiAction.CancelExitBattle -> exitConfirmationState.value = false
                ProductUiAction.ConfirmExitBattle -> {
                    exitConfirmationState.value = false
                    onAction(ProductUiAction.ConfirmExitBattle)
                }
                else -> onAction(action)
            }
        }
    }
    ProductBackHandler(
        state = state,
        exitConfirmationVisible = exitConfirmationState.value,
        onAction = dispatch,
    )
    Box(modifier = modifier.fillMaxSize().testTag("product-shell")) {
        when (val surface = state.surface) {
            is ProductSurfaceUi.Launch -> ProductLaunchScreen(surface, dispatch)
            is ProductSurfaceUi.CampaignMap -> ProductCampaignMapScreen(
                shell = state.shell,
                state = surface,
                onAction = dispatch,
            )
            is ProductSurfaceUi.Setup -> ProductSetupScreen(
                shell = state.shell,
                state = surface,
                onAction = dispatch,
            )
            is ProductSurfaceUi.Battle -> ProductBattleScreen(
                surface, dispatch, reduceMotion = state.reduceMotion,
            )
            is ProductSurfaceUi.Roster -> ProductRosterScreen(
                shell = state.shell,
                state = surface,
                onAction = dispatch,
            )
            is ProductSurfaceUi.Tech -> ProductTechScreen(
                shell = state.shell,
                state = surface,
                onAction = dispatch,
            )
            is ProductSurfaceUi.Shop -> ProductShopScreen(
                shell = state.shell,
                state = surface,
                onAction = dispatch,
            )
            is ProductSurfaceUi.RewardTrack -> ProductRewardTrackScreen(
                shell = state.shell,
                state = surface,
                onAction = dispatch,
            )
            is ProductSurfaceUi.Settings -> ProductSettingsScreen(
                shell = state.shell,
                state = surface,
                onAction = dispatch,
            )
            is ProductSurfaceUi.Arena -> ProductArenaScreen(
                shell = state.shell,
                state = surface,
                onAction = dispatch,
                reduceMotion = state.reduceMotion,
            )
        }
        when (val overlay = state.overlay) {
            null -> Unit
            is ProductOverlayUi.ResumeRun -> ProductResumeDialog(overlay, dispatch)
            is ProductOverlayUi.BattleSlot -> Unit
            is ProductOverlayUi.ServiceResult -> ProductServiceDialog(overlay, dispatch)
        }
        if (exitConfirmationState.value) {
            ProductExitConfirmationDialog(onAction = dispatch)
        }
    }
}

@Composable
private fun ProductBackHandler(
    state: ProductUiModel,
    exitConfirmationVisible: Boolean,
    onAction: (ProductUiAction) -> Unit,
) {
    if (exitConfirmationVisible) {
        BackHandler { onAction(ProductUiAction.CancelExitBattle) }
        return
    }
    val action = when (val surface = state.surface) {
        is ProductSurfaceUi.Launch -> null
        is ProductSurfaceUi.Battle -> when (surface.scene.phase) {
            BattlePhaseUi.ACTIVE,
            BattlePhaseUi.WAVE_INTRO,
            -> ProductUiAction.PauseOrResume
            BattlePhaseUi.PAUSED,
            BattlePhaseUi.INTER_WAVE_CHOICE,
            -> ProductUiAction.RequestExitBattle
            BattlePhaseUi.VICTORY,
            BattlePhaseUi.DEFEAT,
            -> if (surface.scene.result?.claimed == true) {
                ProductUiAction.BackToCampaign
            } else {
                null
            }
        }
        is ProductSurfaceUi.Setup -> ProductUiAction.BackToCampaign
        is ProductSurfaceUi.CampaignMap -> null
        else -> ProductUiAction.Navigate(ProductDestinationUi.CAMPAIGN)
    }
    BackHandler(enabled = action != null && state.overlay == null) {
        action?.let(onAction)
    }
    val unclaimedTerminal = (state.surface as? ProductSurfaceUi.Battle)?.scene?.result
        ?.let { !it.claimed } == true
    BackHandler(enabled = unclaimedTerminal && state.overlay == null) {
        // The claim-once terminal remains visible; the result is never abandoned by system Back.
    }
}

@Composable
private fun ProductExitConfirmationDialog(
    onAction: (ProductUiAction) -> Unit,
) {
    Dialog(
        onDismissRequest = { onAction(ProductUiAction.CancelExitBattle) },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        ProductExitConfirmationOverlay(onAction)
    }
}

@Composable
internal fun ProductExitConfirmationOverlay(
    onAction: (ProductUiAction) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ProductColors.Scrim)
            .testTag("product-exit-confirmation"),
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
                ProductDialogTitle(
                    text = stringResource(R.string.product_exit_confirm_title),
                    textAlign = TextAlign.Center,
                )
                ProductDialogBody(
                    text = stringResource(R.string.product_exit_confirm_body),
                    color = ProductColors.TextSecondary,
                    textAlign = TextAlign.Center,
                )
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ProductPrimaryButton(
                        label = stringResource(R.string.product_exit_cancel),
                        onClick = { onAction(ProductUiAction.CancelExitBattle) },
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "product-exit-cancel",
                    )
                    TextButton(
                        onClick = { onAction(ProductUiAction.ConfirmExitBattle) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = ProductMetrics.actionHeight)
                            .testTag("product-exit-confirm"),
                    ) {
                        Text(
                            text = stringResource(R.string.product_exit_confirm),
                            color = ProductColors.WarningCoral,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductResumeDialog(
    state: ProductOverlayUi.ResumeRun,
    onAction: (ProductUiAction) -> Unit,
) {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
        ),
    ) {
        ProductResumeOverlay(state, onAction)
    }
}

@Composable
internal fun ProductResumeOverlay(
    state: ProductOverlayUi.ResumeRun,
    onAction: (ProductUiAction) -> Unit,
) {
    val panelDescription = stringResource(R.string.campaign_unfinished_panel_description)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ProductColors.Scrim)
            .semantics { contentDescription = panelDescription }
            .testTag("resume-overlay"),
        contentAlignment = Alignment.Center,
    ) {
        ProductCard(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = ProductMetrics.panelMaxWidth)
                .heightIn(max = ProductMetrics.panelMaxHeight)
                .padding(ProductMetrics.screenInset)
                .testTag("resume-panel"),
            highlighted = true,
        ) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ProductDialogTitle(text = stringResource(R.string.campaign_unfinished_title))
                ProductDialogBody(
                    text = stringResource(R.string.campaign_unfinished_body),
                    color = ProductColors.TextSecondary,
                )
                ProductDialogBody(
                    text = state.stageTitle,
                    color = ProductColors.AuroraTeal,
                )
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TextButton(
                        onClick = { onAction(ProductUiAction.DiscardRun) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = ProductMetrics.actionHeight)
                            .testTag("resume-cancel-action"),
                    ) {
                        Text(
                            text = stringResource(R.string.campaign_cancel_action),
                            color = ProductColors.WarningCoral,
                        )
                    }
                    ProductPrimaryButton(
                        label = stringResource(R.string.campaign_continue_action),
                        onClick = { onAction(ProductUiAction.ResumeRun) },
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "resume-continue-action",
                    )
                }
            }
        }
    }
}

@Composable
private fun ProductServiceDialog(
    state: ProductOverlayUi.ServiceResult,
    onAction: (ProductUiAction) -> Unit,
) {
    Dialog(
        onDismissRequest = { onAction(ProductUiAction.CloseOverlay) },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        ProductServiceOverlay(state, onAction)
    }
}

@Composable
internal fun ProductServiceOverlay(
    state: ProductOverlayUi.ServiceResult,
    onAction: (ProductUiAction) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ProductColors.Scrim)
            .testTag("product-service-dialog"),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = ProductMetrics.panelMaxWidth)
                .heightIn(max = ProductMetrics.panelMaxHeight)
                .padding(ProductMetrics.screenInset),
            color = ProductColors.SignalPanel,
            contentColor = ProductColors.TextPrimary,
            shape = MaterialTheme.shapes.extraLarge,
            border = androidx.compose.foundation.BorderStroke(1.dp, ProductColors.AuroraTeal),
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(ProductMetrics.cardPadding),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.product_local_only),
                    style = MaterialTheme.typography.labelLarge,
                    color = ProductColors.AuroraTeal,
                )
                ProductDialogTitle(
                    text = stringResource(R.string.product_service_title),
                    modifier = Modifier.testTag("product-service-title"),
                    textAlign = TextAlign.Center,
                )
                ProductDialogTitle(
                    text = state.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = ProductColors.RelayAmber,
                    textAlign = TextAlign.Center,
                )
                ProductDialogBody(
                    text = stringResource(
                        when (state.kind) {
                            ServiceResultKindUi.REWARDED -> R.string.product_service_rewarded_body
                            ServiceResultKindUi.PURCHASE -> R.string.product_service_purchase_body
                            ServiceResultKindUi.ARENA -> R.string.product_service_arena_body
                        },
                    ),
                    color = ProductColors.TextSecondary,
                    textAlign = TextAlign.Center,
                )
                ProductPrimaryButton(
                    label = stringResource(R.string.product_service_confirm),
                    onClick = { onAction(ProductUiAction.CloseOverlay) },
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "product-service-confirm",
                )
            }
        }
    }
}
