# TASK-03.14 Lifecycle restoration

Status: done
TASK: feature
PLATFORM: android
WHAT: Complete Android lifecycle persistence integration so active and victory contours save and restore deterministically across background, recreate, and process death (US-004, AC-004).
LAYERS: domain, presentation
TEST_TYPES: unit, integration, instrumented-compose-ui
CHANGED_HINT:
- game/src/main/kotlin/dev/mysd/game/campaign/CampaignSession.kt
- game/src/main/kotlin/dev/mysd/game/persistence
- app/src/main/kotlin/dev/mysd/android/MainActivity.kt
- app/src/main/kotlin/dev/mysd/android/campaign/CampaignScreen.kt
CONSTRAINTS:
- Authoritative state remains Android-free, fixed 20 Hz and replay-stable; Activity/View owns no game state.
- Restore only supported RunSave/ProfileStore schemas and preserve deterministic continuity.
- Persist the canonical RunSave at the Android lifecycle boundary and recreate the Android-free
  session from supported data without putting authoritative state in Activity/View.
- Do not add unobserved gameplay, network, account, or production service behavior.
- Require a connected device for instrumented lifecycle checks; never replace required device evidence with JVM-only claims.
- Preserve unrelated working-tree changes and never delete files; use Fakes-only tests.
EVIDENCE:
- FR-004, FR-007, US-004, AC-004, ED-0023, ED-0024
Acceptance-matrix: lifecycle=background,recreate,process-death; state=active,victory
Risk-signals: session/auth lifecycle; persistence or migration; concurrency or cancellation budget

=== SPEC ===
TASK: feature
PLATFORM: android
WHAT: Complete Android lifecycle persistence integration so active and victory contours save and restore deterministically across background, recreate, and process death (US-004, AC-004).
LAYERS: domain, presentation
TEST_TYPES: unit, integration, instrumented-compose-ui
CONSTRAINTS: Android-free deterministic authority; supported save/profile schemas only; save at the Android lifecycle boundary and restore through the existing campaign/session seam; no new mechanics or external services; connected device required for instrumented lifecycle checks; Fakes-only tests.
Acceptance-matrix: lifecycle=background,recreate,process-death; state=active,victory
Risk-signals: session/auth lifecycle; persistence or migration; concurrency or cancellation budget
=== END SPEC ===

Implementation links: commits 54d5d45, e6deac9; files app/src/main/kotlin/dev/mysd/android/MainActivity.kt, app/src/main/kotlin/dev/mysd/android/persistence/AndroidRunSaveStorage.kt, game/src/main/kotlin/dev/mysd/game/campaign/CampaignSession.kt, game/src/test/kotlin/dev/mysd/game/campaign/CampaignLifecyclePersistenceTest.kt, app/src/androidTest/kotlin/dev/mysd/android/campaign/LifecyclePersistenceUiTest.kt, app/src/androidTest/kotlin/dev/mysd/android/persistence/AndroidRunSaveStorageTest.kt; verification: JVM 125 passed, connected 11 passed, public-safety pass.
