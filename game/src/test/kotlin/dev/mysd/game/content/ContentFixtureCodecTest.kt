package dev.mysd.game.content

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ContentFixtureCodecTest {
    @Test
    fun acceptedOriginalFixtureRoundTripsAndIsCanonical() {
        val catalog = OriginalContentFixtures.foundationCatalog()

        val encoded = ContentFixtureCodec.encode(catalog)

        assertEquals(catalog, ContentFixtureCodec.decode(encoded))
        assertEquals(encoded, ContentFixtureCodec.encode(ContentFixtureCodec.decode(encoded)))
        assertTrue(encoded.contains("schemaVersion=2"))
        assertTrue(encoded.contains("contentVersion=2"))
        assertEquals(2, OriginalContentFixtures.CONTENT_VERSION)
        assertEquals(ContentFixtureCodec.CURRENT_CONTENT_VERSION, OriginalContentFixtures.CONTENT_VERSION)
        assertEquals(1, catalog.stageIds.size)
        assertEquals(1, catalog.buildingIds.size)
        assertEquals(1, catalog.enemyIds.size)
        assertEquals(OriginalContentIds.FOUNDATION_BASE, catalog.playableLevel.base.id)
        assertEquals(OriginalContentIds.FOUNDATION_TOWER, catalog.playableLevel.tower.id)
        assertEquals(OriginalContentIds.FOUNDATION_ENEMY_FAMILY, catalog.playableLevel.enemyFamily.id)
        assertEquals(3, catalog.playableLevel.buildSlots.size)
        assertEquals(9, catalog.playableLevel.wave.spawnCount)
        assertEquals(catalog.playableLevel.enemyFamily.id, catalog.playableLevel.wave.enemyFamilyId)
        assertTrue(catalog.playableLevel.stageId in catalog.stageIds)
        assertTrue(catalog.playableLevel.tower.id in catalog.buildingIds)
        assertTrue(catalog.playableLevel.enemyFamily.id in catalog.enemyIds)
        val stableIds = catalog.stageIds + catalog.buildingIds + catalog.unitIds + catalog.enemyIds +
            catalog.enhancementIds + listOf(
            catalog.playableLevel.stageId,
            catalog.playableLevel.base.id,
            catalog.playableLevel.tower.id,
            catalog.playableLevel.enemyFamily.id,
            catalog.playableLevel.wave.id,
        ) + catalog.playableLevel.buildSlots.map { it.id }
        assertTrue(stableIds.all { it.value == it.value.lowercase() })
    }

    @Test
    fun canonicalEncodingIsIndependentOfSetInsertionOrderAndPresentationFields() {
        val catalog = OriginalContentFixtures.foundationCatalog()
        val reordered = catalog.copy(
            enhancementIds = linkedSetOf(
                OriginalContentIds.FOUNDATION_ENHANCEMENT_EMBER_WARD,
                OriginalContentIds.FOUNDATION_ENHANCEMENT,
            ),
        )

        val encoded = ContentFixtureCodec.encode(catalog)

        assertEquals(encoded, ContentFixtureCodec.encode(reordered))
        assertFalse(encoded.contains("display"))
        assertFalse(encoded.contains("asset"))
    }

    @Test
    fun contentIdsRejectBlankInvalidAndTooLongValues() {
        listOf("", " ", "Stage One", "stage_one", "stage/one", "A-stage", "a".repeat(65)).forEach { raw ->
            assertFailsWith<InvalidContentIdException> { ContentId.of(raw) }
        }
        assertEquals("stage-ember-path", ContentId.of("stage-ember-path").value)
    }

    @Test
    fun malformedFixtureAndDuplicateIdsAreRejected() {
        val encoded = ContentFixtureCodec.encode(OriginalContentFixtures.foundationCatalog())

        val blankError = assertFailsWith<MalformedContentFixtureException> {
            ContentFixtureCodec.decode(encoded.replace("stage.0=stage-ember-path", "stage.0="))
        }
        assertEquals("stage.0", blankError.fieldPath)

        val duplicateError = assertFailsWith<MalformedContentFixtureException> {
            ContentFixtureCodec.decode(encoded.replace("stageCount=1", "stageCount=2") + "\nstage.1=stage-ember-path")
        }
        assertEquals("stage.1", duplicateError.fieldPath)

        val missingError = assertFailsWith<MalformedContentFixtureException> {
            ContentFixtureCodec.decode(encoded.replace("stageCount=1", "stageCount=2"))
        }
        assertEquals("stage.1", missingError.fieldPath)
    }

    @Test
    fun futureSchemaAndContentVersionsFailExplicitly() {
        val catalog = OriginalContentFixtures.foundationCatalog()
        val encoded = ContentFixtureCodec.encode(catalog)

        val futureSchema = encoded.replace("schemaVersion=2", "schemaVersion=3")
        val schemaError = assertFailsWith<FutureContentSchemaVersionException> {
            ContentFixtureCodec.decode(futureSchema)
        }
        assertEquals(3, schemaError.version)

        val futureContent = encoded.replace("contentVersion=2", "contentVersion=3")
        val contentError = assertFailsWith<FutureContentVersionException> {
            ContentFixtureCodec.decode(futureContent)
        }
        assertEquals(3, contentError.version)

        assertFailsWith<FutureContentVersionException> {
            ContentFixtureCodec.encode(catalog.copy(contentVersion = 3))
        }
    }

    @Test
    fun olderSchemaAndContentVersionsAreRejectedAsUnsupported() {
        val encoded = ContentFixtureCodec.encode(OriginalContentFixtures.foundationCatalog())

        assertFailsWith<UnsupportedContentSchemaVersionException> {
            ContentFixtureCodec.decode(encoded.replace("schemaVersion=2", "schemaVersion=1"))
        }
        assertFailsWith<UnsupportedContentVersionException> {
            ContentFixtureCodec.decode(encoded.replace("contentVersion=2", "contentVersion=1"))
        }
        assertFailsWith<UnsupportedContentVersionException> {
            ContentFixtureCodec.encode(OriginalContentFixtures.foundationCatalog().copy(contentVersion = 1))
        }
    }

    @Test
    fun fixtureRejectsUnknownAndMissingFieldsDeterministically() {
        val encoded = ContentFixtureCodec.encode(OriginalContentFixtures.foundationCatalog())

        val missing = encoded.lineSequence().filterNot { it == "enemyCount=1" }.joinToString("\n")
        val missingError = assertFailsWith<MalformedContentFixtureException> {
            ContentFixtureCodec.decode(missing)
        }
        assertEquals("enemyCount", missingError.fieldPath)

        val unknown = "$encoded\nunknown=value"
        val unknownError = assertFailsWith<MalformedContentFixtureException> {
            ContentFixtureCodec.decode(unknown)
        }
        assertEquals("unknown", unknownError.fieldPath)
    }

    @Test
    fun levelFixtureRejectsDuplicateIdsWithFieldPath() {
        val encoded = ContentFixtureCodec.encode(OriginalContentFixtures.foundationCatalog())

        val error = assertFailsWith<MalformedContentFixtureException> {
            ContentFixtureCodec.decode(
                encoded.replace(
                    "level.slot.1.id=build-slot-ash-center",
                    "level.slot.1.id=build-slot-ash-left",
                ),
            )
        }

        assertEquals("level.slot.1.id", error.fieldPath)
        assertTrue(error.message.orEmpty().contains("level.slot.1.id"))
    }

    @Test
    fun levelFixtureRejectsNegativeValuesWithFieldPath() {
        val encoded = ContentFixtureCodec.encode(OriginalContentFixtures.foundationCatalog())

        val numericFields = listOf(
            "level.base.health" to 120,
            "level.base.positionTicks" to 120,
            "level.slot.0.positionTicks" to 30,
            "level.slot.1.positionTicks" to 60,
            "level.slot.2.positionTicks" to 90,
            "level.tower.buildCost" to 40,
            "level.tower.damage" to 3,
            "level.tower.cooldownTicks" to 10,
            "level.tower.rangeTicks" to 35,
            "level.tower.upgradeBaseCost" to 30,
            "level.tower.upgradeCostStep" to 20,
            "level.tower.damageStep" to 1,
            "level.tower.cooldownStep" to 2,
            "level.tower.minCooldownTicks" to 4,
            "level.enemy.health" to 8,
            "level.enemy.speedTicks" to 2,
            "level.enemy.baseDamage" to 12,
            "level.wave.spawnCount" to 9,
            "level.wave.spawnIntervalTicks" to 20,
        )

        numericFields.forEach { (fieldPath, currentValue) ->
            val error = assertFailsWith<MalformedContentFixtureException> {
                ContentFixtureCodec.decode(
                    fixtureWithField(encoded, fieldPath, "-1", currentValue.toString()),
                )
            }
            assertEquals(fieldPath, error.fieldPath)
        }
    }

    @Test
    fun levelFixtureRejectsWrongSlotCountAndWaveSizeWithFieldPaths() {
        val encoded = ContentFixtureCodec.encode(OriginalContentFixtures.foundationCatalog())
        val withoutThirdSlot = encoded.lineSequence()
            .filterNot { it.startsWith("level.slot.2.") }
            .joinToString("\n")

        listOf(
            withoutThirdSlot.replace("level.slotCount=3", "level.slotCount=2"),
            encoded.replace("level.slotCount=3", "level.slotCount=4"),
        ).forEach { fixture ->
            val slotError = assertFailsWith<MalformedContentFixtureException> {
                ContentFixtureCodec.decode(fixture)
            }
            assertEquals("level.slotCount", slotError.fieldPath)
        }

        listOf(7, 11).forEach { invalidCount ->
            val waveError = assertFailsWith<MalformedContentFixtureException> {
                ContentFixtureCodec.decode(
                    fixtureWithField(encoded, "level.wave.spawnCount", invalidCount.toString(), "9"),
                )
            }
            assertEquals("level.wave.spawnCount", waveError.fieldPath)
        }
    }

    @Test
    fun finiteWaveAcceptsBothConfiguredBoundaryCounts() {
        val encoded = ContentFixtureCodec.encode(OriginalContentFixtures.foundationCatalog())

        listOf(8, 10).forEach { spawnCount ->
            val decoded = ContentFixtureCodec.decode(
                fixtureWithField(encoded, "level.wave.spawnCount", spawnCount.toString(), "9"),
            )
            assertEquals(spawnCount, decoded.playableLevel.wave.spawnCount)
        }
    }

    @Test
    fun levelFixtureRejectsBlankAndInvalidIdsWithFieldPaths() {
        val encoded = ContentFixtureCodec.encode(OriginalContentFixtures.foundationCatalog())
        val invalidIds = listOf(
            "stage.0" to "",
            "building.0" to "Building One",
            "unit.0" to "unit_bright_mote",
            "enemy.0" to "enemy/ash-sprout",
            "enhancement.0" to "enhancement ember ward",
            "level.stageId" to "stage/ember-path",
            "level.base.id" to "Base",
            "level.slot.0.id" to "",
            "level.tower.id" to "tower id",
            "level.enemy.id" to "enemy_id",
            "level.wave.id" to "wave/id",
            "level.wave.enemyFamilyId" to "Enemy Family",
        )

        invalidIds.forEach { (fieldPath, invalidValue) ->
            val error = assertFailsWith<MalformedContentFixtureException> {
                ContentFixtureCodec.decode(fixtureWithField(encoded, fieldPath, invalidValue))
            }
            assertEquals(fieldPath, error.fieldPath)
        }
    }

    @Test
    fun fixtureRejectsUnknownAndMissingNestedFieldsDeterministically() {
        val encoded = ContentFixtureCodec.encode(OriginalContentFixtures.foundationCatalog())

        val missing = encoded.lineSequence()
            .filterNot { it.startsWith("level.tower.damage=") }
            .joinToString("\n")
        val missingError = assertFailsWith<MalformedContentFixtureException> {
            ContentFixtureCodec.decode(missing)
        }
        assertEquals("level.tower.damage", missingError.fieldPath)

        val unknownError = assertFailsWith<MalformedContentFixtureException> {
            ContentFixtureCodec.decode("$encoded\nlevel.base.displayName=forbidden")
        }
        assertEquals("level.base.displayName", unknownError.fieldPath)
    }

    private fun fixtureWithField(
        encoded: String,
        fieldPath: String,
        replacement: String,
        expectedValue: String? = null,
    ): String {
        val currentValue = expectedValue ?: encoded.lineSequence()
            .first { it.startsWith("$fieldPath=") }
            .substringAfter('=')
        return encoded.replace("$fieldPath=$currentValue", "$fieldPath=$replacement")
    }
}
