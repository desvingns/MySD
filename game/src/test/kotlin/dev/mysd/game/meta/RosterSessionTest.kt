package dev.mysd.game.meta

import dev.mysd.game.content.ContentId
import dev.mysd.game.content.OriginalContentIds
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotSame

class RosterSessionTest {
    @Test
    fun `default snapshot exposes the accepted troop and stable settings`() {
        val snapshot = RosterSession().snapshot()

        assertEquals(RosterSurface.TROOPS, snapshot.surface)
        assertEquals(
            listOf(
                RosterTroopSlot(
                    id = OriginalContentIds.FOUNDATION_UNIT,
                    upgradeAffordanceVisible = true,
                ),
            ),
            snapshot.troopSlots,
        )
        assertEquals(
            listOf(
                RosterSettingOption(RosterSettingId.AUDIO, toggleAffordanceVisible = true),
                RosterSettingOption(RosterSettingId.HAPTICS, toggleAffordanceVisible = true),
            ),
            snapshot.settings,
        )
        assertEquals("setting-audio", snapshot.settings[0].id.stableId)
        assertEquals("setting-haptics", snapshot.settings[1].id.stableId)
    }

    @Test
    fun `constructor copies caller collections and snapshots copy child lists`() {
        val troopIds = mutableListOf(OriginalContentIds.FOUNDATION_UNIT)
        val settingIds = mutableListOf(RosterSettingId.AUDIO, RosterSettingId.HAPTICS)
        val session = RosterSession(troopIds, settingIds)

        troopIds.clear()
        settingIds.clear()

        val first = session.snapshot()
        val second = session.snapshot()

        assertNotSame(first, second)
        assertEquals(listOf(OriginalContentIds.FOUNDATION_UNIT), first.troopSlots.map { it.id })
        assertEquals(
            listOf(RosterSettingId.AUDIO, RosterSettingId.HAPTICS),
            first.settings.map { it.id },
        )
        assertNotSame(first.troopSlots, second.troopSlots)
        assertNotSame(first.settings, second.settings)
    }

    @Test
    fun `opening settings is idempotent and exposes the same immutable roster data`() {
        val session = RosterSession()

        val opened = session.submit(RosterIntent.OpenSettings)
        val repeated = session.submit(RosterIntent.OpenSettings)

        assertEquals(RosterSurface.SETTINGS, opened.surface)
        assertEquals(opened, repeated)
        assertEquals(opened.troopSlots, repeated.troopSlots)
        assertEquals(opened.settings, repeated.settings)
    }

    @Test
    fun `close and confirm settings return to troops without changing affordances`() {
        listOf(RosterIntent.CloseSettings, RosterIntent.ConfirmSettings).forEach { closeIntent ->
            val session = RosterSession()
            val initial = session.snapshot()

            assertEquals(initial, session.submit(closeIntent))
            val settings = session.submit(RosterIntent.OpenSettings)
            val closed = session.submit(closeIntent)

            assertEquals(RosterSurface.SETTINGS, settings.surface)
            assertEquals(initial, closed)
            assertEquals(initial, session.submit(closeIntent))
        }
    }

    @Test
    fun `upgrade and toggle intents are visible affordance no-ops`() {
        val session = RosterSession()
        val initial = session.snapshot()
        val unknownTroop = ContentId.of("unit-unknown")

        listOf(
            RosterIntent.UpgradeTroop(OriginalContentIds.FOUNDATION_UNIT),
            RosterIntent.UpgradeTroop(unknownTroop),
            RosterIntent.ToggleSetting(RosterSettingId.AUDIO),
            RosterIntent.ToggleSetting(RosterSettingId.HAPTICS),
        ).forEach { intent ->
            assertEquals(initial, session.submit(intent))
        }

        assertEquals(RosterSurface.TROOPS, session.snapshot().surface)
        assertEquals(initial.troopSlots, session.snapshot().troopSlots)
        assertEquals(initial.settings, session.snapshot().settings)
    }

    @Test
    fun `toggle remains a no-op while settings surface is open`() {
        val session = RosterSession()
        val settings = session.submit(RosterIntent.OpenSettings)

        assertEquals(
            settings,
            session.submit(RosterIntent.ToggleSetting(RosterSettingId.AUDIO)),
        )
        assertEquals(
            settings,
            session.submit(RosterIntent.ToggleSetting(RosterSettingId.HAPTICS)),
        )
    }

    @Test
    fun `constructor rejects empty and duplicate roster or setting ids`() {
        assertFailsWith<IllegalArgumentException> {
            RosterSession(acceptedTroopIds = emptyList())
        }
        assertFailsWith<IllegalArgumentException> {
            RosterSession(
                acceptedTroopIds = listOf(
                    OriginalContentIds.FOUNDATION_UNIT,
                    OriginalContentIds.FOUNDATION_UNIT,
                ),
            )
        }
        assertFailsWith<IllegalArgumentException> {
            RosterSession(settingIds = emptyList())
        }
        assertFailsWith<IllegalArgumentException> {
            RosterSession(
                settingIds = listOf(RosterSettingId.AUDIO, RosterSettingId.AUDIO),
            )
        }
    }
}
