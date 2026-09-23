package dev.mysd.android.product

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.mysd.android.R
import dev.mysd.android.ui.theme.MySDTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProductMetaUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun rosterAllowsRemovingUnitsAndSelectingThenRemovingTheLastHero() {
        val actions = mutableListOf<ProductUiAction>()
        val roster = mutableStateOf(ProductSurfaceUi.Roster(
            entries = listOf(
                rosterEntry("tower-ember-needle", RosterCategoryUi.TOWER),
                rosterEntry("tower-glass-snare", RosterCategoryUi.TOWER),
                rosterEntry("ally-cinder-guard", RosterCategoryUi.ALLY),
                rosterEntry("hero-solar-pulse", RosterCategoryUi.HERO),
            ),
            equippedIds = setOf(
                "tower-ember-needle",
                "tower-glass-snare",
                "ally-cinder-guard",
            ),
            maxEquipped = 7,
        ))
        composeTestRule.setContent {
            MySDTheme(dynamicColor = false) {
                MySdProductApp(testApp(roster.value), onAction = { action ->
                    actions += action
                    if (action is ProductUiAction.ToggleHeroSkill) {
                        val selected = roster.value.equippedIds
                        roster.value = roster.value.copy(
                            equippedIds = if (action.skillId in selected) {
                                selected - action.skillId
                            } else {
                                selected + action.skillId
                            },
                        )
                    }
                })
            }
        }

        composeTestRule.onNodeWithTag("product-roster-toggle-tower-ember-needle")
            .performScrollTo()
            .performClick()
        composeTestRule.onNodeWithTag("product-roster-toggle-hero-solar-pulse")
            .performScrollTo().assertIsEnabled().performClick()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        composeTestRule.onNodeWithTag("product-roster-toggle-hero-solar-pulse")
            .performScrollTo().assertIsEnabled()
            .assertTextContains(context.getString(R.string.product_roster_remove))
            .performClick()
        composeTestRule.onNodeWithTag("product-roster-toggle-hero-solar-pulse")
            .assertTextContains(context.getString(R.string.product_roster_equip))
        composeTestRule.onNodeWithTag("product-roster-upgrade-hero-solar-pulse")
            .performScrollTo()
            .assertExists()
        composeTestRule.runOnIdle {
            assertEquals(
                listOf(
                    ProductUiAction.ToggleRosterEntry("tower-ember-needle"),
                    ProductUiAction.ToggleHeroSkill("hero-solar-pulse"),
                    ProductUiAction.ToggleHeroSkill("hero-solar-pulse"),
                ),
                actions,
            )
            assertTrue(roster.value.equippedIds.none { it.startsWith("hero-") })
        }
    }

    @Test
    fun rosterKeepsLastCombatRolesAndMaximumLevelsAndExcludesLockedOrMissingHeroes() {
        val actions = mutableListOf<ProductUiAction>()
        setProduct(
            ProductSurfaceUi.Roster(
                entries = listOf(
                    rosterEntry("tower-ember-needle", RosterCategoryUi.TOWER).copy(
                        level = 5,
                        upgradeCost = 0,
                        maximumLevel = true,
                    ),
                    rosterEntry("ally-cinder-guard", RosterCategoryUi.ALLY),
                    rosterEntry("hero-solar-pulse", RosterCategoryUi.HERO).copy(
                        unlocked = false,
                        unlockStage = "Край искр",
                    ),
                ),
                equippedIds = setOf("tower-ember-needle", "ally-cinder-guard"),
                maxEquipped = 7,
            ),
            actions,
        )

        listOf("tower-ember-needle", "ally-cinder-guard").forEach { id ->
            composeTestRule.onNodeWithTag("product-roster-toggle-$id")
                .performScrollTo().assertIsNotEnabled()
        }
        composeTestRule.onNodeWithTag("product-roster-upgrade-tower-ember-needle")
            .performScrollTo().assertIsNotEnabled()
        composeTestRule.onNodeWithTag("product-roster-hero-solar-pulse")
            .performScrollTo().assertExists()
        listOf("hero-solar-pulse", "hero-missing").forEach { id ->
            composeTestRule.onNodeWithTag("product-roster-toggle-$id").assertDoesNotExist()
            composeTestRule.onNodeWithTag("product-roster-upgrade-$id").assertDoesNotExist()
        }
        composeTestRule.runOnIdle { assertTrue(actions.isEmpty()) }
    }

    @Test
    fun shopRoutesRewardedAndPurchaseStubsSeparately() {
        val actions = mutableListOf<ProductUiAction>()
        val shop = ProductSurfaceUi.Shop(
            products = listOf(
                ShopProductUi(
                    "shop-rewarded-cache",
                    "Сигнальный тайник",
                    "Локальная награда",
                    ShopProductKindUi.REWARDED_STUB,
                    0,
                    affordable = false,
                    owned = false,
                ),
                ShopProductUi(
                    "shop-purchase-cache",
                    "Экспедиционный контейнер",
                    "Локальная покупка",
                    ShopProductKindUi.PURCHASE_STUB,
                    0,
                    affordable = false,
                    owned = false,
                ),
            ),
        )
        setProduct(shop, actions)

        composeTestRule.onNodeWithTag("product-shop-action-shop-rewarded-cache")
            .performScrollTo()
            .performClick()
        composeTestRule.onNodeWithTag("product-shop-action-shop-purchase-cache")
            .performScrollTo()
            .performClick()

        composeTestRule.runOnIdle {
            assertEquals(
                listOf(
                    ProductUiAction.RequestRewardedStub("shop-rewarded-cache"),
                    ProductUiAction.RequestPurchaseStub("shop-purchase-cache"),
                ),
                actions,
            )
        }
    }

    @Test
    fun allFifteenRewardTiersAndLocalArenaEntryAreAddressable() {
        val actions = mutableListOf<ProductUiAction>()
        val surface = androidx.compose.runtime.mutableStateOf<ProductSurfaceUi>(
            ProductSurfaceUi.RewardTrack(
                tiers = (1..15).map { tier ->
                    RewardTierUi(
                        id = "reward-tier-$tier",
                        tier = tier,
                        requiredExperience = tier * 10,
                        rewardLabel = "Контейнер маршрута $tier",
                        status = if (tier <= 4) {
                            RewardTierStatusUi.AVAILABLE
                        } else {
                            RewardTierStatusUi.LOCKED
                        },
                    )
                },
                experience = 46,
            ),
        )
        composeTestRule.setContent {
            MySDTheme(dynamicColor = false) {
                MySdProductApp(testApp(surface.value), onAction = { actions += it })
            }
        }

        composeTestRule.onNodeWithTag("product-track-tier-15").assertExists()
        composeTestRule.onNodeWithTag("product-track-claim-4")
            .performScrollTo()
            .performClick()
        composeTestRule.runOnIdle {
            surface.value = ProductSurfaceUi.Arena(
                ArenaUi.Lobby(rating = 790, tickets = 1, recentResults = emptyList()),
            )
        }
        composeTestRule.onNodeWithTag("product-arena-find").performScrollTo().performClick()

        composeTestRule.runOnIdle {
            assertTrue(ProductUiAction.ClaimRewardTier("reward-tier-4") in actions)
            assertTrue(ProductUiAction.ArenaFindOpponent in actions)
        }
    }

    private fun rosterEntry(id: String, category: RosterCategoryUi) = RosterEntryUi(
        id = id,
        title = "Боевой модуль",
        role = "Роль",
        level = 2,
        power = 360,
        upgradeCost = 80,
        affordable = true,
        unlocked = true,
        unlockStage = null,
        category = category,
    )

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
