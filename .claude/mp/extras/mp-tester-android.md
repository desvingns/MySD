# mp-tester-android — MySD overrides

instrumented (androidTest) compose-ui tests — hard rules (D8/R8 will reject
otherwise, and this is only caught by a real gradle compile):

- Test method names must be plain Java identifiers. NO backtick names with spaces
  in androidTest — "Space characters in SimpleName not allowed prior to DEX
  version 040". Backtick-with-spaces is JVM-unit-test only.
- Runner import is exactly `androidx.test.ext.junit.runners.AndroidJUnit4`
  (not `...junit4.runners...`).
- `onNode` / `onAllNodes(SemanticsMatcher)` are members of the compose test rule
  (SemanticsNodeInteractionsProvider) — no top-level import exists.
- Never claim "signatures verified" without an actual `./gradlew ...AndroidTest`
  compile. This tester asserted verification 3x while dex compilation failed.
