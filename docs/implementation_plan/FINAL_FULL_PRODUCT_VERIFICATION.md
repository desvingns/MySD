# Final full-product verification

Status: executing final verification after all MyEngine, game-domain, Android, persistence, UI,
and test-source work in the integrated batch was completed. Current results and stabilization
findings are recorded in `FINAL_FULL_PRODUCT_RESULTS.md`.

This is the only execution gate for the batch. Do not run any command below during production
implementation.

## Preconditions

- MyEngine `engine-runtime`, sandbox migration, docs, and test sources are complete.
- MySD product catalog, battle, meta progression, services, persistence, facade, Android shell,
  resources, and test sources are complete.
- Legacy first-playable terminal persistence and save/hash compatibility paths remain present.
- No agent is still editing either repository.

## MyEngine gate

From the MyEngine checkout used by the composite build:

```powershell
.\gradlew.bat test
powershell.exe -File scripts\me-content-validate.ps1
powershell.exe -File scripts\me-sim-replay.ps1
powershell.exe -File scripts\me-save-compat.ps1
powershell.exe -File scripts\me-benchmark.ps1
.\gradlew.bat :engine-devtools:installDist
powershell.exe -File scripts\me-runtime-paired-benchmark.ps1 -BaselineRoot '<isolated exact 30f4eb1 archive>' -CandidateRoot 'D:/Pet/MyEngine-mysd' -ReportDirectory '<new retained report directory>'
.\gradlew.bat :android:assembleDebug
```

Confirm separately in the resulting reports/evidence:

- `engine-runtime` start/submit/step/snapshot/save/restore and incompatibility results;
- sandbox raw save v1-v7 restore compatibility and unchanged established replay hashes;
- pending-command and terminal restore continuity;
- Android-free dependency scan for `engine-runtime`;
- runtime/session benchmark delta below 5%, or an explicit accepted explanation.

Use the same-machine warmed comparison described in the engine's
`docs/contracts/runtime-benchmark.md`. Do not infer this gate from one cold timing. Retain failed
reports and explain reruns; run no other build/test/emulator workload during benchmark sampling.
The original separate-process source-launch benchmark remains diagnostic evidence: identical-code
A/A showed that it cannot resolve this gate on the current host. The controlled precompiled paired
runner requires absolute A/A delta at most 5% and A/B regression at most 5% in both scenarios; an
invalid calibration is inconclusive, not a pass. Preserve all samples, artifacts, and source hashes.

Only after this gate passes may ENG-036 be accepted and committed. MySD then updates
`gradle/myengine.lock` to that exact commit.

## MySD static and JVM gate

With `MYENGINE_PATH` pointing to the accepted checkout:

```powershell
powershell.exe -File scripts/public-safety.ps1
powershell.exe -File scripts/validate-spec.ps1
powershell.exe -File scripts/validate-evidence.ps1
.\gradlew.bat test :app:lintDebug :app:assembleDebug
```

Required product evidence:

- exact content counts and invalid catalogs;
- first paid legacy upgrade changes stats and the legacy default has a natural defeat path;
- all product commands preserve input state on rejection;
- ten-wave natural victory and defeat;
- every tower, ally, enemy, boss, hero ability, and enhancement participates in a scenario;
- uninterrupted and save/restored per-tick hashes match;
- run/profile current round trips plus supported legacy migrations and future-version failures;
- ledger conservation, duplicate-claim protection, stage unlocks, energy, sweep, roster/loadout,
  technology prerequisites, Shop, reward track, service stubs, and local Arena determinism;
- no production SDK/backend/network/account/payment path.

`ProductLoadVerificationTest` emits `game/build/reports/product-load.json` from a synthetic
conservative catalog peak (largest whole wave plus the ally cap) with 25% additional mobile
entities. It reports repeated-tick hash agreement and JVM reducer p95 separately from device frames.

## Android/device gate

On the supported Pixel 9 profile after JVM/static success:

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest
```

The connected suite must cover every product route, battle actions and guards, enhancement freeze,
both terminals and idempotent rewards, service-stub labeling, lifecycle recreation/process restore,
legacy compatibility surfaces, accessibility semantics, 48 dp targets, and large-font reflow.

Then capture the complete fit registry at compact and Pixel 9 profiles and record:

- structural score at least 90% for comparable accepted states;
- key bounds within 4 dp where reference measurement exists;
- Pixel 9 p95 frame time at most 16.7 ms and jank below 5%;
- deterministic load at release content peak concurrent entities plus 25%.

`ProductVisualCaptureTest` writes a UI-fixture tour to the app-private device directory
`files/product-visual/{native,compact,compact-font-2}`. Pull the PNGs and CSV locally for visual
inspection; these are rendering fixtures, not proof of gameplay reachability or reference parity.
Capture gameplay/device frame metrics separately from the actual application.

For actual app frame evidence, save raw `adb shell dumpsys gfxinfo dev.mysd.android framestats`
windows after a warmup, separately for 1x and 2x battles. Do not capture screenshots or dump UI
hierarchies during the measured window. Parse with `scripts/analyze-gfxinfo.ps1 -InputPaths <files>`;
it uses named columns, excludes flagged/incomplete frames, deduplicates overlapping ring-buffer
entries, and keeps platform jank counters separate from the fraction above 16.7 ms. Report the
sample count and debug/AVD limitation; a low frame-work p95 is not proof of 60 FPS refresh/motion.

Debug captures are diagnostic only, not the release-performance gate. The separate `benchmark`
build type inherits release settings, remains non-debuggable and enables shell profiling through
its own manifest. It uses the local debug signature and unchanged package ID solely so an isolated
development AVD can preserve its real save with `install -r`. Never publish this diagnostic APK.
Build it with `:app:assembleBenchmark` only when no device run is active. Record the APK hash,
merged debuggable/profileable flags, signing identity and device compilation state. For the known
full-AOT diagnostic state, use `cmd package compile -m speed -f dev.mysd.android`, then restart
only this app and warm the actual path before capturing separate 1x/2x windows. This is not a
production speed-profile/Baseline Profile claim. The current release configuration does not enable
R8 shrinking; benchmark inherits that fact rather than silently changing optimization settings.
Non-debuggable builds do not support run-as private-save capture: preserve the debug save before
replacement, use ordinary UI phase evidence outside measured windows, and do not label absent
authoritative paired-save data as verified. Restore the debug APK afterward without clearing data.
Reference: https://developer.android.com/topic/performance/measuring-performance.

Verify actual process death externally after instrumentation: prepare a paused built battle in
the real UI, send HOME, record the persisted run document and old PID, use `am kill`, verify PID
absence, relaunch, verify a different PID and byte-identical restored run. Repeat for unclaimed and
claimed terminal rewards. Reward claims do not change the run document: also compare the profile's
claim identities, currency/reward-point balances, and ledger coordinates, and verify claim availability
before/after the first grant and rejection of a repeated grant. Do not compare the entire profile
byte-for-byte because launch legitimately refreshes wall-clock energy anchors.
Do not substitute ActivityScenario recreation for process-death evidence,
and do not silently replace an ineffective `am kill` with a different termination mechanism.

## Stabilization and publication

Fix all findings as one integrated stabilization pass, then rerun the affected narrow check and the
complete final gate. Update both handoffs, MySD progress/memory, the exact engine lock, and final
evidence. Do not push either repository without explicit user confirmation.
