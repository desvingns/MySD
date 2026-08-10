package dev.mysd.game.campaign

import dev.mysd.game.content.OriginalContentIds
import dev.mysd.game.persistence.PendingCommand
import dev.mysd.game.persistence.RunSave
import dev.mysd.game.persistence.RunTerminalResult
import dev.mysd.game.meta.RosterIntent
import dev.mysd.game.meta.RosterSettingId
import dev.mysd.game.meta.RosterSurface
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CampaignSessionTest {
    private val acceptedStage = CampaignStageId.of("stage-ember-path")
    private val otherAcceptedStage = CampaignStageId.of("stage-cinder-fall")

    @Test
    fun `starts at clean launch with a defensive accepted-stage snapshot`() {
        val inputStages = mutableListOf(acceptedStage)
        val session = CampaignSession(inputStages, unfinishedRun = null)

        inputStages += otherAcceptedStage

        assertEquals(
            CampaignSnapshot(
                route = CampaignRoute.CLEAN_LAUNCH,
                acceptedStageIds = listOf(acceptedStage),
                selectedStageId = null,
                setupOrigin = null,
                unfinishedRunPromptVisible = false,
            ),
            session.snapshot(),
        )
    }

    @Test
    fun `entering campaign exposes the unfinished-run prompt`() {
        val session = unfinishedRunSession()

        val snapshot = session.submit(CampaignIntent.EnterCampaign)

        assertEquals(CampaignRoute.CAMPAIGN_SELECTION, snapshot.route)
        assertEquals(listOf(acceptedStage), snapshot.acceptedStageIds)
        assertNull(snapshot.selectedStageId)
        assertNull(snapshot.setupOrigin)
        assertTrue(snapshot.unfinishedRunPromptVisible)
    }

    @Test
    fun `multi-stage accepted campaign state remains valid and deterministic`() {
        val acceptedStages = listOf(acceptedStage, otherAcceptedStage)
        val first = CampaignSession(acceptedStages, unfinishedRun = null)
        val second = CampaignSession(acceptedStages, unfinishedRun = null)

        val firstSnapshots = buildList {
            add(first.snapshot())
            add(first.submit(CampaignIntent.EnterCampaign))
            add(first.submit(CampaignIntent.SelectLevel(otherAcceptedStage)))
        }
        val secondSnapshots = buildList {
            add(second.snapshot())
            add(second.submit(CampaignIntent.EnterCampaign))
            add(second.submit(CampaignIntent.SelectLevel(otherAcceptedStage)))
        }

        assertEquals(firstSnapshots, secondSnapshots)
        firstSnapshots.forEach { snapshot ->
            assertInvariants(snapshot, acceptedStages)
        }
        assertEquals(CampaignRoute.LEVEL_SETUP, firstSnapshots.last().route)
        assertEquals(otherAcceptedStage, firstSnapshots.last().selectedStageId)
        assertEquals(LevelSetupOrigin.NEW_RUN, firstSnapshots.last().setupOrigin)
    }

    @Test
    fun `active non-terminal run save exposes unfinished-run prompt after entering campaign`() {
        val session = AcceptedCampaignFixture.createSession(runSave = runSave())

        val snapshot = session.submit(CampaignIntent.EnterCampaign)

        assertEquals(CampaignRoute.CAMPAIGN_SELECTION, snapshot.route)
        assertEquals(listOf(acceptedStage), snapshot.acceptedStageIds)
        assertTrue(snapshot.unfinishedRunPromptVisible)
        assertNull(snapshot.selectedStageId)
        assertNull(snapshot.setupOrigin)
    }

    @Test
    fun `inactive and terminal run saves do not expose unfinished-run prompt`() {
        listOf(
            runSave(active = false),
            runSave(active = false, terminalResult = RunTerminalResult.VICTORY),
        ).forEach { persistedRun ->
            val session = AcceptedCampaignFixture.createSession(runSave = persistedRun)

            val snapshot = session.submit(CampaignIntent.EnterCampaign)

            assertEquals(CampaignRoute.CAMPAIGN_SELECTION, snapshot.route)
            assertFalse(snapshot.unfinishedRunPromptVisible)
            assertNull(snapshot.selectedStageId)
            assertNull(snapshot.setupOrigin)
        }
    }

    @Test
    fun `malformed or unaccepted active run save stage is ignored`() {
        listOf(
            "stage_ember_path",
            otherAcceptedStage.value,
        ).forEach { stageId ->
            val snapshot = AcceptedCampaignFixture
                .createSession(runSave(stageId = stageId))
                .submit(CampaignIntent.EnterCampaign)

            assertEquals(CampaignRoute.CAMPAIGN_SELECTION, snapshot.route)
            assertFalse(snapshot.unfinishedRunPromptVisible)
            assertNull(snapshot.selectedStageId)
            assertNull(snapshot.setupOrigin)
        }
    }

    @Test
    fun `selecting an accepted stage opens new-run level setup`() {
        val session = CampaignSession(listOf(acceptedStage), unfinishedRun = null)
        session.submit(CampaignIntent.EnterCampaign)

        val snapshot = session.submit(CampaignIntent.SelectLevel(acceptedStage))

        assertEquals(
            CampaignSnapshot(
                route = CampaignRoute.LEVEL_SETUP,
                acceptedStageIds = listOf(acceptedStage),
                selectedStageId = acceptedStage,
                setupOrigin = LevelSetupOrigin.NEW_RUN,
                unfinishedRunPromptVisible = false,
            ),
            snapshot,
        )
        assertEquals(
            BattleSetupSnapshot(
                stageId = acceptedStage,
                availableChoices = BattleSetupChoice.entries,
                selectedChoice = null,
                tutorialContinuationVisible = true,
                setupCompleted = false,
            ),
            session.battleSetupSnapshot(),
        )
    }

    @Test
    fun `selecting an initial option updates setup state without changing the route snapshot`() {
        val session = CampaignSession(listOf(acceptedStage), unfinishedRun = null)
        session.submit(CampaignIntent.EnterCampaign)
        session.submit(CampaignIntent.SelectLevel(acceptedStage))
        val routeBeforeChoice = session.snapshot()

        val routeAfterChoice = session.submit(
            CampaignIntent.SelectInitialOption(BattleSetupChoice.OPTION_B),
        )

        assertEquals(routeBeforeChoice, routeAfterChoice)
        assertEquals(
            BattleSetupChoice.OPTION_B,
            session.battleSetupSnapshot()?.selectedChoice,
        )
        assertEquals(
            BattleSetupChoice.entries,
            session.battleSetupSnapshot()?.availableChoices,
        )
        assertFalse(session.battleSetupSnapshot()?.setupCompleted ?: true)
    }

    @Test
    fun `start battle is blocked until tutorial continuation makes setup ready`() {
        val session = CampaignSession(listOf(acceptedStage), unfinishedRun = null)
        session.submit(CampaignIntent.EnterCampaign)
        session.submit(CampaignIntent.SelectLevel(acceptedStage))
        session.submit(CampaignIntent.SelectInitialOption(BattleSetupChoice.OPTION_A))

        val beforeContinuation = session.submit(CampaignIntent.StartBattle)

        assertNull(beforeContinuation.battleStart)
        assertEquals(CampaignRoute.LEVEL_SETUP, beforeContinuation.route)

        session.submit(CampaignIntent.ContinueTutorialSetup)
        val started = session.submit(CampaignIntent.StartBattle)

        assertEquals(
            BattleStartTransition(
                stageId = acceptedStage,
                selectedChoice = BattleSetupChoice.OPTION_A,
            ),
            started.battleStart,
        )
        assertEquals(CampaignRoute.LEVEL_SETUP, started.route)
        assertTrue(session.battleSetupSnapshot()?.canStartBattle == true)
    }

    @Test
    fun `start battle remains idempotent after the setup handoff`() {
        val session = CampaignSession(listOf(acceptedStage), unfinishedRun = null)
        session.submit(CampaignIntent.EnterCampaign)
        session.submit(CampaignIntent.SelectLevel(acceptedStage))
        session.submit(CampaignIntent.ContinueTutorialSetup)

        val first = session.submit(CampaignIntent.StartBattle)
        val second = session.submit(CampaignIntent.StartBattle)

        assertEquals(first, second)
        assertEquals(acceptedStage, first.battleStart?.stageId)
        assertNull(first.battleStart?.selectedChoice)
    }

    @Test
    fun `canceling unfinished run returns to campaign selection and permits new setup`() {
        val session = unfinishedRunSession()
        session.submit(CampaignIntent.EnterCampaign)

        val selection = session.submit(CampaignIntent.CancelUnfinishedRun)

        assertEquals(CampaignRoute.CAMPAIGN_SELECTION, selection.route)
        assertNull(selection.selectedStageId)
        assertNull(selection.setupOrigin)
        assertFalse(selection.unfinishedRunPromptVisible)

        val setup = session.submit(CampaignIntent.SelectLevel(acceptedStage))

        assertEquals(CampaignRoute.LEVEL_SETUP, setup.route)
        assertEquals(acceptedStage, setup.selectedStageId)
        assertEquals(LevelSetupOrigin.NEW_RUN, setup.setupOrigin)
        assertFalse(setup.unfinishedRunPromptVisible)
    }

    @Test
    fun `continuing unfinished run opens level setup with unfinished origin`() {
        val session = unfinishedRunSession()
        session.submit(CampaignIntent.EnterCampaign)

        val snapshot = session.submit(CampaignIntent.ContinueUnfinishedRun)

        assertEquals(
            CampaignSnapshot(
                route = CampaignRoute.LEVEL_SETUP,
                acceptedStageIds = listOf(acceptedStage),
                selectedStageId = acceptedStage,
                setupOrigin = LevelSetupOrigin.UNFINISHED_RUN,
                unfinishedRunPromptVisible = false,
            ),
            snapshot,
        )
    }

    @Test
    fun `unknown stage selection is rejected without changing the session`() {
        val session = CampaignSession(listOf(acceptedStage), unfinishedRun = null)
        session.submit(CampaignIntent.EnterCampaign)
        val before = session.snapshot()

        val after = session.submit(CampaignIntent.SelectLevel(otherAcceptedStage))

        assertEquals(before, after)
        assertEquals(before, session.snapshot())
    }

    @Test
    fun `route intents are rejected when their preconditions are not met`() {
        val session = unfinishedRunSession()
        val cleanLaunch = session.snapshot()

        assertEquals(cleanLaunch, session.submit(CampaignIntent.SelectLevel(acceptedStage)))
        assertEquals(cleanLaunch, session.submit(CampaignIntent.CancelUnfinishedRun))
        assertEquals(cleanLaunch, session.submit(CampaignIntent.ContinueUnfinishedRun))

        val prompt = session.submit(CampaignIntent.EnterCampaign)
        assertEquals(prompt, session.submit(CampaignIntent.SelectLevel(acceptedStage)))
        assertEquals(prompt, session.submit(CampaignIntent.EnterCampaign))

        val setup = session.submit(CampaignIntent.ContinueUnfinishedRun)
        assertEquals(setup, session.submit(CampaignIntent.CancelUnfinishedRun))
        assertEquals(setup, session.submit(CampaignIntent.ContinueUnfinishedRun))
        assertEquals(setup, session.submit(CampaignIntent.EnterCampaign))
    }

    @Test
    fun `roster opens only from campaign selection and blocks level selection`() {
        val session = unfinishedRunSession()

        assertEquals(session.snapshot(), session.submit(CampaignIntent.OpenRoster))
        session.submit(CampaignIntent.EnterCampaign)
        assertEquals(
            session.snapshot(),
            session.submit(CampaignIntent.OpenRoster),
        )

        session.submit(CampaignIntent.CancelUnfinishedRun)
        val opened = session.submit(CampaignIntent.OpenRoster)

        assertEquals(CampaignRoute.CAMPAIGN_SELECTION, opened.route)
        assertTrue(opened.rosterOpen)
        assertEquals(
            RosterSurface.TROOPS,
            session.rosterSnapshot()?.surface,
        )
        assertEquals(
            opened,
            session.submit(CampaignIntent.OpenRoster),
        )
        assertEquals(
            opened,
            session.submit(CampaignIntent.SelectLevel(acceptedStage)),
        )
        assertEquals(CampaignRoute.CAMPAIGN_SELECTION, session.snapshot().route)
        assertTrue(session.snapshot().rosterOpen)
    }

    @Test
    fun `campaign routes roster settings and clears the child session on close`() {
        val session = CampaignSession(listOf(acceptedStage), unfinishedRun = null)
        session.submit(CampaignIntent.EnterCampaign)
        val opened = session.submit(CampaignIntent.OpenRoster)
        val initialRoster = assertNotNull(session.rosterSnapshot())

        val settings = session.submit(RosterIntent.OpenSettings)
        assertEquals(RosterSurface.SETTINGS, settings?.surface)
        assertEquals(initialRoster.troopSlots, settings?.troopSlots)
        assertEquals(initialRoster.settings, settings?.settings)

        val toggled = session.submit(RosterIntent.ToggleSetting(RosterSettingId.AUDIO))
        assertEquals(settings, toggled)

        val returnedToTroops = session.submit(RosterIntent.ConfirmSettings)
        assertEquals(RosterSurface.TROOPS, returnedToTroops?.surface)
        assertEquals(opened, session.snapshot())
        assertEquals(returnedToTroops, session.rosterSnapshot())

        val closed = session.submit(CampaignIntent.CloseRoster)
        assertFalse(closed.rosterOpen)
        assertNull(session.rosterSnapshot())
        assertNull(session.submit(RosterIntent.OpenSettings))
    }

    @Test
    fun `closing roster is idempotent and rejects every child command afterwards`() {
        val session = CampaignSession(listOf(acceptedStage), unfinishedRun = null)
        session.submit(CampaignIntent.EnterCampaign)
        session.submit(CampaignIntent.OpenRoster)

        val closed = session.submit(CampaignIntent.CloseRoster)

        assertFalse(closed.rosterOpen)
        assertNull(session.rosterSnapshot())
        assertEquals(closed, session.submit(CampaignIntent.CloseRoster))
        assertNull(session.submit(RosterIntent.OpenSettings))
        assertNull(session.submit(RosterIntent.CloseSettings))
        assertNull(session.submit(RosterIntent.ConfirmSettings))
        assertNull(session.submit(RosterIntent.UpgradeTroop(OriginalContentIds.FOUNDATION_UNIT)))
        assertNull(session.submit(RosterIntent.ToggleSetting(RosterSettingId.AUDIO)))
    }

    @Test
    fun `campaign roster snapshots are immutable copies independent of campaign snapshots`() {
        val session = CampaignSession(listOf(acceptedStage), unfinishedRun = null)
        session.submit(CampaignIntent.EnterCampaign)
        val campaignBeforeRoster = session.snapshot()
        session.submit(CampaignIntent.OpenRoster)

        val first = assertNotNull(session.rosterSnapshot())
        val second = assertNotNull(session.rosterSnapshot())

        assertNotSame(first, second)
        assertNotSame(first.troopSlots, second.troopSlots)
        assertNotSame(first.settings, second.settings)
        assertEquals(campaignBeforeRoster, session.snapshot().copy(rosterOpen = false))
    }

    @Test
    fun `reopening roster creates a fresh child snapshot without changing campaign state`() {
        val session = CampaignSession(listOf(acceptedStage), unfinishedRun = null)
        session.submit(CampaignIntent.EnterCampaign)
        session.submit(CampaignIntent.OpenRoster)
        session.submit(RosterIntent.OpenSettings)
        session.submit(CampaignIntent.CloseRoster)

        val reopened = session.submit(CampaignIntent.OpenRoster)

        assertTrue(reopened.rosterOpen)
        assertEquals(RosterSurface.TROOPS, session.rosterSnapshot()?.surface)
        assertEquals(
            listOf(acceptedStage),
            reopened.acceptedStageIds,
        )
        assertNull(reopened.selectedStageId)
        assertNull(reopened.setupOrigin)
    }

    @Test
    fun `same intent sequence produces deterministic snapshots`() {
        val first = collectSnapshots(unfinishedRunSession())
        val second = collectSnapshots(unfinishedRunSession())

        assertEquals(first, second)
        assertEquals(CampaignRoute.CLEAN_LAUNCH, first[0].route)
        assertEquals(CampaignRoute.CAMPAIGN_SELECTION, first[1].route)
        assertTrue(first[1].unfinishedRunPromptVisible)
        assertEquals(CampaignRoute.CAMPAIGN_SELECTION, first[2].route)
        assertFalse(first[2].unfinishedRunPromptVisible)
        assertEquals(CampaignRoute.LEVEL_SETUP, first[3].route)
        assertEquals(LevelSetupOrigin.NEW_RUN, first[3].setupOrigin)
    }

    @Test
    fun `stage ids reject unstable or malformed values`() {
        listOf(
            "",
            " ",
            "Stage-ember-path",
            "stage_ember_path",
            "stage/ember-path",
            "-stage",
            "stage-",
            "stage--ember",
        ).forEach { raw ->
            assertFailsWith<IllegalArgumentException> { CampaignStageId.of(raw) }
        }

        assertEquals("stage-ember-path", CampaignStageId.of("stage-ember-path").value)
    }

    @Test
    fun `constructor validates campaign invariants`() {
        assertFailsWith<IllegalArgumentException> {
            CampaignSession(emptyList(), unfinishedRun = null)
        }
        assertFailsWith<IllegalArgumentException> {
            CampaignSession(listOf(acceptedStage, acceptedStage), unfinishedRun = null)
        }
        assertFailsWith<IllegalArgumentException> {
            CampaignSession(
                acceptedStageIds = listOf(acceptedStage),
                unfinishedRun = UnfinishedCampaignRun(otherAcceptedStage),
            )
        }
    }

    @Test
    fun `all emitted snapshots preserve route invariants`() {
        val session = unfinishedRunSession()
        val snapshots = buildList {
            add(session.snapshot())
            add(session.submit(CampaignIntent.EnterCampaign))
            add(session.submit(CampaignIntent.CancelUnfinishedRun))
            add(session.submit(CampaignIntent.SelectLevel(acceptedStage)))
        }

        snapshots.forEach { snapshot -> assertInvariants(snapshot) }
    }

    private fun unfinishedRunSession(): CampaignSession = CampaignSession(
        acceptedStageIds = listOf(acceptedStage),
        unfinishedRun = UnfinishedCampaignRun(stageId = acceptedStage),
    )

    private fun collectSnapshots(session: CampaignSession): List<CampaignSnapshot> = buildList {
        add(session.snapshot())
        add(session.submit(CampaignIntent.EnterCampaign))
        add(session.submit(CampaignIntent.CancelUnfinishedRun))
        add(session.submit(CampaignIntent.SelectLevel(acceptedStage)))
    }

    private fun runSave(
        stageId: String = acceptedStage.value,
        active: Boolean = true,
        terminalResult: RunTerminalResult? = null,
    ): RunSave = RunSave(
        runId = "campaign-test-run",
        stageId = stageId,
        contentVersion = 3,
        simulationVersion = 1,
        seed = 7L,
        rngState = 11L,
        tick = 4L,
        active = active,
        pendingCommands = listOf(PendingCommand(1L, 2L, "test-command", null, "")),
        modifiers = emptyList(),
        terminalResult = terminalResult,
    )

    private fun assertInvariants(
        snapshot: CampaignSnapshot,
        acceptedStages: List<CampaignStageId> = listOf(acceptedStage),
    ) {
        assertEquals(acceptedStages, snapshot.acceptedStageIds)

        when (snapshot.route) {
            CampaignRoute.CLEAN_LAUNCH -> {
                assertNull(snapshot.selectedStageId)
                assertNull(snapshot.setupOrigin)
                assertFalse(snapshot.unfinishedRunPromptVisible)
            }

            CampaignRoute.CAMPAIGN_SELECTION -> {
                assertNull(snapshot.selectedStageId)
                assertNull(snapshot.setupOrigin)
            }

            CampaignRoute.LEVEL_SETUP -> {
                assertNotNull(snapshot.selectedStageId)
                assertNotNull(snapshot.setupOrigin)
                assertFalse(snapshot.unfinishedRunPromptVisible)
            }
        }

        if (snapshot.unfinishedRunPromptVisible) {
            assertEquals(CampaignRoute.CAMPAIGN_SELECTION, snapshot.route)
        }

        if (snapshot.rosterOpen) {
            assertEquals(CampaignRoute.CAMPAIGN_SELECTION, snapshot.route)
            assertNull(snapshot.selectedStageId)
            assertNull(snapshot.setupOrigin)
        }
    }
}
