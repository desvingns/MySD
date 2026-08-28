package dev.mysd.game.service

import java.lang.reflect.Modifier
import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
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
        assertEquals(RewardedOpportunityOutcome.NORMAL_REWARD, firstReward.outcome)
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
        assertEquals(RewardedOpportunityOutcome.MULTIPLIER_SHAPED, multiplier.outcome)
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
        assertEquals(RewardedOpportunityOutcome.NORMAL_REWARD, reward.outcome)
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

    @Test
    fun `reward adapter covers every outcome and availability combination`() {
        val localServices = OfflineServiceAdapters.foundation()
        val localPeerServices = OfflineServiceAdapters.foundation()
        val normalBlockedServices = OfflineServiceAdapters.foundation(
            OfflineServiceConfiguration(
                rewardedOpportunityIds = listOf(OfflineServiceIds.REWARDED_MULTIPLIER),
            ),
        )
        val normalBlockedPeerServices = OfflineServiceAdapters.foundation(
            OfflineServiceConfiguration(
                rewardedOpportunityIds = listOf(OfflineServiceIds.REWARDED_MULTIPLIER),
            ),
        )
        val multiplierBlockedServices = OfflineServiceAdapters.foundation(
            OfflineServiceConfiguration(
                rewardedOpportunityIds = listOf(OfflineServiceIds.REWARDED_CLAIM),
            ),
        )
        val multiplierBlockedPeerServices = OfflineServiceAdapters.foundation(
            OfflineServiceConfiguration(
                rewardedOpportunityIds = listOf(OfflineServiceIds.REWARDED_CLAIM),
            ),
        )

        assertRewardCase(
            service = localServices.rewardedOpportunityService,
            deterministicPeer = localPeerServices.rewardedOpportunityService,
            opportunityId = OfflineServiceIds.REWARDED_CLAIM,
            expectedOutcome = RewardedOpportunityOutcome.NORMAL_REWARD,
            expectedAvailability = LocalServiceAvailability.LOCAL_ONLY,
        )
        assertRewardCase(
            service = normalBlockedServices.rewardedOpportunityService,
            deterministicPeer = normalBlockedPeerServices.rewardedOpportunityService,
            opportunityId = OfflineServiceIds.REWARDED_CLAIM,
            expectedOutcome = RewardedOpportunityOutcome.NORMAL_REWARD,
            expectedAvailability = LocalServiceAvailability.BLOCKED,
        )
        assertRewardCase(
            service = localServices.rewardedOpportunityService,
            deterministicPeer = localPeerServices.rewardedOpportunityService,
            opportunityId = OfflineServiceIds.REWARDED_MULTIPLIER,
            expectedOutcome = RewardedOpportunityOutcome.MULTIPLIER_SHAPED,
            expectedAvailability = LocalServiceAvailability.LOCAL_ONLY,
        )
        assertRewardCase(
            service = multiplierBlockedServices.rewardedOpportunityService,
            deterministicPeer = multiplierBlockedPeerServices.rewardedOpportunityService,
            opportunityId = OfflineServiceIds.REWARDED_MULTIPLIER,
            expectedOutcome = RewardedOpportunityOutcome.MULTIPLIER_SHAPED,
            expectedAvailability = LocalServiceAvailability.BLOCKED,
        )
    }

    @Test
    fun `configuration and snapshot collections reject mutation without changing results`() {
        val premiumProduct = ServiceRequestId.of("purchase-premium")
        val configuration = OfflineServiceConfiguration(
            purchaseProductIds = listOf(OfflineServiceIds.PURCHASE_STARTER, premiumProduct),
        )
        val services = OfflineServiceAdapters.foundation(configuration)

        val rewardBefore = services.rewardedOpportunityService.request(
            RewardedOpportunityRequest(OfflineServiceIds.REWARDED_CLAIM),
        )
        val purchaseBefore = services.purchaseCatalogService.requestPurchase(
            PurchaseRequest(OfflineServiceIds.PURCHASE_STARTER),
        )
        val arenaBefore = services.arenaService.request(ArenaRequest())

        assertFailsWith<UnsupportedOperationException> {
            (configuration.rewardedOpportunityIds as MutableList<ServiceRequestId>).clear()
        }
        assertFailsWith<UnsupportedOperationException> {
            (configuration.purchaseProductIds as MutableList<ServiceRequestId>).clear()
        }
        assertFailsWith<UnsupportedOperationException> {
            (configuration.arenaRequestIds as MutableList<ServiceRequestId>).clear()
        }

        assertEquals(
            rewardBefore,
            services.rewardedOpportunityService.request(
                RewardedOpportunityRequest(OfflineServiceIds.REWARDED_CLAIM),
            ),
        )
        assertEquals(
            purchaseBefore,
            services.purchaseCatalogService.requestPurchase(
                PurchaseRequest(OfflineServiceIds.PURCHASE_STARTER),
            ),
        )
        assertEquals(arenaBefore, services.arenaService.request(ArenaRequest()))

        val catalogBefore = services.purchaseCatalogService.catalog()
        assertEquals(2, catalogBefore.products.size)
        assertFailsWith<UnsupportedOperationException> {
            (catalogBefore.products as MutableList<PurchaseProductSnapshot>).clear()
        }
        assertEquals(catalogBefore, services.purchaseCatalogService.catalog())
    }

    @Test
    fun `purchase catalog snapshot has no mutation bypass and stable value equality`() {
        val trace = OfflineServiceAdapters.foundation().purchaseCatalogService.catalog().trace
        val starter = PurchaseProductSnapshot(
            productId = OfflineServiceIds.PURCHASE_STARTER,
            purchaseAffordanceVisible = true,
            transactionDeferred = true,
        )
        val suppliedProducts = mutableListOf(starter)
        val snapshot = PurchaseCatalogSnapshot(trace, suppliedProducts)
        val expected = PurchaseCatalogSnapshot(trace, listOf(starter))

        suppliedProducts += PurchaseProductSnapshot(
            productId = ServiceRequestId.of("purchase-premium"),
            purchaseAffordanceVisible = true,
            transactionDeferred = true,
        )

        assertEquals(expected, snapshot)
        assertEquals(expected.hashCode(), snapshot.hashCode())
        assertFalse(PurchaseCatalogSnapshot::class.java.methods.any { it.name == "copy" })
        assertEquals(1, snapshot.products.size)
        assertFailsWith<UnsupportedOperationException> {
            (snapshot.products as MutableList<PurchaseProductSnapshot>).clear()
        }
    }

    @Test
    fun `reward snapshots expose immutable final state`() {
        val snapshot = OfflineServiceAdapters.foundation().rewardedOpportunityService.request(
            RewardedOpportunityRequest(OfflineServiceIds.REWARDED_MULTIPLIER),
        )
        val snapshotFields = RewardedOpportunitySnapshot::class.java.declaredFields
            .filterNot { it.isSynthetic }
        val traceFields = LocalServiceTrace::class.java.declaredFields
            .filterNot { it.isSynthetic }

        assertTrue(snapshotFields.isNotEmpty())
        assertTrue(snapshotFields.all { Modifier.isFinal(it.modifiers) })
        assertTrue(traceFields.isNotEmpty())
        assertTrue(traceFields.all { Modifier.isFinal(it.modifiers) })
        assertFalse(RewardedOpportunitySnapshot::class.java.methods.any { it.name.startsWith("set") })
        assertEquals(
            snapshot,
            OfflineServiceAdapters.foundation().rewardedOpportunityService.request(
                RewardedOpportunityRequest(OfflineServiceIds.REWARDED_MULTIPLIER),
            ),
        )
    }

    @Test
    fun `service boundary remains Android-free and excludes production integrations`() {
        val source = sequenceOf(
            Paths.get("src/main/kotlin/dev/mysd/game/service/ServiceBoundary.kt"),
            Paths.get("game/src/main/kotlin/dev/mysd/game/service/ServiceBoundary.kt"),
        ).first { it.toFile().exists() }.toFile().readText()

        assertFalse(
            Regex(
                "(?m)^import\\s+(android(?:x)?|com\\.google\\.android\\.gms|" +
                    "com\\.android\\.billingclient|java\\.net|javax\\.net|" +
                    "okhttp3|retrofit2|io\\.ktor\\.client)\\.",
            ).containsMatchIn(source),
        )
        listOf(
            "MobileAds",
            "BillingClient",
            "RewardedAd",
            "OkHttpClient",
            "Retrofit.Builder",
            "HttpURLConnection",
        ).forEach { forbiddenToken ->
            assertFalse(source.contains(forbiddenToken), "Found forbidden integration token: $forbiddenToken")
        }
    }

    private fun assertRewardCase(
        service: RewardedOpportunityService,
        deterministicPeer: RewardedOpportunityService,
        opportunityId: ServiceRequestId,
        expectedOutcome: RewardedOpportunityOutcome,
        expectedAvailability: LocalServiceAvailability,
    ) {
        val request = RewardedOpportunityRequest(opportunityId)
        val result = service.request(request)

        assertEquals(expectedOutcome, result.outcome)
        assertEquals(expectedOutcome == RewardedOpportunityOutcome.MULTIPLIER_SHAPED, result.multiplierShaped)
        assertEquals(expectedAvailability, result.trace.availability)
        assertEquals(expectedAvailability == LocalServiceAvailability.LOCAL_ONLY, result.trace.affordancePreserved)
        assertFalse(result.completionRequired)
        assertFalse(result.claimApplied)
        assertFalse(result.multiplierApplied)
        assertFalse(result.trace.authoritativeStateChanged)
        assertFalse(result.trace.productionIntegrationAttempted)
        assertFalse(result.trace.networkRequestMade)
        assertEquals(result, service.request(request))
        assertEquals(result, deterministicPeer.request(request))
    }
}
