# TASK-04.2 Fit Gate — All Registry States

Date: 2026-08-29  
Status: complete with unscored aggregate fit

## Scope and method

The fit gate evaluated every row in `spec/fit/registry.csv` against the locked rules in
`spec/deviations.md`. Device captures and UI dumps were taken or reconciled on
`emulator-5554` (`Pixel_5(AVD)-14`, Android 14/API 34, 1080×2340 at 440 dpi). Structural
checks found the shipped semantic targets and controls at or above 48 dp where applicable.

The visual score is intentionally not reported. The reference profile is Pixel 9
1080×2424 at 420 dpi, ImageMagick is unavailable, and several registry references are
`preserved_unusable`; no pixel or timing score can be inferred from those conditions.

## Registry reconciliation

| Screen | MySD state | Registry status | Outcome | Structural | Visual / timing disposition | Divergences |
|---|---|---|---|---:|---|---|
| ST-0001 | ROUTE-LAUNCH | accepted | resolved | 100 | Manual multimodal PASS; profile/tooling mismatch leaves pixel score unscored; timing n/a | FIT-03.11-001 resolved |
| ST-0002 | BATTLE-SETUP | accepted | blocked | 100 | EV-0003/0017/0115 preserved_unusable; timing n/a | none claimed |
| ST-0003 | BATTLE-ACTIVE | accepted_visual_fit_deferred | resolved | 100 | Manual multimodal PASS; profile/tooling mismatch leaves pixel score unscored; timing n/a | FIT-03.11-003 resolved |
| ST-0004 | BATTLE-ENHANCEMENT | accepted_visual_fit_deferred | blocked | 100 | EV-0041 preserved_unusable; timing n/a | none claimed |
| ST-0005 | BATTLE-VICTORY | accepted_visual_fit_deferred | blocked | 100 | EV-0097/0099 preserved_unusable; deferred reward actions unchanged; timing n/a | none claimed |
| ST-0006 | ROUTE-CAMPAIGN | accepted | resolved | 100 | Manual multimodal PASS; profile/tooling mismatch leaves pixel score unscored; timing n/a | FIT-03.13-001 resolved |
| ST-0007 | ROUTE-TROOPS | accepted | resolved | 100 | Manual multimodal PASS; profile/tooling mismatch leaves pixel score unscored; timing n/a | FIT-03.13-002 resolved |
| ST-0008 | OVERLAY-SETTINGS | accepted | resolved | 100 | Manual multimodal strong qualitative PASS; profile/tooling mismatch leaves pixel score unscored; timing n/a | FIT-03.13-003 resolved |
| ST-0009 | ROUTE-SHOP-DEFERRED | deferred | deferred | n/a | No capture; Shop remains out of scope | none |
| ST-0010 | ROUTE-TECH-DEFERRED | deferred | deferred | n/a | No capture; Tech remains out of scope | none |
| ST-0011 | ROUTE-ARENA-LOCAL | accepted_service_adapter | blocked | 100 | EV-0149 preserved_unusable; network match remains excluded; timing n/a | none claimed |
| ST-0012 | OVERLAY-RESUME | accepted | resolved | 100 | Manual multimodal strong qualitative PASS; EV-0189 is invalid/unsupported locally and ImageMagick is unavailable; timing n/a | FIT-03.13-005 resolved |
| ST-0013 | EXTERNAL-EXIT-EXCLUDED | excluded | excluded | n/a | Host behavior excluded; no capture required | none |

## Findings and locked scope

There are no remaining unexplained, evidence-backed divergences.
FIT-03.11-001, FIT-03.11-003, FIT-03.13-001, and FIT-03.13-002 are resolved by FIT-04.01,
FIT-04.02, FIT-04.03, FIT-04.04, and FIT-04.05 respectively. FIT-03.13-005 is resolved by
FIT-04.06. Locked deviations and preserved-unusable references remain
explicitly blocked or deferred; no behavior was inferred from them.

DEV-001 through DEV-009 remain in force. In particular, original identity, art, copy,
balance, local rewarded-ad/IAP adapters, local Arena service shape, and accessibility
minimums are preserved. Shop, Tech, defeat, reward transactions, rewarded completion,
IAP payment, account/network Arena, and external-exit behavior were not promoted.

## Blockers and proposed next work

- Aggregate score and threshold (85) cannot be evaluated honestly with the current reference/device/tooling state.
- The fit write-gate was accepted for the six candidate fix SPECs. `fit-04-01-launch-composition.md`
  `fit-04-02-active-battle-composition.md`, `fit-04-03-campaign-composition.md`,
  `fit-04-04-roster-composition.md`, and `fit-04-05-settings-composition.md` are done;
  `fit-04-06-resume-composition.md` is done. All six unexplained divergences are resolved.

## Evidence artifacts

- `build/fit/built/ST-0001..ST-0012` PNG/XML captures, including `TASK-04.2-ST-0012`.
- `build/fit/built/TASK-04.16-FIT-04.01-ST-0001.png` and
  `build/fit/built/TASK-04.16-FIT-04.02-ST-0003.png` — current connected captures for the
  resolved launch and active-battle divergences.
- `build/fit/built/TASK-04.16-FIT-04.03-ST-0006.png` and
  `build/fit/built/TASK-04.16-FIT-04.04-ST-0007.png` — current connected captures for the
  resolved campaign and roster divergences.
- `build/fit/built/TASK-04.16-FIT-04.05-ST-0008.png` — current connected capture for the
  resolved settings divergence; multimodal fit passed and numeric pixel scoring remains unscored.
- `build/fit/built/TASK-04.16-FIT-04.06-ST-0012.png` — current connected capture for the resolved
  resume divergence; multimodal fit passed while the preserved-unusable reference prevents pixel scoring.
- `app/build/outputs/apk/debug/app-debug.apk` — SHA-256
  `4ABA75DDAC86EE2CE71E5DC184EB52CEEDC5ABD6C00D7BB1C596F8B5DC4140BB`.
- Connected test evidence: `16 passed / 0 failed / 0 skipped` on Pixel_5(AVD)-14, covering the
  full regression suite and the ST-0008/ST-0012 screenshot captures.
- Full project runner evidence: `125 passed / 0 failed / 0 skipped`.

No reference assets or copied reference UI text were added to public project files.
