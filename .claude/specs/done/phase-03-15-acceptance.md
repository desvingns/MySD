# TASK-03.15 Phase acceptance

Status: done
TASK: feature
PLATFORM: android
WHAT: Run and close the accepted foundation and Android contour scenarios AC-001 through AC-005 and AC-100 through AC-104 without promoting deferred mechanics.
LAYERS: domain, data, presentation
TEST_TYPES: unit, integration, instrumented-compose-ui, screenshot
CHANGED_HINT:
- spec/acceptance/foundation.feature
- game/src/test
- app/src/androidTest
- docs/implementation_plan/phases/PHASE_03_android_contour.md
CONSTRAINTS:
- Execute only accepted semantic scope and explicit service adapters; preserve all deferred/excluded rows.
- Use deterministic local adapters; no real ads, payments, accounts, network Arena, or copied reference creative.
- Device-rendered checks require a connected booted device; record explicit coverage exceptions where seams are absent.
- Update evidence/traceability only from observed test results; never weaken assertions or delete files.
EVIDENCE:
- AC-001 through AC-005, AC-100 through AC-104, FR-001 through FR-008, FR-100 through FR-107
Acceptance-matrix: scenario=AC-001,AC-002,AC-003,AC-004,AC-005,AC-100,AC-101,AC-102,AC-103,AC-104; result=pass,explicit-blocker
Risk-signals: cross-module data flow; persistence or migration; visual/device work; session/auth lifecycle

=== SPEC ===
TASK: feature
PLATFORM: android
WHAT: Run and close the accepted foundation and Android contour scenarios AC-001 through AC-005 and AC-100 through AC-104 without promoting deferred mechanics.
LAYERS: domain, data, presentation
TEST_TYPES: unit, integration, instrumented-compose-ui, screenshot
CONSTRAINTS: Accepted scope only; deterministic local adapters; no real external services or copied reference creative; device required for visual checks; preserve blockers and exceptions.
Acceptance-matrix: scenario=AC-001,AC-002,AC-003,AC-004,AC-005,AC-100,AC-101,AC-102,AC-103,AC-104; result=pass,explicit-blocker
Risk-signals: cross-module data flow; persistence or migration; visual/device work; session/auth lifecycle
=== END SPEC ===

Implementation links:
- Verified staleness auto-close on 2026-08-29; no new production code or tests were required.
- Existing contour delivery: `1a8c5d9`, `4f86b91`, `3fff27d`, `d265035`, `0e9bec0`, `e3bed14`, `aa64906`, `06d41be`, `c04b175`, `c6aa856`, `d86a819`, `57f3f25`, `de2a611`, `07bcde6`, `24d553d`, `e793697`, `54d5d45`, `e6deac9`.
- Acceptance sources: `spec/acceptance/foundation.feature`, `game/src/test`, `app/src/androidTest`, `scripts/public-safety.ps1`, `scripts/validate-spec.ps1`, `gradle/myengine.lock`.
- Verification: MP runner `125 passed / 0 failed / 0 skipped`, lint `ok`; `:app:connectedDebugAndroidTest` `11 passed / 0 failed / 0 skipped` on `Pixel_5(AVD) - 14`; public-safety `pass` (`124` tracked files, `1156` history paths); spec validator `pass` (`16` requirements, `10` stories, `10` acceptance, `16` trace rows); `git diff --check` passed.
