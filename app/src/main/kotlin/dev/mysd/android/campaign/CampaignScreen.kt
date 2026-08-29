package dev.mysd.android.campaign

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import dev.mysd.android.R
import dev.mysd.android.ui.theme.LaunchAccent
import dev.mysd.android.ui.theme.LaunchBackground
import dev.mysd.android.ui.theme.LaunchBackgroundMid
import dev.mysd.android.ui.theme.LaunchGlow
import dev.mysd.android.ui.theme.LaunchHorizon
import dev.mysd.android.ui.theme.LaunchOnBackground
import dev.mysd.android.ui.theme.LaunchOnPanel
import dev.mysd.android.ui.theme.LaunchPanel
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
import dev.mysd.game.meta.RosterIntent
import dev.mysd.game.meta.RosterSettingId
import dev.mysd.game.meta.RosterSettingOption
import dev.mysd.game.meta.RosterSnapshot
import dev.mysd.game.meta.RosterSurface
import dev.mysd.game.meta.RosterTroopSlot
import dev.mysd.game.service.ArenaLocalState
import dev.mysd.game.service.ArenaSnapshot

@Composable
fun CampaignScreen(
    state: CampaignSnapshot,
    onIntent: (CampaignIntent) -> Unit,
    onActiveBattleIntent: (ActiveBattleIntent) -> Unit = {},
    onEnhancementIntent: (EnhancementIntent) -> Unit = {},
    onRosterIntent: (RosterIntent) -> Unit = {},
    modifier: Modifier = Modifier,
    battleSetup: BattleSetupSnapshot? = null,
    activeBattle: ActiveBattleSnapshot? = null,
    enhancement: EnhancementSnapshot? = null,
    victory: VictorySnapshot? = null,
    roster: RosterSnapshot? = null,
    arena: ArenaSnapshot? = null,
) {
    CampaignScreenContent(
        state = state,
        onIntent = onIntent,
        onActiveBattleIntent = onActiveBattleIntent,
        onEnhancementIntent = onEnhancementIntent,
        onRosterIntent = onRosterIntent,
        modifier = modifier,
        battleSetup = battleSetup,
        activeBattle = activeBattle,
        enhancement = enhancement,
        victory = victory,
        roster = roster,
        arena = arena,
    )
}

@Composable
fun CampaignScreenContent(
    state: CampaignSnapshot,
    onIntent: (CampaignIntent) -> Unit,
    onActiveBattleIntent: (ActiveBattleIntent) -> Unit = {},
    onEnhancementIntent: (EnhancementIntent) -> Unit = {},
    onRosterIntent: (RosterIntent) -> Unit = {},
    modifier: Modifier = Modifier,
    battleSetup: BattleSetupSnapshot? = null,
    activeBattle: ActiveBattleSnapshot? = null,
    enhancement: EnhancementSnapshot? = null,
    victory: VictorySnapshot? = null,
    roster: RosterSnapshot? = null,
    arena: ArenaSnapshot? = null,
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
        if (state.arenaOpen && arena != null) {
            ArenaContent(
                state = arena,
                onCloseArena = { onIntent(CampaignIntent.CloseArena) },
                modifier = modifier,
            )
        } else if (state.rosterOpen && roster != null) {
            RosterContent(
                state = roster,
                onIntent = onRosterIntent,
                onCloseRoster = { onIntent(CampaignIntent.CloseRoster) },
                modifier = modifier,
            )
        } else when (state.route) {
            CampaignRoute.CLEAN_LAUNCH -> CampaignLaunchContent(
                title = stringResource(R.string.campaign_launch_title),
                body = stringResource(R.string.campaign_launch_body),
                actionLabel = stringResource(R.string.campaign_enter_action),
                actionHint = stringResource(R.string.campaign_launch_action_hint),
                onAction = { onIntent(CampaignIntent.EnterCampaign) },
                modifier = modifier,
            )

            CampaignRoute.CAMPAIGN_SELECTION -> CampaignSelectionContent(
                stageIds = state.acceptedStageIds,
                onSelectStage = { stageId -> onIntent(CampaignIntent.SelectLevel(stageId)) },
                onOpenRoster = { onIntent(CampaignIntent.OpenRoster) },
                onOpenArena = { onIntent(CampaignIntent.OpenArena) },
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
fun ArenaContent(
    state: ArenaSnapshot,
    onCloseArena: () -> Unit,
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
            text = stringResource(R.string.arena_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = stringResource(
                if (state.localState == ArenaLocalState.LOCAL_SERVICE_SHAPED) {
                    R.string.arena_local_body
                } else {
                    R.string.arena_blocked_body
                },
            ),
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            text = stringResource(R.string.arena_match_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.secondary,
        )
        TextButton(onClick = onCloseArena) {
            Text(
                text = stringResource(R.string.arena_close_action),
                style = MaterialTheme.typography.labelLarge,
            )
        }
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
    onOpenRoster: () -> Unit,
    onOpenArena: () -> Unit,
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
        Button(onClick = onOpenRoster) {
            Text(
                text = stringResource(R.string.campaign_roster_action),
                style = MaterialTheme.typography.labelLarge,
            )
        }
        Button(onClick = onOpenArena) {
            Text(
                text = stringResource(R.string.campaign_arena_action),
                style = MaterialTheme.typography.labelLarge,
            )
        }
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
fun RosterContent(
    state: RosterSnapshot,
    onIntent: (RosterIntent) -> Unit,
    onCloseRoster: () -> Unit,
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
            text = stringResource(R.string.roster_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = stringResource(R.string.roster_body),
            style = MaterialTheme.typography.bodyLarge,
        )
        state.troopSlots.forEach { slot ->
            RosterTroopContent(
                slot = slot,
                onUpgrade = { onIntent(RosterIntent.UpgradeTroop(slot.id)) },
            )
        }
        if (state.surface == RosterSurface.TROOPS) {
            Button(onClick = { onIntent(RosterIntent.OpenSettings) }) {
                Text(
                    text = stringResource(R.string.roster_settings_action),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
        TextButton(onClick = onCloseRoster) {
            Text(
                text = stringResource(R.string.roster_close_action),
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }

    if (state.surface == RosterSurface.SETTINGS) {
        AlertDialog(
            onDismissRequest = { onIntent(RosterIntent.CloseSettings) },
            title = {
                Text(
                    text = stringResource(R.string.settings_title),
                    style = MaterialTheme.typography.titleLarge,
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.m)) {
                    Text(
                        text = stringResource(R.string.settings_body),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    state.settings.forEach { option ->
                        RosterSettingContent(
                            option = option,
                            onToggle = { onIntent(RosterIntent.ToggleSetting(option.id)) },
                        )
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { onIntent(RosterIntent.CloseSettings) }) {
                    Text(
                        text = stringResource(R.string.settings_close_action),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            },
            confirmButton = {
                Button(onClick = { onIntent(RosterIntent.ConfirmSettings) }) {
                    Text(
                        text = stringResource(R.string.settings_confirm_action),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            },
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = false,
            ),
            shape = MaterialTheme.shapes.medium,
        )
    }
}

@Composable
private fun RosterTroopContent(
    slot: RosterTroopSlot,
    onUpgrade: () -> Unit,
) {
    Text(
        text = troopLabel(slot.id),
        style = MaterialTheme.typography.titleMedium,
    )
    if (slot.upgradeAffordanceVisible) {
        Button(onClick = onUpgrade) {
            Text(
                text = stringResource(R.string.roster_upgrade_action),
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@Composable
private fun RosterSettingContent(
    option: RosterSettingOption,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = settingLabel(option.id),
            style = MaterialTheme.typography.bodyLarge,
        )
        if (option.toggleAffordanceVisible) {
            Switch(
                checked = false,
                onCheckedChange = { onToggle() },
            )
        }
    }
}

@Composable
private fun CampaignLaunchContent(
    title: String,
    body: String,
    actionLabel: String,
    actionHint: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalSpacing.current
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(LaunchBackground, LaunchBackgroundMid, LaunchHorizon),
                ),
            ),
    ) {
        LaunchBackdrop(modifier = Modifier.matchParentSize())
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = spacing.l, vertical = spacing.m),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(LaunchAccent, CircleShape),
                )
                Spacer(modifier = Modifier.size(spacing.s))
                Text(
                    text = stringResource(R.string.campaign_launch_kicker),
                    style = MaterialTheme.typography.labelLarge,
                    color = LaunchOnBackground.copy(alpha = 0.82f),
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.displaySmall,
                    color = LaunchAccent,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.size(spacing.s))
                Text(
                    text = body,
                    style = MaterialTheme.typography.headlineSmall,
                    color = LaunchOnBackground,
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp),
                shape = MaterialTheme.shapes.large,
                color = LaunchPanel,
                tonalElevation = 8.dp,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = spacing.m, vertical = spacing.s),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = actionHint,
                        style = MaterialTheme.typography.labelLarge,
                        color = LaunchOnPanel.copy(alpha = 0.78f),
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.size(spacing.xs))
                    Button(
                        onClick = onAction,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LaunchAccent,
                            contentColor = LaunchBackground,
                        ),
                        shape = MaterialTheme.shapes.medium,
                    ) {
                        Text(
                            text = actionLabel,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LaunchBackdrop(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val orbitCenter = Offset(size.width * 0.5f, size.height * 0.39f)
        val horizonTop = size.height * 0.72f
        val orbitRadius = size.minDimension * 0.23f

        drawCircle(
            color = LaunchGlow.copy(alpha = 0.08f),
            radius = orbitRadius * 1.35f,
            center = orbitCenter,
        )
        drawCircle(
            color = LaunchGlow.copy(alpha = 0.13f),
            radius = orbitRadius,
            center = orbitCenter,
        )
        drawCircle(
            color = LaunchAccent.copy(alpha = 0.92f),
            radius = orbitRadius * 0.36f,
            center = orbitCenter,
        )
        drawCircle(
            color = LaunchOnBackground.copy(alpha = 0.72f),
            radius = orbitRadius * 0.36f,
            center = orbitCenter,
            style = Stroke(width = 2.dp.toPx()),
        )

        val orbitPath = Path().apply {
            moveTo(size.width * 0.08f, size.height * 0.47f)
            cubicTo(
                size.width * 0.28f,
                size.height * 0.29f,
                size.width * 0.72f,
                size.height * 0.50f,
                size.width * 0.94f,
                size.height * 0.31f,
            )
        }
        drawPath(
            path = orbitPath,
            color = LaunchGlow.copy(alpha = 0.52f),
            style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round),
        )

        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    LaunchHorizon.copy(alpha = 0.04f),
                    LaunchHorizon.copy(alpha = 0.78f),
                ),
                startY = horizonTop,
                endY = size.height,
            ),
            topLeft = Offset(0f, horizonTop),
            size = Size(size.width, size.height - horizonTop),
        )
        for (index in 0..6) {
            val x = size.width * (0.08f + index * 0.15f)
            val height = size.height * (0.035f + (index % 3) * 0.018f)
            drawRoundRect(
                color = LaunchGlow.copy(alpha = 0.11f),
                topLeft = Offset(x, horizonTop - height),
                size = Size(size.width * 0.07f, height),
            )
        }

        val stars = listOf(
            0.12f to 0.16f,
            0.24f to 0.27f,
            0.78f to 0.17f,
            0.88f to 0.27f,
            0.69f to 0.34f,
            0.08f to 0.58f,
            0.92f to 0.56f,
        )
        stars.forEachIndexed { index, (x, y) ->
            drawCircle(
                color = if (index % 2 == 0) LaunchAccent else LaunchOnBackground,
                radius = (1.5f + index % 3) * density,
                center = Offset(size.width * x, size.height * y),
            )
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

@Composable
private fun troopLabel(id: dev.mysd.game.content.ContentId): String = when (id) {
    OriginalContentIds.FOUNDATION_UNIT -> stringResource(R.string.roster_troop_bright_mote)
    else -> stringResource(R.string.roster_troop_unknown)
}

@Composable
private fun settingLabel(id: RosterSettingId): String = when (id) {
    RosterSettingId.AUDIO -> stringResource(R.string.settings_audio_option)
    RosterSettingId.HAPTICS -> stringResource(R.string.settings_haptics_option)
}
