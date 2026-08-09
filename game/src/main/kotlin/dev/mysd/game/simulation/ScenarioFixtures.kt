package dev.mysd.game.simulation

import dev.myengine.core.stableHashOf

/** The five accepted, stable scenario fixture kinds for the headless vertical slice. */
enum class ScenarioFixtureKind(
    val stableId: String,
    val phase: ScenarioPhase,
    val terminalClassification: ScenarioTerminalClassification,
    val playability: ScenarioPlayability,
    val evidenceRefs: List<String>,
) {
    SETUP(
        stableId = "fixture_level_1_setup",
        phase = ScenarioPhase.SETUP,
        terminalClassification = ScenarioTerminalClassification.NON_TERMINAL,
        playability = ScenarioPlayability.PLAYABLE,
        evidenceRefs = listOf("ST-0002"),
    ),
    ACTIVE_WAVE(
        stableId = "fixture_early_wave",
        phase = ScenarioPhase.ACTIVE_WAVE,
        terminalClassification = ScenarioTerminalClassification.NON_TERMINAL,
        playability = ScenarioPlayability.PLAYABLE,
        evidenceRefs = listOf("ST-0003"),
    ),
    ENHANCEMENT_CHOICE(
        stableId = "fixture_enhancement_choice",
        phase = ScenarioPhase.ENHANCEMENT_CHOICE,
        terminalClassification = ScenarioTerminalClassification.NON_TERMINAL,
        playability = ScenarioPlayability.PLAYABLE,
        evidenceRefs = listOf("ST-0004"),
    ),
    VICTORY(
        stableId = "fixture_safe_victory",
        phase = ScenarioPhase.VICTORY,
        terminalClassification = ScenarioTerminalClassification.VICTORY,
        playability = ScenarioPlayability.PLAYABLE,
        evidenceRefs = listOf("ST-0005"),
    ),
    STRUCTURED_DEFEAT_BLOCKER(
        stableId = "fixture_defeat_blocker",
        phase = ScenarioPhase.STRUCTURED_DEFEAT_BLOCKER,
        terminalClassification = ScenarioTerminalClassification.STRUCTURED_BLOCKER,
        playability = ScenarioPlayability.BLOCKED,
        evidenceRefs = listOf("ST-0003", "ED-0025"),
    ),
}

enum class ScenarioPhase {
    SETUP,
    ACTIVE_WAVE,
    ENHANCEMENT_CHOICE,
    VICTORY,
    STRUCTURED_DEFEAT_BLOCKER,
}

enum class ScenarioTerminalClassification(val isTerminal: Boolean) {
    NON_TERMINAL(isTerminal = false),
    VICTORY(isTerminal = true),
    STRUCTURED_BLOCKER(isTerminal = false),
}

enum class ScenarioPlayability {
    PLAYABLE,
    BLOCKED,
}

/**
 * An Android-free, seed-bound descriptor for one accepted scenario contour.
 *
 * This type intentionally contains no gameplay state. In particular, a structured blocker is
 * not represented as a defeat result and cannot imply unobserved defeat mechanics.
 */
data class ScenarioFixture(
    val kind: ScenarioFixtureKind,
    val seed: Long,
) {
    val id: String
        get() = kind.stableId

    val phase: ScenarioPhase
        get() = kind.phase

    val terminalClassification: ScenarioTerminalClassification
        get() = kind.terminalClassification

    val playability: ScenarioPlayability
        get() = kind.playability

    val evidenceRefs: List<String>
        get() = kind.evidenceRefs

    val canonicalEncoding: String = listOf(
        "mysd.scenario-fixture|schema=1",
        "id=$id",
        "kind=${kind.name}",
        "seed=$seed",
        "phase=${phase.name}",
        "terminal=${terminalClassification.name}",
        "playability=${playability.name}",
        "evidence=${evidenceRefs.joinToString(",")}",
    ).joinToString("\n")

    val hash: String = stableHashOf {
        add("mysd.scenario-fixture.hash.v1")
        add(canonicalEncoding)
    }
}

/** A complete, canonical catalog of the accepted scenario fixtures for one explicit seed. */
data class ScenarioFixtureCatalog(
    val seed: Long,
    val fixtures: List<ScenarioFixture>,
    val canonicalEncoding: String,
    val hash: String,
) {
    init {
        require(fixtures.size == ScenarioFixtures.ACCEPTED_FIXTURE_KINDS.size) {
            "Scenario fixture catalog is incomplete."
        }
        require(fixtures.map(ScenarioFixture::kind).toSet() == ScenarioFixtures.ACCEPTED_FIXTURE_KINDS.toSet()) {
            "Scenario fixture catalog contains an unknown or duplicate kind."
        }
        require(fixtures.map(ScenarioFixture::id).toSet().size == fixtures.size) {
            "Scenario fixture ids must be unique."
        }
        require(fixtures.all { it.seed == seed }) {
            "Scenario fixture seeds must match the catalog seed."
        }
    }
}

/** Factory for the accepted semantic fixture catalog. */
object ScenarioFixtures {
    internal val ACCEPTED_FIXTURE_KINDS: List<ScenarioFixtureKind> =
        ScenarioFixtureKind.entries.toList()

    fun catalog(seed: Long): ScenarioFixtureCatalog {
        val fixtures = ACCEPTED_FIXTURE_KINDS.map { ScenarioFixture(kind = it, seed = seed) }
        val canonicalEncoding = buildString {
            appendLine("mysd.scenario-fixtures|schema=1")
            appendLine("seed=$seed")
            appendLine("fixtureCount=${fixtures.size}")
            fixtures.forEachIndexed { index, fixture ->
                appendLine("fixture.$index=${fixture.canonicalEncoding.replace('\n', '|')}")
            }
        }.removeSuffix("\n")
        val hash = stableHashOf {
            add("mysd.scenario-fixtures.hash.v1")
            add(canonicalEncoding)
        }
        return ScenarioFixtureCatalog(
            seed = seed,
            fixtures = fixtures,
            canonicalEncoding = canonicalEncoding,
            hash = hash,
        )
    }
}
