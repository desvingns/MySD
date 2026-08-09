package dev.mysd.game.campaign

import dev.mysd.game.battle.ActiveBattleIntent
import dev.mysd.game.battle.EnhancementIntent
import dev.mysd.game.content.OriginalContentIds
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
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

        val refreshed = assertNotNull(session.submit(EnhancementIntent.RefreshOffers))
        assertEquals(1, refreshed.refreshRevision)
        val selected = assertNotNull(
            session.submit(
                EnhancementIntent.SelectOffer(OriginalContentIds.FOUNDATION_ENHANCEMENT),
            ),
        )

        assertTrue(selected.returnToBattle)
        assertEquals(
            OriginalContentIds.FOUNDATION_ENHANCEMENT,
            selected.selectedOfferId,
        )
        assertFalse(assertNotNull(session.activeBattleSnapshot()).enhancementChoiceVisible)
        assertEquals(selected, session.enhancementSnapshot())
    }
}
