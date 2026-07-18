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

The crawl is eligible for Gate 1 when:

- every root tab is reached;
- one full core loop reaches victory or defeat and both terminal states are captured or blocked with
  an explicit reason;
- each visible affordance maps to an edge, deviation, or blocker;
- positive and negative resource/access states exist;
- six consecutive iterations add no node, affordance, or claim;
- every inference below `0.8` is present in `open-questions.md`.

Passing this gate means the inventory is reviewable, not that the game spec is accepted.
