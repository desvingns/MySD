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
        if (catalog.stageIds.isEmpty()) {
            throw MalformedContentFixtureException("Content catalog must contain at least one stage id")
        }
        if (catalog.buildingIds.isEmpty()) {
            throw MalformedContentFixtureException("Content catalog must contain at least one building id")
        }
        if (catalog.unitIds.isEmpty()) {
            throw MalformedContentFixtureException("Content catalog must contain at least one unit id")
        }
        if (catalog.enemyIds.isEmpty()) {
            throw MalformedContentFixtureException("Content catalog must contain at least one enemy id")
        }
        if (catalog.enhancementIds.isEmpty()) {
            throw MalformedContentFixtureException("Content catalog must contain at least one enhancement id")
        }
    }

    private fun validateIds(ids: Set<ContentId>, field: String) {
        ids.forEach { id ->
            if (id.value.isEmpty()) {
                throw MalformedContentFixtureException("Blank content id in $field")
            }
        }
    }
}

/** A minimal accepted fixture that establishes the original-ID namespace without balance data. */
object OriginalContentFixtures {
    const val CONTENT_VERSION: Int = 1

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
    )
}
