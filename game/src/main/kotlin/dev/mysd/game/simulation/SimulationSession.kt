package dev.mysd.game.simulation

import dev.myengine.core.CommandId
import dev.myengine.core.DeterministicEngine
import dev.myengine.core.EngineCommand
import dev.myengine.core.EngineSystem
import dev.myengine.core.HashableState
import dev.myengine.core.StableHash
import dev.myengine.core.TextCommand
import dev.myengine.core.Tick
import dev.myengine.core.stableHashOf
import dev.mysd.game.battle.playable.PlayableBattleCommand
import dev.mysd.game.battle.playable.PlayableBattleCommandCodec
import dev.mysd.game.battle.playable.PlayableBattleEngine
import dev.mysd.game.battle.playable.PlayableBattlePhase
import dev.mysd.game.battle.playable.PlayableBattleState
import dev.mysd.game.battle.playable.PlayableBattleTerminal
import dev.mysd.game.content.ContentId
import dev.mysd.game.persistence.PendingCommand
import dev.mysd.game.persistence.RunSave
import dev.mysd.game.persistence.RunSaveCodec

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
    private val initialTick: Long = 0L,
) {
    init {
        require(initialTick >= 0L) { "initialTick must be non-negative." }
        require(initialTick <= Long.MAX_VALUE - Int.MAX_VALUE) {
            "initialTick is too close to the maximum supported tick."
        }
    }

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
        get() = logicalTick(engine.currentTick.value)

    val pendingMillis: Long
        get() = clock.pendingMillis

    fun submit(command: EngineCommand) {
        commandLog.append(command)
        pendingCommands[command.id] = command
        engine.submit(command.forEngineTimeline())
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

    /** Returns the pending command metadata on the logical (persisted) timeline. */
    internal fun pendingCommands(): List<PendingCommand> = pendingCommands.values
        .map { command ->
            PendingCommand(
                id = command.id.value,
                scheduledTick = command.scheduledTick.value,
                type = command.type,
                actorId = command.actorId,
                payload = command.stablePayload(),
            )
        }

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
                command.scheduledTick.value <= logicalTick(result.tick.value)
            }
            SimulationTickResult(
                tick = logicalTick(result.tick.value),
                commandsProcessed = result.commandsProcessed,
                stateHash = stateHashAt(logicalTick(result.tick.value)),
            )
        }
    }

    fun snapshot(): SimulationSnapshot = SimulationSnapshot(
        tick = currentTick,
        stateHash = stateHashAt(currentTick),
    )

    private fun logicalTick(engineTick: Long): Long = Math.addExact(initialTick, engineTick)

    /** Recomputes the engine hash with the saved logical tick instead of its relative restore tick. */
    private fun stateHashAt(tick: Long): String = stableHashOf {
        add(tick)
        engine.state.appendHash(this)
    }

    /** Schedules restored commands on the relative engine timeline while retaining canonical input metadata. */
    private fun EngineCommand.forEngineTimeline(): EngineCommand {
        if (initialTick == 0L) return this
        val relativeTick = if (scheduledTick.value <= initialTick) {
            0L
        } else {
            scheduledTick.value - initialTick
        }
        return RelativeScheduledCommand(this, Tick(relativeTick))
    }

    private data class RelativeScheduledCommand(
        private val source: EngineCommand,
        override val scheduledTick: Tick,
    ) : EngineCommand {
        override val id: CommandId
            get() = source.id

        override val actorId: Long?
            get() = source.actorId

        override val type: String
            get() = source.type

        override fun stablePayload(): String = source.stablePayload()
    }

    companion object {
        /** Builds the Android-free fixed-step session for the first playable battle. */
        fun playableBattle(
            seed: Long,
            initialState: PlayableBattleState,
            clock: SimulationClock = SimulationClock(),
        ): PlayableBattleSession = PlayableBattleSession(seed, initialState, clock)

        /** Restores only a full playable payload; legacy contour-only saves have no session result. */
        fun restorePlayableBattle(
            runSave: RunSave,
            clock: SimulationClock = SimulationClock(),
        ): PlayableBattleRestoreResult = PlayableBattleSession.restore(runSave, clock)

        /** Decodes the existing RunSave document before entering the same restore seam. */
        fun restorePlayableBattle(
            payload: String,
            clock: SimulationClock = SimulationClock(),
        ): PlayableBattleRestoreResult = restorePlayableBattle(RunSaveCodec.decode(payload), clock)

        /** Alias emphasizing that the result is a newly constructed playable session. */
        fun restorePlayableBattleSession(
            runSave: RunSave,
            clock: SimulationClock = SimulationClock(),
        ): PlayableBattleRestoreResult = restorePlayableBattle(runSave, clock)
    }
}

enum class PlayableBattleRestoreStatus {
    RESTORED,
    UNSUPPORTED_LEGACY,
}

/** Explicit result boundary for full-state restore versus legacy contour-only absence. */
sealed interface PlayableBattleRestoreResult {
    val status: PlayableBattleRestoreStatus

    data class Restored(
        val session: PlayableBattleSession,
    ) : PlayableBattleRestoreResult {
        override val status: PlayableBattleRestoreStatus = PlayableBattleRestoreStatus.RESTORED

        val value: PlayableBattleSession
            get() = session
    }

    data object UnsupportedLegacy : PlayableBattleRestoreResult {
        override val status: PlayableBattleRestoreStatus = PlayableBattleRestoreStatus.UNSUPPORTED_LEGACY
    }
}

typealias PlayableBattleRestoreOutcome = PlayableBattleRestoreResult

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

    val terminalResult: PlayableBattleTerminal?
        get() = state.terminalResult

    val terminal: PlayableBattleTerminal?
        get() = state.terminalResult

    val waveSpawnCount: Int
        get() = state.waveSpawnCount

    val waveSpawnedCount: Int
        get() = state.waveSpawnedCount

    val pendingEnemiesCount: Int
        get() = state.pendingEnemiesCount
}

/**
 * Playable-battle adapter over the existing seeded SimulationSession.
 *
 * The adapter owns no Android concerns. It refuses to feed elapsed wall time into the clock while
 * paused, so both the simulation tick and the clock remainder remain frozen until resume.
 */
class PlayableBattleSession private constructor(
    val seed: Long,
    initialState: PlayableBattleState,
    clock: SimulationClock,
    initialTick: Long,
    val rngState: Long,
    val simulationVersion: Int,
    pendingCommands: List<PendingCommand>,
) {
    constructor(
        seed: Long,
        initialState: PlayableBattleState,
        clock: SimulationClock = SimulationClock(),
    ) : this(
        seed = seed,
        initialState = initialState,
        clock = clock,
        initialTick = 0L,
        rngState = 0L,
        simulationVersion = CURRENT_SIMULATION_VERSION,
        pendingCommands = emptyList(),
    )

    private val stateBox = PlayableBattleStateBox(initialState, seed)
    private val simulation = SimulationSession(
        seed,
        stateBox,
        listOf(PlayableBattleSystem),
        clock,
        initialTick,
    )

    init {
        pendingCommands.forEach { command ->
            simulation.submit(
                TextCommand(
                    id = CommandId(command.id),
                    scheduledTick = Tick(command.scheduledTick),
                    type = command.type,
                    payload = command.payload,
                    actorId = command.actorId,
                ),
            )
        }
    }

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
        if (stateBox.value.isTerminal) {
            return emptyList()
        }
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
            if ((stateBox.value.phase == PlayableBattlePhase.PAUSED || stateBox.value.isTerminal) &&
                !simulation.hasPendingCommand(PlayableBattleCommandCodec.RESUME_TYPE)
            ) {
                break
            }
        }
        return results
    }

    fun state(): PlayableBattleState = stateBox.value

    /** Returns pending commands using the same metadata shape as RunSave. */
    fun pendingCommands(): List<PendingCommand> = simulation.pendingCommands()

    fun snapshot(): PlayableBattleSnapshot = PlayableBattleSnapshot(
        tick = currentTick,
        stateHash = simulation.snapshot().stateHash,
        pendingMillis = pendingMillis,
        state = stateBox.value,
    )

    fun pause(): PlayableBattleSnapshot = submit(PlayableBattleCommand.Pause)

    fun resume(): PlayableBattleSnapshot = submit(PlayableBattleCommand.Resume)

    fun submit(command: PlayableBattleCommand): PlayableBattleSnapshot {
        if (stateBox.value.isTerminal) {
            return snapshot()
        }
        simulation.submit(
            scheduledTick = Tick(currentTick),
            type = PlayableBattleCommandCodec.type(command),
            payload = PlayableBattleCommandCodec.payload(command),
        )
        return snapshot()
    }

    fun buildTower(targetSlotId: ContentId): PlayableBattleSnapshot =
        submit(PlayableBattleCommand.BuildTower(targetSlotId))

    fun upgradeTower(targetSlotId: ContentId): PlayableBattleSnapshot =
        submit(PlayableBattleCommand.UpgradeTower(targetSlotId))

    fun spend(
        targetSlotId: ContentId?,
        cost: Int,
    ): PlayableBattleSnapshot = submit(PlayableBattleCommand.SpendResource(targetSlotId, cost))

    fun canonicalCommandEncoding(): String = simulation.canonicalCommandEncoding()

    fun inputHash(): String = simulation.inputHash()

    fun replayHashChain(): String = simulation.replayHashChain()

    companion object {
        private const val CURRENT_SIMULATION_VERSION: Int = 1

        /** Restores from the existing full RunSave payload without reconstructing legacy state. */
        fun restore(
            runSave: RunSave,
            clock: SimulationClock = SimulationClock(),
        ): PlayableBattleRestoreResult {
            val state = runSave.playableBattleState ?: return PlayableBattleRestoreResult.UnsupportedLegacy

            // SPEC-06 remains the single validation and wire-contract owner for RunSave. Encoding
            // here validates direct in-memory values too, without introducing another payload format.
            RunSaveCodec.encode(runSave)

            return PlayableBattleRestoreResult.Restored(
                PlayableBattleSession(
                    seed = runSave.seed,
                    initialState = state,
                    clock = clock,
                    initialTick = runSave.tick,
                    rngState = runSave.rngState,
                    simulationVersion = runSave.simulationVersion,
                    pendingCommands = runSave.pendingCommands,
                ),
            )
        }

        fun restore(
            payload: String,
            clock: SimulationClock = SimulationClock(),
        ): PlayableBattleRestoreResult = restore(RunSaveCodec.decode(payload), clock)

        fun fromRunSave(
            runSave: RunSave,
            clock: SimulationClock = SimulationClock(),
        ): PlayableBattleRestoreResult = restore(runSave, clock)
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
            context.commands.forEach { engineCommand ->
                PlayableBattleCommandCodec.decode(engineCommand)?.let { command ->
                    context.state.value = PlayableBattleEngine.reduceState(context.state.value, command)
                }
            }
            context.state.value = PlayableBattleEngine.tick(context.state.value)
        }
    }
}
