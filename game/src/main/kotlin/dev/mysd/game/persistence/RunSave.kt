package dev.mysd.game.persistence

import dev.mysd.game.battle.playable.PlayableBattleBaseState
import dev.mysd.game.battle.playable.PlayableBattleEnemyState
import dev.mysd.game.battle.playable.PlayableBattlePhase
import dev.mysd.game.battle.playable.PlayableBattleSlotState
import dev.mysd.game.battle.playable.PlayableBattleState
import dev.mysd.game.battle.playable.PlayableBattleTerminal
import dev.mysd.game.content.ContentId

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
    /** Android-free authoritative state; null is an explicit legacy contour-only save. */
    val playableBattleState: PlayableBattleState? = null,
) {
    /** Short alias for callers that refer to the payload as the playable state. */
    val playableState: PlayableBattleState?
        get() = playableBattleState
}

object RunSaveCodec {
    const val CURRENT_SCHEMA_VERSION: Int = 4
    private const val BOUNDARY = "run-save"
    private const val LEGACY_SCHEMA_VERSION_WITH_SIMULATION: Int = 2
    private const val COMMAND_METADATA_SCHEMA_VERSION: Int = 3
    private const val PLAYABLE_STATE_SCHEMA_VERSION: Int = 4

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
        fields["playableStatePresent"] = if (value.playableBattleState == null) "0" else "1"
        value.playableBattleState?.let { encodePlayableState(fields, it) }
        return PersistenceWire.document(BOUNDARY, CURRENT_SCHEMA_VERSION, fields)
    }

    fun decode(input: String): RunSave {
        val document = PersistenceWire.parse(input, BOUNDARY, CURRENT_SCHEMA_VERSION)
        val fields = document.fields
        val commandCount = PersistenceWire.count(fields, "commandCount")
        val modifierCount = PersistenceWire.count(fields, "modifierCount")
        val statePresent = if (document.version >= PLAYABLE_STATE_SCHEMA_VERSION) {
            PersistenceWire.flag(fields, "playableStatePresent")
        } else {
            false
        }
        val stateSlotCount = if (statePresent) {
            PersistenceWire.count(fields, "state.slotCount")
        } else {
            0
        }
        val stateEnemyCount = if (statePresent) {
            PersistenceWire.count(fields, "state.enemyCount")
        } else {
            0
        }
        val fixedKeys = buildSet {
            addAll(
                setOf(
                    "runId", "stageId", "contentVersion", "seed", "rngState", "tick", "active",
                    "commandCount", "modifierCount", "terminalPresent", "terminalResult",
                ),
            )
            if (document.version >= LEGACY_SCHEMA_VERSION_WITH_SIMULATION) add("simulationVersion")
            repeat(commandCount) {
                if (document.version >= COMMAND_METADATA_SCHEMA_VERSION) {
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
            if (document.version >= PLAYABLE_STATE_SCHEMA_VERSION) {
                add("playableStatePresent")
                if (statePresent) {
                    addAll(playableStateFixedKeys())
                    repeat(stateSlotCount) { index ->
                        add("state.slot.$index.id")
                        add("state.slot.$index.positionTicks")
                        add("state.slot.$index.towerPresent")
                        add("state.slot.$index.towerId")
                        add("state.slot.$index.towerLevel")
                        add("state.slot.$index.towerDamagePresent")
                        add("state.slot.$index.towerDamage")
                        add("state.slot.$index.towerCooldownPresent")
                        add("state.slot.$index.towerCooldownTicks")
                        add("state.slot.$index.towerCooldownRemainingTicks")
                    }
                    repeat(stateEnemyCount) { index ->
                        add("state.enemy.$index.id")
                        add("state.enemy.$index.familyId")
                        add("state.enemy.$index.health")
                        add("state.enemy.$index.positionTicks")
                        add("state.enemy.$index.speedTicks")
                    }
                }
            }
        }
        PersistenceWire.requireExactKeys(document, fixedKeys)

        val simulationVersion = if (document.version == 1) 1 else PersistenceWire.int(fields, "simulationVersion")
        val decodedCommands = (0 until commandCount).map { index ->
            if (document.version >= COMMAND_METADATA_SCHEMA_VERSION) {
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
        val commands = if (document.version >= COMMAND_METADATA_SCHEMA_VERSION) {
            canonicalCommands(decodedCommands)
        } else {
            decodedCommands
        }
        val modifiers = (0 until modifierCount).map { index ->
            PersistenceWire.decodeText(fields, "modifier.$index")
        }
        val terminalPresent = PersistenceWire.flag(fields, "terminalPresent")
        val terminalRaw = PersistenceWire.required(fields, "terminalResult")
        val terminalResult = decodeRunTerminal(terminalPresent, terminalRaw)
        val playableBattleState = if (statePresent) {
            decodePlayableState(fields, stateSlotCount, stateEnemyCount)
        } else {
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
            playableBattleState = playableBattleState,
        )
        validate(result)
        return result
    }

    private fun encodePlayableState(
        fields: MutableMap<String, String>,
        state: PlayableBattleState,
    ) {
        fields["state.stageId"] = PersistenceWire.encodeText(state.stageId.value)
        fields["state.phase"] = state.phase.name
        fields["state.terminalPresent"] = if (state.terminalResult == null) "0" else "1"
        fields["state.terminalResult"] = state.terminalResult?.name ?: ""
        fields["state.base.id"] = PersistenceWire.encodeText(state.base.id.value)
        fields["state.base.health"] = state.base.health.toString()
        fields["state.base.maxHealth"] = state.base.maxHealth.toString()
        fields["state.base.positionTicks"] = state.base.positionTicks.toString()
        fields["state.resource"] = state.resource.toString()
        fields["state.resourceCap"] = state.resourceCap.toString()
        fields["state.incomePerSecond"] = state.incomePerSecond.toString()
        fields["state.incomeRemainderTicks"] = state.incomeRemainderTicks.toString()
        fields["state.towerId"] = PersistenceWire.encodeText(state.towerId.value)
        fields["state.buildCost"] = state.buildCost.toString()
        fields["state.towerBaseDamage"] = state.towerBaseDamage.toString()
        fields["state.towerBaseCooldownTicks"] = state.towerBaseCooldownTicks.toString()
        fields["state.towerUpgradeBaseCost"] = state.towerUpgradeBaseCost.toString()
        fields["state.towerUpgradeCostStep"] = state.towerUpgradeCostStep.toString()
        fields["state.towerDamageStep"] = state.towerDamageStep.toString()
        fields["state.towerCooldownStep"] = state.towerCooldownStep.toString()
        fields["state.towerMinCooldownTicks"] = state.towerMinCooldownTicks.toString()
        fields["state.waveId"] = PersistenceWire.encodeText(state.waveId.value)
        fields["state.enemyFamilyId"] = PersistenceWire.encodeText(state.enemyFamilyId.value)
        fields["state.enemyHealth"] = state.enemyHealth.toString()
        fields["state.enemySpeedTicks"] = state.enemySpeedTicks.toString()
        fields["state.waveSpawnCount"] = state.waveSpawnCount.toString()
        fields["state.waveSpawnedCount"] = state.waveSpawnedCount.toString()
        fields["state.waveElapsedTicks"] = state.waveElapsedTicks.toString()
        fields["state.waveSpawnIntervalTicks"] = state.waveSpawnIntervalTicks.toString()
        fields["state.towerRangeTicks"] = state.towerRangeTicks.toString()
        fields["state.baseLeakDamage"] = state.baseLeakDamage.toString()
        fields["state.slotCount"] = state.slots.size.toString()
        state.slots.forEachIndexed { index, slot ->
            fields["state.slot.$index.id"] = PersistenceWire.encodeText(slot.id.value)
            fields["state.slot.$index.positionTicks"] = slot.positionTicks.toString()
            fields["state.slot.$index.towerPresent"] = if (slot.towerId == null) "0" else "1"
            fields["state.slot.$index.towerId"] = slot.towerId?.let { PersistenceWire.encodeText(it.value) } ?: ""
            fields["state.slot.$index.towerLevel"] = slot.towerLevel.toString()
            fields["state.slot.$index.towerDamagePresent"] = if (slot.towerDamage == null) "0" else "1"
            fields["state.slot.$index.towerDamage"] = slot.towerDamage?.toString() ?: ""
            fields["state.slot.$index.towerCooldownPresent"] = if (slot.towerCooldownTicks == null) "0" else "1"
            fields["state.slot.$index.towerCooldownTicks"] = slot.towerCooldownTicks?.toString() ?: ""
            fields["state.slot.$index.towerCooldownRemainingTicks"] = slot.towerCooldownRemainingTicks.toString()
        }
        fields["state.enemyCount"] = state.enemies.size.toString()
        state.enemies.forEachIndexed { index, enemy ->
            fields["state.enemy.$index.id"] = PersistenceWire.encodeText(enemy.id)
            fields["state.enemy.$index.familyId"] = PersistenceWire.encodeText(enemy.familyId.value)
            fields["state.enemy.$index.health"] = enemy.health.toString()
            fields["state.enemy.$index.positionTicks"] = enemy.positionTicks.toString()
            fields["state.enemy.$index.speedTicks"] = enemy.speedTicks.toString()
        }
    }

    private fun decodePlayableState(
        fields: Map<String, String>,
        slotCount: Int,
        enemyCount: Int,
    ): PlayableBattleState {
        val terminalPresent = PersistenceWire.flag(fields, "state.terminalPresent")
        val terminalRaw = PersistenceWire.required(fields, "state.terminalResult")
        val terminal = decodePlayableTerminal(terminalPresent, terminalRaw)
        val slotIds = mutableSetOf<ContentId>()
        val slots = try {
            (0 until slotCount).map { index ->
                val prefix = "state.slot.$index"
                val slotId = PersistenceWire.contentId(fields, "$prefix.id")
                if (!slotIds.add(slotId)) {
                    throw MalformedPersistenceException("Duplicate playable state slot id: $prefix.id")
                }
                val positionTicks = PersistenceWire.nonNegativeInt(fields, "$prefix.positionTicks")
                val towerPresent = PersistenceWire.flag(fields, "$prefix.towerPresent")
                val towerRaw = PersistenceWire.required(fields, "$prefix.towerId")
                val towerId = decodeOptionalContentId(towerPresent, "$prefix.towerId", towerRaw, fields)
                val towerLevel = PersistenceWire.intInRange(
                    fields,
                    "$prefix.towerLevel",
                    0..PlayableBattleState.MAX_TOWER_LEVEL,
                )
                val towerDamagePresent = PersistenceWire.flag(fields, "$prefix.towerDamagePresent")
                val towerDamageRaw = PersistenceWire.required(fields, "$prefix.towerDamage")
                val towerDamage = decodeOptionalInt(
                    towerDamagePresent,
                    "$prefix.towerDamage",
                    towerDamageRaw,
                    fields,
                )
                towerDamage?.let { PersistenceWire.requireNonNegative(it, "$prefix.towerDamage") }
                val towerCooldownPresent = PersistenceWire.flag(fields, "$prefix.towerCooldownPresent")
                val towerCooldownRaw = PersistenceWire.required(fields, "$prefix.towerCooldownTicks")
                val towerCooldownTicks = decodeOptionalInt(
                    towerCooldownPresent,
                    "$prefix.towerCooldownTicks",
                    towerCooldownRaw,
                    fields,
                )
                towerCooldownTicks?.let {
                    PersistenceWire.requireAtLeast(it, 1, "$prefix.towerCooldownTicks")
                }
                val cooldownRemainingTicks = PersistenceWire.nonNegativeInt(
                    fields,
                    "$prefix.towerCooldownRemainingTicks",
                )
                if (towerId == null) {
                    if (towerLevel != 0) {
                        throw MalformedPersistenceException(
                            "Invalid empty slot field: $prefix.towerLevel",
                        )
                    }
                    if (towerDamage != null) {
                        throw MalformedPersistenceException(
                            "Invalid empty slot field: $prefix.towerDamage",
                        )
                    }
                    if (towerCooldownTicks != null) {
                        throw MalformedPersistenceException(
                            "Invalid empty slot field: $prefix.towerCooldownTicks",
                        )
                    }
                    if (cooldownRemainingTicks != 0) {
                        throw MalformedPersistenceException(
                            "Invalid empty slot field: $prefix.towerCooldownRemainingTicks",
                        )
                    }
                } else if (towerLevel > 0 && (towerDamage == null || towerCooldownTicks == null)) {
                    throw MalformedPersistenceException(
                        "Upgraded tower is missing stats at $prefix.towerLevel",
                    )
                }
                PlayableBattleSlotState(
                    id = slotId,
                    positionTicks = positionTicks,
                    towerId = towerId,
                    towerLevel = towerLevel,
                    towerDamage = towerDamage,
                    towerCooldownTicks = towerCooldownTicks,
                    towerCooldownRemainingTicks = cooldownRemainingTicks,
                )
            }
        } catch (error: PersistenceException) {
            throw error
        } catch (error: IllegalArgumentException) {
            throw MalformedPersistenceException(
                "Invalid playable state slot: ${error.message ?: "state invariant violation"}",
            )
        }
        val enemyIds = mutableSetOf<String>()
        val enemies = try {
            (0 until enemyCount).map { index ->
                val prefix = "state.enemy.$index"
                val enemyId = PersistenceWire.decodeText(fields, "$prefix.id")
                PersistenceWire.requireNonBlank(enemyId, "$prefix.id")
                if (!enemyIds.add(enemyId)) {
                    throw MalformedPersistenceException("Duplicate playable state enemy id: $prefix.id")
                }
                PlayableBattleEnemyState(
                    id = enemyId,
                    familyId = PersistenceWire.contentId(fields, "$prefix.familyId"),
                    health = PersistenceWire.nonNegativeInt(fields, "$prefix.health"),
                    positionTicks = PersistenceWire.nonNegativeInt(fields, "$prefix.positionTicks"),
                    speedTicks = PersistenceWire.nonNegativeInt(fields, "$prefix.speedTicks"),
                )
            }
        } catch (error: PersistenceException) {
            throw error
        } catch (error: IllegalArgumentException) {
            throw MalformedPersistenceException(
                "Invalid playable state enemy: ${error.message ?: "state invariant violation"}",
            )
        }
        val stateStageId = PersistenceWire.contentId(fields, "state.stageId")
        val statePhase = decodePlayablePhase(fields)
        val baseId = PersistenceWire.contentId(fields, "state.base.id")
        val baseMaxHealth = PersistenceWire.nonNegativeInt(fields, "state.base.maxHealth")
        val baseHealth = PersistenceWire.intInRange(
            fields,
            "state.base.health",
            0..baseMaxHealth,
        )
        val basePositionTicks = PersistenceWire.nonNegativeInt(fields, "state.base.positionTicks")
        val resourceCap = PersistenceWire.nonNegativeInt(fields, "state.resourceCap")
        val resource = PersistenceWire.intInRange(fields, "state.resource", 0..resourceCap)
        val incomePerSecond = PersistenceWire.nonNegativeInt(fields, "state.incomePerSecond")
        val incomeRemainderTicks = PersistenceWire.intInRange(
            fields,
            "state.incomeRemainderTicks",
            0 until PlayableBattleState.TICKS_PER_SECOND,
        )
        val towerId = PersistenceWire.contentId(fields, "state.towerId")
        val buildCost = PersistenceWire.nonNegativeInt(fields, "state.buildCost")
        val towerBaseDamage = PersistenceWire.nonNegativeInt(fields, "state.towerBaseDamage")
        val towerBaseCooldownTicks = PersistenceWire.atLeastInt(
            fields,
            "state.towerBaseCooldownTicks",
            1,
        )
        val towerUpgradeBaseCost = PersistenceWire.nonNegativeInt(fields, "state.towerUpgradeBaseCost")
        val towerUpgradeCostStep = PersistenceWire.nonNegativeInt(fields, "state.towerUpgradeCostStep")
        val towerDamageStep = PersistenceWire.nonNegativeInt(fields, "state.towerDamageStep")
        val towerCooldownStep = PersistenceWire.nonNegativeInt(fields, "state.towerCooldownStep")
        val towerMinCooldownTicks = PersistenceWire.atLeastInt(
            fields,
            "state.towerMinCooldownTicks",
            1,
        )
        val waveId = PersistenceWire.contentId(fields, "state.waveId")
        val enemyFamilyId = PersistenceWire.contentId(fields, "state.enemyFamilyId")
        val enemyHealth = PersistenceWire.nonNegativeInt(fields, "state.enemyHealth")
        val enemySpeedTicks = PersistenceWire.nonNegativeInt(fields, "state.enemySpeedTicks")
        val waveSpawnCount = PersistenceWire.intInRange(fields, "state.waveSpawnCount", 8..10)
        val waveSpawnedCount = PersistenceWire.intInRange(
            fields,
            "state.waveSpawnedCount",
            0..waveSpawnCount,
        )
        val waveElapsedTicks = PersistenceWire.nonNegativeInt(fields, "state.waveElapsedTicks")
        val waveSpawnIntervalTicks = PersistenceWire.atLeastInt(
            fields,
            "state.waveSpawnIntervalTicks",
            1,
        )
        val towerRangeTicks = PersistenceWire.nonNegativeInt(fields, "state.towerRangeTicks")
        val baseLeakDamage = PersistenceWire.nonNegativeInt(fields, "state.baseLeakDamage")
        if (enemies.size > waveSpawnedCount) {
            throw MalformedPersistenceException(
                "Living enemies exceed persistence field: state.waveSpawnedCount",
            )
        }
        when (terminal) {
            PlayableBattleTerminal.VICTORY -> {
                if (baseHealth <= 0) {
                    throw MalformedPersistenceException(
                        "Invalid terminal combination: state.base.health must be positive for VICTORY",
                    )
                }
                if (waveSpawnedCount != waveSpawnCount) {
                    throw MalformedPersistenceException(
                        "Invalid terminal combination: state.waveSpawnedCount must equal state.waveSpawnCount for VICTORY",
                    )
                }
                if (enemies.isNotEmpty()) {
                    throw MalformedPersistenceException(
                        "Invalid terminal combination: state.enemyCount must be zero for VICTORY",
                    )
                }
            }

            PlayableBattleTerminal.DEFEAT -> if (baseHealth != 0) {
                throw MalformedPersistenceException(
                    "Invalid terminal combination: state.base.health must be zero for DEFEAT",
                )
            }

            null -> Unit
        }
        val state = try {
            PlayableBattleState(
                stageId = stateStageId,
                phase = statePhase,
                base = PlayableBattleBaseState(
                    id = baseId,
                    health = baseHealth,
                    maxHealth = baseMaxHealth,
                    positionTicks = basePositionTicks,
                ),
                resource = resource,
                resourceCap = resourceCap,
                incomePerSecond = incomePerSecond,
                incomeRemainderTicks = incomeRemainderTicks,
                slots = slots,
                enemies = enemies,
                towerId = towerId,
                buildCost = buildCost,
                towerBaseDamage = towerBaseDamage,
                towerBaseCooldownTicks = towerBaseCooldownTicks,
                towerUpgradeBaseCost = towerUpgradeBaseCost,
                towerUpgradeCostStep = towerUpgradeCostStep,
                towerDamageStep = towerDamageStep,
                towerCooldownStep = towerCooldownStep,
                towerMinCooldownTicks = towerMinCooldownTicks,
                terminalResult = terminal,
                waveId = waveId,
                enemyFamilyId = enemyFamilyId,
                enemyHealth = enemyHealth,
                enemySpeedTicks = enemySpeedTicks,
                waveSpawnCount = waveSpawnCount,
                waveSpawnedCount = waveSpawnedCount,
                waveElapsedTicks = waveElapsedTicks,
                waveSpawnIntervalTicks = waveSpawnIntervalTicks,
                towerRangeTicks = towerRangeTicks,
                baseLeakDamage = baseLeakDamage,
            )
        } catch (error: PersistenceException) {
            throw error
        } catch (error: IllegalArgumentException) {
            throw MalformedPersistenceException(
                "Invalid playable state: ${error.message ?: "state invariant violation"}",
            )
        }
        validatePlayableState(state)
        return state
    }

    private fun decodePlayablePhase(fields: Map<String, String>): PlayableBattlePhase = try {
        PlayableBattlePhase.valueOf(PersistenceWire.required(fields, "state.phase"))
    } catch (_: IllegalArgumentException) {
        throw MalformedPersistenceException("Malformed enum in persistence field: state.phase")
    }

    private fun decodeRunTerminal(present: Boolean, raw: String): RunTerminalResult? {
        if (!present) {
            if (raw.isNotEmpty()) throw MalformedPersistenceException("Unexpected run terminal result")
            return null
        }
        return try {
            RunTerminalResult.valueOf(raw)
        } catch (_: IllegalArgumentException) {
            throw MalformedPersistenceException("Malformed enum in persistence field: terminalResult")
        }
    }

    private fun decodePlayableTerminal(present: Boolean, raw: String): PlayableBattleTerminal? {
        if (!present) {
            if (raw.isNotEmpty()) throw MalformedPersistenceException("Unexpected playable terminal result")
            return null
        }
        return try {
            PlayableBattleTerminal.valueOf(raw)
        } catch (_: IllegalArgumentException) {
            throw MalformedPersistenceException(
                "Malformed enum in persistence field: state.terminalResult",
            )
        }
    }

    private fun decodeOptionalContentId(
        present: Boolean,
        valueKey: String,
        raw: String,
        fields: Map<String, String>,
    ): ContentId? {
        if (present) return PersistenceWire.contentId(fields, valueKey)
        if (raw.isNotEmpty()) {
            throw MalformedPersistenceException("Unexpected value in persistence field: $valueKey")
        }
        return null
    }

    private fun decodeOptionalInt(
        present: Boolean,
        valueKey: String,
        raw: String,
        fields: Map<String, String>,
    ): Int? {
        if (present) return PersistenceWire.int(fields, valueKey)
        if (raw.isNotEmpty()) {
            throw MalformedPersistenceException("Unexpected value in persistence field: $valueKey")
        }
        return null
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
        if (!value.active && value.terminalResult == null) {
            throw MalformedPersistenceException("Inactive run must have a terminal result")
        }
        value.playableBattleState?.let { state ->
            if (state.stageId.value != value.stageId) {
                throw MalformedPersistenceException(
                    "Playable state stage does not match persistence field: stageId",
                )
            }
            validatePlayableState(state)
            val stateTerminal = state.terminalResult?.let(::toRunTerminal)
            if (stateTerminal != value.terminalResult) {
                throw MalformedPersistenceException(
                    "Playable state terminal does not match run terminal result",
                )
            }
            if (state.isTerminal == value.active) {
                throw MalformedPersistenceException(
                    "Playable state active flag does not match run active flag",
                )
            }
        }
    }

    private fun validatePlayableState(state: PlayableBattleState) {
        PersistenceWire.requireNonBlank(state.stageId.value, "state.stageId")
        PersistenceWire.requireNonNegative(state.base.maxHealth, "state.base.maxHealth")
        PersistenceWire.requireInRange(
            state.base.health,
            0..state.base.maxHealth,
            "state.base.health",
        )
        PersistenceWire.requireNonNegative(state.base.positionTicks, "state.base.positionTicks")
        PersistenceWire.requireNonNegative(state.resourceCap, "state.resourceCap")
        PersistenceWire.requireInRange(state.resource, 0..state.resourceCap, "state.resource")
        PersistenceWire.requireNonNegative(state.incomePerSecond, "state.incomePerSecond")
        PersistenceWire.requireInRange(
            state.incomeRemainderTicks,
            0 until PlayableBattleState.TICKS_PER_SECOND,
            "state.incomeRemainderTicks",
        )
        PersistenceWire.requireInRange(state.waveSpawnCount, 8..10, "state.waveSpawnCount")
        PersistenceWire.requireInRange(
            state.waveSpawnedCount,
            0..state.waveSpawnCount,
            "state.waveSpawnedCount",
        )
        PersistenceWire.requireNonNegative(state.waveElapsedTicks, "state.waveElapsedTicks")
        PersistenceWire.requireAtLeast(
            state.waveSpawnIntervalTicks,
            1,
            "state.waveSpawnIntervalTicks",
        )
        PersistenceWire.requireAtLeast(
            state.towerBaseCooldownTicks,
            1,
            "state.towerBaseCooldownTicks",
        )
        PersistenceWire.requireAtLeast(
            state.towerMinCooldownTicks,
            1,
            "state.towerMinCooldownTicks",
        )
        val slotIds = mutableSetOf<ContentId>()
        state.slots.forEachIndexed { index, slot ->
            if (!slotIds.add(slot.id)) {
                throw MalformedPersistenceException(
                    "Duplicate playable state slot id: state.slot.$index.id",
                )
            }
            PersistenceWire.requireNonNegative(slot.positionTicks, "state.slot.$index.positionTicks")
            PersistenceWire.requireInRange(
                slot.towerLevel,
                0..PlayableBattleState.MAX_TOWER_LEVEL,
                "state.slot.$index.towerLevel",
            )
            slot.towerDamage?.let {
                PersistenceWire.requireNonNegative(it, "state.slot.$index.towerDamage")
            }
            slot.towerCooldownTicks?.let {
                PersistenceWire.requireAtLeast(it, 1, "state.slot.$index.towerCooldownTicks")
            }
            PersistenceWire.requireNonNegative(
                slot.towerCooldownRemainingTicks,
                "state.slot.$index.towerCooldownRemainingTicks",
            )
        }
        val enemyIds = mutableSetOf<String>()
        state.enemies.forEachIndexed { index, enemy ->
            if (!enemyIds.add(enemy.id)) {
                throw MalformedPersistenceException(
                    "Duplicate playable state enemy id: state.enemy.$index.id",
                )
            }
            PersistenceWire.requireNonBlank(enemy.id, "state.enemy.$index.id")
            PersistenceWire.requireNonNegative(enemy.health, "state.enemy.$index.health")
            PersistenceWire.requireNonNegative(enemy.positionTicks, "state.enemy.$index.positionTicks")
            PersistenceWire.requireNonNegative(enemy.speedTicks, "state.enemy.$index.speedTicks")
        }
        if (state.enemies.size > state.waveSpawnedCount) {
            throw MalformedPersistenceException("Living enemies exceed state.waveSpawnedCount")
        }
        when (state.terminalResult) {
            PlayableBattleTerminal.VICTORY -> {
                if (state.base.health <= 0) {
                    throw MalformedPersistenceException(
                        "Invalid terminal combination: state.terminalResult=VICTORY requires state.base.health>0",
                    )
                }
                if (state.waveSpawnedCount != state.waveSpawnCount) {
                    throw MalformedPersistenceException(
                        "Invalid terminal combination: state.terminalResult=VICTORY requires state.waveSpawnedCount=state.waveSpawnCount",
                    )
                }
                if (state.enemies.isNotEmpty()) {
                    throw MalformedPersistenceException(
                        "Invalid terminal combination: state.terminalResult=VICTORY requires no living enemies",
                    )
                }
            }

            PlayableBattleTerminal.DEFEAT -> if (state.base.health != 0) {
                throw MalformedPersistenceException(
                    "Invalid terminal combination: state.terminalResult=DEFEAT requires state.base.health=0",
                )
            }

            null -> Unit
        }
    }

    private fun toRunTerminal(terminal: PlayableBattleTerminal): RunTerminalResult = when (terminal) {
        PlayableBattleTerminal.VICTORY -> RunTerminalResult.VICTORY
        PlayableBattleTerminal.DEFEAT -> RunTerminalResult.DEFEAT
    }

    private fun playableStateFixedKeys(): Set<String> = setOf(
        "state.stageId",
        "state.phase",
        "state.terminalPresent",
        "state.terminalResult",
        "state.base.id",
        "state.base.health",
        "state.base.maxHealth",
        "state.base.positionTicks",
        "state.resource",
        "state.resourceCap",
        "state.incomePerSecond",
        "state.incomeRemainderTicks",
        "state.towerId",
        "state.buildCost",
        "state.towerBaseDamage",
        "state.towerBaseCooldownTicks",
        "state.towerUpgradeBaseCost",
        "state.towerUpgradeCostStep",
        "state.towerDamageStep",
        "state.towerCooldownStep",
        "state.towerMinCooldownTicks",
        "state.waveId",
        "state.enemyFamilyId",
        "state.enemyHealth",
        "state.enemySpeedTicks",
        "state.waveSpawnCount",
        "state.waveSpawnedCount",
        "state.waveElapsedTicks",
        "state.waveSpawnIntervalTicks",
        "state.towerRangeTicks",
        "state.baseLeakDamage",
        "state.slotCount",
        "state.enemyCount",
    )

    private fun canonicalCommands(commands: List<PendingCommand>): List<PendingCommand> = commands
        .sortedWith(
            compareBy<PendingCommand> { it.scheduledTick }
                .thenBy { it.id }
                .thenBy { it.type }
                .thenBy { it.actorId ?: Long.MIN_VALUE }
                .thenBy { it.payload },
        )
}
