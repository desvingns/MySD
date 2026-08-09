package dev.mysd.game.campaign

import dev.mysd.game.battle.ActiveBattleIntent
import dev.mysd.game.battle.ActiveBattleSession
import dev.mysd.game.battle.ActiveBattleSnapshot
import dev.mysd.game.battle.EnhancementIntent
import dev.mysd.game.battle.EnhancementSession
import dev.mysd.game.battle.EnhancementSnapshot
import dev.mysd.game.persistence.RunSave

@JvmInline
value class CampaignStageId private constructor(val value: String) {
    companion object {
        private val FORMAT = Regex("[a-z][a-z0-9]*(?:-[a-z0-9]+)*")

        fun of(raw: String): CampaignStageId {
            require(FORMAT.matches(raw)) { "Campaign stage id must be stable and machine-readable." }
            return CampaignStageId(raw)
        }
    }
}

enum class CampaignRoute {
    CLEAN_LAUNCH,
    CAMPAIGN_SELECTION,
    LEVEL_SETUP,
}

enum class LevelSetupOrigin {
    NEW_RUN,
    UNFINISHED_RUN,
}

data class UnfinishedCampaignRun(
    val stageId: CampaignStageId,
)

data class CampaignSnapshot(
    val route: CampaignRoute,
    val acceptedStageIds: List<CampaignStageId>,
    val selectedStageId: CampaignStageId?,
    val setupOrigin: LevelSetupOrigin?,
    val unfinishedRunPromptVisible: Boolean,
    val battleStart: BattleStartTransition? = null,
)

sealed interface CampaignIntent {
    data object EnterCampaign : CampaignIntent

    data class SelectLevel(
        val stageId: CampaignStageId,
    ) : CampaignIntent

    data object CancelUnfinishedRun : CampaignIntent

    data object ContinueUnfinishedRun : CampaignIntent

    data class SelectInitialOption(
        val choice: BattleSetupChoice,
    ) : CampaignIntent

    data object ContinueTutorialSetup : CampaignIntent

    data object StartBattle : CampaignIntent
}

/**
 * Android-free owner of the accepted campaign route.
 *
 * All transitions depend only on the current state and submitted intent. Android receives an
 * immutable snapshot and cannot mutate the authoritative route directly.
 */
class CampaignSession(
    acceptedStageIds: List<CampaignStageId>,
    private val unfinishedRun: UnfinishedCampaignRun?,
) {
    private val stages = acceptedStageIds.toList()

    private var battleSetupSession: BattleSetupSession? = null
    private var activeBattleSession: ActiveBattleSession? = null
    private var enhancementSession: EnhancementSession? = null

    private var state = CampaignSnapshot(
        route = CampaignRoute.CLEAN_LAUNCH,
        acceptedStageIds = stages,
        selectedStageId = null,
        setupOrigin = null,
        unfinishedRunPromptVisible = false,
    )

    init {
        require(stages.isNotEmpty()) { "At least one accepted campaign stage is required." }
        require(stages.distinct().size == stages.size) { "Accepted campaign stages must be unique." }
        require(unfinishedRun == null || unfinishedRun.stageId in stages) {
            "An unfinished run must reference an accepted campaign stage."
        }
    }

    fun snapshot(): CampaignSnapshot = state

    /** Immutable setup snapshot for the selected stage, if level setup has been opened. */
    fun battleSetupSnapshot(): BattleSetupSnapshot? = battleSetupSession?.snapshot()

    /** Immutable active-battle snapshot after the deterministic setup handoff, if started. */
    fun activeBattleSnapshot(): ActiveBattleSnapshot? = activeBattleSession?.snapshot()

    /** Immutable enhancement snapshot after the enhancement-choice surface is opened, if any. */
    fun enhancementSnapshot(): EnhancementSnapshot? = enhancementSession?.snapshot()

    /** Routes touch-to-command input to the authoritative active-battle session. */
    fun submit(intent: ActiveBattleIntent): ActiveBattleSnapshot? {
        val activeBattle = activeBattleSession?.submit(intent) ?: return null
        if (intent == ActiveBattleIntent.OpenEnhancement && activeBattle.enhancementChoiceVisible) {
            enhancementSession = EnhancementSession(
                stageId = activeBattle.stageId,
                selectedSetupChoice = activeBattle.selectedSetupChoice,
            )
        }
        return activeBattle
    }

    /** Routes enhancement input into :game and returns to active battle after selection. */
    fun submit(intent: EnhancementIntent): EnhancementSnapshot? {
        val enhancement = enhancementSession ?: return null
        val snapshot = enhancement.submit(intent)
        if (snapshot.returnToBattle) {
            activeBattleSession?.returnToBattle()
        }
        return snapshot
    }

    fun submit(intent: CampaignIntent): CampaignSnapshot {
        when (intent) {
            is CampaignIntent.SelectInitialOption -> {
                if (state.route == CampaignRoute.LEVEL_SETUP && state.battleStart == null) {
                    battleSetupSession?.selectChoice(intent.choice)
                }
            }

            CampaignIntent.ContinueTutorialSetup -> {
                if (state.route == CampaignRoute.LEVEL_SETUP && state.battleStart == null) {
                    battleSetupSession?.continueTutorial()
                }
            }

            CampaignIntent.StartBattle -> startBattleIfReady()

            else -> {
                val previous = state
                state = reduce(state, intent)
                if (
                    state.route == CampaignRoute.LEVEL_SETUP &&
                    state.selectedStageId != previous.selectedStageId
                ) {
                    battleSetupSession = BattleSetupSession(requireNotNull(state.selectedStageId))
                } else if (state.route != CampaignRoute.LEVEL_SETUP) {
                    battleSetupSession = null
                }
            }
        }
        return state
    }

    private fun startBattleIfReady() {
        val setup = battleSetupSession?.snapshot()
        if (
            state.route != CampaignRoute.LEVEL_SETUP ||
            state.battleStart != null ||
            setup?.canStartBattle != true
        ) {
            return
        }

        state = state.copy(
            battleStart = BattleStartTransition(
                stageId = requireNotNull(state.selectedStageId),
                selectedChoice = setup.selectedChoice,
            ),
        )
        activeBattleSession = ActiveBattleSession(
            stageId = requireNotNull(state.selectedStageId),
            selectedSetupChoice = setup.selectedChoice,
        )
    }

    private fun reduce(
        current: CampaignSnapshot,
        intent: CampaignIntent,
    ): CampaignSnapshot = when (intent) {
        CampaignIntent.EnterCampaign -> {
            if (current.route != CampaignRoute.CLEAN_LAUNCH) current else current.copy(
                route = CampaignRoute.CAMPAIGN_SELECTION,
                unfinishedRunPromptVisible = unfinishedRun != null,
                battleStart = null,
            )
        }

        is CampaignIntent.SelectLevel -> {
            if (
                current.route != CampaignRoute.CAMPAIGN_SELECTION ||
                current.unfinishedRunPromptVisible ||
                intent.stageId !in stages
            ) {
                current
            } else {
                current.toLevelSetup(intent.stageId, LevelSetupOrigin.NEW_RUN)
            }
        }

        CampaignIntent.CancelUnfinishedRun -> {
            if (!current.unfinishedRunPromptVisible) current else current.copy(
                unfinishedRunPromptVisible = false,
            )
        }

        CampaignIntent.ContinueUnfinishedRun -> {
            val run = unfinishedRun
            if (!current.unfinishedRunPromptVisible || run == null) current else {
                current.toLevelSetup(run.stageId, LevelSetupOrigin.UNFINISHED_RUN)
            }
        }

        is CampaignIntent.SelectInitialOption,
        CampaignIntent.ContinueTutorialSetup,
        CampaignIntent.StartBattle,
        -> current
    }

    private fun CampaignSnapshot.toLevelSetup(
        stageId: CampaignStageId,
        origin: LevelSetupOrigin,
    ): CampaignSnapshot = copy(
        route = CampaignRoute.LEVEL_SETUP,
        selectedStageId = stageId,
        setupOrigin = origin,
        unfinishedRunPromptVisible = false,
        battleStart = null,
    )
}

/** Original, balance-free fixture used by the first Android vertical slice. */
object AcceptedCampaignFixture {
    val STAGE_ID: CampaignStageId = CampaignStageId.of("stage-ember-path")

    fun createSession(runSave: RunSave?): CampaignSession = CampaignSession(
        acceptedStageIds = listOf(STAGE_ID),
        unfinishedRun = runSave
            ?.takeIf {
                it.active &&
                    it.terminalResult == null &&
                    it.stageId == STAGE_ID.value
            }
            ?.let { UnfinishedCampaignRun(stageId = STAGE_ID) },
    )
}
