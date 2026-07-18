# Risks

| ID | Risk | Probability | Impact | Mitigation |
|---|---|---:|---:|---|
| RISK-001 | Store listing overstates or omits mechanics | High | High | Luna crawl + Gate 1; listing facts remain candidates |
| RISK-002 | Animated/volatile battle frames explode the state graph | High | High | Three signatures + volatile observations + masks |
| RISK-003 | Reference IP leaks into a public commit/history | Medium | Critical | local-only corpus, public-safety history gate, original-only deviations |
| RISK-004 | Sandbox orchestration becomes a game dependency | High | High | ENG-036 reusable runtime/session extraction before gameplay |
| RISK-005 | New game demand inflates engine backlog from guesses | High | Medium | No demand bump/new ENG-037+ before Gate 1 |
| RISK-006 | Meta save and run save become coupled | Medium | High | Separate versioned stores and migration matrices |
| RISK-007 | Fit target conflicts with accessibility | Medium | Medium | explicit a11y deviations; 48 dp minimum |
| RISK-008 | Service-shaped UI accidentally reaches real services | Low | Critical | interface boundary, deterministic fakes, no SDK dependencies |
| RISK-009 | Pixel 9 performance target lacks observed load | High | Medium | concurrency observation +25%, device profiling before claim |
| RISK-010 | Full implementation is attempted before evidence gates | Medium | High | manifest blocks handoff; scaffold-only app before Gate 2 |
