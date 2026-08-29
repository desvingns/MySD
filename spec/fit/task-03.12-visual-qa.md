# TASK-03.12 Visual QA Record

Date: 2026-08-29
Scope: `ST-0004/BATTLE-ENHANCEMENT` and `ST-0005/BATTLE-VICTORY` only.
Registry source: `spec/fit/registry.csv`.

## Capture evidence

- Device: `emulator-5554`, `sdk_gphone64_x86_64`, Android 14 / API 34, density 440 dpi,
  reported physical size 1080x2340.
- Reference capture profile: Pixel 9, 1080x2424, density 420 dpi. The profile mismatch is
  recorded; pixel similarity is not treated as comparable evidence.
- APK: `app/build/outputs/apk/debug/app-debug.apk` (versionCode 13 / versionName 0.1.12,
  matching this commit), SHA-256
  `bdc29906c579b97533792a9b748d82393ffe66e3533fe6c6df4db85b388b7dc7`.
- On-device path driven through the authoritative session: enter campaign -> set up level ->
  select starting path -> continue setup -> start battle -> open enhancements (ST-0004) ->
  select an offer (return to battle) -> resolve victory (ST-0005). No debug-only surface or
  fixture shortcut was introduced; both states are reachable through the shipped intent flow.
- Device captures and UI dumps are retained locally under `build/fit/built/`: `ST-0004` and
  `ST-0005`.
- SHA-256 values for the retained device captures:
  - `ST-0004.png`: `e2814cd86d8047e4f252455b47a27b8c70990afb59d218c0df35addc16932ec5`
  - `ST-0004.xml`: `d4218b282342acfdb8f7cc85a7de91362510e29ea5dd06e11344a4a6a5f850ab`
  - `ST-0005.png`: `02d9a6e15d9f38206324e3d260f54bb1cd0b9579e1c7247fa556689ecb616f36`
  - `ST-0005.xml`: `e4125bb9854c1241534adf309aa3940b4cb0cb0ce735efc43e460a75351c59f4`
- The deterministic pixel tool was unavailable because ImageMagick is not installed. No pixel
  score or pixel-pass claim is made. Both reference states are marked `preserved_unusable` with
  empty `visual_evidence_ids` in `spec/evidence/state-graph.v1.json`, so no usable reference
  anchor exists for either state and no visual-parity claim is asserted.

## Results

| State | Structural result | Visual result | Evidence-backed divergence / exception |
|---|---|---|---|
| `ST-0004/BATTLE-ENHANCEMENT` | Pass. The device UI dump exposes the three accepted normalized affordances for `ST-0004` (`enhancement_offer`, `refresh`, `filter`): two selectable offers (`Steady Pulse`, `Ember Ward`), a `Refresh offers (0)` secondary action, and a `Filter: All` label. This covers `AF-0011` (`select_enhancement_offer`) and `AF-0012` (`refresh_enhancement_offers`) without adding state. | Blocked. `ST-0004` is `preserved_unusable` with empty `visual_evidence_ids`; the legacy reference PNGs behind `EV-0041` are not usable anchors, so no visual claim is made. | The reference is a full-screen battle overlay drawn over the live `GameCanvas` (which exposes no measurable child bounds, `G1-BL-010`); MySD is a centered Material 3 text-and-button column. Original identity, art, and copy remain covered by `DEV-001`, `DEV-002`, and `DEV-004`; the residual composition/placement difference is recorded as `FIT-03.12-001`. No reference asset or reference copy was added. |
| `ST-0005/BATTLE-VICTORY` | Pass by the accepted victory contour. The device UI dump exposes the terminal victory surface with the reward panel visible (`reward_panel_visible`), matching the `victory` role. The reference `reward_claim` (`AF-0013`, `BL-REWARD-CLAIM-001`) and `rewarded_multiplier` (`AF-0014`, `request_rewarded_double`, `BL-REWARDED-AD-001`) affordances are intentionally absent: MySD keeps reward claim, doubling, and the rewarded path deferred. | Blocked. `ST-0005` is `preserved_unusable` with empty `visual_evidence_ids`; the legacy reference PNGs behind `EV-0097;EV-0099` are not usable anchors, so no visual claim is made. | The victory panel deliberately shows `Reward details remain deferred to a later local boundary.` rather than a claim/double transaction surface. The deferred rewarded-ad and IAP-adjacent behaviour is covered by `DEV-006` and `DEV-007`, and the currency-masked reference region (`0,0,300,160`) plus original copy by `DEV-001`/`DEV-002`/`DEV-004`. The absent claim/multiplier affordances and the composition difference are recorded as `FIT-03.12-002`; the underlying reference affordances remain blocked by `BL-REWARD-CLAIM-001` and `BL-REWARDED-AD-001`. Deferred reward semantics were not changed. |

## Divergences

- `FIT-03.12-001` — `ST-0004`: composition/placement divergence. Reference draws the enhancement
  choice as an overlay on the full-screen battle canvas; MySD renders a centered Material 3
  column (title, body, `Filter: All`, two offer buttons, refresh). Accepted structural affordance
  set matches; the difference is creative composition covered by `DEV-001`/`DEV-002`/`DEV-004`.
- `FIT-03.12-002` — `ST-0005`: reward-surface divergence. Reference exposes `reward_claim` and
  `rewarded_multiplier` affordances (both crawl blockers, `BL-REWARD-CLAIM-001` /
  `BL-REWARDED-AD-001`); MySD shows a deferred-reward-safe victory panel with no claim, doubling,
  transaction, or economy behaviour, per `DEV-006`/`DEV-007`. The victory role and reward-panel
  visibility are preserved.

## Scope outcome

- Both `ST-0004` and `ST-0005` were reached on the connected device through the shipped intent
  flow; accepted affordance coverage was verified against the state-graph nodes.
- No wiring or preview-fixture gap was found; no presentation code change was required to reach
  either state. No production screen, mechanic, authoritative game state (`:game`
  `CampaignSession` remains deferred-reward-safe), raw reference asset, or reference UI copy was
  added for this QA task.
- Neither state is pixel-scored: both references are `preserved_unusable` with no usable visual
  evidence, and ImageMagick is unavailable. Visual parity remains explicitly deferred/blocked for
  both states.
