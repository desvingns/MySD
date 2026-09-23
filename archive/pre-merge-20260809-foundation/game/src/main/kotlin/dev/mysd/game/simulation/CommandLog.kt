package dev.mysd.game.simulation

import dev.myengine.core.CommandId
import dev.myengine.core.CommandQueue
import dev.myengine.core.EngineCommand
import dev.myengine.core.TextCommand
import dev.myengine.core.Tick
import dev.myengine.core.stableHashOf
import java.nio.charset.StandardCharsets
import java.util.Base64

/**
 * Allocates command ids in a non-negative, strictly increasing sequence.
 *
 * An id is consumed even when the caller has not yet submitted the resulting command. This
 * prevents retry paths from reusing an id and keeps an input log stable across submission order.
 */
class CommandIdAllocator(firstId: Long = 0L) {
    private var nextValue: Long? = firstId.also {
        require(it >= 0L) { "firstId must be non-negative." }
    }

    fun allocate(): CommandId {
        val value = nextValue ?: throw IllegalStateException("Command id allocator is exhausted.")
        nextValue = if (value == Long.MAX_VALUE) null else value + 1L
        return CommandId(value)
    }

    /** Advances the next allocation past an externally-created command id. */
    internal fun observe(id: CommandId) {
        val current = nextValue ?: return
        if (id.value >= current) {
            nextValue = if (id.value == Long.MAX_VALUE) null else id.value + 1L
        }
    }

    val nextId: Long?
        get() = nextValue
}

/** Immutable result of hashing a canonical command input. */
data class ReplayHashChain(
    val inputHash: String,
    val hashes: List<String>,
) {
    val finalHash: String
        get() = hashes.lastOrNull() ?: inputHash
}

/**
 * Android-free append-only command log.
 *
 * The log retains submitted commands for replay diagnostics. Canonical views and hashes are
 * derived from copies, so callers cannot mutate the log or authoritative simulation state.
 */
class CommandLog(firstId: Long = 0L) {
    private val idAllocator = CommandIdAllocator(firstId)
    private val commandsById = linkedMapOf<CommandId, EngineCommand>()

    fun allocateId(): CommandId = idAllocator.allocate()

    fun append(command: EngineCommand) {
        if (commandsById.containsKey(command.id)) {
            throw DuplicateCommandIdException(command.id.value)
        }
        commandsById[command.id] = command
        idAllocator.observe(command.id)
    }

    fun submit(command: EngineCommand) = append(command)

    /** Creates and records a text command using the next stable id. */
    fun submit(
        scheduledTick: Tick,
        type: String,
        payload: String,
        actorId: Long? = null,
    ): TextCommand = TextCommand(
        id = allocateId(),
        scheduledTick = scheduledTick,
        type = type,
        payload = payload,
        actorId = actorId,
    ).also(::append)

    /** Returns a defensive insertion-order copy for diagnostics and persistence adapters. */
    fun commands(): List<EngineCommand> = commandsById.values.toList()

    /** Returns commands ordered by the exact comparator from the pinned MyEngine checkout. */
    fun canonicalCommands(): List<EngineCommand> = commands()
        .sortedWith(CommandQueue.commandComparator)

    fun canonicalEncoding(): String = CommandLogCodec.encode(commands())

    fun inputHash(): String = ReplayHashing.inputHash(canonicalEncoding())

    fun replayHashes(): ReplayHashChain = ReplayHashing.chain(commands())

    fun replayHashChain(): String = replayHashes().finalHash

    val size: Int
        get() = commandsById.size
}

/** Deterministic wire encoding for the canonical command input. */
object CommandLogCodec {
    const val CURRENT_SCHEMA_VERSION: Int = 1
    private const val MAGIC = "mysd.command-log"

    fun encode(commands: Iterable<EngineCommand>): String {
        val canonical = canonicalCommands(commands)
        val lines = mutableListOf(
            "$MAGIC|commands",
            "schemaVersion=$CURRENT_SCHEMA_VERSION",
            "commandCount=${canonical.size}",
        )
        canonical.forEachIndexed { index, command ->
            lines += encodeCommand(index, command)
        }
        return lines.joinToString("\n")
    }

    fun canonicalCommands(commands: Iterable<EngineCommand>): List<EngineCommand> {
        val source = commands.toList()
        val ids = hashSetOf<CommandId>()
        source.forEach { command ->
            if (!ids.add(command.id)) {
                throw DuplicateCommandIdException(command.id.value)
            }
        }
        return source.sortedWith(CommandQueue.commandComparator)
    }

    internal fun encodeCommand(index: Int, command: EngineCommand): List<String> {
        require(index >= 0) { "Command encoding index must be non-negative." }
        val actor = command.actorId
        return listOf(
            "command.$index.id=${command.id.value}",
            "command.$index.scheduledTick=${command.scheduledTick.value}",
            "command.$index.type=${encodeText(command.type)}",
            "command.$index.actorPresent=${if (actor == null) 0 else 1}",
            "command.$index.actorId=${actor ?: ""}",
            "command.$index.payload=${encodeText(command.stablePayload())}",
        )
    }

    private fun encodeText(value: String): String = Base64.getEncoder().encodeToString(
        value.toByteArray(StandardCharsets.UTF_8),
    )
}

/** Hashes canonical command input and derives a deterministic per-command replay chain. */
object ReplayHashing {
    private const val INPUT_DOMAIN = "mysd.command-log.input.v1"
    private const val CHAIN_DOMAIN = "mysd.command-log.chain.v1"

    fun inputHash(canonicalEncoding: String): String = stableHashOf {
        add(INPUT_DOMAIN)
        add(canonicalEncoding)
    }

    fun chain(commands: Iterable<EngineCommand>): ReplayHashChain {
        val canonical = CommandLogCodec.canonicalCommands(commands)
        val encoding = CommandLogCodec.encode(canonical)
        val inputHash = inputHash(encoding)
        var previous = inputHash
        val hashes = canonical.mapIndexed { index, command ->
            previous = stableHashOf {
                add(CHAIN_DOMAIN)
                add(previous)
                CommandLogCodec.encodeCommand(index, command).forEach(::add)
            }
            previous
        }
        return ReplayHashChain(inputHash = inputHash, hashes = hashes)
    }
}
