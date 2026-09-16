package dev.mysd.android

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.mysd.android.campaign.ActiveBattleContent
import dev.mysd.android.campaign.CampaignScreenContent
import dev.mysd.android.campaign.PlayableBattleTerminalContent
import dev.mysd.android.ui.theme.MySDTheme
import dev.mysd.game.battle.ActiveBattleIntent
import dev.mysd.game.battle.ActiveBattleSnapshot
import dev.mysd.game.battle.ActiveBattleSpeedIndicator
import dev.mysd.game.battle.playable.PlayableBattleCommand
import dev.mysd.game.battle.playable.PlayableBattleEngine
import dev.mysd.game.battle.playable.PlayableBattlePhase
import dev.mysd.game.battle.playable.PlayableBattleTerminal
import dev.mysd.game.campaign.AcceptedCampaignFixture
import dev.mysd.game.campaign.BattleSetupChoice
import dev.mysd.game.campaign.CampaignRoute
import dev.mysd.game.campaign.CampaignSnapshot
import dev.mysd.game.simulation.PlayableBattleSnapshot
import dev.mysd.game.simulation.ScenarioFixtureKind
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlayableBattleSemanticRepairTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val activeState = PlayableBattleEngine.initialState()
    private val activeSnapshot = PlayableBattleSnapshot(0L, "active", 0L, activeState)
    private val defeatSnapshot = PlayableBattleSnapshot(
        tick = 1L,
        stateHash = "defeat",
        pendingMillis = 0L,
        state = activeState.copy(
            base = activeState.base.copy(health = 0),
            terminalResult = PlayableBattleTerminal.DEFEAT,
        ),
    )

    @Test
    fun restoredTerminalSnapshotWinsOverMissingBattleStartRoute() {
        val launchState = CampaignSnapshot(
            route = CampaignRoute.CLEAN_LAUNCH,
            acceptedStageIds = listOf(AcceptedCampaignFixture.STAGE_ID),
            selectedStageId = null,
            setupOrigin = null,
            unfinishedRunPromptVisible = false,
        )

        composeTestRule.setContent {
            MySDTheme(dynamicColor = false) {
                CampaignScreenContent(
                    state = launchState,
                    onIntent = {},
                    playableBattle = defeatSnapshot,
                )
            }
        }

        composeTestRule
            .onNodeWithText(context.getString(R.string.active_battle_terminal_defeat_title))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(context.getString(R.string.campaign_launch_title))
            .assertDoesNotExist()
        assertTrue(composeTestRule.onAllNodes(hasClickAction()).fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun terminalSemanticsRetainFrozenBattlefieldSummaryWithoutControls() {
        val slotDescriptions = defeatSnapshot.slots.mapIndexed { index, slot ->
            if (slot.isEmpty) {
                context.getString(R.string.active_battle_tile_empty, index + 1)
            } else {
                context.getString(R.string.active_battle_tile_occupied, index + 1, slot.level)
            }
        }
        val fieldDescription = buildList {
            add(context.getString(R.string.active_battle_base_health, 0, defeatSnapshot.base.maxHealth))
            add(context.getString(R.string.active_battle_resource, defeatSnapshot.resource, defeatSnapshot.state.resourceCap))
            add(context.getString(R.string.active_battle_wave, defeatSnapshot.waveSpawnedCount, defeatSnapshot.waveSpawnCount))
            addAll(slotDescriptions)
            add(context.getString(R.string.active_battle_enemies_state, defeatSnapshot.enemies.size))
        }.joinToString("; ")
        val expected = listOf(
            context.getString(R.string.active_battle_terminal_defeat_title),
            context.getString(R.string.active_battle_terminal_defeat_body),
            fieldDescription,
        ).joinToString(". ")

        composeTestRule.setContent {
            MySDTheme(dynamicColor = false) {
                PlayableBattleTerminalContent(defeatSnapshot)
            }
        }

        composeTestRule.onNode(hasContentDescription(expected)).assertIsDisplayed()
        assertTrue(composeTestRule.onAllNodes(hasClickAction()).fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun playableBattleExposesCanonicalTileCommandAndPauseOnly() {
        val legacyIntents = mutableListOf<ActiveBattleIntent>()
        val playableCommands = mutableListOf<PlayableBattleCommand>()
        val activeContour = ActiveBattleSnapshot(
            stageId = AcceptedCampaignFixture.STAGE_ID,
            fixtureId = ScenarioFixtureKind.ACTIVE_WAVE.stableId,
            selectedSetupChoice = BattleSetupChoice.OPTION_A,
            waveActive = true,
            baseVisible = true,
            enemyEntitiesVisible = true,
            enemyEntityIds = listOf("ash-runner"),
            speedAffordanceVisible = true,
            speedIndicator = ActiveBattleSpeedIndicator.DEFAULT,
            pauseResumeAffordanceVisible = true,
            paused = false,
            buildAffordanceVisible = true,
            buildAffordanceSelected = false,
            enhancementAffordanceVisible = true,
            enhancementChoiceVisible = false,
            victoryResolutionAffordanceVisible = true,
        )

        composeTestRule.setContent {
            MySDTheme(dynamicColor = false) {
                ActiveBattleContent(
                    state = activeContour,
                    onIntent = { legacyIntents.add(it) },
                    playableBattle = activeSnapshot,
                    onPlayableBattleCommand = { playableCommands.add(it) },
                )
            }
        }

        listOf(
            context.getString(R.string.active_battle_build_action),
            context.getString(R.string.active_battle_enhancement_action),
            context.getString(R.string.active_battle_victory_action),
            context.getString(
                R.string.active_battle_speed,
                context.getString(R.string.active_battle_speed_default),
            ),
        ).forEach { composeTestRule.onNodeWithText(it).assertDoesNotExist() }
        composeTestRule
            .onNodeWithText(context.getString(R.string.active_battle_pause_action))
            .assertIsDisplayed()

        val firstSlot = activeSnapshot.slots.first()
        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.active_battle_tile_empty, 1))
            .performClick()
        composeTestRule
            .onNodeWithText(context.getString(R.string.active_battle_build_confirm))
            .performClick()

        composeTestRule.runOnIdle {
            assertTrue(legacyIntents.isEmpty())
            assertEquals(listOf(PlayableBattleCommand.BuildTower(firstSlot.id)), playableCommands)
        }
    }

    @Test
    fun pausedAndTerminalSnapshotsSuspendTickerAndActivePublicationWakesIt() = runBlocking {
        val snapshots = MutableStateFlow<PlayableBattleSnapshot?>(
            activeSnapshot.copy(state = activeState.copy(phase = PlayableBattlePhase.PAUSED)),
        )
        val tickGate = Channel<Unit>(Channel.RENDEZVOUS)
        val ticks = Channel<Unit>(Channel.UNLIMITED)
        val job = launch(start = CoroutineStart.UNDISPATCHED) {
            runPlayableBattleTicker(
                snapshots = snapshots,
                awaitNextTick = { tickGate.receive() },
                onTick = { ticks.send(Unit) },
            )
        }

        assertNull(withTimeoutOrNull(100L) { tickGate.send(Unit) })
        snapshots.value = activeSnapshot
        withTimeout(1_000L) { tickGate.send(Unit) }
        withTimeout(1_000L) { ticks.receive() }

        snapshots.value = defeatSnapshot
        yield()
        assertNull(withTimeoutOrNull(100L) { tickGate.send(Unit) })

        snapshots.value = activeSnapshot
        withTimeout(1_000L) { tickGate.send(Unit) }
        withTimeout(1_000L) { ticks.receive() }
        job.cancelAndJoin()
        assertNull(withTimeoutOrNull(100L) { tickGate.send(Unit) })
    }
}
