# MySD Self-Improvement Lessons

Source: `selfimprove/retro/retro-2026-08-10.md`
Applied: 2026-08-10

## Lesson 1 — Require enough runs before acting on pass-rate

- status: APPLIED
- rule: Treat an agent pass-rate as actionable only when the sample contains at least three runs. Smaller samples are observations, not conclusions.
- application: The `semantic-review` rate was investigated because it had 3 runs; the single `fit` partial was kept as a follow-up observation.
- evidence: Historical retro showed `semantic-review` at 2/3 pass and `fit` at 0/1 partial.

## Lesson 2 — Record the concrete failure cause

- status: APPLIED
- rule: Every fail or partial must be grouped by task, correlation, concrete metric, root cause, and recovery result before becoming a lesson. Do not copy the retro checklist as a lesson.
- application: The semantic-review miss is recorded as canonical decode and migration coverage failure in TASK-02.4, followed by a successful auto-fix retry.
- evidence: `selfimprove/runs/2026-08.jsonl`, correlation `20260803-mp-phase02-task02-4`.

## Lesson 3 — Create an eval candidate before changing prompts

- status: APPLIED_AS_EVAL_GATE
- rule: Do not change prompts or pipeline instructions from low-feedback evidence alone. First create a reproducible eval candidate and require it to pass.
- application: The candidate below is queued; no prompt or skill change is made before it passes.

### Eval candidate EC-2026-08-10-semantic-review-run-save-migration

- status: QUEUED
- target: `semantic-review`
- scenario: Review TASK-02.4 RunSave migration and canonical encoding/decoding behavior, including v1/v2 migration coverage and malformed/future rejection.
- regression_signal: The reviewer must identify missing canonical decode and migration coverage when either contract is broken.
- pass_criteria: The review reports the concrete failure locations and required coverage; a corrected implementation passes the same review without weakening assertions.
- evidence: `selfimprove/runs/2026-08.jsonl`, correlation `20260803-mp-phase02-task02-4`.
- prompt_changes: NONE until this candidate is executed and passes.
