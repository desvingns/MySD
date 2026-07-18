# MyEngine Gap Analysis

Status: **pre-Gate 1; demand is not yet incremented**

## Confirmed foundation gaps

| Gap | Evidence | Decision/card | Status |
|---|---|---|---|
| Cross-repo engine consumption was undecided | MyEngine PROC-002 + MySD repository boundary | ADR-0004 / PROC-002 | foundation decision |
| Generic runtime/session orchestration is inside `games:sandbox` and Experimental | `SandboxRuntime`, `SandboxSession`, save codec; `docs/API_STABILITY.md` | ENG-036 | backlog candidate |
| `me-spec` lacks hierarchical game-state and mechanic-claim import | MySD evidence contract + current game-spec pipeline | PROC-015 | backlog candidate |

## Existing capability candidates

Do not add `mysd` demand until Gate 1 confirms use. Candidate cards:

`ENG-009`, `ENG-010`, `ENG-012`, `ENG-015`, `ENG-017`, `ENG-020`, `ENG-021`,
`ENG-022`, `ENG-028`, `ENG-029`, `ENG-034`.

## Potential new reusable families

These are claim families, not backlog cards:

- production buildings and allied-unit lifecycle;
- mobile-unit combat, aggro, and projectiles;
- in-run draft/modifier system;
- campaign stages, energy, and sweep;
- roster/loadout and extended profile progression.

After Gate 1, deduplicate each family against backlog/active/done, API stability, and accepted engine
behavior. Create `ENG-037+` only with observed evidence, EARS/Gherkin acceptance, deterministic
ordering, save/replay impact, content schema, performance gate, and dependency order.

## Game-specific by default

Screen layout, original art/content, balance, MySD navigation composition, and offline service
behavior stay in MySD.
