# Первый playable level — acceptance and balance

Status: backlog
TASK: feature
PLATFORM: android
Depends-on: first-playable-level-09-android-battle-ui.md
WHAT: Закрыть playable-level acceptance: deterministic JVM scenarios, replay/save checks, Android interaction tests, simple visual verification and a balance report for the original baseline.
LAYERS: domain, presentation
CHANGED_HINT:
- `game/src/test` — добавить fixtures и tests для resource, build, upgrades, movement, combat, victory, defeat, replay and migration — G5, G7, G8
- `app/src/androidTest` — покрыть route-to-battle, popup actions, live snapshot rendering, lifecycle and terminal UI — G1, G4, G8
- `docs/implementation_plan/PROGRESS.md` — записать completion evidence и переход feature epic после прохождения verification — G10
TEST_TYPES: unit, integration, replay, persistence, instrumented-compose-ui, screenshot, performance
CONSTRAINTS:
- Tests use fakes and deterministic fixtures; no MockK/Mockito, no weakened assertions or ignored tests — project user profile, G5, G8.
- Same seed and command log must produce identical per-tick hashes before and after save/restore — G5, G7, G10.
- Both victory and defeat are mandatory; a structured blocker cannot substitute for playable defeat — G9, G10, D9.
- Balance report records original parameters, resource curve, build timing, leak timing, and entity concurrency; it does not claim reference parity — G10, G11.
- Device/screenshot verification reports profile and tooling limitations honestly; no pixel score is claimed when comparison infrastructure is unavailable — G8, G11.
Acceptance-matrix: scenario=full-loop,replay,negative; evidence=jvm,android-device
Risk-signals: persistence or migration; visual/device work; cross-module data flow
Traceability: US-FPL-008. Source: G5, G8, G9, G10.

=== SPEC ===
TASK: feature
PLATFORM: android
WHAT: End-to-end acceptance, original balance report, replay coverage, and Android verification for the first playable level.
LAYERS: domain, presentation
TEST_TYPES: unit, integration, replay, persistence, instrumented-compose-ui, screenshot, performance
CONSTRAINTS: deterministic fixtures; both terminals; no ignored/weakened tests; original-only balance; honest device evidence; verify the full loop through the already-restorable persistence boundary.
Acceptance-matrix: scenario=full-loop,replay,negative; evidence=jvm,android-device
Risk-signals: persistence or migration; visual/device work; cross-module data flow
=== END SPEC ===

## Acceptance

Feature: First playable level acceptance
  Covers US-FPL-008. Source: G5, G8, G9, G10.

  @US-FPL-008 @smoke
  Scenario: Complete the full playable loop
    Given the player starts the existing campaign route and reaches the first battle
    When the player waits for resource, builds and upgrades towers, and lets the finite wave run
    Then the screen shows moving enemies, automatic tower attacks, resource changes, and a terminal victory or defeat
    And the result is reproducible from the recorded seed and command log

  @US-FPL-008 @replay
  Scenario: Match uninterrupted and restored runs
    Given an identical seed and command sequence are used for two runs
    When one run is interrupted, saved, restored, and continued
    Then both runs produce the same per-tick hash trajectory and terminal result

  @US-FPL-008 @negative
  Scenario: Verify guarded actions
    Given the player has insufficient resource, an occupied tile, a maximum-level tower, or a terminal run
    When the player attempts the corresponding invalid action
    Then the state remains unchanged
    And the UI exposes a clear guard without crashing

## Gap / context

Этот SPEC превращает девять предыдущих контрактов в проверяемый handoff: после него можно открыть игру на устройстве и увидеть минимальный уровень, а не набор debug-кнопок.

## Implementation links
- commit: —
- files: —
