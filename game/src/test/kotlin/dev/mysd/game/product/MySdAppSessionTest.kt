package dev.mysd.game.product

import dev.mysd.game.product.content.OriginalProductCatalog
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MySdAppSessionTest {
    @Test
    fun facadeStartsWithAnAtomicHomeProjection() {
        val snapshot = MySdAppFactory.create(seed = 7).snapshot()

        assertEquals(MySdRoute.HOME, snapshot.route)
        assertIs<MySdSurfaceSnapshot.Home>(snapshot.surface)
        assertNull(snapshot.battle)
        assertNull(snapshot.overlay)
        assertEquals(10, snapshot.profile.energy.current)
        assertTrue(snapshot.profile.loadoutIds.isNotEmpty())
        assertFalse(snapshot.clock.running)
    }

    @Test
    fun campaignSetupStartsBattleAndPublishesAuthoritativeCostsAndPath() {
        val session = startedBattle(seed = 8)
        val snapshot = session.snapshot()
        val surface = assertIs<MySdSurfaceSnapshot.Battle>(snapshot.surface)

        assertEquals(MySdRoute.BATTLE, snapshot.route)
        assertNotNull(snapshot.battle)
        assertEquals(1_000, snapshot.battle.pathLengthTicks)
        assertTrue(surface.towerBuildCosts.values.all { it > 0 })
        assertTrue(surface.allyDeployCosts.values.all { it > 0 })
        assertEquals(8, snapshot.profile.energy.current)
    }

    @Test
    fun pauseStopsPulsesAndTwoXAdvancesTwoAuthoritativeTicks() {
        val session = startedBattle(seed = 9)

        val paused = session.submit(MySdAppIntent.PauseOrResume).snapshot
        val pausedTick = paused.clock.tick
        assertFalse(paused.clock.running)
        assertEquals(pausedTick, session.pulse().snapshot.clock.tick)

        session.submit(MySdAppIntent.PauseOrResume)
        val spedUp = session.submit(MySdAppIntent.ToggleBattleSpeed).snapshot
        val beforePulse = spedUp.clock.tick
        val afterPulse = session.pulse().snapshot
        assertEquals(MySdBattleSpeed.TWO_X, afterPulse.clock.speed)
        assertEquals(beforePulse + 2, afterPulse.clock.tick)
    }

    @Test
    fun unopposedBattleHasANaturalDefeatPath() {
        val session = startedBattle(seed = 10)
        val terminal = driveUntilTerminal(session, defend = false)

        assertEquals(MySdTerminalResult.DEFEAT, terminal.terminalResult)
        assertEquals(MySdBattlePhase.DEFEAT, terminal.phase)
        assertEquals(0, terminal.baseHealth)
    }

    @Test
    fun activeDefenseHasANaturalVictoryPathAndUnlocksProgressionWhenClaimed() {
        val session = startedBattle(seed = 11)
        val terminal = driveUntilTerminal(session, defend = true)

        assertEquals(MySdTerminalResult.VICTORY, terminal.terminalResult)
        val claim = session.submit(MySdAppIntent.ClaimBattleReward)
        assertTrue(claim.accepted)
        assertIs<MySdOverlaySnapshot.TerminalReward>(claim.snapshot.overlay).also { assertTrue(it.claimed) }

        session.submit(MySdAppIntent.CloseOverlay)
        val campaign = assertIs<MySdSurfaceSnapshot.CampaignMap>(session.snapshot().surface)
        assertTrue(campaign.stages.single { it.ordinal == 2 }.unlocked)

        session.submit(MySdAppIntent.Navigate(MySdRoute.ROSTER))
        val roster = assertIs<MySdSurfaceSnapshot.Roster>(session.snapshot().surface)
        assertTrue(roster.entries.single { it.contentId == OriginalProductCatalog.TOWER_CONTROL.value }.unlocked)
        assertTrue(roster.entries.single { it.contentId == OriginalProductCatalog.ALLY_SKIRMISHER.value }.unlocked)
    }

    @Test
    fun saveRestorePreservesMidBattleAndTerminalRuns() {
        val original = startedBattle(seed = 12)
        original.step(40)
        val restored = assertIs<MySdRestoreResult.Restored>(MySdAppFactory.restore(original.saveBundle())).session

        assertEquals(original.snapshot(), restored.snapshot())
        original.step(60)
        restored.step(60)
        assertEquals(original.snapshot().clock.stableHash, restored.snapshot().clock.stableHash)

        val defeated = startedBattle(seed = 13)
        driveUntilTerminal(defeated, defend = false)
        val terminalRestored = assertIs<MySdRestoreResult.Restored>(
            MySdAppFactory.restore(defeated.saveBundle()),
        ).session.snapshot()
        assertEquals(MySdTerminalResult.DEFEAT, terminalRestored.battle?.terminalResult)
        assertIs<MySdOverlaySnapshot.TerminalReward>(terminalRestored.overlay)
    }

    @Test
    fun rosterLoadoutMaintainsOneTowerAndOneAllyAndSupportsHeroUpgrades() {
        val session = MySdAppFactory.create(seed = 14)
        session.submit(MySdAppIntent.Navigate(MySdRoute.ROSTER))
        val initial = assertIs<MySdSurfaceSnapshot.Roster>(session.snapshot().surface)
        val allyIndex = initial.selectedLoadoutIds.indexOfFirst { it.startsWith("ally-") }

        val rejected = session.submit(MySdAppIntent.ClearLoadoutSlot(allyIndex))
        assertEquals(MySdAppRejection.INVALID_TARGET, rejected.rejection)

        val towerIndex = initial.selectedLoadoutIds.indexOfFirst { it.startsWith("tower-") }
        assertTrue(session.submit(MySdAppIntent.ClearLoadoutSlot(towerIndex)).accepted)
        val afterClear = session.snapshot().profile.loadoutIds
        assertTrue(afterClear.any { it.startsWith("tower-") })
        assertTrue(afterClear.any { it.startsWith("ally-") })

        val locked = session.submit(
            MySdAppIntent.SetLoadoutSlot(afterClear.size, OriginalProductCatalog.TOWER_CONTROL.value),
        )
        assertEquals(MySdAppRejection.LOCKED, locked.rejection)

        val hero = initial.entries.single { it.category == "hero" && it.unlocked }
        val upgraded = session.submit(MySdAppIntent.UpgradeRosterItem(hero.contentId))
        assertTrue(upgraded.accepted)
        val upgradedRoster = assertIs<MySdSurfaceSnapshot.Roster>(upgraded.snapshot.surface)
        assertEquals(2, upgradedRoster.entries.single { it.contentId == hero.contentId }.level)
    }

    @Test
    fun serviceIntegrationsAreExplicitInvokableStubs() {
        val session = MySdAppFactory.create(seed = 15)
        session.submit(MySdAppIntent.Navigate(MySdRoute.SHOP))
        val shop = assertIs<MySdSurfaceSnapshot.Shop>(session.snapshot().surface)
        assertTrue(shop.offers.filter { it.stubOnly }.all { it.affordable })

        val result = session.submit(MySdAppIntent.RequestRewardedStub("daily-cache"))
        assertFalse(result.accepted)
        assertEquals(MySdAppRejection.SERVICE_STUB, result.rejection)
        assertIs<MySdOverlaySnapshot.Message>(result.snapshot.overlay).also { assertTrue(it.serviceStub) }
        assertTrue(session.submit(MySdAppIntent.CloseOverlay).accepted)
    }

    @Test
    fun fullEnergyShopPurchaseIsAtomicAndDoesNotSpendCurrency() {
        val session = MySdAppFactory.create(seed = 16)
        session.submit(MySdAppIntent.Navigate(MySdRoute.SHOP))
        val before = session.snapshot()
        val result = session.submit(MySdAppIntent.BuyShopOffer("shop-supply-small"))

        assertEquals(MySdAppRejection.INVALID_TARGET, result.rejection)
        assertEquals(before.profile.currencies, result.snapshot.profile.currencies)
        assertEquals(before.revision, result.snapshot.revision)
        assertEquals(before.clock.stableHash, result.snapshot.clock.stableHash)
    }

    @Test
    fun energyRefreshIsOverflowSafeAndZeroDeltaDoesNotChangeGameplayHash() {
        val first = MySdAppFactory.create(seed = 17)
        val second = MySdAppFactory.create(seed = 17)
        val firstRefresh = first.submit(MySdAppIntent.RefreshEnergy(100))
        val secondRefresh = second.submit(MySdAppIntent.RefreshEnergy(200))
        assertEquals(0, firstRefresh.snapshot.revision)
        assertEquals(firstRefresh.snapshot.clock.stableHash, secondRefresh.snapshot.clock.stableHash)

        first.submit(MySdAppIntent.Navigate(MySdRoute.CAMPAIGN))
        first.submit(MySdAppIntent.SelectStage(OriginalProductCatalog.releaseOne().orderedStages.first().id.value))
        first.submit(MySdAppIntent.StartSelectedStage)
        first.submit(MySdAppIntent.AbandonRun)
        first.submit(MySdAppIntent.RefreshEnergy(300))
        val recovered = first.submit(MySdAppIntent.RefreshEnergy(Long.MAX_VALUE))
        assertTrue(recovered.accepted)
        assertEquals(recovered.snapshot.profile.energy.maximum, recovered.snapshot.profile.energy.current)
    }

    @Test
    fun localArenaIsDeterministicAndNeverUsesNetwork() {
        val first = MySdAppFactory.create(seed = 18)
        val second = MySdAppFactory.create(seed = 18)
        first.submit(MySdAppIntent.Navigate(MySdRoute.ARENA))
        second.submit(MySdAppIntent.Navigate(MySdRoute.ARENA))

        val a = assertIs<MySdSurfaceSnapshot.Arena>(first.submit(MySdAppIntent.StartArenaExhibition).snapshot.surface)
        val b = assertIs<MySdSurfaceSnapshot.Arena>(second.submit(MySdAppIntent.StartArenaExhibition).snapshot.surface)
        assertEquals(a, b)
        assertFalse(a.exhibition.networkUsed)
    }

    private fun startedBattle(seed: Long): MySdAppSession {
        val session = MySdAppFactory.create(seed)
        session.submit(MySdAppIntent.Navigate(MySdRoute.CAMPAIGN))
        session.submit(MySdAppIntent.SelectStage(OriginalProductCatalog.releaseOne().orderedStages.first().id.value))
        val started = session.submit(MySdAppIntent.StartSelectedStage)
        assertTrue(started.accepted)
        return session
    }

    private fun driveUntilTerminal(session: MySdAppSession, defend: Boolean): MySdBattleSnapshot {
        repeat(3_000) {
            val snapshot = session.snapshot()
            val battle = assertNotNull(snapshot.battle)
            battle.terminalResult?.let { return battle }
            val overlay = snapshot.overlay
            if (overlay is MySdOverlaySnapshot.EnhancementChoice) {
                session.submit(MySdAppIntent.ChooseEnhancement(overlay.offerIds.first()))
                return@repeat
            }
            if (defend) issueDefenseAction(session, snapshot)
            session.step(if (defend) 10 else 2_000)
        }
        error("Battle did not reach a terminal result within the deterministic test budget.")
    }

    private fun issueDefenseAction(session: MySdAppSession, snapshot: MySdAppSnapshot) {
        val battle = assertNotNull(snapshot.battle)
        val surface = assertIs<MySdSurfaceSnapshot.Battle>(snapshot.surface)
        val empty = battle.towerSlots.firstOrNull { it.towerId == null }
        if (empty != null) {
            val preferred = surface.towerBuildCosts.entries
                .filter { it.value <= battle.resource }
                .maxByOrNull { it.value }
            if (preferred != null) {
                session.submit(MySdAppIntent.BuildTower(empty.slotId, preferred.key))
                return
            }
        }
        val upgrade = battle.towerSlots.firstOrNull {
            it.nextUpgradeCost != null && it.nextUpgradeCost <= battle.resource
        }
        if (upgrade != null) {
            session.submit(MySdAppIntent.UpgradeTower(upgrade.slotId))
            return
        }
        val ally = surface.allyDeployCosts.entries.firstOrNull { it.value <= battle.resource }
        if (ally != null && battle.allies.size < 4) {
            session.submit(MySdAppIntent.DeployAlly(ally.key))
            return
        }
        battle.heroSkills.firstOrNull { it.available && battle.baseHealth < battle.baseMaxHealth }?.let {
            session.submit(MySdAppIntent.UseHeroSkill(it.skillId))
        }
    }
}
