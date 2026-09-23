package dev.mysd.game.product.meta

import dev.mysd.game.content.ContentId
import dev.mysd.game.product.MySdRoute
import dev.mysd.game.product.MySdSettingId
import dev.mysd.game.product.MySdTerminalResult
import dev.mysd.game.product.content.OriginalProductCatalog
import dev.mysd.game.product.content.ProductCatalog
import dev.mysd.game.product.content.ProductShopOfferKind
import dev.mysd.game.product.content.ProductStageDefinition
import dev.mysd.game.product.content.ProductTechEffectKind
import kotlin.math.max
import kotlin.math.min

internal enum class ProductLedgerKind {
    INITIAL_GRANT,
    BALANCE_CHECKPOINT,
    ENERGY_REFRESH,
    STAGE_START,
    BATTLE_REWARD,
    SWEEP_REWARD,
    ROSTER_UPGRADE,
    TECHNOLOGY_UNLOCK,
    REWARD_TRACK,
    SHOP,
    ARENA,
}

internal data class ProductLedgerEntry(
    val id: Long,
    val kind: ProductLedgerKind,
    val sourceId: String,
    val softDelta: Long = 0,
    val premiumDelta: Long = 0,
    val energyDelta: Int = 0,
    val rewardTrackDelta: Int = 0,
)

internal data class ProductStageProgress(
    var bestStars: Int = 0,
    var clearCount: Int = 0,
)

internal data class ProductArenaState(
    var opponentFormationId: String = "formation-unscouted",
    var playerPower: Int = 0,
    var opponentPower: Int = 0,
    var result: MySdTerminalResult? = null,
    var runCount: Int = 0,
)

internal class ProductProfileState(
    var profileId: String,
    val sessionSeed: Long,
    var revision: Long,
    var nextEventId: Long,
    var nextRunOrdinal: Long,
    var route: MySdRoute,
    var selectedStageId: ContentId?,
    var serviceMessageCode: String?,
    var softCurrency: Long,
    var premiumShaped: Long,
    var energy: Int,
    val maximumEnergy: Int,
    var lastEnergyEpochSeconds: Long,
    var lastObservedEpochSeconds: Long,
    val unlockedStages: MutableSet<ContentId>,
    val stageProgress: MutableMap<ContentId, ProductStageProgress>,
    val rosterLevels: MutableMap<ContentId, Int>,
    val unlockedRoster: MutableSet<ContentId>,
    val loadoutIds: MutableList<ContentId>,
    val selectedHeroSkillIds: MutableList<ContentId>,
    val unlockedTech: MutableSet<ContentId>,
    var rewardTrackPoints: Int,
    val claimedRewardTiers: MutableSet<ContentId>,
    val claimedBattleRuns: MutableSet<String>,
    val shopPurchases: MutableMap<ContentId, Int>,
    val settings: MutableMap<MySdSettingId, Boolean>,
    val localServiceHistory: MutableList<String>,
    val ledger: MutableList<ProductLedgerEntry>,
    var nextLedgerId: Long,
    val arena: ProductArenaState,
) {
    fun totalStars(): Int = stageProgress.values.sumOf(ProductStageProgress::bestStars)
}

internal data class ProductTechBonuses(
    val towerPowerPermille: Int,
    val allyPowerPermille: Int,
    val economyPermille: Int,
    val heroPowerPermille: Int,
)

internal data class ProductBattleReward(
    val stars: Int,
    val soft: Long,
    val premium: Long,
    val rewardTrackPoints: Int,
)

internal enum class ProductProfileRejection {
    UNKNOWN_CONTENT,
    LOCKED,
    INSUFFICIENT_ENERGY,
    INSUFFICIENT_CURRENCY,
    INVALID_TARGET,
    MAXIMUM_LEVEL,
    PREREQUISITE_MISSING,
    ALREADY_CLAIMED,
    SWEEP_NOT_AVAILABLE,
    SERVICE_STUB,
}

internal sealed interface ProductProfileMutation<out T> {
    data class Applied<T>(val value: T) : ProductProfileMutation<T>
    data class Rejected(val reason: ProductProfileRejection) : ProductProfileMutation<Nothing>
}

/** Authoritative offline profile/economy reducer. Every balance mutation emits a ledger row. */
internal class ProductProfileManager(
    private val catalog: ProductCatalog,
    val state: ProductProfileState,
) {
    fun refreshEnergy(nowEpochSeconds: Long): ProductProfileMutation<Int> {
        if (nowEpochSeconds < 0) return ProductProfileMutation.Rejected(ProductProfileRejection.INVALID_TARGET)
        if (state.lastEnergyEpochSeconds == 0L) {
            state.lastEnergyEpochSeconds = nowEpochSeconds
            state.lastObservedEpochSeconds = nowEpochSeconds
            return ProductProfileMutation.Applied(0)
        }
        if (nowEpochSeconds < state.lastObservedEpochSeconds) {
            return ProductProfileMutation.Rejected(ProductProfileRejection.INVALID_TARGET)
        }
        if (state.energy >= state.maximumEnergy) {
            state.lastEnergyEpochSeconds = nowEpochSeconds
            state.lastObservedEpochSeconds = nowEpochSeconds
            return ProductProfileMutation.Applied(0)
        }
        val elapsed = nowEpochSeconds - state.lastEnergyEpochSeconds
        val missing = state.maximumEnergy - state.energy
        val recovered = min(elapsed / ENERGY_REGEN_SECONDS, missing.toLong()).toInt()
        state.lastObservedEpochSeconds = nowEpochSeconds
        if (recovered <= 0) return ProductProfileMutation.Applied(0)
        val before = state.energy
        state.energy = min(state.maximumEnergy, state.energy + recovered)
        val applied = state.energy - before
        state.lastEnergyEpochSeconds += applied.toLong() * ENERGY_REGEN_SECONDS
        if (state.energy == state.maximumEnergy) state.lastEnergyEpochSeconds = nowEpochSeconds
        ledger(ProductLedgerKind.ENERGY_REFRESH, "clock", energyDelta = applied)
        return ProductProfileMutation.Applied(applied)
    }

    fun spendStageEnergy(stage: ProductStageDefinition): ProductProfileMutation<Unit> {
        if (stage.id !in state.unlockedStages) return ProductProfileMutation.Rejected(ProductProfileRejection.LOCKED)
        if (state.energy < stage.energyCost) {
            return ProductProfileMutation.Rejected(ProductProfileRejection.INSUFFICIENT_ENERGY)
        }
        spendEnergy(stage.energyCost)
        ledger(ProductLedgerKind.STAGE_START, stage.id.value, energyDelta = -stage.energyCost)
        return ProductProfileMutation.Applied(Unit)
    }

    fun replaceLoadout(index: Int, contentId: ContentId): ProductProfileMutation<Unit> {
        if (contentId !in state.rosterLevels || contentId !in catalog.towers && contentId !in catalog.allies) {
            return ProductProfileMutation.Rejected(ProductProfileRejection.UNKNOWN_CONTENT)
        }
        if (contentId !in state.unlockedRoster) return ProductProfileMutation.Rejected(ProductProfileRejection.LOCKED)
        if (index !in 0..state.loadoutIds.size || index >= MAX_LOADOUT_SIZE) {
            return ProductProfileMutation.Rejected(ProductProfileRejection.INVALID_TARGET)
        }
        val candidate = state.loadoutIds.toMutableList()
        val existingIndex = candidate.indexOf(contentId)
        if (existingIndex == index) return ProductProfileMutation.Applied(Unit)
        if (index == candidate.size) {
            if (existingIndex >= 0) return ProductProfileMutation.Applied(Unit)
            candidate += contentId
        } else if (existingIndex >= 0) {
            val displaced = candidate[index]
            candidate[index] = contentId
            candidate[existingIndex] = displaced
        } else {
            candidate[index] = contentId
        }
        if (!validLoadout(candidate)) return ProductProfileMutation.Rejected(ProductProfileRejection.INVALID_TARGET)
        state.loadoutIds.clear()
        state.loadoutIds += candidate
        return ProductProfileMutation.Applied(Unit)
    }

    fun clearLoadout(index: Int): ProductProfileMutation<Unit> {
        if (index !in state.loadoutIds.indices) {
            return ProductProfileMutation.Rejected(ProductProfileRejection.INVALID_TARGET)
        }
        val candidate = state.loadoutIds.toMutableList().also { it.removeAt(index) }
        if (!validLoadout(candidate)) {
            return ProductProfileMutation.Rejected(ProductProfileRejection.INVALID_TARGET)
        }
        state.loadoutIds.removeAt(index)
        return ProductProfileMutation.Applied(Unit)
    }

    fun toggleHeroSkill(skillId: ContentId): ProductProfileMutation<Boolean> {
        if (skillId !in catalog.heroSkills) {
            return ProductProfileMutation.Rejected(ProductProfileRejection.UNKNOWN_CONTENT)
        }
        if (skillId !in state.unlockedRoster) {
            return ProductProfileMutation.Rejected(ProductProfileRejection.LOCKED)
        }
        val selected = skillId !in state.selectedHeroSkillIds
        if (selected && state.selectedHeroSkillIds.size >= MAX_HERO_SKILLS) {
            return ProductProfileMutation.Rejected(ProductProfileRejection.INVALID_TARGET)
        }
        if (selected) {
            state.selectedHeroSkillIds += skillId
            state.selectedHeroSkillIds.sortBy(ContentId::value)
        } else {
            state.selectedHeroSkillIds.remove(skillId)
        }
        return ProductProfileMutation.Applied(selected)
    }

    fun upgradeRoster(contentId: ContentId): ProductProfileMutation<Int> {
        val level = state.rosterLevels[contentId]
            ?: return ProductProfileMutation.Rejected(ProductProfileRejection.UNKNOWN_CONTENT)
        if (contentId !in state.unlockedRoster) return ProductProfileMutation.Rejected(ProductProfileRejection.LOCKED)
        if (level >= MAX_ROSTER_LEVEL) {
            return ProductProfileMutation.Rejected(ProductProfileRejection.MAXIMUM_LEVEL)
        }
        val cost = rosterUpgradeCost(level)
        if (state.softCurrency < cost) {
            return ProductProfileMutation.Rejected(ProductProfileRejection.INSUFFICIENT_CURRENCY)
        }
        state.softCurrency -= cost
        state.rosterLevels[contentId] = level + 1
        ledger(ProductLedgerKind.ROSTER_UPGRADE, contentId.value, softDelta = -cost)
        return ProductProfileMutation.Applied(level + 1)
    }

    fun unlockTech(nodeId: ContentId): ProductProfileMutation<Unit> {
        val node = catalog.techNodes[nodeId]
            ?: return ProductProfileMutation.Rejected(ProductProfileRejection.UNKNOWN_CONTENT)
        if (nodeId in state.unlockedTech) {
            return ProductProfileMutation.Rejected(ProductProfileRejection.ALREADY_CLAIMED)
        }
        if (!state.unlockedTech.containsAll(node.prerequisites)) {
            return ProductProfileMutation.Rejected(ProductProfileRejection.PREREQUISITE_MISSING)
        }
        if (state.softCurrency < node.cost) {
            return ProductProfileMutation.Rejected(ProductProfileRejection.INSUFFICIENT_CURRENCY)
        }
        state.softCurrency -= node.cost
        state.unlockedTech += nodeId
        ledger(ProductLedgerKind.TECHNOLOGY_UNLOCK, nodeId.value, softDelta = -node.cost)
        return ProductProfileMutation.Applied(Unit)
    }

    fun claimRewardTier(tierId: ContentId): ProductProfileMutation<Unit> {
        val tier = catalog.rewardTiers.firstOrNull { it.id == tierId }
            ?: return ProductProfileMutation.Rejected(ProductProfileRejection.UNKNOWN_CONTENT)
        if (tierId in state.claimedRewardTiers) {
            return ProductProfileMutation.Rejected(ProductProfileRejection.ALREADY_CLAIMED)
        }
        if (state.rewardTrackPoints < tier.requiredPoints) {
            return ProductProfileMutation.Rejected(ProductProfileRejection.LOCKED)
        }
        state.claimedRewardTiers += tierId
        val grantedSoft = grantSoft(tier.softCurrency)
        val grantedPremium = grantPremium(tier.premiumShaped)
        val beforeEnergy = state.energy
        state.energy = min(state.maximumEnergy, state.energy + tier.energy)
        discardBankedRechargeIfFull(beforeEnergy)
        ledger(
            ProductLedgerKind.REWARD_TRACK,
            tierId.value,
            softDelta = grantedSoft,
            premiumDelta = grantedPremium,
            energyDelta = state.energy - beforeEnergy,
        )
        return ProductProfileMutation.Applied(Unit)
    }

    fun buyShopOffer(offerId: ContentId): ProductProfileMutation<Unit> {
        val offer = catalog.shopOffers[offerId]
            ?: return ProductProfileMutation.Rejected(ProductProfileRejection.UNKNOWN_CONTENT)
        if (offer.kind == ProductShopOfferKind.REWARDED_STUB || offer.kind == ProductShopOfferKind.PURCHASE_STUB) {
            rememberService("shop:${offer.id.value}:stub")
            return ProductProfileMutation.Rejected(ProductProfileRejection.SERVICE_STUB)
        }
        if (!offer.repeatable && state.shopPurchases.getOrDefault(offerId, 0) > 0) {
            return ProductProfileMutation.Rejected(ProductProfileRejection.ALREADY_CLAIMED)
        }
        if (offer.energyReward > 0 && state.energy >= state.maximumEnergy) {
            return ProductProfileMutation.Rejected(ProductProfileRejection.INVALID_TARGET)
        }
        val cost = offer.softCost ?: 0L
        if (state.softCurrency < cost) {
            return ProductProfileMutation.Rejected(ProductProfileRejection.INSUFFICIENT_CURRENCY)
        }
        state.softCurrency -= cost
        val grantedSoft = grantSoft(offer.softReward)
        val beforeEnergy = state.energy
        state.energy = min(state.maximumEnergy, state.energy + offer.energyReward)
        discardBankedRechargeIfFull(beforeEnergy)
        state.shopPurchases[offerId] = min(MAX_COUNTER, state.shopPurchases.getOrDefault(offerId, 0) + 1)
        ledger(
            ProductLedgerKind.SHOP,
            offerId.value,
            softDelta = grantedSoft - cost,
            energyDelta = state.energy - beforeEnergy,
        )
        return ProductProfileMutation.Applied(Unit)
    }

    fun sweep(stage: ProductStageDefinition): ProductProfileMutation<ProductBattleReward> {
        val progress = state.stageProgress[stage.id]
        if (progress == null || progress.clearCount <= 0 || progress.bestStars < 3) {
            return ProductProfileMutation.Rejected(ProductProfileRejection.SWEEP_NOT_AVAILABLE)
        }
        if (state.energy < stage.energyCost) {
            return ProductProfileMutation.Rejected(ProductProfileRejection.INSUFFICIENT_ENERGY)
        }
        spendEnergy(stage.energyCost)
        val soft = max(1L, stage.reward.softCurrency * SWEEP_REWARD_PERCENT / 100)
        val points = max(1, stage.reward.rewardTrackPoints / 2)
        val grantedSoft = grantSoft(soft)
        val grantedPoints = grantRewardTrackPoints(points)
        progress.clearCount = min(MAX_COUNTER, progress.clearCount + 1)
        ledger(
            ProductLedgerKind.SWEEP_REWARD,
            stage.id.value,
            softDelta = grantedSoft,
            energyDelta = -stage.energyCost,
            rewardTrackDelta = grantedPoints,
        )
        return ProductProfileMutation.Applied(
            ProductBattleReward(progress.bestStars, grantedSoft, 0, grantedPoints),
        )
    }

    fun claimBattleReward(
        runId: String,
        stage: ProductStageDefinition,
        result: MySdTerminalResult,
        baseHealth: Int,
        baseMaxHealth: Int,
    ): ProductProfileMutation<ProductBattleReward> {
        if (runId in state.claimedBattleRuns) {
            return ProductProfileMutation.Rejected(ProductProfileRejection.ALREADY_CLAIMED)
        }
        if (state.claimedBattleRuns.size >= MAX_BATTLE_CLAIMS || runId.isBlank() || runId.length > 128 ||
            stage.id !in state.unlockedStages || baseMaxHealth != stage.baseHealth ||
            baseHealth !in 0..baseMaxHealth ||
            (result == MySdTerminalResult.VICTORY && baseHealth == 0) ||
            (result == MySdTerminalResult.DEFEAT && baseHealth != 0)
        ) return ProductProfileMutation.Rejected(ProductProfileRejection.INVALID_TARGET)
        state.claimedBattleRuns += runId
        if (result == MySdTerminalResult.DEFEAT) {
            val consolation = max(5L, stage.reward.softCurrency / 10)
            val granted = grantSoft(consolation)
            ledger(ProductLedgerKind.BATTLE_REWARD, runId, softDelta = granted)
            return ProductProfileMutation.Applied(ProductBattleReward(0, granted, 0, 0))
        }

        val ratioPermille = if (baseMaxHealth <= 0) 0 else baseHealth * 1_000 / baseMaxHealth
        val stars = 1 +
            (if (ratioPermille >= 500) 1 else 0) +
            (if (ratioPermille >= 800) 1 else 0)
        val progress = state.stageProgress.getOrPut(stage.id) { ProductStageProgress() }
        val firstClear = progress.clearCount == 0
        progress.clearCount = min(MAX_COUNTER, progress.clearCount + 1)
        progress.bestStars = max(progress.bestStars, stars)
        stage.unlocksStageId?.let(state.unlockedStages::add)
        catalog.towers.values.filter { it.unlockAfterStageOrdinal <= stage.ordinal }
            .mapTo(state.unlockedRoster) { it.id }
        catalog.allies.values.filter { it.unlockAfterStageOrdinal <= stage.ordinal }
            .mapTo(state.unlockedRoster) { it.id }
        catalog.heroSkills.values.filter { it.unlockAfterStageOrdinal <= stage.ordinal }
            .mapTo(state.unlockedRoster) { it.id }
        val premium = if (firstClear) stage.reward.firstClearPremiumShaped else 0L
        val grantedSoft = grantSoft(stage.reward.softCurrency)
        val grantedPremium = grantPremium(premium)
        val grantedPoints = grantRewardTrackPoints(stage.reward.rewardTrackPoints)
        ledger(
            ProductLedgerKind.BATTLE_REWARD,
            runId,
            softDelta = grantedSoft,
            premiumDelta = grantedPremium,
            rewardTrackDelta = grantedPoints,
        )
        return ProductProfileMutation.Applied(
            ProductBattleReward(
                stars = stars,
                soft = grantedSoft,
                premium = grantedPremium,
                rewardTrackPoints = grantedPoints,
            ),
        )
    }

    fun toggleSetting(settingId: MySdSettingId): Boolean {
        val next = !(state.settings[settingId] ?: true)
        state.settings[settingId] = next
        return next
    }

    fun rememberService(event: String) {
        if (event.isBlank()) return
        state.localServiceHistory += event
        while (state.localServiceHistory.size > MAX_SERVICE_HISTORY) state.localServiceHistory.removeAt(0)
    }

    fun runArena(): ProductArenaState {
        val playerPower = calculatePlayerPower()
        val ordinal = min(MAX_COUNTER, state.arena.runCount + 1)
        val mixed = mix(state.sessionSeed xor ordinal.toLong() * ARENA_SEED_STEP)
        val variance = ((mixed ushr 1) % 181L).toInt() - 90
        val opponentPower = max(100, playerPower + variance)
        val result = if (playerPower >= opponentPower) MySdTerminalResult.VICTORY else MySdTerminalResult.DEFEAT
        state.arena.opponentFormationId = "formation-${((mixed ushr 12) % 12L) + 1}"
        state.arena.playerPower = playerPower
        state.arena.opponentPower = opponentPower
        state.arena.result = result
        state.arena.runCount = ordinal
        val reward = if (result == MySdTerminalResult.VICTORY) 35L else 10L
        val granted = grantSoft(reward)
        ledger(ProductLedgerKind.ARENA, "arena-$ordinal", softDelta = granted)
        return state.arena.copy()
    }

    fun techBonuses(): ProductTechBonuses {
        var tower = 0
        var ally = 0
        var economy = 0
        var hero = 0
        state.unlockedTech.forEach { nodeId ->
            val node = catalog.techNodes.getValue(nodeId)
            when (node.effectKind) {
                ProductTechEffectKind.TOWER_POWER -> tower += node.magnitudePermille
                ProductTechEffectKind.ALLY_POWER -> ally += node.magnitudePermille
                ProductTechEffectKind.ECONOMY -> economy += node.magnitudePermille
                ProductTechEffectKind.HERO_POWER -> hero += node.magnitudePermille
            }
        }
        return ProductTechBonuses(tower, ally, economy, hero)
    }

    fun calculatePlayerPower(): Int =
        state.unlockedRoster.sumOf { state.rosterLevels.getValue(it) } * 75 +
            state.unlockedTech.size * 110 + state.totalStars() * 40

    fun secondsUntilNextEnergy(): Int {
        if (state.energy >= state.maximumEnergy) return 0
        if (state.lastEnergyEpochSeconds == 0L) return ENERGY_REGEN_SECONDS.toInt()
        val elapsed = max(0L, state.lastObservedEpochSeconds - state.lastEnergyEpochSeconds)
        val remainder = elapsed % ENERGY_REGEN_SECONDS
        return (ENERGY_REGEN_SECONDS - remainder).toInt()
    }

    private fun ledger(
        kind: ProductLedgerKind,
        sourceId: String,
        softDelta: Long = 0,
        premiumDelta: Long = 0,
        energyDelta: Int = 0,
        rewardTrackDelta: Int = 0,
    ) {
        if (state.ledger.size >= MAX_LEDGER_ENTRIES) {
            // A deterministic prefix checkpoint retains exact balance conservation without an
            // unbounded save document. Recent transactions keep their original IDs and details.
            val count = MAX_LEDGER_ENTRIES / 2
            val prefix = state.ledger.take(count)
            val checkpoint = ProductLedgerEntry(
                id = prefix.last().id,
                kind = ProductLedgerKind.BALANCE_CHECKPOINT,
                sourceId = "ledger-prefix-${prefix.last().id}",
                softDelta = prefix.sumOf(ProductLedgerEntry::softDelta),
                premiumDelta = prefix.sumOf(ProductLedgerEntry::premiumDelta),
                energyDelta = prefix.sumOf(ProductLedgerEntry::energyDelta),
                rewardTrackDelta = prefix.sumOf(ProductLedgerEntry::rewardTrackDelta),
            )
            state.ledger.subList(0, count).clear()
            state.ledger.add(0, checkpoint)
        }
        state.ledger += ProductLedgerEntry(
            id = state.nextLedgerId++,
            kind = kind,
            sourceId = sourceId,
            softDelta = softDelta,
            premiumDelta = premiumDelta,
            energyDelta = energyDelta,
            rewardTrackDelta = rewardTrackDelta,
        )
    }

    private fun spendEnergy(amount: Int) {
        val wasFull = state.energy == state.maximumEnergy
        state.energy -= amount
        if (wasFull) state.lastEnergyEpochSeconds = state.lastObservedEpochSeconds
    }

    private fun discardBankedRechargeIfFull(beforeEnergy: Int) {
        if (beforeEnergy < state.maximumEnergy && state.energy == state.maximumEnergy) {
            state.lastEnergyEpochSeconds = state.lastObservedEpochSeconds
        }
    }

    private fun grantSoft(requested: Long): Long {
        val granted = min(requested, MAX_CURRENCY - state.softCurrency)
        state.softCurrency += granted
        return granted
    }

    private fun grantPremium(requested: Long): Long {
        val granted = min(requested, MAX_CURRENCY - state.premiumShaped)
        state.premiumShaped += granted
        return granted
    }

    private fun grantRewardTrackPoints(requested: Int): Int {
        val granted = min(requested, MAX_REWARD_TRACK_POINTS - state.rewardTrackPoints)
        state.rewardTrackPoints += granted
        return granted
    }

    private fun validLoadout(candidate: List<ContentId>): Boolean =
        candidate.size in 2..MAX_LOADOUT_SIZE &&
            candidate.distinct().size == candidate.size &&
            candidate.all { it in catalog.towers || it in catalog.allies } &&
            state.unlockedRoster.containsAll(candidate) &&
            candidate.any(catalog.towers::containsKey) &&
            candidate.any(catalog.allies::containsKey)

    companion object {
        const val MAX_ROSTER_LEVEL: Int = 5
        const val MAX_LOADOUT_SIZE: Int = 7
        const val MAX_HERO_SKILLS: Int = 2
        const val MAX_SERVICE_HISTORY: Int = 100
        const val MAX_LEDGER_ENTRIES: Int = 1_000
        const val MAX_BATTLE_CLAIMS: Int = 10_000
        const val MAX_COUNTER: Int = 1_000_000_000
        const val MAX_REWARD_TRACK_POINTS: Int = 1_000_000_000
        const val MAX_CURRENCY: Long = 9_000_000_000_000_000L
        const val DEFAULT_MAX_ENERGY: Int = 10
        const val ENERGY_REGEN_SECONDS: Long = 300
        private const val SWEEP_REWARD_PERCENT: Long = 60
        private const val ARENA_SEED_STEP: Long = -7046029254386353131L

        fun rosterUpgradeCost(currentLevel: Int): Long = 45L + currentLevel * currentLevel * 25L

        fun create(seed: Long, catalog: ProductCatalog = OriginalProductCatalog.releaseOne()): ProductProfileManager {
            val roster = (catalog.towers.keys + catalog.allies.keys + catalog.heroSkills.keys)
                .sortedBy(ContentId::value)
            val defaultLoadout = catalog.towers.keys.sortedBy(ContentId::value) +
                catalog.allies.keys.sortedBy(ContentId::value)
            val unlockedRoster = buildSet {
                catalog.towers.values.filter { it.unlockAfterStageOrdinal == 0 }.forEach { add(it.id) }
                catalog.allies.values.filter { it.unlockAfterStageOrdinal == 0 }.forEach { add(it.id) }
                catalog.heroSkills.values.filter { it.unlockAfterStageOrdinal == 0 }.forEach { add(it.id) }
            }
            val initialLoadout = defaultLoadout.filter(unlockedRoster::contains)
            check(initialLoadout.any(catalog.towers::containsKey) && initialLoadout.any(catalog.allies::containsKey))
            val firstStage = catalog.orderedStages.first()
            val state = ProductProfileState(
                profileId = "local-${seed.toULong().toString(16)}",
                sessionSeed = seed,
                revision = 0,
                nextEventId = 1,
                nextRunOrdinal = 1,
                route = MySdRoute.HOME,
                selectedStageId = firstStage.id,
                serviceMessageCode = null,
                softCurrency = 500,
                premiumShaped = 3,
                energy = DEFAULT_MAX_ENERGY,
                maximumEnergy = DEFAULT_MAX_ENERGY,
                lastEnergyEpochSeconds = 0,
                lastObservedEpochSeconds = 0,
                unlockedStages = mutableSetOf(firstStage.id),
                stageProgress = mutableMapOf(),
                rosterLevels = roster.associateWith { 1 }.toMutableMap(),
                unlockedRoster = unlockedRoster.toMutableSet(),
                loadoutIds = initialLoadout.toMutableList(),
                selectedHeroSkillIds = catalog.heroSkills.keys.filter(unlockedRoster::contains)
                    .sortedBy(ContentId::value).toMutableList(),
                unlockedTech = mutableSetOf(),
                rewardTrackPoints = 0,
                claimedRewardTiers = mutableSetOf(),
                claimedBattleRuns = mutableSetOf(),
                shopPurchases = mutableMapOf(),
                settings = mutableMapOf(
                    MySdSettingId.SOUND to true,
                    MySdSettingId.MUSIC to true,
                    MySdSettingId.HAPTICS to true,
                    MySdSettingId.REDUCE_MOTION to false,
                ),
                localServiceHistory = mutableListOf(),
                ledger = mutableListOf(
                    ProductLedgerEntry(
                        id = 1,
                        kind = ProductLedgerKind.INITIAL_GRANT,
                        sourceId = "release-one",
                        softDelta = 500,
                        premiumDelta = 3,
                        energyDelta = DEFAULT_MAX_ENERGY,
                    ),
                ),
                nextLedgerId = 2,
                arena = ProductArenaState(),
            )
            return ProductProfileManager(catalog, state)
        }

        private fun mix(input: Long): Long {
            var value = input
            value = (value xor (value ushr 30)) * -4658895280553007687L
            value = (value xor (value ushr 27)) * -7723592293110705685L
            return value xor (value ushr 31)
        }
    }
}
