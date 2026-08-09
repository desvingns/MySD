package dev.mysd.game.meta

import dev.mysd.game.content.ContentId
import dev.mysd.game.content.OriginalContentIds

enum class RosterSurface {
    TROOPS,
    SETTINGS,
}

enum class RosterSettingId(
    val stableId: String,
) {
    AUDIO("setting-audio"),
    HAPTICS("setting-haptics"),
}

data class RosterTroopSlot(
    val id: ContentId,
    val upgradeAffordanceVisible: Boolean,
)

data class RosterSettingOption(
    val id: RosterSettingId,
    val toggleAffordanceVisible: Boolean,
)

data class RosterSnapshot(
    val surface: RosterSurface,
    val troopSlots: List<RosterTroopSlot>,
    val settings: List<RosterSettingOption>,
)

sealed interface RosterIntent {
    data object OpenSettings : RosterIntent

    data object CloseSettings : RosterIntent

    data object ConfirmSettings : RosterIntent

    data class UpgradeTroop(
        val troopId: ContentId,
    ) : RosterIntent

    data class ToggleSetting(
        val settingId: RosterSettingId,
    ) : RosterIntent
}

/**
 * Android-free owner of the accepted troops roster and local settings contour.
 *
 * Upgrade and toggle intents are visible affordance no-ops until their semantics are observed
 * and accepted. The session therefore exposes no mutable progression, loadout, or settings value.
 */
class RosterSession(
    acceptedTroopIds: List<ContentId> = listOf(OriginalContentIds.FOUNDATION_UNIT),
    settingIds: List<RosterSettingId> = RosterSettingId.entries.toList(),
) {
    private val troopSlots = acceptedTroopIds.map { id ->
        RosterTroopSlot(id = id, upgradeAffordanceVisible = true)
    }
    private val settings = settingIds.map { id ->
        RosterSettingOption(id = id, toggleAffordanceVisible = true)
    }

    private var state = RosterSnapshot(
        surface = RosterSurface.TROOPS,
        troopSlots = troopSlots,
        settings = settings,
    )

    init {
        require(troopSlots.isNotEmpty()) { "Roster must contain at least one accepted troop." }
        require(troopSlots.map { it.id }.distinct().size == troopSlots.size) {
            "Roster troop ids must be unique."
        }
        require(settings.isNotEmpty()) { "Roster must contain at least one local setting." }
        require(settings.map { it.id }.distinct().size == settings.size) {
            "Roster setting ids must be unique."
        }
    }

    fun snapshot(): RosterSnapshot = state.copy(
        troopSlots = state.troopSlots.toList(),
        settings = state.settings.toList(),
    )

    fun submit(intent: RosterIntent): RosterSnapshot {
        state = when (intent) {
            RosterIntent.OpenSettings -> if (state.surface == RosterSurface.TROOPS) {
                state.copy(surface = RosterSurface.SETTINGS)
            } else {
                state
            }

            RosterIntent.CloseSettings,
            RosterIntent.ConfirmSettings,
            -> if (state.surface == RosterSurface.SETTINGS) {
                state.copy(surface = RosterSurface.TROOPS)
            } else {
                state
            }

            is RosterIntent.UpgradeTroop,
            is RosterIntent.ToggleSetting,
            -> state
        }
        return snapshot()
    }
}
