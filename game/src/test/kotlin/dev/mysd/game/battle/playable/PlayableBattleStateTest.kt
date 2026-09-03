package dev.mysd.game.battle.playable

import dev.myengine.core.stableHashOf
import dev.mysd.game.content.ContentId
import dev.mysd.game.content.OriginalContentIds
import dev.mysd.game.content.PlayableLevelContentValidator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class PlayableBattleStateTest {
    @Test
    fun `new first level starts with exactly three known empty fixed slots`() {
        val state = PlayableBattleEngine.initialState()

        assertEquals(PlayableLevelContentValidator.REQUIRED_BUILD_SLOT_COUNT, state.slots.size)
        assertEquals(
            listOf(
                OriginalContentIds.FOUNDATION_BUILD_SLOT_1,
                OriginalContentIds.FOUNDATION_BUILD_SLOT_2,
                OriginalContentIds.FOUNDATION_BUILD_SLOT_3,
            ),
            state.slots.map { it.id },
        )
        assertTrue(state.slots.all { it.towerId == null })
        assertTrue(state.slots.all { it.isEmpty })
    }

    @Test
    fun aliasesExposeAuthoritativeResourceRemainderAndSlotState() {
        val state = PlayableBattleEngine.initialState()

        assertEquals(state.resource, state.globalResource)
        assertEquals(state.incomeRemainderTicks, state.resourceRemainderTicks)
        assertEquals(state.slots, state.buildSlots)
        assertTrue(state.slots.all { it.isEmpty && it.occupiedTowerId == null })
    }

    @Test
    fun invalidStateValuesAndDuplicateEntityIdsAreRejected() {
        val valid = PlayableBattleEngine.initialState()

        assertFailsWith<IllegalArgumentException> { valid.copy(resource = -1) }
        assertFailsWith<IllegalArgumentException> { valid.copy(resource = valid.resourceCap + 1) }
        assertFailsWith<IllegalArgumentException> { valid.copy(resourceCap = -1) }
        assertFailsWith<IllegalArgumentException> { valid.copy(incomePerSecond = -1) }
        assertFailsWith<IllegalArgumentException> { valid.copy(incomeRemainderTicks = 20) }
        assertFailsWith<IllegalArgumentException> { valid.copy(buildCost = -1) }
        assertFailsWith<IllegalArgumentException> {
            valid.copy(slots = listOf(valid.slots[0], valid.slots[1].copy(id = valid.slots[0].id)) + valid.slots.drop(2))
        }
        assertFailsWith<IllegalArgumentException> {
            valid.copy(enemies = listOf(valid.enemies[0], valid.enemies[1].copy(id = valid.enemies[0].id)) + valid.enemies.drop(2))
        }
    }

    @Test
    fun stateHashIsStableAndIncludesAuthoritativeEconomyAndPhase() {
        val active = PlayableBattleEngine.initialState(
            initialResource = 50,
        ).copy(incomeRemainderTicks = 3)
        val sameState = active.copy()
        val changedResource = active.copy(resource = 51)
        val changedPhase = active.copy(phase = PlayableBattlePhase.PAUSED)
        val changedTower = active.copy(towerId = ContentId.of("tower-alternate"))
        val changedBuildCost = active.copy(buildCost = active.buildCost + 1)

        fun hash(state: PlayableBattleState): String = stableHashOf { state.appendHash(this) }

        assertEquals(hash(active), hash(sameState))
        assertNotEquals(hash(active), hash(changedResource))
        assertNotEquals(hash(active), hash(changedPhase))
        assertNotEquals(hash(active), hash(changedTower))
        assertNotEquals(hash(active), hash(changedBuildCost))
    }
}
