package dev.mysd.game.simulation

import dev.myengine.core.DeterministicEngine
import dev.myengine.core.EngineCommand
import dev.myengine.core.EngineSystem
import dev.myengine.core.HashableState
import dev.myengine.core.TextCommand
import dev.myengine.core.Tick

/** One authoritative result emitted after a fixed simulation tick. */
data class SimulationTickResult(
    val tick: Long,
    val commandsProcessed: Int,
    val stateHash: String,
)

/** Immutable read boundary for the current authoritative state. */
data class SimulationSnapshot(
    val tick: Long,
    val stateHash: String,
)

/**
 * Android-free owner of one deterministic simulation session.
 *
 * Systems are ordered by the explicit MyEngine order, then by their stable id. Commands are
 * accepted through an Android-free command log and are ordered by the pinned engine command
 * comparator. The session is deliberately unaware of content, persistence, rendering, and input
 * concerns.
 */
class SimulationSession<S : HashableState>(
    val seed: Long,
    initialState: S,
    systems: List<EngineSystem<S>>,
    private val clock: SimulationClock = SimulationClock(),
) {
    private val orderedSystems: List<EngineSystem<S>> = systems
        .onEach { require(it.id.isNotBlank()) { "Simulation system id must not be blank." } }
        .also { require(it.map(EngineSystem<S>::id).toSet().size == it.size) { "Simulation system ids must be unique." } }
        .sortedWith(compareBy<EngineSystem<S>> { it.order }.thenBy { it.id })

    private val commandLog = CommandLog()

    private val engine = DeterministicEngine(
        state = initialState,
        systems = orderedSystems,
        seed = seed,
    )

    /** Stable order used for every tick, exposed for diagnostics and verification. */
    val systemOrder: List<String> = orderedSystems.map { it.id }

    val currentTick: Long
        get() = engine.currentTick.value

    val pendingMillis: Long
        get() = clock.pendingMillis

    fun submit(command: EngineCommand) {
        commandLog.append(command)
        engine.submit(command)
    }

    /** Submits a text command with an id allocated by the session's command log. */
    fun submit(
        scheduledTick: Tick,
        type: String,
        payload: String,
        actorId: Long? = null,
    ): TextCommand = commandLog.submit(scheduledTick, type, payload, actorId).also(engine::submit)

    fun submitAll(commands: Iterable<EngineCommand>) {
        commands.forEach(::submit)
    }

    /** Canonical command input, input hash, and replay chain are immutable value results. */
    fun canonicalCommandEncoding(): String = commandLog.canonicalEncoding()

    fun inputHash(): String = commandLog.inputHash()

    fun replayHashChain(): String = commandLog.replayHashChain()

    /**
     * Accumulates elapsed time and advances only complete 50 ms steps.
     *
     * The returned list is the per-tick trajectory for this call; no frame-rate or wall-clock
     * value enters the authoritative engine.
     */
    fun advance(elapsedMillis: Long): List<SimulationTickResult> {
        val ticks = clock.consume(elapsedMillis)
        return engine.step(ticks).map { result ->
            SimulationTickResult(
                tick = result.tick.value,
                commandsProcessed = result.commandsProcessed,
                stateHash = result.stateHash,
            )
        }
    }

    fun snapshot(): SimulationSnapshot = SimulationSnapshot(
        tick = currentTick,
        stateHash = engine.stateHash(),
    )
}
