# MySD

MySD (working title **Emberwatch**) is an Android-only, original-IP tower-defense game built on
[MyEngine](https://github.com/desvingns/MyEngine). The project studies the observable offline
structure and pacing of the public reference package
[`com.yuegame.defender`](https://play.google.com/store/apps/details?id=com.yuegame.defender), while
using original names, world-building, art, audio, copy, and balance.

## Current status

**Full-product batch implemented; final verification and stabilization in progress.** The integrated
batch includes six ten-wave stages, four towers, three allies, two hero abilities, twelve
enhancements, roster/technology progression, rewards, energy, Shop, and an offline Arena calculation.
Ads and purchases are explicit local placeholders, with no SDK, network, account, or payment.

The user requested the complete engine and game implementation before tests. New work must not be
called verified until the final gate in `docs/implementation_plan/FINAL_FULL_PRODUCT_VERIFICATION.md`
passes. The older accepted `com.gdzsq.crazy_td` evidence corpus does not prove parity with the current
reference package; inaccessible reference behavior remains explicitly unverified.

Current evidence is in `docs/implementation_plan/FINAL_FULL_PRODUCT_RESULTS.md`. Full build-09
Android verification completed 78 passed / 2 failed / 80: case 59 failed in Activity teardown and
case 79 timed out during screenshot capture. Those failures remain retained. Frozen build-10,
including hero-selection/profile-v4 and per-batch autosave corrections, passes 294 JVM tests in
33 suites with zero failures/errors/skips, debug/benchmark lint with zero errors, and all three APK
assemblies. Build-10 affected Android verification passes 27/27 and retains 237 fresh PNGs on the
private ADB 5039 endpoint. The full 88-case run is **incomplete**: 72 passed, case 73 started without
a verdict, and 15 did not start before `INSTRUMENTATION_ABORTED: System has crashed.` Watchdog
terminated system_server after a window/display stall; its underlying graphics cause remains
unestablished. This is neither a complete PASS nor a still-running suite.

Actual update migration from profile 3 to 4 passes, and one real schema-4 unclaimed-terminal
process-death check passes automatically with exact run restoration. The earlier three schema-3
manual-supplemented scenarios and their original driver failures remain separate. Visual acceptance
is **not passed**. The collected build-11 UI batch now source-fixes the measured Setup, campaign,
HUD, inset and Dialog-action issues, a Setup-to-Battle exit-dispatch defect, and covered HUD input.
Its 141-file freeze records **94 Android / 294 JVM cases**. Build-11 passes the fresh 294-test JVM
run, both lint variants and all three APK assemblies; an independent host archive/signature/
manifest audit passes. Build-11 affected Android verification passes **33/33**, including the four
adaptive-layout cases and two functional UI regressions, with 237 fresh captured PNGs. Bounded
review of 142 selected PNGs (46 compact / 56 compact-font-2 / 40 native) finds no new demonstrated
layout blocker; this is not acceptance of all images or real system-font-2 Dialog fit. Two font-2
root captures are blank while their full-display alternatives render correctly; those failed
captures remain retained. The 94-case full Android run is in progress, without a final verdict.
An ordinary build-11 launch timeout and SystemUI ANR also remain retained: affected verification
ran only after one documented Wait action restored the unobstructed UI. The earlier schema-4
process-death result is not current-build proof. Full Android, actual system-font Dialog/current-build
restoration and non-debuggable device-frame gates remain open. The controlled engine performance
comparison passes; this is not a release or exact-parity claim.

## Build

The default local layout is:

```text
D:/Pet/MyEngine
D:/Pet/MySD
```

MySD consumes MyEngine through a Gradle composite build. The compatible engine revision is pinned
in `gradle/myengine.lock`; CI checks out exactly that commit.

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:MYENGINE_PATH = "D:\Pet\MyEngine-mysd"
.\gradlew.bat test :app:assembleDebug
```

For a different checkout location, pass `-Pmyengine.path=<path>` or set `MYENGINE_PATH`.

The active integrated work uses `D:/Pet/MyEngine-mysd`, pinned to technically accepted local commit
`1174d21c4e92fddff6316f93bc99384ab6c1c689` (ENG-036). The unrelated sibling `D:/Pet/MyEngine` is not
the accepted checkout for this batch. The new engine commit has not been pushed: remote publication
requires publishing it before the game lock that references it.

## Play

Choose a campaign stage, select at least one tower and one allied unit, then start the battle.
Choose unlocked hero abilities separately in the roster or setup; they consume no unit slots and
may all be deselected. Existing profiles migrate with their previously automatic ability selection.
Build on fixed slots, upgrade towers, deploy allies, and use hero abilities. Supply replenishes
during combat. After waves 2/4/6/8, choose one of three run enhancements. Pause and 1x/2x controls
change pacing without changing the deterministic 20 Hz simulation.

Claim a terminal reward before leaving or retrying. Stage clears unlock later stages and roster
roles; three stars enable sweep. Roster and technology purchases use earned credits. The game saves
the profile and active run locally as one atomic bundle and pauses when backgrounded.

## Reference safety

Raw screenshots, recordings, APKs, UI dumps, and extracted assets belong only in
`.reference-local/`, which is ignored. Commit only sanitized observations, measurements, hashes,
and provenance under `spec/evidence/`. Every tracked creative binary also needs an approved row in
`assets/provenance.csv`. Run `scripts/public-safety.ps1` before every publication.

## Gates

1. Gate 1 accepts the observed state/feature inventory and mandatory deviations.
2. Gate 2 accepts the complete traceable spec bundle and known coverage gaps.
3. Implementation proceeds through headless vertical slice, Android rendering, parity, meta,
   service-shaped fakes, and the final Pixel 9 fit loop.

See `docs/reference/LUNA_CRAWL_RUNBOOK.md` and `docs/implementation/ROADMAP.md`.
