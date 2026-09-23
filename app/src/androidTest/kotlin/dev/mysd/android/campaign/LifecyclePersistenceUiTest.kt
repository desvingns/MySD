package dev.mysd.android.campaign

import android.content.Context
import android.content.SharedPreferences
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
import dev.mysd.android.persistence.AndroidProductPersistence
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
 * Connected-device lifecycle coverage for playable active/terminal and legacy victory saves.
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
        val scenario = launchPausedActiveRun()
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
    fun backgroundPersistsVictoryPlayableRun() = withCleanRunSave {
        val scenario = launchVictoryRun()
        try {
            scenario.moveToState(Lifecycle.State.CREATED)

            val encoded = requireStoredEncodedSave()
            val saved = RunSaveCodec.decode(encoded)
            assertVictoryPlayableSave(saved)
            assertEquals(encoded, RunSaveCodec.encode(saved))
        } finally {
            scenario.close()
        }
    }

    @Test
    fun completedLegacyRunCanEnterFullProductWithoutLosingOriginalDocument() = withCleanRunSave {
        val scenario = launchVictoryRun()
        try {
            val original = requireStoredEncodedSave()
            click(R.string.product_legacy_continue)
            waitForText(R.string.product_campaign_title)
            val persistence = AndroidProductPersistence(context)
            assertTrue(persistence.hasProductProfile())
            assertNull(persistence.loadRunSave())
            val archive = context.getSharedPreferences(
                AndroidProductPersistence.PRODUCT_PREFERENCES_NAME, Context.MODE_PRIVATE,
            ).getString(AndroidProductPersistence.ARCHIVED_LEGACY_RUN_KEY, null)
            assertEquals(RunSaveCodec.decode(original), RunSaveCodec.decode(requireNotNull(archive)))
        } finally {
            scenario.close()
        }
    }

    @Test
    fun backgroundPersistsDefeatPlayableRunWithoutResumePrompt() = withCleanRunSave {
        val scenario = launchDefeatRun()
        try {
            scenario.moveToState(Lifecycle.State.CREATED)

            val encoded = requireStoredEncodedSave()
            val saved = RunSaveCodec.decode(encoded)
            assertDefeatPlayableSave(saved)
            assertEquals(encoded, RunSaveCodec.encode(saved))
        } finally {
            scenario.close()
        }
    }

    @Test
    fun recreateRestoresActivePlayableRun() = withCleanRunSave {
        val scenario = launchPausedActiveRun()
        try {
            scenario.recreate()
            assertPausedActivePlayableVisible()
            assertActivePlayableSave(requireStoredRunSave())
        } finally {
            scenario.close()
        }
    }

    @Test
    fun recreateRestoresVictoryPlayableRun() = withCleanRunSave {
        val scenario = launchVictoryRun()
        try {
            scenario.recreate()
            assertVictoryTerminalVisible()
            assertVictoryPlayableSave(requireStoredRunSave())
        } finally {
            scenario.close()
        }
    }

    @Test
    fun recreateRestoresVictoryAfterLivePlayableTransition() = withCleanRunSave {
        val active = liveVictoryReadyRun()
        assertTrue(active.active)
        assertNull(active.terminalResult)
        assertNull(active.playableBattleState?.terminalResult)
        seedRunSave(active)

        val scenario = ActivityScenario.launch(MainActivity::class.java)
        try {
            assertVictoryTerminalVisible()

            scenario.recreate()

            assertVictoryPlayableSave(requireStoredRunSave())
            assertVictoryTerminalVisible()
        } finally {
            scenario.close()
        }
    }

    @Test
    fun recreateRestoresDefeatPlayableRunWithoutResumePrompt() = withCleanRunSave {
        val scenario = launchDefeatRun()
        try {
            scenario.recreate()
            assertDefeatTerminalVisible()
            assertDefeatPlayableSave(requireStoredRunSave())
        } finally {
            scenario.close()
        }
    }

    @Test
    fun relaunchRestoresActivePlayableRunFromDurableStorage() = withCleanRunSave {
        var scenario: ActivityScenario<MainActivity>? = launchPausedActiveRun()
        var relaunched: ActivityScenario<MainActivity>? = null
        try {
            checkNotNull(scenario).moveToState(Lifecycle.State.CREATED)
            assertActivePlayableSave(requireStoredRunSave())
            closeScenarioForRelaunch(checkNotNull(scenario))
            scenario = null

            relaunched = ActivityScenario.launch(MainActivity::class.java)
            assertPausedActivePlayableVisible()
            assertActivePlayableSave(requireStoredRunSave())
        } finally {
            scenario?.close()
            relaunched?.close()
        }
    }

    @Test
    fun relaunchRestoresVictoryPlayableRunFromDurableStorage() = withCleanRunSave {
        var scenario: ActivityScenario<MainActivity>? = launchVictoryRun()
        var relaunched: ActivityScenario<MainActivity>? = null
        try {
            checkNotNull(scenario).moveToState(Lifecycle.State.CREATED)
            assertVictoryPlayableSave(requireStoredRunSave())
            closeScenarioForRelaunch(checkNotNull(scenario))
            scenario = null

            relaunched = ActivityScenario.launch(MainActivity::class.java)
            assertVictoryTerminalVisible()
            assertVictoryPlayableSave(requireStoredRunSave())
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
            assertDefeatTerminalVisible()
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
            acknowledgeRecoveryArchive()
            waitForText(R.string.campaign_enter_action)
            click(R.string.campaign_enter_action)
            waitForText(R.string.product_campaign_title)
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
            acknowledgeRecoveryArchive()
            waitForText(R.string.campaign_enter_action)
            click(R.string.campaign_enter_action)
            waitForText(R.string.product_campaign_title)
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
    fun unsupportedPlayableContentStoredRunSaveFallsBackToCleanCampaign() = withCleanRunSave {
        val scenario = run {
            seedRunSave(unsupportedPlayableContentRun())
            ActivityScenario.launch(MainActivity::class.java)
        }
        try {
            assertCleanCampaignWithoutResumePrompt()
        } finally {
            scenario.close()
        }
    }

    @Test
    fun malformedPlayableEnvelopeFallsBackToCleanCampaign() = withCleanRunSave {
        val scenario = run {
            val malformed = RunSaveCodec.encode(defeatRun())
                .replace("\nactive=0\n", "\nactive=1\n")
            seedEncodedSave(malformed)
            ActivityScenario.launch(MainActivity::class.java)
        }
        try {
            assertCleanCampaignWithoutResumePrompt()
        } finally {
            scenario.close()
        }
    }

    @Test
    fun relaunchRestoresHistoricalVictoryContourFromDurableStorage() = withCleanRunSave {
        seedEncodedSave(legacyVictoryPayload())
        var scenario: ActivityScenario<MainActivity>? = ActivityScenario.launch(MainActivity::class.java)
        var relaunched: ActivityScenario<MainActivity>? = null
        try {
            assertLegacyVictoryContourVisible()
            checkNotNull(scenario).moveToState(Lifecycle.State.CREATED)
            assertLegacyVictoryContourSave(requireStoredRunSave())
            closeScenarioForRelaunch(checkNotNull(scenario))
            scenario = null

            relaunched = ActivityScenario.launch(MainActivity::class.java)
            assertLegacyVictoryContourVisible()
            assertLegacyVictoryContourSave(requireStoredRunSave())
        } finally {
            scenario?.close()
            relaunched?.close()
        }
    }

    private fun launchPausedActiveRun(): ActivityScenario<MainActivity> {
        seedRunSave(pausedActiveRun())
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        assertPausedActivePlayableVisible()
        return scenario
    }

    private fun launchVictoryRun(): ActivityScenario<MainActivity> {
        seedRunSave(victoryRun())
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        assertVictoryTerminalVisible()
        return scenario
    }

    private fun launchDefeatRun(): ActivityScenario<MainActivity> {
        seedRunSave(defeatRun())
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        waitForText(R.string.active_battle_terminal_defeat_title)
        return scenario
    }

    private fun assertPausedActivePlayableVisible() {
        waitForText(R.string.active_battle_title)
        waitForText(R.string.active_battle_resume_action)
        waitForDescription(context.getString(R.string.active_battle_tile_empty, 1))
        device.waitForIdle(UI_TIMEOUT_MS)
        assertFalse(
            "Playable active restore must not expose legacy battle controls",
            device.hasObject(By.text(context.getString(R.string.active_battle_build_action))) ||
                device.hasObject(By.text(context.getString(R.string.active_battle_enhancement_action))) ||
                device.hasObject(By.text(context.getString(R.string.active_battle_victory_action))),
        )
    }

    private fun assertLegacyVictoryContourVisible() {
        waitForText(R.string.victory_title)
        waitForText(R.string.victory_reward_panel_title)
        waitForText(R.string.victory_reward_panel_body)
    }

    private fun assertVictoryTerminalVisible() {
        assertPlayableTerminalVisible(
            title = context.getString(R.string.active_battle_terminal_victory_title),
            body = context.getString(R.string.active_battle_terminal_victory_body),
        )
    }

    private fun assertDefeatTerminalVisible() {
        assertPlayableTerminalVisible(
            title = context.getString(R.string.active_battle_terminal_defeat_title),
            body = context.getString(R.string.active_battle_terminal_defeat_body),
        )
    }

    private fun assertPlayableTerminalVisible(
        title: String,
        body: String,
    ) {
        waitForText(title)
        waitForText(body)
        device.waitForIdle(UI_TIMEOUT_MS)
        assertFalse(
            "Playable terminal restore must not show the campaign entry action",
            device.hasObject(By.text(context.getString(R.string.campaign_enter_action))),
        )
        assertFalse(
            "Playable terminal restore must not expose active battle controls",
            device.hasObject(By.text(context.getString(R.string.active_battle_pause_action))) ||
                device.hasObject(By.text(context.getString(R.string.active_battle_resume_action))) ||
                device.hasObject(By.text(context.getString(R.string.active_battle_build_action))) ||
                device.hasObject(By.text(context.getString(R.string.active_battle_enhancement_action))) ||
                device.hasObject(By.text(context.getString(R.string.active_battle_victory_action))),
        )
        assertFalse(
            "Playable terminal restore must not show an unfinished-run prompt",
            device.hasObject(By.text(context.getString(R.string.campaign_unfinished_title))),
        )
    }

    private fun assertCleanCampaignWithoutResumePrompt() {
        acknowledgeRecoveryArchive()
        waitForText(R.string.campaign_enter_action)
        click(R.string.campaign_enter_action)
        waitForText(R.string.product_campaign_title)
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

    private fun acknowledgeRecoveryArchive() {
        waitForText(R.string.product_recovery_title)
        waitForText(R.string.product_recovery_archived)
        click(R.string.product_service_confirm)
        assertTrue(context.getSharedPreferences(
            AndroidProductPersistence.PRODUCT_PREFERENCES_NAME, Context.MODE_PRIVATE,
        ).all.keys.any { it.startsWith(AndroidProductPersistence.RECOVERY_PREFIX) })
    }

    private fun assertActivePlayableSave(saved: RunSave) {
        assertTrue(saved.active)
        assertNull(saved.terminalResult)
        assertTrue("Active lifecycle saves must carry the full playable state", saved.playableBattleState != null)
        assertEquals(saved.stageId, saved.playableBattleState?.stageId?.value)
        assertEquals(PlayableBattlePhase.PAUSED, saved.playableBattleState?.phase)
        assertNull(saved.playableBattleState?.terminalResult)
        assertEquals(67, saved.playableBattleState?.resource)
        assertEquals(100, saved.playableBattleState?.resourceCap)
        assertEquals(11, saved.playableBattleState?.waveElapsedTicks)
        assertEquals(3, saved.playableBattleState?.slots?.size)
        assertTrue(saved.playableBattleState?.enemies?.isNotEmpty() == true)
        assertEquals(41L, saved.tick)
        assertTrue(saved.pendingCommands.isEmpty())
        assertTrue(saved.modifiers.isEmpty())
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

    private fun assertVictoryPlayableSave(saved: RunSave) {
        assertTrue(!saved.active)
        assertEquals(RunTerminalResult.VICTORY, saved.terminalResult)
        assertEquals(
            PlayableBattleTerminal.VICTORY,
            saved.playableBattleState?.terminalResult,
        )
        assertEquals(saved.stageId, saved.playableBattleState?.stageId?.value)
        assertTrue(saved.playableBattleState?.base?.health?.let { it > 0 } == true)
        assertTrue(saved.playableBattleState?.enemies?.isEmpty() == true)
        assertEquals(
            saved.playableBattleState?.waveSpawnCount,
            saved.playableBattleState?.waveSpawnedCount,
        )
        assertEquals(83, saved.playableBattleState?.resource)
        assertEquals(29, saved.playableBattleState?.waveElapsedTicks)
        assertEquals(61L, saved.tick)
        assertTrue(saved.pendingCommands.isEmpty())
        assertTrue(saved.modifiers.isEmpty())
    }

    private fun assertLegacyVictoryContourSave(saved: RunSave) {
        assertTrue(!saved.active)
        assertEquals(RunTerminalResult.VICTORY, saved.terminalResult)
        assertNull(saved.playableBattleState)
        assertEquals(legacyVictoryModifiers(), saved.modifiers)
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

    private fun waitForDescription(description: String) {
        assertTrue(
            "Expected visible content description: $description",
            device.wait(Until.hasObject(By.desc(description)), UI_TIMEOUT_MS),
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

    private fun pausedActiveRun(): RunSave {
        val initial = PlayableBattleEngine.initialState(
            initialResource = 67,
            incomePerSecond = 13,
            phase = PlayableBattlePhase.PAUSED,
        )
        return RunSave(
            runId = "instrumented-paused-playable-run",
            stageId = "stage-ember-path",
            contentVersion = 1,
            simulationVersion = 1,
            seed = 19L,
            rngState = 23L,
            tick = 41L,
            active = true,
            pendingCommands = emptyList(),
            modifiers = emptyList(),
            terminalResult = null,
            playableBattleState = initial.copy(waveElapsedTicks = 11),
        )
    }

    private fun victoryRun(): RunSave {
        val initial = PlayableBattleEngine.initialState(
            initialResource = 83,
            incomePerSecond = 7,
            phase = PlayableBattlePhase.PAUSED,
        )
        val state = initial.copy(
            enemies = emptyList(),
            terminalResult = PlayableBattleTerminal.VICTORY,
            waveSpawnedCount = initial.waveSpawnCount,
            waveElapsedTicks = 29,
        )
        return RunSave(
            runId = "instrumented-victorious-playable-run",
            stageId = "stage-ember-path",
            contentVersion = 1,
            simulationVersion = 1,
            seed = 31L,
            rngState = 37L,
            tick = 61L,
            active = false,
            pendingCommands = emptyList(),
            modifiers = emptyList(),
            terminalResult = RunTerminalResult.VICTORY,
            playableBattleState = state,
        )
    }

    private fun liveVictoryReadyRun(): RunSave {
        val initial = PlayableBattleEngine.initialState(
            initialResource = 83,
            incomePerSecond = 7,
            phase = PlayableBattlePhase.ACTIVE,
        )
        return RunSave(
            runId = "instrumented-live-victory-playable-run",
            stageId = "stage-ember-path",
            contentVersion = 1,
            simulationVersion = 1,
            seed = 31L,
            rngState = 37L,
            tick = 60L,
            active = true,
            pendingCommands = emptyList(),
            modifiers = emptyList(),
            terminalResult = null,
            playableBattleState = initial.copy(
                enemies = emptyList(),
                waveSpawnedCount = initial.waveSpawnCount,
                waveElapsedTicks = 28,
            ),
        )
    }

    private fun legacyVictoryRun(): RunSave = RunSave(
        runId = "instrumented-legacy-victory-contour",
        stageId = "stage-ember-path",
        contentVersion = 1,
        simulationVersion = 1,
        seed = 43L,
        rngState = 47L,
        tick = 71L,
        active = false,
        pendingCommands = emptyList(),
        modifiers = legacyVictoryModifiers(),
        terminalResult = RunTerminalResult.VICTORY,
    )

    private fun legacyVictoryModifiers(): List<String> = listOf(
        "mysd.campaign.contour.v1.phase=victory",
        "mysd.campaign.contour.v1.origin=NEW_RUN",
        "mysd.campaign.contour.v1.setup=setup-option-b",
        "mysd.campaign.contour.v1.speed=ALTERNATE",
        "mysd.campaign.contour.v1.paused=1",
        "mysd.campaign.contour.v1.build=1",
        "mysd.campaign.contour.v1.refresh=0",
        "mysd.campaign.contour.v1.enhancement=enhancement-steady-pulse",
    )

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

    private fun unsupportedPlayableContentRun(): RunSave {
        val initial = PlayableBattleEngine.initialState()
        val state = initial.copy(
            slots = initial.slots.mapIndexed { index, slot ->
                if (index == 0) slot.copy(towerId = dev.mysd.game.content.ContentId.of("tower-unknown")) else slot
            },
        )
        return RunSave(
            runId = "unsupported-playable-content-run",
            stageId = "stage-ember-path",
            contentVersion = 1,
            simulationVersion = 1,
            seed = 19L,
            rngState = 23L,
            tick = 41L,
            active = true,
            pendingCommands = emptyList(),
            modifiers = emptyList(),
            terminalResult = null,
            playableBattleState = state,
        )
    }

    private fun legacyVictoryPayload(): String =
        RunSaveCodec.encode(legacyVictoryRun())
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
        val productPreferences = context.getSharedPreferences(
            AndroidProductPersistence.PRODUCT_PREFERENCES_NAME,
            Context.MODE_PRIVATE,
        )
        val previousProductValues = productPreferences.all.toMap()
        check(
            preferences.edit()
                .remove(AndroidRunSaveStorage.ENCODED_SAVE_KEY)
                .commit(),
        )
        check(productPreferences.edit().clear().commit())
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
            restorePreferences(productPreferences, previousProductValues)
        }
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

    private companion object {
        const val UI_TIMEOUT_MS = 5_000L
    }
}
