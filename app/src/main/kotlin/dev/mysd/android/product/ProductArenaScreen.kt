package dev.mysd.android.product

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.mysd.android.R
import dev.mysd.android.ui.theme.ProductColors
import dev.mysd.android.ui.theme.ProductMetrics

@Composable
internal fun ProductArenaScreen(
    shell: ProductShellUi,
    state: ProductSurfaceUi.Arena,
    onAction: (ProductUiAction) -> Unit,
    modifier: Modifier = Modifier,
    reduceMotion: Boolean = false,
) {
    when (val arena = state.state) {
        is ArenaUi.Battle -> ProductBattleScreen(
            state = ProductSurfaceUi.Battle(arena.scene),
            onAction = onAction,
            modifier = modifier,
            arenaLabel = stringResource(R.string.product_arena_title),
            reduceMotion = reduceMotion,
        )

        else -> ProductScreenFrame(
            shell = shell,
            onAction = onAction,
            modifier = modifier,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(ProductMetrics.screenInset)
                    .testTag("product-arena"),
                verticalArrangement = Arrangement.spacedBy(ProductMetrics.sectionGap),
            ) {
                ProductSectionHeader(
                    title = stringResource(R.string.product_arena_title),
                    body = stringResource(R.string.product_arena_body),
                )
                Surface(
                    color = ProductColors.AuroraTeal.copy(alpha = 0.14f),
                    contentColor = ProductColors.AuroraTeal,
                    shape = MaterialTheme.shapes.small,
                ) {
                    Text(
                        text = stringResource(R.string.product_arena_local_badge),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
                when (arena) {
                    is ArenaUi.Lobby -> ProductArenaLobby(arena, onAction)
                    is ArenaUi.OpponentPreview -> ProductArenaPreview(arena, onAction)
                    is ArenaUi.Result -> ProductArenaResult(arena, onAction)
                    is ArenaUi.Battle -> Unit
                }
            }
        }
    }
}

@Composable
private fun ProductArenaLobby(
    state: ArenaUi.Lobby,
    onAction: (ProductUiAction) -> Unit,
) {
    ProductArenaStats(power = state.rating)
    if (state.recentResults.isNotEmpty()) {
        ProductCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                state.recentResults.take(5).forEach { result ->
                    Text(text = result, color = ProductColors.TextSecondary)
                }
            }
        }
    }
    ProductPrimaryButton(
        label = stringResource(R.string.product_arena_find),
        onClick = { onAction(ProductUiAction.ArenaFindOpponent) },
        enabled = true,
        modifier = Modifier.fillMaxWidth(),
        testTag = "product-arena-find",
    )
}

@Composable
private fun ProductArenaPreview(
    state: ArenaUi.OpponentPreview,
    onAction: (ProductUiAction) -> Unit,
) {
    ProductCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("product-arena-preview")
            .semantics {
                contentDescription = "Локальный соперник ${state.opponentName}"
            },
        highlighted = true,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.product_arena_opponent, state.opponentName),
                style = MaterialTheme.typography.headlineSmall,
                color = ProductColors.TextPrimary,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.product_arena_player_power, state.rating),
                color = ProductColors.AuroraTeal,
            )
            Text(
                text = stringResource(R.string.product_arena_power, state.opponentPower),
                color = ProductColors.WarningCoral,
            )
            Text(
                text = state.seedLabel,
                style = MaterialTheme.typography.labelSmall,
                color = ProductColors.TextMuted,
            )
            ProductPrimaryButton(
                label = stringResource(R.string.product_arena_start),
                onClick = { onAction(ProductUiAction.ArenaStartBattle) },
                modifier = Modifier.fillMaxWidth(),
                testTag = "product-arena-start",
            )
            ProductPrimaryButton(
                label = stringResource(R.string.product_arena_cancel),
                onClick = { onAction(ProductUiAction.Navigate(ProductDestinationUi.CAMPAIGN)) },
                modifier = Modifier.fillMaxWidth(),
                testTag = "product-arena-cancel",
            )
        }
    }
}

@Composable
private fun ProductArenaResult(
    state: ArenaUi.Result,
    onAction: (ProductUiAction) -> Unit,
) {
    ProductCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("product-arena-result"),
        highlighted = true,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(
                    if (state.victory) {
                        R.string.product_arena_victory
                    } else {
                        R.string.product_arena_defeat
                    },
                ),
                style = MaterialTheme.typography.headlineMedium,
                color = if (state.victory) {
                    ProductColors.SuccessMint
                } else {
                    ProductColors.WarningCoral
                },
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.product_arena_opponent, state.opponentName),
                color = ProductColors.TextSecondary,
            )
            ProductPrimaryButton(
                label = stringResource(R.string.product_arena_again),
                onClick = { onAction(ProductUiAction.ArenaStartBattle) },
                modifier = Modifier.fillMaxWidth(),
                testTag = "product-arena-again",
            )
            ProductPrimaryButton(
                label = stringResource(R.string.product_arena_return),
                onClick = { onAction(ProductUiAction.ArenaReturnToLobby) },
                modifier = Modifier.fillMaxWidth(),
                testTag = "product-arena-return",
            )
        }
    }
}

@Composable
private fun ProductArenaStats(power: Int) {
    ProductCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.product_arena_player_power, power),
            color = ProductColors.RelayAmber,
            textAlign = TextAlign.Center,
        )
    }
}
