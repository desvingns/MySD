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
        assertEquals(listOf(OriginalContentIds.FOUNDATION_ENHANCEMENT), snapshot.offers.map { it.id })
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

        val firstSnapshot = first.submit(EnhancementIntent.RefreshOffers)
        val secondSnapshot = second.submit(EnhancementIntent.RefreshOffers)

        assertEquals(firstSnapshot, secondSnapshot)
        assertEquals(1, firstSnapshot.refreshRevision)
        assertEquals(
            listOf(OriginalContentIds.FOUNDATION_ENHANCEMENT),
            firstSnapshot.offers.map { it.id },
        )
        assertNull(firstSnapshot.selectedOfferId)
        assertFalse(firstSnapshot.returnToBattle)
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

        val unchanged = session.submit(
            EnhancementIntent.SelectOffer(
                offerId = dev.mysd.game.content.ContentId.of("enhancement-unknown"),
            ),
        )

        assertNull(unchanged.selectedOfferId)
        assertFalse(unchanged.returnToBattle)
    }

    private fun session(): EnhancementSession = EnhancementSession(
        stageId = AcceptedCampaignFixture.STAGE_ID,
        selectedSetupChoice = BattleSetupChoice.OPTION_A,
    )
}
