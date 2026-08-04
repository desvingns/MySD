package dev.mysd.game.campaign

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BattleSetupSessionTest {
    private val stage = CampaignStageId.of("stage-ember-path")

    @Test
    fun `starts with all accepted choices and a visible tutorial continuation`() {
        val session = BattleSetupSession(stage)

        assertEquals(
            BattleSetupSnapshot(
                stageId = stage,
                availableChoices = BattleSetupChoice.entries,
                selectedChoice = null,
                tutorialContinuationVisible = true,
                setupCompleted = false,
            ),
            session.snapshot(),
        )
        assertFalse(session.snapshot().canStartBattle)
    }

    @Test
    fun `selecting a choice changes only the selected choice`() {
        val session = BattleSetupSession(stage)

        val selected = session.selectChoice(BattleSetupChoice.OPTION_B)

        assertEquals(stage, selected.stageId)
        assertEquals(BattleSetupChoice.entries, selected.availableChoices)
        assertEquals(BattleSetupChoice.OPTION_B, selected.selectedChoice)
        assertTrue(selected.tutorialContinuationVisible)
        assertFalse(selected.setupCompleted)
        assertFalse(selected.canStartBattle)
    }

    @Test
    fun `tutorial continuation completes setup without changing the selected choice`() {
        val session = BattleSetupSession(stage)
        session.selectChoice(BattleSetupChoice.OPTION_C)

        val ready = session.continueTutorial()

        assertEquals(stage, ready.stageId)
        assertEquals(BattleSetupChoice.entries, ready.availableChoices)
        assertEquals(BattleSetupChoice.OPTION_C, ready.selectedChoice)
        assertFalse(ready.tutorialContinuationVisible)
        assertTrue(ready.setupCompleted)
        assertTrue(ready.canStartBattle)
    }

    @Test
    fun `repeating tutorial continuation does not change a completed setup`() {
        val session = BattleSetupSession(stage)
        val first = session.continueTutorial()

        val second = session.continueTutorial()

        assertEquals(first, second)
        assertTrue(second.setupCompleted)
        assertFalse(second.tutorialContinuationVisible)
    }
}
