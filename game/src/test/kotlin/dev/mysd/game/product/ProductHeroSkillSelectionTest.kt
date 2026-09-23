package dev.mysd.game.product

import dev.mysd.game.content.ContentId
import dev.mysd.game.persistence.ProfileStore
import dev.mysd.game.persistence.ProfileStoreCodec
import dev.mysd.game.product.content.OriginalProductCatalog
import dev.mysd.game.product.meta.ProductProfileManager
import dev.mysd.game.product.meta.ProductProfileMutation
import dev.mysd.game.product.persistence.ProductProfileCodec
import dev.mysd.game.product.persistence.ProductProfileDecodeResult
import dev.mysd.game.product.persistence.ProductRunDecodeResult
import dev.mysd.game.product.persistence.ProductRunSaveCodec
import dev.mysd.game.product.runtime.BattleRuntimeAdapter
import dev.mysd.game.product.runtime.ProductBattleRestoreResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ProductHeroSkillSelectionTest {
    private val catalog = OriginalProductCatalog.releaseOne()
    private val repair = OriginalProductCatalog.HERO_REPAIR
    private val pulse = OriginalProductCatalog.HERO_PULSE
    private val both = listOf(repair, pulse).sortedBy(ContentId::value)
    private val selections = listOf(emptyList(), listOf(repair), listOf(pulse), both)

    @Test
    fun newProfileSelectsRepairAndProjectsOnlyUnlockedSetupChoices() {
        val session = MySdAppFactory.create(401)
        session.submit(MySdAppIntent.Navigate(MySdRoute.ROSTER))
        val roster = assertIs<MySdSurfaceSnapshot.Roster>(session.snapshot().surface)
        assertEquals(listOf(repair.value), roster.selectedHeroSkillIds)
        assertEquals(2, roster.entries.count { it.category == "hero" })
        assertFalse(roster.entries.single { it.contentId == pulse.value }.unlocked)

        session.submit(MySdAppIntent.Navigate(MySdRoute.STAGE_SETUP))
        val setup = assertIs<MySdSurfaceSnapshot.StageSetup>(session.snapshot().surface)
        assertEquals(listOf(repair.value), setup.selectedHeroSkillIds)
        assertEquals(listOf(repair.value), setup.availableHeroSkillIds)
        assertEquals(roster.selectedLoadoutIds, setup.selectedLoadoutIds)
        assertTrue(setup.selectedLoadoutIds.none { it.startsWith("hero-") })
        assertTrue(setup.canStart)
    }

    @Test
    fun rosterAndSetupToggleSelectionWithoutSpendingCurrencyOrChangingCombatSlots() {
        val session = MySdAppFactory.create(402)
        session.submit(MySdAppIntent.Navigate(MySdRoute.ROSTER))
        val before = session.snapshot()
        val beforeLedger = profile(session.saveBundle()).ledger.toList()
        val removed = session.submit(MySdAppIntent.ToggleHeroSkill(repair.value))
        assertTrue(removed.accepted)
        assertEquals(before.revision + 1, removed.snapshot.revision)
        assertEquals(emptyList(), assertIs<MySdSurfaceSnapshot.Roster>(removed.snapshot.surface).selectedHeroSkillIds)
        assertEquals("hero_loadout_changed", removed.events.single().type)
        assertEquals(repair.value, removed.events.single().sourceId)
        assertEquals(0L, removed.events.single().amount)
        assertEquals(before.profile, removed.snapshot.profile)
        assertEquals(beforeLedger, profile(session.saveBundle()).ledger)

        session.submit(MySdAppIntent.Navigate(MySdRoute.STAGE_SETUP))
        val selected = session.submit(MySdAppIntent.ToggleHeroSkill(repair.value))
        assertTrue(selected.accepted)
        assertEquals(1L, selected.events.single().amount)
        assertEquals(listOf(repair.value), assertIs<MySdSurfaceSnapshot.StageSetup>(selected.snapshot.surface).selectedHeroSkillIds)
        assertEquals(before.profile.loadoutIds, selected.snapshot.profile.loadoutIds)
        assertEquals(beforeLedger, profile(session.saveBundle()).ledger)
    }

    @Test
    fun bothHeroesAreCanonicalAndDoNotConsumeAnyOfSevenTowerAllySlots() {
        val session = unlockedSession(403)
        session.submit(MySdAppIntent.Navigate(MySdRoute.STAGE_SETUP))
        val setup = assertIs<MySdSurfaceSnapshot.StageSetup>(session.snapshot().surface)
        assertEquals(both.map { it.value }, setup.availableHeroSkillIds)
        setup.availableLoadoutIds.filterNot(setup.selectedLoadoutIds::contains).forEach { id ->
            assertTrue(session.submit(MySdAppIntent.SetLoadoutSlot(session.snapshot().profile.loadoutIds.size, id)).accepted)
        }
        assertTrue(session.submit(MySdAppIntent.ToggleHeroSkill(pulse.value)).accepted)
        assertTrue(session.submit(MySdAppIntent.ToggleHeroSkill(repair.value)).accepted)
        assertTrue(session.submit(MySdAppIntent.ToggleHeroSkill(repair.value)).accepted)
        val selected = assertIs<MySdSurfaceSnapshot.StageSetup>(session.snapshot().surface)
        assertEquals(both.map { it.value }, selected.selectedHeroSkillIds)
        assertEquals(7, selected.selectedLoadoutIds.size)
        assertTrue(session.submit(MySdAppIntent.StartSelectedStage).accepted)
        val battle = assertIs<MySdSurfaceSnapshot.Battle>(session.snapshot().surface)
        assertEquals(4, battle.towerCardIds.size)
        assertEquals(3, battle.allyCardIds.size)
        assertEquals(both.map { it.value }, battle.heroSkillIds)
    }

    @Test
    fun lockedUnknownMalformedAndNonHeroToggleRejectionsAreAtomic() {
        val session = MySdAppFactory.create(404)
        session.submit(MySdAppIntent.Navigate(MySdRoute.ROSTER))
        assertRejectedWithoutMutation(session, MySdAppIntent.ToggleHeroSkill(pulse.value), MySdAppRejection.LOCKED)
        listOf("hero-unknown", "", " Hero-hearth-repair", catalog.towers.keys.first().value,
            catalog.allies.keys.first().value).forEach { id ->
            assertRejectedWithoutMutation(session, MySdAppIntent.ToggleHeroSkill(id), MySdAppRejection.UNKNOWN_CONTENT)
        }
    }

    @Test
    fun heroSelectionRejectsEveryNonEditingRoute() {
        MySdRoute.entries.filterNot { it in setOf(MySdRoute.ROSTER, MySdRoute.STAGE_SETUP, MySdRoute.BATTLE) }
            .forEach { route ->
                val session = MySdAppFactory.create(405)
                assertTrue(session.submit(MySdAppIntent.Navigate(route)).accepted)
                assertRejectedWithoutMutation(session, MySdAppIntent.ToggleHeroSkill(repair.value), MySdAppRejection.WRONG_ROUTE)
            }
    }

    @Test
    fun activePausedAndUnclaimedTerminalRunsCannotChangeHeroSelection() {
        val session = MySdAppFactory.create(406)
        start(session)
        assertRejectedWithoutMutation(session, MySdAppIntent.ToggleHeroSkill(repair.value), MySdAppRejection.WRONG_ROUTE)
        assertTrue(session.submit(MySdAppIntent.PauseOrResume).accepted)
        assertRejectedWithoutMutation(session, MySdAppIntent.ToggleHeroSkill(repair.value), MySdAppRejection.WRONG_ROUTE)
        assertTrue(session.submit(MySdAppIntent.Navigate(MySdRoute.HOME)).accepted)
        assertRejectedWithoutMutation(session, MySdAppIntent.ToggleHeroSkill(repair.value), MySdAppRejection.WRONG_ROUTE)
        assertRejectedWithoutMutation(session, MySdAppIntent.Navigate(MySdRoute.ROSTER), MySdAppRejection.ACTIVE_RUN_EXISTS)
        assertTrue(session.submit(MySdAppIntent.ResumeRun).accepted)
        assertTrue(session.submit(MySdAppIntent.PauseOrResume).accepted)
        defeat(session)
        assertRejectedWithoutMutation(session, MySdAppIntent.Navigate(MySdRoute.STAGE_SETUP), MySdAppRejection.TERMINAL_RUN)
        assertRejectedWithoutMutation(session, MySdAppIntent.ToggleHeroSkill(repair.value), MySdAppRejection.WRONG_ROUTE)
        assertEquals(listOf(repair), profile(session.saveBundle()).selectedHeroSkillIds)
    }

    @Test
    fun claimedTerminalAllowsFutureSelectionWithoutRewritingItsFrozenHeroLoadout() {
        val session = MySdAppFactory.create(407)
        start(session)
        defeat(session)
        assertTrue(session.submit(MySdAppIntent.ClaimBattleReward).accepted)
        session.submit(MySdAppIntent.Navigate(MySdRoute.ROSTER))
        val frozenRun = session.saveBundle().runSave
        assertTrue(session.submit(MySdAppIntent.ToggleHeroSkill(repair.value)).accepted)
        assertEquals(frozenRun, session.saveBundle().runSave)
        val restored = restored(session.saveBundle())
        assertEquals(session.snapshot(), restored.snapshot())
        assertEquals(listOf(repair.value), assertNotNull(restored.snapshot().battle).heroSkills.map { it.skillId })
        assertEquals(emptyList(), assertIs<MySdSurfaceSnapshot.Roster>(restored.snapshot().surface).selectedHeroSkillIds)
        start(restored)
        assertTrue(assertNotNull(restored.snapshot().battle).heroSkills.isEmpty())
    }

    @Test
    fun allFourSelectionsLaunchOnlyChosenSkillsAndContinueAcrossV5RunRestore() {
        selections.forEach { selected ->
            val session = unlockedSession(408)
            session.submit(MySdAppIntent.Navigate(MySdRoute.STAGE_SETUP))
            select(session, selected)
            assertTrue(session.submit(MySdAppIntent.StartSelectedStage).accepted)
            assertEquals(selected.map { it.value }, assertNotNull(session.snapshot().battle).heroSkills.map { it.skillId })
            assertEquals(selected.map { it.value }, assertIs<MySdSurfaceSnapshot.Battle>(session.snapshot().surface).heroSkillIds)
            both.filterNot(selected::contains).forEach { skill ->
                assertRejectedWithoutMutation(session, MySdAppIntent.UseHeroSkill(skill.value), MySdAppRejection.UNKNOWN_CONTENT)
            }
            session.step(1)
            selected.forEach { skill -> assertTrue(session.submit(MySdAppIntent.UseHeroSkill(skill.value)).accepted) }
            session.step(20)
            val bundle = session.saveBundle()
            val run = assertIs<ProductRunDecodeResult.Decoded>(ProductRunSaveCodec.decode(assertNotNull(bundle.runSave))).save
            assertEquals(5, run.schemaVersion)
            assertTrue(run.payload.lineSequence().contains("heroLoadout=${selected.joinToString(",") { it.value }}"))
            val engineRestored = assertIs<ProductBattleRestoreResult.Restored>(BattleRuntimeAdapter.restore(run)).controller
            assertEquals(selected, engineRestored.snapshot().heroSkillIds)
            assertEquals(selected.toSet(), engineRestored.snapshot().skillCooldowns.keys)
            val continuation = restored(bundle)
            assertEquals(bundle, continuation.saveBundle())
            session.step(50)
            continuation.step(50)
            assertEquals(session.snapshot(), continuation.snapshot())
            assertEquals(session.saveBundle(), continuation.saveBundle())
        }
    }

    @Test
    fun activeAtomicPairRejectsAValidButDifferentSelectedHeroCoordinate() {
        val session = MySdAppFactory.create(409)
        start(session)
        val original = session.saveBundle()
        val changed = original.copy(profileSave = replaceField(original.profileSave, "selectedHeroSkillIds", ""))
        assertIs<ProductProfileDecodeResult.Decoded>(ProductProfileCodec.decode(changed.profileSave, catalog))
        assertIs<MySdRestoreResult.Invalid>(MySdAppFactory.restore(changed))
        assertEquals(original, session.saveBundle())
    }

    @Test
    fun schemaFourPersistsEverySelectionCanonicallyIncludingNone() {
        selections.forEach { selected ->
            val manager = unlockedProfile(410)
            manager.state.selectedHeroSkillIds.clear()
            manager.state.selectedHeroSkillIds.addAll(selected)
            val encoded = ProductProfileCodec.encode(manager.state, catalog)
            assertTrue(encoded.lineSequence().contains("schemaVersion=4"))
            assertTrue(encoded.lineSequence().contains("selectedHeroSkillIds=${selected.joinToString(",") { it.value }}"))
            val decoded = assertIs<ProductProfileDecodeResult.Decoded>(ProductProfileCodec.decode(encoded, catalog))
            assertFalse(decoded.migrated)
            assertEquals(selected, decoded.profile.selectedHeroSkillIds)
            assertEquals(encoded, ProductProfileCodec.encode(decoded.profile, catalog))
        }
    }

    @Test
    fun schemaThreeMigratesAllThenUnlockedHeroesRatherThanTheNewDefault() {
        listOf(ProductProfileManager.create(411, catalog), unlockedProfile(411)).forEach { manager ->
            manager.state.selectedHeroSkillIds.clear()
            val old = schemaThree(ProductProfileCodec.encode(manager.state, catalog))
            val decoded = assertIs<ProductProfileDecodeResult.Decoded>(ProductProfileCodec.decode(old, catalog))
            assertTrue(decoded.migrated)
            assertEquals(catalog.heroSkills.keys.filter(manager.state.unlockedRoster::contains).sortedBy(ContentId::value),
                decoded.profile.selectedHeroSkillIds)
            val current = ProductProfileCodec.encode(decoded.profile, catalog)
            assertTrue(current.contains("schemaVersion=4\n"))
            assertEquals(old, schemaThree(current))
        }
    }

    @Test
    fun schemaThreeActivePairMigrationPreservesRunBytesHashAndContinuation() {
        listOf(MySdAppFactory.create(412), unlockedSession(412)).forEach { session ->
            session.submit(MySdAppIntent.Navigate(MySdRoute.STAGE_SETUP))
            val available = assertIs<MySdSurfaceSnapshot.StageSetup>(session.snapshot().surface).availableHeroSkillIds
            select(session, available.map(ContentId::of))
            session.submit(MySdAppIntent.StartSelectedStage)
            session.step(25)
            val current = session.saveBundle()
            val migrated = restored(current.copy(profileSave = schemaThree(current.profileSave)))
            assertEquals(current.runSave, migrated.saveBundle().runSave)
            assertEquals(current, migrated.saveBundle())
            assertEquals(session.snapshot(), migrated.snapshot())
            session.step(60)
            migrated.step(60)
            assertEquals(session.snapshot(), migrated.snapshot())
        }
    }

    @Test
    fun schemaThreeUnclaimedTerminalMigrationPreservesRewardAndFrozenRun() {
        val session = MySdAppFactory.create(413)
        start(session)
        defeat(session)
        val current = session.saveBundle()
        val migrated = restored(current.copy(profileSave = schemaThree(current.profileSave)))
        assertEquals(current, migrated.saveBundle())
        assertEquals(session.snapshot(), migrated.snapshot())
        assertTrue(migrated.submit(MySdAppIntent.ClaimBattleReward).accepted)
        assertEquals(current.runSave, migrated.saveBundle().runSave)
        assertRejectedWithoutMutation(migrated, MySdAppIntent.ClaimBattleReward, MySdAppRejection.ALREADY_CLAIMED)
    }

    @Test
    fun legacySchemaOneAndTwoMigrationSelectsTheSafeUnlockedRepairSkill() {
        val legacy = ProfileStore("hero-legacy", setOf(catalog.orderedStages.first().id.value),
            mapOf("gold" to 300L), 7, emptySet(), emptyList(), emptySet(), emptySet(), emptyList())
        val v2 = ProfileStoreCodec.encode(legacy)
        val v1 = v2.lineSequence().filterNot { it.startsWith("techCount=") || it.startsWith("serviceHistoryCount=") }
            .joinToString("\n").replaceFirst("schemaVersion=2", "schemaVersion=1")
        listOf(v1, v2).forEach { old ->
            val decoded = assertIs<ProductProfileDecodeResult.Decoded>(ProductProfileCodec.decode(old, catalog))
            assertTrue(decoded.migrated)
            assertEquals(listOf(repair), decoded.profile.selectedHeroSkillIds)
            assertEquals("hero-legacy", decoded.profile.profileId)
            val encoded = ProductProfileCodec.encode(decoded.profile, catalog)
            assertIs<MySdRestoreResult.Restored>(MySdAppFactory.restore(MySdSaveBundle(null, encoded)))
        }
    }

    @Test
    fun malformedUnknownLockedDuplicateAndNonCanonicalHeroCoordinatesAreRejected() {
        val initial = MySdAppFactory.create(414).saveBundle()
        listOf(pulse.value, "hero-unknown", catalog.towers.keys.first().value, "${repair.value},${repair.value}",
            "${repair.value},", " ${repair.value}", "HERO-HEARTH-REPAIR").forEach { raw ->
            assertIs<MySdRestoreResult.Invalid>(MySdAppFactory.restore(initial.copy(
                profileSave = replaceField(initial.profileSave, "selectedHeroSkillIds", raw))), raw)
        }
        val unlocked = unlockedSession(414).saveBundle()
        listOf(both.reversed(), both + repair).forEach { invalid ->
            assertIs<MySdRestoreResult.Invalid>(MySdAppFactory.restore(unlocked.copy(profileSave =
                replaceField(unlocked.profileSave, "selectedHeroSkillIds", invalid.joinToString(",") { it.value }))))
        }
    }

    @Test
    fun schemaSpecificKeysRemainStrictWithNoSilentHeroSelectionFallback() {
        val current = MySdAppFactory.create(415).saveBundle()
        val missing = current.profileSave.lineSequence().filterNot { it.startsWith("selectedHeroSkillIds=") }.joinToString("\n")
        val malformed = listOf(missing, current.profileSave + "\nunknownField=1",
            current.profileSave + "\nselectedHeroSkillIds=", current.profileSave.replaceFirst("schemaVersion=4", "schemaVersion=3"))
        malformed.forEach { raw -> assertIs<MySdRestoreResult.Invalid>(MySdAppFactory.restore(current.copy(profileSave = raw))) }
    }

    @Test
    fun futureProfileVersionsRemainTypedIncompatibilitiesAfterVersionFour() {
        val current = MySdAppFactory.create(416).saveBundle()
        listOf(5, 99).forEach { version ->
            val result = assertIs<MySdRestoreResult.Incompatible>(MySdAppFactory.restore(current.copy(
                profileSave = current.profileSave.replaceFirst("schemaVersion=4", "schemaVersion=$version"))))
            assertEquals(MySdRestoreIncompatibility.PROFILE_SCHEMA, result.kind)
            assertEquals("4", result.expected)
            assertEquals(version.toString(), result.actual)
        }
        assertIs<MySdRestoreResult.Incompatible>(MySdAppFactory.restore(current.copy(
            profileSave = current.profileSave.replaceFirst("schemaVersion=4", "schemaVersion=2"))))
    }

    @Test
    fun newlyUnlockedHeroNeverAutoEquipsEvenAfterLegacyMigrationOrChoosingNone() {
        listOf(false, true).forEach { chooseNone ->
            val initial = ProductProfileManager.create(417, catalog)
            val old = schemaThree(ProductProfileCodec.encode(initial.state, catalog))
            val migrated = assertIs<ProductProfileDecodeResult.Decoded>(ProductProfileCodec.decode(old, catalog))
            val manager = ProductProfileManager(catalog, migrated.profile)
            if (chooseNone) assertIs<ProductProfileMutation.Applied<Boolean>>(manager.toggleHeroSkill(repair))
            val expected = manager.state.selectedHeroSkillIds.toList()
            unlockHeroes(manager)
            assertTrue(pulse in manager.state.unlockedRoster)
            assertEquals(expected, manager.state.selectedHeroSkillIds)
            val ledger = manager.state.ledger.toList()
            assertIs<ProductProfileMutation.Applied<Boolean>>(manager.toggleHeroSkill(pulse))
            assertEquals((expected + pulse).sortedBy(ContentId::value), manager.state.selectedHeroSkillIds)
            assertEquals(ledger, manager.state.ledger)
            assertIs<ProductProfileDecodeResult.Decoded>(ProductProfileCodec.decode(ProductProfileCodec.encode(manager.state, catalog), catalog))
        }
    }

    @Test
    fun heroCoordinateChangesHashEvenWithOtherwiseIdenticalProfileCursors() {
        val manager = ProductProfileManager.create(418, catalog)
        val selected = ProductProfileCodec.encode(manager.state, catalog)
        manager.state.selectedHeroSkillIds.clear()
        val none = ProductProfileCodec.encode(manager.state, catalog)
        val selectedSession = restored(MySdSaveBundle(null, selected))
        val noneSession = restored(MySdSaveBundle(null, none))
        assertEquals(selectedSession.snapshot().revision, noneSession.snapshot().revision)
        assertNotEquals(selectedSession.snapshot().clock.stableHash, noneSession.snapshot().clock.stableHash)
        assertEquals(selectedSession.snapshot().clock.stableHash,
            restored(MySdSaveBundle(null, schemaThree(selected))).snapshot().clock.stableHash)
    }

    @Test
    fun identicalSelectionIntentsProduceIdenticalEventsSavesAndHashes() {
        val first = MySdAppFactory.create(419)
        val second = MySdAppFactory.create(419)
        listOf(MySdAppIntent.Navigate(MySdRoute.ROSTER), MySdAppIntent.ToggleHeroSkill(repair.value),
            MySdAppIntent.ToggleHeroSkill(pulse.value), MySdAppIntent.ToggleHeroSkill(repair.value),
            MySdAppIntent.ToggleHeroSkill(repair.value), MySdAppIntent.Navigate(MySdRoute.STAGE_SETUP),
            MySdAppIntent.StartSelectedStage).forEach { intent ->
            assertEquals(first.submit(intent), second.submit(intent))
        }
        assertEquals(first.step(80), second.step(80))
        assertEquals(first.saveBundle(), second.saveBundle())
    }

    private fun profile(bundle: MySdSaveBundle) =
        assertIs<ProductProfileDecodeResult.Decoded>(ProductProfileCodec.decode(bundle.profileSave, catalog)).profile

    private fun unlockedProfile(seed: Long) = ProductProfileManager.create(seed, catalog).also(::unlockHeroes)

    private fun unlockHeroes(manager: ProductProfileManager) {
        catalog.orderedStages.take(3).forEach { stage ->
            assertIs<ProductProfileMutation.Applied<*>>(manager.claimBattleReward("history-${stage.ordinal}", stage,
                MySdTerminalResult.VICTORY, stage.baseHealth, stage.baseHealth))
        }
    }

    private fun unlockedSession(seed: Long) = restored(MySdSaveBundle(null, ProductProfileCodec.encode(unlockedProfile(seed).state, catalog)))

    private fun restored(bundle: MySdSaveBundle) = assertIs<MySdRestoreResult.Restored>(MySdAppFactory.restore(bundle)).session

    private fun start(session: MySdAppSession) {
        assertTrue(session.submit(MySdAppIntent.Navigate(MySdRoute.CAMPAIGN)).accepted)
        assertTrue(session.submit(MySdAppIntent.SelectStage(catalog.orderedStages.first().id.value)).accepted)
        assertTrue(session.submit(MySdAppIntent.StartSelectedStage).accepted)
    }

    private fun select(session: MySdAppSession, selected: List<ContentId>) {
        val current = assertIs<MySdSurfaceSnapshot.StageSetup>(session.snapshot().surface).selectedHeroSkillIds
        both.filter { (it.value in current) != (it in selected) }.forEach { skill ->
            assertTrue(session.submit(MySdAppIntent.ToggleHeroSkill(skill.value)).accepted)
        }
    }

    private fun defeat(session: MySdAppSession) {
        repeat(12) {
            if (session.snapshot().battle?.terminalResult != null) {
                assertEquals(MySdTerminalResult.DEFEAT, session.snapshot().battle?.terminalResult)
                return
            }
            (session.snapshot().overlay as? MySdOverlaySnapshot.EnhancementChoice)?.let {
                assertTrue(session.submit(MySdAppIntent.ChooseEnhancement(it.offerIds.first())).accepted)
            }
            session.step(2_000)
        }
        error("Expected unopposed first-stage defeat within the bounded scenario.")
    }

    private fun assertRejectedWithoutMutation(session: MySdAppSession, intent: MySdAppIntent, reason: MySdAppRejection) {
        val snapshot = session.snapshot()
        val saved = session.saveBundle()
        val result = session.submit(intent)
        assertFalse(result.accepted)
        assertEquals(reason, result.rejection)
        assertTrue(result.events.isEmpty())
        assertEquals(snapshot, result.snapshot)
        assertEquals(saved, session.saveBundle())
    }

    private fun schemaThree(current: String): String = current.lineSequence()
        .filterNot { it.startsWith("selectedHeroSkillIds=") }.joinToString("\n")
        .replaceFirst("schemaVersion=4", "schemaVersion=3")

    private fun replaceField(document: String, key: String, value: String): String =
        document.replace(Regex("(?m)^$key=.*$"), "$key=$value")
}
