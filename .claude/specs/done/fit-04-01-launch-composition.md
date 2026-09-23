# FIT-04.01 Launch composition

Status: done
TASK: feature
PLATFORM: android
WHAT: Fix the evidence-backed ST-0001/ROUTE-LAUNCH composition divergence by moving the accepted launch action into an original full-screen campaign composition with a stable bottom action region while preserving the existing EnterCampaign transition.
LAYERS: presentation
TEST_TYPES: instrumented-compose-ui, screenshot
CHANGED_HINT:
- app/src/main/kotlin/dev/mysd/android/campaign/CampaignScreen.kt
- app/src/main/kotlin/dev/mysd/android/ui/theme
- app/src/main/res/values/strings.xml
CONSTRAINTS:
- Require a connected booted Android device for visual verification; never claim a visual pass without connected evidence.
- Preserve the existing immutable CampaignSnapshot and CampaignIntent boundary; EnterCampaign remains the only launch action.
- Use original MySD composition, shapes, colors, and copy. Do not copy reference assets or UI text into public files.
- Keep the primary action at least 48 dp, respect system insets, and remain usable with scalable text.
- Do not add Shop, Tech, external exit, network, account, or unobserved gameplay behavior.
- Preserve unrelated working-tree changes and never delete files.
EVIDENCE:
- ST-0001, EV-0233, FR-100, AC-100, FIT-03.11-001, DEV-001, DEV-002, DEV-004, DEV-009
Acceptance-matrix: screen=launch; pass=composition,bounds,semantics,accessibility
Risk-signals: visual/device work

=== SPEC ===
TASK: feature
PLATFORM: android
WHAT: Fix the evidence-backed ST-0001/ROUTE-LAUNCH composition divergence by moving the accepted launch action into an original full-screen campaign composition with a stable bottom action region while preserving the existing EnterCampaign transition.
LAYERS: presentation
TEST_TYPES: instrumented-compose-ui, screenshot
CONSTRAINTS: Connected device required; original creative only; immutable state and existing EnterCampaign transition; no new routes or mechanics; minimum 48 dp targets; deferred/excluded scope unchanged.
Acceptance-matrix: screen=launch; pass=composition,bounds,semantics,accessibility
Risk-signals: visual/device work
=== END SPEC ===

Implementation links:
- Commit b0683c5 "feat: refine launch composition"
- app/src/main/kotlin/dev/mysd/android/campaign/CampaignScreen.kt
- app/src/main/kotlin/dev/mysd/android/ui/theme/Color.kt
- app/src/main/res/values/strings.xml
- app/src/androidTest/kotlin/dev/mysd/android/campaign/LaunchContentUiTest.kt
- Connected AndroidTest: 12 passed / 0 failed / 0 skipped on emulator-5554 / Pixel_5(AVD)-14.
- Locked engine verification: ../MyEngine-mysd at 30f4eb17aff0ea2fe6cf80aef970a1e7746dbcbb.
- Scoped fit: FIT-03.11-001 resolved; numeric pixel score unavailable because ImageMagick is not installed and profiles differ.
