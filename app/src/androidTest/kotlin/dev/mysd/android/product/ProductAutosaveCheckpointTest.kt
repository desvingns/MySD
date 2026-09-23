package dev.mysd.android.product

import dev.mysd.android.persistence.ProductPersistenceStorage
import dev.mysd.game.product.MySdAppFactory
import dev.mysd.game.product.MySdAppSnapshot
import dev.mysd.game.product.MySdBattlePhase
import dev.mysd.game.product.MySdOverlaySnapshot
import dev.mysd.game.product.MySdRestoreResult
import dev.mysd.game.product.MySdSaveBundle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Exercises actual facade progression and save requests, without SharedPreferences or a device clock. */
class ProductAutosaveCheckpointTest {
    @Test
    fun eachOneXAndTwoXPulseIsDurableImmediatelyBeforeTwentyTicks() {
        val (model, storage) = startedModel()
        listOf(1L, 2L).forEach { ticksPerPulse ->
            if (ticksPerPulse == 2L) model.submit(ProductUiAction.ChangeSpeed)
            repeat(3) {
                val beforeTick = model.snapshot.value.clock.tick
                val beforeWrites = storage.saveAttempts
                model.pulse()
                assertEquals(beforeTick + ticksPerPulse, model.snapshot.value.clock.tick)
                assertTrue(model.snapshot.value.clock.tick < 20L)
                assertEquals(beforeWrites + 1, storage.saveAttempts)
                assertDurable(model, storage)
            }
        }
    }

    @Test
    fun ordinaryCombatToCombatWaveBoundaryIsAnImmediateCheckpoint() {
        val (model, storage) = startedModel()
        repeat(MAX_PULSES) {
            val before = model.snapshot.value
            val beforeWrites = storage.saveAttempts
            model.pulse()
            val after = model.snapshot.value
            if (before.battle?.waveNumber != after.battle?.waveNumber) {
                assertEquals(1, before.battle?.waveNumber)
                assertEquals(2, after.battle?.waveNumber)
                assertEquals(MySdBattlePhase.COMBAT, before.battle?.phase)
                assertEquals(MySdBattlePhase.COMBAT, after.battle?.phase)
                assertEquals(before.route, after.route)
                assertEquals(before.overlay, after.overlay)
                assertEquals(beforeWrites + 1, storage.saveAttempts)
                assertDurable(model, storage)
                return
            }
        }
        error("First-stage wave boundary was not reached within the bounded natural scenario.")
    }

    @Test
    fun unchangedPausedEnhancementAndTerminalPulsesDoNotRewriteStorage() {
        val (model, storage) = startedModel()
        model.pulse()
        model.submit(ProductUiAction.PauseOrResume)
        assertTrue(requireNotNull(model.snapshot.value.battle).paused)
        assertFrozenPulses(model, storage)

        model.submit(ProductUiAction.PauseOrResume)
        advanceUntil(model) { it.battle?.phase == MySdBattlePhase.ENHANCEMENT }
        assertDurable(model, storage)
        assertFrozenPulses(model, storage)
        val choice = model.snapshot.value.overlay as MySdOverlaySnapshot.EnhancementChoice
        model.submit(ProductUiAction.SelectEnhancement(choice.offerIds.first()))

        advanceUntil(model) { it.battle?.terminalResult != null }
        assertEquals(MySdBattlePhase.DEFEAT, model.snapshot.value.battle?.phase)
        assertDurable(model, storage)
        assertFrozenPulses(model, storage)
    }

    @Test
    fun failedBatchWritesStayDirtyAndRetryCommitsTheLatestAdvancedState() {
        val (model, storage) = startedModel()
        model.pulse()
        val durableBefore = storage.bundle
        val beforeTick = model.snapshot.value.clock.tick
        val beforeWrites = storage.saveAttempts
        storage.failSave = true

        repeat(2) {
            model.pulse()
            assertTrue(model.saveFailed.value)
            assertEquals(durableBefore, storage.bundle)
        }
        assertEquals(beforeTick + 2, model.snapshot.value.clock.tick)
        assertEquals(beforeWrites + 2, storage.saveAttempts)
        val latest = model.snapshot.value
        val latestAttempt = requireNotNull(storage.lastAttemptedBundle)
        assertEquals(latest, restoredSnapshot(latestAttempt))
        assertFalse(model.persistNow())
        assertTrue(model.saveFailed.value)
        assertEquals(durableBefore, storage.bundle)

        storage.failSave = false
        assertTrue(model.persistNow())
        assertFalse(model.saveFailed.value)
        assertEquals(latest, model.snapshot.value)
        assertEquals(latestAttempt, storage.bundle)
        assertDurable(model, storage)
    }

    private fun startedModel(): Pair<ProductViewModel, MemoryStorage> {
        val storage = MemoryStorage()
        val model = ProductViewModel(storage, epochSeconds = { 1_000L })
        model.submit(ProductUiAction.EnterCampaign)
        model.submit(ProductUiAction.SelectStage("stage-ember-path"))
        model.submit(ProductUiAction.StartBattle)
        assertEquals(0L, model.snapshot.value.clock.tick)
        assertTrue(model.snapshot.value.clock.running)
        assertDurable(model, storage)
        return model to storage
    }

    private fun assertFrozenPulses(model: ProductViewModel, storage: MemoryStorage) {
        val before = model.snapshot.value
        val durableBefore = storage.bundle
        val writesBefore = storage.saveAttempts
        repeat(3) { model.pulse() }
        assertEquals(before, model.snapshot.value)
        assertEquals(durableBefore, storage.bundle)
        assertEquals(writesBefore, storage.saveAttempts)
    }

    private fun advanceUntil(model: ProductViewModel, predicate: (MySdAppSnapshot) -> Boolean) {
        repeat(MAX_PULSES) {
            if (predicate(model.snapshot.value)) return
            model.pulse()
        }
        assertTrue("Natural scenario did not reach the expected phase within $MAX_PULSES pulses.",
            predicate(model.snapshot.value))
    }

    private fun assertDurable(model: ProductViewModel, storage: MemoryStorage) {
        assertFalse(model.saveFailed.value)
        assertEquals(model.snapshot.value, restoredSnapshot(storage.bundle))
    }

    private fun restoredSnapshot(bundle: MySdSaveBundle): MySdAppSnapshot {
        val result = MySdAppFactory.restore(bundle)
        assertTrue("Expected the persisted run/profile pair to restore, got $result", result is MySdRestoreResult.Restored)
        return (result as MySdRestoreResult.Restored).session.snapshot()
    }

    private class MemoryStorage : ProductPersistenceStorage {
        var bundle = MySdAppFactory.create().saveBundle()
        var lastAttemptedBundle: MySdSaveBundle? = null
        var saveAttempts = 0
        var failSave = false
        override fun hasProductProfile() = true
        override fun loadRunSave() = bundle.runSave
        override fun loadProfileSave() = bundle.profileSave
        override fun save(bundle: MySdSaveBundle, archivedLegacyRun: String?): Boolean {
            saveAttempts++
            lastAttemptedBundle = bundle
            if (failSave) return false
            this.bundle = bundle
            return true
        }
        override fun archiveRejectedBundle(profile: String?, run: String?, reason: String): Boolean =
            error("A valid in-memory scenario must not require recovery archival: $reason")
    }

    private companion object {
        const val MAX_PULSES = 1_000
    }
}
