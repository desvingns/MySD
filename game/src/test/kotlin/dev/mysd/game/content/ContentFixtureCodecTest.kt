package dev.mysd.game.content

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
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
        assertEquals(1, catalog.stageIds.size)
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
        val encoded = ContentFixtureCodec.encode(OriginalContentFixtures.foundationCatalog())

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

        val error = assertFailsWith<MalformedContentFixtureException> {
            ContentFixtureCodec.decode(encoded.replace("level.base.health=120", "level.base.health=-1"))
        }

        assertEquals("level.base.health", error.fieldPath)
    }

    @Test
    fun levelFixtureRejectsWrongSlotCountAndWaveSizeWithFieldPaths() {
        val encoded = ContentFixtureCodec.encode(OriginalContentFixtures.foundationCatalog())
        val withoutThirdSlot = encoded.lineSequence()
            .filterNot { it.startsWith("level.slot.2.") }
            .joinToString("\n")
        val wrongSlotCount = withoutThirdSlot.replace("level.slotCount=3", "level.slotCount=2")

        val slotError = assertFailsWith<MalformedContentFixtureException> {
            ContentFixtureCodec.decode(wrongSlotCount)
        }
        assertEquals("level.slotCount", slotError.fieldPath)

        val waveError = assertFailsWith<MalformedContentFixtureException> {
            ContentFixtureCodec.decode(encoded.replace("level.wave.spawnCount=9", "level.wave.spawnCount=11"))
        }
        assertEquals("level.wave.spawnCount", waveError.fieldPath)
    }
}
