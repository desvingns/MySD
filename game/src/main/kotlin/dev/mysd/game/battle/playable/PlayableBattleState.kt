package dev.mysd.game.battle.playable

import dev.myengine.core.HashableState
import dev.myengine.core.StableHash
import dev.mysd.game.content.ContentId
import dev.mysd.game.content.OriginalContentFixtures

/** The only phases in scope for the first playable runtime. */
enum class PlayableBattlePhase {
    ACTIVE,
    PAUSED,
}

/** Terminal result of a playable battle, or no result while the battle is still running. */
enum class PlayableBattleTerminal {
    VICTORY,
    DEFEAT,
}

typealias PlayableBattleTerminalResult = PlayableBattleTerminal

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

/** Immutable snapshot of one content-defined fixed build location. */
data class PlayableBattleSlotState(
    val id: ContentId,
    val positionTicks: Int,
    val towerId: ContentId? = null,
    val towerLevel: Int = 0,
    val towerDamage: Int? = null,
    val towerCooldownTicks: Int? = null,
    val towerCooldownRemainingTicks: Int = 0,
) {
    init {
        require(positionTicks >= 0) { "Build slot position must be non-negative." }
        require(towerLevel in 0..PlayableBattleState.MAX_TOWER_LEVEL) {
            "Tower level must be between 0 and ${PlayableBattleState.MAX_TOWER_LEVEL}."
        }
        require(towerDamage == null || towerDamage >= 0) {
            "Tower damage must be non-negative when configured."
        }
        require(towerCooldownTicks == null || towerCooldownTicks >= 1) {
            "Tower cooldown must be at least one tick when configured."
        }
        require(towerCooldownRemainingTicks >= 0) {
            "Tower cooldown remaining must be non-negative."
        }
        if (towerId == null) {
            require(towerLevel == 0) { "An empty build slot must have tower level zero." }
            require(towerDamage == null) { "An empty build slot must not have tower damage." }
            require(towerCooldownTicks == null) {
                "An empty build slot must not have tower cooldown."
            }
            require(towerCooldownRemainingTicks == 0) {
                "An empty build slot must not have a cooldown remaining."
            }
        } else if (towerLevel > 0) {
            require(towerDamage != null && towerCooldownTicks != null) {
                "An upgraded tower must expose its current stats."
            }
        }
    }

    val isEmpty: Boolean
        get() = towerId == null

    val occupiedTowerId: ContentId?
        get() = towerId

    val level: Int
        get() = towerLevel

    val damage: Int?
        get() = towerDamage

    val cooldownTicks: Int?
        get() = towerCooldownTicks

    val cooldownRemainingTicks: Int
        get() = towerCooldownRemainingTicks
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
    val towerId: ContentId = OriginalContentFixtures.foundationPlayableLevel().tower.id,
    val buildCost: Int = OriginalContentFixtures.foundationPlayableLevel().tower.buildCost,
    val towerBaseDamage: Int = OriginalContentFixtures.foundationPlayableLevel().tower.damage,
    val towerBaseCooldownTicks: Int = OriginalContentFixtures.foundationPlayableLevel().tower.cooldownTicks,
    val towerUpgradeBaseCost: Int = OriginalContentFixtures.foundationPlayableLevel().tower.upgradeBaseCost,
    val towerUpgradeCostStep: Int = OriginalContentFixtures.foundationPlayableLevel().tower.upgradeCostStep,
    val towerDamageStep: Int = OriginalContentFixtures.foundationPlayableLevel().tower.damageStep,
    val towerCooldownStep: Int = OriginalContentFixtures.foundationPlayableLevel().tower.cooldownStep,
    val towerMinCooldownTicks: Int = OriginalContentFixtures.foundationPlayableLevel().tower.minCooldownTicks,
    val terminalResult: PlayableBattleTerminal? = null,
    val waveId: ContentId = OriginalContentFixtures.foundationPlayableLevel().wave.id,
    val enemyFamilyId: ContentId = OriginalContentFixtures.foundationPlayableLevel().enemyFamily.id,
    val enemyHealth: Int = OriginalContentFixtures.foundationPlayableLevel().enemyFamily.health,
    val enemySpeedTicks: Int = OriginalContentFixtures.foundationPlayableLevel().enemyFamily.speedTicks,
    val waveSpawnCount: Int = OriginalContentFixtures.foundationPlayableLevel().wave.spawnCount,
    val waveSpawnedCount: Int = enemies.size,
    val waveElapsedTicks: Int = 0,
    val waveSpawnIntervalTicks: Int = OriginalContentFixtures.foundationPlayableLevel().wave.spawnIntervalTicks,
    val towerRangeTicks: Int = OriginalContentFixtures.foundationPlayableLevel().tower.rangeTicks,
    val baseLeakDamage: Int = OriginalContentFixtures.foundationPlayableLevel().enemyFamily.baseDamage,
) : HashableState {
    init {
        require(resourceCap >= 0) { "Resource cap must be non-negative." }
        require(resource in 0..resourceCap) { "Resource must be within its cap." }
        require(incomePerSecond >= 0) { "Income per second must be non-negative." }
        require(incomeRemainderTicks in 0 until TICKS_PER_SECOND) {
            "Income remainder must be within one fixed second."
        }
        require(buildCost >= 0) { "Tower build cost must be non-negative." }
        require(towerBaseDamage >= 0) { "Tower base damage must be non-negative." }
        require(towerBaseCooldownTicks >= 1) { "Tower base cooldown must be at least one tick." }
        require(towerUpgradeBaseCost >= 0) { "Tower upgrade base cost must be non-negative." }
        require(towerUpgradeCostStep >= 0) { "Tower upgrade cost step must be non-negative." }
        require(towerDamageStep >= 0) { "Tower damage step must be non-negative." }
        require(towerCooldownStep >= 0) { "Tower cooldown step must be non-negative." }
        require(towerMinCooldownTicks >= 1) { "Tower minimum cooldown must be at least one tick." }
        require(waveId.value.isNotBlank()) { "Wave id must not be blank." }
        require(enemyFamilyId.value.isNotBlank()) { "Enemy family id must not be blank." }
        require(enemyHealth >= 0) { "Enemy health must be non-negative." }
        require(enemySpeedTicks >= 0) { "Enemy speed must be non-negative." }
        require(waveSpawnCount in 8..10) { "Wave spawn count must be between 8 and 10." }
        require(waveSpawnedCount in 0..waveSpawnCount) {
            "Wave spawned count must be within the configured wave count."
        }
        require(waveElapsedTicks >= 0) { "Wave elapsed ticks must be non-negative." }
        require(waveSpawnIntervalTicks >= 1) { "Wave spawn interval must be at least one tick." }
        require(towerRangeTicks >= 0) { "Tower range must be non-negative." }
        require(baseLeakDamage >= 0) { "Base leak damage must be non-negative." }
        require(enemies.size <= waveSpawnedCount) {
            "Living enemies cannot exceed the number of spawned enemies."
        }
        require(slots.map { it.id }.toSet().size == slots.size) {
            "Build slot ids must be unique."
        }
        require(enemies.map { it.id }.toSet().size == enemies.size) {
            "Enemy entity ids must be unique."
        }
        when (terminalResult) {
            PlayableBattleTerminal.VICTORY -> {
                require(base.health > 0) { "Victory requires a living base." }
                require(waveSpawnedCount == waveSpawnCount) {
                    "Victory requires every wave enemy to be spawned."
                }
                require(enemies.isEmpty()) { "Victory requires no living enemies." }
            }

            PlayableBattleTerminal.DEFEAT -> require(base.health == 0) {
                "Defeat requires the base health to be zero."
            }

            null -> Unit
        }
    }

    /** Alias used by economy-facing callers that name this resource globally. */
    val globalResource: Int
        get() = resource

    val resourceRemainderTicks: Int
        get() = incomeRemainderTicks

    val buildSlots: List<PlayableBattleSlotState>
        get() = slots

    val configuredTowerId: ContentId
        get() = towerId

    val configuredBuildCost: Int
        get() = buildCost

    val towerBuildCost: Int
        get() = buildCost

    val baseDamage: Int
        get() = towerBaseDamage

    val baseCooldownTicks: Int
        get() = towerBaseCooldownTicks

    val upgradeBaseCost: Int
        get() = towerUpgradeBaseCost

    val upgradeCostStep: Int
        get() = towerUpgradeCostStep

    val damageStep: Int
        get() = towerDamageStep

    val cooldownStep: Int
        get() = towerCooldownStep

    val minCooldownTicks: Int
        get() = towerMinCooldownTicks

    /** Compatibility alias for callers that name the terminal field `terminal`. */
    val terminal: PlayableBattleTerminal?
        get() = terminalResult

    val isTerminal: Boolean
        get() = terminalResult != null

    val pendingEnemiesCount: Int
        get() = waveSpawnCount - waveSpawnedCount

    val pendingEnemyCount: Int
        get() = pendingEnemiesCount

    val livingEnemiesCount: Int
        get() = enemies.size

    val waveTotalCount: Int
        get() = waveSpawnCount

    val spawnedEnemiesCount: Int
        get() = waveSpawnedCount

    val leakDamage: Int
        get() = baseLeakDamage

    val enemyBaseDamage: Int
        get() = baseLeakDamage

    override fun appendHash(hash: StableHash) {
        hash.add("mysd.playable-battle-state.v3")
            .add(stageId.value)
            .add(phase.name)
            .add(terminalResult?.name ?: "none")
            .add(resource)
            .add(resourceCap)
            .add(incomePerSecond)
            .add(incomeRemainderTicks)
            .add(towerId.value)
            .add(buildCost)
            .add(towerBaseDamage)
            .add(towerBaseCooldownTicks)
            .add(towerUpgradeBaseCost)
            .add(towerUpgradeCostStep)
            .add(towerDamageStep)
            .add(towerCooldownStep)
            .add(towerMinCooldownTicks)
            .add(waveId.value)
            .add(enemyFamilyId.value)
            .add(enemyHealth)
            .add(enemySpeedTicks)
            .add(waveSpawnCount)
            .add(waveSpawnedCount)
            .add(waveElapsedTicks)
            .add(waveSpawnIntervalTicks)
            .add(towerRangeTicks)
            .add(baseLeakDamage)
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
            hash.add(slot.towerLevel)
            hash.add(slot.towerDamage != null)
            slot.towerDamage?.let(hash::add)
            hash.add(slot.towerCooldownTicks != null)
            slot.towerCooldownTicks?.let(hash::add)
            hash.add(slot.towerCooldownRemainingTicks)
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

    companion object {
        const val MAX_TOWER_LEVEL: Int = 2
        const val TICKS_PER_SECOND: Int = 20
    }
}

typealias PlayableBaseState = PlayableBattleBaseState
typealias PlayableSlotState = PlayableBattleSlotState
typealias PlayableEnemyState = PlayableBattleEnemyState
