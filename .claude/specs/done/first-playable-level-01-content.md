# Первый playable level — content contract

Status: done
TASK: feature
PLATFORM: android
WHAT: Ввести versioned оригинальный content contract для первого playable level: одну сцену, отдельную главную базу, три фиксированных build tile, одну башню, одну семью врагов и параметры одной конечной волны.
LAYERS: domain, data
CHANGED_HINT:
- `game/src/main/kotlin/dev/mysd/game/content/ContentCatalog.kt` — расширить валидируемый каталог ссылкой на playable-level content — G6
- `game/src/main/kotlin/dev/mysd/game/content/ContentFixtureCodec.kt` — кодировать и декодировать новые versioned level fields с exact-key validation — G6
- `game/src/main/kotlin/dev/mysd/game/content/ContentExceptions.kt` — возвращать field-path ошибки для malformed level fixtures — G6
- `game/src/main/kotlin/dev/mysd/game/content/PlayableLevelContent.kt` — добавить typed definitions для базы, трёх slots, башни, врага и волны — (assumption)
TEST_TYPES: unit, content-fixture
CONSTRAINTS:
- На wire входят только стабильные IDs и числовые data-параметры; display text и assets остаются за пределами game content boundary — G6.
- Все новые значения оригинальные для MySD; reference screenshot не является источником чисел — G11.
- Catalog обязан отвергать пустые/дублирующиеся IDs, отрицательные параметры, неверное количество slots и wave size вне диапазона 8–10 — G6, D9.
- Content version повышается только согласованно с codec и invalid fixtures — G7.
Risk-signals: —
Acceptance-matrix: fixture=valid,invalid

=== SPEC ===
TASK: feature
PLATFORM: android
WHAT: Versioned content contract for the original first playable level.
LAYERS: domain, data
TEST_TYPES: unit, content-fixture
CONSTRAINTS: typed IDs; original balance; exact-key codec; invalid fixtures; no UI strings/assets in the game catalog.
=== END SPEC ===

## Acceptance

Feature: First playable level content
  Covers US-FPL-001. Source: G6, D4, D5, D8, D9.

  @US-FPL-001
  Scenario: Load the valid original level catalog
    Given the content fixture declares one accepted stage, one base, one tower, one enemy family, and three fixed build tiles
    When the game loads the first playable level catalog
    Then the catalog is accepted with a stable content version
    And the level exposes one finite wave with 8 to 10 configured enemy spawns

  @US-FPL-001 @validation
  Scenario: Reject an invalid level fixture
    Given a level fixture contains a duplicate build tile ID or a negative health value
    When the game validates the fixture
    Then the fixture is rejected with a field-specific validation error
    And no playable battle session is created

## Gap / context

Сейчас оригинальные IDs существуют, но catalog не описывает playable geometry, balance или wave data. Этот SPEC создаёт единственный источник данных для следующих runtime SPEC-ов.

## Implementation links
- commit: 71b89df, e7fb9be
- files: game/src/main/kotlin/dev/mysd/game/content/ContentCatalog.kt; game/src/main/kotlin/dev/mysd/game/content/ContentExceptions.kt; game/src/main/kotlin/dev/mysd/game/content/ContentFixtureCodec.kt; game/src/main/kotlin/dev/mysd/game/content/PlayableLevelContent.kt; game/src/test/kotlin/dev/mysd/game/content/ContentFixtureCodecTest.kt
