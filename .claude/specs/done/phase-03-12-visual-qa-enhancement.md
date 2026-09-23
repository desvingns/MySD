# TASK-03.12 Visual QA — enhancement and victory surfaces

Status: done
TASK: feature
PLATFORM: android
WHAT: Render and compare enhancement and victory surfaces against the accepted fit registry, recording evidence-backed divergences while preserving deferred reward semantics.
LAYERS: presentation
TEST_TYPES: instrumented-compose-ui, screenshot
CHANGED_HINT:
- app/src/main/kotlin/dev/mysd/android/campaign/CampaignScreen.kt
- game/src/main/kotlin/dev/mysd/game/campaign/CampaignSession.kt
- spec/fit/registry.csv
- spec/evidence/state-graph.v1.json
CONSTRAINTS:
- Visual/device task: require a connected booted Android device before execution and never claim visual evidence without it.
- Compare ST-0004/BATTLE-ENHANCEMENT and ST-0005/BATTLE-VICTORY only.
- Reward claim, rewarded completion, doubling, transaction, and economy effects remain deferred.
- Keep original MySD creative content; never copy raw reference assets or UI text into public files.
- Preserve unrelated working-tree changes and never delete files.
EVIDENCE:
- ST-0004, ST-0005, FR-103, FR-104, FR-106, AC-102, AC-104, DEV-006, DEV-007
Acceptance-matrix: screen=enhancement,victory; pass=structural,visual
Risk-signals: visual/device work; cross-module data flow; security or payment

=== SPEC ===
TASK: feature
PLATFORM: android
WHAT: Render and compare enhancement and victory surfaces against the accepted fit registry, recording evidence-backed divergences while preserving deferred reward semantics.
LAYERS: presentation
TEST_TYPES: instrumented-compose-ui, screenshot
CONSTRAINTS: Connected device required; compare only ST-0004/ST-0005; deferred reward semantics remain unchanged; original creative only.
Acceptance-matrix: screen=enhancement,victory; pass=structural,visual
Risk-signals: visual/device work; cross-module data flow; security or payment
=== END SPEC ===

Implementation links:
- Commit 8054578 "feat: record enhancement and victory visual QA" — spec/fit/task-03.12-visual-qa.md
  (device QA record for ST-0004/ST-0005), app/build.gradle.kts (versionCode 13 / versionName 0.1.12)
- Commit 495833a "test: add ST-0004 and ST-0005 instrumented compose-ui tests" —
  app/src/androidTest/kotlin/dev/mysd/android/campaign/{EnhancementContentUiTest,VictoryContentUiTest}.kt,
  gradle/libs.versions.toml (compose-ui-test + androidx-test-ext-junit catalog aliases)
- build/fit/built/{ST-0004,ST-0005}.{png,xml} — device captures (emulator-5554, Pixel_5 AVD, API 34), gitignored
- Divergences recorded: FIT-03.12-001 (ST-0004 composition), FIT-03.12-002 (ST-0005 absent reward-claim/multiplier)
- Pushed: e793697..495833a -> origin/main on 2026-08-29

Gates: deterministic reviewer pass; semantic review pass (risk low, coverage 4/4, 1 warning fixed —
APK SHA / version bump COMPAT-001); instrumented compose-ui 2/2 pass on emulator-5554; independent
critic pass (risk standard, 1 ship-acceptable createComposeRule deprecation warning); full verifier
pass; scoped fit skipped (ST-0004/ST-0005 references preserved_unusable — visual parity deferred).
Docs step skipped (docsAgent: inert). No production/presentation code or :game state changed.
