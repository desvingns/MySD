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

    /** Commits the complete encoded document before Android tears down the activity process. */
    fun saveEncodedSave(encodedSave: String?): Boolean {
        val editor = preferences.edit()
        if (encodedSave == null) {
            editor.remove(ENCODED_SAVE_KEY)
        } else {
            editor.putString(ENCODED_SAVE_KEY, encodedSave)
        }
        return editor.commit()
    }

    internal companion object {
        const val PREFERENCES_NAME = "dev.mysd.android.run-save"
        const val ENCODED_SAVE_KEY = "encoded-versioned-save"
    }
}
