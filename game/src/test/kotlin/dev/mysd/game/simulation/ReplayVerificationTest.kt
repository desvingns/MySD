package dev.mysd.game.simulation

import dev.mysd.game.battle.playable.PlayableBattleEngine
import dev.mysd.game.persistence.PendingCommand
import dev.mysd.game.persistence.RunSave
import dev.mysd.game.persistence.RunSaveCodec
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.readText
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ReplayVerificationTest {
    @Test
    fun equalTracesPassEvenWhenListsAreDifferentInstances() {
        val uninterrupted = trace(1, 2, 3)
        val saveRestored = uninterrupted.map { it.copy() }

        assertTrue(uninterrupted !== saveRestored)
        val result = ReplayVerification.compare(uninterrupted, saveRestored)

        assertTrue(result.passed)
        assertTrue(result.isMatch)
        assertNull(result.mismatch)
        assertNull(result.diagnostic)
        assertEquals(result, ReplayVerification.requireMatch(uninterrupted, saveRestored))
    }

    @Test
    fun reportsFirstStateHashMismatchWithExpectedAndActualValues() {
        val result = ReplayVerification.compare(
            uninterrupted = trace(1, 2, 3),
            saveRestored = trace(1, 2, 3).mapIndexed { index, tick ->
                if (index == 1) tick.copy(stateHash = "restored-different") else tick
            },
        )

        val mismatch = assertNotNull(result.mismatch)
        assertFalse(result.passed)
        assertEquals(1, mismatch.index)
        assertEquals(ReplayMismatchKind.STATE_HASH, mismatch.kind)
        assertEquals(2L, mismatch.tick)
        assertEquals(2L, mismatch.expectedTick)
        assertEquals(2L, mismatch.actualTick)
        assertEquals("hash-2", mismatch.expectedStateHash)
        assertEquals("restored-different", mismatch.actualStateHash)
        assertEquals(
            "Replay mismatch: kind=STATE_HASH, index=1, tick=2, " +
                "expectedTick=2, actualTick=2, expected=hash-2, actual=restored-different",
            mismatch.diagnostic,
        )
    }

    @Test
    fun reportsTickMismatchAndReorderedResultsBeforeHashDifferences() {
        val expected = trace(1, 2, 3)
        val reordered = listOf(expected[1], expected[0], expected[2])

        val result = ReplayVerification.compare(expected, reordered)

        assertEquals(ReplayMismatchKind.TICK, result.mismatch?.kind)
        assertEquals(0, result.mismatch?.index)
        assertEquals(1L, result.mismatch?.expectedTick)
        assertEquals(2L, result.mismatch?.actualTick)
    }

    @Test
    fun reportsMissingResultAtTheFirstAbsentIndex() {
        val result = ReplayVerification.compare(trace(1, 2, 3), trace(1, 2))

        val mismatch = assertNotNull(result.mismatch)
        assertEquals(ReplayMismatchKind.MISSING_RESULT, mismatch.kind)
        assertEquals(2, mismatch.index)
        assertEquals(3L, mismatch.tick)
        assertEquals(3L, mismatch.expectedTick)
        assertNull(mismatch.actualTick)
        assertEquals("hash-3", mismatch.expectedStateHash)
        assertNull(mismatch.actualStateHash)
    }

    @Test
    fun reportsExtraResultAtTheFirstUnexpectedIndex() {
        val result = ReplayVerification.compare(trace(1, 2), trace(1, 2, 3))

        val mismatch = assertNotNull(result.mismatch)
        assertEquals(ReplayMismatchKind.EXTRA_RESULT, mismatch.kind)
        assertEquals(2, mismatch.index)
        assertEquals(3L, mismatch.tick)
        assertNull(mismatch.expectedTick)
        assertEquals(3L, mismatch.actualTick)
        assertNull(mismatch.expectedStateHash)
        assertEquals("hash-3", mismatch.actualStateHash)
    }

    @Test
    fun diagnosticAndRequireMatchFailureAreStable() {
        val first = ReplayVerification.compare(trace(1, 2), trace(1, 9))
        val second = ReplayVerification.compare(trace(1, 2), trace(1, 9))

        assertEquals(first.diagnostic, second.diagnostic)
        val failure = assertFailsWith<IllegalStateException> {
            first.requireMatch()
        }
        assertEquals(first.diagnostic, failure.message)
        assertContains(failure.message.orEmpty(), "index=1")
        assertContains(failure.message.orEmpty(), "expected=hash-2")
        assertContains(failure.message.orEmpty(), "actual=hash-9")
    }

    @Test
    fun activePlayablePayloadRestoresStateAndContinuesTheSameHashTrajectory() {
        val initial = PlayableBattleEngine.initialState(
            initialResource = 100,
            incomePerSecond = 13,
        )
        val targetSlot = initial.slots.first().id
        val uninterrupted = SimulationSession.playableBattle(seed = 73L, initialState = initial)

        uninterrupted.buildTower(targetSlot)
        uninterrupted.advance(7 * SimulationClock.TICK_DURATION_MILLIS)
        val savedSnapshot = uninterrupted.snapshot()
        assertTrue(uninterrupted.state().slots.any { it.towerId != null })
        assertTrue(uninterrupted.state().enemies.isNotEmpty())
        uninterrupted.upgradeTower(targetSlot)
        uninterrupted.spend(targetSlotId = null, cost = 0)

        val saved = RunSave(
            runId = "playable-run-001",
            stageId = uninterrupted.state().stageId.value,
            contentVersion = 2,
            simulationVersion = 1,
            seed = uninterrupted.seed,
            rngState = uninterrupted.rngState,
            tick = uninterrupted.currentTick,
            active = true,
            pendingCommands = listOf(
                PendingCommand(
                    id = 2L,
                    scheduledTick = uninterrupted.currentTick,
                    type = "playable-battle.spend-resource",
                    actorId = null,
                    payload = "|0",
                ),
                PendingCommand(
                    id = 1L,
                    scheduledTick = uninterrupted.currentTick,
                    type = "playable-battle.upgrade-tower",
                    actorId = null,
                    payload = targetSlot.value,
                ),
            ),
            modifiers = emptyList(),
            terminalResult = null,
            playableBattleState = uninterrupted.state(),
        )
        val payload = RunSaveCodec.encode(saved)
        val decoded = RunSaveCodec.decode(payload)

        assertEquals(saved.playableBattleState, decoded.playableBattleState)
        assertEquals(
            listOf(
                PendingCommand(
                    id = 1L,
                    scheduledTick = saved.tick,
                    type = "playable-battle.upgrade-tower",
                    actorId = null,
                    payload = targetSlot.value,
                ),
                PendingCommand(
                    id = 2L,
                    scheduledTick = saved.tick,
                    type = "playable-battle.spend-resource",
                    actorId = null,
                    payload = "|0",
                ),
            ),
            decoded.pendingCommands,
        )

        val outcome = SimulationSession.restorePlayableBattle(payload)
        assertEquals(PlayableBattleRestoreStatus.RESTORED, outcome.status)
        val restored = when (outcome) {
            is PlayableBattleRestoreResult.Restored -> outcome.session
            PlayableBattleRestoreResult.UnsupportedLegacy -> error("Full playable payload was not restored")
        }

        assertEquals(saved.playableBattleState, restored.state())
        assertEquals(savedSnapshot, restored.snapshot())
        assertEquals(uninterrupted.snapshot(), restored.snapshot())
        assertEquals(saved.tick, restored.currentTick)
        assertEquals(saved.rngState, restored.rngState)
        assertEquals(saved.seed, restored.seed)
        assertEquals(saved.simulationVersion, restored.simulationVersion)
        assertEquals(0L, restored.pendingMillis)

        uninterrupted.spend(targetSlotId = null, cost = 0)
        restored.spend(targetSlotId = null, cost = 0)
        val restoredCanonical = restored.canonicalCommandEncoding()
        assertContains(restoredCanonical, "commandCount=3")
        assertContains(restoredCanonical, "command.0.id=1")
        assertContains(restoredCanonical, "command.1.id=2")
        assertContains(restoredCanonical, "command.2.id=3")
        assertTrue(restored.inputHash().isNotBlank())
        assertTrue(restored.replayHashChain().isNotBlank())

        val uninterruptedTrajectory = buildList {
            addAll(uninterrupted.advance(125L))
            addAll(uninterrupted.advance(125L))
        }
        val restoredTrajectory = buildList {
            addAll(restored.advance(125L))
            addAll(restored.advance(125L))
        }

        ReplayVerification.requireMatch(uninterruptedTrajectory, restoredTrajectory)
        assertEquals(uninterrupted.snapshot(), restored.snapshot())
        assertEquals(1, restored.state().slots.first().towerLevel)
        assertTrue(restored.state().enemies.isNotEmpty())
    }

    @Test
    fun contourOnlyRunSaveReturnsExplicitLegacyOutcomeWithoutPlayableState() {
        val legacy = RunSave(
            runId = "legacy-run",
            stageId = "stage-alpha",
            contentVersion = 3,
            simulationVersion = 7,
            seed = -42L,
            rngState = -7L,
            tick = 12L,
            active = true,
            pendingCommands = listOf(PendingCommand(4L, 0L, "place", null, "tower-a")),
            modifiers = listOf("legacy-contour"),
            terminalResult = null,
        )

        val currentContourPayload = RunSaveCodec.encode(legacy)
        val historicalContourPayload = currentContourPayload
            .lineSequence()
            .filterNot { it.startsWith("playableStatePresent=") }
            .joinToString("\n")
            .replaceFirst("schemaVersion=${RunSaveCodec.CURRENT_SCHEMA_VERSION}", "schemaVersion=3")

        assertNull(RunSaveCodec.decode(currentContourPayload).playableBattleState)
        assertNull(RunSaveCodec.decode(historicalContourPayload).playableBattleState)

        val outcomes = listOf(
            SimulationSession.restorePlayableBattle(legacy),
            SimulationSession.restorePlayableBattle(currentContourPayload),
            SimulationSession.restorePlayableBattle(historicalContourPayload),
        )

        outcomes.forEach { outcome ->
            assertEquals(PlayableBattleRestoreStatus.UNSUPPORTED_LEGACY, outcome.status)
            when (outcome) {
                is PlayableBattleRestoreResult.Restored ->
                    error("Legacy contour must not create playable state")
                PlayableBattleRestoreResult.UnsupportedLegacy -> Unit
            }
        }
    }

    @Test
    fun replayVerificationBoundaryContainsNoAndroidImports() {
        val sourceRoot = sequenceOf(
            Path("src/main/kotlin/dev/mysd/game/simulation"),
            Path("game/src/main/kotlin/dev/mysd/game/simulation"),
        ).first { it.exists() && it.isDirectory() }
        val source = sourceRoot.resolve("ReplayVerification.kt").readText()

        assertFalse(Regex("(?m)^import\\s+android\\.").containsMatchIn(source))
        assertFalse(source.contains("android."))
    }

    private fun trace(vararg ticks: Long): List<SimulationTickResult> = ticks.map { tick ->
        SimulationTickResult(
            tick = tick,
            commandsProcessed = 0,
            stateHash = "hash-$tick",
        )
    }
}
