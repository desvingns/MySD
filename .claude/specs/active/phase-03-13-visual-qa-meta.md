# TASK-03.13 Visual QA — meta and service-shaped surfaces

Status: active
TASK: feature
PLATFORM: android
WHAT: Capture and compare the already-rendered campaign, roster, settings, local Arena, and resume surfaces against the accepted fit registry, adding only the evidence/test seams needed to record evidence-backed divergences.
LAYERS: presentation
TEST_TYPES: instrumented-compose-ui, screenshot
CHANGED_HINT:
- app/src/main/kotlin/dev/mysd/android/campaign/CampaignScreen.kt
- app/src/main/kotlin/dev/mysd/android/MainActivity.kt
- game/src/main/kotlin/dev/mysd/game/campaign/CampaignSession.kt
- spec/fit/registry.csv
CONSTRAINTS:
- Visual/device task: require a connected booted Android device before execution and never claim visual evidence without it.
- Compare ST-0006, ST-0007, ST-0008, ST-0011, and ST-0012 only.
- Existing route/session/UI contours overlap this SPEC; focus implementation on the remaining device captures, structural checks, and fit record rather than reimplementing those surfaces.
- Preserve deferred roster upgrades, settings effects, Arena network/account/match behavior, and external exit scope.
- Keep original MySD creative content; never copy raw reference assets or UI text into public files.
- Preserve unrelated working-tree changes and never delete files.
EVIDENCE:
- ST-0006, ST-0007, ST-0008, ST-0011, ST-0012, FR-100, FR-105, FR-107, AC-100, AC-103, AC-104
Acceptance-matrix: screen=campaign,roster,settings,arena,resume; pass=structural,visual
Risk-signals: visual/device work; cross-module data flow; session/auth lifecycle

=== SPEC ===
TASK: feature
PLATFORM: android
WHAT: Capture and compare the already-rendered campaign, roster, settings, local Arena, and resume surfaces against the accepted fit registry, adding only the evidence/test seams needed to record evidence-backed divergences.
LAYERS: presentation
TEST_TYPES: instrumented-compose-ui, screenshot
CONSTRAINTS: Connected device required; compare only ST-0006/ST-0007/ST-0008/ST-0011/ST-0012; preserve existing route/session/UI contours; add only evidence/test seams; deferred mechanics unchanged; original creative only.
Acceptance-matrix: screen=campaign,roster,settings,arena,resume; pass=structural,visual
Risk-signals: visual/device work; cross-module data flow; session/auth lifecycle
=== END SPEC ===

Implementation links: pending
