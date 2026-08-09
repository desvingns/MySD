package dev.mysd.android.campaign

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.DialogProperties
import dev.mysd.android.R
import dev.mysd.android.ui.theme.LocalSpacing
import dev.mysd.game.battle.ActiveBattleIntent
import dev.mysd.game.battle.ActiveBattleSnapshot
import dev.mysd.game.battle.ActiveBattleSpeedIndicator
import dev.mysd.game.battle.EnhancementIntent
import dev.mysd.game.battle.EnhancementOffer
import dev.mysd.game.battle.EnhancementSnapshot
import dev.mysd.game.battle.VictorySnapshot
import dev.mysd.game.campaign.CampaignIntent
import dev.mysd.game.campaign.CampaignRoute
import dev.mysd.game.campaign.CampaignSnapshot
import dev.mysd.game.campaign.CampaignStageId
import dev.mysd.game.campaign.BattleSetupChoice
import dev.mysd.game.campaign.BattleSetupSnapshot
import dev.mysd.game.campaign.BattleStartTransition
import dev.mysd.game.campaign.LevelSetupOrigin
import dev.mysd.game.content.OriginalContentIds

@Composable
fun CampaignScreen(
    state: CampaignSnapshot,
    onIntent: (CampaignIntent) -> Unit,
    onActiveBattleIntent: (ActiveBattleIntent) -> Unit = {},
    onEnhancementIntent: (EnhancementIntent) -> Unit = {},
    modifier: Modifier = Modifier,
    battleSetup: BattleSetupSnapshot? = null,
    activeBattle: ActiveBattleSnapshot? = null,
    enhancement: EnhancementSnapshot? = null,
    victory: VictorySnapshot? = null,
) {
    CampaignScreenContent(
        state = state,
        onIntent = onIntent,
        onActiveBattleIntent = onActiveBattleIntent,
        onEnhancementIntent = onEnhancementIntent,
        modifier = modifier,
        battleSetup = battleSetup,
        activeBattle = activeBattle,
        enhancement = enhancement,
        victory = victory,
    )
}

@Composable
fun CampaignScreenContent(
    state: CampaignSnapshot,
    onIntent: (CampaignIntent) -> Unit,
    onActiveBattleIntent: (ActiveBattleIntent) -> Unit = {},
    onEnhancementIntent: (EnhancementIntent) -> Unit = {},
    modifier: Modifier = Modifier,
    battleSetup: BattleSetupSnapshot? = null,
    activeBattle: ActiveBattleSnapshot? = null,
    enhancement: EnhancementSnapshot? = null,
    victory: VictorySnapshot? = null,
) {
    val battleStart = state.battleStart
    if (battleStart != null) {
        if (victory != null) {
            VictoryContent(
                state = victory,
                modifier = modifier,
            )
        } else if (enhancement != null && !enhancement.returnToBattle) {
            EnhancementContent(
                state = enhancement,
                onIntent = onEnhancementIntent,
                modifier = modifier,
            )
        } else if (activeBattle != null) {
            ActiveBattleContent(
                state = activeBattle,
                onIntent = onActiveBattleIntent,
                modifier = modifier,
            )
        } else {
            BattleStartContent(
                transition = battleStart,
                modifier = modifier,
            )
        }
    } else {
        when (state.route) {
            CampaignRoute.CLEAN_LAUNCH -> CampaignRouteContent(
                title = stringResource(R.string.campaign_launch_title),
                body = stringResource(R.string.campaign_launch_body),
                actionLabel = stringResource(R.string.campaign_enter_action),
                titleColor = MaterialTheme.colorScheme.primary,
                onAction = { onIntent(CampaignIntent.EnterCampaign) },
                modifier = modifier,
            )

            CampaignRoute.CAMPAIGN_SELECTION -> CampaignSelectionContent(
                stageIds = state.acceptedStageIds,
                onSelectStage = { stageId -> onIntent(CampaignIntent.SelectLevel(stageId)) },
                modifier = modifier,
            )

            CampaignRoute.LEVEL_SETUP -> if (battleSetup != null) {
                BattleSetupContent(
                    state = battleSetup,
                    onIntent = onIntent,
                    modifier = modifier,
                )
            } else {
                CampaignRouteContent(
                    title = stringResource(R.string.campaign_level_setup_title),
                    body = stageTitle(requireNotNull(state.selectedStageId)),
                    detail = stringResource(
                        when (requireNotNull(state.setupOrigin)) {
                            LevelSetupOrigin.NEW_RUN -> R.string.campaign_new_setup_body
                            LevelSetupOrigin.UNFINISHED_RUN -> R.string.campaign_unfinished_setup_body
                        },
                    ),
                    titleColor = MaterialTheme.colorScheme.tertiary,
                    modifier = modifier,
                )
            }
        }
    }

    if (state.unfinishedRunPromptVisible) {
        UnfinishedRunPrompt(
            onCancel = { onIntent(CampaignIntent.CancelUnfinishedRun) },
            onContinue = { onIntent(CampaignIntent.ContinueUnfinishedRun) },
        )
    }
}

@Composable
fun ActiveBattleContent(
    state: ActiveBattleSnapshot,
    onIntent: (ActiveBattleIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalSpacing.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(spacing.l),
        verticalArrangement = Arrangement.spacedBy(spacing.m, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.active_battle_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = stringResource(R.string.active_battle_stage, stageTitle(state.stageId)),
            style = MaterialTheme.typography.bodyLarge,
        )
        if (state.waveActive) {
            Text(
                text = stringResource(R.string.active_battle_wave_activity),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
        if (state.baseVisible) {
            Text(
                text = stringResource(R.string.active_battle_base_visible),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        if (state.enemyEntitiesVisible) {
            Text(
                text = stringResource(
                    R.string.active_battle_enemies_visible,
                    state.enemyEntityIds.size,
                ),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        if (state.speedAffordanceVisible) {
            Button(onClick = { onIntent(ActiveBattleIntent.ChangeSpeed) }) {
                Text(
                    text = stringResource(
                        R.string.active_battle_speed,
                        speedIndicatorLabel(state.speedIndicator),
                    ),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
        if (state.pauseResumeAffordanceVisible) {
            Button(onClick = { onIntent(ActiveBattleIntent.PauseOrResume) }) {
                Text(
                    text = stringResource(
                        if (state.paused) {
                            R.string.active_battle_resume_action
                        } else {
                            R.string.active_battle_pause_action
                        },
                    ),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
        if (state.buildAffordanceVisible) {
            Button(onClick = { onIntent(ActiveBattleIntent.SelectBuildAffordance) }) {
                Text(
                    text = stringResource(
                        if (state.buildAffordanceSelected) {
                            R.string.active_battle_build_selected
                        } else {
                            R.string.active_battle_build_action
                        },
                    ),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
        if (state.enhancementAffordanceVisible && !state.enhancementChoiceVisible) {
            Button(onClick = { onIntent(ActiveBattleIntent.OpenEnhancement) }) {
                Text(
                    text = stringResource(R.string.active_battle_enhancement_action),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
        if (state.victoryResolutionAffordanceVisible && !state.enhancementChoiceVisible) {
            Button(onClick = { onIntent(ActiveBattleIntent.ResolveVictory) }) {
                Text(
                    text = stringResource(R.string.active_battle_victory_action),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

@Composable
fun VictoryContent(
    state: VictorySnapshot,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalSpacing.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(spacing.l),
        verticalArrangement = Arrangement.spacedBy(spacing.m, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.victory_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = stringResource(R.string.active_battle_stage, stageTitle(state.stageId)),
            style = MaterialTheme.typography.bodyLarge,
        )
        if (state.rewardPanelVisible) {
            Text(
                text = stringResource(R.string.victory_reward_panel_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
            Text(
                text = stringResource(R.string.victory_reward_panel_body),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@Composable
fun EnhancementContent(
    state: EnhancementSnapshot,
    onIntent: (EnhancementIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalSpacing.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(spacing.l),
        verticalArrangement = Arrangement.spacedBy(spacing.m, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.enhancement_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = stringResource(R.string.enhancement_body),
            style = MaterialTheme.typography.bodyLarge,
        )
        if (state.allFilterVisible) {
            Text(
                text = stringResource(R.string.enhancement_filter_all),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
        state.offers.forEach { offer ->
            EnhancementOfferContent(
                offer = offer,
                selected = state.selectedOfferId == offer.id,
                onSelect = { onIntent(EnhancementIntent.SelectOffer(offer.id)) },
            )
        }
        if (state.refreshAffordanceVisible) {
            Button(onClick = { onIntent(EnhancementIntent.RefreshOffers) }) {
                Text(
                    text = stringResource(
                        R.string.enhancement_refresh_action,
                        state.refreshRevision,
                    ),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

@Composable
private fun EnhancementOfferContent(
    offer: EnhancementOffer,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    Button(onClick = onSelect) {
        Text(
            text = stringResource(
                if (selected) {
                    R.string.enhancement_offer_selected
                } else {
                    R.string.enhancement_offer_action
                },
                enhancementLabel(offer),
            ),
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
fun BattleSetupContent(
    state: BattleSetupSnapshot,
    onIntent: (CampaignIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalSpacing.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(spacing.l),
        verticalArrangement = Arrangement.spacedBy(spacing.m, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.battle_setup_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.tertiary,
        )
        Text(
            text = stageTitle(state.stageId),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.secondary,
        )
        Text(
            text = stringResource(R.string.battle_setup_body),
            style = MaterialTheme.typography.bodyLarge,
        )
        state.availableChoices.forEach { choice ->
            Button(
                onClick = { onIntent(CampaignIntent.SelectInitialOption(choice)) },
                shape = MaterialTheme.shapes.medium,
            ) {
                Text(
                    text = choiceLabel(choice),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
        state.selectedChoice?.let { choice ->
            Text(
                text = stringResource(R.string.battle_setup_selected, choiceLabel(choice)),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
        if (state.tutorialContinuationVisible) {
            Button(
                onClick = { onIntent(CampaignIntent.ContinueTutorialSetup) },
                shape = MaterialTheme.shapes.medium,
            ) {
                Text(
                    text = stringResource(R.string.battle_setup_continue_action),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
        if (state.setupCompleted) {
            Text(
                text = stringResource(R.string.battle_setup_ready_body),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Button(
                onClick = { onIntent(CampaignIntent.StartBattle) },
                shape = MaterialTheme.shapes.medium,
            ) {
                Text(
                    text = stringResource(R.string.battle_start_action),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

@Composable
private fun BattleStartContent(
    transition: BattleStartTransition,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalSpacing.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(spacing.l),
        verticalArrangement = Arrangement.spacedBy(spacing.m, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.battle_started_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = stageTitle(transition.stageId),
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            text = stringResource(R.string.battle_started_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.secondary,
        )
    }
}

@Composable
private fun CampaignSelectionContent(
    stageIds: List<CampaignStageId>,
    onSelectStage: (CampaignStageId) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalSpacing.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(spacing.l),
        verticalArrangement = Arrangement.spacedBy(spacing.l, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.campaign_selection_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.secondary,
        )
        stageIds.forEach { stageId ->
            Text(text = stageTitle(stageId), style = MaterialTheme.typography.titleLarge)
            Text(
                text = stringResource(R.string.campaign_level_body),
                style = MaterialTheme.typography.bodyLarge,
            )
            Button(onClick = { onSelectStage(stageId) }) {
                Text(
                    text = stringResource(R.string.campaign_level_setup_action),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

@Composable
private fun CampaignRouteContent(
    title: String,
    body: String,
    detail: String? = null,
    actionLabel: String? = null,
    titleColor: androidx.compose.ui.graphics.Color,
    onAction: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val spacing = LocalSpacing.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(spacing.l),
        verticalArrangement = Arrangement.spacedBy(spacing.l, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = titleColor,
        )
        Text(text = body, style = MaterialTheme.typography.titleLarge)
        detail?.let {
            Text(text = it, style = MaterialTheme.typography.bodyLarge)
        }
        actionLabel?.let {
            Button(onClick = onAction) {
                Text(text = it, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun UnfinishedRunPrompt(
    onCancel: () -> Unit,
    onContinue: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = {
            Text(
                text = stringResource(R.string.campaign_unfinished_title),
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Text(
                text = stringResource(R.string.campaign_unfinished_body),
                style = MaterialTheme.typography.bodyLarge,
            )
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text(
                    text = stringResource(R.string.campaign_cancel_action),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        },
        confirmButton = {
            Button(onClick = onContinue) {
                Text(
                    text = stringResource(R.string.campaign_continue_action),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
        ),
        shape = MaterialTheme.shapes.medium,
    )
}

@Composable
private fun stageTitle(stageId: CampaignStageId): String = when (stageId) {
    dev.mysd.game.campaign.AcceptedCampaignFixture.STAGE_ID -> {
        stringResource(R.string.campaign_level_ember_path)
    }

    else -> stageId.value
}

@Composable
private fun choiceLabel(choice: BattleSetupChoice): String = when (choice) {
    BattleSetupChoice.OPTION_A -> stringResource(R.string.battle_setup_choice_a)
    BattleSetupChoice.OPTION_B -> stringResource(R.string.battle_setup_choice_b)
    BattleSetupChoice.OPTION_C -> stringResource(R.string.battle_setup_choice_c)
}

@Composable
private fun speedIndicatorLabel(indicator: ActiveBattleSpeedIndicator): String = when (indicator) {
    ActiveBattleSpeedIndicator.DEFAULT -> stringResource(R.string.active_battle_speed_default)
    ActiveBattleSpeedIndicator.ALTERNATE -> stringResource(R.string.active_battle_speed_alternate)
}

@Composable
private fun enhancementLabel(offer: EnhancementOffer): String = when (offer.id) {
    OriginalContentIds.FOUNDATION_ENHANCEMENT -> {
        stringResource(R.string.enhancement_offer_steady_pulse)
    }

    OriginalContentIds.FOUNDATION_ENHANCEMENT_EMBER_WARD -> {
        stringResource(R.string.enhancement_offer_ember_ward)
    }

    else -> stringResource(R.string.enhancement_offer_unknown)
}
