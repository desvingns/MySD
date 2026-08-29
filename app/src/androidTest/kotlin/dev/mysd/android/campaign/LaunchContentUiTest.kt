package dev.mysd.android.campaign

import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.mysd.android.R
import dev.mysd.android.ui.theme.MySDTheme
import dev.mysd.game.campaign.AcceptedCampaignFixture
import dev.mysd.game.campaign.CampaignIntent
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** ST-0001 launch composition and primary-action boundary. */
@RunWith(AndroidJUnit4::class)
class LaunchContentUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

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
            .onNodeWithText(context.getString(R.string.campaign_launch_kicker))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(context.getString(R.string.campaign_launch_action_hint))
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText(context.getString(R.string.campaign_enter_action))
            .assertIsDisplayed()
            .assert(hasClickAction())
            .assertHeightIsAtLeast(48.dp)
            .performClick()

        composeTestRule.runOnIdle {
            assertEquals(listOf(CampaignIntent.EnterCampaign), emittedIntents)
        }
    }
}
