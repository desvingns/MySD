package dev.mysd.game.product

import dev.mysd.game.product.content.OriginalProductCatalog
import dev.mysd.game.product.content.ProductAllyRole
import dev.mysd.game.product.content.ProductEnemyRole
import dev.mysd.game.product.content.ProductEnhancementKind
import dev.mysd.game.product.content.ProductTowerRole
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ProductCatalogTest {
    private val catalog = OriginalProductCatalog.releaseOne()

    @Test
    fun releaseOneContainsTheCompleteOriginalProductMatrix() {
        assertEquals(6, catalog.stages.size)
        assertTrue(catalog.stages.values.all { it.waves.size == 10 })
        assertEquals(ProductTowerRole.entries.toSet(), catalog.towers.values.map { it.role }.toSet())
        assertEquals(ProductAllyRole.entries.toSet(), catalog.allies.values.map { it.role }.toSet())
        assertEquals(6, catalog.enemies.values.count { !it.boss })
        assertEquals(2, catalog.enemies.values.count { it.role == ProductEnemyRole.BOSS && it.boss })
        assertEquals(2, catalog.heroSkills.size)
        assertEquals(ProductEnhancementKind.entries.toSet(), catalog.enhancements.values.map { it.kind }.toSet())
        assertEquals(12, catalog.techNodes.size)
        assertEquals(15, catalog.rewardTiers.size)
        assertEquals(4, catalog.shopOffers.size)
    }

    @Test
    fun everyStageUsesTenWavesAndFourDeterministicEnhancementBreaks() {
        catalog.orderedStages.forEach { stage ->
            assertEquals((1..10).toList(), stage.waves.indices.map { it + 1 })
            assertEquals(setOf(2, 4, 6, 8), stage.enhancementAfterWaves)
            assertEquals(4, stage.buildSlots.size)
            assertTrue(stage.waves[4].groups.any { catalog.enemies.getValue(it.enemyId).boss })
            assertTrue(stage.waves[9].groups.any { catalog.enemies.getValue(it.enemyId).boss })
        }
    }

    @Test
    fun technologyGraphIsReachableAndAcyclicByConstruction() {
        val unlocked = mutableSetOf<dev.mysd.game.content.ContentId>()
        while (unlocked.size < catalog.techNodes.size) {
            val next = catalog.techNodes.values
                .filter { it.id !in unlocked && unlocked.containsAll(it.prerequisites) }
            assertTrue(next.isNotEmpty())
            unlocked += next.map { it.id }
        }
        assertEquals(catalog.techNodes.keys, unlocked)
    }

    @Test
    fun unlockCoordinatesAlwaysLeaveAPlayableStartingRoster() {
        val startingTowers = catalog.towers.values.filter { it.unlockAfterStageOrdinal == 0 }
        val startingAllies = catalog.allies.values.filter { it.unlockAfterStageOrdinal == 0 }
        val startingHeroes = catalog.heroSkills.values.filter { it.unlockAfterStageOrdinal == 0 }
        assertTrue(startingTowers.isNotEmpty())
        assertTrue(startingAllies.isNotEmpty())
        assertTrue(startingHeroes.isNotEmpty())
        assertTrue((catalog.towers.values + catalog.allies.values + catalog.heroSkills.values)
            .all { definition ->
                when (definition) {
                    is dev.mysd.game.product.content.ProductTowerDefinition -> definition.unlockAfterStageOrdinal in 0..5
                    is dev.mysd.game.product.content.ProductAllyDefinition -> definition.unlockAfterStageOrdinal in 0..5
                    is dev.mysd.game.product.content.ProductHeroSkillDefinition -> definition.unlockAfterStageOrdinal in 0..5
                    else -> false
                }
            })
    }

    @Test
    fun invalidNumericSlotsSchedulesAndTowerProgressionAreRejectedAtContentBoundary() {
        val stage = catalog.orderedStages.first()
        val tower = catalog.towers.getValue(OriginalProductCatalog.TOWER_RAPID)
        listOf(
            stage.copy(buildSlots = stage.buildSlots.mapIndexed { index, slot ->
                if (index == 0) slot.copy(positionTicks = -1) else slot
            }),
            stage.copy(incomePerSecond = Int.MAX_VALUE),
            stage.copy(energyCost = 11),
            stage.copy(waves = stage.waves.mapIndexed { index, wave ->
                if (index != 0) wave else wave.copy(groups = wave.groups.map { it.copy(intervalTicks = Int.MAX_VALUE) })
            }),
        ).forEach { invalid ->
            assertFailsWith<IllegalArgumentException> { catalog.copy(stages = catalog.stages + (stage.id to invalid)) }
        }
        listOf(tower.copy(damagePerLevel = -1), tower.copy(rangeTicks = 0),
            tower.copy(minimumCooldownTicks = tower.cooldownTicks + 1),
            tower.copy(damage = Int.MAX_VALUE)).forEach { invalid ->
            assertFailsWith<IllegalArgumentException> { catalog.copy(towers = catalog.towers + (tower.id to invalid)) }
        }
    }
}
