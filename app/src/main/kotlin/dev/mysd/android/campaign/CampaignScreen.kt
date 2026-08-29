package dev.mysd.android.campaign

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.mysd.android.R
import dev.mysd.android.ui.theme.BattleAction
import dev.mysd.android.ui.theme.BattleBackground
import dev.mysd.android.ui.theme.BattleBase
import dev.mysd.android.ui.theme.BattleEnemy
import dev.mysd.android.ui.theme.BattleFieldMid
import dev.mysd.android.ui.theme.BattleHorizon
import dev.mysd.android.ui.theme.BattleHud
import dev.mysd.android.ui.theme.BattleMetrics
import dev.mysd.android.ui.theme.BattleOnBackground
import dev.mysd.android.ui.theme.CampaignAccent
import dev.mysd.android.ui.theme.CampaignBackground
import dev.mysd.android.ui.theme.CampaignDisabled
import dev.mysd.android.ui.theme.CampaignMetrics
import dev.mysd.android.ui.theme.CampaignOnBackground
import dev.mysd.android.ui.theme.CampaignOnSurface
import dev.mysd.android.ui.theme.CampaignSupport
import dev.mysd.android.ui.theme.CampaignSurface
import dev.mysd.android.ui.theme.BattleOnHud
import dev.mysd.android.ui.theme.LaunchAccent
import dev.mysd.android.ui.theme.LaunchBackground
import dev.mysd.android.ui.theme.LaunchBackgroundMid
import dev.mysd.android.ui.theme.LaunchGlow
import dev.mysd.android.ui.theme.LaunchHorizon
import dev.mysd.android.ui.theme.LaunchOnBackground
import dev.mysd.android.ui.theme.LaunchOnPanel
import dev.mysd.android.ui.theme.LaunchPanel
import dev.mysd.android.ui.theme.LocalSpacing
import dev.mysd.android.ui.theme.RosterAccent
import dev.mysd.android.ui.theme.RosterBackground
import dev.mysd.android.ui.theme.RosterCard
import dev.mysd.android.ui.theme.RosterDisabled
import dev.mysd.android.ui.theme.RosterMetrics
import dev.mysd.android.ui.theme.RosterOnBackground
import dev.mysd.android.ui.theme.RosterOnSurface
import dev.mysd.android.ui.theme.RosterRouteInactive
import dev.mysd.android.ui.theme.RosterSupport
import dev.mysd.android.ui.theme.RosterSurface as RosterSurfaceColor
import dev.mysd.android.ui.theme.SettingsConfirmAction
import dev.mysd.android.ui.theme.SettingsCloseAction
import dev.mysd.android.ui.theme.SettingsMetrics
import dev.mysd.android.ui.theme.SettingsOnPanel
import dev.mysd.android.ui.theme.SettingsOverlayScrim
import dev.mysd.android.ui.theme.SettingsPanel
import dev.mysd.android.ui.theme.SettingsPanelBorder
import dev.mysd.android.ui.theme.SettingsSwitchThumb
import dev.mysd.android.ui.theme.SettingsSwitchTrack
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
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BattleBackground),
    ) {
        ActiveBattleFieldBackdrop(
            state = state,
            modifier = Modifier.matchParentSize(),
        )
        ActiveBattleHud(
            state = state,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .systemBarsPadding()
                .padding(BattleMetrics.hudInset),
        )
        ActiveBattleEdgeControls(
            state = state,
            onIntent = onIntent,
            contentPadding = PaddingValues(
                horizontal = BattleMetrics.hudInset,
                vertical = BattleMetrics.hudInset,
            ),
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxSize()
                .systemBarsPadding(),
            buttonHorizontalPadding = spacing.s,
            buttonVerticalPadding = spacing.xs,
        )
    }
}

@Composable
private fun ActiveBattleHud(
    state: ActiveBattleSnapshot,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalSpacing.current
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = BattleHud,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.m, vertical = spacing.s),
            verticalArrangement = Arrangement.spacedBy(spacing.xs),
        ) {
            Text(
                text = stringResource(R.string.active_battle_title),
                style = MaterialTheme.typography.titleLarge,
                color = BattleOnHud,
            )
            Text(
                text = stringResource(R.string.active_battle_stage, stageTitle(state.stageId)),
                style = MaterialTheme.typography.bodyLarge,
                color = BattleOnHud.copy(alpha = 0.86f),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing.s),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (state.waveActive) {
                    Text(
                        text = stringResource(R.string.active_battle_wave_activity),
                        style = MaterialTheme.typography.labelLarge,
                        color = BattleAction,
                    )
                }
                if (state.enemyEntitiesVisible) {
                    Text(
                        text = stringResource(
                            R.string.active_battle_enemies_visible,
                            state.enemyEntityIds.size,
                        ),
                        style = MaterialTheme.typography.labelLarge,
                        color = BattleOnHud.copy(alpha = 0.76f),
                    )
                }
            }
        }
    }
}

@Composable
private fun ActiveBattleEdgeControls(
    state: ActiveBattleSnapshot,
    onIntent: (ActiveBattleIntent) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    buttonHorizontalPadding: androidx.compose.ui.unit.Dp,
    buttonVerticalPadding: androidx.compose.ui.unit.Dp,
) {
    Box(
        modifier = modifier.padding(contentPadding),
    ) {
        Column(
            modifier = Modifier.align(Alignment.CenterStart),
            verticalArrangement = Arrangement.spacedBy(BattleMetrics.controlGap),
            horizontalAlignment = Alignment.Start,
        ) {
            if (state.speedAffordanceVisible) {
                BattleEdgeButton(
                    label = stringResource(
                        R.string.active_battle_speed,
                        speedIndicatorLabel(state.speedIndicator),
                    ),
                    onClick = { onIntent(ActiveBattleIntent.ChangeSpeed) },
                    horizontalPadding = buttonHorizontalPadding,
                    verticalPadding = buttonVerticalPadding,
                )
            }
            if (state.pauseResumeAffordanceVisible) {
                BattleEdgeButton(
                    label = stringResource(
                        if (state.paused) {
                            R.string.active_battle_resume_action
                        } else {
                            R.string.active_battle_pause_action
                        },
                    ),
                    onClick = { onIntent(ActiveBattleIntent.PauseOrResume) },
                    horizontalPadding = buttonHorizontalPadding,
                    verticalPadding = buttonVerticalPadding,
                )
            }
        }

        Column(
            modifier = Modifier.align(Alignment.CenterEnd),
            verticalArrangement = Arrangement.spacedBy(BattleMetrics.controlGap),
            horizontalAlignment = Alignment.End,
        ) {
            if (state.buildAffordanceVisible) {
                BattleEdgeButton(
                    label = stringResource(
                        if (state.buildAffordanceSelected) {
                            R.string.active_battle_build_selected
                        } else {
                            R.string.active_battle_build_action
                        },
                    ),
                    onClick = { onIntent(ActiveBattleIntent.SelectBuildAffordance) },
                    horizontalPadding = buttonHorizontalPadding,
                    verticalPadding = buttonVerticalPadding,
                )
            }
            if (state.enhancementAffordanceVisible && !state.enhancementChoiceVisible) {
                BattleEdgeButton(
                    label = stringResource(R.string.active_battle_enhancement_action),
                    onClick = { onIntent(ActiveBattleIntent.OpenEnhancement) },
                    horizontalPadding = buttonHorizontalPadding,
                    verticalPadding = buttonVerticalPadding,
                )
            }
            if (state.victoryResolutionAffordanceVisible && !state.enhancementChoiceVisible) {
                BattleEdgeButton(
                    label = stringResource(R.string.active_battle_victory_action),
                    onClick = { onIntent(ActiveBattleIntent.ResolveVictory) },
                    horizontalPadding = buttonHorizontalPadding,
                    verticalPadding = buttonVerticalPadding,
                )
            }
        }
    }
}

@Composable
private fun BattleEdgeButton(
    label: String,
    onClick: () -> Unit,
    horizontalPadding: androidx.compose.ui.unit.Dp,
    verticalPadding: androidx.compose.ui.unit.Dp,
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .heightIn(min = BattleMetrics.edgeControlSize)
            .widthIn(min = BattleMetrics.edgeControlSize),
        contentPadding = PaddingValues(
            horizontal = horizontalPadding,
            vertical = verticalPadding,
        ),
        colors = ButtonDefaults.buttonColors(
            containerColor = BattleAction,
            contentColor = BattleBackground,
        ),
        shape = MaterialTheme.shapes.medium,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
    }
}

@Composable
private fun ActiveBattleFieldBackdrop(
    state: ActiveBattleSnapshot,
    modifier: Modifier = Modifier,
) {
    val fieldDescription = listOfNotNull(
        if (state.baseVisible) {
            stringResource(R.string.active_battle_base_visible)
        } else {
            null
        },
        if (state.enemyEntitiesVisible) {
            stringResource(
                R.string.active_battle_enemies_visible,
                state.enemyEntityIds.size,
            )
        } else {
            null
        },
    ).joinToString(separator = "; ")

    Canvas(
        modifier = modifier.semantics {
            contentDescription = fieldDescription
        },
    ) {
        val horizon = size.height * 0.48f
        val groundPath = Path().apply {
            moveTo(0f, horizon)
            lineTo(size.width, horizon)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(BattleBackground, BattleFieldMid, BattleHorizon),
            ),
            size = size,
        )
        drawPath(
            path = groundPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    BattleHorizon.copy(alpha = 0.42f),
                    BattleFieldMid.copy(alpha = 0.88f),
                ),
                startY = horizon,
                endY = size.height,
            ),
        )

        val gridStroke = BattleMetrics.controlGap.toPx() * 0.2f
        for (index in 1..5) {
            val y = horizon + (size.height - horizon) * index / 6f
            drawLine(
                color = BattleOnBackground.copy(alpha = 0.08f),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = gridStroke,
            )
        }
        for (index in 0..7) {
            val x = size.width * index / 7f
            drawLine(
                color = BattleOnBackground.copy(alpha = 0.06f),
                start = Offset(x, horizon),
                end = Offset(size.width * (0.5f + (index - 3.5f) * 0.34f), size.height),
                strokeWidth = gridStroke,
            )
        }

        if (state.waveActive) {
            val waveCenter = Offset(size.width * 0.5f, horizon * 0.78f)
            drawCircle(
                color = BattleAction.copy(alpha = 0.07f),
                radius = size.minDimension * 0.18f,
                center = waveCenter,
            )
            drawCircle(
                color = BattleAction.copy(alpha = 0.48f),
                radius = size.minDimension * 0.07f,
                center = waveCenter,
                style = Stroke(width = gridStroke * 2.5f),
            )
        }

        if (state.baseVisible) {
            drawBattleBase(
                center = Offset(size.width * 0.16f, size.height * 0.73f),
                radius = size.minDimension * 0.12f,
                strokeWidth = gridStroke * 2.5f,
            )
        }

        if (state.enemyEntitiesVisible) {
            state.enemyEntityIds.forEachIndexed { index, _ ->
                val column = index % 3
                val row = index / 3
                drawBattleEnemy(
                    center = Offset(
                        x = size.width * (0.7f + column * 0.1f),
                        y = horizon + size.height * (0.13f + row * 0.1f),
                    ),
                    radius = size.minDimension * 0.038f,
                    strokeWidth = gridStroke * 2f,
                )
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBattleBase(
    center: Offset,
    radius: Float,
    strokeWidth: Float,
) {
    drawCircle(
        color = BattleBase.copy(alpha = 0.14f),
        radius = radius * 1.65f,
        center = center,
    )
    drawCircle(
        color = BattleBase.copy(alpha = 0.22f),
        radius = radius,
        center = center,
    )
    val corePath = Path().apply {
        moveTo(center.x, center.y - radius * 0.8f)
        lineTo(center.x + radius * 0.72f, center.y - radius * 0.24f)
        lineTo(center.x + radius * 0.48f, center.y + radius * 0.7f)
        lineTo(center.x - radius * 0.48f, center.y + radius * 0.7f)
        lineTo(center.x - radius * 0.72f, center.y - radius * 0.24f)
        close()
    }
    drawPath(path = corePath, color = BattleBase)
    drawPath(
        path = corePath,
        color = BattleOnBackground.copy(alpha = 0.86f),
        style = Stroke(width = strokeWidth),
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBattleEnemy(
    center: Offset,
    radius: Float,
    strokeWidth: Float,
) {
    drawCircle(
        color = BattleEnemy.copy(alpha = 0.18f),
        radius = radius * 2.1f,
        center = center,
    )
    val enemyPath = Path().apply {
        moveTo(center.x, center.y - radius)
        lineTo(center.x + radius, center.y)
        lineTo(center.x, center.y + radius)
        lineTo(center.x - radius, center.y)
        close()
    }
    drawPath(path = enemyPath, color = BattleEnemy)
    drawPath(
        path = enemyPath,
        color = BattleOnBackground.copy(alpha = 0.74f),
        style = Stroke(width = strokeWidth),
    )
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
    val routeBarHeightPx = remember { mutableIntStateOf(0) }
    val routeBarHeight = with(LocalDensity.current) { routeBarHeightPx.intValue.toDp() }
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CampaignBackground),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding(),
        ) {
            CampaignHeader(modifier = Modifier.fillMaxWidth())
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = CampaignMetrics.contentInset)
                    .padding(bottom = routeBarHeight + spacing.m),
                verticalArrangement = Arrangement.spacedBy(CampaignMetrics.sectionGap),
            ) {
                Text(
                    text = stringResource(R.string.campaign_selection_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = CampaignOnBackground,
                )
                Text(
                    text = stringResource(R.string.campaign_header_body),
                    style = MaterialTheme.typography.bodyLarge,
                    color = CampaignOnBackground.copy(alpha = 0.78f),
                )
                stageIds.forEachIndexed { index, stageId ->
                    CampaignLevelCard(
                        stageId = stageId,
                        index = index,
                        total = stageIds.size,
                        onSelect = { onSelectStage(stageId) },
                    )
                }
            }
        }
        CampaignRouteBar(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .onSizeChanged { routeBarHeightPx.intValue = it.height },
            onOpenRoster = onOpenRoster,
            onOpenArena = onOpenArena,
        )
    }
}

@Composable
private fun CampaignHeader(
    modifier: Modifier = Modifier,
) {
    val spacing = LocalSpacing.current
    Surface(
        modifier = modifier,
        color = CampaignSurface,
        contentColor = CampaignOnSurface,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = CampaignMetrics.contentInset,
                    vertical = CampaignMetrics.cardPadding,
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(LocalSpacing.current.m),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.campaign_header_kicker),
                    style = MaterialTheme.typography.labelMedium,
                    color = CampaignSupport,
                )
                Text(
                    text = stringResource(R.string.campaign_selection_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = CampaignOnSurface,
                )
            }
            Surface(
                color = CampaignAccent.copy(alpha = 0.16f),
                contentColor = CampaignAccent,
                shape = MaterialTheme.shapes.small,
            ) {
                Text(
                    text = stringResource(R.string.campaign_status_local),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(
                        horizontal = CampaignMetrics.cardPadding,
                        vertical = spacing.xs,
                    ),
                )
            }
        }
    }
}

@Composable
private fun CampaignLevelCard(
    stageId: CampaignStageId,
    index: Int,
    total: Int,
    onSelect: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = CampaignSurface,
        contentColor = CampaignOnSurface,
        border = BorderStroke(1.dp, CampaignSupport.copy(alpha = 0.35f)),
    ) {
        Column(
            modifier = Modifier.padding(CampaignMetrics.cardPadding),
            verticalArrangement = Arrangement.spacedBy(LocalSpacing.current.s),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(LocalSpacing.current.s),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.campaign_level_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = CampaignSupport,
                    )
                    Text(
                        text = stageTitle(stageId),
                        style = MaterialTheme.typography.headlineSmall,
                        color = CampaignOnSurface,
                    )
                }
                Text(
                    text = stringResource(R.string.campaign_level_marker, index + 1, total),
                    style = MaterialTheme.typography.labelLarge,
                    color = CampaignAccent,
                )
            }
            Text(
                text = stringResource(R.string.campaign_level_body),
                style = MaterialTheme.typography.bodyLarge,
                color = CampaignOnSurface.copy(alpha = 0.84f),
            )
            Button(
                onClick = onSelect,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = CampaignMetrics.minTouchTarget),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CampaignAccent,
                    contentColor = CampaignBackground,
                ),
            ) {
                Text(
                    text = stringResource(R.string.campaign_level_setup_action),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

@Composable
private fun CampaignRouteBar(
    onOpenRoster: () -> Unit,
    onOpenArena: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        color = CampaignSurface,
        contentColor = CampaignOnSurface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = LocalSpacing.current.s, vertical = LocalSpacing.current.xs),
            verticalArrangement = Arrangement.spacedBy(LocalSpacing.current.xs),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(LocalSpacing.current.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = CampaignMetrics.minTouchTarget),
                    color = CampaignAccent.copy(alpha = 0.18f),
                    contentColor = CampaignAccent,
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = CampaignMetrics.minTouchTarget),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.campaign_route_campaign),
                            style = MaterialTheme.typography.labelLarge,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                CampaignRouteButton(
                    label = stringResource(R.string.campaign_route_troops),
                    onClick = onOpenRoster,
                    modifier = Modifier.weight(1f),
                )
                CampaignRouteButton(
                    label = stringResource(R.string.campaign_route_arena),
                    onClick = onOpenArena,
                    modifier = Modifier.weight(1f),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(LocalSpacing.current.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CampaignRouteButton(
                    label = stringResource(R.string.campaign_shop_action),
                    enabled = false,
                    modifier = Modifier.weight(1f),
                )
                CampaignRouteButton(
                    label = stringResource(R.string.campaign_tech_action),
                    enabled = false,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun CampaignRouteButton(
    label: String,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .heightIn(min = CampaignMetrics.minTouchTarget),
        contentPadding = PaddingValues(horizontal = LocalSpacing.current.xs, vertical = LocalSpacing.current.s),
        colors = ButtonDefaults.buttonColors(
            containerColor = CampaignSupport.copy(alpha = 0.14f),
            contentColor = CampaignOnSurface,
            disabledContainerColor = CampaignBackground.copy(alpha = 0.35f),
            disabledContentColor = CampaignDisabled,
        ),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
        )
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
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(RosterBackground),
    ) {
        RosterBackdrop(modifier = Modifier.matchParentSize())
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding(),
        ) {
            RosterHeader(onCloseRoster = onCloseRoster)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = RosterMetrics.contentInset, vertical = spacing.m),
                verticalArrangement = Arrangement.spacedBy(RosterMetrics.cardGap),
            ) {
                Text(
                    text = stringResource(R.string.roster_body),
                    style = MaterialTheme.typography.bodyLarge,
                    color = RosterOnBackground.copy(alpha = 0.82f),
                )
                state.troopSlots.forEach { slot ->
                    RosterTroopContent(
                        slot = slot,
                        onUpgrade = { onIntent(RosterIntent.UpgradeTroop(slot.id)) },
                    )
                }
                if (state.surface == RosterSurface.TROOPS) {
                    Button(
                        onClick = { onIntent(RosterIntent.OpenSettings) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = RosterMetrics.minTouchTarget),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RosterSupport.copy(alpha = 0.18f),
                            contentColor = RosterOnSurface,
                        ),
                    ) {
                        Text(
                            text = stringResource(R.string.roster_settings_action),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }
            RosterRouteBar(onCloseRoster = onCloseRoster)
        }

        if (state.surface == RosterSurface.SETTINGS) {
            SettingsOverlay(
                state = state,
                onIntent = onIntent,
            )
        }
    }
}

@Composable
private fun SettingsOverlay(
    state: RosterSnapshot,
    onIntent: (RosterIntent) -> Unit,
) {
    val spacing = LocalSpacing.current
    Dialog(
        onDismissRequest = { onIntent(RosterIntent.CloseSettings) },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(SettingsOverlayScrim)
                .testTag("settings-overlay"),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = SettingsMetrics.panelMaxWidth)
                    .padding(horizontal = SettingsMetrics.panelInset)
                    .testTag("settings-panel"),
                shape = MaterialTheme.shapes.extraLarge,
                color = SettingsPanel,
                contentColor = SettingsOnPanel,
                border = BorderStroke(1.dp, SettingsPanelBorder),
            ) {
                Column(
                    modifier = Modifier.padding(SettingsMetrics.panelPadding),
                    verticalArrangement = Arrangement.spacedBy(spacing.m),
                ) {
                    Text(
                        text = stringResource(R.string.settings_title),
                        style = MaterialTheme.typography.headlineSmall,
                        color = SettingsOnPanel,
                    )
                    Text(
                        text = stringResource(R.string.settings_body),
                        style = MaterialTheme.typography.bodyLarge,
                        color = SettingsOnPanel.copy(alpha = 0.82f),
                    )
                    Column(
                        verticalArrangement = Arrangement.spacedBy(SettingsMetrics.optionGap),
                    ) {
                        state.settings.forEach { option ->
                            RosterSettingContent(
                                option = option,
                                onToggle = { onIntent(RosterIntent.ToggleSetting(option.id)) },
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(
                            onClick = { onIntent(RosterIntent.CloseSettings) },
                            modifier = Modifier.heightIn(min = SettingsMetrics.minTouchTarget),
                        ) {
                            Text(
                                text = stringResource(R.string.settings_close_action),
                                style = MaterialTheme.typography.labelLarge,
                                color = SettingsCloseAction,
                            )
                        }
                        Button(
                            onClick = { onIntent(RosterIntent.ConfirmSettings) },
                            modifier = Modifier.heightIn(min = SettingsMetrics.minTouchTarget),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SettingsConfirmAction,
                                contentColor = RosterBackground,
                            ),
                        ) {
                            Text(
                                text = stringResource(R.string.settings_confirm_action),
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RosterTroopContent(
    slot: RosterTroopSlot,
    onUpgrade: () -> Unit,
) {
    val label = troopLabel(slot.id)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = RosterCard,
        contentColor = RosterOnSurface,
        border = BorderStroke(1.dp, RosterSupport.copy(alpha = 0.32f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(RosterMetrics.cardPadding),
            horizontalArrangement = Arrangement.spacedBy(RosterMetrics.cardGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RosterTroopEmblem(
                label = label,
                modifier = Modifier.size(72.dp),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(LocalSpacing.current.s),
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleLarge,
                    color = RosterOnSurface,
                )
                if (slot.upgradeAffordanceVisible) {
                    Button(
                        onClick = onUpgrade,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = RosterMetrics.minTouchTarget),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RosterAccent,
                            contentColor = RosterBackground,
                        ),
                    ) {
                        Text(
                            text = stringResource(R.string.roster_upgrade_action),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RosterSettingContent(
    option: RosterSettingOption,
    onToggle: () -> Unit,
) {
    val label = settingLabel(option.id)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = RosterMetrics.minTouchTarget),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
        )
        if (option.toggleAffordanceVisible) {
            Switch(
                modifier = Modifier
                    .size(SettingsMetrics.minTouchTarget)
                    .semantics { contentDescription = label }
                    .testTag("settings-switch-${option.id.stableId}"),
                checked = false,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(
                    uncheckedThumbColor = SettingsSwitchThumb,
                    uncheckedTrackColor = SettingsSwitchTrack,
                    checkedThumbColor = SettingsSwitchThumb,
                    checkedTrackColor = SettingsSwitchTrack,
                ),
            )
        }
    }
}

@Composable
private fun RosterHeader(
    onCloseRoster: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = RosterSurfaceColor,
        contentColor = RosterOnSurface,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = RosterMetrics.contentInset,
                    vertical = RosterMetrics.cardPadding,
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(LocalSpacing.current.m),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.roster_header_kicker),
                    style = MaterialTheme.typography.labelMedium,
                    color = RosterSupport,
                )
                Text(
                    text = stringResource(R.string.roster_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = RosterOnSurface,
                )
            }
            TextButton(
                onClick = onCloseRoster,
                modifier = Modifier
                    .heightIn(min = RosterMetrics.minTouchTarget)
                    .widthIn(min = RosterMetrics.minTouchTarget),
                colors = ButtonDefaults.textButtonColors(contentColor = RosterAccent),
            ) {
                Text(
                    text = stringResource(R.string.roster_close_action),
                    style = MaterialTheme.typography.labelLarge,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun RosterRouteBar(
    onCloseRoster: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        color = RosterSurfaceColor,
        contentColor = RosterOnSurface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = LocalSpacing.current.s, vertical = LocalSpacing.current.xs),
            verticalArrangement = Arrangement.spacedBy(LocalSpacing.current.xs),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(LocalSpacing.current.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RosterRouteButton(
                    label = stringResource(R.string.campaign_route_campaign),
                    onClick = onCloseRoster,
                    modifier = Modifier.weight(1f),
                )
                RosterRouteButton(
                    label = stringResource(R.string.campaign_route_troops),
                    enabled = false,
                    selected = true,
                    modifier = Modifier.weight(1f),
                )
                RosterRouteButton(
                    label = stringResource(R.string.campaign_route_arena),
                    enabled = false,
                    modifier = Modifier.weight(1f),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(LocalSpacing.current.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RosterRouteButton(
                    label = stringResource(R.string.campaign_shop_action),
                    enabled = false,
                    modifier = Modifier.weight(1f),
                )
                RosterRouteButton(
                    label = stringResource(R.string.campaign_tech_action),
                    enabled = false,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun RosterRouteButton(
    label: String,
    enabled: Boolean = true,
    selected: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .testTag("roster-route-${label.lowercase().replace(' ', '-')}")
            .widthIn(min = RosterMetrics.routeItemMinWidth)
            .heightIn(min = RosterMetrics.routeHeight),
        contentPadding = PaddingValues(
            horizontal = LocalSpacing.current.xs,
            vertical = LocalSpacing.current.s,
        ),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) {
                RosterAccent.copy(alpha = 0.2f)
            } else {
                RosterSupport.copy(alpha = 0.14f)
            },
            contentColor = RosterOnSurface,
            disabledContainerColor = if (selected) {
                RosterAccent.copy(alpha = 0.2f)
            } else {
                RosterRouteInactive.copy(alpha = 0.42f)
            },
            disabledContentColor = if (selected) RosterAccent else RosterDisabled,
        ),
        shape = MaterialTheme.shapes.medium,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun RosterTroopEmblem(
    label: String,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(
        R.string.roster_troop_illustration_description,
        label,
    )
    Canvas(
        modifier = modifier.semantics {
            contentDescription = description
        },
    ) {
        val center = Offset(size.width * 0.5f, size.height * 0.46f)
        val radius = size.minDimension * 0.24f
        drawCircle(
            color = RosterSupport.copy(alpha = 0.18f),
            radius = radius * 1.85f,
            center = center,
        )
        drawCircle(
            color = RosterAccent,
            radius = radius,
            center = center,
        )
        drawCircle(
            color = RosterBackground.copy(alpha = 0.78f),
            radius = radius * 0.42f,
            center = center,
        )
        drawLine(
            color = RosterOnBackground.copy(alpha = 0.8f),
            start = Offset(size.width * 0.2f, size.height * 0.78f),
            end = Offset(size.width * 0.8f, size.height * 0.78f),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round,
        )
    }
}

@Composable
private fun RosterBackdrop(
    modifier: Modifier = Modifier,
) {
    val description = stringResource(R.string.roster_illustration_description)
    Canvas(
        modifier = modifier.semantics {
            contentDescription = description
        },
    ) {
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    RosterBackground,
                    RosterBackground.copy(alpha = 0.72f),
                    RosterSurfaceColor,
                ),
            ),
            size = size,
        )
        val beacon = Offset(size.width * 0.82f, size.height * 0.18f)
        val beaconRadius = size.minDimension * 0.16f
        drawCircle(
            color = RosterSupport.copy(alpha = 0.08f),
            radius = beaconRadius * 1.9f,
            center = beacon,
        )
        drawCircle(
            color = RosterSupport.copy(alpha = 0.16f),
            radius = beaconRadius,
            center = beacon,
        )
        drawCircle(
            color = RosterAccent.copy(alpha = 0.9f),
            radius = beaconRadius * 0.22f,
            center = beacon,
        )
        val ridge = Path().apply {
            moveTo(0f, size.height * 0.62f)
            cubicTo(
                size.width * 0.22f,
                size.height * 0.5f,
                size.width * 0.42f,
                size.height * 0.68f,
                size.width * 0.62f,
                size.height * 0.56f,
            )
            cubicTo(
                size.width * 0.78f,
                size.height * 0.47f,
                size.width * 0.9f,
                size.height * 0.58f,
                size.width,
                size.height * 0.5f,
            )
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(
            path = ridge,
            color = RosterCard.copy(alpha = 0.35f),
        )
        for (index in 0..5) {
            val x = size.width * (0.08f + index * 0.17f)
            val top = size.height * (0.68f - (index % 3) * 0.035f)
            drawLine(
                color = RosterSupport.copy(alpha = 0.12f),
                start = Offset(x, top),
                end = Offset(x + size.width * 0.08f, top - size.height * 0.05f),
                strokeWidth = 1.dp.toPx(),
                cap = StrokeCap.Round,
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
