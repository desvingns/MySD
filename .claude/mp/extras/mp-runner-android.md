# mp-runner-android — MySD overrides

- MySD has NO `detekt` and NO `lint` task on `:app`. `./gradlew :app:detekt` fails
  with "task 'detekt' not found". Treat any detekt/lint failure from the runner as
  `n/a`, not `failed` — it is a false pipeline failure.
- Real gates (per AGENTS.md): `./gradlew test :app:assembleDebug` plus on-device
  `./gradlew :app:connectedDebugAndroidTest`.
- `local.properties` is gitignored and may be absent in a fresh shell; create it
  with `sdk.dir` before running gradle. No `ANDROID_HOME` in orchestrator env.
- Debug APK is not byte-reproducible (expected). QA-record APK SHA documents
  capture provenance only (same pattern as TASK-03.11).
