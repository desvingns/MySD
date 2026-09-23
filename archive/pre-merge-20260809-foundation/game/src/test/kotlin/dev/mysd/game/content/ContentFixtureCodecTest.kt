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
        assertTrue(encoded.contains("schemaVersion=1"))
        assertTrue(encoded.contains("contentVersion=1"))
        val stableIds = catalog.stageIds + catalog.buildingIds + catalog.unitIds + catalog.enemyIds + catalog.enhancementIds
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

        assertFailsWith<MalformedContentFixtureException> {
            ContentFixtureCodec.decode(encoded.replace("stage.0=stage-ember-path", "stage.0="))
        }
        assertFailsWith<MalformedContentFixtureException> {
            ContentFixtureCodec.decode(encoded.replace("stageCount=1", "stageCount=2") + "\nstage.1=stage-ember-path")
        }
        assertFailsWith<MalformedContentFixtureException> {
            ContentFixtureCodec.decode(encoded.replace("stageCount=1", "stageCount=2"))
        }
    }

    @Test
    fun futureSchemaAndContentVersionsFailExplicitly() {
        val encoded = ContentFixtureCodec.encode(OriginalContentFixtures.foundationCatalog())

        val futureSchema = encoded.replace("schemaVersion=1", "schemaVersion=2")
        val schemaError = assertFailsWith<FutureContentSchemaVersionException> {
            ContentFixtureCodec.decode(futureSchema)
        }
        assertEquals(2, schemaError.version)

        val futureContent = encoded.replace("contentVersion=1", "contentVersion=2")
        val contentError = assertFailsWith<FutureContentVersionException> {
            ContentFixtureCodec.decode(futureContent)
        }
        assertEquals(2, contentError.version)
    }

    @Test
    fun fixtureRejectsUnknownAndMissingFieldsDeterministically() {
        val encoded = ContentFixtureCodec.encode(OriginalContentFixtures.foundationCatalog())

        val missing = encoded.lineSequence().filterNot { it == "enemyCount=1" }.joinToString("\n")
        assertFailsWith<MalformedContentFixtureException> { ContentFixtureCodec.decode(missing) }

        val unknown = "$encoded\nunknown=value"
        assertFailsWith<MalformedContentFixtureException> { ContentFixtureCodec.decode(unknown) }
    }
}
