package dev.mysd.game.battle

import dev.mysd.game.campaign.BattleSetupChoice
import dev.mysd.game.campaign.AcceptedCampaignFixture
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class ActiveBattleSessionTest {
    @Test
    fun `initial snapshot exposes the accepted active wave contour`() {
        val snapshot = ActiveBattleSession(
            stageId = AcceptedCampaignFixture.STAGE_ID,
            selectedSetupChoice = BattleSetupChoice.OPTION_B,
        ).snapshot()

        assertEquals("fixture_early_wave", snapshot.fixtureId)
        assertTrue(snapshot.waveActive)
        assertTrue(snapshot.baseVisible)
        assertTrue(snapshot.enemyEntitiesVisible)
        assertEquals(listOf("ash-runner"), snapshot.enemyEntityIds)
        assertTrue(snapshot.speedAffordanceVisible)
        assertEquals(ActiveBattleSpeedIndicator.DEFAULT, snapshot.speedIndicator)
        assertTrue(snapshot.pauseResumeAffordanceVisible)
        assertFalse(snapshot.paused)
        assertTrue(snapshot.buildAffordanceVisible)
        assertFalse(snapshot.buildAffordanceSelected)
    }

    @Test
    fun `speed command changes only the visible indicator without defining a multiplier`() {
        val session = session()
        val before = session.snapshot()

        val alternate = session.submit(ActiveBattleIntent.ChangeSpeed)

        assertEquals(ActiveBattleSpeedIndicator.ALTERNATE, alternate.speedIndicator)
        assertEquals(before.waveActive, alternate.waveActive)
        assertEquals(before.baseVisible, alternate.baseVisible)
        assertEquals(before.enemyEntityIds, alternate.enemyEntityIds)
        assertNotEquals(before.speedIndicator, alternate.speedIndicator)
    }

    @Test
    fun `pause and resume commands expose a deterministic coarse contour`() {
        val session = session()
        val before = session.snapshot()

        val paused = session.submit(ActiveBattleIntent.PauseOrResume)
        val resumed = session.submit(ActiveBattleIntent.PauseOrResume)

        assertTrue(paused.paused)
        assertFalse(resumed.paused)
        assertEquals(before.waveActive, paused.waveActive)
        assertEquals(before.waveActive, resumed.waveActive)
        assertEquals(before.baseVisible, paused.baseVisible)
        assertEquals(before.enemyEntitiesVisible, paused.enemyEntitiesVisible)
    }

    @Test
    fun `build command records affordance selection without cost or effect state`() {
        val session = session()

        val selected = session.submit(ActiveBattleIntent.SelectBuildAffordance)
        val repeated = session.submit(ActiveBattleIntent.SelectBuildAffordance)

        assertTrue(selected.buildAffordanceVisible)
        assertTrue(selected.buildAffordanceSelected)
        assertEquals(selected, repeated)
        assertTrue(selected.waveActive)
        assertTrue(selected.baseVisible)
        assertTrue(selected.enemyEntitiesVisible)
    }

    @Test
    fun `same intent sequence produces identical immutable snapshots`() {
        val first = session()
        val second = session()
        val intents = listOf(
            ActiveBattleIntent.ChangeSpeed,
            ActiveBattleIntent.PauseOrResume,
            ActiveBattleIntent.SelectBuildAffordance,
            ActiveBattleIntent.PauseOrResume,
        )

        val firstSnapshots = intents.map(first::submit)
        val secondSnapshots = intents.map(second::submit)

        assertEquals(firstSnapshots, secondSnapshots)
        assertEquals(firstSnapshots.last(), first.snapshot())
    }

    private fun session(): ActiveBattleSession = ActiveBattleSession(
        stageId = AcceptedCampaignFixture.STAGE_ID,
        selectedSetupChoice = BattleSetupChoice.OPTION_A,
    )
}
