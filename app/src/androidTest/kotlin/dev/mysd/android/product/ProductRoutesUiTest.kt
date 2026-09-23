package dev.mysd.android.product

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.mysd.android.ui.theme.MySDTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProductRoutesUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun everyProductRouteHasAStableSemanticRoot() {
        val model = mutableStateOf(testApp(ProductSurfaceUi.Launch()))
        composeTestRule.setContent {
            MySDTheme(dynamicColor = false) {
                MySdProductApp(model.value, onAction = {})
            }
        }

        assertRoute(model, ProductSurfaceUi.Launch(), "product-launch")
        assertRoute(
            model,
            ProductSurfaceUi.CampaignMap(
                listOf(
                    CampaignStageUi(
                        "stage-ember-path",
                        "Тропа углей",
                        "Угольный сектор",
                        StageStatusUi.AVAILABLE,
                        0,
                        2,
                        10,
                    ),
                ),
            ),
            "product-campaign-map",
        )
        assertRoute(model, testSetup(), "product-stage-setup")
        assertRoute(
            model,
            ProductSurfaceUi.Battle(testBattleScene()),
            "product-battle",
        )
        assertRoute(
            model,
            ProductSurfaceUi.Roster(emptyList(), emptySet(), 7),
            "product-roster",
        )
        assertRoute(
            model,
            ProductSurfaceUi.Tech(
                listOf(
                    TechNodeUi(
                        "tech-1-1",
                        "Калибровка линз",
                        "Башенная матрица",
                        TechNodeStatusUi.AVAILABLE,
                        70,
                        "Башенная матрица",
                        true,
                        0,
                        0,
                    ),
                ),
            ),
            "product-tech",
        )
        assertRoute(model, ProductSurfaceUi.Shop(emptyList()), "product-shop")
        assertRoute(
            model,
            ProductSurfaceUi.RewardTrack(emptyList(), 0),
            "product-reward-track",
        )
        assertRoute(
            model,
            ProductSurfaceUi.Settings(ProductSettingUi.entries.associateWith { true }),
            "product-settings",
        )
        assertRoute(
            model,
            ProductSurfaceUi.Arena(
                ArenaUi.Lobby(rating = 0, tickets = 1, recentResults = emptyList()),
            ),
            "product-arena",
        )
    }

    @Test
    fun resumeAndLocalServiceDialogsExposeStableAnchors() {
        val model = mutableStateOf(
            testApp(
                ProductSurfaceUi.Launch(),
                ProductOverlayUi.ResumeRun("Тропа углей"),
            ),
        )
        composeTestRule.setContent {
            MySDTheme(dynamicColor = false) {
                MySdProductApp(model.value, onAction = {})
            }
        }
        composeTestRule.onNodeWithTag("resume-overlay").assertExists()
        composeTestRule.runOnIdle {
            model.value = testApp(
                ProductSurfaceUi.Shop(emptyList()),
                ProductOverlayUi.ServiceResult(
                    ServiceResultKindUi.REWARDED,
                    "Локальная заглушка",
                ),
            )
        }
        composeTestRule.onNodeWithTag("product-service-dialog").assertExists()
    }

    private fun assertRoute(
        state: androidx.compose.runtime.MutableState<ProductUiModel>,
        surface: ProductSurfaceUi,
        tag: String,
    ) {
        composeTestRule.runOnIdle { state.value = testApp(surface) }
        composeTestRule.onNodeWithTag(tag).assertExists()
    }

    private fun testSetup() = ProductSurfaceUi.Setup(
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
}
