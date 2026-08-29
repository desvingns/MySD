package dev.mysd.game.campaign

import dev.mysd.game.battle.ActiveBattleIntent
import dev.mysd.game.battle.EnhancementIntent
import dev.mysd.game.content.OriginalContentIds
import dev.mysd.game.persistence.RunSaveCodec
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class CampaignLifecyclePersistenceTest {

    @Test
    fun `active contour round trips through the supported run save`() {
        val session = startedSession()
        session.submit(ActiveBattleIntent.ChangeSpeed)
        session.submit(ActiveBattleIntent.PauseOrResume)
        session.submit(ActiveBattleIntent.SelectBuildAffordance)

        val saved = assertNotNull(session.runSave())
        val restored = restore(saved)

        assertEquals(session.snapshot(), restored.snapshot())
        assertEquals(session.activeBattleSnapshot(), restored.activeBattleSnapshot())
        assertNull(restored.victorySnapshot())
        assertEquals(saved, restored.runSave())
    }

    @Test
    fun `victory contour round trips through the supported terminal run save`() {
        val session = startedSession()
        session.submit(ActiveBattleIntent.OpenEnhancement)
        session.submit(EnhancementIntent.SelectOffer(OriginalContentIds.FOUNDATION_ENHANCEMENT_EMBER_WARD))
        session.submit(ActiveBattleIntent.ResolveVictory)

        val saved = assertNotNull(session.runSave())
        val restored = restore(saved)

        assertEquals(session.snapshot(), restored.snapshot())
        assertEquals(session.activeBattleSnapshot(), restored.activeBattleSnapshot())
        assertEquals(session.victorySnapshot(), restored.victorySnapshot())
        assertEquals(saved, restored.runSave())
    }

    @Test
    fun `save encoding remains canonical across lifecycle restoration`() {
        val session = startedSession()
        val saved = assertNotNull(session.runSave())
        val encoded = RunSaveCodec.encode(saved)

        assertEquals(encoded, RunSaveCodec.encode(RunSaveCodec.decode(encoded)))
        assertEquals(saved, restore(RunSaveCodec.decode(encoded)).runSave())
    }

    private fun startedSession(): CampaignSession {
        val session = AcceptedCampaignFixture.createSession(runSave = null)
        session.submit(CampaignIntent.EnterCampaign)
        session.submit(CampaignIntent.SelectLevel(AcceptedCampaignFixture.STAGE_ID))
        session.submit(CampaignIntent.SelectInitialOption(BattleSetupChoice.OPTION_B))
        session.submit(CampaignIntent.ContinueTutorialSetup)
        session.submit(CampaignIntent.StartBattle)
        return session
    }

    private fun restore(saved: dev.mysd.game.persistence.RunSave): CampaignSession =
        AcceptedCampaignFixture.createSession(
            runSave = RunSaveCodec.decode(RunSaveCodec.encode(saved)),
        )
}
