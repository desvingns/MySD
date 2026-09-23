package dev.mysd.android.persistence

import android.content.Context
import dev.mysd.game.product.MySdSaveBundle
import java.security.MessageDigest

internal interface ProductPersistenceStorage {
    fun hasProductProfile(): Boolean
    fun loadRunSave(): String?
    fun loadProfileSave(): String?
    fun save(bundle: MySdSaveBundle, archivedLegacyRun: String? = null): Boolean
    fun archiveRejectedBundle(profile: String?, run: String?, reason: String): Boolean
}

/**
 * Android-owned encoded storage for the immutable product save bundle.
 *
 * The app never interprets either document. Product run and profile values commit atomically in
 * one SharedPreferences transaction. The legacy run storage is read only as a migration source.
 */
internal class AndroidProductPersistence(
    context: Context,
) : ProductPersistenceStorage {
    private val productPreferences = context.getSharedPreferences(
        PRODUCT_PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )
    private val runStorage = AndroidRunSaveStorage(context)

    override fun hasProductProfile(): Boolean = productPreferences.contains(PROFILE_SAVE_KEY)

    override fun loadRunSave(): String? = try {
        if (productPreferences.contains(HAS_RUN_SAVE_KEY)) {
            if (productPreferences.getBoolean(HAS_RUN_SAVE_KEY, false)) {
                productPreferences.getString(RUN_SAVE_KEY, null)
            } else {
                null
            }
        } else {
            runStorage.loadEncodedSave()
        }
    } catch (_: ClassCastException) {
        runStorage.loadEncodedSave()
    }

    override fun loadProfileSave(): String? = try {
        productPreferences.getString(PROFILE_SAVE_KEY, null)
    } catch (_: ClassCastException) {
        null
    }

    /** Synchronously flushes the immutable bundle before lifecycle teardown. */
    override fun save(bundle: MySdSaveBundle, archivedLegacyRun: String?): Boolean {
        val editor = productPreferences.edit()
            .putBoolean(HAS_RUN_SAVE_KEY, bundle.runSave != null)
            .putString(PROFILE_SAVE_KEY, bundle.profileSave)
        if (archivedLegacyRun != null) editor.putString(ARCHIVED_LEGACY_RUN_KEY, archivedLegacyRun)
        if (bundle.runSave == null) {
            editor.remove(RUN_SAVE_KEY)
        } else {
            editor.putString(RUN_SAVE_KEY, bundle.runSave)
        }
        val committed = editor.commit()
        if (committed) {
            // Compatibility mirror only; the atomic product transaction above is authoritative.
            runStorage.saveEncodedSave(bundle.runSave)
        }
        return committed
    }

    /** Content-addressed recovery copies are never overwritten by a later fallback/autosave. */
    override fun archiveRejectedBundle(profile: String?, run: String?, reason: String): Boolean {
        val source = "${profile?.length ?: -1}:$profile${run?.length ?: -1}:$run"
        val digest = MessageDigest.getInstance("SHA-256").digest(source.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
        val key = "$RECOVERY_PREFIX$digest"
        return productPreferences.edit()
            .putString("$key-profile", profile)
            .putString("$key-run", run)
            .putString("$key-reason", reason)
            .commit()
    }

    internal companion object {
        const val PRODUCT_PREFERENCES_NAME = "dev.mysd.android.product-profile"
        const val PROFILE_SAVE_KEY = "encoded-versioned-profile"
        const val RUN_SAVE_KEY = "encoded-versioned-run"
        const val HAS_RUN_SAVE_KEY = "has-versioned-run"
        const val ARCHIVED_LEGACY_RUN_KEY = "archived-legacy-run-before-product"
        const val RECOVERY_PREFIX = "recovery-"
    }
}
