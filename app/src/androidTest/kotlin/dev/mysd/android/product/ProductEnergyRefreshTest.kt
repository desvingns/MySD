package dev.mysd.android.product

import android.content.Context
import android.content.SharedPreferences
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.mysd.android.persistence.AndroidProductPersistence
import dev.mysd.android.persistence.AndroidRunSaveStorage
import dev.mysd.game.product.MySdAppFactory
import dev.mysd.game.product.MySdAppIntent
import dev.mysd.game.product.MySdRoute
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProductEnergyRefreshTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun injectedClockRestoresEnergyWithoutAdvancingBattleTicks() = withCleanStorage {
        val session = MySdAppFactory.create(seed = 41L)
        assertTrue(session.submit(MySdAppIntent.RefreshEnergy(1_000L)).accepted)
        assertTrue(session.submit(MySdAppIntent.Navigate(MySdRoute.CAMPAIGN)).accepted)
        assertTrue(session.submit(MySdAppIntent.SelectStage("stage-ember-path")).accepted)
        assertTrue(session.submit(MySdAppIntent.StartSelectedStage).accepted)
        val before = session.snapshot()
        assertEquals(8, before.profile.energy.current)
        assertTrue(AndroidProductPersistence(context).save(session.saveBundle()))

        val viewModel = ProductViewModel(
            persistence = AndroidProductPersistence(context),
            epochSeconds = { 1_600L },
        )

        assertEquals(10, viewModel.snapshot.value.profile.energy.current)
        assertEquals(before.clock.tick, viewModel.snapshot.value.clock.tick)
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
