# Full-product final verification results

Status: all device/host gates that this host can execute are complete on final **build 13**
(2026-09-23/24). Release frame-performance acceptance remains open: it needs a physical device
(the SwiftShader AVD fails the frame budget even for system Settings). No reference-parity claim.

## Current checkpoint — build 13 final verification

Build-11 full-run interruption was diagnosed and recovered; two demonstrated defects were fixed
(builds 12 and 13) and every gate was re-run on the final APKs. Evidence directories are under
`build/reports/`; each has a README with exact hashes and limits.

- **Build-11 stop cause established** (`product-full11-20260923/device-results-recovered-after-host-stop/`):
  the Codex root/worker turns were interrupted at 19:39:18Z/19:39:22Z; the emulator launcher was a
  child of the worker shell and ran its termination handler (graceful QEMU exit). Not a guest,
  renderer or MySD failure. Recovered device log: **81 PASS / 0 FAIL**, test 82 without verdict,
  12 not started; real stores byte-identical to the pre-run baseline. The AVD is now created via
  `Win32_Process.Create` (`avd-swiftshader-resume-20260923-02/`); four pre-MySD boot ANRs were
  cleared by recorded Wait/Close actions.
- **Build-11 full rerun: 94/94 PASS** (`product-full11-rerun-20260923/`), 237 fresh tour PNGs whose
  app content is pixel-identical to the reviewed affected11 tour (status bar only differs).
- **Defect 1 — Dialog whole words at real system font 2** (`product-dialog-system-font2-build11-20260923-01/`):
  the Resume Dialog split "Незавершённый" at font 2 / 320x480 dp; Compose Dialog windows ignore the
  Activity-local font override used by the tours. Build 12 adds `WholeWordText`
  (`ProductDialogTitle`/`ProductDialogBody`), extracts all Dialog contents into testable overlays and
  adds `ProductDialogLayoutTest` (4 cases). Build-12 host gate 294 JVM / lint 0 errors / 3 APKs;
  affected12 55/55, full12 98/98; real font-2 Resume, service, Exit and build Dialogs keep whole
  words. An independent review found no blockers.
- **Defect 2 — stale build Dialog** (`mysd-real-play-build12-20260923-02/`): ordinary build-12 play
  re-opened the tower build Dialog without a tap after a wave-end choice and in a retried run
  (unkeyed `rememberSaveable` slot selection). Build 13 keys it to run and input-enabled phase and
  adds `ProductBattleUiTest.buildDialogSelectionDoesNotSurviveModalPhasesOrANewRun`.
- **Build 13** (`apks-build13/`, freeze `source-freeze-build13-20260923.json`, 143 files): 294 JVM
  tests / 33 suites with zero failures/errors/skips, lint 0 errors (39/38 warnings), debug
  `6a5002af…2969`, test `60a55862…0ff7`, benchmark `59c4b119…d801`, same local certificate, no
  INTERNET. Host static gates **15/15 PASS** (`product-static-gates-build13-20260923-01/`).
- **Full13: 99/99 PASS**, `OK (99 tests)` (`product-full13-20260923/`); no ANR/crash after boot;
  real stores byte-identical before/after; 237 fresh tour PNGs, pixel-identical to build 12 below
  the status bar.
- **Actual system-font-2 Dialogs on build 13: PASS** (`product-dialog-system-font2-build13-20260923-01/`).
- **Real OS process death on build 13: 3/3 automatic PASS** (`process-death-build13-20260923.md`):
  terminal-unclaimed, terminal-claimed (no re-grant; independent repeated-claim audit +9 credits,
  one BATTLE_REWARD row) and paused-built (level-1 tower, tick 82). Run documents exact; only
  full-energy clocks moved.
- **Non-debuggable frame windows on build 13** (`mysd-benchmark13-frames-20260923/`): speed-compiled
  benchmark APK, natural first-stage runs, valid active after-evidence. 1x p95 240.42 ms (jank
  98.15%), 2x p95 117.45 ms (jank 85.58%) — **FAIL** vs 16.7 ms / <5%. A control window on system
  Settings in the same AVD gives p95 118.20 ms (jank 65.98%), so the result is environment-bound
  and not a release-device verdict. Debug APK restored; save continuity verified.
- Residual warnings (no demonstrated failure): legacy migration-only Resume dialog body/buttons are
  not covered at font 2; at 320x480 dp + font 2 the battlefield is ~104 dp tall and slot targets
  overlap; the build Dialog does not pause the real-time battle (design, not changed).
- Not performed: the optional `com.yuegame.defender` compatibility observation (third-party APK on
  the shared AVD; not needed for MySD acceptance). Exact parity remains unverified.

## Historical — session pause before build-13 (superseded)

Session pause: the user requested a new goal-mode session. At that point the already-running
build-11 full device gate showed 80/94 passed, zero failures/skips, current case 81; this is a
checkpoint only. Read `build/reports/product-full11-20260923/` for any later final verdict.
At 19:44:21 UTC private ADB no longer saw `emulator-5680`; its emulator processes were absent.
At 19:45:27 UTC no emulator/QEMU process remained; stdout last wrote at 19:39:58.143, stderr was
empty, and the cause was unknown. The worker returned ADB ownership without rebooting. The run is
incomplete with 14 verdicts unknown; see `build/reports/product-full11-20260923/README.md`,
`host-stop-handoff.json`, and the 65-file host hash manifest. Diagnose the emulator exit on resume.
No additional device or performance gate was launched as part of this handoff. Remaining acceptance
is listed at the top of `.ai/handoff.md`.

All engine, domain, Android, and test sources were frozen before the first execution. Subsequent
changes belong to final stabilization and must be identified with their failing check or a
source-grounded contract-audit finding. Later APKs do not inherit a device PASS from earlier APKs.

## Previous build-11 checkpoint — affected gate passes; full/device acceptance pending then

The collected UI stabilization and its regression sources were completed and independently
reviewed before execution. `build/reports/source-freeze-build11-20260923.json` records 141 files
at **2026-09-23T18:42:22.7525153Z**, with **294 JVM / 94 Android cases expected**. The build began
at 18:43 UTC, including a forced fresh `:game:test`. That JVM task now passes **294 tests / 33
suites / zero failures, errors or skips**, with suite timestamps 18:43:31.372 through 18:43:40.109
UTC (`build/reports/build11-jvm-checkpoint-20260923.json`). The overall build subsequently completed
**BUILD SUCCESSFUL in 11m37s**, with 148 tasks (47 executed, 101 up-to-date). Debug lint has 0 errors /
39 warnings; benchmark lint 0 errors / 38 warnings; debug, instrumentation and benchmark APKs
assemble. Evidence is `build/reports/full-product-final-20260923-11.log`. The earlier checkpoint's
RUNNING field remains an immutable historical observation, not the current completed outcome.

Independent host archive/signature/packaged-manifest audit passes in
`build/reports/apks-build11/README.md` and `provenance.json`. All 141 source hashes/lengths match the
freeze, with zero missing/extra scoped files. All three archived APKs exactly match their outputs
and share the verified local v2 signing certificate. Minimum/target SDKs are 26/36; INTERNET is
absent. The benchmark is non-debuggable, shell-profileable and release-derived without R8 code
shrinking, not production-signed or performance-accepted. Existing fresh JVM XML, lint XML, merged
and decoded manifests, tool output, source freeze and completed build log are retained alongside:

| Build-11 artifact | Bytes | SHA-256 |
| --- | ---: | --- |
| Debug | 13080478 | `df841b00e78c05b7d5bf8ea966c7dea2cefc73f244955c4ed63920763b36c63d` |
| Instrumentation | 1366252 | `aadde0bec4ed63bfb4e107f7be1d14f8b0680cfb15a266a6150f1b85a99cff95` |
| Benchmark | 9136535 | `0d8eb7022108d4a39f1121152d160b33168cff5c7f9f8e1dd73a28f62537d671` |

This audit ran no build, test or device command and does not itself establish installed identity.
Subsequent device-owned affected execution independently verified the installed target/test hashes
above and passed **33/33**, zero failures/skips, with complete `OK (33 tests)`. It ran from
19:15:04 through 19:20:45 UTC, 334.618s JUnit / 341s helper time. Evidence is
`build/reports/product-affected11-20260923/README.md` and `device-results/`. No timeout, source,
install or data-clear change occurred during that run. The **94-case full Android run has started
but has no final verdict**; an affected subset is not a full-suite pass.

Host static/tool attempt02 passes 12/12 unchanged checks, with exact outputs in
`build/reports/product-static-gates-build11-20260923-02/`. Attempt01 remains 5 PASS / 7 environment
FAIL because Windows PowerShell 5.1 inherited incompatible PowerShell 7 module paths and could not
resolve Get-FileHash. Only the corrected runner's child-process module path changed; no machine-wide
setting or product/test source was changed. Its four historical save comparisons exercise schema 3,
not new build-11/schema-4 OS-death proof. Earlier APKs do not verify build-11.

The bounded source changes address measured full-width Setup choice names, adaptive campaign
title/status/metadata, concise visible 1x/2x labels with complete accessibility descriptions,
enlarged-type HUD spacing, Activity overlay and non-navigation screen insets, and full-width
Exit/Resume actions. Ordinary Setup -> Battle -> Pause had exposed a real exit-dispatch defect:
the saveable state holder changed while a stable callback retained the old holder. Dispatch is
now keyed by the current holder. Covered battle HUD input is also disabled, hidden from the
merged semantics tree and blocked at the background pointer layer during modal phases. This
batch changes no engine, domain or save schema.

Four new `ProductAdaptiveLayoutTest` cases cover 320/411dp at font scales 1/2, complete text at
actual node width, unchanged 48dp targets and a 96dp compact field floor, all four catalog slot
positions, plus real window insets with both compact and full-height overlay variants. Two new
`ProductBattleUiTest` cases cover repeated real-ViewModel Setup-to-Battle transitions with the same
callback, and covered-HUD input/semantics while foreground actions remain available. These tests
compile in the completed build and now pass in the affected device run. The per-test timeout remains
120000ms; these results do not establish every untested gesture or natural progression path.

The Android worker owns private ADB 5039. A controlled data-preserving cold boot uses explicit
SwiftShader on the same API35 AVD, four CPUs/2560MB/WHPX, physical 1080x2424/density420/font1. This
diagnostic environment change is not an established cure for the full-10 graphics stall and cannot
establish physical-device performance. The build-11 app/test update installs succeeded with both
stores byte-identical immediately afterward, but the sole ordinary launch returned `Status: timeout`
after 14538ms. A later supplement found the exact paused schema-4 run at COMBAT tick18 with no
claim/currency/ledger/revision change and only legitimate full-energy clocks +2995s; a SystemUI ANR
covered the game. One documented Wait action at 19:12:24 UTC later recovered the unobstructed paused
UI without a new process. The affected run is explicitly **post-Wait**. Neither its pass nor the
manual supplement erases the launch timeout/ANR or proves OS-process death. See
`build/reports/product-upgrade-build11-20260923-01/README.md`.

The affected capture TAR is 26,280,448 bytes, SHA-256
`b4e4b4e1fdadc5787d84c93a94124375152d1d321750cf4583ee7068099404dc`; exact device/host hash/length
and safe paths were verified before extraction. Each profile has 20 fixtures, 79 fresh manifest
rows/PNGs, no missing/unlisted files: **237 fresh images**, not 237 visually accepted images.
The independent bounded reports `visual-review-compact.md`, `visual-review-compact-font-2.md` and
`visual-review-native.md` in that run directory identify **46 / 56 / 40 selected images** respectively
(142 total). They find no new demonstrated production-layout blocker in their selected views.
Full-width Setup names, stacked campaign metadata, whole short HUD captions and scrolled vertical
actions are readable. Automated AdaptiveLayout geometry is separate evidence for actual insets,
four catalog slots and minimum targets; screenshots do not inherit numerical geometry assertions.

Two font-2 claimed-terminal root PNGs show only background and are unusable state evidence; their
full-display counterparts render correctly. The corresponding compact/native roots render correctly.
The font-2 hero-action focus does not reveal its named control, and two enhancement-focus images do
not show their own complete Apply control; alternate images/geometry checks remain separate evidence,
not repairs to those captures. Capture timing is only a hypothesis for blank roots. Separate Resume
and service Dialog windows are physically native/font1 even in compact/font-2 directories, and Exit
confirmation is not a fixture in these tours. Final full-suite, actual system-font Dialog,
current-build real OS-death and non-debuggable frame gates remain open; there is no final visual,
reference-parity or release acceptance.

## Previous build-10 checkpoint — affected gate passes; full Android run incomplete

Frozen build-10 completed successfully in 10 minutes 15 seconds: `test :app:lintDebug
:app:lintBenchmark :app:assembleDebug :app:assembleDebugAndroidTest :app:assembleBenchmark`.
Evidence is `build/reports/full-product-final-20260923-10.log`. Its retained XML reports contain
**294 JVM tests / 33 suites / zero failures, errors or skips**. Debug lint has 0 errors / 39
warnings; benchmark lint has 0 errors / 38 warnings. Debug, instrumentation and non-debuggable
benchmark APKs are retained under `build/reports/apks-build10/`. Assembly alone does not establish
installed identity; the later affected-run report independently matches the installed target to
`48ae0d01fd9dac0c12e26629e1b6bd7e5f5229ff283812a1dc6b573050c6501a` and instrumentation to
`f7b185c49d3ce952d6d9669418f8379ed1f5e802d1c24f4833efdf2f0fa76194`.

Build-10 affected Android execution passed **27/27**, zero failures/skips, in 249.671 seconds
JUnit time (253 seconds helper time), ending 17:41:33 UTC. Evidence is
`build/reports/product-affected10-20260923/`, including raw device-owned results and installed-file
identity checks. It includes the hero-selection, autosave, lifecycle, persistence-failure,
Battle/Meta, strict layout and visual-tour cases. The verified capture TAR is 27236864 bytes,
SHA-256 `3c1c610c02112b84ff041c6b5bdca787d1c0e613158b92e562b3981e5445e632`; host/device hashes and
lengths match. Each of three profiles has 79 fresh manifest-listed PNGs and 20 fixtures, with no
missing/unlisted files: **237 fresh images**, not 237 visually accepted images.

The unchanged build-10 **full 88-case run is INCOMPLETE**, not passing or still running. It started
17:43:37 UTC and recorded **72 PASS**, then started case 73,
`ProductMetaLayoutTest.nativeWidthLargeFontMetaCardsKeepCopyAndActionsReadable`, without a verdict;
the remaining **15 cases did not start**. The raw log ends with two reports of
`INSTRUMENTATION_ABORTED: System has crashed.` No completed assertion failure was reported before
the abort, but this does not make the run 88/88 or certify case 73. The completion file contains
only `end_utc=2026-09-23T18:01:39Z`, without elapsed time or exit status. Evidence is
`build/reports/product-full10-20260923/device-results/`; a checkpoint/completion flag is not a
final JUnit verdict. Subsequent retained system/DropBox logs establish Watchdog terminating
system_server PID 610 after 74-second-overdue WindowManager/display/animation checks, followed by
replacement PID 6914 without a kernel reboot. The animation thread held WindowManagerGlobalLock
while waiting in synchronous SurfaceComposer layer capture for an OS task snapshot. The underlying
graphics-service/guest/host cause remains unestablished; no persistence, memory or ADB root cause
is proved. See that directory's `README.md` and `ANR_ANALYSIS.md`. The affected 27-case pass and
earlier full-09 78/80 result remain separate, unchanged evidence. The full-run exported visual TAR
equals the affected-10 TAR: those 237 images are not fresh full-10 tour evidence.

These runs use the same API35 AVD through **private ADB 127.0.0.1:5039 / emulator-5680**. The
private-server PID remains 28484 in before/after records; shared ADB 5037 and other devices were not
stopped. The preceding cold boot and endpoint isolation preserved both preference hashes and did
not change RAM/GPU/kernel/data. Details and rejected startup attempts remain in
`build/reports/avd-private-endpoint-20260923-01/README.md`. Cold boot and isolation changed together;
neither later success nor this abort establishes a single-variable cause for earlier UI stalls.

**Visual acceptance is not passed.** The affected-10 review reports cover 41 compact, 46
compact-font-2 and 28 native PNGs, with exact selected filenames recorded in that directory's
`compact-visual-review.md`, `compact-font-2-visual-review.md` and `native-visual-review.md`.
Confirmed enlarged-font findings are Setup half-width choices with `maxLines=2` losing part of
the tower identity, campaign metadata compressed into a letter column, and HUD speed-word
fragmentation. The compact enhancement heading/scroll content intersects the status clock;
the build-10 source had no system inset on that overlay. The native Setup start action also
extends into the gesture region, without proving the action is inaccessible. Subsequent ordinary
MainActivity system-font-2 inspection confirms Activity pause/Setup inset issues and Exit Dialog
word fragmentation. The Dialog itself has safe window bounds, so duplicate Activity-style padding
is not inferred. The Service Dialog fits at native system-font-2; ordinary Resume was not reached.
Evidence is `build/reports/product-dialog-system-font2-build10-20260923-01/README.md`; the original
font/display configuration was restored. Roster full-width actions and vertical terminal controls
are readable in the selected captures, but they do not cancel the remaining findings. Separate
Dialog windows retain native physical-window/font behavior and do not certify compact/system-font-2
Dialog fit. The collected build-11 fixes and their later bounded affected/visual results are recorded
above; they do not relabel these historical findings or certify every window/font combination.
No reference-parity or final visual PASS is claimed from capture success or source review.

Independent source-freeze audit matched all 140 recorded SHA-256 hashes and byte lengths, found
no extra/missing files in the frozen source/configuration scope, and confirmed the clean engine
checkout at the exact lock commit. Evidence: `source-freeze-build10-20260923.json` and
`source-freeze-publication-audit-build10-20260923.md` under `build/reports/`. The ten host-only static
and tooling checks also pass, with exact stdout/stderr retained in
`build/reports/product-static-gates-build10-20260923-01/`. Its four archived real-save comparisons
exercise schema 3 only; they do not prove new schema-4 device restoration or migration. The later
actual update and automatic schema-4 process-death evidence are recorded separately below.

Build-09 affected execution completed **7/7 PASS**, zero failures/skips, on the API35 AVD.
The device-owned full build-09 run completed **78 PASS / 2 FAIL / 80**, with no skipped cases,
from 16:30:46 through 17:09:50 UTC (2344 seconds); it verifies only those installed APKs.
Case 59's teardown failure is described below. Case 79, `compactViewportTour`, exceeded the
unchanged 120000ms limit in ScreenCapture/UiAutomation.takeScreenshot; compact-font-2 and native
tours passed. Same-APK isolated case-59 evidence is separate and cannot erase the full-suite
failure. Shell exit 0 does not change the two-failure JUnit verdict. The full-09 accepted capture
export has 233 current-manifest images: compact has 77 rows, while compact-font-2/native have 78
each. Its extra compact service-confirm PNG is a prior file, not fresh evidence. Use
`build/reports/product-full09-20260923/verified-captures/`; the first truncated export and partial
extraction are retained as failed artifacts, not accepted captures.

The affected run retained 234 PNGs (20 fixtures / 78 images per profile) and all three manifests
under `build/reports/product-affected09-20260923/`. Its archive SHA-256 is
`f195b5cdc8282fd583baf554c197b4d7a878491377c547b7e4827ab2a2f16746`. Successful capture does
not mean every image was reviewed, nor does native Dialog capture prove compact/font-2 Dialog fit.
The native09 follow-up independently inspected `battle-active-root-0.png`,
`battle-active-device-hero-action.png`, `battle-enhancement-device-initial.png`,
`battle-paused-device-resume.png`, `arena-result-device-result-actions.png`, and
`launch-device-initial.png`: no blocking visual findings. Twenty compact09 images and four
compact-font-2 terminal-action images were previously reviewed; Roster/navigation findings led to
the source corrections below. This is explicitly partial historical review, not all-234 signoff.

A bounded contract audit found that FR-110/FR-113's hero-ability loadout selection was missing:
all unlocked skills were automatically equipped. The corrective source batch now adds an explicit
independent 0–2-skill selection, Setup checkboxes, Roster controls, profile schema 4, and schema-3
migration preserving the old implicit set. Newly unlocked abilities require explicit selection;
empty selection remains valid across battle-v5 save/restore. Active/unclaimed runs reject editing;
claimed terminal runs retain their frozen abilities while the next-run selection may change.
The MyEngine pin and battle wire schema are unchanged. Twenty new domain tests and three new
ViewModel/UI tests are authored, along with one additional Roster guard test. All implementation
and hero-selection test sources were frozen before build-10; the domain tests now pass and Android
tests compile; the affected build-10 connected cases now pass, while the full run is incomplete.
A final whole-contract audit also found
the autosave checkpoint omission described below.

The same review exposed already-existing narrow Roster buttons splitting words and compact
navigation labels wrapping. Roster actions are now full-width and vertical; small-width/large-font
navigation scrolls horizontally with whole single-line labels. Four existing strict layout cases
retain their assertions and add these controls. Source review found no Android DTO/mapper/intent
integration defects. The 294 JVM tests and affected 27 Android cases are executed passing results;
the 88-case full run is incomplete as recorded above. Independent domain/Android/autosave source
reviews found no concrete integration defects; later visual findings are not negated by that review.

The original implementation saved ordinary battle progress only every 20 logical ticks and did
not separately detect COMBAT-to-COMBAT wave changes. That conflicts with the batch contract's
completed-batch and wave-transition checkpoints. `ProductViewModel.pulse` now saves every changed
authoritative batch (1x/2x), while frozen no-change pulses do not write. Existing atomic paired
storage, visible failure state and retry policy are unchanged. Four Android regression tests were
complete before build-10, compile successfully and pass in the affected Android run. This
is a contract correction, not an asserted performance fix; new persistence frequency must be
included in the subsequent device measurement.

The build-09 full-suite failure in `ProductBattleUiTest.victoryOffersClaimBeforeRetryAndOptionalStubCheck`
is an ActivityScenario teardown timeout, with last delivered state PAUSED. The preserved
`product-full09-20260923/system-through-failure59.log` shows EmptyActivity did launch and resume,
so this is not the earlier repeated-EmptyActivity handshake failure. ComponentActivity finish was
requested at 16:55:55, system destroy timed out at 16:56:13, and actual STOPPED/DESTROYED callbacks
arrived at 16:57:28, about 93 seconds after finish. SystemUI simultaneously logged 5385 skipped
frames (about 90 seconds). This supports a shared system/window/graphics stall but does not prove
its root cause. The static fixture callback only records an action; it does not run the product
ViewModel, persistence or a real reward transaction. Teardown can mask a body failure, so no
all-assertions-passed claim is made. No production change or close workaround is justified yet.

An 8.005472-second build-09 trace is retained in
`build/reports/mysd-atrace-build09-20260923-01/device/`, with `ANALYSIS.md` in its parent; raw trace SHA-256
`ac8fb8bfd682b54c116fa9a886631e2534f73004850b862e5b508c0744daf34c`.
Six main-thread fsync spans total 116.699ms, while 55 RenderThread waits total 2868.040ms.
Render drawing/flush and JIT contention are also substantial. The counter interval has no swap-out,
direct-reclaim or allocstall increments, so a RAM-pressure or fsync-only diagnosis is unsupported.
The next discriminating capture uses the newly authored non-debuggable `benchmark` variant with
recorded full-AOT compilation and warmup. The variant now builds and passes lint in build-10, but
has not been measured. Debug frame failures remain failures; no device performance threshold is relaxed.

## Build-10 actual migration and schema-4 process death

The independently audited real update in
`build/reports/product-upgrade-build10-20260923-01/README.md` passes for this bounded profile-3 to
profile-4 migration. Archived build-10 APKs were installed with `install -r`, followed by ordinary
MainActivity launch; no save injection, uninstall or data clear was used. The stored natural
unclaimed defeat remains `run-449cc26fa53192ff-1`, stage-ember-path, tick 540, with exact 1784-byte run
document SHA-256 `45bc7eb6fe5482ea286eeb171ccaaa81dbb0a56ed56eaf866cee8f21630eb773`.

Every profile field was reconciled: 33 old fields become 34, with 26 ordinal-equal, seven energy-
refresh-related changes and one added field. `selectedHeroSkillIds=hero-hearth-repair` is exactly
the previously implicit unlocked-hero selection. Credits stay 509 and the current run stays
unclaimed. Energy 9 -> 10 is the capped recovery over 4713 seconds since the old anchor; the
observed clock advances 4537 seconds and both clocks end at the full-cap refresh time. Exactly one
ENERGY_REFRESH ledger row and one revision/event/ledger-cursor advance account for +1 energy;
all old rows and other balances remain exact. The audit enumerates all fields and the arithmetic,
not merely a permissive changed-field whitelist. This is actual update migration, not OS death,
arbitrary migration coverage or interrupted-write proof.

A separate automatic process-death check of the migrated schema-4 unclaimed terminal passes in
`build/reports/mysd-process-death-host-20260923-terminal-unclaimed-03/`. Its exact installed build-10
identity was checked on private ADB 5039. HOME/background followed by `am kill` made PID 4246
disappear (`pidof` status 1 and old `/proc` absent); ordinary cold relaunch returned `Status: ok`
and distinct PID 4438. `completed.json` records automatic PASS, not a manual supplement.
Strict same-schema comparison retains exact run bytes and hero selection, claims, currency,
ledger, revision and event/ledger cursors. Only full-energy clock coordinates advance 79 seconds,
with zero energy grant or new row. Root reviewed the byte-identical before/after PNGs showing the
same unclaimed defeat and available claim. This proves that one schema-4 state, not claimed/paused
schema-4 restoration, a live empty-hero path, natural victory, physical-device behavior or parity.
All earlier schema-3 driver failures and manual supplements remain exactly as recorded below.

## Executed gates

| Gate | Result | Evidence / limitation |
|---|---|---|
| MySD public safety | pass, exit 0 | 146 tracked files; 2019 history paths; creative_assets_checked=0 |
| MySD spec | pass, exit 0 | 26 requirements; 18 stories; 18 acceptance cases; 26 trace rows |
| MySD evidence | pass, exit 0 | 13 nodes; 25 edges; 12 observations; accepted old corpus only |
| MySD diff whitespace | pass, exit 0 | empty `git diff --check` output |
| New resource source review | pass by inspection | two original vector drawables and procedural PCM have approved provenance; no new raster/audio/font/binary media found |
| Runtime Android/game dependency boundary | pass by source inspection | `engine-runtime` depends only on engine-core; no Android/AWT/game imports |
| Game Android/network boundary | pass by source inspection | no Android/AWT/production SDK imports in game; app manifest has no INTERNET |
| MyEngine JVM + Android assemble | pass, exit 0 | 184 tests / 35 suites, zero failures/errors/skips; BUILD SUCCESSFUL; `D:/Pet/MyEngine-mysd/build/reports/eng036-final-20260923-01.log` |
| Engine selfcheck | pass, exit 0 | no missing files, adapter drift, invalid JSON, or missing marketplace sources |
| Engine content validation | pass, exit 0 | validated 2 packs |
| Engine replay | pass, exit 0 | tick 35 canonical `12a65fd2b87593cf`; kill `bb37eefc1903cc77` unchanged |
| Engine save compatibility | pass, exit 0 | scoped save runner plus full sandbox suite |
| Engine cold balance smoke | pass, exit 0 | canonical 849 ms / kill 141 ms; not a regression comparison |
| Engine original warmed diagnostics | historical FAIL in both comparisons, exit 1 | first canonical +36.74%, kill +26.68%; second canonical +17.33%, kill -0.58%; all sample hashes match; raw reports retained; later controlled acceptance is a separate row |
| Engine controlled paired comparison | A/A calibration pass; A/B overall FAIL | A/A canonical -0.554% / kill +0.434%; A/B canonical +4.580% / kill +8.500%; identities, goldens and every sample retained in paired directory 02 |
| Engine sandbox callback stabilization | functional pass; final timing in paired-03 row | unchanged tower/reward/incident handling extracted into private helpers; callback 433 to 239 bytecodes; 184 tests / 35 suites / zero failures or errors, replay and save compatibility pass |
| Engine final controlled paired comparison | pass, exit 0 | paired directory 03; A/A canonical -3.440% / kill +0.596%; A/B canonical -4.094% / kill -3.761%; unchanged 5% threshold and all hashes/isolation checks pass |
| MySD JVM/lint/assemble + instrumentation compile | first diagnostic FAIL, exit 1 | 269 JVM tests / 31 suites; one stale legacy upgrade assertion failed; Android compilation found semantics receiver shadowing; lint/assemble/instrumentation compile not reached |
| MySD second diagnostic | partial; overall exit 1 | all 269 JVM tests pass; app debug APK assembled; instrumentation compilation failed on four invalid top-level imports; current lint report not produced |
| MySD third diagnostic | pass, exit 0 | test/lintDebug/assembleDebug/assembleDebugAndroidTest; 269 JVM tests passing from second run, unchanged/up-to-date in third; lint 0 errors / 40 warnings; APK and instrumentation APK available |
| MySD fourth diagnostic after rendering/backup stabilization | pass, exit 0 | same full task set; new app/instrumentation sources compile; JVM unchanged/up-to-date; lint 0 errors / 39 warnings; connected execution still required |
| MySD synthetic peak +25% load | pass in first JVM run | 35 mobile entities + 4 towers; 256 samples; reducer p95 0.1247 ms; repeated digest `b94cb689a90283b4`; not a device frame measurement |
| Connected Android first run | incomplete / diagnostic abort | Pixel_9 API37.1; 12 passed, 13th blocked in Compose waitForNextChoreographerFrame; target process stopped intentionally after retained JDWP stack/screenshots; 62 not executed |
| Isolated formerly blocked campaign test | pass | exact same APKs, manual instrumentation, 8.962 seconds; no production/test source change |
| Connected Android second manual run | incomplete / diagnostic abort | 30 pass, 15 fail, 1 aborted in progress, 29 not started; black surfaces, Compose frame waits and host ADB stream loss require same-APK environment isolation |
| Claim-history capacity regression | pass, exit 0 | five JVM tests; restored/live 10,000-claim boundaries, exact victory/defeat grants, duplicate and stale-pair rejection; `product-claim-cap-stabilization-20260923-01.log` |
| MySD fifth final consumer gate | partial; overall exit 1 | all 274 JVM tests / 32 suites pass; cosmetic legacy background import used the wrong package and failed Android compilation; retained log 05 |
| MySD sixth final consumer gate | pass, exit 0 | accepted engine `1174d21`; all 274 JVM tests / 32 suites, zero failures/errors/skips; lint 0 errors / 39 warnings; both APKs assembled; retained log 06 |
| MySD seventh final consumer gate | pass, exit 0 | 78 seconds; stabilization sources compile; unchanged 274 JVM tests / 32 suites up-to-date, no failures/errors/skips; lint 0 errors / 39 warnings; both APKs assembled; log 07 |
| MySD eighth final consumer gate | pass, exit 0 | 75 seconds; same full task set and unchanged 274 JVM tests; lint 0 errors / 39 warnings; log 08 |
| MySD ninth final consumer gate | pass, exit 0 | 125 seconds; terminal action layout and corrected text geometry checks compile; unchanged 274 JVM tests; lint and both APKs pass; log 09 |
| MySD tenth frozen consumer gate | pass, BUILD SUCCESSFUL | 10m15s; 294 JVM tests / 33 suites / zero failures/errors/skips; debug lint 0 errors / 39 warnings, benchmark lint 0 errors / 38 warnings; debug, instrumentation and benchmark APKs assembled; log 10 |
| Build-10 source-freeze/publication audit | pass, bounded source inspection | 140/140 file hashes and sizes match; no new/missing scoped sources; clean accepted engine checkout; manual vector/PCM provenance and untracked path review; publication selection still required |
| Build-10 host static/tool regression | pass, 10/10 commands, exit 0 | public safety, spec, evidence, diff check, three-scenario analyzer, four archived schema-3 paired-save comparisons, Bash runner syntax; no device execution |
| API35 same-build detached isolation | pass, three isolated tests | legacy handoff 14.646 s, malformed recovery 29.445 s, compact/font-2 settings 17.474 s; complete device-owned logs; does not erase API37.1 failures |
| API35 detached full Android suite 03 | FAIL, complete | build-06 APKs; 72 passed / 3 failed / 0 skipped, all 75 cases executed; 1393 seconds, final marker 15:25:44 UTC; shell exit 0 is not the JUnit verdict |
| Build-06 visual tour | complete with findings | 20 fixtures x 3 profiles, 69 PNGs reviewed; layout/copy findings and Dialog capture gap recorded below; not reference-parity evidence |
| Process-death automatic driver attempts | FAIL, preserved | build-09 unclaimed and paused-built relaunch returned exit 255; claimed relaunch returned Status: timeout; all three attempts had already recorded actual old-process disappearance |
| Process-death manual supplements | supported, bounded manual proof | terminal unclaimed, terminal claimed and paused-built: distinct new PIDs, exact run restoration, profile invariants/legitimate energy refresh and before/after UI independently audited; not automatic driver passes |
| Real first reward claim | supported, exact paired-save delta | credits 500 -> 509; one current-run marker and one BATTLE_REWARD ledger row; one revision/event/ledger advance; no extra reward on claimed restoration |
| Gfxinfo reset-window analyzer self-test | pass, three synthetic scenarios | existing named-column/quantile/dedup checks plus pre-reset exclusion, exact-boundary inclusion and window/global precedence; tooling evidence only |
| Corrected real-app frame window | FAIL / insufficient acceptance evidence | 6 frames; p95/max 3771.164976ms, median 2065.524292ms; 6/6 above unchanged 16.7ms; one pre-window and two invalid rows excluded; debug AVD only |
| Build-09 affected Android gate | pass, complete | 7/7 tests, zero failures/skips; 296.191s JUnit; 234 PNGs captured, manual review separate |
| Build-09 full Android gate | FAIL, complete | 78 PASS / 2 FAIL / 80; teardown59 and screenshot79 timeout; exact build09, not the later source batch |
| Corrective hero-selection/autosave batch | frozen; JVM/compilation and affected Android pass | profile v4 + legacy migration, optional independent skills, Android Setup/Roster controls, every completed batch persisted; 20 additional JVM tests pass and 8 additional Android cases pass in affected-10; no engine change |
| Build-10 affected Android gate | pass, complete | 27/27, zero failures/skips; 249.671s JUnit; exact installed APKs on private ADB 5039; 237 fresh PNGs, visual review separate |
| Actual build-09 to build-10 update migration | pass, bounded real-save proof | profile 3 -> 4; every field reconciled, exact natural unclaimed run, prior implicit hero selection retained, only quantified capped energy +1 and one ledger/revision/event advance |
| Build-10 schema-4 unclaimed process death | automatic pass, bounded | old PID 4246 absent, cold new PID 4438; exact run/hero/claim/currency/ledger, full-energy clocks +79s only; not a replacement for old failed drivers |
| Build-10 full Android gate | INCOMPLETE / instrumentation abort | 72 PASS; case 73 started without verdict; 15 not started; Watchdog killed system_server after window/display/animation blockage; truncated metadata and all raw logs retained, deeper graphics cause unestablished |
| Build-10 offline visual review | findings open, not accepted | selected 41 compact / 46 compact-font-2 / 28 native PNGs; Setup name loss, campaign squeeze, HUD fragmentation and inset observations; Dialog system-font coverage not inferred |
| Collected UI stabilization batch (build-11) | host build PASS; affected Android PASS | 141 files frozen at 18:42:22 UTC; 11m37s, 148 tasks; fresh 294 JVM tests / 33 suites / zero failures/errors/skips; both lint variants and three APKs pass; six new Android regressions, 94 Android expected; no domain/engine/schema change |
| Build-11 artifact/source identity audit | PASS, host-only | three APK source/archive hashes and sizes match; common verified v2 certificate; min26/target36/no INTERNET; benchmark non-debuggable/shell-profileable; 141/141 freeze files exact with no extra/missing scoped sources; no installed-device claim |
| Build-11 host static/tool regression attempt01 | 5 PASS / 7 environment FAIL, retained | Windows PowerShell 5.1 inherited incompatible PowerShell 7 module paths; Get-FileHash unavailable; raw streams preserved |
| Build-11 host static/tool regression attempt02 | PASS, 12/12 | child-only module-path correction; unchanged gates including source freeze/engine identity and four historical schema-3 pairs; no new device proof |
| Build-11 ordinary launch and continuity | automatic launch FAIL; bounded manual supplement | Status: timeout, 14538ms; later same exact paused tick18 run/profile invariants under SystemUI ANR; one Wait restored unobstructed UI; not OS-process-death proof |
| Build-11 affected Android gate | PASS, complete | 33/33, zero failures/skips; 334.618s JUnit; installed archive hashes match, private ADB5039/SwiftShader/post-Wait condition; 237 fresh PNGs; all six new regressions pass |
| Build-11 bounded visual review | no new demonstrated layout blocker in selected views | 46 compact / 56 compact-font-2 / 40 native, 142 selected of 237; two blank font-2 roots and three incomplete focus captures retained; native/font1 Dialog caveat; no all-image or parity acceptance |
| Build-11 full Android gate | RUNNING, no final verdict | 94 expected; affected-subset success is not full-suite acceptance |
| Remaining final Android/device gates | pending | complete full Android result, actual system-font Dialog smoke, current-build real OS-death and non-debuggable device frame acceptance; schema-4 proof remains scenario-bounded |

Public safety scans tracked/index/history paths, not every untracked source and not XML/Kotlin as
creative assets. The explicit new-resource review supplements that check; a scoped publication
candidate must be checked again after staging. Old evidence validation does not establish official
`com.yuegame.defender` parity.

## Source-review findings closed before execution

- Repeated UI input previously advanced full simulation ticks. The opt-in MyEngine current-tick
  command drain and MySD command-only reducer now keep input separate from time.
- Profile/run restoration enforces transaction identity and launch-coordinate coherence.
- Ally-health and hero-cooldown enhancements canonicalize already-active entities/timers.
- Invalid save fallback preserves content-addressed originals before any overwrite. Failed durable
  saves retain dirty in-memory state, expose a warning, and retry while RESUMED.
- Large-font/compact layout, battle lifecycle, legacy handoff, and local feedback sources were
  completed before test execution. Their correctness is still subject to connected tests.

## Engine comparison baseline

Exact baseline: `30f4eb17aff0ea2fe6cf80aef970a1e7746dbcbb`, retained as
`D:/Pet/MyEngine-mysd/build/eng036-baseline-20260923` plus its original ZIP archive.
The original separate-process diagnostic used `scripts/me-runtime-benchmark.ps1`; the controlled
acceptance comparison now uses `scripts/me-runtime-paired-benchmark.ps1` as explained below. Do not
substitute the unrelated sibling MyEngine checkout.

The first comparison uses 32 warmup batches, 41 measured batches, 8 sessions per batch and 3 forks,
selected before execution for JIT stabilization. Host: Windows 11 Pro 10.0.22631, OpenJDK
21.0.10+-14961533-b1163.108. Pixel 8 was saved as `mysd-pre-final-20260923` and stopped before
sampling. No concurrent builds/tests/emulator workloads are intentionally run during the comparison.

The first wall-time comparison failed its unchanged 5% limit. Baseline fork medians were dispersed
(canonical 11.89 / 5.42 / 5.12 ms); this is a reason to investigate attribution, not to discard the
failure. Engine acceptance and lock publication remain blocked on resolving this gate. While the
engine agent inspects the comparison read-only, an initial MySD diagnostic gate is running to expose
independent integration failures. It must be repeated against the finally accepted engine revision.

## Final stabilization findings

- MySD first diagnostic (`build/reports/full-product-final-20260923-01.log`) found a Kotlin semantics
  receiver collision: `selected` bound to a captured destination parameter instead of semantics.
  Fixed to `this.selected`; source-frozen pending rerun.
- Legacy `PlayableBattleStateTest` still expected one stat increment after two paid upgrades despite
  the deliberate first-upgrade correction. Updated the expected second increment and added an
  explicit first-upgrade damage assertion. The production upgrade formula was not changed to satisfy
  the stale test; behavioral engine upgrade tests already passed.
- Second diagnostic (`build/reports/full-product-final-20260923-02.log`) compiled the application
  and passed all 269 JVM tests. Instrumentation tests imported `assertExists`/`assertDoesNotExist`
  as top-level extensions even though the methods belong to the interaction object. Removed those
  four imports across three files; assertions themselves remain. Third diagnostic rerun started.

Third diagnostic (`build/reports/full-product-final-20260923-03.log`) passed all requested tasks.
Lint warnings remain visible (SDK/dependency age, existing modifier conventions, backup extraction
declaration, unused resources, versioned icon hints, KTX/catalog suggestions); none are suppressed.
This is provisional integration evidence against the worktree, not an accepted engine pin.
The second benchmark report (`eng036-runtime-benchmark-20260923-02.json`) failed canonical +17.33%
while kill passed at -0.58%. It adds CPU/allocation/GC diagnostics outside the unchanged wall-time
block with the same 32/41/8/3 settings. The host remained free of other intentional build/test/AVD
workloads. Matched JFR profiles were retained as `eng036-{baseline,candidate}-profile-20260923.jfr`.
They showed late C2 recompilation; no source-supported runtime regression has been identified.
Two diagnostic runs of the SAME baseline reported +108.56% canonical / +130.55% kill despite
identical allocation counts (`eng036-baseline-aa-{first,second}-20260923.json`). Thus the current
separate-process comparison cannot resolve a 5% delta on this host; this does not turn either failed
gate into a pass. A paired isolated-classloader A/A then A/B measurement is being authored, with
fixed 128 warmups / 101 samples / 8 sessions, own-process CPU affinity, and the unchanged 5% limit.
Independent source review found no blocker in the paired methodology. Loaded JAR/content hashes,
baseline commit/candidate diff, and harness identities are retained outside timed regions. The
first paired runner directory (`eng036-paired-20260923-01`) stopped before any measurement because
Windows PowerShell returned a null process ExitCode despite a successful self-test. Caching the
process handle repairs that startup issue; the one prescribed measured set is running in new
directory `eng036-paired-20260923-02`. Neither A/A nor A/B timing was discarded from directory 01.
The prescribed set is complete: A/A stable/pass, but A/B kill exceeds the unchanged 5% limit.
Runtime production sources remain unchanged while the specific hot-path regression is investigated.
Host reservation is released for provisional Android connected execution; it must not be confused
with acceptance of the engine pin.

Additional source verification found that the declared 60 FPS presentation target had no smoothing
between authoritative 20 Hz mobile-entity snapshots. Presentation-only interpolation is being
stabilized with reduce-motion and phase-freeze coverage; no authoritative time/state changes are
authorized by that rendering fix. The earlier successful Android APK is superseded once this
source fix lands and must be rebuilt/rechecked before device execution.

The draw-only smoothing and explicit backup exclusions are source-complete with five connected
test cases. Fourth MySD build/lint/instrumentation compilation passed, exit 0, in one minute
(`build/reports/full-product-final-20260923-04.log`). The gfxinfo analyzer's
synthetic self-test passed two scenarios, including two distinct windows with identical timestamps
and overlapping report files; this is tooling evidence only, not actual device timing.

Fourth-build provisional APK SHA-256: app
`d9babfb6b53083c9c1a6203ed5d677a3fde66ed84ce4b3dd1ca2a9c1806b66d4`, instrumentation
`c76e1b49f495a46c31be2cbfe34eb570b1aed9ac7fa73b06c6cba549674c8337`.
Both are local ignored build artifacts, not publication candidates yet.

## Android diagnostic environment and first run

- Pixel_9 / emulator-5554 uses the existing Android 17 / API37.1 image and normal configured GPU.
  Recoverable snapshot `mysd-pre-connected-20260923-01` was saved before execution. No wipe or
  manual app-data clear was performed. Guest wall date comes from its old snapshot (September 21),
  so report filenames/host timestamps are the September 23 evidence anchor.
- `full-product-connected-20260923-01.log` reached 12 passes then blocked in the 13th test,
  `CampaignContentUiTest.st0006_rendersSnapshotCampaignShell_preservesAcceptedActionsAndDeferredRoutes`.
  JDWP stack (`mysd-connected-hang-stack-20260923-01.log`) locates `FutureTask.get -> Espresso.onIdle
  -> waitForNextChoreographerFrame -> assertIsDisplayed`, source line 78. No capture or shell-copy
  operation had been reached. Main Looper remained alive; power state was Awake/Display ON.
- The debugger was detached and all threads resumed. The target instrumentation process was then
  intentionally force-stopped after more than six minutes without progress. AGP's resulting
  `Process crashed` is the diagnostic abort, not an observed spontaneous application crash.
  First reports/results are retained under `connected-first-20260923-01` and
  `connected-first-results-20260923-01` in local build reports.
- AGP automatically removed its temporary packages on cleanup (the target was absent before the
  run too). The same hashed APKs were reinstalled; isolated manual instrumentation passed the exact
  blocked test in 8.962 seconds (`full-product-connected-isolated-campaign-20260923-01.log`).
- Full manual instrumentation is now running with a per-test 120-second bound. This also preserves
  app-private visual captures after completion rather than losing them to AGP cleanup. A stable full
  result is still required; the isolated pass does not erase the first run's incompleteness.
- The second run passed the formerly blocked campaign test but reported missing-text assertions
  in legacy handoff and recovery scenarios. Host ADB instrumentation stdout disconnected at test 21
  (exit 255, without a terminal test result); the device runner continued, as independently observed
  through timestamped TestRunner logcat. Its remaining outcome must not be inferred from host exit.
  Several different MainActivity fixtures became black; BLASTSyncEngine reported an unacknowledged
  window-transition commit. These observations require environment isolation before attributing
  every missing-text assertion to independent product defects. No source fix was made for these
  assertions at this point.
- This second diagnostic was stopped after repeated black surfaces/Compose frame waits: 30 passed,
  15 failed, one in progress aborted, and 29 not started. Device-log continuation, a second JDWP
  stack, and six earlier legacy fit PNGs are retained. Product visual fixtures were not reached.
  Snapshot `mysd-post-connected-diagnostic-20260923-02` was saved without replacing the pre-test
  snapshot. Pixel_9 was then stopped normally with `adb emu kill`; its emulator/QEMU PIDs were
  confirmed absent. Repeated unexpected ADB server restarts are an additional host observation,
  not yet an established cause. Same-APK cold-boot isolation is pending the engine quiet window.
- Independent read-only stack/log review finds no demonstrated MySD deadlock: main is idle in
  `nativePollOnce`, while the test waits in Espresso/Compose next-frame synchronization. Native
  RenderThread evidence is absent, so a GPU fault is not established. The legacy-to-product
  assertion occurred before recorded BLAST errors and must be isolated independently rather than
  automatically classified as an environment failure. Cold-boot checks should also run the final
  compact settings assertion first, then its preceding accessibility sequence if needed.
- Original API37.1 cold boot did not complete in five minutes; one verbose/kernel diagnostic was
  bounded to 120 seconds and also failed readiness. Logs establish framework boot/service lookup
  stalls, not a specific kernel or MySD fault. Both original snapshots remain retained. A separate
  new `MySD_Pixel9_API35_20260923` AVD was created with an existing API35 image and matching Pixel9
  viewport/density, without copying or clearing existing device data. Exact environment is retained
  in `build/reports/android-isolation-environment-20260923.md`.
- On API35, the isolated compact settings test passed on the same build-04 APK. A later legacy
  test failed earlier than on API37.1 (before the Continue tap), with `DeadObjectException` during
  `UiAutomation.disconnect` after loss of its host instrumentation stream. Server PID stayed stable
  while guest transport identity changed; this occurrence is a reconnect, not an ADB server restart.
  Detached device-side instrumentation is the next isolation step so its owner/log lifetime does
  not depend on a continuous host shell. No product-code fix is justified by these observations yet.
- Detached same-APK/API35 isolation passed all three scenarios: legacy handoff 14.646 seconds,
  malformed-run recovery 29.445 seconds, compact/font-2 settings 17.474 seconds. Device-local
  instrumentation logs provide complete `OK (1 test)` outcomes without the previous UiAutomation
  dead-object failure. The direct malformed run had failed in `UiDevice.getInstance` before any
  app launch. This specifically isolates the host-owned automation failure; it does not prove that
  every API37.1 black-frame or boot symptom shares that cause.
- Actual legacy screenshot review found a default-white strip beneath the dark compatibility
  surface. Only its containing Column background was changed to `ProductColors.Void`; layout and
  migration logic are unchanged. Final consumer build 05 found the new color import pointed at the
  wrong package. Root corrected that one import to `ui.theme`; the failure is retained in
  `full-product-final-20260923-05.log`, and full gate 06 is running against accepted engine pin.
  Original build-04 APKs remain under `build/reports/apks-build04` for diagnostic reproducibility.

## Additional boundary review

Independent persistence review found a supported-history limit dead-end: after 10,000 claimed
battles another run could start, but its reward could not be claimed and unclaimed terminal guards
prevented exit. A bounded, facade-scoped history repair and stale-pair/duplicate regressions are
being implemented as final stabilization. Direct manager idempotency must not be weakened.

The repair is source-complete and its five targeted JVM tests passed in 18 seconds. Only the
synchronized facade compacts the full history for its current unclaimed terminal, retaining the
current duplicate marker and restoring old markers on typed rejection. Strict profile/latest-run
identity rejects an evicted older run paired with the current profile. Direct manager capacity and
duplicate guards remain unchanged; whole-bundle offline rollback prevention is not claimed. The
complete MySD gate and Android artifacts still need rebuilding after this domain-only patch.

The post-review sandbox helper extraction passed the full 184-test engine gate, fresh distribution
assembly, replay and save compatibility. Evidence: `eng036-after-helper-20260923-01.log`,
`eng036-after-helper-replay-20260923.log`, and `eng036-after-helper-save-20260923.log` in engine build
reports. The next prescribed paired A/A and A/B set will run only after the AVD is stopped; the
smaller callback is a performance hypothesis, not an accepted measurement.

That one prescribed post-fix set is now complete and passes. Paired directory 03 retains all samples
and identity files, with report SHA-256
`fcba53a30178293fde7b7b726e6c6cffe89406a23e3f8a3191408d8efc71a1f8` and unchanged harness SHA-256
`0ccff0377029212d41421394dd8df14263aef1840a03c92ac805213844dec9d5`. A/A absolute deltas are within 5%
and neither A/B scenario regresses. The root independently reviewed the report, generic runtime
boundary and adapter changes; ENG-036 technical acceptance is local, with scoped documentation and
commit/pin closeout pending. No remote publication has been authorized.

Root then completed the explicit 39-file local engine commit
`1174d21c4e92fddff6316f93bc99384ab6c1c689` (38 feature/docs files plus the required generated retro),
verified a clean engine worktree, and updated `gradle/myengine.lock` to that exact SHA. The one engine
run telemetry event records technical success with partial delivery because publication is not
authorized; prior failed gates/retries remain in it. A two-line documentation whitespace issue found
only after staging was repaired before commit. No runtime/test/harness source changed after the
accepted paired03 measurement. The final consumer gate must now run against this exact local pin;
neither repository was pushed.

Final consumer gate 06 completed successfully in 2 minutes 53 seconds against the exact accepted
engine pin (`build/reports/full-product-final-20260923-06.log`). Its 274 tests in 32 suites have
zero failures, errors, or skips; lint has zero errors and 39 unsuppressed warnings. The original
content-load test's latest 256-sample reducer p95 is 0.1729 ms, digest `b94cb689a90283b4`, for
35 mobile entities plus four towers. This remains synthetic JVM evidence, not Android frame timing.
Public safety, specification, old-corpus evidence, diff checks and the two-scenario gfxinfo analyzer
self-test also passed in the companion static gate.

Final build-06 APK SHA-256: app
`b28286430dc13a06b825e5941a32bbe3dece8397d47d33b6ad4e43c2ea1e35df`, instrumentation
`c76e1b49f495a46c31be2cbfe34eb570b1aed9ac7fa73b06c6cba549674c8337`.
The full 75-test Android gate is being run on the separate API35 AVD with detached, device-owned
instrumentation. Replacing the installed APK uses `install -r`, without clearing application data.

The detached full run reached 40 passing cases with two failures and continued into the product
suite. Test 32 (`ResumeContentUiTest`) exposed a stale combined fixture: Cancel correctly creates
the new product profile, but the second launch seeds only the legacy save. The product profile
therefore correctly wins. The missing launch-action assertion was masked by the 120-second test
timeout during ActivityScenario cleanup. The first Cancel title assertion actually passed because
both campaign surfaces include that title; a text mismatch was an initial hypothesis, not the
established cause. The fixture will be split into independent Cancel/capture and Continue cases,
retaining all assertions and making both preference-store restoration attempts exception-safe.
No production change is justified. The full log excerpt is retained as
`full-product-detached-20260923-03-resume32-device.log`.

Test 33 (`RosterContentUiTest` settings) passed its preceding semantics assertions but timed out
inside `captureToImage` / `WindowCapture.withDrawingEnabled` (2 seconds). A relation to preceding
Activity cleanup is unproven; an unchanged isolated recheck is required before changing capture
logic. Later roster and all storage tests passed, so a persistent global black-window failure is
not established. The whole run continues to collect independent failures before stabilization.

## Complete device run 03 and final stabilization

The full device-owned run completed all 75 cases: 72 passed, three failed, zero skipped. Raw evidence
is retained under `build/reports/full-product-detached-20260923-03/`, including instrumentation,
system logcat, completion metadata, all visual profiles and external FIT captures. Installed-target
and installed-test SHA logs independently match build 06; the first metadata `pm path` failed a
Binder transaction and is not the identity proof. A zero-byte settings FIT PNG is retained as failed
capture evidence, not counted as a valid image.

The third failure was `ProductActivityRelaunchTest.pausedBuiltBattleSurvivesRecreationAndFreshActivityWithoutTickOrEntityLoss`.
Initial/recreated paused visibility and first stopped save equality passed. The final timeout
occurred in ActivityScenario cleanup; the final in-block assertions were not separately logged, so
their success is not claimed. System logs show repeated EmptyActivity launches after CREATED,
not a background-launch denial. AndroidX core 1.7.0 source and the matching approximately 45-second
destroy delays support a repeated-resume-handshake explanation; the missing broadcast itself was
not directly logged. The source-only repair resumes the paused/settings scenario after stopped-state
assertions and before close, checks DESTROYED, logs milestones, preserves primary failures and
attempts both preference restorations independently. No production lifecycle fix or larger timeout
was substituted. The legacy Cancel/capture and Continue branches are now independent tests.

All three visual tours passed as tests, but review of all 23 PNGs in each profile found:

| Finding | Bounded source repair / evidence consequence |
|---|---|
| Shop title and demo badge overlap/squeeze | Stack title and badge at full card width |
| Three-column tech cards split words and actions on phones | Adaptive 1–3 columns with 160dp x font-scale minimum, preserving prerequisite order |
| Compact reward description squeezed between number and action | Full-width copy and action below the tier header |
| Enhancement heading shows live HUD underneath | Opaque product background for this full-screen overlay |
| Product settings/setup body still describes earlier contour behavior | Separate accurate original product copy; legacy resources preserved |
| Both Compose roots capture Activity background instead of Dialog | Keep baseline root captures, add full-display screenshots after Dialog-tag/title assertions; native Dialog window is not claimed as compact |
| Large-font initial views omit below-fold controls/status | Add scroll-only action/status captures and assert immutable fixtures |

The independent settings PixelCopy timeout is addressed with a full-display UiDevice capture after
all existing semantics/bounds checks. This is justified by the measured Dialog capture gap as well
as the timeout; it does not establish that the preceding lifecycle failure caused it. Four new
layout tests cover 320/411dp at font scales 1/2, real text layouts, action bounds/payloads and disabled
guards. Combined with the legacy split, the next complete suite expects 80 cases. Source review is
not a runtime pass. Build-06 artifacts/captures remain immutable and do not verify the new sources.

Gate 07 passed and the build-06 APK pair is retained under `build/reports/apks-build06/`. The new
target SHA-256 is `80e4791d75aec958bedcab4ef29c0641a4a295d76b3ff3e9a99423e5d28fc5e4`; instrumentation
is `e2e288d9c139e77c0fb660a00c25b4c9c633360d680c9b528453e1c84798ef68`. Both installed APKs were
independently hashed and match. The immediate detached runner metadata could not resolve their
paths, so separate installed-SHA logs are authoritative for identity. The 14 affected Android
cases began at 15:42:37 UTC; complete verdict and visual review are pending. No app data was cleared.

The affected-07 run completed at 15:47:36 UTC: 7 passed / 7 failed / zero skipped of 14 in 299 seconds.
Both legacy branches, all three lifecycle cases and both Roster cases passed. All four new layout
cases reported `didOverflowWidth` for a tech title; the compact real-catalog PNGs nevertheless show
readable whole words, so visible clipping or fractional-rounding causality is not asserted. The
follow-up makes the centered tech Column/title/status explicitly full-width and adds numeric
diagnostics without weakening strict overflow/word checks. All three capture tours reached the
service fixtures but failed because the title text matched both the heading and fixture subtitle.
A unique heading tag plus exact text matcher resolves that ambiguity. Completed pre-failure PNGs
are useful evidence only where recorded in each new manifest; leftover files from the prior tour
are not promoted. The entire affected-07 directory and build-07 APK pair are preserved. Gate 08 is
running after this bounded source freeze.

Gate 08 passed. Its 14 affected device cases completed 10 passing / 4 failing / zero skipped. All
three visual tours now completed; independent review of all six full-display service Dialog images
confirms readable local-only, no-ad/no-payment/no-grant explanations and the confirmation action.
Their native separate windows do not inherit the compact Box or simulated font-2 density; no such
Dialog accessibility coverage is inferred. Scrolled compact/font-2 terminal screenshots exposed
fragmented Retry/Return words; those actions are now stacked full-width with unchanged guards.

The four layout failures moved from titles to shrink-wrap button labels. Numeric diagnostics show
240px actual text width but reconstructed paragraph widths of 266/524px. Exact Compose foundation
1.11.3 `ParagraphLayoutCache.slowCreateTextLayoutResultOrNull` rebuilds the semantics paragraph with
previous maximum constraints while returning the original layout size. The revised test uses the
same intrinsics at the measured node width, checking strict line/height bounds, whole words, full
text and no ellipsis rather than misusing that reconstructed raw overflow flag. Raw diagnostics are
retained. This is not an epsilon increase or a claimed production clipping fix. Gate 09 passed with
target `855d707900844948b5df2e5dc39b5db57befa0aea90cb42971accfe1b5fdd723` and instrumentation
`c27d0a0ac6ad5c776a165ebbeb2702e1c13f9fdfc59096b643ecbc7f31bb4439`; the later Android results are recorded above.

## Initial real-app performance diagnostics

Build 08 was launched normally with the previously paused natural run. That run was abandoned via
its ordinary confirmation, and a new first-stage battle was started from the real setup UI. No
fixture or save injection was used. No-command play reached the second-wave enhancement, followed
by the first offered enhancement and natural defeat at tick 540 without a reward claim.

The initial detached 1x frame capture retained 23 complete frames and p95 970.39ms, but its run-as
XML outputs were empty without an error. Thus its continuous-combat phase is unverified. A second,
host-captured 1x window has valid paired saves (same run, COMBAT, unpaused, ONE_X, tick 340 to 360).
Its first analyzer result was 7 frames / p95 4221.14ms / 100% above budget. The maximum frame begins
1.867 seconds before the process `Stats since`, so it crosses the reset boundary. The analyzer now
excludes complete unflagged rows whose IntendedVsync is strictly earlier than the effective
boundary, using the window's own Stats since when present and the process boundary otherwise;
equality belongs to the window. Flagged/incomplete classification and timestamp/scope deduplication
are preserved, and the 16.7ms budget is unchanged. It exposes global/per-file
`pre_window_rows_excluded`, the process `stats_since_ns` and every scoped boundary. All three
synthetic self-test scenarios passed, including a crossing frame, boundary equality, window-local
override and process fallback. This is an analyzer correctness check, not a device-performance pass.

Fresh result: `build/reports/mysd-frames-host-20260923-1x-01/analysis-reset-window-20260923.json`.
The raw framestats SHA-256 is
`8905875f541d71a8ff6fc703e85402796bd6ec63826a034e52d1849960e90555`. Process Stats since is
5185387865900ns; the overriding window boundary is 5185387749700ns. One pre-window frame is
excluded. Two other rows have completion timestamps earlier than intended vsync and remain
excluded as incomplete. The corrected sample is 6 frames, p95/max 3771.164976ms and median
2065.524292ms, with all 6 above 16.7ms. Flagged and duplicate exclusions are zero. Raw platform
counters remain separately reported as 8 total / 6 janky / 75%, not rewritten to match the filtered
sample. All raw files and the original 7-frame report remain retained.

These are failing/inconclusive debug-AVD smoke observations, not physical-device acceptance. Main
work, RenderThread queue and post-swap completion all show delays; synchronous preference writes
are not established as the sole cause. Host GPU acceleration/WHPX are active; host RAM/CPU are not
exhausted. Historical guest swap alone does not prove current pressure. The next discriminating
step is a bounded system trace/current counter delta, not ungrounded gameplay or balance changes.

## Audited process death and real claim

The natural no-command first-stage defeat above was reached on build 08. Build 09 changes the
terminal action layout and its test geometry; its domain and persistence implementation are the
same as build 08. The following build-09 checks therefore continue from the preserved real run,
not a newly injected fixture. These artifacts alone do not independently trace the entire preceding
gameplay path. They are not evidence of exact reference-package mechanics or visual parity.

Independent audit: `build/reports/process-death-terminal-independent-audit-20260923.md`. Retained
directories are `mysd-process-death-host-20260923-terminal-unclaimed-02`,
`mysd-real-claim-20260923-01`, and `mysd-process-death-host-20260923-terminal-claimed-01` under
`build/reports/`. The additional paused-built directory is
`mysd-process-death-host-20260923-paused-built-01`. All three process-death metadata files identify
the API35 AVD and installed/expected build-09 APK hash
`855d707900844948b5df2e5dc39b5db57befa0aea90cb42971accfe1b5fdd723`.

| Scenario | Process evidence | Supplemented result | Original automatic failure |
|---|---|---|---|
| Terminal unclaimed | PID 15287; HOME/background; am kill; pidof status 1 and old /proc absent; later PID 15623 | Exact run restored; claim still available; credits and ledger unchanged | Relaunch host exit 255, no Status: ok |
| Terminal claimed | PID 15623; HOME/background; am kill; pidof status 1 and old /proc absent; later PID 15830 | Exact run restored; reward remains claimed; no second grant | Relaunch Status: timeout |
| Paused built battle | PID 15830; HOME/background; am kill; pidof status 1 and old /proc absent; later PID 16479 | Exact paused COMBAT run and built tower restored; only legitimate energy +1 | Relaunch host exit 255, no Status: ok |

All three `failed.json` files remain failed and unchanged. Manual `after-observed-pid.txt`,
`after-observed.xml`, `after-observed.png`, and `after-observed-comparison.json` provide the later
observations; they do not repair the automatic launch handshake or create automatic PASS results.
Their command/timestamp provenance is narrower than the driver's `host-calls.json`. In the logged
background snapshots, Launcher is resumed and MySD is not visible (unclaimed STOPPING, claimed and
paused-built STOPPED); the later successful pidof+/proc probes are the process-disappearance evidence.

For the terminal/claim sequence, local independent decoding rechecked all six XML files and all 33
profile wire fields. All three paired run documents are ordinal-string equal; every run SHA-256 is
`eebfbdd9834b471772d95a5eeb674dc0b76787412e50b1b8fea4d3edd2cd0dba`. Run ID is
`run-746785d5eb9fb2f3-1`, stage `stage-ember-path`, tick 540, phase/terminal DEFEAT, base health 0.
Unclaimed restore changes only `energyAnchor`/`energyObserved` by +99 seconds; claimed restore
changes only those clocks by +85 seconds. Energy stays 10/10, so these full-energy clock updates
produce no resource grant or ledger/revision event. All other profile fields remain exact.

The real claim pair changes credits 500 -> 509 and adds exactly one marker for the current run,
one ledger row (ID 6, BATTLE_REWARD, current run source, soft delta +9 and all other deltas zero),
revision 712 -> 713, event cursor 12 -> 13 and ledger cursor 6 -> 7. Run bytes do not change. The
claimed restoration retains credits 509 and that one marker/row. The byte-identical XML chain is
also intact: unclaimed after-observed equals claim before (SHA-256
`d17d658404ba001460ee18ee45c5b38871be533def508a9b7235c6326669719a`), and claim after equals
claimed-driver before (`0377ab14b54967fdd4ac1db644d71ba0bf4c5732df67a717d804872be811f81e`).

Visual review confirms defeat at wave 3/10, the +9-credit offer and available claim before the
transaction; afterward and after claimed restoration it shows the claimed status, no claim button,
and enabled retry/map actions. This proves one reward transaction and no repeat grant on restore,
not a deliberately repeated domain-claim invocation. It does not establish interruption-at-write
atomicity, arbitrary migrations, physical-device behavior or timing reliability.

The paused-built scenario was reached through ordinary UI play, with no save injection. Its two
XML files independently compare run documents equal with SHA-256
`7a25bdf62af1e55dfa5515b5cefab7820aeb9a85bb6078b444dc43fbf19d5dba`: run
`run-449cc26fa53192ff-1`, stage-ember-path, COMBAT, paused=1, terminal empty, tick 447. Slot-1-1
retains tower-ember-needle level 1, position 210 and cooldown 2; all other slots and battle state are
unchanged. Both PNGs show the paused battle, matching wave/base/plasma HUD and actions.

Only the seven allowed energy-refresh profile fields change: energy 8 -> 9; anchor 1790179879 ->
1790180179 (+300 seconds); observed clock 1790179879 -> 1790180355 (+476 seconds); one appended
ledger row ID 8, ENERGY_REFRESH/source clock, energy +1 and all other deltas zero; revision
1168 -> 1169, event cursor 21 -> 22 and ledger cursor 8 -> 9. This is exactly one 300s recharge
interval with 176 seconds retained toward the next one. Credits stay 509, and the prior claimed
run marker and sole reward row remain unchanged. The strict retained restore comparison passes;
the launch exit-255 automatic result is still failed. Neither a continuously advancing unpaused
battle nor an interruption during a write was tested here. The current checkpoint above supersedes
the earlier pending gate-09 execution status; the goal is not complete.

## Outstanding goal evidence

The official package remains runtime-blocked on the available reference AVD combinations. Original
content/mechanics are explicit MySD decisions. Complete new-package behavioral/visual parity remains
unproven regardless of internal verification results. No test result may silently narrow that goal.
