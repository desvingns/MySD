package dev.mysd.game.battle.playable

import dev.myengine.core.EngineCommand
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

    /** Requests the next sequential upgrade for an occupied tower. */
    data class UpgradeTower(
        val targetSlotId: ContentId,
    ) : PlayableBattleCommand {
        val slotId: ContentId
            get() = targetSlotId
    }
}

/** Canonical wire representation shared by command submission and replay reconstruction. */
internal object PlayableBattleCommandCodec {
    const val PAUSE_TYPE: String = "playable-battle.pause"
    const val RESUME_TYPE: String = "playable-battle.resume"
    const val SPEND_RESOURCE_TYPE: String = "playable-battle.spend-resource"
    const val BUILD_TOWER_TYPE: String = "playable-battle.build-tower"
    const val UPGRADE_TOWER_TYPE: String = "playable-battle.upgrade-tower"

    fun type(command: PlayableBattleCommand): String = when (command) {
        PlayableBattleCommand.Pause -> PAUSE_TYPE
        PlayableBattleCommand.Resume -> RESUME_TYPE
        is PlayableBattleCommand.SpendResource -> SPEND_RESOURCE_TYPE
        is PlayableBattleCommand.BuildTower -> BUILD_TOWER_TYPE
        is PlayableBattleCommand.UpgradeTower -> UPGRADE_TOWER_TYPE
    }

    fun payload(command: PlayableBattleCommand): String = when (command) {
        PlayableBattleCommand.Pause,
        PlayableBattleCommand.Resume,
        -> ""

        is PlayableBattleCommand.SpendResource ->
            listOf(command.targetSlotId?.value.orEmpty(), command.cost).joinToString("|")

        is PlayableBattleCommand.BuildTower -> command.targetSlotId.value

        is PlayableBattleCommand.UpgradeTower -> command.targetSlotId.value
    }

    fun decode(command: EngineCommand): PlayableBattleCommand? = when (command.type) {
        PAUSE_TYPE -> {
            require(command.stablePayload().isEmpty()) { "Pause command payload must be empty." }
            PlayableBattleCommand.Pause
        }

        RESUME_TYPE -> {
            require(command.stablePayload().isEmpty()) { "Resume command payload must be empty." }
            PlayableBattleCommand.Resume
        }

        SPEND_RESOURCE_TYPE -> decodeSpend(command.stablePayload())
        BUILD_TOWER_TYPE -> PlayableBattleCommand.BuildTower(ContentId.of(command.stablePayload()))
        UPGRADE_TOWER_TYPE -> PlayableBattleCommand.UpgradeTower(ContentId.of(command.stablePayload()))
        else -> null
    }

    private fun decodeSpend(payload: String): PlayableBattleCommand.SpendResource {
        val separator = payload.indexOf('|')
        require(separator >= 0 && separator == payload.lastIndexOf('|')) {
            "Spend resource command payload must contain one separator."
        }
        val rawTarget = payload.substring(0, separator)
        val cost = payload.substring(separator + 1).toIntOrNull()
        require(cost != null) { "Spend resource command cost must be an integer." }
        return PlayableBattleCommand.SpendResource(
            targetSlotId = rawTarget.takeIf(String::isNotEmpty)?.let(ContentId::of),
            cost = cost,
        )
    }
}
