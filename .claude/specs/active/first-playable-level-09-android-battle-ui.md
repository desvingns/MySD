# Первый playable level — Android battle UI

Status: active
TASK: feature
PLATFORM: android
Depends-on: first-playable-level-08-campaign-lifecycle-restore.md
WHAT: Подключить существующую Android battle composition к уже восстанавливаемому PlayableBattleSession: публиковать immutable live snapshots через lifecycle-safe ticker и поддержать command-only popup строительства/улучшения, живые entity и terminal states.
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
- Existing static battle composition and Material tokens are retained where useful; this slice closes only the missing PlayableBattleSnapshot projection, command wiring, popup state, and lifecycle ticker. No new gameplay rules or persistence path are introduced.
- A full `PlayableBattleSnapshot` is the canonical battle surface. Its pause/resume and tile popup commands supersede the legacy contour-only speed, build-selector, enhancement, manual-victory, and reward-panel controls; those controls must not compete with authoritative runtime commands.
- Historical contour-only saves remain readable through their explicit compatibility path. Reintroducing auxiliary controls into a full playable run requires fresh evidence for `com.yuegame.defender` and a separate command/persistence contract; it is deferred rather than inferred in this slice.
Acceptance-matrix: interaction=build,upgrade,live,terminal; outcome=visible,guarded
Risk-signals: visual/device work; session/auth lifecycle; cross-module data flow
Traceability: US-FPL-007. Source: G1, G4, D3, D4, D12.

=== SPEC ===
TASK: feature
PLATFORM: android
WHAT: Connect the existing portrait battle composition to the restorable PlayableBattleSession with immutable live snapshots, command-only build/upgrade popups, live entities, and terminal screens.
LAYERS: presentation
TEST_TYPES: instrumented-compose-ui, screenshot, android-lifecycle
CONSTRAINTS: preserve route; retain useful existing composition/tokens; immutable snapshots; command-only input; original simple graphics; lifecycle-safe ticker; accessible semantics; consume the already-restorable playable session without new gameplay rules or a second restore/persistence path.
Acceptance-matrix: interaction=build,upgrade,live,terminal; outcome=visible,guarded
Risk-signals: visual/device work; session/auth lifecycle; cross-module data flow
DESIGN_TOKENS: BattleTile, BattleTileBorder, BattleTileSelected, BattleTower, BattleProjectile, BattlePopup, BattlePopupBorder, BattleOnPopup, BattleGuarded, BattleGuardedSurface, BattleVictory, BattleDefeat, BattleTerminalScrim
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

The accepted structural anchor belongs to the legacy `com.gdzsq.crazy_td` evidence corpus. It supports this original-IP first-playable slice, but it is not a parity claim for the newly supplied `com.yuegame.defender` package; that package requires a fresh delta crawl before its additional behavior or balance can become requirements.

## Implementation links
- commits: `fa9fc96`, `4d9ed0f`, `2e68e5b`, `1681bbb`, `4d9ae6d`, `1115ea7`, `eb86882`
- production/config: `app/build.gradle.kts`, `app/src/main/kotlin/dev/mysd/android/MainActivity.kt`, `app/src/main/kotlin/dev/mysd/android/campaign/CampaignScreen.kt`, `app/src/main/res/values/strings.xml`, `app/src/main/kotlin/dev/mysd/android/ui/theme/Color.kt`, `game/src/main/kotlin/dev/mysd/game/campaign/CampaignSession.kt`
- tests: `app/src/androidTest/kotlin/dev/mysd/android/PlayableBattleContentUiTest.kt`, `app/src/androidTest/kotlin/dev/mysd/android/PlayableBattleSemanticRepairTest.kt`, `app/src/androidTest/kotlin/dev/mysd/android/campaign/LifecyclePersistenceUiTest.kt`
- verification: `213/0/0` JVM tests with lint `ok`; `34/0/0` connected tests on Pixel 9 API 37; post-geometry battle UI `6/0/0`; public-safety pass (`146` tracked files, `1920` history paths); exact `test :app:assembleDebug` pass; scoped fit `92/100` with `7/7` checks and no unexplained divergence; screenshot `build/fit/built/FPL-09-playable-battle-vertical.png` SHA-256 `F3477FE52CED6F7978606084C82EB6411FE16F3486B83A1673C4289CB31B8923`

## Handoff

- Prerequisite regression `PERSISTENCE-001` was confirmed after the original verification pass:
  a live canonical transition from active battle to `VICTORY` makes `CampaignSession.runSave()`
  return `null`. `MainActivity` forwards that value to `AndroidRunSaveStorage`, which removes the
  durable save on lifecycle stop. Seeded terminal-victory restore works, so the gap is the live
  transition, not the codec or restore format.
- Root cause: the early canonical terminal branch in `CampaignSession.runSave()` handles only
  `DEFEAT`; the later path asks `activeBattleSnapshot()`, which intentionally rejects every terminal
  playable state. Existing lifecycle tests seed a ready-made playable victory or exercise the
  legacy manual victory contour and therefore do not cover `active -> live victory -> save`.
- Required repair before close-out: add a JVM regression that reaches victory from a supported
  non-terminal full save, asserts a non-null canonical terminal save, codec round-trip, restore,
  and terminal freeze; generalize the existing terminal save branch to emit either `VICTORY` or
  `DEFEAT` without changing legacy contour-only compatibility. Add a targeted connected lifecycle
  case if the device seam can cross the terminal transition deterministically.
- Do not change balance in this repair. The shipped defaults independently prove that no-command
  play cannot defeat the base (`120 - 9 * 12 = 12` HP); that mandatory defeat reachability and the
  balance report belong to dependent FPL-10.
- Re-run scoped JVM, targeted connected lifecycle, full FPL-09 verification, public safety, exact
  locked-engine build, and scoped fit after the repair. Keep the push gate unresolved until the
  user explicitly answers `y`.
