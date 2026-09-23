package dev.mysd.android.product

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.SystemClock
import android.view.HapticFeedbackConstants
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import dev.mysd.game.product.MySdAppIntent
import dev.mysd.game.product.MySdAppSnapshot
import dev.mysd.game.product.MySdSettingId
import dev.mysd.game.product.MySdTerminalResult
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

/** Presentation-only feedback never enters simulation state, saves, or replay hashes. */
internal enum class ProductFeedbackCue(val haptic: Boolean = false) {
    TAP(true), BUILD(true), ABILITY(true), REWARD(true), HIT, BASE_HIT, VICTORY(true), DEFEAT(true),
}

internal data class ProductFeedbackOptions(val sound: Boolean, val music: Boolean, val haptics: Boolean) {
    companion object {
        fun from(snapshot: MySdAppSnapshot) = ProductFeedbackOptions(
            sound = snapshot.profile.settings[MySdSettingId.SOUND] == true,
            music = snapshot.profile.settings[MySdSettingId.MUSIC] == true,
            haptics = snapshot.profile.settings[MySdSettingId.HAPTICS] == true,
        )
    }
}

internal object ProductFeedbackPolicy {
    fun submission(
        previous: MySdAppSnapshot,
        current: MySdAppSnapshot,
        intent: MySdAppIntent,
        accepted: Boolean,
    ): ProductFeedbackCue? {
        if (!accepted) return null
        val battleCue = battle(previous, current)
        return battleCue?.takeIf { it == ProductFeedbackCue.VICTORY || it == ProductFeedbackCue.DEFEAT }
            ?: action(intent, accepted = true)
    }

    fun action(intent: MySdAppIntent, accepted: Boolean): ProductFeedbackCue? {
        if (!accepted) return null
        return when (intent) {
            is MySdAppIntent.BuildTower,
            is MySdAppIntent.UpgradeTower,
            is MySdAppIntent.DeployAlly,
            -> ProductFeedbackCue.BUILD
            is MySdAppIntent.UseHeroSkill -> ProductFeedbackCue.ABILITY
            is MySdAppIntent.ChooseEnhancement,
            is MySdAppIntent.UnlockTech,
            is MySdAppIntent.UpgradeRosterItem,
            is MySdAppIntent.ClaimRewardTier,
            MySdAppIntent.ClaimBattleReward,
            is MySdAppIntent.BuyShopOffer,
            is MySdAppIntent.SweepStage,
            -> ProductFeedbackCue.REWARD
            is MySdAppIntent.RefreshEnergy -> null
            else -> ProductFeedbackCue.TAP
        }
    }

    fun battle(previous: MySdAppSnapshot, current: MySdAppSnapshot): ProductFeedbackCue? {
        val before = previous.battle ?: return null
        val after = current.battle ?: return null
        if (before.runId != after.runId) return null
        if (before.terminalResult == null && after.terminalResult != null) {
            return if (after.terminalResult == MySdTerminalResult.VICTORY) {
                ProductFeedbackCue.VICTORY
            } else {
                ProductFeedbackCue.DEFEAT
            }
        }
        if (after.baseHealth < before.baseHealth) return ProductFeedbackCue.BASE_HIT
        val remaining = after.enemies.associateBy { it.entityId }
        return if (before.enemies.any { enemy ->
                (remaining[enemy.entityId]?.health ?: 0) < enemy.health
            }) ProductFeedbackCue.HIT else null
    }

    fun playSound(resumed: Boolean, options: ProductFeedbackOptions): Boolean = resumed && options.sound
    fun playMusic(resumed: Boolean, options: ProductFeedbackOptions): Boolean = resumed && options.music
    fun vibrate(resumed: Boolean, options: ProductFeedbackOptions, cue: ProductFeedbackCue): Boolean =
        resumed && options.haptics && cue.haptic
}

@Composable
internal fun ProductFeedbackHost(viewModel: ProductViewModel, lifecycle: Lifecycle) {
    val context = LocalContext.current.applicationContext
    val view = LocalView.current
    val audio = remember(context) { ProductAudioFeedback(context) }
    DisposableEffect(audio) { onDispose { audio.close() } }
    LaunchedEffect(viewModel, lifecycle, audio, view) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            try {
                coroutineScope {
                    launch {
                        viewModel.snapshot.map { ProductFeedbackOptions.from(it) }.distinctUntilChanged()
                            .collect { audio.setOptions(it) }
                    }
                    launch {
                        viewModel.feedback.collect { cue ->
                            val options = ProductFeedbackOptions.from(viewModel.snapshot.value)
                            if (ProductFeedbackPolicy.playSound(true, options)) audio.play(cue)
                            if (ProductFeedbackPolicy.vibrate(true, options, cue)) {
                                view.performHapticFeedback(
                                    if (cue == ProductFeedbackCue.VICTORY || cue == ProductFeedbackCue.DEFEAT) {
                                        HapticFeedbackConstants.LONG_PRESS
                                    } else {
                                        HapticFeedbackConstants.VIRTUAL_KEY
                                    },
                                )
                            }
                        }
                    }
                }
            } finally {
                audio.suspendPlayback()
            }
        }
    }
}

/** Original procedural PCM score and effects. No asset, network, or storage access. */
internal object ProductPcmScore {
    const val SAMPLE_RATE = 16_000

    fun music(): ShortArray {
        // An original eight-second, four-chord arpeggio. Every note has a zero-ended envelope.
        val roots = doubleArrayOf(130.8128, 174.6141, 146.8324, 195.9977)
        val intervals = doubleArrayOf(1.0, 1.5, 2.0, 2.5)
        return ShortArray(SAMPLE_RATE * 8) { sample ->
            val seconds = sample.toDouble() / SAMPLE_RATE
            val noteTime = seconds % 0.5
            val chord = (seconds / 2).toInt().coerceAtMost(3)
            val note = (seconds / 0.5).toInt() % 4
            val envelope = sin(PI * noteTime / 0.5).let { it * it }
            val frequency = roots[chord] * intervals[note]
            val signal = sin(2 * PI * frequency * seconds) * 0.65 +
                sin(2 * PI * roots[chord] * seconds) * 0.25
            (signal * envelope * 2_600).toInt().toShort()
        }
    }

    fun effect(cue: ProductFeedbackCue): ShortArray {
        val duration = when (cue) {
            ProductFeedbackCue.VICTORY, ProductFeedbackCue.DEFEAT -> 0.6
            ProductFeedbackCue.REWARD, ProductFeedbackCue.ABILITY -> 0.25
            else -> 0.10
        }
        val start = when (cue) {
            ProductFeedbackCue.TAP -> 640.0
            ProductFeedbackCue.BUILD -> 220.0
            ProductFeedbackCue.ABILITY -> 330.0
            ProductFeedbackCue.REWARD -> 523.25
            ProductFeedbackCue.HIT -> 180.0
            ProductFeedbackCue.BASE_HIT -> 95.0
            ProductFeedbackCue.VICTORY -> 392.0
            ProductFeedbackCue.DEFEAT -> 196.0
        }
        val slope = when (cue) {
            ProductFeedbackCue.VICTORY, ProductFeedbackCue.REWARD, ProductFeedbackCue.BUILD -> 0.9
            ProductFeedbackCue.DEFEAT, ProductFeedbackCue.BASE_HIT, ProductFeedbackCue.HIT -> -0.45
            else -> 0.25
        }
        return ShortArray((SAMPLE_RATE * duration).toInt()) { sample ->
            val seconds = sample.toDouble() / SAMPLE_RATE
            val progress = seconds / duration
            val envelope = sin(PI * progress) * (1.0 - progress)
            // Integrated linear frequency sweep keeps phase continuous.
            val phase = 2 * PI * start * (seconds + slope * seconds * seconds / (2 * duration))
            (sin(phase) * envelope * 5_000).toInt().toShort()
        }
    }
}

private class ProductAudioFeedback(context: Context) : AutoCloseable {
    private val manager = context.getSystemService(AudioManager::class.java)
    private val attributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_GAME)
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
        .build()
    private var options = ProductFeedbackOptions(false, false, false)
    private var active = false
    private var focused = false
    private var closed = false
    private var music: AudioTrack? = null
    private val effects = mutableMapOf<ProductFeedbackCue, AudioTrack>()
    private var lastImpactMillis = Long.MIN_VALUE
    private val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
        .setAudioAttributes(attributes)
        .setOnAudioFocusChangeListener { focus ->
            focused = focus == AudioManager.AUDIOFOCUS_GAIN
            if (focused && active && options.music) startMusic() else pauseTracks()
        }
        .build()

    fun setOptions(value: ProductFeedbackOptions) {
        if (closed) return
        options = value
        active = true
        if (!value.sound) effects.values.forEach { track -> safely { track.pause() } }
        if (!value.sound && !value.music) {
            suspendPlayback()
            return
        }
        if (!focused) {
            focused = manager.requestAudioFocus(focusRequest) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
        if (value.music && focused) startMusic() else safely { music?.pause() }
    }

    fun play(cue: ProductFeedbackCue) {
        if (closed || !active || !focused || !options.sound) return
        if (cue == ProductFeedbackCue.HIT || cue == ProductFeedbackCue.BASE_HIT) {
            val now = SystemClock.elapsedRealtime()
            if (lastImpactMillis != Long.MIN_VALUE && now - lastImpactMillis < 120) return
            lastImpactMillis = now
        }
        val track = effects[cue] ?: createTrack(ProductPcmScore.effect(cue), loop = false)?.also {
            effects[cue] = it
        } ?: return
        safely {
            track.stop()
            track.reloadStaticData()
            track.play()
        }
    }

    private fun startMusic() {
        if (closed || !active || !focused || !options.music) return
        val track = music ?: createTrack(ProductPcmScore.music(), loop = true)?.also { music = it }
        safely { track?.play() }
    }

    private fun createTrack(samples: ShortArray, loop: Boolean): AudioTrack? {
        var track: AudioTrack? = null
        return try {
            track = AudioTrack.Builder()
                .setAudioAttributes(attributes)
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(ProductPcmScore.SAMPLE_RATE)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build(),
                )
                .setTransferMode(AudioTrack.MODE_STATIC)
                .setBufferSizeInBytes(samples.size * 2)
                .build()
            check(track.state == AudioTrack.STATE_INITIALIZED)
            check(track.write(samples, 0, samples.size) == samples.size)
            if (loop) check(track.setLoopPoints(0, samples.size, -1) == AudioTrack.SUCCESS)
            track
        } catch (_: RuntimeException) {
            safely { track?.release() }
            null // A device without an output still has a fully playable game.
        }
    }

    fun suspendPlayback() {
        active = false
        pauseTracks()
        // A transient loss still owns a focus request, even though focused is already false.
        manager.abandonAudioFocusRequest(focusRequest)
        focused = false
    }

    private fun pauseTracks() {
        safely { music?.pause() }
        effects.values.forEach { safely { it.pause() } }
    }

    override fun close() {
        if (closed) return
        suspendPlayback()
        closed = true
        safely { music?.release() }
        effects.values.forEach { safely { it.release() } }
        effects.clear()
        music = null
    }

    private inline fun safely(block: () -> Unit) {
        try { block() } catch (_: IllegalStateException) { /* Audio hardware may disappear. */ }
    }
}
