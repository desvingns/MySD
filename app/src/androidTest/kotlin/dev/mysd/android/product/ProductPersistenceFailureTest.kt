package dev.mysd.android.product

import dev.mysd.android.persistence.ProductPersistenceStorage
import dev.mysd.game.product.MySdAppFactory
import dev.mysd.game.product.MySdSaveBundle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductPersistenceFailureTest {
    @Test
    fun acceptedSpendRemainsDirtyAndVisibleUntilRetryCommitsTheSameState() {
        val storage = MemoryStorage()
        val model = ProductViewModel(storage, epochSeconds = { 1_000L })
        model.submit(ProductUiAction.Navigate(ProductDestinationUi.ROSTER))
        val durableBefore = storage.bundle
        val creditsBefore = model.snapshot.value.profile.currencies.soft
        storage.failSave = true

        model.submit(ProductUiAction.UpgradeRosterEntry("tower-ember-needle"))

        assertTrue(model.snapshot.value.profile.currencies.soft < creditsBefore)
        assertTrue(model.saveFailed.value)
        assertEquals(durableBefore, storage.bundle)
        assertFalse(model.persistNow())
        val acceptedState = model.snapshot.value
        storage.failSave = false
        assertTrue(model.persistNow())
        assertFalse(model.saveFailed.value)
        assertEquals(acceptedState, model.snapshot.value)
        assertNotEquals(durableBefore, storage.bundle)
    }

    @Test
    fun failedRecoveryArchivePreventsAnyDestructiveFallbackWriteUntilRetrySucceeds() {
        val storage = MemoryStorage()
        storage.bundle = storage.bundle.copy(runSave = "original-incompatible-run")
        storage.failArchive = true
        val original = storage.bundle
        val model = ProductViewModel(storage, epochSeconds = { 1_000L })

        assertTrue(model.saveFailed.value)
        assertFalse(requireNotNull(model.recoveryNotice.value).copyArchived)
        assertEquals(0, storage.successfulSaves)
        assertEquals(original, storage.bundle)
        storage.failArchive = false
        assertTrue(model.persistNow())
        assertFalse(model.saveFailed.value)
        assertEquals(original, storage.archived)
        assertTrue(requireNotNull(model.recoveryNotice.value).copyArchived)
        assertNull(storage.bundle.runSave)
    }

    private class MemoryStorage : ProductPersistenceStorage {
        var bundle = MySdAppFactory.create().saveBundle()
        var archived: MySdSaveBundle? = null
        var failSave = false
        var failArchive = false
        var successfulSaves = 0
        override fun hasProductProfile() = true
        override fun loadRunSave() = bundle.runSave
        override fun loadProfileSave() = bundle.profileSave
        override fun save(bundle: MySdSaveBundle, archivedLegacyRun: String?): Boolean {
            if (failSave) return false
            this.bundle = bundle
            successfulSaves++
            return true
        }
        override fun archiveRejectedBundle(profile: String?, run: String?, reason: String): Boolean {
            if (failArchive) return false
            archived = MySdSaveBundle(run, requireNotNull(profile))
            return true
        }
    }
}
