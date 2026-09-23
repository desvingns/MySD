# Первый playable level — tower upgrades

Status: done
TASK: feature
PLATFORM: android
WHAT: Добавить для построенной башни два последовательных улучшения, каждое с детерминированной стоимостью и изменением боевой характеристики.
LAYERS: domain
CHANGED_HINT:
- `game/src/main/kotlin/dev/mysd/game/battle/playable/PlayableBattleState.kt` — хранить tower level 0..2 и upgraded stats — (assumption)
- `game/src/main/kotlin/dev/mysd/game/battle/playable/PlayableBattleEngine.kt` — атомарно обрабатывать upgrade command и stat transition — (assumption)
- `game/src/main/kotlin/dev/mysd/game/battle/playable/PlayableBattleCommand.kt` — добавить replay-safe upgrade command для occupied slot — (assumption)
TEST_TYPES: unit, integration, replay
Risk-signals: —
Acceptance-matrix: occupancy=occupied,empty; level=0,1,2; outcome=accepted,rejected
CONSTRAINTS:
- Upgrade доступен только для occupied slot; пустой slot не может быть улучшен — D12.
- После двух улучшений дальнейший upgrade rejected без списания ресурса — D12.
- Upgrade costs/stats — оригинальный balance baseline, закреплённый content fixtures, а не reference values — G11.
- Общий battle engine редактируется последовательно после build-placement SPEC — decomposition clash rule.

=== SPEC ===
TASK: feature
PLATFORM: android
WHAT: Two sequential deterministic tower upgrades with resource costs and stat changes.
LAYERS: domain
TEST_TYPES: unit, integration, replay
CONSTRAINTS: occupied slots only; max level 2; atomic resource spend; original balance fixtures.
=== END SPEC ===

### Calculation: Tower upgrade cost and stats

- Formula: `upgradeCost(level) = baseUpgradeCost + level * upgradeCostStep` for `level in {0,1}`; `nextDamage(level) = baseDamage + level * damageStep`; `nextCooldown(level) = max(minCooldownTicks, baseCooldownTicks - level * cooldownStep)`.
- Symbols: `level` = Int, `0..2`; costs and damage = Int, `>= 0` resource/HP units; cooldown values = Int, `>= 1` simulation ticks. All constants come from the original level content fixture.
- Precision: integer arithmetic; clamp cooldown to `minCooldownTicks`; no floating point.
- Edge cases: level 2 has no next upgrade; insufficient resource leaves level, stats, and resource unchanged; invalid negative constants reject the content fixture.
- Worked examples (fixtures using the baseline constants `baseUpgradeCost=30`, `upgradeCostStep=20`, `baseDamage=2`, `damageStep=1`, `baseCooldownTicks=10`, `cooldownStep=2`, `minCooldownTicks=4`):

  | current level | expected cost | next damage | next cooldown |
  |---:|---:|---:|---:|
  | 0 | 30 | 2 | 10 |
  | 1 | 50 | 3 | 8 |
  | 2 | rejected | — | — |

- Determinism: the same pre-upgrade snapshot and command produces the same post-upgrade snapshot and hash.

## Acceptance

Feature: Tower upgrades
  Covers US-FPL-004. Source: D12.

  @US-FPL-004
  Scenario: Upgrade an occupied tower
    Given slot 1 contains a level 0 tower and the player has enough resource
    When the player submits an upgrade command for slot 1
    Then the tower becomes level 1
    And its configured damage or cooldown stat changes
    And exactly the configured upgrade cost is deducted

  @US-FPL-004 @validation
  Scenario: Reject an upgrade when the tower is already at maximum level
    Given slot 1 contains a level 2 tower
    When the player submits an upgrade command for slot 1
    Then the tower remains level 2
    And the resource is unchanged

  @US-FPL-004 @empty
  Scenario: Do not upgrade an empty slot
    Given slot 3 is empty
    When the player submits an upgrade command for slot 3
    Then no tower is created
    And no resource is deducted

## Gap / context

Пользователь должен увидеть простой, но реальный цикл «построил → накопил → улучшил». Этот SPEC делает upgrade частью authoritative state, а не только подписью popup.

## Implementation links
- commit: 24a020f756c956eda4b1bc41ceebcf0d466cda1f
- files:
  - game/src/main/kotlin/dev/mysd/game/battle/playable/PlayableBattleState.kt
  - game/src/main/kotlin/dev/mysd/game/battle/playable/PlayableBattleCommand.kt
  - game/src/main/kotlin/dev/mysd/game/battle/playable/PlayableBattleEngine.kt
  - game/src/main/kotlin/dev/mysd/game/simulation/SimulationSession.kt
  - game/src/test/kotlin/dev/mysd/game/battle/playable/PlayableBattleCommandTest.kt
  - game/src/test/kotlin/dev/mysd/game/battle/playable/PlayableBattleEngineTest.kt
  - game/src/test/kotlin/dev/mysd/game/battle/playable/PlayableBattleStateTest.kt
  - game/src/test/kotlin/dev/mysd/game/simulation/SimulationSessionTest.kt
