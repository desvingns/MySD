package dev.mysd.game.product

import dev.mysd.game.content.ContentId
import dev.mysd.game.product.content.OriginalProductCatalog
import dev.mysd.game.product.content.ProductCatalog
import dev.mysd.game.product.content.ProductShopOfferKind
import dev.mysd.game.product.meta.ProductBattleReward
import dev.mysd.game.product.meta.ProductProfileManager
import dev.mysd.game.product.meta.ProductProfileMutation
import dev.mysd.game.product.meta.ProductProfileRejection
import dev.mysd.game.product.persistence.ProductProfileCodec
import dev.mysd.game.product.persistence.ProductProfileDecodeResult
import dev.mysd.game.product.persistence.ProductRunDecodeResult
import dev.mysd.game.product.persistence.ProductRunSaveCodec
import dev.mysd.game.product.runtime.BattleRuntimeAdapter
import dev.mysd.game.product.runtime.ProductBattleAction
import dev.mysd.game.product.runtime.ProductBattleActionResult
import dev.mysd.game.product.runtime.ProductBattleController
import dev.mysd.game.product.runtime.ProductBattleIncompatibility
import dev.mysd.game.product.runtime.ProductBattleLaunch
import dev.mysd.game.product.runtime.ProductBattleRejection
import dev.mysd.game.product.runtime.ProductBattleRestoreResult
import dev.mysd.game.product.runtime.ProductBattleStateView
import dev.mysd.game.product.runtime.ProductBattleStepResult
import kotlin.math.max

/** Complete Android-free application/session reducer used by the shipping shell. */
internal class DefaultMySdAppSession private constructor(
    private val catalog: ProductCatalog,
    private val profile: ProductProfileManager,
    private var battle: ProductBattleController?,
) : MySdAppSession {
    @Synchronized
    override fun snapshot(): MySdAppSnapshot = project()

    @Synchronized
    override fun submit(intent: MySdAppIntent): MySdAppResult {
        if (!hasMutationCapacity()) return rejected(MySdAppRejection.INVALID_TARGET)
        return when (intent) {
        is MySdAppIntent.Navigate -> navigate(intent.route)
        is MySdAppIntent.SelectStage -> selectStage(intent.stageId)
        MySdAppIntent.StartSelectedStage -> startSelectedStage()
        MySdAppIntent.ResumeRun -> resumeRun()
        MySdAppIntent.AbandonRun -> abandonRun()
        MySdAppIntent.PauseOrResume -> withBattleRoute { controller, state ->
            submitBattle(
                controller,
                state,
                ProductBattleAction.SetPaused(!state.paused),
                "battle_pause",
                state.runId,
            )
        }
        MySdAppIntent.ToggleBattleSpeed -> withBattleRoute { controller, state ->
            val speed = if (state.speed == MySdBattleSpeed.ONE_X) MySdBattleSpeed.TWO_X else MySdBattleSpeed.ONE_X
            submitBattle(controller, state, ProductBattleAction.SetSpeed(speed), "battle_speed", speed.name)
        }
        is MySdAppIntent.BuildTower -> withBattleRoute { controller, state ->
            val slot = contentIdOrReject(intent.slotId) ?: return rejected(MySdAppRejection.UNKNOWN_CONTENT)
            val tower = contentIdOrReject(intent.towerId) ?: return rejected(MySdAppRejection.UNKNOWN_CONTENT)
            submitBattle(controller, state, ProductBattleAction.BuildTower(slot, tower), "tower_built", intent.towerId)
        }
        is MySdAppIntent.UpgradeTower -> withBattleRoute { controller, state ->
            val slot = contentIdOrReject(intent.slotId) ?: return rejected(MySdAppRejection.UNKNOWN_CONTENT)
            submitBattle(controller, state, ProductBattleAction.UpgradeTower(slot), "tower_upgraded", intent.slotId)
        }
        is MySdAppIntent.DeployAlly -> withBattleRoute { controller, state ->
            val ally = contentIdOrReject(intent.allyId) ?: return rejected(MySdAppRejection.UNKNOWN_CONTENT)
            submitBattle(controller, state, ProductBattleAction.DeployAlly(ally), "ally_deployed", intent.allyId)
        }
        is MySdAppIntent.UseHeroSkill -> withBattleRoute { controller, state ->
            val skill = contentIdOrReject(intent.skillId) ?: return rejected(MySdAppRejection.UNKNOWN_CONTENT)
            submitBattle(controller, state, ProductBattleAction.UseHeroSkill(skill), "hero_skill", intent.skillId)
        }
        is MySdAppIntent.ChooseEnhancement -> withBattleRoute { controller, state ->
            val enhancement = contentIdOrReject(intent.enhancementId)
                ?: return rejected(MySdAppRejection.UNKNOWN_CONTENT)
            submitBattle(
                controller,
                state,
                ProductBattleAction.ChooseEnhancement(enhancement),
                "enhancement_chosen",
                intent.enhancementId,
            )
        }
        MySdAppIntent.RerollEnhancements -> withBattleRoute { controller, state ->
            submitBattle(
                controller,
                state,
                ProductBattleAction.RerollEnhancements,
                "enhancements_rerolled",
                state.runId,
            )
        }
        MySdAppIntent.ClaimBattleReward -> claimBattleReward()
        is MySdAppIntent.SetLoadoutSlot -> setLoadout(intent.index, intent.contentId)
        is MySdAppIntent.ClearLoadoutSlot -> clearLoadout(intent.index)
        is MySdAppIntent.ToggleHeroSkill -> toggleHeroSkill(intent.skillId)
        is MySdAppIntent.UpgradeRosterItem -> upgradeRoster(intent.contentId)
        is MySdAppIntent.UnlockTech -> unlockTech(intent.nodeId)
        is MySdAppIntent.BuyShopOffer -> buyShopOffer(intent.offerId)
        is MySdAppIntent.ClaimRewardTier -> claimRewardTier(intent.tierId)
        is MySdAppIntent.SweepStage -> sweepStage(intent.stageId)
        is MySdAppIntent.ToggleSetting -> toggleSetting(intent.settingId)
        is MySdAppIntent.RefreshEnergy -> refreshEnergy(intent.nowEpochSeconds)
        is MySdAppIntent.RequestRewardedStub -> serviceStub("rewarded:${intent.opportunityId}")
        is MySdAppIntent.RequestPurchaseStub -> serviceStub("purchase:${intent.productId}")
        MySdAppIntent.StartArenaExhibition -> startArena()
            MySdAppIntent.CloseOverlay -> closeOverlay()
        }
    }

    @Synchronized
    override fun pulse(): MySdAppResult {
        val state = battle?.snapshot() ?: return unchanged()
        if (profile.state.route != MySdRoute.BATTLE || state.paused ||
            state.phase == MySdBattlePhase.ENHANCEMENT || state.terminalResult != null
        ) return unchanged()
        if (!hasMutationCapacity()) return rejected(MySdAppRejection.INVALID_TARGET)
        return advanceBattle(state.speed.ticksPerPulse)
    }

    @Synchronized
    override fun step(ticks: Int): MySdAppResult {
        if (ticks !in 1..MAX_TICKS_PER_STEP) return rejected(MySdAppRejection.INVALID_TICK_COUNT)
        if (profile.state.route != MySdRoute.BATTLE) return rejected(MySdAppRejection.WRONG_ROUTE)
        val state = battle?.snapshot() ?: return rejected(MySdAppRejection.NO_ACTIVE_RUN)
        if (state.paused || state.phase == MySdBattlePhase.ENHANCEMENT || state.terminalResult != null) {
            return unchanged()
        }
        if (!hasMutationCapacity()) return rejected(MySdAppRejection.INVALID_TARGET)
        return advanceBattle(ticks)
    }

    @Synchronized
    override fun saveBundle(): MySdSaveBundle = MySdSaveBundle(
        runSave = battle?.save()?.let(ProductRunSaveCodec::encode),
        profileSave = ProductProfileCodec.encode(profile.state, catalog),
    )

    private fun navigate(route: MySdRoute): MySdAppResult {
        battle?.snapshot()?.let { active ->
            if (active.terminalResult == null && route != MySdRoute.BATTLE && route != MySdRoute.HOME) {
                return rejected(MySdAppRejection.ACTIVE_RUN_EXISTS)
            }
            if (active.terminalResult != null && active.runId !in profile.state.claimedBattleRuns &&
                route != MySdRoute.BATTLE
            ) return rejected(MySdAppRejection.TERMINAL_RUN)
        }
        when (route) {
            MySdRoute.BATTLE -> if (battle == null) return rejected(MySdAppRejection.NO_ACTIVE_RUN)
            MySdRoute.STAGE_SETUP -> {
                val selected = profile.state.selectedStageId
                if (selected == null || selected !in profile.state.unlockedStages) {
                    return rejected(MySdAppRejection.LOCKED)
                }
            }
            else -> Unit
        }
        profile.state.route = route
        profile.state.serviceMessageCode = null
        return accepted(event("route_changed", route.name))
    }

    private fun selectStage(rawStageId: String): MySdAppResult {
        if (profile.state.route != MySdRoute.CAMPAIGN && profile.state.route != MySdRoute.STAGE_SETUP) {
            return rejected(MySdAppRejection.WRONG_ROUTE)
        }
        val stageId = contentIdOrReject(rawStageId) ?: return rejected(MySdAppRejection.UNKNOWN_CONTENT)
        if (stageId !in catalog.stages) return rejected(MySdAppRejection.UNKNOWN_CONTENT)
        if (stageId !in profile.state.unlockedStages) return rejected(MySdAppRejection.LOCKED)
        profile.state.selectedStageId = stageId
        profile.state.route = MySdRoute.STAGE_SETUP
        return accepted(event("stage_selected", rawStageId))
    }

    private fun startSelectedStage(): MySdAppResult {
        if (profile.state.route != MySdRoute.STAGE_SETUP) return rejected(MySdAppRejection.WRONG_ROUTE)
        val stageId = profile.state.selectedStageId ?: return rejected(MySdAppRejection.INVALID_TARGET)
        val stage = catalog.stages[stageId] ?: return rejected(MySdAppRejection.UNKNOWN_CONTENT)
        battle?.snapshot()?.let { current ->
            if (current.terminalResult == null || current.runId !in profile.state.claimedBattleRuns) {
                return rejected(MySdAppRejection.ACTIVE_RUN_EXISTS)
            }
        }
        when (val spent = profile.spendStageEnergy(stage)) {
            is ProductProfileMutation.Rejected -> return rejected(mapProfileRejection(spent.reason))
            is ProductProfileMutation.Applied -> Unit
        }
        val bonuses = profile.techBonuses()
        val runOrdinal = profile.state.nextRunOrdinal++
        val seed = mix(profile.state.sessionSeed xor runOrdinal * RUN_SEED_STEP xor stage.ordinal.toLong())
        battle = BattleRuntimeAdapter.start(
            seed = seed,
            launch = ProductBattleLaunch(
                stageId = stageId,
                loadoutIds = profile.state.loadoutIds.toList(),
                heroSkillIds = stage.heroSkillIds.filter(profile.state.selectedHeroSkillIds::contains),
                rosterLevels = profile.state.rosterLevels.toMap(),
                towerPowerPermille = bonuses.towerPowerPermille,
                allyPowerPermille = bonuses.allyPowerPermille,
                economyPermille = bonuses.economyPermille,
                heroPowerPermille = bonuses.heroPowerPermille,
            ),
        )
        profile.state.route = MySdRoute.BATTLE
        profile.state.serviceMessageCode = null
        return accepted(
            event("stage_started", stageId.value, -stage.energyCost.toLong()),
        )
    }

    private fun resumeRun(): MySdAppResult {
        val active = battle?.snapshot() ?: return rejected(MySdAppRejection.NO_ACTIVE_RUN)
        profile.state.route = MySdRoute.BATTLE
        profile.state.selectedStageId = active.stageId
        return accepted(event("run_resumed", active.runId))
    }

    private fun abandonRun(): MySdAppResult {
        val active = battle?.snapshot() ?: return rejected(MySdAppRejection.NO_ACTIVE_RUN)
        if (active.terminalResult != null && active.runId !in profile.state.claimedBattleRuns) {
            return rejected(MySdAppRejection.TERMINAL_RUN)
        }
        val runId = active.runId
        battle = null
        profile.state.route = MySdRoute.CAMPAIGN
        return accepted(event("run_abandoned", runId))
    }

    private inline fun withBattleRoute(
        block: (ProductBattleController, ProductBattleStateView) -> MySdAppResult,
    ): MySdAppResult {
        if (profile.state.route != MySdRoute.BATTLE) return rejected(MySdAppRejection.WRONG_ROUTE)
        val controller = battle ?: return rejected(MySdAppRejection.NO_ACTIVE_RUN)
        return block(controller, controller.snapshot())
    }

    private fun submitBattle(
        controller: ProductBattleController,
        before: ProductBattleStateView,
        action: ProductBattleAction,
        eventType: String,
        sourceId: String,
    ): MySdAppResult = when (val result = controller.submit(action)) {
        is ProductBattleActionResult.Rejected -> rejected(mapBattleRejection(result.reason))
        is ProductBattleActionResult.Accepted -> {
            val events = mutableListOf(event(eventType, sourceId))
            appendTerminalEvent(before, controller.snapshot(), events)
            accepted(*events.toTypedArray())
        }
    }

    private fun advanceBattle(ticks: Int): MySdAppResult {
        val controller = battle ?: return rejected(MySdAppRejection.NO_ACTIVE_RUN)
        val before = controller.snapshot()
        return when (val result = controller.step(ticks)) {
            ProductBattleStepResult.InvalidTickCount -> rejected(MySdAppRejection.INVALID_TICK_COUNT)
            is ProductBattleStepResult.Advanced -> {
                if (result.advancedTicks == 0) return unchanged()
                val events = mutableListOf<MySdAppEvent>()
                appendTerminalEvent(before, controller.snapshot(), events)
                accepted(*events.toTypedArray())
            }
        }
    }

    private fun appendTerminalEvent(
        before: ProductBattleStateView,
        after: ProductBattleStateView,
        events: MutableList<MySdAppEvent>,
    ) {
        if (before.terminalResult == null && after.terminalResult != null) {
            events += event(
                type = if (after.terminalResult == MySdTerminalResult.VICTORY) "battle_victory" else "battle_defeat",
                sourceId = after.runId,
            )
        }
    }

    private fun claimBattleReward(): MySdAppResult {
        if (profile.state.route != MySdRoute.BATTLE) return rejected(MySdAppRejection.WRONG_ROUTE)
        val state = battle?.snapshot() ?: return rejected(MySdAppRejection.NO_ACTIVE_RUN)
        val terminal = state.terminalResult ?: return rejected(MySdAppRejection.INVALID_PHASE)
        val stage = catalog.stages.getValue(state.stageId)
        // Only this facade may compact historical claim guards: it accepts no caller-supplied
        // run ID, and restore binds the one retained run to the latest profile ordinal/seed/stage.
        // An older run paired with this profile is rejected before a session exists. Replaying an
        // entire older offline bundle is outside this local idempotency guarantee.
        // Keep a current duplicate intact; a successful new claim installs its own marker in the
        // same synchronized mutation/save bundle. Restore the old markers on any typed rejection.
        val compactedClaims = if (state.runId !in profile.state.claimedBattleRuns &&
            profile.state.claimedBattleRuns.size >= ProductProfileManager.MAX_BATTLE_CLAIMS
        ) {
            profile.state.claimedBattleRuns.toSet().also { profile.state.claimedBattleRuns.clear() }
        } else {
            null
        }
        return when (
            val claimed = profile.claimBattleReward(
                runId = state.runId,
                stage = stage,
                result = terminal,
                baseHealth = state.baseHealth,
                baseMaxHealth = state.baseMaxHealth,
            )
        ) {
            is ProductProfileMutation.Rejected -> {
                compactedClaims?.let(profile.state.claimedBattleRuns::addAll)
                rejected(mapProfileRejection(claimed.reason))
            }
            is ProductProfileMutation.Applied -> accepted(
                *rewardEvents("battle_reward", state.runId, claimed.value).toTypedArray(),
            )
        }
    }

    private fun setLoadout(index: Int, rawContentId: String): MySdAppResult {
        if (profile.state.route != MySdRoute.ROSTER && profile.state.route != MySdRoute.STAGE_SETUP) {
            return rejected(MySdAppRejection.WRONG_ROUTE)
        }
        val contentId = contentIdOrReject(rawContentId) ?: return rejected(MySdAppRejection.UNKNOWN_CONTENT)
        return mutation(profile.replaceLoadout(index, contentId)) {
            listOf(event("loadout_changed", rawContentId, index.toLong()))
        }
    }

    private fun clearLoadout(index: Int): MySdAppResult {
        if (profile.state.route != MySdRoute.ROSTER && profile.state.route != MySdRoute.STAGE_SETUP) {
            return rejected(MySdAppRejection.WRONG_ROUTE)
        }
        val source = profile.state.loadoutIds.getOrNull(index)?.value ?: return rejected(MySdAppRejection.INVALID_TARGET)
        return mutation(profile.clearLoadout(index)) { listOf(event("loadout_cleared", source, index.toLong())) }
    }

    private fun upgradeRoster(rawContentId: String): MySdAppResult {
        if (profile.state.route != MySdRoute.ROSTER) return rejected(MySdAppRejection.WRONG_ROUTE)
        val contentId = contentIdOrReject(rawContentId) ?: return rejected(MySdAppRejection.UNKNOWN_CONTENT)
        val before = profile.state.rosterLevels[contentId] ?: return rejected(MySdAppRejection.UNKNOWN_CONTENT)
        val cost = ProductProfileManager.rosterUpgradeCost(before)
        return mutation(profile.upgradeRoster(contentId)) {
            listOf(event("roster_upgraded", rawContentId, -cost))
        }
    }

    private fun toggleHeroSkill(rawSkillId: String): MySdAppResult {
        if (profile.state.route != MySdRoute.ROSTER && profile.state.route != MySdRoute.STAGE_SETUP) {
            return rejected(MySdAppRejection.WRONG_ROUTE)
        }
        battle?.snapshot()?.let { active ->
            if (active.terminalResult == null || active.runId !in profile.state.claimedBattleRuns) {
                return rejected(MySdAppRejection.ACTIVE_RUN_EXISTS)
            }
        }
        val skillId = contentIdOrReject(rawSkillId) ?: return rejected(MySdAppRejection.UNKNOWN_CONTENT)
        return mutation(profile.toggleHeroSkill(skillId)) { selected ->
            listOf(event("hero_loadout_changed", rawSkillId, if (selected) 1L else 0L))
        }
    }

    private fun unlockTech(rawNodeId: String): MySdAppResult {
        if (profile.state.route != MySdRoute.TECHNOLOGY) return rejected(MySdAppRejection.WRONG_ROUTE)
        val nodeId = contentIdOrReject(rawNodeId) ?: return rejected(MySdAppRejection.UNKNOWN_CONTENT)
        val cost = catalog.techNodes[nodeId]?.cost ?: return rejected(MySdAppRejection.UNKNOWN_CONTENT)
        return mutation(profile.unlockTech(nodeId)) {
            listOf(event("technology_unlocked", rawNodeId, -cost))
        }
    }

    private fun buyShopOffer(rawOfferId: String): MySdAppResult {
        if (profile.state.route != MySdRoute.SHOP) return rejected(MySdAppRejection.WRONG_ROUTE)
        val offerId = contentIdOrReject(rawOfferId) ?: return rejected(MySdAppRejection.UNKNOWN_CONTENT)
        val offer = catalog.shopOffers[offerId] ?: return rejected(MySdAppRejection.UNKNOWN_CONTENT)
        return when (val result = profile.buyShopOffer(offerId)) {
            is ProductProfileMutation.Applied -> accepted(
                event("shop_offer_bought", rawOfferId, -(offer.softCost ?: 0L)),
            )
            is ProductProfileMutation.Rejected -> {
                if (result.reason == ProductProfileRejection.SERVICE_STUB) {
                    profile.state.serviceMessageCode = "service_stub:${offer.kind.name.lowercase()}"
                    changedRejected(MySdAppRejection.SERVICE_STUB, event("service_stub", rawOfferId))
                } else {
                    rejected(mapProfileRejection(result.reason))
                }
            }
        }
    }

    private fun claimRewardTier(rawTierId: String): MySdAppResult {
        if (profile.state.route != MySdRoute.REWARD_TRACK) return rejected(MySdAppRejection.WRONG_ROUTE)
        val tierId = contentIdOrReject(rawTierId) ?: return rejected(MySdAppRejection.UNKNOWN_CONTENT)
        val tier = catalog.rewardTiers.firstOrNull { it.id == tierId }
            ?: return rejected(MySdAppRejection.UNKNOWN_CONTENT)
        return mutation(profile.claimRewardTier(tierId)) {
            listOf(
                event("reward_tier_claimed", rawTierId, tier.softCurrency),
                event("premium_granted", rawTierId, tier.premiumShaped),
            )
        }
    }

    private fun sweepStage(rawStageId: String): MySdAppResult {
        if (profile.state.route != MySdRoute.CAMPAIGN && profile.state.route != MySdRoute.STAGE_SETUP) {
            return rejected(MySdAppRejection.WRONG_ROUTE)
        }
        val stageId = contentIdOrReject(rawStageId) ?: return rejected(MySdAppRejection.UNKNOWN_CONTENT)
        val stage = catalog.stages[stageId] ?: return rejected(MySdAppRejection.UNKNOWN_CONTENT)
        if (profile.state.route == MySdRoute.STAGE_SETUP && profile.state.selectedStageId != stageId) {
            return rejected(MySdAppRejection.INVALID_TARGET)
        }
        return when (val result = profile.sweep(stage)) {
            is ProductProfileMutation.Rejected -> rejected(mapProfileRejection(result.reason))
            is ProductProfileMutation.Applied -> accepted(
                *rewardEvents("stage_swept", rawStageId, result.value).toTypedArray(),
            )
        }
    }

    private fun toggleSetting(settingId: MySdSettingId): MySdAppResult {
        if (profile.state.route != MySdRoute.SETTINGS) return rejected(MySdAppRejection.WRONG_ROUTE)
        val enabled = profile.toggleSetting(settingId)
        return accepted(event("setting_toggled", settingId.name, if (enabled) 1L else 0L))
    }

    private fun refreshEnergy(nowEpochSeconds: Long): MySdAppResult =
        when (val refreshed = profile.refreshEnergy(nowEpochSeconds)) {
            is ProductProfileMutation.Rejected -> rejected(mapProfileRejection(refreshed.reason))
            is ProductProfileMutation.Applied -> if (refreshed.value == 0) unchanged() else accepted(
                event("energy_refreshed", "clock", refreshed.value.toLong()),
            )
        }

    private fun serviceStub(code: String): MySdAppResult {
        if (code.length > 160 || code.any(Char::isISOControl)) return rejected(MySdAppRejection.INVALID_TARGET)
        profile.rememberService(code)
        profile.state.serviceMessageCode = code
        return changedRejected(MySdAppRejection.SERVICE_STUB, event("service_stub", code))
    }

    private fun startArena(): MySdAppResult {
        if (profile.state.route != MySdRoute.ARENA) return rejected(MySdAppRejection.WRONG_ROUTE)
        val result = profile.runArena()
        return accepted(
            event(
                if (result.result == MySdTerminalResult.VICTORY) "arena_victory" else "arena_defeat",
                result.opponentFormationId,
            ),
        )
    }

    private fun closeOverlay(): MySdAppResult {
        if (profile.state.serviceMessageCode != null) {
            profile.state.serviceMessageCode = null
            return accepted(event("overlay_closed", "message"))
        }
        val state = battle?.snapshot()
        if (state != null && state.terminalResult != null) {
            if (state.runId !in profile.state.claimedBattleRuns) return rejected(MySdAppRejection.INVALID_PHASE)
            profile.state.route = MySdRoute.CAMPAIGN
            return accepted(event("overlay_closed", "terminal"))
        }
        if (profile.state.route == MySdRoute.HOME && state != null && state.terminalResult == null) {
            return rejected(MySdAppRejection.ACTIVE_RUN_EXISTS)
        }
        return rejected(MySdAppRejection.INVALID_PHASE)
    }

    private fun <T> mutation(
        mutation: ProductProfileMutation<T>,
        events: (T) -> List<MySdAppEvent>,
    ): MySdAppResult = when (mutation) {
        is ProductProfileMutation.Applied -> accepted(*events(mutation.value).toTypedArray())
        is ProductProfileMutation.Rejected -> rejected(mapProfileRejection(mutation.reason))
    }

    private fun rewardEvents(type: String, sourceId: String, reward: ProductBattleReward): List<MySdAppEvent> =
        buildList {
            add(event(type, sourceId, reward.soft))
            if (reward.premium != 0L) add(event("premium_granted", sourceId, reward.premium))
            if (reward.rewardTrackPoints != 0) {
                add(event("reward_track_progress", sourceId, reward.rewardTrackPoints.toLong()))
            }
            if (reward.stars != 0) add(event("stars_earned", sourceId, reward.stars.toLong()))
        }

    private fun accepted(vararg events: MySdAppEvent): MySdAppResult {
        profile.state.revision += 1
        return MySdAppResult(true, null, project(), events.toList())
    }

    private fun unchanged(): MySdAppResult = MySdAppResult(true, null, project())

    private fun rejected(rejection: MySdAppRejection): MySdAppResult =
        MySdAppResult(false, rejection, project())

    private fun changedRejected(rejection: MySdAppRejection, vararg events: MySdAppEvent): MySdAppResult {
        profile.state.revision += 1
        return MySdAppResult(false, rejection, project(), events.toList())
    }

    private fun event(type: String, sourceId: String, amount: Long = 0): MySdAppEvent = MySdAppEvent(
        id = profile.state.nextEventId++,
        type = type,
        sourceId = sourceId,
        amount = amount,
    )

    private fun hasMutationCapacity(): Boolean =
        profile.state.revision < ProductProfileManager.MAX_COUNTER.toLong() &&
            profile.state.nextEventId <= ProductProfileManager.MAX_COUNTER.toLong() - MAX_EVENTS_PER_MUTATION &&
            profile.state.nextRunOrdinal < ProductProfileManager.MAX_COUNTER.toLong() &&
            profile.state.nextLedgerId < ProductProfileManager.MAX_COUNTER.toLong()

    private fun project(): MySdAppSnapshot {
        val battleState = battle?.snapshot()
        return MySdAppSnapshot(
            revision = profile.state.revision,
            route = profile.state.route,
            profile = MySdProfileSnapshot(
                profileId = profile.state.profileId,
                currencies = MySdCurrencySnapshot(profile.state.softCurrency, profile.state.premiumShaped),
                energy = MySdEnergySnapshot(
                    current = profile.state.energy,
                    maximum = profile.state.maximumEnergy,
                    secondsUntilNext = profile.secondsUntilNextEnergy(),
                ),
                totalStars = profile.state.totalStars(),
                rewardTrackPoints = profile.state.rewardTrackPoints,
                loadoutIds = profile.state.loadoutIds.map { it.value },
                settings = profile.state.settings.toMap(),
            ),
            battle = battleState?.let(::projectBattle),
            overlay = projectOverlay(battleState),
            clock = MySdClockSnapshot(
                tick = battleState?.tick ?: 0,
                running = profile.state.route == MySdRoute.BATTLE && battleState != null &&
                    !battleState.paused && battleState.phase != MySdBattlePhase.ENHANCEMENT &&
                    battleState.terminalResult == null,
                speed = battleState?.speed ?: MySdBattleSpeed.ONE_X,
                stableHash = stableAppHash(battleState),
            ),
            surface = projectSurface(battleState),
        )
    }

    private fun projectBattle(state: ProductBattleStateView): MySdBattleSnapshot {
        val stage = catalog.stages.getValue(state.stageId)
        return MySdBattleSnapshot(
            runId = state.runId,
            stageId = state.stageId.value,
            phase = state.phase,
            paused = state.paused,
            speed = state.speed,
            waveNumber = state.waveIndex + 1,
            totalWaves = stage.waves.size,
            resource = state.resource,
            resourceCap = state.resourceCap,
            baseHealth = state.baseHealth,
            baseMaxHealth = state.baseMaxHealth,
            towerSlots = state.slots.map { slot ->
                val tower = slot.towerId?.let(catalog.towers::get)
                MySdTowerSlotSnapshot(
                    slotId = slot.id.value,
                    positionTicks = slot.positionTicks,
                    towerId = slot.towerId?.value,
                    level = slot.level,
                    cooldownRemainingTicks = slot.cooldownRemainingTicks,
                    nextUpgradeCost = tower?.upgradeCosts?.getOrNull(slot.level - 1),
                    maximumLevel = slot.level >= ProductBattleController.MAX_TOWER_LEVEL,
                )
            },
            allies = state.allies.map { ally ->
                MySdAllySnapshot(
                    entityId = ally.entityId,
                    allyId = ally.allyId.value,
                    health = max(0, ally.health),
                    maxHealth = ally.maxHealth,
                    positionTicks = ally.positionTicks,
                )
            },
            enemies = state.enemies.map { enemy ->
                MySdEnemySnapshot(
                    entityId = enemy.entityId,
                    enemyId = enemy.enemyId.value,
                    health = max(0, enemy.health),
                    maxHealth = enemy.maxHealth,
                    positionTicks = enemy.positionTicks,
                    boss = catalog.enemies.getValue(enemy.enemyId).boss,
                )
            },
            heroSkills = state.heroSkillIds.map { skillId ->
                val definition = catalog.heroSkills.getValue(skillId)
                val remaining = state.skillCooldowns.getValue(skillId)
                MySdHeroSkillSnapshot(
                    skillId = skillId.value,
                    cooldownRemainingTicks = remaining,
                    cooldownTicks = state.skillCooldownTotals[skillId] ?: definition.cooldownTicks,
                    available = remaining == 0 && state.phase == MySdBattlePhase.COMBAT && !state.paused,
                )
            },
            enhancementIds = state.enhancements.map { it.value },
            terminalResult = state.terminalResult,
            pathLengthTicks = stage.basePositionTicks,
        )
    }

    private fun projectOverlay(state: ProductBattleStateView?): MySdOverlaySnapshot? {
        profile.state.serviceMessageCode?.let { return MySdOverlaySnapshot.Message(it, serviceStub = true) }
        if (state != null && profile.state.route == MySdRoute.BATTLE) {
            if (state.phase == MySdBattlePhase.ENHANCEMENT) {
                return MySdOverlaySnapshot.EnhancementChoice(
                    offerIds = state.enhancementOffers.map { it.value },
                    rerollsRemaining = state.rerollsRemaining,
                )
            }
            state.terminalResult?.let { result ->
                val reward = previewBattleReward(state, result)
                return MySdOverlaySnapshot.TerminalReward(
                    result = result,
                    stars = reward.stars,
                    softReward = reward.soft,
                    rewardTrackPoints = reward.rewardTrackPoints,
                    claimed = state.runId in profile.state.claimedBattleRuns,
                )
            }
        }
        if (state != null && state.terminalResult == null && profile.state.route == MySdRoute.HOME) {
            return MySdOverlaySnapshot.ResumeRun(state.stageId.value)
        }
        return null
    }

    private fun previewBattleReward(
        state: ProductBattleStateView,
        result: MySdTerminalResult,
    ): ProductBattleReward {
        val stage = catalog.stages.getValue(state.stageId)
        if (result == MySdTerminalResult.DEFEAT) {
            return ProductBattleReward(0, max(5L, stage.reward.softCurrency / 10), 0, 0)
        }
        val ratio = if (state.baseMaxHealth == 0) 0 else state.baseHealth * 1_000 / state.baseMaxHealth
        val stars = 1 + (if (ratio >= 500) 1 else 0) + (if (ratio >= 800) 1 else 0)
        val firstClear = (profile.state.stageProgress[state.stageId]?.clearCount ?: 0) == 0
        return ProductBattleReward(
            stars,
            stage.reward.softCurrency,
            if (firstClear) stage.reward.firstClearPremiumShaped else 0,
            stage.reward.rewardTrackPoints,
        )
    }

    private fun projectSurface(state: ProductBattleStateView?): MySdSurfaceSnapshot = when (profile.state.route) {
        MySdRoute.HOME -> MySdSurfaceSnapshot.Home
        MySdRoute.CAMPAIGN -> MySdSurfaceSnapshot.CampaignMap(catalog.orderedStages.map(::stageCard))
        MySdRoute.STAGE_SETUP -> {
            val selected = profile.state.selectedStageId ?: catalog.orderedStages.first().id
            MySdSurfaceSnapshot.StageSetup(
                stage = stageCard(catalog.stages.getValue(selected)),
                selectedLoadoutIds = profile.state.loadoutIds.map { it.value },
                availableLoadoutIds = profile.state.unlockedRoster
                    .filter { it in catalog.towers || it in catalog.allies }
                    .sortedBy(ContentId::value)
                    .map { it.value },
                canStart = selected in profile.state.unlockedStages &&
                    profile.state.energy >= catalog.stages.getValue(selected).energyCost &&
                    (state == null || state.terminalResult != null && state.runId in profile.state.claimedBattleRuns),
                selectedHeroSkillIds = profile.state.selectedHeroSkillIds.map { it.value },
                availableHeroSkillIds = catalog.heroSkills.keys.filter(profile.state.unlockedRoster::contains)
                    .sortedBy(ContentId::value).map { it.value },
            )
        }
        MySdRoute.BATTLE -> {
            val active = state
            val loadout = active?.loadoutIds ?: profile.state.loadoutIds
            val towerIds = loadout.filter(catalog.towers::containsKey)
            val allyIds = loadout.filter(catalog.allies::containsKey)
            MySdSurfaceSnapshot.Battle(
                towerCardIds = towerIds.map { it.value },
                allyCardIds = allyIds.map { it.value },
                heroSkillIds = active?.heroSkillIds?.map { it.value } ?: emptyList(),
                towerBuildCosts = towerIds.associate { it.value to catalog.towers.getValue(it).buildCost },
                allyDeployCosts = allyIds.associate { it.value to catalog.allies.getValue(it).deployCost },
            )
        }
        MySdRoute.ROSTER -> MySdSurfaceSnapshot.Roster(
            entries = profile.state.rosterLevels.toSortedMap(compareBy(ContentId::value)).map { (id, level) ->
                MySdRosterEntrySnapshot(
                    contentId = id.value,
                    category = when (id) {
                        in catalog.towers -> "tower"
                        in catalog.allies -> "ally"
                        else -> "hero"
                    },
                    level = level,
                    maximumLevel = ProductProfileManager.MAX_ROSTER_LEVEL,
                    unlocked = id in profile.state.unlockedRoster,
                    upgradeCost = if (id !in profile.state.unlockedRoster || level >= ProductProfileManager.MAX_ROSTER_LEVEL) null
                    else ProductProfileManager.rosterUpgradeCost(level),
                    unlockAfterStageOrdinal = catalog.towers[id]?.unlockAfterStageOrdinal
                        ?: catalog.allies[id]?.unlockAfterStageOrdinal
                        ?: catalog.heroSkills.getValue(id).unlockAfterStageOrdinal,
                )
            },
            selectedLoadoutIds = profile.state.loadoutIds.map { it.value },
            selectedHeroSkillIds = profile.state.selectedHeroSkillIds.map { it.value },
        )
        MySdRoute.TECHNOLOGY -> MySdSurfaceSnapshot.Technology(
            nodes = catalog.techNodes.values.sortedWith(compareBy({ it.branchId.value }, { it.id.value })).map { node ->
                MySdTechNodeSnapshot(
                    nodeId = node.id.value,
                    branchId = node.branchId.value,
                    unlocked = node.id in profile.state.unlockedTech,
                    prerequisitesMet = profile.state.unlockedTech.containsAll(node.prerequisites),
                    cost = node.cost,
                )
            },
        )
        MySdRoute.SHOP -> MySdSurfaceSnapshot.Shop(
            offers = catalog.shopOffers.values.sortedBy { it.id.value }.map { offer ->
                val stub = offer.kind == ProductShopOfferKind.REWARDED_STUB ||
                    offer.kind == ProductShopOfferKind.PURCHASE_STUB
                MySdShopOfferSnapshot(
                    offerId = offer.id.value,
                    kind = offer.kind.name,
                    cost = offer.softCost,
                    affordable = stub || (
                        (offer.softCost ?: 0) <= profile.state.softCurrency &&
                            (offer.energyReward == 0 || profile.state.energy < profile.state.maximumEnergy) &&
                            (offer.repeatable || profile.state.shopPurchases.getOrDefault(offer.id, 0) == 0)
                        ),
                    stubOnly = stub,
                    softReward = offer.softReward,
                    energyReward = offer.energyReward,
                    repeatable = offer.repeatable,
                    purchased = profile.state.shopPurchases.getOrDefault(offer.id, 0),
                )
            },
        )
        MySdRoute.REWARD_TRACK -> MySdSurfaceSnapshot.RewardTrack(
            tiers = catalog.rewardTiers.map { tier ->
                MySdRewardTierSnapshot(
                    tierId = tier.id.value,
                    requiredPoints = tier.requiredPoints,
                    unlocked = profile.state.rewardTrackPoints >= tier.requiredPoints,
                    claimed = tier.id in profile.state.claimedRewardTiers,
                    softReward = tier.softCurrency,
                    premiumShapedReward = tier.premiumShaped,
                    energyReward = tier.energy,
                )
            },
        )
        MySdRoute.SETTINGS -> MySdSurfaceSnapshot.Settings(profile.state.settings.toMap())
        MySdRoute.ARENA -> MySdSurfaceSnapshot.Arena(
            MySdArenaSnapshot(
                opponentFormationId = profile.state.arena.opponentFormationId,
                playerPower = if (profile.state.arena.playerPower == 0) profile.calculatePlayerPower()
                else profile.state.arena.playerPower,
                opponentPower = profile.state.arena.opponentPower,
                result = profile.state.arena.result,
                networkUsed = false,
            ),
        )
    }

    private fun stageCard(stage: dev.mysd.game.product.content.ProductStageDefinition): MySdStageCardSnapshot {
        val progress = profile.state.stageProgress[stage.id]
        return MySdStageCardSnapshot(
            stageId = stage.id.value,
            regionId = stage.regionId.value,
            ordinal = stage.ordinal,
            unlocked = stage.id in profile.state.unlockedStages,
            energyCost = stage.energyCost,
            bestStars = progress?.bestStars ?: 0,
            clearCount = progress?.clearCount ?: 0,
            sweepAvailable = (progress?.bestStars ?: 0) >= 3,
        )
    }

    private fun stableAppHash(state: ProductBattleStateView?): String {
        var hash = FNV_OFFSET
        fun add(text: String) {
            text.encodeToByteArray().forEach { byte ->
                hash = hash xor (byte.toInt() and 0xff).toLong()
                hash *= FNV_PRIME
            }
            hash = hash xor 0
            hash *= FNV_PRIME
        }
        add(profile.state.profileId)
        add(profile.state.sessionSeed.toString())
        add(profile.state.revision.toString())
        add(profile.state.nextEventId.toString())
        add(profile.state.nextRunOrdinal.toString())
        add(profile.state.route.name)
        add(profile.state.selectedStageId?.value ?: "")
        add(profile.state.serviceMessageCode ?: "")
        add(profile.state.softCurrency.toString())
        add(profile.state.premiumShaped.toString())
        add(profile.state.energy.toString())
        add(profile.state.maximumEnergy.toString())
        add(profile.state.rewardTrackPoints.toString())
        profile.state.unlockedStages.sortedBy(ContentId::value).forEach { add(it.value) }
        profile.state.stageProgress.toSortedMap(compareBy(ContentId::value)).forEach { (id, progress) ->
            add(id.value)
            add(progress.bestStars.toString())
            add(progress.clearCount.toString())
        }
        profile.state.rosterLevels.toSortedMap(compareBy(ContentId::value)).forEach { (id, level) ->
            add(id.value)
            add(level.toString())
        }
        profile.state.unlockedRoster.sortedBy(ContentId::value).forEach { add(it.value) }
        profile.state.loadoutIds.forEach { add(it.value) }
        profile.state.unlockedTech.sortedBy(ContentId::value).forEach { add(it.value) }
        profile.state.claimedRewardTiers.sortedBy(ContentId::value).forEach { add(it.value) }
        profile.state.claimedBattleRuns.sorted().forEach(::add)
        profile.state.shopPurchases.toSortedMap(compareBy(ContentId::value)).forEach { (id, count) ->
            add(id.value)
            add(count.toString())
        }
        MySdSettingId.entries.forEach { setting ->
            add(setting.name)
            add(profile.state.settings.getValue(setting).toString())
        }
        profile.state.localServiceHistory.forEach(::add)
        profile.state.ledger.sortedBy { it.id }.forEach { entry ->
            add(entry.id.toString())
            add(entry.kind.name)
            add(entry.sourceId)
            add(entry.softDelta.toString())
            add(entry.premiumDelta.toString())
            add(entry.energyDelta.toString())
            add(entry.rewardTrackDelta.toString())
        }
        add(profile.state.nextLedgerId.toString())
        add(profile.state.arena.opponentFormationId)
        add(profile.state.arena.playerPower.toString())
        add(profile.state.arena.opponentPower.toString())
        add(profile.state.arena.result?.name ?: "")
        add(profile.state.arena.runCount.toString())
        // Existing auto-equipped profiles/replays retain their hash. Explicit subsets, including
        // none, add a disjoint coordinate without changing the battle hash or run schema.
        val legacyHeroSelection = catalog.heroSkills.keys.filter(profile.state.unlockedRoster::contains)
            .sortedBy(ContentId::value)
        if (profile.state.selectedHeroSkillIds != legacyHeroSelection) {
            add("selected-hero-skills")
            add(profile.state.selectedHeroSkillIds.size.toString())
            profile.state.selectedHeroSkillIds.forEach { add(it.value) }
        }
        add(state?.let { battle?.stableHash() } ?: "no-battle")
        return hash.toULong().toString(16).padStart(16, '0')
    }

    private fun contentIdOrReject(raw: String): ContentId? = try {
        ContentId.of(raw)
    } catch (_: IllegalArgumentException) {
        null
    }

    private fun mapBattleRejection(reason: ProductBattleRejection): MySdAppRejection = when (reason) {
        ProductBattleRejection.UNKNOWN_CONTENT -> MySdAppRejection.UNKNOWN_CONTENT
        ProductBattleRejection.INVALID_TARGET -> MySdAppRejection.INVALID_TARGET
        ProductBattleRejection.INVALID_PHASE -> MySdAppRejection.INVALID_PHASE
        ProductBattleRejection.INSUFFICIENT_RESOURCE -> MySdAppRejection.INSUFFICIENT_CURRENCY
        ProductBattleRejection.MAXIMUM_LEVEL -> MySdAppRejection.MAXIMUM_LEVEL
        ProductBattleRejection.TERMINAL -> MySdAppRejection.TERMINAL_RUN
    }

    private fun mapProfileRejection(reason: ProductProfileRejection): MySdAppRejection = when (reason) {
        ProductProfileRejection.UNKNOWN_CONTENT -> MySdAppRejection.UNKNOWN_CONTENT
        ProductProfileRejection.LOCKED -> MySdAppRejection.LOCKED
        ProductProfileRejection.INSUFFICIENT_ENERGY -> MySdAppRejection.INSUFFICIENT_ENERGY
        ProductProfileRejection.INSUFFICIENT_CURRENCY -> MySdAppRejection.INSUFFICIENT_CURRENCY
        ProductProfileRejection.INVALID_TARGET -> MySdAppRejection.INVALID_TARGET
        ProductProfileRejection.MAXIMUM_LEVEL -> MySdAppRejection.MAXIMUM_LEVEL
        ProductProfileRejection.PREREQUISITE_MISSING -> MySdAppRejection.PREREQUISITE_MISSING
        ProductProfileRejection.ALREADY_CLAIMED -> MySdAppRejection.ALREADY_CLAIMED
        ProductProfileRejection.SWEEP_NOT_AVAILABLE -> MySdAppRejection.SWEEP_NOT_AVAILABLE
        ProductProfileRejection.SERVICE_STUB -> MySdAppRejection.SERVICE_STUB
    }

    companion object {
        private const val MAX_TICKS_PER_STEP: Int = 100_000
        private const val MAX_EVENTS_PER_MUTATION: Int = 4
        private const val RUN_SEED_STEP: Long = -7046029254386353131L
        private const val FNV_OFFSET: Long = -3750763034362895579L
        private const val FNV_PRIME: Long = 1099511628211L

        fun create(seed: Long): MySdAppSession {
            val catalog = OriginalProductCatalog.releaseOne()
            return DefaultMySdAppSession(catalog, ProductProfileManager.create(seed, catalog), battle = null)
        }

        fun restore(bundle: MySdSaveBundle): MySdRestoreResult {
            val catalog = OriginalProductCatalog.releaseOne()
            val profile = when (val decoded = ProductProfileCodec.decode(bundle.profileSave, catalog)) {
                is ProductProfileDecodeResult.Decoded -> ProductProfileManager(catalog, decoded.profile)
                is ProductProfileDecodeResult.Incompatible -> return MySdRestoreResult.Incompatible(
                    MySdRestoreIncompatibility.PROFILE_SCHEMA,
                    decoded.expected,
                    decoded.actual,
                )
                is ProductProfileDecodeResult.Invalid -> return MySdRestoreResult.Invalid(decoded.reason)
            }
            val battle = when (val runSave = bundle.runSave) {
                null -> null
                else -> when (val envelope = ProductRunSaveCodec.decode(runSave)) {
                    is ProductRunDecodeResult.Decoded -> when (val restored = BattleRuntimeAdapter.restore(envelope.save)) {
                        is ProductBattleRestoreResult.Restored -> restored.controller
                        is ProductBattleRestoreResult.Incompatible -> return MySdRestoreResult.Incompatible(
                            kind = when (restored.kind) {
                                ProductBattleIncompatibility.RUNTIME -> MySdRestoreIncompatibility.RUNTIME
                                ProductBattleIncompatibility.CONTENT -> MySdRestoreIncompatibility.CONTENT
                                ProductBattleIncompatibility.RUN_SCHEMA -> MySdRestoreIncompatibility.RUN_SCHEMA
                            },
                            expected = restored.expected,
                            actual = restored.actual,
                        )
                        is ProductBattleRestoreResult.Invalid -> return MySdRestoreResult.Invalid(restored.reason)
                    }
                    is ProductRunDecodeResult.Incompatible -> return MySdRestoreResult.Incompatible(
                        MySdRestoreIncompatibility.RUN_SCHEMA,
                        envelope.expected,
                        envelope.actual,
                    )
                    is ProductRunDecodeResult.Invalid -> return MySdRestoreResult.Invalid(envelope.reason)
                }
            }
            val selectedFallback = catalog.orderedStages.firstOrNull { it.id in profile.state.unlockedStages }?.id
                ?: catalog.orderedStages.first().id
            val restoredSelection = profile.state.selectedStageId
            if (restoredSelection == null || restoredSelection !in profile.state.unlockedStages) {
                profile.state.selectedStageId = selectedFallback
            }
            if (battle == null && profile.state.route == MySdRoute.BATTLE) profile.state.route = MySdRoute.HOME
            battle?.snapshot()?.let { state ->
                val stage = catalog.stages.getValue(state.stageId)
                val expectedSeed = mix(profile.state.sessionSeed xor
                    (profile.state.nextRunOrdinal - 1) * RUN_SEED_STEP xor stage.ordinal.toLong())
                if (profile.state.nextRunOrdinal < 2 ||
                    state.runId != "run-${expectedSeed.toULong().toString(16)}-${stage.ordinal}"
                ) return MySdRestoreResult.Invalid("Saved run identity does not belong to this profile transaction.")
                if (state.stageId !in profile.state.unlockedStages) {
                    return MySdRestoreResult.Invalid("Saved run stage is locked in the restored profile.")
                }
                if (!profile.state.unlockedRoster.containsAll(state.loadoutIds) ||
                    !profile.state.unlockedRoster.containsAll(state.heroSkillIds)
                ) return MySdRestoreResult.Invalid("Saved run uses locked profile content.")
                val claimed = state.runId in profile.state.claimedBattleRuns
                if (state.terminalResult == null && claimed) {
                    return MySdRestoreResult.Invalid("A non-terminal run cannot have a claimed reward.")
                }
                if (!claimed) {
                    val bonuses = profile.techBonuses()
                    if (state.loadoutIds != profile.state.loadoutIds ||
                        state.heroSkillIds != stage.heroSkillIds.filter(profile.state.selectedHeroSkillIds::contains) ||
                        state.rosterLevels != profile.state.rosterLevels ||
                        state.towerPowerPermille != bonuses.towerPowerPermille ||
                        state.allyPowerPermille != bonuses.allyPowerPermille ||
                        state.economyPermille != bonuses.economyPermille ||
                        state.heroPowerPermille != bonuses.heroPowerPermille
                    ) return MySdRestoreResult.Invalid("Saved run and profile progression coordinates diverge.")
                }
                if (state.terminalResult != null && !claimed) {
                    profile.state.route = MySdRoute.BATTLE
                } else if (state.terminalResult == null &&
                    profile.state.route != MySdRoute.BATTLE && profile.state.route != MySdRoute.HOME
                ) {
                    profile.state.route = MySdRoute.HOME
                }
            }
            return MySdRestoreResult.Restored(DefaultMySdAppSession(catalog, profile, battle))
        }

        private fun mix(input: Long): Long {
            var value = input
            value = (value xor (value ushr 30)) * -4658895280553007687L
            value = (value xor (value ushr 27)) * -7723592293110705685L
            return value xor (value ushr 31)
        }
    }
}
