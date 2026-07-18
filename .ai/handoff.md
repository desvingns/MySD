# MySD Handoff

## DONE

- Repository foundation with Gradle composite build and pinned MyEngine commit.
- Android/JVM scaffold proving engine resolution.
- CI design for exact engine checkout, public-safety, tests, and Android assemble.
- Luna crawl runbook, local raw corpus contract, state graph/capture schemas, claim ledger, and
  coverage gate.
- Pre-Gate spec baseline, deviations, gap analysis, implementation roadmap, and process playbook.

## DECISIONS

- Public original-only repository.
- Raw reference artifacts remain local and ignored.
- Offline services use deterministic local adapters.
- No gameplay requirements or new evidence-driven ENG cards before Gate 1.
- Brain inbox promotion remains human-gated and occurs after Gate 2.

## NEXT

1. Run Luna crawl on Pixel 9 using `docs/reference/LUNA_CRAWL_RUNBOOK.md`.
2. Review the compact inventory and accept/revise Gate 1.
3. Sol expands the full bundle, runs evaluator/traceability, and presents Gate 2.
4. Implement ENG-036 and accepted reusable gaps through MyEngine feature runs.
5. Begin the headless vertical slice only after Gate 2.

## BLOCKERS

- Gate 1: no Luna device corpus yet.
- Gate 2: blocked by Gate 1.
- Full game implementation: blocked by Gate 2 and accepted engine gaps.

## VERIFICATION

- `scripts/validate-evidence.ps1` -> pass (empty pre-crawl graph, Gate 1 not started).
- `scripts/validate-spec.ps1` -> pass (8 FR, 5 US, 5 AC, 8 trace rows).
- `scripts/public-safety.ps1` -> pass (66 tracked files, no public history yet, creative
  provenance gate enabled).
- `.\gradlew.bat :game:test :app:assembleDebug` with JDK 17 and Android SDK -> pass.
- MyEngine `scripts/me-selfcheck.ps1` in the isolated backlog worktree -> pass.
- Independent `me-verifier` -> pass after strengthening Gate 1 evidence references and creative
  asset provenance checks.
