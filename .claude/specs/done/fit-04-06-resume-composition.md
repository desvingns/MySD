# FIT-04.06 Resume composition

Status: done
TASK: feature
PLATFORM: android
WHAT: Fix the evidence-backed ST-0012/OVERLAY-RESUME composition divergence by presenting the existing unfinished-run prompt as an original full-width resume panel with stable action placement while preserving cancel and continue behavior.
LAYERS: presentation
TEST_TYPES: instrumented-compose-ui, screenshot
CHANGED_HINT:
- app/src/main/kotlin/dev/mysd/android/campaign/CampaignScreen.kt
- app/src/main/kotlin/dev/mysd/android/ui/theme
- app/src/main/res/values/strings.xml
CONSTRAINTS:
- Require a connected booted Android device for visual verification; never claim a visual pass without connected evidence.
- Preserve the existing CampaignSession unfinished-run state and CancelUnfinishedRun/ContinueUnfinishedRun intents.
- Keep the prompt modal and non-dismissible by outside tap/back except through the existing cancel action.
- Use original MySD panel treatment and copy; do not copy reference assets or UI text.
- Maintain minimum 48 dp targets, scalable text, deterministic lifecycle restoration, and no new gameplay/service behavior.
- Preserve unrelated working-tree changes and never delete files.
EVIDENCE:
- ST-0012, EV-0189, FR-100, AC-100, FIT-03.13-005, DEV-001, DEV-002, DEV-004, DEV-009
Acceptance-matrix: screen=resume; action=cancel,continue; pass=composition,bounds,semantics,accessibility
Risk-signals: visual/device work; session/auth lifecycle; cross-module data flow

=== SPEC ===
TASK: feature
PLATFORM: android
WHAT: Fix the evidence-backed ST-0012/OVERLAY-RESUME composition divergence by presenting the existing unfinished-run prompt as an original full-width resume panel with stable action placement while preserving cancel and continue behavior.
LAYERS: presentation
TEST_TYPES: instrumented-compose-ui, screenshot
CONSTRAINTS: Connected device required; preserve existing prompt state and intents; modal behavior and lifecycle restoration unchanged; original creative only; minimum 48 dp targets; no new mechanics.
Acceptance-matrix: screen=resume; action=cancel,continue; pass=composition,bounds,semantics,accessibility
Risk-signals: visual/device work; session/auth lifecycle; cross-module data flow
=== END SPEC ===
