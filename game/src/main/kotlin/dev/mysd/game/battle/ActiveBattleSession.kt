package dev.mysd.game.battle

import dev.mysd.game.campaign.BattleSetupChoice
import dev.mysd.game.campaign.CampaignStageId
import dev.mysd.game.simulation.ScenarioFixtureKind

enum class ActiveBattleSpeedIndicator {
    DEFAULT,
    ALTERNATE,
}

data class ActiveBattleSnapshot(
    val stageId: CampaignStageId,
    val fixtureId: String,
    val selectedSetupChoice: BattleSetupChoice?,
    val waveActive: Boolean,
    val baseVisible: Boolean,
    val enemyEntitiesVisible: Boolean,
    val enemyEntityIds: List<String>,
    val speedAffordanceVisible: Boolean,
    val speedIndicator: ActiveBattleSpeedIndicator,
    val pauseResumeAffordanceVisible: Boolean,
    val paused: Boolean,
    val buildAffordanceVisible: Boolean,
    val buildAffordanceSelected: Boolean,
) {
    init {
        require(fixtureId == ScenarioFixtureKind.ACTIVE_WAVE.stableId) {
            "Active battle must use the accepted active-wave fixture."
        }
        require(waveActive) { "Active battle must expose wave activity." }
        require(enemyEntityIds.isNotEmpty()) { "Active battle requires a visible enemy contour." }
    }
}

sealed interface ActiveBattleIntent {
    data object ChangeSpeed : ActiveBattleIntent

    data object PauseOrResume : ActiveBattleIntent

    data object SelectBuildAffordance : ActiveBattleIntent
}

/**
 * Android-free owner of the accepted active-battle contour.
 *
 * This session deliberately exposes only the observed surface. Speed changes the visible indicator
 * without defining a multiplier, pause/resume changes a coarse contour flag without changing a
 * simulation clock, and build selection records no cost or gameplay effect.
 */
class ActiveBattleSession(
    private val stageId: CampaignStageId,
    private val selectedSetupChoice: BattleSetupChoice?,
) {
    private var state = ActiveBattleSnapshot(
        stageId = stageId,
        fixtureId = ScenarioFixtureKind.ACTIVE_WAVE.stableId,
        selectedSetupChoice = selectedSetupChoice,
        waveActive = true,
        baseVisible = true,
        enemyEntitiesVisible = true,
        enemyEntityIds = listOf("ash-runner"),
        speedAffordanceVisible = true,
        speedIndicator = ActiveBattleSpeedIndicator.DEFAULT,
        pauseResumeAffordanceVisible = true,
        paused = false,
        buildAffordanceVisible = true,
        buildAffordanceSelected = false,
    )

    fun snapshot(): ActiveBattleSnapshot = state.copy(
        enemyEntityIds = state.enemyEntityIds.toList(),
    )

    fun submit(intent: ActiveBattleIntent): ActiveBattleSnapshot {
        state = when (intent) {
            ActiveBattleIntent.ChangeSpeed -> if (state.speedAffordanceVisible) {
                state.copy(
                    speedIndicator = when (state.speedIndicator) {
                        ActiveBattleSpeedIndicator.DEFAULT -> ActiveBattleSpeedIndicator.ALTERNATE
                        ActiveBattleSpeedIndicator.ALTERNATE -> ActiveBattleSpeedIndicator.DEFAULT
                    },
                )
            } else {
                state
            }

            ActiveBattleIntent.PauseOrResume -> if (state.pauseResumeAffordanceVisible) {
                state.copy(paused = !state.paused)
            } else {
                state
            }

            ActiveBattleIntent.SelectBuildAffordance -> if (state.buildAffordanceVisible) {
                state.copy(buildAffordanceSelected = true)
            } else {
                state
            }
        }
        return snapshot()
    }
}
