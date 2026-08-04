package dev.mysd.android.persistence

import android.content.Context

/** Android-owned storage boundary for the encoded, versioned run save document. */
internal class AndroidRunSaveStorage(
    context: Context,
) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun loadEncodedSave(): String? = try {
        preferences.getString(ENCODED_SAVE_KEY, null)
    } catch (_: ClassCastException) {
        null
    }

    internal companion object {
        const val PREFERENCES_NAME = "dev.mysd.android.run-save"
        const val ENCODED_SAVE_KEY = "encoded-versioned-save"
    }
}
