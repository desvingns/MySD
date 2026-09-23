package dev.mysd.game.persistence

data class PendingCommand(
    val id: Long,
    val scheduledTick: Long,
    val type: String,
    val actorId: Long?,
    val payload: String,
) {
    /** Source compatibility for the schema-v2 persistence shape. */
    @Deprecated("Use the complete pending-command identity and metadata.")
    constructor(sequence: Long, name: String, payload: String) : this(
        id = sequence,
        scheduledTick = 0L,
        type = name,
        actorId = null,
        payload = payload,
    )
}

enum class RunTerminalResult {
    VICTORY,
    DEFEAT,
}

data class RunSave(
    val runId: String,
    val stageId: String,
    val contentVersion: Int,
    val simulationVersion: Int,
    val seed: Long,
    val rngState: Long,
    val tick: Long,
    val active: Boolean,
    val pendingCommands: List<PendingCommand>,
    val modifiers: List<String>,
    val terminalResult: RunTerminalResult?,
)

object RunSaveCodec {
    const val CURRENT_SCHEMA_VERSION: Int = 3
    private const val BOUNDARY = "run-save"
    private const val LEGACY_SCHEMA_VERSION_WITH_SIMULATION: Int = 2

    fun encode(value: RunSave): String {
        validate(value)
        val fields = linkedMapOf(
            "runId" to PersistenceWire.encodeText(value.runId),
            "stageId" to PersistenceWire.encodeText(value.stageId),
            "contentVersion" to value.contentVersion.toString(),
            "simulationVersion" to value.simulationVersion.toString(),
            "seed" to value.seed.toString(),
            "rngState" to value.rngState.toString(),
            "tick" to value.tick.toString(),
            "active" to if (value.active) "1" else "0",
            "commandCount" to value.pendingCommands.size.toString(),
        )
        canonicalCommands(value.pendingCommands).forEachIndexed { index, command ->
            fields["command.$index.id"] = command.id.toString()
            fields["command.$index.scheduledTick"] = command.scheduledTick.toString()
            fields["command.$index.type"] = PersistenceWire.encodeText(command.type)
            fields["command.$index.actorPresent"] = if (command.actorId == null) "0" else "1"
            fields["command.$index.actorId"] = command.actorId?.toString() ?: ""
            fields["command.$index.payload"] = PersistenceWire.encodeText(command.payload)
        }
        fields["modifierCount"] = value.modifiers.size.toString()
        value.modifiers.forEachIndexed { index, modifier ->
            fields["modifier.$index"] = PersistenceWire.encodeText(modifier)
        }
        fields["terminalPresent"] = if (value.terminalResult == null) "0" else "1"
        fields["terminalResult"] = value.terminalResult?.name ?: ""
        return PersistenceWire.document(BOUNDARY, CURRENT_SCHEMA_VERSION, fields)
    }

    fun decode(input: String): RunSave {
        val document = PersistenceWire.parse(input, BOUNDARY, CURRENT_SCHEMA_VERSION)
        val fields = document.fields
        val commandCount = PersistenceWire.count(fields, "commandCount")
        val modifierCount = PersistenceWire.count(fields, "modifierCount")
        val fixedKeys = buildSet {
            addAll(
                setOf(
                    "runId", "stageId", "contentVersion", "seed", "rngState", "tick", "active",
                    "commandCount", "modifierCount", "terminalPresent", "terminalResult",
                ),
            )
            if (document.version >= LEGACY_SCHEMA_VERSION_WITH_SIMULATION) add("simulationVersion")
            repeat(commandCount) {
                if (document.version >= CURRENT_SCHEMA_VERSION) {
                    add("command.$it.id")
                    add("command.$it.scheduledTick")
                    add("command.$it.type")
                    add("command.$it.actorPresent")
                    add("command.$it.actorId")
                    add("command.$it.payload")
                } else {
                    add("command.$it.sequence")
                    add("command.$it.name")
                    add("command.$it.payload")
                }
            }
            repeat(modifierCount) { add("modifier.$it") }
        }
        PersistenceWire.requireExactKeys(document, fixedKeys)

        val simulationVersion = if (document.version == 1) 1 else PersistenceWire.int(fields, "simulationVersion")
        val decodedCommands = (0 until commandCount).map { index ->
            if (document.version >= CURRENT_SCHEMA_VERSION) {
                val actorPresent = PersistenceWire.flag(fields, "command.$index.actorPresent")
                val actorRaw = PersistenceWire.required(fields, "command.$index.actorId")
                val actorId = if (actorPresent) {
                    actorRaw.toLongOrNull()
                        ?: throw MalformedPersistenceException("Malformed long in persistence field: command.$index.actorId")
                } else {
                    if (actorRaw.isNotEmpty()) {
                        throw MalformedPersistenceException("Unexpected actor id in persistence field: command.$index.actorId")
                    }
                    null
                }
                PendingCommand(
                    id = PersistenceWire.long(fields, "command.$index.id"),
                    scheduledTick = PersistenceWire.long(fields, "command.$index.scheduledTick"),
                    type = PersistenceWire.decodeText(fields, "command.$index.type"),
                    actorId = actorId,
                    payload = PersistenceWire.decodeText(fields, "command.$index.payload"),
                )
            } else {
                PendingCommand(
                    id = PersistenceWire.long(fields, "command.$index.sequence"),
                    scheduledTick = 0L,
                    type = PersistenceWire.decodeText(fields, "command.$index.name"),
                    actorId = null,
                    payload = PersistenceWire.decodeText(fields, "command.$index.payload"),
                )
            }
        }
        val commands = if (document.version >= CURRENT_SCHEMA_VERSION) {
            canonicalCommands(decodedCommands)
        } else {
            decodedCommands
        }
        val modifiers = (0 until modifierCount).map { index ->
            PersistenceWire.decodeText(fields, "modifier.$index")
        }
        val terminalPresent = PersistenceWire.flag(fields, "terminalPresent")
        val terminalRaw = PersistenceWire.required(fields, "terminalResult")
        val terminalResult = if (terminalPresent) {
            try {
                RunTerminalResult.valueOf(terminalRaw)
            } catch (_: IllegalArgumentException) {
                throw MalformedPersistenceException("Malformed run terminal result")
            }
        } else {
            if (terminalRaw.isNotEmpty()) throw MalformedPersistenceException("Unexpected run terminal result")
            null
        }
        val result = RunSave(
            runId = PersistenceWire.decodeText(fields, "runId"),
            stageId = PersistenceWire.decodeText(fields, "stageId"),
            contentVersion = PersistenceWire.int(fields, "contentVersion"),
            simulationVersion = simulationVersion,
            seed = PersistenceWire.long(fields, "seed"),
            rngState = PersistenceWire.long(fields, "rngState"),
            tick = PersistenceWire.long(fields, "tick"),
            active = PersistenceWire.flag(fields, "active"),
            pendingCommands = commands,
            modifiers = modifiers,
            terminalResult = terminalResult,
        )
        validate(result)
        return result
    }

    private fun validate(value: RunSave) {
        PersistenceWire.requireNonBlank(value.runId, "runId")
        PersistenceWire.requireNonBlank(value.stageId, "stageId")
        PersistenceWire.requireNonNegative(value.contentVersion, "contentVersion")
        PersistenceWire.requireNonNegative(value.simulationVersion, "simulationVersion")
        PersistenceWire.requireNonNegative(value.tick, "tick")
        val ids = mutableSetOf<Long>()
        value.pendingCommands.forEach { command ->
            PersistenceWire.requireNonNegative(command.id, "command.id")
            if (!ids.add(command.id)) {
                throw MalformedPersistenceException("Duplicate pending command id")
            }
            PersistenceWire.requireNonNegative(command.scheduledTick, "command.scheduledTick")
            PersistenceWire.requireNonBlank(command.type, "command.type")
        }
        value.modifiers.forEach { PersistenceWire.requireNonBlank(it, "modifier") }
        if (value.active && value.terminalResult != null) {
            throw MalformedPersistenceException("Active run cannot have a terminal result")
        }
    }

    private fun canonicalCommands(commands: List<PendingCommand>): List<PendingCommand> = commands
        .sortedWith(
            compareBy<PendingCommand> { it.scheduledTick }
                .thenBy { it.id }
                .thenBy { it.type }
                .thenBy { it.actorId ?: Long.MIN_VALUE }
                .thenBy { it.payload },
        )
}
