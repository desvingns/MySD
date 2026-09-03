# Первый playable level — enemy combat and outcomes

Status: done
TASK: feature
PLATFORM: android
WHAT: Расширить существующий Android-free playable runtime от пассивного движения врагов до детерминированного spawn-ритма, автоматического огня башен, уничтожения башни при контакте, урона главной базе, победы и поражения.
LAYERS: domain
CHANGED_HINT:
- `game/src/main/kotlin/dev/mysd/game/battle/playable/PlayableBattleState.kt` — хранить enemy position/HP, base HP, projectiles or hit events, wave counters and terminal phase — (assumption)
- `game/src/main/kotlin/dev/mysd/game/battle/playable/PlayableBattleEngine.kt` — реализовать deterministic spawn, movement, targeting, hit, tower collision and terminal resolution — (assumption)
- `game/src/main/kotlin/dev/mysd/game/simulation/ScenarioFixtures.kt` — заменить structured defeat blocker для playable fixture на positive victory/defeat scenarios, сохранив старые fixture IDs — G9, G10
TEST_TYPES: unit, integration, replay, scenario
Risk-signals: concurrency, cross-module data flow
Acceptance-matrix: state=active,tower-contact,victory,defeat; terminal=none,victory,defeat
CONSTRAINTS:
- Preserve the already-shipped resource, fixed-slot build, tower-upgrade, pause/resume, and passive-movement contours; implement only the missing combat, wave, and terminal behavior — existing baseline.
- Baseline wave contains 9 enemies, configurable only within the accepted 8–10 range — (assumption), D9.
- Enemies enter from the top and move downward along the fixed path; no branching path is required — D3, D7.
- Towers automatically target the nearest in-range enemy; ties resolve by stable enemy ID — D11.
- Reaching an occupied tower destroys that tower immediately; the enemy continues toward the base — D7.
- Victory requires no living or pending wave enemies; defeat requires base HP to reach zero. Both terminals are required — D9.
- No kill reward is added; the only resource source is passive income — D10.
- Existing structured defeat blocker must not remain the only defeat representation after this SPEC — G9, G11.

=== SPEC ===
TASK: feature
PLATFORM: android
WHAT: Deterministic top-to-bottom enemy wave, tower combat, tower/base destruction, victory, and defeat.
LAYERS: domain
TEST_TYPES: unit, integration, replay, scenario
CONSTRAINTS: finite 8–10 enemy wave; nearest-target auto-fire; tower collision destroys tower; base HP terminal; no kill rewards.
=== END SPEC ===

### Calculation: Movement and hit resolution

- Formula: `positionTicksNext = positionTicks + speedTicks`; an enemy reaches a tile when `positionTicksNext >= tilePositionTicks`; `enemyHpNext = enemyHp - towerDamage` on a hit; a hit is available when `cooldownRemaining == 0` and the target is in range; after firing, `cooldownRemaining = cooldownTicks`.
- Symbols: all positions and speeds are Int simulation ticks/tiles; `enemyHp`, `towerDamage`, `baseHp`, `baseDamage` are non-negative Ints; `cooldownTicks >= 1`.
- Precision: integer tick arithmetic; resolve commands, movement, collisions, and attacks in a documented stable order within each tick.
- Edge cases: dead enemies are removed before next target selection; an enemy reaching a tower removes the tower once; an enemy reaching the base applies one configured leak damage; victory is evaluated after final enemy death; defeat wins the terminal tie if base HP reaches zero on the same tick as the last enemy death.
- Worked examples (fixtures):

  | enemy position | speed/tick | tile position | ticks to tile |
  |---:|---:|---:|---:|
  | 0 | 4 | 40 | 10 |
  | 3 | 4 | 40 | 10 (ceil((40-3)/4)) |
  | 40 | 4 | 40 | 0; collision is resolved immediately |

  | enemy HP | tower damage | hits | expected HP |
  |---:|---:|---:|---:|
  | 5 | 2 | 0 | 5 |
  | 5 | 2 | 2 | 1 |
  | 5 | 2 | 3 | 0 and enemy removed |

- Determinism: same seed, content, command log and tick count yield identical enemy trajectories, terminal result and replay hash.

## Acceptance

Feature: Enemy combat and level outcomes
  Covers US-FPL-005. Source: D7, D9, D11.

  @US-FPL-005
  Scenario: Towers destroy descending enemies
    Given a finite wave is active and an affordable tower is built in range of the path
    When simulation ticks advance through enemy movement and tower cooldowns
    Then the tower fires automatically at the nearest enemy
    And a defeated enemy is removed from the wave

  @US-FPL-005 @tower-contact
  Scenario: An enemy destroys a tower and continues to the base
    Given an enemy reaches an occupied build tile before being defeated
    When the collision is resolved
    Then that tower is destroyed
    And the enemy remains active and continues downward toward the main base

  @US-FPL-005 @terminal
  Scenario: Complete the wave with victory
    Given all wave enemies can be defeated before the base reaches zero health
    When the final living enemy is destroyed and no pending enemy remains
    Then the level enters victory
    And no further build, upgrade, movement, or damage mutation is applied

  @US-FPL-005 @terminal @defeat
  Scenario: Lose when the main base is destroyed
    Given enough enemies reach the main base to reduce its health to zero
    When the base damage is resolved
    Then the level enters defeat
    And the run exposes a terminal defeat result

## Gap / context

Это главный переход от «на экране нарисован враг» к игре: появляются movement, shots, collision, terminal states и возможность реально увидеть, как уровень играется.

## Implementation links
- commits: `c388660ee9c2c42bac3a75aae9c404eb9001a936`, `1d8af03`
- files: `game/src/main/kotlin/dev/mysd/game/battle/playable/PlayableBattleEngine.kt`, `game/src/main/kotlin/dev/mysd/game/battle/playable/PlayableBattleState.kt`, `game/src/main/kotlin/dev/mysd/game/content/PlayableLevelContent.kt`, `game/src/main/kotlin/dev/mysd/game/simulation/ScenarioFixtures.kt`, `game/src/main/kotlin/dev/mysd/game/simulation/SimulationSession.kt`, `game/src/test/kotlin/dev/mysd/game/battle/playable/PlayableBattleCombatTest.kt`, `game/src/test/kotlin/dev/mysd/game/battle/playable/PlayableBattleStateTest.kt`, `game/src/test/kotlin/dev/mysd/game/campaign/CampaignEnhancementIntegrationTest.kt`, `game/src/test/kotlin/dev/mysd/game/simulation/ScenarioFixturesTest.kt`
- staleness_auto_rescoped=1
