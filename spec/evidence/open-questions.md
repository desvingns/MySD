# Open questions after reference crawl

These are investigation questions, not requirements.

1. Defeat was not observed. The safe crawl reached victory and a resume confirmation state; no further destructive or risky flow was used to force defeat.
2. The exact semantics of the speed control are unresolved: the visible indicator changed, but the multiplier was not independently confirmed.
3. The pause control produced a stable visual interval, but an explicit paused state was not confirmed.
4. The exact root content for Shop and Tech needs a later controlled review because the selected tab and visible content did not converge in the capture window.
5. Arena remains intentionally blocked: no network Arena request was made.
6. Victory reward claim and rewarded doubling are service-adapter questions. No ad was completed and no purchase was attempted; a test-ad surface appeared after an ambiguous tap and was stopped immediately.
7. Enhancement refresh behavior, costs, persistence, and deterministic rules require repeated controlled trials.
8. The three initial-choice trials are too small to establish a mechanic requirement or balance rule.
9. Visible upgrade controls in the roster were not activated; no upgrade behavior should be inferred.
10. The system Back result was an external store surface, not a confirmed in-game navigation rule.
11. Any future functional requirement must cite passed evidence or an explicit human decision at Gate 1.

Low-confidence claims (below 0.8) are recorded in mechanic-claims.csv with open_question status.

## Product-decision disposition — 2026-09-16

The questions above remain unresolved as reference facts. They no longer block original MySD
production design because the human explicitly authorized a complete integrated implementation and
delegated the unobserved decisions. Defeat, 1x/2x speed, pause, Shop, Tech, local Arena exhibition,
reward/claim behavior, deterministic enhancement rules, roster progression, and setup/loadout
effects are therefore implemented as original MySD semantics under `DEV-010`. Their implementation
must never increase the confidence or change the observation status of CL-0001 through CL-0005.

## Supplemental evidence after controlled follow-up

- Graph-level low-confidence investigation clusters: 9. Mechanic claims below 0.8 remain five rows, CL-0001 through CL-0005; these counts are intentionally separate.
- Defeat remains unobserved and is represented by graph edge ED-0025 as an explicit safe-boundary blocker. The approved Gate 1 policy, graph schema, and validator now treat that structured blocker as sufficient; defeat mechanics remain outside requirements unless separately decided.
- Shop selections EV-0139 and EV-0200 and Tech selections EV-0147 and EV-0202 left the visible campaign surface unchanged; both areas remain deferred.
- Reward, purchase, ad, Arena network, credentials, and personal-data paths remain unexecuted.
