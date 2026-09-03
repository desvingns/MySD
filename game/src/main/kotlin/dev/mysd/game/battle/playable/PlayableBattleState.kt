package dev.mysd.game.battle.playable

import dev.myengine.core.HashableState
import dev.myengine.core.StableHash
import dev.mysd.game.content.ContentId

/** The only phases in scope for the first playable runtime. */
enum class PlayableBattlePhase {
    ACTIVE,
    PAUSED,
}

/** Immutable snapshot of the playable level's main base. */
data class PlayableBattleBaseState(
    val id: ContentId,
    val health: Int,
    val maxHealth: Int,
    val positionTicks: Int,
) {
    init {
        require(maxHealth >= 0) { "Base max health must be non-negative." }
        require(health in 0..maxHealth) { "Base health must be within its max health." }
        require(positionTicks >= 0) { "Base position must be non-negative." }
    }

    val currentHealth: Int
        get() = health
}

/** Immutable snapshot of one fixed build location. Occupancy is reserved for the next SPEC. */
data class PlayableBattleSlotState(
    val id: ContentId,
    val positionTicks: Int,
    val towerId: ContentId? = null,
) {
    init {
        require(positionTicks >= 0) { "Build slot position must be non-negative." }
    }

    val isEmpty: Boolean
        get() = towerId == null

    val occupiedTowerId: ContentId?
        get() = towerId
}

/** Immutable snapshot of one enemy entity moving along the deterministic path. */
data class PlayableBattleEnemyState(
    val id: String,
    val familyId: ContentId,
    val health: Int,
    val positionTicks: Int,
    val speedTicks: Int = 0,
) {
    init {
        require(id.isNotBlank()) { "Enemy id must not be blank." }
        require(health >= 0) { "Enemy health must be non-negative." }
        require(positionTicks >= 0) { "Enemy position must be non-negative." }
        require(speedTicks >= 0) { "Enemy speed must be non-negative." }
    }

    val entityId: String
        get() = id
}

/**
 * Immutable, replay-hashable authoritative state for the first playable battle.
 *
 * The lists are exposed as Kotlin read-only lists and every reducer creates new list instances.
 * Rendering and input consume this boundary; neither owns a mutable copy of the battle state.
 */
data class PlayableBattleState(
    val stageId: ContentId,
    val phase: PlayableBattlePhase,
    val base: PlayableBattleBaseState,
    val resource: Int,
    val resourceCap: Int,
    val incomePerSecond: Int,
    val incomeRemainderTicks: Int,
    val slots: List<PlayableBattleSlotState>,
    val enemies: List<PlayableBattleEnemyState>,
) : HashableState {
    init {
        require(resourceCap >= 0) { "Resource cap must be non-negative." }
        require(resource in 0..resourceCap) { "Resource must be within its cap." }
        require(incomePerSecond >= 0) { "Income per second must be non-negative." }
        require(incomeRemainderTicks in 0 until TICKS_PER_SECOND) {
            "Income remainder must be within one fixed second."
        }
        require(slots.map { it.id }.toSet().size == slots.size) {
            "Build slot ids must be unique."
        }
        require(enemies.map { it.id }.toSet().size == enemies.size) {
            "Enemy entity ids must be unique."
        }
    }

    /** Alias used by economy-facing callers that name this resource globally. */
    val globalResource: Int
        get() = resource

    val resourceRemainderTicks: Int
        get() = incomeRemainderTicks

    val buildSlots: List<PlayableBattleSlotState>
        get() = slots

    override fun appendHash(hash: StableHash) {
        hash.add("mysd.playable-battle-state.v1")
            .add(stageId.value)
            .add(phase.name)
            .add(resource)
            .add(resourceCap)
            .add(incomePerSecond)
            .add(incomeRemainderTicks)
            .add(base.id.value)
            .add(base.health)
            .add(base.maxHealth)
            .add(base.positionTicks)

        hash.add(slots.size)
        slots.forEach { slot ->
            hash.add(slot.id.value)
                .add(slot.positionTicks)
                .add(slot.towerId != null)
            slot.towerId?.let { hash.add(it.value) }
        }

        hash.add(enemies.size)
        enemies.forEach { enemy ->
            hash.add(enemy.id)
                .add(enemy.familyId.value)
                .add(enemy.health)
                .add(enemy.positionTicks)
                .add(enemy.speedTicks)
        }
    }

    private companion object {
        const val TICKS_PER_SECOND: Int = 20
    }
}

typealias PlayableBaseState = PlayableBattleBaseState
typealias PlayableSlotState = PlayableBattleSlotState
typealias PlayableEnemyState = PlayableBattleEnemyState
