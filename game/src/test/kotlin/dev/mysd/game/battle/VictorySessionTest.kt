package dev.mysd.game.battle

import dev.mysd.game.campaign.AcceptedCampaignFixture
import dev.mysd.game.campaign.BattleSetupChoice
import dev.mysd.game.content.OriginalContentIds
import dev.mysd.game.simulation.ScenarioFixtureKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VictorySessionTest {
    @Test
    fun `victory session exposes the accepted fixture and immutable reward panel shape`() {
        val snapshot = VictorySession(
            stageId = AcceptedCampaignFixture.STAGE_ID,
            selectedSetupChoice = BattleSetupChoice.OPTION_B,
            selectedEnhancementId = OriginalContentIds.FOUNDATION_ENHANCEMENT,
        ).snapshot()

        assertEquals(ScenarioFixtureKind.VICTORY.stableId, snapshot.fixtureId)
        assertEquals(AcceptedCampaignFixture.STAGE_ID, snapshot.stageId)
        assertEquals(BattleSetupChoice.OPTION_B, snapshot.selectedSetupChoice)
        assertEquals(
            OriginalContentIds.FOUNDATION_ENHANCEMENT,
            snapshot.selectedEnhancementId,
        )
        assertTrue(snapshot.rewardPanelVisible)
    }
}
