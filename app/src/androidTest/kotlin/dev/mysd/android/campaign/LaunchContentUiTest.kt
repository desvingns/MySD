package dev.mysd.android.campaign

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import dev.mysd.android.R
import dev.mysd.android.ui.theme.MySDTheme
import dev.mysd.game.campaign.AcceptedCampaignFixture
import dev.mysd.game.campaign.CampaignIntent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** ST-0001 launch composition and primary-action boundary. */
@RunWith(AndroidJUnit4::class)
class LaunchContentUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val device = UiDevice.getInstance(instrumentation)

    @Test
    fun st0001_rendersFullScreenLaunchComposition_andEmitsEnterCampaignOnly() {
        val emittedIntents = mutableListOf<CampaignIntent>()
        val state = AcceptedCampaignFixture.createSession(runSave = null).snapshot()

        composeTestRule.setContent {
            MySDTheme(dynamicColor = false) {
                CampaignScreenContent(
                    state = state,
                    onIntent = { emittedIntents.add(it) },
                )
            }
        }

        composeTestRule
            .onNodeWithText(context.getString(R.string.campaign_launch_title))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(context.getString(R.string.campaign_launch_body))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(context.getString(R.string.campaign_launch_kicker))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(context.getString(R.string.campaign_launch_action_hint))
            .assertIsDisplayed()

        val primaryAction = composeTestRule
            .onNodeWithText(context.getString(R.string.campaign_enter_action))
            .assertIsDisplayed()
            .assert(hasClickAction())
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertHeightIsAtLeast(48.dp)
            .assertWidthIsAtLeast(48.dp)

        val rootBounds = composeTestRule.onRoot().fetchSemanticsNode().boundsInRoot
        val actionBounds = primaryAction.fetchSemanticsNode().boundsInRoot
        assertTrue(
            "Launch action must remain in the bottom half of the full-screen composition",
            actionBounds.top >= rootBounds.center.y,
        )
        assertTrue(
            "Launch action must remain inside the full-screen composition",
            actionBounds.bottom <= rootBounds.bottom,
        )

        val clickableNodes = composeTestRule.onAllNodes(hasClickAction()).fetchSemanticsNodes()
        assertEquals(
            "Clean launch must expose exactly one clickable action",
            1,
            clickableNodes.size,
        )

        device.waitForIdle()
        val outputDirectory = requireNotNull(context.getExternalFilesDir("fit"))
        check(outputDirectory.mkdirs() || outputDirectory.isDirectory)
        val screenshot = File(outputDirectory, "FIT-04-01-ST-0001.png")
        assertTrue(device.takeScreenshot(screenshot))
        assertTrue(screenshot.isFile)
        assertTrue(screenshot.length() > 0L)

        primaryAction.performClick()

        composeTestRule.runOnIdle {
            assertEquals(listOf(CampaignIntent.EnterCampaign), emittedIntents)
        }
    }
}
