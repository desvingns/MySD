package dev.mysd.game.persistence

import dev.myengine.core.stableHashOf
import dev.mysd.game.battle.playable.PlayableBattleEngine
import dev.mysd.game.battle.playable.PlayableBattlePhase
import dev.mysd.game.battle.playable.PlayableBattleState
import dev.mysd.game.battle.playable.PlayableBattleTerminal
import dev.mysd.game.content.ContentId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import java.util.LinkedHashMap
import java.util.LinkedHashSet

class PersistenceCodecTest {
    @Test
    fun runAndProfileRoundTripPreserveTheirBoundaries() {
        val run = sampleRun()
        val profile = sampleProfile()

        assertEquals(run, RunSaveCodec.decode(RunSaveCodec.encode(run)))
        assertEquals(profile, ProfileStoreCodec.decode(ProfileStoreCodec.encode(profile)))
    }

    @Test
    fun runAndProfilePayloadsCannotCrossDecode() {
        val runPayload = RunSaveCodec.encode(sampleRun())
        val profilePayload = ProfileStoreCodec.encode(sampleProfile())

        assertFailsWith<MalformedPersistenceException> { ProfileStoreCodec.decode(runPayload) }
        assertFailsWith<MalformedPersistenceException> { RunSaveCodec.decode(profilePayload) }
    }

    @Test
    fun supportedHistoricalVersionsMigrateWithExplicitDefaults() {
        val migratedRunV1 = RunSaveCodec.decode(legacyRunPayload(version = 1))
        assertLegacyRun(
            migratedRunV1,
            simulationVersion = 1,
            active = true,
            terminalResult = null,
        )
        assertEquals(null, migratedRunV1.playableBattleState)

        val migratedRunV2 = RunSaveCodec.decode(
            legacyRunPayload(version = 2, active = false, terminalResult = RunTerminalResult.VICTORY),
        )
        assertLegacyRun(
            migratedRunV2,
            simulationVersion = 7,
            active = false,
            terminalResult = RunTerminalResult.VICTORY,
        )

        val profileV1 = ProfileStoreCodec.encode(sampleProfile())
            .lineSequence()
            .filterNot { it.startsWith("tech.") || it.startsWith("serviceHistory.") }
            .filterNot { it == "techCount=1" || it == "serviceHistoryCount=1" }
            .joinToString("\n")
            .replace("schemaVersion=2", "schemaVersion=1")
        val migratedProfile = ProfileStoreCodec.decode(profileV1)
        assertTrue(migratedProfile.tech.isEmpty())
        assertTrue(migratedProfile.localServiceHistory.isEmpty())
    }

    @Test
    fun `legacy v3 inactive contour without terminal does not invent playable state`() {
        val migratedRun = RunSaveCodec.decode(legacyV3RunPayload(active = false))

        assertEquals("legacy-run", migratedRun.runId)
        assertEquals("stage-alpha", migratedRun.stageId)
        assertEquals(3, migratedRun.contentVersion)
        assertEquals(7, migratedRun.simulationVersion)
        assertEquals(false, migratedRun.active)
        assertEquals(null, migratedRun.terminalResult)
        assertEquals(
            PendingCommand(
                id = 4L,
                scheduledTick = 18L,
                type = "place",
                actorId = 9L,
                payload = "tower-a",
            ),
            migratedRun.pendingCommands.single(),
        )
        assertEquals(null, migratedRun.playableBattleState)
    }

    @Test
    fun legacyInactiveContourWithoutTerminalRemainsReadable() {
        val migratedRun = RunSaveCodec.decode(
            legacyRunPayload(version = 2, active = false, terminalResult = null),
        )

        assertLegacyRun(
            migratedRun,
            simulationVersion = 7,
            active = false,
            terminalResult = null,
        )
        assertEquals(null, migratedRun.playableBattleState)
    }

    @Test
    fun profileDuplicateSetEntriesAreRejectedBeforeSetConversion() {
        val encoded = ProfileStoreCodec.encode(sampleProfile())
        assertFailsWith<MalformedPersistenceException> {
            ProfileStoreCodec.decode(duplicateSetEntry(encoded, "unlockedCount", "unlocked", 2))
        }
        assertFailsWith<MalformedPersistenceException> {
            ProfileStoreCodec.decode(duplicateSetEntry(encoded, "rosterCount", "roster", 2))
        }
        assertFailsWith<MalformedPersistenceException> {
            ProfileStoreCodec.decode(duplicateSetEntry(encoded, "techCount", "tech", 1))
        }
        assertFailsWith<MalformedPersistenceException> {
            ProfileStoreCodec.decode(duplicateSetEntry(encoded, "claimCount", "claim", 1))
        }
    }

    @Test
    fun malformedInputIsRejectedDeterministically() {
        val malformed = RunSaveCodec.encode(sampleRun()).replaceFirst("tick=12", "tick=not-a-number")

        val first = assertFailsWith<MalformedPersistenceException> { RunSaveCodec.decode(malformed) }
        val second = assertFailsWith<MalformedPersistenceException> { RunSaveCodec.decode(malformed) }
        assertEquals(first.message, second.message)
    }

    @Test
    fun profileMalformedInputIsRejectedDeterministically() {
        val malformed = ProfileStoreCodec.encode(sampleProfile()).replaceFirst("energy=5", "energy=not-a-number")

        val first = assertFailsWith<MalformedPersistenceException> { ProfileStoreCodec.decode(malformed) }
        val second = assertFailsWith<MalformedPersistenceException> { ProfileStoreCodec.decode(malformed) }
        assertEquals(first.message, second.message)
    }

    @Test
    fun futureSchemaVersionFailsExplicitly() {
        val future = RunSaveCodec.encode(sampleRun())
            .replace("schemaVersion=${RunSaveCodec.CURRENT_SCHEMA_VERSION}", "schemaVersion=99")

        val error = assertFailsWith<FutureSchemaVersionException> { RunSaveCodec.decode(future) }
        assertEquals("run-save", error.boundary)
        assertEquals(99, error.version)
    }

    @Test
    fun profileFutureSchemaVersionFailsExplicitly() {
        val future = ProfileStoreCodec.encode(sampleProfile()).replace("schemaVersion=2", "schemaVersion=99")

        val error = assertFailsWith<FutureSchemaVersionException> { ProfileStoreCodec.decode(future) }
        assertEquals("profile-store", error.boundary)
        assertEquals(99, error.version)
    }

    @Test
    fun profileEncodingIsCanonicalForReorderedCollections() {
        val first = sampleProfile()
        val reordered = first.copy(
            unlockedStages = LinkedHashSet(listOf("stage-beta", "stage-alpha")),
            currencies = LinkedHashMap<String, Long>().apply {
                put("gems", 3L)
                put("gold", 120L)
            },
            roster = LinkedHashSet(listOf("unit-b", "unit-a")),
            tech = LinkedHashSet(listOf("tech-a")),
            claims = LinkedHashSet(listOf("daily-001")),
        )

        assertEquals(ProfileStoreCodec.encode(first), ProfileStoreCodec.encode(reordered))
    }

    @Test
    fun profileDecodingCanonicalizesFieldOrderAndPreservesOrderedLists() {
        val profile = sampleProfile().copy(
            loadout = listOf("unit-b", "unit-a"),
            localServiceHistory = listOf("service-1", "service-2"),
        )
        val encoded = ProfileStoreCodec.encode(profile)
        val lines = encoded.lineSequence().toList()
        val reordered = (lines.take(2) + lines.drop(2).asReversed()).joinToString("\n")

        val decoded = ProfileStoreCodec.decode(reordered)

        assertEquals(profile, decoded)
        assertEquals(encoded, ProfileStoreCodec.encode(decoded))
    }

    @Test
    fun profileRejectsDuplicateLoadoutEntries() {
        assertFailsWith<MalformedPersistenceException> {
            ProfileStoreCodec.encode(sampleProfile().copy(loadout = listOf("unit-b", "unit-b")))
        }

        val encoded = ProfileStoreCodec.encode(sampleProfile())
        val duplicate = encoded + "\nloadout.1=" +
            encoded.lineSequence().first { it.startsWith("loadout.0=") }.substringAfter('=')
        val withDuplicateCount = duplicate.replace("loadoutCount=1", "loadoutCount=2")

        assertFailsWith<MalformedPersistenceException> {
            ProfileStoreCodec.decode(withDuplicateCount)
        }
    }

    @Test
    fun profileRejectsNegativeEnergyAndCurrency() {
        assertFailsWith<MalformedPersistenceException> {
            ProfileStoreCodec.encode(sampleProfile().copy(energy = -1))
        }
        assertFailsWith<MalformedPersistenceException> {
            ProfileStoreCodec.encode(
                sampleProfile().copy(currencies = mapOf("gold" to -1L, "gems" to 3L)),
            )
        }

        val negativeEnergy = ProfileStoreCodec.encode(sampleProfile()).replace("energy=5", "energy=-1")
        val negativeCurrency = ProfileStoreCodec.encode(sampleProfile()).replace("currency.1.amount=120", "currency.1.amount=-1")
        assertFailsWith<MalformedPersistenceException> { ProfileStoreCodec.decode(negativeEnergy) }
        assertFailsWith<MalformedPersistenceException> { ProfileStoreCodec.decode(negativeCurrency) }
    }

    @Test
    fun profileRejectsLoadoutItemsOutsideRoster() {
        val invalid = sampleProfile().copy(loadout = listOf("unit-missing"))

        assertFailsWith<MalformedPersistenceException> {
            ProfileStoreCodec.encode(invalid)
        }

        val encoded = ProfileStoreCodec.encode(sampleProfile())
            .replace("loadout.0=" + PersistenceWire.encodeText("unit-b"), "loadout.0=" + PersistenceWire.encodeText("unit-missing"))
        assertFailsWith<MalformedPersistenceException> {
            ProfileStoreCodec.decode(encoded)
        }
    }

    @Test
    fun runEncodingIsCanonicalForReorderedPendingCommands() {
        val first = sampleRun().copy(
            pendingCommands = listOf(
                PendingCommand(4L, 2L, "z", 7L, "last"),
                PendingCommand(1L, 1L, "b", null, "null-actor"),
                PendingCommand(3L, 1L, "a", 2L, "actor"),
                PendingCommand(2L, 0L, "a", 1L, "first"),
            ),
        )
        val reordered = first.copy(pendingCommands = first.pendingCommands.asReversed())

        assertEquals(RunSaveCodec.encode(first), RunSaveCodec.encode(reordered))
        assertEquals(
            listOf(2L, 1L, 3L, 4L),
            RunSaveCodec.decode(RunSaveCodec.encode(reordered)).pendingCommands.map { it.id },
        )
    }

    @Test
    fun runDecodingCanonicalizesV3PendingCommandsRegardlessOfPayloadCommandOrder() {
        val source = sampleRun().copy(
            pendingCommands = listOf(
                PendingCommand(4L, 2L, "z", 7L, "last"),
                PendingCommand(1L, 1L, "b", null, "null-actor"),
                PendingCommand(3L, 1L, "a", 2L, "actor"),
                PendingCommand(2L, 0L, "a", 1L, "first"),
            ),
        )
        val payload = reorderV3CommandBlocks(
            RunSaveCodec.encode(source),
            order = listOf(3, 1, 0, 2),
        )

        val decoded = RunSaveCodec.decode(payload)

        assertEquals(listOf(2L, 1L, 3L, 4L), decoded.pendingCommands.map { it.id })
        assertEquals(
            RunSaveCodec.encode(source),
            RunSaveCodec.encode(decoded),
        )
    }

    @Test
    fun runRoundTripPreservesSignedRandomStateAndExactPendingCommandMetadata() {
        val run = sampleRun().copy(
            seed = Long.MIN_VALUE,
            rngState = Long.MAX_VALUE,
            pendingCommands = listOf(PendingCommand(9L, 17L, "enhance", -4L, "tower-a|payload")),
        )

        assertEquals(run, RunSaveCodec.decode(RunSaveCodec.encode(run)))
    }

    @Test
    fun runRejectsDuplicateFieldsAndPendingCommandIds() {
        val encoded = RunSaveCodec.encode(sampleRun())
        assertFailsWith<MalformedPersistenceException> {
            RunSaveCodec.decode("$encoded\nrunId=${PersistenceWire.encodeText("duplicate")}")
        }

        val duplicateSource = sampleRun().copy(
            pendingCommands = listOf(
                PendingCommand(2L, 1L, "a", null, "one"),
                PendingCommand(3L, 1L, "b", null, "two"),
            ),
        )
        val duplicateId = RunSaveCodec.encode(duplicateSource).replace("command.1.id=3", "command.1.id=2")
        assertFailsWith<MalformedPersistenceException> { RunSaveCodec.decode(duplicateId) }
    }

    @Test
    fun runRejectsInvalidTerminalCombinations() {
        val invalid = RunSaveCodec.encode(sampleRun())
            .replace("active=1", "active=1")
            .replace("terminalPresent=0", "terminalPresent=1")
            .replace("terminalResult=", "terminalResult=VICTORY")

        val decodeError = assertFailsWith<MalformedPersistenceException> {
            RunSaveCodec.decode(invalid)
        }
        assertEquals("Active run cannot have a terminal result", decodeError.message)

        val encodeError = assertFailsWith<MalformedPersistenceException> {
            RunSaveCodec.encode(sampleRun().copy(active = true, terminalResult = RunTerminalResult.DEFEAT))
        }
        assertEquals("Active run cannot have a terminal result", encodeError.message)
    }

    @Test
    fun runDecodingDoesNotDependOnInputFieldOrder() {
        val encoded = RunSaveCodec.encode(sampleRun())
        val lines = encoded.lineSequence().toList()
        val reordered = (lines.take(2) + lines.drop(2).asReversed()).joinToString("\n")

        assertEquals(encoded, RunSaveCodec.encode(RunSaveCodec.decode(reordered)))
    }

    @Test
    fun playableStateRoundTripPreservesAuthoritativeEntityOrderAndHash() {
        val initial = PlayableBattleEngine.initialState()
        val firstEnemy = initial.enemies.single().copy(
            id = "enemy-z",
            health = 5,
            positionTicks = 7,
        )
        val secondEnemy = firstEnemy.copy(id = "enemy-a", health = 4, positionTicks = 11, speedTicks = 3)
        val state = initial.copy(
            phase = PlayableBattlePhase.ACTIVE,
            slots = listOf(
                initial.slots[1].copy(
                    id = ContentId.of("slot-z"),
                    towerId = initial.towerId,
                    towerLevel = PlayableBattleState.MAX_TOWER_LEVEL,
                    towerDamage = initial.towerBaseDamage + initial.towerDamageStep,
                    towerCooldownTicks = initial.towerBaseCooldownTicks - initial.towerCooldownStep,
                    towerCooldownRemainingTicks = 3,
                ),
                initial.slots[0].copy(id = ContentId.of("slot-a")),
            ),
            enemies = listOf(firstEnemy, secondEnemy),
            waveSpawnedCount = 2,
            waveElapsedTicks = 13,
            incomeRemainderTicks = 7,
        )
        val run = sampleRun().copy(
            active = true,
            stageId = state.stageId.value,
            playableBattleState = state,
        )

        val encoded = RunSaveCodec.encode(run)
        val decodedRun = RunSaveCodec.decode(encoded)
        val decoded = requireNotNull(decodedRun.playableBattleState)

        assertEquals(state, decoded)
        assertEquals(listOf("slot-z", "slot-a"), decoded.slots.map { it.id.value })
        assertEquals(listOf("enemy-z", "enemy-a"), decoded.enemies.map { it.id })
        fun hash(value: dev.mysd.game.battle.playable.PlayableBattleState): String =
            stableHashOf { value.appendHash(this) }
        assertEquals(hash(state), hash(decoded))
        assertTrue(
            hash(state) != hash(state.copy(slots = state.slots.asReversed(), enemies = state.enemies.asReversed())),
        )
        assertEquals(encoded, RunSaveCodec.encode(decodedRun))
    }

    @Test
    fun `defeat payload round trip keeps terminal state frozen`() {
        val initial = PlayableBattleEngine.initialState()
        val defeatState = initial.copy(
            base = initial.base.copy(health = 0),
            terminalResult = PlayableBattleTerminal.DEFEAT,
        )
        val run = sampleRun().copy(
            active = false,
            stageId = defeatState.stageId.value,
            terminalResult = RunTerminalResult.DEFEAT,
            playableBattleState = defeatState,
        )

        val encoded = RunSaveCodec.encode(run)
        val decoded = RunSaveCodec.decode(encoded)

        assertEquals(run, decoded)
        assertEquals(false, decoded.active)
        assertEquals(RunTerminalResult.DEFEAT, decoded.terminalResult)
        assertEquals(PlayableBattleTerminal.DEFEAT, decoded.playableState?.terminalResult)
        assertEquals(0, decoded.playableState?.base?.health)
        assertEquals(encoded, RunSaveCodec.encode(decoded))

        val invalidTerminalPayload = replaceField(encoded, "state.base.health", "1")
        val invalidTerminalError = assertFailsWith<MalformedPersistenceException> {
            RunSaveCodec.decode(invalidTerminalPayload)
        }
        assertEquals(
            "Invalid terminal combination: state.base.health must be zero for DEFEAT",
            invalidTerminalError.message,
        )
    }

    @Test
    fun `playable state terminal must match run terminal and active flag`() {
        val initial = PlayableBattleEngine.initialState()
        val nonTerminal = sampleRun().copy(
            active = true,
            stageId = initial.stageId.value,
            terminalResult = null,
            playableBattleState = initial,
        )

        val terminalMismatch = assertFailsWith<MalformedPersistenceException> {
            RunSaveCodec.encode(
                nonTerminal.copy(active = false, terminalResult = RunTerminalResult.DEFEAT),
            )
        }
        assertEquals("Playable state terminal does not match run terminal result", terminalMismatch.message)

        val defeatState = initial.copy(
            base = initial.base.copy(health = 0),
            terminalResult = PlayableBattleTerminal.DEFEAT,
        )
        val activeTerminal = assertFailsWith<MalformedPersistenceException> {
            RunSaveCodec.encode(
                nonTerminal.copy(
                    active = true,
                    terminalResult = RunTerminalResult.DEFEAT,
                    playableBattleState = defeatState,
                ),
            )
        }
        assertEquals("Active run cannot have a terminal result", activeTerminal.message)
    }

    @Test
    fun `run rejects active zero-health playable state without terminal result`() {
        val initial = PlayableBattleEngine.initialState()
        val encoded = RunSaveCodec.encode(
            sampleRun().copy(
                stageId = initial.stageId.value,
                playableBattleState = initial,
            ),
        )
        val invalid = replaceField(encoded, "state.base.health", "0")

        val error = assertFailsWith<MalformedPersistenceException> {
            RunSaveCodec.decode(invalid)
        }

        assertEquals(
            "Invalid terminal combination: state.terminalResult=null requires state.base.health>0",
            error.message,
        )
        assertFailsWith<MalformedPersistenceException> {
            RunSaveCodec.encode(
                sampleRun().copy(
                    stageId = initial.stageId.value,
                    playableBattleState = initial.copy(base = initial.base.copy(health = 0)),
                ),
            )
        }
    }

    @Test
    fun `malformed playable state reports exact nested field paths`() {
        val state = PlayableBattleEngine.initialState()
        val run = sampleRun().copy(
            stageId = state.stageId.value,
            playableBattleState = state,
        )
        val encoded = RunSaveCodec.encode(run)

        val malformedResource = replaceField(encoded, "state.resource", "not-an-int")
        val malformedResourceError = assertFailsWith<MalformedPersistenceException> {
            RunSaveCodec.decode(malformedResource)
        }
        assertEquals(
            "Malformed integer in persistence field: state.resource",
            malformedResourceError.message,
        )

        val negativeResource = replaceField(encoded, "state.resource", "-1")
        val negativeResourceError = assertFailsWith<MalformedPersistenceException> {
            RunSaveCodec.decode(negativeResource)
        }
        assertEquals(
            "Value outside range in persistence field: state.resource",
            negativeResourceError.message,
        )

        val negativeEnemyHealth = replaceField(encoded, "state.enemy.0.health", "-1")
        val negativeEnemyError = assertFailsWith<MalformedPersistenceException> {
            RunSaveCodec.decode(negativeEnemyHealth)
        }
        assertEquals(
            "Negative value in persistence field: state.enemy.0.health",
            negativeEnemyError.message,
        )
    }

    @Test
    fun `malformed playable state rejects duplicate ids and wrong counts`() {
        val initial = PlayableBattleEngine.initialState()
        val firstEnemy = initial.enemies.single().copy(id = "enemy-z")
        val state = initial.copy(
            slots = listOf(
                initial.slots[0].copy(id = ContentId.of("slot-z")),
                initial.slots[1].copy(id = ContentId.of("slot-a")),
            ),
            enemies = listOf(firstEnemy, firstEnemy.copy(id = "enemy-a")),
            waveSpawnedCount = 2,
        )
        val encoded = RunSaveCodec.encode(
            sampleRun().copy(stageId = state.stageId.value, playableBattleState = state),
        )

        val duplicateSlot = replaceField(
            encoded,
            "state.slot.1.id",
            PersistenceWire.encodeText("slot-z"),
        )
        val duplicateSlotError = assertFailsWith<MalformedPersistenceException> {
            RunSaveCodec.decode(duplicateSlot)
        }
        assertEquals(
            "Duplicate playable state slot id: state.slot.1.id",
            duplicateSlotError.message,
        )

        val duplicateEnemy = replaceField(
            encoded,
            "state.enemy.1.id",
            PersistenceWire.encodeText("enemy-z"),
        )
        val duplicateEnemyError = assertFailsWith<MalformedPersistenceException> {
            RunSaveCodec.decode(duplicateEnemy)
        }
        assertEquals(
            "Duplicate playable state enemy id: state.enemy.1.id",
            duplicateEnemyError.message,
        )

        val wrongSlotCount = replaceField(encoded, "state.slotCount", "3")
        val wrongSlotCountError = assertFailsWith<MalformedPersistenceException> {
            RunSaveCodec.decode(wrongSlotCount)
        }
        assertTrue(wrongSlotCountError.message.orEmpty().contains("state.slot.2.id"))

        val wrongEnemyCount = replaceField(encoded, "state.enemyCount", "3")
        val wrongEnemyCountError = assertFailsWith<MalformedPersistenceException> {
            RunSaveCodec.decode(wrongEnemyCount)
        }
        assertTrue(wrongEnemyCountError.message.orEmpty().contains("state.enemy.2.id"))
    }

    @Test
    fun `malformed run counts reject negative and oversized inputs`() {
        val encoded = RunSaveCodec.encode(sampleRun())

        val negativeCommandCount = replaceField(encoded, "commandCount", "-1")
        val negativeCommandError = assertFailsWith<MalformedPersistenceException> {
            RunSaveCodec.decode(negativeCommandCount)
        }
        assertEquals(
            "Invalid item count in persistence field: commandCount",
            negativeCommandError.message,
        )

        val oversizedModifierCount = replaceField(encoded, "modifierCount", "10001")
        val oversizedModifierError = assertFailsWith<MalformedPersistenceException> {
            RunSaveCodec.decode(oversizedModifierCount)
        }
        assertEquals(
            "Invalid item count in persistence field: modifierCount",
            oversizedModifierError.message,
        )

        val wrongCommandCount = replaceField(encoded, "commandCount", "0")
        val wrongCommandCountError = assertFailsWith<MalformedPersistenceException> {
            RunSaveCodec.decode(wrongCommandCount)
        }
        assertTrue(wrongCommandCountError.message.orEmpty().contains("command.0.id"))

        val malformedField = "$encoded\nmalformed-without-separator"
        val malformedFieldError = assertFailsWith<MalformedPersistenceException> {
            RunSaveCodec.decode(malformedField)
        }
        assertEquals("Malformed persistence field", malformedFieldError.message)
    }

    private fun sampleRun() = RunSave(
        runId = "run-001",
        stageId = "stage-alpha",
        contentVersion = 3,
        simulationVersion = 1,
        seed = -42,
        rngState = -7,
        tick = 12,
        active = true,
        pendingCommands = listOf(PendingCommand(4L, 2L, "place", null, "tower-a")),
        modifiers = listOf("calm-start"),
        terminalResult = null,
    )

    private fun sampleProfile() = ProfileStore(
        profileId = "profile-001",
        unlockedStages = setOf("stage-alpha", "stage-beta"),
        currencies = mapOf("gold" to 120L, "gems" to 3L),
        energy = 5,
        roster = setOf("unit-a", "unit-b"),
        loadout = listOf("unit-b"),
        tech = setOf("tech-a"),
        claims = setOf("daily-001"),
        localServiceHistory = listOf("rewarded-ad-local:daily-001"),
    )

    private fun duplicateSetEntry(payload: String, countField: String, itemPrefix: String, count: Int): String {
        val firstValue = payload.lineSequence()
            .first { it.startsWith("$itemPrefix.0=") }
            .substringAfter('=')
        return payload.replace("$countField=$count", "$countField=${count + 1}") +
            "\n$itemPrefix.$count=$firstValue"
    }

    private fun assertLegacyRun(
        run: RunSave,
        simulationVersion: Int,
        active: Boolean,
        terminalResult: RunTerminalResult?,
    ) {
        assertEquals("legacy-run", run.runId)
        assertEquals("stage-alpha", run.stageId)
        assertEquals(3, run.contentVersion)
        assertEquals(simulationVersion, run.simulationVersion)
        assertEquals(-42L, run.seed)
        assertEquals(-7L, run.rngState)
        assertEquals(12L, run.tick)
        assertEquals(active, run.active)
        assertEquals(1, run.pendingCommands.size)
        val command = run.pendingCommands.single()
        assertEquals(4L, command.id)
        assertEquals(0L, command.scheduledTick)
        assertEquals("place", command.type)
        assertEquals(null, command.actorId)
        assertEquals("tower-a", command.payload)
        assertEquals(listOf("calm-start"), run.modifiers)
        assertEquals(terminalResult, run.terminalResult)
        assertEquals(null, run.playableBattleState)
    }

    private fun reorderV3CommandBlocks(payload: String, order: List<Int>): String {
        val lines = payload.lineSequence().toList()
        val commandLines = lines.filter { it.startsWith("command.") }
        val commandCount = commandLines
            .map { it.substringAfter("command.").substringBefore('.').toInt() }
            .maxOrNull()
            ?.plus(1)
            ?: 0
        assertEquals(commandCount, order.size)

        val blocks = (0 until commandCount).associateWith { index ->
            commandLines.filter { it.startsWith("command.$index.") }
        }
        val reorderedCommands = order.flatMapIndexed { newIndex, oldIndex ->
            blocks.getValue(oldIndex).map { line ->
                line.replaceFirst("command.$oldIndex.", "command.$newIndex.")
            }
        }
        return (lines.filterNot { it.startsWith("command.") } + reorderedCommands).joinToString("\n")
    }

    private fun legacyRunPayload(
        version: Int,
        active: Boolean = true,
        terminalResult: RunTerminalResult? = null,
    ): String {
        val fields = linkedMapOf(
            "runId" to PersistenceWire.encodeText("legacy-run"),
            "stageId" to PersistenceWire.encodeText("stage-alpha"),
            "contentVersion" to "3",
            "seed" to "-42",
            "rngState" to "-7",
            "tick" to "12",
            "active" to if (active) "1" else "0",
            "commandCount" to "1",
            "command.0.sequence" to "4",
            "command.0.name" to PersistenceWire.encodeText("place"),
            "command.0.payload" to PersistenceWire.encodeText("tower-a"),
            "modifierCount" to "1",
            "modifier.0" to PersistenceWire.encodeText("calm-start"),
            "terminalPresent" to if (terminalResult == null) "0" else "1",
            "terminalResult" to (terminalResult?.name ?: ""),
        )
        if (version >= 2) fields["simulationVersion"] = "7"
        return PersistenceWire.document("run-save", version, fields)
    }

    private fun legacyV3RunPayload(active: Boolean): String {
        val fields = linkedMapOf(
            "runId" to PersistenceWire.encodeText("legacy-run"),
            "stageId" to PersistenceWire.encodeText("stage-alpha"),
            "contentVersion" to "3",
            "simulationVersion" to "7",
            "seed" to "-42",
            "rngState" to "-7",
            "tick" to "12",
            "active" to if (active) "1" else "0",
            "commandCount" to "1",
            "command.0.id" to "4",
            "command.0.scheduledTick" to "18",
            "command.0.type" to PersistenceWire.encodeText("place"),
            "command.0.actorPresent" to "1",
            "command.0.actorId" to "9",
            "command.0.payload" to PersistenceWire.encodeText("tower-a"),
            "modifierCount" to "1",
            "modifier.0" to PersistenceWire.encodeText("calm-start"),
            "terminalPresent" to "0",
            "terminalResult" to "",
        )
        return PersistenceWire.document("run-save", 3, fields)
    }

    private fun replaceField(payload: String, key: String, replacement: String): String {
        var replaced = false
        return payload.lineSequence().map { line ->
            if (!replaced && line.startsWith("$key=")) {
                replaced = true
                "$key=$replacement"
            } else {
                line
            }
        }.joinToString("\n")
    }
}
