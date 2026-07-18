# Non-Functional Requirements

## Performance

- NFR-001: authoritative simulation runs at fixed 20 Hz.
- NFR-002: render targets 60 FPS on Pixel 9.
- NFR-003: Pixel 9 p95 frame time is at most 16.7 ms and jank is below 5%.
- NFR-004: the load scenario uses accepted observed maximum concurrent entities plus 25%.

## Reliability

- NFR-005: deterministic scenario tests compare per-tick hashes.
- NFR-006: run/profile persistence has roundtrip, migration matrix, malformed-input, lifecycle, and
  process-death coverage.
- NFR-007: unsupported future save versions fail explicitly.

## Operability

- NFR-008: content validation reports stable field paths and IDs.
- NFR-009: every release tag records the exact MyEngine commit.
- NFR-010: builds and tests do not require network after dependencies and pinned repositories are
  available; gameplay itself is offline.
