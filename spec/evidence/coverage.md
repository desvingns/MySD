# Reference coverage report

Status: Gate 1 and relaxed Gate 2 accepted; implementation and per-surface Visual Fit remain phase-gated.

## Approved Gate 1 policy (2026-07-19)

- Gate 1 reviews semantic/behavioral inventory and scope, not full visual parity.
- Victory is observed; structured blocker `ED-0025` satisfies defeat coverage without unsafe forcing.
- Observed potion `0` plus the structured unavailable-energy blocker satisfies negative-access coverage.
- Shop and Tech are deferred. Ads, IAP, and Arena are limited to deterministic local adapters.
- Invalid legacy PNGs remain preserved and unusable for visual claims. Bounds, complete valid visual
  anchors, perceptual hashes, and before/after PNG parity move to per-surface Visual Fit Gates.
- Low-confidence claims remain open questions and cannot create requirements.

## Recovery re-crawl update (2026-07-18/19)

- The 11 existing recovery PNG probes were binary-valid and are now indexed with their paired UI dumps as `probe_not_promoted`; route/state/action linkage was not recorded, repeated battle frames were observed, and one pair was host-launcher output.
- A further 23 binary-safe PNG probes were captured on `emulator-5554` (Pixel 9, 1080x2424). Every file passed PNG magic, Pillow decode, dimensions, and SHA-256 checks. Only `EV-0233` (clean launch, `ST-0001`) and `EV-0243` (active battle, `ST-0003`) were promoted as node anchors with fresh DCT pHash signatures; no edge was promoted.
- A test-ad surface appeared during an interaction attempt. It was closed immediately; no rewarded-ad completion, purchase, credential entry, or Arena request occurred. Its captures remain local and `probe_not_promoted`.
- No recovery capture was used to overwrite a legacy artifact. The legacy 95 files remain untouched and invalid for visual use.
- Independent graph-reference audit: 59 screenshot references checked, 9 valid, 50 legacy PNG references fail magic/decode, with zero missing paths and zero SHA-256 mismatches. These files cannot support visual claims but do not block trace-backed behavioral inventory.

| Area | Result | Evidence / note |
| --- | --- | --- |
| Device preflight | pass | Pixel 9 AVD, Android 17/API 37, 1080x2424, en-US, package version 1.0/code 18. |
| Clean launch after authorized clear | pass | Fresh launch and first setup contour captured. |
| Battle setup options | pass | Three early runs used three different initial options. |
| Early battle repetition | pass | Three early-battle trials were observed; one reached a full victory. |
| Wave progression | pass | Wave activity, between-wave enhancement choice, refresh, and final victory were captured. |
| Victory terminal | pass | Victory reward panel captured. |
| Defeat terminal | structured blocker accepted for Gate 1 | No safe defeat path was reached; `ED-0025` records the approved safety-boundary blocker. |
| Campaign root / Battle route | pass | Campaign selection and level start were captured. |
| Troops route | pass | Roster, unit slots, and upgrade affordances were visible. |
| Shop route | deferred | Controlled selections EV-0139 and EV-0200 changed tab selection, but no distinct Shop surface was confirmed. |
| Tech route | deferred | Controlled selections EV-0147 and EV-0202 changed tab selection, but no distinct Tech surface was confirmed. |
| Arena route | blocker | Network Arena was intentionally not run; service adapter remains out of scope. |
| Settings route | pass | Local settings overlay opened and closed; no personal-data field was entered. |
| Rewarded ad / purchase | blocked | No purchase or rewarded-ad completion; a test-ad surface appeared after an ambiguous victory tap and was stopped immediately. |
| Affordance coverage | pass for observed contour | Graph maps captured affordances to edges or explicit blockers; no unmatched IDs. |
| Positive/negative meta | pass for Gate 1 | Energy 22/30, crystal 340, and potion 0 were observed; the unavailable-energy transition remains a structured blocker. |
| Discovery plateau | pass | Six controlled follow-up iterations added no node, affordance, or mechanic claim. |
| Inference hygiene | pass | Low-confidence mechanics remain open questions, not requirements. |

Gate 1 is accepted. Shop/Tech, external service semantics, unobserved mechanics, and visual-fit
work remain outside production requirements. Gate 2 accepted the semantic bundle only; implementation
and per-surface Visual Fit remain phase-gated.

## Post-gate product decision (2026-09-16)

The historical coverage verdict above is unchanged. A later explicit human decision authorized a
complete original MySD product in one integrated batch and promoted Shop/Tech plus the otherwise
blocked gameplay/meta semantics into production scope under `DEV-010`. This is design authority,
not new reference evidence: table rows marked deferred/blocker remain accurate descriptions of the
crawl, and no mechanic-claim confidence is increased.

## positive-negative-meta

Positive observations are energy 22/30, crystal 340, and potion 0. A safe energy-unavailable
transition was not reached: the known route would require repeated finite battle runs, while the
relevant costs and reward resolution remain unconfirmed. This is a structured coverage blocker,
not an inferred locked-state result.

## Controlled follow-up evidence

- Plateau iterations 1–6 are recorded in `.reference-local/traces/trace.jsonl` as `TR-PLATEAU-0001` through `TR-PLATEAU-0006`; each has screenshot/UI-dump before/after pairs and empty new-node, new-affordance, and new-claim arrays.
- Victory and resume linkage is now explicit in graph edges ED-0023 and ED-0024. Defeat is represented as structured blocker ED-0025; `terminal_states_seen` intentionally remains `victory` only.
- The Cocos surface exposes a single full-screen GameCanvas in the UI dumps. Affordance bounds cannot be derived from UI-node bounds without inventing coordinates; this is retained as structured inventory blocker G1-BL-010.
- Structural and semantic digests are populated for all 13 nodes. Masked DCT perceptual hashes are populated for the six nodes with new valid PNG captures; legacy visual captures remain preserved and are listed under G1-BL-011 rather than rewritten.
- The contract, coverage schema, and validator now share the approved structured-terminal-blocker policy.
