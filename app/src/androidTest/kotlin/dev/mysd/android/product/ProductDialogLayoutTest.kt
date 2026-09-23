package dev.mysd.android.product

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.MultiParagraph
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.mysd.android.R
import dev.mysd.android.ui.theme.AppTypography
import dev.mysd.android.ui.theme.MySDTheme
import dev.mysd.game.product.MySdAppFactory
import dev.mysd.game.product.MySdAppIntent
import dev.mysd.game.product.MySdRoute
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * A Compose Dialog window provides its own LocalDensity, so Dialog content follows the system font
 * scale and never the Activity-local override used by the compact/font-2 tours. Real system font 2
 * on a 320x480dp display split "Незавершённый" in the Resume dialog title. The extracted window
 * content is verified here at the dialog window size and requested font scale.
 */
@RunWith(AndroidJUnit4::class)
class ProductDialogLayoutTest {
    @get:Rule val rule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private var requestedFontScale = 1f
    private var windowWidth = 0.dp

    @Test fun compactDialogsKeepWholeWordsAndActions() = verifyDialogs(320, 1f)
    @Test fun nativeWidthDialogsKeepWholeWordsAndActions() = verifyDialogs(411, 1f)
    @Test fun compactLargeFontDialogsKeepWholeWordsAndActions() = verifyDialogs(320, 2f)
    @Test fun nativeWidthLargeFontDialogsKeepWholeWordsAndActions() = verifyDialogs(411, 2f)

    private sealed interface DialogFixture {
        data class Resume(val stageTitle: String) : DialogFixture
        data object Exit : DialogFixture
        data class Service(val state: ProductOverlayUi.ServiceResult) : DialogFixture
        data class Recovery(val notice: ProductRecoveryNotice) : DialogFixture
        data class Slot(val slot: BattleSlotUi) : DialogFixture
    }

    private fun verifyDialogs(widthDp: Int, fontScale: Float) {
        requestedFontScale = fontScale
        val scene = catalogBattleScene()
        val fixture = mutableStateOf<DialogFixture>(DialogFixture.Exit)
        val actions = mutableListOf<ProductUiAction>()
        // Full-width dialogs fill their window. The platform-width Recovery dialog is checked at a
        // conservative 280dp on compact (narrower than the platform's ~95% minimum width) and at
        // 95% of the native width.
        fun widthOf(dialog: DialogFixture): Dp = if (dialog is DialogFixture.Recovery) {
            if (widthDp < 360) 280.dp else (widthDp * 0.95f).dp
        } else {
            widthDp.dp
        }
        fun show(dialog: DialogFixture) = rule.runOnIdle {
            windowWidth = widthOf(dialog)
            fixture.value = dialog
        }
        windowWidth = widthOf(fixture.value)
        rule.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, fontScale)) {
                val current = fixture.value
                Box(Modifier.width(widthOf(current)).height(WindowHeight)) {
                    MySDTheme(dynamicColor = false) {
                        key(current) {
                            when (current) {
                                is DialogFixture.Resume -> ProductResumeOverlay(
                                    ProductOverlayUi.ResumeRun(current.stageTitle),
                                ) { actions += it }
                                DialogFixture.Exit -> ProductExitConfirmationOverlay { actions += it }
                                is DialogFixture.Service -> ProductServiceOverlay(current.state) { actions += it }
                                is DialogFixture.Recovery -> ProductRecoveryPanel(current.notice) {}
                                is DialogFixture.Slot -> ProductBattleSlotOverlay(
                                    slot = current.slot,
                                    towerIds = scene.availableTowerIds,
                                    towerBuildCosts = scene.towerBuildCosts,
                                    resource = 999,
                                    onAction = { actions += it },
                                    onDismiss = {},
                                )
                            }
                        }
                    }
                }
            }
        }
        val typography = AppTypography

        STAGE_IDS.map(::productTitle).forEach { stageTitle ->
            show(DialogFixture.Resume(stageTitle))
            assertDialogText("resume-panel", R.string.campaign_unfinished_title, typography.headlineSmall.fontSize, scroll = false)
            assertDialogText("resume-panel", R.string.campaign_unfinished_body, typography.bodyLarge.fontSize)
            assertCompleteText(textIn("resume-panel", stageTitle), stageTitle, typography.bodyLarge.fontSize)
            assertButtonCopy("resume-cancel-action", R.string.campaign_cancel_action)
            assertButtonCopy("resume-continue-action", R.string.campaign_continue_action)
        }

        show(DialogFixture.Exit)
        assertDialogText("product-exit-confirmation", R.string.product_exit_confirm_title, typography.headlineSmall.fontSize, scroll = false)
        assertDialogText("product-exit-confirmation", R.string.product_exit_confirm_body, typography.bodyLarge.fontSize)
        assertButtonCopy("product-exit-cancel", R.string.product_exit_cancel)
        assertButtonCopy("product-exit-confirm", R.string.product_exit_confirm)

        listOf(
            ServiceResultKindUi.REWARDED to "rewarded:shop-rewarded-cache",
            ServiceResultKindUi.PURCHASE to "purchase:shop-purchase-cache",
            ServiceResultKindUi.ARENA to "arena-route",
        ).forEach { (kind, code) ->
            val state = ProductOverlayUi.ServiceResult(kind, productTitle(code))
            show(DialogFixture.Service(state))
            assertDialogText("product-service-dialog", R.string.product_local_only, typography.labelLarge.fontSize, scroll = false)
            val serviceTitle = context.getString(R.string.product_service_title)
            assertCompleteText(
                rule.onNodeWithTag("product-service-title", useUnmergedTree = true),
                serviceTitle,
                typography.headlineSmall.fontSize,
            )
            assertCompleteText(textIn("product-service-dialog", state.title), state.title, typography.titleMedium.fontSize)
            assertDialogText(
                "product-service-dialog",
                when (kind) {
                    ServiceResultKindUi.REWARDED -> R.string.product_service_rewarded_body
                    ServiceResultKindUi.PURCHASE -> R.string.product_service_purchase_body
                    ServiceResultKindUi.ARENA -> R.string.product_service_arena_body
                },
                typography.bodyLarge.fontSize,
            )
            assertButtonCopy("product-service-confirm", R.string.product_service_confirm)
        }

        // Two notices cover all four body strings.
        listOf(ProductRecoveryNotice(true, true), ProductRecoveryNotice(false, false)).forEach { notice ->
            show(DialogFixture.Recovery(notice))
            assertDialogText("product-recovery-notice", R.string.product_recovery_title, typography.titleLarge.fontSize, scroll = false)
            assertDialogText(
                "product-recovery-notice",
                if (notice.profileRetained) R.string.product_recovery_run_body else R.string.product_recovery_profile_body,
                typography.bodyLarge.fontSize,
            )
            assertDialogText(
                "product-recovery-notice",
                if (notice.copyArchived) R.string.product_recovery_archived else R.string.product_recovery_archive_failed,
                typography.bodyLarge.fontSize,
            )
            assertButtonCopy("product-recovery-confirm", R.string.product_service_confirm)
        }

        val emptySlot = scene.slots.first { it.towerName == null }
        show(DialogFixture.Slot(emptySlot))
        assertDialogText("product-build-dialog", R.string.active_battle_build_popup_title, typography.headlineSmall.fontSize, scroll = false)
        scene.availableTowerIds.take(4).forEach { towerId ->
            val tag = "product-build-${emptySlot.id}-$towerId"
            val label = "${context.getString(R.string.product_battle_build)} · ${productTitle(towerId)} · " +
                "${scene.towerBuildCosts[towerId] ?: 0}"
            assertCompleteText(textIn(tag, label), label, textSize = null)
            assertAction(tag)
        }
        val builtSlot = emptySlot.copy(towerName = productTitle("tower-ember-needle"), level = 2, canAfford = true)
        show(DialogFixture.Slot(builtSlot))
        assertDialogText("product-build-dialog", R.string.active_battle_upgrade_popup_title, typography.headlineSmall.fontSize, scroll = false)
        val level = context.getString(R.string.product_roster_level, builtSlot.level)
        assertCompleteText(textIn("product-build-dialog", level), level, typography.bodyLarge.fontSize)
        assertButtonCopy("product-upgrade-${builtSlot.id}", R.string.product_battle_upgrade)
        rule.runOnIdle { assertEquals("Layout assertions must not dispatch actions", emptyList<ProductUiAction>(), actions) }
    }

    private fun assertDialogText(tag: String, textId: Int, preferredSize: TextUnit, scroll: Boolean = true) {
        val text = context.getString(textId)
        assertCompleteText(textIn(tag, text), text, preferredSize, scroll)
    }

    private fun assertButtonCopy(tag: String, labelId: Int) {
        val label = context.getString(labelId)
        assertCompleteText(textIn(tag, label), label, textSize = null)
        assertAction(tag)
    }

    private fun assertAction(tag: String) {
        val node = rule.onNodeWithTag(tag)
        node.performScrollTo()
        node.assertIsDisplayed().assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp)
        val bounds = node.getUnclippedBoundsInRoot()
        assertTrue(
            "$tag must be fully inside the ${windowWidth}x480dp dialog window after scrolling: $bounds",
            bounds.left >= 0.dp && bounds.top >= 0.dp && bounds.right <= windowWidth && bounds.bottom <= WindowHeight,
        )
    }

    private fun textIn(tag: String, text: String) = rule.onNode(
        hasText(text) and hasAnyAncestor(hasTestTag(tag)),
        useUnmergedTree = true,
    )

    /**
     * Complete, unellipsized text whose words are never split. At font scale 1 the preferred
     * [textSize] must be retained: whole-word fallback may only shrink text that does not fit.
     */
    private fun assertCompleteText(
        node: SemanticsNodeInteraction,
        expected: String,
        textSize: TextUnit?,
        scroll: Boolean = true,
    ) {
        if (scroll) node.performScrollTo()
        node.assertIsDisplayed().assertTextEquals(expected)
        val results = mutableListOf<TextLayoutResult>()
        node.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(results) }
        assertEquals("Exactly one text layout for $expected", 1, results.size)
        val layout = results.single()
        val actual = node.fetchSemanticsNode().size
        assertEquals("The semantics text must be complete", expected, layout.layoutInput.text.text)
        assertEquals("The requested font scale must reach the text", requestedFontScale, layout.layoutInput.density.fontScale, 0f)
        assertEquals("Layout result must retain measured node dimensions", actual, layout.size)
        if (textSize != null && requestedFontScale == 1f) {
            assertEquals("Fitting text must keep its preferred style: $expected", textSize, layout.layoutInput.style.fontSize)
        }
        // Same reconstruction as ProductAdaptiveLayoutTest: Compose 1.11 semantics layouts may use
        // copyMaxDimensions() constraints, so re-measure the exact intrinsics at the actual width.
        val paragraph = MultiParagraph(
            intrinsics = layout.multiParagraph.intrinsics,
            constraints = Constraints(maxWidth = actual.width),
            maxLines = Int.MAX_VALUE,
            overflow = TextOverflow.Clip,
        )
        val detail = buildString {
            append("text=\"").append(expected).append("\" actual=").append(actual)
            append(" fontSize=").append(layout.layoutInput.style.fontSize)
            append(" paragraph=").append(paragraph.width).append('x').append(paragraph.height)
            append(" lines=").append((0 until paragraph.lineCount).joinToString { line ->
                "${paragraph.getLineStart(line)}..${paragraph.getLineEnd(line, visibleEnd = true)}"
            })
        }
        assertTrue("Complete text must fit measured width: $detail", paragraph.width <= actual.width)
        assertTrue("Complete text must fit measured height: $detail", paragraph.height <= actual.height)
        assertTrue("Expected rendered lines: $detail", paragraph.lineCount > 0)
        assertEquals("No trailing text may disappear: $detail", expected.length, paragraph.getLineEnd(paragraph.lineCount - 1))
        for (line in 0 until paragraph.lineCount) {
            assertFalse("No ellipsis may hide text: $detail", paragraph.isLineEllipsized(line))
            assertTrue(
                "Glyph line must fit the node: $detail",
                paragraph.getLineLeft(line) >= 0f && paragraph.getLineRight(line) <= actual.width,
            )
            if (line < paragraph.lineCount - 1) {
                val end = paragraph.getLineEnd(line, visibleEnd = true)
                assertFalse(
                    "A dialog must not split a word: $detail",
                    end in 1 until expected.length && expected[end - 1].isLetterOrDigit() && expected[end].isLetterOrDigit(),
                )
            }
        }
    }

    private fun catalogBattleScene(): BattleSceneUi {
        val session = MySdAppFactory.create(seed = 83L)
        assertTrue(session.submit(MySdAppIntent.Navigate(MySdRoute.CAMPAIGN)).accepted)
        assertTrue(session.submit(MySdAppIntent.SelectStage("stage-ember-path")).accepted)
        assertTrue(session.submit(MySdAppIntent.StartSelectedStage).accepted)
        val scene = (session.snapshot().toProductUiModel().surface as ProductSurfaceUi.Battle).scene
        assertTrue("The production catalog must supply build choices", scene.availableTowerIds.isNotEmpty())
        return scene
    }

    private companion object {
        val WindowHeight: Dp = 480.dp
        val STAGE_IDS = listOf(
            "stage-ember-path", "stage-glass-garden", "stage-brass-ravine",
            "stage-pulse-vault", "stage-night-orchard", "stage-dawn-engine",
        )
    }
}
