package dev.mysd.android.campaign

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import dev.mysd.android.R
import dev.mysd.android.ui.theme.CampaignMetrics
import dev.mysd.android.ui.theme.MySDTheme
import dev.mysd.game.campaign.AcceptedCampaignFixture
import dev.mysd.game.campaign.CampaignIntent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** ST-0006 campaign composition, route semantics, and deferred-action boundary. */
@RunWith(AndroidJUnit4::class)
class CampaignContentUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val device = UiDevice.getInstance(instrumentation)

    @Test
    fun st0006_rendersSnapshotCampaignShell_preservesAcceptedActionsAndDeferredRoutes() {
        val emittedIntents = mutableListOf<CampaignIntent>()
        val session = AcceptedCampaignFixture.createSession(runSave = null)
        val state = session.submit(CampaignIntent.EnterCampaign)
        val stageId = state.acceptedStageIds.single()

        composeTestRule.setContent {
            MySDTheme(dynamicColor = false) {
                CampaignScreenContent(
                    state = state,
                    onIntent = { emittedIntents.add(it) },
                )
            }
        }

        composeTestRule
            .onNodeWithText(context.getString(R.string.campaign_header_kicker))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(context.getString(R.string.campaign_header_body))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(context.getString(R.string.campaign_status_local))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(context.getString(R.string.campaign_level_label))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(context.getString(R.string.campaign_level_ember_path))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(context.getString(R.string.campaign_level_body))
            .assertIsDisplayed()

        val levelAction = composeTestRule
            .onNodeWithText(context.getString(R.string.campaign_level_setup_action))
            .assertIsDisplayed()
            .assertHeightIsAtLeast(CampaignMetrics.minTouchTarget)
            .assertWidthIsAtLeast(CampaignMetrics.minTouchTarget)
        val troopsAction = composeTestRule
            .onNodeWithText(context.getString(R.string.campaign_route_troops))
            .assertIsDisplayed()
            .assertHeightIsAtLeast(CampaignMetrics.minTouchTarget)
            .assertWidthIsAtLeast(CampaignMetrics.minTouchTarget)
        val arenaAction = composeTestRule
            .onNodeWithText(context.getString(R.string.campaign_route_arena))
            .assertIsDisplayed()
            .assertHeightIsAtLeast(CampaignMetrics.minTouchTarget)
            .assertWidthIsAtLeast(CampaignMetrics.minTouchTarget)

        listOf(
            R.string.campaign_shop_action,
            R.string.campaign_tech_action,
        ).forEach { deferredAction ->
            composeTestRule
                .onNodeWithText(context.getString(deferredAction))
                .assertIsDisplayed()
                .assertIsNotEnabled()
                .assertHeightIsAtLeast(CampaignMetrics.minTouchTarget)
                .assertWidthIsAtLeast(CampaignMetrics.minTouchTarget)
        }

        assertEquals(
            "Only accepted campaign actions expose click semantics",
            3,
            composeTestRule.onAllNodes(hasClickAction()).fetchSemanticsNodes().count {
                !it.config.contains(SemanticsProperties.Disabled)
            },
        )

        levelAction.performClick()
        troopsAction.performClick()
        arenaAction.performClick()

        composeTestRule.runOnIdle {
            assertEquals(
                listOf(
                    CampaignIntent.SelectLevel(stageId),
                    CampaignIntent.OpenRoster,
                    CampaignIntent.OpenArena,
                ),
                emittedIntents,
            )
        }

        device.waitForIdle()
        val outputDirectory = requireNotNull(context.getExternalFilesDir("fit"))
        check(outputDirectory.mkdirs() || outputDirectory.isDirectory)
        val screenshot = File(outputDirectory, "FIT-04-03-ST-0006.png")
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
        val remoteScreenshot = "/sdcard/Download/FIT-04-03-ST-0006.png"
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
    }
}
