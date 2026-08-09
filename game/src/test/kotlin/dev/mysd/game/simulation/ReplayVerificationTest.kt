package dev.mysd.game.simulation

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
