package dev.mysd.android.campaign

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import dev.mysd.android.R
import dev.mysd.android.ui.theme.BattleMetrics
import dev.mysd.android.ui.theme.MySDTheme
import dev.mysd.game.battle.ActiveBattleIntent
import dev.mysd.game.battle.ActiveBattleSnapshot
import dev.mysd.game.battle.ActiveBattleSpeedIndicator
import dev.mysd.game.campaign.AcceptedCampaignFixture
import dev.mysd.game.campaign.BattleSetupChoice
import dev.mysd.game.simulation.ScenarioFixtureKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** ST-0003 / BATTLE-ACTIVE composition and existing intent boundary. */
@RunWith(AndroidJUnit4::class)
class ActiveBattleContentUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val device = UiDevice.getInstance(instrumentation)

    private val defaultState = ActiveBattleSnapshot(
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
        victoryResolutionAffordanceVisible = false,
    )

    @Test
    fun st0003_rendersFullScreenBattlefieldAndEmitsExistingIntents() {
        val emittedIntents = mutableListOf<ActiveBattleIntent>()

        composeTestRule.setContent {
            MySDTheme(dynamicColor = false) {
                ActiveBattleContent(
                    state = defaultState,
                    onIntent = { emittedIntents.add(it) },
                )
            }
        }

        composeTestRule
            .onNodeWithText(context.getString(R.string.active_battle_title))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(
                context.getString(
                    R.string.active_battle_stage,
                    context.getString(R.string.campaign_level_ember_path),
                ),
            )
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(context.getString(R.string.active_battle_wave_activity))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(
                context.getString(
                    R.string.active_battle_enemies_visible,
                    defaultState.enemyEntityIds.size,
                ),
            )
            .assertIsDisplayed()

        val expectedBattlefieldDescription = listOfNotNull(
            if (defaultState.baseVisible) {
                context.getString(R.string.active_battle_base_visible)
            } else {
                null
            },
            if (defaultState.enemyEntitiesVisible) {
                context.getString(
                    R.string.active_battle_enemies_visible,
                    defaultState.enemyEntityIds.size,
                )
            } else {
                null
            },
        ).joinToString(separator = "; ")
        composeTestRule
            .onNode(hasContentDescription(expectedBattlefieldDescription))
            .assertIsDisplayed()

        val actionLabels = listOf(
            context.getString(
                R.string.active_battle_speed,
                context.getString(R.string.active_battle_speed_default),
            ),
            context.getString(R.string.active_battle_pause_action),
            context.getString(R.string.active_battle_build_action),
            context.getString(R.string.active_battle_enhancement_action),
        )
        actionLabels.forEach { label ->
            composeTestRule
                .onNodeWithText(label)
                .assertIsDisplayed()
                .assertHeightIsAtLeast(BattleMetrics.minTouchTarget)
                .assertWidthIsAtLeast(BattleMetrics.minTouchTarget)
        }

        assertEquals(
            "Active battle exposes only the accepted visible controls",
            actionLabels.size,
            composeTestRule.onAllNodes(hasClickAction()).fetchSemanticsNodes().size,
        )

        device.waitForIdle()
        val outputDirectory = requireNotNull(context.getExternalFilesDir("fit"))
        check(outputDirectory.mkdirs() || outputDirectory.isDirectory)
        val screenshot = File(outputDirectory, "FIT-04-02-ST-0003.png")
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
        val remoteScreenshot = "/sdcard/Download/FIT-04-02-ST-0003.png"
        device.executeShellCommand("cp ${screenshot.absolutePath} $remoteScreenshot")
        assertTrue(
            "Expected a non-empty remote screenshot at $remoteScreenshot",
            device.executeShellCommand("ls -l $remoteScreenshot")
                .lineSequence()
                .any { line ->
                    val fields = line.trim().split(Regex("\\s+"))
                    fields.lastOrNull() == remoteScreenshot &&
                        fields.getOrNull(4)?.toLongOrNull()?.let { it > 0L } == true
                },
        )

        composeTestRule.onNodeWithText(actionLabels[0]).performClick()
        composeTestRule.onNodeWithText(actionLabels[1]).performClick()
        composeTestRule.onNodeWithText(actionLabels[2]).performClick()
        composeTestRule.onNodeWithText(actionLabels[3]).performClick()

        composeTestRule.runOnIdle {
            assertEquals(
                listOf(
                    ActiveBattleIntent.ChangeSpeed,
                    ActiveBattleIntent.PauseOrResume,
                    ActiveBattleIntent.SelectBuildAffordance,
                    ActiveBattleIntent.OpenEnhancement,
                ),
                emittedIntents,
            )
        }
    }
}
