package dev.mysd.game.product.meta

import dev.mysd.game.product.MySdTerminalResult
import dev.mysd.game.product.content.OriginalProductCatalog
import dev.mysd.game.product.persistence.ProductProfileCodec
import dev.mysd.game.product.persistence.ProductProfileDecodeResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ProductProfileManagerTest {
    private val catalog = OriginalProductCatalog.releaseOne()

    @Test
    fun economyLedgerReconcilesEveryAuthoritativeBalance() {
        val manager = ProductProfileManager.create(seed = 51, catalog)
        val stage = catalog.orderedStages.first()
        assertIs<ProductProfileMutation.Applied<Unit>>(manager.spendStageEnergy(stage))
        assertIs<ProductProfileMutation.Applied<Int>>(manager.upgradeRoster(OriginalProductCatalog.TOWER_RAPID))
        assertIs<ProductProfileMutation.Applied<ProductArenaState>>(manager.runArena().let { ProductProfileMutation.Applied(it) })

        assertEquals(manager.state.softCurrency, manager.state.ledger.sumOf(ProductLedgerEntry::softDelta))
        assertEquals(manager.state.premiumShaped, manager.state.ledger.sumOf(ProductLedgerEntry::premiumDelta))
        assertEquals(manager.state.energy, manager.state.ledger.sumOf(ProductLedgerEntry::energyDelta))
        assertEquals(manager.state.rewardTrackPoints, manager.state.ledger.sumOf(ProductLedgerEntry::rewardTrackDelta))

        val encoded = ProductProfileCodec.encode(manager.state, catalog)
        assertIs<ProductProfileDecodeResult.Decoded>(ProductProfileCodec.decode(encoded, catalog))
    }

    @Test
    fun spendingFromFullStartsRechargeAndFillingToFullDiscardsBankedTime() {
        val manager = ProductProfileManager.create(seed = 52, catalog)
        val stage = catalog.orderedStages.first()
        manager.refreshEnergy(100)
        manager.spendStageEnergy(stage)
        assertEquals(100, manager.state.lastEnergyEpochSeconds)
        assertEquals(8, manager.state.energy)

        manager.refreshEnergy(400)
        assertEquals(9, manager.state.energy)
        assertEquals(400, manager.state.lastEnergyEpochSeconds)

        assertIs<ProductProfileMutation.Applied<Unit>>(
            manager.buyShopOffer(dev.mysd.game.content.ContentId.of("shop-supply-small")),
        )
        assertEquals(10, manager.state.energy)
        assertEquals(400, manager.state.lastEnergyEpochSeconds)

        manager.spendStageEnergy(stage)
        manager.refreshEnergy(699)
        assertEquals(8, manager.state.energy)
        manager.refreshEnergy(700)
        assertEquals(9, manager.state.energy)
    }

    @Test
    fun maliciousClockCannotOverflowRecovery() {
        val manager = ProductProfileManager.create(seed = 53, catalog)
        manager.refreshEnergy(1)
        manager.spendStageEnergy(catalog.orderedStages.first())

        val result = assertIs<ProductProfileMutation.Applied<Int>>(manager.refreshEnergy(Long.MAX_VALUE))
        assertEquals(2, result.value)
        assertEquals(manager.state.maximumEnergy, manager.state.energy)
        assertTrue(manager.state.lastEnergyEpochSeconds >= 0)
        assertTrue(manager.state.lastObservedEpochSeconds >= manager.state.lastEnergyEpochSeconds)
    }

    @Test
    fun fullEnergyOfferRejectsBeforeCurrencyOrLedgerMutation() {
        val manager = ProductProfileManager.create(seed = 54, catalog)
        val beforeSoft = manager.state.softCurrency
        val beforeLedger = manager.state.ledger.toList()

        val result = assertIs<ProductProfileMutation.Rejected>(
            manager.buyShopOffer(dev.mysd.game.content.ContentId.of("shop-supply-small")),
        )
        assertEquals(ProductProfileRejection.INVALID_TARGET, result.reason)
        assertEquals(beforeSoft, manager.state.softCurrency)
        assertEquals(beforeLedger, manager.state.ledger)
    }

    @Test
    fun threeStarVictoryUnlocksNextStageAndNextRosterBand() {
        val manager = ProductProfileManager.create(seed = 55, catalog)
        val stage = catalog.orderedStages.first()
        val reward = assertIs<ProductProfileMutation.Applied<ProductBattleReward>>(
            manager.claimBattleReward(
                runId = "run-test-one",
                stage = stage,
                result = MySdTerminalResult.VICTORY,
                baseHealth = stage.baseHealth,
                baseMaxHealth = stage.baseHealth,
            ),
        ).value

        assertEquals(3, reward.stars)
        assertTrue(stage.unlocksStageId in manager.state.unlockedStages)
        assertTrue(OriginalProductCatalog.TOWER_CONTROL in manager.state.unlockedRoster)
        assertTrue(OriginalProductCatalog.ALLY_SKIRMISHER in manager.state.unlockedRoster)
        assertTrue(OriginalProductCatalog.HERO_PULSE !in manager.state.unlockedRoster)
    }

    @Test
    fun fullCampaignAllRewardTiersAndEntireTechnologyGraphAreClaimableExactlyOnce() {
        val manager = ProductProfileManager.create(56, catalog)
        catalog.orderedStages.forEach { stage ->
            val rewarded = assertIs<ProductProfileMutation.Applied<ProductBattleReward>>(
                manager.claimBattleReward("run-${stage.ordinal}", stage, MySdTerminalResult.VICTORY,
                    stage.baseHealth, stage.baseHealth),
            )
            assertEquals(3, rewarded.value.stars)
        }
        val last = catalog.orderedStages.last()
        repeat(4) { index ->
            manager.claimBattleReward("run-repeat-$index", last, MySdTerminalResult.VICTORY,
                last.baseHealth, last.baseHealth)
        }
        assertEquals(18, manager.state.totalStars())
        assertEquals(manager.state.rosterLevels.keys, manager.state.unlockedRoster)
        catalog.rewardTiers.forEach { tier ->
            assertIs<ProductProfileMutation.Applied<Unit>>(manager.claimRewardTier(tier.id))
            val saved = ProductProfileCodec.encode(manager.state, catalog)
            assertEquals(ProductProfileRejection.ALREADY_CLAIMED,
                assertIs<ProductProfileMutation.Rejected>(manager.claimRewardTier(tier.id)).reason)
            assertEquals(saved, ProductProfileCodec.encode(manager.state, catalog))
        }
        val pending = catalog.techNodes.values.toMutableList()
        while (pending.isNotEmpty()) {
            val node = pending.first { manager.state.unlockedTech.containsAll(it.prerequisites) }
            assertIs<ProductProfileMutation.Applied<Unit>>(manager.unlockTech(node.id))
            assertEquals(ProductProfileRejection.ALREADY_CLAIMED,
                assertIs<ProductProfileMutation.Rejected>(manager.unlockTech(node.id)).reason)
            pending.remove(node)
        }
        assertEquals(12, manager.state.unlockedTech.size)
        val bonuses = manager.techBonuses()
        assertEquals(ProductTechBonuses(300, 300, 300, 300), bonuses)
        assertIs<ProductProfileDecodeResult.Decoded>(ProductProfileCodec.decode(
            ProductProfileCodec.encode(manager.state, catalog), catalog))
    }

    @Test
    fun lockedPrerequisiteAndUnmasteredSweepAreAtomicThenMasteredSweepConservesLedger() {
        val manager = ProductProfileManager.create(57, catalog)
        val child = catalog.techNodes.values.first { it.prerequisites.isNotEmpty() }
        val stage = catalog.orderedStages.first()
        val before = ProductProfileCodec.encode(manager.state, catalog)
        assertEquals(ProductProfileRejection.PREREQUISITE_MISSING,
            assertIs<ProductProfileMutation.Rejected>(manager.unlockTech(child.id)).reason)
        assertEquals(ProductProfileRejection.SWEEP_NOT_AVAILABLE,
            assertIs<ProductProfileMutation.Rejected>(manager.sweep(stage)).reason)
        assertEquals(before, ProductProfileCodec.encode(manager.state, catalog))

        manager.claimBattleReward("run-low-stars", stage, MySdTerminalResult.VICTORY, 1, stage.baseHealth)
        assertEquals(ProductProfileRejection.SWEEP_NOT_AVAILABLE,
            assertIs<ProductProfileMutation.Rejected>(manager.sweep(stage)).reason)
        manager.claimBattleReward("run-three-stars", stage, MySdTerminalResult.VICTORY, stage.baseHealth, stage.baseHealth)
        val energy = manager.state.energy
        val soft = manager.state.softCurrency
        val swept = assertIs<ProductProfileMutation.Applied<ProductBattleReward>>(manager.sweep(stage)).value
        assertEquals(energy - stage.energyCost, manager.state.energy)
        assertEquals(soft + swept.soft, manager.state.softCurrency)
        assertEquals(0L, swept.premium)
        assertEquals(manager.state.energy, manager.state.ledger.sumOf(ProductLedgerEntry::energyDelta))
        assertEquals(manager.state.softCurrency, manager.state.ledger.sumOf(ProductLedgerEntry::softDelta))
    }

    @Test
    fun longLivedLedgerCheckpointsRemainBoundedAndRoundTrip() {
        val manager = ProductProfileManager.create(58, catalog)
        repeat(ProductProfileManager.MAX_LEDGER_ENTRIES * 3) { manager.runArena() }
        assertTrue(manager.state.ledger.size <= ProductProfileManager.MAX_LEDGER_ENTRIES)
        assertEquals(ProductLedgerKind.BALANCE_CHECKPOINT, manager.state.ledger.first().kind)
        assertEquals(manager.state.softCurrency, manager.state.ledger.sumOf(ProductLedgerEntry::softDelta))
        val saved = ProductProfileCodec.encode(manager.state, catalog)
        val restored = assertIs<ProductProfileDecodeResult.Decoded>(ProductProfileCodec.decode(saved, catalog))
        assertEquals(saved, ProductProfileCodec.encode(restored.profile, catalog))
    }

    @Test
    fun saturatedRewardBalancesRecordOnlyActualGrantedAmounts() {
        val manager = ProductProfileManager.create(59, catalog)
        manager.state.softCurrency = ProductProfileManager.MAX_CURRENCY - 1
        manager.state.ledger[0] = manager.state.ledger[0].copy(softDelta = manager.state.softCurrency)
        val stage = catalog.orderedStages.first()
        val reward = assertIs<ProductProfileMutation.Applied<ProductBattleReward>>(
            manager.claimBattleReward("run-cap", stage, MySdTerminalResult.VICTORY, stage.baseHealth, stage.baseHealth),
        ).value
        assertEquals(1L, reward.soft)
        assertEquals(ProductProfileManager.MAX_CURRENCY, manager.state.softCurrency)
        assertEquals(1L, manager.state.ledger.last().softDelta)
        assertIs<ProductProfileDecodeResult.Decoded>(ProductProfileCodec.decode(
            ProductProfileCodec.encode(manager.state, catalog), catalog))
    }

    @Test
    fun clockRollbackDoesNotMoveRechargeCountdownBackwards() {
        val manager = ProductProfileManager.create(60, catalog)
        manager.refreshEnergy(1_000)
        manager.spendStageEnergy(catalog.orderedStages.first())
        manager.refreshEnergy(1_200)
        val before = ProductProfileCodec.encode(manager.state, catalog)
        assertEquals(ProductProfileRejection.INVALID_TARGET,
            assertIs<ProductProfileMutation.Rejected>(manager.refreshEnergy(1_100)).reason)
        assertEquals(before, ProductProfileCodec.encode(manager.state, catalog))
    }
}
