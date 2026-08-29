# PHASE 03 — Accepted Android contour and service boundaries

<!-- mp:plan:gen id=PHASE_03 hash=8f34b6c2 generated=2026-08-03 -->
## Goal

Implement the accepted campaign, battle, enhancement, victory, roster, settings, reward, and Arena contours on Android and the headless session.

## Anchors (re-read before starting)

- §2.9 FR-100 Enter the accepted campaign contour — slug:fr-100-enter-the-accepted-campaign-contour h:7f3d7674 (≈L50–58 @2026-08-03)
- §2.10 FR-101 Preserve the observed early-battle setup contour — slug:fr-101-preserve-the-observed-early-battle-setup-contour h:1c5b1389 (≈L59–67 @2026-08-03)
- §2.11 FR-102 Expose the observed active-battle affordance contour — slug:fr-102-expose-the-observed-active-battle-affordance-contour h:e51d3969 (≈L68–76 @2026-08-03)
- §2.12 FR-103 Preserve the observed enhancement-choice contour — slug:fr-103-preserve-the-observed-enhancement-choice-contour h:cf9c6a1c (≈L77–85 @2026-08-03)
- §2.13 FR-104 Resolve the observed victory contour — slug:fr-104-resolve-the-observed-victory-contour h:0e82d826 (≈L86–94 @2026-08-03)
- §2.14 FR-105 Preserve roster and local-settings surface contours — slug:fr-105-preserve-roster-and-local-settings-surface-contours h:01bb46ce (≈L95–102 @2026-08-03)
- §2.15 FR-003 Offline service boundary — slug:fr-003-offline-service-boundary h:4feac61f (≈L17–21 @2026-08-03)
- §2.16 FR-106 and FR-107 service adapters — slug:fr-106-keep-the-reward-boundary-local-and-deterministic h:868a471a (≈L103–121 @2026-08-03)

## Prerequisites

- PHASE_02 — done (deterministic headless runtime, persistence, and replay fixtures).

## Deliverables (per module)

- `:game` — accepted contour orchestration, deterministic service adapters, and lifecycle-safe session integration.
- `:app` — Android navigation, immutable snapshot rendering, touch-to-command input, and lifecycle integration.

## Task checklist

- [x] TASK-03.1 Re-read anchors above and confirm only accepted semantic contours are implemented.
- [x] TASK-03.2 **Campaign route** — implement launch, campaign selection, level setup, and unfinished-run prompt contours (FR-100, US-100, AC-100).
- [x] TASK-03.3 **Battle setup** — implement accepted setup choices and start-battle path without inferred effects (FR-101, US-101, AC-101).
- [x] TASK-03.4 **Active battle** — implement wave, base/enemy, speed, pause/resume, and available-build affordance contours (FR-102, US-101, AC-101).
- [x] TASK-03.5 **Enhancement surface** — implement offers, filter visibility, refresh, selection, and return-to-battle contour (FR-103, US-102, AC-102).
- [x] TASK-03.6 **Victory surface** — implement safe local victory resolution and reward panel while retaining ED-0025 as the defeat blocker (FR-104, US-102, AC-102).
- [x] TASK-03.7 **Roster/settings** — implement roster and settings open/close contours with upgrade and toggle effects deferred (FR-105, US-103, AC-103).
- [x] TASK-03.8 **Offline service boundary** — implement deterministic local-adapter handling for accepted service-shaped requests without production SDKs or backends (FR-003, US-003, AC-003).
- [x] TASK-03.9 **Reward adapter** — implement deterministic local normal-reward and multiplier-shaped outcomes (FR-106, US-104, AC-104).
- [x] TASK-03.10 **Arena adapter** — implement offline service-shaped Arena state without network, account, or match behavior (FR-107, US-104, AC-104).
- [x] TASK-03.11 **Visual QA** — render ST-0001/ROUTE-LAUNCH, ST-0002/BATTLE-SETUP, and ST-0003/BATTLE-ACTIVE against registry evidence and file divergences.
- [x] TASK-03.12 **Visual QA** — render ST-0004/BATTLE-ENHANCEMENT and ST-0005/BATTLE-VICTORY against registry evidence and file divergences.
- [x] TASK-03.13 **Visual QA** — render ST-0006/ROUTE-CAMPAIGN, ST-0007/ROUTE-TROOPS, ST-0008/OVERLAY-SETTINGS, ST-0011/ROUTE-ARENA-LOCAL, and ST-0012/OVERLAY-RESUME against registry evidence and file divergences.
- [x] TASK-03.14 **Lifecycle** — verify pause, background, recreate, and process-death restoration through the accepted contour (US-004, AC-004).
- [x] TASK-03.15 **Acceptance** — run the foundation feature scenarios AC-001 through AC-005 and AC-100 through AC-104.
- [x] TASK-03.16 Update PROGRESS.md and close the verified transition to PHASE_04.

## Done criteria

All accepted semantic routes and service-shaped boundaries work offline, lifecycle restoration preserves deterministic continuity, and per-screen structural fit tasks are recorded.

## Verification commands

```bash
cd "$(git rev-parse --show-toplevel)"
./gradlew.bat test :app:assembleDebug
powershell.exe -File scripts/public-safety.ps1
```
<!-- /mp:plan:gen -->

## Notes for next session

Human-owned; no planner-generated notes.

- TASK-03.8 implementation commit: `57f3f25`; reward outcome and Arena adapter semantics remain scoped to TASK-03.9 and TASK-03.10.
- TASK-03.9 implementation commits: `de2a611`, `07bcde6`; deterministic normal/multiplier-shaped outcome enum and 4-cell availability/outcome coverage shipped. Scoped runner `112 passed / 0 failed / 0 skipped`; full runner `112 passed / 0 failed / 0 skipped`, lint ok, assembleDebug successful; public-safety pass. Push was not possible because `GITHUB_TOKEN` is not set.
- TASK-03.10 implementation commits: `24d553d`, `e793697`; offline local/service-shaped Arena route is wired through campaign/session/UI with immutable network-match-blocked state. 118 tests passed; lint ok; verifier pass; both delivery commits were pushed to `origin/main`.
- TASK-03.11 device QA record: commit `a07c6d5`; ST-0001/ST-0002/ST-0003 structural evidence passed on `emulator-5554`. No pixel score was claimed because the device/reference profiles differ and ImageMagick is unavailable; FIT-03.11-001/002/003 remain recorded for PHASE_04.
- TASK-03.12 device QA commits: `8054578`, `495833a`; instrumented Compose checks passed 2/2 and the reviewer/verifier gates passed. ST-0004/ST-0005 visual parity is blocked by preserved-unusable references; FIT-03.12-001/002 and deferred reward semantics remain unchanged.
- TASK-03.13 device QA commits: `f2658e4`, `655cf66`, `c05d9c0`, `7e84371`; connected `ResumeContentUiTest` passed 1/1 and the full runner reported `118 passed / 0 failed / 0 skipped`, lint ok, public-safety pass. No aggregate fit score was produced or claimed; five visual cells remain deferred/uncheckable because the Pixel 9/reference and emulator profiles mismatch, ImageMagick pixel comparison is unavailable, and relevant references include preserved-unusable captures. FIT-03.13-001 through FIT-03.13-005 remain for PHASE_04.
- TASK-03.14 lifecycle commits: `54d5d45`, `e6deac9`; JVM `125 passed / 0 failed / 0 skipped`, connected `11 passed / 0 failed / 0 skipped`, public-safety pass, and `:app:assembleDebug` successful. Deterministic review and full verifier were the recorded fallback after two semantic-review stalls.
- TASK-03.15 acceptance evidence: MP runner `125 passed / 0 failed / 0 skipped`, lint ok; `:app:connectedDebugAndroidTest` `11 passed / 0 failed / 0 skipped` on `Pixel_5(AVD) - 14`; public-safety pass (`124` tracked files, `1156` history paths); spec validator pass (`16` requirements, `10` stories, `10` acceptance, `16` trace rows); `git diff --check` passed.
- PHASE_03 exit: TASK-03.9 through TASK-03.15 are complete and all task rows above are checked. Accepted semantic contours, offline service boundaries, lifecycle restoration, and per-screen structural QA records are verified; visual parity gaps and deferred/excluded mechanics remain explicit. PHASE_04 is now the sole active phase for fit-gate and deferred-scope closure.
