package dev.mysd.game.battle

import dev.mysd.game.campaign.BattleSetupChoice
import dev.mysd.game.campaign.CampaignStageId
import dev.mysd.game.battle.playable.PlayableBattleState
import dev.mysd.game.battle.playable.PlayableBattlePhase
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
    val enhancementAffordanceVisible: Boolean,
    val enhancementChoiceVisible: Boolean,
    val victoryResolutionAffordanceVisible: Boolean,
) {
    init {
        require(fixtureId == ScenarioFixtureKind.ACTIVE_WAVE.stableId) {
            "Active battle must use the accepted active-wave fixture."
        }
        require(waveActive) { "Active battle must expose wave activity." }
        require(enemyEntitiesVisible == enemyEntityIds.isNotEmpty()) {
            "Enemy visibility must match the projected enemy entities."
        }
    }
}

sealed interface ActiveBattleIntent {
    data object ChangeSpeed : ActiveBattleIntent

    data object PauseOrResume : ActiveBattleIntent

    data object SelectBuildAffordance : ActiveBattleIntent

    data object OpenEnhancement : ActiveBattleIntent

    data object ResolveVictory : ActiveBattleIntent
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
        enhancementAffordanceVisible = true,
        enhancementChoiceVisible = false,
        victoryResolutionAffordanceVisible = false,
    )

    fun snapshot(): ActiveBattleSnapshot = state.copy(
        enemyEntityIds = state.enemyEntityIds.toList(),
    )

    /**
     * Rebuilds every active-battle field represented by the authoritative playable state.
     *
     * Speed, build, and enhancement fields intentionally remain contour-owned because they are
     * not part of the playable payload. The canonical state owns stage identity, wave/base and
     * enemy visibility, enemy identities, and pause phase.
     */
    fun synchronizeWithPlayableState(playableState: PlayableBattleState): ActiveBattleSnapshot {
        require(!playableState.isTerminal) {
            "A terminal playable state cannot be projected as an active battle."
        }
        val enemyEntityIds = playableState.enemies.map { it.id }
        state = state.copy(
            stageId = CampaignStageId.of(playableState.stageId.value),
            waveActive = playableState.terminalResult == null,
            baseVisible = playableState.base.id.value.isNotBlank(),
            enemyEntitiesVisible = enemyEntityIds.isNotEmpty(),
            enemyEntityIds = enemyEntityIds,
            paused = playableState.phase == PlayableBattlePhase.PAUSED,
        )
        return snapshot()
    }

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

            ActiveBattleIntent.OpenEnhancement -> if (state.enhancementAffordanceVisible) {
                state.copy(
                    enhancementChoiceVisible = true,
                    victoryResolutionAffordanceVisible = false,
                )
            } else {
                state
            }

            ActiveBattleIntent.ResolveVictory -> state
        }
        return snapshot()
    }

    fun returnToBattle(): ActiveBattleSnapshot {
        state = state.copy(
            enhancementChoiceVisible = false,
            victoryResolutionAffordanceVisible = true,
        )
        return snapshot()
    }

    fun victoryResolutionReady(): Boolean =
        state.victoryResolutionAffordanceVisible && !state.enhancementChoiceVisible
}
