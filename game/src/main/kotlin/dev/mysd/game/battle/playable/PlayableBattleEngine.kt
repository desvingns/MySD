package dev.mysd.game.battle.playable

import dev.mysd.game.content.OriginalContentFixtures
import dev.mysd.game.content.PlayableLevelContent
import dev.mysd.game.content.ContentId
import dev.mysd.game.simulation.SimulationClock
import kotlin.math.min

/** Pure result of the integer-only passive-income calculation. */
data class PassiveResourceAccumulation(
    val resource: Int,
    val gain: Long,
    val remainderTicks: Int,
) {
    val nextResource: Int
        get() = resource

    val nextRemainderTicks: Int
        get() = remainderTicks
}

enum class PlayableBattleSpendRejection {
    INSUFFICIENT_RESOURCE,
    UNKNOWN_SLOT,
    TARGET_SLOT_OCCUPIED,
    TARGET_SLOT_EMPTY,
    TOWER_MAX_LEVEL,
    BATTLE_PAUSED,
}

/** Result boundary for an atomic resource mutation. */
data class PlayableBattleSpendResult(
    val accepted: Boolean,
    val state: PlayableBattleState,
    val targetSlotId: dev.mysd.game.content.ContentId?,
    val rejection: PlayableBattleSpendRejection? = null,
) {
    val resource: Int
        get() = state.resource

    val successful: Boolean
        get() = accepted
}

/** Deterministic values produced by one accepted tower upgrade. */
data class PlayableBattleTowerUpgrade(
    val currentLevel: Int,
    val cost: Int,
    val nextLevel: Int,
    val nextDamage: Int,
    val nextCooldownTicks: Int,
)

/** Pure fixed-step reducer for the first playable battle. */
object PlayableBattleEngine {
    const val TICKS_PER_SECOND: Int = SimulationClock.TICK_RATE_HZ
    const val MAX_TOWER_LEVEL: Int = PlayableBattleState.MAX_TOWER_LEVEL
    const val DEFAULT_INITIAL_RESOURCE: Int = 50
    const val DEFAULT_RESOURCE_CAP: Int = 100
    const val DEFAULT_INCOME_PER_SECOND: Int = 10

    /** Creates the original first-level runtime state without any Android or wall-clock input. */
    fun initialState(
        level: PlayableLevelContent = OriginalContentFixtures.foundationPlayableLevel(),
        initialResource: Int = DEFAULT_INITIAL_RESOURCE,
        resourceCap: Int = DEFAULT_RESOURCE_CAP,
        incomePerSecond: Int = DEFAULT_INCOME_PER_SECOND,
        phase: PlayableBattlePhase = PlayableBattlePhase.ACTIVE,
        enemies: List<PlayableBattleEnemyState> = defaultEnemies(level),
    ): PlayableBattleState = PlayableBattleState(
        stageId = level.stageId,
        phase = phase,
        base = PlayableBattleBaseState(
            id = level.base.id,
            health = level.base.health,
            maxHealth = level.base.health,
            positionTicks = level.base.positionTicks,
        ),
        resource = initialResource,
        resourceCap = resourceCap,
        incomePerSecond = incomePerSecond,
        incomeRemainderTicks = 0,
        slots = level.buildSlots.map { slot ->
            PlayableBattleSlotState(
                id = slot.id,
                positionTicks = slot.positionTicks,
            )
        },
        enemies = enemies,
        towerId = level.tower.id,
        buildCost = level.tower.buildCost,
        towerBaseDamage = level.tower.damage,
        towerBaseCooldownTicks = level.tower.cooldownTicks,
        towerUpgradeBaseCost = level.tower.upgradeBaseCost,
        towerUpgradeCostStep = level.tower.upgradeCostStep,
        towerDamageStep = level.tower.damageStep,
        towerCooldownStep = level.tower.cooldownStep,
        towerMinCooldownTicks = level.tower.minCooldownTicks,
    )

    fun createInitialState(
        level: PlayableLevelContent = OriginalContentFixtures.foundationPlayableLevel(),
        initialResource: Int = DEFAULT_INITIAL_RESOURCE,
        resourceCap: Int = DEFAULT_RESOURCE_CAP,
        incomePerSecond: Int = DEFAULT_INCOME_PER_SECOND,
        phase: PlayableBattlePhase = PlayableBattlePhase.ACTIVE,
        enemies: List<PlayableBattleEnemyState> = defaultEnemies(level),
    ): PlayableBattleState = initialState(
        level = level,
        initialResource = initialResource,
        resourceCap = resourceCap,
        incomePerSecond = incomePerSecond,
        phase = phase,
        enemies = enemies,
    )

    /**
     * Calculates passive income using only integer arithmetic:
     * raw = incomePerSecond * deltaTicks + incomeRemainderTicks.
     */
    fun calculatePassiveIncome(
        currentResource: Int,
        incomePerSecond: Int,
        deltaTicks: Int,
        incomeRemainderTicks: Int,
        resourceCap: Int,
    ): PassiveResourceAccumulation {
        require(resourceCap >= 0) { "Resource cap must be non-negative." }
        require(currentResource in 0..resourceCap) { "Resource must be within its cap." }
        require(incomePerSecond >= 0) { "Income per second must be non-negative." }
        require(deltaTicks >= 0) { "Delta ticks must be non-negative." }
        require(incomeRemainderTicks in 0 until TICKS_PER_SECOND) {
            "Income remainder must be within one fixed second."
        }

        val raw = incomePerSecond.toLong() * deltaTicks.toLong() + incomeRemainderTicks.toLong()
        val gain = raw / TICKS_PER_SECOND
        val nextResource = min(resourceCap.toLong(), currentResource.toLong() + gain).toInt()
        return PassiveResourceAccumulation(
            resource = nextResource,
            gain = gain,
            remainderTicks = (raw % TICKS_PER_SECOND).toInt(),
        )
    }

    fun accumulateResource(
        state: PlayableBattleState,
        deltaTicks: Int,
    ): PlayableBattleState {
        require(deltaTicks >= 0) { "Delta ticks must be non-negative." }
        if (deltaTicks == 0 || state.phase == PlayableBattlePhase.PAUSED) {
            return state
        }

        val accumulation = calculatePassiveIncome(
            currentResource = state.resource,
            incomePerSecond = state.incomePerSecond,
            deltaTicks = deltaTicks,
            incomeRemainderTicks = state.incomeRemainderTicks,
            resourceCap = state.resourceCap,
        )
        return state.copy(
            resource = accumulation.resource,
            incomeRemainderTicks = accumulation.remainderTicks,
        )
    }

    /** Reduces exactly one complete 50 ms tick. */
    fun tick(state: PlayableBattleState): PlayableBattleState = advance(state, 1)

    /** Reduces complete ticks only; a paused state is an identity for every delta. */
    fun advance(
        state: PlayableBattleState,
        deltaTicks: Int,
    ): PlayableBattleState {
        require(deltaTicks >= 0) { "Delta ticks must be non-negative." }
        if (deltaTicks == 0 || state.phase == PlayableBattlePhase.PAUSED) {
            return state
        }

        val resourceState = accumulateResource(state, deltaTicks)
        val advancedEnemies = state.enemies.map { enemy ->
            val displacement = Math.multiplyExact(enemy.speedTicks.toLong(), deltaTicks.toLong())
            val nextPosition = Math.toIntExact(
                Math.addExact(enemy.positionTicks.toLong(), displacement),
            )
            enemy.copy(positionTicks = nextPosition)
        }
        return resourceState.copy(enemies = advancedEnemies)
    }

    fun pause(state: PlayableBattleState): PlayableBattleState =
        if (state.phase == PlayableBattlePhase.PAUSED) state else state.copy(phase = PlayableBattlePhase.PAUSED)

    fun resume(state: PlayableBattleState): PlayableBattleState =
        if (state.phase == PlayableBattlePhase.ACTIVE) state else state.copy(phase = PlayableBattlePhase.ACTIVE)

    /**
     * Attempts one resource mutation as a single pure transition. On rejection the exact input
     * state is returned, so resource and target slot cannot be partially changed.
     */
    fun spend(
        state: PlayableBattleState,
        targetSlotId: dev.mysd.game.content.ContentId?,
        cost: Int,
    ): PlayableBattleSpendResult {
        require(cost >= 0) { "Spend cost must be non-negative." }
        val rejection = when {
            state.phase == PlayableBattlePhase.PAUSED -> PlayableBattleSpendRejection.BATTLE_PAUSED
            targetSlotId != null && state.slots.none { it.id == targetSlotId } ->
                PlayableBattleSpendRejection.UNKNOWN_SLOT
            targetSlotId != null && state.slots.first { it.id == targetSlotId }.towerId != null ->
                PlayableBattleSpendRejection.TARGET_SLOT_OCCUPIED
            cost > state.resource -> PlayableBattleSpendRejection.INSUFFICIENT_RESOURCE
            else -> null
        }

        return if (rejection == null) {
            PlayableBattleSpendResult(
                accepted = true,
                state = state.copy(resource = state.resource - cost),
                targetSlotId = targetSlotId,
            )
        } else {
            PlayableBattleSpendResult(
                accepted = false,
                state = state,
                targetSlotId = targetSlotId,
                rejection = rejection,
            )
        }
    }

    fun trySpend(
        state: PlayableBattleState,
        targetSlotId: ContentId?,
        cost: Int,
    ): PlayableBattleSpendResult = spend(state, targetSlotId, cost)

    /** Places the configured tower only after the complete atomic spend has been accepted. */
    fun buildTower(
        state: PlayableBattleState,
        targetSlotId: ContentId,
    ): PlayableBattleSpendResult {
        val spendResult = spend(
            state = state,
            targetSlotId = targetSlotId,
            cost = state.buildCost,
        )
        if (!spendResult.accepted) {
            return spendResult
        }

        val placedState = spendResult.state.copy(
            slots = spendResult.state.slots.map { slot ->
                if (slot.id == targetSlotId) {
                    slot.copy(
                        towerId = state.towerId,
                        towerLevel = 0,
                        towerDamage = null,
                        towerCooldownTicks = null,
                    )
                } else {
                    slot
                }
            },
        )
        return spendResult.copy(state = placedState)
    }

    fun tryBuildTower(
        state: PlayableBattleState,
        targetSlotId: ContentId,
    ): PlayableBattleSpendResult = buildTower(state, targetSlotId)

    /**
     * Calculates one upgrade from the current level using the level fixture's integer-only
     * formula. Level zero uses the configured base stats; level one applies one further step.
     */
    fun calculateTowerUpgrade(
        state: PlayableBattleState,
        currentLevel: Int,
    ): PlayableBattleTowerUpgrade {
        require(currentLevel in 0 until MAX_TOWER_LEVEL) {
            "Tower upgrade level must be between 0 and ${MAX_TOWER_LEVEL - 1}."
        }

        val cost = Math.toIntExact(
            Math.addExact(
                state.towerUpgradeBaseCost.toLong(),
                Math.multiplyExact(
                    currentLevel.toLong(),
                    state.towerUpgradeCostStep.toLong(),
                ),
            ),
        )
        val nextDamage = Math.toIntExact(
            Math.addExact(
                state.towerBaseDamage.toLong(),
                Math.multiplyExact(currentLevel.toLong(), state.towerDamageStep.toLong()),
            ),
        )
        val steppedCooldown = state.towerBaseCooldownTicks.toLong() -
            Math.multiplyExact(currentLevel.toLong(), state.towerCooldownStep.toLong())
        val nextCooldownTicks = Math.toIntExact(
            maxOf(state.towerMinCooldownTicks.toLong(), steppedCooldown),
        )

        return PlayableBattleTowerUpgrade(
            currentLevel = currentLevel,
            cost = cost,
            nextLevel = currentLevel + 1,
            nextDamage = nextDamage,
            nextCooldownTicks = nextCooldownTicks,
        )
    }

    /** Applies one sequential upgrade, preserving the exact input state on every rejection. */
    fun upgradeTower(
        state: PlayableBattleState,
        targetSlotId: ContentId,
    ): PlayableBattleSpendResult {
        val slot = state.slots.firstOrNull { it.id == targetSlotId }
        val rejection = when {
            state.phase == PlayableBattlePhase.PAUSED -> PlayableBattleSpendRejection.BATTLE_PAUSED
            slot == null -> PlayableBattleSpendRejection.UNKNOWN_SLOT
            slot.towerId == null -> PlayableBattleSpendRejection.TARGET_SLOT_EMPTY
            slot.towerLevel >= MAX_TOWER_LEVEL -> PlayableBattleSpendRejection.TOWER_MAX_LEVEL
            else -> null
        }
        if (rejection != null) {
            return PlayableBattleSpendResult(
                accepted = false,
                state = state,
                targetSlotId = targetSlotId,
                rejection = rejection,
            )
        }

        checkNotNull(slot)
        val upgrade = calculateTowerUpgrade(state, slot.towerLevel)
        if (upgrade.cost > state.resource) {
            return PlayableBattleSpendResult(
                accepted = false,
                state = state,
                targetSlotId = targetSlotId,
                rejection = PlayableBattleSpendRejection.INSUFFICIENT_RESOURCE,
            )
        }

        val upgradedState = state.copy(
            resource = state.resource - upgrade.cost,
            slots = state.slots.map { currentSlot ->
                if (currentSlot.id == targetSlotId) {
                    currentSlot.copy(
                        towerLevel = upgrade.nextLevel,
                        towerDamage = upgrade.nextDamage,
                        towerCooldownTicks = upgrade.nextCooldownTicks,
                    )
                } else {
                    currentSlot
                }
            },
        )
        return PlayableBattleSpendResult(
            accepted = true,
            state = upgradedState,
            targetSlotId = targetSlotId,
        )
    }

    fun tryUpgradeTower(
        state: PlayableBattleState,
        targetSlotId: ContentId,
    ): PlayableBattleSpendResult = upgradeTower(state, targetSlotId)

    fun reduce(
        state: PlayableBattleState,
        command: PlayableBattleCommand,
    ): PlayableBattleSpendResult = when (command) {
        PlayableBattleCommand.Pause -> PlayableBattleSpendResult(
            accepted = state.phase == PlayableBattlePhase.ACTIVE,
            state = pause(state),
            targetSlotId = null,
        )

        PlayableBattleCommand.Resume -> PlayableBattleSpendResult(
            accepted = state.phase == PlayableBattlePhase.PAUSED,
            state = resume(state),
            targetSlotId = null,
        )

        is PlayableBattleCommand.SpendResource -> spend(
            state = state,
            targetSlotId = command.targetSlotId,
            cost = command.cost,
        )

        is PlayableBattleCommand.BuildTower -> buildTower(
            state = state,
            targetSlotId = command.targetSlotId,
        )

        is PlayableBattleCommand.UpgradeTower -> upgradeTower(
            state = state,
            targetSlotId = command.targetSlotId,
        )
    }

    fun reduceState(
        state: PlayableBattleState,
        command: PlayableBattleCommand,
    ): PlayableBattleState = reduce(state, command).state

    private fun defaultEnemies(level: PlayableLevelContent): List<PlayableBattleEnemyState> =
        (0 until level.wave.spawnCount).map { index ->
            PlayableBattleEnemyState(
                id = "${level.enemyFamily.id.value}-$index",
                familyId = level.enemyFamily.id,
                health = level.enemyFamily.health,
                positionTicks = 0,
                speedTicks = level.enemyFamily.speedTicks,
            )
        }
}
