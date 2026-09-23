# Product Brief

Status: **full-product human decision active; integrated implementation in progress**

## Замысел

MySD (working identity: Emberwatch) — мобильная tower-defense стратегия с короткими
campaign-сессиями и долгосрочным развитием.
Цель reverse-spec — воспроизвести наблюдаемую структуру экранов, core loop, темп и UX референса,
сохранив полностью оригинальный мир, визуальный язык, звук, тексты и баланс.

## Аудитория

Игроки Android, которым нужен понятный offline TD loop, короткая сессия и видимая мета-прогрессия.
Точная длительность сессии, глубина управления и объём контента определяются после crawl.

## Ценность

- детерминированный offline core;
- быстрый вход в бой и читаемый прогресс;
- campaign, roster и technology surfaces в одном локальном профиле;
- отсутствие обязательной сети, платежей и рекламы в первой версии.

## In scope

- весь подтверждённый наблюдением offline-контур и явно утверждённый 16.09.2026 оригинальный
  full-product contour;
- шесть оригинальных этапов по десять волн, несколько defensive/allied/enemy roles, героические
  способности, deterministic enhancement breaks, victory/defeat и reward settlement;
- campaign progression, energy, currencies, roster/loadout, technology, reward track, claims,
  mastered-stage sweep, Shop и deterministic local Arena exhibition;
- Android/Pixel 9;
- deterministic local adapters для rewarded, IAP и Arena;
- replay/save/migration/fit gates.

## Out of scope

- реальные ad/payment SDK;
- аккаунты, backend и network Arena;
- reference assets, названия, тексты, звуки и точные balance values;
- exact unobserved reference behavior; such behavior is implemented only as an explicitly original
  MySD design decision.

## Success criteria

После Gate 2 метрики дополняются observed baselines. Уже зафиксированы:

- 100% FR/US/AC traceability;
- ноль orphan states и unmatched affordances;
- structural fit не ниже 90% на Pixel 9;
- ключевые bounds в пределах ±4 dp;
- критические timings в пределах ±15%;
- p95 frame не выше 16.7 ms и jank ниже 5% на Pixel 9;
- load benchmark: observed concurrent entities +25%.
