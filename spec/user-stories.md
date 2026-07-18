# User Stories

Status: baseline only

## US-001 — Reproducible contributor build

As a contributor, I want MySD to consume the pinned engine revision so that local and CI behavior
does not depend on whichever MyEngine checkout happens to be current.

Links: FR-001.

## US-002 — Safe public repository

As the repository owner, I want publication to fail on raw reference artifacts so that evidence
collection cannot leak third-party binaries or creative assets.

Links: FR-002, FR-006.

## US-003 — Offline player

As a player, I want service-shaped features to have deterministic local behavior so that the first
release remains playable without ads, payments, accounts, or Arena backend.

Links: FR-003.

## US-004 — Returning player

As a returning player, I want run and profile data to survive lifecycle/process death and supported
schema upgrades without changing replay outcomes.

Links: FR-004, FR-007.

## US-005 — Spec reviewer

As a spec reviewer, I want every state and mechanic requirement to cite accepted evidence or an
explicit product decision so that inferred behavior does not silently enter implementation.

Links: FR-005, FR-008.
