# TASK-03.10 Arena adapter

Status: done
TASK: feature
PLATFORM: android
WHAT: Implement the offline service-shaped Arena state without network, account, or match behavior (FR-107, US-104, AC-104).
LAYERS: domain/data service boundary
TEST_TYPES: unit, integration, deterministic/offline boundary checks
CHANGED_HINT:
- game/src/main/kotlin/dev/mysd/game/service/ServiceBoundary.kt
- game/src/test/kotlin/dev/mysd/game/service/ServiceBoundaryTest.kt
- game/src/main/kotlin/dev/mysd/game/campaign/CampaignSession.kt
CONSTRAINTS:
- Preserve the accepted Arena affordance through a deterministic local adapter.
- Keep local-service-shaped and network-match-blocked states explicit and immutable.
- Do not contact a network, require an account, create a match, add a production backend, or add copied reference content.
- Preserve unrelated working-tree changes and never delete files; use Fakes-only tests.
EVIDENCE:
- FR-107, US-104, AC-104, ST-0011, ED-0017, DEV-008
Acceptance-matrix: request=accepted,unknown; state=local-service-shaped,network-match-blocked
Risk-signals: cross-module data flow; state or concurrency

=== SPEC ===
TASK: feature
PLATFORM: android
WHAT: Implement the offline service-shaped Arena state without network, account, or match behavior (FR-107, US-104, AC-104).
LAYERS: domain/data service boundary
TEST_TYPES: unit, integration, deterministic/offline boundary checks
CONSTRAINTS: Deterministic local adapter; immutable local/blocked states; no network, account, match, production backend, or copied reference content; preserve existing scope and Fakes-only tests.
Acceptance-matrix: request=accepted,unknown; state=local-service-shaped,network-match-blocked
Risk-signals: cross-module data flow; state or concurrency
=== END SPEC ===

Implementation links: 24d553d, e793697; tests: 118 passed; lint: ok; verifier: pass
