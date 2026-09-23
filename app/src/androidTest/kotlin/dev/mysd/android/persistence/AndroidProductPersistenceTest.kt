package dev.mysd.android.persistence

import android.content.Context
import android.content.SharedPreferences
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.mysd.game.product.MySdSaveBundle
import dev.mysd.game.product.MySdAppFactory
import dev.mysd.android.product.ProductViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AndroidProductPersistenceTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun completeBundleSurvivesSecondStorageInstance() = withCleanStorage {
        val storage = AndroidProductPersistence(context)
        assertTrue(storage.save(MySdSaveBundle(runSave = "product-run", profileSave = "profile")))

        val restored = AndroidProductPersistence(context)
        assertTrue(restored.hasProductProfile())
        assertEquals("profile", restored.loadProfileSave())
        assertEquals("product-run", restored.loadRunSave())
    }

    @Test
    fun explicitNoRunDoesNotResurrectLegacyMigrationValue() = withCleanStorage {
        val storage = AndroidProductPersistence(context)
        assertTrue(storage.save(MySdSaveBundle(runSave = null, profileSave = "profile")))
        assertTrue(AndroidRunSaveStorage(context).saveEncodedSave("stale-legacy-run"))

        assertNull(AndroidProductPersistence(context).loadRunSave())
        val productPreferences = context.getSharedPreferences(
            AndroidProductPersistence.PRODUCT_PREFERENCES_NAME,
            Context.MODE_PRIVATE,
        )
        assertTrue(productPreferences.contains(AndroidProductPersistence.HAS_RUN_SAVE_KEY))
        assertFalse(productPreferences.getBoolean(AndroidProductPersistence.HAS_RUN_SAVE_KEY, true))
    }

    @Test
    fun legacyRunIsMigrationSourceBeforeFirstProductCommit() = withCleanStorage {
        assertTrue(AndroidRunSaveStorage(context).saveEncodedSave("legacy-run"))

        assertEquals("legacy-run", AndroidProductPersistence(context).loadRunSave())
    }

    @Test
    fun rejectedRunIsArchivedBeforeFallbackAutosaveAndNoticeExplainsLostRun() = withCleanStorage {
        val profile = MySdAppFactory.create().saveBundle().profileSave
        val rejectedRun = "corrupt-product-run-with-original-payload"
        val storage = AndroidProductPersistence(context)
        assertTrue(storage.save(MySdSaveBundle(rejectedRun, profile)))

        val model = ProductViewModel(storage, epochSeconds = { 1_000L })
        assertTrue(requireNotNull(model.recoveryNotice.value).copyArchived)
        assertTrue(requireNotNull(model.recoveryNotice.value).profileRetained)
        assertNull(model.snapshot.value.battle)
        assertTrue(model.persistNow())
        val copies = context.getSharedPreferences(
            AndroidProductPersistence.PRODUCT_PREFERENCES_NAME, Context.MODE_PRIVATE,
        ).all.filterKeys { it.startsWith(AndroidProductPersistence.RECOVERY_PREFIX) }
        assertTrue(copies.values.contains(rejectedRun))
        assertTrue(copies.values.contains(profile))
        assertNull(storage.loadRunSave())
        model.dismissRecoveryNotice()
        assertNull(model.recoveryNotice.value)
        assertTrue(model.persistNow())
        assertEquals(copies, context.getSharedPreferences(
            AndroidProductPersistence.PRODUCT_PREFERENCES_NAME, Context.MODE_PRIVATE,
        ).all.filterKeys { it.startsWith(AndroidProductPersistence.RECOVERY_PREFIX) })
    }

    @Test
    fun legacyHandoffKeepsOriginalDocumentBesideAtomicProductBundle() = withCleanStorage {
        val storage = AndroidProductPersistence(context)
        assertTrue(storage.save(MySdAppFactory.create().saveBundle(), archivedLegacyRun = "legacy-original"))
        assertEquals("legacy-original", context.getSharedPreferences(
            AndroidProductPersistence.PRODUCT_PREFERENCES_NAME, Context.MODE_PRIVATE,
        ).getString(AndroidProductPersistence.ARCHIVED_LEGACY_RUN_KEY, null))
        assertTrue(storage.hasProductProfile())
        assertNull(storage.loadRunSave())
    }

    private fun withCleanStorage(block: () -> Unit) {
        val product = context.getSharedPreferences(
            AndroidProductPersistence.PRODUCT_PREFERENCES_NAME,
            Context.MODE_PRIVATE,
        )
        val legacy = context.getSharedPreferences(
            AndroidRunSaveStorage.PREFERENCES_NAME,
            Context.MODE_PRIVATE,
        )
        val previousProduct = product.all.toMap()
        val previousLegacy = legacy.all.toMap()
        check(product.edit().clear().commit())
        check(legacy.edit().clear().commit())
        try {
            block()
        } finally {
            restore(product, previousProduct)
            restore(legacy, previousLegacy)
        }
    }

    private fun restore(preferences: SharedPreferences, values: Map<String, *>) {
        val editor = preferences.edit().clear()
        values.forEach { (key, value) ->
            when (value) {
                is Boolean -> editor.putBoolean(key, value)
                is Float -> editor.putFloat(key, value)
                is Int -> editor.putInt(key, value)
                is Long -> editor.putLong(key, value)
                is String -> editor.putString(key, value)
                is Set<*> -> editor.putStringSet(key, value.filterIsInstance<String>().toSet())
            }
        }
        check(editor.commit())
    }
}
