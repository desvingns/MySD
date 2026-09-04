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
import dev.mysd.game.battle.playable.PlayableBattleEngine
import dev.mysd.game.battle.playable.PlayableBattlePhase
import dev.mysd.game.battle.playable.PlayableBattleTerminal
import dev.mysd.game.persistence.PendingCommand
import dev.mysd.game.persistence.RunSave
import dev.mysd.game.persistence.RunSaveCodec
import dev.mysd.game.persistence.RunTerminalResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Connected-device lifecycle coverage for playable active/defeat and legacy victory saves.
 *
 * Coverage exception: this project has no instrumentation process-kill harness. The relaunch
 * tests close ActivityScenario and launch a fresh Activity in the same instrumentation process;
 * they verify the durable storage boundary but do not claim OS process-death coverage.
 */
@RunWith(AndroidJUnit4::class)
class LifecyclePersistenceUiTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val device = UiDevice.getInstance(instrumentation)

    @Test
    fun backgroundPersistsActivePlayableRun() = withCleanRunSave {
        val scenario = launchActiveContour()
        try {
            scenario.moveToState(Lifecycle.State.CREATED)

            val encoded = requireStoredEncodedSave()
            val saved = RunSaveCodec.decode(encoded)
            assertActivePlayableSave(saved)
            assertEquals(encoded, RunSaveCodec.encode(saved))
        } finally {
            scenario.close()
        }
    }

    @Test
    fun backgroundPersistsVictoryContour() = withCleanRunSave {
        val scenario = launchVictoryContour()
        try {
            scenario.moveToState(Lifecycle.State.CREATED)

            assertVictoryContourSave(requireStoredRunSave())
        } finally {
            scenario.close()
        }
    }

    @Test
    fun backgroundPersistsDefeatPlayableRunWithoutResumePrompt() = withCleanRunSave {
        val scenario = launchDefeatRun()
        try {
            scenario.moveToState(Lifecycle.State.CREATED)

            assertDefeatPlayableSave(requireStoredRunSave())
        } finally {
            scenario.close()
        }
    }

    @Test
    fun recreateRestoresActivePlayableRun() = withCleanRunSave {
        val scenario = launchActiveContour()
        try {
            scenario.recreate()
            assertActiveContourVisible()
            assertActivePlayableSave(requireStoredRunSave())
        } finally {
            scenario.close()
        }
    }

    @Test
    fun recreateRestoresVictoryContour() = withCleanRunSave {
        val scenario = launchVictoryContour()
        try {
            scenario.recreate()
            assertVictoryContourVisible()
            assertVictoryContourSave(requireStoredRunSave())
        } finally {
            scenario.close()
        }
    }

    @Test
    fun recreateRestoresDefeatPlayableRunWithoutResumePrompt() = withCleanRunSave {
        val scenario = launchDefeatRun()
        try {
            scenario.recreate()
            assertDefeatRestoredUi()
            assertDefeatPlayableSave(requireStoredRunSave())
        } finally {
            scenario.close()
        }
    }

    @Test
    fun relaunchRestoresActivePlayableRunFromDurableStorage() = withCleanRunSave {
        var scenario: ActivityScenario<MainActivity>? = launchActiveContour()
        var relaunched: ActivityScenario<MainActivity>? = null
        try {
            checkNotNull(scenario).moveToState(Lifecycle.State.CREATED)
            assertActivePlayableSave(requireStoredRunSave())
            closeScenarioForRelaunch(checkNotNull(scenario))
            scenario = null

            relaunched = ActivityScenario.launch(MainActivity::class.java)
            assertActiveContourVisible()
            assertActivePlayableSave(requireStoredRunSave())
        } finally {
            scenario?.close()
            relaunched?.close()
        }
    }

    @Test
    fun relaunchRestoresVictoryContourFromDurableStorage() = withCleanRunSave {
        var scenario: ActivityScenario<MainActivity>? = launchVictoryContour()
        var relaunched: ActivityScenario<MainActivity>? = null
        try {
            checkNotNull(scenario).moveToState(Lifecycle.State.CREATED)
            assertVictoryContourSave(requireStoredRunSave())
            closeScenarioForRelaunch(checkNotNull(scenario))
            scenario = null

            relaunched = ActivityScenario.launch(MainActivity::class.java)
            assertVictoryContourVisible()
            assertVictoryContourSave(requireStoredRunSave())
        } finally {
            scenario?.close()
            relaunched?.close()
        }
    }

    @Test
    fun relaunchRestoresDefeatPlayableRunWithoutResumePrompt() = withCleanRunSave {
        var scenario: ActivityScenario<MainActivity>? = launchDefeatRun()
        var relaunched: ActivityScenario<MainActivity>? = null
        try {
            checkNotNull(scenario).moveToState(Lifecycle.State.CREATED)
            assertDefeatPlayableSave(requireStoredRunSave())
            closeScenarioForRelaunch(checkNotNull(scenario))
            scenario = null

            relaunched = ActivityScenario.launch(MainActivity::class.java)
            assertDefeatRestoredUi()
            assertDefeatPlayableSave(requireStoredRunSave())
        } finally {
            scenario?.close()
            relaunched?.close()
        }
    }

    @Test
    fun malformedStoredRunSaveFallsBackToCleanCampaign() = withCleanRunSave {
        val scenario = run {
            seedEncodedSave("not-a-run-save")
            ActivityScenario.launch(MainActivity::class.java)
        }
        try {
            waitForText(R.string.campaign_enter_action)
            click(R.string.campaign_enter_action)
            waitForText(R.string.campaign_selection_title)
            assertFalse(
                "Malformed storage must not create a resume prompt",
                device.wait(
                    Until.hasObject(By.text(context.getString(R.string.campaign_unfinished_title))),
                    300L,
                ),
            )
        } finally {
            scenario.close()
        }
    }

    @Test
    fun unsupportedStageStoredRunSaveFallsBackToCleanCampaign() = withCleanRunSave {
        val scenario = run {
            seedRunSave(unsupportedStageRun())
            ActivityScenario.launch(MainActivity::class.java)
        }
        try {
            waitForText(R.string.campaign_enter_action)
            click(R.string.campaign_enter_action)
            waitForText(R.string.campaign_selection_title)
            assertFalse(
                "An unsupported stage must not create a resume prompt",
                device.wait(
                    Until.hasObject(By.text(context.getString(R.string.campaign_unfinished_title))),
                    300L,
                ),
            )
        } finally {
            scenario.close()
        }
    }

    @Test
    fun relaunchRestoresHistoricalVictoryContourFromDurableStorage() = withCleanRunSave {
        var scenario: ActivityScenario<MainActivity>? = launchVictoryContour()
        var relaunched: ActivityScenario<MainActivity>? = null
        try {
            checkNotNull(scenario).moveToState(Lifecycle.State.CREATED)
            val historicalPayload = legacyContourPayload(requireStoredRunSave())
            closeScenarioForRelaunch(checkNotNull(scenario))
            scenario = null

            seedEncodedSave(historicalPayload)
            relaunched = ActivityScenario.launch(MainActivity::class.java)
            assertVictoryContourVisible()
            assertVictoryContourSave(requireStoredRunSave())
        } finally {
            scenario?.close()
            relaunched?.close()
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
        click(
            R.string.active_battle_speed,
            context.getString(R.string.active_battle_speed_default),
        )
        click(R.string.active_battle_pause_action)
        click(R.string.active_battle_build_action)
        assertActiveContourVisible()
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

    private fun launchDefeatRun(): ActivityScenario<MainActivity> {
        seedRunSave(defeatRun())
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        waitForText(R.string.campaign_enter_action)
        return scenario
    }

    private fun assertActiveContourVisible() {
        waitForText(R.string.active_battle_title)
        waitForText(
            context.getString(
                R.string.active_battle_speed,
                context.getString(R.string.active_battle_speed_alternate),
            ),
        )
        waitForText(R.string.active_battle_resume_action)
        waitForText(R.string.active_battle_build_selected)
    }

    private fun assertVictoryContourVisible() {
        waitForText(R.string.victory_title)
        waitForText(R.string.victory_reward_panel_title)
        waitForText(R.string.victory_reward_panel_body)
    }

    private fun assertDefeatRestoredUi() {
        waitForText(R.string.campaign_enter_action)
        click(R.string.campaign_enter_action)
        waitForText(R.string.campaign_selection_title)
        device.waitForIdle(UI_TIMEOUT_MS)
        assertFalse(
            "Defeat restore must not show an unfinished-run prompt",
            device.hasObject(By.text(context.getString(R.string.campaign_unfinished_title))),
        )
        assertFalse(
            "Defeat restore must not show an active battle surface",
            device.hasObject(By.text(context.getString(R.string.active_battle_title))),
        )
    }

    private fun assertActivePlayableSave(saved: RunSave) {
        assertTrue(saved.active)
        assertNull(saved.terminalResult)
        assertTrue("Active lifecycle saves must carry the full playable state", saved.playableBattleState != null)
        assertEquals(saved.stageId, saved.playableBattleState?.stageId?.value)
        assertEquals(PlayableBattlePhase.PAUSED, saved.playableBattleState?.phase)
        assertEquals(50, saved.playableBattleState?.resource)
        assertEquals(100, saved.playableBattleState?.resourceCap)
        assertEquals(3, saved.playableBattleState?.slots?.size)
        assertTrue(saved.playableBattleState?.enemies?.isNotEmpty() == true)
        assertEquals(
            listOf(
                "mysd.campaign.contour.v1.phase=active",
                "mysd.campaign.contour.v1.origin=NEW_RUN",
                "mysd.campaign.contour.v1.setup=setup-option-b",
                "mysd.campaign.contour.v1.speed=ALTERNATE",
                "mysd.campaign.contour.v1.paused=1",
                "mysd.campaign.contour.v1.build=1",
                "mysd.campaign.contour.v1.refresh=0",
                "mysd.campaign.contour.v1.enhancement=none",
            ),
            saved.modifiers,
        )
    }

    private fun assertDefeatPlayableSave(saved: RunSave) {
        assertTrue(!saved.active)
        assertEquals(RunTerminalResult.DEFEAT, saved.terminalResult)
        assertEquals(
            PlayableBattleTerminal.DEFEAT,
            saved.playableBattleState?.terminalResult,
        )
        assertEquals(0, saved.playableBattleState?.base?.health)
        assertEquals(saved.stageId, saved.playableBattleState?.stageId?.value)
        assertEquals(73, saved.playableBattleState?.resource)
        assertEquals(17, saved.playableBattleState?.waveElapsedTicks)
        assertEquals(41L, saved.tick)
        assertEquals(1, saved.pendingCommands.size)
    }

    private fun assertVictoryContourSave(saved: RunSave) {
        assertTrue(!saved.active)
        assertEquals(RunTerminalResult.VICTORY, saved.terminalResult)
        assertEquals(
            "mysd.campaign.contour.v1.phase=victory",
            saved.modifiers.first(),
        )
        assertEquals(
            "mysd.campaign.contour.v1.enhancement=enhancement-steady-pulse",
            saved.modifiers.last(),
        )
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
        return RunSaveCodec.decode(requireStoredEncodedSave())
    }

    private fun requireStoredEncodedSave(): String = checkNotNull(
        context.getSharedPreferences(
            AndroidRunSaveStorage.PREFERENCES_NAME,
            Context.MODE_PRIVATE,
        ).getString(AndroidRunSaveStorage.ENCODED_SAVE_KEY, null),
    )

    private fun seedEncodedSave(encodedSave: String) {
        check(
            context.getSharedPreferences(
                AndroidRunSaveStorage.PREFERENCES_NAME,
                Context.MODE_PRIVATE,
            ).edit()
                .putString(AndroidRunSaveStorage.ENCODED_SAVE_KEY, encodedSave)
                .commit(),
        )
    }

    private fun seedRunSave(save: RunSave) {
        seedEncodedSave(RunSaveCodec.encode(save))
    }

    private fun closeScenarioForRelaunch(scenario: ActivityScenario<MainActivity>) {
        scenario.moveToState(Lifecycle.State.CREATED)
        scenario.close()
    }

    private fun unsupportedStageRun(): RunSave = RunSave(
        runId = "unsupported-stage-run",
        stageId = "stage-cinder-fall",
        contentVersion = 1,
        simulationVersion = 1,
        seed = 19L,
        rngState = 23L,
        tick = 41L,
        active = true,
        pendingCommands = emptyList(),
        modifiers = emptyList(),
        terminalResult = null,
    )

    private fun legacyContourPayload(save: RunSave): String =
        RunSaveCodec.encode(save)
            .lineSequence()
            .filterNot { it.startsWith("playableStatePresent=") }
            .joinToString("\n")
            .replaceFirst(
                "schemaVersion=${RunSaveCodec.CURRENT_SCHEMA_VERSION}",
                "schemaVersion=3",
            )

    private fun defeatRun(): RunSave {
        val initial = PlayableBattleEngine.initialState(
            initialResource = 73,
            incomePerSecond = 13,
            phase = PlayableBattlePhase.PAUSED,
        )
        val state = initial.copy(
            base = initial.base.copy(health = 0),
            terminalResult = PlayableBattleTerminal.DEFEAT,
            waveElapsedTicks = 17,
        )
        return RunSave(
            runId = "instrumented-defeated-playable-run",
            stageId = "stage-ember-path",
            contentVersion = 1,
            simulationVersion = 1,
            seed = 19L,
            rngState = 23L,
            tick = 41L,
            active = false,
            pendingCommands = listOf(PendingCommand(3L, 41L, "deferred-command", null, "payload")),
            modifiers = emptyList(),
            terminalResult = RunTerminalResult.DEFEAT,
            playableBattleState = state,
        )
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
