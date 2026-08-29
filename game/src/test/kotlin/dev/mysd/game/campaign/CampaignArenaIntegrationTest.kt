package dev.mysd.game.campaign

import dev.mysd.game.service.ArenaLocalState
import dev.mysd.game.service.ArenaRequest
import dev.mysd.game.service.ArenaService
import dev.mysd.game.service.ArenaSnapshot
import dev.mysd.game.service.DeterministicLocalArenaService
import dev.mysd.game.service.LocalServiceAvailability
import dev.mysd.game.service.LocalServiceTrace
import dev.mysd.game.service.OfflineServiceIds
import dev.mysd.game.service.ServiceRequestId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CampaignArenaIntegrationTest {
    private val stage = AcceptedCampaignFixture.STAGE_ID

    @Test
    fun `accepted Arena route is integrated through an injected fake service`() {
        val expected = localArenaSnapshot()
        val fake = FakeArenaService(expected)
        val session = CampaignSession(
            acceptedStageIds = listOf(stage),
            unfinishedRun = null,
            arenaService = fake,
        )

        assertEquals(session.snapshot(), session.submit(CampaignIntent.OpenArena))

        session.submit(CampaignIntent.EnterCampaign)
        val opened = session.submit(CampaignIntent.OpenArena)

        assertEquals(CampaignRoute.CAMPAIGN_SELECTION, opened.route)
        assertTrue(opened.arenaOpen)
        assertEquals(expected, session.arenaSnapshot())
        assertEquals(listOf(ArenaRequest()), fake.requests)
        assertFalse(expected.matchRequestEnabled)
        assertFalse(expected.accountRequired)

        assertEquals(opened, session.submit(CampaignIntent.OpenArena))
        assertEquals(1, fake.requests.size)
        assertEquals(opened, session.submit(CampaignIntent.SelectLevel(stage)))

        val closed = session.submit(CampaignIntent.CloseArena)
        assertFalse(closed.arenaOpen)
        assertNull(session.arenaSnapshot())

        val reopened = session.submit(CampaignIntent.OpenArena)
        assertTrue(reopened.arenaOpen)
        assertEquals(expected, session.arenaSnapshot())
        assertEquals(2, fake.requests.size)
        assertEquals(fake.requests[0], fake.requests[1])
    }

    @Test
    fun `Arena route stays blocked by an unfinished-run prompt until the prompt is resolved`() {
        val fake = FakeArenaService(localArenaSnapshot())
        val session = CampaignSession(
            acceptedStageIds = listOf(stage),
            unfinishedRun = UnfinishedCampaignRun(stage),
            arenaService = fake,
        )

        session.submit(CampaignIntent.EnterCampaign)
        val prompt = session.submit(CampaignIntent.OpenArena)

        assertTrue(prompt.unfinishedRunPromptVisible)
        assertFalse(prompt.arenaOpen)
        assertNull(session.arenaSnapshot())
        assertTrue(fake.requests.isEmpty())

        session.submit(CampaignIntent.CancelUnfinishedRun)
        val opened = session.submit(CampaignIntent.OpenArena)

        assertTrue(opened.arenaOpen)
        assertFalse(opened.unfinishedRunPromptVisible)
        assertEquals(1, fake.requests.size)
    }

    @Test
    fun `CampaignSession preserves a blocked service snapshot without enabling account or match behavior`() {
        val blocked = blockedArenaSnapshot()
        val fake = FakeArenaService(blocked)
        val session = CampaignSession(
            acceptedStageIds = listOf(stage),
            unfinishedRun = null,
            arenaService = fake,
        )

        session.submit(CampaignIntent.EnterCampaign)
        val opened = session.submit(CampaignIntent.OpenArena)

        assertTrue(opened.arenaOpen)
        assertEquals(blocked, session.arenaSnapshot())
        assertEquals(ArenaLocalState.NETWORK_MATCH_BLOCKED, blocked.localState)
        assertFalse(blocked.matchRequestEnabled)
        assertFalse(blocked.accountRequired)
        assertFalse(blocked.trace.networkRequestMade)
        assertFalse(blocked.trace.authoritativeStateChanged)
        assertFalse(blocked.trace.productionIntegrationAttempted)
    }

    @Test
    fun `accepted Arena route snapshots are deterministic across equivalent sessions`() {
        val first = AcceptedCampaignFixture.createSession(runSave = null)
        val second = AcceptedCampaignFixture.createSession(runSave = null)

        val firstSnapshots = collectArenaSnapshots(first)
        val secondSnapshots = collectArenaSnapshots(second)

        assertEquals(firstSnapshots, secondSnapshots)
        assertEquals(CampaignRoute.CLEAN_LAUNCH, firstSnapshots[0].route)
        assertEquals(CampaignRoute.CAMPAIGN_SELECTION, firstSnapshots[1].route)
        assertTrue(firstSnapshots[2].arenaOpen)
        assertEquals(first.arenaSnapshot(), second.arenaSnapshot())
        assertNotSame(first.arenaSnapshot(), second.arenaSnapshot())
        assertEquals(ArenaLocalState.LOCAL_SERVICE_SHAPED, first.arenaSnapshot()?.localState)
        assertFalse(first.arenaSnapshot()?.trace?.networkRequestMade ?: true)
        assertFalse(first.arenaSnapshot()?.trace?.authoritativeStateChanged ?: true)
    }

    @Test
    fun `local and blocked Arena states are fresh immutable values with no external behavior`() {
        val service = DeterministicLocalArenaService()
        val acceptedRequest = ArenaRequest(OfflineServiceIds.ARENA_ROUTE)
        val unknownRequest = ArenaRequest(
            routeId = ServiceRequestId.of("unknown-arena-route"),
        )

        val localFirst = service.request(acceptedRequest)
        val localSecond = service.request(acceptedRequest)
        val blockedFirst = service.request(unknownRequest)
        val blockedSecond = service.request(unknownRequest)

        assertEquals(localFirst, localSecond)
        assertEquals(blockedFirst, blockedSecond)
        assertNotSame(localFirst, localSecond)
        assertNotSame(blockedFirst, blockedSecond)
        assertEquals(ArenaLocalState.LOCAL_SERVICE_SHAPED, localFirst.localState)
        assertEquals(ArenaLocalState.NETWORK_MATCH_BLOCKED, blockedFirst.localState)
        assertTrue(localFirst.trace.affordancePreserved)
        assertFalse(blockedFirst.trace.affordancePreserved)

        listOf(localFirst, blockedFirst).forEach { snapshot ->
            assertFalse(snapshot.matchRequestEnabled)
            assertFalse(snapshot.accountRequired)
            assertFalse(snapshot.trace.authoritativeStateChanged)
            assertFalse(snapshot.trace.productionIntegrationAttempted)
            assertFalse(snapshot.trace.networkRequestMade)
        }
    }

    private fun collectArenaSnapshots(session: CampaignSession): List<CampaignSnapshot> = buildList {
        add(session.snapshot())
        add(session.submit(CampaignIntent.EnterCampaign))
        add(session.submit(CampaignIntent.OpenArena))
    }

    private fun localArenaSnapshot(): ArenaSnapshot = ArenaSnapshot(
        trace = LocalServiceTrace(
            serviceId = OfflineServiceIds.ARENA_SERVICE,
            requestId = OfflineServiceIds.ARENA_ROUTE,
            availability = LocalServiceAvailability.LOCAL_ONLY,
            affordancePreserved = true,
            authoritativeStateChanged = false,
            productionIntegrationAttempted = false,
            networkRequestMade = false,
        ),
        localState = ArenaLocalState.LOCAL_SERVICE_SHAPED,
        matchRequestEnabled = false,
        accountRequired = false,
    )

    private fun blockedArenaSnapshot(): ArenaSnapshot = ArenaSnapshot(
        trace = LocalServiceTrace(
            serviceId = OfflineServiceIds.ARENA_SERVICE,
            requestId = ServiceRequestId.of("unknown-arena-route"),
            availability = LocalServiceAvailability.BLOCKED,
            affordancePreserved = false,
            authoritativeStateChanged = false,
            productionIntegrationAttempted = false,
            networkRequestMade = false,
        ),
        localState = ArenaLocalState.NETWORK_MATCH_BLOCKED,
        matchRequestEnabled = false,
        accountRequired = false,
    )

    private class FakeArenaService(
        private val snapshot: ArenaSnapshot,
    ) : ArenaService {
        val requests = mutableListOf<ArenaRequest>()

        override fun request(request: ArenaRequest): ArenaSnapshot {
            requests += request
            return snapshot
        }
    }
}
