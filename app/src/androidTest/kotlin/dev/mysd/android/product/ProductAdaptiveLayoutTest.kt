package dev.mysd.android.product

import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.MultiParagraph
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.mysd.android.R
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

/** Actual-width copy and safe-window regressions for the observed batch-10 layout defects. */
@RunWith(AndroidJUnit4::class)
class ProductAdaptiveLayoutTest {
    @get:Rule val rule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var hostView: View
    private var viewport = Rect.Zero
    private var pixelsPerDp = 0f
    private var requestedFontScale = 1f

    @Test fun compactLayoutKeepsCompleteCopyAndSafeActions() = verifyLayout(320, 1f)
    @Test fun nativeWidthLayoutKeepsCompleteCopyAndSafeActions() = verifyLayout(411, 1f)
    @Test fun compactLargeFontLayoutKeepsCompleteCopyAndSafeActions() = verifyLayout(320, 2f)
    @Test fun nativeWidthLargeFontLayoutKeepsCompleteCopyAndSafeActions() = verifyLayout(411, 2f)

    private fun verifyLayout(widthDp: Int, fontScale: Float) {
        requestedFontScale = fontScale
        val actions = mutableListOf<ProductUiAction>()
        val expectedActions = mutableListOf<ProductUiAction>()
        val setup = setup()
        val surface = mutableStateOf<ProductSurfaceUi>(setup)
        val fullWindowHeight = mutableStateOf(false)
        rule.setContent {
            val density = LocalDensity.current.density
            val view = LocalView.current
            SideEffect {
                hostView = view
                pixelsPerDp = density
            }
            CompositionLocalProvider(LocalDensity provides Density(density, fontScale)) {
                Box(
                    Modifier.width(widthDp.dp)
                        .then(if (fullWindowHeight.value) Modifier.fillMaxHeight() else Modifier.height(480.dp))
                        .onGloballyPositioned { coordinates ->
                            val origin = coordinates.positionInWindow()
                            viewport = Rect(
                                origin.x, origin.y,
                                origin.x + coordinates.size.width, origin.y + coordinates.size.height,
                            )
                        },
                ) {
                    MySDTheme(dynamicColor = false) {
                        // These are independent layout fixtures, not a lifecycle/dispatch test.
                        // Reset scrolling between fixtures without introducing test-only insets.
                        key(surface.value, fullWindowHeight.value) {
                            val state = testApp(surface.value)
                            MySdProductApp(
                                state.copy(
                                    shell = state.shell.copy(
                                        showMetaNavigation = surface.value is ProductSurfaceUi.CampaignMap,
                                    ),
                                    reduceMotion = true,
                                ),
                                onAction = { actions += it },
                            )
                        }
                    }
                }
            }
        }

        // Check every original choice, not just the previously truncated Solar label. The
        // empty slot exposes both roles; existing selected-role guards remain covered too.
        setup.availableLoadout.forEach { option ->
            val tag = "product-loadout-2-${option.id}"
            assertCompleteText(textIn(tag, option.title), option.title)
            assertAction(tag).assertIsEnabled()
        }
        listOf("product-loadout-0-tower-ember-needle", "product-loadout-1-ally-cinder-guard")
            .forEach { assertAction(it).assertIsNotEnabled() }
        assertAction("product-loadout-2-tower-sunburst-mortar").performClick()
        expectedActions += ProductUiAction.SetLoadoutSlot(2, "tower-sunburst-mortar")
        assertSetupStart()

        val campaign = campaign()
        rule.runOnIdle { surface.value = campaign }
        campaign.stages.forEach { stage ->
            val fields = listOf(
                "title" to stage.title,
                "status" to context.getString(when (stage.status) {
                    StageStatusUi.AVAILABLE -> R.string.product_stage_available
                    StageStatusUi.COMPLETED -> R.string.product_stage_completed
                    StageStatusUi.LOCKED -> R.string.product_stage_locked
                }),
                "stars" to context.getString(R.string.product_stage_stars, stage.stars),
                "waves" to context.getString(R.string.product_stage_waves, stage.waveCount),
                "energy" to context.getString(R.string.product_stage_energy_cost, stage.energyCost),
            )
            fields.forEach { (field, expected) ->
                assertCompleteText(
                    rule.onNodeWithTag("product-stage-$field-${stage.id}", useUnmergedTree = true),
                    expected,
                )
            }
            assertCompleteText(textIn("product-stage-${stage.id}", stage.subtitle), stage.subtitle)
            // Fetch after the last scroll: every rectangle is now in the same coordinate frame.
            val rectangles = fields.map { (field, _) ->
                rawBounds(rule.onNodeWithTag("product-stage-$field-${stage.id}", useUnmergedTree = true))
            }
            rectangles.forEachIndexed { index, rect ->
                rectangles.drop(index + 1).forEach { other ->
                    assertFalse("Stage ${stage.id} labels must not overlap: $rect vs $other", rect.overlaps(other))
                }
            }
            val action = assertAction("product-stage-open-${stage.id}")
            if (stage.status == StageStatusUi.LOCKED) action.assertIsNotEnabled()
            else {
                action.assertIsEnabled().performClick()
                expectedActions += ProductUiAction.SelectStage(stage.id)
            }
        }

        val catalogSlots = catalogBattleSlots()
        val activeScene = testBattleScene().copy(slots = catalogSlots)
        listOf(1, 2).forEach { speed ->
            rule.runOnIdle {
                surface.value = ProductSurfaceUi.Battle(activeScene.copy(speedMultiplier = speed))
            }
            val speedTag = "product-battle-speed"
            val speedLabel = context.getString(R.string.product_battle_speed_value, speed)
            val speedButton = assertAction(speedTag, scroll = false)
            speedButton.assertContentDescriptionEquals(context.getString(R.string.product_battle_speed, speed))
            assertCompleteText(textIn(speedTag, speedLabel), speedLabel, scroll = false, singleLine = true)
            val pauseLabel = context.getString(R.string.product_battle_pause)
            assertCompleteText(textIn("product-battle-pause", pauseLabel), pauseLabel, scroll = false, singleLine = true)
            val pauseButton = assertAction("product-battle-pause", scroll = false)
            assertBattleGeometry(catalogSlots)
            speedButton.performClick()
            pauseButton.performClick()
            expectedActions += ProductUiAction.ChangeSpeed
            expectedActions += ProductUiAction.PauseOrResume
        }

        // 480dp exercises the constrained content. A second, real full-height viewport reaches
        // the navigation bar: a top-anchored 480dp fixture alone cannot detect bottom-bar overlap.
        listOf(false, true).forEach { fullHeight ->
            rule.runOnIdle { fullWindowHeight.value = fullHeight }
            val enhancement = activeScene.copy(
                phase = BattlePhaseUi.INTER_WAVE_CHOICE,
                enhancementChoices = activeScene.enhancementChoices.map {
                    it.copy(description = productDescription(it.id))
                },
            )
            rule.runOnIdle { surface.value = ProductSurfaceUi.Battle(enhancement) }
            assertInitialOverlayTitle("product-enhancement-overlay", R.string.product_enhancement_title)
            enhancement.enhancementChoices.forEach { choice ->
                assertCompleteText(textIn("product-enhancement-${choice.id}", choice.title), choice.title)
                assertButtonCopy("product-enhancement-choose-${choice.id}", R.string.product_enhancement_choose)
            }
            val rerollLabel = context.getString(R.string.product_enhancement_reroll, 1)
            assertCompleteText(textIn("product-enhancement-reroll", rerollLabel), rerollLabel)
            val reroll = assertAction("product-enhancement-reroll")
            assertSafeScrollViewport(reroll)

            rule.runOnIdle { surface.value = ProductSurfaceUi.Battle(activeScene.copy(phase = BattlePhaseUi.PAUSED)) }
            assertInitialOverlayTitle("product-pause-overlay", R.string.product_battle_paused_title)
            assertButtonCopy("product-battle-resume", R.string.active_battle_resume_action)
            val exit = assertButtonCopy("product-battle-exit", R.string.product_battle_exit)
            assertSafeScrollViewport(exit)

            // Cover both terminal titles and both pre/post-claim action states without awarding
            // anything: all callbacks in this class are presentation-only records.
            // Distribute the four combinations across the two viewports instead of repeating
            // every terminal assertion twice in one test's unchanged 120-second time budget.
            val terminalStates = if (fullHeight) listOf(false to false, true to true)
                else listOf(true to false, false to true)
            terminalStates.forEach { (victory, claimed) ->
                val result = BattleResultUi(victory, 10, 35, 90, 10, claimed, victory && !claimed)
                rule.runOnIdle {
                    surface.value = ProductSurfaceUi.Battle(activeScene.copy(
                        phase = if (victory) BattlePhaseUi.VICTORY else BattlePhaseUi.DEFEAT,
                        result = result,
                    ))
                }
                assertInitialOverlayTitle(
                    "product-terminal-overlay",
                    if (victory) R.string.active_battle_terminal_victory_title
                    else R.string.active_battle_terminal_defeat_title,
                )
                if (claimed) {
                    rule.onNodeWithTag("product-claim-battle-reward").assertDoesNotExist()
                    val claimedCopy = context.getString(R.string.product_result_claimed)
                    assertCompleteText(textIn("product-terminal-overlay", claimedCopy), claimedCopy)
                } else {
                    assertButtonCopy("product-claim-battle-reward", R.string.product_result_claim).assertIsEnabled()
                    if (victory) {
                        assertButtonCopy("product-rewarded-multiplier", R.string.product_result_multiplier)
                            .assertIsEnabled()
                    }
                }
                val retry = assertButtonCopy("product-retry-battle", R.string.product_result_retry)
                val map = assertButtonCopy("product-return-map", R.string.product_result_map)
                if (claimed) {
                    retry.assertIsEnabled()
                    map.assertIsEnabled()
                } else {
                    retry.assertIsNotEnabled()
                    map.assertIsNotEnabled()
                }
                val retryRect = rawBounds(retry)
                val mapRect = rawBounds(map)
                assertTrue("Terminal actions must not overlap: $retryRect / $mapRect", mapRect.top >= retryRect.bottom)
                assertSafeScrollViewport(map)
            }
        }
        rule.runOnIdle { surface.value = setup }
        assertSetupStart()
        assertSafeScrollViewport(rule.onNodeWithTag("product-start-battle"))
        rule.runOnIdle { assertEquals("Only the explicitly tested typed callbacks may escape", expectedActions, actions) }
    }

    private fun assertSetupStart() {
        val label = context.getString(R.string.product_setup_start)
        assertCompleteText(textIn("product-start-battle", label), label)
        assertAction("product-start-battle").assertIsEnabled()
    }

    private fun assertInitialOverlayTitle(overlayTag: String, titleId: Int) {
        val title = context.getString(titleId)
        val node = textIn(overlayTag, title)
        // Do not scroll first: the original overlap occurred on the initial topmost heading.
        assertCompleteText(node, title, scroll = false)
        assertSafeScrollViewport(node)
    }

    private fun assertButtonCopy(tag: String, labelId: Int): SemanticsNodeInteraction {
        val label = context.getString(labelId)
        assertCompleteText(textIn(tag, label), label)
        return assertAction(tag)
    }

    private fun assertAction(tag: String, scroll: Boolean = true): SemanticsNodeInteraction {
        val node = rule.onNodeWithTag(tag)
        if (scroll) node.performScrollTo()
        node.assertIsDisplayed().assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp)
        assertFullyVisible(node, tag)
        return node
    }

    private fun textIn(tag: String, text: String) = rule.onNode(
        hasText(text) and hasAnyAncestor(hasTestTag(tag)),
        useUnmergedTree = true,
    )

    private fun assertCompleteText(
        node: SemanticsNodeInteraction,
        expected: String,
        scroll: Boolean = true,
        singleLine: Boolean = false,
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
        // Compose 1.11's semantics result can use copyMaxDimensions() constraints while retaining
        // the shrink-wrapped node size (ParagraphLayoutCache.slowCreateTextLayoutResultOrNull).
        // Measure its exact font/style intrinsics at the ACTUAL width, with unlimited lines and
        // height. Keeping the production maxLines=2 here would reproduce, and conceal, name loss.
        val paragraph = MultiParagraph(
            intrinsics = layout.multiParagraph.intrinsics,
            constraints = Constraints(maxWidth = actual.width),
            maxLines = Int.MAX_VALUE,
            overflow = TextOverflow.Clip,
        )
        val detail = buildString {
            append("text=\"").append(expected).append("\" actual=").append(actual)
            append(" input=").append(layout.layoutInput.constraints)
            append(" productionMaxLines=").append(layout.layoutInput.maxLines)
            append(" paragraph=").append(paragraph.width).append('x').append(paragraph.height)
            append(" lines=").append((0 until paragraph.lineCount).joinToString { line ->
                "${paragraph.getLineStart(line)}..${paragraph.getLineEnd(line, visibleEnd = true)}:" +
                    "${paragraph.getLineLeft(line)},${paragraph.getLineTop(line)}.." +
                    "${paragraph.getLineRight(line)},${paragraph.getLineBottom(line)}"
            })
        }
        assertTrue("Complete text must fit measured width: $detail", paragraph.width <= actual.width)
        assertTrue("Complete text must fit measured height: $detail", paragraph.height <= actual.height)
        assertTrue("Expected rendered lines: $detail", paragraph.lineCount > 0)
        assertFalse("No line limit may omit text: $detail", paragraph.didExceedMaxLines)
        if (singleLine) assertEquals("This compact action must be one line: $detail", 1, paragraph.lineCount)
        assertEquals("No trailing name may disappear: $detail", expected.length, paragraph.getLineEnd(paragraph.lineCount - 1))
        for (line in 0 until paragraph.lineCount) {
            assertFalse("No ellipsis may hide text: $detail", paragraph.isLineEllipsized(line))
            assertTrue(
                "Glyph line must fit the node: $detail",
                paragraph.getLineLeft(line) >= 0f && paragraph.getLineRight(line) <= actual.width &&
                    paragraph.getLineTop(line) >= 0f && paragraph.getLineBottom(line) <= actual.height,
            )
            if (line < paragraph.lineCount - 1) {
                val end = paragraph.getLineEnd(line, visibleEnd = true)
                assertFalse(
                    "A squeezed layout must not split a word: $detail",
                    end in 1 until expected.length && expected[end - 1].isLetterOrDigit() && expected[end].isLetterOrDigit(),
                )
            }
        }
        assertFullyVisible(node, expected)
    }

    private fun assertBattleGeometry(slots: List<BattleSlotUi>) {
        val fieldNode = rule.onNodeWithTag("product-battle-canvas")
        fieldNode.assertHeightIsAtLeast(96.dp)
        val field = rawBounds(fieldNode)
        assertFullyVisible(fieldNode, "battle field")
        val hud = rawBounds(rule.onNodeWithTag("product-battle-hud"))
        val tray = rawBounds(rule.onNodeWithTag("product-battle-actions"))
        assertTrue("HUD must not cover the field: $hud / $field", hud.bottom <= field.top)
        assertTrue("Action tray must not cover the field: $field / $tray", field.bottom <= tray.top)
        slots.forEach { slot ->
            val target = assertAction("product-battle-slot-${slot.id}", scroll = false)
            assertContains(field, rawBounds(target), "The whole 48dp target ${slot.id} must remain inside the field")
        }
    }

    private fun assertFullyVisible(node: SemanticsNodeInteraction, label: String) {
        val semantics = node.fetchSemanticsNode()
        val raw = rawBounds(semantics)
        assertTrue("Nonempty geometry expected for $label: $raw", raw.width > 0f && raw.height > 0f)
        assertContains(semantics.boundsInWindow, raw, "$label must not be clipped by any ancestor")
        assertContains(safeViewport(), raw, "$label must fit the actual viewport outside system bars/cutouts")
    }

    private fun assertSafeScrollViewport(node: SemanticsNodeInteraction) {
        var ancestor: SemanticsNode? = node.fetchSemanticsNode().parent
        while (ancestor != null && !ancestor.config.contains(SemanticsProperties.VerticalScrollAxisRange)) {
            ancestor = ancestor.parent
        }
        val scroller = checkNotNull(ancestor) { "The tested overlay/action must have a vertical scroll ancestor" }
        val visibleScroller = scroller.boundsInWindow
        assertTrue("Scroller must have an actual viewport", visibleScroller.width > 0f && visibleScroller.height > 0f)
        assertContains(safeViewport(), visibleScroller, "The scroll viewport itself must not extend under system bars")
    }

    private fun safeViewport(): Rect = rule.runOnIdle {
        val root = hostView.rootView
        val insets = checkNotNull(ViewCompat.getRootWindowInsets(root)) { "Actual window insets are required, not zero-inset fallbacks" }
        val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
        assertTrue("Status-bar geometry is a required fixture precondition", bars.top > 0)
        assertTrue("Physical density must be available", pixelsPerDp > 0f)
        val origin = IntArray(2)
        root.getLocationInWindow(origin)
        val windowSafe = Rect(
            origin[0].toFloat() + bars.left,
            origin[1].toFloat() + bars.top,
            origin[0].toFloat() + root.width - bars.right,
            origin[1].toFloat() + root.height - bars.bottom,
        )
        val safe = Rect(
            maxOf(viewport.left, windowSafe.left), maxOf(viewport.top, windowSafe.top),
            minOf(viewport.right, windowSafe.right), minOf(viewport.bottom, windowSafe.bottom),
        )
        assertTrue("Measured fixture and system-safe window must intersect: $viewport / $windowSafe", safe.width > 0f && safe.height > 0f)
        safe
    }

    private fun rawBounds(node: SemanticsNodeInteraction) = rawBounds(node.fetchSemanticsNode())

    private fun rawBounds(node: SemanticsNode): Rect {
        val origin = node.positionInWindow
        return Rect(origin.x, origin.y, origin.x + node.size.width, origin.y + node.size.height)
    }

    private fun assertContains(outer: Rect, inner: Rect, label: String) {
        assertTrue(
            "$label: outer=$outer inner=$inner",
            inner.left >= outer.left && inner.top >= outer.top && inner.right <= outer.right && inner.bottom <= outer.bottom,
        )
    }

    private fun catalogBattleSlots(): List<BattleSlotUi> {
        val session = MySdAppFactory.create(seed = 83L)
        assertTrue(session.submit(MySdAppIntent.Navigate(MySdRoute.CAMPAIGN)).accepted)
        assertTrue(session.submit(MySdAppIntent.SelectStage("stage-ember-path")).accepted)
        assertTrue(session.submit(MySdAppIntent.StartSelectedStage).accepted)
        val slots = (session.snapshot().toProductUiModel().surface as ProductSurfaceUi.Battle).scene.slots
        assertEquals("The production catalog must supply all four slot targets", 4, slots.size)
        assertEquals(4, slots.map { it.id }.toSet().size)
        // Their actual mapper y positions are checked geometrically above. 96dp is this compact
        // fixture's minimum, not a claim that arbitrary future path positions fit that height.
        return slots
    }

    private fun setup(): ProductSurfaceUi.Setup {
        val choices = listOf(
            "tower-ember-needle" to "Угольная игла",
            "tower-sunburst-mortar" to "Солнечная мортира",
            "tower-glass-snare" to "Стеклянная сеть",
            "tower-seed-forge" to "Кузница семян",
            "ally-cinder-guard" to "Пепельный страж",
            "ally-bright-skirmisher" to "Светлый разведчик",
            "ally-arc-striker" to "Дуговой стрелок",
        ).map { (id, title) ->
            SetupChoiceUi(id, title, productDescription(id), if (id.startsWith("tower-")) LoadoutKindUi.TOWER else LoadoutKindUi.ALLY)
        }
        return ProductSurfaceUi.Setup(
            "stage-ember-path", "Тропа углей", "Угольный сектор", 10, 2,
            energyAvailable = true, sweepAvailable = false, heroName = "Командир Маяка",
            loadout = listOf(
                LoadoutSlotUi(0, "tower-ember-needle", "Угольная игла", LoadoutKindUi.TOWER),
                LoadoutSlotUi(1, "ally-cinder-guard", "Пепельный страж", LoadoutKindUi.ALLY),
                LoadoutSlotUi(2, null, null, null),
            ),
            availableLoadout = choices, canStart = true,
        )
    }

    private fun campaign() = ProductSurfaceUi.CampaignMap(listOf(
        CampaignStageUi("stage-ember-path", "Тропа углей", "Угольный сектор", StageStatusUi.AVAILABLE, 0, 2, 10),
        CampaignStageUi("stage-glass-garden", "Стеклянный сад", "Орбитальный край", StageStatusUi.COMPLETED, 3, 2, 10),
        CampaignStageUi("stage-dawn-engine", "Двигатель рассвета", "Орбитальный край", StageStatusUi.LOCKED, 0, 2, 10),
    ))
}
