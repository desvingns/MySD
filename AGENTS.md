# AGENTS.md - Working On MySD

## Communication

- Respond to the user in Russian.
- Write project memory and machine-facing evidence schemas in English.
- Never delete files; archive superseded artifacts.

## Product boundary

- Android is the only shipping platform; JVM is used for headless simulation and tests.
- MySD uses original IP, art, audio, text, iconography, and balance.
- Reference APKs, screenshots, recordings, UI dumps, extracted assets, and verbatim UI copy are
  local-only under `.reference-local/` and never enter git history.
- Every public image, audio, or font needs an approved `assets/provenance.csv` row with original,
  generated, or licensed origin and a known license.
- Ads, IAP, and Arena use interfaces plus deterministic local adapters in the first release. No
  production SDK, payment, account, or backend integration is in scope.

## Start checklist

Read in order:

1. `AGENTS.md`
2. `.ai/handoff.md`
3. `spec/00_manifest.yaml`
4. `docs/reference/LUNA_CRAWL_RUNBOOK.md`
5. `docs/implementation/ROADMAP.md`
6. `gradle/myengine.lock`

## Evidence gates

- Do not convert a mechanic hypothesis into an FR until observed evidence or a human decision
  supports it.
- Gate 1 locks inventory and scope before the production bundle is authored.
- Gate 2 locks the evaluated bundle before production gameplay code starts.
- Every observed affordance maps to a state-graph edge, explicit deviation, or blocker.
- Volatile counters are observations, not separate state nodes.

## Engine boundary

- Consume MyEngine through the composite build and exact commit in `gradle/myengine.lock`.
- Update the pin only after the corresponding MyEngine change is accepted and verified.
- Simulation remains Android-free, deterministic at 20 Hz, seedable, replay-hashable, and save
  migration-aware.
- Rendering and input do not own authoritative game state.
- Game layout, original content, balance, and local service behavior stay in MySD.

## Verification

Run the narrowest relevant checks, plus before publication:

```powershell
powershell.exe -File scripts/public-safety.ps1
.\gradlew.bat test :app:assembleDebug
```

Update `.ai/handoff.md` after substantial work. Durable project lessons go to
`.ai/memory/MEMORY.md`; cross-project candidates are appended to `D:/Pet/brain/inbox/` only at the
planned human gate.
