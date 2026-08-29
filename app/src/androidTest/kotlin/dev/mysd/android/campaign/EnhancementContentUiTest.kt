package dev.mysd.android.campaign

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.mysd.android.R
import dev.mysd.android.ui.theme.MySDTheme
import dev.mysd.game.battle.EnhancementIntent
import dev.mysd.game.battle.EnhancementOffer
import dev.mysd.game.battle.EnhancementSnapshot
import dev.mysd.game.campaign.AcceptedCampaignFixture
import dev.mysd.game.campaign.BattleSetupChoice
import dev.mysd.game.content.OriginalContentIds
import dev.mysd.game.simulation.ScenarioFixtureKind
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * ST-0004 / BATTLE-ENHANCEMENT structural affordance contract.
 *
 * Locks the three accepted normalized affordances observed in the TASK-03.12 visual-QA record
 * (AF-0011 select_enhancement_offer, AF-0012 refresh_enhancement_offers, filter label) and
 * verifies that selecting an offer and refreshing emit the corresponding [EnhancementIntent]
 * values. No reference copy, no new UI, no payment or economy affordance.
 */
@RunWith(AndroidJUnit4::class)
class EnhancementContentUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private val defaultState = EnhancementSnapshot(
        stageId = AcceptedCampaignFixture.STAGE_ID,
        fixtureId = ScenarioFixtureKind.ENHANCEMENT_CHOICE.stableId,
        selectedSetupChoice = BattleSetupChoice.OPTION_A,
        offers = listOf(
            EnhancementOffer(OriginalContentIds.FOUNDATION_ENHANCEMENT),
            EnhancementOffer(OriginalContentIds.FOUNDATION_ENHANCEMENT_EMBER_WARD),
        ),
        allFilterVisible = true,
        refreshAffordanceVisible = true,
        refreshRevision = 0,
        selectedOfferId = null,
        returnToBattle = false,
    )

    @Test
    fun st0004_rendersOffersFilterRefresh_emitsAcceptedIntents() {
        val emittedIntents = mutableListOf<EnhancementIntent>()

        composeTestRule.setContent {
            MySDTheme(dynamicColor = false) {
                EnhancementContent(
                    state = defaultState,
                    onIntent = { emittedIntents.add(it) },
                )
            }
        }

        // Section title is displayed
        composeTestRule
            .onNodeWithText(context.getString(R.string.enhancement_title))
            .assertIsDisplayed()

        // Filter label (AF-0012 filter affordance) is displayed
        composeTestRule
            .onNodeWithText(context.getString(R.string.enhancement_filter_all))
            .assertIsDisplayed()

        // Both accepted enhancement offers are displayed (AF-0011)
        val steadyPulseLabel = context.getString(R.string.enhancement_offer_steady_pulse)
        val emberWardLabel = context.getString(R.string.enhancement_offer_ember_ward)
        composeTestRule
            .onNodeWithText(
                context.getString(R.string.enhancement_offer_action, steadyPulseLabel),
            )
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(
                context.getString(R.string.enhancement_offer_action, emberWardLabel),
            )
            .assertIsDisplayed()

        // Refresh affordance is displayed (AF-0012)
        composeTestRule
            .onNodeWithText(context.getString(R.string.enhancement_refresh_action, 0))
            .assertIsDisplayed()

        // Selecting the first offer emits SelectOffer intent for the correct content id
        composeTestRule
            .onNodeWithText(
                context.getString(R.string.enhancement_offer_action, steadyPulseLabel),
            )
            .performClick()
        composeTestRule.runOnIdle {
            assertEquals(1, emittedIntents.size)
            assertEquals(
                EnhancementIntent.SelectOffer(OriginalContentIds.FOUNDATION_ENHANCEMENT),
                emittedIntents.last(),
            )
        }

        // Refresh button emits RefreshOffers intent
        composeTestRule
            .onNodeWithText(context.getString(R.string.enhancement_refresh_action, 0))
            .performClick()
        composeTestRule.runOnIdle {
            assertEquals(2, emittedIntents.size)
            assertEquals(EnhancementIntent.RefreshOffers, emittedIntents.last())
        }
    }
}
