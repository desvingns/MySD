package dev.mysd.game.battle

import dev.mysd.game.campaign.AcceptedCampaignFixture
import dev.mysd.game.campaign.BattleSetupChoice
import dev.mysd.game.content.OriginalContentIds
import dev.mysd.game.simulation.ScenarioFixtureKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
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

    @Test
    fun `victory snapshot rejects a non-victory fixture`() {
        assertFailsWith<IllegalArgumentException> {
            VictorySnapshot(
                stageId = AcceptedCampaignFixture.STAGE_ID,
                fixtureId = ScenarioFixtureKind.ACTIVE_WAVE.stableId,
                selectedSetupChoice = BattleSetupChoice.OPTION_A,
                selectedEnhancementId = OriginalContentIds.FOUNDATION_ENHANCEMENT,
                rewardPanelVisible = true,
            )
        }
    }

    @Test
    fun `repeated reads preserve the immutable reward-panel snapshot`() {
        val session = VictorySession(
            stageId = AcceptedCampaignFixture.STAGE_ID,
            selectedSetupChoice = BattleSetupChoice.OPTION_A,
            selectedEnhancementId = OriginalContentIds.FOUNDATION_ENHANCEMENT_EMBER_WARD,
        )

        val first = session.snapshot()
        val second = session.snapshot()

        assertEquals(first, second)
        assertEquals(ScenarioFixtureKind.VICTORY.stableId, second.fixtureId)
        assertTrue(second.rewardPanelVisible)
    }
}
