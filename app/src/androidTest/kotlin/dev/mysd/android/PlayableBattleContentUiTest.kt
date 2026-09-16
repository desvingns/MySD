package dev.mysd.android

import android.graphics.Bitmap
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import dev.mysd.android.campaign.ActiveBattleContent
import dev.mysd.android.campaign.PlayableBattleTerminalContent
import dev.mysd.android.ui.theme.MySDTheme
import dev.mysd.game.battle.ActiveBattleSnapshot
import dev.mysd.game.battle.ActiveBattleSpeedIndicator
import dev.mysd.game.battle.playable.PlayableBattleCommand
import dev.mysd.game.battle.playable.PlayableBattleEngine
import dev.mysd.game.battle.playable.PlayableBattlePhase
import dev.mysd.game.battle.playable.PlayableBattleState
import dev.mysd.game.battle.playable.PlayableBattleTerminal
import dev.mysd.game.campaign.AcceptedCampaignFixture
import dev.mysd.game.campaign.BattleSetupChoice
import dev.mysd.game.simulation.PlayableBattleSnapshot
import dev.mysd.game.simulation.ScenarioFixtureKind
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlayableBattleContentUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val device = UiDevice.getInstance(instrumentation)

    @Test
    fun buildCommandProjectsTowerAndDeductedCostFromNewSnapshot() {
        val initialState = PlayableBattleEngine.initialState(
            initialResource = 500,
            resourceCap = 500,
        )
        val initialSnapshot = snapshot(initialState)
        val renderedSnapshot = mutableStateOf(initialSnapshot)
        val commands = mutableListOf<PlayableBattleCommand>()
        val slot = initialState.slots.first()

        composeTestRule.setContent {
            MySDTheme(dynamicColor = false) {
                ActiveBattleContent(
                    state = activeContour(),
                    onIntent = {},
                    playableBattle = renderedSnapshot.value,
                    onPlayableBattleCommand = { command ->
                        commands += command
                        val result = PlayableBattleEngine.buildTower(
                            renderedSnapshot.value.state,
                            (command as PlayableBattleCommand.BuildTower).slotId,
                        )
                        assertTrue(result.accepted)
                        renderedSnapshot.value = snapshot(result.state, tick = 1L)
                    },
                )
            }
        }

        composeTestRule
            .onNodeWithText(
                context.getString(
                    R.string.active_battle_resource,
                    initialState.resource,
                    initialState.resourceCap,
                ),
            )
            .assertIsDisplayed()
        captureScreenshot("FPL-09-playable-battle.png")
        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.active_battle_tile_empty, 1))
            .performClick()
        composeTestRule
            .onNodeWithText(
                context.getString(R.string.active_battle_build_popup_body, initialState.buildCost),
            )
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(context.getString(R.string.active_battle_build_confirm))
            .performClick()

        composeTestRule
            .onNodeWithContentDescription(
                context.getString(R.string.active_battle_tile_occupied, 1, 0),
            )
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(
                context.getString(
                    R.string.active_battle_resource,
                    initialState.resource - initialState.buildCost,
                    initialState.resourceCap,
                ),
            )
            .assertIsDisplayed()
        composeTestRule.runOnIdle {
            assertEquals(listOf(PlayableBattleCommand.BuildTower(slot.id)), commands)
            assertEquals(500, initialSnapshot.resource)
            assertTrue(initialSnapshot.slots.first().isEmpty)
            assertTrue(renderedSnapshot.value.state !== initialSnapshot.state)
        }
    }

    @Test
    fun insufficientBuildGuardDisablesConfirmationAndSubmitsNoCommand() {
        val template = PlayableBattleEngine.initialState()
        assertTrue(template.buildCost > 0)
        val guardedState = template.copy(resource = template.buildCost - 1)
        val commands = mutableListOf<PlayableBattleCommand>()

        setActiveBattle(snapshot(guardedState), commands)

        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.active_battle_tile_empty, 1))
            .performClick()
        composeTestRule
            .onNodeWithText(
                context.getString(R.string.active_battle_guard_insufficient, guardedState.buildCost),
            )
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(context.getString(R.string.active_battle_build_confirm))
            .assertIsDisplayed()
            .assertIsNotEnabled()
        composeTestRule.runOnIdle { assertTrue(commands.isEmpty()) }
    }

    @Test
    fun upgradeProjectionShowsNextCostAndMaximumGuardRejectsFurtherCommand() {
        val funded = PlayableBattleEngine.initialState(
            initialResource = 500,
            resourceCap = 500,
        )
        val built = PlayableBattleEngine.buildTower(funded, funded.slots.first().id)
        assertTrue(built.accepted)
        val initialSnapshot = snapshot(built.state)
        val renderedSnapshot = mutableStateOf(initialSnapshot)
        val commands = mutableListOf<PlayableBattleCommand>()
        val slotId = built.state.slots.first().id

        composeTestRule.setContent {
            MySDTheme(dynamicColor = false) {
                ActiveBattleContent(
                    state = activeContour(),
                    onIntent = {},
                    playableBattle = renderedSnapshot.value,
                    onPlayableBattleCommand = { command ->
                        commands += command
                        val result = PlayableBattleEngine.upgradeTower(
                            renderedSnapshot.value.state,
                            (command as PlayableBattleCommand.UpgradeTower).slotId,
                        )
                        assertTrue(result.accepted)
                        renderedSnapshot.value = snapshot(
                            state = result.state,
                            tick = renderedSnapshot.value.tick + 1L,
                        )
                    },
                )
            }
        }

        val firstUpgrade = PlayableBattleEngine.calculateTowerUpgrade(built.state, 0)
        openFirstSlot()
        composeTestRule
            .onNodeWithText(
                context.getString(
                    R.string.active_battle_upgrade_popup_body,
                    firstUpgrade.nextLevel,
                    firstUpgrade.cost,
                ),
            )
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(context.getString(R.string.active_battle_upgrade_confirm))
            .performClick()
        composeTestRule
            .onNodeWithContentDescription(
                context.getString(R.string.active_battle_tile_occupied, 1, 1),
            )
            .assertIsDisplayed()

        val secondUpgrade = PlayableBattleEngine.calculateTowerUpgrade(
            renderedSnapshot.value.state,
            1,
        )
        openFirstSlot(level = 1)
        composeTestRule
            .onNodeWithText(
                context.getString(
                    R.string.active_battle_upgrade_popup_body,
                    secondUpgrade.nextLevel,
                    secondUpgrade.cost,
                ),
            )
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(context.getString(R.string.active_battle_upgrade_confirm))
            .performClick()
        openFirstSlot(level = 2)
        composeTestRule
            .onNodeWithText(context.getString(R.string.active_battle_upgrade_max_body))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(context.getString(R.string.active_battle_guard_max_level))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(context.getString(R.string.active_battle_upgrade_confirm))
            .assertIsDisplayed()
            .assertIsNotEnabled()
        composeTestRule.runOnIdle {
            assertEquals(
                listOf(
                    PlayableBattleCommand.UpgradeTower(slotId),
                    PlayableBattleCommand.UpgradeTower(slotId),
                ),
                commands,
            )
            assertEquals(0, initialSnapshot.slots.first().level)
            assertEquals(2, renderedSnapshot.value.slots.first().level)
            assertEquals(
                built.state.resource - firstUpgrade.cost - secondUpgrade.cost,
                renderedSnapshot.value.resource,
            )
        }
    }

    @Test
    fun immutableLiveSnapshotRecompositionUpdatesEnemyResourceAndBase() {
        val initialState = PlayableBattleEngine.initialState()
        val initialSnapshot = snapshot(initialState)
        val renderedSnapshot = mutableStateOf(initialSnapshot)

        composeTestRule.setContent {
            MySDTheme(dynamicColor = false) {
                ActiveBattleContent(
                    state = activeContour(),
                    onIntent = {},
                    playableBattle = renderedSnapshot.value,
                    onPlayableBattleCommand = {},
                )
            }
        }

        composeTestRule.waitForIdle()
        val beforeEnemyMove = composeTestRule.onRoot().captureToImage().asAndroidBitmap()
        val movedEnemies = initialState.enemies.map { enemy ->
            enemy.copy(positionTicks = enemy.positionTicks + 1)
        }
        val movedState = initialState.copy(enemies = movedEnemies)
        composeTestRule.runOnIdle {
            renderedSnapshot.value = snapshot(movedState, tick = 1L)
        }
        composeTestRule.waitForIdle()
        val afterEnemyMove = composeTestRule.onRoot().captureToImage().asAndroidBitmap()
        assertFalse("Enemy movement must redraw the battle frame", beforeEnemyMove.sameAs(afterEnemyMove))

        val updatedState = movedState.copy(
            resource = movedState.resource + 7,
            base = movedState.base.copy(health = movedState.base.health - 11),
            enemies = emptyList(),
        )
        composeTestRule.runOnIdle {
            renderedSnapshot.value = snapshot(updatedState, tick = 2L)
        }

        composeTestRule
            .onNodeWithText(
                context.getString(
                    R.string.active_battle_resource,
                    updatedState.resource,
                    updatedState.resourceCap,
                ),
            )
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(
                context.getString(
                    R.string.active_battle_base_health,
                    updatedState.base.health,
                    updatedState.base.maxHealth,
                ),
            )
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(context.getString(R.string.active_battle_enemies_state, 0))
            .assertIsDisplayed()
        composeTestRule.runOnIdle {
            assertEquals(initialState.resource, initialSnapshot.resource)
            assertEquals(initialState.base.health, initialSnapshot.base.health)
            assertEquals(initialState.enemies, initialSnapshot.enemies)
            assertTrue(renderedSnapshot.value.state !== initialSnapshot.state)
        }
    }

    @Test
    fun pausedSnapshotGuardsCommandsAndHidesLegacyControlsExceptCanonicalResume() {
        val pausedState = PlayableBattleEngine.initialState(phase = PlayableBattlePhase.PAUSED)
        val commands = mutableListOf<PlayableBattleCommand>()

        setActiveBattle(
            playableBattle = snapshot(pausedState),
            commands = commands,
            contour = activeContour(paused = true),
        )

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
            .onNodeWithText(context.getString(R.string.active_battle_resume_action))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.active_battle_tile_empty, 1))
            .performClick()
        composeTestRule
            .onNodeWithText(context.getString(R.string.active_battle_guard_paused))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(context.getString(R.string.active_battle_build_confirm))
            .assertIsNotEnabled()
        composeTestRule.runOnIdle { assertTrue(commands.isEmpty()) }
    }

    @Test
    fun victoryAndDefeatTerminalsAreVisibleAndExposeNoGameplayControls() {
        val active = PlayableBattleEngine.initialState()
        val victory = active.copy(
            enemies = emptyList(),
            waveSpawnedCount = active.waveSpawnCount,
            terminalResult = PlayableBattleTerminal.VICTORY,
        )
        val defeat = active.copy(
            base = active.base.copy(health = 0),
            terminalResult = PlayableBattleTerminal.DEFEAT,
        )
        val terminalSnapshot = mutableStateOf(snapshot(victory))

        composeTestRule.setContent {
            MySDTheme(dynamicColor = false) {
                PlayableBattleTerminalContent(terminalSnapshot.value)
            }
        }

        composeTestRule
            .onNodeWithText(context.getString(R.string.active_battle_terminal_victory_title))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(context.getString(R.string.active_battle_terminal_victory_body))
            .assertIsDisplayed()
        assertTrue(composeTestRule.onAllNodes(hasClickAction()).fetchSemanticsNodes().isEmpty())

        composeTestRule.runOnIdle {
            terminalSnapshot.value = snapshot(defeat, tick = 1L)
        }
        composeTestRule
            .onNodeWithText(context.getString(R.string.active_battle_terminal_defeat_title))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(context.getString(R.string.active_battle_terminal_defeat_body))
            .assertIsDisplayed()
        assertTrue(composeTestRule.onAllNodes(hasClickAction()).fetchSemanticsNodes().isEmpty())
    }

    private fun setActiveBattle(
        playableBattle: PlayableBattleSnapshot,
        commands: MutableList<PlayableBattleCommand>,
        contour: ActiveBattleSnapshot = activeContour(),
    ) {
        composeTestRule.setContent {
            MySDTheme(dynamicColor = false) {
                ActiveBattleContent(
                    state = contour,
                    onIntent = {},
                    playableBattle = playableBattle,
                    onPlayableBattleCommand = { commands += it },
                )
            }
        }
    }

    private fun openFirstSlot(level: Int = 0) {
        composeTestRule
            .onNodeWithContentDescription(
                context.getString(R.string.active_battle_tile_occupied, 1, level),
            )
            .performClick()
    }

    private fun snapshot(state: PlayableBattleState, tick: Long = 0L): PlayableBattleSnapshot =
        PlayableBattleSnapshot(
            tick = tick,
            stateHash = "instrumented-$tick",
            pendingMillis = 0L,
            state = state,
        )

    private fun activeContour(paused: Boolean = false): ActiveBattleSnapshot = ActiveBattleSnapshot(
        stageId = AcceptedCampaignFixture.STAGE_ID,
        fixtureId = ScenarioFixtureKind.ACTIVE_WAVE.stableId,
        selectedSetupChoice = BattleSetupChoice.OPTION_A,
        waveActive = true,
        baseVisible = true,
        enemyEntitiesVisible = true,
        enemyEntityIds = listOf("ember-scout-01"),
        speedAffordanceVisible = true,
        speedIndicator = ActiveBattleSpeedIndicator.DEFAULT,
        pauseResumeAffordanceVisible = true,
        paused = paused,
        buildAffordanceVisible = true,
        buildAffordanceSelected = false,
        enhancementAffordanceVisible = true,
        enhancementChoiceVisible = false,
        victoryResolutionAffordanceVisible = true,
    )

    private fun captureScreenshot(fileName: String) {
        device.waitForIdle()
        val outputDirectory = requireNotNull(context.getExternalFilesDir("fit"))
        check(outputDirectory.mkdirs() || outputDirectory.isDirectory)
        val screenshot = File(outputDirectory, fileName)
        val screenshotWritten = screenshot.outputStream().use { output ->
            composeTestRule
                .onRoot()
                .captureToImage()
                .asAndroidBitmap()
                .compress(Bitmap.CompressFormat.PNG, 100, output)
        }
        assertTrue(screenshotWritten)
        assertTrue(screenshot.isFile)
        assertTrue(screenshot.length() > 0L)
        val remoteScreenshot = "/sdcard/Download/$fileName"
        device.executeShellCommand("cp ${screenshot.absolutePath} $remoteScreenshot")
        val remoteSize = device.executeShellCommand("stat -c %s $remoteScreenshot")
            .trim()
            .toLongOrNull()
        assertTrue(
            "Expected a non-empty original MySD frame at $remoteScreenshot",
            remoteSize != null && remoteSize > 0L,
        )
    }
}
