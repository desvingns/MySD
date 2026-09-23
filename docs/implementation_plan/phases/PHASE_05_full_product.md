# PHASE_05 — Integrated complete product and runtime

Status: implementation complete; final verification and evidence-driven stabilization in progress.

## Authority

The user requested the whole game and necessary MyEngine work, autonomous decisions, local service
stubs, and one implementation batch followed by tests. FR-108..FR-117 and DEV-010 describe the
original product choices. They do not replace the wider reference-fidelity goal or prove parity.

## Source closure

- ENG-036 Android-free runtime descriptor/session, typed saves, deterministic queue/tick behavior,
  compatible sandbox facade and Android adapter, source tests and warmed benchmark harness.
- Original six-stage campaign, ten waves, every tower/ally/enemy/boss/hero/modifier role, reachable
  terminals, deterministic battle guards and enhancements.
- Profile ledger, energy, roster/loadout, tech graph, stars/unlocks, claim-once rewards and sweep.
- Local Shop and deferred service stubs, deterministic Arena calculation, all route/overlay states.
- Atomic paired saves, supported legacy compatibility, incompatible/malformed typed rejection,
  interrupted/restored trajectory coverage and immutable rendering projections.
- Android lifecycle, accessible compact/large-font layout, original vector presentation, settings,
  procedural audio/haptic feedback without external creative assets or production services.
- Complete source tests and verification fixtures, including conservative content-peak +25% load.

## Remaining gate

Source closure preceded all execution. Follow `../FINAL_FULL_PRODUCT_VERIFICATION.md`; record
actual results in `../FINAL_FULL_PRODUCT_RESULTS.md`, fix failures in the integrated stabilization
pass, then rerun affected/full gates. Engine JVM/assemble/replay/save/content checks and MySD
JVM/lint/APK/instrumentation compilation have passed. Engine controlled warmed performance now
passes after retained diagnostics and a semantics-preserving sandbox hot-path fix. Exact accepted
local engine commit `1174d21c4e92fddff6316f93bc99384ab6c1c689` is now pinned without publication.
Frozen final consumer build-10 passes 294 JVM tests / 33 suites with zero failures/errors/skips,
debug lint (0 errors / 39 warnings), benchmark lint (0 errors / 38 warnings), and debug,
instrumentation and benchmark APK assemblies.
Three same-build detached checks passed on the separate API35 AVD. Full detached run 03 completed
72 passing / 3 failing / 0 skipped of 75 cases. The 69 baseline PNGs were reviewed; test fixture and
cleanup fixes, window-level capture coverage, and measured layout repairs are source-complete.
Affected build-09 execution passed 7/7, with 234 PNGs retained. Full build-09 execution completed
78 passed / 2 failed / 80, zero skipped: Activity teardown case 59 and screenshot timeout case 79.
These failures remain failures. Unclaimed terminal, claimed terminal and paused-built real process death have
independently audited manual-supplemented evidence, not automatic-driver passes. The final contract
audit found missing explicit hero selection and per-completed-batch autosaves. Those corrections,
profile-v4 migration, Roster/navigation reflow and additional tests were frozen before successful
build-10 execution. The affected build-10 Android run passed 27/27 with 237 fresh manifest-listed
PNGs (79 per profile / 20 fixtures), using private ADB 5039 / emulator-5680. Its full 88-case run is
incomplete: 72 passed, case 73 began without a verdict, and 15 did not start before instrumentation
reported `System has crashed.` The truncated completion file has no elapsed/exit result. Retained
system/DropBox logs establish Watchdog terminating system_server after WindowManager/display/
animation blockage during synchronous layer capture; the underlying graphics cause is unknown.
No full-suite PASS is supported.

Actual build-09 to build-10 profile-3 to profile-4 migration passed with exact natural-run bytes
and quantitatively justified energy refresh. A separate real schema-4 unclaimed-terminal
process-death scenario passed automatically, including actual old-PID disappearance and exact
restoration. These bounded results do not relabel the earlier schema-3 manual supplements or
driver failures, nor prove every schema-4 state.

Visual acceptance was not passed by successful build-10 capture. Reviewed enlarged-font Setup choices
lost part of the tower name, campaign metadata collapsed into a narrow column, and the HUD speed
label fragmented; compact enhancement content overlapped the status clock and a native Setup action
extended into the gesture area. Ordinary system-font-2 inspection confirmed the inset issues and
fragmented Exit Dialog actions; it does not certify Resume. The collected build-11 UI stabilization
is now source-complete, including these fixes, full-width Resume/Exit actions, stable state-holder
exit dispatch after Setup-to-Battle, and hidden/disabled/blocked covered HUD input. Four adaptive
layout and two functional UI regressions were independently reviewed before execution. The
141-file source freeze at 18:42:22 UTC records 94 Android / 294 JVM cases. Build-11 completed
successfully in 11m37s: fresh 294 JVM tests / 33 suites with zero failures/errors/skips, debug lint
0 errors / 39 warnings, benchmark lint 0 errors / 38 warnings, and three APK assemblies. Independent
host archive/signature/manifest and unchanged-freeze inspection passes. No engine/domain/save change
belongs to this UI batch. Affected build-11 verification now passes 33/33, zero failures/skips,
including all six new regressions; 237 manifest-matched fresh PNGs are retained. Independent bounded
review covers 46 compact / 56 compact-font-2 / 40 native images (142 total), finding no new
demonstrated layout blocker. Two blank font-2 claimed-root captures have valid full-display
alternatives, and three named focus images do not fully expose their target; these limits remain
explicit. Native Dialog windows do not inherit the simulated compact/font-2 fixture constraints.
The full 94-case run is underway without a final verdict. The affected pass follows one documented
Wait recovery from a retained ordinary-launch timeout/SystemUI ANR, not a clean-launch pass.
Actual system-font Dialog fit, current-build real process death, full Android and non-debuggable
device frame gates remain open. Prior failures remain retained.
Keep exact engine identity, code/test state, reference limits, public provenance and goal status
honest. Missing reference observations and unmeasured device performance are not passing evidence.
