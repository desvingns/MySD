package dev.mysd.android.product

import dev.mysd.game.product.MySdAppIntent
import dev.mysd.game.product.MySdAppFactory
import dev.mysd.game.product.MySdRoute
import dev.mysd.game.product.MySdBattlePhase
import dev.mysd.game.product.MySdTerminalResult
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class ProductFeedbackTest {
    @Test
    fun terminalTransitionDuringImmediateCommandTakesPrecedenceOverAbilityCue() {
        val session = MySdAppFactory.create()
        session.submit(MySdAppIntent.Navigate(MySdRoute.CAMPAIGN))
        session.submit(MySdAppIntent.SelectStage("stage-ember-path"))
        session.submit(MySdAppIntent.StartSelectedStage)
        val before = session.snapshot()
        val after = before.copy(battle = requireNotNull(before.battle).copy(
            phase = MySdBattlePhase.VICTORY,
            terminalResult = MySdTerminalResult.VICTORY,
        ))
        val intent = MySdAppIntent.UseHeroSkill("hero-solar-pulse")
        assertEquals(ProductFeedbackCue.VICTORY, ProductFeedbackPolicy.submission(before, after, intent, true))
        assertEquals(ProductFeedbackCue.ABILITY, ProductFeedbackPolicy.submission(before, before, intent, true))
        assertNull(ProductFeedbackPolicy.submission(before, before, intent, false))
    }

    @Test
    fun lifecycleAndSettingsIndependentlyGateSoundMusicAndHaptics() {
        val enabled = ProductFeedbackOptions(sound = true, music = true, haptics = true)
        assertFalse(ProductFeedbackPolicy.playSound(false, enabled))
        assertFalse(ProductFeedbackPolicy.playMusic(false, enabled))
        assertFalse(ProductFeedbackPolicy.vibrate(false, enabled, ProductFeedbackCue.BUILD))
        assertTrue(ProductFeedbackPolicy.playSound(true, enabled))
        assertTrue(ProductFeedbackPolicy.playMusic(true, enabled))
        assertTrue(ProductFeedbackPolicy.vibrate(true, enabled, ProductFeedbackCue.BUILD))
        assertFalse(ProductFeedbackPolicy.vibrate(true, enabled, ProductFeedbackCue.HIT))
        assertFalse(ProductFeedbackPolicy.playSound(true, enabled.copy(sound = false)))
        assertFalse(ProductFeedbackPolicy.playMusic(true, enabled.copy(music = false)))
        assertFalse(ProductFeedbackPolicy.vibrate(true, enabled.copy(haptics = false), ProductFeedbackCue.BUILD))
    }

    @Test
    fun onlyAcceptedGameplayActionsGenerateFeedbackAndEnergyCatchupIsSilent() {
        val build = MySdAppIntent.BuildTower("slot-one", "tower-ember-needle")
        assertNull(ProductFeedbackPolicy.action(build, accepted = false))
        assertEquals(ProductFeedbackCue.BUILD, ProductFeedbackPolicy.action(build, accepted = true))
        assertNull(ProductFeedbackPolicy.action(MySdAppIntent.RefreshEnergy(100L), accepted = true))
        assertEquals(ProductFeedbackCue.REWARD, ProductFeedbackPolicy.action(MySdAppIntent.ClaimBattleReward, true))
    }

    @Test
    fun originalPcmIsDeterministicBoundedNonSilentAndHasSoftEndpoints() {
        val music = ProductPcmScore.music()
        assertArrayEquals(music, ProductPcmScore.music())
        assertEquals(ProductPcmScore.SAMPLE_RATE * 8, music.size)
        assertTrue(music.any { abs(it.toInt()) > 500 })
        assertTrue(music.all { abs(it.toInt()) < 3_000 })
        ProductFeedbackCue.entries.forEach { cue ->
            val effect = ProductPcmScore.effect(cue)
            assertArrayEquals(effect, ProductPcmScore.effect(cue))
            assertTrue(effect.any { abs(it.toInt()) > 500 })
            assertEquals(0, effect.first().toInt())
            assertTrue(abs(effect.last().toInt()) <= 1)
        }
    }
}
