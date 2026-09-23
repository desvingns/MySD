package dev.mysd.android.product

import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.mysd.game.product.MySdAppFactory
import dev.mysd.game.product.MySdAppIntent
import dev.mysd.game.product.MySdRoute
import dev.mysd.game.product.MySdSettingId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProductBattleMotionTest {
    @get:Rule
    val compose = createComposeRule()

    private lateinit var motion: ProductBattleMotion

    @Test
    fun presentationFramesCannotAdvanceSessionAndSettingsMappingPreservesRunIdentity() {
        val session = MySdAppFactory.create()
        session.submit(MySdAppIntent.Navigate(MySdRoute.CAMPAIGN))
        session.submit(MySdAppIntent.SelectStage("stage-ember-path"))
        session.submit(MySdAppIntent.StartSelectedStage)
        val snapshot = session.snapshot()
        val bundle = session.saveBundle()
        val model = snapshot.toProductUiModel()
        val scene = (model.surface as ProductSurfaceUi.Battle).scene
        assertEquals(requireNotNull(snapshot.battle).runId, scene.runId)
        assertEquals(snapshot.profile.settings[MySdSettingId.REDUCE_MOTION] == true, model.reduceMotion)
        val reduced = snapshot.copy(profile = snapshot.profile.copy(
            settings = snapshot.profile.settings + (MySdSettingId.REDUCE_MOTION to true),
        )).toProductUiModel()
        assertTrue(reduced.reduceMotion)

        compose.mainClock.autoAdvance = false
        compose.setContent { rememberProductBattleMotion(scene, reduceMotion = false, resumed = true) }
        compose.mainClock.advanceTimeBy(1_000)
        compose.runOnIdle {
            assertEquals(snapshot, session.snapshot())
            assertEquals(bundle, session.saveBundle())
        }
    }

    @Test
    fun framesInterpolateBothSidesWithoutMutatingLogicalSnapshot() {
        val original = sceneAt(0.2f)
        val logical = mutableStateOf(original)
        compose.mainClock.autoAdvance = false
        compose.setContent {
            val positions = rememberProductBattleMotion(logical.value, reduceMotion = false, resumed = true)
            SideEffect { motion = positions }
        }
        compose.waitForIdle()
        assertAt(0.2f)

        val target = sceneAt(0.8f)
        compose.runOnIdle { logical.value = target }
        compose.mainClock.advanceTimeByFrame()
        compose.mainClock.advanceTimeBy(32)
        compose.runOnIdle {
            val enemy = motion.enemies.getValue("enemy-1").value
            val ally = motion.allies.getValue("ally-1").value
            assertTrue("Enemy must have an intermediate visual frame: $enemy", enemy.x > 0.2f && enemy.x < 0.8f)
            assertEquals(enemy, ally)
            assertSame(target, logical.value)
            assertEquals(0.8f, logical.value.enemies.single().xFraction, 0f)
            assertEquals(0.2f, original.enemies.single().xFraction, 0f)
        }
        compose.mainClock.advanceTimeBy(100)
        assertAt(0.8f)
        compose.runOnIdle { assertSame(target, logical.value) }
    }

    @Test
    fun disabledMotionAndEverySuspendedPhaseSnapAndStayFrozen() {
        val logical = mutableStateOf(sceneAt(0.2f))
        val reduceMotion = mutableStateOf(false)
        val resumed = mutableStateOf(true)
        compose.mainClock.autoAdvance = false
        compose.setContent {
            val positions = rememberProductBattleMotion(logical.value, reduceMotion.value, resumed.value)
            SideEffect { motion = positions }
        }
        compose.waitForIdle()

        val suspended = listOf(
            BattlePhaseUi.PAUSED, BattlePhaseUi.INTER_WAVE_CHOICE,
            BattlePhaseUi.VICTORY, BattlePhaseUi.DEFEAT,
        )
        suspended.forEach { phase ->
            compose.runOnIdle { logical.value = sceneAt(0.8f).copy(phase = phase) }
            compose.mainClock.advanceTimeByFrame()
            assertAt(0.8f)
            compose.mainClock.advanceTimeBy(200)
            assertAt(0.8f)
            compose.runOnIdle { logical.value = sceneAt(0.2f) }
            compose.mainClock.advanceTimeByFrame()
            assertAt(0.2f) // Resuming creates a fresh visual baseline, not a stale catch-up.
        }

        compose.runOnIdle {
            reduceMotion.value = true
            logical.value = sceneAt(0.6f)
        }
        compose.mainClock.advanceTimeByFrame()
        assertAt(0.6f)
        compose.mainClock.advanceTimeBy(200)
        assertAt(0.6f)

        compose.runOnIdle {
            reduceMotion.value = false
            resumed.value = false
            logical.value = sceneAt(0.9f)
        }
        compose.mainClock.advanceTimeByFrame()
        assertAt(0.9f)
        compose.mainClock.advanceTimeBy(200)
        assertAt(0.9f)
    }

    @Test
    fun newRunsAndSpawnsSnapWhileRemovedEntitiesDisappearImmediately() {
        val logical = mutableStateOf(sceneAt(0.2f).copy(runId = "run-one"))
        compose.mainClock.autoAdvance = false
        compose.setContent {
            val positions = rememberProductBattleMotion(logical.value, reduceMotion = false, resumed = true)
            SideEffect { motion = positions }
        }
        compose.waitForIdle()

        compose.runOnIdle { logical.value = sceneAt(0.8f).copy(runId = "run-two", speedMultiplier = 2) }
        compose.mainClock.advanceTimeByFrame()
        assertAt(0.8f)

        compose.runOnIdle {
            logical.value = logical.value.copy(
                enemies = listOf(logical.value.enemies.single().copy(id = "new-enemy", xFraction = 0.4f)),
                allies = emptyList(),
            )
        }
        compose.mainClock.advanceTimeByFrame()
        compose.runOnIdle {
            assertFalse(motion.enemies.containsKey("enemy-1"))
            assertTrue(motion.allies.isEmpty())
            assertEquals(0.4f, motion.enemies.getValue("new-enemy").value.x, 0f)
        }
    }

    private fun assertAt(x: Float) = compose.runOnIdle {
        assertEquals(Offset(x, 0.5f), motion.enemies.getValue("enemy-1").value)
        assertEquals(Offset(x, 0.5f), motion.allies.getValue("ally-1").value)
    }

    private fun sceneAt(x: Float): BattleSceneUi {
        val scene = testBattleScene()
        val enemy = scene.enemies.single().copy(xFraction = x, yFraction = 0.5f)
        return scene.copy(
            runId = "motion-test-run",
            enemies = listOf(enemy),
            allies = listOf(enemy.copy(id = "ally-1", role = BattleEntityRoleUi.ALLY_GUARD)),
        )
    }
}
