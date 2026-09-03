package dev.mysd.game.simulation

import dev.myengine.core.DeterministicEngine
import dev.myengine.core.EngineCommand
import dev.myengine.core.EngineSystem
import dev.myengine.core.HashableState
import dev.myengine.core.StableHash
import dev.myengine.core.TextCommand
import dev.myengine.core.Tick
import dev.mysd.game.battle.playable.PlayableBattleCommand
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
    private val playableCommandLog = CommandLog()
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

    /** Advances only complete ticks while active; paused elapsed time is intentionally discarded. */
    fun advance(elapsedMillis: Long): List<SimulationTickResult> {
        require(elapsedMillis >= 0L) { "elapsedMillis must be non-negative." }
        if (stateBox.value.phase == PlayableBattlePhase.PAUSED) {
            return emptyList()
        }
        return simulation.advance(elapsedMillis)
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
        stateBox.value = PlayableBattleEngine.reduceState(stateBox.value, command)
        record(command)
        return snapshot()
    }

    fun buildTower(targetSlotId: ContentId): PlayableBattleSnapshot =
        submit(PlayableBattleCommand.BuildTower(targetSlotId))

    fun spend(
        targetSlotId: ContentId?,
        cost: Int,
    ) = PlayableBattleEngine.spend(stateBox.value, targetSlotId, cost).also { result ->
        stateBox.value = result.state
        record(PlayableBattleCommand.SpendResource(targetSlotId, cost))
    }

    fun canonicalCommandEncoding(): String = playableCommandLog.canonicalEncoding()

    fun inputHash(): String = playableCommandLog.inputHash()

    fun replayHashChain(): String = playableCommandLog.replayHashChain()

    private fun record(command: PlayableBattleCommand) {
        val type: String
        val payload: String
        when (command) {
            PlayableBattleCommand.Pause -> {
                type = "playable-battle.pause"
                payload = ""
            }

            PlayableBattleCommand.Resume -> {
                type = "playable-battle.resume"
                payload = ""
            }

            is PlayableBattleCommand.SpendResource -> {
                type = "playable-battle.spend-resource"
                payload = listOf(command.targetSlotId?.value ?: "", command.cost).joinToString("|")
            }

            is PlayableBattleCommand.BuildTower -> {
                type = "playable-battle.build-tower"
                payload = command.targetSlotId.value
            }
        }
        playableCommandLog.submit(
            scheduledTick = Tick(currentTick),
            type = type,
            payload = payload,
        )
    }

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
            context.state.value = PlayableBattleEngine.tick(context.state.value)
        }
    }
}
