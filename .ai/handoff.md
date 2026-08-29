# MySD Handoff

## DONE

- Repository foundation with Gradle composite build and pinned MyEngine commit.
- Android/JVM scaffold proving engine resolution.
- CI design for exact engine checkout, public-safety, tests, and Android assemble.
- Luna crawl runbook, local raw corpus contract, state graph/capture schemas, claim ledger, and
  coverage gate.
- Pre-Gate spec baseline, deviations, gap analysis, implementation roadmap, and process playbook.

- Reference crawl completed on the authorized Pixel 9 AVD for com.gdzsq.crazy_td.
- Raw screenshots and UI dumps remain local under .reference-local; the sanitized evidence bundle is Gate 1 accepted for semantic/behavioral scope.
- Three early-battle trials were captured with varied setup choices; victory was observed, defeat remains blocked.
- Follow-up crawl added 16 indexed captures, six empty plateau iterations, action-level traces for all 25 graph edges, explicit victory/resume linkage, and structured defeat/resource/bounds/signature blockers.
- No personal data, purchase, rewarded-ad completion, or Arena network request was performed.
- Independent Gate 1 recheck confirmed 276/276 indexed raw artifacts exist with matching SHA-256, 25/25 action traces and before/after anchors, zero unmatched affordances, and a state graph conforming to its v1 schema.
- Before Gate 2 evaluation, the production bundle was frozen; the relaxed evaluator then added only
  accepted-scope semantic rows. Engine-gap analysis, MyEngine backlog, and the pinned MyEngine commit
  remained unchanged; no new engine demand or production SDK scope was introduced.

## DECISIONS

- Public original-only repository.
- Raw reference artifacts remain local and ignored.
- Offline services use deterministic local adapters.
- No gameplay requirements or new evidence-driven ENG cards before Gate 1.
- Brain inbox promotion remains human-gated and occurs after Gate 2.
- Human accepted the relaxed Gate 1 inventory, scope proposals INV-001 through INV-008, and DEV-001
  through DEV-009. Complete visual evidence moves to per-surface Visual Fit Gates.
- Structured defeat blocker ED-0025 and observed potion=0 plus the unavailable-energy blocker are
  sufficient for Gate 1. GameCanvas bounds may remain structurally unavailable.
- Shop and Tech stay deferred; ads, IAP, and Arena remain deterministic local-adapter boundaries;
  low-confidence claims cannot become requirements.
- Gate 2 was accepted in relaxed semantic scope after evaluator pass; Visual Fit and implementation
  remain separate phase gates.

## NEXT

1. Start PHASE_04 TASK-04.1 by rereading the fit registry, intended deviations, and fit thresholds.
2. Run the PHASE_04 fit gate over every registry state, preserving the recorded visual blockers and explicit deferred/excluded scope.
3. Bridge any engine demand only through a separate accepted MyEngine feature run; do not promote Shop, Tech, defeat, reward transaction, real-ad, IAP, or network Arena behavior.

## IMPLEMENTATION UPDATE — 2026-08-03

- PHASE_02 TASK-02.2 is complete: Android-free `SimulationClock` advances at exactly 20 Hz,
  `SimulationSession` delegates seeded deterministic stepping to the locked MyEngine core,
  systems use stable order/id sorting, and public snapshots expose immutable tick/hash metadata only.
- Verification: project-local runner `21 passed / 0 failed / 0 skipped`, lint `ok`, full verifier pass.
- Active composite checkout was restored to the exact `gradle/myengine.lock` SHA
  `30f4eb17aff0ea2fe6cf80aef970a1e7746dbcbb`; untracked MyEngine `.kotlin/` remains preserved.
- PHASE_02 TASK-02.3 is complete: `CommandLog` allocates monotonic IDs, rejects duplicates,
  canonicalizes with the pinned MyEngine comparator, emits delimiter-safe encoding, and exposes
  deterministic input/replay hashes through `SimulationSession`.
- Verification: project-local runner `28 passed / 0 failed / 0 skipped`, lint `ok`, full verifier pass.
- PHASE_02 TASK-02.4 is complete: RunSave is schema v3 with canonical pending command identity/metadata,
  signed deterministic state, explicit terminal invariants, and tested v1/v2 migrations; it remains
  separate from ProfileStore and Android state.
- Verification: project-local runner `34 passed / 0 failed / 0 skipped`, lint `ok`, semantic review pass, full verifier pass.
- PHASE_02 TASK-02.5 is complete: ProfileStore schema v2 canonically persists progression,
  currencies, energy, roster/loadout, tech, claims, and deterministic local service history;
  set/map/list invariants and v1 defaults are covered without mixing RunSave state.
- Verification: project-local runner `38 passed / 0 failed / 0 skipped`, lint `ok`, semantic review pass, full verifier pass.
- PHASE_02 TASK-02.6 is complete as a validation-only task: RunSave/ProfileStore migration
  branches and typed malformed/duplicate/unknown/future rejection are present and canonical
  post-migration encoding is stable.
- Verification: static migration checks pass; project-local runner `38 passed / 0 failed / 0 skipped`, lint `ok`.
- PHASE_02 TASK-02.7 is complete: five seedable Android-free fixture descriptors cover the
  accepted setup, active-wave, enhancement, and safe-victory contours plus the explicit ED-0025
  defeat blocker; blocker is non-terminal and not playable.
- Verification: project-local runner `43 passed / 0 failed / 0 skipped`, lint `ok`, full verifier pass.
- PHASE_02 TASK-02.8 is complete: replay verification compares uninterrupted and save/restore
  trajectories by ordered tick/state hash, with stable first-mismatch diagnostics and missing/
  extra/reordered detection; actual lifecycle reconstruction remains a later integration concern.
- Verification: project-local runner `50 passed / 0 failed / 0 skipped`, lint `ok`, full verifier pass.
- PHASE_02 is complete: all TASK-02.1 through TASK-02.9 are checked, the phase row is `done`,
  and PHASE_03 is now the sole `active` phase. The next work is the accepted Android contour and
  deterministic offline service boundaries; no PHASE_03 production work was mixed into PHASE_02.
- PHASE_03 TASK-03.1 is complete: accepted FR-003/FR-100–FR-107, AC-003/AC-100–AC-104,
  deviations, fit registry, and deferred/excluded scope were re-read. Next is TASK-03.2 campaign route.
- PHASE_03 TASK-03.3 is complete: accepted battle setup choices, tutorial continuation, and a
  deterministic start-battle handoff were added without inferred choice effects, copied reference
  content, or active-battle mechanics. Production commit is `c69a3e2`; tester changes remain in
  the working tree pending human delivery review.
- Verification: project-local runner `74 passed / 0 failed / 0 skipped`, lint `ok`, deterministic
  reviewer pass, semantic review pass, independent critic pass with a warning about uncommitted
  tester files, and full verifier pass. Compose UI and Android navigation smoke coverage are
  explicit exceptions because the current `:app` module lacks those test dependencies/seams.
- PHASE_03 TASK-03.4 is complete: the Android-free active-battle contour now exposes deterministic
  wave/base/enemy visibility plus speed, pause/resume, and available-build affordances. Speed has
  no multiplier, pause has no clock effect, and build selection has no cost/effect semantics.
- Verification: `:game:test`, `:app:assembleDebug`, `public-safety.ps1`, and `git diff --check`
  passed. Compose UI and Android navigation smoke coverage remain explicit exceptions because the
  current `:app` module lacks those test dependencies/seams.

- PHASE_03 TASK-03.5 is complete: the Android-free enhancement contour exposes two stable original
  offers, all-filter visibility, deterministic refresh revision, selection, and return-to-battle.
  Repeated OpenEnhancement is idempotent while the choice contour is visible; a later false-to-true
  transition creates a fresh choice session. Offer effects, costs, persistence, and reroll rules
  remain deferred.
- Delivery commits: `0e9bec0`, `e3bed14`, `aa64906`, and `06d41be`. The foundation content,
  persistence, simulation, and test sources required by the app/game composite are now tracked in
  the task chain, and the final diff-check is clean.
- Verification: clean git archive passed `:game:test`, `:app:assembleDebug`, and `:app:lintDebug`
  with 85 tests passed, 0 failed, 0 skipped, using MyEngine lock SHA
  `30f4eb17aff0ea2fe6cf80aef970a1e7746dbcbb`. Deterministic reviewer, semantic review,
  independent critic, and full verifier passed. Compose UI and Android navigation smoke coverage
  remain explicit exceptions because the current `:app` module lacks those dependencies/seams.
- Scoped fit for `ST-0004/BATTLE-ENHANCEMENT` was skipped: reference `EV-0041` is preserved_unusable
  with an invalid PNG, so no visual score or divergence was invented.
- PHASE_03 TASK-03.6 is complete: the accepted playable contour now reaches a deterministic local
  Victory surface with an immutable reward-panel snapshot after enhancement return-to-battle;
  ED-0025 remains a structured non-terminal defeat blocker and no defeat mechanic was added.
- Delivery commits are `c04b175` (`feat: add safe victory reward panel`) and `c6aa856`
  (`test: strengthen victory contour coverage`). Fresh installed APK manual smoke reached
  campaign -> setup -> active battle -> enhancement -> return -> Resolve victory -> Victory/reward
  panel. Reward claim, doubling, transaction, economy, and service semantics remain deferred.
- Verification: MP runner `91 passed / 0 failed / 0 skipped`, lint ok; public-safety pass;
  `test :app:assembleDebug` successful; full reviewer, semantic review, independent critic, and
  verifier passed. Compose/navigation smoke remains an explicit dependency exception. Scoped fit
  skipped because `ST-0005` reference evidence is preserved_unusable.

## BLOCKERS

- Gate 1 is accepted under relaxed policy; no evidence-policy blocker remains.
- Shop, Tech, reward transaction semantics, Arena network, and unobserved mechanics are deferred and cannot create requirements.
- Legacy invalid PNGs, incomplete visual signatures, and Canvas child bounds are non-blocking for Gate 1 and remain work for per-surface Visual Fit Gates.
- Gate 2 is accepted in relaxed semantic scope; implementation and Visual Fit remain phase-gated.
- Push of `c04b175` and `c6aa856` to `origin` was attempted after explicit approval but failed with
  GitHub `Invalid username or token`; commits remain local and no further credential retry was made.

## VERIFICATION

- Independent corpus audit -> pass for existence and SHA-256 (276 indexed artifacts; 137 screenshots, 138 UI dumps, 1 metadata record; zero missing or mismatched files).
- Action trace audit -> pass (25 unique edge traces with screenshot/UI-dump pairs; six empty plateau iterations); graph edges contain before/after screenshot anchors.
- State graph schema audit -> pass against the draft 2020-12 constraints used by state-graph.v1.schema.json.
- Signature/bounds audit -> deferred to Visual Fit Gates (7 nodes with usable visual evidence, 1 valid anchor with signature deferred, 5 preserved-unusable legacy contours; all 26 affordances carry a structured bounds-unavailable reason).
- `scripts/validate-evidence.ps1` -> pass in the accepted-state branch (13 nodes, 25 edges, 12 observations; Gate 1 status `accepted`).
- `scripts/validate-spec.ps1` -> pass for the relaxed Gate 2 bundle (16 FR, 10 US, 10 AC, 16 trace rows).
- `scripts/evaluate-gate2.ps1` -> pass in relaxed mode (16 FR, 10 US, 10 AC, 16 trace rows, 13/13 fit-registry nodes).
- `scripts/public-safety.ps1` -> pass (rechecked at Gate 1; no raw reference artifact is tracked).
- `.\gradlew.bat :game:test :app:assembleDebug` with JDK 17 and Android SDK -> pass.
- MyEngine `scripts/me-selfcheck.ps1` in the isolated backlog worktree -> pass.
- Independent `me-verifier` -> pass after strengthening Gate 1 evidence references and creative
  asset provenance checks.
- Relaxed-policy ready-state validation -> pass; explicit terminal and negative-access blockers are machine-validated, deferred scope cannot promote before human acceptance, and visual evidence is tiered per node.
- Independent relaxed-policy `me-verifier` -> pass after structured blocker, evidence-tier, bounds-reason, and deferred-scope enforcement checks.

## RECOVERY CRAWL UPDATE — 2026-07-19

- The 11 pre-existing recovery screenshots and paired UI dumps were audited. All PNG probes passed binary validation, but they were not promoted because the trace contains no action-level route/state linkage; repeated battle frames and one launcher frame were retained as `probe_not_promoted`.
- A further 23 binary-safe captures were made on the authorized Pixel 9 AVD. `EV-0233` is a validated fresh anchor for `ST-0001`; `EV-0243` is a validated fresh anchor for `ST-0003`. Both use fresh DCT pHash signatures; no graph edge was promoted.
- All 34 recovery screenshots are indexed with paired UI evidence mappings and SHA-256 values. Legacy 95 screenshots remain untouched. Technical valid PNG total is 42; eight nodes have valid anchors, seven currently support visual claims, and complete before/after visual pairs remain deferred.
- A test-ad surface appeared during a probe and was closed immediately. No rewarded-ad completion, purchase, credential entry, Arena request, or unsafe defeat forcing was performed.
- The recovery crawl itself ended blocked; the relaxed policy was accepted for Gate 1 and Gate 2. Production implementation, backlog bridge, and Visual Fit remain phase-gated.

## IMPLEMENTATION UPDATE — 2026-08-10

- PHASE_03 TASK-03.7 is complete: roster and local-settings open/close contours are wired through the Android-free `RosterSession` and `CampaignSession`; upgrade and toggle intents remain visible-affordance no-ops, and immutable snapshot boundaries are covered.
- Delivery commit: `d86a819` (`test: cover roster and settings contours`); the production contour was already present in `230aad4`.
- Verification: deterministic reviewer, semantic review, independent critic, full verifier, and MP runner passed; runner reported `104 passed / 0 failed / 0 skipped`, lint `ok`. `test :app:assembleDebug` and `scripts/public-safety.ps1` passed with 110 tracked files and 886 history paths checked.
- Scoped fit for `ST-0007/ROUTE-TROOPS` and `ST-0008/OVERLAY-SETTINGS` returned overall score `50` (screen scores `34` and `66`) with major structure/geometry divergences. Proposed fit SPECs were not written; creative, copy, and accessibility deviations remain acknowledged.
- Push of `d86a819` was attempted after explicit approval but failed with GitHub `Invalid username or token`; no further credential retry was made.
- PHASE_03 TASK-03.8 is complete: the Android-free service boundary now exposes
  `RewardedOpportunityService`, `PurchaseCatalogService`, and `ArenaService` through deterministic
  local adapters. Accepted service-shaped requests preserve affordance/result shape while reward
  claim/doubling, purchase transactions, real-ad completion, account behavior, and Arena network
  matches remain deferred or blocked.
- Delivery commits: `57f3f25`, `150d6ed`, `2348bdc`, `caaaa93`, `b3ed42f`, and `f603ed0`.
  The later fixes harden runtime-unmodifiable configuration/snapshot collections and remove the
  public `copy` mutation bypass from the purchase catalog snapshot.
- Verification: MP runner `109 passed / 0 failed / 0 skipped`, lint ok; `scripts/public-safety.ps1`
  pass; `test :app:assembleDebug` successful with the Android Studio JBR and Android SDK; full
  verifier, semantic review, independent critic, and deterministic reviewer passed. No app wiring
  or screenshot record was required for this domain/data task.
- Next accepted task is PHASE_03 TASK-03.9 Reward adapter; push for TASK-03.8 was not attempted.
- Push for TASK-03.8 was attempted after explicit approval but failed with GitHub `Invalid username or token`;
  no further credential retry was made and all delivery commits remain local.

## IMPLEMENTATION UPDATE — 2026-08-28

- PHASE_03 TASK-03.9 is complete: the Android-free reward boundary now exposes deterministic
  `NORMAL_REWARD` and `MULTIPLIER_SHAPED` outcome shapes for accepted and blocked requests without
  applying claim, ad-completion, multiplier, transaction, economy, SDK, account, payment, or
  network semantics.
- Delivery commits: `de2a611` and `07bcde6`.
- Verification: scoped runner `112 passed / 0 failed / 0 skipped`; full runner `112 passed / 0 failed / 0 skipped`,
  lint ok, `:app:assembleDebug` successful; public-safety pass (113 tracked files, 850 history paths,
  zero creative assets); deterministic reviewer, semantic review, independent critic, and full verifier passed.
- Next accepted task is PHASE_03 TASK-03.10 Arena adapter. Push was not possible because `GITHUB_TOKEN`
  is not set; no credential retry was made.

## TESTING UPDATE — 2026-08-29

- Added Fakes-only Arena coverage for the accepted `CampaignSession` route, unfinished-run guard,
  deterministic repeated snapshots, and propagation of a blocked service-shaped snapshot.
- Extended service-boundary coverage for unknown Arena requests, immutable local/blocked snapshots,
  and the absence of network, account, match, production-integration, and authoritative-state
  behavior. Existing campaign, integration, and service tests were reviewed and their assertions
  were retained.
- Verification executed after the repair: scoped runner `118 passed / 0 failed / 0 skipped`; full
  runner `118 passed / 0 failed / 0 skipped`, lint `ok`; deterministic reviewer, semantic review,
  independent critic, and full verifier passed. Compose/navigation coverage remains an explicit
  dependency exception because `:app` has no Android test dependencies; the public
  `CampaignScreenContent` seam was confirmed.

## IMPLEMENTATION UPDATE — 2026-08-29

- PHASE_03 TASK-03.10 is complete: the offline service-shaped Arena state is wired from the
  campaign surface through `CampaignSession` and the Android UI, while local-service-shaped and
  network-match-blocked states remain explicit, immutable, deterministic, and offline-only.
- Delivery commits: `24d553d` (`feat: add offline Arena route`) and `e793697`
  (`fix: repair Arena integration test nullability`). Both were pushed successfully to
  `origin/main`.
- TASK-03.10 is archived in `.claude/specs/done`; no successor task or chain was started.

## VISUAL QA UPDATE — 2026-08-29

- PHASE_03 TASK-03.13 is complete: device-backed structural evidence and fit records were added
  for campaign, roster, settings, local Arena, and resume surfaces; no product-surface rewrite was
  introduced because the routes already existed.
- Delivery commits: `f2658e4`, `655cf66`, `c05d9c0`, `7e84371`, and board close-out `d7924b4`.
- Verification: final connected `ResumeContentUiTest` `1 passed / 0 failed / 0 skipped`; full MP
  runner `118 passed / 0 failed / 0 skipped`, lint `ok`; public-safety pass. No aggregate fit score
  was produced or claimed: five visual cells remain explicitly deferred/uncheckable because the
  Pixel 9/reference and emulator profiles mismatch, ImageMagick pixel comparison is unavailable,
  and relevant reference captures are preserved-unusable/unreadable; structural evidence remains
  recorded.
- TASK-03.13 is archived in `.claude/specs/done`. The final tester assertion remains an intentional
  uncommitted worktree change and was included in the final connected run.
- Next backlog task: PHASE_03 TASK-03.14 Lifecycle.

## LIFECYCLE RESTORATION UPDATE — 2026-08-29

- PHASE_03 TASK-03.14 is complete: campaign run-save persistence now covers background,
  Activity recreation, and process-death restoration for active and victory contours, with
  Android-free authority retained in `CampaignSession` and encoded storage isolated in the app.
- Delivery commits: `54d5d45`, `e6deac9`, and board close-out `505d347`; all pushed to `origin/main`.
- Verification: JVM `125 passed / 0 failed / 0 skipped`; connected `11 passed / 0 failed / 0 skipped`;
  public-safety pass; `test :app:assembleDebug` successful. Semantic reviewers stalled twice,
  so deterministic review and the full verifier evidence were used as the recorded fallback.
- TASK-03.14 is archived in `.claude/specs/done`. The next runnable backlog remains TASK-03.15
  Acceptance, followed by TASK-03.16 Progress.

## PHASE_03 CLOSE-OUT — 2026-08-29

- TASK-03.9 Reward adapter — commits `de2a611`, `07bcde6`; scoped and full runners each reported `112 passed / 0 failed / 0 skipped`, lint ok, `:app:assembleDebug` and public-safety passed, and deterministic reviewer, semantic review, independent critic, and full verifier passed. Push was unavailable because `GITHUB_TOKEN` is not set.
- TASK-03.10 Arena adapter — commits `24d553d`, `e793697`; the offline service-shaped Arena route is wired through campaign/session/UI with immutable local and network-match-blocked states. Existing evidence records 118 tests passed, lint ok, verifier pass, and successful push to `origin/main`.
- TASK-03.11 core visual QA — commit `a07c6d5`; connected `emulator-5554` evidence for ST-0001/ST-0002/ST-0003 records structural coverage and FIT-03.11-001/002/003. No pixel score was claimed: the Pixel 9 reference profile differs from the emulator profile and ImageMagick was unavailable; ST-0002 remains blocked by preserved-unusable reference PNGs.
- TASK-03.12 enhancement/victory visual QA — commits `8054578`, `495833a`; instrumented Compose checks passed 2/2, deterministic/semantic/critic/verifier gates passed, and visual fit was skipped because ST-0004/ST-0005 references are preserved-unusable. FIT-03.12-001/002 remain explicit; deferred reward claim, doubling, transaction, and economy semantics were unchanged.
- TASK-03.13 meta/service/resume visual QA — commits `f2658e4`, `655cf66`, `c05d9c0`, `7e84371`; final connected `ResumeContentUiTest` passed 1/1, full MP runner reported `118 passed / 0 failed / 0 skipped`, lint ok, and public-safety passed. No aggregate fit score was produced or claimed; five visual cells remain deferred/uncheckable because relevant references include preserved-unusable captures, the Pixel 9/reference and emulator profiles mismatch, and ImageMagick pixel comparison is unavailable. FIT-03.13-001 through FIT-03.13-005 remain explicit and no pixel score was claimed.
- TASK-03.14 lifecycle restoration — commits `54d5d45`, `e6deac9`, with board close-out `505d347`; JVM `125 passed / 0 failed / 0 skipped`, connected `11 passed / 0 failed / 0 skipped`, public-safety pass, and `:app:assembleDebug` successful. Deterministic review and full verifier were the recorded fallback after two semantic-review stalls.
- TASK-03.15 phase acceptance — existing contour delivery was rechecked without new production work: MP runner `125 passed / 0 failed / 0 skipped`, lint ok; connected `11 passed / 0 failed / 0 skipped` on `Pixel_5(AVD) - 14`; public-safety `pass` (`124` tracked files, `1156` history paths); spec validator `pass` (`16` requirements, `10` stories, `10` acceptance, `16` trace rows); `git diff --check` passed.
- PHASE_03 exit verification — TASK-03.9 through TASK-03.15 are complete, all accepted semantic routes and service-shaped boundaries are verified offline, lifecycle restoration preserves the Android-free deterministic authority, and all requested per-screen structural QA records exist. Remaining visual parity blockers are the FIT-03.11-001/002/003, FIT-03.12-001/002, and FIT-03.13-001..005 records, including preserved-unusable references, profile mismatch, unavailable ImageMagick pixel comparison, and explicitly uncheckable visual cells. Deferred/excluded Shop, Tech, defeat, reward transaction, real-ad, IAP, account, network Arena, and external-exit behavior remains frozen.
- Transition verified — PHASE_03 is `done` and PHASE_04 is the sole `active` phase in `docs/implementation_plan/PROGRESS.md`; the next work is the PHASE_04 fit gate and deferred-scope closure.
