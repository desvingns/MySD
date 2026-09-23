# PHASE 01 — Foundation, scope, runtime and persistence

<!-- mp:plan:gen id=PHASE_01 hash=6c7a1e01 generated=2026-08-03 -->
## Goal

Establish the locked engine, public-safety, evidence-gated, original-content, deterministic-runtime, and versioned-persistence foundations.

## Anchors (re-read before starting)

- §1.1 System boundary — slug:system-boundary h:4b877aac (≈L5–22 @2026-08-03)
- §1.2 State model — slug:state-model h:4e05de73 (≈L23–39 @2026-08-03)
- §1.3 Persistence — slug:persistence h:0138d972 (≈L40–48 @2026-08-03)
- §2.1 FR-001 Pinned engine consumption — slug:fr-001-pinned-engine-consumption h:483665bb (≈L7–11 @2026-08-03)
- §2.2 FR-002 Public evidence safety — slug:fr-002-public-evidence-safety h:3bf5345b (≈L12–16 @2026-08-03)
- §2.3 FR-005 Evidence-gated scope — slug:fr-005-evidence-gated-scope h:ed9153e3 (≈L27–32 @2026-08-03)
- §2.4 FR-006 Original public identity — slug:fr-006-original-public-identity h:0735e301 (≈L33–37 @2026-08-03)
- §2.5 FR-007 Versioned persistence — slug:fr-007-versioned-persistence h:c23b95b8 (≈L38–42 @2026-08-03)
- §2.6 FR-008 Observable fit registry — slug:fr-008-observable-fit-registry h:56776a04 (≈L43–47 @2026-08-03)
- §4.1 Confirmed foundation gaps — slug:confirmed-foundation-gaps h:2bbb6c7e (≈L5–12 @2026-08-03)

## Prerequisites

- None.

## Deliverables (per module)

- `:game` — deterministic engine boundary, persistence contracts, content validation, and scope-safe domain foundations.
- `:app` — Android shell integration boundary and public-safety build wiring.

## Task checklist

- [x] TASK-01.1 Re-read anchors above and confirm the accepted scope before coding.
- [x] TASK-01.2 **Engine pin** — verify composite-build resolution at the locked MyEngine commit (FR-001, US-001, AC-001).
- [x] TASK-01.3 **Public safety** — validate rejection of raw reference artifacts and original-only public content (FR-002, FR-006, US-002, AC-002).
- [x] TASK-01.4 **Scope ledger** — keep unsupported mechanics in evidence claims, open questions, or explicit deferrals (FR-005, US-005, AC-005).
- [x] TASK-01.5 **Fit registry** — validate accepted, deferred, and excluded registry rows against FR/AC coverage (FR-008, US-005).
- [x] TASK-01.6 **Runtime boundary** — preserve Android-free deterministic simulation and snapshot-only rendering/input ownership (FR-004, US-004).
- [x] TASK-01.7 **Persistence schema** — define versioned run/profile boundaries and migration rejection rules (FR-007, US-004, AC-004).
- [x] TASK-01.8 **Engine gaps** — record ENG-036, PROC-002, and PROC-015 as gated dependencies without inventing new engine demand.
- [x] TASK-01.9 **Content boundary** — define stable original content IDs and versioned validation fixtures.
- [x] TASK-01.10 Update PROGRESS.md.

## Done criteria

Pinned-engine smoke, public-safety, traceability, and persistence-boundary checks are defined and passing.

## Verification commands

```bash
cd "$(git rev-parse --show-toplevel)"
powershell.exe -File scripts/public-safety.ps1
./gradlew.bat test :app:assembleDebug
```
<!-- /mp:plan:gen -->

## Notes for next session

Human-owned; no planner-generated notes.
