# TASK-03.13 Visual QA Record

Date: 2026-08-29  
Scope: `ST-0006/ROUTE-CAMPAIGN`, `ST-0007/ROUTE-TROOPS`, `ST-0008/OVERLAY-SETTINGS`, `ST-0011/ROUTE-ARENA-LOCAL`, and `ST-0012/OVERLAY-RESUME` only.  
Registry source: `spec/fit/registry.csv`.

## Capture evidence

- Device gate: `emulator-5554`, state `device`, `sdk_gphone64_x86_64`, Android 14 / API 34,
  physical size 1080x2340, density 440 dpi.
- Reference profile: Pixel 9, 1080x2424, density 420 dpi. The profile mismatch is recorded;
  pixel similarity is not treated as comparable evidence.
- APK: `app/build/outputs/apk/debug/app-debug.apk`, versionCode 13 / versionName 0.1.12,
  SHA-256 `e26d170aba3404d3f653a97b92cd6ed2d6ab18b3276a8096713c01f1a1b3bb19`.
- The shipped UI was driven through the authoritative campaign entry, roster, settings, and
  local Arena actions. No debug-only navigation, production service, account, network match,
  external exit, or deferred mechanic was used.
- Retained device screenshot/UI-dump pairs under `build/fit/built/`:
  - `ST-0006.png` `86e991327d65ca35b63c89c0bb773b436bb37c4709c761adef80eee8bbbef03d`
  - `ST-0006.xml` `23ec4219ea16a86799e74b4d54245bf91351aafc59555df7c28f8a90f1f0ea77`
  - `ST-0007.png` `0f4dad3fb78d767fdf5ffa7d2af6595fc166b250ecf2f642bdf33cdbb18680452`
  - `ST-0007.xml` `178e21c8e7cebc5a25807adae9b127edd412e5b4c0d737e0422a5832a0d01eef`
  - `ST-0008.png` `92133d1acba3b0b31fcced1f27515f80070da151b553e2f9a669538477cc8a78`
  - `ST-0008.xml` `b9189fdbb4732fe8f93c34d9ddd4e5f989a913566708af67658ccba21364a302`
  - `ST-0011.png` `4f19fb02853438aec5872ccc9d2de5d121522bdb9ca1abb66ebabe3ef4649396`
  - `ST-0011.xml` `7ded5dfbc1d58553271127371a2e2902bed383f79f75d573f0fd12bdcc6c7212`
- `ST-0012` built screenshot/UI dump is not present. The existing shipped Activity reads an
  encoded run save but exposes no user path that creates one in this contour. No substitute
  image, UI dump, raw reference asset, or fabricated score was created.
- The deterministic ImageMagick pixel tool is unavailable (`convert` is the Windows system
  utility, not ImageMagick). Pixel scores are therefore unavailable for all five states.

## Results

| State | Structural result | Visual result | Evidence-backed divergence / blocker |
|---|---|---|---|
| `ST-0006/ROUTE-CAMPAIGN` | Pass for the shipped campaign contour. The dump exposes campaign title, roster and Arena navigation, the accepted local level, and level-setup action. | Blocked/uncheckable. Reference PNGs `EV-0196` and `EV-0200` are preserved evidence, but no objective pixel score is available and profiles differ. | MySD exposes the accepted implemented local route rather than the reference's broader root-tab/canvas composition; Shop/Tech affordances are not present in this shipped contour. Record `FIT-03.13-001`; no new mechanics or routes added. |
| `ST-0007/ROUTE-TROOPS` | Pass. The dump exposes the roster surface, one visible troop slot, deferred upgrade affordance, settings entry, and close action. | Blocked/uncheckable for objective pixels for the same tool/profile reasons. | MySD is an original text/Material surface rather than the reference illustrated roster composition. Deferred upgrade behavior remains unchanged. Record `FIT-03.13-002`. |
| `ST-0008/OVERLAY-SETTINGS` | Pass. The dump exposes the settings overlay, two local toggle controls, close, and confirm actions. | Blocked/uncheckable for objective pixels for the same tool/profile reasons. | MySD preserves the local settings overlay contour while toggle effects remain deferred; creative/layout divergence is recorded as `FIT-03.13-003`. |
| `ST-0011/ROUTE-ARENA-LOCAL` | Pass. The dump exposes the local Arena service-shaped state and close action. Network/account/match semantics were not run. | Blocked/uncheckable: registry reference state is `preserved_unusable` with no valid visual evidence. | Local offline/service-shaped Arena is intentionally distinct from any network Arena behavior; `DEV-008` remains in force. Record `FIT-03.13-004`. |
| `ST-0012/OVERLAY-RESUME` | Blocked. No built capture pair is present, so the structural acceptance row cannot be evidenced on device in this pass. | Blocked/uncheckable: no built image and no objective pixel tool; reference `EV-0189` is not compared without a built pair. | The existing Android route/session code and JVM evidence cover the resume prompt contour, but this device capture remains an explicit evidence blocker `FIT-03.13-005`. No persistence or UI behavior was invented. |

## Scope outcome

- Four of five requested states have fresh device screenshot/UI-dump pairs with exact APK and
  file hashes. Their structural results are recorded from the captured element trees.
- `ST-0012` is explicitly blocked because its required built pair is absent; this record does
  not claim capture, structural pass, visual pass, or a pixel score for it.
- Visual comparison is blocked/uncheckable for every scoped state: three reference states lack
  objective pixel tooling/profile comparability, Arena has `preserved_unusable` reference
  evidence, and resume lacks a built pair.
- No production code, gameplay/service semantics, external exit, reference asset, or reference
  UI copy was added. Deferred roster upgrades, settings effects, Arena network/account/match
  semantics, and external exit remain out of scope.
