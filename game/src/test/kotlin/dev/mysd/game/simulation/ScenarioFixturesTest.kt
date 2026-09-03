package dev.mysd.game.simulation

import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.readText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class ScenarioFixturesTest {
    @Test
    fun catalogIsCompleteAndUsesExactlyTheAcceptedUniqueIds() {
        val catalog = ScenarioFixtures.catalog(seed = 17L)

        assertEquals(5, catalog.fixtures.size)
        assertEquals(
            setOf(
                "fixture_level_1_setup",
                "fixture_early_wave",
                "fixture_enhancement_choice",
                "fixture_safe_victory",
                "fixture_defeat_blocker",
            ),
            catalog.fixtures.map(ScenarioFixture::id).toSet(),
        )
        assertEquals(5, catalog.fixtures.map(ScenarioFixture::kind).toSet().size)
        assertEquals(5, catalog.fixtures.map(ScenarioFixture::phase).toSet().size)
        assertEquals(
            ScenarioFixtureKind.entries.toSet(),
            catalog.fixtures.map(ScenarioFixture::kind).toSet(),
        )
        assertEquals(
            mapOf(
                "fixture_level_1_setup" to listOf("ST-0002"),
                "fixture_early_wave" to listOf("ST-0003"),
                "fixture_enhancement_choice" to listOf("ST-0004"),
                "fixture_safe_victory" to listOf("ST-0005"),
                "fixture_defeat_blocker" to listOf("ST-0003", "ED-0025"),
            ),
            catalog.fixtures.associate { it.id to it.evidenceRefs },
        )
    }

    @Test
    fun sameSeedProducesIdenticalCanonicalFixtureCatalogAndHash() {
        val first = ScenarioFixtures.catalog(seed = 23L)
        val second = ScenarioFixtures.catalog(seed = 23L)

        assertEquals(first, second)
        assertEquals(first.canonicalEncoding, second.canonicalEncoding)
        assertEquals(first.hash, second.hash)
        assertTrue(first.fixtures.zip(second.fixtures).all { (left, right) -> left.hash == right.hash })
    }

    @Test
    fun differentSeedParticipatesInCanonicalEncodingAndHash() {
        val first = ScenarioFixtures.catalog(seed = 23L)
        val second = ScenarioFixtures.catalog(seed = 24L)

        assertNotEquals(first.canonicalEncoding, second.canonicalEncoding)
        assertNotEquals(first.hash, second.hash)
        assertTrue(first.fixtures.zip(second.fixtures).all { (left, right) -> left.id == right.id })
        assertTrue(first.fixtures.zip(second.fixtures).all { (left, right) -> left.hash != right.hash })
    }

    @Test
    fun defeatFixtureIsPlayableAndTerminalWhileKeepingItsStableId() {
        val defeat = ScenarioFixtures.catalog(seed = 23L).fixtures.single {
            it.kind == ScenarioFixtureKind.DEFEAT
        }

        assertEquals("fixture_defeat_blocker", defeat.id)
        assertEquals(ScenarioPhase.DEFEAT, defeat.phase)
        assertEquals(ScenarioTerminalClassification.DEFEAT, defeat.terminalClassification)
        assertTrue(defeat.terminalClassification.isTerminal)
        assertEquals(ScenarioPlayability.PLAYABLE, defeat.playability)
        assertEquals(listOf("ST-0003", "ED-0025"), defeat.evidenceRefs)
        assertTrue(ScenarioFixtures.catalog(seed = 23L).fixtures.any {
            it.terminalClassification == ScenarioTerminalClassification.DEFEAT
        })
    }

    @Test
    fun terminalScenarioFixturesArePositivePlayableVictoryAndDefeatOutcomes() {
        val fixtures = ScenarioFixtures.catalog(seed = 23L).fixtures
            .filter { it.terminalClassification.isTerminal }

        assertEquals(
            setOf(ScenarioFixtureKind.VICTORY, ScenarioFixtureKind.DEFEAT),
            fixtures.map(ScenarioFixture::kind).toSet(),
        )
        assertTrue(fixtures.all { it.playability == ScenarioPlayability.PLAYABLE })
        assertEquals(
            mapOf(
                "fixture_safe_victory" to ScenarioTerminalClassification.VICTORY,
                "fixture_defeat_blocker" to ScenarioTerminalClassification.DEFEAT,
            ),
            fixtures.associate { it.id to it.terminalClassification },
        )
    }

    @Test
    fun scenarioFixturesRemainAndroidFree() {
        val sourceRoot = sequenceOf(
            Path("src/main/kotlin/dev/mysd/game/simulation"),
            Path("game/src/main/kotlin/dev/mysd/game/simulation"),
        ).first { it.exists() && it.isDirectory() }
        val source = sourceRoot.resolve("ScenarioFixtures.kt").readText()

        assertFalse(Regex("(?m)^import\\s+android(\\.|x\\.)").containsMatchIn(source))
        assertTrue(Path("build.gradle.kts").exists() || Path("game/build.gradle.kts").exists())
    }
}
