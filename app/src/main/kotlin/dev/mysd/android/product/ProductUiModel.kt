package dev.mysd.android.product

import androidx.compose.runtime.Immutable

@Immutable
data class ProductUiModel(
    val shell: ProductShellUi,
    val surface: ProductSurfaceUi,
    val overlay: ProductOverlayUi? = null,
    val reduceMotion: Boolean = false,
)

@Immutable
data class ProductShellUi(
    val energy: Int,
    val energyCap: Int,
    val credits: Long,
    val crystals: Long,
    val rewardTrackExperience: Int,
    val rewardTrackTarget: Int,
    val selectedDestination: ProductDestinationUi?,
    val showMetaNavigation: Boolean,
)

enum class ProductDestinationUi {
    CAMPAIGN,
    ROSTER,
    TECH,
    SHOP,
    ARENA,
}

sealed interface ProductSurfaceUi {
    @Immutable
    data class Launch(
        val canEnter: Boolean = true,
    ) : ProductSurfaceUi

    @Immutable
    data class CampaignMap(
        val stages: List<CampaignStageUi>,
    ) : ProductSurfaceUi

    @Immutable
    data class Setup(
        val stageId: String,
        val title: String,
        val threatLabel: String,
        val waveCount: Int,
        val energyCost: Int,
        val energyAvailable: Boolean,
        val sweepAvailable: Boolean,
        val heroName: String,
        val loadout: List<LoadoutSlotUi>,
        val availableLoadout: List<SetupChoiceUi>,
        val canStart: Boolean,
        val heroChoices: List<SetupHeroChoiceUi> = emptyList(),
    ) : ProductSurfaceUi

    @Immutable
    data class Battle(
        val scene: BattleSceneUi,
    ) : ProductSurfaceUi

    @Immutable
    data class Roster(
        val entries: List<RosterEntryUi>,
        val equippedIds: Set<String>,
        val maxEquipped: Int,
    ) : ProductSurfaceUi

    @Immutable
    data class Tech(
        val nodes: List<TechNodeUi>,
    ) : ProductSurfaceUi

    @Immutable
    data class Shop(
        val products: List<ShopProductUi>,
    ) : ProductSurfaceUi

    @Immutable
    data class RewardTrack(
        val tiers: List<RewardTierUi>,
        val experience: Int,
    ) : ProductSurfaceUi

    @Immutable
    data class Settings(
        val values: Map<ProductSettingUi, Boolean>,
    ) : ProductSurfaceUi

    @Immutable
    data class Arena(
        val state: ArenaUi,
    ) : ProductSurfaceUi
}

@Immutable
data class CampaignStageUi(
    val id: String,
    val title: String,
    val subtitle: String,
    val status: StageStatusUi,
    val stars: Int,
    val energyCost: Int,
    val waveCount: Int,
)

enum class StageStatusUi {
    LOCKED,
    AVAILABLE,
    COMPLETED,
}

@Immutable
data class LoadoutSlotUi(
    val index: Int,
    val contentId: String?,
    val label: String?,
    val kind: LoadoutKindUi?,
)

@Immutable
data class SetupChoiceUi(
    val id: String,
    val title: String,
    val description: String,
    val kind: LoadoutKindUi,
)

@Immutable
data class SetupHeroChoiceUi(
    val id: String,
    val title: String,
    val description: String,
    val selected: Boolean,
)

enum class LoadoutKindUi {
    TOWER,
    ALLY,
}

enum class BattlePhaseUi {
    WAVE_INTRO,
    ACTIVE,
    PAUSED,
    INTER_WAVE_CHOICE,
    VICTORY,
    DEFEAT,
}

@Immutable
data class BattleSceneUi(
    val stageTitle: String,
    val phase: BattlePhaseUi,
    val wave: Int,
    val totalWaves: Int,
    val resource: Int,
    val resourceCap: Int,
    val speedMultiplier: Int,
    val baseHealth: Int,
    val baseMaxHealth: Int,
    val slots: List<BattleSlotUi>,
    val enemies: List<BattleEntityUi>,
    val allies: List<BattleEntityUi>,
    val abilities: List<HeroAbilityUi>,
    val availableTowerIds: List<String>,
    val availableAllyIds: List<String>,
    val towerBuildCosts: Map<String, Int>,
    val allyDeployCosts: Map<String, Int>,
    val enhancementChoices: List<EnhancementChoiceUi> = emptyList(),
    val enhancementRerollsRemaining: Int = 0,
    val result: BattleResultUi? = null,
    val runId: String = "",
)

@Immutable
data class BattleSlotUi(
    val id: String,
    val index: Int,
    val xFraction: Float,
    val yFraction: Float,
    val towerName: String?,
    val level: Int,
    val cost: Int,
    val canAfford: Boolean,
    val maxLevel: Boolean,
)

@Immutable
data class BattleEntityUi(
    val id: String,
    val label: String,
    val xFraction: Float,
    val yFraction: Float,
    val health: Int,
    val maxHealth: Int,
    val role: BattleEntityRoleUi,
)

enum class BattleEntityRoleUi {
    ENEMY_LIGHT,
    ENEMY_ARMORED,
    ENEMY_BOSS,
    ALLY_SKIRMISHER,
    ALLY_GUARD,
    ALLY_RANGED,
}

@Immutable
data class HeroAbilityUi(
    val id: String,
    val title: String,
    val ready: Boolean,
    val cooldownSeconds: Int,
)

@Immutable
data class EnhancementChoiceUi(
    val id: String,
    val title: String,
    val description: String,
    val tier: Int,
)

@Immutable
data class BattleResultUi(
    val victory: Boolean,
    val completedWaves: Int,
    val defeatedEnemies: Int,
    val credits: Int,
    val experience: Int,
    val claimed: Boolean,
    val multiplierAvailable: Boolean,
)

@Immutable
data class RosterEntryUi(
    val id: String,
    val title: String,
    val role: String,
    val level: Int,
    val power: Int,
    val upgradeCost: Long,
    val affordable: Boolean,
    val unlocked: Boolean,
    val unlockStage: String?,
    val category: RosterCategoryUi = RosterCategoryUi.TOWER,
    val maximumLevel: Boolean = false,
)

enum class RosterCategoryUi {
    TOWER,
    ALLY,
    HERO,
}

enum class TechNodeStatusUi {
    LOCKED,
    AVAILABLE,
    UNLOCKED,
}

@Immutable
data class TechNodeUi(
    val id: String,
    val title: String,
    val description: String,
    val status: TechNodeStatusUi,
    val cost: Long,
    val prerequisiteLabel: String?,
    val affordable: Boolean,
    val column: Int,
    val row: Int,
)

enum class ShopProductKindUi {
    SOFT_CURRENCY,
    REWARDED_STUB,
    PURCHASE_STUB,
}

@Immutable
data class ShopProductUi(
    val id: String,
    val title: String,
    val description: String,
    val kind: ShopProductKindUi,
    val cost: Long,
    val affordable: Boolean,
    val owned: Boolean,
    val softReward: Long = 0,
    val energyReward: Int = 0,
    val purchased: Int = 0,
)

enum class RewardTierStatusUi {
    LOCKED,
    AVAILABLE,
    CLAIMED,
}

@Immutable
data class RewardTierUi(
    val id: String,
    val tier: Int,
    val requiredExperience: Int,
    val rewardLabel: String,
    val status: RewardTierStatusUi,
    val softReward: Long = 0,
    val crystalReward: Long = 0,
    val energyReward: Int = 0,
)

sealed interface ArenaUi {
    @Immutable
    data class Lobby(
        val rating: Int,
        val tickets: Int,
        val recentResults: List<String>,
    ) : ArenaUi

    @Immutable
    data class OpponentPreview(
        val rating: Int,
        val tickets: Int,
        val opponentName: String,
        val opponentPower: Int,
        val seedLabel: String,
    ) : ArenaUi

    @Immutable
    data class Battle(
        val rating: Int,
        val tickets: Int,
        val opponentName: String,
        val scene: BattleSceneUi,
    ) : ArenaUi

    @Immutable
    data class Result(
        val rating: Int,
        val tickets: Int,
        val opponentName: String,
        val victory: Boolean,
        val ratingDelta: Int,
    ) : ArenaUi
}

sealed interface ProductOverlayUi {
    @Immutable
    data class ResumeRun(
        val stageTitle: String,
    ) : ProductOverlayUi

    @Immutable
    data class BattleSlot(
        val slot: BattleSlotUi,
    ) : ProductOverlayUi

    @Immutable
    data class ServiceResult(
        val kind: ServiceResultKindUi,
        val title: String,
    ) : ProductOverlayUi
}

enum class ServiceResultKindUi {
    REWARDED,
    PURCHASE,
    ARENA,
}

sealed interface ProductUiAction {
    data object EnterCampaign : ProductUiAction
    data class Navigate(val destination: ProductDestinationUi) : ProductUiAction
    data object OpenRewardTrack : ProductUiAction
    data object OpenSettings : ProductUiAction
    data object CloseOverlay : ProductUiAction
    data object ResumeRun : ProductUiAction
    data object DiscardRun : ProductUiAction
    data class SelectStage(val stageId: String) : ProductUiAction
    data object BackToCampaign : ProductUiAction
    data class SetLoadoutSlot(val index: Int, val contentId: String) : ProductUiAction
    data class ToggleHeroSkill(val skillId: String) : ProductUiAction
    data object StartBattle : ProductUiAction
    data object SweepStage : ProductUiAction
    data object PauseOrResume : ProductUiAction
    /** Presentation-only request. The product host asks for confirmation before abandoning. */
    data object RequestExitBattle : ProductUiAction
    data object CancelExitBattle : ProductUiAction
    data object ConfirmExitBattle : ProductUiAction
    data object ChangeSpeed : ProductUiAction
    data class SelectBattleSlot(val slotId: String) : ProductUiAction
    data class BuildTower(val slotId: String, val towerId: String) : ProductUiAction
    data class UpgradeTower(val slotId: String) : ProductUiAction
    data class DeployAlly(val allyId: String) : ProductUiAction
    data class UseAbility(val abilityId: String) : ProductUiAction
    data class SelectEnhancement(val enhancementId: String) : ProductUiAction
    data object RerollEnhancements : ProductUiAction
    data object ClaimBattleReward : ProductUiAction
    data object ClaimBattleRewardMultiplier : ProductUiAction
    data object RetryBattle : ProductUiAction
    data class ToggleRosterEntry(val entryId: String) : ProductUiAction
    data class UpgradeRosterEntry(val entryId: String) : ProductUiAction
    data class UnlockTech(val nodeId: String) : ProductUiAction
    data class BuyShopProduct(val productId: String) : ProductUiAction
    data class RequestRewardedStub(val opportunityId: String) : ProductUiAction
    data class RequestPurchaseStub(val productId: String) : ProductUiAction
    data class ClaimRewardTier(val tierId: String) : ProductUiAction
    data class ToggleSetting(val id: ProductSettingUi) : ProductUiAction
    data object ArenaFindOpponent : ProductUiAction
    data object ArenaStartBattle : ProductUiAction
    data object ArenaCancelPreview : ProductUiAction
    data object ArenaReturnToLobby : ProductUiAction
}

enum class ProductSettingUi {
    SOUND,
    MUSIC,
    HAPTICS,
    REDUCED_MOTION,
}
