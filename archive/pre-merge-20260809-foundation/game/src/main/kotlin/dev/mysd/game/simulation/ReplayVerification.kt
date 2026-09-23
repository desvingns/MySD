package dev.mysd.game.simulation

/** The first kind of difference found between two tick trajectories. */
enum class ReplayMismatchKind {
    TICK,
    STATE_HASH,
    MISSING_RESULT,
    EXTRA_RESULT,
}

/**
 * Stable evidence for the first replay trajectory difference.
 *
 * A null entry means that the corresponding trajectory has no result at [index]. The
 * [commandsProcessed] field is intentionally not compared: replay verification covers the
 * authoritative tick and state-hash trajectory only.
 */
data class ReplayMismatch(
    val index: Int,
    val kind: ReplayMismatchKind,
    val expected: SimulationTickResult?,
    val actual: SimulationTickResult?,
) {
    val tick: Long?
        get() = expected?.tick ?: actual?.tick

    val expectedTick: Long?
        get() = expected?.tick

    val actualTick: Long?
        get() = actual?.tick

    val expectedStateHash: String?
        get() = expected?.stateHash

    val actualStateHash: String?
        get() = actual?.stateHash

    /** Deterministic, single-line diagnostic suitable for logs and test failures. */
    val diagnostic: String
        get() = buildString {
            append("Replay mismatch: ")
            append("kind=").append(kind)
            append(", index=").append(index)
            append(", tick=").append(tick ?: "<none>")
            append(", expectedTick=").append(expectedTick ?: "<none>")
            append(", actualTick=").append(actualTick ?: "<none>")
            append(", expected=").append(expectedStateHash ?: "<none>")
            append(", actual=").append(actualStateHash ?: "<none>")
        }
}

/** Result of comparing an uninterrupted trajectory with a save/restore trajectory. */
data class ReplayVerificationResult(
    val passed: Boolean,
    val mismatch: ReplayMismatch? = null,
) {
    init {
        require(passed == (mismatch == null)) {
            "A passed replay verification must not contain a mismatch, and a failed one must."
        }
    }

    val isMatch: Boolean
        get() = passed

    val diagnostic: String?
        get() = mismatch?.diagnostic

    /** Returns this result when it passed, otherwise fails with the stable first-mismatch text. */
    fun requireMatch(): ReplayVerificationResult {
        check(passed) { mismatch!!.diagnostic }
        return this
    }
}

/**
 * Compares two already-produced trajectories. This type deliberately does not perform or imply
 * save/restore; lifecycle code supplies the uninterrupted and restored results independently.
 */
object ReplayVerification {
    fun compare(
        uninterrupted: List<SimulationTickResult>,
        saveRestored: List<SimulationTickResult>,
    ): ReplayVerificationResult {
        val sharedSize = minOf(uninterrupted.size, saveRestored.size)
        for (index in 0 until sharedSize) {
            val expected = uninterrupted[index]
            val actual = saveRestored[index]
            if (expected.tick != actual.tick) {
                return mismatch(index, ReplayMismatchKind.TICK, expected, actual)
            }
            if (expected.stateHash != actual.stateHash) {
                return mismatch(index, ReplayMismatchKind.STATE_HASH, expected, actual)
            }
        }

        return when {
            uninterrupted.size > saveRestored.size -> mismatch(
                index = sharedSize,
                kind = ReplayMismatchKind.MISSING_RESULT,
                expected = uninterrupted[sharedSize],
                actual = null,
            )

            saveRestored.size > uninterrupted.size -> mismatch(
                index = sharedSize,
                kind = ReplayMismatchKind.EXTRA_RESULT,
                expected = null,
                actual = saveRestored[sharedSize],
            )

            else -> ReplayVerificationResult(passed = true)
        }
    }

    fun requireMatch(
        uninterrupted: List<SimulationTickResult>,
        saveRestored: List<SimulationTickResult>,
    ): ReplayVerificationResult = compare(uninterrupted, saveRestored).requireMatch()

    private fun mismatch(
        index: Int,
        kind: ReplayMismatchKind,
        expected: SimulationTickResult?,
        actual: SimulationTickResult?,
    ): ReplayVerificationResult = ReplayVerificationResult(
        passed = false,
        mismatch = ReplayMismatch(index, kind, expected, actual),
    )
}
