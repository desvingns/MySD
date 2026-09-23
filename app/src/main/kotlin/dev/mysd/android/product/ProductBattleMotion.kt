package dev.mysd.android.product

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.geometry.Offset
import dev.mysd.game.simulation.SimulationClock

/** Draw-only coordinates. Logical DTOs, input, semantics and saves never consume these states. */
internal data class ProductBattleMotion(
    val enemies: Map<String, State<Offset>>,
    val allies: Map<String, State<Offset>>,
)

@Composable
internal fun rememberProductBattleMotion(
    scene: BattleSceneUi,
    reduceMotion: Boolean,
    resumed: Boolean,
): ProductBattleMotion {
    val animate = resumed && !reduceMotion &&
        scene.phase in setOf(BattlePhaseUi.WAVE_INTRO, BattlePhaseUi.ACTIVE)
    return ProductBattleMotion(
        enemies = entityPositions(scene.runId, "enemy", scene.enemies, animate),
        allies = entityPositions(scene.runId, "ally", scene.allies, animate),
    )
}

@Composable
private fun entityPositions(
    runId: String,
    side: String,
    entities: List<BattleEntityUi>,
    animate: Boolean,
): Map<String, State<Offset>> = entities.associate { entity ->
    entity.id to key(runId, side, entity.id) {
        val target = Offset(entity.xFraction, entity.yFraction)
        if (animate) {
            // Both 1x and 2x publish once per wall-clock pulse; 2x advances two logical ticks.
            // Retarget from the current visual position, without extrapolating past known state.
            // New entities/runs start at their known position and removed entities vanish at once.
            animateOffsetAsState(
                targetValue = target,
                animationSpec = tween(
                    durationMillis = SimulationClock.TICK_DURATION_MILLIS.toInt(),
                    easing = LinearEasing,
                ),
                label = "battle-$side-${entity.id}",
            )
        } else {
            // Leaving this branch cancels the animation. Pause, choices, terminal, reduced motion
            // and background all snap immediately to the authoritative position and then stay still.
            rememberUpdatedState(target)
        }
    }
}
