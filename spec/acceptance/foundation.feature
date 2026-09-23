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

  @AC-100 @FR-100 @US-100
  Scenario: Enter campaign and handle an unfinished local run
    Given a clean local launch and an accepted campaign contour
    When the player starts a campaign level or responds to an unfinished-run prompt
    Then the observed setup, cancel, and continue contours are available
    And energy and currency values remain observations rather than copied balance requirements

  @AC-101 @FR-101 @FR-102 @US-101
  Scenario: Start and operate the accepted early battle contour
    Given an accepted campaign level with setup choices
    When the player continues setup and starts the battle
    Then wave activity and the observed base/enemy contour are presented
    And speed, pause/resume, and available-build affordances are exposed
    And unconfirmed multiplier, cost, and effect semantics remain deferred

  @AC-102 @FR-103 @FR-104 @US-102
  Scenario: Choose an enhancement and resolve the safe victory contour
    Given the accepted battle reaches an enhancement-choice phase
    When the player views or refreshes offers and selects an offer
    Then the battle can return to the active contour
    And the safe local run can resolve to the observed victory reward panel
    And defeat remains represented by structured blocker ED-0025 rather than an inferred mechanic

  @AC-103 @FR-105 @US-103
  Scenario: Open roster and local settings
    Given the accepted troops route
    When the player opens local settings and confirms the overlay
    Then the roster and settings open/close contours are preserved
    And unobserved upgrade and toggle effects remain deferred

  @AC-104 @FR-106 @FR-107 @US-104
  Scenario: Keep accepted service boundaries deterministic and offline
    Given a reward or Arena-shaped affordance is invoked
    When the local service boundary handles the request
    Then a deterministic local adapter returns the configured result shape
    And no production SDK, payment, account, or network Arena request is made

  @AC-105 @FR-108 @FR-112 @US-105
  Scenario: Progress through the complete offline campaign
    Given a fresh local profile and the original six-stage campaign
    When the player spends available energy, completes stages, claims rewards, and sweeps a mastered stage
    Then unlocks, stars, currencies, energy, and claim-once state are recorded as deterministic ledger entries
    And locked, duplicate, or unaffordable actions leave the profile unchanged

  @AC-106 @FR-109 @FR-110 @US-106
  Scenario: Resolve a complete multi-wave defense
    Given an original stage, seed, and valid loadout
    When towers and allies defend the base through ten fixed-tick waves
    Then normal, elite, and boss compositions resolve through deterministic commands and systems
    And natural command sequences can reach both victory and defeat
    And pause or 1x/2x presentation speed never changes authoritative tick results

  @AC-107 @FR-111 @US-107
  Scenario: Choose deterministic run enhancements
    Given a battle has completed wave 2, 4, 6, or 8
    When the battle pauses for three offers and the player selects one
    Then the offers are distinct and reproducible from the run state
    And the selected effect persists through the remainder of the run and save restore

  @AC-108 @FR-113 @US-108
  Scenario: Upgrade roster and technology
    Given a local profile with a known balance and unlock set
    When the player upgrades an eligible roster item or technology node and changes the loadout
    Then costs and prerequisites are enforced atomically
    And the resulting levels, nodes, and loadout survive profile restore

  @AC-109 @FR-114 @US-109
  Scenario: Use the local shop without a real transaction
    Given the deterministic local Shop catalog
    When the player buys a soft-currency offer or invokes a rewarded or purchase-shaped offer
    Then the soft-currency mutation is represented by the profile ledger
    And the service-shaped offer returns an explicit local deferred result
    And no ad SDK, billing SDK, account, backend, or network is contacted

  @AC-110 @FR-115 @US-110
  Scenario: Play a deterministic local Arena exhibition
    Given a profile and Arena seed
    When the player starts the local exhibition
    Then the opponent formation and result preview are reproducible
    And no matchmaking, leaderboard, account, or network request occurs

  @AC-111 @FR-116 @US-111
  Scenario: Persist accessible settings
    Given the local settings route
    When the player changes sound, music, haptics, or reduce-motion preferences and restarts the app
    Then the selected local values are restored
    And Android system text scaling reflows every route without a duplicate in-app text setting
    And interactive controls retain semantic labels, non-colour cues, and 48 dp targets

  @AC-112 @FR-117 @US-112
  Scenario: Restore a complete product session
    Given a supported profile and an active full-product run
    When both are persisted and restored at an arbitrary authoritative tick
    Then route, entities, waves, enhancements, ledgers, pending commands, and terminal guards are preserved
    And continued per-tick hashes match an uninterrupted run with the same seed and commands
    And supported legacy run and profile payloads migrate explicitly
