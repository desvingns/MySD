package dev.mysd.android.campaign

import android.content.Context
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import dev.mysd.android.MainActivity
import dev.mysd.android.R
import dev.mysd.android.persistence.AndroidRunSaveStorage
import dev.mysd.game.persistence.PendingCommand
import dev.mysd.game.persistence.RunSave
import dev.mysd.game.persistence.RunSaveCodec
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** ST-0012 structural/device seam for a valid unfinished run loaded by MainActivity. */
@RunWith(AndroidJUnit4::class)
class ResumeContentUiTest {

    @Test
    fun st0012_loadsSeededRunThroughActivity_andCapturesEvidence() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val preferences = context.getSharedPreferences(
            AndroidRunSaveStorage.PREFERENCES_NAME,
            Context.MODE_PRIVATE,
        )
        val hadPreviousSave = preferences.contains(AndroidRunSaveStorage.ENCODED_SAVE_KEY)
        val previousEncodedSave = preferences.getString(
            AndroidRunSaveStorage.ENCODED_SAVE_KEY,
            null,
        )
        var scenario: ActivityScenario<MainActivity>? = null
        val device = UiDevice.getInstance(instrumentation)

        try {
            seedRunSave(context)
            scenario = ActivityScenario.launch(MainActivity::class.java)
            val outputDirectory = requireNotNull(context.getExternalFilesDir("fit"))
            assertTrue(
                device.wait(
                    Until.hasObject(By.text(context.getString(R.string.campaign_enter_action))),
                    UI_TIMEOUT_MS,
                ),
            )
            device.findObject(By.text(context.getString(R.string.campaign_enter_action))).click()
            assertTrue(
                device.wait(
                    Until.hasObject(By.text(context.getString(R.string.campaign_unfinished_title))),
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
        } finally {
            scenario?.close()
            restoreRunSave(context, hadPreviousSave, previousEncodedSave)
        }
    }

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

    private fun restoreRunSave(
        context: Context,
        hadPreviousSave: Boolean,
        previousEncodedSave: String?,
    ) {
        val editor = context.getSharedPreferences(
            AndroidRunSaveStorage.PREFERENCES_NAME,
            Context.MODE_PRIVATE,
        ).edit()
        if (hadPreviousSave) {
            editor.putString(AndroidRunSaveStorage.ENCODED_SAVE_KEY, previousEncodedSave)
        } else {
            editor.remove(AndroidRunSaveStorage.ENCODED_SAVE_KEY)
        }
        check(
            editor.commit(),
        )
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
        const val UI_TIMEOUT_MS = 5_000L
    }
}
