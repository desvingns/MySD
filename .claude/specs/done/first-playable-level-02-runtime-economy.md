# Первый playable level — runtime and economy

Status: done
TASK: feature
PLATFORM: android
WHAT: Создать Android-free authoritative battle state, который обновляется на фиксированных 20 Гц и начисляет глобальный ресурс только во время активной симуляции.
LAYERS: domain
CHANGED_HINT:
- `game/src/main/kotlin/dev/mysd/game/simulation/SimulationSession.kt` — подключить playable battle state к существующему seeded deterministic stepping — G5
- `game/src/main/kotlin/dev/mysd/game/simulation/SimulationClock.kt` — сохранить 50 ms tick contract для resource/entity advancement — G5
- `game/src/main/kotlin/dev/mysd/game/battle/playable/PlayableBattleState.kt` — определить immutable snapshot state для base, resource, slots, enemies и phase — (assumption)
- `game/src/main/kotlin/dev/mysd/game/battle/playable/PlayableBattleEngine.kt` — реализовать pure tick reduction и resource accumulation — (assumption)
TEST_TYPES: unit, integration, replay
CONSTRAINTS:
- Renderer and input не владеют authoritative state; state живёт в `:game` — G5.
- Wall-clock не читается из engine; caller supplies elapsed time, а state advances only by complete 50 ms ticks — G5.
- Пауза останавливает ticks и passive income; resume продолжает с тем же remainder — (assumption), D11.
- Resource не может стать отрицательным или превысить cap; spend operation должен быть atomic.
Risk-signals: —
Acceptance-matrix: phase=active,paused; operation=tick,spend

=== SPEC ===
TASK: feature
PLATFORM: android
WHAT: Deterministic 20 Hz playable battle state and passive global economy.
LAYERS: domain
TEST_TYPES: unit, integration, replay
CONSTRAINTS: Android-free; seeded; fixed 50 ms ticks; pause freezes simulation; resource mutations are atomic.
=== END SPEC ===

### Calculation: Passive resource accumulation

- Formula: `raw = incomePerSecond * deltaTicks + incomeRemainderTicks`; `gain = floor(raw / 20)`; `nextRemainder = raw mod 20`; `nextResource = min(resourceCap, currentResource + gain)`.
- Symbols: `incomePerSecond` = Int, `>= 0`, resource units/second; `deltaTicks` = Int, `>= 0`; `incomeRemainderTicks` = Int, `0..19`; `resourceCap` = Int, `>= 0`; `currentResource` = Int, `0..resourceCap`.
- Precision: integer arithmetic only; no floating point; clamp at the state boundary.
- Edge cases: `deltaTicks == 0` leaves resource and remainder unchanged; `incomePerSecond == 0` yields zero gain; cap saturation preserves any deterministic remainder; negative inputs reject as invalid content/state.
- Worked examples (fixtures):

  | current | income/sec | ticks | remainder | cap | expected resource | expected remainder |
  |---:|---:|---:|---:|---:|---:|---:|
  | 50 | 10 | 0 | 0 | 100 | 50 | 0 |
  | 50 | 10 | 20 | 0 | 100 | 60 | 0 |
  | 95 | 10 | 20 | 0 | 100 | 100 | 0 |

- Determinism: the calculation is a pure function; advancing 20 ticks at once equals advancing ten 2-tick chunks with the same initial state.

## Acceptance

Feature: Playable battle runtime and economy
  Covers US-FPL-002. Source: G5, D6, D10.

  @US-FPL-002
  Scenario: Accumulate resource during an active run
    Given the battle is active with 50 resource units and a configured passive income rate
    When 20 complete simulation ticks are advanced
    Then the resource increases by exactly one configured second of income
    And the state hash is deterministic for the same seed and commands

  @US-FPL-002 @paused
  Scenario: Pause freezes simulation and income
    Given the battle is paused with a non-zero resource remainder
    When wall time passes without advancing active simulation ticks
    Then the enemy positions and resource remain unchanged
    And resuming continues from the preserved remainder

  @US-FPL-002 @validation
  Scenario: Reject a spend that exceeds the available resource
    Given the active battle has less resource than the tower build cost
    When a build spend is submitted
    Then the resource remains unchanged
    And the target slot remains empty

## Gap / context

Текущий `SimulationSession` умеет тики и hash, но battle state не содержит ресурса и сущностей. Этот SPEC создаёт рабочую временную основу для строительства и combat.

## Implementation links
- commit: 811590b, 4365b00
- files: game/src/main/kotlin/dev/mysd/game/battle/playable/PlayableBattleEngine.kt, game/src/main/kotlin/dev/mysd/game/battle/playable/PlayableBattleState.kt, game/src/main/kotlin/dev/mysd/game/simulation/SimulationSession.kt, game/src/test/kotlin/dev/mysd/game/battle/playable/PlayableBattleEngineTest.kt, game/src/test/kotlin/dev/mysd/game/battle/playable/PlayableBattleStateTest.kt, game/src/test/kotlin/dev/mysd/game/simulation/SimulationSessionTest.kt
- staleness_auto_closed: 1
- targeted_check: :game:test --tests dev.mysd.game.battle.playable.PlayableBattleEngineTest --tests dev.mysd.game.battle.playable.PlayableBattleStateTest (pass)
