# Первый playable level — headless restore and replay continuity

Status: backlog
TASK: feature
PLATFORM: android
WHAT: Научить Android-free playable battle session восстанавливаться из полного `RunSave` payload и доказывать continuity: active run продолжает тот же hash trajectory, а defeat run остаётся terminal-frozen после restore.
LAYERS: domain, data
CHANGED_HINT:
- `game/src/main/kotlin/dev/mysd/game/simulation/SimulationSession.kt` — добавить restore seam для playable session из сохранённого authoritative state и canonical command metadata — G5, G7
- `game/src/main/kotlin/dev/mysd/game/battle/playable/PlayableBattleState.kt` — закрепить restore-time invariants для active и defeat snapshots — D9
- `game/src/test/kotlin/dev/mysd/game/simulation/ReplayVerificationTest.kt` — сравнить uninterrupted и restored trajectories на playable payload — G5
- `game/src/test/kotlin/dev/mysd/game/battle/playable/PlayableBattleCombatTest.kt` — проверить freeze/no-op contract после restore of defeat — D9
TEST_TYPES: unit, integration, replay, persistence
CONSTRAINTS:
- Restore rebuilds the authoritative playable session only from `RunSave` payload and canonical deterministic metadata; `CampaignSession`, Activity and Compose state are out of scope — G1, G5, G7.
- Same seed, saved state and command log must produce the same per-tick hash trajectory as uninterrupted execution for active runs — G5, G7.
- Restored defeat remains terminal-frozen: advance, pause/resume, build, upgrade and other mutating commands stay no-op or rejected exactly as in uninterrupted terminal state — D9.
- Legacy contour-only saves remain outside this restore path; they are not silently promoted into a synthetic playable state — G7.
Acceptance-matrix: state=active,defeat; operation=restore,resume; result=state-preserved,trajectory-preserved,terminal-frozen
Risk-signals: persistence or migration; concurrency or cancellation budget; cross-module data flow
Traceability: US-FPL-006. Source: G5, G7, D9.

=== SPEC ===
TASK: feature
PLATFORM: android
WHAT: Restore a playable battle session from saved payload and prove replay continuity or terminal freeze headlessly.
LAYERS: domain, data
TEST_TYPES: unit, integration, replay, persistence
CONSTRAINTS: restore from RunSave payload only; match uninterrupted trajectory for active runs; keep restored defeat fully frozen; do not route legacy contour saves through synthetic playable-state reconstruction.
Acceptance-matrix: state=active,defeat; operation=restore,resume; result=state-preserved,trajectory-preserved,terminal-frozen
Risk-signals: persistence or migration; concurrency or cancellation budget; cross-module data flow
=== END SPEC ===

## Acceptance

Feature: Headless playable restore continuity
  Covers US-FPL-006. Source: G5, G7, D9.

  @US-FPL-006 @restore
  Scenario: Restore an active run and continue the same deterministic trajectory
    Given an uninterrupted playable run is saved at an arbitrary tick with living enemies and at least one built tower
    When the saved payload is restored into a new playable battle session and both runs continue with the same command log
    Then the restored session snapshot matches the saved state
    And subsequent per-tick hashes match uninterrupted execution

  @US-FPL-006 @terminal @defeat
  Scenario: Restore a defeated run without reactivating it
    Given a playable run was saved after the base reached zero health
    When the saved payload is restored into a new playable battle session
    Then the restored session reports defeat immediately
    And further advance or mutating commands do not change its state hash

  @US-FPL-006 @negative
  Scenario: Reject or bypass a contour-only save on the playable restore seam
    Given a historical run save has no playable-state payload
    When the headless playable restore seam is asked to build a session from it
    Then the seam returns an explicit unsupported or legacy outcome
    And it does not invent missing enemy, tower or resource state

## Gap / context

После SPEC-06 wire contract сможет хранить полный playable state, но ещё не будет доказано, что из этого payload действительно поднимается тот же Android-free battle run. Этот SPEC закрывает именно restore/replay seam до вмешательства campaign или Android.

## Implementation links
- commit: —
- files: —
