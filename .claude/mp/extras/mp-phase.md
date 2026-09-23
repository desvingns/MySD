# MP workflow override

- When the user explicitly invokes `--phase` or `--feature --next`, treat the workflow as SPEC-approved and do not ask the separate `SPEC ok?` confirmation gate.
- Preserve all later safety, verification, human checklist, and push gates.
- This override is a user preference for this project and applies to both Codex and Claude MP runs.
