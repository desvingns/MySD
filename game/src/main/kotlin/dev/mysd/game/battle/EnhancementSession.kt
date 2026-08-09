package dev.mysd.game.battle

import dev.mysd.game.content.ContentId
import dev.mysd.game.content.OriginalContentIds
import dev.mysd.game.campaign.BattleSetupChoice
import dev.mysd.game.campaign.CampaignStageId
import dev.mysd.game.simulation.ScenarioFixtureKind

/**
 * A visible enhancement offer in the accepted contour. The id is content-owned; no display
 * copy, cost, effect, or persistence data is part of this boundary.
 */
data class EnhancementOffer(
    val id: ContentId,
)

data class EnhancementSnapshot(
    val stageId: CampaignStageId,
    val fixtureId: String,
    val selectedSetupChoice: BattleSetupChoice?,
    val offers: List<EnhancementOffer>,
    val allFilterVisible: Boolean,
    val refreshAffordanceVisible: Boolean,
    val refreshRevision: Int,
    val selectedOfferId: ContentId?,
    val returnToBattle: Boolean,
) {
    init {
        require(fixtureId == ScenarioFixtureKind.ENHANCEMENT_CHOICE.stableId) {
            "Enhancement surface must use the accepted enhancement fixture."
        }
        require(offers.isNotEmpty()) { "Enhancement surface requires a visible offer." }
        require(refreshRevision >= 0) { "Enhancement refresh revision cannot be negative." }
        require(selectedOfferId == null || offers.any { it.id == selectedOfferId }) {
            "Selected enhancement must be one of the visible offers."
        }
    }
}

sealed interface EnhancementIntent {
    data object RefreshOffers : EnhancementIntent

    data class SelectOffer(
        val offerId: ContentId,
    ) : EnhancementIntent
}

/**
 * Android-free owner of the accepted enhancement-choice contour.
 *
 * Refresh is intentionally a visible, deterministic contour operation only. It does not reroll
 * offers or attach costs/effects until those mechanics are observed and accepted.
 */
class EnhancementSession(
    private val stageId: CampaignStageId,
    private val selectedSetupChoice: BattleSetupChoice?,
) {
    private var state = EnhancementSnapshot(
        stageId = stageId,
        fixtureId = ScenarioFixtureKind.ENHANCEMENT_CHOICE.stableId,
        selectedSetupChoice = selectedSetupChoice,
        offers = listOf(EnhancementOffer(OriginalContentIds.FOUNDATION_ENHANCEMENT)),
        allFilterVisible = true,
        refreshAffordanceVisible = true,
        refreshRevision = 0,
        selectedOfferId = null,
        returnToBattle = false,
    )

    fun snapshot(): EnhancementSnapshot = state.copy(offers = state.offers.toList())

    fun submit(intent: EnhancementIntent): EnhancementSnapshot {
        state = when (intent) {
            EnhancementIntent.RefreshOffers -> if (
                state.refreshAffordanceVisible && !state.returnToBattle
            ) {
                state.copy(refreshRevision = state.refreshRevision + 1)
            } else {
                state
            }

            is EnhancementIntent.SelectOffer -> if (
                !state.returnToBattle && state.offers.any { it.id == intent.offerId }
            ) {
                state.copy(
                    selectedOfferId = intent.offerId,
                    returnToBattle = true,
                )
            } else {
                state
            }
        }
        return snapshot()
    }
}
