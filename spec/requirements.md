# Requirements

Status: **full-product human decision active (2026-09-16); implementation in one integrated batch**

## Normative baseline

### FR-001 — Pinned engine consumption

While configuring any local or CI build, the system shall resolve MyEngine through a Gradle
composite build at the exact commit recorded in `gradle/myengine.lock`.

### FR-002 — Public evidence safety

When publishing any revision, the system shall reject tracked/history paths containing reference
APKs, screenshots/video/UI dumps, extracted assets, or other declared raw evidence.

### FR-003 — Offline service boundary

When rewarded ads, purchases, or Arena are requested in the first release, the system shall invoke
an interface-backed deterministic local adapter without contacting a production SDK or backend.

### FR-004 — Deterministic simulation

While a run is active, the system shall advance authoritative simulation at fixed 20 Hz using
seeded randomness, stable system ordering, command submission, and replay-hashable state.

### FR-005 — Evidence-gated scope

When a reference mechanic has not been observed or explicitly decided by the human, the
specification shall keep it as a candidate claim/open question and shall not promote it to a
gameplay requirement.

### FR-006 — Original public identity

Where the reference contains protected creative expression, MySD shall use original names, setting,
art, iconography, audio, UI prose, and balance values.

### FR-007 — Versioned persistence

When run or profile state is persisted, the system shall write an explicit schema version and shall
restore supported historical versions through tested migrations.

### FR-008 — Observable fit registry

After Gate 1, every in-scope reference state shall map to a MySD state, an explicit deviation, or a
blocker in the fit registry.

## Accepted semantic scope after Gate 1

### FR-100 — Enter the accepted campaign contour

Given a clean local launch, the system shall expose the observed campaign selection, start-level
setup, and unfinished-run prompt contours, including the observed cancel and continue paths. Energy
and currency values remain observations, not prescribed economy numbers.

Accepted inventory: `INV-001`.
Evidence: `ST-0001`, `ST-0002`, `ST-0006`, `ST-0012`, `ED-0001`, `ED-0013`, `ED-0020`, `ED-0021`, `ED-0024`.

### FR-101 — Preserve the observed early-battle setup contour

Given an accepted campaign level, the system shall expose the observed setup choices, tutorial
continuation, and start-battle path without requiring copied text, art, balance values, or inferred
choice effects.

Accepted inventory: `INV-002`.
Evidence: `ST-0002`, `ED-0002`, `ED-0003`, `ED-0004`, `ED-0005`, `ED-0006`.

### FR-102 — Expose the observed active-battle affordance contour

During the accepted early battle, the system shall expose wave activity, the visible base/enemy
contour, and the observed speed, pause/resume, and available-build affordances. Exact multiplier,
pause semantics, build cost, and build effect remain open questions.

Accepted inventory: `INV-002`.
Evidence: `ST-0003`, `AF-0007`, `AF-0008`, `AF-0009`, `ED-0007`, `ED-0008`, `ED-0009`.

### FR-103 — Preserve the observed enhancement-choice contour

Between observed battle phases, the system shall expose an enhancement-choice surface with offers,
filter visibility, refresh affordance, and return to active battle after an offer selection. Offer
effects, costs, persistence, and deterministic selection rules remain deferred.

Accepted inventory: `INV-002`.
Evidence: `ST-0004`, `AF-0010`, `AF-0011`, `AF-0012`, `ED-0010`, `ED-0011`, `ED-0012`.

### FR-104 — Resolve the observed victory contour

The accepted safe local battle contour shall be able to resolve to the observed victory reward
panel. Defeat remains represented by structured blocker `ED-0025` and does not create an inferred
defeat mechanic requirement.

Accepted inventory: `INV-002`, `INV-007`.
Evidence: `ST-0005`, `ED-0023`, `ED-0025`, `BL-REWARD-CLAIM-001`, `BL-REWARDED-AD-001`.

### FR-105 — Preserve roster and local-settings surface contours

The system shall expose the observed roster surface and local settings open/close contour. Roster
upgrade effects and settings-toggle semantics remain blocked until separately observed or decided.

Accepted inventory: `INV-003`.
Evidence: `ST-0007`, `ST-0008`, `ED-0015`, `ED-0018`, `ED-0019`.

### FR-106 — Keep the reward boundary local and deterministic

When the accepted victory reward affordances are invoked, the system shall use a deterministic
local adapter for normal reward and rewarded-multiplier-shaped outcomes. No real ad, purchase, or
transaction is required or permitted by this bundle.

Accepted inventory: `INV-007`.
Evidence: `ST-0005`, `AF-0013`, `AF-0014`, `BL-REWARD-CLAIM-001`, `BL-REWARDED-AD-001`.
Deviation: `DEV-006`, `DEV-007`.

### FR-107 — Keep Arena service-shaped and offline

When the accepted Arena route is invoked, the system shall use a deterministic local service-shaped
adapter and shall not contact a network match, account, or production backend.

Accepted inventory: `INV-006`.
Evidence: `ST-0011`, `ED-0017`.
Deviation: `DEV-008`.

## Human-decided full-product scope — 2026-09-16

The following requirements are original MySD product decisions. Their inventory links identify the
accepted surface family, not evidence for exact reference behavior, names, visuals, or numbers.

### FR-108 — Deliver the complete offline campaign

Given a local profile, the system shall expose six original stages, deterministic unlock order,
energy guards, stage setup, replay, stars, and a mastered-stage sweep path without requiring a
network or account.

Accepted inventory: `INV-001`.
Authority: human product decision, 2026-09-16.

### FR-109 — Resolve deterministic multi-wave battles

While a stage is active, the system shall resolve ten fixed-tick waves containing original normal,
elite, and boss compositions, including construction, upgrades, mobile allies, hostile movement,
attacks, leaks, resources, pause, 1x/2x speed, and naturally reachable victory and defeat.

Accepted inventory: `INV-002`.
Authority: human product decision, 2026-09-16; values and content are original.

### FR-110 — Support loadout and battlefield roles

Before and during battle, the system shall support an original loadout drawn from four tower roles,
three allied-unit roles, and two hero abilities, with deterministic command guards and no
authoritative state owned by Android rendering or input.

Accepted inventory: `INV-002`, `INV-003`.
Authority: human product decision, 2026-09-16.

### FR-111 — Apply deterministic run enhancements

After waves 2, 4, 6, and 8, the system shall pause battle and present three distinct deterministic
enhancement offers; selecting one shall persist its authoritative effect for the rest of the run.

Accepted inventory: `INV-002`.
Authority: human product decision, 2026-09-16.

### FR-112 — Settle rewards and sweep through a ledger

When a battle or sweep resolves, the system shall calculate original rewards, stars, unlocks, and
claim-once progress as explicit deterministic ledger entries, rejecting duplicate claims and
unaffordable or locked sweep requests atomically.

Accepted inventory: `INV-001`, `INV-007`.
Authority: human product decision, 2026-09-16.

### FR-113 — Provide real roster and technology progression

When the player uses roster or technology routes, the system shall support affordable upgrades,
loadout selection, prerequisites, locked/unlocked nodes, and persisted settings rather than no-op
controls.

Accepted inventory: `INV-003`, `INV-005`.
Authority: human product decision, 2026-09-16.

### FR-114 — Provide a local shop with service-shaped stubs

When the player uses the Shop, soft-currency products may transact through the local profile ledger,
while rewarded and purchase-shaped products shall return deterministic deferred placeholders and
shall never invoke an ad SDK, billing SDK, backend, account, or network.

Accepted inventory: `INV-004`, `INV-007`.
Authority: human product decision, 2026-09-16; deviations `DEV-006`, `DEV-007`.

### FR-115 — Provide a deterministic local Arena exhibition

When the player enters Arena, the system shall create a seedable local exhibition against a generated
formation and a local result preview, while matchmaking, rankings, accounts, and network traffic
remain absent.

Accepted inventory: `INV-006`.
Authority: human product decision, 2026-09-16; deviation `DEV-008`.

### FR-116 — Persist accessible local settings

When sound, music, haptics, or reduce-motion preferences change, the system shall persist their
local values. When the Android system text scale changes, every route shall reflow while retaining
semantic labels, non-colour status cues, and at least 48 dp interactive targets.

Accepted inventory: `INV-003`.
Authority: human product decision, 2026-09-16; deviation `DEV-009`.

### FR-117 — Restore the complete product session deterministically

When a full-product run or profile is saved and restored, the system shall preserve content/schema
identity, tick, seed/RNG state, pending commands, waves, entities, enhancements, meta ledger, and
route state, while retaining explicit legacy run/profile migration behavior.

Accepted inventory: `INV-001`, `INV-002`, `INV-003`.
Authority: human product decision, 2026-09-16.

## Still excluded

System Back external exit (`INV-008`), real service transactions, copied creative expression, and
claims of exact unobserved reference behavior remain excluded. Complete visual fit remains a final
masked structural comparison obligation rather than permission to copy creative regions.
