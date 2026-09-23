package dev.mysd.game.campaign

import dev.mysd.game.battle.ActiveBattleIntent
import dev.mysd.game.battle.EnhancementIntent
import dev.mysd.game.battle.playable.PlayableBattleCommand
import dev.mysd.game.battle.playable.PlayableBattleEngine
import dev.mysd.game.battle.playable.PlayableBattlePhase
import dev.mysd.game.battle.playable.PlayableBattleTerminal
import dev.mysd.game.content.ContentId
import dev.mysd.game.content.OriginalContentIds
import dev.mysd.game.persistence.PendingCommand
import dev.mysd.game.persistence.RunSave
import dev.mysd.game.persistence.RunSaveCodec
import dev.mysd.game.persistence.RunTerminalResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CampaignLifecyclePersistenceTest {

    @Test
    fun `active playable state survives background persistence boundary`() {
        assertActivePlayableRestoresAfter(LifecycleEvent.BACKGROUND)
    }

    @Test
    fun `active playable state survives activity recreation persistence boundary`() {
        assertActivePlayableRestoresAfter(LifecycleEvent.RECREATE)
    }

    @Test
    fun `active playable state survives process death persistence boundary`() {
        assertActivePlayableRestoresAfter(LifecycleEvent.PROCESS_DEATH)
    }

    @Test
    fun `defeat playable state survives background persistence boundary without resume`() {
        assertDefeatPlayableRestoresAfter(LifecycleEvent.BACKGROUND)
    }

    @Test
    fun `defeat playable state survives activity recreation persistence boundary without resume`() {
        assertDefeatPlayableRestoresAfter(LifecycleEvent.RECREATE)
    }

    @Test
    fun `defeat playable state survives process death persistence boundary without resume`() {
        assertDefeatPlayableRestoresAfter(LifecycleEvent.PROCESS_DEATH)
    }

    @Test
    fun `active restore uses full payload even when contour metadata is absent`() {
        val initial = PlayableBattleEngine.initialState(
            initialResource = 73,
            incomePerSecond = 13,
            phase = PlayableBattlePhase.PAUSED,
        )
        val saved = RunSave(
            runId = "active-playable-run-without-contour",
            stageId = AcceptedCampaignFixture.STAGE_ID.value,
            contentVersion = 1,
            simulationVersion = 1,
            seed = 19L,
            rngState = 23L,
            tick = 41L,
            active = true,
            pendingCommands = emptyList(),
            modifiers = emptyList(),
            terminalResult = null,
            playableBattleState = initial.copy(waveElapsedTicks = 17),
        )

        val restored = restoreAcrossNewStorageInstance(saved, LifecycleEvent.PROCESS_DEATH)

        assertEquals(saved.playableBattleState, restored.playableBattleState())
        assertEquals(saved, restored.runSave())
        assertTrue(restored.activeBattleSnapshot()?.paused == true)
        assertFalse(restored.snapshot().unfinishedRunPromptVisible)
    }

    @Test
    fun `active restore treats full playable state as authoritative over contour metadata`() {
        val authoritativeState = PlayableBattleEngine.initialState(
            initialResource = 73,
            incomePerSecond = 13,
            phase = PlayableBattlePhase.PAUSED,
        ).copy(waveElapsedTicks = 17)
        val saved = RunSave(
            runId = "active-playable-run-with-conflicting-contour",
            stageId = AcceptedCampaignFixture.STAGE_ID.value,
            contentVersion = 1,
            simulationVersion = 1,
            seed = 19L,
            rngState = 23L,
            tick = 41L,
            active = true,
            pendingCommands = emptyList(),
            modifiers = listOf(
                "mysd.campaign.contour.v1.phase=active",
                "mysd.campaign.contour.v1.origin=NEW_RUN",
                "mysd.campaign.contour.v1.setup=setup-option-a",
                "mysd.campaign.contour.v1.speed=DEFAULT",
                "mysd.campaign.contour.v1.paused=0",
                "mysd.campaign.contour.v1.build=0",
                "mysd.campaign.contour.v1.refresh=0",
                "mysd.campaign.contour.v1.enhancement=none",
            ),
            terminalResult = null,
            playableBattleState = authoritativeState,
        )

        val restored = restore(saved)

        assertEquals(authoritativeState, restored.playableBattleState())
        assertEquals(PlayableBattlePhase.PAUSED, restored.playableBattleState()?.phase)
        assertTrue(restored.activeBattleSnapshot()?.paused == true)
        assertEquals(authoritativeState, restored.runSave()?.playableBattleState)
    }

    @Test
    fun `active restore ignores stale paused marker when full playable state is active`() {
        val saved = RunSave(
            runId = "active-playable-run-with-stale-paused-marker",
            stageId = AcceptedCampaignFixture.STAGE_ID.value,
            contentVersion = 1,
            simulationVersion = 1,
            seed = 19L,
            rngState = 23L,
            tick = 41L,
            active = true,
            pendingCommands = emptyList(),
            modifiers = listOf(
                "mysd.campaign.contour.v1.phase=active",
                "mysd.campaign.contour.v1.paused=1",
            ),
            terminalResult = null,
            playableBattleState = PlayableBattleEngine.initialState(
                phase = PlayableBattlePhase.ACTIVE,
            ),
        )

        val restored = restore(saved)

        assertEquals(PlayableBattlePhase.ACTIVE, restored.playableBattleState()?.phase)
        assertFalse(restored.activeBattleSnapshot()?.paused == true)
        assertEquals(PlayableBattlePhase.ACTIVE, restored.runSave()?.playableBattleState?.phase)
    }

    @Test
    fun `active battle input and ticks update the canonical playable save`() {
        val session = startedSession()

        session.advance(50L)
        assertEquals(1, session.playableBattleState()?.waveElapsedTicks)
        assertEquals(1L, session.runSave()?.tick)

        session.submit(ActiveBattleIntent.PauseOrResume)

        assertEquals(PlayableBattlePhase.PAUSED, session.playableBattleState()?.phase)
        assertTrue(session.activeBattleSnapshot()?.paused == true)
        assertEquals(PlayableBattlePhase.PAUSED, session.runSave()?.playableBattleState?.phase)

        session.submit(ActiveBattleIntent.PauseOrResume)

        assertEquals(PlayableBattlePhase.ACTIVE, session.playableBattleState()?.phase)
        assertFalse(session.activeBattleSnapshot()?.paused == true)
        assertEquals(PlayableBattlePhase.ACTIVE, session.runSave()?.playableBattleState?.phase)
    }

    @Test
    fun `active projection follows every canonical battle field after restore input and advance`() {
        val initial = PlayableBattleEngine.initialState(
            phase = PlayableBattlePhase.ACTIVE,
            enemies = listOf(
                PlayableBattleEngine.initialState().enemies.single().copy(
                    id = "restored-enemy",
                    positionTicks = 0,
                ),
            ),
            waveSpawnedCount = 1,
        )
        val saved = RunSave(
            runId = "active-playable-projection-run",
            stageId = AcceptedCampaignFixture.STAGE_ID.value,
            contentVersion = 1,
            simulationVersion = 1,
            seed = 19L,
            rngState = 23L,
            tick = 41L,
            active = true,
            pendingCommands = emptyList(),
            modifiers = emptyList(),
            terminalResult = null,
            playableBattleState = initial,
        )

        val restored = restore(saved)
        fun assertProjectionMatchesCanonical() {
            val canonical = assertNotNull(restored.playableBattleState())
            val projection = assertNotNull(restored.activeBattleSnapshot())
            assertEquals(canonical.stageId.value, projection.stageId.value)
            assertEquals(!canonical.isTerminal, projection.waveActive)
            assertEquals(canonical.base.id.value.isNotBlank(), projection.baseVisible)
            assertEquals(canonical.enemies.isNotEmpty(), projection.enemyEntitiesVisible)
            assertEquals(canonical.enemies.map { it.id }, projection.enemyEntityIds)
            assertEquals(canonical.phase == PlayableBattlePhase.PAUSED, projection.paused)
        }

        assertProjectionMatchesCanonical()
        restored.submit(ActiveBattleIntent.PauseOrResume)
        assertProjectionMatchesCanonical()
        restored.submit(ActiveBattleIntent.PauseOrResume)
        restored.advance(1_000L)
        assertProjectionMatchesCanonical()
        assertTrue(restored.playableBattleState()?.enemies.orEmpty().size >= 2)
    }

    @Test
    fun `restored terminal run ignores an independently supplied unfinished run`() {
        val defeated = defeatedRunSave()
        val restored = CampaignSession(
            acceptedStageIds = listOf(AcceptedCampaignFixture.STAGE_ID),
            unfinishedRun = UnfinishedCampaignRun(AcceptedCampaignFixture.STAGE_ID),
            restoredRunSave = defeated,
        )

        val entered = restored.submit(CampaignIntent.EnterCampaign)

        assertFalse(entered.unfinishedRunPromptVisible)
        assertEquals(PlayableBattleTerminal.DEFEAT, restored.playableBattleState()?.terminalResult)
        assertNull(restored.activeBattleSnapshot())
    }

    @Test
    fun `malformed and unsupported run saves are ignored without reconstruction`() {
        val playableState = PlayableBattleEngine.initialState()
        val malformed = RunSave(
            runId = "malformed-playable-run",
            stageId = AcceptedCampaignFixture.STAGE_ID.value,
            contentVersion = 1,
            simulationVersion = 1,
            seed = 19L,
            rngState = 23L,
            tick = 41L,
            active = true,
            pendingCommands = emptyList(),
            modifiers = emptyList(),
            terminalResult = RunTerminalResult.DEFEAT,
            playableBattleState = playableState,
        )
        val unsupported = RunSave(
            runId = "unsupported-stage-run",
            stageId = "stage-cinder-fall",
            contentVersion = 1,
            simulationVersion = 1,
            seed = 19L,
            rngState = 23L,
            tick = 41L,
            active = true,
            pendingCommands = emptyList(),
            modifiers = emptyList(),
            terminalResult = null,
        )
        val mismatchedState = RunSave(
            runId = "mismatched-playable-stage-run",
            stageId = AcceptedCampaignFixture.STAGE_ID.value,
            contentVersion = 1,
            simulationVersion = 1,
            seed = 19L,
            rngState = 23L,
            tick = 41L,
            active = true,
            pendingCommands = emptyList(),
            modifiers = emptyList(),
            terminalResult = null,
            playableBattleState = playableState.copy(
                stageId = ContentId.of("stage-cinder-fall"),
            ),
        )
        val mismatchedContent = RunSave(
            runId = "mismatched-playable-content-run",
            stageId = AcceptedCampaignFixture.STAGE_ID.value,
            contentVersion = 1,
            simulationVersion = 1,
            seed = 19L,
            rngState = 23L,
            tick = 41L,
            active = true,
            pendingCommands = emptyList(),
            modifiers = emptyList(),
            terminalResult = null,
            playableBattleState = playableState.copy(
                towerId = ContentId.of("tower-unknown"),
            ),
        )
        val mismatchedOccupiedTower = RunSave(
            runId = "mismatched-occupied-tower-run",
            stageId = AcceptedCampaignFixture.STAGE_ID.value,
            contentVersion = 1,
            simulationVersion = 1,
            seed = 19L,
            rngState = 23L,
            tick = 41L,
            active = true,
            pendingCommands = emptyList(),
            modifiers = emptyList(),
            terminalResult = null,
            playableBattleState = playableState.copy(
                slots = playableState.slots.mapIndexed { index, slot ->
                    if (index == 0) slot.copy(towerId = ContentId.of("tower-unknown")) else slot
                },
            ),
        )

        listOf(
            malformed,
            unsupported,
            mismatchedState,
            mismatchedContent,
            mismatchedOccupiedTower,
        ).forEach { persistedRun ->
            val session = AcceptedCampaignFixture.createSession(persistedRun)

            assertEquals(CampaignRoute.CLEAN_LAUNCH, session.snapshot().route)
            assertNull(session.runSave())
            assertNull(session.playableBattleState())
            assertFalse(session.submit(CampaignIntent.EnterCampaign).unfinishedRunPromptVisible)
            assertNull(session.activeBattleSnapshot())
        }
    }

    @Test
    fun `legacy terminal envelope without playable defeat state is ignored`() {
        val unsupportedDefeat = RunSave(
            runId = "legacy-defeat-without-state",
            stageId = AcceptedCampaignFixture.STAGE_ID.value,
            contentVersion = 1,
            simulationVersion = 1,
            seed = 19L,
            rngState = 23L,
            tick = 41L,
            active = false,
            pendingCommands = emptyList(),
            modifiers = emptyList(),
            terminalResult = RunTerminalResult.DEFEAT,
        )

        val session = AcceptedCampaignFixture.createSession(unsupportedDefeat)

        assertNull(session.runSave())
        assertFalse(session.submit(CampaignIntent.EnterCampaign).unfinishedRunPromptVisible)
        assertNull(session.playableBattleState())
    }

    @Test
    fun `start battle does not publish contour when accepted stage has no playable content`() {
        val unsupportedStage = CampaignStageId.of("stage-cinder-fall")
        val session = CampaignSession(
            acceptedStageIds = listOf(AcceptedCampaignFixture.STAGE_ID, unsupportedStage),
            unfinishedRun = null,
        )

        session.submit(CampaignIntent.EnterCampaign)
        session.submit(CampaignIntent.SelectLevel(unsupportedStage))
        session.submit(CampaignIntent.SelectInitialOption(BattleSetupChoice.OPTION_A))
        session.submit(CampaignIntent.ContinueTutorialSetup)
        session.submit(CampaignIntent.StartBattle)

        assertEquals(CampaignRoute.LEVEL_SETUP, session.snapshot().route)
        assertNull(session.snapshot().battleStart)
        assertNull(session.activeBattleSnapshot())
        assertNull(session.playableBattleState())
    }

    @Test
    fun `victory contour survives background persistence boundary`() {
        assertVictoryContourRestoresAfter(LifecycleEvent.BACKGROUND)
    }

    @Test
    fun `victory contour survives activity recreation persistence boundary`() {
        assertVictoryContourRestoresAfter(LifecycleEvent.RECREATE)
    }

    @Test
    fun `victory contour survives process death persistence boundary`() {
        assertVictoryContourRestoresAfter(LifecycleEvent.PROCESS_DEATH)
    }

    @Test
    fun `live playable victory emits restorable canonical terminal save and stays frozen`() {
        val initial = PlayableBattleEngine.initialState(
            initialResource = 100,
            phase = PlayableBattlePhase.ACTIVE,
        )
        val nearVictory = initial.copy(
            slots = initial.slots.mapIndexed { index, slot ->
                if (index == 0) slot.copy(towerId = initial.towerId) else slot
            },
            enemies = listOf(
                initial.enemies.single().copy(
                    health = initial.towerBaseDamage,
                    positionTicks = 0,
                ),
            ),
            waveSpawnedCount = initial.waveSpawnCount,
        )
        val nonTerminalSave = RunSave(
            runId = "live-playable-victory-run",
            stageId = AcceptedCampaignFixture.STAGE_ID.value,
            contentVersion = 1,
            simulationVersion = 1,
            seed = 19L,
            rngState = 23L,
            tick = 41L,
            active = true,
            pendingCommands = emptyList(),
            modifiers = listOf(
                "local-test-modifier",
                "mysd.campaign.contour.v1.phase=active",
                "mysd.campaign.contour.v1.origin=NEW_RUN",
                "mysd.campaign.contour.v1.setup=setup-option-b",
                "mysd.campaign.contour.v1.speed=ALTERNATE",
                "mysd.campaign.contour.v1.paused=0",
                "mysd.campaign.contour.v1.build=1",
                "mysd.campaign.contour.v1.refresh=0",
                "mysd.campaign.contour.v1.enhancement=${OriginalContentIds.FOUNDATION_ENHANCEMENT_EMBER_WARD.value}",
            ),
            terminalResult = null,
            playableBattleState = nearVictory,
        )
        val session = restore(nonTerminalSave)

        assertEquals(nonTerminalSave, session.runSave())
        assertEquals(PlayableBattleTerminal.VICTORY, session.advance(50L)?.terminalResult)

        val liveTerminal = assertNotNull(session.playableBattleSnapshot())
        assertNull(session.activeBattleSnapshot())
        listOf(
            ActiveBattleIntent.ChangeSpeed,
            ActiveBattleIntent.PauseOrResume,
            ActiveBattleIntent.SelectBuildAffordance,
            ActiveBattleIntent.OpenEnhancement,
            ActiveBattleIntent.ResolveVictory,
        ).forEach { intent ->
            assertNull(session.submit(intent))
        }
        assertNull(session.submit(EnhancementIntent.RefreshOffers))
        listOf(
            PlayableBattleCommand.Pause,
            PlayableBattleCommand.Resume,
            PlayableBattleCommand.SpendResource(targetSlotId = null, cost = 0),
            PlayableBattleCommand.BuildTower(nearVictory.slots[1].id),
            PlayableBattleCommand.UpgradeTower(nearVictory.slots[0].id),
        ).forEach { command ->
            assertEquals(liveTerminal, session.submit(command))
        }
        assertEquals(liveTerminal, session.advance(5_000L))
        assertEquals(liveTerminal, session.playableBattleSnapshot())

        val terminalSave = assertNotNull(session.runSave())
        assertFalse(terminalSave.active)
        assertEquals(RunTerminalResult.VICTORY, terminalSave.terminalResult)
        assertEquals(PlayableBattleTerminal.VICTORY, terminalSave.playableBattleState?.terminalResult)
        assertEquals(
            listOf(
                "local-test-modifier",
                "mysd.campaign.contour.v1.phase=victory",
                "mysd.campaign.contour.v1.origin=NEW_RUN",
                "mysd.campaign.contour.v1.setup=setup-option-b",
                "mysd.campaign.contour.v1.speed=ALTERNATE",
                "mysd.campaign.contour.v1.paused=0",
                "mysd.campaign.contour.v1.build=1",
                "mysd.campaign.contour.v1.refresh=0",
                "mysd.campaign.contour.v1.enhancement=${OriginalContentIds.FOUNDATION_ENHANCEMENT_EMBER_WARD.value}",
            ),
            terminalSave.modifiers,
        )

        val encoded = RunSaveCodec.encode(terminalSave)
        val decoded = RunSaveCodec.decode(encoded)
        assertEquals(terminalSave, decoded)
        assertEquals(encoded, RunSaveCodec.encode(decoded))

        val restored = AcceptedCampaignFixture.createSession(decoded)
        val frozen = assertNotNull(restored.playableBattleSnapshot())
        assertEquals(PlayableBattleTerminal.VICTORY, frozen.terminalResult)
        assertEquals(LevelSetupOrigin.NEW_RUN, restored.snapshot().setupOrigin)
        assertEquals(BattleSetupChoice.OPTION_B, restored.snapshot().battleStart?.selectedChoice)
        val restoredVictory = assertNotNull(restored.victorySnapshot())
        assertEquals(BattleSetupChoice.OPTION_B, restoredVictory.selectedSetupChoice)
        assertEquals(
            OriginalContentIds.FOUNDATION_ENHANCEMENT_EMBER_WARD,
            restoredVictory.selectedEnhancementId,
        )
        assertNull(restored.activeBattleSnapshot())
        assertNull(restored.submit(ActiveBattleIntent.ChangeSpeed))
        assertEquals(frozen, restored.submit(PlayableBattleCommand.Pause))
        assertEquals(frozen, restored.advance(5_000L))
        assertEquals(frozen, restored.playableBattleSnapshot())
        assertEquals(restoredVictory, restored.victorySnapshot())
        assertEquals(decoded, restored.runSave())
    }

    @Test
    fun `live playable defeat emits restorable canonical terminal save and stays frozen`() {
        val initial = PlayableBattleEngine.initialState(
            initialResource = 100,
            phase = PlayableBattlePhase.ACTIVE,
        )
        val nearDefeat = initial.copy(
            base = initial.base.copy(health = initial.baseLeakDamage),
            enemies = listOf(
                initial.enemies.single().copy(
                    positionTicks = initial.base.positionTicks - initial.enemySpeedTicks,
                ),
            ),
        )
        val nonTerminalSave = RunSave(
            runId = "live-playable-defeat-run",
            stageId = AcceptedCampaignFixture.STAGE_ID.value,
            contentVersion = 1,
            simulationVersion = 1,
            seed = 29L,
            rngState = 31L,
            tick = 43L,
            active = true,
            pendingCommands = emptyList(),
            modifiers = listOf(
                "local-defeat-modifier",
                "mysd.campaign.contour.v1.phase=active",
                "mysd.campaign.contour.v1.origin=NEW_RUN",
                "mysd.campaign.contour.v1.setup=setup-option-a",
                "mysd.campaign.contour.v1.speed=DEFAULT",
                "mysd.campaign.contour.v1.paused=0",
                "mysd.campaign.contour.v1.build=0",
                "mysd.campaign.contour.v1.refresh=0",
                "mysd.campaign.contour.v1.enhancement=none",
            ),
            terminalResult = null,
            playableBattleState = nearDefeat,
        )
        val session = restore(nonTerminalSave)

        assertEquals(nonTerminalSave, session.runSave())
        assertEquals(PlayableBattleTerminal.DEFEAT, session.advance(50L)?.terminalResult)

        val liveTerminal = assertNotNull(session.playableBattleSnapshot())
        assertNull(session.activeBattleSnapshot())
        assertNull(session.submit(ActiveBattleIntent.PauseOrResume))
        assertEquals(liveTerminal, session.submit(PlayableBattleCommand.Pause))
        assertEquals(liveTerminal, session.advance(5_000L))

        val terminalSave = assertNotNull(session.runSave())
        assertFalse(terminalSave.active)
        assertEquals(RunTerminalResult.DEFEAT, terminalSave.terminalResult)
        assertEquals(PlayableBattleTerminal.DEFEAT, terminalSave.playableBattleState?.terminalResult)
        assertEquals(listOf("local-defeat-modifier"), terminalSave.modifiers)

        val encoded = RunSaveCodec.encode(terminalSave)
        val decoded = RunSaveCodec.decode(encoded)
        assertEquals(terminalSave, decoded)
        assertEquals(encoded, RunSaveCodec.encode(decoded))

        val restored = AcceptedCampaignFixture.createSession(decoded)
        val frozen = assertNotNull(restored.playableBattleSnapshot())
        assertEquals(PlayableBattleTerminal.DEFEAT, frozen.terminalResult)
        assertNull(restored.activeBattleSnapshot())
        assertNull(restored.submit(ActiveBattleIntent.ChangeSpeed))
        assertEquals(frozen, restored.submit(PlayableBattleCommand.Pause))
        assertEquals(frozen, restored.advance(5_000L))
        assertEquals(frozen, restored.playableBattleSnapshot())
        assertEquals(decoded, restored.runSave())
    }

    @Test
    fun `resume tick finalizes a live victory through the same terminal boundary`() {
        val initial = PlayableBattleEngine.initialState(
            initialResource = 100,
            phase = PlayableBattlePhase.PAUSED,
        )
        val nearVictory = initial.copy(
            slots = initial.slots.mapIndexed { index, slot ->
                if (index == 0) slot.copy(towerId = initial.towerId) else slot
            },
            enemies = listOf(
                initial.enemies.single().copy(
                    health = initial.towerBaseDamage,
                    positionTicks = 0,
                ),
            ),
            waveSpawnedCount = initial.waveSpawnCount,
        )
        val session = restore(
            RunSave(
                runId = "resume-to-live-victory-run",
                stageId = AcceptedCampaignFixture.STAGE_ID.value,
                contentVersion = 1,
                simulationVersion = 1,
                seed = 37L,
                rngState = 41L,
                tick = 47L,
                active = true,
                pendingCommands = emptyList(),
                modifiers = listOf(
                    "mysd.campaign.contour.v1.phase=active",
                    "mysd.campaign.contour.v1.origin=NEW_RUN",
                    "mysd.campaign.contour.v1.setup=setup-option-a",
                    "mysd.campaign.contour.v1.speed=DEFAULT",
                    "mysd.campaign.contour.v1.paused=1",
                    "mysd.campaign.contour.v1.build=0",
                    "mysd.campaign.contour.v1.refresh=0",
                    "mysd.campaign.contour.v1.enhancement=${OriginalContentIds.FOUNDATION_ENHANCEMENT.value}",
                ),
                terminalResult = null,
                playableBattleState = nearVictory,
            ),
        )

        assertTrue(session.activeBattleSnapshot()?.paused == true)
        assertNull(session.submit(ActiveBattleIntent.PauseOrResume))
        assertEquals(PlayableBattleTerminal.VICTORY, session.playableBattleState()?.terminalResult)
        assertNull(session.activeBattleSnapshot())
        assertNull(session.submit(ActiveBattleIntent.ChangeSpeed))
        val terminalSave = assertNotNull(session.runSave())
        assertEquals(RunTerminalResult.VICTORY, terminalSave.terminalResult)
        assertTrue("mysd.campaign.contour.v1.phase=victory" in terminalSave.modifiers)
        assertTrue(
            "mysd.campaign.contour.v1.enhancement=${OriginalContentIds.FOUNDATION_ENHANCEMENT.value}" in
                terminalSave.modifiers,
        )
    }

    @Test
    fun `legacy contour victory freezes its hidden playable session`() {
        val session = startedSession()
        session.submit(ActiveBattleIntent.OpenEnhancement)
        session.submit(
            EnhancementIntent.SelectOffer(
                OriginalContentIds.FOUNDATION_ENHANCEMENT_EMBER_WARD,
            ),
        )
        session.submit(ActiveBattleIntent.ResolveVictory)

        val victory = assertNotNull(session.victorySnapshot())
        val frozen = assertNotNull(session.playableBattleSnapshot())
        val frozenState = assertNotNull(session.playableBattleState())
        val frozenSave = assertNotNull(session.runSave())
        assertFalse(frozenSave.active)
        assertEquals(RunTerminalResult.VICTORY, frozenSave.terminalResult)
        assertNull(frozenSave.playableBattleState)

        assertEquals(frozen, session.submit(PlayableBattleCommand.Pause))
        assertEquals(frozenSave, session.runSave())
        assertEquals(frozen, session.advance(5_000L))
        assertEquals(frozen, session.playableBattleSnapshot())
        assertEquals(frozenState, session.playableBattleState())
        assertEquals(victory, session.victorySnapshot())
        assertEquals(frozenSave, session.runSave())

        val restored = restore(frozenSave)
        assertEquals(victory, restored.victorySnapshot())
        assertEquals(frozenSave, restored.runSave())
    }

    @Test
    fun `historical contour-only victory save remains compatible`() {
        val currentSave = assertNotNull(victoryContourSession().runSave())
        val legacySave = RunSaveCodec.decode(legacyContourPayload(currentSave))

        assertNull(legacySave.playableBattleState)
        assertEquals(RunTerminalResult.VICTORY, legacySave.terminalResult)

        val restored = AcceptedCampaignFixture.createSession(legacySave)

        assertEquals(CampaignRoute.LEVEL_SETUP, restored.snapshot().route)
        assertEquals(LevelSetupOrigin.NEW_RUN, restored.snapshot().setupOrigin)
        assertNotNull(restored.victorySnapshot())
        assertNull(restored.playableBattleState())
        assertEquals(legacySave, restored.runSave())
    }

    @Test
    fun `save encoding remains canonical across lifecycle restoration`() {
        val session = startedSession()
        val saved = assertNotNull(session.runSave())
        val encoded = RunSaveCodec.encode(saved)

        assertEquals(encoded, RunSaveCodec.encode(RunSaveCodec.decode(encoded)))
        assertEquals(saved, restore(RunSaveCodec.decode(encoded)).runSave())
    }

    private fun assertActivePlayableRestoresAfter(event: LifecycleEvent) {
        val session = activeContourSession()
        val saved = assertNotNull(session.runSave())

        assertTrue(saved.active)
        assertNull(saved.terminalResult)
        assertEquals(session.playableBattleState(), saved.playableBattleState)

        val restored = restoreAcrossNewStorageInstance(saved, event)

        assertEquals(session.snapshot(), restored.snapshot())
        assertEquals(session.activeBattleSnapshot(), restored.activeBattleSnapshot())
        assertEquals(session.playableBattleSnapshot(), restored.playableBattleSnapshot())
        assertEquals(saved.playableBattleState, restored.playableBattleState())
        assertNull(restored.enhancementSnapshot())
        assertNull(restored.victorySnapshot())
        assertEquals(saved, restored.runSave())
    }

    private fun assertDefeatPlayableRestoresAfter(event: LifecycleEvent) {
        val saved = defeatedRunSave()
        val restored = restoreAcrossNewStorageInstance(saved, event)

        assertFalse(restored.snapshot().unfinishedRunPromptVisible)
        assertNull(restored.activeBattleSnapshot())
        assertEquals(PlayableBattleTerminal.DEFEAT, restored.playableBattleSnapshot()?.terminalResult)
        assertEquals(saved.playableBattleState, restored.playableBattleState())
        assertEquals(CampaignRoute.CLEAN_LAUNCH, restored.snapshot().route)
        assertFalse(restored.submit(CampaignIntent.EnterCampaign).unfinishedRunPromptVisible)
        assertNull(restored.submit(ActiveBattleIntent.PauseOrResume))
        assertNull(restored.submit(EnhancementIntent.RefreshOffers))
        assertEquals(saved, restored.runSave())
    }

    private fun assertVictoryContourRestoresAfter(event: LifecycleEvent) {
        val session = victoryContourSession()
        val saved = assertNotNull(session.runSave())

        assertTrue(!saved.active)
        assertEquals(dev.mysd.game.persistence.RunTerminalResult.VICTORY, saved.terminalResult)

        val restored = restoreAcrossNewStorageInstance(saved, event)

        assertEquals(session.snapshot(), restored.snapshot())
        assertEquals(session.activeBattleSnapshot(), restored.activeBattleSnapshot())
        assertEquals(session.victorySnapshot(), restored.victorySnapshot())
        assertEquals(saved, restored.runSave())
    }

    private fun activeContourSession(): CampaignSession {
        val session = startedSession()
        session.submit(ActiveBattleIntent.ChangeSpeed)
        session.submit(ActiveBattleIntent.PauseOrResume)
        session.submit(ActiveBattleIntent.SelectBuildAffordance)
        return session
    }

    private fun victoryContourSession(): CampaignSession {
        val session = activeContourSession()
        session.submit(ActiveBattleIntent.OpenEnhancement)
        session.submit(
            EnhancementIntent.SelectOffer(
                OriginalContentIds.FOUNDATION_ENHANCEMENT_EMBER_WARD,
            ),
        )
        session.submit(ActiveBattleIntent.ResolveVictory)
        return session
    }

    private fun defeatedRunSave(): RunSave {
        val state = PlayableBattleEngine.initialState(
            initialResource = 73,
            incomePerSecond = 13,
            phase = PlayableBattlePhase.PAUSED,
        ).copy(
            base = PlayableBattleEngine.initialState().base.copy(health = 0),
            terminalResult = PlayableBattleTerminal.DEFEAT,
            waveElapsedTicks = 17,
        )
        return RunSave(
            runId = "defeated-playable-run",
            stageId = AcceptedCampaignFixture.STAGE_ID.value,
            contentVersion = 1,
            simulationVersion = 1,
            seed = 19L,
            rngState = 23L,
            tick = 41L,
            active = false,
            pendingCommands = listOf(PendingCommand(3L, 41L, "deferred-command", null, "payload")),
            modifiers = emptyList(),
            terminalResult = RunTerminalResult.DEFEAT,
            playableBattleState = state,
        )
    }

    private fun restoreAcrossNewStorageInstance(
        saved: RunSave,
        event: LifecycleEvent,
    ): CampaignSession {
        val durableStorage = mutableMapOf<LifecycleEvent, String>()
        FakeRunSaveStorage(durableStorage).write(event, saved)
        val encoded = FakeRunSaveStorage(durableStorage).read(event)
        return restore(RunSaveCodec.decode(encoded))
    }

    private fun startedSession(): CampaignSession {
        val session = AcceptedCampaignFixture.createSession(runSave = null)
        session.submit(CampaignIntent.EnterCampaign)
        session.submit(CampaignIntent.SelectLevel(AcceptedCampaignFixture.STAGE_ID))
        session.submit(CampaignIntent.SelectInitialOption(BattleSetupChoice.OPTION_B))
        session.submit(CampaignIntent.ContinueTutorialSetup)
        session.submit(CampaignIntent.StartBattle)
        return session
    }

    private fun restore(saved: dev.mysd.game.persistence.RunSave): CampaignSession =
        AcceptedCampaignFixture.createSession(
            runSave = RunSaveCodec.decode(RunSaveCodec.encode(saved)),
        )

    private fun legacyContourPayload(save: RunSave): String =
        RunSaveCodec.encode(save)
            .lineSequence()
            .filterNot { it.startsWith("playableStatePresent=") }
            .joinToString("\n")
            .replaceFirst(
                "schemaVersion=${RunSaveCodec.CURRENT_SCHEMA_VERSION}",
                "schemaVersion=3",
            )

    private enum class LifecycleEvent {
        BACKGROUND,
        RECREATE,
        PROCESS_DEATH,
    }

    private class FakeRunSaveStorage(
        private val durableStorage: MutableMap<LifecycleEvent, String>,
    ) {
        fun write(event: LifecycleEvent, save: RunSave) {
            durableStorage[event] = RunSaveCodec.encode(save)
        }

        fun read(event: LifecycleEvent): String = checkNotNull(durableStorage[event])
    }
}
