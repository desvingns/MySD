# Android Appendix

## Baseline

- Android only; minSdk 26, target/compile SDK 36.
- Java/Kotlin target 17.
- Pixel 9 is the reference-fit device profile.
- One Activity may host the first shell; lifecycle state is delegated to game/session APIs.
- Fixed simulation policy: 20 Hz. Render target: 60 FPS.

## Input and rendering

- Touch input becomes commands; no view mutates authoritative state.
- Rendering consumes the latest immutable snapshot/frame.
- System Back behavior is specified per accepted node after crawl.
- Touch targets default to at least 48 dp. Any smaller must-match reference control becomes an
  accessibility deviation rather than lowering the MySD target.

## Lifecycle

- Pause/background stops presentation pacing and persists at an arbitrary tick.
- Recreate/process death restores pending commands, next command ID, run/profile versions, and
  deterministic continuity.
- Run and profile writes are atomic from the application perspective.

## Fit

- Capture on the same Pixel 9 profile, resolution, density, locale, and font scale as reference.
- Structural score per accepted state: at least 90%.
- Key bounds tolerance: ±4 dp.
- Critical transition timing tolerance: ±15% of accepted observations.
- Reference-IP masked regions are excluded from pixel verdicts but not from layout bounds.
