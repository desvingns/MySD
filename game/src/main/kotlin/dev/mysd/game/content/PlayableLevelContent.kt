package dev.mysd.game.content

/**
 * Original, Android-free data for the first playable level. The model deliberately contains
 * stable IDs and numeric parameters only; presentation text and assets are resolved elsewhere.
 */
data class PlayableLevelContent(
    val stageId: ContentId,
    val base: MainBaseContent,
    val buildSlots: List<BuildTileContent>,
    val tower: TowerContent,
    val enemyFamily: EnemyFamilyContent,
    val wave: WaveContent,
) {
    init {
        PlayableLevelContentValidator.validate(this)
    }

    val sceneId: ContentId
        get() = stageId

    val buildTiles: List<BuildTileContent>
        get() = buildSlots

    val enemy: EnemyFamilyContent
        get() = enemyFamily
}

data class MainBaseContent(
    val id: ContentId,
    val health: Int,
    val positionTicks: Int,
) {
    val maxHealth: Int
        get() = health
}

data class BuildTileContent(
    val id: ContentId,
    val positionTicks: Int,
) {
    val pathPositionTicks: Int
        get() = positionTicks
}

data class TowerContent(
    val id: ContentId,
    val buildCost: Int,
    val damage: Int,
    val cooldownTicks: Int,
    val rangeTicks: Int,
    val upgradeBaseCost: Int,
    val upgradeCostStep: Int,
    val damageStep: Int,
    val cooldownStep: Int,
    val minCooldownTicks: Int,
) {
    val baseDamage: Int
        get() = damage
}

data class EnemyFamilyContent(
    val id: ContentId,
    val health: Int,
    val speedTicks: Int,
    val baseDamage: Int,
)

data class WaveContent(
    val id: ContentId,
    val enemyFamilyId: ContentId,
    val spawnCount: Int,
    val spawnIntervalTicks: Int,
) {
    val enemyId: ContentId
        get() = enemyFamilyId
}

typealias BaseContent = MainBaseContent
typealias BuildSlotContent = BuildTileContent
typealias EnemyContent = EnemyFamilyContent

object PlayableLevelContentValidator {
    const val REQUIRED_BUILD_SLOT_COUNT: Int = 3
    const val MIN_WAVE_SPAWN_COUNT: Int = 8
    const val MAX_WAVE_SPAWN_COUNT: Int = 10

    fun validate(level: PlayableLevelContent) {
        validateId(level.stageId, "level.stageId")
        validateId(level.base.id, "level.base.id")
        validateId(level.tower.id, "level.tower.id")
        validateId(level.enemyFamily.id, "level.enemy.id")
        validateId(level.wave.id, "level.wave.id")
        validateId(level.wave.enemyFamilyId, "level.wave.enemyFamilyId")

        if (level.buildSlots.size != REQUIRED_BUILD_SLOT_COUNT) {
            throw MalformedContentFixtureException(
                "Expected exactly $REQUIRED_BUILD_SLOT_COUNT build slots",
                "level.slotCount",
            )
        }

        val declaredIds = linkedMapOf(
            "level.stageId" to level.stageId,
            "level.base.id" to level.base.id,
            "level.tower.id" to level.tower.id,
            "level.enemy.id" to level.enemyFamily.id,
            "level.wave.id" to level.wave.id,
        )
        level.buildSlots.forEachIndexed { index, slot ->
            val path = "level.slot.$index.id"
            validateId(slot.id, path)
            if (declaredIds.put(path, slot.id) != null) {
                throw MalformedContentFixtureException("Duplicate content id", path)
            }
        }
        val seenIds = linkedMapOf<ContentId, String>()
        declaredIds.forEach { (path, id) ->
            val firstPath = seenIds.put(id, path)
            if (firstPath != null) {
                throw MalformedContentFixtureException("Duplicate content id; first declared at $firstPath", path)
            }
        }

        requireNonNegative(level.base.health, "level.base.health")
        requireNonNegative(level.base.positionTicks, "level.base.positionTicks")
        level.buildSlots.forEachIndexed { index, slot ->
            requireNonNegative(slot.positionTicks, "level.slot.$index.positionTicks")
        }
        requireNonNegative(level.tower.buildCost, "level.tower.buildCost")
        requireNonNegative(level.tower.damage, "level.tower.damage")
        requireNonNegative(level.tower.cooldownTicks, "level.tower.cooldownTicks")
        requireNonNegative(level.tower.rangeTicks, "level.tower.rangeTicks")
        requireNonNegative(level.tower.upgradeBaseCost, "level.tower.upgradeBaseCost")
        requireNonNegative(level.tower.upgradeCostStep, "level.tower.upgradeCostStep")
        requireNonNegative(level.tower.damageStep, "level.tower.damageStep")
        requireNonNegative(level.tower.cooldownStep, "level.tower.cooldownStep")
        requireNonNegative(level.tower.minCooldownTicks, "level.tower.minCooldownTicks")
        requireNonNegative(level.enemyFamily.health, "level.enemy.health")
        requireNonNegative(level.enemyFamily.speedTicks, "level.enemy.speedTicks")
        requireNonNegative(level.enemyFamily.baseDamage, "level.enemy.baseDamage")
        if (level.wave.spawnIntervalTicks < 1) {
            throw MalformedContentFixtureException(
                "Wave spawn interval must be at least one tick",
                "level.wave.spawnIntervalTicks",
            )
        }

        if (level.wave.spawnCount !in MIN_WAVE_SPAWN_COUNT..MAX_WAVE_SPAWN_COUNT) {
            throw MalformedContentFixtureException(
                "Wave spawn count must be between $MIN_WAVE_SPAWN_COUNT and $MAX_WAVE_SPAWN_COUNT",
                "level.wave.spawnCount",
            )
        }
        if (level.wave.enemyFamilyId != level.enemyFamily.id) {
            throw MalformedContentFixtureException(
                "Wave references an undeclared enemy family",
                "level.wave.enemyFamilyId",
            )
        }
    }

    private fun validateId(id: ContentId, fieldPath: String) {
        if (id.value.isBlank()) {
            throw MalformedContentFixtureException("Content id must not be blank", fieldPath)
        }
    }

    private fun requireNonNegative(value: Int, fieldPath: String) {
        if (value < 0) {
            throw MalformedContentFixtureException("Value must not be negative", fieldPath)
        }
    }
}
