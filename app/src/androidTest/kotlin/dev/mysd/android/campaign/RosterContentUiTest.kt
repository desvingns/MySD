package dev.mysd.android.campaign

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import dev.mysd.android.R
import dev.mysd.android.ui.theme.MySDTheme
import dev.mysd.android.ui.theme.RosterMetrics
import dev.mysd.game.meta.RosterIntent
import dev.mysd.game.meta.RosterSession
import dev.mysd.game.meta.RosterSnapshot
import dev.mysd.game.meta.RosterSurface
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** ST-0007 roster composition, deferred upgrade boundary, and existing close/settings actions. */
@RunWith(AndroidJUnit4::class)
class RosterContentUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val device = UiDevice.getInstance(instrumentation)

    @Test
    fun st0007_rendersTroopCardsAndPersistentRoutes_upgradeRemainsDeferred() {
        val session = RosterSession()
        val initialState = session.snapshot()
        var renderedState by mutableStateOf(initialState)
        val emittedIntents = mutableListOf<RosterIntent>()

        composeTestRule.setContent {
            MySDTheme(dynamicColor = false) {
                RosterContent(
                    state = renderedState,
                    onIntent = { intent ->
                        emittedIntents += intent
                        renderedState = session.submit(intent)
                    },
                    onCloseRoster = {},
                )
            }
        }

        composeTestRule
            .onNodeWithText(context.getString(R.string.roster_body))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(context.getString(R.string.roster_troop_bright_mote))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(context.getString(R.string.roster_upgrade_action))
            .assertIsDisplayed()
            .assertHeightIsAtLeast(RosterMetrics.minTouchTarget)
            .assertWidthIsAtLeast(RosterMetrics.minTouchTarget)
        composeTestRule
            .onNode(
                hasContentDescription(context.getString(R.string.roster_illustration_description)),
            )
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText(context.getString(R.string.campaign_route_campaign))
            .assertIsDisplayed()
            .assertHeightIsAtLeast(RosterMetrics.routeHeight)
            .assertWidthIsAtLeast(RosterMetrics.routeItemMinWidth)
        listOf(
            "campaign" to true,
            "troops" to false,
            "arena" to false,
            "shop" to false,
            "tech" to false,
        ).forEach { (routeId, enabled) ->
            val route = composeTestRule
                .onNodeWithTag("roster-route-$routeId")
            .assertIsDisplayed()
            .assertHeightIsAtLeast(RosterMetrics.routeHeight)
            .assertWidthIsAtLeast(RosterMetrics.routeItemMinWidth)
            if (!enabled) route.assertIsNotEnabled()
        }

        val beforeUpgrade = renderedState
        composeTestRule
            .onNodeWithText(context.getString(R.string.roster_upgrade_action))
            .performClick()

        composeTestRule.runOnIdle {
            assertEquals(
                listOf(RosterIntent.UpgradeTroop(initialState.troopSlots.single().id)),
                emittedIntents,
            )
            assertEquals(beforeUpgrade, renderedState)
            assertEquals(beforeUpgrade, session.snapshot())
            assertEquals(RosterSurface.TROOPS, renderedState.surface)
        }

        captureScreenshot("FIT-04-04-ST-0007.png")
    }

    @Test
    fun st0007_preservesSettingsAndCloseActions_withAccessibleTargets() {
        val session = RosterSession()
        var renderedState by mutableStateOf(session.snapshot())
        val emittedIntents = mutableListOf<RosterIntent>()
        var closeCount = 0

        composeTestRule.setContent {
            MySDTheme(dynamicColor = false) {
                RosterContent(
                    state = renderedState,
                    onIntent = { intent ->
                        emittedIntents += intent
                        renderedState = session.submit(intent)
                    },
                    onCloseRoster = { closeCount += 1 },
                )
            }
        }

        composeTestRule
            .onNodeWithText(context.getString(R.string.roster_settings_action))
            .assertIsDisplayed()
            .assertHeightIsAtLeast(RosterMetrics.minTouchTarget)
            .assertWidthIsAtLeast(RosterMetrics.minTouchTarget)
            .performClick()
        composeTestRule
            .onNodeWithText(context.getString(R.string.settings_title))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(context.getString(R.string.settings_close_action))
            .assertIsDisplayed()
            .assertHeightIsAtLeast(RosterMetrics.minTouchTarget)
            .assertWidthIsAtLeast(RosterMetrics.minTouchTarget)
            .performClick()

        composeTestRule.runOnIdle {
            assertEquals(RosterSurface.TROOPS, renderedState.surface)
        }

        composeTestRule
            .onNodeWithText(context.getString(R.string.roster_settings_action))
            .performClick()
        composeTestRule
            .onNodeWithText(context.getString(R.string.settings_confirm_action))
            .assertIsDisplayed()
            .assertHeightIsAtLeast(RosterMetrics.minTouchTarget)
            .assertWidthIsAtLeast(RosterMetrics.minTouchTarget)
            .performClick()
        composeTestRule
            .onNodeWithText(context.getString(R.string.roster_close_action))
            .assertIsDisplayed()
            .assertHeightIsAtLeast(RosterMetrics.minTouchTarget)
            .assertWidthIsAtLeast(RosterMetrics.minTouchTarget)
            .performClick()

        composeTestRule.runOnIdle {
            assertEquals(
                listOf(
                    RosterIntent.OpenSettings,
                    RosterIntent.CloseSettings,
                    RosterIntent.OpenSettings,
                    RosterIntent.ConfirmSettings,
                ),
                emittedIntents,
            )
            assertEquals(1, closeCount)
            assertEquals(RosterSurface.TROOPS, renderedState.surface)
        }
    }

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
        val remoteScreenshotSize = device
            .executeShellCommand("stat -c %s $remoteScreenshot")
            .trim()
            .toLongOrNull()
        assertTrue(
            "Expected a non-empty remote screenshot at $remoteScreenshot",
            remoteScreenshotSize != null && remoteScreenshotSize > 0L,
        )
    }
}
