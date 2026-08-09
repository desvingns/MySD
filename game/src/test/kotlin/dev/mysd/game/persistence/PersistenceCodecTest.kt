package dev.mysd.game.persistence

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

        assertFailsWith<MalformedPersistenceException> { RunSaveCodec.decode(invalid) }
        assertFailsWith<MalformedPersistenceException> {
            RunSaveCodec.encode(sampleRun().copy(active = true, terminalResult = RunTerminalResult.DEFEAT))
        }
    }

    @Test
    fun runDecodingDoesNotDependOnInputFieldOrder() {
        val encoded = RunSaveCodec.encode(sampleRun())
        val lines = encoded.lineSequence().toList()
        val reordered = (lines.take(2) + lines.drop(2).asReversed()).joinToString("\n")

        assertEquals(encoded, RunSaveCodec.encode(RunSaveCodec.decode(reordered)))
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
}
