# Первый playable level — campaign and Android lifecycle restore

Status: backlog
TASK: feature
PLATFORM: android
WHAT: Подключить `CampaignSession`, `MainActivity` и `AndroidRunSaveStorage` к полному playable run payload так, чтобы active и defeat runs переживали background, recreate и process death без возврата к contour-only reconstruction.
LAYERS: domain, data, presentation
CHANGED_HINT:
- `game/src/main/kotlin/dev/mysd/game/campaign/CampaignSession.kt` — читать и публиковать playable save state через existing campaign route и unfinished-run boundary — G1, G2, G7
- `app/src/main/kotlin/dev/mysd/android/MainActivity.kt` — сохранять и загружать canonical full run save на lifecycle boundary — G1, G7
- `app/src/main/kotlin/dev/mysd/android/persistence/AndroidRunSaveStorage.kt` — оставить encoded-document storage boundary стабильным для новых payload sizes — G1
- `game/src/test/kotlin/dev/mysd/game/campaign/CampaignLifecyclePersistenceTest.kt` — покрыть active/defeat lifecycle restore через campaign seam — G2, G7
- `app/src/androidTest/kotlin/dev/mysd/android/campaign/LifecyclePersistenceUiTest.kt` — проверить background/recreate/process-death на device для active/defeat saves — G1
TEST_TYPES: integration, persistence, android-lifecycle, instrumented-compose-ui
CONSTRAINTS:
- Existing PHASE_03 lifecycle boundary is reused, not replaced; no new navigation branch or new meta progression scope is introduced — G1, G2.
- `CampaignSession` restores only supported stage payloads and keeps authoritative battle state outside Activity/View — G1, G2, G7.
- Existing victory compatibility remains accepted, but active/defeat playable payload becomes the canonical restore path for the first playable level — G7, D9.
- A saved defeat run must not reappear as an unfinished active run; lifecycle restore keeps it terminal-guarded end to end — D9.
- No reward, economy, account, network, shop or tech scope is added — project boundary.
Acceptance-matrix: boundary=campaign-session,android-storage; lifecycle=background,recreate,process-death; state=active,defeat
Risk-signals: session/auth lifecycle; persistence or migration; cross-module data flow
Traceability: US-FPL-006. Source: G1, G2, G7, D9.

=== SPEC ===
TASK: feature
PLATFORM: android
WHAT: Reconnect CampaignSession and Android lifecycle storage to active/defeat playable saves.
LAYERS: domain, data, presentation
TEST_TYPES: integration, persistence, android-lifecycle, instrumented-compose-ui
CONSTRAINTS: reuse the existing lifecycle boundary; restore only supported stage payloads; keep authoritative state outside Activity/View; never revive a defeated run as active; add no new gameplay or service scope.
Acceptance-matrix: boundary=campaign-session,android-storage; lifecycle=background,recreate,process-death; state=active,defeat
Risk-signals: session/auth lifecycle; persistence or migration; cross-module data flow
=== END SPEC ===

## Acceptance

Feature: Campaign and Android lifecycle restore for playable runs
  Covers US-FPL-006. Source: G1, G2, G7, D9.

  @US-FPL-006 @active
  Scenario: Background, recreation and process death preserve an active playable run
    Given the user reached an active playable battle and a full run payload was persisted
    When Android backgrounds, recreates, or relaunches the activity process
    Then the same encoded save is reloaded through the existing lifecycle boundary
    And the campaign seam restores the same active run instead of a contour-only substitute

  @US-FPL-006 @terminal @defeat
  Scenario: Background, recreation and process death preserve a defeated playable run
    Given the user saved a playable run after the base reached zero health
    When Android backgrounds, recreates, or relaunches the activity process
    Then the restored run remains defeat end to end
    And the unfinished-run prompt does not revive it as an active run

  @US-FPL-006 @negative
  Scenario: Ignore an unsupported or malformed playable save safely
    Given local storage contains a malformed document or a supported save for an unaccepted stage
    When `MainActivity` loads the lifecycle save boundary
    Then the invalid save is ignored safely
    And no partial authoritative state leaks into the campaign session

## Gap / context

Старый lifecycle contour уже работает для marker-only active/victory. После SPEC-07 будет настоящий authoritative restore seam; этот SPEC протянет его через campaign и Android storage без смешивания с будущим battlefield UI.

## Implementation links
- commit: —
- files: —
