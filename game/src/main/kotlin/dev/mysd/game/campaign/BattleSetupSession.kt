package dev.mysd.game.campaign

import dev.mysd.game.simulation.ScenarioFixtureKind

/**
 * The accepted setup choices are identifiers only. Their gameplay effects are intentionally not
 * represented until the corresponding mechanics are observed and accepted.
 */
enum class BattleSetupChoice(
    val stableId: String,
) {
    OPTION_A("setup-option-a"),
    OPTION_B("setup-option-b"),
    OPTION_C("setup-option-c"),
}

data class BattleSetupSnapshot(
    val stageId: CampaignStageId,
    val availableChoices: List<BattleSetupChoice>,
    val selectedChoice: BattleSetupChoice?,
    val tutorialContinuationVisible: Boolean,
    val setupCompleted: Boolean,
) {
    val canStartBattle: Boolean
        get() = setupCompleted
}

private sealed interface BattleSetupIntent {
    data class SelectChoice(
        val choice: BattleSetupChoice,
    ) : BattleSetupIntent

    data object ContinueTutorial : BattleSetupIntent
}

/**
 * Android-free owner of the accepted setup contour for one campaign stage.
 *
 * Selection is deliberately a visible state change only: no cost, multiplier, balance value, or
 * gameplay effect is attached to a choice. Tutorial continuation is the deterministic seam that
 * makes the setup ready for the later active-battle contour.
 */
class BattleSetupSession(
    private val stageId: CampaignStageId,
) {
    private val choices = BattleSetupChoice.entries.toList()

    private var state = BattleSetupSnapshot(
        stageId = stageId,
        availableChoices = choices,
        selectedChoice = null,
        tutorialContinuationVisible = true,
        setupCompleted = false,
    )

    fun snapshot(): BattleSetupSnapshot = state

    fun selectChoice(choice: BattleSetupChoice): BattleSetupSnapshot = submit(
        BattleSetupIntent.SelectChoice(choice),
    )

    fun continueTutorial(): BattleSetupSnapshot = submit(BattleSetupIntent.ContinueTutorial)

    private fun submit(intent: BattleSetupIntent): BattleSetupSnapshot {
        state = when (intent) {
            is BattleSetupIntent.SelectChoice -> state.copy(
                selectedChoice = intent.choice,
            )

            BattleSetupIntent.ContinueTutorial -> if (state.tutorialContinuationVisible) {
                state.copy(
                    tutorialContinuationVisible = false,
                    setupCompleted = true,
                )
            } else {
                state
            }
        }
        return state
    }
}

/**
 * Deterministic handoff emitted by setup. The fixture id identifies the next accepted contour but
 * does not start or implement its active simulation.
 */
data class BattleStartTransition(
    val stageId: CampaignStageId,
    val selectedChoice: BattleSetupChoice?,
    val nextFixtureId: String = ScenarioFixtureKind.ACTIVE_WAVE.stableId,
)
