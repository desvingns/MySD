# TASK-03.11 Visual QA — core battle surfaces

Status: done
TASK: feature
PLATFORM: android
WHAT: Render and compare launch, battle setup, and active battle surfaces against the accepted fit registry, recording only evidence-backed structural and visual divergences.
LAYERS: presentation
TEST_TYPES: instrumented-compose-ui, screenshot
CHANGED_HINT:
- app/src/main/kotlin/dev/mysd/android/campaign/CampaignScreen.kt
- app/src/main/kotlin/dev/mysd/android/MainActivity.kt
- spec/fit/registry.csv
- spec/evidence/state-graph.v1.json
CONSTRAINTS:
- Visual/device task: require a connected booted Android device before execution and never claim visual evidence without it.
- Compare ST-0001/ROUTE-LAUNCH, ST-0002/BATTLE-SETUP, and ST-0003/BATTLE-ACTIVE only.
- Keep original MySD creative content; never copy raw reference assets or UI text into public files.
- Record fit divergences and explicit exceptions; do not expand deferred mechanics or authoritative UI state.
- Preserve unrelated working-tree changes and never delete files.
EVIDENCE:
- ST-0001, ST-0002, ST-0003, FR-100, FR-101, FR-102, AC-100, AC-101
Acceptance-matrix: screen=launch,battle-setup,battle-active; pass=structural,visual
Risk-signals: visual/device work; cross-module data flow

=== SPEC ===
TASK: feature
PLATFORM: android
WHAT: Render and compare launch, battle setup, and active battle surfaces against the accepted fit registry, recording only evidence-backed structural and visual divergences.
LAYERS: presentation
TEST_TYPES: instrumented-compose-ui, screenshot
CONSTRAINTS: Connected device required; compare only ST-0001/ST-0002/ST-0003; original creative only; record divergences without adding mechanics.
Acceptance-matrix: screen=launch,battle-setup,battle-active; pass=structural,visual
Risk-signals: visual/device work; cross-module data flow
=== END SPEC ===

Implementation links:
- Commit a07c6d5 "feat: record core surface visual QA"
- spec/fit/task-03.11-visual-qa.md — structural + visual QA record for ST-0001/ST-0002/ST-0003
- spec/fit/registry.csv — accepted fit registry rows (ST-0001..ST-0013) used as comparison source
- build/fit/built/{ST-0001,ST-0002,ST-0002-ready,ST-0003}.{png,xml} — device captures (emulator-5554, API 34)
- app/build.gradle.kts — versionCode 12 / versionName 0.1.11 for the QA build

Closed via /mp --feature --next staleness pre-check on 2026-08-29: the SPEC's WHAT
(compare launch / battle-setup / battle-active against the fit registry and record
evidence-backed divergences — FIT-03.11-001/002/003) was already delivered by the
commit above. Phase 2 skipped by user decision ("close as already delivered").
No new instrumented-compose-ui/screenshot test files were added because the task
resolved as a pure device QA-recording pass, not a presentation-code change.
