package dev.mysd.game.simulation

/**
 * Wall-clock accumulator for the authoritative simulation.
 *
 * The accumulator only counts elapsed time supplied by the caller. It never reads a clock and
 * therefore cannot make authoritative state depend on rendering or platform scheduling.
 */
class SimulationClock(
    private var remainderMillis: Long = 0L,
) {
    init {
        require(remainderMillis in 0 until TICK_DURATION_MILLIS) {
            "Clock remainder must be within one fixed step."
        }
    }

    /** Returns how many complete 50 ms simulation steps are ready to execute. */
    fun consume(elapsedMillis: Long): Int {
        require(elapsedMillis >= 0L) { "elapsedMillis must be non-negative." }
        require(Long.MAX_VALUE - remainderMillis >= elapsedMillis) {
            "elapsedMillis overflows the simulation clock."
        }

        val accumulatedMillis = remainderMillis + elapsedMillis
        val ticks = accumulatedMillis / TICK_DURATION_MILLIS
        remainderMillis = accumulatedMillis % TICK_DURATION_MILLIS
        require(ticks <= Int.MAX_VALUE.toLong()) {
            "A single clock advance cannot produce more than Int.MAX_VALUE ticks."
        }
        return ticks.toInt()
    }

    val pendingMillis: Long
        get() = remainderMillis

    companion object {
        const val TICK_RATE_HZ: Int = 20
        const val TICK_DURATION_MILLIS: Long = 50L
    }
}
