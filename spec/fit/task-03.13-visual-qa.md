# TASK-03.13 Visual QA Record

Date: 2026-08-29  
Scope: `ST-0006/ROUTE-CAMPAIGN`, `ST-0007/ROUTE-TROOPS`, `ST-0008/OVERLAY-SETTINGS`, `ST-0011/ROUTE-ARENA-LOCAL`, and `ST-0012/OVERLAY-RESUME` only.  
Registry source: `spec/fit/registry.csv`.

## Capture evidence

- Device gate: `emulator-5554`, state `device`, `sdk_gphone64_x86_64`, Android 14 / API 34,
  physical size 1080x2340, density 440 dpi.
- Reference profile: Pixel 9, 1080x2424, density 420 dpi. The profile mismatch is recorded;
  pixel similarity is not treated as comparable evidence.
- APK: `app/build/outputs/apk/debug/app-debug.apk`, versionCode 16 / versionName 0.1.15,
  SHA-256 `4b80f687c0e4427ff68db7e8f3661ee7e6a159c0be184a855f859725adc68803`.
- The shipped UI was driven through the authoritative campaign entry, roster, settings, and
  local Arena actions. No debug-only navigation, production service, account, network match,
  external exit, or deferred mechanic was used.
- Retained device screenshot/UI-dump pairs under `build/fit/built/`:
  - `ST-0006.png` `86e991327d65ca35b63c89c0bb773b436bb37c4709c761adef80eee8bbbef03d`
  - `ST-0006.xml` `23ec4219ea16a86799e74b4d54245bf91351aafc59555df7c28f8a90f1f0ea77`
  - `ST-0007.png` `0f4dad3fb78d767fdf5ffa7d2af6595fc166b250ec2f642bdf33cdbb18680452`
  - `ST-0007.xml` `178e21c8e7cebc5a25807adae9b127edd412e5b4c0d737e0422a5832a0d01eef`
  - `ST-0008.png` `3ce3652058dd2825e654fc09f044b937e6df19e26d9c04ade6b6bd9870f2def5`
  - `ST-0008.xml` `b9189fdbb4732fe8f93c34d9ddd4e5f989a913566708af67658ccba21364a302`
- `ST-0011.png` `4f19fb02853438aec5872ccc9d2de5d121522bdb9ca1abb66ebabe3ef4649396`
- `ST-0011.xml` `7ded5dfbc1d58553271127371a2e2902bed383f79f75d573f0fd12bdcc6c7212`
- `ST-0012.png` `790b1aee173eab54ff652a3330cb085215aea54d5183b1f523afdadf699abb29`
- `ST-0012.xml` `2aee6cb31b5fc7e6b352b159f9b3396d8c77a9377eb59a7494e38675d035cfc1`
- ST-0012 was captured by the instrumented-only
  `ResumeContentUiTest.st0012_loadsSeededRunThroughActivity_andCapturesEvidence`: it writes a
  valid active `RunSave` through the canonical storage keys, launches `MainActivity`, enters the
  campaign through the shipped action, verifies both resume actions, and captures the pair. The
  fixture is test-only and restores the prior encoded-save value (or removes only the fixture key)
  in `finally`; no substitute image, raw reference
  asset, or fabricated score was created.
- The deterministic ImageMagick pixel tool is unavailable (`convert` is the Windows system
  utility, not ImageMagick). Pixel scores are therefore unavailable for all five states.

## Results

| State | Structural result | Visual result | Evidence-backed divergence / blocker |
|---|---|---|---|
| `ST-0006/ROUTE-CAMPAIGN` | Pass for the shipped campaign contour. The dump exposes campaign title, roster and Arena navigation, the accepted local level, and level-setup action. | Deferred/uncheckable — not a pass: reference PNGs `EV-0196` and `EV-0200` use the Pixel 9 1080x2424@420 profile, while the built pair uses emulator-5554 1080x2340@440; the objective ImageMagick pixel tool is unavailable, so no score is claimed. | MySD exposes the accepted implemented local route rather than the reference's broader root-tab/canvas composition; Shop/Tech affordances are not present in this shipped contour. Record `FIT-03.13-001`; no new mechanics or routes added. |
| `ST-0007/ROUTE-TROOPS` | Pass. The dump exposes the roster surface, one visible troop slot, deferred upgrade affordance, settings entry, and close action. | Deferred/uncheckable — not a pass: device/reference profiles differ and the objective ImageMagick pixel tool is unavailable; no visual score is claimed. | MySD is an original text/Material surface rather than the reference illustrated roster composition. Deferred upgrade behavior remains unchanged. Record `FIT-03.13-002`. |
| `ST-0008/OVERLAY-SETTINGS` | Pass. The dump exposes the settings overlay, two local toggle controls, close, and confirm actions. | Deferred/uncheckable — not a pass: device/reference profiles differ and the objective ImageMagick pixel tool is unavailable; no visual score is claimed. | MySD preserves the local settings overlay contour while toggle effects remain deferred; creative/layout divergence is recorded as `FIT-03.13-003`. |
| `ST-0011/ROUTE-ARENA-LOCAL` | Pass. The dump exposes the local Arena service-shaped state and close action. Network/account/match semantics were not run. | Deferred/uncheckable — not a pass: registry reference state is `preserved_unusable`, and the objective ImageMagick pixel tool/profile comparison is unavailable; no visual score is claimed. | Local offline/service-shaped Arena is intentionally distinct from any network Arena behavior; `DEV-008` remains in force. Record `FIT-03.13-004`. |
| `ST-0012/OVERLAY-RESUME` | Pass. The built pair exposes the shipped campaign-entry resume overlay, its local-run body, and both cancel/continue actions after the fixture-backed Activity/storage path. | Deferred/uncheckable — not a pass: the built pair is now present, but device/reference profiles differ and the objective ImageMagick pixel tool is unavailable; no visual score is claimed. | The instrumented fixture provides evidence for the existing Android route/session contour without adding persistence or UI behavior. Record `FIT-03.13-005`; no new mechanics were added. |

## Scope outcome

- All five requested states have fresh device screenshot/UI-dump pairs captured with the exact
  version 16/0.1.15 APK and recorded file hashes. Their structural results are recorded from
  the captured element trees.
- Visual comparison remains deferred/uncheckable for every scoped state, explicitly not passed:
  the device/reference profiles are incompatible, the objective pixel tool is unavailable, and
  Arena's reference evidence is `preserved_unusable`; no pixel score is invented.
- No production code, gameplay/service semantics, external exit, reference asset, or reference
  UI copy was added. Deferred roster upgrades, settings effects, Arena network/account/match
  semantics, and external exit remain out of scope.
