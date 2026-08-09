package dev.mysd.game.campaign

import dev.mysd.game.meta.RosterIntent
import dev.mysd.game.meta.RosterSettingId
import dev.mysd.game.meta.RosterSurface
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CampaignRosterIntegrationTest {
    @Test
    fun `offline campaign contour reaches troops settings and back without progression effects`() {
        val session = AcceptedCampaignFixture.createSession(runSave = null)

        session.submit(CampaignIntent.EnterCampaign)
        val beforeRoster = session.snapshot()
        val roster = session.submit(CampaignIntent.OpenRoster)
        val rosterSnapshot = assertNotNull(session.rosterSnapshot())

        assertEquals(CampaignRoute.CAMPAIGN_SELECTION, roster.route)
        assertTrue(roster.rosterOpen)
        assertEquals(RosterSurface.TROOPS, rosterSnapshot.surface)
        assertTrue(rosterSnapshot.troopSlots.all { it.upgradeAffordanceVisible })
        assertTrue(rosterSnapshot.settings.all { it.toggleAffordanceVisible })

        val settings = assertNotNull(session.submit(RosterIntent.OpenSettings))
        assertEquals(RosterSurface.SETTINGS, settings.surface)
        val afterToggle = session.submit(RosterIntent.ToggleSetting(RosterSettingId.HAPTICS))
        assertEquals(settings, afterToggle)

        val troops = assertNotNull(session.submit(RosterIntent.CloseSettings))
        assertEquals(RosterSurface.TROOPS, troops.surface)
        assertEquals(beforeRoster, session.snapshot().copy(rosterOpen = false))

        val closed = session.submit(CampaignIntent.CloseRoster)
        assertFalse(closed.rosterOpen)
        assertNull(session.rosterSnapshot())
        assertEquals(CampaignRoute.CAMPAIGN_SELECTION, closed.route)
        assertEquals(AcceptedCampaignFixture.STAGE_ID, closed.acceptedStageIds.single())
        assertNull(closed.selectedStageId)
        assertNull(closed.setupOrigin)
    }
}
