package dev.mysd.android.persistence

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Integration coverage for the Android-owned durable run-save boundary. */
@RunWith(AndroidJUnit4::class)
class AndroidRunSaveStorageTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun encodedSaveSurvivesSecondStorageInstance() = withCleanRunSave {
        val encodedSave = "v3|run-id|stage-ember-path|active"
        val firstStorage = AndroidRunSaveStorage(context)

        assertTrue(firstStorage.saveEncodedSave(encodedSave))

        val secondStorage = AndroidRunSaveStorage(context)

        assertEquals(encodedSave, secondStorage.loadEncodedSave())
    }

    @Test
    fun nullSaveRemovesDocumentForSecondStorageInstance() = withCleanRunSave {
        val firstStorage = AndroidRunSaveStorage(context)
        assertTrue(firstStorage.saveEncodedSave("encoded-save"))

        assertTrue(firstStorage.saveEncodedSave(null))

        val secondStorage = AndroidRunSaveStorage(context)

        assertNull(secondStorage.loadEncodedSave())
    }

    private fun withCleanRunSave(block: () -> Unit) {
        val preferences = context.getSharedPreferences(
            AndroidRunSaveStorage.PREFERENCES_NAME,
            Context.MODE_PRIVATE,
        )
        val hadPreviousSave = preferences.contains(AndroidRunSaveStorage.ENCODED_SAVE_KEY)
        val previousEncodedSave = preferences.getString(
            AndroidRunSaveStorage.ENCODED_SAVE_KEY,
            null,
        )
        assertTrue(preferences.edit().remove(AndroidRunSaveStorage.ENCODED_SAVE_KEY).commit())

        try {
            block()
        } finally {
            val editor = preferences.edit()
            if (hadPreviousSave) {
                editor.putString(AndroidRunSaveStorage.ENCODED_SAVE_KEY, previousEncodedSave)
            } else {
                editor.remove(AndroidRunSaveStorage.ENCODED_SAVE_KEY)
            }
            assertTrue(editor.commit())
        }
    }
}
