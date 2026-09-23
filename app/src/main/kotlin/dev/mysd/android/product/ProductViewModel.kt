package dev.mysd.android.product

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dev.mysd.android.persistence.ProductPersistenceStorage
import dev.mysd.game.product.MySdAppFactory
import dev.mysd.game.product.MySdAppIntent
import dev.mysd.game.product.MySdAppResult
import dev.mysd.game.product.MySdAppSession
import dev.mysd.game.product.MySdAppSnapshot
import dev.mysd.game.product.MySdRestoreResult
import dev.mysd.game.product.MySdRoute
import dev.mysd.game.product.MySdSaveBundle
import dev.mysd.game.product.MySdSettingId
import dev.mysd.game.product.MySdSurfaceSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Owns exactly one Android-free product session for the Activity/ViewModel lifetime. */
internal class ProductViewModel(
    private val persistence: ProductPersistenceStorage,
    private val epochSeconds: () -> Long = SYSTEM_EPOCH_SECONDS,
) : ViewModel() {
    private val restoration = restoreOrCreate(persistence)
    private val session: MySdAppSession = restoration.session
    private var pendingRecovery = restoration.pendingRecovery
    private val mutableRecoveryNotice = MutableStateFlow(restoration.notice)
    val recoveryNotice = mutableRecoveryNotice.asStateFlow()
    private val mutableSnapshot = MutableStateFlow(session.snapshot())
    val snapshot: StateFlow<MySdAppSnapshot> = mutableSnapshot.asStateFlow()
    private val mutableFeedback = MutableSharedFlow<ProductFeedbackCue>(
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val feedback = mutableFeedback.asSharedFlow()
    private val mutableSaveFailed = MutableStateFlow(false)
    val saveFailed = mutableSaveFailed.asStateFlow()

    init {
        refreshEnergy()
    }

    fun submit(action: ProductUiAction) {
        when (action) {
            ProductUiAction.EnterCampaign -> submit(MySdAppIntent.Navigate(MySdRoute.CAMPAIGN))
            is ProductUiAction.Navigate -> submit(
                MySdAppIntent.Navigate(action.destination.toRoute()),
            )
            ProductUiAction.OpenRewardTrack -> submit(
                MySdAppIntent.Navigate(MySdRoute.REWARD_TRACK),
            )
            ProductUiAction.OpenSettings -> submit(MySdAppIntent.Navigate(MySdRoute.SETTINGS))
            ProductUiAction.CloseOverlay -> submit(MySdAppIntent.CloseOverlay)
            ProductUiAction.ResumeRun -> submit(MySdAppIntent.ResumeRun)
            ProductUiAction.DiscardRun -> submit(MySdAppIntent.AbandonRun)
            is ProductUiAction.SelectStage -> submit(MySdAppIntent.SelectStage(action.stageId))
            ProductUiAction.BackToCampaign -> leaveCurrentSurface()
            is ProductUiAction.SetLoadoutSlot -> submit(
                MySdAppIntent.SetLoadoutSlot(action.index, action.contentId),
            )
            is ProductUiAction.ToggleHeroSkill -> submit(MySdAppIntent.ToggleHeroSkill(action.skillId))
            ProductUiAction.StartBattle -> submit(MySdAppIntent.StartSelectedStage)
            ProductUiAction.SweepStage -> currentSetupStageId()?.let { stageId ->
                submit(MySdAppIntent.SweepStage(stageId))
            }
            ProductUiAction.PauseOrResume -> submit(MySdAppIntent.PauseOrResume)
            ProductUiAction.RequestExitBattle,
            ProductUiAction.CancelExitBattle,
            -> Unit
            ProductUiAction.ConfirmExitBattle -> leaveCurrentSurface()
            ProductUiAction.ChangeSpeed -> submit(MySdAppIntent.ToggleBattleSpeed)
            is ProductUiAction.SelectBattleSlot -> Unit
            is ProductUiAction.BuildTower -> submit(
                MySdAppIntent.BuildTower(action.slotId, action.towerId),
            )
            is ProductUiAction.UpgradeTower -> submit(MySdAppIntent.UpgradeTower(action.slotId))
            is ProductUiAction.DeployAlly -> submit(MySdAppIntent.DeployAlly(action.allyId))
            is ProductUiAction.UseAbility -> submit(MySdAppIntent.UseHeroSkill(action.abilityId))
            is ProductUiAction.SelectEnhancement -> submit(
                MySdAppIntent.ChooseEnhancement(action.enhancementId),
            )
            ProductUiAction.RerollEnhancements -> submit(MySdAppIntent.RerollEnhancements)
            ProductUiAction.ClaimBattleReward -> submit(MySdAppIntent.ClaimBattleReward)
            ProductUiAction.ClaimBattleRewardMultiplier -> submit(
                MySdAppIntent.RequestRewardedStub(REWARDED_MULTIPLIER_ID),
            )
            ProductUiAction.RetryBattle -> retryCurrentStage()
            is ProductUiAction.ToggleRosterEntry -> equipRosterEntry(action.entryId)
            is ProductUiAction.UpgradeRosterEntry -> submit(
                MySdAppIntent.UpgradeRosterItem(action.entryId),
            )
            is ProductUiAction.UnlockTech -> submit(MySdAppIntent.UnlockTech(action.nodeId))
            is ProductUiAction.BuyShopProduct -> submit(
                MySdAppIntent.BuyShopOffer(action.productId),
            )
            is ProductUiAction.RequestRewardedStub -> submit(
                MySdAppIntent.RequestRewardedStub(action.opportunityId),
            )
            is ProductUiAction.RequestPurchaseStub -> submit(
                MySdAppIntent.RequestPurchaseStub(action.productId),
            )
            is ProductUiAction.ClaimRewardTier -> submit(
                MySdAppIntent.ClaimRewardTier(action.tierId),
            )
            is ProductUiAction.ToggleSetting -> submit(
                MySdAppIntent.ToggleSetting(action.id.toDomain()),
            )
            ProductUiAction.ArenaFindOpponent,
            ProductUiAction.ArenaStartBattle,
            -> submit(MySdAppIntent.StartArenaExhibition)
            ProductUiAction.ArenaCancelPreview,
            ProductUiAction.ArenaReturnToLobby,
            -> submit(MySdAppIntent.Navigate(MySdRoute.CAMPAIGN))
        }
    }

    fun pulse() {
        val previous = mutableSnapshot.value
        val result = session.pulse()
        publish(result)
        val current = result.snapshot
        ProductFeedbackPolicy.battle(previous, current)?.let(mutableFeedback::tryEmit)
        val importantTransition = previous.route != current.route ||
            previous.battle?.phase != current.battle?.phase ||
            previous.overlay?.let { it::class } != current.overlay?.let { it::class }
        // One pulse is one completed authoritative batch (one or two fixed ticks). Persist its
        // exact result, including ordinary COMBAT-to-COMBAT wave changes. Frozen pulses do not
        // write again; failed writes retain the visible state and use the existing retry path.
        if (importantTransition || current.clock.tick != previous.clock.tick) {
            persistNow()
        }
    }

    /** Applies wall-clock energy catch-up without advancing any authoritative battle tick. */
    fun refreshEnergy() {
        submit(MySdAppIntent.RefreshEnergy(epochSeconds()))
    }

    fun persistNow(): Boolean {
        pendingRecovery?.let { source ->
            if (!persistence.archiveRejectedBundle(source.profile, source.run, source.reason)) {
                mutableSaveFailed.value = true
                return false
            }
            pendingRecovery = null
            mutableRecoveryNotice.value = mutableRecoveryNotice.value?.copy(copyArchived = true)
        }
        val bundle = session.saveBundle()
        val saved = persistence.save(bundle)
        mutableSaveFailed.value = !saved
        return saved
    }

    fun dismissRecoveryNotice() { mutableRecoveryNotice.value = null }

    override fun onCleared() {
        persistNow()
        super.onCleared()
    }

    private fun submit(intent: MySdAppIntent): MySdAppResult {
        val previous = mutableSnapshot.value
        val result = session.submit(intent)
        publish(result)
        ProductFeedbackPolicy.submission(previous, result.snapshot, intent, result.accepted)
            ?.let(mutableFeedback::tryEmit)
        persistNow()
        return result
    }

    private fun publish(result: MySdAppResult) {
        mutableSnapshot.value = result.snapshot
    }

    private fun leaveCurrentSurface() {
        if (mutableSnapshot.value.route == MySdRoute.BATTLE) {
            submit(MySdAppIntent.AbandonRun)
        } else {
            submit(MySdAppIntent.Navigate(MySdRoute.CAMPAIGN))
        }
    }

    private fun retryCurrentStage() {
        val stageId = mutableSnapshot.value.battle?.stageId ?: return
        val abandoned = submit(MySdAppIntent.AbandonRun)
        if (!abandoned.accepted) return
        val selected = submit(MySdAppIntent.SelectStage(stageId))
        if (!selected.accepted) return
        submit(MySdAppIntent.StartSelectedStage)
    }

    private fun currentSetupStageId(): String? =
        (mutableSnapshot.value.surface as? MySdSurfaceSnapshot.StageSetup)?.stage?.stageId

    private fun equipRosterEntry(contentId: String) {
        val roster = mutableSnapshot.value.surface as? MySdSurfaceSnapshot.Roster ?: return
        val equippedIndex = roster.selectedLoadoutIds.indexOf(contentId)
        if (equippedIndex >= 0) {
            submit(MySdAppIntent.ClearLoadoutSlot(equippedIndex))
            return
        }
        val entry = roster.entries.firstOrNull { it.contentId == contentId } ?: return
        if (entry.category.contains("hero", ignoreCase = true)) {
            submit(MySdAppIntent.ToggleHeroSkill(contentId))
            return
        }
        val towerCategory = entry.category.contains("tower", ignoreCase = true)
        val target = if (roster.selectedLoadoutIds.size < MAX_LOADOUT_SIZE) {
            roster.selectedLoadoutIds.size
        } else {
            roster.selectedLoadoutIds.indexOfFirst { equippedId ->
                equippedId.isTowerId() == towerCategory
            }.takeIf { it >= 0 } ?: return
        }
        submit(MySdAppIntent.SetLoadoutSlot(target, contentId))
    }

    internal class Factory(
        private val persistence: ProductPersistenceStorage,
        private val epochSeconds: () -> Long = SYSTEM_EPOCH_SECONDS,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(ProductViewModel::class.java)) {
                "Unsupported ViewModel class: ${modelClass.name}"
            }
            return ProductViewModel(persistence, epochSeconds) as T
        }
    }

    private companion object {
        const val REWARDED_MULTIPLIER_ID = "rewarded-multiplier"
        const val MAX_LOADOUT_SIZE = 7
        val SYSTEM_EPOCH_SECONDS: () -> Long = { System.currentTimeMillis() / 1_000L }
    }
}

internal data class ProductRecoveryNotice(val profileRetained: Boolean, val copyArchived: Boolean)
private data class RejectedProductSource(val profile: String?, val run: String?, val reason: String)
private data class ProductRestoration(
    val session: MySdAppSession,
    val notice: ProductRecoveryNotice? = null,
    val pendingRecovery: RejectedProductSource? = null,
)

private fun restoreOrCreate(persistence: ProductPersistenceStorage): ProductRestoration {
    val fresh = MySdAppFactory.create()
    val defaults = fresh.saveBundle()
    val storedProfile = persistence.loadProfileSave()
    val storedRun = persistence.loadRunSave()
    if (storedProfile == null && storedRun == null) return ProductRestoration(fresh)
    val bundle = MySdSaveBundle(
        runSave = storedRun,
        profileSave = storedProfile ?: defaults.profileSave,
    )
    return when (val restored = MySdAppFactory.restore(bundle)) {
        is MySdRestoreResult.Restored -> ProductRestoration(restored.session)
        is MySdRestoreResult.Incompatible,
        is MySdRestoreResult.Invalid,
        -> {
            val fallback = if (storedProfile == null) {
                ProductRestoration(fresh)
            } else {
                when (
                    val profileOnly = MySdAppFactory.restore(
                        bundle.copy(runSave = null),
                    )
                ) {
                    is MySdRestoreResult.Restored -> ProductRestoration(
                        profileOnly.session,
                        ProductRecoveryNotice(profileRetained = true, copyArchived = false),
                    )
                    is MySdRestoreResult.Incompatible,
                    is MySdRestoreResult.Invalid,
                    -> ProductRestoration(fresh)
                }
            }
            val source = RejectedProductSource(storedProfile, storedRun, restored.toString())
            val archived = persistence.archiveRejectedBundle(source.profile, source.run, source.reason)
            fallback.copy(
                notice = ProductRecoveryNotice(fallback.notice?.profileRetained == true, archived),
                pendingRecovery = if (archived) null else source,
            )
        }
    }
}

private fun String.isTowerId(): Boolean = startsWith("tower-")

private fun ProductDestinationUi.toRoute(): MySdRoute = when (this) {
    ProductDestinationUi.CAMPAIGN -> MySdRoute.CAMPAIGN
    ProductDestinationUi.ROSTER -> MySdRoute.ROSTER
    ProductDestinationUi.TECH -> MySdRoute.TECHNOLOGY
    ProductDestinationUi.SHOP -> MySdRoute.SHOP
    ProductDestinationUi.ARENA -> MySdRoute.ARENA
}

private fun ProductSettingUi.toDomain(): MySdSettingId = when (this) {
    ProductSettingUi.SOUND -> MySdSettingId.SOUND
    ProductSettingUi.MUSIC -> MySdSettingId.MUSIC
    ProductSettingUi.HAPTICS -> MySdSettingId.HAPTICS
    ProductSettingUi.REDUCED_MOTION -> MySdSettingId.REDUCE_MOTION
}
