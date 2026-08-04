package dev.mysd.game.campaign

import dev.mysd.game.simulation.ScenarioFixtureKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CampaignSetupIntegrationTest {
    @Test
    fun `new campaign setup produces a deterministic active-battle handoff`() {
        val session = AcceptedCampaignFixture.createSession(runSave = null)

        assertEquals(CampaignRoute.CLEAN_LAUNCH, session.snapshot().route)
        session.submit(CampaignIntent.EnterCampaign)
        session.submit(CampaignIntent.SelectLevel(AcceptedCampaignFixture.STAGE_ID))

        val setup = assertNotNull(session.battleSetupSnapshot())
        assertEquals(BattleSetupChoice.entries, setup.availableChoices)
        assertNull(setup.selectedChoice)
        assertTrue(setup.tutorialContinuationVisible)
        assertFalse(setup.setupCompleted)

        session.submit(CampaignIntent.SelectInitialOption(BattleSetupChoice.OPTION_C))
        session.submit(CampaignIntent.ContinueTutorialSetup)
        val readySetup = assertNotNull(session.battleSetupSnapshot())
        assertEquals(BattleSetupChoice.OPTION_C, readySetup.selectedChoice)
        assertFalse(readySetup.tutorialContinuationVisible)
        assertTrue(readySetup.canStartBattle)

        val started = session.submit(CampaignIntent.StartBattle)
        assertEquals(
            BattleStartTransition(
                stageId = AcceptedCampaignFixture.STAGE_ID,
                selectedChoice = BattleSetupChoice.OPTION_C,
            ),
            started.battleStart,
        )
        assertEquals(
            ScenarioFixtureKind.ACTIVE_WAVE.stableId,
            started.battleStart?.nextFixtureId,
        )
    }

    @Test
    fun `unfinished-run continuation reaches the same setup handoff without network state`() {
        val session = CampaignSession(
            acceptedStageIds = listOf(AcceptedCampaignFixture.STAGE_ID),
            unfinishedRun = UnfinishedCampaignRun(AcceptedCampaignFixture.STAGE_ID),
        )

        session.submit(CampaignIntent.EnterCampaign)
        session.submit(CampaignIntent.ContinueUnfinishedRun)
        session.submit(CampaignIntent.SelectInitialOption(BattleSetupChoice.OPTION_A))
        session.submit(CampaignIntent.ContinueTutorialSetup)

        val started = session.submit(CampaignIntent.StartBattle)

        assertEquals(LevelSetupOrigin.UNFINISHED_RUN, started.setupOrigin)
        assertEquals(AcceptedCampaignFixture.STAGE_ID, started.battleStart?.stageId)
        assertEquals(BattleSetupChoice.OPTION_A, started.battleStart?.selectedChoice)
        assertEquals(ScenarioFixtureKind.ACTIVE_WAVE.stableId, started.battleStart?.nextFixtureId)
    }
}
