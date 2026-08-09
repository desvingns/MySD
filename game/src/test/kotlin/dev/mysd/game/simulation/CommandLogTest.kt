package dev.mysd.game.simulation

import dev.myengine.core.CommandId
import dev.myengine.core.CommandQueue
import dev.myengine.core.TextCommand
import dev.myengine.core.Tick
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.readText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class CommandLogTest {
    @Test
    fun canonicalOrderIsIndependentOfInsertionOrderAndMatchesPinnedComparator() {
        val commands = listOf(
            command(id = 4, tick = 2, type = "z", actorId = 7, payload = "last"),
            command(id = 1, tick = 1, type = "b", actorId = null, payload = "null-actor"),
            command(id = 3, tick = 1, type = "a", actorId = 2, payload = "actor"),
            command(id = 2, tick = 0, type = "a", actorId = 1, payload = "first"),
        )
        val first = CommandLog().also { commands.forEach(it::append) }
        val second = CommandLog().also { commands.asReversed().forEach(it::append) }

        assertEquals(commands.sortedWith(CommandQueue.commandComparator), first.canonicalCommands())
        assertEquals(first.canonicalCommands(), second.canonicalCommands())
        assertEquals(first.canonicalEncoding(), second.canonicalEncoding())
    }

    @Test
    fun idsAreNonNegativeMonotonicAndAdvancePastExternallyProvidedIds() {
        val log = CommandLog()

        assertEquals(0L, log.allocateId().value)
        assertEquals(1L, log.allocateId().value)
        log.append(command(id = 9, tick = 1, type = "external", actorId = null, payload = "x"))
        assertEquals(10L, log.allocateId().value)

        assertFailsWith<IllegalArgumentException> { CommandLog(firstId = -1L) }
    }

    @Test
    fun duplicateIdsAreRejectedWithoutReplacingOriginalCommand() {
        val log = CommandLog()
        val original = command(id = 5, tick = 1, type = "original", actorId = null, payload = "one")
        val duplicate = command(id = 5, tick = 2, type = "duplicate", actorId = null, payload = "two")

        log.append(original)
        val error = assertFailsWith<DuplicateCommandIdException> { log.append(duplicate) }

        assertTrue(error.message!!.contains("5"))
        assertEquals(listOf(original), log.commands())
    }

    @Test
    fun allocatedTextCommandsAreRecordedWithStableIds() {
        val log = CommandLog(firstId = 7)

        val first = log.submit(Tick(2), "build", "tile:1", actorId = 4)
        val second = log.submit(Tick(2), "build", "tile:2", actorId = 4)

        assertEquals(listOf(7L, 8L), log.commands().map { it.id.value })
        assertSame(first, log.commands()[0])
        assertSame(second, log.commands()[1])
    }

    @Test
    fun canonicalEncodingIsDeterministicAndDelimitersAreSafe() {
        val command = command(
            id = 0,
            tick = 3,
            type = "type|with=delimiters",
            actorId = null,
            payload = "payload\nwith unicode ✓",
        )

        val encoded = CommandLogCodec.encode(listOf(command))

        assertEquals(encoded, CommandLogCodec.encode(listOf(command)))
        assertTrue(encoded.startsWith("mysd.command-log|commands\nschemaVersion=1\ncommandCount=1"))
        assertFalse(encoded.contains("type|with=delimiters"))
        assertFalse(encoded.contains("payload\nwith unicode"))
        assertTrue(encoded.contains("command.0.actorPresent=0\ncommand.0.actorId="))
    }

    @Test
    fun inputHashAndReplayChainAreStableAndSensitiveToCanonicalInput() {
        val commands = listOf(
            command(id = 2, tick = 2, type = "b", actorId = 9, payload = "two"),
            command(id = 1, tick = 1, type = "a", actorId = null, payload = "one"),
        )
        val first = CommandLog().also { commands.forEach(it::append) }
        val second = CommandLog().also { commands.asReversed().forEach(it::append) }
        val changed = CommandLog().also {
            commands.map { it.copy(type = if (it.id.value == 1L) "changed" else it.type) }
                .forEach(it::append)
        }

        assertEquals(first.inputHash(), second.inputHash())
        assertEquals(first.replayHashes(), second.replayHashes())
        assertEquals(first.inputHash(), first.replayHashes().inputHash)
        assertEquals(first.replayHashChain(), first.replayHashes().finalHash)
        assertEquals(commands.size, first.replayHashes().hashes.size)
        assertNotEquals(first.inputHash(), changed.inputHash())
        assertNotEquals(first.replayHashChain(), changed.replayHashChain())
    }

    @Test
    fun commandLogBoundaryIsJvmOnlyAndExposesNoMutableAuthoritativeState() {
        val sourceRoot = sequenceOf(
            Path("src/main/kotlin/dev/mysd/game/simulation"),
            Path("game/src/main/kotlin/dev/mysd/game/simulation"),
        ).first { it.exists() && it.isDirectory() }
        val source = sourceRoot.toFile().walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .joinToString("\n") { it.readText() }

        assertFalse(Regex("(?m)^import\\s+android\\.").containsMatchIn(source))
        assertFalse(CommandLog::class.java.methods.any { it.name == "getState" })
        assertFalse(CommandLog::class.java.methods.any { it.name == "getAuthoritativeState" })
    }

    private fun command(
        id: Long,
        tick: Long,
        type: String,
        actorId: Long?,
        payload: String,
    ): TextCommand = TextCommand(
        id = CommandId(id),
        scheduledTick = Tick(tick),
        type = type,
        payload = payload,
        actorId = actorId,
    )
}
