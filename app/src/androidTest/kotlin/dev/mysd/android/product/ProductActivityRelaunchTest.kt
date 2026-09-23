package dev.mysd.android.product

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import dev.mysd.android.MainActivity
import dev.mysd.android.R
import dev.mysd.android.persistence.AndroidProductPersistence
import dev.mysd.android.persistence.AndroidRunSaveStorage
import dev.mysd.game.product.MySdAppFactory
import dev.mysd.game.product.MySdAppIntent
import dev.mysd.game.product.MySdRoute
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProductActivityRelaunchTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val device = UiDevice.getInstance(instrumentation)

    @Test
    fun productRouteSurvivesLifecycleStopAndFreshActivityLaunch() = withCleanStorage {
        val session = MySdAppFactory.create(seed = 57L)
        assertTrue(session.submit(MySdAppIntent.Navigate(MySdRoute.SETTINGS)).accepted)
        assertTrue(AndroidProductPersistence(context).save(session.saveBundle()))

        var scenario: ActivityScenario<MainActivity>? = null
        var primaryFailure: Throwable? = null
        try {
            Log.i(TAG, "settings: launching first Activity")
            scenario = ActivityScenario.launch(MainActivity::class.java)
            assertSettingsVisible()
            Log.i(TAG, "settings: first visibility assertion passed")
            scenario.moveToState(Lifecycle.State.CREATED)
            Log.i(TAG, "settings: lifecycle stop reached")
            closeScenarioFromResumed(scenario, "settings first close")
            scenario = null

            scenario = ActivityScenario.launch(MainActivity::class.java)
            assertSettingsVisible()
            Log.i(TAG, "settings: fresh Activity visibility assertion passed")
        } catch (failure: Throwable) {
            primaryFailure = failure
            Log.e(TAG, "settings: primary failure before teardown", failure)
            throw failure
        } finally {
            completeCleanup(
                primaryFailure,
                "settings final close" to { closeScenarioFromResumed(scenario, "settings final close") },
            )
        }
    }

    @Test
    fun activeBattleStopsAtLifecycleBoundaryAndContinuesOnlyWhenResumed() = withCleanStorage {
        val session = preparedBattle()
        assertTrue(AndroidProductPersistence(context).save(session.saveBundle()))
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        var primaryFailure: Throwable? = null
        try {
            lateinit var viewModel: ProductViewModel
            scenario.onActivity { viewModel = ViewModelProvider(it)[ProductViewModel::class.java] }
            scenario.moveToState(Lifecycle.State.CREATED)
            val stopped = viewModel.snapshot.value
            val stored = AndroidProductPersistence(context).loadRunSave()
            Thread.sleep(250L)
            assertEquals(stopped.clock.tick, viewModel.snapshot.value.clock.tick)
            assertEquals(stopped.battle, viewModel.snapshot.value.battle)
            assertEquals(stored, AndroidProductPersistence(context).loadRunSave())
            Log.i(TAG, "active: stopped tick, entities and saved bytes assertions passed")

            scenario.moveToState(Lifecycle.State.RESUMED)
            val deadline = System.currentTimeMillis() + UI_TIMEOUT_MS
            while (viewModel.snapshot.value.clock.tick == stopped.clock.tick && System.currentTimeMillis() < deadline) {
                Thread.sleep(20L)
            }
            assertTrue(viewModel.snapshot.value.clock.tick > stopped.clock.tick)
            Log.i(TAG, "active: explicit resume advances tick assertion passed")
        } catch (failure: Throwable) {
            primaryFailure = failure
            Log.e(TAG, "active: primary failure before teardown", failure)
            throw failure
        } finally {
            // This scenario already resumed as part of its actual lifecycle assertion.
            completeCleanup(primaryFailure, "active final close" to { scenario.close() })
        }
    }

    @Test
    fun pausedBuiltBattleSurvivesRecreationAndFreshActivityWithoutTickOrEntityLoss() = withCleanStorage {
        val session = preparedBattle()
        val slot = requireNotNull(session.snapshot().battle).towerSlots.first().slotId
        assertTrue(session.submit(MySdAppIntent.BuildTower(slot, "tower-ember-needle")).accepted)
        assertTrue(session.step(40).accepted)
        assertTrue(session.submit(MySdAppIntent.PauseOrResume).accepted)
        val expected = session.snapshot()
        val expectedRun = session.saveBundle().runSave
        assertTrue(AndroidProductPersistence(context).save(session.saveBundle()))
        var scenario: ActivityScenario<MainActivity>? = null
        var primaryFailure: Throwable? = null
        try {
            Log.i(TAG, "paused: launching first Activity")
            scenario = ActivityScenario.launch(MainActivity::class.java)
            assertPausedBattleVisible()
            Log.i(TAG, "paused: initial visibility assertion passed")
            scenario.recreate()
            assertPausedBattleVisible()
            Log.i(TAG, "paused: recreated visibility assertion passed")
            scenario.moveToState(Lifecycle.State.CREATED)
            assertEquals(expectedRun, AndroidProductPersistence(context).loadRunSave())
            Log.i(TAG, "paused: recreated stopped run bytes assertion passed")
            closeScenarioFromResumed(scenario, "paused first close")
            scenario = null

            Log.i(TAG, "paused: launching fresh Activity")
            scenario = ActivityScenario.launch(MainActivity::class.java)
            assertPausedBattleVisible()
            Log.i(TAG, "paused: fresh Activity visibility assertion passed")
            scenario.onActivity {
                val actual = ViewModelProvider(it)[ProductViewModel::class.java].snapshot.value
                assertEquals(expected.clock.tick, actual.clock.tick)
                assertEquals(expected.battle, actual.battle)
            }
            Log.i(TAG, "paused: fresh Activity tick and entities assertions passed")
            scenario.moveToState(Lifecycle.State.CREATED)
            assertEquals(expectedRun, AndroidProductPersistence(context).loadRunSave())
            Log.i(TAG, "paused: fresh stopped run bytes assertion passed")
        } catch (failure: Throwable) {
            primaryFailure = failure
            Log.e(TAG, "paused: primary failure before teardown", failure)
            throw failure
        } finally {
            completeCleanup(
                primaryFailure,
                "paused final close" to { closeScenarioFromResumed(scenario, "paused final close") },
            )
        }
    }

    private fun preparedBattle() = MySdAppFactory.create(seed = 71L).also {
        assertTrue(it.submit(MySdAppIntent.Navigate(MySdRoute.CAMPAIGN)).accepted)
        assertTrue(it.submit(MySdAppIntent.SelectStage("stage-ember-path")).accepted)
        assertTrue(it.submit(MySdAppIntent.StartSelectedStage).accepted)
    }

    private fun assertPausedBattleVisible() {
        assertTrue(device.wait(
            Until.hasObject(By.text(context.getString(R.string.product_battle_paused_title))),
            UI_TIMEOUT_MS,
        ))
    }

    private fun assertSettingsVisible() {
        assertTrue(
            device.wait(
                Until.hasObject(By.text(context.getString(R.string.product_settings_title))),
                UI_TIMEOUT_MS,
            ),
        )
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
        var primaryFailure: Throwable? = null
        try {
            check(product.edit().clear().commit())
            check(legacy.edit().clear().commit())
            block()
        } catch (failure: Throwable) {
            primaryFailure = failure
            Log.e(TAG, "storage fixture: failure before preferences restoration", failure)
            throw failure
        } finally {
            completeCleanup(
                primaryFailure,
                "restore product" to { restore(product, previousProduct) },
                "restore legacy" to { restore(legacy, previousLegacy) },
            )
        }
    }

    private fun closeScenarioFromResumed(scenario: ActivityScenario<MainActivity>?, label: String) {
        if (scenario == null) return
        Log.i(TAG, "$label: before cleanup; state=${scenario.state}")
        completeCleanup(
            null,
            "$label resume" to {
                if (scenario.state != Lifecycle.State.DESTROYED) {
                    // All stopped-state assertions ran first. Paused battles remain frozen here.
                    // Resume dismisses EmptyActivity before close starts a fresh handshake.
                    scenario.moveToState(Lifecycle.State.RESUMED)
                }
            },
            label to { scenario.close() },
        )
        assertEquals(Lifecycle.State.DESTROYED, scenario.state)
        Log.i(TAG, "$label: DESTROYED observed")
    }

    private fun completeCleanup(primaryFailure: Throwable?, vararg steps: Pair<String, () -> Unit>) {
        var failure = primaryFailure
        steps.forEach { (label, action) ->
            try {
                action()
            } catch (cleanupFailure: Throwable) {
                Log.e(TAG, "$label: cleanup failure", cleanupFailure)
                val existing = failure
                if (existing == null) failure = cleanupFailure
                else if (existing !== cleanupFailure) existing.addSuppressed(cleanupFailure)
            }
        }
        if (primaryFailure == null) failure?.let { throw it }
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

    private companion object {
        const val TAG = "ProductRelaunchTest"
        const val UI_TIMEOUT_MS = 5_000L
    }
}
