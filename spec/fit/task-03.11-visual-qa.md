# TASK-03.11 Visual QA Record

Date: 2026-08-29
Scope: `ST-0001/ROUTE-LAUNCH`, `ST-0002/BATTLE-SETUP`, `ST-0003/BATTLE-ACTIVE` only.
Registry source: `spec/fit/registry.csv`.

## Capture evidence

- Device: `emulator-5554`, `sdk_gphone64_x86_64`, Android 14 / API 34, density 440 dpi,
  reported physical size 1080x2340.
- Reference capture profile: Pixel 9, 1080x2424, density 420 dpi. The profile mismatch is
  recorded; pixel similarity is not treated as comparable evidence.
- APK: `app/build/outputs/apk/debug/app-debug.apk`, SHA-256
  `3a1ed37d9cc25f4db87408eebf0573b6d6b7662f7b6e9e996ad3616cdc0de1eb`.
- Device captures and UI dumps are retained locally under `build/fit/built/`:
  `ST-0001`, `ST-0002`, `ST-0002-ready`, and `ST-0003`.
- SHA-256 values for the retained device captures:
  - `ST-0001.png`: `428fe70b278945d7e9d6befe2dd68ff519a0039913ae9c64cf18f0306025f54e`
  - `ST-0001.xml`: `cd7119e4a1b8e6285c19b45b90900b4c74949ddccf33543806f95d8f062a1296`
  - `ST-0002.png`: `bc9a59c1136e215507114a9451ca7ba566e17b80c0b6717c6bb50fbc2ca59307`
  - `ST-0002.xml`: `5cdefe03b7b923e247a95c2e1a4d1d44465933d90d23be1f847315b2c61518bb`
  - `ST-0002-ready.png`: `b1d6d033de1bc5073b050ae8f308e9985d80247cbde89b28f0ab2ac6dea82969`
  - `ST-0002-ready.xml`: `0b337b795b965335c6a16503d6e95010e6d082e3ec3c172ad8e28753ee01855e`
  - `ST-0003.png`: `9683c2d8b5fa0076477b0363bb539006c771bedcbc70347326eb3fdbd8109997`
  - `ST-0003.xml`: `713cb042350a1cdc9b3cdb6351921a8a0114649722121f6a6cf235e401d355a6`
- The deterministic pixel tool was unavailable because ImageMagick is not installed. No pixel
  score or pixel-pass claim is made.

## Results

| State | Structural result | Visual result | Evidence-backed divergence / exception |
|---|---|---|---|
| `ST-0001/ROUTE-LAUNCH` | Pass. The device UI dump exposes one clickable primary action, matching the graph's `primary_start` affordance. | Manual comparison only; no pixel score. | The reference is a full-screen illustrated launch composition with a bottom action region; MySD is a centered Material 3 text surface with a centered action. Original identity, art, palette, and copy remain covered by `DEV-001`, `DEV-002`, and `DEV-004`; the remaining composition/placement difference is recorded as `FIT-03.11-001`. |
| `ST-0002/BATTLE-SETUP` | Pass by the captured setup contour. The initial capture exposes choices A/B/C plus continue; the ready capture exposes choices A/B/C plus start. Their union covers the accepted graph affordances without adding state. | Blocked. `EV-0003`, `EV-0017`, and `EV-0115` are preserved-unusable legacy PNGs, so no visual claim is made for this state. | The reference visual comparison remains `FIT-03.11-002` pending a valid reference anchor. No reference asset or reference copy was added. |
| `ST-0003/BATTLE-ACTIVE` | Pass. The device UI dump exposes the four accepted action roles: speed, pause/resume, build, and enhancement. Base and enemy visibility are represented by the immutable snapshot's existing semantic surface. | Manual comparison only; no pixel score. | The reference is a full-screen battle canvas with battlefield, HUD, base/enemy composition, and edge controls; MySD is a centered text-and-button surface. Original creative deviation is covered by `DEV-001`, `DEV-002`, and `DEV-004`, while the missing battlefield composition is recorded as `FIT-03.11-003`. No mechanic or authoritative UI state was added. |

## Scope outcome

- Route reachability and accepted affordance coverage were verified on the connected device.
- `ST-0001` and `ST-0003` have valid reference screenshots (`EV-0233` and `EV-0243`) but are
  not pixel-scored because of the device-profile mismatch and missing ImageMagick tool.
- `ST-0002` remains visually unscorable until a valid reference screenshot is available.
- No production screen, mechanic, authoritative state, raw reference asset, or reference UI copy
  was added for this QA task.
