# MySD

MySD is an Android-first, original-IP tower-defense game built on
[MyEngine](https://github.com/desvingns/MyEngine). The project studies the observable offline
structure and pacing of the public reference package
[`com.gdzsq.crazy_td`](https://play.google.com/store/apps/details?id=com.gdzsq.crazy_td), while
using original names, world-building, art, audio, copy, and balance.

## Current status

**Repository foundation / evidence intake.** The build and engine pin are operational. Game
requirements remain provisional until the Luna crawl passes Gate 1. Production gameplay must not
be inferred from the store listing alone.

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
.\gradlew.bat test :app:assembleDebug
```

For a different checkout location, pass `-Pmyengine.path=<path>` or set `MYENGINE_PATH`.

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
