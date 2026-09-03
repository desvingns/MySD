package dev.mysd.game.battle.playable

import dev.mysd.game.content.ContentId

/** Immutable domain commands accepted by the Android-free playable battle boundary. */
sealed interface PlayableBattleCommand {
    data object Pause : PlayableBattleCommand

    data object Resume : PlayableBattleCommand

    data class SpendResource(
        val targetSlotId: ContentId?,
        val cost: Int,
    ) : PlayableBattleCommand

    /** Requests construction of the one tower configured by the current level. */
    data class BuildTower(
        val targetSlotId: ContentId,
    ) : PlayableBattleCommand {
        val slotId: ContentId
            get() = targetSlotId
    }
}
