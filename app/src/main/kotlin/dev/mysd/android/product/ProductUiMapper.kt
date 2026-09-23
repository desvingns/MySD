package dev.mysd.android.product

import dev.mysd.game.product.MySdAppSnapshot
import dev.mysd.game.product.MySdArenaSnapshot
import dev.mysd.game.product.MySdBattlePhase
import dev.mysd.game.product.MySdBattleSnapshot
import dev.mysd.game.product.MySdOverlaySnapshot
import dev.mysd.game.product.MySdRoute
import dev.mysd.game.product.MySdSettingId
import dev.mysd.game.product.MySdStageCardSnapshot
import dev.mysd.game.product.MySdSurfaceSnapshot
import dev.mysd.game.product.MySdTerminalResult
import kotlin.math.absoluteValue

internal fun MySdAppSnapshot.toProductUiModel(): ProductUiModel {
    val rewardTarget = (profile.rewardTrackPoints / REWARD_TIER_STEP + 1) * REWARD_TIER_STEP
    return ProductUiModel(
        shell = ProductShellUi(
            energy = profile.energy.current,
            energyCap = profile.energy.maximum,
            credits = profile.currencies.soft,
            crystals = profile.currencies.premiumShaped,
            rewardTrackExperience = profile.rewardTrackPoints,
            rewardTrackTarget = rewardTarget.coerceAtLeast(REWARD_TIER_STEP),
            selectedDestination = route.destination(),
            showMetaNavigation = route in META_ROUTES,
        ),
        surface = surface.toProductSurface(this),
        overlay = overlay.toProductOverlay(),
        reduceMotion = profile.settings[MySdSettingId.REDUCE_MOTION] == true,
    )
}

private fun MySdSurfaceSnapshot.toProductSurface(app: MySdAppSnapshot): ProductSurfaceUi = when (this) {
    MySdSurfaceSnapshot.Home -> ProductSurfaceUi.Launch()

    is MySdSurfaceSnapshot.CampaignMap -> ProductSurfaceUi.CampaignMap(
        stages = stages.map(MySdStageCardSnapshot::toUi),
    )

    is MySdSurfaceSnapshot.StageSetup -> ProductSurfaceUi.Setup(
        stageId = stage.stageId,
        title = stageTitle(stage),
        threatLabel = regionTitle(stage.regionId),
        waveCount = DEFAULT_VISIBLE_WAVES,
        energyCost = stage.energyCost,
        energyAvailable = app.profile.energy.current >= stage.energyCost,
        sweepAvailable = stage.sweepAvailable,
        heroName = DEFAULT_HERO_TITLE,
        loadout = (0 until loadoutVisibleSize(selectedLoadoutIds.size)).map { index ->
            val id = selectedLoadoutIds.getOrNull(index)
            LoadoutSlotUi(
                index = index,
                contentId = id,
                label = id?.let(::productTitle),
                kind = id?.toLoadoutKind(),
            )
        },
        availableLoadout = availableLoadoutIds.map { id ->
            SetupChoiceUi(
                id = id,
                title = productTitle(id),
                description = productDescription(id),
                kind = id.toLoadoutKind(),
            )
        },
        canStart = canStart,
        heroChoices = availableHeroSkillIds.map { id ->
            SetupHeroChoiceUi(
                id = id,
                title = productTitle(id),
                description = productDescription(id),
                selected = id in selectedHeroSkillIds,
            )
        },
    )

    is MySdSurfaceSnapshot.Battle -> ProductSurfaceUi.Battle(
        scene = requireNotNull(app.battle) { "Battle route requires a battle snapshot." }
            .toBattleScene(
                towerCardIds = towerCardIds,
                allyCardIds = allyCardIds,
                heroSkillIds = heroSkillIds,
                towerBuildCosts = towerBuildCosts,
                allyDeployCosts = allyDeployCosts,
                overlay = app.overlay,
            ),
    )

    is MySdSurfaceSnapshot.Roster -> ProductSurfaceUi.Roster(
        entries = entries.map { entry ->
            RosterEntryUi(
                id = entry.contentId,
                title = productTitle(entry.contentId),
                role = productTitle(entry.category),
                level = entry.level,
                power = 0,
                upgradeCost = entry.upgradeCost ?: 0L,
                affordable = entry.upgradeCost?.let { app.profile.currencies.soft >= it } == true,
                unlocked = entry.unlocked,
                unlockStage = entry.unlockAfterStageOrdinal.takeIf { it > 0 }?.toString(),
                category = entry.category.toRosterCategory(),
                maximumLevel = entry.level >= entry.maximumLevel || entry.upgradeCost == null,
            )
        },
        equippedIds = (selectedLoadoutIds + selectedHeroSkillIds).toSet(),
        maxEquipped = LOADOUT_SIZE,
    )

    is MySdSurfaceSnapshot.Technology -> ProductSurfaceUi.Tech(
        nodes = nodes.mapIndexed { index, node ->
            TechNodeUi(
                id = node.nodeId,
                title = productTitle(node.nodeId),
                description = productDescription(node.branchId),
                status = when {
                    node.unlocked -> TechNodeStatusUi.UNLOCKED
                    node.prerequisitesMet -> TechNodeStatusUi.AVAILABLE
                    else -> TechNodeStatusUi.LOCKED
                },
                cost = node.cost,
                prerequisiteLabel = productTitle(node.branchId),
                affordable = app.profile.currencies.soft >= node.cost,
                column = index % TECH_COLUMNS,
                row = index / TECH_COLUMNS,
            )
        },
    )

    is MySdSurfaceSnapshot.Shop -> ProductSurfaceUi.Shop(
        products = offers.map { offer ->
            ShopProductUi(
                id = offer.offerId,
                title = productTitle(offer.offerId),
                description = productDescription(offer.offerId),
                kind = when {
                    !offer.stubOnly -> ShopProductKindUi.SOFT_CURRENCY
                    offer.kind.contains("reward", ignoreCase = true) ||
                        offer.kind.contains("ad", ignoreCase = true) -> {
                        ShopProductKindUi.REWARDED_STUB
                    }
                    else -> ShopProductKindUi.PURCHASE_STUB
                },
                cost = offer.cost ?: 0L,
                affordable = offer.affordable,
                owned = !offer.repeatable && offer.purchased > 0,
                softReward = offer.softReward,
                energyReward = offer.energyReward,
                purchased = offer.purchased,
            )
        },
    )

    is MySdSurfaceSnapshot.RewardTrack -> ProductSurfaceUi.RewardTrack(
        tiers = tiers.mapIndexed { index, tier ->
            RewardTierUi(
                id = tier.tierId,
                tier = index + 1,
                requiredExperience = tier.requiredPoints,
                rewardLabel = productTitle(tier.tierId),
                status = when {
                    tier.claimed -> RewardTierStatusUi.CLAIMED
                    tier.unlocked -> RewardTierStatusUi.AVAILABLE
                    else -> RewardTierStatusUi.LOCKED
                },
                softReward = tier.softReward,
                crystalReward = tier.premiumShapedReward,
                energyReward = tier.energyReward,
            )
        },
        experience = app.profile.rewardTrackPoints,
    )

    is MySdSurfaceSnapshot.Settings -> ProductSurfaceUi.Settings(
        values = values.mapKeys { (id, _) -> id.toUi() },
    )

    is MySdSurfaceSnapshot.Arena -> ProductSurfaceUi.Arena(
        state = exhibition.toUi(),
    )
}

private fun MySdBattleSnapshot.toBattleScene(
    towerCardIds: List<String>,
    allyCardIds: List<String>,
    heroSkillIds: List<String>,
    towerBuildCosts: Map<String, Int>,
    allyDeployCosts: Map<String, Int>,
    overlay: MySdOverlaySnapshot?,
): BattleSceneUi {
    val extent = pathLengthTicks.coerceAtLeast(1)
    val terminalOverlay = overlay as? MySdOverlaySnapshot.TerminalReward
    val enhancementOverlay = overlay as? MySdOverlaySnapshot.EnhancementChoice
    return BattleSceneUi(
        runId = runId,
        stageTitle = productTitle(stageId),
        phase = if (paused) {
            BattlePhaseUi.PAUSED
        } else {
            when (phase) {
                MySdBattlePhase.PREPARATION -> BattlePhaseUi.WAVE_INTRO
                MySdBattlePhase.COMBAT -> BattlePhaseUi.ACTIVE
                MySdBattlePhase.ENHANCEMENT -> BattlePhaseUi.INTER_WAVE_CHOICE
                MySdBattlePhase.VICTORY -> BattlePhaseUi.VICTORY
                MySdBattlePhase.DEFEAT -> BattlePhaseUi.DEFEAT
            }
        },
        wave = waveNumber,
        totalWaves = totalWaves,
        resource = resource,
        resourceCap = resourceCap,
        speedMultiplier = speed.ticksPerPulse,
        baseHealth = baseHealth,
        baseMaxHealth = baseMaxHealth,
        slots = towerSlots.mapIndexed { index, slot ->
            val (x, y) = pathPosition(slot.positionTicks, extent)
            BattleSlotUi(
                id = slot.slotId,
                index = index,
                xFraction = x,
                yFraction = y,
                towerName = slot.towerId?.let(::productTitle),
                level = slot.level,
                cost = slot.nextUpgradeCost ?: 0,
                canAfford = slot.nextUpgradeCost?.let { resource >= it } ?: true,
                maxLevel = slot.maximumLevel,
            )
        },
        enemies = enemies.map { enemy ->
            val (x, y) = pathPosition(enemy.positionTicks, extent)
            BattleEntityUi(
                id = enemy.entityId.toString(),
                label = productTitle(enemy.enemyId),
                xFraction = x,
                yFraction = y,
                health = enemy.health,
                maxHealth = enemy.maxHealth,
                role = enemyRole(enemy.enemyId, enemy.boss),
            )
        },
        allies = allies.map { ally ->
            val (x, y) = pathPosition(ally.positionTicks, extent)
            BattleEntityUi(
                id = ally.entityId.toString(),
                label = productTitle(ally.allyId),
                xFraction = x,
                yFraction = y,
                health = ally.health,
                maxHealth = ally.maxHealth,
                role = allyRole(ally.allyId),
            )
        },
        abilities = heroSkills.map { skill ->
            HeroAbilityUi(
                id = skill.skillId,
                title = productTitle(skill.skillId),
                ready = skill.available,
                cooldownSeconds = ticksToSeconds(skill.cooldownRemainingTicks),
            )
        }.ifEmpty {
            heroSkillIds.map { id ->
                HeroAbilityUi(id, productTitle(id), ready = false, cooldownSeconds = 0)
            }
        },
        availableTowerIds = towerCardIds,
        availableAllyIds = allyCardIds,
        towerBuildCosts = towerBuildCosts.toMap(),
        allyDeployCosts = allyDeployCosts.toMap(),
        enhancementChoices = (enhancementOverlay?.offerIds ?: enhancementIds).mapIndexed { index, id ->
            EnhancementChoiceUi(
                id = id,
                title = productTitle(id),
                description = productDescription(id),
                tier = index + 1,
            )
        },
        enhancementRerollsRemaining = enhancementOverlay?.rerollsRemaining ?: 0,
        result = terminalOverlay?.let { terminal ->
            BattleResultUi(
                victory = terminal.result == MySdTerminalResult.VICTORY,
                completedWaves = waveNumber,
                defeatedEnemies = 0,
                credits = terminal.softReward.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
                experience = terminal.rewardTrackPoints,
                claimed = terminal.claimed,
                multiplierAvailable = !terminal.claimed && terminal.result == MySdTerminalResult.VICTORY,
            )
        },
    )
}

private fun MySdOverlaySnapshot?.toProductOverlay(): ProductOverlayUi? = when (this) {
    null,
    is MySdOverlaySnapshot.EnhancementChoice,
    is MySdOverlaySnapshot.TerminalReward,
    -> null

    is MySdOverlaySnapshot.ResumeRun -> ProductOverlayUi.ResumeRun(
        stageTitle = productTitle(stageId),
    )

    is MySdOverlaySnapshot.Message -> ProductOverlayUi.ServiceResult(
        kind = when {
            code.contains("purchase", ignoreCase = true) -> ServiceResultKindUi.PURCHASE
            code.contains("arena", ignoreCase = true) -> ServiceResultKindUi.ARENA
            else -> ServiceResultKindUi.REWARDED
        },
        title = productTitle(code),
    )
}

private fun MySdStageCardSnapshot.toUi(): CampaignStageUi = CampaignStageUi(
    id = stageId,
    title = stageTitle(this),
    subtitle = regionTitle(regionId),
    status = when {
        !unlocked -> StageStatusUi.LOCKED
        clearCount > 0 -> StageStatusUi.COMPLETED
        else -> StageStatusUi.AVAILABLE
    },
    stars = bestStars,
    energyCost = energyCost,
    waveCount = DEFAULT_VISIBLE_WAVES,
)

private fun MySdArenaSnapshot.toUi(): ArenaUi = when {
    opponentFormationId == UNSCOUTED_FORMATION_ID && result == null -> ArenaUi.Lobby(
        rating = playerPower,
        tickets = 1,
        recentResults = emptyList(),
    )
    result == null -> ArenaUi.OpponentPreview(
        rating = playerPower,
        tickets = 1,
        opponentName = productTitle(opponentFormationId),
        opponentPower = opponentPower,
        seedLabel = arenaScenarioLabel(opponentFormationId),
    )
    else -> ArenaUi.Result(
        rating = playerPower,
        tickets = 1,
        opponentName = productTitle(opponentFormationId),
        victory = result == MySdTerminalResult.VICTORY,
        ratingDelta = 0,
    )
}

private fun arenaScenarioLabel(formationId: String): String {
    val marker = (formationId.hashCode().toLong().absoluteValue % 997L) + 1L
    return "Локальный сценарий №$marker"
}

private fun MySdSettingId.toUi(): ProductSettingUi = when (this) {
    MySdSettingId.SOUND -> ProductSettingUi.SOUND
    MySdSettingId.MUSIC -> ProductSettingUi.MUSIC
    MySdSettingId.HAPTICS -> ProductSettingUi.HAPTICS
    MySdSettingId.REDUCE_MOTION -> ProductSettingUi.REDUCED_MOTION
}

private fun MySdRoute.destination(): ProductDestinationUi? = when (this) {
    MySdRoute.CAMPAIGN,
    MySdRoute.STAGE_SETUP,
    MySdRoute.BATTLE,
    -> ProductDestinationUi.CAMPAIGN

    MySdRoute.ROSTER -> ProductDestinationUi.ROSTER
    MySdRoute.TECHNOLOGY -> ProductDestinationUi.TECH
    MySdRoute.SHOP -> ProductDestinationUi.SHOP
    MySdRoute.ARENA -> ProductDestinationUi.ARENA
    MySdRoute.HOME,
    MySdRoute.REWARD_TRACK,
    MySdRoute.SETTINGS,
    -> null
}

private fun stageTitle(stage: MySdStageCardSnapshot): String = productTitle(stage.stageId)

private fun regionTitle(regionId: String): String = productTitle(regionId)

private fun pathPosition(positionTicks: Int, extent: Int): Pair<Float, Float> {
    val t = (positionTicks.toFloat() / extent.coerceAtLeast(1)).coerceIn(0f, 1f)
    val inverse = 1f - t
    fun cubic(start: Float, control1: Float, control2: Float, end: Float): Float =
        inverse * inverse * inverse * start +
            3f * inverse * inverse * t * control1 +
            3f * inverse * t * t * control2 +
            t * t * t * end
    return cubic(0.22f, 0.82f, 0.18f, 0.5f) to
        cubic(0.22f, 0.35f, 0.62f, 0.86f)
}

private fun ticksToSeconds(ticks: Int): Int =
    ((ticks.coerceAtLeast(0) + TICKS_PER_SECOND - 1) / TICKS_PER_SECOND).absoluteValue

/** Existing slots plus exactly one legal append target until the seven-slot cap is reached. */
private fun loadoutVisibleSize(selectedSize: Int): Int =
    if (selectedSize < LOADOUT_SIZE) selectedSize + 1 else LOADOUT_SIZE

private fun String.toLoadoutKind(): LoadoutKindUi =
    if (startsWith("tower-")) LoadoutKindUi.TOWER else LoadoutKindUi.ALLY

private fun String.toRosterCategory(): RosterCategoryUi = when {
    contains("hero", ignoreCase = true) -> RosterCategoryUi.HERO
    contains("ally", ignoreCase = true) -> RosterCategoryUi.ALLY
    else -> RosterCategoryUi.TOWER
}

private fun enemyRole(enemyId: String, boss: Boolean): BattleEntityRoleUi = when {
    boss -> BattleEntityRoleUi.ENEMY_BOSS
    enemyId == "enemy-brass-shell" || enemyId == "enemy-siege-bloom" -> {
        BattleEntityRoleUi.ENEMY_ARMORED
    }
    else -> BattleEntityRoleUi.ENEMY_LIGHT
}

private fun allyRole(allyId: String): BattleEntityRoleUi = when (allyId) {
    "ally-bright-skirmisher" -> BattleEntityRoleUi.ALLY_SKIRMISHER
    "ally-arc-striker" -> BattleEntityRoleUi.ALLY_RANGED
    else -> BattleEntityRoleUi.ALLY_GUARD
}

private val META_ROUTES = setOf(
    MySdRoute.CAMPAIGN,
    MySdRoute.ROSTER,
    MySdRoute.TECHNOLOGY,
    MySdRoute.SHOP,
    MySdRoute.REWARD_TRACK,
    MySdRoute.SETTINGS,
    MySdRoute.ARENA,
)

private const val REWARD_TIER_STEP = 10
private const val LOADOUT_SIZE = 7
private const val DEFAULT_VISIBLE_WAVES = 10
private const val DEFAULT_HERO_TITLE = "Командир Маяка"
private const val TECH_COLUMNS = 3
private const val TICKS_PER_SECOND = 20
private const val UNSCOUTED_FORMATION_ID = "formation-unscouted"
