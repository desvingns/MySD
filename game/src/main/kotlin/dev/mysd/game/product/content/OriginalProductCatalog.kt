package dev.mysd.game.product.content

import dev.mysd.game.content.ContentId

/** Original Emberwatch release-one content. No reference identifiers, prose, art, or balance enter this fixture. */
object OriginalProductCatalog {
    const val CONTENT_VERSION: Int = 1

    val PACK_ID: ContentId = id("pack-emberwatch-release-one")

    val TOWER_RAPID = id("tower-ember-needle")
    val TOWER_BURST = id("tower-sunburst-mortar")
    val TOWER_CONTROL = id("tower-glass-snare")
    val TOWER_SUPPORT = id("tower-seed-forge")

    val ALLY_DEFENDER = id("ally-cinder-guard")
    val ALLY_SKIRMISHER = id("ally-bright-skirmisher")
    val ALLY_RANGED = id("ally-arc-striker")

    val ENEMY_RUNNER = id("enemy-ash-runner")
    val ENEMY_SWARM = id("enemy-cinder-swarm")
    val ENEMY_ARMOR = id("enemy-brass-shell")
    val ENEMY_DISRUPTOR = id("enemy-void-disruptor")
    val ENEMY_SIEGE = id("enemy-siege-bloom")
    val ENEMY_FLYER = id("enemy-sky-shard")
    val BOSS_ORCHID = id("boss-iron-orchid")
    val BOSS_ENGINE = id("boss-night-engine")

    val HERO_REPAIR = id("hero-hearth-repair")
    val HERO_PULSE = id("hero-solar-pulse")

    private val towerIds = listOf(TOWER_RAPID, TOWER_BURST, TOWER_CONTROL, TOWER_SUPPORT)
    private val allyIds = listOf(ALLY_DEFENDER, ALLY_SKIRMISHER, ALLY_RANGED)
    private val ordinaryEnemyIds = listOf(
        ENEMY_RUNNER,
        ENEMY_SWARM,
        ENEMY_ARMOR,
        ENEMY_DISRUPTOR,
        ENEMY_SIEGE,
        ENEMY_FLYER,
    )
    private val heroIds = listOf(HERO_REPAIR, HERO_PULSE)
    private val enhancementIds = listOf(
        "enhancement-keen-sparks",
        "enhancement-quick-coils",
        "enhancement-long-sight",
        "enhancement-radiant-burst",
        "enhancement-glass-frost",
        "enhancement-iron-bloom",
        "enhancement-bright-arms",
        "enhancement-swift-step",
        "enhancement-deep-reserve",
        "enhancement-wide-vault",
        "enhancement-ember-wall",
        "enhancement-heroic-cycle",
    ).map(::id)

    fun releaseOne(): ProductCatalog {
        val towers = listOf(
            ProductTowerDefinition(TOWER_RAPID, ProductTowerRole.RAPID, 35, listOf(24, 38, 58), 6, 3, 7, 1, 3, 250),
            ProductTowerDefinition(TOWER_BURST, ProductTowerRole.BURST, 55, listOf(35, 52, 74), 9, 4, 16, 2, 8, 300, splashRadiusTicks = 85),
            ProductTowerDefinition(TOWER_CONTROL, ProductTowerRole.CONTROL, 45, listOf(30, 46, 68), 3, 2, 12, 1, 6, 270, slowPermille = 300, slowDurationTicks = 30, unlockAfterStageOrdinal = 1),
            ProductTowerDefinition(TOWER_SUPPORT, ProductTowerRole.SUPPORT, 50, listOf(32, 48, 70), 2, 1, 14, 1, 8, 230, supportIncomePerSecond = 4, unlockAfterStageOrdinal = 2),
        ).associateBy(ProductTowerDefinition::id)
        val allies = listOf(
            ProductAllyDefinition(ALLY_DEFENDER, ProductAllyRole.DEFENDER, 34, 46, 5, 4, 26, 10),
            ProductAllyDefinition(ALLY_SKIRMISHER, ProductAllyRole.SKIRMISHER, 28, 25, 7, 7, 18, 7, unlockAfterStageOrdinal = 1),
            ProductAllyDefinition(ALLY_RANGED, ProductAllyRole.RANGED, 38, 20, 9, 5, 120, 12, unlockAfterStageOrdinal = 2),
        ).associateBy(ProductAllyDefinition::id)
        val enemies = listOf(
            ProductEnemyDefinition(ENEMY_RUNNER, ProductEnemyRole.RUNNER, 18, 10, 3, 20, 10, 12, 0, 5),
            ProductEnemyDefinition(ENEMY_SWARM, ProductEnemyRole.SWARM, 12, 8, 2, 18, 8, 8, 0, 4),
            ProductEnemyDefinition(ENEMY_ARMOR, ProductEnemyRole.ARMOR, 42, 5, 6, 22, 14, 18, 3, 9),
            ProductEnemyDefinition(ENEMY_DISRUPTOR, ProductEnemyRole.DISRUPTOR, 28, 7, 5, 80, 18, 14, 1, 8),
            ProductEnemyDefinition(ENEMY_SIEGE, ProductEnemyRole.SIEGE, 55, 4, 9, 45, 20, 22, 2, 12),
            ProductEnemyDefinition(ENEMY_FLYER, ProductEnemyRole.FLYER, 24, 12, 4, 24, 9, 15, 0, 7),
            ProductEnemyDefinition(BOSS_ORCHID, ProductEnemyRole.BOSS, 220, 3, 12, 55, 16, 32, 4, 40, boss = true),
            ProductEnemyDefinition(BOSS_ENGINE, ProductEnemyRole.BOSS, 310, 2, 15, 70, 14, 40, 6, 55, boss = true),
        ).associateBy(ProductEnemyDefinition::id)
        val heroSkills = listOf(
            ProductHeroSkillDefinition(HERO_REPAIR, ProductHeroSkillKind.BASE_REPAIR, cooldownTicks = 360, magnitude = 28),
            ProductHeroSkillDefinition(HERO_PULSE, ProductHeroSkillKind.AREA_PULSE, cooldownTicks = 420, magnitude = 24, unlockAfterStageOrdinal = 3),
        ).associateBy(ProductHeroSkillDefinition::id)
        val enhancementKinds = ProductEnhancementKind.entries
        val enhancements = enhancementIds.mapIndexed { index, enhancementId ->
            ProductEnhancementDefinition(
                id = enhancementId,
                kind = enhancementKinds[index],
                magnitudePermille = when (enhancementKinds[index]) {
                    ProductEnhancementKind.BASE_ARMOR -> 200
                    ProductEnhancementKind.TOWER_COOLDOWN,
                    ProductEnhancementKind.HERO_COOLDOWN,
                    -> 150
                    else -> 250
                },
                maxStacks = 3,
            )
        }.associateBy(ProductEnhancementDefinition::id)
        val stages = buildStages()
        val techNodes = buildTechNodes()
        val rewardTiers = (1..15).map { tier ->
            ProductRewardTierDefinition(
                id = id("reward-tier-$tier"),
                requiredPoints = tier * 10,
                softCurrency = 45L + tier * 15L,
                premiumShaped = if (tier % 5 == 0) 1L else 0L,
                energy = if (tier % 3 == 0) 2 else 0,
            )
        }
        val shopOffers = listOf(
            ProductShopOfferDefinition(id("shop-supply-small"), ProductShopOfferKind.ENERGY, 80, 0, 3, repeatable = true),
            ProductShopOfferDefinition(id("shop-supply-large"), ProductShopOfferKind.ENERGY, 180, 0, 7, repeatable = true),
            ProductShopOfferDefinition(id("shop-rewarded-cache"), ProductShopOfferKind.REWARDED_STUB, null, 0, 0, repeatable = true),
            ProductShopOfferDefinition(id("shop-purchase-cache"), ProductShopOfferKind.PURCHASE_STUB, null, 0, 0, repeatable = true),
        ).associateBy(ProductShopOfferDefinition::id)
        return ProductCatalog(
            packId = PACK_ID,
            contentVersion = CONTENT_VERSION,
            stages = stages.associateBy(ProductStageDefinition::id),
            towers = towers,
            allies = allies,
            enemies = enemies,
            heroSkills = heroSkills,
            enhancements = enhancements,
            techNodes = techNodes.associateBy(ProductTechNodeDefinition::id),
            rewardTiers = rewardTiers,
            shopOffers = shopOffers,
        )
    }

    private fun buildStages(): List<ProductStageDefinition> {
        val stageIds = listOf(
            "stage-ember-path",
            "stage-glass-garden",
            "stage-brass-ravine",
            "stage-pulse-vault",
            "stage-night-orchard",
            "stage-dawn-engine",
        ).map(::id)
        val regions = listOf(id("region-ember"), id("region-orbit"))
        return stageIds.mapIndexed { stageIndex, stageId ->
            val ordinal = stageIndex + 1
            ProductStageDefinition(
                id = stageId,
                regionId = regions[if (stageIndex < 3) 0 else 1],
                ordinal = ordinal,
                energyCost = 2 + stageIndex / 2,
                baseHealth = 96 + stageIndex * 14,
                basePositionTicks = 1_000,
                initialResource = 105 - stageIndex * 3,
                resourceCap = 220 + stageIndex * 20,
                incomePerSecond = 13,
                buildSlots = listOf(210, 410, 610, 810).mapIndexed { slotIndex, position ->
                    ProductBuildSlotDefinition(id("slot-$ordinal-${slotIndex + 1}"), position)
                },
                towerIds = towerIds,
                allyIds = allyIds,
                heroSkillIds = heroIds,
                enhancementPoolIds = enhancementIds,
                enhancementAfterWaves = setOf(2, 4, 6, 8),
                waves = (1..10).map { wave -> buildWave(stageIndex, wave, stageId) },
                reward = ProductStageReward(
                    softCurrency = 90L + stageIndex * 35L,
                    rewardTrackPoints = 10 + stageIndex * 2,
                    firstClearPremiumShaped = 1,
                ),
                unlocksStageId = stageIds.getOrNull(stageIndex + 1),
            )
        }
    }

    private fun buildWave(stageIndex: Int, waveNumber: Int, stageId: ContentId): ProductWaveDefinition {
        val primary = ordinaryEnemyIds[(stageIndex + waveNumber - 1) % ordinaryEnemyIds.size]
        val secondary = ordinaryEnemyIds[(stageIndex + waveNumber + 1) % ordinaryEnemyIds.size]
        val groups = buildList {
            add(
                ProductSpawnGroup(
                    enemyId = primary,
                    count = 3 + waveNumber + stageIndex / 2,
                    firstSpawnTick = 1,
                    intervalTicks = maxOf(6, 18 - waveNumber),
                ),
            )
            if (waveNumber >= 3) {
                add(
                    ProductSpawnGroup(
                        enemyId = secondary,
                        count = 1 + waveNumber / 3,
                        firstSpawnTick = 10,
                        intervalTicks = maxOf(8, 22 - waveNumber),
                    ),
                )
            }
            if (waveNumber == 5 || waveNumber == 10) {
                add(
                    ProductSpawnGroup(
                        enemyId = if ((stageIndex + waveNumber) % 2 == 0) BOSS_ORCHID else BOSS_ENGINE,
                        count = 1,
                        firstSpawnTick = 28,
                        intervalTicks = 1,
                    ),
                )
            }
        }
        return ProductWaveDefinition(
            id = id("wave-${stageId.value.removePrefix("stage-")}-$waveNumber"),
            groups = groups,
        )
    }

    private fun buildTechNodes(): List<ProductTechNodeDefinition> {
        val branches = listOf(
            id("tech-branch-tower") to ProductTechEffectKind.TOWER_POWER,
            id("tech-branch-ally") to ProductTechEffectKind.ALLY_POWER,
            id("tech-branch-economy") to ProductTechEffectKind.ECONOMY,
            id("tech-branch-hero") to ProductTechEffectKind.HERO_POWER,
        )
        return branches.flatMapIndexed { branchIndex, (branchId, effect) ->
            val ids = (1..3).map { depth -> id("tech-${branchIndex + 1}-$depth") }
            ids.mapIndexed { depthIndex, nodeId ->
                ProductTechNodeDefinition(
                    id = nodeId,
                    branchId = branchId,
                    cost = 70L + branchIndex * 20L + depthIndex * 55L,
                    prerequisites = if (depthIndex == 0) emptySet() else setOf(ids[depthIndex - 1]),
                    effectKind = effect,
                    magnitudePermille = 75 + depthIndex * 25,
                )
            }
        }
    }

    private fun id(raw: String): ContentId = ContentId.of(raw)
}
