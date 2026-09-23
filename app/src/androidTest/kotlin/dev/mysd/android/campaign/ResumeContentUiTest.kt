package dev.mysd.android.campaign

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import dev.mysd.android.MainActivity
import dev.mysd.android.R
import dev.mysd.android.persistence.AndroidRunSaveStorage
import dev.mysd.android.persistence.AndroidProductPersistence
import dev.mysd.game.persistence.PendingCommand
import dev.mysd.game.persistence.RunSave
import dev.mysd.game.persistence.RunSaveCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.math.ceil

/** ST-0012 structural/device seam for a valid unfinished run loaded by MainActivity. */
@RunWith(AndroidJUnit4::class)
class ResumeContentUiTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val device = UiDevice.getInstance(instrumentation)

    @Test
    fun st0012_loadsSeededRunThroughActivity_andCapturesEvidence() =
        withSeededRun("capture-cancel") { productPreferences ->
            val outputDirectory = requireNotNull(context.getExternalFilesDir("fit"))
            enterCampaign(device, context)
            assertResumePromptVisible(device, context)
            assertResumePanelBounds(device, context)
            device.waitForIdle(UI_TIMEOUT_MS)
            check(outputDirectory.mkdirs() || outputDirectory.isDirectory)
            val screenshot = File(outputDirectory, "ST-0012.png")
            val uiDump = File(outputDirectory, "ST-0012.xml")
            assertTrue(device.takeScreenshot(screenshot))
            device.dumpWindowHierarchy(uiDump)
            assertTrue(screenshot.isFile)
            assertTrue(screenshot.length() > 0L)
            assertTrue(uiDump.isFile)
            assertTrue(uiDump.length() > 0L)
            device.executeShellCommand("cp ${screenshot.absolutePath} /sdcard/Download/ST-0012.png")
            device.executeShellCommand("cp ${uiDump.absolutePath} /sdcard/Download/ST-0012.xml")
            assertRemoteCaptureIsValid(device, "/sdcard/Download/ST-0012.png")
            assertRemoteCaptureIsValid(device, "/sdcard/Download/ST-0012.xml")

            val fitScreenshot = File(outputDirectory, "FIT-04-06-ST-0012.png")
            assertTrue(device.takeScreenshot(fitScreenshot))
            assertTrue(fitScreenshot.isFile)
            assertTrue(fitScreenshot.length() > 0L)
            device.executeShellCommand(
                "cp ${fitScreenshot.absolutePath} /sdcard/Download/FIT-04-06-ST-0012.png",
            )
            assertRemoteCaptureIsValid(device, "/sdcard/Download/FIT-04-06-ST-0012.png")

            clickText(device, context, R.string.campaign_cancel_action)
            assertPromptGone(device, context)
            assertTrue(
                "Campaign selection should be visible after Cancel",
                device.wait(
                    Until.hasObject(By.text(context.getString(R.string.campaign_selection_title))),
                    UI_TIMEOUT_MS,
                ),
            )
            assertTrue(
                "Cancel should hand off to the full product campaign",
                device.wait(
                    Until.hasObject(By.text(context.getString(R.string.product_campaign_title))),
                    UI_TIMEOUT_MS,
                ),
            )
            val productPersistence = AndroidProductPersistence(context)
            assertTrue(productPersistence.hasProductProfile())
            assertNotNull(productPersistence.loadProfileSave())
            assertTrue(productPreferences.contains(AndroidProductPersistence.HAS_RUN_SAVE_KEY))
            assertFalse(productPreferences.getBoolean(AndroidProductPersistence.HAS_RUN_SAVE_KEY, true))
            assertNull(productPersistence.loadRunSave())
            assertEquals(
                "Cancel must archive the exact legacy source before committing product state",
                RunSaveCodec.encode(unfinishedRun()),
                productPreferences.getString(AndroidProductPersistence.ARCHIVED_LEGACY_RUN_KEY, null),
            )
            Log.i(TAG, "capture-cancel: captures, bounds, handoff and archive assertions passed")
        }

    @Test
    fun st0012_continueSeededRunThroughActivityShowsLevelSetup() =
        withSeededRun("continue") {
            enterCampaign(device, context)
            assertResumePromptVisible(device, context)
            clickText(device, context, R.string.campaign_continue_action)
            assertPromptGone(device, context)
            assertTrue(
                "Level setup should be visible after Continue",
                device.wait(
                    Until.hasObject(By.text(context.getString(R.string.battle_setup_title))),
                    UI_TIMEOUT_MS,
                ),
            )
            assertTrue(
                device.wait(
                    Until.hasObject(By.text(context.getString(R.string.battle_setup_body))),
                    UI_TIMEOUT_MS,
                ),
            )
            Log.i(TAG, "continue: prompt dismissal and setup assertions passed")
        }

    private fun enterCampaign(device: UiDevice, context: Context) {
        clickText(device, context, R.string.campaign_enter_action)
    }

    private fun assertResumePromptVisible(device: UiDevice, context: Context) {
        assertTrue(
            device.wait(
                Until.hasObject(By.text(context.getString(R.string.campaign_unfinished_title))),
                UI_TIMEOUT_MS,
            ),
        )
        // Keep the pre-existing body assertion as the fixture's primary copy check.
        assertTrue(
            device.wait(
                Until.hasObject(By.text(context.getString(R.string.campaign_unfinished_body))),
                UI_TIMEOUT_MS,
            ),
        )
        assertTrue(
            device.wait(
                Until.hasObject(By.text(context.getString(R.string.campaign_cancel_action))),
                UI_TIMEOUT_MS,
            ),
        )
        assertTrue(
            device.wait(
                Until.hasObject(By.text(context.getString(R.string.campaign_continue_action))),
                UI_TIMEOUT_MS,
            ),
        )
        assertTrue(
            device.wait(
                Until.hasObject(By.desc(context.getString(R.string.campaign_unfinished_panel_description))),
                UI_TIMEOUT_MS,
            ),
        )
    }

    private fun assertResumePanelBounds(device: UiDevice, context: Context) {
        assertVisibleBounds(
            device,
            By.desc(panelSurfaceDescription(context)),
            "resume panel",
        )
        assertMinimumTouchTarget(
            device,
            By.desc(context.getString(R.string.campaign_cancel_action)),
            "Cancel",
            context,
        )
        assertMinimumTouchTarget(
            device,
            By.desc(context.getString(R.string.campaign_continue_action)),
            "Continue",
            context,
        )
    }

    private fun assertPromptGone(device: UiDevice, context: Context) {
        assertTrue(
            "Resume panel should disappear after the action",
            device.wait(Until.gone(By.desc(panelSurfaceDescription(context))), UI_TIMEOUT_MS),
        )
        assertTrue(
            "Resume prompt should disappear after the action",
            device.wait(
                Until.gone(By.text(context.getString(R.string.campaign_unfinished_title))),
                UI_TIMEOUT_MS,
            ),
        )
    }

    private fun clickText(device: UiDevice, context: Context, stringRes: Int) {
        val selector = By.text(context.getString(stringRes))
        assertTrue("Expected visible action: $stringRes", device.wait(Until.hasObject(selector), UI_TIMEOUT_MS))
        device.findObject(selector).click()
        device.waitForIdle(UI_TIMEOUT_MS)
    }

    private fun assertVisibleBounds(device: UiDevice, selector: BySelector, label: String) {
        assertTrue("Expected visible $label", device.wait(Until.hasObject(selector), UI_TIMEOUT_MS))
        val bounds = device.findObject(selector).visibleBounds
        assertTrue("$label must have visible width", bounds.width() > 0)
        assertTrue("$label must have visible height", bounds.height() > 0)
    }

    private fun assertMinimumTouchTarget(
        device: UiDevice,
        selector: BySelector,
        label: String,
        context: Context,
    ) {
        assertVisibleBounds(device, selector, "$label action")
        val bounds = device.findObject(selector).visibleBounds
        val density = context.resources.displayMetrics.density
        val minimumHeightPx = ceil(MIN_TOUCH_TARGET_DP * density).toInt()
        val minimumWidthPx = ceil(MIN_ACTION_WIDTH_DP * density).toInt()
        assertTrue(
            "$label action height ${bounds.height()}px < ${minimumHeightPx}px (${MIN_TOUCH_TARGET_DP}dp)",
            bounds.height() >= minimumHeightPx,
        )
        assertTrue(
            "$label action width ${bounds.width()}px < ${minimumWidthPx}px (${MIN_ACTION_WIDTH_DP}dp)",
            bounds.width() >= minimumWidthPx,
        )
    }

    private fun panelSurfaceDescription(context: Context): String =
        "${context.getString(R.string.campaign_unfinished_panel_description)} surface"

    private fun seedRunSave(context: Context) {
        check(
            context.getSharedPreferences(
                AndroidRunSaveStorage.PREFERENCES_NAME,
                Context.MODE_PRIVATE,
            ).edit()
                .putString(
                    AndroidRunSaveStorage.ENCODED_SAVE_KEY,
                    RunSaveCodec.encode(unfinishedRun()),
                )
                .commit(),
        )
    }

    private fun withSeededRun(label: String, block: (SharedPreferences) -> Unit) {
        val legacyPreferences = context.getSharedPreferences(
            AndroidRunSaveStorage.PREFERENCES_NAME,
            Context.MODE_PRIVATE,
        )
        val productPreferences = context.getSharedPreferences(
            AndroidProductPersistence.PRODUCT_PREFERENCES_NAME,
            Context.MODE_PRIVATE,
        )
        val previousLegacyValues = legacyPreferences.all.toMap()
        val previousProductValues = productPreferences.all.toMap()
        var scenario: ActivityScenario<MainActivity>? = null
        var primaryFailure: Throwable? = null
        try {
            // Cancel creates a product profile; each independent legacy branch needs both stores clean.
            check(productPreferences.edit().clear().commit())
            check(legacyPreferences.edit().clear().commit())
            seedRunSave(context)
            scenario = ActivityScenario.launch(MainActivity::class.java)
            block(productPreferences)
        } catch (failure: Throwable) {
            primaryFailure = failure
            Log.e(TAG, "$label: primary failure before teardown", failure)
            throw failure
        } finally {
            completeCleanup(
                primaryFailure,
                "$label close" to { closeScenarioFromResumed(scenario) },
                "$label restore product" to { restorePreferences(productPreferences, previousProductValues) },
                "$label restore legacy" to { restorePreferences(legacyPreferences, previousLegacyValues) },
            )
        }
    }

    private fun closeScenarioFromResumed(scenario: ActivityScenario<MainActivity>?) {
        if (scenario == null) return
        Log.i(TAG, "before scenario cleanup; state=${scenario.state}")
        completeCleanup(
            null,
            "resume for cleanup" to {
                if (scenario.state != Lifecycle.State.DESTROYED) {
                    // Closing directly from CREATED repeats the already resumed EmptyActivity handshake.
                    scenario.moveToState(Lifecycle.State.RESUMED)
                }
            },
            "scenario close" to { scenario.close() },
        )
        assertEquals(Lifecycle.State.DESTROYED, scenario.state)
        Log.i(TAG, "scenario cleanup reached DESTROYED")
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

    private fun restorePreferences(
        preferences: SharedPreferences,
        values: Map<String, *>,
    ) {
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

    private fun assertRemoteCaptureIsValid(device: UiDevice, remotePath: String) {
        val listing = device.executeShellCommand("ls -l $remotePath")
            .lineSequence()
            .firstOrNull { it.contains(remotePath) }
            ?.trim()
            .orEmpty()
        val fields = listing.split(Regex("\\s+"))
        val byteCount = fields.getOrNull(4)?.toLongOrNull()
        assertTrue(
            "Expected a non-empty remote capture at $remotePath",
            fields.lastOrNull() == remotePath && byteCount != null && byteCount > 0L,
        )
    }

    private fun unfinishedRun(): RunSave = RunSave(
        runId = "visual-qa-resume-fixture",
        stageId = "stage-ember-path",
        contentVersion = 3,
        simulationVersion = 1,
        seed = 7L,
        rngState = 11L,
        tick = 4L,
        active = true,
        pendingCommands = listOf(PendingCommand(1L, 2L, "visual-qa-fixture", null, "")),
        modifiers = emptyList(),
        terminalResult = null,
    )

    private companion object {
        const val TAG = "ResumeContentUiTest"
        const val UI_TIMEOUT_MS = 5_000L
        const val MIN_TOUCH_TARGET_DP = 48f
        const val MIN_ACTION_WIDTH_DP = 112f
    }
}
