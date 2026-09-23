package dev.mysd.game.product.content

import dev.mysd.game.content.ContentId

enum class ProductTowerRole { RAPID, BURST, CONTROL, SUPPORT }
enum class ProductAllyRole { DEFENDER, SKIRMISHER, RANGED }
enum class ProductEnemyRole { RUNNER, SWARM, ARMOR, DISRUPTOR, SIEGE, FLYER, BOSS }
enum class ProductHeroSkillKind { BASE_REPAIR, AREA_PULSE }
enum class ProductEnhancementKind {
    TOWER_DAMAGE,
    TOWER_COOLDOWN,
    TOWER_RANGE,
    SPLASH_RADIUS,
    SLOW_STRENGTH,
    ALLY_HEALTH,
    ALLY_DAMAGE,
    ALLY_SPEED,
    RESOURCE_INCOME,
    RESOURCE_CAP,
    BASE_ARMOR,
    HERO_COOLDOWN,
}

enum class ProductTechEffectKind {
    TOWER_POWER,
    ALLY_POWER,
    ECONOMY,
    HERO_POWER,
}

enum class ProductShopOfferKind { SOFT_CURRENCY, ENERGY, REWARDED_STUB, PURCHASE_STUB }

data class ProductTowerDefinition(
    val id: ContentId,
    val role: ProductTowerRole,
    val buildCost: Int,
    val upgradeCosts: List<Int>,
    val damage: Int,
    val damagePerLevel: Int,
    val cooldownTicks: Int,
    val cooldownReductionPerLevel: Int,
    val minimumCooldownTicks: Int,
    val rangeTicks: Int,
    val splashRadiusTicks: Int = 0,
    val slowPermille: Int = 0,
    val slowDurationTicks: Int = 0,
    val supportIncomePerSecond: Int = 0,
    val unlockAfterStageOrdinal: Int = 0,
)

data class ProductAllyDefinition(
    val id: ContentId,
    val role: ProductAllyRole,
    val deployCost: Int,
    val health: Int,
    val damage: Int,
    val speedTicks: Int,
    val attackRangeTicks: Int,
    val cooldownTicks: Int,
    val unlockAfterStageOrdinal: Int = 0,
)

data class ProductEnemyDefinition(
    val id: ContentId,
    val role: ProductEnemyRole,
    val health: Int,
    val speedTicks: Int,
    val attackDamage: Int,
    val attackRangeTicks: Int,
    val cooldownTicks: Int,
    val baseLeakDamage: Int,
    val armor: Int,
    val killReward: Int,
    val boss: Boolean = false,
)

data class ProductHeroSkillDefinition(
    val id: ContentId,
    val kind: ProductHeroSkillKind,
    val cooldownTicks: Int,
    val magnitude: Int,
    val unlockAfterStageOrdinal: Int = 0,
)

data class ProductEnhancementDefinition(
    val id: ContentId,
    val kind: ProductEnhancementKind,
    val magnitudePermille: Int,
    val maxStacks: Int,
)

data class ProductSpawnGroup(
    val enemyId: ContentId,
    val count: Int,
    val firstSpawnTick: Int,
    val intervalTicks: Int,
)

data class ProductWaveDefinition(
    val id: ContentId,
    val groups: List<ProductSpawnGroup>,
)

data class ProductBuildSlotDefinition(
    val id: ContentId,
    val positionTicks: Int,
)

data class ProductStageReward(
    val softCurrency: Long,
    val rewardTrackPoints: Int,
    val firstClearPremiumShaped: Long,
)

data class ProductStageDefinition(
    val id: ContentId,
    val regionId: ContentId,
    val ordinal: Int,
    val energyCost: Int,
    val baseHealth: Int,
    val basePositionTicks: Int,
    val initialResource: Int,
    val resourceCap: Int,
    val incomePerSecond: Int,
    val buildSlots: List<ProductBuildSlotDefinition>,
    val towerIds: List<ContentId>,
    val allyIds: List<ContentId>,
    val heroSkillIds: List<ContentId>,
    val enhancementPoolIds: List<ContentId>,
    val enhancementAfterWaves: Set<Int>,
    val waves: List<ProductWaveDefinition>,
    val reward: ProductStageReward,
    val unlocksStageId: ContentId?,
)

data class ProductTechNodeDefinition(
    val id: ContentId,
    val branchId: ContentId,
    val cost: Long,
    val prerequisites: Set<ContentId>,
    val effectKind: ProductTechEffectKind,
    val magnitudePermille: Int,
)

data class ProductRewardTierDefinition(
    val id: ContentId,
    val requiredPoints: Int,
    val softCurrency: Long,
    val premiumShaped: Long,
    val energy: Int,
)

data class ProductShopOfferDefinition(
    val id: ContentId,
    val kind: ProductShopOfferKind,
    val softCost: Long?,
    val softReward: Long,
    val energyReward: Int,
    val repeatable: Boolean,
)

data class ProductCatalog(
    val packId: ContentId,
    val contentVersion: Int,
    val stages: Map<ContentId, ProductStageDefinition>,
    val towers: Map<ContentId, ProductTowerDefinition>,
    val allies: Map<ContentId, ProductAllyDefinition>,
    val enemies: Map<ContentId, ProductEnemyDefinition>,
    val heroSkills: Map<ContentId, ProductHeroSkillDefinition>,
    val enhancements: Map<ContentId, ProductEnhancementDefinition>,
    val techNodes: Map<ContentId, ProductTechNodeDefinition>,
    val rewardTiers: List<ProductRewardTierDefinition>,
    val shopOffers: Map<ContentId, ProductShopOfferDefinition>,
) {
    init {
        ProductCatalogValidator.validate(this)
    }

    val orderedStages: List<ProductStageDefinition>
        get() = stages.values.sortedBy(ProductStageDefinition::ordinal)
}

object ProductCatalogValidator {
    private const val MAX_COMBAT_VALUE = 1_000_000
    private const val MAX_REWARD_VALUE = 1_000_000L
    const val REQUIRED_STAGE_COUNT = 6
    const val REQUIRED_WAVE_COUNT = 10
    const val REQUIRED_TOWER_COUNT = 4
    const val REQUIRED_ALLY_COUNT = 3
    const val REQUIRED_ENEMY_COUNT = 8
    const val REQUIRED_HERO_SKILL_COUNT = 2
    const val REQUIRED_ENHANCEMENT_COUNT = 12
    const val REQUIRED_TECH_COUNT = 12
    const val REQUIRED_REWARD_TIER_COUNT = 15
    const val REQUIRED_SHOP_OFFER_COUNT = 4

    fun validate(catalog: ProductCatalog) {
        require(catalog.contentVersion > 0) { "Product content version must be positive." }
        require(catalog.stages.size == REQUIRED_STAGE_COUNT) { "Release one requires six stages." }
        require(catalog.towers.size == REQUIRED_TOWER_COUNT) { "Release one requires four towers." }
        require(catalog.allies.size == REQUIRED_ALLY_COUNT) { "Release one requires three allies." }
        require(catalog.enemies.size == REQUIRED_ENEMY_COUNT) { "Release one requires six enemies and two bosses." }
        require(catalog.enemies.values.count(ProductEnemyDefinition::boss) == 2) { "Release one requires two bosses." }
        require(catalog.heroSkills.size == REQUIRED_HERO_SKILL_COUNT) { "Release one requires two hero skills." }
        require(catalog.enhancements.size == REQUIRED_ENHANCEMENT_COUNT) { "Release one requires twelve enhancements." }
        require(catalog.techNodes.size == REQUIRED_TECH_COUNT) { "Release one requires twelve technology nodes." }
        require(catalog.rewardTiers.size == REQUIRED_REWARD_TIER_COUNT) { "Release one requires fifteen reward tiers." }
        require(catalog.shopOffers.size == REQUIRED_SHOP_OFFER_COUNT) { "Release one requires four shop offers." }
        require(catalog.stages.keys == catalog.stages.values.map { it.id }.toSet()) { "Stage map keys must match stage IDs." }
        require(catalog.towers.keys == catalog.towers.values.map { it.id }.toSet()) { "Tower map keys must match tower IDs." }
        require(catalog.allies.keys == catalog.allies.values.map { it.id }.toSet()) { "Ally map keys must match ally IDs." }
        require(catalog.enemies.keys == catalog.enemies.values.map { it.id }.toSet()) { "Enemy map keys must match enemy IDs." }
        require(catalog.heroSkills.keys == catalog.heroSkills.values.map { it.id }.toSet()) { "Skill map keys must match skill IDs." }
        require(catalog.enhancements.keys == catalog.enhancements.values.map { it.id }.toSet()) { "Enhancement map keys must match enhancement IDs." }
        require(catalog.techNodes.keys == catalog.techNodes.values.map { it.id }.toSet()) { "Tech map keys must match tech IDs." }
        require(catalog.shopOffers.keys == catalog.shopOffers.values.map { it.id }.toSet()) { "Shop map keys must match offer IDs." }
        val allIds = catalog.stages.keys + catalog.towers.keys + catalog.allies.keys + catalog.enemies.keys +
            catalog.heroSkills.keys + catalog.enhancements.keys + catalog.techNodes.keys +
            catalog.rewardTiers.map { it.id } + catalog.shopOffers.keys
        val expectedIdCount = catalog.stages.size + catalog.towers.size + catalog.allies.size + catalog.enemies.size +
            catalog.heroSkills.size + catalog.enhancements.size + catalog.techNodes.size +
            catalog.rewardTiers.size + catalog.shopOffers.size
        require(allIds.size == expectedIdCount) { "Content IDs must be unique across product families." }
        require(catalog.towers.values.map { it.role }.toSet() == ProductTowerRole.entries.toSet()) { "Every tower role is required." }
        require(catalog.allies.values.map { it.role }.toSet() == ProductAllyRole.entries.toSet()) { "Every ally role is required." }
        require(catalog.enemies.values.map { it.role }.toSet() == ProductEnemyRole.entries.toSet()) { "Every enemy role is required." }
        require(catalog.heroSkills.values.map { it.kind }.toSet() == ProductHeroSkillKind.entries.toSet()) { "Every hero skill kind is required." }
        require(catalog.enhancements.values.map { it.kind }.toSet() == ProductEnhancementKind.entries.toSet()) { "Every enhancement kind is required." }

        val stageOrdinals = catalog.stages.values.map(ProductStageDefinition::ordinal).sorted()
        require(stageOrdinals == (1..REQUIRED_STAGE_COUNT).toList()) { "Stage ordinals must be contiguous." }
        catalog.orderedStages.forEachIndexed { index, stage ->
            require(stage.energyCost in 1..10) { "Stage energy cost must fit the release-one energy capacity." }
            require(stage.baseHealth in 1..MAX_COMBAT_VALUE && stage.basePositionTicks in 1..MAX_COMBAT_VALUE) { "Stage base must be playable." }
            require(stage.resourceCap in 1..MAX_COMBAT_VALUE) { "Resource capacity is outside safe bounds." }
            require(stage.initialResource in 0..stage.resourceCap) { "Initial resource must be within cap." }
            require(stage.incomePerSecond in 0..MAX_COMBAT_VALUE) { "Income is outside safe bounds." }
            require(stage.buildSlots.size == 4) { "Release-one stages require four build slots." }
            require(stage.buildSlots.map { it.id }.distinct().size == stage.buildSlots.size) { "Build slot IDs must be unique." }
            require(stage.buildSlots.zipWithNext().all { (a, b) -> a.positionTicks < b.positionTicks }) { "Build slots must follow path order." }
            require(stage.buildSlots.all { it.positionTicks in 0 until stage.basePositionTicks }) {
                "Build slots must lie on the playable path before the base."
            }
            require(stage.waves.size == REQUIRED_WAVE_COUNT) { "Every stage requires ten waves." }
            require(stage.waves.map { it.id }.distinct().size == stage.waves.size) { "Wave IDs must be unique per stage." }
            require(stage.enhancementAfterWaves == setOf(2, 4, 6, 8)) { "Enhancement choices belong after waves 2, 4, 6, and 8." }
            require(stage.towerIds.size == REQUIRED_TOWER_COUNT && catalog.towers.keys.containsAll(stage.towerIds)) { "Stage tower roster is incomplete." }
            require(stage.allyIds.size == REQUIRED_ALLY_COUNT && catalog.allies.keys.containsAll(stage.allyIds)) { "Stage ally roster is incomplete." }
            require(stage.heroSkillIds.size == REQUIRED_HERO_SKILL_COUNT && catalog.heroSkills.keys.containsAll(stage.heroSkillIds)) { "Stage skill roster is incomplete." }
            require(stage.enhancementPoolIds.size == REQUIRED_ENHANCEMENT_COUNT && catalog.enhancements.keys.containsAll(stage.enhancementPoolIds)) { "Stage enhancement pool is incomplete." }
            require(stage.towerIds.distinct().size == stage.towerIds.size) { "Stage tower roster contains duplicates." }
            require(stage.allyIds.distinct().size == stage.allyIds.size) { "Stage ally roster contains duplicates." }
            require(stage.heroSkillIds.distinct().size == stage.heroSkillIds.size) { "Stage hero roster contains duplicates." }
            require(stage.enhancementPoolIds.distinct().size == stage.enhancementPoolIds.size) { "Stage enhancement pool contains duplicates." }
            require(stage.reward.softCurrency in 0..MAX_REWARD_VALUE && stage.reward.rewardTrackPoints in 0..MAX_COMBAT_VALUE &&
                stage.reward.firstClearPremiumShaped in 0..MAX_REWARD_VALUE
            ) { "Stage reward values must be non-negative." }
            stage.waves.forEach { wave ->
                require(wave.groups.isNotEmpty()) { "Wave requires a spawn group." }
                require(wave.groups.sumOf { it.count.toLong() } in 1..10_000L) { "Wave population is outside save bounds." }
                wave.groups.forEach { group ->
                    require(group.enemyId in catalog.enemies) { "Wave references an unknown enemy." }
                    require(group.count > 0 && group.firstSpawnTick >= 0 && group.intervalTicks > 0) { "Spawn group values are invalid." }
                    require(group.firstSpawnTick.toLong() + (group.count - 1L) * group.intervalTicks <= MAX_COMBAT_VALUE) {
                        "Wave spawn schedule is outside safe clock bounds."
                    }
                }
            }
            val expectedUnlock = catalog.orderedStages.getOrNull(index + 1)?.id
            require(stage.unlocksStageId == expectedUnlock) { "Stage unlock chain must be linear." }
        }

        catalog.towers.values.forEach { tower ->
            require(listOf(tower.buildCost, tower.damage, tower.damagePerLevel, tower.cooldownTicks,
                tower.cooldownReductionPerLevel, tower.minimumCooldownTicks, tower.rangeTicks,
                tower.splashRadiusTicks, tower.slowDurationTicks, tower.supportIncomePerSecond)
                .all { it in 0..MAX_COMBAT_VALUE } && tower.upgradeCosts.all { it in 0..MAX_COMBAT_VALUE }
            ) { "Tower values exceed safe arithmetic bounds." }
            require(tower.buildCost >= 0 && tower.upgradeCosts.size == 3) { "Tower requires three non-negative upgrade levels." }
            require(tower.upgradeCosts.all { it >= 0 }) { "Tower upgrade costs must be non-negative." }
            require(tower.damage >= 0 && tower.cooldownTicks > 0 && tower.minimumCooldownTicks > 0) { "Tower combat values are invalid." }
            require(tower.damagePerLevel >= 0 && tower.cooldownReductionPerLevel >= 0 &&
                tower.minimumCooldownTicks <= tower.cooldownTicks && tower.rangeTicks > 0 &&
                tower.splashRadiusTicks >= 0 && tower.supportIncomePerSecond >= 0
            ) { "Tower progression values are invalid." }
            require(tower.slowPermille in 0..900 && tower.slowDurationTicks >= 0) { "Tower slow values are invalid." }
            require(tower.unlockAfterStageOrdinal in 0 until REQUIRED_STAGE_COUNT) { "Tower unlock coordinate is invalid." }
        }
        catalog.allies.values.forEach { ally ->
            require(listOf(ally.deployCost, ally.health, ally.damage, ally.speedTicks,
                ally.attackRangeTicks, ally.cooldownTicks).all { it in 0..MAX_COMBAT_VALUE }
            ) { "Ally values exceed safe arithmetic bounds." }
            require(ally.deployCost >= 0 && ally.health > 0 && ally.damage >= 0) { "Ally values are invalid." }
            require(ally.speedTicks > 0 && ally.attackRangeTicks >= 0 && ally.cooldownTicks > 0) { "Ally timing is invalid." }
            require(ally.unlockAfterStageOrdinal in 0 until REQUIRED_STAGE_COUNT) { "Ally unlock coordinate is invalid." }
        }
        catalog.enemies.values.forEach { enemy ->
            require(listOf(enemy.health, enemy.speedTicks, enemy.attackDamage, enemy.attackRangeTicks,
                enemy.cooldownTicks, enemy.baseLeakDamage, enemy.armor, enemy.killReward)
                .all { it in 0..MAX_COMBAT_VALUE }) { "Enemy values exceed safe arithmetic bounds." }
            require(enemy.health > 0 && enemy.speedTicks > 0 && enemy.baseLeakDamage > 0) { "Enemy values are invalid." }
            require(enemy.attackDamage >= 0 && enemy.attackRangeTicks >= 0 && enemy.cooldownTicks > 0) { "Enemy combat values are invalid." }
            require(enemy.armor >= 0 && enemy.killReward >= 0) { "Enemy economy values are invalid." }
            require(enemy.boss == (enemy.role == ProductEnemyRole.BOSS)) { "Boss flag and role must agree." }
        }
        catalog.heroSkills.values.forEach { skill ->
            require(skill.cooldownTicks in 1..MAX_COMBAT_VALUE && skill.magnitude in 1..MAX_COMBAT_VALUE) { "Hero skill values are invalid." }
            require(skill.unlockAfterStageOrdinal in 0 until REQUIRED_STAGE_COUNT) { "Hero unlock coordinate is invalid." }
        }
        catalog.enhancements.values.forEach { enhancement ->
            require(enhancement.magnitudePermille in 1..5_000 && enhancement.maxStacks in 1..4) { "Enhancement values are invalid." }
        }
        catalog.techNodes.values.forEach { node ->
            require(node.cost in 0..MAX_REWARD_VALUE && node.magnitudePermille in 1..400) { "Technology values are invalid." }
            require(catalog.techNodes.keys.containsAll(node.prerequisites)) { "Technology prerequisite is unknown." }
            require(node.id !in node.prerequisites) { "Technology cannot require itself." }
        }
        require(isAcyclic(catalog.techNodes)) { "Technology graph must be acyclic." }
        require(catalog.rewardTiers.zipWithNext().all { (a, b) -> a.requiredPoints < b.requiredPoints }) { "Reward tiers must be strictly ordered." }
        require(catalog.rewardTiers.map { it.id }.distinct().size == catalog.rewardTiers.size) { "Reward tier IDs must be unique." }
        require(catalog.rewardTiers.all {
            it.requiredPoints in 1..MAX_COMBAT_VALUE && it.softCurrency in 0..MAX_REWARD_VALUE &&
                it.premiumShaped in 0..MAX_REWARD_VALUE && it.energy in 0..10
        }) { "Reward tier values are invalid." }
        catalog.shopOffers.values.forEach { offer ->
            require(offer.softCost == null || offer.softCost in 0..MAX_REWARD_VALUE) { "Shop cost is outside safe bounds." }
            require(offer.softReward in 0..MAX_REWARD_VALUE && offer.energyReward in 0..10) { "Shop reward values are invalid." }
            when (offer.kind) {
                ProductShopOfferKind.SOFT_CURRENCY -> require(
                    offer.softCost != null && offer.softReward > 0 && offer.energyReward == 0,
                ) { "Soft-currency offers require a cost and a soft reward." }
                ProductShopOfferKind.ENERGY -> require(
                    offer.softCost != null && offer.softReward == 0L && offer.energyReward > 0,
                ) { "Energy offers require a cost and an energy reward." }
                ProductShopOfferKind.REWARDED_STUB,
                ProductShopOfferKind.PURCHASE_STUB,
                -> require(
                    offer.softCost == null && offer.softReward == 0L && offer.energyReward == 0 && offer.repeatable,
                ) {
                    "Service placeholders must not grant local value."
                }
            }
        }
        require(catalog.towers.values.any { it.unlockAfterStageOrdinal == 0 }) {
            "Release one requires an initially unlocked tower."
        }
        require(catalog.allies.values.any { it.unlockAfterStageOrdinal == 0 }) {
            "Release one requires an initially unlocked ally."
        }
        require(catalog.heroSkills.values.any { it.unlockAfterStageOrdinal == 0 }) {
            "Release one requires an initially unlocked hero ability."
        }
    }

    private fun isAcyclic(nodes: Map<ContentId, ProductTechNodeDefinition>): Boolean {
        val visiting = mutableSetOf<ContentId>()
        val visited = mutableSetOf<ContentId>()
        fun visit(id: ContentId): Boolean {
            if (id in visited) return true
            if (!visiting.add(id)) return false
            if (!nodes.getValue(id).prerequisites.all(::visit)) return false
            visiting.remove(id)
            visited += id
            return true
        }
        return nodes.keys.all(::visit)
    }
}
