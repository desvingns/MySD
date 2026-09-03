package dev.mysd.game.battle.playable

import dev.mysd.game.content.OriginalContentFixtures
import dev.mysd.game.content.PlayableLevelContent
import dev.mysd.game.simulation.ReplayVerification
import dev.mysd.game.simulation.SimulationSession
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class PlayableBattleCombatTest {
    @Test
    fun finiteWaveSpawnsOneEnemyAtStartAndThenAtConfiguredIntervals() {
        val initial = PlayableBattleEngine.initialState(incomePerSecond = 0)

        assertEquals(1, initial.enemies.size)
        assertEquals(1, initial.waveSpawnedCount)
        assertEquals(8, initial.pendingEnemiesCount)
        assertEquals(listOf("enemy-ash-sprout-0"), initial.enemies.map { it.id })

        val beforeSecondSpawn = PlayableBattleEngine.advance(initial, 19)
        assertEquals(1, beforeSecondSpawn.enemies.size)
        assertEquals(38, beforeSecondSpawn.enemies.single().positionTicks)
        assertEquals(19, beforeSecondSpawn.waveElapsedTicks)

        val atSecondSpawn = PlayableBattleEngine.advance(initial, 20)
        assertEquals(2, atSecondSpawn.enemies.size)
        assertEquals(
            listOf("enemy-ash-sprout-0", "enemy-ash-sprout-1"),
            atSecondSpawn.enemies.map { it.id },
        )
        assertEquals(listOf(40, 2), atSecondSpawn.enemies.map { it.positionTicks })
        assertEquals(2, atSecondSpawn.waveSpawnedCount)
        assertEquals(7, atSecondSpawn.pendingEnemiesCount)
    }

    @Test
    fun towerTargetsNearestEnemyAndUsesStableIdForEqualDistance() {
        val level = combatLevel(
            waveCount = 8,
            towerDamage = 1,
            towerCooldownTicks = 10,
            towerRangeTicks = 100,
            enemyHealth = 3,
            enemySpeedTicks = 0,
        )
        val initial = PlayableBattleEngine.initialState(
            level = level,
            initialResource = 100,
            incomePerSecond = 0,
            enemies = listOf(
                enemy(level, "enemy-z", positionTicks = 40),
                enemy(level, "enemy-a", positionTicks = 20),
            ),
            waveSpawnedCount = 8,
        )
        val built = PlayableBattleEngine.buildTower(initial, initial.slots.first().id).state

        val afterHit = PlayableBattleEngine.tick(built)

        assertEquals(2, afterHit.enemies.single { it.id == "enemy-a" }.health)
        assertEquals(3, afterHit.enemies.single { it.id == "enemy-z" }.health)
        assertEquals(10, afterHit.slots.first().towerCooldownRemainingTicks)
    }

    @Test
    fun towerCooldownCountsIntegerTicksBetweenAutomaticHits() {
        val level = combatLevel(
            waveCount = 8,
            towerDamage = 1,
            towerCooldownTicks = 2,
            towerRangeTicks = 35,
            enemyHealth = 4,
            enemySpeedTicks = 0,
        )
        val initial = PlayableBattleEngine.initialState(
            level = level,
            initialResource = 100,
            incomePerSecond = 0,
            enemies = listOf(enemy(level, "enemy-a", positionTicks = 20)),
            waveSpawnedCount = 8,
        )
        val built = PlayableBattleEngine.buildTower(initial, initial.slots.first().id).state

        val first = PlayableBattleEngine.tick(built)
        val second = PlayableBattleEngine.tick(first)
        val third = PlayableBattleEngine.tick(second)

        assertEquals(3, first.enemies.single().health)
        assertEquals(3, second.enemies.single().health)
        assertEquals(2, third.enemies.single().health)
        assertEquals(1, second.slots.first().towerCooldownRemainingTicks)
        assertEquals(2, third.slots.first().towerCooldownRemainingTicks)
    }

    @Test
    fun reachingOccupiedTowerDestroysOnlyTheTowerAndLeavesEnemyAlive() {
        val level = combatLevel(
            waveCount = 8,
            towerDamage = 8,
            towerCooldownTicks = 1,
            towerRangeTicks = 0,
            enemyHealth = 8,
            enemySpeedTicks = 2,
        )
        val initial = PlayableBattleEngine.initialState(
            level = level,
            initialResource = 100,
            incomePerSecond = 0,
            enemies = listOf(enemy(level, "enemy-a", positionTicks = 28)),
            waveSpawnedCount = 8,
        )
        val built = PlayableBattleEngine.buildTower(initial, initial.slots.first().id).state

        val afterContact = PlayableBattleEngine.tick(built)

        assertTrue(afterContact.slots.first().isEmpty)
        assertEquals(1, afterContact.enemies.size)
        assertEquals(8, afterContact.enemies.single().health)
        assertEquals(30, afterContact.enemies.single().positionTicks)
        assertEquals(null, afterContact.terminalResult)
    }

    @Test
    fun enemyReachingBaseAppliesOneLeakAndDefeatWinsSameTickTie() {
        val level = combatLevel(
            waveCount = 8,
            baseHealth = 12,
            baseDamage = 12,
            enemyHealth = 8,
            enemySpeedTicks = 1,
        )
        val initial = PlayableBattleEngine.initialState(
            level = level,
            incomePerSecond = 0,
            enemies = listOf(enemy(level, "enemy-a", positionTicks = 119)),
            waveSpawnedCount = 8,
        )

        val defeated = PlayableBattleEngine.tick(initial)

        assertEquals(0, defeated.base.health)
        assertTrue(defeated.enemies.isEmpty())
        assertEquals(PlayableBattleTerminal.DEFEAT, defeated.terminalResult)
        assertEquals(PlayableBattleTerminal.DEFEAT, defeated.terminal)
    }

    @Test
    fun victoryRequiresTheLastLivingEnemyAndNoPendingSpawns() {
        val level = combatLevel(
            waveCount = 8,
            towerDamage = 8,
            towerCooldownTicks = 1,
            towerRangeTicks = 100,
            enemyHealth = 8,
            enemySpeedTicks = 0,
        )
        val initial = PlayableBattleEngine.initialState(
            level = level,
            initialResource = 100,
            incomePerSecond = 0,
            enemies = (0 until 8).map { index -> enemy(level, "enemy-$index", positionTicks = 0) },
            waveSpawnedCount = 8,
        )
        val built = PlayableBattleEngine.buildTower(initial, initial.slots.first().id).state

        val victory = PlayableBattleEngine.advance(built, 8)

        assertEquals(PlayableBattleTerminal.VICTORY, victory.terminalResult)
        assertTrue(victory.enemies.isEmpty())
        assertEquals(8, victory.waveSpawnedCount)
        assertEquals(0, victory.pendingEnemiesCount)
        assertTrue(victory.base.health > 0)
    }

    @Test
    fun terminalStateRejectsCommandsAndFreezesTheWholeSession() {
        val level = combatLevel(
            waveCount = 8,
            towerDamage = 8,
            towerCooldownTicks = 1,
            towerRangeTicks = 100,
            enemyHealth = 8,
            enemySpeedTicks = 0,
        )
        val initial = PlayableBattleEngine.initialState(
            level = level,
            initialResource = 100,
            incomePerSecond = 0,
            enemies = listOf(enemy(level, "enemy-a", positionTicks = 0)),
            waveSpawnedCount = 8,
        )
        val built = PlayableBattleEngine.buildTower(initial, initial.slots.first().id).state
        val victory = PlayableBattleEngine.tick(built)
        assertEquals(PlayableBattleTerminal.VICTORY, victory.terminalResult)

        assertSame(victory, PlayableBattleEngine.advance(victory, 100))
        assertSame(victory, PlayableBattleEngine.pause(victory))
        assertSame(victory, PlayableBattleEngine.resume(victory))
        assertEquals(
            PlayableBattleSpendRejection.BATTLE_TERMINAL,
            PlayableBattleEngine.buildTower(victory, victory.slots[1].id).rejection,
        )
        assertEquals(
            PlayableBattleSpendRejection.BATTLE_TERMINAL,
            PlayableBattleEngine.upgradeTower(victory, victory.slots.first().id).rejection,
        )
        assertFalse(PlayableBattleEngine.reduce(victory, PlayableBattleCommand.Pause).accepted)

        val session = SimulationSession.playableBattle(seed = 42L, initialState = victory)
        val before = session.snapshot()
        assertTrue(session.advance(5_000).isEmpty())
        assertEquals(before, session.snapshot())
        assertEquals(before, session.buildTower(victory.slots[1].id))
    }

    @Test
    fun sameSeedAndCommandsProduceIdenticalTerminalReplayTrajectory() {
        val level = combatLevel(
            waveCount = 8,
            towerDamage = 8,
            towerCooldownTicks = 1,
            towerRangeTicks = 100,
            enemyHealth = 8,
            enemySpeedTicks = 0,
        )
        val initial = PlayableBattleEngine.initialState(
            level = level,
            initialResource = 100,
            incomePerSecond = 0,
            enemies = (0 until 8).map { index -> enemy(level, "enemy-$index", positionTicks = 0) },
            waveSpawnedCount = 8,
        )
        val first = SimulationSession.playableBattle(seed = 99L, initialState = initial)
        val second = SimulationSession.playableBattle(seed = 99L, initialState = initial)
        first.buildTower(initial.slots.first().id)
        second.buildTower(initial.slots.first().id)

        val firstTrajectory = first.advance(5_000)
        val secondTrajectory = second.advance(5_000)

        assertEquals(firstTrajectory, secondTrajectory)
        assertTrue(ReplayVerification.compare(firstTrajectory, secondTrajectory).passed)
        assertEquals(PlayableBattleTerminal.VICTORY, first.state().terminalResult)
        assertEquals(first.snapshot(), second.snapshot())
        assertEquals(8, firstTrajectory.size)
    }

    private fun combatLevel(
        waveCount: Int,
        baseHealth: Int = 120,
        baseDamage: Int = 12,
        towerDamage: Int = 3,
        towerCooldownTicks: Int = 10,
        towerRangeTicks: Int = 35,
        enemyHealth: Int = 8,
        enemySpeedTicks: Int = 2,
    ): PlayableLevelContent {
        val foundation = OriginalContentFixtures.foundationPlayableLevel()
        return foundation.copy(
            base = foundation.base.copy(health = baseHealth),
            tower = foundation.tower.copy(
                damage = towerDamage,
                cooldownTicks = towerCooldownTicks,
                rangeTicks = towerRangeTicks,
            ),
            enemyFamily = foundation.enemyFamily.copy(
                health = enemyHealth,
                speedTicks = enemySpeedTicks,
                baseDamage = baseDamage,
            ),
            wave = foundation.wave.copy(spawnCount = waveCount),
        )
    }

    private fun enemy(
        level: PlayableLevelContent,
        id: String,
        positionTicks: Int,
    ): PlayableBattleEnemyState = PlayableBattleEnemyState(
        id = id,
        familyId = level.enemyFamily.id,
        health = level.enemyFamily.health,
        positionTicks = positionTicks,
        speedTicks = level.enemyFamily.speedTicks,
    )
}
