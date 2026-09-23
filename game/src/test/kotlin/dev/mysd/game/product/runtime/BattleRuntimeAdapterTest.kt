package dev.mysd.game.product.runtime

import dev.mysd.game.product.MySdBattleSpeed
import dev.mysd.game.product.MySdBattlePhase
import dev.mysd.game.product.MySdTerminalResult
import dev.mysd.game.product.content.OriginalProductCatalog
import dev.mysd.game.product.meta.ProductProfileManager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class BattleRuntimeAdapterTest {
    private val catalog = OriginalProductCatalog.releaseOne()

    @Test
    fun identicalSeedCommandsAndTicksProduceIdenticalHashes() {
        val first = controller(seed = 61)
        val second = controller(seed = 61)
        val stage = catalog.orderedStages.first()
        val actions = listOf(
            ProductBattleAction.BuildTower(stage.buildSlots.first().id, OriginalProductCatalog.TOWER_BURST),
            ProductBattleAction.SetSpeed(MySdBattleSpeed.TWO_X),
        )
        actions.forEach { action ->
            assertIs<ProductBattleActionResult.Accepted>(first.submit(action))
            assertIs<ProductBattleActionResult.Accepted>(second.submit(action))
        }
        first.step(200)
        second.step(200)

        assertEquals(first.snapshot(), second.snapshot())
        assertEquals(first.stableHash(), second.stableHash())
    }

    @Test
    fun saveRestorePreservesContinuationAndCallerOwnedCommandCursor() {
        val uninterrupted = controller(seed = 62)
        uninterrupted.step(50)
        val saved = uninterrupted.save()
        val restored = assertIs<ProductBattleRestoreResult.Restored>(BattleRuntimeAdapter.restore(saved)).controller

        assertEquals(uninterrupted.snapshot(), restored.snapshot())
        assertEquals(uninterrupted.stableHash(), restored.stableHash())
        uninterrupted.step(75)
        restored.step(75)
        assertEquals(uninterrupted.snapshot(), restored.snapshot())
        assertEquals(uninterrupted.stableHash(), restored.stableHash())
    }

    @Test
    fun schemaFourPayloadMigratesSpeedAndHeroCoordinates() {
        val current = controller(seed = 63, allHeroes = true)
        current.step(25)
        val save = current.save()
        val migratedPayload = save.payload.lineSequence()
            .filterNot { it.startsWith("speed=") || it.startsWith("heroLoadout=") }
            .joinToString("\n")
            .replaceFirst("schemaVersion=5", "schemaVersion=4")
        val legacy = save.copy(schemaVersion = 4, payload = migratedPayload)

        val restored = assertIs<ProductBattleRestoreResult.Restored>(BattleRuntimeAdapter.restore(legacy)).controller
        assertEquals(MySdBattleSpeed.ONE_X, restored.snapshot().speed)
        assertEquals(catalog.orderedStages.first().heroSkillIds, restored.snapshot().heroSkillIds)
        assertEquals(current.stableHash(), restored.stableHash())
    }

    @Test
    fun futureSchemaAndMalformedCompatiblePayloadAreTypedFailures() {
        val save = controller(seed = 64).save()
        val future = assertIs<ProductBattleRestoreResult.Incompatible>(
            BattleRuntimeAdapter.restore(save.copy(schemaVersion = 99)),
        )
        assertEquals(ProductBattleIncompatibility.RUN_SCHEMA, future.kind)

        val malformedPayload = save.payload.replaceFirst(Regex("slots=[^\\n]*"), "slots=bad")
        assertIs<ProductBattleRestoreResult.Invalid>(
            BattleRuntimeAdapter.restore(save.copy(payload = malformedPayload)),
        )
    }

    @Test
    fun boundedRestoreRejectsOversizedPayloadBeforeCollectionAllocation() {
        val save = controller(seed = 65).save()
        val oversized = save.copy(payload = "x".repeat(2_000_001))
        assertIs<ProductBattleRestoreResult.Invalid>(BattleRuntimeAdapter.restore(oversized))
    }

    @Test
    fun pausedActionsRejectWithoutMutatingHashOrCursor() {
        val battle = controller(seed = 66)
        assertIs<ProductBattleActionResult.Accepted>(battle.submit(ProductBattleAction.SetPaused(true)))
        val before = battle.save()
        val slot = battle.snapshot().slots.first().id
        listOf(
            ProductBattleAction.BuildTower(slot, OriginalProductCatalog.TOWER_RAPID),
            ProductBattleAction.DeployAlly(OriginalProductCatalog.ALLY_DEFENDER),
            ProductBattleAction.UseHeroSkill(OriginalProductCatalog.HERO_REPAIR),
        ).forEach { action ->
            assertEquals(ProductBattleRejection.INVALID_PHASE,
                assertIs<ProductBattleActionResult.Rejected>(battle.submit(action)).reason)
            assertEquals(before, battle.save())
        }
        assertEquals(0, assertIs<ProductBattleStepResult.Advanced>(battle.step(100)).advancedTicks)
        assertEquals(before, battle.save())
        val restored = assertIs<ProductBattleRestoreResult.Restored>(BattleRuntimeAdapter.restore(before)).controller
        assertEquals(battle.stableHash(), restored.stableHash())
    }

    @Test
    fun immediateInputsNeverAdvanceSimulationTimeOrPassiveSystems() {
        val battle = controller(seed = 68, allHeroes = true)
        val initial = battle.snapshot()
        val slotId = initial.slots.first().id
        val acceptedIds = mutableListOf<Long>()
        fun accept(action: ProductBattleAction) {
            val result = assertIs<ProductBattleActionResult.Accepted>(battle.submit(action))
            assertEquals(0, result.advancedTicks)
            acceptedIds += result.commandId
        }
        accept(ProductBattleAction.BuildTower(slotId, OriginalProductCatalog.TOWER_RAPID))
        accept(ProductBattleAction.UpgradeTower(slotId))
        accept(ProductBattleAction.DeployAlly(OriginalProductCatalog.ALLY_DEFENDER))
        val prepared = battle.snapshot()
        assertEquals(0L, prepared.tick)
        assertEquals(MySdBattlePhase.PREPARATION, prepared.phase)
        assertEquals(initial.resource - 35 - 24 - 34, prepared.resource)
        assertEquals(0, prepared.waveElapsedTicks)
        assertTrue(prepared.enemies.isEmpty())
        assertEquals(955, prepared.allies.single().positionTicks)
        assertIs<ProductBattleRestoreResult.Restored>(BattleRuntimeAdapter.restore(battle.save()))

        battle.step(1)
        accept(ProductBattleAction.UseHeroSkill(OriginalProductCatalog.HERO_REPAIR))
        val frozen = battle.snapshot()
        assertEquals(360, frozen.skillCooldowns.getValue(OriginalProductCatalog.HERO_REPAIR))
        repeat(100) {
            accept(ProductBattleAction.SetSpeed(MySdBattleSpeed.TWO_X))
            accept(ProductBattleAction.SetSpeed(MySdBattleSpeed.ONE_X))
            accept(ProductBattleAction.SetPaused(true))
            accept(ProductBattleAction.SetPaused(false))
        }
        assertEquals(frozen, battle.snapshot())
        assertEquals(acceptedIds.distinct(), acceptedIds)
        assertTrue(acceptedIds.zipWithNext().all { (a, b) -> b == a + 1 })
        val restored = assertIs<ProductBattleRestoreResult.Restored>(BattleRuntimeAdapter.restore(battle.save())).controller
        assertEquals(battle.stableHash(), restored.stableHash())
        battle.step(1)
        restored.step(1)
        assertEquals(frozen.tick + 1, battle.snapshot().tick)
        assertEquals(359, battle.snapshot().skillCooldowns.getValue(OriginalProductCatalog.HERO_REPAIR))
        assertEquals(battle.stableHash(), restored.stableHash())
    }

    @Test
    fun immediateHeroKillAndEnhancementChoiceRemainSaveableWithoutClockStep() {
        val battle = strongController(1)
        battle.step(1)
        val before = battle.snapshot().tick
        assertIs<ProductBattleActionResult.Accepted>(
            battle.submit(ProductBattleAction.UseHeroSkill(OriginalProductCatalog.HERO_PULSE)))
        assertEquals(before, battle.snapshot().tick)
        assertTrue(battle.snapshot().enemies.all { it.health > 0 })
        assertIs<ProductBattleRestoreResult.Restored>(BattleRuntimeAdapter.restore(battle.save()))
    }

    @Test
    fun allSixStagesReachNaturalVictoryWithProgressedDefenseAndDefeatWithoutDefense() {
        catalog.orderedStages.forEach { stage ->
            val defended = strongController(stage.ordinal)
            driveDefense(defended)
            assertEquals(MySdTerminalResult.VICTORY, defended.snapshot().terminalResult, stage.id.value)
            assertEquals(10, defended.snapshot().waveIndex + 1)
            assertEquals(4, defended.snapshot().enhancements.size)
            assertIs<ProductBattleRestoreResult.Restored>(BattleRuntimeAdapter.restore(defended.save()))

            val empty = strongController(stage.ordinal)
            repeat(20) {
                if (empty.snapshot().phase == MySdBattlePhase.ENHANCEMENT) {
                    empty.submit(ProductBattleAction.ChooseEnhancement(empty.snapshot().enhancementOffers.first()))
                }
                empty.step(2_000)
            }
            assertEquals(MySdTerminalResult.DEFEAT, empty.snapshot().terminalResult, stage.id.value)
            val terminalSave = empty.save()
            assertEquals(ProductBattleRejection.TERMINAL,
                assertIs<ProductBattleActionResult.Rejected>(empty.submit(ProductBattleAction.SetPaused(true))).reason)
            assertEquals(terminalSave, empty.save())
        }
    }

    @Test
    fun largeStepStopsAtEnhancementBoundaryAndChoiceSaveRestores() {
        val battle = strongController(1)
        val stage = catalog.orderedStages.first()
        battle.submit(ProductBattleAction.BuildTower(stage.buildSlots.first().id, OriginalProductCatalog.TOWER_BURST))
        battle.submit(ProductBattleAction.BuildTower(stage.buildSlots[1].id, OriginalProductCatalog.TOWER_RAPID))
        var reachedChoice = false
        repeat(200) {
            if (reachedChoice || battle.snapshot().terminalResult != null) return@repeat
            val before = battle.snapshot().tick
            val stepped = assertIs<ProductBattleStepResult.Advanced>(battle.step(50))
            assertEquals(before + stepped.advancedTicks, battle.snapshot().tick)
            if (battle.snapshot().phase == MySdBattlePhase.ENHANCEMENT) {
                reachedChoice = true
                val frozen = battle.save()
                assertEquals(0, assertIs<ProductBattleStepResult.Advanced>(battle.step(100_000)).advancedTicks)
                assertEquals(frozen, battle.save())
                val restored = assertIs<ProductBattleRestoreResult.Restored>(BattleRuntimeAdapter.restore(frozen)).controller
                assertEquals(battle.stableHash(), restored.stableHash())
                val choice = ProductBattleAction.ChooseEnhancement(battle.snapshot().enhancementOffers.first())
                val choiceTick = battle.snapshot().tick
                battle.submit(choice)
                restored.submit(choice)
                assertEquals(choiceTick, battle.snapshot().tick)
                assertEquals(0, battle.snapshot().waveElapsedTicks)
                assertIs<ProductBattleRestoreResult.Restored>(BattleRuntimeAdapter.restore(battle.save()))
                assertEquals(battle.stableHash(), restored.stableHash())
            }
        }
        assertTrue(reachedChoice, "A stepped batch must expose the wave-two choice without consuming it.")
    }

    @Test
    fun malformedEnvelopeAndNonCanonicalPhaseCoordinatesAreTypedFailures() {
        val save = controller(seed = 67).save()
        assertIs<ProductBattleRestoreResult.Invalid>(BattleRuntimeAdapter.restore(save.copy(contentPackId = "")))
        listOf("tick" to Long.MAX_VALUE.toString(), "phase" to "VICTORY", "nextCommandId" to Long.MAX_VALUE.toString(),
            "waveIndex" to "8", "rerolls" to "2").forEach { (field, value) ->
            val malformed = save.payload.replace(Regex("(?m)^$field=.*$"), "$field=$value")
            assertIs<ProductBattleRestoreResult.Invalid>(BattleRuntimeAdapter.restore(save.copy(payload = malformed)), field)
        }
    }

    private fun strongController(ordinal: Int): ProductBattleController {
        val stage = catalog.orderedStages[ordinal - 1]
        val profile = ProductProfileManager.create(ordinal.toLong(), catalog)
        return BattleRuntimeAdapter.start(ordinal.toLong(), ProductBattleLaunch(
            stage.id, stage.towerIds + stage.allyIds, stage.heroSkillIds,
            profile.state.rosterLevels.mapValues { 5 }, 300, 300, 300, 300,
        ))
    }

    private fun driveDefense(battle: ProductBattleController) {
        repeat(4_000) {
            val state = battle.snapshot()
            if (state.terminalResult != null) return
            if (state.phase == MySdBattlePhase.ENHANCEMENT) {
                battle.submit(ProductBattleAction.ChooseEnhancement(state.enhancementOffers.first()))
                assertIs<ProductBattleRestoreResult.Restored>(BattleRuntimeAdapter.restore(battle.save()))
                return@repeat
            }
            state.slots.forEach { slot ->
                if (slot.towerId == null) {
                    battle.submit(ProductBattleAction.BuildTower(slot.id, OriginalProductCatalog.TOWER_BURST))
                } else if (slot.level < ProductBattleController.MAX_TOWER_LEVEL) {
                    battle.submit(ProductBattleAction.UpgradeTower(slot.id))
                }
            }
            battle.snapshot().heroSkillIds.forEach { battle.submit(ProductBattleAction.UseHeroSkill(it)) }
            if (battle.snapshot().slots.all { it.level == ProductBattleController.MAX_TOWER_LEVEL }) {
                battle.submit(ProductBattleAction.DeployAlly(OriginalProductCatalog.ALLY_DEFENDER))
            }
            battle.step(5)
        }
        error("Stage did not terminate within the deterministic scenario budget.")
    }

    private fun controller(seed: Long, allHeroes: Boolean = false): ProductBattleController {
        val profile = ProductProfileManager.create(seed, catalog)
        val stage = catalog.orderedStages.first()
        val bonuses = profile.techBonuses()
        return BattleRuntimeAdapter.start(
            seed,
            ProductBattleLaunch(
                stageId = stage.id,
                loadoutIds = profile.state.loadoutIds,
                heroSkillIds = if (allHeroes) stage.heroSkillIds else stage.heroSkillIds.filter(profile.state.unlockedRoster::contains),
                rosterLevels = profile.state.rosterLevels,
                towerPowerPermille = bonuses.towerPowerPermille,
                allyPowerPermille = bonuses.allyPowerPermille,
                economyPermille = bonuses.economyPermille,
                heroPowerPermille = bonuses.heroPowerPermille,
            ),
        )
    }
}
