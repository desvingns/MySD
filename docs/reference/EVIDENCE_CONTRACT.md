# Reference Evidence Contract

## Observer/author separation

Luna owns observation records. Sol owns inventory normalization, requirements, engine-gap analysis,
and fit contracts. Sol may challenge or request additional evidence, but must not rewrite an
observed edge into a more convenient behavior.

## Provenance levels

| Source | Meaning | Can create an FR? |
|---|---|---|
| `observed` | Reproducible before/action/after evidence | Yes, after Gate 1 |
| `public_listing` | Public store description or metadata | Inventory candidate only |
| `inferred` | Best explanation of incomplete evidence | No; open question/claim |
| `human_decision` | Explicit MySD product decision | Yes, marked as a deviation/constraint |

## Public/private split

Raw media and UI dumps remain in `.reference-local/`. Public artifacts contain only:

- hashes and capture metadata;
- semantic summaries in original wording;
- dp bounds and timings;
- evidence IDs and local relative pointers;
- confidence and source labels;
- aggregated observations that do not reconstruct reference assets or prose.

No binary/XML capture is allowed under `spec/evidence/`. Separately, every public creative image,
audio file, or font requires an approved original/generated/licensed row in
`assets/provenance.csv`; this is independent of reference evidence IDs.

## Deduplication

All three signatures participate:

1. `structural_signature`: activity plus normalized affordances; volatile labels removed.
2. `visual_signature`: perceptual hash after declared masks for currencies, timers, HP, wave
   counters, and animated regions.
3. `semantic_signature`: route, overlay, battle phase, and stable flags.

Nodes merge only when structural and semantic signatures agree and the masked visual distance is
within the recorded threshold. A conflict remains two nodes or an open question.

## Mechanic claims

One row is one falsifiable hypothesis. Controlled variables and sample count are mandatory before a
claim can reach confidence `>= 0.8`. Supporting/contradicting evidence IDs use semicolon-separated
`EV-NNNN` values, must resolve to the evidence index, and must have passed IP/privacy review.
Contradicting evidence is retained. Promotion to FR/ENG happens only after Gate 1 and adds the final
IDs to the same row.

## Coverage gate

Gate 1 is a semantic/behavioral inventory and scope gate. It does not certify visual parity. The
crawl is eligible for Gate 1 review when:

- every root tab is reached;
- one full core loop reaches a terminal state, and every other required terminal is either observed
  or represented by a structured safety/access blocker;
- each visible affordance maps to an edge, deviation, or blocker;
- positive resource/access evidence exists, and negative coverage is represented by an observed
  zero/unavailable state or by an observed zero-resource state plus a structured blocker for an
  unavailable transition that cannot be reached safely;
- six consecutive iterations add no node, affordance, or claim;
- every inference below `0.8` is present in `open-questions.md`.

Passing this gate means the inventory is reviewable, not that the game spec is accepted.

## Evidence tiers and Visual Fit Gate

Behavioral claims may use action-level traces, UI dumps, stable semantic/structural signatures, and
sanitized observations. A screenshot is required only when a claim is visual. Corrupted legacy PNGs
remain preserved with their original hashes but are unusable for visual claims.

Per-surface Visual Fit Gates run during implementation of each accepted surface. They own valid
visual anchors, before/after PNG pairs, perceptual hashes, masks, measurable bounds, composition,
and timing parity. Missing visual anchors or hashes do not block Gate 1 when the behavioral contour
is otherwise traceable. If GameCanvas child bounds are unavailable, record a structured reason and
defer measurement; never invent coordinates.

Deferred or excluded areas create no production requirements. Shop and Tech remain deferred until
distinct content is observed or a later human product decision adds original scope. Ads, IAP, and
Arena are limited to deterministic local adapters. Low-confidence claims never become requirements.
