# Luna Crawl Runbook

Status: ready for a separate Pixel 9 observation session
Reference package: `com.gdzsq.crazy_td`
Raw output root: `D:\Pet\MySD\.reference-local` (gitignored)
Sanitized output root: `D:\Pet\MySD\spec\evidence`

## Purpose

Build an evidence-backed, hierarchical state graph for the complete observable offline contour of
the reference game. The crawl records what the app does; it does not decide what MySD should copy.
Sol performs the separate authoring step after Gate 1.

## Ready-to-paste prompt for Luna

```text
Ты — Luna, observer/reverse-spec crawler. Работай с установленным reference game
com.gdzsq.crazy_td на Pixel 9 в Android Studio Emulator. Репозиторий результата:
D:\Pet\MySD.

Цель: наблюдением собрать полный offline state graph, evidence registry и mechanic-claim ledger.
Не проектируй MySD, не пиши требования и не делай выводы без evidence. Raw screenshots, видео,
UI dumps и trace сохраняй ТОЛЬКО в D:\Pet\MySD\.reference-local\; эта папка gitignored.
В spec\evidence разрешены только обезличенные факты, измерения, хэши, короткие семантические
описания и ссылки на evidence IDs — без чужих изображений, ассетов и дословных UI-текстов.

Перед первым действием:
1. Прочитай AGENTS.md, docs\reference\LUNA_CRAWL_RUNBOOK.md,
   docs\reference\EVIDENCE_CONTRACT.md и spec\evidence\state-graph.v1.schema.json.
2. Заполни .reference-local\capture-meta.json: package, versionName/versionCode, locale,
   Android/API, Pixel 9 profile, resolution, density, font scale, install source и captured_at_utc.
3. Зафиксируй чистый launch evidence pair: screenshot + UI dump; вычисли SHA-256.
4. Не меняй font scale/locale/resolution после первого capture. Не используй реальные credentials.

Для каждого стабильного состояния:
- capture before и after: screenshot, UI dump, timestamp, optional 5–15 s clip;
- создай/обнови node в .reference-local\traces\state-graph.v1.json;
- тип node только screen | overlay | battle_phase | meta_state;
- задай parent, route, phase, semantic_flags, visible_affordances, evidence_ids, confidence;
- structural_signature = activity + нормализованные affordances без currency/HP/wave/timer;
- visual_signature = perceptual hash после маски валют, таймеров, HP и анимации;
- semantic_signature = route + overlay + battle phase + значимые flags;
- точные HP/currency/wave/timer записывай в observations, не создавай узел из-за нового числа.

Для каждого действия создай edge с action, preconditions, cost observation, observed_effect,
wait/timing, before/after evidence, source observed|inferred и confidence. Каждый видимый
интерактивный элемент должен иметь edge, explicit deviation или blocker.

Скрытые правила записывай отдельно в spec\evidence\mechanic-claims.csv: claim, hypothesis,
controlled variables, sample count, supporting/contradicting evidence, confidence. Claim не
становится requirement. Confidence < 0.8 обязательно переноси в open-questions.md.

Маршрут:
1. Clean launch, onboarding, permissions, settings, system Back.
2. Все root tabs и каждый affordance: Shop → Troops → Battle → Tech → Arena.
3. Battle selection: level, energy, start, sweep, rewards, locked/available stages.
4. Полный бой: pre-wave, wave, pause/resume, x2, buildings, allied production, enhancement
   choice/refresh, base damage, victory и defeat.
5. Meta: unlock, troop/loadout, building upgrades, tech DAG, currencies, claim/spend,
   insufficient-resource.
6. Повтори один ранний бой минимум трижды; меняй одну controlled variable за эксперимент.
7. Purchases, настоящие ads и Arena network НЕ запускай. Зафиксируй affordances/guards как
   blocked/service_adapter, затем вернись.

После каждого шага обновляй coverage.md. Остановись только когда:
- достигнуты все root tabs;
- core loop дошёл до terminal state;
- все affordances сопоставлены;
- сняты positive и negative states;
- шесть итераций подряд не появляется новый state/affordance/mechanic claim;
- все inference < 0.8 находятся в open questions.

Не коммить и не пушь. В конце:
1. запусти powershell.exe -File scripts\validate-evidence.ps1;
2. подготовь sanitized state graph в spec\evidence\state-graph.v1.json без raw media/text;
3. выдай JSON-отчёт: verdict, captured_states, edges, root_tabs_seen, terminal_states_seen,
   unmatched_affordances, low_confidence_claims, blockers, raw_root, sanitized_files.
4. остановись на Gate 1 и передай результат Sol; не начинай авторинг production spec.
```

## File layout

```text
.reference-local/
  capture-meta.json
  screenshots/<EVIDENCE_ID>.png
  ui-dumps/<EVIDENCE_ID>.xml
  recordings/<TRACE_ID>.mp4
  traces/trace.jsonl
  traces/state-graph.v1.json
  coverage.md
spec/evidence/
  capture-meta.example.json
  evidence-index.csv
  state-graph.v1.json
  mechanic-claims.csv
  open-questions.md
```

Raw filenames use evidence IDs, not screen names or copied UI text.

## Capture protocol

For every action:

1. Relaunch/replay the shortest known path to the source state.
2. Wait until the transition/animation is stable; record the wait separately.
3. Capture `before` screenshot and UI dump.
4. Perform exactly one semantic action.
5. Capture `after` screenshot and UI dump.
6. Hash files with SHA-256 and append a row to the local evidence index.
7. Deduplicate using all three signatures. A visual-only match never overrides a semantic mismatch.
8. If an action changes only a volatile number, add an observation to the existing node.

Use bounds in dp. Derive them from captured density; never estimate dp from pixels without recording
the conversion.

## State hierarchy

- `screen`: root or route-level surfaces such as battle selection, troops, tech, shop, arena,
  settings.
- `overlay`: modal/partial surfaces such as enhancement choice, pause, reward, confirmation,
  insufficient resource.
- `battle_phase`: pre-wave, active wave, paused, enhancement, victory, defeat under the battle
  screen.
- `meta_state`: locked/unlocked, affordable/unaffordable, energy available/empty,
  reward unclaimed/claimed under the owning screen.

Parent relationships express containment, not navigation. Edges express navigation and state
transitions.

## Guardrails

- Never buy, subscribe, authenticate, enter personal data, complete a real ad, or join network
  Arena.
- Do not extract or decompile assets.
- Do not copy marketing or UI prose into the public repo. Summarize semantics.
- A blocked external service is a valid terminal observation, not a crawl failure.
- Preserve all prior captures. If a capture is superseded, mark it `superseded`; do not delete it.

## Handoff contract

Luna hands Sol:

- `capture-meta.json`;
- sanitized `state-graph.v1.json`;
- evidence index with hashes;
- mechanic claims and open questions;
- coverage report and blockers;
- a compact Gate 1 inventory table.

Sol must reject the handoff when a root tab or terminal battle state is missing, any visible
affordance is unmatched, or low-confidence inference leaked into a requirement candidate.
