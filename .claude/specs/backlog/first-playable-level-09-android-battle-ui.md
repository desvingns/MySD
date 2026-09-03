# Первый playable level — Android battle UI

Status: backlog
TASK: feature
PLATFORM: android
Depends-on: first-playable-level-08-campaign-lifecycle-restore.md
WHAT: Заменить debug battle composition на простой настоящий portrait battlefield: HUD ресурса и волны, вертикальный путь сверху вниз, три fixed build tile, popup строительства/улучшения, живые враги, башни и отдельная главная база.
LAYERS: presentation
CHANGED_HINT:
- `app/src/main/kotlin/dev/mysd/android/MainActivity.kt` — добавить lifecycle-safe ticker/collector, который вызывает game session и публикует immutable snapshots — G1, G4
- `app/src/main/kotlin/dev/mysd/android/campaign/CampaignScreen.kt` — отрисовать playable battlefield, live entity positions, three tiles, build/upgrade popup and terminal states — G4
- `app/src/main/res/values/strings.xml` — добавить оригинальные русские labels, errors, resource/wave/base announcements and accessibility descriptions — G1, G11
- `app/src/main/kotlin/dev/mysd/android/ui/theme` — добавить простые оригинальные visual tokens without reference assets — G11
TEST_TYPES: instrumented-compose-ui, screenshot, android-lifecycle
CONSTRAINTS:
- Existing `Launch → Campaign → Level Setup → Battle` route remains the entry point — D1, G2.
- Android renders immutable game snapshots and translates touch into commands; it cannot mutate authoritative state directly — G1, G4, G5.
- Tap on empty tile opens build popup; tap on occupied tile opens upgrade popup; insufficient/max actions are visibly guarded — D4, D12.
- Use simple original vector/Canvas shapes or approved assets; do not copy the attached screenshot's art, text, or exact visual identity — G11.
- The ticker must stop on pause and lifecycle stop, and must not create a second authoritative clock — G4, G5.
- The ticker and rendering seam consume the already-restorable playable session from SPEC-08; they must not re-own persistence logic or invent a second restore path.
- Keep touch targets accessible and compatible with scalable text; Canvas content descriptions must expose base, tiles, enemies and terminal state — G8, G11.
Acceptance-matrix: interaction=build,upgrade,live,terminal; outcome=visible,guarded
Risk-signals: visual/device work; session/auth lifecycle; cross-module data flow
Traceability: US-FPL-007. Source: G1, G4, D3, D4, D12.

=== SPEC ===
TASK: feature
PLATFORM: android
WHAT: Real portrait battlefield UI with HUD, fixed build tiles, build/upgrade popup, live entities, and terminal screens.
LAYERS: presentation
TEST_TYPES: instrumented-compose-ui, screenshot, android-lifecycle
CONSTRAINTS: preserve route; immutable snapshots; command-only input; original simple graphics; lifecycle-safe ticker; accessible semantics; consume the already-restorable playable session without a second restore path.
Acceptance-matrix: interaction=build,upgrade,live,terminal; outcome=visible,guarded
Risk-signals: visual/device work; session/auth lifecycle; cross-module data flow
=== END SPEC ===

## Acceptance

Feature: Android playable battlefield
  Covers US-FPL-007. Source: G1, G4, D3, D4, D12.

  @US-FPL-007
  Scenario: Build a tower through the tile popup
    Given the active battlefield shows three empty fixed build tiles and enough resource
    When the player selects an empty tile and confirms the only available tower type
    Then the tile shows the built tower
    And the resource HUD shows the configured cost deduction

  @US-FPL-007 @upgrade
  Scenario: Open the upgrade popup for an occupied tile
    Given a fixed tile contains a level 0 tower and enough resource is available
    When the player selects the occupied tile and confirms the upgrade action
    Then the tile shows the next tower level
    And the popup exposes the next upgrade cost or the maximum-level guard

  @US-FPL-007 @live
  Scenario: Render live movement without owning the game state
    Given an active playable battle is running
    When simulation snapshots advance
    Then enemy positions, tower shots, resource, and base health update on screen
    And the Android layer only renders the latest immutable snapshot

  @US-FPL-007 @terminal
  Scenario: Show a terminal result
    Given the game session reports victory or defeat
    When the Android screen receives the terminal snapshot
    Then the corresponding result is visible
    And gameplay controls no longer submit mutating commands

## Gap / context

Текущий экран уже похож на battlefield composition, но это статичная debug-витрина. Этот SPEC подключает real snapshots и даёт пользователю именно тот короткий визуальный игровой цикл, который показан на присланном референсе; persistence остаётся в SPEC-08.

## Implementation links
- commit: —
- files: —
