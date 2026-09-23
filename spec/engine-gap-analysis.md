# MyEngine Gap Analysis

Status: **ENG-036 technically accepted locally and pinned; remote publication pending**

## Confirmed foundation gaps

| Gap | Evidence | Decision/card | Status |
|---|---|---|---|
| Cross-repo engine consumption was undecided | MyEngine PROC-002 + MySD repository boundary | ADR-0004 / PROC-002 | foundation decision |
| Generic runtime/session orchestration was trapped inside `games:sandbox` | `SandboxRuntime`, `SandboxSession`, save codec; `docs/API_STABILITY.md` | ENG-036 | 184 tests, compatibility/content/static/build and calibrated paired performance gates pass; exact local pin `1174d21c4e92fddff6316f93bc99384ab6c1c689`; Experimental API behind MySD adapter |
| `me-spec` lacks hierarchical game-state and mechanic-claim import | MySD evidence contract + current game-spec pipeline | PROC-015 | implemented separately at engine `005c084eba4de2ce9ee14f9fbedd8d708929ba66`; NOT included in MySD's current `1174d21` pin, where the card remains backlog |

## Existing capability candidates

Gate 1 confirms the sanitized evidence bundle. PROC-015's reusable intake/deduplication bridge exists
in a separate engine lineage, not in this consumer pin; MySD continues to use its local evidence
validators. No additional engine merge or pin change is implied. Candidate cards below remain
subject to the accepted inventory and must not be minted from deferred/open-question claims:

`ENG-009`, `ENG-010`, `ENG-012`, `ENG-015`, `ENG-017`, `ENG-020`, `ENG-021`,
`ENG-022`, `ENG-028`, `ENG-029`, `ENG-034`.

## Game-side families reviewed for reuse

The 2026-09-16 product decision accepts these MySD families, but it does not by itself prove that
they belong in the reusable engine:

- production buildings and allied-unit lifecycle;
- mobile-unit combat, aggro, and projectiles;
- in-run draft/modifier system;
- campaign stages, energy, and sweep;
- roster/loadout and extended profile progression.

They remain in MySD for the integrated batch. Create `ENG-037+` only after at least one second game
demonstrates a reusable abstraction and the usual EARS/Gherkin, deterministic ordering,
save/replay, schema, performance, and dependency gates are satisfied.

## Game-specific by default

Screen layout, original art/content, balance, MySD navigation composition, and offline service
behavior stay in MySD.
