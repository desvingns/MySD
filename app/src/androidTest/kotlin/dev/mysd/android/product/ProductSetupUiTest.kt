package dev.mysd.android.product

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.mysd.android.ui.theme.MySDTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProductSetupUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun setupRendersCurrentSlotsAndOneLegalAppendSlot() {
        val actions = mutableListOf<ProductUiAction>()
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
                LoadoutSlotUi(2, null, null, null),
            ),
            availableLoadout = listOf(
                SetupChoiceUi(
                    "tower-ember-needle",
                    "Угольная игла",
                    "Башня",
                    LoadoutKindUi.TOWER,
                ),
                SetupChoiceUi(
                    "tower-glass-snare",
                    "Стеклянная сеть",
                    "Башня",
                    LoadoutKindUi.TOWER,
                ),
                SetupChoiceUi(
                    "ally-cinder-guard",
                    "Пепельный страж",
                    "Союзник",
                    LoadoutKindUi.ALLY,
                ),
            ),
            canStart = true,
        )
        composeTestRule.setContent {
            MySDTheme(dynamicColor = false) {
                MySdProductApp(testApp(setup), onAction = { actions += it })
            }
        }

        composeTestRule.onNodeWithTag("product-loadout-slot-2").assertExists()
        composeTestRule.onNodeWithTag("product-loadout-0-tower-glass-snare")
            .performScrollTo()
            .performClick()
        composeTestRule.onNodeWithTag("product-loadout-2-ally-cinder-guard")
            .performScrollTo()
            .performClick()

        composeTestRule.runOnIdle {
            assertEquals(
                listOf(
                    ProductUiAction.SetLoadoutSlot(0, "tower-glass-snare"),
                    ProductUiAction.SetLoadoutSlot(2, "ally-cinder-guard"),
                ),
                actions,
            )
        }
    }
}
