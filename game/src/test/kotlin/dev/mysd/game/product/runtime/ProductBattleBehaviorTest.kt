package dev.mysd.game.product.runtime

import dev.myengine.core.CommandId
import dev.myengine.core.TextCommand
import dev.myengine.core.Tick
import dev.mysd.game.content.ContentId
import dev.mysd.game.product.MySdBattlePhase
import dev.mysd.game.product.MySdBattleSpeed
import dev.mysd.game.product.content.OriginalProductCatalog
import dev.mysd.game.product.content.ProductEnhancementKind
import dev.mysd.game.product.content.ProductEnemyRole
import dev.mysd.game.product.meta.ProductProfileManager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Minimal deterministic system fixtures isolate effects; whole stages are covered separately. */
class ProductBattleBehaviorTest {
    private val catalog = OriginalProductCatalog.releaseOne()
    private val stage = catalog.orderedStages.first()
    private val reducer = ProductBattleReducer(catalog)

    @Test
    fun allTwelveEnhancementsChangeTheirActualReducerOutcome() {
        ProductEnhancementKind.entries.forEach { kind ->
            val plain = fixture()
            val enhanced = fixture().also { it.enhancements += enhancement(kind) }
            when (kind) {
                ProductEnhancementKind.TOWER_DAMAGE,
                ProductEnhancementKind.TOWER_COOLDOWN,
                ProductEnhancementKind.TOWER_RANGE,
                ProductEnhancementKind.SPLASH_RADIUS,
                ProductEnhancementKind.SLOW_STRENGTH -> {
                    val towerId = when (kind) {
                        ProductEnhancementKind.SPLASH_RADIUS -> OriginalProductCatalog.TOWER_BURST
                        ProductEnhancementKind.SLOW_STRENGTH -> OriginalProductCatalog.TOWER_CONTROL
                        else -> OriginalProductCatalog.TOWER_RAPID
                    }
                    listOf(plain, enhanced).forEach { state ->
                        state.slots[0].towerId = towerId
                        state.slots[0].level = 1
                        val position = if (kind == ProductEnhancementKind.TOWER_RANGE) 470 else 210
                        state.enemies += enemy(1, position)
                        if (kind == ProductEnhancementKind.SPLASH_RADIUS) state.enemies += enemy(2, 305)
                        advance(state)
                    }
                    when (kind) {
                        ProductEnhancementKind.TOWER_COOLDOWN -> assertTrue(
                            enhanced.slots[0].cooldownRemainingTicks < plain.slots[0].cooldownRemainingTicks, kind.name)
                        ProductEnhancementKind.SLOW_STRENGTH -> assertTrue(
                            enhanced.enemies[0].slowPermille > plain.enemies[0].slowPermille, kind.name)
                        ProductEnhancementKind.SPLASH_RADIUS -> assertTrue(
                            enhanced.enemies[0].health < plain.enemies[0].health, kind.name)
                        else -> assertTrue(enhanced.enemies[0].health < plain.enemies[0].health, kind.name)
                    }
                }
                ProductEnhancementKind.ALLY_HEALTH -> {
                    listOf(plain, enhanced).forEach { advance(it, "deploy-ally", OriginalProductCatalog.ALLY_DEFENDER.value) }
                    assertTrue(enhanced.allies.single().maxHealth > plain.allies.single().maxHealth, kind.name)
                }
                ProductEnhancementKind.ALLY_DAMAGE,
                ProductEnhancementKind.ALLY_SPEED -> {
                    listOf(plain, enhanced).forEach { state ->
                        state.allies += ally(1, 500)
                        if (kind == ProductEnhancementKind.ALLY_DAMAGE) state.enemies += enemy(2, 480)
                        advance(state)
                    }
                    if (kind == ProductEnhancementKind.ALLY_DAMAGE) {
                        assertTrue(enhanced.enemies.single().health < plain.enemies.single().health, kind.name)
                    } else assertTrue(enhanced.allies.single().positionTicks < plain.allies.single().positionTicks, kind.name)
                }
                ProductEnhancementKind.RESOURCE_INCOME -> {
                    repeat(20) { advance(plain); advance(enhanced) }
                    assertTrue(enhanced.resource > plain.resource, kind.name)
                }
                ProductEnhancementKind.RESOURCE_CAP -> {
                    plain.resource = stage.resourceCap
                    enhanced.resource = stage.resourceCap
                    repeat(20) { advance(plain); advance(enhanced) }
                    assertEquals(stage.resourceCap, plain.resource)
                    assertTrue(enhanced.resource > plain.resource, kind.name)
                }
                ProductEnhancementKind.BASE_ARMOR -> {
                    listOf(plain, enhanced).forEach { it.enemies += enemy(1, 995); advance(it) }
                    assertTrue(enhanced.baseHealth > plain.baseHealth, kind.name)
                }
                ProductEnhancementKind.HERO_COOLDOWN -> {
                    listOf(plain, enhanced).forEach { advance(it, "use-hero", OriginalProductCatalog.HERO_REPAIR.value) }
                    assertTrue(enhanced.skillCooldowns.getValue(OriginalProductCatalog.HERO_REPAIR) <
                        plain.skillCooldowns.getValue(OriginalProductCatalog.HERO_REPAIR), kind.name)
                }
            }
        }
    }

    @Test
    fun choicesUpdateExistingAllyHealthAndExistingHeroTimersImmediately() {
        val health = fixture().also { state ->
            state.phase = MySdBattlePhase.ENHANCEMENT
            state.waveIndex = 1
            state.allies += ally(1, 700).also { it.health -= 10 }
            state.enhancementOffers += enhancement(ProductEnhancementKind.ALLY_HEALTH)
        }
        val before = health.allies.single().maxHealth
        advance(health, "choose-enhancement", health.enhancementOffers.single().value)
        assertTrue(health.allies.single().maxHealth > before)
        assertEquals(10, health.allies.single().maxHealth - health.allies.single().health)

        val cooldown = fixture().also { state ->
            state.phase = MySdBattlePhase.ENHANCEMENT
            state.waveIndex = 1
            state.skillCooldowns[OriginalProductCatalog.HERO_REPAIR] = 360
            state.enhancementOffers += enhancement(ProductEnhancementKind.HERO_COOLDOWN)
        }
        advance(cooldown, "choose-enhancement", cooldown.enhancementOffers.single().value)
        assertEquals(305, cooldown.skillCooldowns.getValue(OriginalProductCatalog.HERO_REPAIR))
    }

    @Test
    fun everyEnemyMovesAndAppliesItsRoleSpecificInteraction() {
        catalog.enemies.values.forEach { definition ->
            val state = fixture()
            state.enemies += enemy(1, 400, definition.id)
            advance(state)
            assertEquals(400 + definition.speedTicks, state.enemies.single().positionTicks, definition.id.value)

            val contact = fixture()
            contact.enemies += enemy(1, 500, definition.id)
            contact.allies += ally(2, 505)
            advance(contact)
            if (definition.role == ProductEnemyRole.FLYER) {
                assertEquals(46, contact.allies.single().health)
                assertTrue(contact.enemies.single().positionTicks > 500)
            } else {
                assertEquals(46 - definition.attackDamage, contact.allies.single().health, definition.id.value)
                assertEquals(500, contact.enemies.single().positionTicks)
            }
            if (definition.role == ProductEnemyRole.SIEGE || definition.role == ProductEnemyRole.BOSS) {
                val siege = fixture()
                siege.enemies += enemy(1, stage.basePositionTicks - definition.attackRangeTicks, definition.id)
                advance(siege)
                assertEquals(stage.baseHealth - definition.attackDamage, siege.baseHealth, definition.id.value)
            }
        }
        val disrupt = fixture()
        disrupt.slots[0].cooldownRemainingTicks = 10
        disrupt.enemies += enemy(1, disrupt.slots[0].positionTicks, OriginalProductCatalog.ENEMY_DISRUPTOR)
        advance(disrupt)
        assertEquals(10, disrupt.slots[0].cooldownRemainingTicks)
    }

    @Test
    fun everyAllyUsesItsOwnRangeDamageSpeedAndCooldownAndSupportProducesSupply() {
        catalog.allies.values.forEach { definition ->
            val moving = fixture()
            moving.allies += ProductAllyState(1, definition.id, definition.health, definition.health, 500)
            advance(moving)
            assertEquals(500 - definition.speedTicks, moving.allies.single().positionTicks, definition.id.value)
            val attacking = fixture()
            attacking.allies += ProductAllyState(1, definition.id, definition.health, definition.health, 500)
            attacking.enemies += enemy(2, 500)
            advance(attacking)
            assertEquals(1_000 - definition.damage, attacking.enemies.single().health, definition.id.value)
            assertEquals(definition.cooldownTicks, attacking.allies.single().cooldownRemainingTicks)
        }
        val plain = fixture()
        val support = fixture().also {
            it.slots[0].towerId = OriginalProductCatalog.TOWER_SUPPORT
            it.slots[0].level = 1
        }
        repeat(20) { advance(plain); advance(support) }
        assertEquals(4, support.resource - plain.resource)
    }

    @Test
    fun simultaneousHeroKillAndBaseDefeatRetireDeadEntitiesBeforeTerminalSave() {
        val state = fixture()
        state.baseHealth = 1
        state.enemies += enemy(1, 100).also { it.health = 1 }
        state.enemies += enemy(2, 995)
        advance(state, "use-hero", OriginalProductCatalog.HERO_PULSE.value)
        assertEquals(MySdBattlePhase.DEFEAT, state.phase)
        assertTrue(state.enemies.all { it.health > 0 })
        assertTrue(state.enemies.none { it.entityId == 1L })
    }

    private fun enhancement(kind: ProductEnhancementKind): ContentId =
        catalog.enhancements.values.single { it.kind == kind }.id

    private fun enemy(id: Long, position: Int, contentId: ContentId = OriginalProductCatalog.ENEMY_RUNNER) =
        ProductEnemyState(id, contentId, 1_000, 1_000, position)

    private fun ally(id: Long, position: Int) =
        ProductAllyState(id, OriginalProductCatalog.ALLY_DEFENDER, 46, 46, position)

    private fun advance(state: ProductBattleState, type: String? = null, payload: String = "") {
        state.tick += 1
        val commands = if (type == null) emptyList() else listOf(
            TextCommand(CommandId(state.nextCommandId++), Tick(state.tick), type, payload),
        )
        reducer.advance(state, commands)
    }

    private fun fixture(): ProductBattleState {
        val profile = ProductProfileManager.create(1, catalog)
        return ProductBattleState(
            seed = 1, runId = "run-1-1",
            launch = ProductBattleLaunch(stage.id, stage.towerIds + stage.allyIds, stage.heroSkillIds,
                profile.state.rosterLevels.toMap(), 0, 0, 0, 0),
            tick = 100, phase = MySdBattlePhase.COMBAT, paused = false, speed = MySdBattleSpeed.ONE_X,
            waveIndex = 0, waveElapsedTicks = 100,
            spawnedByGroup = stage.waves.first().groups.map { it.count }.toMutableList(),
            resource = 105, incomeRemainder = 0, baseHealth = stage.baseHealth,
            slots = stage.buildSlots.map { ProductTowerSlotState(it.id, it.positionTicks) }.toMutableList(),
            allies = mutableListOf(), enemies = mutableListOf(),
            skillCooldowns = stage.heroSkillIds.associateWith { 0 }.toMutableMap(),
            enhancements = mutableListOf(), enhancementOffers = mutableListOf(), rerollsRemaining = 1,
            nextEntityId = 10, nextCommandId = 1, rngState = 1, terminalResult = null,
        )
    }
}
