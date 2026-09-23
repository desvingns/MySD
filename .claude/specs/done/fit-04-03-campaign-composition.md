# FIT-04.03 Campaign composition

Status: done
TASK: feature
PLATFORM: android
WHAT: Fix the evidence-backed ST-0006/ROUTE-CAMPAIGN composition divergence by introducing an original campaign shell with a structured header, level card, and bottom route presentation while keeping only the accepted campaign, Troops, and local Arena actions interactive.
LAYERS: presentation
TEST_TYPES: instrumented-compose-ui, screenshot
CHANGED_HINT:
- app/src/main/kotlin/dev/mysd/android/campaign/CampaignScreen.kt
- app/src/main/kotlin/dev/mysd/android/ui/theme
- app/src/main/res/values/strings.xml
CONSTRAINTS:
- Require a connected booted Android device for visual verification; never claim a visual pass without connected evidence.
- Preserve CampaignSnapshot and existing actions for stage selection, roster, and local Arena.
- Shop and Tech remain deferred: if represented in the shell they must be visibly disabled/non-interactive and must not create routes or intents.
- Use original MySD visual treatment and copy; do not copy reference assets or UI text.
- Maintain minimum 48 dp targets, scalable text, and no authoritative UI state.
- Preserve unrelated working-tree changes and never delete files.
EVIDENCE:
- ST-0006, EV-0196, EV-0200, FR-100, AC-100, FIT-03.13-001, DEV-001, DEV-002, DEV-004, DEV-005, DEV-009
Acceptance-matrix: screen=campaign; route=accepted,disabled-deferred; pass=composition,bounds,semantics,accessibility
Risk-signals: visual/device work; cross-module data flow
DESIGN_TOKENS: CampaignBackground, CampaignSurface, CampaignAccent, CampaignSupport, CampaignOnBackground, CampaignOnSurface, CampaignDisabled, CampaignMetrics

=== SPEC ===
TASK: feature
PLATFORM: android
WHAT: Fix the evidence-backed ST-0006/ROUTE-CAMPAIGN composition divergence by introducing an original campaign shell with a structured header, level card, and bottom route presentation while keeping only the accepted campaign, Troops, and local Arena actions interactive.
LAYERS: presentation
TEST_TYPES: instrumented-compose-ui, screenshot
CONSTRAINTS: Connected device required; preserve accepted campaign/roster/local Arena actions; Shop and Tech remain disabled/deferred; original creative only; no new routes or mechanics; minimum 48 dp targets.
Acceptance-matrix: screen=campaign; route=accepted,disabled-deferred; pass=composition,bounds,semantics,accessibility
Risk-signals: visual/device work; cross-module data flow
=== END SPEC ===

Implementation links:
- Commits b524340 (campaign theme tokens), cb831321c53424dd16af7d3c64451fef0d2285ba (campaign shell), c28a788cfa4bfb92db49bdfd8e79295a434ac550 (test coverage repair), and 2195206e068ea26b2ad0edef1dbbbf7b3182e835 (compact-width bounds repair).
- app/src/main/kotlin/dev/mysd/android/campaign/CampaignScreen.kt
- app/src/main/kotlin/dev/mysd/android/ui/theme/Color.kt
- app/src/main/kotlin/dev/mysd/android/ui/theme/Spacing.kt
- app/src/main/res/values/strings.xml
- app/src/androidTest/kotlin/dev/mysd/android/campaign/CampaignContentUiTest.kt
- Project runner: 125 passed / 0 failed / 0 skipped; lint ok; screenshot runner skipped because not configured.
- Exact locked-engine build: `./gradlew -Pmyengine.path=../MyEngine-mysd test :app:assembleDebug --no-daemon` — BUILD SUCCESSFUL.
- Connected Android: 14 passed / 0 failed / 0 skipped on emulator-5554 / Pixel_5(AVD)-14.
- Public safety: pass; no reference assets or copied reference UI text added.
- Scoped fit: FIT-03.13-001 resolved; structural PASS, bounds PASS, numeric pixel score unavailable because ImageMagick is not installed and profiles differ; manual multimodal fit PASS.
