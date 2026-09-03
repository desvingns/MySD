package dev.mysd.game.content

/**
 * A persistable/replay-safe identifier. Display text, localization keys, and user-facing prose
 * must never enter this type.
 */
@JvmInline
value class ContentId private constructor(val value: String) {
    companion object {
        private const val MAX_LENGTH = 64
        private val FORMAT = Regex("[a-z][a-z0-9]*(?:-[a-z0-9]+)*")

        fun of(raw: String): ContentId {
            if (raw.length > MAX_LENGTH || !FORMAT.matches(raw)) {
                throw InvalidContentIdException(raw)
            }
            return ContentId(raw)
        }
    }
}

/** Stable original MySD identifiers. These names are identifiers only, not display content. */
object OriginalContentIds {
    val FOUNDATION_STAGE = ContentId.of("stage-ember-path")
    val FOUNDATION_BUILDING = ContentId.of("building-seed-forge")
    val FOUNDATION_UNIT = ContentId.of("unit-bright-mote")
    val FOUNDATION_ENEMY = ContentId.of("enemy-ash-sprout")
    val FOUNDATION_ENHANCEMENT = ContentId.of("enhancement-steady-pulse")
    val FOUNDATION_ENHANCEMENT_EMBER_WARD = ContentId.of("enhancement-ember-ward")
    val FOUNDATION_BASE = ContentId.of("base-ember-heart")
    val FOUNDATION_BUILD_SLOT_1 = ContentId.of("build-slot-ash-left")
    val FOUNDATION_BUILD_SLOT_2 = ContentId.of("build-slot-ash-center")
    val FOUNDATION_BUILD_SLOT_3 = ContentId.of("build-slot-ash-right")
    /** The first playable tower reuses the original building namespace entry. */
    val FOUNDATION_TOWER = FOUNDATION_BUILDING
    val FOUNDATION_WAVE = ContentId.of("wave-ember-path-final")

    val FOUNDATION_SCENE = FOUNDATION_STAGE
    val FOUNDATION_BUILD_TILE_1 = FOUNDATION_BUILD_SLOT_1
    val FOUNDATION_BUILD_TILE_2 = FOUNDATION_BUILD_SLOT_2
    val FOUNDATION_BUILD_TILE_3 = FOUNDATION_BUILD_SLOT_3
    val FOUNDATION_ENEMY_FAMILY = FOUNDATION_ENEMY
}

/**
 * The data boundary consumed by the deterministic game layer. The sets are deliberately typed
 * and contain no display names, assets, balance, or UI text.
 */
data class ContentCatalog(
    val contentVersion: Int,
    val stageIds: Set<ContentId>,
    val buildingIds: Set<ContentId>,
    val unitIds: Set<ContentId>,
    val enemyIds: Set<ContentId>,
    val enhancementIds: Set<ContentId>,
    val playableLevel: PlayableLevelContent,
) {
    init {
        ContentCatalogValidator.validate(this)
    }
}

object ContentCatalogValidator {
    fun validate(catalog: ContentCatalog) {
        if (catalog.contentVersion < 1) {
            throw UnsupportedContentVersionException(catalog.contentVersion)
        }
        validateIds(catalog.stageIds, "stageIds")
        validateIds(catalog.buildingIds, "buildingIds")
        validateIds(catalog.unitIds, "unitIds")
        validateIds(catalog.enemyIds, "enemyIds")
        validateIds(catalog.enhancementIds, "enhancementIds")
        PlayableLevelContentValidator.validate(catalog.playableLevel)
        if (catalog.stageIds.isEmpty()) {
            throw MalformedContentFixtureException(
                "Content catalog must contain at least one stage id",
                "stageIds",
            )
        }
        if (catalog.buildingIds.isEmpty()) {
            throw MalformedContentFixtureException(
                "Content catalog must contain at least one building id",
                "buildingIds",
            )
        }
        if (catalog.unitIds.isEmpty()) {
            throw MalformedContentFixtureException(
                "Content catalog must contain at least one unit id",
                "unitIds",
            )
        }
        if (catalog.enemyIds.isEmpty()) {
            throw MalformedContentFixtureException(
                "Content catalog must contain at least one enemy id",
                "enemyIds",
            )
        }
        if (catalog.enhancementIds.isEmpty()) {
            throw MalformedContentFixtureException(
                "Content catalog must contain at least one enhancement id",
                "enhancementIds",
            )
        }
        if (catalog.playableLevel.stageId !in catalog.stageIds) {
            throw MalformedContentFixtureException(
                "Playable level stage must be declared in stageIds",
                "level.stageId",
            )
        }
        if (catalog.playableLevel.tower.id !in catalog.buildingIds) {
            throw MalformedContentFixtureException(
                "Playable level tower must be declared in buildingIds",
                "level.tower.id",
            )
        }
        if (catalog.playableLevel.enemyFamily.id !in catalog.enemyIds) {
            throw MalformedContentFixtureException(
                "Playable level enemy family must be declared in enemyIds",
                "level.enemy.id",
            )
        }
    }

    private fun validateIds(ids: Set<ContentId>, field: String) {
        ids.forEach { id ->
            if (id.value.isEmpty()) {
                throw MalformedContentFixtureException("Blank content id", field)
            }
        }
    }
}

/** The original first playable level fixture used by the Android-free content boundary. */
object OriginalContentFixtures {
    const val CONTENT_VERSION: Int = 2

    fun foundationCatalog(): ContentCatalog = ContentCatalog(
        contentVersion = CONTENT_VERSION,
        stageIds = setOf(OriginalContentIds.FOUNDATION_STAGE),
        buildingIds = setOf(OriginalContentIds.FOUNDATION_BUILDING),
        unitIds = setOf(OriginalContentIds.FOUNDATION_UNIT),
        enemyIds = setOf(OriginalContentIds.FOUNDATION_ENEMY),
        enhancementIds = setOf(
            OriginalContentIds.FOUNDATION_ENHANCEMENT,
            OriginalContentIds.FOUNDATION_ENHANCEMENT_EMBER_WARD,
        ),
        playableLevel = foundationPlayableLevel(),
    )

    fun foundationPlayableLevel(): PlayableLevelContent = PlayableLevelContent(
        stageId = OriginalContentIds.FOUNDATION_STAGE,
        base = MainBaseContent(
            id = OriginalContentIds.FOUNDATION_BASE,
            health = 120,
            positionTicks = 120,
        ),
        buildSlots = listOf(
            BuildTileContent(OriginalContentIds.FOUNDATION_BUILD_SLOT_1, positionTicks = 30),
            BuildTileContent(OriginalContentIds.FOUNDATION_BUILD_SLOT_2, positionTicks = 60),
            BuildTileContent(OriginalContentIds.FOUNDATION_BUILD_SLOT_3, positionTicks = 90),
        ),
        tower = TowerContent(
            id = OriginalContentIds.FOUNDATION_TOWER,
            buildCost = 40,
            damage = 3,
            cooldownTicks = 10,
            rangeTicks = 35,
            upgradeBaseCost = 30,
            upgradeCostStep = 20,
            damageStep = 1,
            cooldownStep = 2,
            minCooldownTicks = 4,
        ),
        enemyFamily = EnemyFamilyContent(
            id = OriginalContentIds.FOUNDATION_ENEMY,
            health = 8,
            speedTicks = 2,
            baseDamage = 12,
        ),
        wave = WaveContent(
            id = OriginalContentIds.FOUNDATION_WAVE,
            enemyFamilyId = OriginalContentIds.FOUNDATION_ENEMY,
            spawnCount = 9,
            spawnIntervalTicks = 20,
        ),
    )
}
