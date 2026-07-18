# Requirements

Status: **baseline constraints only; gameplay FRs wait for Gate 1**

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

## Pending after Gate 1

Battle, campaign, economy, roster, tech, sweep, reward, and navigation requirements will be assigned
`FR-100+` only from accepted inventory/evidence.
