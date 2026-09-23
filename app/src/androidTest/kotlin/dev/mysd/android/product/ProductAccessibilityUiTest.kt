package dev.mysd.android.product

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.mysd.android.R
import dev.mysd.android.ui.theme.MySDTheme
import org.junit.Rule
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProductAccessibilityUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun compactLargeFontSettingsRemainLabeledWholeRowTouchTargets() {
        val actions = mutableListOf<ProductUiAction>()
        composeTestRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 2f)) {
                Box(Modifier.width(320.dp).height(480.dp)) {
                    MySDTheme(dynamicColor = false) {
                        MySdProductApp(
                            testApp(ProductSurfaceUi.Settings(ProductSettingUi.entries.associateWith { true })),
                            onAction = { actions += it },
                        )
                    }
                }
            }
        }
        ProductSettingUi.entries.forEach { setting ->
            composeTestRule.onNodeWithTag("product-setting-${setting.name.lowercase()}")
                .performScrollTo().assertIsDisplayed().assertHeightIsAtLeast(48.dp).performClick()
        }
        composeTestRule.runOnIdle {
            assertEquals(ProductSettingUi.entries.map { ProductUiAction.ToggleSetting(it) }, actions)
        }
    }

    @Test
    fun battleEntitiesAndActionsExposeAccessibleSemanticsAndTouchTargets() {
        composeTestRule.setContent {
            MySDTheme(dynamicColor = false) {
                MySdProductApp(
                    testApp(ProductSurfaceUi.Battle(testBattleScene())),
                    onAction = {},
                )
            }
        }

        composeTestRule.onNodeWithContentDescription(
            context.getString(
                R.string.product_battle_enemy_description,
                "Пепельный бегун",
                12,
                18,
            ),
        ).assertIsDisplayed()
        composeTestRule.onNodeWithTag("product-battle-pause")
            .assertHeightIsAtLeast(48.dp)
            .assertWidthIsAtLeast(48.dp)
        composeTestRule.onNodeWithTag("product-deploy-ally-cinder-guard")
            .assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun compactViewportWithLargeFontKeepsSetupPrimaryActionReachable() {
        val setup = ProductSurfaceUi.Setup(
            stageId = "stage-ember-path",
            title = "Тропа углей",
            threatLabel = "Угольный сектор",
            waveCount = 10,
            energyCost = 2,
            energyAvailable = true,
            sweepAvailable = false,
            heroName = "Командир Маяка",
            loadout = listOf(
                LoadoutSlotUi(0, "tower-ember-needle", "Угольная игла", LoadoutKindUi.TOWER),
                LoadoutSlotUi(1, "ally-cinder-guard", "Пепельный страж", LoadoutKindUi.ALLY),
            ),
            availableLoadout = emptyList(),
            canStart = true,
        )
        composeTestRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, fontScale = 2f),
            ) {
                Box(Modifier.width(320.dp).height(480.dp)) {
                    MySDTheme(dynamicColor = false) {
                        MySdProductApp(testApp(setup), onAction = {})
                    }
                }
            }
        }

        composeTestRule.onNodeWithTag("product-start-battle")
            .performScrollTo()
            .assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp)
        composeTestRule.onNodeWithTag("product-resource-energy").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("product-resource-credits").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("product-resource-crystals").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("open-reward-track")
            .assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun compactViewportWithLargeFontKeepsTerminalActionsReachable() {
        val terminal = ProductSurfaceUi.Battle(
            testBattleScene(
                phase = BattlePhaseUi.DEFEAT,
                result = BattleResultUi(
                    victory = false,
                    completedWaves = 8,
                    defeatedEnemies = 42,
                    credits = 55,
                    experience = 8,
                    claimed = false,
                    multiplierAvailable = false,
                ),
            ),
        )
        composeTestRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, fontScale = 2f),
            ) {
                Box(Modifier.width(320.dp).height(480.dp)) {
                    MySDTheme(dynamicColor = false) {
                        MySdProductApp(testApp(terminal), onAction = {})
                    }
                }
            }
        }

        composeTestRule.onNodeWithTag("product-claim-battle-reward")
            .performScrollTo()
            .assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp)
        composeTestRule.onNodeWithTag("product-return-map")
            .performScrollTo()
            .assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun compactViewportWithLargeFontKeepsMetaHeaderAndShopActionReachable() {
        val shop = ProductSurfaceUi.Shop(
            listOf(
                ShopProductUi(
                    id = "shop-rewarded-cache",
                    title = "Сигнальный тайник",
                    description = "Локальная заглушка без рекламы, сети и начисления награды.",
                    kind = ShopProductKindUi.REWARDED_STUB,
                    cost = 0,
                    affordable = false,
                    owned = false,
                ),
            ),
        )
        composeTestRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, fontScale = 2f),
            ) {
                Box(Modifier.width(320.dp).height(480.dp)) {
                    MySDTheme(dynamicColor = false) {
                        MySdProductApp(testApp(shop), onAction = {})
                    }
                }
            }
        }

        composeTestRule.onNodeWithTag("product-resource-energy").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("product-resource-credits").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("product-resource-crystals").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("open-reward-track").assertIsDisplayed()
        composeTestRule.onNodeWithTag("product-shop-action-shop-rewarded-cache")
            .performScrollTo()
            .assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp)
    }
}
