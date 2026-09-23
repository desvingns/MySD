# MySD Handoff

## CURRENT — engine merged into main; build14 (2026-09-24)

- MyEngine: ENG-036 branch `1174d21` merged into main as `74454178af22…` (conflict with a parallel
  main ENG-036 variant resolved in favour of the MySD-accepted API; main's Phase 14+ sandbox work
  kept). Engine suite 481/0, content/replay/save-compat/selfcheck, Android assemble pass.
- MySD pins that commit; build14 (lock-only change): 294 JVM, lint 0 errors, full Android 99/99.
  Evidence `build/reports/product-full14-20260924/`. Everything is on both repos' main.

## PREVIOUS — build13 final verification complete on this host (2026-09-24)

- DONE: build11 full-run stop diagnosed — Codex turns interrupted 19:39:18Z/19:39:22Z; emulator
  launcher (child of worker shell 29228) ran its termination handler. Recovered log 81 PASS/0 FAIL,
  test82 no verdict; stores exact. AVD now started via Win32_Process.Create
  (`build/reports/launch-swiftshader-detached-20260923-02.ps1`); boot ANRs cleared (recorded).
- DONE: build11 full rerun 94/94 PASS. Real font-2 Dialog check FAILED on build11 (Resume title
  split mid-word) -> build12 (WholeWordText, extracted Dialog overlays, ProductDialogLayoutTest x4):
  affected12 55/55, full12 98/98, real font-2 Dialogs PASS. Ordinary build12 play found a stale
  build Dialog re-opening after wave-end choice/retry -> build13 (slot selection keyed to run and
  input phase + ProductBattleUiTest regression; review warnings hardened).
- FINAL build13 (`build/reports/apks-build13/`): debug `6a5002af…2969`, test `60a55862…0ff7`,
  benchmark `59c4b119…d801`; 294 JVM PASS, lint 0 errors; static gates 15/15; full13 99/99 PASS;
  font-2 Dialogs PASS; OS process death 3/3 automatic PASS (`build/reports/process-death-build13-20260923.md`);
  benchmark 1x/2x p95 240.42/117.45 ms FAIL on SwiftShader, Settings control p95 118.20 ms.
- STATE: device has build13 debug + test installed; real save = paused 2x first-stage run
  `run-59741a7044f2742-1` (tick 265), energy 5, credits 536, 4 claims. Font 1.0, 1080x2424 @420,
  no override. Emulator (launcher 19408/QEMU 30996) and private ADB 5039 (PID 28484) left running.
- OPEN: (1) physical-device (Pixel 9) release frame gate — cannot be decided on this AVD;
  (2) optional bounded `com.yuegame.defender` observation not performed, exact parity unverified;
  (3) residual warnings: legacy migration-only Resume body/buttons untested at font 2; 320x480 dp +
  font-2 battlefield slot targets overlap; build Dialog does not pause the real-time battle.
- DECISIONS pending user: commit/push of the large uncommitted batch (nothing committed or pushed
  in this session); brain-inbox candidates (emulator detachment, binder fd/FUSE, Dialog density)
  await the human gate. MyEngine unchanged at `1174d21c…`, clean.
- Evidence index: `docs/implementation_plan/FINAL_FULL_PRODUCT_RESULTS.md` (top section).

## RESOLVED — SESSION PAUSE (2026-09-23; superseded by the section above)

- The user explicitly requested a documented handoff and short prompt for a new goal-mode
  session. Stop new implementation and verification work in this session. The already-running
  build-11 full Android run may finish and be archived; do not launch supplemental font-2,
  process-death, benchmark, or reference attempts here.
- Continue the original goal, not a narrowed prototype: implement the full original-IP MySD game
  and required MyEngine changes with ads/IAP as local stubs. The user asked for one complete
  implementation batch before tests. The source batch is frozen and the engine is locally pinned;
  future changes require a concrete failure or evidence-backed gap.
- Current baseline: MyEngine commit `1174d21c4e92fddff6316f93bc99384ab6c1c689` is clean,
  184 engine tests and paired performance gates passed. MySD build-11 source freeze covers 141
  files; 294 JVM tests pass, both lint variants have zero errors, three APKs build, host static
  gate passes 12/12, and affected Android tests pass 33/33. Exact evidence and APK SHA values are
  in `docs/implementation_plan/FINAL_FULL_PRODUCT_RESULTS.md` and `build/reports/apks-build11/`.
- At pause request, full-11 device run was still active at 80/94 passed, 0 failed/skipped,
  current case 81. This is **not** a full pass. Read the newer
  `build/reports/product-full11-20260923/` verdict before continuing. The Android worker owns
  private ADB `127.0.0.1:5039`, serial `emulator-5680`; do not use shared port 5037 or device
  commands until explicit ownership handoff. At 19:44:21 UTC private ADB reported the device
  missing, and the previously owned emulator processes were absent, while its private ADB server
  remained alive. At 19:45:27 UTC the worker confirmed no emulator/QEMU process; emulator stdout's
  last write was 19:39:58.143, stderr was empty and exit cause remains unknown. The worker returned
  ADB ownership to root without rebooting. `build/reports/product-full11-20260923/README.md`,
  `host-stop-handoff.json`, and its 65-file host SHA manifest retain the incomplete evidence.
  Treat 14 remaining verdicts as unknown; diagnose the emulator exit before any restart.
  Keep failed/incomplete historical evidence.
- Remaining acceptance work after the full run: verify actual system-font-2 Dialogs with the
  prepared helper, final-APK real OS process death in paused-built, terminal-unclaimed and
  terminal-claimed states, and non-debuggable 1x/2x frame windows with the frozen benchmark helper.
  Review the exact artifact/report limitations; a successful fixture tour does not prove natural
  reachability. If a gate fails, diagnose and fix the demonstrated defect, then rerun the narrow
  affected gate and any final gate invalidated by the change.
- Exact parity with current `com.yuegame.defender` remains unverified. The prior runbook/public
  graph describe older `com.gdzsq.crazy_td` and must not be relabeled. A read-only audit found
  four intact local-only official Play APK splits for version 2.7/vc18. If time and device health
  permit after MySD acceptance, make at most one bounded ordinary offline compatibility observation
  using fresh screenshots and strict stop conditions; see agent handoff below or reference runbook.
  Do not copy proprietary assets or infer mechanics from blocked screens.
- Keep PHASE_05 active and goal incomplete until evidence supports completion. Do not push or
  delete files. The product worktree intentionally contains many in-progress changes; preserve
  unrelated/user edits. Update this handoff, `docs/implementation_plan/PROGRESS.md`, and
  `docs/implementation_plan/FINAL_FULL_PRODUCT_RESULTS.md` with final verdicts in the new session.

## PREVIOUS — build11 final verification (2026-09-23; superseded by build13 above)

- Goal remains active. User requires one complete engine/game implementation batch before tests.
  Production and test sources are complete and frozen; no further source edits are planned.
- Engine: clean accepted local1174d21c4e92fddff6316f93bc99384ab6c1c689, exact lock. Its184tests,
  content/replay/save checks and paired benchmark passed. No remote push or unrelated staging.
- Build11:141-file freeze,294fresh JVM tests/33suites PASS, debug/benchmark lint0errors39/38warnings,
  all three APKs built/audited, host static gate02 12/12PASS. Artifacts: `build/reports/apks-build11/`.
- Android affected11:33/33PASS, zero failures/skips. Full11:94-case device-owned run is IN PROGRESS,
  not yet a pass. Consult `build/reports/product-full11-20260923/checkpoint-latest.json`.
- Affected11 capture integrity:237freshPNG/3profiles/20fixtures. Scoped142-image visual review found
  no new demonstrated production-layout blocker; precise limitations are retained in its reports.
- Android worker exclusively owns privateADB127.0.0.1:5039 / emulator-5680. Owned API35 AVD now uses
  explicit SwiftShader, same userdata/settings. Server28484, launcher2240/QEMU31608. Never touch5037.
  Its coldboot had pre-MySD SystemUI/service ANRs; one ordinary Wait resolved the dialog. Current
  tests run under this documented post-Wait condition, not proof of a renderer/root-cause cure.
- Real profile survived build11 install-r exactly; later ordinary launch timed out but supplemental
  rendering/pair proof restored paused COMBAT tick18/run-9940f1829be67fc5-1, empty heroes, no towers.
  Only full-energy clock advanced. This is not final OS-process-death verification.
- Remaining gates: finish full94 and preserve its captures, actual compact systemfont2 Dialogs,
  final-APK real OS-death in paused-built/unclaimed/claimed states, release-like1x/2x frame windows.
  Benchmark11 capture helper/plan is frozen/reviewed; thresholds16.7ms/<5% are unchanged.
- Exact fresh-reference parity remains unverified; old reference corpus and original human-decided
  mechanics must not be relabeled as observations of com.yuegame.defender. No goal-complete claim.
- All old failed/incomplete builds/device runs remain retained. Detailed chronology follows.

## Integrated full-product batch chronology (2026-09-23)

This section supersedes older NEXT/BLOCKERS and deferred-product statements below; historical
verification records are preserved and must not be presented as verification of this batch.

- User direction: finish the complete MyEngine + MySD implementation as one batch; run tests,
  builds, lint, validators, benchmarks, and device checks only after all implementation and test
  sources are complete. The goal is still active and is not narrowed to a prototype.
- Working contract: `.ai/local/full-product-batch-2026-09-16.md`; public scope FR-108..FR-117,
  DEV-010, and `docs/implementation/ROADMAP.md`. Shop, technology, progression, original defeat,
  sweep, and the complete offline product are now in scope, not deferred.
- MyEngine checkout: `D:/Pet/MyEngine-mysd`, ENG-036 `engine-runtime` plus sandbox/Android adapters.
  Accepted local commit and current lock: `1174d21c4e92fddff6316f93bc99384ab6c1c689`.
  The explicit 39-file ENG-036 commit follows independent functional/performance acceptance and
  includes closeout docs/retro; engine worktree is clean. No push was performed. Never stage
  unrelated dirty files or publish without explicit authorization.
- MySD has the complete original six-stage/ten-wave catalog, battle reducer, profile ledger,
  progression, service stubs, Arena calculation, paired persistence, product facade, Android
  routes, adaptive original icon, and test sources in the worktree. Full implementation and test
  sources were frozen before final verification began. Subsequent fixes are final stabilization.
- Final verification: engine JVM/Android gate passed (184 tests, zero failures/errors/skips),
  selfcheck/content/replay/save gates passed. Original noisy and paired-02 failed benchmarks are
  retained. Semantics-preserving sandbox helper extraction reduced the callback from 433 to 239
  bytecodes; the prescribed final paired-03 set passed A/A -3.440% / +0.596% and A/B -4.094% /
  -3.761%, all goldens verified, unchanged 5% limit. One engine telemetry event and required retro
  were recorded; do not duplicate either. Local commit/pin are complete; no remote publication.
  MySD final gate 06 against that pin passes 274 JVM tests / 32 suites with no failures/errors/skips,
  lint (0 errors / 39 warnings), debug APK and instrumentation APK. Five new regressions cover
  bounded facade claim-history repair without weakening direct-manager duplicate guards.
  App SHA-256: b28286430dc13a06b825e5941a32bbe3dece8397d47d33b6ad4e43c2ea1e35df;
  test APK: c76e1b49f495a46c31be2cbfe34eb570b1aed9ac7fa73b06c6cba549674c8337.
  Earlier Pixel_9/API37.1 diagnostics and two recoverable snapshots remain preserved. Later cold
  boot and one bounded kernel diagnostic did not reach readiness; a new separate
  MySD_Pixel9_API35_20260923 AVD is now running with matching Pixel9 viewport/density.
  Host-owned instrumentation suffered transport/UiAutomation lifetime failures. Detached same-APK
  legacy handoff, malformed recovery, and compact/font-2 settings each passed on API35. This does
  not explain away every API37.1 symptom. Full device-owned API35 run 03 completed all 75 cases:
  72 passed, 3 failed, zero skipped. Failures: stale combined legacy/product fixture, settings
  PixelCopy timeout, and paused-Activity close timeout. Lifecycle cleanup now resumes after the
  stopped-state assertions, avoiding the AndroidX repeated EmptyActivity handshake. No timeout
  was increased. Both stores are independently restored, preserving primary failures.
  All 69 baseline tour PNGs were reviewed; shop/tech/reward layout, enhancement opacity and stale
  product copy are source-fixed. Root-only Dialog captures did not capture the Dialog windows;
  full-display and scrolled-action capture coverage is added without changing fixture state.
  The stabilization batch adds one legacy case and four layout cases (80 total expected).
  The complete post-stabilization Android gate, final visual review and performance acceptance
  remain; manual-supplemented process-death evidence is updated below.
- Later stabilization evidence: gate 07 and gate 08 builds/lint passed; affected-07 was 7/14 and
  affected-08 was 10/14. All previously failing legacy/lifecycle/Roster cases passed in both. All
  affected-08 visual tours passed and real full-display service Dialog images were reviewed.
  The remaining four failures came from Compose 1.11.3's reconstructed TextLayoutResult using
  max constraints with old shrink-wrap size, not a demonstrated clipped button. The test now
  reconstructs at actual node width and retains strict glyph/height/word/no-ellipsis checks.
  Scrolled screenshots additionally exposed real large-font terminal action fragmentation; those
  actions are now full-width vertical and covered by the same four layout cases.
  Gate 09 passed (274 unchanged JVM tests, lint and both APKs); target SHA-256
  `855d707900844948b5df2e5dc39b5db57befa0aea90cb42971accfe1b5fdd723`, test APK
  `c27d0a0ac6ad5c776a165ebbeb2702e1c13f9fdfc59096b643ecbc7f31bb4439`.
  Gate-09 affected Android execution later passed 7/7; its full run finished 78/80 (checkpoint below).
  Final visual verification remains pending. Native Dialog captures
  do not prove compact/font-2 Dialog layout: their separate windows retained native density.
  Actual natural no-command stage-one defeat was reached on build 08 (tick 540, unclaimed);
  build 09 preserved the same domain/persistence implementation and continued from that save.
- Terminal process-death evidence is now independently audited as manual-supplemented proof,
  not an automatic driver pass. On build 09, unclaimed PID 15287 disappeared (`pidof` exit 1 and
  old `/proc` absent) and PID 15623 restored the same unclaimed defeat; claimed PID 15623 then
  disappeared and PID 15830 restored the claimed defeat. Both original `failed.json` results
  remain: the first relaunch returned exit 255; the second returned `Status: timeout`.
  Later retained PID/XML/PNG captures supply the manual supplement. Run documents are exactly
  equal in both restore pairs and across the intervening claim (tick 540, stage-ember-path,
  run-746785d5eb9fb2f3-1). Claim changed credits 500 -> 509, added exactly one current-run marker
  and one BATTLE_REWARD row, and advanced revision/event/ledger cursors once. Restoring the
  claimed terminal caused no additional reward. Only full-energy clock fields advanced on
  restoration (+99s and +85s); other profile fields remained exact. This does not exercise a
  deliberately repeated domain claim call or independently trace all preceding gameplay.
  Evidence: ignored `build/reports/mysd-process-death-host-20260923-terminal-unclaimed-02/`,
  `mysd-real-claim-20260923-01/`, `mysd-process-death-host-20260923-terminal-claimed-01/`, and
  `build/reports/process-death-terminal-independent-audit-20260923.md`.
- Paused-built process-death evidence is also independently supported by a manual supplement in
  `build/reports/mysd-process-death-host-20260923-paused-built-01/`. Ordinary UI play, with no save
  injection, built a level-1 tower-ember-needle in slot-1-1 and paused run-449cc26fa53192ff-1 at
  COMBAT tick 447. PID 15830 disappeared (pidof status 1 and old /proc absent); later PID 16479
  displayed the same pause. Run bytes, tower level/cooldown and all battle state are exact.
  The sole resource delta is legitimate 300s-clock recovery from energy 8 -> 9 over 476 observed
  seconds, with one ENERGY_REFRESH row and one revision/event/ledger advance; credits stay 509
  and the prior claim marker/reward row are unchanged. The original relaunch exit-255 failed.json
  remains failed. This is not automatic driver success or proof of uninterrupted gameplay.
  The independent audit's paused-built supplement records details. The later gate-09 execution
  checkpoint below supersedes the earlier pending status; final visual verification remains open.
- Corrected real-app frame analysis remains a failing debug-AVD observation, not performance
  acceptance. In `build/reports/mysd-frames-host-20260923-1x-01/`, the original 7-frame report is
  retained. New `analysis-reset-window-20260923.json` excludes one complete frame whose
  IntendedVsync precedes the window's Stats since, plus the two already-invalid completion rows:
  6 valid frames, p95/max 3771.164976ms, median 2065.524292ms, all 6 above unchanged 16.7ms.
  The analyzer reports global/per-file pre-window exclusions and raw process/window boundaries;
  window-local boundary overrides the process boundary and equality is included. All three
  synthetic analyzer self-test scenarios passed (tooling evidence, not device performance).
  Raw platform counters remain separate: total 8, janky 6 (75%). Detached run-as capture had
  returned empty XML; host-side captures avoid that invalid evidence path. Do not infer RAM,
  synchronous fsync, or another sole cause from these observations.
- Independent integration review found and source-fixed input-triggered extra simulation ticks:
  ENG-036 now exposes an opt-in protected current-tick command drain; MySD applies input-only
  mutations without running passive systems. Sandbox retains its old behavior. Source regressions
  cover 400 repeated controls without clock/income/cooldown/movement advance.
- A same-machine engine baseline archive is retained at
  `D:/Pet/MyEngine-mysd/build/eng036-baseline-20260923` (exact pre-extraction baseline
  `30f4eb17aff0ea2fe6cf80aef970a1e7746dbcbb`). The warmed comparison
  script has a retained failed first report. MySD peak+25% load/capture fixtures are authored.
- Final execution plan: `docs/implementation_plan/FINAL_FULL_PRODUCT_VERIFICATION.md`.
  Current evidence: `docs/implementation_plan/FINAL_FULL_PRODUCT_RESULTS.md`.
  Earlier passing first-playable reports do not satisfy this new gate.
- Reference limitation persists: official `com.yuegame.defender` v2.7/code18 cannot yet be crawled
  reliably on the available AVD profiles (native page-alignment failure, tutorial ANR, or Play
  dependency). The accepted older `com.gdzsq.crazy_td` corpus is not proof of exact new-package
  parity. Original implementation decisions must never be relabeled as observations.
- No files are deleted. Raw reference data remains ignored under `.reference-local/`.

### Post-build-09 completion audit and corrective source batch

- Final bounded whole-contract review also found that ordinary completed pulses were saved only
  every 20 ticks, missing the promised per-batch/ordinary-wave checkpoint. Root changed
  `ProductViewModel.pulse` to persist each changed tick batch (1x/2x), leaving frozen pulses without
  redundant writes and retaining atomic paired storage/failure retry. Four memory-backed Android
  regressions are source-frozen and independently reviewed before build 10. This is a contract
  correction, not a proven performance fix. All source was frozen before build10 execution;
  build10 subsequently passed all 294 JVM tests / 33 suites and the complete build/lint gate.

- Affected-09 completed 7/7 Android tests (4 text/layout regressions and 3 visual tours), zero
  failures. Its 234 PNGs / 3 manifests are preserved in
  `build/reports/product-affected09-20260923/product-visual/`. The full 80-case build-09 run then
  finished at 17:09:50 UTC after 2344 seconds: 78 PASS / 2 FAIL / 80, zero skipped. Failures are
  test59 ActivityScenario DESTROYED timeout and test79 compact screenshot timeout. The other two
  tours passed. This remains historical evidence for that exact APK, not later source. The Android
  worker archived all evidence and same-APK isolated test59 passed 1/1 (99.675s JUnit). That slow
  isolated pass does not erase the full-suite failure. Full09's verified second TAR export has
  233 fresh manifest-listed images; the compact final service image is stale and not fresh proof.
- Readonly host diagnostics found an additional scrcpy-distribution ADB client polling
  `dumpsys package com.google.android.gms` on emulator-5554, and the host adb.log explicitly records
  repeated server kills by remote request. Its controlling process is unknown and was not stopped.
  SDK and scrcpy ADB versions match (1.0.41 / 37.0.0); no version-mismatch diagnosis is supported.
  Evidence is `build/reports/host-adb-interference-20260923-1708.md`. A first exec-out transfer of
  the full09 visual TAR was truncated despite exit 0; the worker retains it and retries by pull
  with exact device/host size and SHA checks. This does not automatically explain all test stalls.
  Root saved a new recovery snapshot and hash-verified both preference exports, then restarted
  only this AVD at17:29:32 UTC on private ADB5039 / console5680 / transport5681. It is now
  emulator-5680, private server PID28484, QEMU PID31220. RAM/GPU/kernel/data are unchanged; both
  preference hashes match before launching the app. USB, mDNS and emulator scanning are disabled
  only in the private processes. Shared5037 was not stopped. Setup details and two preserved
  rejected startup commands are in `build/reports/avd-private-endpoint-20260923-01/README.md`.
  Same-build09 private-endpoint isolated test59 passed1/1 in5.647s (previous99.675s); the serverPID
  stayed28484. Coldboot+isolation are confounded, not a causal finding. Root installed both build10
  APKs with `install -r` over exact preserved data, then ordinary launch migrated profile3->4 and
  restored the exact natural run. Evidence: `build/reports/product-upgrade-build10-20260923-01/`.
  Root then obtained automatic schema4 terminal-unclaimed OS-death PASS: PID4246 absent, newPID4438,
  exactrun, invariant hero selection/claim/currency/ledger, only full-energy clock+79s. Before/after
  screenshots are identical and reviewed. Evidence: `mysd-process-death-host-20260923-terminal-unclaimed-03`.
  Affected10 passed27/27 (JUnit249.671s, helper253s). Verified237fresh PNGs,79/profile20fixtures,
  zero missing/unlisted; archiveSHA3c1c610c02112b84ff041c6b5bdca787d1c0e613158b92e562b3981e5445e632.
  Full10(88) is INCOMPLETE:72PASS, test73 started without verdict,15 not started. At18:01:37UTC
  Android Watchdog killed system_server610 after WindowManager/display/animation checks were74s
  overdue; replacement6914 started with unchanged kernel/host-ADB lifetime. The final sentinel
  is truncated, not a complete runner/JUnit success. No automatic rerun was made. The visual TAR
  exported after this abort is exactly the retained affected10 TAR, NOT fresh full10 tour evidence.
  `product-full10-20260923/README.md`, `ANR_ANALYSIS.md`, full logs and DropBox retain the failure.
  DropBox additionally shows android.anim holding WindowManagerGlobalLock while waiting for
  SurfaceFlinger captureLayersSync during an OS task snapshot; underlying graphics-stall origin
  is not established. Direct private ANR trace reads were denied; no privilege bypass was attempted.
  ADB returned to root after the abort. Root reviewed46font2 captures, reviewers41compact and28native
  (115distinct of237). Confirmed findings: Setup choice-name truncation, campaign header/metadata
  squeeze, HUD speed-word fragmentation and missing safe insets. Findings remain visual failures
  despite affected27PASS.
- Root completed ordinary MainActivity system-font2 inspection on build10, including true separate
  Service and Exit Dialogs and an actual320x480dp display override. Fullwidth Service content fits;
  Exit half-width actions fragment words, while its own Dialog viewport already avoids systembars.
  Activity pause content behind it overlaps the statusbar; Setup content extends to the gesture
  area. Resume systemfont2 was not reached through this ordinary route and is not certified.
  Restored exact original font_scale1.0 and physical1080x2424/density420 at18:19:28UTC.
  Evidence: `build/reports/product-dialog-system-font2-build10-20260923-01/README.md`.
  Ordinary second-total reward claim is independently audited: credits509->518, one new claim and
  BATTLE_REWARD row, exact run1784bytes, old claim retained, only legitimate full-energy clock
  movement. Then ordinary Setup disabled the unlocked hero and started run-9940f1829be67fc5-1;
  final inspection save is COMBAT tick18, paused, empty hero selection, no built tower.
  This is build10 supplementary evidence, not final build11 process-death acceptance.
- Actual Setup->Battle->Pause exposed a functional exit-dispatch defect: tap Exit and system Back
  did not open confirmation. Fresh process launch directly into Battle restored working Back/Exit.
  Independent source review confirms rememberSaveable(isBattle) replaced its state holder while
  remember(onAction) retained a dispatcher targeting the old holder under a stable host callback.
  Direct-Battle fixtures masked the route-transition case. UI stabilization batch11 is complete:
  explicit holder-keyed dispatcher, full-width Exit/Resume and Setup choices,
  adaptive campaign header/metadata, concise visible1x/2x with full speed semantics, safe Activity
  overlay/non-navigation insets and enlarged-type HUD spacing. No engine/domain/save change.
  Covered HUD input and semantics are also disabled during modal battle phases. One real-ViewModel
  stable-callback transition regression, one modal-input regression and four adaptive-layout cases
  are authored and independently reviewed (94 Android cases total). Root froze all141 files at
  2026-09-23T18:42:22.7525153Z in `build/reports/source-freeze-build11-20260923.json` before any
  execution. Build11 passed in11m37s (148tasks,47executed,101up-to-date): all three APK variants,
  debug/benchmark lint and forced fresh `:game:test`,294tests/33suites, zero failures/errors/skips.
  Fresh suite timestamps are18:43:31.372Z..18:43:40.109Z; the dedicated checkpoint and full log
  are in `build/reports/`. The independent three-APK audit passed; archives, signature/manifest
  evidence and provenance are in `build/reports/apks-build11/`. DebugSHA
  `df841b00e78c05b7d5bf8ea966c7dea2cefc73f244955c4ed63920763b36c63d`, testSHA
  `aadde0bec4ed63bfb4e107f7be1d14f8b0680cfb15a266a6150f1b85a99cff95`, benchmarkSHA
  `0d8eb7022108d4a39f1121152d160b33168cff5c7f9f8e1dd73a28f62537d671`. All retain the
  local debug signature, package/version and noINTERNET; benchmark is nondebuggable/shell-profileable.
  Android/device verification is not yet complete.
  Debug lint has0errors39warnings; benchmark lint has0errors38warnings. Host static gate11-02
  passed12/12 (public safety146tracked/2019historical, specification, evidence, diff whitespace,
  three synthetic frame-analysis scenarios, four retained schema3 proof pairs, runner syntax,
  exact141-file source freeze and clean engine pin). These historical proof comparisons are not
  new schema4 device evidence. Gate11-01 is retained:5PASS/7environmentFAIL because Windows
  PowerShell5.1 inherited incompatible PowerShell7 module paths and could not resolve Get-FileHash.
  Gate02 changed only child-process PSModulePath to its own system modules; the standard command
  resolved and all unchanged checks passed. No machine-wide environment setting was changed.
  Android worker owns private ADB5039. One controlled SwiftShader coldboot passed readiness and
  exact before-app-launch preservation of both stores and installed build10 APKs. All401offline
  backup files (11,503,940,056bytes) plus locator INI were hash-verified; snapshot
  `mysd-before-software-20260923-1856` is retained. New launcher2240/QEMU31608 started19:00:32UTC,
  server28484 unchanged; actualGLES/Vulkan SwiftShader, WHPX/4CPU/2560MB/API35rev9/kernel and
  physical1080x2424/density420/font1 are recorded. Coldboot took222329ms. Before shutdown a
  second Watchdog event at18:53:34 blocked android.io85s; this is a different observed thread,
  not proof of a sole GPU cause. Early CE-store-unavailable and pm-path-timeout captures remain
  failed diagnostics, not data-loss evidence. See `build/reports/avd-swiftshader-20260923-01/README.md`.
  Build11 install-r then affected33/full94 device-owned execution is authorized after ordinary
  launch continuity; no device verdict yet. This diagnostic change is not an established
  graphics-stall cure or a physical-device performance result. No further source edits are planned.
  Build11 app/test install-r subsequently succeeded, installed hashes match the archive and both
  stores were exact immediately after install. The sole ordinary launch returned Status:timeout
  (14538ms, UNKNOWN launch state); it is retained as a failed automatic launch, not relabeled PASS.
  A later read-only supplement found PID4707 rendering the same paused tick18 battle and strict
  restore comparisonPASS: exactrun, unchanged currency/claims/ledger/revision, only full-energy
  clock+2995s, legacy store exact. However a real SystemUI ANR dialog covers the game in
  `product-upgrade-build11-20260923-01/supplement-after-launch.png`. No instrumentation has started.
  Root authorized one observed ordinary Wait action and bounded settling after logs, not repeated
  dismissal/restart. Any recurring ANR stops device execution. Root/engine are host-only meanwhile.
  One XML-grounded Wait tap occurred19:12:24UTC. Captures19:13:44..47 show unobscured pause;
  SystemUI935/MySD4707/system_server623 remain the same live processes, and bounded post-Wait
  logs contain no new ANR/watchdog. Affected33 is now authorized/running once in this explicitly
  post-Wait condition. The earlier launch timeout and SystemUI ANR remain failures, not erased.
  Independent host-log review establishes that SystemUI ANR predates MySD: duplicate ANR entries
  at19:05:05.893/19:05:43.309 in the new boot log versus MySD START19:09:10.259 and
  Displayed19:09:27.451(+17s315ms). Other Android services also had pre-app boot delays/ANRs.
  No exact blocked SystemUI stack is available; neither MySD responsibility nor a GPU cure is
  established. Supported lastanr output says noANR despite the actual PNG/XML and boot-log evidence.
  Build11 affected Android gate completed33/33PASS, zero failures/skips, final JUnitOK and complete
  sentinel captured19:21:11UTC in `build/reports/product-affected11-20260923/`. This includes all
  four adaptive cases, stable-callback exit transitions, four-phase modal input/semantics, and
  three fixture visual tours. No OS-process-death or frame-performance acceptance is inferred
  from this pass; the complete94-case gate remains pending.
  The affected11 tour archive is verified:237freshPNG,79/profile,20fixtures/profile, no missing
  or unlisted images, device/host TAR size26,280,448bytes and exactSHA before extraction.
  Scoped visual reviews inspected142distinctPNG: native40, compact46, compact-font-2 56;
  every profile covers all20 named fixtures. No new demonstrated production-layout blocker was
  found. See the three `visual-review-*.md` reports in the affected11 directory for exact scope.
  Two font2 claimed-root images are blank and unusable; full-display counterparts show correct
  panels. Several named focus images do not visually prove their named target. Native Dialogs
  remain physicalfont1, not compact-font2 evidence. Real systemfont2 Dialog verification is pending.
  Full11/94 started19:23:43UTC; at19:29:32 it has23PASS/0FAIL, case24 running. No complete verdict.
- A bounded contract audit found a real omission in FR-110/FR-113: hero abilities could unlock,
  upgrade and execute, but could not be selected independently; all unlocked abilities were
  automatically equipped. A corrective source batch now adds explicit hero selection in roster
  and setup, independent of seven tower/ally slots. Profile schema 4 migrates schema 3 and older by
  retaining their implicit all-unlocked selection; existing runtime schema 5 is unchanged. Empty
  selection is valid. New tests are authored before any execution against the corrective batch.
  Do not claim build-09 tests verify this later implementation. Engine pin is unchanged.
- The same visual audit found pre-existing narrow roster action fragmentation. The corrective
  Android source stacks full-width roster content/actions and enables horizontally scrollable
  whole-word navigation for narrow widths as well as enlarged font. New strict assertions retain
  prior text-fit/action guards. Root Android hero tests add 3 cases, meta guard tests add 1, so the
  expected full Android suite becomes 88 with the four autosave cases (not yet executed).
  The domain adds 20 tests, giving 294 passing JVM tests / 33 suites with zero failures/errors/skips.
  All corrective production/test sources
  are now frozen and independent Android/domain/autosave source reviews found no concrete defects.
  The140-file freeze was independently rechecked with exact hashes/lengths and no added/missing
  sources. Build10 passed in10m15s: test, lintDebug, lintBenchmark, assembleDebug,
  assembleDebugAndroidTest, assembleBenchmark. Debug lint0errors39warnings; benchmark0errors38warnings.
  Build09 and build10 APKs are separately archived under `build/reports/apks-build09/` and
  `build/reports/apks-build10/`; do not overwrite either. Ten host-only gates passed, including
  strict regressions against four old schema3 proof pairs (not new schema4 device proof).
- Eight-second build-09 atrace is retained at
  `build/reports/mysd-atrace-build09-20260923-01/ANALYSIS.md`: six main-thread fsync operations
  total 116.7ms, while main RenderThread waits total 2.868s and include overlapping flush/swap/
  dequeue stalls. JIT is active but not the sole cause. No observed swap-out/reclaim/allocation
  stall counter increase justifies a RAM or persistence rewrite. No production performance change
  was made. A separate non-debuggable, locally signed, shell-profileable benchmark variant is now
  built to obtain release-like diagnostics after Android verification. Its APK and debug APK
  have the same local signing certificate; no real benchmark measurement is claimed yet.
- Original profile-3 proof helpers are archived in ignored
  `build/reports/proof-helpers-profile3-20260923/`. Current readonly helpers additionally accept
  strict same-schema profile-4 comparisons and treat selectedHeroSkillIds as invariant; cross-schema
  migration is deliberately not accepted as a plain restoration comparison. Existing evidence and
  automatic failures remain unchanged.

## Historical handoff (before the full-product decision)

## DONE

- Repository foundation with Gradle composite build and pinned MyEngine commit.
- Android/JVM scaffold proving engine resolution.
- CI design for exact engine checkout, public-safety, tests, and Android assemble.
- Luna crawl runbook, local raw corpus contract, state graph/capture schemas, claim ledger, and
  coverage gate.
- Pre-Gate spec baseline, deviations, gap analysis, implementation roadmap, and process playbook.

- Reference crawl completed on the authorized Pixel 9 AVD for com.gdzsq.crazy_td.
- Raw screenshots and UI dumps remain local under .reference-local; the sanitized evidence bundle is Gate 1 accepted for semantic/behavioral scope.
- Three early-battle trials were captured with varied setup choices; victory was observed, defeat remains blocked.
- Follow-up crawl added 16 indexed captures, six empty plateau iterations, action-level traces for all 25 graph edges, explicit victory/resume linkage, and structured defeat/resource/bounds/signature blockers.
- No personal data, purchase, rewarded-ad completion, or Arena network request was performed.
- Independent Gate 1 recheck confirmed 276/276 indexed raw artifacts exist with matching SHA-256, 25/25 action traces and before/after anchors, zero unmatched affordances, and a state graph conforming to its v1 schema.
- Before Gate 2 evaluation, the production bundle was frozen; the relaxed evaluator then added only
  accepted-scope semantic rows. Engine-gap analysis, MyEngine backlog, and the pinned MyEngine commit
  remained unchanged; no new engine demand or production SDK scope was introduced.

## DECISIONS

- Public original-only repository.
- Raw reference artifacts remain local and ignored.
- Offline services use deterministic local adapters.
- No gameplay requirements or new evidence-driven ENG cards before Gate 1.
- Brain inbox promotion remains human-gated and occurs after Gate 2.
- Human accepted the relaxed Gate 1 inventory, scope proposals INV-001 through INV-008, and DEV-001
  through DEV-009. Complete visual evidence moves to per-surface Visual Fit Gates.
- Structured defeat blocker ED-0025 and observed potion=0 plus the unavailable-energy blocker are
  sufficient for Gate 1. GameCanvas bounds may remain structurally unavailable.
- Shop and Tech stay deferred; ads, IAP, and Arena remain deterministic local-adapter boundaries;
  low-confidence claims cannot become requirements.
- Gate 2 was accepted in relaxed semantic scope after evaluator pass; Visual Fit and implementation
  remain separate phase gates.

## NEXT

1. PHASE_04 is complete: all six evidence-backed divergence SPECs are in `.claude/specs/done/` and all unexplained fit divergences are resolved.
2. Keep the aggregate pixel score unclaimed while the Pixel 9/reference profile, invalid legacy references, and ImageMagick tooling remain incompatible; locked deferred/blocked scope stays unchanged.
3. Bridge any engine demand only through a separate accepted MyEngine feature run; do not promote Shop, Tech, defeat, reward transaction, real-ad, IAP, or network Arena behavior.

## IMPLEMENTATION UPDATE — 2026-08-03

- PHASE_02 TASK-02.2 is complete: Android-free `SimulationClock` advances at exactly 20 Hz,
  `SimulationSession` delegates seeded deterministic stepping to the locked MyEngine core,
  systems use stable order/id sorting, and public snapshots expose immutable tick/hash metadata only.
- Verification: project-local runner `21 passed / 0 failed / 0 skipped`, lint `ok`, full verifier pass.
- Active composite checkout was restored to the exact `gradle/myengine.lock` SHA
  `30f4eb17aff0ea2fe6cf80aef970a1e7746dbcbb`; untracked MyEngine `.kotlin/` remains preserved.
- PHASE_02 TASK-02.3 is complete: `CommandLog` allocates monotonic IDs, rejects duplicates,
  canonicalizes with the pinned MyEngine comparator, emits delimiter-safe encoding, and exposes
  deterministic input/replay hashes through `SimulationSession`.
- Verification: project-local runner `28 passed / 0 failed / 0 skipped`, lint `ok`, full verifier pass.
- PHASE_02 TASK-02.4 is complete: RunSave is schema v3 with canonical pending command identity/metadata,
  signed deterministic state, explicit terminal invariants, and tested v1/v2 migrations; it remains
  separate from ProfileStore and Android state.
- Verification: project-local runner `34 passed / 0 failed / 0 skipped`, lint `ok`, semantic review pass, full verifier pass.
- PHASE_02 TASK-02.5 is complete: ProfileStore schema v2 canonically persists progression,
  currencies, energy, roster/loadout, tech, claims, and deterministic local service history;
  set/map/list invariants and v1 defaults are covered without mixing RunSave state.
- Verification: project-local runner `38 passed / 0 failed / 0 skipped`, lint `ok`, semantic review pass, full verifier pass.
- PHASE_02 TASK-02.6 is complete as a validation-only task: RunSave/ProfileStore migration
  branches and typed malformed/duplicate/unknown/future rejection are present and canonical
  post-migration encoding is stable.
- Verification: static migration checks pass; project-local runner `38 passed / 0 failed / 0 skipped`, lint `ok`.
- PHASE_02 TASK-02.7 is complete: five seedable Android-free fixture descriptors cover the
  accepted setup, active-wave, enhancement, and safe-victory contours plus the explicit ED-0025
  defeat blocker; blocker is non-terminal and not playable.
- Verification: project-local runner `43 passed / 0 failed / 0 skipped`, lint `ok`, full verifier pass.
- PHASE_02 TASK-02.8 is complete: replay verification compares uninterrupted and save/restore
  trajectories by ordered tick/state hash, with stable first-mismatch diagnostics and missing/
  extra/reordered detection; actual lifecycle reconstruction remains a later integration concern.
- Verification: project-local runner `50 passed / 0 failed / 0 skipped`, lint `ok`, full verifier pass.
- PHASE_02 is complete: all TASK-02.1 through TASK-02.9 are checked, the phase row is `done`,
  and PHASE_03 is now the sole `active` phase. The next work is the accepted Android contour and
  deterministic offline service boundaries; no PHASE_03 production work was mixed into PHASE_02.
- PHASE_03 TASK-03.1 is complete: accepted FR-003/FR-100–FR-107, AC-003/AC-100–AC-104,
  deviations, fit registry, and deferred/excluded scope were re-read. Next is TASK-03.2 campaign route.
- PHASE_03 TASK-03.3 is complete: accepted battle setup choices, tutorial continuation, and a
  deterministic start-battle handoff were added without inferred choice effects, copied reference
  content, or active-battle mechanics. Production commit is `c69a3e2`; tester changes remain in
  the working tree pending human delivery review.
- Verification: project-local runner `74 passed / 0 failed / 0 skipped`, lint `ok`, deterministic
  reviewer pass, semantic review pass, independent critic pass with a warning about uncommitted
  tester files, and full verifier pass. Compose UI and Android navigation smoke coverage are
  explicit exceptions because the current `:app` module lacks those test dependencies/seams.
- PHASE_03 TASK-03.4 is complete: the Android-free active-battle contour now exposes deterministic
  wave/base/enemy visibility plus speed, pause/resume, and available-build affordances. Speed has
  no multiplier, pause has no clock effect, and build selection has no cost/effect semantics.
- Verification: `:game:test`, `:app:assembleDebug`, `public-safety.ps1`, and `git diff --check`
  passed. Compose UI and Android navigation smoke coverage remain explicit exceptions because the
  current `:app` module lacks those test dependencies/seams.

- PHASE_03 TASK-03.5 is complete: the Android-free enhancement contour exposes two stable original
  offers, all-filter visibility, deterministic refresh revision, selection, and return-to-battle.
  Repeated OpenEnhancement is idempotent while the choice contour is visible; a later false-to-true
  transition creates a fresh choice session. Offer effects, costs, persistence, and reroll rules
  remain deferred.
- Delivery commits: `0e9bec0`, `e3bed14`, `aa64906`, and `06d41be`. The foundation content,
  persistence, simulation, and test sources required by the app/game composite are now tracked in
  the task chain, and the final diff-check is clean.
- Verification: clean git archive passed `:game:test`, `:app:assembleDebug`, and `:app:lintDebug`
  with 85 tests passed, 0 failed, 0 skipped, using MyEngine lock SHA
  `30f4eb17aff0ea2fe6cf80aef970a1e7746dbcbb`. Deterministic reviewer, semantic review,
  independent critic, and full verifier passed. Compose UI and Android navigation smoke coverage
  remain explicit exceptions because the current `:app` module lacks those dependencies/seams.
- Scoped fit for `ST-0004/BATTLE-ENHANCEMENT` was skipped: reference `EV-0041` is preserved_unusable
  with an invalid PNG, so no visual score or divergence was invented.
- PHASE_03 TASK-03.6 is complete: the accepted playable contour now reaches a deterministic local
  Victory surface with an immutable reward-panel snapshot after enhancement return-to-battle;
  ED-0025 remains a structured non-terminal defeat blocker and no defeat mechanic was added.
- Delivery commits are `c04b175` (`feat: add safe victory reward panel`) and `c6aa856`
  (`test: strengthen victory contour coverage`). Fresh installed APK manual smoke reached
  campaign -> setup -> active battle -> enhancement -> return -> Resolve victory -> Victory/reward
  panel. Reward claim, doubling, transaction, economy, and service semantics remain deferred.
- Verification: MP runner `91 passed / 0 failed / 0 skipped`, lint ok; public-safety pass;
  `test :app:assembleDebug` successful; full reviewer, semantic review, independent critic, and
  verifier passed. Compose/navigation smoke remains an explicit dependency exception. Scoped fit
  skipped because `ST-0005` reference evidence is preserved_unusable.

## BLOCKERS

- Gate 1 is accepted under relaxed policy; no evidence-policy blocker remains.
- Shop, Tech, reward transaction semantics, Arena network, and unobserved mechanics are deferred and cannot create requirements.
- Legacy invalid PNGs, incomplete visual signatures, and Canvas child bounds are non-blocking for Gate 1 and remain work for per-surface Visual Fit Gates.
- Gate 2 is accepted in relaxed semantic scope; implementation and Visual Fit remain phase-gated.
- Push of `c04b175` and `c6aa856` to `origin` was attempted after explicit approval but failed with
  GitHub `Invalid username or token`; commits remain local and no further credential retry was made.

## VERIFICATION

- Independent corpus audit -> pass for existence and SHA-256 (276 indexed artifacts; 137 screenshots, 138 UI dumps, 1 metadata record; zero missing or mismatched files).
- Action trace audit -> pass (25 unique edge traces with screenshot/UI-dump pairs; six empty plateau iterations); graph edges contain before/after screenshot anchors.
- State graph schema audit -> pass against the draft 2020-12 constraints used by state-graph.v1.schema.json.
- Signature/bounds audit -> deferred to Visual Fit Gates (7 nodes with usable visual evidence, 1 valid anchor with signature deferred, 5 preserved-unusable legacy contours; all 26 affordances carry a structured bounds-unavailable reason).
- `scripts/validate-evidence.ps1` -> pass in the accepted-state branch (13 nodes, 25 edges, 12 observations; Gate 1 status `accepted`).
- `scripts/validate-spec.ps1` -> pass for the relaxed Gate 2 bundle (16 FR, 10 US, 10 AC, 16 trace rows).
- `scripts/evaluate-gate2.ps1` -> pass in relaxed mode (16 FR, 10 US, 10 AC, 16 trace rows, 13/13 fit-registry nodes).
- `scripts/public-safety.ps1` -> pass (rechecked at Gate 1; no raw reference artifact is tracked).
- `.\gradlew.bat :game:test :app:assembleDebug` with JDK 17 and Android SDK -> pass.
- MyEngine `scripts/me-selfcheck.ps1` in the isolated backlog worktree -> pass.
- Independent `me-verifier` -> pass after strengthening Gate 1 evidence references and creative
  asset provenance checks.
- Relaxed-policy ready-state validation -> pass; explicit terminal and negative-access blockers are machine-validated, deferred scope cannot promote before human acceptance, and visual evidence is tiered per node.
- Independent relaxed-policy `me-verifier` -> pass after structured blocker, evidence-tier, bounds-reason, and deferred-scope enforcement checks.

## RECOVERY CRAWL UPDATE — 2026-07-19

- The 11 pre-existing recovery screenshots and paired UI dumps were audited. All PNG probes passed binary validation, but they were not promoted because the trace contains no action-level route/state linkage; repeated battle frames and one launcher frame were retained as `probe_not_promoted`.
- A further 23 binary-safe captures were made on the authorized Pixel 9 AVD. `EV-0233` is a validated fresh anchor for `ST-0001`; `EV-0243` is a validated fresh anchor for `ST-0003`. Both use fresh DCT pHash signatures; no graph edge was promoted.
- All 34 recovery screenshots are indexed with paired UI evidence mappings and SHA-256 values. Legacy 95 screenshots remain untouched. Technical valid PNG total is 42; eight nodes have valid anchors, seven currently support visual claims, and complete before/after visual pairs remain deferred.
- A test-ad surface appeared during a probe and was closed immediately. No rewarded-ad completion, purchase, credential entry, Arena request, or unsafe defeat forcing was performed.
- The recovery crawl itself ended blocked; the relaxed policy was accepted for Gate 1 and Gate 2. Production implementation, backlog bridge, and Visual Fit remain phase-gated.

## IMPLEMENTATION UPDATE — 2026-08-10

- PHASE_03 TASK-03.7 is complete: roster and local-settings open/close contours are wired through the Android-free `RosterSession` and `CampaignSession`; upgrade and toggle intents remain visible-affordance no-ops, and immutable snapshot boundaries are covered.
- Delivery commit: `d86a819` (`test: cover roster and settings contours`); the production contour was already present in `230aad4`.
- Verification: deterministic reviewer, semantic review, independent critic, full verifier, and MP runner passed; runner reported `104 passed / 0 failed / 0 skipped`, lint `ok`. `test :app:assembleDebug` and `scripts/public-safety.ps1` passed with 110 tracked files and 886 history paths checked.
- Scoped fit for `ST-0007/ROUTE-TROOPS` and `ST-0008/OVERLAY-SETTINGS` returned overall score `50` (screen scores `34` and `66`) with major structure/geometry divergences. Proposed fit SPECs were not written; creative, copy, and accessibility deviations remain acknowledged.
- Push of `d86a819` was attempted after explicit approval but failed with GitHub `Invalid username or token`; no further credential retry was made.
- PHASE_03 TASK-03.8 is complete: the Android-free service boundary now exposes
  `RewardedOpportunityService`, `PurchaseCatalogService`, and `ArenaService` through deterministic
  local adapters. Accepted service-shaped requests preserve affordance/result shape while reward
  claim/doubling, purchase transactions, real-ad completion, account behavior, and Arena network
  matches remain deferred or blocked.
- Delivery commits: `57f3f25`, `150d6ed`, `2348bdc`, `caaaa93`, `b3ed42f`, and `f603ed0`.
  The later fixes harden runtime-unmodifiable configuration/snapshot collections and remove the
  public `copy` mutation bypass from the purchase catalog snapshot.
- Verification: MP runner `109 passed / 0 failed / 0 skipped`, lint ok; `scripts/public-safety.ps1`
  pass; `test :app:assembleDebug` successful with the Android Studio JBR and Android SDK; full
  verifier, semantic review, independent critic, and deterministic reviewer passed. No app wiring
  or screenshot record was required for this domain/data task.
- Next accepted task is PHASE_03 TASK-03.9 Reward adapter; push for TASK-03.8 was not attempted.
- Push for TASK-03.8 was attempted after explicit approval but failed with GitHub `Invalid username or token`;
  no further credential retry was made and all delivery commits remain local.

## IMPLEMENTATION UPDATE — 2026-08-28

- PHASE_03 TASK-03.9 is complete: the Android-free reward boundary now exposes deterministic
  `NORMAL_REWARD` and `MULTIPLIER_SHAPED` outcome shapes for accepted and blocked requests without
  applying claim, ad-completion, multiplier, transaction, economy, SDK, account, payment, or
  network semantics.
- Delivery commits: `de2a611` and `07bcde6`.
- Verification: scoped runner `112 passed / 0 failed / 0 skipped`; full runner `112 passed / 0 failed / 0 skipped`,
  lint ok, `:app:assembleDebug` successful; public-safety pass (113 tracked files, 850 history paths,
  zero creative assets); deterministic reviewer, semantic review, independent critic, and full verifier passed.
- Next accepted task is PHASE_03 TASK-03.10 Arena adapter. Push was not possible because `GITHUB_TOKEN`
  is not set; no credential retry was made.

## TESTING UPDATE — 2026-08-29

- Added Fakes-only Arena coverage for the accepted `CampaignSession` route, unfinished-run guard,
  deterministic repeated snapshots, and propagation of a blocked service-shaped snapshot.
- Extended service-boundary coverage for unknown Arena requests, immutable local/blocked snapshots,
  and the absence of network, account, match, production-integration, and authoritative-state
  behavior. Existing campaign, integration, and service tests were reviewed and their assertions
  were retained.
- Verification executed after the repair: scoped runner `118 passed / 0 failed / 0 skipped`; full
  runner `118 passed / 0 failed / 0 skipped`, lint `ok`; deterministic reviewer, semantic review,
  independent critic, and full verifier passed. Compose/navigation coverage remains an explicit
  dependency exception because `:app` has no Android test dependencies; the public
  `CampaignScreenContent` seam was confirmed.

## IMPLEMENTATION UPDATE — 2026-08-29

- PHASE_03 TASK-03.10 is complete: the offline service-shaped Arena state is wired from the
  campaign surface through `CampaignSession` and the Android UI, while local-service-shaped and
  network-match-blocked states remain explicit, immutable, deterministic, and offline-only.
- Delivery commits: `24d553d` (`feat: add offline Arena route`) and `e793697`
  (`fix: repair Arena integration test nullability`). Both were pushed successfully to
  `origin/main`.
- TASK-03.10 is archived in `.claude/specs/done`; no successor task or chain was started.

## VISUAL QA UPDATE — 2026-08-29

- PHASE_03 TASK-03.13 is complete: device-backed structural evidence and fit records were added
  for campaign, roster, settings, local Arena, and resume surfaces; no product-surface rewrite was
  introduced because the routes already existed.
- Delivery commits: `f2658e4`, `655cf66`, `c05d9c0`, `7e84371`, and board close-out `d7924b4`.
- Verification: final connected `ResumeContentUiTest` `1 passed / 0 failed / 0 skipped`; full MP
  runner `118 passed / 0 failed / 0 skipped`, lint `ok`; public-safety pass. No aggregate fit score
  was produced or claimed: five visual cells remain explicitly deferred/uncheckable because the
  Pixel 9/reference and emulator profiles mismatch, ImageMagick pixel comparison is unavailable,
  and relevant reference captures are preserved-unusable/unreadable; structural evidence remains
  recorded.
- TASK-03.13 is archived in `.claude/specs/done`. The final tester assertion remains an intentional
  uncommitted worktree change and was included in the final connected run.
- Next backlog task: PHASE_03 TASK-03.14 Lifecycle.

## LIFECYCLE RESTORATION UPDATE — 2026-08-29

- PHASE_03 TASK-03.14 is complete: campaign run-save persistence now covers background,
  Activity recreation, and process-death restoration for active and victory contours, with
  Android-free authority retained in `CampaignSession` and encoded storage isolated in the app.
- Delivery commits: `54d5d45`, `e6deac9`, and board close-out `505d347`; all pushed to `origin/main`.
- Verification: JVM `125 passed / 0 failed / 0 skipped`; connected `11 passed / 0 failed / 0 skipped`;
  public-safety pass; `test :app:assembleDebug` successful. Semantic reviewers stalled twice,
  so deterministic review and the full verifier evidence were used as the recorded fallback.
- TASK-03.14 is archived in `.claude/specs/done`. The next runnable backlog remains TASK-03.15
  Acceptance, followed by TASK-03.16 Progress.

## PHASE_03 CLOSE-OUT — 2026-08-29

- TASK-03.9 Reward adapter — commits `de2a611`, `07bcde6`; scoped and full runners each reported `112 passed / 0 failed / 0 skipped`, lint ok, `:app:assembleDebug` and public-safety passed, and deterministic reviewer, semantic review, independent critic, and full verifier passed. Push was unavailable because `GITHUB_TOKEN` is not set.
- TASK-03.10 Arena adapter — commits `24d553d`, `e793697`; the offline service-shaped Arena route is wired through campaign/session/UI with immutable local and network-match-blocked states. Existing evidence records 118 tests passed, lint ok, verifier pass, and successful push to `origin/main`.
- TASK-03.11 core visual QA — commit `a07c6d5`; connected `emulator-5554` evidence for ST-0001/ST-0002/ST-0003 records structural coverage and FIT-03.11-001/002/003. No pixel score was claimed: the Pixel 9 reference profile differs from the emulator profile and ImageMagick was unavailable; ST-0002 remains blocked by preserved-unusable reference PNGs.
- TASK-03.12 enhancement/victory visual QA — commits `8054578`, `495833a`; instrumented Compose checks passed 2/2, deterministic/semantic/critic/verifier gates passed, and visual fit was skipped because ST-0004/ST-0005 references are preserved-unusable. FIT-03.12-001/002 remain explicit; deferred reward claim, doubling, transaction, and economy semantics were unchanged.
- TASK-03.13 meta/service/resume visual QA — commits `f2658e4`, `655cf66`, `c05d9c0`, `7e84371`; final connected `ResumeContentUiTest` passed 1/1, full MP runner reported `118 passed / 0 failed / 0 skipped`, lint ok, and public-safety passed. No aggregate fit score was produced or claimed; five visual cells remain deferred/uncheckable because relevant references include preserved-unusable captures, the Pixel 9/reference and emulator profiles mismatch, and ImageMagick pixel comparison is unavailable. FIT-03.13-001 through FIT-03.13-005 remain explicit and no pixel score was claimed.
- TASK-03.14 lifecycle restoration — commits `54d5d45`, `e6deac9`, with board close-out `505d347`; JVM `125 passed / 0 failed / 0 skipped`, connected `11 passed / 0 failed / 0 skipped`, public-safety pass, and `:app:assembleDebug` successful. Deterministic review and full verifier were the recorded fallback after two semantic-review stalls.
- TASK-03.15 phase acceptance — existing contour delivery was rechecked without new production work: MP runner `125 passed / 0 failed / 0 skipped`, lint ok; connected `11 passed / 0 failed / 0 skipped` on `Pixel_5(AVD) - 14`; public-safety `pass` (`124` tracked files, `1156` history paths); spec validator `pass` (`16` requirements, `10` stories, `10` acceptance, `16` trace rows); `git diff --check` passed.
- PHASE_03 exit verification — TASK-03.9 through TASK-03.15 are complete, all accepted semantic routes and service-shaped boundaries are verified offline, lifecycle restoration preserves the Android-free deterministic authority, and all requested per-screen structural QA records exist. Remaining visual parity blockers are the FIT-03.11-001/002/003, FIT-03.12-001/002, and FIT-03.13-001..005 records, including preserved-unusable references, profile mismatch, unavailable ImageMagick pixel comparison, and explicitly uncheckable visual cells. Deferred/excluded Shop, Tech, defeat, reward transaction, real-ad, IAP, account, network Arena, and external-exit behavior remains frozen.
- Transition verified — PHASE_03 is `done` and PHASE_04 is the sole `active` phase in `docs/implementation_plan/PROGRESS.md`; the next work is the PHASE_04 fit gate and deferred-scope closure.
- PHASE_04 TASK-04.1 is complete: the 13-row fit registry, locked DEV-001 through DEV-009 deviations, and fit thresholds were re-read and confirmed; spec validation passed (`16` requirements, `10` stories, `10` acceptance, `16` trace rows), with no production changes.

## PHASE_04 FIT UPDATE — 2026-08-29

- TASK-04.2 completed the fit pass for all 13 registry rows; the durable record is
  `spec/fit/task-04.2-fit.md`.
- The fit gate is blocked/partial, with no aggregate score claimed: the connected device
  is `emulator-5554` / `Pixel_5(AVD)-14` while references use Pixel 9 1080x2424@420,
  ImageMagick is unavailable, and preserved-unusable references affect ST-0002, ST-0004,
  ST-0005, ST-0011, and ST-0013.
- Six evidence-backed divergences remain: FIT-03.11-001, FIT-03.11-003, FIT-03.13-001,
  FIT-03.13-002, FIT-03.13-003, and FIT-03.13-005. Six candidate fix SPECs were proposed,
  but none was written to `.claude/specs/backlog/` pending the human fit write-gate.
- No production behavior, deferred scope, reference asset, or reference UI copy was added.
- Verification evidence: connected `11 passed / 0 failed / 0 skipped`; full runner
  `125 passed / 0 failed / 0 skipped`; APK SHA-256
  `D8B45BF433AB7665436088545143BFCA325508F96526AE88E1B0376935459526`.

## PHASE_04 FIT WRITE-GATE UPDATE — 2026-08-29

- The explicit fit write-gate was accepted and six presentation-only divergence SPECs were created.
- FIT-04.01 Launch composition is done: commit `b0683c5`; connected `12/0/0`; FIT-03.11-001 resolved by an original full-screen launch composition; numeric pixel score remains unscored.
- FIT-04.02 Active battle composition is done: commits `b999f34`, `05e49d5`, `44a7d5f`, and `a113960c`; project runner `125/0/0`, connected `13/0/0`, public-safety pass, exact locked-engine build pass, full verifier pass; FIT-03.11-003 resolved by an original Canvas battlefield with HUD, base/enemy region, and edge controls.
- Current capture: `build/fit/built/TASK-04.16-FIT-04.02-ST-0003.png`; multimodal fit PASS with structural/bounds PASS. Pixel score is `null` because ImageMagick is unavailable and reference/device profiles differ; screenshot runner is an accepted infrastructure exception.
- Semantic review initially found an out-of-scope version bump; repair restored `versionCode=16` and `versionName=0.1.15`. A follow-up accessibility assertion covers the battlefield Canvas contentDescription.
- No MyEngine source change or separate engine feature task was needed: `../MyEngine-mysd` is clean at the exact `gradle/myengine.lock` SHA `30f4eb17aff0ea2fe6cf80aef970a1e7746dbcbb`.
- Remaining unexplained divergence is FIT-03.13-005. FIT-04.03 campaign, FIT-04.04 roster, and FIT-04.05 settings compositions are done; FIT-04.06 resume composition is active.

## PHASE_04 FIT PROGRESS UPDATE — 2026-08-30

- FIT-04.03 Campaign composition is done: commits `b524340`, `cb831321`, `c28a788`, `2195206`, and `fe17f55`; connected `16/0/0` after fixing a real campaign/lifecycle viewport regression; FIT-03.13-001 resolved.
- FIT-04.04 Roster composition is done: commits `7625ac7`, `32622f5`, and `9379cc`; connected `16/0/0`; FIT-03.13-002 resolved. The final capture is `build/fit/built/TASK-04.16-FIT-04.04-ST-0007.png`; it shows all five route items in a responsive 3+2 layout.
- Both screens use original presentation-only shells, retain existing snapshot/intent boundaries, and add no game, economy, service, reference asset, or copied UI scope.
- FIT-04.05 Settings composition is done: commits `054687a`, `afaee47`, and `0ba7a9c`; connected `16/0/0`; ST-0008/FIT-03.13-003 resolved. The final capture is `build/fit/built/TASK-04.16-FIT-04.05-ST-0008.png`; it received a strong qualitative multimodal fit pass, while numeric pixel scoring remains unscored because ImageMagick is unavailable and reference/device profiles differ.
- FIT-04.06 Resume composition is done: commits `4571054`, `7ab5455`, and `fcc4921`; connected `16/0/0`; ST-0012/FIT-03.13-005 resolved. The final capture is `build/fit/built/TASK-04.16-FIT-04.06-ST-0012.png`; it received a strong qualitative multimodal fit pass, while EV-0189 is invalid/unsupported locally and numeric pixel scoring remains unscored.
- PHASE_04 exit verification passed: runner `125/0/0`, exact lock build and public-safety passed, connected suite `16/0/0`; MyEngine checkout remains clean at lock SHA `30f4eb17aff0ea2fe6cf80aef970a1e7746dbcbb`.

## LOCALIZATION UPDATE — 2026-08-30

- The Android build now uses Russian as the project default via `.claude/mp/config.json` (`uiLang: ru`).
- All 88 user-visible Android resources in `app/src/main/res/values/strings.xml`, including accessibility descriptions, route labels, battle states, deferred-service messages, and content names, were translated to Russian; resource IDs and machine-readable content IDs were preserved.
- Verification: XML/config parse passed; English UI-copy scan found no remaining English values apart from the MySD brand; `bash .codex/scripts/mp-runner-android.sh false` passed with `125 passed / 0 failed / 0 skipped`, `:app:assembleDebug`, and `:app:lintDebug`.
- The generated debug APK is at `app/build/outputs/apk/debug/app-debug.apk`.

## FIRST PLAYABLE LEVEL SPEC UPDATE — 2026-09-03

- Feature-mode mp-spec elicitation is complete for `first-playable-level`; the user confirmed the
  existing campaign route, portrait top-to-bottom composition, three fixed build tiles, one tower,
  one enemy family, passive global resource, two upgrades, tower destruction, base defeat, and a
  finite 8–10 enemy wave.
- Gate 1 decomposition is accepted and recorded in
  `C:/Users/Admin/AppSpecs/mysd-first-playable-level/pipeline/decomposition.json`; the project
  board contains the overview plus eight ordered backlog SPECs
  `first-playable-level-01-content` through `first-playable-level-08-acceptance-balance`.
- Grounding facts G1–G11 and the decision ledger are recorded in the feature pipeline; all emitted
  `CHANGED_HINT` entries cite a grounding fact or are explicitly marked as assumptions.
- Light evaluation passed: decomposition JSON parsed, all dependencies resolve in order, all nine
  board files exist, Gherkin scenarios have one `When`, domain-math sections include formulas,
  symbols, precision, edge cases, worked examples, and determinism, and `git diff --check` passed.
- No production code, build output, reference asset, or public UI copy was changed by this spec
  run. The next handoff command is `/mp --feature --next`.

## FIRST PLAYABLE LEVEL CONTENT CONTRACT — 2026-09-03

- `first-playable-level-01-content` shipped with commits `71b89df` and `e7fb9be`; the SPEC is
  closed in `.claude/specs/done/` and the code commits are pushed to `origin/main`.
- The Android-free content boundary now provides a versioned typed catalog for one stage, one
  separate base, three fixed build slots, one tower, one enemy family, and a finite 9-enemy wave.
  Codec validation rejects duplicate/invalid IDs, negative numeric values, wrong slot counts,
  invalid wave sizes, missing/unknown fields, and future versions with field paths.
- Verification: scoped `:game` runner `133 passed / 0 failed / 0 skipped`; full runner `133 passed /
  0 failed / 0 skipped`, lint ok, assembleDebug completed, public-safety pass, deterministic
  reviewer pass, and full verifier pass. No runtime economy, placement, combat, UI, or persistence
  integration was added.
- The next runnable backlog item is `first-playable-level-02-runtime-economy`; the successor
  `--feature --next --chain` task was created in the current main checkout.

## FIRST PLAYABLE LEVEL COMBAT OUTCOMES — 2026-09-03

- `first-playable-level-05-enemy-combat-outcomes` is complete and closed in `.claude/specs/done/`.
  The Android-free runtime now owns deterministic finite-wave spawning, top-to-bottom movement,
  nearest-target tower auto-fire with stable ID tie-breaks, tower contact destruction, base leak
  damage, victory/defeat terminals, same-tick defeat precedence, and terminal-state freezing.
- Existing resource, fixed-slot build, tower-upgrade, pause/resume, and replay boundaries remain
  intact; no kill rewards, production SDKs, network behavior, or copied reference content were added.
- Delivery commits: `c388660ee9c2c42bac3a75aae9c404eb9001a936` (implementation), `1d8af03`
  (tester coverage), and `f5ff040` (SPEC close-out). The initial developer worker stalled, but a
  recovery pass completed the implementation and returned a valid payload; a reduced semantic
  review then passed with 12/12 frozen matrix cells and no findings.
- Verification: scoped `:game:test --rerun-tasks` passed; full runner with exact lock checkout
  `../MyEngine-mysd @ 30f4eb17aff0ea2fe6cf80aef970a1e7746dbcbb` reported `183 passed / 0 failed /
  0 skipped`, lint ok, assembleDebug ok; public-safety and `git diff --check` passed; full verifier
  passed. The default `../MyEngine` checkout is preserved untouched because it belongs to another
  dirty workstream.
- The epic remains open with `first-playable-level-06-save-replay-lifecycle`,
  `first-playable-level-07-android-battle-ui`, and `first-playable-level-08-acceptance-balance` in
  backlog; the next chain task should consume the lowest runnable SPEC on `main`.

## FIRST PLAYABLE LEVEL RESTORE UPDATE — 2026-09-04

- `first-playable-level-07-playable-battle-restore` is complete and closed in `.claude/specs/done/`.
  The Android-free `PlayableBattleSession` now restores from the existing full `RunSave` payload,
  preserves logical tick and pending command scheduling, returns an explicit unsupported outcome
  for contour-only saves, and keeps restored defeat terminal-frozen.
- Delivery commits are `a99a19c` (restore seam) and `8f917de` (restore/replay coverage), with
  board close-out commits `ac009d1` and `d7b7882`; all were pushed to `origin/main`.
- Verification: scoped `:game` runner `199 passed / 0 failed / 0 skipped`; full runner `199 passed /
  0 failed / 0 skipped`, lint ok, assembleDebug successful, deterministic reviewer pass, and full
  verifier pass. Semantic primary pass covered 12/12 cells but its liveness retry was recorded
  partial/stalled; the independent critic passed with three non-blocking warnings about latent RNG
  cursor continuity, pending-only command-log scope, and active paused-resume coverage.
- This slice intentionally does not wire CampaignSession, Activity, Compose, or Android storage;
  `first-playable-level-08-campaign-lifecycle-restore` remains the next runnable SPEC.

## FIRST PLAYABLE LEVEL CAMPAIGN LIFECYCLE RESTORE — 2026-09-04

- `first-playable-level-08-campaign-lifecycle-restore` is complete and closed in
  `.claude/specs/done/`. Campaign lifecycle persistence now restores full playable active and
  defeat state through Android storage while preserving the legacy victory contour.
- Delivery commits: `3a2c8b5`, `c342259`, `dcb587c`, and `dff144e`; board close-out commit
  `70cf3c3` was pushed to `origin/main`.
- Verification: final runner `213 passed / 0 failed / 0 skipped`; lint, assembleDebug,
  androidTest compile/package, public-safety, deterministic review, semantic review, and full
  verifier passed. Connected runtime was blocked by Windows Permission denied on pre-existing
  locked UTP logcat result files; OS-level process death remains a documented limitation because
  instrumentation uses ActivityScenario close/relaunch in one process.
- The epic remains open; the next runnable backlog contains
  `first-playable-level-09-android-battle-ui` and
  `first-playable-level-10-acceptance-balance`.

## FRESH REFERENCE COMPATIBILITY UPDATE — 2026-09-16

- The newly supplied live package is `com.yuegame.defender` 2.7 (`versionCode 18`), distinct from
  the accepted legacy `com.gdzsq.crazy_td` 1.0 corpus. The official Play artifact was installed on
  the Pixel 8 API 35 compatibility AVD; its raw split APK set and SHA-256 inventory remain local at
  `.reference-local/packages/com.yuegame.defender/vc-18/apk/`.
- Four isolated raw runs are complete. Pixel 9 API 37 is blocked before content because the native
  library's 8192-byte program alignment is incompatible with the 16384-byte system page size.
  Pixel 8 API 35 reaches the opening Stage 1 tutorial but cannot confirm its first transition:
  the override-resolution run produced an ANR and the native-resolution control remained visually
  unchanged after one input. Native 1080x2400 falsified the display-transform hypothesis.
- A read-only Pixel 5 API 34 control cannot launch the locally installed official split set because
  the system requires Google Play enablement. Its system dialog did accept a low-level emulator
  mouse action, so that control confirms the emulator hardware-input path itself is functional.
  A read-only Android 14 boot against the Pixel 8 Android 15 userdata remained ADB-offline and was
  stopped without saving; it is excluded from app evidence.
- Static APK-content inspection was stopped as soon as the runbook prohibition on extracting or
  decompiling assets was re-read. No extracted contents were persisted, transient observations are
  explicitly non-evidence, and the retained APKs are raw local artifacts only.
- The package-level blocker matrix is recorded in
  `.reference-local/packages/com.yuegame.defender/vc-18/compatibility-summary.md`. All run indices,
  JSON/JSONL ledgers, and indexed SHA-256 values were revalidated. No credentials, purchase, ad
  completion, data clear, or Arena network action occurred.
- Fresh-package Gate 1 remains ineligible: only package identity, launch constraints, the privacy
  decline path, an offline loading dependency, and one opening tutorial anchor are reproducible.
  No live-package observation was promoted into public requirements or production code. A physical
  4 KB Android device, or an already-authorized API 34 Google Play AVD, is the next valid runtime
  observation environment.
- Production code and the locked MyEngine checkout were not changed in this evidence pass. FPL-09
  remains technically verified in seven local commits ahead of `origin/main`; its explicit push
  confirmation is still unresolved, so FPL-10 was not started.

## FPL-09 TERMINAL PERSISTENCE AUDIT — 2026-09-16

- A read-only source audit confirmed a prerequisite lifecycle regression after the FPL-09
  verification pass. A canonical battle that transitions live from active to `VICTORY` leaves
  `victorySession` unset, is rejected by the terminal guard in `activeBattleSnapshot()`, and makes
  `CampaignSession.runSave()` return `null`; Android then removes the durable save on lifecycle
  stop. Full terminal-victory codec/restore support already exists, but current tests only seed a
  ready-made victory payload or exercise the legacy manual victory contour.
- The active SPEC now records `PERSISTENCE-001` and the required focused repair: pin the live
  transition with a regression test, emit the existing canonical terminal payload for both victory
  and defeat, preserve legacy contour-only compatibility, then repeat the scoped and full FPL-09
  gates before asking for push again. No production or test file was changed in this audit.
- A separate balance audit confirms default defeat is unreachable without changing the shipped
  tuning: base health is `120`, the finite wave contains `9` enemies, and each leak deals `12`, so
  the no-command path wins with `12` HP. The terminal reducer itself supports defeat under custom
  fixtures. Rebalancing and mandatory playable defeat remain FPL-10 scope and were not started.
- The visual/device pre-flight is currently unavailable because all emulators were intentionally
  stopped after the fresh-reference compatibility work. Resume the repair only with the required
  booted development AVD; do not substitute JVM-only evidence for the connected FPL-09 gate.
