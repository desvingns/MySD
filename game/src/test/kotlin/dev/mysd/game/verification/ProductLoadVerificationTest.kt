package dev.mysd.game.verification

import dev.myengine.core.stableHashOf
import dev.mysd.game.product.MySdBattlePhase
import dev.mysd.game.product.MySdBattleSpeed
import dev.mysd.game.product.content.OriginalProductCatalog
import dev.mysd.game.product.runtime.ProductAllyState
import dev.mysd.game.product.runtime.ProductBattleController
import dev.mysd.game.product.runtime.ProductBattleLaunch
import dev.mysd.game.product.runtime.ProductBattleReducer
import dev.mysd.game.product.runtime.ProductBattleState
import dev.mysd.game.product.runtime.ProductEnemyState
import dev.mysd.game.product.runtime.ProductTowerSlotState
import java.nio.file.Files
import java.nio.file.Path
import kotlin.math.ceil
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Synthetic release-load gate, not reference evidence or an Android frame benchmark.
 * Waves cannot overlap, so all enemies in the largest catalog wave plus the ally cap is a
 * conservative upper bound on live mobile entities. We exercise that bound plus 25%, every
 * enemy/ally/tower role, and simultaneous ready-to-fire cooldowns. Fixture construction is
 * outside the timer; the timed operation is exactly one authoritative reducer tick.
 */
class ProductLoadVerificationTest {
    private val catalog = OriginalProductCatalog.releaseOne()
    private val stage = catalog.orderedStages.last()
    private val maximumWaveEnemies = catalog.stages.values.maxOf { candidate ->
        candidate.waves.maxOf { wave -> wave.groups.sumOf { it.count } }
    }
    private val peakBound = maximumWaveEnemies + ProductBattleController.MAX_ALLIES
    private val stressedCount = ceil(peakBound * 1.25).toInt()

    @Test
    fun contentPeakPlusTwentyFivePercentRemainsDeterministicWithinTickBudget() {
        val reducer = ProductBattleReducer(catalog)
        val canonical = fixture()
        reducer.advance(canonical, emptyList())
        val expectedDigest = digest(canonical)
        repeat(96) { reducer.advance(fixture(), emptyList()) }

        val samples = LongArray(256) {
            val state = fixture()
            assertEquals(stressedCount, state.enemies.size + state.allies.size)
            val started = System.nanoTime()
            reducer.advance(state, emptyList())
            val elapsed = System.nanoTime() - started
            assertEquals(expectedDigest, digest(state), "Repeated full-load tick changed state")
            assertTrue(state.baseHealth > 0)
            assertTrue(state.enemies.all { it.health <= it.maxHealth })
            assertTrue(state.allies.all { it.health <= it.maxHealth })
            elapsed
        }.sorted()
        val p95 = samples[ceil(samples.size * 0.95).toInt() - 1]
        val median = samples[samples.size / 2]
        val report = """
            {
              "scenario": "release-content-conservative-peak-plus-25-percent",
              "measurement": "JVM reducer tick; not Android frame time",
              "contentVersion": ${catalog.contentVersion},
              "maximumWaveEnemies": $maximumWaveEnemies,
              "allyCap": ${ProductBattleController.MAX_ALLIES},
              "conservativeMobileEntityPeak": $peakBound,
              "stressMobileEntities": $stressedCount,
              "activeTowerCount": ${stage.buildSlots.size},
              "warmupSamples": 96,
              "measuredSamples": ${samples.size},
              "medianNanoseconds": $median,
              "p95Nanoseconds": $p95,
              "tickBudgetNanoseconds": 50000000,
              "deterministicDigest": "$expectedDigest"
            }
        """.trimIndent()
        val reportPath = Path.of("build/reports/product-load.json")
        Files.createDirectories(reportPath.parent)
        Files.writeString(reportPath, report)
        println(report)
        assertTrue(p95 < 50_000_000L, "20 Hz reducer budget exceeded: p95=${p95}ns")
    }

    private fun fixture(): ProductBattleState {
        val allyCount = ProductBattleController.MAX_ALLIES
        val enemyCount = stressedCount - allyCount
        val enemyTypes = catalog.enemies.values.toList()
        val allyTypes = catalog.allies.values.toList()
        val launch = ProductBattleLaunch(
            stageId = stage.id,
            loadoutIds = stage.towerIds + stage.allyIds,
            heroSkillIds = stage.heroSkillIds,
            rosterLevels = (catalog.towers.keys + catalog.allies.keys + catalog.heroSkills.keys)
                .associateWith { 1 },
            towerPowerPermille = 0,
            allyPowerPermille = 0,
            economyPermille = 0,
            heroPowerPermille = 0,
        )
        return ProductBattleState(
            seed = 940_025L,
            runId = "synthetic-load-25",
            launch = launch,
            tick = 1L,
            phase = MySdBattlePhase.COMBAT,
            paused = false,
            speed = MySdBattleSpeed.ONE_X,
            waveIndex = 9,
            waveElapsedTicks = 500,
            spawnedByGroup = stage.waves.last().groups.map { it.count }.toMutableList(),
            resource = stage.resourceCap / 2,
            incomeRemainder = 0,
            baseHealth = stage.baseHealth,
            slots = stage.buildSlots.mapIndexed { index, slot ->
                ProductTowerSlotState(slot.id, slot.positionTicks, stage.towerIds[index % stage.towerIds.size], 4, 0)
            }.toMutableList(),
            allies = MutableList(allyCount) { index ->
                val type = allyTypes[index % allyTypes.size]
                ProductAllyState(index + 1L, type.id, type.health, type.health, 390 + index * 2)
            },
            enemies = MutableList(enemyCount) { index ->
                val type = enemyTypes[index % enemyTypes.size]
                val health = type.health
                ProductEnemyState(allyCount + index + 1L, type.id, health, health, 360 + index % 40)
            },
            skillCooldowns = stage.heroSkillIds.associateWith { 0 }.toMutableMap(),
            enhancements = mutableListOf(),
            enhancementOffers = mutableListOf(),
            rerollsRemaining = 1,
            nextEntityId = stressedCount + 1L,
            nextCommandId = 1L,
            rngState = 42L,
            terminalResult = null,
        )
    }

    private fun digest(state: ProductBattleState): String = stableHashOf {
        add(state.tick).add(state.phase.name).add(state.paused).add(state.speed.name)
        add(state.waveIndex).add(state.waveElapsedTicks).add(state.baseHealth)
        add(state.resource).add(state.incomeRemainder).add(state.rngState)
        add(state.nextEntityId).add(state.nextCommandId).add(state.terminalResult?.name ?: "")
        state.spawnedByGroup.forEach { add(it) }
        state.slots.forEach { add(it.id.value).add(it.level).add(it.cooldownRemainingTicks) }
        state.allies.forEach {
            add(it.entityId).add(it.health).add(it.maxHealth).add(it.positionTicks).add(it.cooldownRemainingTicks)
        }
        state.enemies.forEach {
            add(it.entityId).add(it.health).add(it.maxHealth).add(it.positionTicks)
                .add(it.cooldownRemainingTicks).add(it.slowRemainingTicks).add(it.slowPermille)
        }
        state.skillCooldowns.toSortedMap(compareBy { it.value }).forEach { (id, ticks) -> add(id.value).add(ticks) }
        state.enhancements.forEach { add(it.value) }
        state.enhancementOffers.forEach { add(it.value) }
        add(state.rerollsRemaining)
    }
}
