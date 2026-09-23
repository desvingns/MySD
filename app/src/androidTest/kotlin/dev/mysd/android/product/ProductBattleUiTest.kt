package dev.mysd.android.product

import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.mysd.android.persistence.ProductPersistenceStorage
import dev.mysd.android.ui.theme.MySDTheme
import dev.mysd.game.product.MySdAppFactory
import dev.mysd.game.product.MySdRoute
import dev.mysd.game.product.MySdSaveBundle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProductBattleUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun buildDialogOffersEveryTowerAndAllyDeploymentIsTyped() {
        val actions = mutableListOf<ProductUiAction>()
        setProduct(ProductSurfaceUi.Battle(testBattleScene()), actions)

        composeTestRule.onNodeWithTag("product-battle-slot-slot-1").performClick()
        listOf(
            "tower-ember-needle",
            "tower-sunburst-mortar",
            "tower-glass-snare",
            "tower-seed-forge",
        ).forEach { towerId ->
            composeTestRule.onNodeWithTag("product-build-slot-1-$towerId").assertIsDisplayed()
        }
        composeTestRule
            .onNodeWithTag("product-build-slot-1-tower-glass-snare")
            .performClick()
        composeTestRule.onNodeWithTag("product-deploy-ally-cinder-guard").performClick()

        composeTestRule.runOnIdle {
            assertTrue(
                ProductUiAction.BuildTower("slot-1", "tower-glass-snare") in actions,
            )
            assertTrue(ProductUiAction.DeployAlly("ally-cinder-guard") in actions)
        }
    }

    @Test
    fun buildDialogSelectionDoesNotSurviveModalPhasesOrANewRun() {
        val actions = mutableListOf<ProductUiAction>()
        fun battle(runId: String, phase: BattlePhaseUi = BattlePhaseUi.ACTIVE) = ProductSurfaceUi.Battle(
            testBattleScene(
                phase = phase,
                result = if (phase == BattlePhaseUi.DEFEAT) {
                    BattleResultUi(false, 3, 14, 9, 0, claimed = false, multiplierAvailable = false)
                } else null,
            ).copy(runId = runId),
        )
        val surface = mutableStateOf(battle("run-a"))
        composeTestRule.setContent {
            MySDTheme(dynamicColor = false) {
                MySdProductApp(testApp(surface.value), onAction = { actions += it })
            }
        }
        // Real-device reproduction: the dialog was open when wave 2 ended; applying the wave-end
        // choice, and later retrying a new run, re-opened it for the old slot without any tap.
        listOf(BattlePhaseUi.INTER_WAVE_CHOICE, BattlePhaseUi.PAUSED, BattlePhaseUi.DEFEAT).forEach { phase ->
            composeTestRule.onNodeWithTag("product-battle-slot-slot-1").performClick()
            composeTestRule.onNodeWithTag("product-build-dialog").assertIsDisplayed()
            composeTestRule.runOnIdle { surface.value = battle("run-a", phase) }
            composeTestRule.onNodeWithTag("product-build-dialog").assertDoesNotExist()
            composeTestRule.runOnIdle { surface.value = battle("run-a") }
            composeTestRule.onNodeWithTag("product-build-dialog").assertDoesNotExist()
        }
        composeTestRule.onNodeWithTag("product-battle-slot-slot-1").performClick()
        composeTestRule.onNodeWithTag("product-build-dialog").assertIsDisplayed()
        composeTestRule.runOnIdle { surface.value = battle("run-b") }
        composeTestRule.onNodeWithTag("product-build-dialog").assertDoesNotExist()

        // A deliberate choice made during the wave intro stays open when the same run turns active.
        composeTestRule.runOnIdle { surface.value = battle("run-b", BattlePhaseUi.WAVE_INTRO) }
        composeTestRule.onNodeWithTag("product-battle-slot-slot-1").performClick()
        composeTestRule.onNodeWithTag("product-build-dialog").assertIsDisplayed()
        composeTestRule.runOnIdle { surface.value = battle("run-b") }
        composeTestRule.onNodeWithTag("product-build-dialog").assertIsDisplayed()
        composeTestRule.runOnIdle {
            assertTrue("Selecting a slot is presentation state, not a game action: $actions", actions.isEmpty())
        }
    }

    @Test
    fun pausedExitRequiresExplicitConfirmationAndCancelKeepsRun() {
        val actions = mutableListOf<ProductUiAction>()
        setProduct(
            ProductSurfaceUi.Battle(testBattleScene(BattlePhaseUi.PAUSED)),
            actions,
        )

        composeTestRule.onNodeWithTag("product-battle-exit").performClick()
        composeTestRule.onNodeWithTag("product-exit-confirmation").assertIsDisplayed()
        composeTestRule.onNodeWithTag("product-exit-cancel").performClick()
        composeTestRule.onNodeWithTag("product-battle-exit").assertIsDisplayed().performClick()
        composeTestRule.onNodeWithTag("product-exit-confirm").performClick()

        composeTestRule.runOnIdle {
            assertEquals(listOf(ProductUiAction.ConfirmExitBattle), actions)
        }
    }

    @Test
    fun enhancementRerollAndDefeatConsolationAreReachable() {
        val actions = mutableListOf<ProductUiAction>()
        val surface = androidx.compose.runtime.mutableStateOf(
            ProductSurfaceUi.Battle(testBattleScene(BattlePhaseUi.INTER_WAVE_CHOICE)),
        )
        composeTestRule.setContent {
            MySDTheme(dynamicColor = false) {
                MySdProductApp(testApp(surface.value), onAction = { actions += it })
            }
        }

        composeTestRule.onNodeWithTag("product-enhancement-reroll").performClick()
        composeTestRule.runOnIdle {
            surface.value = ProductSurfaceUi.Battle(
                testBattleScene(
                    phase = BattlePhaseUi.DEFEAT,
                    result = BattleResultUi(
                        victory = false,
                        completedWaves = 3,
                        defeatedEnemies = 14,
                        credits = 20,
                        experience = 4,
                        claimed = false,
                        multiplierAvailable = false,
                    ),
                ),
            )
        }
        composeTestRule.onNodeWithTag("product-claim-battle-reward").performClick()

        composeTestRule.runOnIdle {
            assertEquals(
                listOf(
                    ProductUiAction.RerollEnhancements,
                    ProductUiAction.ClaimBattleReward,
                ),
                actions,
            )
        }
    }

    @Test
    fun victoryOffersClaimBeforeRetryAndOptionalStubCheck() {
        val actions = mutableListOf<ProductUiAction>()
        setProduct(
            ProductSurfaceUi.Battle(
                testBattleScene(
                    phase = BattlePhaseUi.VICTORY,
                    result = BattleResultUi(
                        victory = true,
                        completedWaves = 10,
                        defeatedEnemies = 60,
                        credits = 90,
                        experience = 10,
                        claimed = false,
                        multiplierAvailable = true,
                    ),
                ),
            ),
            actions,
        )

        composeTestRule.onNodeWithTag("product-claim-battle-reward").assertIsDisplayed()
        composeTestRule.onNodeWithTag("product-rewarded-multiplier").performClick()
        composeTestRule.runOnIdle {
            assertEquals(listOf(ProductUiAction.ClaimBattleRewardMultiplier), actions)
        }
    }

    @Test
    fun stableCallbackKeepsExitConfirmationAcrossRepeatedSetupToBattleTransitions() {
        val storage = ExitFlowMemoryStorage()
        val model = ProductViewModel(storage, epochSeconds = { 1_000L })
        model.submit(ProductUiAction.EnterCampaign)
        model.submit(ProductUiAction.SelectStage("stage-ember-path"))
        val actions = mutableListOf<ProductUiAction>()
        val expectedActions = mutableListOf<ProductUiAction>()
        // Deliberately created once outside composition: recreating this callback or keying the
        // app by surface would conceal a dispatch lambda retaining an obsolete saveable holder.
        val stableOnAction: (ProductUiAction) -> Unit = { action ->
            actions += action
            model.submit(action)
        }
        lateinit var backDispatcher: OnBackPressedDispatcher
        composeTestRule.setContent {
            val snapshot by model.snapshot.collectAsState()
            val backOwner = checkNotNull(LocalOnBackPressedDispatcherOwner.current)
            SideEffect { backDispatcher = backOwner.onBackPressedDispatcher }
            MySDTheme(dynamicColor = false) {
                MySdProductApp(snapshot.toProductUiModel(), onAction = stableOnAction)
            }
        }

        repeat(2) { cycle ->
            composeTestRule.onNodeWithTag("product-stage-setup").assertIsDisplayed()
            composeTestRule.onNodeWithTag("product-exit-confirmation").assertDoesNotExist()
            composeTestRule.onNodeWithTag("product-start-battle")
                .performScrollTo().assertIsDisplayed().performClick()
            expectedActions += ProductUiAction.StartBattle
            composeTestRule.runOnIdle {
                assertEquals(MySdRoute.BATTLE, model.snapshot.value.route)
                // Advance the actual PREPARATION -> COMBAT transition, without a live ticker.
                model.pulse()
                val battle = model.snapshot.value.toProductUiModel().surface as ProductSurfaceUi.Battle
                assertEquals(BattlePhaseUi.ACTIVE, battle.scene.phase)
                assertEquals(expectedActions, actions)
            }
            composeTestRule.onNodeWithTag("product-battle").assertIsDisplayed()
            composeTestRule.onNodeWithTag("product-battle-pause").performClick()
            expectedActions += ProductUiAction.PauseOrResume
            composeTestRule.onNodeWithTag("product-pause-overlay").assertIsDisplayed()
            val pausedSnapshot = composeTestRule.runOnIdle {
                val battle = model.snapshot.value.toProductUiModel().surface as ProductSurfaceUi.Battle
                assertEquals(BattlePhaseUi.PAUSED, battle.scene.phase)
                model.snapshot.value
            }
            val pausedBundle = storage.bundle

            composeTestRule.onNodeWithTag("product-battle-exit").performScrollTo().performClick()
            composeTestRule.onNodeWithTag("product-exit-confirmation").assertIsDisplayed()
            composeTestRule.onNodeWithTag("product-exit-cancel").performScrollTo().performClick()
            composeTestRule.onNodeWithTag("product-exit-confirmation").assertDoesNotExist()
            composeTestRule.onNodeWithTag("product-pause-overlay").assertIsDisplayed()
            composeTestRule.runOnIdle {
                assertEquals(pausedSnapshot, model.snapshot.value)
                assertEquals(pausedBundle, storage.bundle)
                // Neither RequestExitBattle nor CancelExitBattle may escape presentation.
                assertEquals(expectedActions, actions)
                backDispatcher.onBackPressed()
            }
            composeTestRule.onNodeWithTag("product-exit-confirmation").assertIsDisplayed()
            composeTestRule.runOnIdle {
                assertEquals(pausedSnapshot, model.snapshot.value)
                assertEquals(pausedBundle, storage.bundle)
                assertEquals(expectedActions, actions)
            }
            composeTestRule.onNodeWithTag("product-exit-confirm").performScrollTo().performClick()
            expectedActions += ProductUiAction.ConfirmExitBattle
            composeTestRule.onNodeWithTag("product-exit-confirmation").assertDoesNotExist()
            composeTestRule.onNodeWithTag("product-pause-overlay").assertDoesNotExist()
            composeTestRule.onNodeWithTag("product-campaign-map").assertIsDisplayed()
            composeTestRule.runOnIdle {
                assertEquals(expectedActions, actions)
                assertEquals(cycle + 1, actions.count { it == ProductUiAction.ConfirmExitBattle })
                assertEquals(MySdRoute.CAMPAIGN, model.snapshot.value.route)
                assertNull(model.snapshot.value.battle)
                assertNull(storage.bundle.runSave)
            }
            if (cycle == 0) {
                composeTestRule.onNodeWithTag("product-stage-open-stage-ember-path")
                    .performScrollTo().performClick()
                expectedActions += ProductUiAction.SelectStage("stage-ember-path")
            }
        }
    }

    @Test
    fun modalBattlePhasesBlockCoveredHudInputAndSemanticsButKeepOverlayActions() {
        val actions = mutableListOf<ProductUiAction>()
        val surface = mutableStateOf(ProductSurfaceUi.Battle(testBattleScene()))
        val stableOnAction: (ProductUiAction) -> Unit = { actions += it }
        composeTestRule.setContent {
            MySDTheme(dynamicColor = false) {
                MySdProductApp(testApp(surface.value), onAction = stableOnAction)
            }
        }
        val cases = listOf(
            Triple(BattlePhaseUi.PAUSED, "product-battle-resume", ProductUiAction.PauseOrResume),
            Triple(
                BattlePhaseUi.INTER_WAVE_CHOICE,
                "product-enhancement-choose-enhancement-keen-sparks",
                ProductUiAction.SelectEnhancement("enhancement-keen-sparks"),
            ),
            Triple(BattlePhaseUi.VICTORY, "product-claim-battle-reward", ProductUiAction.ClaimBattleReward),
            Triple(BattlePhaseUi.DEFEAT, "product-claim-battle-reward", ProductUiAction.ClaimBattleReward),
        )
        val hudTags = listOf("product-battle-speed", "product-battle-pause")
        cases.forEach { (phase, primaryTag, expectedAction) ->
            composeTestRule.runOnIdle {
                actions.clear()
                surface.value = ProductSurfaceUi.Battle(testBattleScene())
            }
            val battleBounds = unclippedBattleNodeBounds("product-battle")
            val activeHudBounds = hudTags.associateWith { tag ->
                composeTestRule.onNodeWithTag(tag).assertIsDisplayed().assertIsEnabled()
                unclippedBattleNodeBounds(tag).also { bounds ->
                    assertTrue("Active $tag must be wholly inside the battle before a modal covers it",
                        bounds.width > 0f && bounds.height > 0f &&
                            bounds.left >= battleBounds.left && bounds.top >= battleBounds.top &&
                            bounds.right <= battleBounds.right && bounds.bottom <= battleBounds.bottom)
                }
            }
            composeTestRule.runOnIdle {
                surface.value = ProductSurfaceUi.Battle(testBattleScene(
                    phase = phase,
                    result = if (phase == BattlePhaseUi.VICTORY || phase == BattlePhaseUi.DEFEAT) {
                        BattleResultUi(
                            victory = phase == BattlePhaseUi.VICTORY,
                            completedWaves = 3,
                            defeatedEnemies = 14,
                            credits = 20,
                            experience = 4,
                            claimed = false,
                            multiplierAvailable = false,
                        )
                    } else null,
                ))
            }
            // The merged tree is the user-facing semantics tree; the unmerged tree deliberately
            // retains hidden descendants so disabled HUD controls can also be checked directly.
            hudTags.forEach { tag ->
                composeTestRule.onNodeWithTag(tag).assertDoesNotExist()
                composeTestRule.onNodeWithTag(tag, useUnmergedTree = true).assertIsNotEnabled()
            }
            composeTestRule.onNodeWithTag("product-battle-slot-slot-1").assertDoesNotExist()
            composeTestRule.onNodeWithTag("product-enemy-enemy-1").assertDoesNotExist()

            val modalClickBounds = composeTestRule.onAllNodes(hasClickAction())
                .fetchSemanticsNodes().map { it.boundsInRoot }
            activeHudBounds.forEach { (tag, bounds) ->
                // Use the measured HUD region, never hard-coded screen pixels. Avoid a genuine
                // foreground action: a tap on one must not be mistaken for background leakage.
                val candidates = listOf(0.5f, 0.25f, 0.75f).flatMap { xFraction ->
                    listOf(0.5f, 0.25f, 0.75f).map { yFraction ->
                        Offset(bounds.left + bounds.width * xFraction, bounds.top + bounds.height * yFraction)
                    }
                }
                val point = requireNotNull(candidates.firstOrNull { candidate ->
                    battleBounds.contains(candidate) && modalClickBounds.none { it.contains(candidate) }
                }) { "No uncovered $tag probe point in $phase; the fixture cannot test background hit-through." }
                composeTestRule.onNodeWithTag("product-battle").performTouchInput {
                    click(point - battleBounds.topLeft)
                }
                composeTestRule.runOnIdle {
                    assertTrue("Covered HUD $tag dispatched an action in $phase: $actions", actions.isEmpty())
                    assertEquals(phase, surface.value.scene.phase)
                }
            }
            composeTestRule.onNodeWithTag(primaryTag)
                .performScrollTo().assertIsDisplayed().assertIsEnabled().performClick()
            composeTestRule.runOnIdle {
                assertEquals("The $phase overlay must remain independently interactive",
                    listOf(expectedAction), actions)
            }
        }
    }

    private fun unclippedBattleNodeBounds(tag: String): Rect {
        val node = composeTestRule.onNodeWithTag(tag).fetchSemanticsNode()
        // Semantics position + actual layout size avoids silently shrinking the probe to a
        // clipped viewport rectangle, while staying in the same root coordinates as touch input.
        return Rect(
            left = node.positionInRoot.x,
            top = node.positionInRoot.y,
            right = node.positionInRoot.x + node.size.width,
            bottom = node.positionInRoot.y + node.size.height,
        )
    }

    /** Real facade/mapper flow with no access to the device's persistent player profile. */
    private class ExitFlowMemoryStorage : ProductPersistenceStorage {
        var bundle = MySdAppFactory.create(seed = 47L).saveBundle()
        override fun hasProductProfile() = true
        override fun loadRunSave() = bundle.runSave
        override fun loadProfileSave() = bundle.profileSave
        override fun save(bundle: MySdSaveBundle, archivedLegacyRun: String?): Boolean {
            this.bundle = bundle
            return true
        }
        override fun archiveRejectedBundle(profile: String?, run: String?, reason: String): Boolean =
            error("The valid in-memory exit scenario must not require recovery archival: $reason")
    }

    private fun setProduct(
        surface: ProductSurfaceUi,
        actions: MutableList<ProductUiAction>,
    ) {
        composeTestRule.setContent {
            MySDTheme(dynamicColor = false) {
                MySdProductApp(testApp(surface), onAction = { actions += it })
            }
        }
    }
}
