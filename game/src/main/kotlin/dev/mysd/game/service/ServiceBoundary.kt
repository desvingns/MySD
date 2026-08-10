package dev.mysd.game.service

import java.util.ArrayList
import java.util.Collections

/** Stable identifier for a service-shaped command or local catalog entry. */
@JvmInline
value class ServiceRequestId private constructor(val value: String) {
    companion object {
        private const val MAX_LENGTH = 64
        private val FORMAT = Regex("[a-z][a-z0-9]*(?:-[a-z0-9]+)*")

        fun of(raw: String): ServiceRequestId {
            require(raw.length <= MAX_LENGTH && FORMAT.matches(raw)) {
                "Service request id must be stable and machine-readable."
            }
            return ServiceRequestId(raw)
        }
    }
}

/** Original MySD identifiers used by the first-release local service fixtures. */
object OfflineServiceIds {
    val REWARDED_SERVICE = ServiceRequestId.of("rewarded-opportunity-service")
    val PURCHASE_SERVICE = ServiceRequestId.of("purchase-catalog-service")
    val ARENA_SERVICE = ServiceRequestId.of("arena-service")
    val REWARDED_CLAIM = ServiceRequestId.of("rewarded-claim")
    val REWARDED_MULTIPLIER = ServiceRequestId.of("rewarded-multiplier")
    val PURCHASE_CATALOG = ServiceRequestId.of("purchase-catalog")
    val PURCHASE_STARTER = ServiceRequestId.of("purchase-starter")
    val ARENA_ROUTE = ServiceRequestId.of("arena-route")
}

/** The only externally visible outcomes supported by the first-release boundary. */
enum class LocalServiceAvailability {
    LOCAL_ONLY,
    BLOCKED,
}

/** Immutable evidence that a request stayed inside the deterministic local boundary. */
data class LocalServiceTrace(
    val serviceId: ServiceRequestId,
    val requestId: ServiceRequestId,
    val availability: LocalServiceAvailability,
    val affordancePreserved: Boolean,
    val authoritativeStateChanged: Boolean,
    val productionIntegrationAttempted: Boolean,
    val networkRequestMade: Boolean,
) {
    init {
        require(!productionIntegrationAttempted) {
            "Production service integration is outside the first-release boundary."
        }
        require(!networkRequestMade) {
            "Network service requests are outside the first-release boundary."
        }
        require(!authoritativeStateChanged) {
            "Local service requests cannot change authoritative game state."
        }
        if (availability == LocalServiceAvailability.BLOCKED) {
            require(!affordancePreserved) {
                "A blocked service request cannot claim to preserve its affordance."
            }
        }
    }
}

/** Deterministic fixture configuration; values are IDs only, never display content or balance. */
class OfflineServiceConfiguration(
    rewardedOpportunityIds: List<ServiceRequestId> = listOf(
        OfflineServiceIds.REWARDED_CLAIM,
        OfflineServiceIds.REWARDED_MULTIPLIER,
    ),
    purchaseProductIds: List<ServiceRequestId> = listOf(OfflineServiceIds.PURCHASE_STARTER),
    arenaRequestIds: List<ServiceRequestId> = listOf(OfflineServiceIds.ARENA_ROUTE),
) {
    val rewardedOpportunityIds: List<ServiceRequestId> = runtimeUnmodifiableCopy(rewardedOpportunityIds)
    val purchaseProductIds: List<ServiceRequestId> = runtimeUnmodifiableCopy(purchaseProductIds)
    val arenaRequestIds: List<ServiceRequestId> = runtimeUnmodifiableCopy(arenaRequestIds)

    init {
        requireUniqueNonEmpty(this.rewardedOpportunityIds, "rewarded opportunities")
        requireUniqueNonEmpty(this.purchaseProductIds, "purchase products")
        requireUniqueNonEmpty(this.arenaRequestIds, "Arena requests")
    }

    private fun requireUniqueNonEmpty(ids: List<ServiceRequestId>, field: String) {
        require(ids.isNotEmpty()) { "Offline $field must not be empty." }
        require(ids.size == ids.distinct().size) { "Offline $field must be unique." }
    }
}

data class RewardedOpportunityRequest(
    val opportunityId: ServiceRequestId,
)

data class RewardedOpportunitySnapshot(
    val trace: LocalServiceTrace,
    val multiplierShaped: Boolean,
    val completionRequired: Boolean,
    val claimApplied: Boolean,
    val multiplierApplied: Boolean,
) {
    init {
        require(!completionRequired) {
            "Real ad completion is deferred from the local service boundary."
        }
        require(!claimApplied && !multiplierApplied) {
            "Reward claim and multiplier semantics are deferred."
        }
    }
}

interface RewardedOpportunityService {
    fun request(request: RewardedOpportunityRequest): RewardedOpportunitySnapshot
}

data class PurchaseCatalogRequest(
    val catalogId: ServiceRequestId = OfflineServiceIds.PURCHASE_CATALOG,
)

data class PurchaseProductSnapshot(
    val productId: ServiceRequestId,
    val purchaseAffordanceVisible: Boolean,
    val transactionDeferred: Boolean,
)

data class PurchaseCatalogSnapshot private constructor(
    val trace: LocalServiceTrace,
    val products: List<PurchaseProductSnapshot>,
) {
    init {
        require(products.map { it.productId }.distinct().size == products.size) {
            "Purchase catalog product ids must be unique."
        }
        require(products.all { it.transactionDeferred }) {
            "Purchase transactions are deferred from the local catalog boundary."
        }
    }

    companion object {
        operator fun invoke(
            trace: LocalServiceTrace,
            products: List<PurchaseProductSnapshot>,
        ): PurchaseCatalogSnapshot = PurchaseCatalogSnapshot(
            trace = trace,
            products = runtimeUnmodifiableCopy(products),
        )
    }
}

data class PurchaseRequest(
    val productId: ServiceRequestId,
)

data class PurchaseResult(
    val trace: LocalServiceTrace,
    val transactionDeferred: Boolean,
    val transactionApplied: Boolean,
) {
    init {
        require(transactionDeferred) { "Purchase transaction semantics are deferred." }
        require(!transactionApplied) { "A local purchase adapter cannot apply a transaction." }
    }
}

interface PurchaseCatalogService {
    fun catalog(request: PurchaseCatalogRequest = PurchaseCatalogRequest()): PurchaseCatalogSnapshot

    fun requestPurchase(request: PurchaseRequest): PurchaseResult
}

data class ArenaRequest(
    val routeId: ServiceRequestId = OfflineServiceIds.ARENA_ROUTE,
)

enum class ArenaLocalState {
    LOCAL_SERVICE_SHAPED,
    NETWORK_MATCH_BLOCKED,
}

data class ArenaSnapshot(
    val trace: LocalServiceTrace,
    val localState: ArenaLocalState,
    val matchRequestEnabled: Boolean,
    val accountRequired: Boolean,
) {
    init {
        require(!matchRequestEnabled) { "Network Arena match behavior is excluded." }
        require(!accountRequired) { "Account behavior is excluded from the local boundary." }
    }
}

interface ArenaService {
    fun request(request: ArenaRequest): ArenaSnapshot
}

/** The service bundle used by the first Android release. It contains no production integrations. */
data class OfflineServiceBundle(
    val rewardedOpportunityService: RewardedOpportunityService,
    val purchaseCatalogService: PurchaseCatalogService,
    val arenaService: ArenaService,
)

object OfflineServiceAdapters {
    fun foundation(
        configuration: OfflineServiceConfiguration = OfflineServiceConfiguration(),
    ): OfflineServiceBundle = OfflineServiceBundle(
        rewardedOpportunityService = DeterministicLocalRewardedOpportunityService(configuration),
        purchaseCatalogService = DeterministicLocalPurchaseCatalogService(configuration),
        arenaService = DeterministicLocalArenaService(configuration),
    )
}

class DeterministicLocalRewardedOpportunityService(
    private val configuration: OfflineServiceConfiguration = OfflineServiceConfiguration(),
) : RewardedOpportunityService {
    override fun request(request: RewardedOpportunityRequest): RewardedOpportunitySnapshot {
        val accepted = request.opportunityId in configuration.rewardedOpportunityIds
        return RewardedOpportunitySnapshot(
            trace = trace(
                serviceId = OfflineServiceIds.REWARDED_SERVICE,
                requestId = request.opportunityId,
                accepted = accepted,
            ),
            multiplierShaped = accepted && request.opportunityId == OfflineServiceIds.REWARDED_MULTIPLIER,
            completionRequired = false,
            claimApplied = false,
            multiplierApplied = false,
        )
    }
}

class DeterministicLocalPurchaseCatalogService(
    private val configuration: OfflineServiceConfiguration = OfflineServiceConfiguration(),
) : PurchaseCatalogService {
    override fun catalog(request: PurchaseCatalogRequest): PurchaseCatalogSnapshot {
        val accepted = request.catalogId == OfflineServiceIds.PURCHASE_CATALOG
        val products = if (accepted) {
            configuration.purchaseProductIds
                .sortedBy(ServiceRequestId::value)
                .map { productId ->
                    PurchaseProductSnapshot(
                        productId = productId,
                        purchaseAffordanceVisible = true,
                        transactionDeferred = true,
                    )
                }
        } else {
            emptyList()
        }
        return PurchaseCatalogSnapshot(
            trace = trace(
                serviceId = OfflineServiceIds.PURCHASE_SERVICE,
                requestId = request.catalogId,
                accepted = accepted,
            ),
            products = runtimeUnmodifiableCopy(products),
        )
    }

    override fun requestPurchase(request: PurchaseRequest): PurchaseResult {
        val accepted = request.productId in configuration.purchaseProductIds
        return PurchaseResult(
            trace = trace(
                serviceId = OfflineServiceIds.PURCHASE_SERVICE,
                requestId = request.productId,
                accepted = accepted,
            ),
            transactionDeferred = true,
            transactionApplied = false,
        )
    }
}

class DeterministicLocalArenaService(
    private val configuration: OfflineServiceConfiguration = OfflineServiceConfiguration(),
) : ArenaService {
    override fun request(request: ArenaRequest): ArenaSnapshot {
        val accepted = request.routeId in configuration.arenaRequestIds
        return ArenaSnapshot(
            trace = trace(
                serviceId = OfflineServiceIds.ARENA_SERVICE,
                requestId = request.routeId,
                accepted = accepted,
            ),
            localState = if (accepted) {
                ArenaLocalState.LOCAL_SERVICE_SHAPED
            } else {
                ArenaLocalState.NETWORK_MATCH_BLOCKED
            },
            matchRequestEnabled = false,
            accountRequired = false,
        )
    }
}

private fun trace(
    serviceId: ServiceRequestId,
    requestId: ServiceRequestId,
    accepted: Boolean,
): LocalServiceTrace = LocalServiceTrace(
    serviceId = serviceId,
    requestId = requestId,
    availability = if (accepted) {
        LocalServiceAvailability.LOCAL_ONLY
    } else {
        LocalServiceAvailability.BLOCKED
    },
    affordancePreserved = accepted,
    authoritativeStateChanged = false,
    productionIntegrationAttempted = false,
    networkRequestMade = false,
)

private fun <T> runtimeUnmodifiableCopy(values: List<T>): List<T> =
    Collections.unmodifiableList(ArrayList(values))
