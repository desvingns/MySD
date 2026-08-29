package dev.mysd.android.campaign

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.mysd.android.R
import dev.mysd.android.ui.theme.MySDTheme
import dev.mysd.game.battle.VictorySnapshot
import dev.mysd.game.campaign.AcceptedCampaignFixture
import dev.mysd.game.campaign.BattleSetupChoice
import dev.mysd.game.content.OriginalContentIds
import dev.mysd.game.simulation.ScenarioFixtureKind
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * ST-0005 / BATTLE-VICTORY structural affordance contract.
 *
 * Locks the accepted victory surface observed in the TASK-03.12 visual-QA record: victory title
 * present, reward panel visible with the deferred-reward-safe body, and — the security/payment
 * risk signal — NO reward-claim (AF-0013 / BL-REWARD-CLAIM-001) or rewarded-multiplier
 * (AF-0014 / BL-REWARDED-AD-001) affordances exist. Deferred reward semantics are preserved
 * per DEV-006 / DEV-007.
 */
@RunWith(AndroidJUnit4::class)
class VictoryContentUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private val defaultState = VictorySnapshot(
        stageId = AcceptedCampaignFixture.STAGE_ID,
        fixtureId = ScenarioFixtureKind.VICTORY.stableId,
        selectedSetupChoice = BattleSetupChoice.OPTION_A,
        selectedEnhancementId = OriginalContentIds.FOUNDATION_ENHANCEMENT,
        rewardPanelVisible = true,
    )

    @Test
    fun st0005_rendersVictoryTitleAndDeferredRewardPanel_noClaimOrMultiplier() {
        composeTestRule.setContent {
            MySDTheme(dynamicColor = false) {
                VictoryContent(state = defaultState)
            }
        }

        // Victory title is displayed
        composeTestRule
            .onNodeWithText(context.getString(R.string.victory_title))
            .assertIsDisplayed()

        // Reward panel title and deferred-reward-safe body are shown (rewardPanelVisible = true)
        composeTestRule
            .onNodeWithText(context.getString(R.string.victory_reward_panel_title))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(context.getString(R.string.victory_reward_panel_body))
            .assertIsDisplayed()

        // Security/payment risk signal: no reward-claim, doubling, or transaction affordances.
        // VictoryContent must contain zero interactive controls — only Text composables render
        // the deferred-reward-safe surface. BL-REWARD-CLAIM-001 and BL-REWARDED-AD-001 enforce
        // this boundary; any clickable node here would indicate a regression.
        val interactiveNodes = composeTestRule.onAllNodes(hasClickAction()).fetchSemanticsNodes()
        assertTrue(
            "VictoryContent must not expose any claim, doubling, or transaction affordances;" +
                " found ${interactiveNodes.size} clickable node(s)",
            interactiveNodes.isEmpty(),
        )
    }
}
