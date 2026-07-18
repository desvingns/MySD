package dev.mysd.game

import dev.myengine.core.Tick

/**
 * Build-time proof that MySD resolves the pinned MyEngine composite. Gameplay starts only after
 * the reference inventory passes Gate 1.
 */
object FoundationStatus {
    const val phase: String = "evidence-intake"

    fun engineTickAfterStart(): Long = Tick(0).next().value
}
