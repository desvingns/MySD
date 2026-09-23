package dev.mysd.android.product

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.mysd.android.persistence.ProductPersistenceStorage
import dev.mysd.android.ui.theme.MySDTheme
import dev.mysd.game.product.MySdAppFactory
import dev.mysd.game.product.MySdSaveBundle
import dev.mysd.game.product.MySdSurfaceSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Actual ViewModel/facade/mapper path; memory storage never alters the device's real profile. */
@RunWith(AndroidJUnit4::class)
class ProductHeroLoadoutUiTest {
    @get:Rule val rule = createComposeRule()

    @Test fun compactSetupSelectsAndPersistsHeroSkills() = verifySetup(1f)
    @Test fun largeFontSetupSelectsAndPersistsHeroSkills() = verifySetup(2f)

    private fun verifySetup(fontScale: Float) {
        val storage = MemoryStorage()
        val model = ProductViewModel(storage, epochSeconds = { 1_000L })
        model.submit(ProductUiAction.EnterCampaign)
        model.submit(ProductUiAction.SelectStage("stage-ember-path"))
        val combatLoadout = (model.snapshot.value.surface as MySdSurfaceSnapshot.StageSetup).selectedLoadoutIds
        rule.setContent {
            val snapshot by model.snapshot.collectAsState()
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, fontScale)) {
                Box(Modifier.size(320.dp, 480.dp)) {
                    MySDTheme(dynamicColor = false) {
                        MySdProductApp(snapshot.toProductUiModel(), onAction = model::submit)
                    }
                }
            }
        }
        val repair = rule.onNodeWithTag("product-setup-hero-hero-hearth-repair")
        repair.performScrollTo().assertIsDisplayed().assertIsOn()
            .assertWidthIsAtLeast(200.dp).assertHeightIsAtLeast(48.dp).performClick()
        repair.assertIsOff()
        rule.onNodeWithTag("product-setup-hero-hero-solar-pulse").assertDoesNotExist()
        rule.runOnIdle {
            val setup = model.snapshot.value.surface as MySdSurfaceSnapshot.StageSetup
            assertTrue(setup.selectedHeroSkillIds.isEmpty())
            assertEquals(combatLoadout, setup.selectedLoadoutIds)
            val restored = ProductViewModel(storage, epochSeconds = { 1_000L })
            val restoredSetup = restored.snapshot.value.surface as MySdSurfaceSnapshot.StageSetup
            assertTrue(restoredSetup.selectedHeroSkillIds.isEmpty())
            assertTrue((restored.snapshot.value.toProductUiModel().surface as ProductSurfaceUi.Setup)
                .heroChoices.none { it.selected })
        }
        repair.performClick().assertIsOn()
        repair.performClick().assertIsOff()
        rule.onNodeWithTag("product-start-battle").performScrollTo().performClick()
        rule.runOnIdle {
            val battle = model.snapshot.value.toProductUiModel().surface as ProductSurfaceUi.Battle
            assertTrue(battle.scene.abilities.isEmpty())
            val restored = ProductViewModel(storage, epochSeconds = { 1_000L })
            val restoredBattle = restored.snapshot.value.toProductUiModel().surface as ProductSurfaceUi.Battle
            assertTrue(restoredBattle.scene.abilities.isEmpty())
            assertEquals(model.snapshot.value.clock.tick, restored.snapshot.value.clock.tick)
        }
    }

    @Test fun rosterHeroSelectionIsIndependentOfCombatSlotsAndRejectsLockedHeroes() {
        val storage = MemoryStorage()
        val model = ProductViewModel(storage, epochSeconds = { 1_000L })
        model.submit(ProductUiAction.Navigate(ProductDestinationUi.ROSTER))
        val before = model.snapshot.value.surface as MySdSurfaceSnapshot.Roster
        assertEquals(listOf("hero-hearth-repair"), before.selectedHeroSkillIds)
        assertTrue("hero-hearth-repair" in
            (model.snapshot.value.toProductUiModel().surface as ProductSurfaceUi.Roster).equippedIds)
        model.submit(ProductUiAction.ToggleHeroSkill("hero-hearth-repair"))
        val removed = model.snapshot.value.surface as MySdSurfaceSnapshot.Roster
        assertTrue(removed.selectedHeroSkillIds.isEmpty())
        assertEquals(before.selectedLoadoutIds, removed.selectedLoadoutIds)
        assertTrue("hero-hearth-repair" !in
            (model.snapshot.value.toProductUiModel().surface as ProductSurfaceUi.Roster).equippedIds)
        val durable = storage.bundle
        model.submit(ProductUiAction.ToggleHeroSkill("hero-solar-pulse"))
        assertEquals(durable, storage.bundle)
        // The generic roster action remains compatible with callers that identify entries by ID.
        model.submit(ProductUiAction.ToggleRosterEntry("hero-hearth-repair"))
        val equipped = model.snapshot.value.surface as MySdSurfaceSnapshot.Roster
        assertEquals(before.selectedHeroSkillIds, equipped.selectedHeroSkillIds)
        assertEquals(before.selectedLoadoutIds, equipped.selectedLoadoutIds)
        val restored = ProductViewModel(storage, epochSeconds = { 1_000L })
        assertEquals(equipped.selectedHeroSkillIds,
            (restored.snapshot.value.surface as MySdSurfaceSnapshot.Roster).selectedHeroSkillIds)
    }

    private class MemoryStorage : ProductPersistenceStorage {
        var bundle = MySdAppFactory.create(seed = 47L).saveBundle()
        override fun hasProductProfile() = true
        override fun loadRunSave() = bundle.runSave
        override fun loadProfileSave() = bundle.profileSave
        override fun save(bundle: MySdSaveBundle, archivedLegacyRun: String?): Boolean {
            this.bundle = bundle
            return true
        }
        override fun archiveRejectedBundle(profile: String?, run: String?, reason: String) = false
    }
}
