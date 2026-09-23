# FIT-04.05 Settings composition

Status: done
TASK: feature
PLATFORM: android
WHAT: Fix the evidence-backed ST-0008/OVERLAY-SETTINGS composition divergence by replacing the default dialog presentation with an original bounded settings panel that keeps the current local toggle, close, and confirm affordances and their deferred semantics.
LAYERS: presentation
TEST_TYPES: instrumented-compose-ui, screenshot
CHANGED_HINT:
- app/src/main/kotlin/dev/mysd/android/campaign/CampaignScreen.kt
- app/src/main/kotlin/dev/mysd/android/ui/theme
- app/src/main/res/values/strings.xml
CONSTRAINTS:
- Require a connected booted Android device for visual verification; never claim a visual pass without connected evidence.
- Preserve RosterSession and RosterSnapshot semantics: toggle intents remain no-op until accepted; close/confirm return to the roster surface.
- Use original MySD panel treatment and copy; do not copy reference assets or UI text.
- Keep the overlay modal, dismiss behavior explicit, minimum 48 dp targets, and text scalable.
- Do not add settings persistence, audio/haptics effects, account, service, or network behavior.
- Preserve unrelated working-tree changes and never delete files.
EVIDENCE:
- ST-0008, EV-0206, FR-105, AC-103, FIT-03.13-003, DEV-002, DEV-004, DEV-009
Acceptance-matrix: screen=settings; action=toggle,close,confirm; pass=composition,bounds,semantics,accessibility
Risk-signals: visual/device work; cross-module data flow
DESIGN_TOKENS: SettingsOverlayScrim, SettingsPanel, SettingsPanelBorder, SettingsOnPanel, SettingsSwitchTrack, SettingsSwitchThumb, SettingsConfirmAction, SettingsCloseAction, SettingsMetrics

=== SPEC ===
TASK: feature
PLATFORM: android
WHAT: Fix the evidence-backed ST-0008/OVERLAY-SETTINGS composition divergence by replacing the default dialog presentation with an original bounded settings panel that keeps the current local toggle, close, and confirm affordances and their deferred semantics.
LAYERS: presentation
TEST_TYPES: instrumented-compose-ui, screenshot
CONSTRAINTS: Connected device required; preserve RosterSession semantics and modal behavior; toggle effects remain deferred; original creative only; no settings persistence or services; minimum 48 dp targets.
Acceptance-matrix: screen=settings; action=toggle,close,confirm; pass=composition,bounds,semantics,accessibility
Risk-signals: visual/device work; cross-module data flow
=== END SPEC ===
