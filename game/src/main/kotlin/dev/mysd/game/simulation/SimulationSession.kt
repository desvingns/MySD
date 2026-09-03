package dev.mysd.game.simulation

import dev.myengine.core.CommandId
import dev.myengine.core.DeterministicEngine
import dev.myengine.core.EngineCommand
import dev.myengine.core.EngineSystem
import dev.myengine.core.HashableState
import dev.myengine.core.StableHash
import dev.myengine.core.TextCommand
import dev.myengine.core.Tick
import dev.mysd.game.battle.playable.PlayableBattleCommand
import dev.mysd.game.battle.playable.PlayableBattleCommandCodec
import dev.mysd.game.battle.playable.PlayableBattleEngine
import dev.mysd.game.battle.playable.PlayableBattlePhase
import dev.mysd.game.battle.playable.PlayableBattleState
import dev.mysd.game.content.ContentId

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
    private val pendingCommands = linkedMapOf<CommandId, EngineCommand>()

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
        pendingCommands[command.id] = command
        engine.submit(command)
    }

    /** Submits a text command with an id allocated by the session's command log. */
    fun submit(
        scheduledTick: Tick,
        type: String,
        payload: String,
        actorId: Long? = null,
    ): TextCommand {
        val command = TextCommand(
            id = commandLog.allocateId(),
            scheduledTick = scheduledTick,
            type = type,
            payload = payload,
            actorId = actorId,
        )
        submit(command)
        return command
    }

    fun submitAll(commands: Iterable<EngineCommand>) {
        commands.forEach(::submit)
    }

    /** Canonical command input, input hash, and replay chain are immutable value results. */
    fun canonicalCommandEncoding(): String = commandLog.canonicalEncoding()

    fun inputHash(): String = commandLog.inputHash()

    fun replayHashChain(): String = commandLog.replayHashChain()

    internal fun hasPendingCommand(type: String): Boolean = pendingCommands.values.any { it.type == type }

    /**
     * Accumulates elapsed time and advances only complete 50 ms steps.
     *
     * The returned list is the per-tick trajectory for this call; no frame-rate or wall-clock
     * value enters the authoritative engine.
     */
    fun advance(elapsedMillis: Long): List<SimulationTickResult> {
        val ticks = clock.consume(elapsedMillis)
        return engine.step(ticks).map { result ->
            pendingCommands.entries.removeIf { (_, command) ->
                command.scheduledTick.value <= result.tick.value
            }
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

    companion object {
        /** Builds the Android-free fixed-step session for the first playable battle. */
        fun playableBattle(
            seed: Long,
            initialState: PlayableBattleState,
            clock: SimulationClock = SimulationClock(),
        ): PlayableBattleSession = PlayableBattleSession(seed, initialState, clock)
    }
}

/** Immutable read boundary for a playable battle plus simulation timing metadata. */
data class PlayableBattleSnapshot(
    val tick: Long,
    val stateHash: String,
    val pendingMillis: Long,
    val state: PlayableBattleState,
) {
    val phase: PlayableBattlePhase
        get() = state.phase

    val resource: Int
        get() = state.resource

    val globalResource: Int
        get() = state.resource

    val resourceRemainderTicks: Int
        get() = state.incomeRemainderTicks

    val base
        get() = state.base

    val slots
        get() = state.slots

    val enemies
        get() = state.enemies
}

/**
 * Playable-battle adapter over the existing seeded SimulationSession.
 *
 * The adapter owns no Android concerns. It refuses to feed elapsed wall time into the clock while
 * paused, so both the simulation tick and the clock remainder remain frozen until resume.
 */
class PlayableBattleSession(
    seed: Long,
    initialState: PlayableBattleState,
    clock: SimulationClock = SimulationClock(),
) {
    private val stateBox = PlayableBattleStateBox(initialState, seed)
    private val simulation = SimulationSession(
        seed,
        stateBox,
        listOf(PlayableBattleSystem),
        clock,
    )

    val seed: Long = seed

    val currentTick: Long
        get() = simulation.currentTick

    val pendingMillis: Long
        get() = simulation.pendingMillis

    /**
     * Advances complete ticks until the queued pause is applied; paused elapsed time is discarded
     * unless a queued resume command needs a fixed tick to reach the authoritative system.
     */
    fun advance(elapsedMillis: Long): List<SimulationTickResult> {
        require(elapsedMillis >= 0L) { "elapsedMillis must be non-negative." }
        if (stateBox.value.phase == PlayableBattlePhase.PAUSED &&
            !simulation.hasPendingCommand(PlayableBattleCommandCodec.RESUME_TYPE)
        ) {
            return emptyList()
        }

        val results = mutableListOf<SimulationTickResult>()
        var remainingMillis = elapsedMillis
        while (remainingMillis > 0L) {
            val stepMillis = minOf(remainingMillis, SimulationClock.TICK_DURATION_MILLIS)
            results += simulation.advance(stepMillis)
            remainingMillis -= stepMillis
            if (stateBox.value.phase == PlayableBattlePhase.PAUSED &&
                !simulation.hasPendingCommand(PlayableBattleCommandCodec.RESUME_TYPE)
            ) {
                break
            }
        }
        return results
    }

    fun state(): PlayableBattleState = stateBox.value

    fun snapshot(): PlayableBattleSnapshot = PlayableBattleSnapshot(
        tick = currentTick,
        stateHash = simulation.snapshot().stateHash,
        pendingMillis = pendingMillis,
        state = stateBox.value,
    )

    fun pause(): PlayableBattleSnapshot = submit(PlayableBattleCommand.Pause)

    fun resume(): PlayableBattleSnapshot = submit(PlayableBattleCommand.Resume)

    fun submit(command: PlayableBattleCommand): PlayableBattleSnapshot {
        simulation.submit(
            scheduledTick = Tick(currentTick),
            type = PlayableBattleCommandCodec.type(command),
            payload = PlayableBattleCommandCodec.payload(command),
        )
        return snapshot()
    }

    fun buildTower(targetSlotId: ContentId): PlayableBattleSnapshot =
        submit(PlayableBattleCommand.BuildTower(targetSlotId))

    fun spend(
        targetSlotId: ContentId?,
        cost: Int,
    ): PlayableBattleSnapshot = submit(PlayableBattleCommand.SpendResource(targetSlotId, cost))

    fun canonicalCommandEncoding(): String = simulation.canonicalCommandEncoding()

    fun inputHash(): String = simulation.inputHash()

    fun replayHashChain(): String = simulation.replayHashChain()

    private class PlayableBattleStateBox(
        var value: PlayableBattleState,
        private val seed: Long,
    ) : HashableState {
        override fun appendHash(hash: StableHash) {
            hash.add("mysd.playable-battle-session.v1")
                .add(seed)
            value.appendHash(hash)
        }
    }

    private object PlayableBattleSystem : EngineSystem<PlayableBattleStateBox> {
        override val id: String = "playable-battle"
        override val order: Int = 0

        override fun update(context: dev.myengine.core.SimulationContext<PlayableBattleStateBox>) {
            context.commands.forEach { engineCommand ->
                PlayableBattleCommandCodec.decode(engineCommand)?.let { command ->
                    context.state.value = PlayableBattleEngine.reduceState(context.state.value, command)
                }
            }
            context.state.value = PlayableBattleEngine.tick(context.state.value)
        }
    }
}
