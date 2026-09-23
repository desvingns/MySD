package dev.mysd.game.product.persistence

import dev.mysd.game.persistence.ProfileStore
import dev.mysd.game.persistence.ProfileStoreCodec
import dev.mysd.game.product.MySdAppFactory
import dev.mysd.game.product.MySdAppIntent
import dev.mysd.game.product.MySdRestoreIncompatibility
import dev.mysd.game.product.MySdRestoreResult
import dev.mysd.game.product.MySdRoute
import dev.mysd.game.product.MySdSaveBundle
import dev.mysd.game.product.content.OriginalProductCatalog
import dev.mysd.game.product.runtime.BattleRuntimeAdapter
import dev.mysd.game.product.runtime.ProductBattleSaveEnvelope
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class ProductPersistenceTest {
    @Test
    fun completeProfileRoundTripsCanonically() {
        val session = MySdAppFactory.create(seed = 41)
        session.submit(MySdAppIntent.Navigate(MySdRoute.SETTINGS))
        session.submit(MySdAppIntent.ToggleSetting(dev.mysd.game.product.MySdSettingId.HAPTICS))
        val first = session.saveBundle()

        val restored = assertIs<MySdRestoreResult.Restored>(MySdAppFactory.restore(first)).session
        val second = restored.saveBundle()

        assertEquals(first, second)
        assertEquals(session.snapshot(), restored.snapshot())
    }

    @Test
    fun legacyProfileSchemaMigratesWithIdentityAndSafeStartingRoster() {
        val legacy = ProfileStore(
            profileId = "legacy-player",
            unlockedStages = setOf("stage-ember-path"),
            currencies = mapOf("gold" to 321L, "gems" to 4L),
            energy = 6,
            roster = setOf("legacy-unit"),
            loadout = listOf("legacy-unit"),
            tech = emptySet(),
            claims = emptySet(),
            localServiceHistory = listOf("legacy-offline-result"),
        )
        val restored = assertIs<MySdRestoreResult.Restored>(
            MySdAppFactory.restore(MySdSaveBundle(null, ProfileStoreCodec.encode(legacy))),
        ).session.snapshot()

        assertEquals("legacy-player", restored.profile.profileId)
        assertEquals(321L, restored.profile.currencies.soft)
        assertEquals(4L, restored.profile.currencies.premiumShaped)
        assertTrue(restored.profile.loadoutIds.any { it.startsWith("tower-") })
        assertTrue(restored.profile.loadoutIds.any { it.startsWith("ally-") })
    }

    @Test
    fun futureProfileAndRunEnvelopeSchemasReturnTypedIncompatibility() {
        val base = MySdAppFactory.create(seed = 42).saveBundle()
        val futureProfile = base.profileSave.replaceFirst("schemaVersion=4", "schemaVersion=99")
        val profileResult = assertIs<MySdRestoreResult.Incompatible>(
            MySdAppFactory.restore(base.copy(profileSave = futureProfile)),
        )
        assertEquals(MySdRestoreIncompatibility.PROFILE_SCHEMA, profileResult.kind)

        val futureRun = ProductRunSaveCodec.encode(
            ProductBattleSaveEnvelope(
                runtimeId = BattleRuntimeAdapter.RUNTIME_ID,
                contentPackId = OriginalProductCatalog.PACK_ID.value,
                contentPackVersion = OriginalProductCatalog.CONTENT_VERSION.toString(),
                schemaId = BattleRuntimeAdapter.SAVE_SCHEMA_ID,
                schemaVersion = 99,
                payload = "future",
            ),
        )
        val runResult = assertIs<MySdRestoreResult.Incompatible>(
            MySdAppFactory.restore(base.copy(runSave = futureRun)),
        )
        assertEquals(MySdRestoreIncompatibility.RUN_SCHEMA, runResult.kind)
    }

    @Test
    fun wrongRuntimeAndContentReturnTheirOwnTypedFailures() {
        val profile = MySdAppFactory.create(seed = 43).saveBundle().profileSave
        val wrongRuntime = ProductRunSaveCodec.encode(
            ProductBattleSaveEnvelope(
                runtimeId = "other-runtime",
                contentPackId = OriginalProductCatalog.PACK_ID.value,
                contentPackVersion = OriginalProductCatalog.CONTENT_VERSION.toString(),
                schemaId = BattleRuntimeAdapter.SAVE_SCHEMA_ID,
                schemaVersion = BattleRuntimeAdapter.CURRENT_SAVE_SCHEMA,
                payload = "ignored",
            ),
        )
        assertEquals(
            MySdRestoreIncompatibility.RUNTIME,
            assertIs<MySdRestoreResult.Incompatible>(
                MySdAppFactory.restore(MySdSaveBundle(wrongRuntime, profile)),
            ).kind,
        )

        val wrongContent = ProductRunSaveCodec.encode(
            ProductBattleSaveEnvelope(
                runtimeId = BattleRuntimeAdapter.RUNTIME_ID,
                contentPackId = "other-pack",
                contentPackVersion = "1",
                schemaId = BattleRuntimeAdapter.SAVE_SCHEMA_ID,
                schemaVersion = BattleRuntimeAdapter.CURRENT_SAVE_SCHEMA,
                payload = "ignored",
            ),
        )
        assertEquals(
            MySdRestoreIncompatibility.CONTENT,
            assertIs<MySdRestoreResult.Incompatible>(
                MySdAppFactory.restore(MySdSaveBundle(wrongContent, profile)),
            ).kind,
        )
    }

    @Test
    fun ledgerMismatchAndOversizedImportsAreRejectedWithoutPartialSession() {
        val bundle = MySdAppFactory.create(seed = 44).saveBundle()
        val tampered = bundle.profileSave.replaceFirst("softCurrency=500", "softCurrency=501")
        assertIs<MySdRestoreResult.Invalid>(MySdAppFactory.restore(bundle.copy(profileSave = tampered)))

        val oversized = "x".repeat(2_000_001)
        assertIs<MySdRestoreResult.Invalid>(MySdAppFactory.restore(MySdSaveBundle(null, oversized)))
    }

    @Test
    fun wallClockAnchorsPersistButDoNotEnterGameplayHash() {
        val first = MySdAppFactory.create(seed = 45)
        val second = MySdAppFactory.create(seed = 45)
        first.submit(MySdAppIntent.RefreshEnergy(100))
        second.submit(MySdAppIntent.RefreshEnergy(500))

        assertEquals(first.snapshot().clock.stableHash, second.snapshot().clock.stableHash)
        assertNotEquals(first.saveBundle().profileSave, second.saveBundle().profileSave)
    }

    @Test
    fun atomicBundleRejectsForeignRunAndChangedLaunchCoordinates() {
        fun started(seed: Long) = MySdAppFactory.create(seed).also {
            it.submit(MySdAppIntent.Navigate(MySdRoute.CAMPAIGN))
            it.submit(MySdAppIntent.SelectStage(OriginalProductCatalog.releaseOne().orderedStages.first().id.value))
            it.submit(MySdAppIntent.StartSelectedStage)
        }
        val first = started(81).saveBundle()
        val second = started(82).saveBundle()
        assertIs<MySdRestoreResult.Invalid>(MySdAppFactory.restore(first.copy(runSave = second.runSave)))
        val changedRoster = first.profileSave.replace("tower-ember-needle:1", "tower-ember-needle:2")
        assertIs<MySdRestoreResult.Invalid>(MySdAppFactory.restore(first.copy(profileSave = changedRoster)))
    }

    @Test
    fun malformedProfileCoordinatesCannotCreateInvalidProductSession() {
        val bundle = MySdAppFactory.create(83).saveBundle()
        listOf(
            "nextEventId" to Long.MAX_VALUE.toString(),
            "revision" to Long.MAX_VALUE.toString(),
            "maximumEnergy" to Int.MAX_VALUE.toString(),
            "unlockedStages" to "stage-ember-path,stage-brass-ravine",
            "shopPurchases" to "unknown-offer:1",
            "arenaRunCount" to Int.MAX_VALUE.toString(),
        ).forEach { (key, value) ->
            val changed = bundle.profileSave.replace(Regex("(?m)^$key=.*$"), "$key=$value")
            assertNotEquals(bundle.profileSave, changed, "The mutation must address an encoded coordinate: $key")
            assertIs<MySdRestoreResult.Invalid>(MySdAppFactory.restore(bundle.copy(profileSave = changed)), key)
        }
    }

    @Test
    fun unclaimedTerminalRewardIsRestoredOnBattleRouteAndCannotBeAbandoned() {
        val session = MySdAppFactory.create(84)
        session.submit(MySdAppIntent.Navigate(MySdRoute.CAMPAIGN))
        session.submit(MySdAppIntent.SelectStage(OriginalProductCatalog.releaseOne().orderedStages.first().id.value))
        session.submit(MySdAppIntent.StartSelectedStage)
        repeat(8) {
            val choice = session.snapshot().overlay as? dev.mysd.game.product.MySdOverlaySnapshot.EnhancementChoice
            if (choice != null) session.submit(MySdAppIntent.ChooseEnhancement(choice.offerIds.first()))
            session.step(20_000)
        }
        assertEquals(dev.mysd.game.product.MySdTerminalResult.DEFEAT, session.snapshot().battle?.terminalResult)
        assertEquals(dev.mysd.game.product.MySdAppRejection.TERMINAL_RUN,
            session.submit(MySdAppIntent.AbandonRun).rejection)
        val terminal = session.saveBundle()
        val hidden = terminal.copy(profileSave = terminal.profileSave.replace("route=BATTLE", "route=SHOP"))
        val restored = assertIs<MySdRestoreResult.Restored>(MySdAppFactory.restore(hidden)).session
        assertEquals(MySdRoute.BATTLE, restored.snapshot().route)
        assertTrue(restored.submit(MySdAppIntent.ClaimBattleReward).accepted)
        assertTrue(restored.submit(MySdAppIntent.AbandonRun).accepted)
    }
}
