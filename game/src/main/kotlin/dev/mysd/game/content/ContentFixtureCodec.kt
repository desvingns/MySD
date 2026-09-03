package dev.mysd.game.content

/**
 * Deterministic, Android-free fixture wire format. The wire contains stable IDs and numeric
 * parameters only; display text and creative assets intentionally have no representation here.
 */
object ContentFixtureCodec {
    const val CURRENT_SCHEMA_VERSION: Int = 2
    const val CURRENT_CONTENT_VERSION: Int = OriginalContentFixtures.CONTENT_VERSION
    private const val MAGIC = "mysd.content-catalog"
    private const val MAX_ITEMS = 1_024

    fun encode(catalog: ContentCatalog): String {
        ContentCatalogValidator.validate(catalog)
        if (catalog.contentVersion > CURRENT_CONTENT_VERSION) {
            throw FutureContentVersionException(catalog.contentVersion, CURRENT_CONTENT_VERSION)
        }
        if (catalog.contentVersion != CURRENT_CONTENT_VERSION) {
            throw UnsupportedContentVersionException(catalog.contentVersion)
        }
        val fields = linkedMapOf(
            "contentVersion" to catalog.contentVersion.toString(),
            "stageCount" to catalog.stageIds.size.toString(),
        )
        catalog.stageIds.sortedBy(ContentId::value).forEachIndexed { index, id -> fields["stage.$index"] = id.value }
        fields["buildingCount"] = catalog.buildingIds.size.toString()
        catalog.buildingIds.sortedBy(ContentId::value).forEachIndexed { index, id -> fields["building.$index"] = id.value }
        fields["unitCount"] = catalog.unitIds.size.toString()
        catalog.unitIds.sortedBy(ContentId::value).forEachIndexed { index, id -> fields["unit.$index"] = id.value }
        fields["enemyCount"] = catalog.enemyIds.size.toString()
        catalog.enemyIds.sortedBy(ContentId::value).forEachIndexed { index, id -> fields["enemy.$index"] = id.value }
        fields["enhancementCount"] = catalog.enhancementIds.size.toString()
        catalog.enhancementIds.sortedBy(ContentId::value).forEachIndexed { index, id -> fields["enhancement.$index"] = id.value }
        encodePlayableLevel(fields, catalog.playableLevel)
        return buildList {
            add("$MAGIC|catalog")
            add("schemaVersion=$CURRENT_SCHEMA_VERSION")
            fields.forEach { (key, value) -> add("$key=$value") }
        }.joinToString("\n")
    }

    fun decode(input: String): ContentCatalog {
        val document = parse(input)
        val schemaVersion = document["schemaVersion"]!!.toIntOrNull()
            ?: throw MalformedContentFixtureException("Malformed content fixture schema version", "schemaVersion")
        if (schemaVersion > CURRENT_SCHEMA_VERSION) {
            throw FutureContentSchemaVersionException(schemaVersion, CURRENT_SCHEMA_VERSION)
        }
        if (schemaVersion != CURRENT_SCHEMA_VERSION) {
            throw UnsupportedContentSchemaVersionException(schemaVersion)
        }

        val contentVersion = int(document, "contentVersion")
        if (contentVersion > CURRENT_CONTENT_VERSION) {
            throw FutureContentVersionException(contentVersion, CURRENT_CONTENT_VERSION)
        }
        if (contentVersion != CURRENT_CONTENT_VERSION) {
            throw UnsupportedContentVersionException(contentVersion)
        }

        val stageCount = count(document, "stageCount")
        val buildingCount = count(document, "buildingCount")
        val unitCount = count(document, "unitCount")
        val enemyCount = count(document, "enemyCount")
        val enhancementCount = count(document, "enhancementCount")
        val slotCount = count(document, "level.slotCount")
        if (slotCount != PlayableLevelContentValidator.REQUIRED_BUILD_SLOT_COUNT) {
            throw MalformedContentFixtureException(
                "Expected exactly ${PlayableLevelContentValidator.REQUIRED_BUILD_SLOT_COUNT} build slots",
                "level.slotCount",
            )
        }
        val expectedKeys = expectedKeys(
            stageCount,
            buildingCount,
            unitCount,
            enemyCount,
            enhancementCount,
            slotCount,
        )
        if (document.keys != expectedKeys) {
            val missing = (expectedKeys - document.keys).sorted().firstOrNull()
            val unknown = (document.keys - expectedKeys).sorted().firstOrNull()
            if (missing != null) throw MalformedContentFixtureException("Missing content fixture field", missing)
            if (unknown != null) throw MalformedContentFixtureException("Unexpected content fixture field", unknown)
        }
        val stageIds = ids(document, "stage", stageCount)
        val buildingIds = ids(document, "building", buildingCount)
        val unitIds = ids(document, "unit", unitCount)
        val enemyIds = ids(document, "enemy", enemyCount)
        val enhancementIds = ids(document, "enhancement", enhancementCount)
        val playableLevel = PlayableLevelContent(
            stageId = id(document, "level.stageId"),
            base = MainBaseContent(
                id = id(document, "level.base.id"),
                health = int(document, "level.base.health"),
                positionTicks = int(document, "level.base.positionTicks"),
            ),
            buildSlots = (0 until slotCount).map { index ->
                BuildTileContent(
                    id = id(document, "level.slot.$index.id"),
                    positionTicks = int(document, "level.slot.$index.positionTicks"),
                )
            },
            tower = TowerContent(
                id = id(document, "level.tower.id"),
                buildCost = int(document, "level.tower.buildCost"),
                damage = int(document, "level.tower.damage"),
                cooldownTicks = int(document, "level.tower.cooldownTicks"),
                rangeTicks = int(document, "level.tower.rangeTicks"),
                upgradeBaseCost = int(document, "level.tower.upgradeBaseCost"),
                upgradeCostStep = int(document, "level.tower.upgradeCostStep"),
                damageStep = int(document, "level.tower.damageStep"),
                cooldownStep = int(document, "level.tower.cooldownStep"),
                minCooldownTicks = int(document, "level.tower.minCooldownTicks"),
            ),
            enemyFamily = EnemyFamilyContent(
                id = id(document, "level.enemy.id"),
                health = int(document, "level.enemy.health"),
                speedTicks = int(document, "level.enemy.speedTicks"),
                baseDamage = int(document, "level.enemy.baseDamage"),
            ),
            wave = WaveContent(
                id = id(document, "level.wave.id"),
                enemyFamilyId = id(document, "level.wave.enemyFamilyId"),
                spawnCount = int(document, "level.wave.spawnCount"),
                spawnIntervalTicks = int(document, "level.wave.spawnIntervalTicks"),
            ),
        )
        return ContentCatalog(contentVersion, stageIds, buildingIds, unitIds, enemyIds, enhancementIds, playableLevel)
    }

    private fun parse(input: String): Map<String, String> {
        if (input.isEmpty() || input.contains('\r')) {
            throw MalformedContentFixtureException("Content fixture is empty or uses unsupported line endings")
        }
        val lines = input.split('\n')
        if (lines.size < 2 || lines[0] != "$MAGIC|catalog" || !lines[1].startsWith("schemaVersion=")) {
            throw MalformedContentFixtureException("Invalid content fixture header")
        }
        val fields = linkedMapOf<String, String>()
        lines.drop(1).forEach { line ->
            val separator = line.indexOf('=')
            if (separator <= 0) {
                throw MalformedContentFixtureException("Malformed or duplicate content fixture field")
            }
            val key = line.substring(0, separator)
            if (key in fields) {
                throw MalformedContentFixtureException("Duplicate content fixture field", key)
            }
            fields[key] = line.substring(separator + 1)
        }
        return fields
    }

    private fun count(fields: Map<String, String>, key: String): Int {
        val value = int(fields, key)
        if (value !in 0..MAX_ITEMS) throw MalformedContentFixtureException("Invalid item count", key)
        return value
    }

    private fun int(fields: Map<String, String>, key: String): Int =
        fields[key]?.toIntOrNull() ?: throw MalformedContentFixtureException("Malformed integer", key)

    private fun id(fields: Map<String, String>, key: String): ContentId {
        val raw = fields[key] ?: throw MalformedContentFixtureException("Missing content id", key)
        return try {
            ContentId.of(raw)
        } catch (_: InvalidContentIdException) {
            throw MalformedContentFixtureException("Invalid content id", key)
        }
    }

    private fun ids(fields: Map<String, String>, prefix: String, count: Int): Set<ContentId> {
        val expected = (0 until count).map { "$prefix.$it" }.toSet()
        val actual = fields.keys.filter { it.startsWith("$prefix.") }.toSet()
        if (actual != expected) {
            val missing = (expected - actual).sorted().firstOrNull()
            val unknown = (actual - expected).sorted().firstOrNull()
            throw MalformedContentFixtureException(
                "Invalid $prefix fixture fields",
                missing ?: unknown ?: prefix,
            )
        }
        val result = linkedSetOf<ContentId>()
        (0 until count).forEach { index ->
            val key = "$prefix.$index"
            val id = id(fields, key)
            if (!result.add(id)) throw MalformedContentFixtureException("Duplicate $prefix content id", key)
        }
        return result
    }

    private fun encodePlayableLevel(fields: MutableMap<String, String>, level: PlayableLevelContent) {
        fields["level.stageId"] = level.stageId.value
        fields["level.base.id"] = level.base.id.value
        fields["level.base.health"] = level.base.health.toString()
        fields["level.base.positionTicks"] = level.base.positionTicks.toString()
        fields["level.slotCount"] = level.buildSlots.size.toString()
        level.buildSlots.forEachIndexed { index, slot ->
            fields["level.slot.$index.id"] = slot.id.value
            fields["level.slot.$index.positionTicks"] = slot.positionTicks.toString()
        }
        fields["level.tower.id"] = level.tower.id.value
        fields["level.tower.buildCost"] = level.tower.buildCost.toString()
        fields["level.tower.damage"] = level.tower.damage.toString()
        fields["level.tower.cooldownTicks"] = level.tower.cooldownTicks.toString()
        fields["level.tower.rangeTicks"] = level.tower.rangeTicks.toString()
        fields["level.tower.upgradeBaseCost"] = level.tower.upgradeBaseCost.toString()
        fields["level.tower.upgradeCostStep"] = level.tower.upgradeCostStep.toString()
        fields["level.tower.damageStep"] = level.tower.damageStep.toString()
        fields["level.tower.cooldownStep"] = level.tower.cooldownStep.toString()
        fields["level.tower.minCooldownTicks"] = level.tower.minCooldownTicks.toString()
        fields["level.enemy.id"] = level.enemyFamily.id.value
        fields["level.enemy.health"] = level.enemyFamily.health.toString()
        fields["level.enemy.speedTicks"] = level.enemyFamily.speedTicks.toString()
        fields["level.enemy.baseDamage"] = level.enemyFamily.baseDamage.toString()
        fields["level.wave.id"] = level.wave.id.value
        fields["level.wave.enemyFamilyId"] = level.wave.enemyFamilyId.value
        fields["level.wave.spawnCount"] = level.wave.spawnCount.toString()
        fields["level.wave.spawnIntervalTicks"] = level.wave.spawnIntervalTicks.toString()
    }

    private fun expectedKeys(
        stageCount: Int,
        buildingCount: Int,
        unitCount: Int,
        enemyCount: Int,
        enhancementCount: Int,
        slotCount: Int,
    ): Set<String> = buildSet {
        addAll(
            setOf(
                "schemaVersion",
                "contentVersion",
                "stageCount",
                "buildingCount",
                "unitCount",
                "enemyCount",
                "enhancementCount",
                "level.stageId",
                "level.base.id",
                "level.base.health",
                "level.base.positionTicks",
                "level.slotCount",
                "level.tower.id",
                "level.tower.buildCost",
                "level.tower.damage",
                "level.tower.cooldownTicks",
                "level.tower.rangeTicks",
                "level.tower.upgradeBaseCost",
                "level.tower.upgradeCostStep",
                "level.tower.damageStep",
                "level.tower.cooldownStep",
                "level.tower.minCooldownTicks",
                "level.enemy.id",
                "level.enemy.health",
                "level.enemy.speedTicks",
                "level.enemy.baseDamage",
                "level.wave.id",
                "level.wave.enemyFamilyId",
                "level.wave.spawnCount",
                "level.wave.spawnIntervalTicks",
            ),
        )
        repeat(stageCount) { add("stage.$it") }
        repeat(buildingCount) { add("building.$it") }
        repeat(unitCount) { add("unit.$it") }
        repeat(enemyCount) { add("enemy.$it") }
        repeat(enhancementCount) { add("enhancement.$it") }
        repeat(slotCount) {
            add("level.slot.$it.id")
            add("level.slot.$it.positionTicks")
        }
    }
}
