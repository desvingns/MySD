package dev.mysd.game.simulation

import dev.mysd.game.battle.playable.PlayableBattleEngine
import dev.mysd.game.battle.playable.PlayableBattlePhase
import dev.myengine.core.CommandId
import dev.myengine.core.EngineSystem
import dev.myengine.core.HashableState
import dev.myengine.core.SeededRandom
import dev.myengine.core.StableHash
import dev.myengine.core.TextCommand
import dev.myengine.core.Tick
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.readText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class SimulationSessionTest {
    @Test
    fun fixedClockAdvancesOnlyAtTwentyHertzAndPreservesRemainder() {
        val clock = SimulationClock()

        assertEquals(0, clock.consume(49))
        assertEquals(49L, clock.pendingMillis)
        assertEquals(1, clock.consume(1))
        assertEquals(0L, clock.pendingMillis)
        assertEquals(2, clock.consume(100))
        assertEquals(20, SimulationClock.TICK_RATE_HZ)
        assertEquals(50L, SimulationClock.TICK_DURATION_MILLIS)
    }

    @Test
    fun playableBattleRunsTwentyActiveTicksForOneSecondWithDeterministicHashes() {
        val initial = PlayableBattleEngine.initialState(
            initialResource = 50,
            resourceCap = 100,
            incomePerSecond = 10,
        )
        val first = SimulationSession.playableBattle(seed = 42L, initialState = initial)
        val second = SimulationSession.playableBattle(seed = 42L, initialState = initial)

        val firstTrajectory = first.advance(1_000)
        val secondTrajectory = second.advance(1_000)

        assertEquals(PlayableBattleEngine.TICKS_PER_SECOND, firstTrajectory.size)
        assertEquals((1L..20L).toList(), firstTrajectory.map { it.tick })
        assertEquals(firstTrajectory, secondTrajectory)
        assertTrue(firstTrajectory.all { it.stateHash.isNotBlank() })
        assertEquals(20L, first.currentTick)
        assertEquals(0L, first.pendingMillis)
        assertEquals(60, first.state().resource)
        assertEquals(0, first.state().incomeRemainderTicks)
        assertEquals(40, first.state().enemies.first().positionTicks)
    }

    @Test
    fun pausedPlayableBattleFreezesFullStateAndResumeKeepsClockRemainder() {
        val session = SimulationSession.playableBattle(
            seed = 7L,
            initialState = PlayableBattleEngine.initialState().copy(incomeRemainderTicks = 19),
        )

        session.advance(25)
        val beforePause = session.snapshot()
        val paused = session.pause()
        assertEquals(PlayableBattlePhase.ACTIVE, paused.phase)
        session.advance(25)
        val afterPausedElapsed = session.snapshot()

        assertEquals(PlayableBattlePhase.PAUSED, afterPausedElapsed.phase)
        assertEquals(1L, afterPausedElapsed.tick)
        assertEquals(0L, afterPausedElapsed.pendingMillis)
        assertEquals(19, afterPausedElapsed.resourceRemainderTicks)
        session.advance(10_000)
        assertEquals(afterPausedElapsed, session.snapshot())
        assertEquals(beforePause.tick + 1L, afterPausedElapsed.tick)
        assertEquals(beforePause.resource, afterPausedElapsed.resource)
        assertEquals(beforePause.enemies, afterPausedElapsed.enemies)

        session.resume()
        val resumed = session.snapshot()
        assertEquals(PlayableBattlePhase.PAUSED, resumed.phase)
        session.advance(50)
        val afterResumeTick = session.snapshot()

        assertEquals(2L, afterResumeTick.tick)
        assertEquals(0L, afterResumeTick.pendingMillis)
        assertEquals(51, afterResumeTick.resource)
        assertEquals(9, afterResumeTick.resourceRemainderTicks)
    }

    @Test
    fun playableBattleRejectsNegativeElapsedMillis() {
        val session = SimulationSession.playableBattle(
            seed = 1L,
            initialState = PlayableBattleEngine.initialState(),
        )

        assertFailsWith<IllegalArgumentException> { session.advance(-1L) }
    }

    @Test
    fun sameSeedAndCommandsProduceIdenticalPerTickStateHashTrajectory() {
        val commands = listOf(
            command(id = 2, tick = 2, payload = "5"),
            command(id = 1, tick = 1, payload = "3"),
        )

        val first = run(seed = 42L, commands = commands)
        val second = run(seed = 42L, commands = commands.asReversed())

        assertEquals(first, second)
        assertEquals(listOf(1L, 2L, 3L, 4L), first.map { it.tick })
        assertTrue(first.zipWithNext().all { (before, after) -> before.stateHash != after.stateHash })
    }

    @Test
    fun seedParticipatesInTheAuthoritativeTrajectory() {
        val commands = listOf(command(id = 1, tick = 1, payload = "3"))

        assertNotEquals(run(seed = 42L, commands = commands), run(seed = 43L, commands = commands))
    }

    @Test
    fun systemsUseExplicitOrderBeforeEachTick() {
        val state = TestState()
        val session = SimulationSession(
            seed = 7L,
            initialState = state,
            systems = listOf(
                RecordingSystem(id = "z-last", order = 20),
                RecordingSystem(id = "a-first", order = 10),
            ),
        )

        assertEquals(listOf("a-first", "z-last"), session.systemOrder)
        session.advance(50)

        assertEquals(listOf("a-first@1", "z-last@1"), state.events)
    }

    @Test
    fun snapshotExposesOnlyImmutableAuthoritativeMetadata() {
        val session = SimulationSession(
            seed = 1L,
            initialState = TestState(),
            systems = listOf(RecordingSystem(id = "only", order = 0)),
        )

        session.advance(150)
        val snapshot = session.snapshot()

        assertEquals(3L, snapshot.tick)
        assertEquals(snapshot.stateHash, session.advance(0).let { session.snapshot().stateHash })
        assertFalse(SimulationSnapshot::class.java.declaredFields.any { it.name == "state" })
        assertFalse(SimulationSession::class.java.methods.any { it.name == "getState" })
    }

    @Test
    fun simulationBoundaryIsJvmOnlyAndContainsNoAndroidImports() {
        val sourceRoot = sequenceOf(
            Path("src/main/kotlin/dev/mysd/game/simulation"),
            Path("game/src/main/kotlin/dev/mysd/game/simulation"),
        ).first { it.exists() && it.isDirectory() }
        val source = sourceRoot.toFile().walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .joinToString("\n") { it.readText() }

        assertFalse(Regex("(?m)^import\\s+android\\.").containsMatchIn(source))
        assertTrue(Path("build.gradle.kts").exists() || Path("game/build.gradle.kts").exists())
    }

    private fun run(seed: Long, commands: List<TextCommand>): List<SimulationTickResult> {
        val session = SimulationSession(
            seed = seed,
            initialState = TestState(),
            systems = listOf(
                CommandSystem(id = "commands", order = 10),
                RandomSystem(id = "random", order = 20),
            ),
        )
        session.submitAll(commands)
        return session.advance(200)
    }

    private fun command(id: Long, tick: Long, payload: String): TextCommand = TextCommand(
        id = CommandId(id),
        scheduledTick = Tick(tick),
        type = "set-value",
        payload = payload,
    )

    private data class TestState(
        var tickCount: Long = 0,
        var value: Long = 0,
        var randomValue: Long = 0,
        val events: MutableList<String> = mutableListOf(),
    ) : HashableState {
        override fun appendHash(hash: StableHash) {
            hash.add(tickCount).add(value).add(randomValue)
            events.forEach(hash::add)
        }
    }

    private class CommandSystem(
        override val id: String,
        override val order: Int,
    ) : EngineSystem<TestState> {
        override fun update(context: dev.myengine.core.SimulationContext<TestState>) {
            context.state.tickCount = context.tick.value
            context.commands.forEach { context.state.value += it.stablePayload().toLong() }
            context.state.events += "$id@${context.tick.value}"
        }
    }

    private class RandomSystem(
        override val id: String,
        override val order: Int,
    ) : EngineSystem<TestState> {
        override fun update(context: dev.myengine.core.SimulationContext<TestState>) {
            context.state.randomValue = context.random.nextLong()
            context.state.events += "$id@${context.tick.value}"
        }
    }

    private class RecordingSystem(
        override val id: String,
        override val order: Int,
    ) : EngineSystem<TestState> {
        override fun update(context: dev.myengine.core.SimulationContext<TestState>) {
            context.state.tickCount = context.tick.value
            context.state.events += "$id@${context.tick.value}"
        }
    }
}
