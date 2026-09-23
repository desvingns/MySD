# PHASE 02 — Deterministic headless battle runtime

<!-- mp:plan:gen id=PHASE_02 hash=7d91e2a4 generated=2026-08-03 -->
## Goal

Deliver the Android-free deterministic session, command/replay behavior, separate run/profile persistence, migrations, and vertical-slice fixtures.

## Anchors (re-read before starting)

- §2.7 FR-004 Deterministic simulation — slug:fr-004-deterministic-simulation h:0cb144d5 (≈L22–26 @2026-08-03)
- §2.8 FR-007 Versioned persistence — slug:fr-007-versioned-persistence h:c23b95b8 (≈L38–42 @2026-08-03)
- §3.4 US-004 Returning player — slug:us-004-returning-player h:0df8ec98 (≈L26–32 @2026-08-03)
- §5.1 Delivery rule — slug:delivery-rule h:6285db4b (≈L30–33 @2026-08-03)
- §5.2 Reliability — slug:reliability h:2c2776de (≈L10–16 @2026-08-03)

## Prerequisites

- PHASE_01 — done (foundation, scope, runtime boundary, and persistence contracts).

## Deliverables (per module)

- `:game` — fixed-rate simulation, command log, replay hashes, run/profile stores, migrations, and deterministic fixtures.

## Task checklist

- [x] TASK-02.1 Re-read anchors above and confirm deterministic ordering before coding.
- [x] TASK-02.2 **Simulation clock** — implement fixed 20 Hz authoritative advancement with seeded randomness (FR-004, US-004, AC-004).
- [x] TASK-02.3 **Command log** — implement stable command submission, IDs, ordering, and replay hashing.
- [x] TASK-02.4 **Run save** — serialize active run state, pending commands, RNG state, content version, and terminal result (FR-007).
- [x] TASK-02.5 **Profile store** — serialize progression, currencies, roster, claims, and local service history separately from run save.
- [x] TASK-02.6 **Migration** — implement supported migrations, malformed-input rejection, and explicit future-version failure (US-004, AC-004).
- [x] TASK-02.7 **Scenario fixtures** — add seedable headless fixtures for setup, active wave, enhancement, victory, and structured defeat blocker.
- [x] TASK-02.8 **Replay verification** — compare uninterrupted and save/restore per-tick hash trajectories.
- [x] TASK-02.9 Update PROGRESS.md.

## Done criteria

The same seed and command log produce identical per-tick hashes before and after supported save/restore and migration.

## Verification commands

```bash
cd "$(git rev-parse --show-toplevel)"
./gradlew.bat test
./gradlew.bat test :app:assembleDebug
```
<!-- /mp:plan:gen -->

## Notes for next session

Human-owned; no planner-generated notes.
