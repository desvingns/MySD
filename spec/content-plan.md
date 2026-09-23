# Content Plan

Status: full-product original content baseline accepted by human decision on 2026-09-16

## Originality rules

All public content uses original names, silhouettes, palette, icons, lore, audio, prose, reward
numbers, enemy/building/unit families, and stage geometry. Reference evidence informs functional
roles and screen structure only.

## Accepted semantic families

- campaign entry and observed level setup;
- early battle setup, active wave, and enhancement-choice contours;
- roster surface and local settings open/close contour;
- deterministic local reward and Arena service fixtures.

## Human-decided release-one families

- six stages in two original regions, each with ten waves;
- four tower roles, three allied-unit roles, six ordinary enemy roles, and two bosses;
- two hero abilities and twelve run enhancements;
- explicit independent hero-ability selection, including an empty selection; older profile saves
  preserve their all-unlocked implicit selection during migration, without changing an existing run;
- roster/loadout levels and a twelve-node prerequisite-aware technology graph;
- energy, soft and premium-shaped local currencies, stars, rewards, fifteen claim-once track tiers,
  and mastered-stage sweep;
- four Shop entries spanning soft-currency behavior plus rewarded/purchase-shaped local stubs;
- deterministic local Arena exhibition content with no matchmaking or backend.

These counts and semantics are original product decisions, not inferred reference facts. Every family
still needs schema version, validation errors with field paths, stable IDs, replay impact, save
impact, and invalid-content coverage.

## Delivery rule

Implement the accepted families in the single 2026-09-16 batch. Author all scenario tests, replay
goldens, migrations, and balance/load fixtures before executing the final integrated verification
cycle; do not run intermediate checks.
