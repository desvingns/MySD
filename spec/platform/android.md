# Android Appendix

## Baseline

- Android only; minSdk 26, target/compile SDK 36.
- Java/Kotlin target 17.
- Pixel 9 is the reference-fit device profile.
- One Activity hosts the full product shell; lifecycle state is delegated to the Android-free
  `MySdAppSession` facade.
- Fixed simulation policy: 20 Hz. Render target: 60 FPS.

## Input and rendering

- Touch input becomes commands; no view mutates authoritative state.
- Rendering consumes the latest immutable snapshot/frame.
- Compose imports product snapshot/intent types only and never imports MyEngine runtime APIs.
- System Back behavior is specified per accepted node after crawl.
- Touch targets default to at least 48 dp. Any smaller must-match reference control becomes an
  accessibility deviation rather than lowering the MySD target.

## Lifecycle

- Pause/background stops presentation pacing and persists at an arbitrary tick.
- Recreate/process death restores pending commands, next command ID, run/profile versions, and
  deterministic continuity.
- Run and profile writes are atomic from the application perspective.
- One session owner survives configuration recreation; no second ticker may be created.
- The ticker runs only while `RESUMED` and `clock.shouldAdvance` is true. Pause, enhancement choice,
  and terminal states stop it, and background time is never simulated as catch-up.
- Commands, every completed authoritative pulse batch (one tick at 1x or two ticks at 2x), wave
  transitions, enhancement choices, terminal transitions, and economy mutations create checkpoints;
  `onStop` performs a synchronous final flush. Frozen pulses do not create redundant writes. Failed
  persistence keeps the current state visible and retries without advancing the simulation.

## Fit

- Capture on the same Pixel 9 profile, resolution, density, locale, and font scale as reference.
- Structural score per accepted state: at least 90%.
- Key bounds tolerance: ±4 dp.
- Critical transition timing tolerance: ±15% of accepted observations.
- Reference-IP masked regions are excluded from pixel verdicts but not from layout bounds.
- Product routes cover campaign map, setup, battle, enhancement, terminal/reward, roster, Tech,
  Shop, reward track, settings, and local Arena at compact and Pixel 9 profiles.
