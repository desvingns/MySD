# Первый playable level — run-save codec and state payload

Status: done
TASK: feature
PLATFORM: android
WHAT: Добавить в versioned `RunSave` явный Android-free payload полного `PlayableBattleState`, чтобы playable run сохранял ресурс, башни, апгрейды, врагов, base HP, wave counters, tick и terminal defeat без опоры на contour-only markers.
LAYERS: domain, data
CHANGED_HINT:
- `game/src/main/kotlin/dev/mysd/game/persistence/RunSave.kt` — расширить versioned payload полным playable battle state и explicit absent-payload compatibility branch — G7
- `game/src/main/kotlin/dev/mysd/game/persistence/PersistenceWire.kt` — поддержать exact-key validation для nested state fields и typed malformed errors — G7
- `game/src/test/kotlin/dev/mysd/game/persistence/PersistenceCodecTest.kt` — закрыть round-trip, terminal invariants и legacy-safe migration cases — G7
TEST_TYPES: unit, persistence, migration
CONSTRAINTS:
- RunSave remains separate from ProfileStore; profile currencies are not silently used as active-run state — G7.
- Existing supported schema migrations remain valid; malformed and future versions reject safely — G7.
- Legacy v1–v3 saves decode safely, but they must not synthesize missing `PlayableBattleState` data — G7.
- Payload carries only Android-free authoritative battle state plus deterministic save metadata; no Activity, Compose, screenshot, bounds or renderer state enters the document — G1, G5, G7.
- Defeat payload remains terminal at the codec boundary; active payload cannot encode a terminal result, and invalid combinations reject deterministically — D9, G7.
- Validation rejects malformed counts, duplicate entity/slot IDs, negative or out-of-range battle fields, wrong wave counters, and future schemas with field-path evidence — G7.
Acceptance-matrix: state=active,defeat; operation=encode,decode,migrate; result=payload-preserved,terminal-frozen
Risk-signals: persistence or migration; cross-module data flow
Traceability: US-FPL-006. Source: G5, G7, D9.

=== SPEC ===
TASK: feature
PLATFORM: android
WHAT: Versioned RunSave payload for complete PlayableBattleState plus legacy-safe migration and validation.
LAYERS: domain, data
TEST_TYPES: unit, persistence, migration
CONSTRAINTS: keep RunSave/ProfileStore separate; support legacy decode without inventing missing gameplay state; persist only Android-free authoritative battle data; reject malformed or invalid terminal combinations deterministically.
Acceptance-matrix: state=active,defeat; operation=encode,decode,migrate; result=payload-preserved,terminal-frozen
Risk-signals: persistence or migration; cross-module data flow
=== END SPEC ===

## Acceptance

Feature: Playable run-save payload codec
  Covers US-FPL-006. Source: G5, G7, D9.

  @US-FPL-006
  Scenario: Round-trip an active playable payload
    Given a playable run has non-zero resource, built and upgraded towers, living enemies, base health above zero, and a saved tick
    When the run is encoded and decoded by the current RunSave codec
    Then the decoded payload matches the saved PlayableBattleState exactly
    And the run remains non-terminal and active

  @US-FPL-006 @migration
  Scenario: Decode a supported historical contour save without inventing battle state
    Given a supported historical RunSave document predates the full playable-state payload
    When the save is decoded by the current codec
    Then the legacy fields remain readable through an explicit compatibility path
    And no missing PlayableBattleState fields are silently synthesized

  @US-FPL-006 @terminal
  Scenario: Round-trip a defeated playable payload
    Given a playable run has base health equal to zero and a defeat terminal result
    When the run is encoded and decoded by the current RunSave codec
    Then the decoded payload remains defeat
    And the decoded terminal fields satisfy the same freeze invariants as the source state

## Gap / context

Текущий `RunSave` умеет только contour-level metadata. Для playable level этого уже недостаточно: после SPEC-05 весь authoritative state существует, но wire contract пока не может его полностью зафиксировать.

## Implementation links
- commits: 5ed23213222508bc6ee8e1ba3da0133227a8b9e2, 0dc92acdf3bf980a6d717a37e0d82b95a679ce04, 5f3f051eb5262c58aa33c8162db1e4558fb29aaa
- files: app/build.gradle.kts, game/src/main/kotlin/dev/mysd/game/persistence/PersistenceWire.kt, game/src/main/kotlin/dev/mysd/game/persistence/RunSave.kt, game/src/test/kotlin/dev/mysd/game/persistence/PersistenceCodecTest.kt, game/src/test/kotlin/dev/mysd/game/persistence/PersistenceWireTest.kt
- verification: scoped :game:test pass; full runner 196 passed / 0 failed / 0 skipped; lint ok; deterministic reviewer pass; semantic review pass with warnings only; independent critic pass with warnings only; full verifier pass
