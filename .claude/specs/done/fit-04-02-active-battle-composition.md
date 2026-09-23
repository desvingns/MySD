# FIT-04.02 Active battle composition

Status: done
TASK: feature
PLATFORM: android
WHAT: Fix the evidence-backed ST-0003/BATTLE-ACTIVE composition divergence by presenting the existing battle snapshot in an original full-screen battlefield layout with a HUD, visible base/enemy region, and edge action controls without changing battle semantics.
LAYERS: presentation
TEST_TYPES: unit, instrumented-compose-ui, screenshot
CHANGED_HINT:
- app/src/main/kotlin/dev/mysd/android/campaign/CampaignScreen.kt
- app/src/main/kotlin/dev/mysd/android/ui/theme
- app/src/main/res/values/strings.xml
CONSTRAINTS:
- Require a connected booted Android device for visual verification; never claim a visual pass without connected evidence.
- Render only fields already present in ActiveBattleSnapshot; speed, pause/resume, build selection, enhancement, and victory intents keep their current no-op/effect boundaries.
- Use original MySD vector/Canvas shapes and composition; do not copy reference assets or UI text.
- Keep authoritative state in :game, preserve immutable snapshot rendering, minimum 48 dp targets, and scalable text.
- Do not add defeat, economy, waves beyond the snapshot, Shop, Tech, external services, or network behavior.
- Preserve unrelated working-tree changes and never delete files.
EVIDENCE:
- ST-0003, EV-0243, FR-102, AC-101, FIT-03.11-003, DEV-001, DEV-002, DEV-003, DEV-004, DEV-005, DEV-009
Acceptance-matrix: screen=active-battle; pass=composition,bounds,semantics,accessibility
Risk-signals: visual/device work; cross-module data flow
DESIGN_TOKENS: color.BattleBackground, color.BattleFieldMid, color.BattleHorizon, color.BattleHud, color.BattleAction, color.BattleBase, color.BattleEnemy, color.BattleOnBackground, color.BattleOnHud, spacing.BattleMetrics.minTouchTarget, spacing.BattleMetrics.edgeControlSize, spacing.BattleMetrics.hudInset, spacing.BattleMetrics.controlGap

=== SPEC ===
TASK: feature
PLATFORM: android
WHAT: Fix the evidence-backed ST-0003/BATTLE-ACTIVE composition divergence by presenting the existing battle snapshot in an original full-screen battlefield layout with a HUD, visible base/enemy region, and edge action controls without changing battle semantics.
LAYERS: presentation
TEST_TYPES: unit, instrumented-compose-ui, screenshot
CONSTRAINTS: Connected device required; render existing snapshot fields only; original creative; immutable state boundary; no new mechanics, economy, or routes; minimum 48 dp targets; deferred scope unchanged.
Acceptance-matrix: screen=active-battle; pass=composition,bounds,semantics,accessibility
Risk-signals: visual/device work; cross-module data flow
=== END SPEC ===

Implementation links:
- Commits b999f34 (battle theme tokens), 05e49d5 (battlefield composition), 44a7d5f (versioning scope repair), and a113960c (accessibility assertion).
- app/src/main/kotlin/dev/mysd/android/campaign/CampaignScreen.kt
- app/src/main/kotlin/dev/mysd/android/ui/theme/Color.kt
- app/src/main/kotlin/dev/mysd/android/ui/theme/Spacing.kt
- app/src/androidTest/kotlin/dev/mysd/android/campaign/ActiveBattleContentUiTest.kt
- app/build.gradle.kts (version unchanged after repair: versionCode 16, versionName 0.1.15)
- Project runner: 125 passed / 0 failed / 0 skipped; lint ok; screenshot runner skipped because not configured.
- Exact locked-engine build: `./gradlew -Pmyengine.path=../MyEngine-mysd test :app:assembleDebug --no-daemon` — BUILD SUCCESSFUL.
- Connected Android: 13 passed / 0 failed / 0 skipped on emulator-5554 / Pixel_5(AVD)-14, Android 14/API 34.
- Public safety: pass; no reference assets or copied reference UI text added.
- Scoped fit: FIT-03.11-003 resolved; structural PASS, bounds PASS, numeric pixel score unavailable because ImageMagick is not installed and profiles differ; manual multimodal fit PASS.
