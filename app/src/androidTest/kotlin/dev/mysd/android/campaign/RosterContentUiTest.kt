package dev.mysd.android.campaign

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
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
import dev.mysd.android.ui.theme.SettingsMetrics
import dev.mysd.game.meta.RosterIntent
import dev.mysd.game.meta.RosterSettingId
import dev.mysd.game.meta.RosterSession
import dev.mysd.game.meta.RosterSnapshot
import dev.mysd.game.meta.RosterSurface
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** ST-0007 roster composition/deferred upgrade; ST-0008 settings overlay and close actions. */
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
    fun st0008_rendersSettingsOverlayAndPreservesDeferredActions() {
        val session = RosterSession()
        val initialState = session.snapshot()
        var renderedState by mutableStateOf(initialState)
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
        val settingsState = renderedState
        composeTestRule
            .onNodeWithTag("settings-overlay")
            .assertIsDisplayed()
        val settingsPanel = composeTestRule
            .onNodeWithTag("settings-panel")
            .assertIsDisplayed()
        val maxPanelWidthPx = with(composeTestRule.density) {
            SettingsMetrics.panelMaxWidth.toPx()
        }
        assertTrue(
            "Expected settings panel width <= ${SettingsMetrics.panelMaxWidth}",
            settingsPanel.fetchSemanticsNode().boundsInRoot.width <= maxPanelWidthPx,
        )
        composeTestRule
            .onNodeWithText(context.getString(R.string.settings_title))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(context.getString(R.string.settings_body))
            .assertIsDisplayed()
        listOf(
            RosterSettingId.AUDIO to R.string.settings_audio_option,
            RosterSettingId.HAPTICS to R.string.settings_haptics_option,
        ).forEach { (settingId, labelResource) ->
            composeTestRule
                .onNodeWithText(context.getString(labelResource))
                .assertIsDisplayed()
            composeTestRule
                .onNode(hasContentDescription(context.getString(labelResource)))
                .assertIsDisplayed()
            composeTestRule
                .onNodeWithTag("settings-switch-${settingId.stableId}")
                .assertIsDisplayed()
                .assertIsOff()
                .assertHeightIsAtLeast(SettingsMetrics.minTouchTarget)
                .assertWidthIsAtLeast(SettingsMetrics.minTouchTarget)
                .performClick()
        }
        composeTestRule.runOnIdle {
            assertEquals(settingsState, renderedState)
            assertEquals(settingsState, session.snapshot())
        }
        captureScreenshot("FIT-04-05-ST-0008.png", rootTag = "settings-overlay")

        device.pressBack()
        composeTestRule.waitForIdle()
        composeTestRule.runOnIdle {
            assertReturnedToTroops(initialState, renderedState)
        }

        composeTestRule
            .onNodeWithText(context.getString(R.string.roster_settings_action))
            .performClick()
        composeTestRule
            .onNodeWithText(context.getString(R.string.settings_close_action))
            .assertIsDisplayed()
            .assertHeightIsAtLeast(SettingsMetrics.minTouchTarget)
            .assertWidthIsAtLeast(SettingsMetrics.minTouchTarget)
            .performClick()

        composeTestRule.runOnIdle {
            assertReturnedToTroops(initialState, renderedState)
        }

        composeTestRule
            .onNodeWithText(context.getString(R.string.roster_settings_action))
            .performClick()
        composeTestRule
            .onNodeWithText(context.getString(R.string.settings_confirm_action))
            .assertIsDisplayed()
            .assertHeightIsAtLeast(SettingsMetrics.minTouchTarget)
            .assertWidthIsAtLeast(SettingsMetrics.minTouchTarget)
            .performClick()

        composeTestRule.runOnIdle {
            assertReturnedToTroops(initialState, renderedState)
        }

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
                    RosterIntent.ToggleSetting(RosterSettingId.AUDIO),
                    RosterIntent.ToggleSetting(RosterSettingId.HAPTICS),
                    RosterIntent.CloseSettings,
                    RosterIntent.OpenSettings,
                    RosterIntent.CloseSettings,
                    RosterIntent.OpenSettings,
                    RosterIntent.ConfirmSettings,
                ),
                emittedIntents,
            )
            assertEquals(1, closeCount)
            assertReturnedToTroops(initialState, renderedState)
        }
    }

    private fun assertReturnedToTroops(
        initialState: RosterSnapshot,
        actualState: RosterSnapshot,
    ) {
        assertEquals(initialState.troopSlots, actualState.troopSlots)
        assertEquals(initialState.settings, actualState.settings)
        assertEquals(RosterSurface.TROOPS, actualState.surface)
        assertEquals(initialState.copy(surface = RosterSurface.TROOPS), actualState)
    }

    private fun captureScreenshot(fileName: String, rootTag: String? = null) {
        device.waitForIdle()
        val outputDirectory = requireNotNull(context.getExternalFilesDir("fit"))
        check(outputDirectory.mkdirs() || outputDirectory.isDirectory)
        val screenshot = File(outputDirectory, fileName)
        val screenshotWritten = screenshot.outputStream().use { output ->
            val image = if (rootTag == null) {
                composeTestRule.onRoot().captureToImage()
            } else {
                composeTestRule.onNodeWithTag(rootTag).captureToImage()
            }
            image.asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, output)
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
