package dev.mysd.android.campaign

import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import dev.mysd.android.MainActivity
import dev.mysd.android.R
import dev.mysd.android.persistence.AndroidRunSaveStorage
import dev.mysd.game.persistence.RunSave
import dev.mysd.game.persistence.RunSaveCodec
import dev.mysd.game.persistence.RunTerminalResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Connected-device lifecycle coverage for the active and victory Android contours. */
@RunWith(AndroidJUnit4::class)
class LifecyclePersistenceUiTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val device = UiDevice.getInstance(instrumentation)

    @Test
    fun backgroundPersistsActiveContour() = withCleanRunSave {
        val scenario = launchActiveContour()
        try {
            scenario.moveToState(Lifecycle.State.CREATED)

            val saved = requireStoredRunSave()
            assertTrue(saved.active)
            assertNull(saved.terminalResult)
        } finally {
            scenario.close()
        }
    }

    @Test
    fun backgroundPersistsVictoryContour() = withCleanRunSave {
        val scenario = launchVictoryContour()
        try {
            scenario.moveToState(Lifecycle.State.CREATED)

            val saved = requireStoredRunSave()
            assertTrue(!saved.active)
            assertEquals(RunTerminalResult.VICTORY, saved.terminalResult)
        } finally {
            scenario.close()
        }
    }

    @Test
    fun recreateRestoresActiveContour() = withCleanRunSave {
        val scenario = launchActiveContour()
        try {
            scenario.recreate()
            waitForText(R.string.active_battle_title)
            assertTrue(requireStoredRunSave().active)
        } finally {
            scenario.close()
        }
    }

    @Test
    fun recreateRestoresVictoryContour() = withCleanRunSave {
        val scenario = launchVictoryContour()
        try {
            scenario.recreate()
            waitForText(R.string.victory_title)
            assertEquals(RunTerminalResult.VICTORY, requireStoredRunSave().terminalResult)
        } finally {
            scenario.close()
        }
    }

    @Test
    fun processDeathRestoresActiveContour() = withCleanRunSave {
        val scenario = launchActiveContour()
        scenario.moveToState(Lifecycle.State.CREATED)
        scenario.close()

        val relaunched = ActivityScenario.launch(MainActivity::class.java)
        try {
            waitForText(R.string.active_battle_title)
            assertTrue(requireStoredRunSave().active)
        } finally {
            relaunched.close()
        }
    }

    @Test
    fun processDeathRestoresVictoryContour() = withCleanRunSave {
        val scenario = launchVictoryContour()
        scenario.moveToState(Lifecycle.State.CREATED)
        scenario.close()

        val relaunched = ActivityScenario.launch(MainActivity::class.java)
        try {
            waitForText(R.string.victory_title)
            assertEquals(RunTerminalResult.VICTORY, requireStoredRunSave().terminalResult)
        } finally {
            relaunched.close()
        }
    }

    private fun launchActiveContour(): ActivityScenario<MainActivity> {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        waitForText(R.string.campaign_enter_action)
        click(R.string.campaign_enter_action)
        click(R.string.campaign_level_setup_action)
        click(R.string.battle_setup_choice_b)
        click(R.string.battle_setup_continue_action)
        click(R.string.battle_start_action)
        waitForText(R.string.active_battle_title)
        return scenario
    }

    private fun launchVictoryContour(): ActivityScenario<MainActivity> {
        val scenario = launchActiveContour()
        click(R.string.active_battle_enhancement_action)
        click(
            R.string.enhancement_offer_action,
            context.getString(R.string.enhancement_offer_steady_pulse),
        )
        click(R.string.active_battle_victory_action)
        waitForText(R.string.victory_title)
        return scenario
    }

    private fun click(stringRes: Int, vararg formatArgs: Any) {
        val text = context.getString(stringRes, *formatArgs)
        waitForText(text)
        device.findObject(By.text(text)).click()
    }

    private fun waitForText(stringRes: Int) {
        waitForText(context.getString(stringRes))
    }

    private fun waitForText(text: String) {
        assertTrue(
            "Expected visible text: $text",
            device.wait(Until.hasObject(By.text(text)), UI_TIMEOUT_MS),
        )
    }

    private fun requireStoredRunSave(): RunSave {
        val encoded = context.getSharedPreferences(
            AndroidRunSaveStorage.PREFERENCES_NAME,
            Context.MODE_PRIVATE,
        ).getString(AndroidRunSaveStorage.ENCODED_SAVE_KEY, null)
        return RunSaveCodec.decode(checkNotNull(encoded))
    }

    private fun <T> withCleanRunSave(block: () -> T): T {
        val preferences = context.getSharedPreferences(
            AndroidRunSaveStorage.PREFERENCES_NAME,
            Context.MODE_PRIVATE,
        )
        val hadPreviousSave = preferences.contains(AndroidRunSaveStorage.ENCODED_SAVE_KEY)
        val previousEncodedSave = preferences.getString(
            AndroidRunSaveStorage.ENCODED_SAVE_KEY,
            null,
        )
        check(
            preferences.edit()
                .remove(AndroidRunSaveStorage.ENCODED_SAVE_KEY)
                .commit(),
        )
        return try {
            block()
        } finally {
            val editor = preferences.edit()
            if (hadPreviousSave) {
                editor.putString(AndroidRunSaveStorage.ENCODED_SAVE_KEY, previousEncodedSave)
            } else {
                editor.remove(AndroidRunSaveStorage.ENCODED_SAVE_KEY)
            }
            check(editor.commit())
        }
    }

    private companion object {
        const val UI_TIMEOUT_MS = 5_000L
    }
}
