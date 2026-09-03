package dev.mysd.game.battle.playable

import dev.mysd.game.content.OriginalContentFixtures
import dev.mysd.game.content.PlayableLevelContent
import dev.mysd.game.content.ContentId
import dev.mysd.game.simulation.SimulationClock
import kotlin.math.abs
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
    BATTLE_TERMINAL,
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
        waveSpawnedCount: Int? = null,
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
        enemies = enemies.sortedBy(PlayableBattleEnemyState::id),
        towerId = level.tower.id,
        buildCost = level.tower.buildCost,
        towerBaseDamage = level.tower.damage,
        towerBaseCooldownTicks = level.tower.cooldownTicks,
        towerUpgradeBaseCost = level.tower.upgradeBaseCost,
        towerUpgradeCostStep = level.tower.upgradeCostStep,
        towerDamageStep = level.tower.damageStep,
        towerCooldownStep = level.tower.cooldownStep,
        towerMinCooldownTicks = level.tower.minCooldownTicks,
        waveId = level.wave.id,
        enemyFamilyId = level.enemyFamily.id,
        enemyHealth = level.enemyFamily.health,
        enemySpeedTicks = level.enemyFamily.speedTicks,
        waveSpawnCount = level.wave.spawnCount,
        waveSpawnedCount = waveSpawnedCount ?: enemies.size,
        waveElapsedTicks = 0,
        waveSpawnIntervalTicks = level.wave.spawnIntervalTicks,
        towerRangeTicks = level.tower.rangeTicks,
        baseLeakDamage = level.enemyFamily.baseDamage,
    )

    fun createInitialState(
        level: PlayableLevelContent = OriginalContentFixtures.foundationPlayableLevel(),
        initialResource: Int = DEFAULT_INITIAL_RESOURCE,
        resourceCap: Int = DEFAULT_RESOURCE_CAP,
        incomePerSecond: Int = DEFAULT_INCOME_PER_SECOND,
        phase: PlayableBattlePhase = PlayableBattlePhase.ACTIVE,
        enemies: List<PlayableBattleEnemyState> = defaultEnemies(level),
        waveSpawnedCount: Int? = null,
    ): PlayableBattleState = initialState(
        level = level,
        initialResource = initialResource,
        resourceCap = resourceCap,
        incomePerSecond = incomePerSecond,
        phase = phase,
        enemies = enemies,
        waveSpawnedCount = waveSpawnedCount,
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
        if (deltaTicks == 0 || state.phase == PlayableBattlePhase.PAUSED || state.isTerminal) {
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
    fun tick(state: PlayableBattleState): PlayableBattleState = advanceOneTick(state)

    /** Reduces complete ticks only; a paused state is an identity for every delta. */
    fun advance(
        state: PlayableBattleState,
        deltaTicks: Int,
    ): PlayableBattleState {
        require(deltaTicks >= 0) { "Delta ticks must be non-negative." }
        if (deltaTicks == 0 || state.phase == PlayableBattlePhase.PAUSED || state.isTerminal) {
            return state
        }

        var nextState = state
        var remainingTicks = deltaTicks
        while (remainingTicks > 0 && !nextState.isTerminal) {
            nextState = advanceOneTick(nextState)
            remainingTicks -= 1
        }
        return nextState
    }

    fun pause(state: PlayableBattleState): PlayableBattleState =
        if (state.phase == PlayableBattlePhase.PAUSED || state.isTerminal) {
            state
        } else {
            state.copy(phase = PlayableBattlePhase.PAUSED)
        }

    fun resume(state: PlayableBattleState): PlayableBattleState =
        if (state.phase == PlayableBattlePhase.ACTIVE || state.isTerminal) {
            state
        } else {
            state.copy(phase = PlayableBattlePhase.ACTIVE)
        }

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
            state.isTerminal -> PlayableBattleSpendRejection.BATTLE_TERMINAL
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
            state.isTerminal -> PlayableBattleSpendRejection.BATTLE_TERMINAL
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
            accepted = state.phase == PlayableBattlePhase.ACTIVE && !state.isTerminal,
            state = pause(state),
            targetSlotId = null,
        )

        PlayableBattleCommand.Resume -> PlayableBattleSpendResult(
            accepted = state.phase == PlayableBattlePhase.PAUSED && !state.isTerminal,
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

    /**
     * Applies one active simulation tick in a fixed order:
     * commands are reduced by the session before this method, then spawning, movement, tower
     * contact/base leaks, tower attacks, and finally terminal resolution happen here.
     *
     * The first enemy is present at tick zero. Subsequent enemies spawn when the elapsed wave
     * tick reaches each configured interval. A spawned enemy also moves during its spawn tick.
     */
    private fun advanceOneTick(state: PlayableBattleState): PlayableBattleState {
        if (state.phase == PlayableBattlePhase.PAUSED || state.isTerminal) {
            return state
        }

        val resourceState = accumulateResource(state, 1)
        val elapsedTicks = Math.addExact(resourceState.waveElapsedTicks, 1)
        var spawnedCount = resourceState.waveSpawnedCount
        val spawnedEnemies = resourceState.enemies.toMutableList()
        while (spawnedCount < resourceState.waveSpawnCount &&
            elapsedTicks >= Math.multiplyExact(
                spawnedCount.toLong(),
                resourceState.waveSpawnIntervalTicks.toLong(),
            )
        ) {
            spawnedEnemies += PlayableBattleEnemyState(
                id = "${resourceState.enemyFamilyId.value}-$spawnedCount",
                familyId = resourceState.enemyFamilyId,
                health = resourceState.enemyHealth,
                positionTicks = 0,
                speedTicks = resourceState.enemySpeedTicks,
            )
            spawnedCount += 1
        }

        val enemiesToMove = spawnedEnemies
            .sortedBy(PlayableBattleEnemyState::id)
            .map { enemy ->
                val displacement = Math.multiplyExact(enemy.speedTicks.toLong(), 1L)
                val nextPosition = Math.toIntExact(
                    Math.addExact(enemy.positionTicks.toLong(), displacement),
                )
                MovedEnemy(
                    previousPositionTicks = enemy.positionTicks,
                    state = enemy.copy(positionTicks = nextPosition),
                )
            }

        val slotsAfterContact = destroyContactedTowers(
            slots = resourceState.slots,
            movedEnemies = enemiesToMove,
        )
        val baseAndEnemies = resolveBaseLeaks(
            base = resourceState.base,
            movedEnemies = enemiesToMove,
            baseLeakDamage = resourceState.baseLeakDamage,
        )
        val attackResult = resolveTowerAttacks(
            slots = slotsAfterContact,
            enemies = baseAndEnemies.enemies,
            towerRangeTicks = resourceState.towerRangeTicks,
            towerBaseDamage = resourceState.towerBaseDamage,
            towerBaseCooldownTicks = resourceState.towerBaseCooldownTicks,
        )
        val nextBase = baseAndEnemies.base
        val terminal = when {
            nextBase.health <= 0 -> PlayableBattleTerminal.DEFEAT
            attackResult.enemies.isEmpty() && spawnedCount >= resourceState.waveSpawnCount ->
                PlayableBattleTerminal.VICTORY

            else -> null
        }

        return resourceState.copy(
            base = nextBase,
            slots = attackResult.slots,
            enemies = attackResult.enemies.sortedBy(PlayableBattleEnemyState::id),
            waveSpawnedCount = spawnedCount,
            waveElapsedTicks = elapsedTicks,
            terminalResult = terminal,
        )
    }

    private data class MovedEnemy(
        val previousPositionTicks: Int,
        val state: PlayableBattleEnemyState,
    )

    private data class BaseLeakResolution(
        val base: PlayableBattleBaseState,
        val enemies: List<PlayableBattleEnemyState>,
    )

    private data class TowerAttackResolution(
        val slots: List<PlayableBattleSlotState>,
        val enemies: List<PlayableBattleEnemyState>,
    )

    private fun destroyContactedTowers(
        slots: List<PlayableBattleSlotState>,
        movedEnemies: List<MovedEnemy>,
    ): List<PlayableBattleSlotState> {
        val contactedSlotIds = slots
            .asSequence()
            .filter { it.towerId != null }
            .filter { slot ->
                movedEnemies.any { enemy ->
                    enemy.previousPositionTicks <= slot.positionTicks &&
                        enemy.state.positionTicks >= slot.positionTicks
                }
            }
            .map { it.id }
            .toSet()
        return slots.map { slot ->
            if (slot.id in contactedSlotIds) {
                slot.copy(
                    towerId = null,
                    towerLevel = 0,
                    towerDamage = null,
                    towerCooldownTicks = null,
                    towerCooldownRemainingTicks = 0,
                )
            } else {
                slot
            }
        }
    }

    private fun resolveBaseLeaks(
        base: PlayableBattleBaseState,
        movedEnemies: List<MovedEnemy>,
        baseLeakDamage: Int,
    ): BaseLeakResolution {
        val remainingEnemies = movedEnemies
            .filterNot { it.state.positionTicks >= base.positionTicks }
            .map(MovedEnemy::state)
        val leakCount = movedEnemies.count { it.state.positionTicks >= base.positionTicks }
        val damage = Math.multiplyExact(leakCount.toLong(), baseLeakDamage.toLong())
        val nextHealth = Math.max(0L, base.health.toLong() - damage).toInt()
        return BaseLeakResolution(
            base = if (leakCount == 0) base else base.copy(health = nextHealth),
            enemies = remainingEnemies,
        )
    }

    private fun resolveTowerAttacks(
        slots: List<PlayableBattleSlotState>,
        enemies: List<PlayableBattleEnemyState>,
        towerRangeTicks: Int,
        towerBaseDamage: Int,
        towerBaseCooldownTicks: Int,
    ): TowerAttackResolution {
        val nextSlots = slots.toMutableList()
        val nextEnemies = enemies.toMutableList()
        slots.indices.sortedBy { slots[it].id.value }.forEach { slotIndex ->
            val slot = nextSlots[slotIndex]
            val towerId = slot.towerId ?: return@forEach
            val cooldownRemaining = maxOf(0, slot.towerCooldownRemainingTicks - 1)
            val damage = slot.towerDamage ?: towerBaseDamage
            val cooldownTicks = slot.towerCooldownTicks ?: towerBaseCooldownTicks
            val target = nextEnemies
                .asSequence()
                .filter { enemy -> abs(enemy.positionTicks - slot.positionTicks) <= towerRangeTicks }
                .minWithOrNull(
                    compareBy<PlayableBattleEnemyState> {
                        abs(it.positionTicks - slot.positionTicks)
                    }.thenBy(PlayableBattleEnemyState::id),
                )
            if (cooldownRemaining == 0 && target != null) {
                val targetIndex = nextEnemies.indexOfFirst { it.id == target.id }
                val damaged = target.copy(health = maxOf(0, target.health - damage))
                if (damaged.health == 0) {
                    nextEnemies.removeAt(targetIndex)
                } else {
                    nextEnemies[targetIndex] = damaged
                }
                nextSlots[slotIndex] = slot.copy(
                    towerId = towerId,
                    towerCooldownRemainingTicks = cooldownTicks,
                )
            } else {
                nextSlots[slotIndex] = slot.copy(
                    towerId = towerId,
                    towerCooldownRemainingTicks = cooldownRemaining,
                )
            }
        }
        return TowerAttackResolution(
            slots = nextSlots,
            enemies = nextEnemies,
        )
    }

    private fun defaultEnemies(level: PlayableLevelContent): List<PlayableBattleEnemyState> =
        listOf(
            PlayableBattleEnemyState(
                id = "${level.enemyFamily.id.value}-0",
                familyId = level.enemyFamily.id,
                health = level.enemyFamily.health,
                positionTicks = 0,
                speedTicks = level.enemyFamily.speedTicks,
            ),
        )
}
