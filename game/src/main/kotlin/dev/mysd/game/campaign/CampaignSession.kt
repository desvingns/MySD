package dev.mysd.game.campaign

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
)

sealed interface CampaignIntent {
    data object EnterCampaign : CampaignIntent

    data class SelectLevel(
        val stageId: CampaignStageId,
    ) : CampaignIntent

    data object CancelUnfinishedRun : CampaignIntent

    data object ContinueUnfinishedRun : CampaignIntent
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

    fun submit(intent: CampaignIntent): CampaignSnapshot {
        state = reduce(state, intent)
        return state
    }

    private fun reduce(
        current: CampaignSnapshot,
        intent: CampaignIntent,
    ): CampaignSnapshot = when (intent) {
        CampaignIntent.EnterCampaign -> {
            if (current.route != CampaignRoute.CLEAN_LAUNCH) current else current.copy(
                route = CampaignRoute.CAMPAIGN_SELECTION,
                unfinishedRunPromptVisible = unfinishedRun != null,
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
    }

    private fun CampaignSnapshot.toLevelSetup(
        stageId: CampaignStageId,
        origin: LevelSetupOrigin,
    ): CampaignSnapshot = copy(
        route = CampaignRoute.LEVEL_SETUP,
        selectedStageId = stageId,
        setupOrigin = origin,
        unfinishedRunPromptVisible = false,
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
