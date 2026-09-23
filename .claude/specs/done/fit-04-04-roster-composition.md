# FIT-04.04 Roster composition

Status: done
TASK: feature
PLATFORM: android
WHAT: Fix the evidence-backed ST-0007/ROUTE-TROOPS composition divergence by presenting the existing roster snapshot in an original illustrated-style roster shell with structured troop cards and persistent route controls while keeping upgrade behavior deferred.
LAYERS: presentation
TEST_TYPES: instrumented-compose-ui, screenshot
CHANGED_HINT:
- app/src/main/kotlin/dev/mysd/android/campaign/CampaignScreen.kt
- app/src/main/kotlin/dev/mysd/android/ui/theme
- app/src/main/res/drawable
CONSTRAINTS:
- Require a connected booted Android device for visual verification; never claim a visual pass without connected evidence.
- Render only existing RosterSnapshot data; UpgradeTroop remains a visible deferred no-op and must not mutate progression.
- Use original MySD vector/Canvas visuals or generated local primitives; no reference asset, copied UI text, or external service.
- Keep settings and close actions wired exactly as they are; minimum 48 dp targets and scalable text apply.
- Do not add loadout, economy, unlock, Shop, Tech, account, or network behavior.
- Preserve unrelated working-tree changes and never delete files.
EVIDENCE:
- ST-0007, EV-0204, EV-0208, FR-105, AC-103, FIT-03.13-002, DEV-001, DEV-002, DEV-004, DEV-009
Acceptance-matrix: screen=roster; state=troops,upgrade-deferred; pass=composition,bounds,semantics,accessibility
Risk-signals: visual/device work; cross-module data flow
DESIGN_TOKENS: RosterBackground, RosterSurface, RosterCard, RosterAccent, RosterSupport, RosterOnBackground, RosterOnSurface, RosterDisabled, RosterRouteInactive, RosterMetrics.minTouchTarget, RosterMetrics.contentInset, RosterMetrics.cardPadding, RosterMetrics.cardGap, RosterMetrics.routeHeight, RosterMetrics.routeItemMinWidth

=== SPEC ===
TASK: feature
PLATFORM: android
WHAT: Fix the evidence-backed ST-0007/ROUTE-TROOPS composition divergence by presenting the existing roster snapshot in an original illustrated-style roster shell with structured troop cards and persistent route controls while keeping upgrade behavior deferred.
LAYERS: presentation
TEST_TYPES: instrumented-compose-ui, screenshot
CONSTRAINTS: Connected device required; existing RosterSnapshot only; upgrade remains deferred/no-op; settings and close remain wired; original creative only; no new progression or service behavior; minimum 48 dp targets.
Acceptance-matrix: screen=roster; state=troops,upgrade-deferred; pass=composition,bounds,semantics,accessibility
Risk-signals: visual/device work; cross-module data flow
=== END SPEC ===
