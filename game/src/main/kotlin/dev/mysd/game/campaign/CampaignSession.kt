package dev.mysd.game.campaign

import dev.mysd.game.battle.ActiveBattleIntent
import dev.mysd.game.battle.ActiveBattleSession
import dev.mysd.game.battle.ActiveBattleSnapshot
import dev.mysd.game.battle.ActiveBattleSpeedIndicator
import dev.mysd.game.battle.EnhancementIntent
import dev.mysd.game.battle.EnhancementSession
import dev.mysd.game.battle.EnhancementSnapshot
import dev.mysd.game.battle.VictorySession
import dev.mysd.game.battle.VictorySnapshot
import dev.mysd.game.battle.playable.PlayableBattleEngine
import dev.mysd.game.battle.playable.PlayableBattlePhase
import dev.mysd.game.battle.playable.PlayableBattleState
import dev.mysd.game.battle.playable.PlayableBattleTerminal
import dev.mysd.game.content.ContentId
import dev.mysd.game.content.OriginalContentFixtures
import dev.mysd.game.content.OriginalContentIds
import dev.mysd.game.content.PlayableLevelContent
import dev.mysd.game.meta.RosterIntent
import dev.mysd.game.meta.RosterSession
import dev.mysd.game.meta.RosterSnapshot
import dev.mysd.game.persistence.PendingCommand
import dev.mysd.game.persistence.RunSave
import dev.mysd.game.persistence.RunSaveCodec
import dev.mysd.game.persistence.RunTerminalResult
import dev.mysd.game.service.ArenaRequest
import dev.mysd.game.service.ArenaService
import dev.mysd.game.service.ArenaSnapshot
import dev.mysd.game.service.OfflineServiceAdapters
import dev.mysd.game.simulation.PlayableBattleRestoreResult
import dev.mysd.game.simulation.PlayableBattleSession
import dev.mysd.game.simulation.PlayableBattleSnapshot
import dev.mysd.game.simulation.SimulationClock
import dev.mysd.game.simulation.SimulationSession

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
    val rosterOpen: Boolean = false,
    val arenaOpen: Boolean = false,
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

    data object OpenRoster : CampaignIntent

    data object CloseRoster : CampaignIntent

    data object OpenArena : CampaignIntent

    data object CloseArena : CampaignIntent
}

/**
 * Android-free owner of the accepted campaign route.
 *
 * All transitions depend only on the current state and submitted intent. Android receives an
 * immutable snapshot and cannot mutate the authoritative route directly.
 */
class CampaignSession(
    acceptedStageIds: List<CampaignStageId>,
    unfinishedRun: UnfinishedCampaignRun?,
    private val arenaService: ArenaService = OfflineServiceAdapters.foundation().arenaService,
    restoredRunSave: RunSave? = null,
) {
    private val stages = acceptedStageIds.toList()
    private val unfinishedRun: UnfinishedCampaignRun?
    private var lifecycleRunSave: RunSave? = null
    private var persistContourMetadata = true

    private var battleSetupSession: BattleSetupSession? = null
    private var activeBattleSession: ActiveBattleSession? = null

    /**
     * Authoritative Android-free playable-battle simulation for the current run.
     *
     * The session is the canonical owner of the full battle state. The active-battle contour is a
     * presentation projection derived alongside it; the contour markers are never the authoritative
     * source for a supported playable save.
     */
    private var playableBattleSession: PlayableBattleSession? = null
    private var enhancementSession: EnhancementSession? = null
    private var victorySession: VictorySession? = null
    private var rosterSession: RosterSession? = null
    private var arenaState: ArenaSnapshot? = null
    private var selectedEnhancementId: ContentId? = null

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
        val supportedRunSave = restoredRunSave?.takeIf(::isSupportedRunSave)
        this.unfinishedRun = normalizeUnfinishedRun(
            requested = unfinishedRun,
            restoredRunSave = restoredRunSave,
            supportedRunSave = supportedRunSave,
        )
        lifecycleRunSave = supportedRunSave
        persistContourMetadata = supportedRunSave?.playableBattleState == null ||
            supportedRunSave.modifiers.any { it.startsWith(CONTOUR_MARKER_PREFIX) }
        restoreSavedRun(supportedRunSave)
    }

    @Synchronized
    fun snapshot(): CampaignSnapshot = state

    /** Immutable authoritative playable-battle snapshot after a supported run is started or restored. */
    @Synchronized
    fun playableBattleSnapshot(): PlayableBattleSnapshot? = playableBattleSession?.snapshot()

    /** Direct read boundary for the canonical full state owned by the campaign session. */
    @Synchronized
    fun playableBattleState(): PlayableBattleState? = playableBattleSession?.state()

    /** Immutable setup snapshot for the selected stage, if level setup has been opened. */
    @Synchronized
    fun battleSetupSnapshot(): BattleSetupSnapshot? = battleSetupSession?.snapshot()

    /** Immutable active-battle projection derived from the canonical playable state when present. */
    @Synchronized
    fun activeBattleSnapshot(): ActiveBattleSnapshot? {
        val active = activeBattleSession ?: return null
        if (victorySession != null) return null
        val playableState = playableBattleSession?.state()
        if (playableState?.isTerminal == true) return null
        if (playableState != null) syncActiveBattleProjection()
        return active.snapshot()
    }

    /** Immutable enhancement snapshot after the enhancement-choice surface is opened, if any. */
    @Synchronized
    fun enhancementSnapshot(): EnhancementSnapshot? = enhancementSession?.snapshot()

    /** Immutable victory/reward-panel snapshot after the deterministic local handoff, if resolved. */
    @Synchronized
    fun victorySnapshot(): VictorySnapshot? = victorySession?.snapshot()

    /** Immutable roster/settings snapshot after the accepted troops route is opened, if any. */
    @Synchronized
    fun rosterSnapshot(): RosterSnapshot? = rosterSession?.snapshot()

    /** Immutable Arena service-shaped snapshot after the accepted local route is opened, if any. */
    @Synchronized
    fun arenaSnapshot(): ArenaSnapshot? = arenaState

    /**
     * Returns the supported canonical run save for the current campaign contour.
     *
     * The contour markers use the existing RunSave modifiers list as namespaced, presentation
     * restoration metadata. They do not add mechanics or alter the deterministic simulation.
     */
    @Synchronized
    fun runSave(): RunSave? {
        val playableSession = playableBattleSession
        val playableState = playableSession?.state()
        if (playableState?.terminalResult == PlayableBattleTerminal.DEFEAT) {
            val base = lifecycleRunSave ?: newRunSave(CampaignStageId.of(playableState.stageId.value))
            return base.copy(
                stageId = playableState.stageId.value,
                simulationVersion = playableSession.simulationVersion,
                seed = playableSession.seed,
                rngState = playableSession.rngState,
                tick = playableSession.currentTick,
                active = false,
                pendingCommands = playableSession.pendingCommands(),
                terminalResult = RunTerminalResult.DEFEAT,
                playableBattleState = playableState,
            )
        }
        val activeBattle = activeBattleSession
        if (activeBattle == null) {
            // No active contour is projected. A terminal playable save (for example a defeat) is
            // returned frozen and terminal-guarded so it can never resurface as an unfinished
            // active run, while a supported non-terminal save is returned as-is.
            return lifecycleRunSave?.takeIf {
                it.terminalResult != null || it.active
            }
        }

        val activeSnapshot = if (victorySession != null) {
            // Victory remains a contour-only compatibility surface; no active projection is
            // exposed there because legacy saves cannot reconstruct canonical battle entities.
            activeBattle.snapshot()
        } else {
            activeBattleSnapshot() ?: return null
        }
        val phase = when {
            victorySession != null -> PersistedContourPhase.VICTORY
            enhancementSession?.snapshot()?.returnToBattle == false -> PersistedContourPhase.ENHANCEMENT
            else -> PersistedContourPhase.ACTIVE
        }
        val base = lifecycleRunSave ?: newRunSave(activeSnapshot.stageId)
        val terminalResult = if (phase == PersistedContourPhase.VICTORY) {
            RunTerminalResult.VICTORY
        } else {
            null
        }
        // Newly started and active playable runs emit the canonical full playable payload. Existing
        // victory compatibility remains a legacy contour-only save with no authoritative state.
        val playableBattleState = if (phase == PersistedContourPhase.VICTORY) {
            null
        } else {
            playableState
        }
        val canonicalPlayableMetadata = playableSession?.let {
            Triple(
                it.currentTick,
                it.pendingCommands(),
                it.simulationVersion,
            )
        }
        val modifiers = if (persistContourMetadata) {
            contourModifiers(
                baseModifiers = base.modifiers,
                phase = phase,
                active = activeSnapshot,
                enhancement = enhancementSession?.snapshot(),
                selectedEnhancementId = selectedEnhancementId,
                setupOrigin = state.setupOrigin ?: LevelSetupOrigin.UNFINISHED_RUN,
            )
        } else {
            base.modifiers
        }
        return base.copy(
            stageId = activeSnapshot.stageId.value,
            simulationVersion = canonicalPlayableMetadata?.third ?: base.simulationVersion,
            seed = playableSession?.seed ?: base.seed,
            rngState = playableSession?.rngState ?: base.rngState,
            tick = canonicalPlayableMetadata?.first ?: base.tick,
            active = terminalResult == null,
            pendingCommands = canonicalPlayableMetadata?.second ?: base.pendingCommands.toList(),
            modifiers = modifiers,
            terminalResult = terminalResult,
            playableBattleState = playableBattleState,
        )
    }

    /** Routes touch-to-command input to the authoritative active-battle session. */
    @Synchronized
    fun submit(intent: ActiveBattleIntent): ActiveBattleSnapshot? {
        val activeBattleSession = activeBattleSession ?: return null
        if (victorySession != null) {
            syncActiveBattleProjection()
            return activeBattleSession.snapshot()
        }
        val wasEnhancementChoiceVisible = activeBattleSession.snapshot().enhancementChoiceVisible
        if (intent == ActiveBattleIntent.PauseOrResume && playableBattleSession != null) {
            val playable = requireNotNull(playableBattleSession)
            if (playable.state().phase == PlayableBattlePhase.ACTIVE) {
                playable.pause()
            } else {
                playable.resume()
            }
            // The existing contour is synchronous. Apply the queued canonical command at the
            // next fixed tick before publishing the projection and lifecycle save.
            playable.advance(SimulationClock.TICK_DURATION_MILLIS)
        } else {
            activeBattleSession.submit(intent)
        }
        syncActiveBattleProjection()
        val activeBattle = activeBattleSession.snapshot()
        if (
            intent == ActiveBattleIntent.OpenEnhancement &&
            !wasEnhancementChoiceVisible &&
            activeBattle.enhancementChoiceVisible
        ) {
            enhancementSession = EnhancementSession(
                stageId = activeBattle.stageId,
                selectedSetupChoice = activeBattle.selectedSetupChoice,
            )
        }
        if (
            intent == ActiveBattleIntent.ResolveVictory &&
            activeBattleSession.victoryResolutionReady()
        ) {
            val enhancementId = selectedEnhancementId
            if (enhancementId != null) {
                victorySession = VictorySession(
                    stageId = activeBattle.stageId,
                    selectedSetupChoice = activeBattle.selectedSetupChoice,
                    selectedEnhancementId = enhancementId,
                )
            }
        }
        return activeBattleSession.snapshot()
    }

    /** Advances the authoritative playable session and republishes its projection. */
    @Synchronized
    fun advance(elapsedMillis: Long): PlayableBattleSnapshot? {
        val playable = playableBattleSession ?: return null
        playable.advance(elapsedMillis)
        syncActiveBattleProjection()
        if (playable.state().terminalResult == PlayableBattleTerminal.DEFEAT) {
            activeBattleSession = null
            enhancementSession = null
            victorySession = null
            selectedEnhancementId = null
        }
        return playable.snapshot()
    }

    /** Routes enhancement input into :game and returns to active battle after selection. */
    @Synchronized
    fun submit(intent: EnhancementIntent): EnhancementSnapshot? {
        val enhancement = enhancementSession ?: return null
        val snapshot = enhancement.submit(intent)
        if (snapshot.returnToBattle) {
            selectedEnhancementId = snapshot.selectedOfferId
            activeBattleSession?.returnToBattle()
            syncActiveBattleProjection()
        }
        return snapshot
    }

    /** Routes roster and local-settings input into the Android-free roster session. */
    @Synchronized
    fun submit(intent: RosterIntent): RosterSnapshot? {
        if (!state.rosterOpen) return rosterSession?.snapshot()
        return rosterSession?.submit(intent)
    }

    @Synchronized
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
                if (intent == CampaignIntent.CancelUnfinishedRun) {
                    lifecycleRunSave = null
                    playableBattleSession = null
                } else if (intent is CampaignIntent.SelectLevel) {
                    lifecycleRunSave = null
                    playableBattleSession = null
                }
                if (
                    state.route == CampaignRoute.LEVEL_SETUP &&
                    state.selectedStageId != previous.selectedStageId
                ) {
                    battleSetupSession = BattleSetupSession(requireNotNull(state.selectedStageId))
                } else if (state.route != CampaignRoute.LEVEL_SETUP) {
                    battleSetupSession = null
                }
                if (state.rosterOpen && !previous.rosterOpen) {
                    rosterSession = RosterSession()
                } else if (!state.rosterOpen) {
                    rosterSession = null
                }
                if (state.arenaOpen && !previous.arenaOpen) {
                    arenaState = arenaService.request(ArenaRequest())
                } else if (!state.arenaOpen) {
                    arenaState = null
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

        val stageId = requireNotNull(state.selectedStageId)
        // Do not publish an active contour unless its canonical playable owner exists.
        val playable = startPlayableSession(stageId) ?: return
        state = state.copy(
            battleStart = BattleStartTransition(
                stageId = stageId,
                selectedChoice = setup.selectedChoice,
            ),
        )
        playableBattleSession = playable
        activeBattleSession = ActiveBattleSession(
            stageId = stageId,
            selectedSetupChoice = setup.selectedChoice,
        )
        syncActiveBattleProjection()
    }

    private fun startPlayableSession(stageId: CampaignStageId): PlayableBattleSession? =
        playableLevelFor(stageId)?.let { level ->
            SimulationSession.playableBattle(
                seed = 0L,
                initialState = PlayableBattleEngine.initialState(level),
            )
        }

    private fun syncActiveBattleProjection() {
        val active = activeBattleSession ?: return
        val playableState = playableBattleSession?.state() ?: return
        if (playableState.isTerminal) return
        active.synchronizeWithPlayableState(playableState)
    }

    private fun playableLevelFor(stageId: CampaignStageId): PlayableLevelContent? =
        OriginalContentFixtures.foundationPlayableLevel()
            .takeIf { it.stageId.value == stageId.value }

    /**
     * Restores a supported run through the existing Android lifecycle boundary.
     *
     * A full playable payload is the canonical restore path for active and defeat saves; legacy
     * contour-only saves keep the existing compatibility fallback, especially victory.
     */
    private fun restoreSavedRun(runSave: RunSave?) {
        val restored = runSave?.takeIf(::isSupportedRunSave) ?: return
        val playableState = restored.playableBattleState
        if (playableState != null) {
            restorePlayableRun(restored, playableState)
        } else {
            restoreLegacyContour(restored)
        }
    }

    private fun restorePlayableRun(runSave: RunSave, playableState: PlayableBattleState) {
        val restoredSession = try {
            when (val restore = SimulationSession.restorePlayableBattle(runSave)) {
                is PlayableBattleRestoreResult.Restored -> restore.session
                PlayableBattleRestoreResult.UnsupportedLegacy -> return
            }
        } catch (_: IllegalArgumentException) {
            // A direct caller may provide an in-memory malformed payload. The Android boundary
            // already rejects it during decode; this guard keeps the domain seam equally atomic.
            return
        }
        playableBattleSession = restoredSession

        val stageId = CampaignStageId.of(runSave.stageId)
        val contour = parseContour(runSave)
        when (playableState.terminalResult) {
            PlayableBattleTerminal.DEFEAT -> {
                // A terminal playable save stays frozen in the authoritative session. Do not
                // create an active contour or an unfinished-run route for a defeated run.
                return
            }

            PlayableBattleTerminal.VICTORY -> {
                restorePlayableVictory(stageId, contour)
            }

            null -> {
                restorePlayableActive(stageId, contour)
            }
        }
    }

    private fun restorePlayableActive(
        stageId: CampaignStageId,
        contour: RestoredContour?,
    ) {
        val selectedSetupChoice = contour?.selectedSetupChoice
        state = state.copy(
            route = CampaignRoute.LEVEL_SETUP,
            selectedStageId = stageId,
            setupOrigin = contour?.setupOrigin ?: LevelSetupOrigin.UNFINISHED_RUN,
            battleStart = BattleStartTransition(
                stageId = stageId,
                selectedChoice = selectedSetupChoice,
            ),
        )
        activeBattleSession = ActiveBattleSession(
            stageId = stageId,
            selectedSetupChoice = selectedSetupChoice,
        )
        // The full payload owns represented phase. A contour paused marker is only legacy
        // presentation metadata and is ignored when it conflicts with playableState.phase.
        syncActiveBattleProjection()
        if (contour?.speedIndicator == ActiveBattleSpeedIndicator.ALTERNATE) {
            activeBattleSession?.submit(ActiveBattleIntent.ChangeSpeed)
        }
        if (contour?.buildSelected == true) {
            activeBattleSession?.submit(ActiveBattleIntent.SelectBuildAffordance)
        }

        when (contour?.phase) {
            PersistedContourPhase.ACTIVE -> {
                contour.selectedEnhancementId?.let {
                    selectedEnhancementId = it
                    activeBattleSession?.returnToBattle()
                }
            }

            PersistedContourPhase.ENHANCEMENT -> {
                activeBattleSession?.submit(ActiveBattleIntent.OpenEnhancement)
                enhancementSession = EnhancementSession(
                    stageId = stageId,
                    selectedSetupChoice = selectedSetupChoice,
                )
                repeat(contour.refreshRevision) {
                    enhancementSession?.submit(EnhancementIntent.RefreshOffers)
                }
            }

            PersistedContourPhase.VICTORY,
            null,
            -> Unit
        }
        syncActiveBattleProjection()
    }

    private fun restorePlayableVictory(
        stageId: CampaignStageId,
        contour: RestoredContour?,
    ) {
        val selectedSetupChoice = contour?.selectedSetupChoice
        state = state.copy(
            route = CampaignRoute.LEVEL_SETUP,
            selectedStageId = stageId,
            setupOrigin = contour?.setupOrigin ?: LevelSetupOrigin.UNFINISHED_RUN,
            battleStart = BattleStartTransition(
                stageId = stageId,
                selectedChoice = selectedSetupChoice,
            ),
        )
        selectedEnhancementId = contour?.selectedEnhancementId
            ?: OriginalContentIds.FOUNDATION_ENHANCEMENT
        victorySession = VictorySession(
            stageId = stageId,
            selectedSetupChoice = selectedSetupChoice,
            selectedEnhancementId = requireNotNull(selectedEnhancementId),
        )
    }

    private fun restoreLegacyContour(runSave: RunSave?) {
        val restored = runSave?.takeIf { it.stageId in stages.map(CampaignStageId::value) }
            ?.let(::parseContour)
            ?: return

        val stageId = CampaignStageId.of(requireNotNull(runSave).stageId)
        state = state.copy(
            route = CampaignRoute.LEVEL_SETUP,
            selectedStageId = stageId,
            setupOrigin = restored.setupOrigin,
            battleStart = BattleStartTransition(
                stageId = stageId,
                selectedChoice = restored.selectedSetupChoice,
            ),
        )
        activeBattleSession = ActiveBattleSession(
            stageId = stageId,
            selectedSetupChoice = restored.selectedSetupChoice,
        )
        if (restored.speedIndicator == ActiveBattleSpeedIndicator.ALTERNATE) {
            activeBattleSession?.submit(ActiveBattleIntent.ChangeSpeed)
        }
        if (restored.paused) {
            activeBattleSession?.submit(ActiveBattleIntent.PauseOrResume)
        }
        if (restored.buildSelected) {
            activeBattleSession?.submit(ActiveBattleIntent.SelectBuildAffordance)
        }

        when (restored.phase) {
            PersistedContourPhase.ACTIVE -> {
                restored.selectedEnhancementId?.let {
                    selectedEnhancementId = it
                    activeBattleSession?.returnToBattle()
                }
            }

            PersistedContourPhase.ENHANCEMENT -> {
                activeBattleSession?.submit(ActiveBattleIntent.OpenEnhancement)
                enhancementSession = EnhancementSession(
                    stageId = stageId,
                    selectedSetupChoice = restored.selectedSetupChoice,
                )
                repeat(restored.refreshRevision) {
                    enhancementSession?.submit(EnhancementIntent.RefreshOffers)
                }
            }

            PersistedContourPhase.VICTORY -> {
                selectedEnhancementId = restored.selectedEnhancementId
                    ?: OriginalContentIds.FOUNDATION_ENHANCEMENT
                activeBattleSession?.returnToBattle()
                victorySession = VictorySession(
                    stageId = stageId,
                    selectedSetupChoice = restored.selectedSetupChoice,
                    selectedEnhancementId = requireNotNull(selectedEnhancementId),
                )
            }
        }
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
                current.rosterOpen ||
                current.arenaOpen ||
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

        CampaignIntent.OpenRoster -> if (
            current.route == CampaignRoute.CAMPAIGN_SELECTION &&
                !current.unfinishedRunPromptVisible &&
                !current.rosterOpen &&
                !current.arenaOpen
        ) {
            current.copy(rosterOpen = true)
        } else {
            current
        }

        CampaignIntent.CloseRoster -> if (current.rosterOpen) {
            current.copy(rosterOpen = false)
        } else {
            current
        }

        CampaignIntent.OpenArena -> if (
            current.route == CampaignRoute.CAMPAIGN_SELECTION &&
                !current.unfinishedRunPromptVisible &&
                !current.rosterOpen &&
                !current.arenaOpen
        ) {
            current.copy(arenaOpen = true)
        } else {
            current
        }

        CampaignIntent.CloseArena -> if (current.arenaOpen) {
            current.copy(arenaOpen = false)
        } else {
            current
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

    private fun isSupportedRunSave(runSave: RunSave): Boolean =
        stages.firstOrNull { it.value == runSave.stageId }?.let { stageId ->
            val playableState = runSave.playableBattleState
            val playableLevel = playableLevelFor(stageId)
            runSave.hasSupportedEnvelope() &&
                (playableState == null || playableLevel != null && playableState.matches(playableLevel)) &&
                runCatching { RunSaveCodec.encode(runSave) }.isSuccess
        } == true

    private fun normalizeUnfinishedRun(
        requested: UnfinishedCampaignRun?,
        restoredRunSave: RunSave?,
        supportedRunSave: RunSave?,
    ): UnfinishedCampaignRun? {
        if (restoredRunSave == null) return requested
        return supportedRunSave
            ?.takeIf {
                it.active &&
                    it.terminalResult == null &&
                    it.playableBattleState?.isTerminal != true
            }
            ?.let { UnfinishedCampaignRun(CampaignStageId.of(it.stageId)) }
    }
}

private fun RunSave.hasSupportedEnvelope(): Boolean {
    val state = playableBattleState ?: return when {
        active -> terminalResult == null
        else -> terminalResult == RunTerminalResult.VICTORY
    }
    val stateTerminal = when (state.terminalResult) {
        PlayableBattleTerminal.VICTORY -> RunTerminalResult.VICTORY
        PlayableBattleTerminal.DEFEAT -> RunTerminalResult.DEFEAT
        null -> null
    }
    return terminalResult == stateTerminal && active == !state.isTerminal
}

/** Original, balance-free fixture used by the first Android vertical slice. */
object AcceptedCampaignFixture {
    val STAGE_ID: CampaignStageId = CampaignStageId.of("stage-ember-path")

    fun createSession(
        runSave: RunSave?,
        arenaService: ArenaService = OfflineServiceAdapters.foundation().arenaService,
    ): CampaignSession {
        val supportedRunSave = runSave?.takeIf { isSupportedRunSave(it, STAGE_ID) }
        return CampaignSession(
            acceptedStageIds = listOf(STAGE_ID),
            unfinishedRun = supportedRunSave
            ?.takeIf {
                it.active &&
                    it.terminalResult == null &&
                    it.stageId == STAGE_ID.value
            }
            ?.let { UnfinishedCampaignRun(stageId = STAGE_ID) },
            arenaService = arenaService,
            restoredRunSave = supportedRunSave,
        )
    }
}

private fun isSupportedRunSave(runSave: RunSave, stageId: CampaignStageId): Boolean =
    runSave.stageId == stageId.value &&
        runSave.hasSupportedEnvelope() &&
        (runSave.playableBattleState == null ||
            runSave.playableBattleState.matches(OriginalContentFixtures.foundationPlayableLevel())) &&
        runCatching { RunSaveCodec.encode(runSave) }.isSuccess

private fun PlayableBattleState.matches(level: PlayableLevelContent): Boolean =
    stageId == level.stageId &&
        base.id == level.base.id &&
        base.maxHealth == level.base.health &&
        base.positionTicks == level.base.positionTicks &&
        slots.map { it.id to it.positionTicks } == level.buildSlots.map { it.id to it.positionTicks } &&
        towerId == level.tower.id &&
        buildCost == level.tower.buildCost &&
        towerBaseDamage == level.tower.damage &&
        towerBaseCooldownTicks == level.tower.cooldownTicks &&
        towerUpgradeBaseCost == level.tower.upgradeBaseCost &&
        towerUpgradeCostStep == level.tower.upgradeCostStep &&
        towerDamageStep == level.tower.damageStep &&
        towerCooldownStep == level.tower.cooldownStep &&
        towerMinCooldownTicks == level.tower.minCooldownTicks &&
        waveId == level.wave.id &&
        enemyFamilyId == level.enemyFamily.id &&
        enemyHealth == level.enemyFamily.health &&
        enemySpeedTicks == level.enemyFamily.speedTicks &&
        waveSpawnCount == level.wave.spawnCount &&
        waveSpawnIntervalTicks == level.wave.spawnIntervalTicks &&
        towerRangeTicks == level.tower.rangeTicks &&
        baseLeakDamage == level.enemyFamily.baseDamage &&
        slots.all { slot ->
            slot.towerId == null || slot.towerId == towerId
        } &&
        enemies.all {
            it.familyId == level.enemyFamily.id &&
                it.speedTicks == level.enemyFamily.speedTicks
        }

private enum class PersistedContourPhase {
    ACTIVE,
    ENHANCEMENT,
    VICTORY,
}

private data class RestoredContour(
    val phase: PersistedContourPhase,
    val setupOrigin: LevelSetupOrigin,
    val selectedSetupChoice: BattleSetupChoice?,
    val selectedEnhancementId: ContentId?,
    val speedIndicator: ActiveBattleSpeedIndicator,
    val paused: Boolean,
    val buildSelected: Boolean,
    val refreshRevision: Int,
)

private const val CONTOUR_MARKER_PREFIX = "mysd.campaign.contour.v1."
private const val NO_VALUE = "none"
private const val MAX_RESTORED_REFRESHES = 100

private fun marker(key: String, value: String): String = "$CONTOUR_MARKER_PREFIX$key=$value"

private fun markerValue(modifiers: List<String>, key: String): String? = modifiers
    .firstOrNull { it.startsWith("$CONTOUR_MARKER_PREFIX$key=") }
    ?.substringAfter('=', missingDelimiterValue = "")
    ?.takeIf(String::isNotEmpty)

private fun parseContour(runSave: RunSave): RestoredContour? {
    val phase = when (markerValue(runSave.modifiers, "phase")) {
        "active" -> PersistedContourPhase.ACTIVE
        "enhancement" -> PersistedContourPhase.ENHANCEMENT
        "victory" -> PersistedContourPhase.VICTORY
        else -> return null
    }
    if (
        (phase == PersistedContourPhase.VICTORY &&
            (runSave.active || runSave.terminalResult != RunTerminalResult.VICTORY)) ||
        (phase != PersistedContourPhase.VICTORY &&
            (!runSave.active || runSave.terminalResult != null))
    ) {
        return null
    }
    val selectedSetupChoice = markerValue(runSave.modifiers, "setup")
        ?.takeUnless { it == NO_VALUE }
        ?.let { raw -> BattleSetupChoice.entries.firstOrNull { it.stableId == raw } }
    val selectedEnhancementId = markerValue(runSave.modifiers, "enhancement")
        ?.takeUnless { it == NO_VALUE }
        ?.let { raw ->
            when (raw) {
                OriginalContentIds.FOUNDATION_ENHANCEMENT.value -> OriginalContentIds.FOUNDATION_ENHANCEMENT
                OriginalContentIds.FOUNDATION_ENHANCEMENT_EMBER_WARD.value -> OriginalContentIds.FOUNDATION_ENHANCEMENT_EMBER_WARD
                else -> null
            }
        }
    val speedIndicator = when (markerValue(runSave.modifiers, "speed")) {
        ActiveBattleSpeedIndicator.ALTERNATE.name -> ActiveBattleSpeedIndicator.ALTERNATE
        else -> ActiveBattleSpeedIndicator.DEFAULT
    }
    val paused = markerValue(runSave.modifiers, "paused") == "1"
    val buildSelected = markerValue(runSave.modifiers, "build") == "1"
    val refreshRevision = markerValue(runSave.modifiers, "refresh")
        ?.toIntOrNull()
        ?.takeIf { it in 0..MAX_RESTORED_REFRESHES }
        ?: 0
    return RestoredContour(
        phase = phase,
        setupOrigin = when (markerValue(runSave.modifiers, "origin")) {
            LevelSetupOrigin.NEW_RUN.name -> LevelSetupOrigin.NEW_RUN
            else -> LevelSetupOrigin.UNFINISHED_RUN
        },
        selectedSetupChoice = selectedSetupChoice,
        selectedEnhancementId = selectedEnhancementId,
        speedIndicator = speedIndicator,
        paused = paused,
        buildSelected = buildSelected,
        refreshRevision = refreshRevision,
    )
}

private fun contourModifiers(
    baseModifiers: List<String>,
    phase: PersistedContourPhase,
    active: ActiveBattleSnapshot,
    enhancement: EnhancementSnapshot?,
    selectedEnhancementId: ContentId?,
    setupOrigin: LevelSetupOrigin,
): List<String> {
    val preserved = baseModifiers.filterNot { it.startsWith(CONTOUR_MARKER_PREFIX) }
    return preserved + buildList {
        add(marker("phase", phase.name.lowercase()))
        add(marker("origin", setupOrigin.name))
        add(marker("setup", active.selectedSetupChoice?.stableId ?: NO_VALUE))
        add(marker("speed", active.speedIndicator.name))
        add(marker("paused", if (active.paused) "1" else "0"))
        add(marker("build", if (active.buildAffordanceSelected) "1" else "0"))
        add(marker("refresh", enhancement?.refreshRevision?.toString() ?: "0"))
        add(marker("enhancement", selectedEnhancementId?.value ?: NO_VALUE))
    }
}

private fun newRunSave(stageId: CampaignStageId): RunSave = RunSave(
    runId = "campaign-${stageId.value}",
    stageId = stageId.value,
    contentVersion = 1,
    simulationVersion = 1,
    seed = 0L,
    rngState = 0L,
    tick = 0L,
    active = true,
    pendingCommands = emptyList<PendingCommand>(),
    modifiers = emptyList(),
    terminalResult = null,
)
