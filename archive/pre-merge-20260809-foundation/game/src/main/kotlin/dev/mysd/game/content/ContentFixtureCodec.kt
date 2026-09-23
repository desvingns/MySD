package dev.mysd.game.content

/**
 * Deterministic, Android-free fixture wire format. IDs are the only content values on the wire;
 * display text and creative assets intentionally have no representation here.
 */
object ContentFixtureCodec {
    const val CURRENT_SCHEMA_VERSION: Int = 1
    const val CURRENT_CONTENT_VERSION: Int = 1
    private const val MAGIC = "mysd.content-catalog"
    private const val MAX_ITEMS = 1_024

    fun encode(catalog: ContentCatalog): String {
        ContentCatalogValidator.validate(catalog)
        if (catalog.contentVersion > CURRENT_CONTENT_VERSION) {
            throw FutureContentVersionException(catalog.contentVersion, CURRENT_CONTENT_VERSION)
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
        return buildList {
            add("$MAGIC|catalog")
            add("schemaVersion=$CURRENT_SCHEMA_VERSION")
            fields.forEach { (key, value) -> add("$key=$value") }
        }.joinToString("\n")
    }

    fun decode(input: String): ContentCatalog {
        val document = parse(input)
        val schemaVersion = document["schemaVersion"]!!.toIntOrNull()
            ?: throw MalformedContentFixtureException("Malformed content fixture schema version")
        if (schemaVersion > CURRENT_SCHEMA_VERSION) {
            throw FutureContentSchemaVersionException(schemaVersion, CURRENT_SCHEMA_VERSION)
        }
        if (schemaVersion < 1) throw UnsupportedContentSchemaVersionException(schemaVersion)

        val contentVersion = int(document, "contentVersion")
        if (contentVersion > CURRENT_CONTENT_VERSION) {
            throw FutureContentVersionException(contentVersion, CURRENT_CONTENT_VERSION)
        }
        if (contentVersion < 1) throw UnsupportedContentVersionException(contentVersion)

        val stageCount = count(document, "stageCount")
        val buildingCount = count(document, "buildingCount")
        val unitCount = count(document, "unitCount")
        val enemyCount = count(document, "enemyCount")
        val enhancementCount = count(document, "enhancementCount")
        val expectedKeys = buildSet {
            addAll(setOf("schemaVersion", "contentVersion", "stageCount", "buildingCount", "unitCount", "enemyCount", "enhancementCount"))
            repeat(stageCount) { add("stage.$it") }
            repeat(buildingCount) { add("building.$it") }
            repeat(unitCount) { add("unit.$it") }
            repeat(enemyCount) { add("enemy.$it") }
            repeat(enhancementCount) { add("enhancement.$it") }
        }
        if (document.keys != expectedKeys) throw MalformedContentFixtureException("Unexpected content fixture field")
        val stageIds = ids(document, "stage", stageCount)
        val buildingIds = ids(document, "building", buildingCount)
        val unitIds = ids(document, "unit", unitCount)
        val enemyIds = ids(document, "enemy", enemyCount)
        val enhancementIds = ids(document, "enhancement", enhancementCount)
        return ContentCatalog(contentVersion, stageIds, buildingIds, unitIds, enemyIds, enhancementIds)
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
            if (separator <= 0 || line.substring(0, separator) in fields) {
                throw MalformedContentFixtureException("Malformed or duplicate content fixture field")
            }
            fields[line.substring(0, separator)] = line.substring(separator + 1)
        }
        val required = setOf(
            "schemaVersion", "contentVersion", "stageCount", "buildingCount", "unitCount", "enemyCount", "enhancementCount",
        )
        if (!fields.keys.containsAll(required)) {
            throw MalformedContentFixtureException("Missing content fixture field")
        }
        return fields
    }

    private fun count(fields: Map<String, String>, key: String): Int {
        val value = int(fields, key)
        if (value !in 0..MAX_ITEMS) throw MalformedContentFixtureException("Invalid item count in $key")
        return value
    }

    private fun int(fields: Map<String, String>, key: String): Int =
        fields[key]?.toIntOrNull() ?: throw MalformedContentFixtureException("Malformed integer in $key")

    private fun ids(fields: Map<String, String>, prefix: String, count: Int): Set<ContentId> {
        val expected = (0 until count).map { "$prefix.$it" }.toSet()
        val actual = fields.keys.filter { it.startsWith("$prefix.") }.toSet()
        if (actual != expected) throw MalformedContentFixtureException("Invalid $prefix fixture fields")
        val result = linkedSetOf<ContentId>()
        (0 until count).forEach { index ->
            val raw = fields["$prefix.$index"]
                ?: throw MalformedContentFixtureException("Missing $prefix.$index")
            val id = try {
                ContentId.of(raw)
            } catch (error: InvalidContentIdException) {
                throw MalformedContentFixtureException("Invalid $prefix.$index content id")
            }
            if (!result.add(id)) throw MalformedContentFixtureException("Duplicate $prefix content id")
        }
        return result
    }
}
