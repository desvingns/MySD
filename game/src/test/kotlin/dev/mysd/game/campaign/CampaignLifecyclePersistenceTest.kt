package dev.mysd.game.campaign

import dev.mysd.game.battle.ActiveBattleIntent
import dev.mysd.game.battle.EnhancementIntent
import dev.mysd.game.battle.playable.PlayableBattleEngine
import dev.mysd.game.battle.playable.PlayableBattlePhase
import dev.mysd.game.battle.playable.PlayableBattleTerminal
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
