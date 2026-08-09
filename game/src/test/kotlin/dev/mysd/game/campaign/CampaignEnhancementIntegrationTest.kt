package dev.mysd.game.campaign

import dev.mysd.game.battle.ActiveBattleIntent
import dev.mysd.game.battle.EnhancementIntent
import dev.mysd.game.content.OriginalContentIds
import dev.mysd.game.simulation.ScenarioFixtureKind
import dev.mysd.game.simulation.ScenarioPlayability
import dev.mysd.game.simulation.ScenarioTerminalClassification
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CampaignEnhancementIntegrationTest {
    @Test
    fun `resolve victory before enhancement handoff is a safe no-op`() {
        val session = AcceptedCampaignFixture.createSession(runSave = null)
        session.submit(CampaignIntent.EnterCampaign)
        session.submit(CampaignIntent.SelectLevel(AcceptedCampaignFixture.STAGE_ID))
        session.submit(CampaignIntent.SelectInitialOption(BattleSetupChoice.OPTION_A))
        session.submit(CampaignIntent.ContinueTutorialSetup)
        session.submit(CampaignIntent.StartBattle)

        val active = assertNotNull(session.activeBattleSnapshot())
        val attempted = assertNotNull(session.submit(ActiveBattleIntent.ResolveVictory))

        assertEquals(active, attempted)
        assertEquals(active, session.activeBattleSnapshot())
        assertNull(session.victorySnapshot())
        assertFalse(attempted.victoryResolutionAffordanceVisible)
    }

    @Test
    fun `active battle opens enhancement and selection resolves the safe victory contour`() {
        val session = AcceptedCampaignFixture.createSession(runSave = null)
        session.submit(CampaignIntent.EnterCampaign)
        session.submit(CampaignIntent.SelectLevel(AcceptedCampaignFixture.STAGE_ID))
        session.submit(CampaignIntent.SelectInitialOption(BattleSetupChoice.OPTION_B))
        session.submit(CampaignIntent.ContinueTutorialSetup)
        session.submit(CampaignIntent.StartBattle)

        val open = assertNotNull(session.submit(ActiveBattleIntent.OpenEnhancement))
        assertTrue(open.enhancementChoiceVisible)
        val enhancement = assertNotNull(session.enhancementSnapshot())
        assertEquals(2, enhancement.offers.size)
        assertEquals(enhancement.offers.size, enhancement.offers.distinct().size)
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
        assertEquals(1, session.enhancementSnapshot()?.refreshRevision)

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
        val returnedBattle = assertNotNull(session.activeBattleSnapshot())
        assertFalse(returnedBattle.enhancementChoiceVisible)
        assertTrue(returnedBattle.victoryResolutionAffordanceVisible)
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

        val selectedForVictory = assertNotNull(
            session.submit(
                EnhancementIntent.SelectOffer(OriginalContentIds.FOUNDATION_ENHANCEMENT),
            ),
        )
        assertTrue(selectedForVictory.returnToBattle)
        assertTrue(session.activeBattleSnapshot()?.victoryResolutionAffordanceVisible == true)

        session.submit(ActiveBattleIntent.ResolveVictory)

        val victory = assertNotNull(session.victorySnapshot())
        assertEquals(ScenarioFixtureKind.VICTORY.stableId, victory.fixtureId)
        assertEquals(AcceptedCampaignFixture.STAGE_ID, victory.stageId)
        assertEquals(BattleSetupChoice.OPTION_B, victory.selectedSetupChoice)
        assertEquals(
            OriginalContentIds.FOUNDATION_ENHANCEMENT,
            victory.selectedEnhancementId,
        )
        assertTrue(victory.rewardPanelVisible)
        assertEquals(
            ScenarioTerminalClassification.VICTORY,
            ScenarioFixtureKind.VICTORY.terminalClassification,
        )
        assertEquals(
            ScenarioTerminalClassification.STRUCTURED_BLOCKER,
            ScenarioFixtureKind.STRUCTURED_DEFEAT_BLOCKER.terminalClassification,
        )
        assertEquals(
            ScenarioPlayability.BLOCKED,
            ScenarioFixtureKind.STRUCTURED_DEFEAT_BLOCKER.playability,
        )
        assertFalse(ScenarioFixtureKind.STRUCTURED_DEFEAT_BLOCKER.terminalClassification.isTerminal)

        session.submit(ActiveBattleIntent.ResolveVictory)
        assertEquals(victory, session.victorySnapshot())
    }
}
