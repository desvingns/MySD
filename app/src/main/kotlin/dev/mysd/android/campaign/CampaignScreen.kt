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
import dev.mysd.game.campaign.CampaignIntent
import dev.mysd.game.campaign.CampaignRoute
import dev.mysd.game.campaign.CampaignSnapshot
import dev.mysd.game.campaign.CampaignStageId
import dev.mysd.game.campaign.LevelSetupOrigin

@Composable
fun CampaignScreen(
    state: CampaignSnapshot,
    onIntent: (CampaignIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    CampaignScreenContent(
        state = state,
        onIntent = onIntent,
        modifier = modifier,
    )
}

@Composable
fun CampaignScreenContent(
    state: CampaignSnapshot,
    onIntent: (CampaignIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state.route) {
        CampaignRoute.CLEAN_LAUNCH -> CampaignRouteContent(
            title = stringResource(R.string.campaign_launch_title),
            body = stringResource(R.string.campaign_launch_body),
            actionLabel = stringResource(R.string.campaign_enter_action),
            titleColor = MaterialTheme.colorScheme.primary,
            onAction = { onIntent(CampaignIntent.EnterCampaign) },
            modifier = modifier,
        )

        CampaignRoute.CAMPAIGN_SELECTION -> {
            val stageId = state.acceptedStageIds.single()
            CampaignRouteContent(
                title = stringResource(R.string.campaign_selection_title),
                body = stageTitle(stageId),
                detail = stringResource(R.string.campaign_level_body),
                actionLabel = stringResource(R.string.campaign_level_setup_action),
                titleColor = MaterialTheme.colorScheme.secondary,
                onAction = { onIntent(CampaignIntent.SelectLevel(stageId)) },
                modifier = modifier,
            )
        }

        CampaignRoute.LEVEL_SETUP -> CampaignRouteContent(
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

    if (state.unfinishedRunPromptVisible) {
        UnfinishedRunPrompt(
            onCancel = { onIntent(CampaignIntent.CancelUnfinishedRun) },
            onContinue = { onIntent(CampaignIntent.ContinueUnfinishedRun) },
        )
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
