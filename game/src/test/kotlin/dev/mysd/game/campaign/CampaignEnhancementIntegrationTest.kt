package dev.mysd.game.campaign

import dev.mysd.game.battle.ActiveBattleIntent
import dev.mysd.game.battle.EnhancementIntent
import dev.mysd.game.content.OriginalContentIds
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CampaignEnhancementIntegrationTest {
    @Test
    fun `active battle opens enhancement and selection returns to active contour`() {
        val session = AcceptedCampaignFixture.createSession(runSave = null)
        session.submit(CampaignIntent.EnterCampaign)
        session.submit(CampaignIntent.SelectLevel(AcceptedCampaignFixture.STAGE_ID))
        session.submit(CampaignIntent.SelectInitialOption(BattleSetupChoice.OPTION_B))
        session.submit(CampaignIntent.ContinueTutorialSetup)
        session.submit(CampaignIntent.StartBattle)

        val open = assertNotNull(session.submit(ActiveBattleIntent.OpenEnhancement))
        assertTrue(open.enhancementChoiceVisible)
        val enhancement = assertNotNull(session.enhancementSnapshot())
        assertTrue(enhancement.allFilterVisible)
        assertTrue(enhancement.refreshAffordanceVisible)
        assertFalse(enhancement.returnToBattle)

        val repeatedOpen = assertNotNull(session.submit(ActiveBattleIntent.OpenEnhancement))
        assertEquals(open, repeatedOpen)
        assertEquals(enhancement, session.enhancementSnapshot())

        val refreshed = assertNotNull(session.submit(EnhancementIntent.RefreshOffers))
        assertEquals(1, refreshed.refreshRevision)
        val repeatedOpenAfterRefresh = assertNotNull(
            session.submit(ActiveBattleIntent.OpenEnhancement),
        )
        assertEquals(refreshed, session.enhancementSnapshot())
        assertEquals(refreshed.refreshRevision, session.enhancementSnapshot()?.refreshRevision)
        assertEquals(refreshed.selectedOfferId, session.enhancementSnapshot()?.selectedOfferId)
        assertEquals(open, repeatedOpenAfterRefresh)

        val selected = assertNotNull(
            session.submit(
                EnhancementIntent.SelectOffer(
                    OriginalContentIds.FOUNDATION_ENHANCEMENT_EMBER_WARD,
                ),
            ),
        )

        assertTrue(selected.returnToBattle)
        assertEquals(
            OriginalContentIds.FOUNDATION_ENHANCEMENT_EMBER_WARD,
            selected.selectedOfferId,
        )
        assertFalse(assertNotNull(session.activeBattleSnapshot()).enhancementChoiceVisible)
        assertEquals(selected, session.enhancementSnapshot())

        val laterOpen = assertNotNull(session.submit(ActiveBattleIntent.OpenEnhancement))
        assertTrue(laterOpen.enhancementChoiceVisible)
        val freshEnhancement = assertNotNull(session.enhancementSnapshot())
        assertEquals(0, freshEnhancement.refreshRevision)
        assertEquals(
            listOf(
                OriginalContentIds.FOUNDATION_ENHANCEMENT,
                OriginalContentIds.FOUNDATION_ENHANCEMENT_EMBER_WARD,
            ),
            freshEnhancement.offers.map { it.id },
        )
        assertFalse(freshEnhancement.returnToBattle)
        assertNull(freshEnhancement.selectedOfferId)
    }
}
