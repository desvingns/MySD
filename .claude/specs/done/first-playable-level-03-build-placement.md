# Первый playable level — fixed build placement

Status: done
TASK: feature
PLATFORM: android
WHAT: Добавить replay-safe BuildTower-команду, которая размещает один тип башни на трёх фиксированных точках с проверкой ресурса, занятости и допустимого slot ID.
LAYERS: domain
CHANGED_HINT:
- `game/src/main/kotlin/dev/mysd/game/battle/playable/PlayableBattleState.kt` — хранить occupancy и stable slot IDs для трёх fixed tiles — (assumption)
- `game/src/main/kotlin/dev/mysd/game/battle/playable/PlayableBattleEngine.kt` — атомарно обрабатывать `BuildTower` и списание ресурса — (assumption)
- `game/src/main/kotlin/dev/mysd/game/battle/playable/PlayableBattleCommand.kt` — определить replay-safe command для выбора slot и строительства — (assumption)
TEST_TYPES: unit, integration, replay
CONSTRAINTS:
- В domain нет свободного размещения: разрешены только три content-defined slots — D4, D8.
- В первом срезе доступен ровно один tower type — D5.
- Недостаток ресурса, occupied slot и неизвестный slot ID являются no-op/rejected outcomes без частичного списания — D6.
- Команды должны проходить существующий deterministic command/replay boundary — G5.
- Existing slot occupancy and atomic spend are already present; this SPEC closes only the missing BuildTower command, placement transition, and replay coverage.
Risk-signals: —
Acceptance-matrix: phase=active; target=empty,unknown,occupied; resource=affordable,insufficient

=== SPEC ===
TASK: feature
PLATFORM: android
WHAT: Three fixed build slots and a deterministic one-tower construction command.
LAYERS: domain
TEST_TYPES: unit, integration, replay
CONSTRAINTS: fixed slots only; one tower type; atomic spend; replay-safe command; no UI dependency.
=== END SPEC ===

## Acceptance

Feature: Fixed-slot tower construction
  Covers US-FPL-003. Source: D4, D5, D6, G5.

  @US-FPL-003
  Scenario: Build a tower on an affordable empty slot
    Given the battle has enough resource and slot 2 is empty
    When the player submits a build command for slot 2
    Then slot 2 contains the configured tower type
    And exactly the configured build cost is deducted

  @US-FPL-003 @validation
  Scenario: Reject construction on an unavailable slot
    Given the battle has enough resource
    When the player submits a build command for an unknown or already occupied slot
    Then no tower is added
    And the resource is unchanged

  @US-FPL-003 @empty
  Scenario: Keep all three slots empty at level start
    Given a new first-level run has just started
    When the initial battle snapshot is produced
    Then the three fixed slots are visible in the state
    And all three slots are empty

## Gap / context

Сейчас кнопка `Выбрать постройку` только меняет presentation flag. Этот SPEC добавляет реальную domain-команду, которую позже вызовет Android popup.

## Implementation links
- commit: 906d5a2, a9eba3e, aac506f
- files: app/build.gradle.kts, game/src/main/kotlin/dev/mysd/game/battle/playable/PlayableBattleCommand.kt, game/src/main/kotlin/dev/mysd/game/battle/playable/PlayableBattleEngine.kt, game/src/main/kotlin/dev/mysd/game/battle/playable/PlayableBattleState.kt, game/src/main/kotlin/dev/mysd/game/simulation/SimulationSession.kt, game/src/test/kotlin/dev/mysd/game/battle/playable/PlayableBattleEngineTest.kt, game/src/test/kotlin/dev/mysd/game/battle/playable/PlayableBattleStateTest.kt, game/src/test/kotlin/dev/mysd/game/simulation/SimulationSessionTest.kt
- targeted_checks: :game:test --tests dev.mysd.game.battle.playable.PlayableBattleEngineTest --tests dev.mysd.game.simulation.SimulationSessionTest (pass); full runner 160 passed / 0 failed / 0 skipped; :app:lintDebug (pass); git diff --check (pass)
- semantic_review: pass; coverage 6/6; independent_critic: pass with warnings recorded in verifier context
- staleness_auto_rescoped: 1
