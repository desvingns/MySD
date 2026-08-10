package dev.mysd.game.service

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotSame
import kotlin.test.assertTrue

class ServiceBoundaryTest {
    @Test
    fun `foundation bundle handles accepted requests deterministically and offline`() {
        val first = OfflineServiceAdapters.foundation()
        val second = OfflineServiceAdapters.foundation()

        val firstReward = first.rewardedOpportunityService.request(
            RewardedOpportunityRequest(OfflineServiceIds.REWARDED_CLAIM),
        )
        val secondReward = second.rewardedOpportunityService.request(
            RewardedOpportunityRequest(OfflineServiceIds.REWARDED_CLAIM),
        )
        assertEquals(firstReward, secondReward)
        assertEquals(OfflineServiceIds.REWARDED_SERVICE, firstReward.trace.serviceId)
        assertEquals(LocalServiceAvailability.LOCAL_ONLY, firstReward.trace.availability)
        assertTrue(firstReward.trace.affordancePreserved)
        assertFalse(firstReward.trace.authoritativeStateChanged)
        assertFalse(firstReward.trace.productionIntegrationAttempted)
        assertFalse(firstReward.trace.networkRequestMade)
        assertFalse(firstReward.claimApplied)
        assertFalse(firstReward.multiplierApplied)

        val firstCatalog = first.purchaseCatalogService.catalog()
        val secondCatalog = second.purchaseCatalogService.catalog()
        assertEquals(firstCatalog, secondCatalog)
        assertEquals(LocalServiceAvailability.LOCAL_ONLY, firstCatalog.trace.availability)
        assertEquals(OfflineServiceIds.PURCHASE_SERVICE, firstCatalog.trace.serviceId)
        assertTrue(firstCatalog.products.single().purchaseAffordanceVisible)
        assertTrue(firstCatalog.products.single().transactionDeferred)
        assertNotSame(firstCatalog.products, secondCatalog.products)

        val purchase = first.purchaseCatalogService.requestPurchase(
            PurchaseRequest(OfflineServiceIds.PURCHASE_STARTER),
        )
        assertEquals(LocalServiceAvailability.LOCAL_ONLY, purchase.trace.availability)
        assertTrue(purchase.transactionDeferred)
        assertFalse(purchase.transactionApplied)

        val arena = first.arenaService.request(ArenaRequest())
        assertEquals(LocalServiceAvailability.LOCAL_ONLY, arena.trace.availability)
        assertEquals(ArenaLocalState.LOCAL_SERVICE_SHAPED, arena.localState)
        assertEquals(OfflineServiceIds.ARENA_SERVICE, arena.trace.serviceId)
        assertFalse(arena.matchRequestEnabled)
        assertFalse(arena.accountRequired)
        assertFalse(arena.trace.networkRequestMade)
    }

    @Test
    fun `multiplier-shaped and arena requests preserve shape without applying semantics`() {
        val services = OfflineServiceAdapters.foundation()

        val multiplier = services.rewardedOpportunityService.request(
            RewardedOpportunityRequest(OfflineServiceIds.REWARDED_MULTIPLIER),
        )
        assertTrue(multiplier.multiplierShaped)
        assertTrue(multiplier.trace.affordancePreserved)
        assertFalse(multiplier.completionRequired)
        assertFalse(multiplier.multiplierApplied)

        val arena = services.arenaService.request(
            ArenaRequest(OfflineServiceIds.ARENA_ROUTE),
        )
        assertEquals(ArenaLocalState.LOCAL_SERVICE_SHAPED, arena.localState)
        assertFalse(arena.matchRequestEnabled)
        assertFalse(arena.accountRequired)
    }

    @Test
    fun `unconfigured requests are blocked without external integration`() {
        val services = OfflineServiceAdapters.foundation()
        val unknown = ServiceRequestId.of("unknown-service-request")

        val reward = services.rewardedOpportunityService.request(
            RewardedOpportunityRequest(unknown),
        )
        assertEquals(LocalServiceAvailability.BLOCKED, reward.trace.availability)
        assertFalse(reward.trace.affordancePreserved)
        assertFalse(reward.trace.productionIntegrationAttempted)
        assertFalse(reward.trace.networkRequestMade)

        val catalog = services.purchaseCatalogService.catalog(PurchaseCatalogRequest(unknown))
        assertEquals(LocalServiceAvailability.BLOCKED, catalog.trace.availability)
        assertTrue(catalog.products.isEmpty())

        val purchase = services.purchaseCatalogService.requestPurchase(PurchaseRequest(unknown))
        assertEquals(LocalServiceAvailability.BLOCKED, purchase.trace.availability)
        assertFalse(purchase.transactionApplied)

        val arena = services.arenaService.request(ArenaRequest(unknown))
        assertEquals(LocalServiceAvailability.BLOCKED, arena.trace.availability)
        assertEquals(ArenaLocalState.NETWORK_MATCH_BLOCKED, arena.localState)
        assertFalse(arena.trace.networkRequestMade)
    }
}
