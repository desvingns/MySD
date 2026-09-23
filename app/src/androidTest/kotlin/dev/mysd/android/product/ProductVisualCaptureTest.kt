package dev.mysd.android.product

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import dev.mysd.android.R
import dev.mysd.android.ui.theme.MySDTheme
import dev.mysd.game.product.MySdAppFactory
import dev.mysd.game.product.MySdAppIntent
import dev.mysd.game.product.MySdRoute
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Reproducible UI-only capture tour. These fixtures prove rendering, not gameplay reachability or
 * reference parity. PNGs stay in the device's app-private files directory until final local review.
 */
@RunWith(AndroidJUnit4::class)
class ProductVisualCaptureTest {
    @get:Rule
    val rule = createComposeRule()

    @Test fun nativeViewportTour() = captureTour("native", compact = false, scale = 1f)
    @Test fun compactViewportTour() = captureTour("compact", compact = true, scale = 1f)
    @Test fun compactLargeFontTour() = captureTour("compact-font-2", compact = true, scale = 2f)

    private fun captureTour(profile: String, compact: Boolean, scale: Float) {
        val fixtures = fixtures()
        val current = mutableStateOf(fixtures.first())
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val device = UiDevice.getInstance(instrumentation)
        val directory = File(context.filesDir, "product-visual/$profile")
        assertTrue(directory.isDirectory || directory.mkdirs())
        rule.setContent {
            val physicalDensity = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(physicalDensity, scale)) {
                key(current.value.first) {
                    Box(if (compact) Modifier.size(320.dp, 480.dp) else Modifier.fillMaxSize()) {
                        MySDTheme(dynamicColor = false) {
                            MySdProductApp(current.value.second, onAction = {})
                        }
                    }
                }
            }
        }
        val viewport = if (compact) "compact-320x480dp" else "native-activity"
        val manifest = StringBuilder(
            "fixture,root,width_px,height_px,font_scale,source,capture_kind,filename,fixture_viewport,window_scope,focus\n",
        )
        fun record(name: String, root: String, file: File, width: Int, height: Int, kind: String, focus: String) {
            manifest.append("$name,$root,$width,$height,$scale,ui-fixture,$kind,${file.name},$viewport,")
            manifest.append(if (kind == "ui-device") "physical-display-dialog-not-compact" else "compose-root-window-not-certified")
            manifest.append(",$focus\n")
            // Preserve each completed capture's metadata even if a later assertion fails.
            File(directory, "manifest.csv").writeText(manifest.toString())
        }
        fun captureDisplay(name: String, focus: String) {
            rule.waitForIdle()
            val file = File(directory, "$name-device-$focus.png")
            assertTrue("Full-display screenshot failed: ${file.name}", device.takeScreenshot(file))
            assertTrue(file.isFile && file.length() > 0L)
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, bounds)
            assertTrue("Invalid screenshot dimensions: ${file.name}", bounds.outWidth > 0 && bounds.outHeight > 0)
            record(name, "device", file, bounds.outWidth, bounds.outHeight, "ui-device", focus)
        }
        fixtures.forEach { fixture ->
            rule.runOnIdle { current.value = fixture }
            rule.waitForIdle()
            when (fixture.second.overlay) {
                is ProductOverlayUi.ResumeRun -> {
                    rule.onNodeWithTag("resume-overlay").assertIsDisplayed()
                    rule.onNodeWithText(context.getString(R.string.campaign_unfinished_title)).assertIsDisplayed()
                }
                is ProductOverlayUi.ServiceResult -> {
                    rule.onNodeWithTag("product-service-dialog").assertIsDisplayed()
                    rule.onNode(
                        hasTestTag("product-service-title") and hasText(context.getString(R.string.product_service_title)),
                    ).assertIsDisplayed()
                }
                else -> Unit
            }
            val roots = rule.onAllNodes(isRoot())
            val count = roots.fetchSemanticsNodes().size
            assertTrue("No rendered root for ${fixture.first}", count > 0)
            repeat(count) { index ->
                val bitmap = roots[index].captureToImage().asAndroidBitmap()
                assertTrue(bitmap.width > 0 && bitmap.height > 0)
                val name = "${fixture.first}-root-$index.png"
                val file = File(directory, name)
                file.outputStream().use { stream ->
                    assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream))
                }
                record(fixture.first, index.toString(), file, bitmap.width, bitmap.height, "compose-pixelcopy", "initial")
            }
            // Compose root PixelCopy can capture the Activity behind a Dialog. Retain it for
            // comparison, but use a real full-display screenshot as the window-level evidence.
            // Compact applies only to the Activity Box: platform Dialogs use the native window.
            captureDisplay(fixture.first, "initial")
            focusTargets(fixture.second).forEach { target ->
                val node = if (target.tag != null) {
                    rule.onNodeWithTag(target.tag)
                } else {
                    rule.onNodeWithText(context.getString(requireNotNull(target.textResource)))
                }
                node.performScrollTo().assertIsDisplayed()
                captureDisplay(fixture.first, target.name)
                rule.runOnIdle { assertEquals("Scrolling must not alter the fixture", fixture, current.value) }
            }
        }
        File(directory, "manifest.csv").writeText(manifest.toString())
    }

    private data class FocusTarget(val name: String, val tag: String? = null, val textResource: Int? = null)

    /** Scroll only; never click, emit a game action, change settings or advance a fixture. */
    private fun focusTargets(state: ProductUiModel): List<FocusTarget> {
        when (state.overlay) {
            is ProductOverlayUi.ResumeRun -> return listOf(FocusTarget("resume-actions", "resume-continue-action"))
            is ProductOverlayUi.ServiceResult -> return listOf(FocusTarget("service-confirm", "product-service-confirm"))
            else -> Unit
        }
        return when (val surface = state.surface) {
            is ProductSurfaceUi.Launch -> listOf(FocusTarget("enter", "product-enter-campaign"))
            is ProductSurfaceUi.CampaignMap -> listOf(
                FocusTarget("stage-action", "product-stage-open-${surface.stages.first().id}"),
            )
            is ProductSurfaceUi.Setup -> surface.heroChoices.map {
                FocusTarget("hero-${it.id}", "product-setup-hero-${it.id}")
            } + FocusTarget("start", "product-start-battle")
            is ProductSurfaceUi.Roster -> listOf(
                FocusTarget("upgrade", "product-roster-upgrade-${surface.entries.first { it.unlocked }.id}"),
            )
            is ProductSurfaceUi.Tech -> listOf(
                FocusTarget("unlock", "product-tech-unlock-${surface.nodes.first { it.status == TechNodeStatusUi.AVAILABLE }.id}"),
            )
            is ProductSurfaceUi.Shop -> surface.products.distinctBy { it.kind }.map {
                FocusTarget("shop-${it.kind.name.lowercase()}", "product-shop-action-${it.id}")
            }
            is ProductSurfaceUi.RewardTrack -> listOf(
                FocusTarget("tier-action", "product-track-claim-${surface.tiers.first().tier}"),
            )
            is ProductSurfaceUi.Settings -> ProductSettingUi.entries.map {
                FocusTarget("setting-${it.name.lowercase()}", "product-setting-${it.name.lowercase()}")
            } + FocusTarget("settings-close", "product-settings-close")
            is ProductSurfaceUi.Arena -> when (val arena = surface.state) {
                is ArenaUi.Lobby -> listOf(FocusTarget("find", "product-arena-find"))
                is ArenaUi.Result -> listOf(
                    FocusTarget("result", textResource = if (arena.victory) R.string.product_arena_victory else R.string.product_arena_defeat),
                    FocusTarget("result-actions", "product-arena-return"),
                )
                is ArenaUi.OpponentPreview -> listOf(FocusTarget("start", "product-arena-start"))
                is ArenaUi.Battle -> emptyList()
            }
            is ProductSurfaceUi.Battle -> when (surface.scene.phase) {
                BattlePhaseUi.PAUSED -> listOf(
                    FocusTarget("resume", "product-battle-resume"), FocusTarget("exit", "product-battle-exit"),
                )
                BattlePhaseUi.INTER_WAVE_CHOICE -> surface.scene.enhancementChoices.map {
                    FocusTarget("choose-${it.id}", "product-enhancement-choose-${it.id}")
                } + FocusTarget("reroll", "product-enhancement-reroll")
                BattlePhaseUi.VICTORY, BattlePhaseUi.DEFEAT -> buildList {
                    if (requireNotNull(surface.scene.result).claimed) {
                        add(FocusTarget("claimed-status", textResource = R.string.product_result_claimed))
                    } else {
                        add(FocusTarget("claim", "product-claim-battle-reward"))
                    }
                    add(FocusTarget("terminal-actions", "product-return-map"))
                }
                else -> surface.scene.abilities.take(1).map {
                    FocusTarget("hero-action", "product-ability-${it.id}")
                }
            }
        }
    }

    private fun fixtures(): List<Pair<String, ProductUiModel>> = buildList {
        val session = MySdAppFactory.create(903_025L)
        add("launch" to session.snapshot().toProductUiModel())
        listOf(
            "campaign" to MySdRoute.CAMPAIGN,
            "roster" to MySdRoute.ROSTER,
            "technology" to MySdRoute.TECHNOLOGY,
            "shop" to MySdRoute.SHOP,
            "reward-track" to MySdRoute.REWARD_TRACK,
            "settings" to MySdRoute.SETTINGS,
            "arena-lobby" to MySdRoute.ARENA,
        ).forEach { (name, route) ->
            assertTrue(session.submit(MySdAppIntent.Navigate(route)).accepted)
            add(name to session.snapshot().toProductUiModel())
        }
        assertTrue(session.submit(MySdAppIntent.StartArenaExhibition).accepted)
        add("arena-result" to session.snapshot().toProductUiModel())
        assertTrue(session.submit(MySdAppIntent.Navigate(MySdRoute.CAMPAIGN)).accepted)
        assertTrue(session.submit(MySdAppIntent.SelectStage("stage-ember-path")).accepted)
        add("stage-setup" to session.snapshot().toProductUiModel())

        val battle = testBattleScene().copy(
            enhancementChoices = listOf(
                "enhancement-keen-sparks", "enhancement-quick-coils", "enhancement-long-sight",
            ).mapIndexed { index, id -> EnhancementChoiceUi(id, productTitle(id), productDescription(id), index + 1) },
        )
        add("battle-active" to testApp(ProductSurfaceUi.Battle(battle)))
        add("battle-paused" to testApp(ProductSurfaceUi.Battle(battle.copy(phase = BattlePhaseUi.PAUSED))))
        add("battle-enhancement" to testApp(ProductSurfaceUi.Battle(battle.copy(phase = BattlePhaseUi.INTER_WAVE_CHOICE))))
        listOf(true, false).forEach { victory ->
            listOf(false, true).forEach { claimed ->
                val name = (if (victory) "victory" else "defeat") + (if (claimed) "-claimed" else "-unclaimed")
                val scene = battle.copy(
                    phase = if (victory) BattlePhaseUi.VICTORY else BattlePhaseUi.DEFEAT,
                    baseHealth = if (victory) battle.baseHealth else 0,
                    result = BattleResultUi(victory, if (victory) 10 else 4, 35, 90, 10, claimed, false),
                )
                add(name to testApp(ProductSurfaceUi.Battle(scene)))
            }
        }
        add("resume" to testApp(ProductSurfaceUi.Launch(), ProductOverlayUi.ResumeRun("Тропа углей")))
        listOf(ServiceResultKindUi.REWARDED, ServiceResultKindUi.PURCHASE).forEach { kind ->
            add("stub-${kind.name.lowercase()}" to testApp(
                ProductSurfaceUi.Shop(emptyList()),
                ProductOverlayUi.ServiceResult(kind, "Локальная заглушка"),
            ))
        }
    }
}
