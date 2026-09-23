package dev.mysd.android.product

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.MultiParagraph
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.mysd.android.R
import dev.mysd.android.ui.theme.MySDTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Regressions for the measured compact/native card squeeze, not reference-parity assertions. */
@RunWith(AndroidJUnit4::class)
class ProductMetaLayoutTest {
    @get:Rule
    val rule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun compactMetaCardsKeepCopyAndActionsReadable() = verifyMetaLayout(320, 1f)
    @Test fun nativeWidthMetaCardsKeepCopyAndActionsReadable() = verifyMetaLayout(411, 1f)
    @Test fun compactLargeFontMetaCardsKeepCopyAndActionsReadable() = verifyMetaLayout(320, 2f)
    @Test fun nativeWidthLargeFontMetaCardsKeepCopyAndActionsReadable() = verifyMetaLayout(411, 2f)

    private fun verifyMetaLayout(widthDp: Int, fontScale: Float) {
        val actions = mutableListOf<ProductUiAction>()
        val surface = mutableStateOf<ProductSurfaceUi>(shop())
        rule.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, fontScale)) {
                Box(Modifier.size(widthDp.dp, 480.dp)) {
                    MySDTheme(dynamicColor = false) {
                        key(surface.value) {
                            MySdProductApp(testApp(surface.value), onAction = { actions += it })
                        }
                    }
                }
            }
        }

        val scrollableNavigation = widthDp < 360 || fontScale >= 1.5f
        listOf(
            "campaign" to R.string.product_nav_campaign,
            "roster" to R.string.product_nav_roster,
            "tech" to R.string.product_nav_tech,
            "shop" to R.string.product_nav_shop,
            "arena" to R.string.product_nav_arena,
        ).forEach { (id, labelId) ->
            val button = rule.onNodeWithTag("product-nav-$id")
            if (scrollableNavigation) button.performScrollTo()
            button.assertIsDisplayed().assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp)
            val label = rule.onNodeWithTag("product-nav-label-$id", useUnmergedTree = true)
            label.assertIsDisplayed().assertTextEquals(context.getString(labelId))
            assertTextFits(label, scrollTo = false, requireSingleLine = true)
        }

        val shopTitle = rule.onNodeWithTag("product-shop-title-stub", useUnmergedTree = true)
        val shopBadge = rule.onNodeWithTag("product-shop-badge-stub", useUnmergedTree = true)
        assertTextFits(shopTitle, keepWordsWhole = false)
        assertTextFits(shopBadge)
        shopBadge.assertWidthIsAtLeast(200.dp)
        // Use unclipped layout positions: one element may lie above the scrolled viewport.
        val titleNode = shopTitle.fetchSemanticsNode()
        val badgeNode = shopBadge.fetchSemanticsNode()
        assertTrue(
            "Demo badge must sit below the title, never overlap it",
            badgeNode.positionInRoot.y >= titleNode.positionInRoot.y + titleNode.size.height - 1f,
        )
        rule.onNodeWithTag("product-shop-action-stub")
            .performScrollTo().assertIsDisplayed().assertHeightIsAtLeast(48.dp).performClick()
        rule.onNodeWithTag("product-shop-action-owned")
            .performScrollTo().assertIsNotEnabled()

        rule.runOnIdle { surface.value = tech() }
        listOf("first", "second", "third").forEach { id ->
            assertTextFits(rule.onNodeWithTag("product-tech-title-$id", useUnmergedTree = true))
        }
        val first = rule.onNodeWithTag("product-tech-first").fetchSemanticsNode()
        val second = rule.onNodeWithTag("product-tech-second").fetchSemanticsNode()
        if (widthDp == 411 && fontScale == 1f) {
            assertTrue("411dp at 1x should retain two readable columns", second.positionInRoot.x > first.positionInRoot.x)
        } else {
            assertEquals("Compact/enlarged type should use one column", first.positionInRoot.x, second.positionInRoot.x, 1f)
        }
        val unlockLabel = context.getString(R.string.product_tech_unlock, 80)
        assertTextFits(rule.onNode(
            hasText(unlockLabel) and hasAnyAncestor(hasTestTag("product-tech-unlock-first")),
            useUnmergedTree = true,
        ))
        rule.onNodeWithTag("product-tech-unlock-first")
            .performScrollTo().assertIsDisplayed().assertHeightIsAtLeast(48.dp).performClick()
        rule.onNodeWithTag("product-tech-unlock-second")
            .performScrollTo().assertIsNotEnabled()
        rule.onNodeWithTag("product-tech-unlock-third").assertDoesNotExist()

        rule.runOnIdle { surface.value = rewards() }
        val rewardCopy = rule.onNodeWithTag("product-track-copy-1", useUnmergedTree = true)
        assertTextFits(rewardCopy)
        rewardCopy.assertWidthIsAtLeast(200.dp)
        rule.onNodeWithTag("product-track-claim-1")
            .performScrollTo().assertIsDisplayed().assertWidthIsAtLeast(200.dp)
            .assertHeightIsAtLeast(48.dp).performClick()
        val copyNode = rewardCopy.fetchSemanticsNode()
        val claimNode = rule.onNodeWithTag("product-track-claim-1").fetchSemanticsNode()
        assertTrue(
            "Claim action must follow the copy instead of consuming its horizontal space",
            claimNode.positionInRoot.y >= copyNode.positionInRoot.y + copyNode.size.height - 1f,
        )
        rule.onNodeWithTag("product-track-claim-2").performScrollTo().assertIsNotEnabled()

        rule.runOnIdle { surface.value = roster() }
        listOf("ally-cinder-guard", "hero-solar-pulse").forEach { id ->
            listOf("title", "role").forEach { field ->
                val text = rule.onNodeWithTag("product-roster-$field-$id", useUnmergedTree = true)
                assertTextFits(text)
                text.assertWidthIsAtLeast(200.dp)
            }
            listOf(
                "toggle" to context.getString(R.string.product_roster_remove),
                "upgrade" to context.getString(R.string.product_roster_upgrade, 70),
            ).forEach { (action, label) ->
                val tag = "product-roster-$action-$id"
                assertTextFits(rule.onNode(
                    hasText(label) and hasAnyAncestor(hasTestTag(tag)),
                    useUnmergedTree = true,
                ))
                rule.onNodeWithTag(tag).performScrollTo().assertIsDisplayed()
                    .assertWidthIsAtLeast(200.dp).assertHeightIsAtLeast(48.dp)
            }
            val title = rule.onNodeWithTag("product-roster-title-$id", useUnmergedTree = true)
                .fetchSemanticsNode()
            val toggle = rule.onNodeWithTag("product-roster-toggle-$id").fetchSemanticsNode()
            val upgrade = rule.onNodeWithTag("product-roster-upgrade-$id").fetchSemanticsNode()
            assertEquals("Roster actions must use the full title width", title.size.width, toggle.size.width)
            assertEquals("Roster upgrade must use the full title width", title.size.width, upgrade.size.width)
            assertTrue(
                "Roster actions must stack vertically instead of splitting whole words",
                upgrade.positionInRoot.y >= toggle.positionInRoot.y + toggle.size.height,
            )
        }
        rule.onNodeWithTag("product-roster-toggle-ally-cinder-guard")
            .performScrollTo().assertIsNotEnabled()
        rule.onNodeWithTag("product-roster-toggle-hero-solar-pulse")
            .performScrollTo().performClick()
        rule.onNodeWithTag("product-roster-upgrade-hero-solar-pulse")
            .performScrollTo().performClick()

        rule.runOnIdle {
            surface.value = ProductSurfaceUi.Settings(ProductSettingUi.entries.associateWith { true })
        }
        rule.onNodeWithText(context.getString(R.string.product_settings_body))
            .performScrollTo().assertExists()
        rule.onNodeWithText(context.getString(R.string.settings_body)).assertDoesNotExist()

        rule.runOnIdle {
            surface.value = ProductSurfaceUi.Battle(testBattleScene(
                phase = BattlePhaseUi.VICTORY,
                result = BattleResultUi(true, 10, 35, 90, 10, true, false),
            ))
        }
        listOf(
            "product-retry-battle" to R.string.product_result_retry,
            "product-return-map" to R.string.product_result_map,
        ).forEach { (tag, labelId) ->
            assertTextFits(rule.onNode(
                hasText(context.getString(labelId)) and hasAnyAncestor(hasTestTag(tag)),
                useUnmergedTree = true,
            ))
            rule.onNodeWithTag(tag).performScrollTo().assertIsDisplayed()
                .assertWidthIsAtLeast(200.dp).assertHeightIsAtLeast(48.dp).performClick()
        }
        val retryNode = rule.onNodeWithTag("product-retry-battle").fetchSemanticsNode()
        val returnNode = rule.onNodeWithTag("product-return-map").fetchSemanticsNode()
        assertTrue(
            "Terminal actions must stack vertically instead of splitting long words",
            returnNode.positionInRoot.y >= retryNode.positionInRoot.y + retryNode.size.height,
        )
        rule.onNodeWithTag("product-claim-battle-reward").assertDoesNotExist()
        rule.runOnIdle {
            assertEquals(
                listOf(
                    ProductUiAction.RequestPurchaseStub("stub"),
                    ProductUiAction.UnlockTech("first"),
                    ProductUiAction.ClaimRewardTier("tier-1"),
                    ProductUiAction.ToggleHeroSkill("hero-solar-pulse"),
                    ProductUiAction.UpgradeRosterEntry("hero-solar-pulse"),
                    ProductUiAction.RetryBattle,
                    ProductUiAction.BackToCampaign,
                ),
                actions,
            )
        }
    }

    private fun assertTextFits(
        node: SemanticsNodeInteraction,
        keepWordsWhole: Boolean = true,
        scrollTo: Boolean = true,
        requireSingleLine: Boolean = false,
    ) {
        if (scrollTo) node.performScrollTo()
        node.assertExists()
        val layouts = mutableListOf<TextLayoutResult>()
        node.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertEquals("Exactly one text layout expected", 1, layouts.size)
        val layout = layouts.single()
        val text = layout.layoutInput.text.text
        val actualSize = node.fetchSemanticsNode().size
        // Compose foundation 1.11.3, commonMain/androidx/compose/foundation/text/modifiers/
        // ParagraphLayoutCache.kt: slowCreateTextLayoutResultOrNull recreates a MultiParagraph
        // under prevConstraints.copyMaxDimensions() but retains the original shrink-wrap
        // layoutSize. Its raw didOverflowWidth therefore is not a visual-overflow predicate.
        // Official source: https://dl.google.com/android/maven2/androidx/compose/foundation/
        // foundation-android/1.11.3/foundation-android-1.11.3-sources.jar
        // Reuse the exact text/style/font/density intrinsics at the measured node width. Leave
        // height unconstrained so missing vertical space cannot be hidden by reconstruction.
        val paragraph = MultiParagraph(
            intrinsics = layout.multiParagraph.intrinsics,
            constraints = Constraints(maxWidth = actualSize.width),
            maxLines = layout.layoutInput.maxLines,
            overflow = layout.layoutInput.overflow,
        )
        val diagnostics = buildString {
            append("text=\"").append(text).append("\"")
            append(" actualSize=").append(actualSize)
            append(" size=").append(layout.size)
            append(" paragraphWidth=").append(layout.multiParagraph.width)
            append(" paragraphHeight=").append(layout.multiParagraph.height)
            append(" constraints=").append(layout.layoutInput.constraints)
            append(" fontScale=").append(layout.layoutInput.density.fontScale)
            append(" rawDidOverflowWidth=").append(layout.didOverflowWidth)
            append(" rawDidOverflowHeight=").append(layout.didOverflowHeight)
            append(" rawLines=")
            append((0 until layout.lineCount).joinToString { line ->
                "[$line: offsets=${layout.getLineStart(line)}..${layout.getLineEnd(line, visibleEnd = true)}, " +
                    "bounds=${layout.getLineLeft(line)},${layout.getLineTop(line)}.." +
                    "${layout.getLineRight(line)},${layout.getLineBottom(line)}]"
            })
            append(" measuredParagraph=").append(paragraph.width).append('x').append(paragraph.height)
            append(" measuredLines=")
            append((0 until paragraph.lineCount).joinToString { line ->
                "[$line: offsets=${paragraph.getLineStart(line)}..${paragraph.getLineEnd(line, visibleEnd = true)}, " +
                    "bounds=${paragraph.getLineLeft(line)},${paragraph.getLineTop(line)}.." +
                    "${paragraph.getLineRight(line)},${paragraph.getLineBottom(line)}]"
            })
        }
        assertEquals("Semantics must retain actual node dimensions: $diagnostics", actualSize, layout.size)
        assertTrue("Text must fit actual width: $diagnostics", paragraph.width <= actualSize.width)
        assertTrue("Text must fit actual height: $diagnostics", paragraph.height <= actualSize.height)
        assertFalse("All text must remain visible: $diagnostics", paragraph.didExceedMaxLines)
        assertTrue("Text must have a rendered line: $diagnostics", paragraph.lineCount > 0)
        if (requireSingleLine) {
            assertEquals("Navigation labels must remain on one complete line: $diagnostics", 1, paragraph.lineCount)
        }
        assertEquals(
            "No trailing text may be omitted: $diagnostics",
            text.length,
            paragraph.getLineEnd(paragraph.lineCount - 1),
        )
        for (line in 0 until paragraph.lineCount) {
            assertFalse("No line may be ellipsized: $diagnostics", paragraph.isLineEllipsized(line))
            assertTrue(
                "Line must stay inside actual horizontal bounds: $diagnostics",
                paragraph.getLineLeft(line) >= 0f && paragraph.getLineRight(line) <= actualSize.width &&
                    paragraph.getLineWidth(line) <= actualSize.width,
            )
            assertTrue(
                "Line must stay inside actual vertical bounds: $diagnostics",
                paragraph.getLineTop(line) >= 0f && paragraph.getLineBottom(line) <= actualSize.height,
            )
        }
        if (keepWordsWhole) {
            for (line in 0 until paragraph.lineCount - 1) {
                val end = paragraph.getLineEnd(line, visibleEnd = true)
                assertFalse(
                    "Word split by squeezed layout: $diagnostics",
                    end in 1 until text.length && text[end - 1].isLetterOrDigit() && text[end].isLetterOrDigit(),
                )
            }
        }
    }

    private fun roster() = ProductSurfaceUi.Roster(
        entries = listOf(
            RosterEntryUi(
                "ally-cinder-guard", "Пепельный страж", "Мобильный боец", 1, 100, 70,
                affordable = true, unlocked = true, unlockStage = null, category = RosterCategoryUi.ALLY,
            ),
            RosterEntryUi(
                "hero-solar-pulse", "Солнечный импульс", "Навык командира", 1, 100, 70,
                affordable = true, unlocked = true, unlockStage = null, category = RosterCategoryUi.HERO,
            ),
        ),
        equippedIds = setOf("ally-cinder-guard", "hero-solar-pulse"),
        maxEquipped = 7,
    )

    private fun shop() = ProductSurfaceUi.Shop(listOf(
        ShopProductUi(
            id = "stub",
            title = "Экспедиционный контейнер",
            description = "Локальная заглушка без оплаты и начисления награды.",
            kind = ShopProductKindUi.PURCHASE_STUB,
            cost = 0,
            affordable = false,
            owned = false,
        ),
        ShopProductUi(
            id = "owned",
            title = "Полученный контейнер",
            description = "Повторная покупка недоступна.",
            kind = ShopProductKindUi.PURCHASE_STUB,
            cost = 0,
            affordable = false,
            owned = true,
        ),
    ))

    private fun tech() = ProductSurfaceUi.Tech(listOf(
        TechNodeUi("third", "Несокрушимый отряд", "", TechNodeStatusUi.LOCKED, 160, "Слаженный строй", false, 2, 0),
        TechNodeUi("first", "Полевая выучка", "", TechNodeStatusUi.AVAILABLE, 80, null, true, 0, 0),
        TechNodeUi("second", "Слаженный строй", "", TechNodeStatusUi.AVAILABLE, 120, null, false, 1, 0),
    ))

    private fun rewards() = ProductSurfaceUi.RewardTrack(
        tiers = listOf(
            RewardTierUi("tier-1", 1, 10, "Контейнер маршрута 1", RewardTierStatusUi.AVAILABLE, softReward = 20),
            RewardTierUi("tier-2", 2, 20, "Контейнер маршрута 2", RewardTierStatusUi.LOCKED),
        ),
        experience = 10,
    )
}
