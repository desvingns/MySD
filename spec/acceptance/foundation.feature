Feature: Reproducible and public-safe MySD foundation

  @AC-001 @FR-001 @US-001
  Scenario: Resolve the pinned engine in CI
    Given gradle/myengine.lock contains a reachable MyEngine commit
    And CI has checked out MyEngine at that commit
    When the MySD test and Android assemble tasks run through the composite build
    Then the engine smoke test passes
    And the Android debug application assembles

  @AC-002 @FR-002 @FR-006 @US-002
  Scenario: Reject raw reference evidence from public history
    Given a tracked or historical path contains a forbidden raw reference artifact
    When the public safety gate runs
    Then the gate fails
    And it reports the offending path

  @AC-003 @FR-003 @US-003
  Scenario: Use a local service adapter
    Given the first-release service configuration
    When the player invokes an accepted ad, purchase, or Arena-shaped affordance
    Then a deterministic local adapter handles the request
    And no production SDK or backend is contacted

  @AC-004 @FR-004 @FR-007 @US-004
  Scenario: Preserve deterministic state across restore
    Given the same content version, seed, and command log
    When a run is saved and restored at a supported schema version
    Then its per-tick hash trajectory matches the uninterrupted run

  @AC-005 @FR-005 @FR-008 @US-005
  Scenario: Prevent unsupported mechanic promotion
    Given a mechanic claim has no accepted observed evidence or human decision
    When the spec traceability gate runs
    Then no gameplay FR links to that claim
    And the claim remains in the ledger or open questions
