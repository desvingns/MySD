package dev.mysd.game.battle.playable

import dev.mysd.game.content.ContentId
import dev.mysd.game.content.OriginalContentFixtures
import dev.mysd.game.simulation.SimulationSession
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.assertTrue

class PlayableBattleEngineTest {
    @Test
    fun twentyActiveTicksAccumulateExactlyOneConfiguredSecondAndAdvanceEntities() {
        val initial = PlayableBattleEngine.initialState(
            initialResource = 50,
            resourceCap = 100,
            incomePerSecond = 10,
        )

        val result = PlayableBattleEngine.advance(initial, 20)

        assertEquals(60, result.resource)
        assertEquals(0, result.incomeRemainderTicks)
        assertEquals(40, result.enemies.first().positionTicks)
    }

    @Test
    fun passiveIncomeUsesIntegerRemainderAndPreservesRemainderAtCap() {
        val first = PlayableBattleEngine.calculatePassiveIncome(
            currentResource = 50,
            incomePerSecond = 7,
            deltaTicks = 3,
            incomeRemainderTicks = 2,
            resourceCap = 100,
        )
        val capped = PlayableBattleEngine.calculatePassiveIncome(
            currentResource = 99,
            incomePerSecond = 10,
            deltaTicks = 20,
            incomeRemainderTicks = 1,
            resourceCap = 100,
        )

        assertEquals(1, first.gain)
        assertEquals(51, first.resource)
        assertEquals(3, first.remainderTicks)
        assertEquals(100, capped.resource)
        assertEquals(1, capped.remainderTicks)
    }

    @Test
    fun integerIncomeAccumulatesAcrossTicksWithoutFractionalResource() {
        val initial = PlayableBattleEngine.initialState(
            initialResource = 0,
            resourceCap = 100,
            incomePerSecond = 7,
        )

        val afterNineteenTicks = (1..19).fold(initial) { state, _ ->
            PlayableBattleEngine.tick(state)
        }
        val afterTwentyTicks = PlayableBattleEngine.tick(afterNineteenTicks)

        assertEquals(6, afterNineteenTicks.resource)
        assertEquals(13, afterNineteenTicks.incomeRemainderTicks)
        assertEquals(7, afterTwentyTicks.resource)
        assertEquals(0, afterTwentyTicks.incomeRemainderTicks)
    }

    @Test
    fun pausedReductionIsAnIdentityForResourceRemainderAndEnemyPositions() {
        val initial = PlayableBattleEngine.initialState(
            incomePerSecond = 10,
            phase = PlayableBattlePhase.PAUSED,
        ).copy(incomeRemainderTicks = 19)

        val result = PlayableBattleEngine.advance(initial, 20)

        assertSame(initial, result)
        assertEquals(initial.resource, result.resource)
        assertEquals(initial.incomeRemainderTicks, result.incomeRemainderTicks)
        assertEquals(initial.enemies.map { it.positionTicks }, result.enemies.map { it.positionTicks })
    }

    @Test
    fun insufficientSpendIsAtomicAndLeavesTargetSlotEmpty() {
        val initial = PlayableBattleEngine.initialState(initialResource = 10)
        val target = initial.slots.first().id

        val result = PlayableBattleEngine.spend(initial, target, cost = 40)

        assertFalse(result.accepted)
        assertEquals(PlayableBattleSpendRejection.INSUFFICIENT_RESOURCE, result.rejection)
        assertSame(initial, result.state)
        assertEquals(10, result.resource)
        assertTrue(result.state.slots.first { it.id == target }.isEmpty)
    }

    @Test
    fun acceptedSpendIsAtomicAndDoesNotMakeResourceNegative() {
        val initial = PlayableBattleEngine.initialState(initialResource = 10)
        val target = initial.slots.first().id

        val result = PlayableBattleEngine.spend(initial, target, cost = 10)

        assertTrue(result.accepted)
        assertEquals(0, result.resource)
        assertTrue(result.state.slots.first { it.id == target }.isEmpty)
        assertTrue(result.state.resource in 0..result.state.resourceCap)
    }

    @Test
    fun unknownOrOccupiedTargetIsRejectedAtomically() {
        val initial = PlayableBattleEngine.initialState(initialResource = 100)
        val unknownTarget = ContentId.of("build-slot-unknown")
        val unknownResult = PlayableBattleEngine.spend(initial, unknownTarget, cost = 10)

        val target = initial.slots.first().id
        val occupied = initial.copy(
            slots = listOf(initial.slots.first().copy(towerId = ContentId.of("tower-placed"))) +
                initial.slots.drop(1),
        )
        val occupiedResult = PlayableBattleEngine.spend(occupied, target, cost = 10)

        assertFalse(unknownResult.accepted)
        assertEquals(PlayableBattleSpendRejection.UNKNOWN_SLOT, unknownResult.rejection)
        assertSame(initial, unknownResult.state)
        assertEquals(100, unknownResult.resource)
        assertFalse(occupiedResult.accepted)
        assertEquals(PlayableBattleSpendRejection.TARGET_SLOT_OCCUPIED, occupiedResult.rejection)
        assertSame(occupied, occupiedResult.state)
        assertEquals(100, occupiedResult.resource)
    }

    @Test
    fun pausedSpendIsRejectedWithoutChangingResourceOrTargetSlot() {
        val initial = PlayableBattleEngine.initialState(
            initialResource = 50,
            phase = PlayableBattlePhase.PAUSED,
        )
        val target = initial.slots.first().id

        val result = PlayableBattleEngine.spend(initial, target, cost = 10)

        assertFalse(result.accepted)
        assertEquals(PlayableBattleSpendRejection.BATTLE_PAUSED, result.rejection)
        assertSame(initial, result.state)
        assertEquals(50, result.resource)
        assertTrue(result.state.slots.first { it.id == target }.isEmpty)
    }

    @Test
    fun `build tower on affordable empty slot places configured tower and deducts exact cost`() {
        val initial = PlayableBattleEngine.initialState(
            initialResource = 100,
            incomePerSecond = 0,
        )
        val target = initial.slots[1].id

        val result = PlayableBattleEngine.buildTower(initial, target)

        assertTrue(result.accepted)
        assertTrue(result.successful)
        assertEquals(target, result.targetSlotId)
        assertEquals(null, result.rejection)
        assertEquals(initial.resource - initial.buildCost, result.resource)
        assertEquals(initial.towerId, result.state.slots[1].towerId)
        assertEquals(1, result.state.slots.count { !it.isEmpty })
        assertTrue(result.state.slots.filter { it.id != target }.all { it.isEmpty })
    }

    @Test
    fun `build tower rejects unknown slot without partial mutation`() {
        val initial = PlayableBattleEngine.initialState(
            initialResource = 100,
            incomePerSecond = 0,
        )
        val unknownSlot = ContentId.of("build-slot-unknown")

        val result = PlayableBattleEngine.buildTower(initial, unknownSlot)

        assertFalse(result.accepted)
        assertEquals(PlayableBattleSpendRejection.UNKNOWN_SLOT, result.rejection)
        assertSame(initial, result.state)
        assertEquals(100, result.resource)
        assertTrue(result.state.slots.all { it.isEmpty })
    }

    @Test
    fun `build tower rejects occupied slot without partial mutation`() {
        val initial = PlayableBattleEngine.initialState(
            initialResource = 100,
            incomePerSecond = 0,
        )
        val target = initial.slots[0].id
        val occupied = initial.copy(
            slots = initial.slots.map { slot ->
                if (slot.id == target) slot.copy(towerId = initial.towerId) else slot
            },
        )

        val result = PlayableBattleEngine.buildTower(occupied, target)

        assertFalse(result.accepted)
        assertEquals(PlayableBattleSpendRejection.TARGET_SLOT_OCCUPIED, result.rejection)
        assertSame(occupied, result.state)
        assertEquals(100, result.resource)
        assertEquals(initial.towerId, result.state.slots.first { it.id == target }.towerId)
    }

    @Test
    fun `build tower rejects unaffordable empty slot without partial mutation`() {
        val initial = PlayableBattleEngine.initialState(
            initialResource = 0,
            incomePerSecond = 0,
        )
        val target = initial.slots[2].id

        val result = PlayableBattleEngine.buildTower(initial, target)

        assertFalse(result.accepted)
        assertEquals(PlayableBattleSpendRejection.INSUFFICIENT_RESOURCE, result.rejection)
        assertSame(initial, result.state)
        assertEquals(0, result.resource)
        assertTrue(result.state.slots.all { it.isEmpty })
    }

    @Test
    fun `build tower rejects paused battle without partial mutation`() {
        val initial = PlayableBattleEngine.initialState(
            initialResource = 100,
            incomePerSecond = 0,
            phase = PlayableBattlePhase.PAUSED,
        )
        val target = initial.slots[1].id

        val result = PlayableBattleEngine.buildTower(initial, target)

        assertFalse(result.accepted)
        assertEquals(PlayableBattleSpendRejection.BATTLE_PAUSED, result.rejection)
        assertSame(initial, result.state)
        assertEquals(100, result.resource)
        assertTrue(result.state.slots.all { it.isEmpty })
    }

    @Test
    fun invalidEconomyAndTickInputsAreRejected() {
        val initial = PlayableBattleEngine.initialState()

        assertFailsWith<IllegalArgumentException> {
            PlayableBattleEngine.calculatePassiveIncome(
                currentResource = -1,
                incomePerSecond = 10,
                deltaTicks = 1,
                incomeRemainderTicks = 0,
                resourceCap = 100,
            )
        }
        assertFailsWith<IllegalArgumentException> {
            PlayableBattleEngine.calculatePassiveIncome(
                currentResource = 50,
                incomePerSecond = -1,
                deltaTicks = 1,
                incomeRemainderTicks = 0,
                resourceCap = 100,
            )
        }
        assertFailsWith<IllegalArgumentException> {
            PlayableBattleEngine.calculatePassiveIncome(
                currentResource = 50,
                incomePerSecond = 10,
                deltaTicks = 1,
                incomeRemainderTicks = 20,
                resourceCap = 100,
            )
        }
        assertFailsWith<IllegalArgumentException> {
            PlayableBattleEngine.advance(initial, deltaTicks = -1)
        }
        assertFailsWith<IllegalArgumentException> {
            PlayableBattleEngine.spend(initial, initial.slots.first().id, cost = -1)
        }
    }

    @Test
    fun sessionUsesFixedClockAndKeepsClockRemainderAcrossPause() {
        val initial = PlayableBattleEngine.initialState().copy(incomeRemainderTicks = 19)
        val session = SimulationSession.playableBattle(seed = 7L, initialState = initial)

        session.advance(25)
        val beforePause = session.snapshot()
        session.pause()
        val queuedPause = session.snapshot()
        val pauseTicks = session.advance(25)
        val paused = session.snapshot()
        session.advance(1_000)
        val whilePaused = session.snapshot()

        assertEquals(0L, beforePause.tick)
        assertEquals(25L, beforePause.pendingMillis)
        assertEquals(PlayableBattlePhase.ACTIVE, queuedPause.phase)
        assertEquals(1, pauseTicks.single().commandsProcessed)
        assertEquals(1L, paused.tick)
        assertEquals(PlayableBattlePhase.PAUSED, paused.phase)
        assertEquals(paused, whilePaused)
        assertEquals(1L, whilePaused.tick)
        assertEquals(0L, whilePaused.pendingMillis)
        assertEquals(beforePause.resource, whilePaused.resource)
        assertEquals(
            beforePause.enemies.map { it.positionTicks },
            whilePaused.enemies.map { it.positionTicks },
        )

        session.resume()
        val queuedResume = session.snapshot()
        session.advance(50)
        val afterResume = session.snapshot()

        assertEquals(PlayableBattlePhase.PAUSED, queuedResume.phase)
        assertEquals(2L, afterResume.tick)
        assertEquals(0L, afterResume.pendingMillis)
        assertEquals(51, afterResume.resource)
        assertEquals(9, afterResume.resourceRemainderTicks)
    }

    @Test
    fun queuedBuildTowerUsesSimulationReplayBoundaryAndIsVisibleAfterOneTick() {
        val initial = PlayableBattleEngine.initialState(
            initialResource = 50,
            resourceCap = 100,
            incomePerSecond = 0,
        )
        val first = SimulationSession.playableBattle(seed = 42L, initialState = initial)
        val second = SimulationSession.playableBattle(seed = 42L, initialState = initial)
        val target = initial.slots[1].id

        first.buildTower(target)
        second.buildTower(target)

        assertTrue(first.state().slots[1].isEmpty)
        assertTrue(first.advance(49).isEmpty())
        assertTrue(second.advance(49).isEmpty())
        assertEquals(49L, first.pendingMillis)
        assertEquals(49L, second.pendingMillis)
        assertEquals(first.canonicalCommandEncoding(), second.canonicalCommandEncoding())
        assertEquals(first.inputHash(), second.inputHash())
        assertEquals(first.replayHashChain(), second.replayHashChain())

        val firstTick = first.advance(1)
        val secondTick = second.advance(1)
        val firstSnapshot = first.snapshot()
        val secondSnapshot = second.snapshot()

        assertEquals(1, firstTick.single().commandsProcessed)
        assertEquals(firstTick, secondTick)
        assertEquals(initial.towerId, firstSnapshot.state.slots[1].towerId)
        assertEquals(initial.resource - initial.buildCost, firstSnapshot.resource)
        assertEquals(firstSnapshot, secondSnapshot)
        assertEquals(first.replayHashChain(), second.replayHashChain())
    }

    @Test
    fun sameSeedAndCommandsProduceTheSamePlayableReplayTrajectory() {
        val initial = PlayableBattleEngine.initialState(
            initialResource = 50,
            incomePerSecond = 13,
        )
        val first = SimulationSession.playableBattle(seed = 42L, initialState = initial)
        val second = SimulationSession.playableBattle(seed = 42L, initialState = initial)

        first.advance(49)
        second.advance(49)
        first.advance(951)
        second.advance(951)
        first.spend(ContentId.of("build-slot-ash-left"), 10)
        second.spend(ContentId.of("build-slot-ash-left"), 10)

        assertEquals(first.snapshot(), second.snapshot())
        assertEquals(first.snapshot().stateHash, second.snapshot().stateHash)
    }
}
