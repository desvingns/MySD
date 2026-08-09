package dev.mysd.game.battle

import dev.mysd.game.campaign.BattleSetupChoice
import dev.mysd.game.campaign.CampaignStageId
import dev.mysd.game.content.ContentId
import dev.mysd.game.simulation.ScenarioFixtureKind

/** Immutable snapshot of the accepted safe victory contour and its reward-panel shape. */
data class VictorySnapshot(
    val stageId: CampaignStageId,
    val fixtureId: String,
    val selectedSetupChoice: BattleSetupChoice?,
    val selectedEnhancementId: ContentId,
    val rewardPanelVisible: Boolean,
) {
    init {
        require(fixtureId == ScenarioFixtureKind.VICTORY.stableId) {
            "Victory surface must use the accepted victory fixture."
        }
        require(rewardPanelVisible) {
            "Victory surface must expose the reward panel."
        }
    }
}

/** Android-free owner of the deterministic, local victory fixture handoff. */
class VictorySession(
    private val stageId: CampaignStageId,
    private val selectedSetupChoice: BattleSetupChoice?,
    private val selectedEnhancementId: ContentId,
) {
    private val state = VictorySnapshot(
        stageId = stageId,
        fixtureId = ScenarioFixtureKind.VICTORY.stableId,
        selectedSetupChoice = selectedSetupChoice,
        selectedEnhancementId = selectedEnhancementId,
        rewardPanelVisible = true,
    )

    fun snapshot(): VictorySnapshot = state
}
