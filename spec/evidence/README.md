# Sanitized Evidence

This directory is safe to commit. It stores schemas, hashes, measurements, semantic summaries,
confidence, and trace links. It must not contain reference screenshots, recordings, UI dumps,
extracted assets, or verbatim UI copy.

## Workflow

1. Luna captures raw evidence under `.reference-local/`.
2. Luna hashes each raw item and records an `EV-NNNN` entry.
3. Luna distils state/edge facts into `state-graph.v1.json`.
4. Sol verifies provenance and promotes only Gate 1 accepted inventory into requirements.
5. The public-safety gate runs before commit and against the full public git history.

An empty graph is expected before the crawl. `gate1_status: not_started` is not a passing state.
