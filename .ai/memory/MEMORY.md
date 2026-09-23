# MySD Project Memory

- The product is an original-IP Android TD informed by observable reference behavior.
- The 2026-09-16 full-product decision supersedes old deferred Shop/Tech/original-mechanic scope.
  The user requires one complete engine+game implementation batch before executing checks.
  Earlier accepted-contour test results must not be reused as full-product verification.
- Raw reference evidence is local-only under `.reference-local/`.
- Observer and author are separate roles; Gate 1 precedes gameplay FRs and engine demand.
- State identity combines structural, masked-visual, and semantic signatures; volatile values are
  observations.
- Mechanic claims retain contradictions and need controlled samples before promotion.
- Cross-project lineage is `EV -> OB/ED -> CL -> INV -> FR -> US -> AC -> GAP/ENG -> TEST -> FIT`.
- MyEngine is consumed through a composite build pinned by exact SHA.
- ENG-036 (`1174d21`) was merged into MyEngine main on 2026-09-24; MySD pins merge commit
  `74454178af229a61185a622aa68a6bd5d279ac5b`. main briefly carried a second, independent ENG-036
  variant: check the engine main for parallel work on the same card before accepting a feature.
  Generic sessions stay Android-free. MySD uses current-tick command draining so repeated UI input
  cannot grant passive time, movement, income, or cooldown progress.
- A complete diagnostic Android result must come from the runner's terminal outcome, not its host
  ADB shell exit. In the 2026-09-23 API35 isolation, host transport loss invalidated UiAutomation;
  device-owned detached instrumentation passed the same legacy/recovery/font cases. This does not
  establish the cause of separate API37.1 boot or black-window failures.
- Actual reward process-restoration checks must compare profile claim IDs, balances and ledger as
  well as the run document: claiming changes the profile, not the frozen terminal run. Wall-clock
  energy anchors may legitimately change during launch and must be distinguished from reward grants.
- API35 process-death checks on 2026-09-23 proved PID absence and exact run restoration for paused
  built, unclaimed terminal and claimed terminal states. `am start -W` returned exit 255 or timeout
  although a new process subsequently rendered the correct state. Retain the failed automatic
  result and attach separately audited manual observations; never relabel the driver as passing.
- Compose foundation 1.11.3 reconstructs some semantics TextLayoutResults at max constraints but
  keeps the original shrink-wrap size. Diagnose actual node dimensions, text intrinsics and glyph
  bounds before treating raw didOverflowWidth as visible clipping. Keep height/full-text/whole-word
  assertions and inspect screenshots; reconstruction alone does not prove the rendered appearance.
- Compose root screenshots omit separate native Dialog windows. A LocalDensity test wrapper also
  does not prove system-window compact/font-scale coverage. Use full-display captures and disclose
  the real window profile instead of labeling it with the Activity fixture's simulated density.
- Android frame ring buffers can contain frames whose IntendedVsync precedes Stats since. Exclude
  those rows before deduplication, using the per-window boundary or process fallback, and retain
  impossible timestamp rows as rejected diagnostic evidence. Short debug-AVD captures do not prove
  release-device performance or the cause of observed stalls.
- An unstable shared Windows ADB5037 endpoint can be isolated without touching USB or other
  emulators: task-local ADB_USB=0, ADB_MDNS=0, ADB_MDNS_AUTO_CONNECT=0, ADB_EMU=0; explicit owned
  emulator console/transport5680/5681 and ANDROID_ADB_SERVER_PORT=5039. Bind a private server with
  `-L tcp:5039 ... server nodaemon` and verify its actual127.0.0.1 listener/PID. Numeric127.0.0.1
  in a listen specification is rejected by adb37, while `-H127.0.0.1 start-server` refuses remote
  autospawn; explicit-H is correct for clients of an already running server. Do not use a dummy
  mDNS allowlist as an off switch. Save data/snapshot first and distinguish coldboot confounding.
- The build10 API35/private-endpoint terminal-unclaimed schema4 OS-death check completed
  automatically: PID4246 absence, PID4438 relaunch, exactrun and invariant claim/ledger/hero state.
  It does not retroactively change the earlier build09 failed-driver/manual-supplement records.
- A rememberSaveable(input) MutableState holder can be replaced when a route category changes.
  A separate remember(callback) dispatcher then retains the obsolete holder if the callback is
  stable. The build10 ordinary Setup->Battle path demonstrated a missing Exit Dialog, while a
  fresh launch directly into Battle worked. Keep the holder an explicit dispatch remember key;
  regress transitions with one stable callback, not only fixtures mounted directly in the state.
- The build10 full Android run was aborted by the system watchdog, not completed with88 passes.
  DropBox showed the WindowManager global lock held while an OS task snapshot synchronously
  waited on SurfaceFlinger captureLayersSync. Final logcat stacks were already idle after recovery;
  use the earlier captured ANR stacks before attributing the lock owner or underlying cause.
  This establishes the proximal wait, not a proven host GPU, memory, persistence or ADB cause.
- An emulator launched with Start-Process from an agent shell dies with that shell: the launcher's
  console/job handler logs `Wait for emulator (pid ...) 20 seconds to shutdown gracefully before
  kill` and QEMU exits (build11 full run, 2026-09-23 19:39Z, after the Codex turn was interrupted).
  Create long-lived emulators via Win32_Process.Create (cmd wrapper sets the private ADB env), and
  keep device runs setsid/nohup device-owned so a host interruption cannot abort them.
- Binder shell tools (`settings`, `wm`, i.e. `cmd`) pass their stdout/stderr fds to system_server;
  a /sdcard (FUSE) fd fails with `Failed transaction (2147483646)`. Capture through pipes. Likewise
  `run-as pkg cat f > /sdcard/x` writes 0 bytes; use `run-as pkg cat f | cat > /sdcard/x`.
- A Compose Dialog window provides its own LocalDensity, so Activity-local font overrides never
  reach Dialog content. Test Dialog content extracted from its window at the window size and font
  scale, and inspect at a real system font scale; build11 split "Незавершённый" at font 2/320 dp.
- Battle UI selection state must be keyed to the run and the input-enabled phase: an unkeyed
  rememberSaveable slot choice re-opened the build Dialog after a wave-end choice and in a retried
  run (build12, fixed build13). The build Dialog does not pause the real-time battle.
- On the SwiftShader API35 AVD even system Settings misses 16.7 ms (p95 ~118 ms); frame gates there
  are diagnostic only and need a physical-device measurement for acceptance.
- The brain inbox candidate is intentionally deferred until Gate 2.
- MP workflow preference: `--phase` and `--feature --next` are implicitly SPEC-approved; skip the separate SPEC confirmation gate while preserving later verification and push gates.
- PHASE_04 fit closure (2026-08-30): all six presentation-only divergence SPECs were implemented and
  verified on Pixel_5(AVD)-14; connected evidence reached 16/16 and the fit ledger has zero unexplained
  divergences. Numeric pixel scoring remains intentionally unclaimed when reference profiles,
  preserved-unusable captures, or ImageMagick prevent an honest comparison.
