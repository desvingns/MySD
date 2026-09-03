# Первый playable level — headless restore and replay continuity

Status: done
TASK: feature
PLATFORM: android
WHAT: Добавить Android-free restore seam, который поднимает `PlayableBattleSession` из уже готового полного `RunSave` payload и доказывает continuity active run либо terminal freeze для defeat.
LAYERS: domain, data
CHANGED_HINT:
- `game/src/main/kotlin/dev/mysd/game/simulation/SimulationSession.kt` — добавить restore seam для playable session из сохранённого authoritative state и canonical deterministic metadata — G5, G7
- `game/src/main/kotlin/dev/mysd/game/persistence/RunSave.kt` — использовать существующий full-state payload и сохранить explicit legacy absence без synthetic reconstruction — G7
- `game/src/test/kotlin/dev/mysd/game/simulation/ReplayVerificationTest.kt` — сравнить uninterrupted и restored trajectories на playable payload — G5
- `game/src/test/kotlin/dev/mysd/game/battle/playable/PlayableBattleCombatTest.kt` — проверить freeze/no-op contract после restore of defeat — D9
TEST_TYPES: unit, integration, replay, persistence
CONSTRAINTS:
- SPEC-06 already owns the versioned full-state codec; this slice consumes that payload and must not duplicate or redesign its wire format — G7.
- Restore rebuilds the authoritative playable session only from `RunSave` payload and canonical deterministic metadata; `CampaignSession`, Activity and Compose state are out of scope — G1, G5, G7.
- Same seed, saved state and command log must produce the same per-tick hash trajectory as uninterrupted execution for active runs — G5, G7.
- Restored defeat remains terminal-frozen: advance, pause/resume, build, upgrade and other mutating commands stay no-op or rejected exactly as in uninterrupted terminal state — D9.
- Legacy contour-only saves remain outside this restore path; they are not silently promoted into a synthetic playable state and return an explicit unsupported/absent-payload outcome — G7.
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

SPEC-06 уже поставил wire contract полного playable state; оставшийся gap — доказать, что из этого payload действительно поднимается тот же Android-free battle run. Этот SPEC закрывает только restore/replay seam до вмешательства campaign или Android.

Staleness: `staleness_auto_rescoped=1`; existing RunSave payload codec is retained as the dependency, while session restore and restore-specific proofs remain to implement.

## Implementation links
- commits: a99a19c, 8f917de
- files: game/src/main/kotlin/dev/mysd/game/simulation/SimulationSession.kt, game/src/test/kotlin/dev/mysd/game/battle/playable/PlayableBattleCombatTest.kt, game/src/test/kotlin/dev/mysd/game/simulation/ReplayVerificationTest.kt
- verification: scoped :game runner 199 passed / 0 failed / 0 skipped; full runner 199 passed / 0 failed / 0 skipped; lint ok; assembleDebug successful; deterministic reviewer pass; semantic primary pass with 12/12 coverage and liveness retry recorded partial/stalled; independent critic pass with three non-blocking warnings; full verifier pass
- staleness_auto_rescoped: 1
