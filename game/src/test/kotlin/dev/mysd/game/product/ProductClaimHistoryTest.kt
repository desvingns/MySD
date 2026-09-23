package dev.mysd.game.product

import dev.mysd.game.product.content.OriginalProductCatalog
import dev.mysd.game.product.meta.ProductProfileManager
import dev.mysd.game.product.meta.ProductProfileMutation
import dev.mysd.game.product.meta.ProductProfileRejection
import dev.mysd.game.product.meta.ProductProfileState
import dev.mysd.game.product.persistence.ProductProfileCodec
import dev.mysd.game.product.persistence.ProductProfileDecodeResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ProductClaimHistoryTest {
    private val catalog = OriginalProductCatalog.releaseOne()
    private val stage = catalog.orderedStages.first()

    @Test
    fun restoredUnclaimedTerminalAtHistoryCapacityGrantsExactRewardAndCanExit() {
        listOf(false, true).forEach { defend ->
            val original = started(seed = if (defend) 11L else 10L)
            finish(original, defend)
            val terminal = fullHistory(original.saveBundle())
            val restored = restore(terminal)
            val before = restored.snapshot()
            val reward = assertIs<MySdOverlaySnapshot.TerminalReward>(before.overlay)
            val beforeProfile = decode(terminal)
            val runId = assertNotNull(before.battle).runId
            assertFalse(reward.claimed)
            assertEquals(ProductProfileManager.MAX_BATTLE_CLAIMS, beforeProfile.claimedBattleRuns.size)

            val claim = restored.submit(MySdAppIntent.ClaimBattleReward)

            assertTrue(claim.accepted)
            assertEquals(before.profile.currencies.soft + reward.softReward, claim.snapshot.profile.currencies.soft)
            val premium = if (defend) stage.reward.firstClearPremiumShaped else 0L
            assertEquals(before.profile.currencies.premiumShaped + premium, claim.snapshot.profile.currencies.premiumShaped)
            assertEquals(before.profile.rewardTrackPoints + reward.rewardTrackPoints, claim.snapshot.profile.rewardTrackPoints)
            assertEquals(before.profile.energy, claim.snapshot.profile.energy)
            val claimedBundle = restored.saveBundle()
            val claimedProfile = decode(claimedBundle)
            assertEquals(setOf(runId), claimedProfile.claimedBattleRuns)
            assertEquals(beforeProfile.nextLedgerId + 1, claimedProfile.nextLedgerId)
            assertEquals(runId, claimedProfile.ledger.last().sourceId)
            assertEquals(terminal.runSave, claimedBundle.runSave)
            assertDuplicateUnchanged(restored)
            val relaunched = restore(claimedBundle)
            assertDuplicateUnchanged(relaunched)
            assertTrue(relaunched.submit(MySdAppIntent.AbandonRun).accepted)
            assertTrue(relaunched.submit(MySdAppIntent.SelectStage(stage.id.value)).accepted)
            assertTrue(relaunched.submit(MySdAppIntent.StartSelectedStage).accepted)
        }
    }

    @Test
    fun fullHistoryBeforeStartRemainsIntactUntilLiveTerminalClaim() {
        val session = restore(fullHistory(MySdAppFactory.create(seed = 205L).saveBundle()))
        assertTrue(session.submit(MySdAppIntent.Navigate(MySdRoute.CAMPAIGN)).accepted)
        assertTrue(session.submit(MySdAppIntent.SelectStage(stage.id.value)).accepted)
        assertTrue(session.submit(MySdAppIntent.StartSelectedStage).accepted)
        val before = session.saveBundle()
        assertEquals(ProductProfileManager.MAX_BATTLE_CLAIMS, decode(before).claimedBattleRuns.size)
        assertEquals(MySdAppRejection.INVALID_PHASE, session.submit(MySdAppIntent.ClaimBattleReward).rejection)
        assertEquals(before, session.saveBundle())

        finish(session, defend = false)
        assertTrue(session.submit(MySdAppIntent.ClaimBattleReward).accepted)
        assertEquals(setOf(assertNotNull(session.snapshot().battle).runId), decode(session.saveBundle()).claimedBattleRuns)
    }

    @Test
    fun alreadyClaimedTerminalAtCapacityRejectsWithoutCompactingHistory() {
        val session = started(seed = 206L)
        finish(session, defend = false)
        assertTrue(session.submit(MySdAppIntent.ClaimBattleReward).accepted)
        val atCapacity = restore(fullHistory(session.saveBundle()))

        assertDuplicateUnchanged(atCapacity)
        assertEquals(ProductProfileManager.MAX_BATTLE_CLAIMS, decode(atCapacity.saveBundle()).claimedBattleRuns.size)
        assertTrue(atCapacity.submit(MySdAppIntent.AbandonRun).accepted)
    }

    @Test
    fun evictedOldRunCannotBePairedWithLatestProfileToReclaimReward() {
        val session = started(seed = 207L)
        finish(session, defend = false)
        val olderRun = session.saveBundle().runSave
        val olderRunId = assertNotNull(session.snapshot().battle).runId
        assertTrue(session.submit(MySdAppIntent.ClaimBattleReward).accepted)
        assertTrue(session.submit(MySdAppIntent.AbandonRun).accepted)
        assertTrue(session.submit(MySdAppIntent.SelectStage(stage.id.value)).accepted)
        assertTrue(session.submit(MySdAppIntent.StartSelectedStage).accepted)
        finish(session, defend = false)
        val current = restore(fullHistory(session.saveBundle()))
        assertTrue(current.submit(MySdAppIntent.ClaimBattleReward).accepted)
        val latest = current.saveBundle()
        assertFalse(olderRunId in decode(latest).claimedBattleRuns)

        assertIs<MySdRestoreResult.Invalid>(MySdAppFactory.restore(latest.copy(runSave = olderRun)))
        assertDuplicateUnchanged(restore(latest))
    }

    @Test
    fun directProfileManagerRetainsCapacityAndDuplicateGuardsWithoutMutation() {
        val manager = ProductProfileManager.create(seed = 208L, catalog = catalog)
        repeat(ProductProfileManager.MAX_BATTLE_CLAIMS) { manager.state.claimedBattleRuns += "history-$it" }
        val before = ProductProfileCodec.encode(manager.state, catalog)
        fun claim(runId: String) = manager.claimBattleReward(
            runId, stage, MySdTerminalResult.DEFEAT, baseHealth = 0, baseMaxHealth = stage.baseHealth,
        )

        assertEquals(ProductProfileRejection.INVALID_TARGET,
            assertIs<ProductProfileMutation.Rejected>(claim("new-direct-run")).reason)
        assertEquals(before, ProductProfileCodec.encode(manager.state, catalog))
        assertEquals(ProductProfileRejection.ALREADY_CLAIMED,
            assertIs<ProductProfileMutation.Rejected>(claim("history-0")).reason)
        assertEquals(before, ProductProfileCodec.encode(manager.state, catalog))
    }

    private fun assertDuplicateUnchanged(session: MySdAppSession) {
        val before = session.saveBundle()
        val snapshot = session.snapshot()
        assertEquals(MySdAppRejection.ALREADY_CLAIMED, session.submit(MySdAppIntent.ClaimBattleReward).rejection)
        assertEquals(before, session.saveBundle())
        assertEquals(snapshot, session.snapshot())
    }

    private fun fullHistory(bundle: MySdSaveBundle): MySdSaveBundle {
        val profile = decode(bundle)
        var ordinal = 0
        while (profile.claimedBattleRuns.size < ProductProfileManager.MAX_BATTLE_CLAIMS) {
            profile.claimedBattleRuns += "history-${ordinal++}"
        }
        return bundle.copy(profileSave = ProductProfileCodec.encode(profile, catalog))
    }

    private fun decode(bundle: MySdSaveBundle): ProductProfileState =
        assertIs<ProductProfileDecodeResult.Decoded>(ProductProfileCodec.decode(bundle.profileSave, catalog)).profile

    private fun restore(bundle: MySdSaveBundle): MySdAppSession =
        assertIs<MySdRestoreResult.Restored>(MySdAppFactory.restore(bundle)).session

    private fun started(seed: Long): MySdAppSession = MySdAppFactory.create(seed).also { session ->
        assertTrue(session.submit(MySdAppIntent.Navigate(MySdRoute.CAMPAIGN)).accepted)
        assertTrue(session.submit(MySdAppIntent.SelectStage(stage.id.value)).accepted)
        assertTrue(session.submit(MySdAppIntent.StartSelectedStage).accepted)
    }

    private fun finish(session: MySdAppSession, defend: Boolean) {
        repeat(3_000) {
            val snapshot = session.snapshot()
            val battle = assertNotNull(snapshot.battle)
            if (battle.terminalResult != null) {
                assertEquals(if (defend) MySdTerminalResult.VICTORY else MySdTerminalResult.DEFEAT, battle.terminalResult)
                return
            }
            val choice = snapshot.overlay as? MySdOverlaySnapshot.EnhancementChoice
            if (choice != null) {
                assertTrue(session.submit(MySdAppIntent.ChooseEnhancement(choice.offerIds.first())).accepted)
            } else {
                if (defend) issueDefenseAction(session, snapshot)
                assertTrue(session.step(if (defend) 10 else 2_000).accepted)
            }
        }
        error("Fixture failed to reach its natural terminal within the deterministic budget.")
    }

    private fun issueDefenseAction(session: MySdAppSession, snapshot: MySdAppSnapshot) {
        val battle = assertNotNull(snapshot.battle)
        val surface = assertIs<MySdSurfaceSnapshot.Battle>(snapshot.surface)
        val empty = battle.towerSlots.firstOrNull { it.towerId == null }
        if (empty != null) {
            surface.towerBuildCosts.entries.filter { it.value <= battle.resource }.maxByOrNull { it.value }?.let {
                session.submit(MySdAppIntent.BuildTower(empty.slotId, it.key))
                return
            }
        }
        battle.towerSlots.firstOrNull { it.nextUpgradeCost != null && it.nextUpgradeCost <= battle.resource }?.let {
            session.submit(MySdAppIntent.UpgradeTower(it.slotId))
            return
        }
        if (battle.allies.size < 4) {
            surface.allyDeployCosts.entries.firstOrNull { it.value <= battle.resource }?.let {
                session.submit(MySdAppIntent.DeployAlly(it.key))
                return
            }
        }
        battle.heroSkills.firstOrNull { it.available && battle.baseHealth < battle.baseMaxHealth }?.let {
            session.submit(MySdAppIntent.UseHeroSkill(it.skillId))
        }
    }
}
