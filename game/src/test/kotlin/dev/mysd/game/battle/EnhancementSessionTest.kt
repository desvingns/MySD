package dev.mysd.game.battle

import dev.mysd.game.campaign.AcceptedCampaignFixture
import dev.mysd.game.campaign.BattleSetupChoice
import dev.mysd.game.content.OriginalContentIds
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EnhancementSessionTest {
    @Test
    fun `initial snapshot exposes offers filter and refresh contour`() {
        val snapshot = session().snapshot()

        assertEquals("fixture_enhancement_choice", snapshot.fixtureId)
        assertEquals(
            listOf(
                OriginalContentIds.FOUNDATION_ENHANCEMENT,
                OriginalContentIds.FOUNDATION_ENHANCEMENT_EMBER_WARD,
            ),
            snapshot.offers.map { it.id },
        )
        assertEquals(2, snapshot.offers.size)
        assertEquals(snapshot.offers.size, snapshot.offers.distinct().size)
        assertTrue(snapshot.allFilterVisible)
        assertTrue(snapshot.refreshAffordanceVisible)
        assertEquals(0, snapshot.refreshRevision)
        assertNull(snapshot.selectedOfferId)
        assertFalse(snapshot.returnToBattle)
    }

    @Test
    fun `refresh is deterministic and does not invent reroll or cost semantics`() {
        val first = session()
        val second = session()
        val beforeRefresh = first.snapshot()

        val firstSnapshot = first.submit(EnhancementIntent.RefreshOffers)
        val secondSnapshot = second.submit(EnhancementIntent.RefreshOffers)

        assertEquals(firstSnapshot, secondSnapshot)
        assertEquals(beforeRefresh.copy(refreshRevision = 1), firstSnapshot)
        assertEquals(1, firstSnapshot.refreshRevision)

        val repeatedRefresh = first.submit(EnhancementIntent.RefreshOffers)

        assertEquals(firstSnapshot.copy(refreshRevision = 2), repeatedRefresh)
        assertEquals(firstSnapshot.offers, repeatedRefresh.offers)
        assertTrue(repeatedRefresh.allFilterVisible)
        assertTrue(repeatedRefresh.refreshAffordanceVisible)
        assertNull(repeatedRefresh.selectedOfferId)
        assertFalse(repeatedRefresh.returnToBattle)
    }

    @Test
    fun `selecting a visible offer emits return to battle contour`() {
        val session = session()

        val selected = session.submit(
            EnhancementIntent.SelectOffer(OriginalContentIds.FOUNDATION_ENHANCEMENT),
        )

        assertEquals(OriginalContentIds.FOUNDATION_ENHANCEMENT, selected.selectedOfferId)
        assertTrue(selected.returnToBattle)
        assertEquals(selected, session.snapshot())
    }

    @Test
    fun `unknown offer is ignored`() {
        val session = session()
        val before = session.snapshot()

        val unchanged = session.submit(
            EnhancementIntent.SelectOffer(
                offerId = dev.mysd.game.content.ContentId.of("enhancement-unknown"),
            ),
        )

        assertEquals(before, unchanged)
        assertEquals(before, session.snapshot())
    }

    private fun session(): EnhancementSession = EnhancementSession(
        stageId = AcceptedCampaignFixture.STAGE_ID,
        selectedSetupChoice = BattleSetupChoice.OPTION_A,
    )
}
