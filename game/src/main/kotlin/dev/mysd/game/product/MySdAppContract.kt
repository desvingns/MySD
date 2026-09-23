package dev.mysd.game.product

/** Android-free route identifiers for the complete offline product shell. */
enum class MySdRoute {
    HOME,
    CAMPAIGN,
    STAGE_SETUP,
    BATTLE,
    ROSTER,
    TECHNOLOGY,
    SHOP,
    REWARD_TRACK,
    SETTINGS,
    ARENA,
}

enum class MySdBattlePhase {
    PREPARATION,
    COMBAT,
    ENHANCEMENT,
    VICTORY,
    DEFEAT,
}

enum class MySdBattleSpeed(val ticksPerPulse: Int) {
    ONE_X(1),
    TWO_X(2),
}

enum class MySdTerminalResult {
    VICTORY,
    DEFEAT,
}

enum class MySdSettingId {
    SOUND,
    MUSIC,
    HAPTICS,
    REDUCE_MOTION,
}

data class MySdCurrencySnapshot(
    val soft: Long,
    val premiumShaped: Long,
)

data class MySdEnergySnapshot(
    val current: Int,
    val maximum: Int,
    val secondsUntilNext: Int,
)

data class MySdProfileSnapshot(
    val profileId: String,
    val currencies: MySdCurrencySnapshot,
    val energy: MySdEnergySnapshot,
    val totalStars: Int,
    val rewardTrackPoints: Int,
    val loadoutIds: List<String>,
    val settings: Map<MySdSettingId, Boolean>,
)

data class MySdClockSnapshot(
    val tick: Long,
    val running: Boolean,
    val speed: MySdBattleSpeed,
    val stableHash: String,
)

data class MySdStageCardSnapshot(
    val stageId: String,
    val regionId: String,
    val ordinal: Int,
    val unlocked: Boolean,
    val energyCost: Int,
    val bestStars: Int,
    val clearCount: Int,
    val sweepAvailable: Boolean,
)

data class MySdTowerSlotSnapshot(
    val slotId: String,
    val positionTicks: Int,
    val towerId: String?,
    val level: Int,
    val cooldownRemainingTicks: Int,
    val nextUpgradeCost: Int? = null,
    val maximumLevel: Boolean = false,
)

data class MySdAllySnapshot(
    val entityId: Long,
    val allyId: String,
    val health: Int,
    val maxHealth: Int,
    val positionTicks: Int,
)

data class MySdEnemySnapshot(
    val entityId: Long,
    val enemyId: String,
    val health: Int,
    val maxHealth: Int,
    val positionTicks: Int,
    val boss: Boolean,
)

data class MySdHeroSkillSnapshot(
    val skillId: String,
    val cooldownRemainingTicks: Int,
    val cooldownTicks: Int,
    val available: Boolean,
)

data class MySdBattleSnapshot(
    val runId: String,
    val stageId: String,
    val phase: MySdBattlePhase,
    val paused: Boolean,
    val speed: MySdBattleSpeed,
    val waveNumber: Int,
    val totalWaves: Int,
    val resource: Int,
    val resourceCap: Int,
    val baseHealth: Int,
    val baseMaxHealth: Int,
    val towerSlots: List<MySdTowerSlotSnapshot>,
    val allies: List<MySdAllySnapshot>,
    val enemies: List<MySdEnemySnapshot>,
    val heroSkills: List<MySdHeroSkillSnapshot>,
    val enhancementIds: List<String>,
    val terminalResult: MySdTerminalResult?,
    val pathLengthTicks: Int = 1_000,
)

sealed interface MySdSurfaceSnapshot {
    data object Home : MySdSurfaceSnapshot

    data class CampaignMap(
        val stages: List<MySdStageCardSnapshot>,
    ) : MySdSurfaceSnapshot

    data class StageSetup(
        val stage: MySdStageCardSnapshot,
        val selectedLoadoutIds: List<String>,
        val availableLoadoutIds: List<String>,
        val canStart: Boolean,
        val selectedHeroSkillIds: List<String> = emptyList(),
        val availableHeroSkillIds: List<String> = emptyList(),
    ) : MySdSurfaceSnapshot

    data class Battle(
        val towerCardIds: List<String>,
        val allyCardIds: List<String>,
        val heroSkillIds: List<String>,
        val towerBuildCosts: Map<String, Int> = emptyMap(),
        val allyDeployCosts: Map<String, Int> = emptyMap(),
    ) : MySdSurfaceSnapshot

    data class Roster(
        val entries: List<MySdRosterEntrySnapshot>,
        val selectedLoadoutIds: List<String>,
        val selectedHeroSkillIds: List<String> = emptyList(),
    ) : MySdSurfaceSnapshot

    data class Technology(
        val nodes: List<MySdTechNodeSnapshot>,
    ) : MySdSurfaceSnapshot

    data class Shop(
        val offers: List<MySdShopOfferSnapshot>,
    ) : MySdSurfaceSnapshot

    data class RewardTrack(
        val tiers: List<MySdRewardTierSnapshot>,
    ) : MySdSurfaceSnapshot

    data class Settings(
        val values: Map<MySdSettingId, Boolean>,
    ) : MySdSurfaceSnapshot

    data class Arena(
        val exhibition: MySdArenaSnapshot,
    ) : MySdSurfaceSnapshot
}

data class MySdRosterEntrySnapshot(
    val contentId: String,
    val category: String,
    val level: Int,
    val maximumLevel: Int,
    val unlocked: Boolean,
    val upgradeCost: Long?,
    val unlockAfterStageOrdinal: Int = 0,
)

data class MySdTechNodeSnapshot(
    val nodeId: String,
    val branchId: String,
    val unlocked: Boolean,
    val prerequisitesMet: Boolean,
    val cost: Long,
)

data class MySdShopOfferSnapshot(
    val offerId: String,
    val kind: String,
    val cost: Long?,
    val affordable: Boolean,
    val stubOnly: Boolean,
    val softReward: Long = 0,
    val energyReward: Int = 0,
    val repeatable: Boolean = true,
    val purchased: Int = 0,
)

data class MySdRewardTierSnapshot(
    val tierId: String,
    val requiredPoints: Int,
    val unlocked: Boolean,
    val claimed: Boolean,
    val softReward: Long = 0,
    val premiumShapedReward: Long = 0,
    val energyReward: Int = 0,
)

data class MySdArenaSnapshot(
    val opponentFormationId: String,
    val playerPower: Int,
    val opponentPower: Int,
    val result: MySdTerminalResult?,
    val networkUsed: Boolean,
)

sealed interface MySdOverlaySnapshot {
    data class EnhancementChoice(
        val offerIds: List<String>,
        val rerollsRemaining: Int,
    ) : MySdOverlaySnapshot

    data class TerminalReward(
        val result: MySdTerminalResult,
        val stars: Int,
        val softReward: Long,
        val rewardTrackPoints: Int,
        val claimed: Boolean,
    ) : MySdOverlaySnapshot

    data class ResumeRun(
        val stageId: String,
    ) : MySdOverlaySnapshot

    data class Message(
        val code: String,
        val serviceStub: Boolean,
    ) : MySdOverlaySnapshot
}

/** One immutable projection published atomically to Android. */
data class MySdAppSnapshot(
    val revision: Long,
    val route: MySdRoute,
    val profile: MySdProfileSnapshot,
    val battle: MySdBattleSnapshot?,
    val overlay: MySdOverlaySnapshot?,
    val clock: MySdClockSnapshot,
    val surface: MySdSurfaceSnapshot,
)

sealed interface MySdAppIntent {
    data class Navigate(val route: MySdRoute) : MySdAppIntent
    data class SelectStage(val stageId: String) : MySdAppIntent
    data object StartSelectedStage : MySdAppIntent
    data object ResumeRun : MySdAppIntent
    data object AbandonRun : MySdAppIntent
    data object PauseOrResume : MySdAppIntent
    data object ToggleBattleSpeed : MySdAppIntent
    data class BuildTower(val slotId: String, val towerId: String) : MySdAppIntent
    data class UpgradeTower(val slotId: String) : MySdAppIntent
    data class DeployAlly(val allyId: String) : MySdAppIntent
    data class UseHeroSkill(val skillId: String) : MySdAppIntent
    data class ChooseEnhancement(val enhancementId: String) : MySdAppIntent
    data object RerollEnhancements : MySdAppIntent
    data object ClaimBattleReward : MySdAppIntent
    data class SetLoadoutSlot(val index: Int, val contentId: String) : MySdAppIntent
    data class ClearLoadoutSlot(val index: Int) : MySdAppIntent
    data class ToggleHeroSkill(val skillId: String) : MySdAppIntent
    data class UpgradeRosterItem(val contentId: String) : MySdAppIntent
    data class UnlockTech(val nodeId: String) : MySdAppIntent
    data class BuyShopOffer(val offerId: String) : MySdAppIntent
    data class ClaimRewardTier(val tierId: String) : MySdAppIntent
    data class SweepStage(val stageId: String) : MySdAppIntent
    data class ToggleSetting(val settingId: MySdSettingId) : MySdAppIntent
    data class RefreshEnergy(val nowEpochSeconds: Long) : MySdAppIntent
    data class RequestRewardedStub(val opportunityId: String) : MySdAppIntent
    data class RequestPurchaseStub(val productId: String) : MySdAppIntent
    data object StartArenaExhibition : MySdAppIntent
    data object CloseOverlay : MySdAppIntent
}

enum class MySdAppRejection {
    WRONG_ROUTE,
    UNKNOWN_CONTENT,
    LOCKED,
    ACTIVE_RUN_EXISTS,
    NO_ACTIVE_RUN,
    INSUFFICIENT_ENERGY,
    INSUFFICIENT_CURRENCY,
    INVALID_TARGET,
    INVALID_PHASE,
    MAXIMUM_LEVEL,
    PREREQUISITE_MISSING,
    ALREADY_CLAIMED,
    SWEEP_NOT_AVAILABLE,
    TERMINAL_RUN,
    SERVICE_STUB,
    INVALID_TICK_COUNT,
}

data class MySdAppEvent(
    val id: Long,
    val type: String,
    val sourceId: String,
    val amount: Long = 0L,
)

data class MySdAppResult(
    val accepted: Boolean,
    val rejection: MySdAppRejection?,
    val snapshot: MySdAppSnapshot,
    val events: List<MySdAppEvent> = emptyList(),
)

data class MySdSaveBundle(
    val runSave: String?,
    val profileSave: String,
)

enum class MySdRestoreIncompatibility {
    RUNTIME,
    CONTENT,
    RUN_SCHEMA,
    PROFILE_SCHEMA,
}

sealed interface MySdRestoreResult {
    data class Restored(val session: MySdAppSession) : MySdRestoreResult

    data class Incompatible(
        val kind: MySdRestoreIncompatibility,
        val expected: String,
        val actual: String,
    ) : MySdRestoreResult

    data class Invalid(val reason: String) : MySdRestoreResult
}
