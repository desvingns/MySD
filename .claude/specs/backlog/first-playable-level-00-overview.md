# Первый playable level — epic overview

Status: backlog
TASK: feature
PLATFORM: android
WHAT: Превратить существующий маршрут `Launch → Campaign → Level Setup → Battle` в настоящий минимальный игровой уровень. Игрок видит вертикальное поле, получает пассивный ресурс, строит один тип башни на трёх фиксированных точках, улучшает башни и наблюдает конечную волну врагов с победой или поражением. Референсный скриншот используется только для общей композиционной идеи; публичное содержание MySD остаётся оригинальным.
LAYERS: domain, data, presentation
CHANGED_HINT:
- `game/src/main/kotlin/dev/mysd/game/persistence/RunSave.kt` — довести playable-run persistence от contour-only save до полного state payload — G7
- `game/src/main/kotlin/dev/mysd/game/campaign/CampaignSession.kt` — перевести restore с marker-only contour на authoritative playable payload — G2, G7
- `app/src/main/kotlin/dev/mysd/android/MainActivity.kt` — сохранить lifecycle boundary, но читать и писать уже canonical full run payload — G1, G7
TEST_TYPES: unit, integration, replay, persistence, instrumented-compose-ui, screenshot
CONSTRAINTS:
- Фича расширяет существующий `stage-ember-path`, не добавляет новый debug-route — G2, D1.
- Главная база отдельна от трёх фиксированных build tiles — D8.
- В MVP ровно один тип башни, одна семья врагов и одна конечная волна из примерно 8–10 врагов — D5, D9.
- Ресурс глобальный и пассивно растёт во время активной симуляции; отдельной ресурсной постройки нет — D10.
- Башни автоматически атакуют ближайшего врага в радиусе; manual targeting не добавляется — D11.
- Все ID, числа, графика и текст оригинальные для MySD; содержимое присланного скриншота не копируется — G6, G11.
- RunSave и ProfileStore остаются разделёнными; playable battle state не переносится в profile boundary — G7.
- Остаток бывшего SPEC-06 делится на codec → headless restore/replay → campaign/android lifecycle, после чего UI и acceptance идут как отдельные SPEC-ы.

Epic: first-playable-level
Order: 00 of 10
Depends-on: —
Date: 2026-09-03

## Goal

Дать проекту первый короткий, но настоящий игровой цикл: открыть уровень, дождаться ресурса, поставить башни, улучшить их, увидеть движение и уничтожение врагов, а также проверить победу и поражение. Текущая кампания и setup flow сохраняются; меняется только переход от декоративного battle contour к функциональной детерминированной игре.

## Locked decisions

- D1: использовать существующий `stage-ember-path` и текущий campaign route.
- D2: поставляем playable level, а не ещё один UI-only contour.
- D3: portrait top-to-bottom battlefield с простой оригинальной графикой.
- D4/D8: три фиксированные точки строительства плюс отдельная главная база.
- D5: один тип башни и одна семья врагов.
- D6/D10: глобальный пассивный ресурс для строительства и улучшений.
- D7: враг, достигший башни, уничтожает её и продолжает путь к базе.
- D9: одна конечная волна примерно из 8–10 врагов; зачистка даёт победу, уничтожение базы — поражение.
- D11: автоматическая атака ближайшей цели в радиусе с фиксированным cooldown.
- D12: на занятой точке открывается popup с двумя последовательными улучшениями.
- D13: уже shipped contour-only lifecycle из PHASE_03 переиспользуется как boundary; split не открывает новый meta/UI scope.
- (assumption): конкретные значения ресурса, HP, урона, cooldown и скорости выбираются как оригинальный balance baseline и закрепляются fixtures.
- (assumption): пауза останавливает simulation time и пассивное начисление ресурса.

## Design capsule

- Почему split нужен сейчас: остаток SPEC-06 одновременно трогает schema migration, authoritative battle restore, replay continuity, campaign resume и Android lifecycle. Это честные 24 acceptance cells, и текущий seam уже показывает три разных риска.
- Slice 06 фиксирует wire contract: полный PlayableBattleState payload, schema versioning, legacy-safe decode и validation без попытки сразу восстанавливать campaign/UI flow.
- Slice 07 фиксирует Android-free restore: playable session должен подниматься из saved payload и либо продолжать тот же hash trajectory, либо оставаться terminal-frozen для defeat.
- Slice 08 подключает уже готовый payload к `CampaignSession` и Android storage lifecycle без добавления нового gameplay или UI scope.
- Carry-over: battlefield rendering и final acceptance остаются теми же работами, просто становятся 09 и 10 после split.

## SPECs (run via `/mp --feature --next` in Order)

| Order | File | Depends-on | Layers | Summary |
|---|---|---|---|---|
| 01 | `first-playable-level-01-content.md` | — | domain, data | Versioned content contract for the original first level. |
| 02 | `first-playable-level-02-runtime-economy.md` | 01 | domain | Fixed 20 Hz battle state and passive resource income. |
| 03 | `first-playable-level-03-build-placement.md` | 01–02 | domain | Three fixed slots, affordability, and construction command. |
| 04 | `first-playable-level-04-tower-upgrades.md` | 03 | domain | Two sequential tower upgrades and deterministic stat changes. |
| 05 | `first-playable-level-05-enemy-combat-outcomes.md` | 02–04 | domain | Enemy wave, movement, combat, destruction, victory, defeat. |
| 06 | `first-playable-level-06-save-replay-lifecycle.md` | 05 | domain, data | Versioned RunSave payload for complete PlayableBattleState plus legacy-safe migration and validation. |
| 07 | `first-playable-level-07-playable-battle-restore.md` | 06 | domain, data | Restore a playable session from saved payload and prove replay continuity or terminal freeze headlessly. |
| 08 | `first-playable-level-08-campaign-lifecycle-restore.md` | 07 | domain, data, presentation | Reconnect CampaignSession and Android storage lifecycle to active/defeat playable saves. |
| 09 | `first-playable-level-09-android-battle-ui.md` | 08 | presentation | Real battlefield UI, popup interactions, ticker, and rendering. |
| 10 | `first-playable-level-10-acceptance-balance.md` | 09 | domain, presentation | Full acceptance, balance, and device verification. |

## Why this ordering

Сначала фиксируется data contract сохранения, чтобы restore не зависел от временных marker-only hacks. Затем отдельно закрывается Android-free authoritative restore и replay continuity: это самый дешёвый способ доказать deterministic continuity без вмешательства Compose или lifecycle. Только после этого campaign/session и Android storage boundary подключаются к полному payload. Battlefield UI остаётся после persistence seam, а финальная acceptance/balance работа остаётся последним замыкающим SPEC-ом.

## Verified seam for this split

- `RunSave` v3 уже хранит `seed`, `rngState`, `tick`, `pendingCommands`, `modifiers` и `terminalResult`, но не хранит полный `PlayableBattleState` payload.
- `CampaignSession` сейчас восстанавливает только contour markers через `modifiers` и знает `ACTIVE`, `ENHANCEMENT`, `VICTORY`; defeat restore path для playable payload отсутствует.
- `PlayableBattleState` уже содержит resource, slots, tower levels/stats, enemies, base HP, wave counters и `terminalResult`, включая строгие invariants для victory/defeat.
- `PlayableBattleSession` уже умеет не двигаться после terminal state; этот freeze должен стать восстановимым после save/load, а не только во время uninterrupted run.
- Существующие lifecycle tests покрывают active/victory contour старого seam; playable defeat lifecycle и full-state restore ещё не закрыты.

## Key facts (verified)

- G1: `MainActivity` создаёт `CampaignSession`, читает `RunSave` и передаёт snapshots/intents в `CampaignScreen` — `app/src/main/kotlin/dev/mysd/android/MainActivity.kt:20-82`.
- G2: `CampaignSession` владеет campaign route и создаёт `ActiveBattleSession` после `StartBattle` — `game/src/main/kotlin/dev/mysd/game/campaign/CampaignSession.kt:91-137`, `:242-309`.
- G3: текущий `ActiveBattleSession` меняет только speed/pause/build flags и не содержит gameplay — `game/src/main/kotlin/dev/mysd/game/battle/ActiveBattleSession.kt:39-133`.
- G4: текущий Canvas/HUD уже имеет точку расширения, но `MainActivity` обновляет snapshot только после intent — `app/src/main/kotlin/dev/mysd/android/campaign/CampaignScreen.kt:310-484`; `app/src/main/kotlin/dev/mysd/android/MainActivity.kt:35-82`.
- G5: Android-free `SimulationSession` и `SimulationClock` дают seeded deterministic 20 Hz stepping — `game/src/main/kotlin/dev/mysd/game/simulation/SimulationSession.kt:23-103`; `game/src/main/kotlin/dev/mysd/game/simulation/SimulationClock.kt:3-39`.
- G6: original content IDs и typed content catalog уже существуют, но balance и display text в catalog не входят — `game/src/main/kotlin/dev/mysd/game/content/ContentCatalog.kt:3-99`.
- G7: `RunSave` v3 хранит seed/RNG/tick/commands/terminal, а `ProfileStore` отдельно хранит currencies/progression — `game/src/main/kotlin/dev/mysd/game/persistence/RunSave.kt:21-72`; `game/src/main/kotlin/dev/mysd/game/persistence/ProfileStore.kt:3-16`.
- G8: JVM JUnit 5 и Android instrumented/Compose test dependencies уже подключены — `game/build.gradle.kts:17-30`; `app/build.gradle.kts:34-49`.
- G9: scenario catalog уже содержит setup, active wave, enhancement, victory и structured defeat blocker — `game/src/main/kotlin/dev/mysd/game/simulation/ScenarioFixtures.kt:5-74`.
- G10: roadmap прямо предусматривает один stage, base, production building, allied unit, enemy family, victory и defeat, с replay/save gates — `docs/implementation/ROADMAP.md:58-77`.
- G11: старый Gate 2 bundle откладывал exact economy и defeat mechanics; этот epic является новым explicit feature decision — `spec/requirements.md:122-127`; `.ai/handoff.md:22-43`.

## Implementation links
- commit: —
- files: —
