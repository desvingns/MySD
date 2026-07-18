# Security and Privacy

The first release has no account, production payment, ad SDK, analytics SDK, or Arena backend.
Local saves contain game progression only.

- No reference APK, raw media, UI dump, credential, token, or personal data enters git.
- Local service fakes never collect identifiers or make network calls.
- Imported content and saves are validated, versioned, and bounded before allocation.
- Debug/export tools redact local absolute paths from public evidence where practical.
- Future production services require a new security/privacy review and explicit scope decision.
