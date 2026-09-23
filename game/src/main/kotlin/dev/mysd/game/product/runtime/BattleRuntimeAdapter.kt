package dev.mysd.game.product.runtime

import dev.myengine.core.CommandId
import dev.myengine.core.EngineCommand
import dev.myengine.core.TextCommand
import dev.myengine.core.Tick
import dev.myengine.core.TickRate
import dev.myengine.core.stableHashOf
import dev.myengine.runtime.CommandIdPolicy
import dev.myengine.runtime.ContentPackIdentity
import dev.myengine.runtime.DeterministicGameSession
import dev.myengine.runtime.ExperimentalGameRuntimeApi
import dev.myengine.runtime.GameRuntimeDescriptor
import dev.myengine.runtime.GameRuntimeIdentity
import dev.myengine.runtime.SaveSchemaIdentity
import dev.myengine.runtime.SaveIncompatibility
import dev.myengine.runtime.SessionRestoreResult
import dev.myengine.runtime.SessionSaveResult
import dev.myengine.runtime.SessionStartResult
import dev.myengine.runtime.SessionStepResult
import dev.myengine.runtime.SessionSubmitResult
import dev.myengine.runtime.VersionedGameSave
import dev.myengine.runtime.validateSaveCompatibility
import dev.mysd.game.content.ContentId
import dev.mysd.game.persistence.MalformedPersistenceException
import dev.mysd.game.persistence.PersistenceException
import dev.mysd.game.persistence.PersistenceWire
import dev.mysd.game.product.MySdBattlePhase
import dev.mysd.game.product.MySdBattleSpeed
import dev.mysd.game.product.MySdTerminalResult
import dev.mysd.game.product.content.OriginalProductCatalog
import dev.mysd.game.product.content.ProductAllyDefinition
import dev.mysd.game.product.content.ProductCatalog
import dev.mysd.game.product.content.ProductEnemyRole
import dev.mysd.game.product.content.ProductEnhancementKind
import dev.mysd.game.product.content.ProductHeroSkillKind
import dev.mysd.game.product.content.ProductSpawnGroup
import dev.mysd.game.product.content.ProductStageDefinition
import dev.mysd.game.product.content.ProductTowerDefinition
import dev.mysd.game.product.content.ProductTowerRole
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

private const val MAX_BATTLE_SAVE_CHARS: Int = 2_000_000
private const val MAX_BATTLE_COLLECTION_ITEMS: Int = 10_000
private const val MAX_BATTLE_FIELD_CHARS: Int = 1_000_000

private fun boundedBattleSplit(
    raw: String,
    delimiter: Char,
    field: String,
    maximum: Int = MAX_BATTLE_COLLECTION_ITEMS,
): List<String> {
    if (raw.isEmpty()) return emptyList()
    if (raw.length > MAX_BATTLE_FIELD_CHARS) throw MalformedPersistenceException("Battle field is too large: $field")
    var count = 1
    raw.forEach { if (it == delimiter && ++count > maximum) throw MalformedPersistenceException("Too many battle items: $field") }
    return raw.split(delimiter)
}

/** Immutable launch data derived from the profile before a run starts. */
internal data class ProductBattleLaunch(
    val stageId: ContentId,
    val loadoutIds: List<ContentId>,
    val heroSkillIds: List<ContentId>,
    val rosterLevels: Map<ContentId, Int>,
    val towerPowerPermille: Int,
    val allyPowerPermille: Int,
    val economyPermille: Int,
    val heroPowerPermille: Int,
)

internal data class ProductTowerSlotState(
    val id: ContentId,
    val positionTicks: Int,
    var towerId: ContentId? = null,
    var level: Int = 0,
    var cooldownRemainingTicks: Int = 0,
)

internal data class ProductAllyState(
    val entityId: Long,
    val allyId: ContentId,
    var health: Int,
    var maxHealth: Int,
    var positionTicks: Int,
    var cooldownRemainingTicks: Int = 0,
)

internal data class ProductEnemyState(
    val entityId: Long,
    val enemyId: ContentId,
    var health: Int,
    val maxHealth: Int,
    var positionTicks: Int,
    var cooldownRemainingTicks: Int = 0,
    var slowRemainingTicks: Int = 0,
    var slowPermille: Int = 0,
)

internal class ProductBattleState(
    val seed: Long,
    val runId: String,
    val launch: ProductBattleLaunch,
    var tick: Long,
    var phase: MySdBattlePhase,
    var paused: Boolean,
    var speed: MySdBattleSpeed,
    var waveIndex: Int,
    var waveElapsedTicks: Int,
    val spawnedByGroup: MutableList<Int>,
    var resource: Int,
    var incomeRemainder: Int,
    var baseHealth: Int,
    val slots: MutableList<ProductTowerSlotState>,
    val allies: MutableList<ProductAllyState>,
    val enemies: MutableList<ProductEnemyState>,
    val skillCooldowns: MutableMap<ContentId, Int>,
    val enhancements: MutableList<ContentId>,
    val enhancementOffers: MutableList<ContentId>,
    var rerollsRemaining: Int,
    var nextEntityId: Long,
    var nextCommandId: Long,
    var rngState: Long,
    var terminalResult: MySdTerminalResult?,
) {
    val stageId: ContentId get() = launch.stageId
}

internal data class ProductBattleProjection(
    val state: ProductBattleStateView,
)

/** Deep immutable runtime snapshot; no mutable authoritative collection escapes the adapter. */
internal data class ProductBattleStateView(
    val runId: String,
    val stageId: ContentId,
    val loadoutIds: List<ContentId>,
    val heroSkillIds: List<ContentId>,
    val rosterLevels: Map<ContentId, Int>,
    val towerPowerPermille: Int,
    val allyPowerPermille: Int,
    val economyPermille: Int,
    val heroPowerPermille: Int,
    val tick: Long,
    val phase: MySdBattlePhase,
    val paused: Boolean,
    val speed: MySdBattleSpeed,
    val waveIndex: Int,
    val waveElapsedTicks: Int,
    val spawnedByGroup: List<Int>,
    val resource: Int,
    val resourceCap: Int,
    val baseHealth: Int,
    val baseMaxHealth: Int,
    val slots: List<ProductTowerSlotState>,
    val allies: List<ProductAllyState>,
    val enemies: List<ProductEnemyState>,
    val skillCooldowns: Map<ContentId, Int>,
    val skillCooldownTotals: Map<ContentId, Int>,
    val enhancements: List<ContentId>,
    val enhancementOffers: List<ContentId>,
    val rerollsRemaining: Int,
    val terminalResult: MySdTerminalResult?,
)

internal sealed interface ProductBattleAction {
    data class SetPaused(val paused: Boolean) : ProductBattleAction
    data class SetSpeed(val speed: MySdBattleSpeed) : ProductBattleAction
    data class BuildTower(val slotId: ContentId, val towerId: ContentId) : ProductBattleAction
    data class UpgradeTower(val slotId: ContentId) : ProductBattleAction
    data class DeployAlly(val allyId: ContentId) : ProductBattleAction
    data class UseHeroSkill(val skillId: ContentId) : ProductBattleAction
    data class ChooseEnhancement(val enhancementId: ContentId) : ProductBattleAction
    data object RerollEnhancements : ProductBattleAction
}

internal enum class ProductBattleRejection {
    UNKNOWN_CONTENT,
    INVALID_TARGET,
    INVALID_PHASE,
    INSUFFICIENT_RESOURCE,
    MAXIMUM_LEVEL,
    TERMINAL,
}

internal sealed interface ProductBattleActionResult {
    data class Accepted(val commandId: Long, val advancedTicks: Int) : ProductBattleActionResult
    data class Rejected(val reason: ProductBattleRejection) : ProductBattleActionResult
}

internal sealed interface ProductBattleRestoreResult {
    data class Restored(val controller: ProductBattleController) : ProductBattleRestoreResult
    data class Incompatible(
        val kind: ProductBattleIncompatibility,
        val expected: String,
        val actual: String,
    ) : ProductBattleRestoreResult
    data class Invalid(val reason: String) : ProductBattleRestoreResult
}

internal enum class ProductBattleIncompatibility { RUNTIME, CONTENT, RUN_SCHEMA }

internal data class ProductBattleSaveEnvelope(
    val runtimeId: String,
    val contentPackId: String,
    val contentPackVersion: String,
    val schemaId: String,
    val schemaVersion: Int,
    val payload: String,
)

internal sealed interface ProductBattleStepResult {
    data class Advanced(val requestedTicks: Int, val advancedTicks: Int, val terminal: Boolean) : ProductBattleStepResult
    data object InvalidTickCount : ProductBattleStepResult
}

/**
 * Canonical projection of the twelve enhancement families. Keeping the mapping in one place makes
 * both the reducer and save validation use the same authoritative semantics.
 */
internal data class ProductBattleModifiers(
    val towerDamagePermille: Int = 0,
    val towerCooldownPermille: Int = 0,
    val towerRangePermille: Int = 0,
    val splashRadiusPermille: Int = 0,
    val slowStrengthPermille: Int = 0,
    val allyHealthPermille: Int = 0,
    val allyDamagePermille: Int = 0,
    val allySpeedPermille: Int = 0,
    val resourceIncomePermille: Int = 0,
    val resourceCapPermille: Int = 0,
    val baseArmorPermille: Int = 0,
    val heroCooldownPermille: Int = 0,
)

internal object ProductBattleModifierResolver {
    fun resolve(catalog: ProductCatalog, selected: List<ContentId>): ProductBattleModifiers {
        fun total(kind: ProductEnhancementKind): Int = selected.sumOf { id ->
            catalog.enhancements.getValue(id).takeIf { it.kind == kind }?.magnitudePermille ?: 0
        }
        return ProductBattleModifiers(
            towerDamagePermille = total(ProductEnhancementKind.TOWER_DAMAGE),
            towerCooldownPermille = total(ProductEnhancementKind.TOWER_COOLDOWN),
            towerRangePermille = total(ProductEnhancementKind.TOWER_RANGE),
            splashRadiusPermille = total(ProductEnhancementKind.SPLASH_RADIUS),
            slowStrengthPermille = total(ProductEnhancementKind.SLOW_STRENGTH),
            allyHealthPermille = total(ProductEnhancementKind.ALLY_HEALTH),
            allyDamagePermille = total(ProductEnhancementKind.ALLY_DAMAGE),
            allySpeedPermille = total(ProductEnhancementKind.ALLY_SPEED),
            resourceIncomePermille = total(ProductEnhancementKind.RESOURCE_INCOME),
            resourceCapPermille = total(ProductEnhancementKind.RESOURCE_CAP),
            baseArmorPermille = total(ProductEnhancementKind.BASE_ARMOR),
            heroCooldownPermille = total(ProductEnhancementKind.HERO_COOLDOWN),
        )
    }
}

/** Role traits consumed directly by the reducer and exercised as a complete behavior matrix. */
internal object ProductBattleRoleRules {
    fun ignoresAllies(role: ProductEnemyRole): Boolean = role == ProductEnemyRole.FLYER
    fun attacksBaseAtRange(role: ProductEnemyRole): Boolean =
        role == ProductEnemyRole.SIEGE || role == ProductEnemyRole.BOSS
    fun disruptsTowers(role: ProductEnemyRole): Boolean = role == ProductEnemyRole.DISRUPTOR
    fun splashes(role: ProductTowerRole): Boolean = role == ProductTowerRole.BURST
    fun slows(role: ProductTowerRole): Boolean = role == ProductTowerRole.CONTROL
    fun generatesIncome(role: ProductTowerRole): Boolean = role == ProductTowerRole.SUPPORT
}

/**
 * The only MySD boundary that knows engine-runtime. The Android/product facades consume the
 * controller below and never import reusable runtime types.
 */
@OptIn(ExperimentalGameRuntimeApi::class)
internal object BattleRuntimeAdapter {
    const val RUNTIME_ID: String = "mysd-emberwatch-battle"
    const val SAVE_SCHEMA_ID: String = "mysd-product-run"
    const val CURRENT_SAVE_SCHEMA: Int = 5
    const val OLDEST_SAVE_SCHEMA: Int = 4
    const val TICKS_PER_SECOND: Int = 20
    const val MAX_SAFE_TICK: Long = 1_000_000_000_000L
    const val MAX_SAFE_ID_CURSOR: Long = 1_000_000_000_000L

    private val catalog: ProductCatalog = OriginalProductCatalog.releaseOne()

    fun start(seed: Long, launch: ProductBattleLaunch): ProductBattleController {
        val descriptor = Descriptor(catalog, launch)
        val result = descriptor.start(seed)
        val session = (result as SessionStartResult.Started<ProductBattleProjection>).session
            as ProductBattleGameSession
        return ProductBattleController(catalog, session)
    }

    fun restore(envelope: ProductBattleSaveEnvelope): ProductBattleRestoreResult {
        if (envelope.runtimeId.isBlank() || envelope.contentPackId.isBlank() ||
            envelope.contentPackVersion.isBlank() || envelope.schemaId.isBlank() || envelope.schemaVersion <= 0
        ) return ProductBattleRestoreResult.Invalid("Save envelope identity is invalid.")
        val save = VersionedGameSave(
            runtimeId = envelope.runtimeId,
            contentPack = ContentPackIdentity(envelope.contentPackId, envelope.contentPackVersion),
            schemaId = envelope.schemaId,
            schemaVersion = envelope.schemaVersion,
            payload = envelope.payload,
        )
        val descriptor = Descriptor(catalog, launch = null)
        return when (val restored = descriptor.restore(save)) {
            is SessionRestoreResult.Restored -> ProductBattleRestoreResult.Restored(
                ProductBattleController(catalog, restored.session as ProductBattleGameSession),
            )
            is SessionRestoreResult.Incompatible -> ProductBattleRestoreResult.Incompatible(
                kind = when (restored.kind) {
                    SaveIncompatibility.RUNTIME_ID -> ProductBattleIncompatibility.RUNTIME
                    SaveIncompatibility.CONTENT_PACK_ID,
                    SaveIncompatibility.CONTENT_PACK_VERSION,
                    -> ProductBattleIncompatibility.CONTENT
                    SaveIncompatibility.SAVE_SCHEMA_ID,
                    SaveIncompatibility.SAVE_SCHEMA_VERSION,
                    -> ProductBattleIncompatibility.RUN_SCHEMA
                },
                expected = restored.expected,
                actual = restored.actual,
            )
            is SessionRestoreResult.InvalidSave -> ProductBattleRestoreResult.Invalid(restored.reason)
        }
    }

    private class Descriptor(
        private val catalog: ProductCatalog,
        private val launch: ProductBattleLaunch?,
    ) : GameRuntimeDescriptor<ProductBattleProjection> {
        override val identity: GameRuntimeIdentity = GameRuntimeIdentity(
            id = RUNTIME_ID,
            tickRate = TickRate(TICKS_PER_SECOND),
            contentPack = ContentPackIdentity(catalog.packId.value, catalog.contentVersion.toString()),
            saveSchema = SaveSchemaIdentity(SAVE_SCHEMA_ID, OLDEST_SAVE_SCHEMA, CURRENT_SAVE_SCHEMA),
            stableSystemOrder = listOf(
                "commands",
                "income-and-cooldowns",
                "wave-spawn",
                "enemy-system",
                "ally-system",
                "tower-system",
                "cleanup-and-progression",
            ),
            commandIdPolicy = CommandIdPolicy.CALLER_OWNED,
            maxTicksPerStep = 100_000,
        )

        override fun start(seed: Long): SessionStartResult<ProductBattleProjection> {
            val requested = launch
                ?: return SessionStartResult.Failed("A launch definition is required for a new run.")
            val stage = catalog.stages[requested.stageId]
                ?: return SessionStartResult.Failed("Unknown stage: ${requested.stageId.value}")
            val allowedLoadout = stage.towerIds.toSet() + stage.allyIds.toSet()
            val invalidLoadout = requested.loadoutIds.size !in 2..ProductBattleController.MAX_LOADOUT_SIZE ||
                requested.loadoutIds.distinct().size != requested.loadoutIds.size ||
                requested.loadoutIds.any { it !in allowedLoadout } ||
                requested.loadoutIds.none(catalog.towers::containsKey) ||
                requested.loadoutIds.none(catalog.allies::containsKey)
            if (invalidLoadout) return SessionStartResult.Failed("Launch loadout is invalid.")
            if (requested.heroSkillIds.distinct().size != requested.heroSkillIds.size ||
                requested.heroSkillIds.any { it !in stage.heroSkillIds }
            ) {
                return SessionStartResult.Failed("Launch hero roster is invalid.")
            }
            val fullRoster = catalog.towers.keys + catalog.allies.keys + catalog.heroSkills.keys
            if (requested.rosterLevels.keys != fullRoster || requested.rosterLevels.values.any { it !in 1..5 }) {
                return SessionStartResult.Failed("Launch roster levels are invalid.")
            }
            if (listOf(
                    requested.towerPowerPermille,
                    requested.allyPowerPermille,
                    requested.economyPermille,
                    requested.heroPowerPermille,
                ).any { it !in 0..5_000 }
            ) return SessionStartResult.Failed("Launch bonuses are invalid.")
            val state = newState(seed, requested, stage)
            return SessionStartResult.Started(ProductBattleGameSession(this, catalog, state))
        }

        override fun restore(save: VersionedGameSave): SessionRestoreResult<ProductBattleProjection> {
            validateSaveCompatibility(identity, save)?.let { return it }
            return try {
                val decoded = BattlePayloadCodec.decode(save.payload, save.schemaVersion, catalog)
                SessionRestoreResult.Restored(
                    ProductBattleGameSession(
                        descriptor = this,
                        catalog = catalog,
                        state = decoded.state,
                        pendingCommands = decoded.pendingCommands,
                    ),
                )
            } catch (failure: PersistenceException) {
                SessionRestoreResult.InvalidSave(failure.message ?: "Invalid battle save.")
            } catch (failure: IllegalArgumentException) {
                SessionRestoreResult.InvalidSave(failure.message ?: "Invalid battle save.")
            } catch (failure: RuntimeException) {
                SessionRestoreResult.InvalidSave(failure.message ?: "Invalid battle save.")
            }
        }
    }

    private fun newState(
        seed: Long,
        launch: ProductBattleLaunch,
        stage: ProductStageDefinition,
    ): ProductBattleState = ProductBattleState(
        seed = seed,
        runId = "run-${seed.toULong().toString(16)}-${stage.ordinal}",
        launch = launch,
        tick = 0,
        phase = MySdBattlePhase.PREPARATION,
        paused = false,
        speed = MySdBattleSpeed.ONE_X,
        waveIndex = 0,
        waveElapsedTicks = 0,
        spawnedByGroup = MutableList(stage.waves.first().groups.size) { 0 },
        resource = stage.initialResource,
        incomeRemainder = 0,
        baseHealth = stage.baseHealth,
        slots = stage.buildSlots.map { ProductTowerSlotState(it.id, it.positionTicks) }.toMutableList(),
        allies = mutableListOf(),
        enemies = mutableListOf(),
        skillCooldowns = launch.heroSkillIds.associateWith { 0 }.toMutableMap(),
        enhancements = mutableListOf(),
        enhancementOffers = mutableListOf(),
        rerollsRemaining = 1,
        nextEntityId = 1,
        nextCommandId = 1,
        rngState = seed xor 0x4d7953445f72756eL,
        terminalResult = null,
    )
}

@OptIn(ExperimentalGameRuntimeApi::class)
internal class ProductBattleGameSession(
    descriptor: GameRuntimeDescriptor<ProductBattleProjection>,
    private val catalog: ProductCatalog,
    internal val state: ProductBattleState,
    pendingCommands: List<EngineCommand> = emptyList(),
) : DeterministicGameSession<ProductBattleProjection>(descriptor, state.seed, pendingCommands) {
    private val reducer = ProductBattleReducer(catalog)

    override val authoritativeTick: Tick get() = Tick(state.tick)
    override val terminal: Boolean get() = state.terminalResult != null

    fun submitAction(action: ProductBattleAction): SessionSubmitResult {
        val id = CommandId(state.nextCommandId)
        if (isTerminal || state.nextCommandId >= BattleRuntimeAdapter.MAX_SAFE_ID_CURSOR) {
            return SessionSubmitResult.Rejected(id, SessionSubmitResult.Reason.TERMINAL_SESSION)
        }
        val submitted = submit(BattleCommandCodec.encode(id, currentTick, action))
        if (submitted is SessionSubmitResult.Accepted) {
            state.nextCommandId += 1
            reducer.applyCommands(state, drainCommandsAtCurrentTick())
        }
        return submitted
    }

    override fun advanceOneTick(tick: Tick, commands: List<EngineCommand>) {
        state.tick = tick.value
        reducer.advance(state, commands)
    }

    override fun projectSnapshot(): ProductBattleProjection = ProductBattleProjection(
        ProductBattleProjector.project(state, catalog),
    )

    override fun stableHashValue(): String = ProductBattleHasher.hash(state, pendingCommandSnapshot())

    override fun encodeSavePayload(pendingCommands: List<EngineCommand>): String =
        BattlePayloadCodec.encode(state, pendingCommands)
}

@OptIn(ExperimentalGameRuntimeApi::class)
internal class ProductBattleController(
    private val catalog: ProductCatalog,
    private val session: ProductBattleGameSession,
) {
    fun snapshot(): ProductBattleStateView = session.snapshot().value.state
    fun stableHash(): String = session.stableHash()
    fun save(): ProductBattleSaveEnvelope = when (val result = session.save()) {
        is SessionSaveResult.Saved -> result.save.let { save ->
            ProductBattleSaveEnvelope(
                runtimeId = save.runtimeId,
                contentPackId = save.contentPack.id,
                contentPackVersion = save.contentPack.version,
                schemaId = save.schemaId,
                schemaVersion = save.schemaVersion,
                payload = save.payload,
            )
        }
        is SessionSaveResult.Failed -> error(result.reason)
    }

    fun step(ticks: Int): ProductBattleStepResult {
        if (ticks !in 1..session.descriptor.identity.maxTicksPerStep) {
            return ProductBattleStepResult.InvalidTickCount
        }
        var advanced = 0
        while (advanced < ticks) {
            val before = snapshot()
            if (before.paused || before.phase == MySdBattlePhase.ENHANCEMENT || before.terminalResult != null) break
            when (val one = session.step(1)) {
                is SessionStepResult.Rejected -> return ProductBattleStepResult.InvalidTickCount
                is SessionStepResult.Advanced -> advanced += one.advancedTicks
            }
            val after = snapshot()
            if (after.paused || after.phase == MySdBattlePhase.ENHANCEMENT || after.terminalResult != null) break
        }
        return ProductBattleStepResult.Advanced(
            requestedTicks = ticks,
            advancedTicks = advanced,
            terminal = session.isTerminal,
        )
    }

    fun submit(action: ProductBattleAction): ProductBattleActionResult {
        validate(action)?.let { return ProductBattleActionResult.Rejected(it) }
        val submit = session.submitAction(action)
        if (submit is SessionSubmitResult.Rejected) {
            return ProductBattleActionResult.Rejected(ProductBattleRejection.TERMINAL)
        }
        return ProductBattleActionResult.Accepted(
            commandId = (submit as SessionSubmitResult.Accepted).commandId.value,
            advancedTicks = 0,
        )
    }

    private fun validate(action: ProductBattleAction): ProductBattleRejection? {
        val state = snapshot()
        if (state.terminalResult != null) return ProductBattleRejection.TERMINAL
        if (session.state.tick >= BattleRuntimeAdapter.MAX_SAFE_TICK ||
            session.state.nextCommandId >= BattleRuntimeAdapter.MAX_SAFE_ID_CURSOR
        ) return ProductBattleRejection.INVALID_TARGET
        if (state.paused && action !is ProductBattleAction.SetPaused && action !is ProductBattleAction.SetSpeed) {
            return ProductBattleRejection.INVALID_PHASE
        }
        val stage = catalog.stages.getValue(state.stageId)
        return when (action) {
            is ProductBattleAction.SetPaused,
            is ProductBattleAction.SetSpeed,
            -> null
            is ProductBattleAction.BuildTower -> {
                val slot = state.slots.firstOrNull { it.id == action.slotId }
                    ?: return ProductBattleRejection.INVALID_TARGET
                val tower = catalog.towers[action.towerId]
                    ?: return ProductBattleRejection.UNKNOWN_CONTENT
                when {
                    action.towerId !in stage.towerIds || action.towerId !in stateLoadout(state) ->
                        ProductBattleRejection.UNKNOWN_CONTENT
                    slot.towerId != null -> ProductBattleRejection.INVALID_TARGET
                    state.resource < tower.buildCost -> ProductBattleRejection.INSUFFICIENT_RESOURCE
                    state.phase == MySdBattlePhase.ENHANCEMENT -> ProductBattleRejection.INVALID_PHASE
                    else -> null
                }
            }
            is ProductBattleAction.UpgradeTower -> {
                val slot = state.slots.firstOrNull { it.id == action.slotId }
                    ?: return ProductBattleRejection.INVALID_TARGET
                val towerId = slot.towerId ?: return ProductBattleRejection.INVALID_TARGET
                val tower = catalog.towers.getValue(towerId)
                when {
                    slot.level >= MAX_TOWER_LEVEL -> ProductBattleRejection.MAXIMUM_LEVEL
                    state.resource < tower.upgradeCosts[slot.level - 1] ->
                        ProductBattleRejection.INSUFFICIENT_RESOURCE
                    state.phase == MySdBattlePhase.ENHANCEMENT -> ProductBattleRejection.INVALID_PHASE
                    else -> null
                }
            }
            is ProductBattleAction.DeployAlly -> {
                val ally = catalog.allies[action.allyId]
                    ?: return ProductBattleRejection.UNKNOWN_CONTENT
                when {
                    action.allyId !in stage.allyIds || action.allyId !in stateLoadout(state) ->
                        ProductBattleRejection.UNKNOWN_CONTENT
                    state.allies.size >= MAX_ALLIES -> ProductBattleRejection.INVALID_TARGET
                    state.resource < ally.deployCost -> ProductBattleRejection.INSUFFICIENT_RESOURCE
                    state.phase == MySdBattlePhase.ENHANCEMENT -> ProductBattleRejection.INVALID_PHASE
                    else -> null
                }
            }
            is ProductBattleAction.UseHeroSkill -> when {
                action.skillId !in session.state.launch.heroSkillIds || action.skillId !in catalog.heroSkills ->
                    ProductBattleRejection.UNKNOWN_CONTENT
                state.phase != MySdBattlePhase.COMBAT -> ProductBattleRejection.INVALID_PHASE
                state.skillCooldowns.getValue(action.skillId) > 0 -> ProductBattleRejection.INVALID_TARGET
                else -> null
            }
            is ProductBattleAction.ChooseEnhancement -> when {
                state.phase != MySdBattlePhase.ENHANCEMENT -> ProductBattleRejection.INVALID_PHASE
                action.enhancementId !in state.enhancementOffers -> ProductBattleRejection.INVALID_TARGET
                else -> null
            }
            ProductBattleAction.RerollEnhancements -> when {
                state.phase != MySdBattlePhase.ENHANCEMENT -> ProductBattleRejection.INVALID_PHASE
                state.rerollsRemaining <= 0 -> ProductBattleRejection.INVALID_TARGET
                else -> null
            }
        }
    }

    private fun stateLoadout(state: ProductBattleStateView): Set<ContentId> =
        session.state.launch.loadoutIds.toSet()

    companion object {
        const val MAX_TOWER_LEVEL: Int = 4
        const val MAX_ALLIES: Int = 8
        const val MAX_LOADOUT_SIZE: Int = 7
    }
}

private object BattleCommandCodec {
    private const val SET_PAUSED = "set-paused"
    private const val SET_SPEED = "set-speed"
    private const val BUILD = "build-tower"
    private const val UPGRADE = "upgrade-tower"
    private const val DEPLOY = "deploy-ally"
    private const val HERO = "use-hero"
    private const val ENHANCEMENT = "choose-enhancement"
    private const val REROLL = "reroll-enhancements"

    fun encode(id: CommandId, tick: Tick, action: ProductBattleAction): TextCommand = when (action) {
        is ProductBattleAction.SetPaused -> text(id, tick, SET_PAUSED, if (action.paused) "1" else "0")
        is ProductBattleAction.SetSpeed -> text(id, tick, SET_SPEED, action.speed.name)
        is ProductBattleAction.BuildTower -> text(
            id,
            tick,
            BUILD,
            "${action.slotId.value}|${action.towerId.value}",
        )
        is ProductBattleAction.UpgradeTower -> text(id, tick, UPGRADE, action.slotId.value)
        is ProductBattleAction.DeployAlly -> text(id, tick, DEPLOY, action.allyId.value)
        is ProductBattleAction.UseHeroSkill -> text(id, tick, HERO, action.skillId.value)
        is ProductBattleAction.ChooseEnhancement -> text(id, tick, ENHANCEMENT, action.enhancementId.value)
        ProductBattleAction.RerollEnhancements -> text(id, tick, REROLL, "")
    }

    fun decode(command: EngineCommand): ProductBattleAction? = try {
        when (command.type) {
            SET_PAUSED -> when (command.stablePayload()) {
                "0" -> ProductBattleAction.SetPaused(false)
                "1" -> ProductBattleAction.SetPaused(true)
                else -> null
            }
            SET_SPEED -> ProductBattleAction.SetSpeed(MySdBattleSpeed.valueOf(command.stablePayload()))
            BUILD -> {
                val parts = boundedBattleSplit(command.stablePayload(), '|', "build command", maximum = 2)
                if (parts.size != 2) null else ProductBattleAction.BuildTower(
                    ContentId.of(parts[0]),
                    ContentId.of(parts[1]),
                )
            }
            UPGRADE -> ProductBattleAction.UpgradeTower(ContentId.of(command.stablePayload()))
            DEPLOY -> ProductBattleAction.DeployAlly(ContentId.of(command.stablePayload()))
            HERO -> ProductBattleAction.UseHeroSkill(ContentId.of(command.stablePayload()))
            ENHANCEMENT -> ProductBattleAction.ChooseEnhancement(ContentId.of(command.stablePayload()))
            REROLL -> if (command.stablePayload().isEmpty()) ProductBattleAction.RerollEnhancements else null
            else -> null
        }
    } catch (_: IllegalArgumentException) {
        null
    }

    private fun text(id: CommandId, tick: Tick, type: String, payload: String) =
        TextCommand(id = id, scheduledTick = tick, type = type, payload = payload)
}

internal class ProductBattleReducer(
    private val catalog: ProductCatalog,
) {
    /** Inputs change only their explicit outcome; elapsed-time systems belong to [advance]. */
    fun applyCommands(state: ProductBattleState, commands: List<EngineCommand>) {
        commands.forEach { command ->
            BattleCommandCodec.decode(command)?.let { applyCommand(state, it) }
        }
        // Area attacks can finish a wave immediately. Retire killed entities and publish the
        // corresponding choice/terminal state without generating passive income or elapsed time.
        if (state.phase == MySdBattlePhase.COMBAT && state.enemies.any { it.health <= 0 }) {
            cleanupAndProgress(state, stage(state))
        }
    }

    fun advance(state: ProductBattleState, commands: List<EngineCommand>) {
        applyCommands(state, commands)
        if (state.tick >= BattleRuntimeAdapter.MAX_SAFE_TICK) {
            exhaustBattleClock(state)
            return
        }
        if (state.terminalResult != null || state.paused || state.phase == MySdBattlePhase.ENHANCEMENT) return
        if (state.phase == MySdBattlePhase.PREPARATION) state.phase = MySdBattlePhase.COMBAT

        val stage = stage(state)
        if (state.waveElapsedTicks >= MAX_PHASE_TICKS) {
            exhaustBattleClock(state)
            return
        }
        tickIncomeAndCooldowns(state, stage)
        spawnWave(state, stage)
        tickEnemies(state, stage)
        if (state.terminalResult != null) {
            // A hero pulse may kill one enemy on the same tick that another defeats the base.
            // Retire dead entities before a terminal snapshot/save is exposed.
            cleanupAndProgress(state, stage)
            return
        }
        tickAllies(state)
        tickTowers(state)
        cleanupAndProgress(state, stage)
    }

    private fun applyCommand(state: ProductBattleState, action: ProductBattleAction) {
        if (state.terminalResult != null) return
        if (state.paused && action !is ProductBattleAction.SetPaused && action !is ProductBattleAction.SetSpeed) return
        val stage = stage(state)
        when (action) {
            is ProductBattleAction.SetPaused -> state.paused = action.paused
            is ProductBattleAction.SetSpeed -> state.speed = action.speed
            is ProductBattleAction.BuildTower -> {
                if (state.phase == MySdBattlePhase.ENHANCEMENT) return
                val slot = state.slots.firstOrNull { it.id == action.slotId && it.towerId == null } ?: return
                val tower = catalog.towers[action.towerId] ?: return
                if (action.towerId !in stage.towerIds || action.towerId !in state.launch.loadoutIds) return
                if (state.resource < tower.buildCost) return
                state.resource -= tower.buildCost
                slot.towerId = action.towerId
                slot.level = 1
                slot.cooldownRemainingTicks = 0
            }
            is ProductBattleAction.UpgradeTower -> {
                if (state.phase == MySdBattlePhase.ENHANCEMENT) return
                val slot = state.slots.firstOrNull { it.id == action.slotId } ?: return
                val tower = slot.towerId?.let(catalog.towers::get) ?: return
                if (slot.level !in 1 until ProductBattleController.MAX_TOWER_LEVEL) return
                val cost = tower.upgradeCosts[slot.level - 1]
                if (state.resource < cost) return
                state.resource -= cost
                slot.level += 1
            }
            is ProductBattleAction.DeployAlly -> {
                if (state.phase == MySdBattlePhase.ENHANCEMENT || state.allies.size >= ProductBattleController.MAX_ALLIES) return
                val definition = catalog.allies[action.allyId] ?: return
                if (action.allyId !in stage.allyIds || action.allyId !in state.launch.loadoutIds) return
                if (state.resource < definition.deployCost) return
                val entityId = allocateEntityId(state) ?: return
                state.resource -= definition.deployCost
                val maxHealth = scaledAllyHealth(state, definition)
                state.allies += ProductAllyState(
                    entityId = entityId,
                    allyId = action.allyId,
                    health = maxHealth,
                    maxHealth = maxHealth,
                    positionTicks = max(0, stage.basePositionTicks - 45),
                )
            }
            is ProductBattleAction.UseHeroSkill -> {
                if (state.phase != MySdBattlePhase.COMBAT) return
                val skill = catalog.heroSkills[action.skillId] ?: return
                if (state.skillCooldowns[action.skillId] != 0) return
                when (skill.kind) {
                    ProductHeroSkillKind.BASE_REPAIR -> {
                        val amount = scale(skill.magnitude, heroPowerMultiplier(state, skill.id))
                        state.baseHealth = min(stage.baseHealth, state.baseHealth + amount)
                    }
                    ProductHeroSkillKind.AREA_PULSE -> {
                        val damage = scale(skill.magnitude, heroPowerMultiplier(state, skill.id))
                        state.enemies.forEach { enemy ->
                            val armor = catalog.enemies.getValue(enemy.enemyId).armor
                            enemy.health -= max(1, damage - armor)
                        }
                    }
                }
                state.skillCooldowns[action.skillId] = effectiveHeroCooldown(state, skill.cooldownTicks)
            }
            is ProductBattleAction.ChooseEnhancement -> {
                if (state.phase != MySdBattlePhase.ENHANCEMENT || action.enhancementId !in state.enhancementOffers) return
                val definition = catalog.enhancements[action.enhancementId] ?: return
                if (state.enhancements.count { it == action.enhancementId } >= definition.maxStacks) return
                state.enhancements += action.enhancementId
                // Existing entities and timers share the new authoritative modifier coordinate.
                // This also keeps a save taken immediately after a choice canonical.
                if (definition.kind == ProductEnhancementKind.ALLY_HEALTH) {
                    state.allies.forEach { ally ->
                        val maximum = scaledAllyHealth(state, catalog.allies.getValue(ally.allyId))
                        ally.health = min(maximum, ally.health + (maximum - ally.maxHealth))
                        ally.maxHealth = maximum
                    }
                }
                if (definition.kind == ProductEnhancementKind.HERO_COOLDOWN) {
                    state.skillCooldowns.replaceAll { id, remaining ->
                        min(remaining, effectiveHeroCooldown(state, catalog.heroSkills.getValue(id).cooldownTicks))
                    }
                }
                state.enhancementOffers.clear()
                advanceToNextWave(state, stage)
            }
            ProductBattleAction.RerollEnhancements -> {
                if (state.phase != MySdBattlePhase.ENHANCEMENT || state.rerollsRemaining <= 0) return
                state.rerollsRemaining -= 1
                rollEnhancementOffers(state, stage)
            }
        }
    }

    private fun tickIncomeAndCooldowns(state: ProductBattleState, stage: ProductStageDefinition) {
        state.slots.forEach { if (it.cooldownRemainingTicks > 0) it.cooldownRemainingTicks -= 1 }
        state.allies.forEach { if (it.cooldownRemainingTicks > 0) it.cooldownRemainingTicks -= 1 }
        state.enemies.forEach { if (it.cooldownRemainingTicks > 0) it.cooldownRemainingTicks -= 1 }
        state.skillCooldowns.replaceAll { _, value -> max(0, value - 1) }

        val supportIncome = state.slots.sumOf { slot ->
            val tower = slot.towerId?.let(catalog.towers::get)
            if (tower != null && ProductBattleRoleRules.generatesIncome(tower.role)) {
                tower.supportIncomePerSecond * slot.level
            } else {
                0
            }
        }
        val modifiers = modifiers(state)
        val enhancedIncome = scale(
            stage.incomePerSecond + supportIncome,
            1_000 + state.launch.economyPermille + modifiers.resourceIncomePermille,
        )
        state.incomeRemainder += enhancedIncome
        if (state.incomeRemainder >= BattleRuntimeAdapter.TICKS_PER_SECOND) {
            val whole = state.incomeRemainder / BattleRuntimeAdapter.TICKS_PER_SECOND
            state.resource = min(effectiveResourceCap(state, stage), state.resource + whole)
            state.incomeRemainder %= BattleRuntimeAdapter.TICKS_PER_SECOND
        }
    }

    private fun spawnWave(state: ProductBattleState, stage: ProductStageDefinition) {
        val wave = stage.waves[state.waveIndex]
        state.waveElapsedTicks += 1
        wave.groups.forEachIndexed { index, group ->
            val spawned = state.spawnedByGroup[index]
            if (spawned >= group.count) return@forEachIndexed
            val due = group.firstSpawnTick + spawned * group.intervalTicks
            if (state.waveElapsedTicks >= due) {
                val enemy = catalog.enemies.getValue(group.enemyId)
                val stageScale = 1_000 + (stage.ordinal - 1) * 90 + state.waveIndex * 35
                val maxHealth = scale(enemy.health, stageScale)
                val entityId = allocateEntityId(state) ?: return
                state.enemies += ProductEnemyState(
                    entityId = entityId,
                    enemyId = enemy.id,
                    health = maxHealth,
                    maxHealth = maxHealth,
                    positionTicks = 0,
                )
                state.spawnedByGroup[index] = spawned + 1
            }
        }
    }

    private fun tickEnemies(state: ProductBattleState, stage: ProductStageDefinition) {
        val leaking = mutableListOf<ProductEnemyState>()
        state.enemies.sortedBy(ProductEnemyState::entityId).forEach { enemyState ->
            if (enemyState.health <= 0) return@forEach
            val definition = catalog.enemies.getValue(enemyState.enemyId)
            if (enemyState.slowRemainingTicks > 0) {
                enemyState.slowRemainingTicks -= 1
                if (enemyState.slowRemainingTicks == 0) enemyState.slowPermille = 0
            }

            val target = if (ProductBattleRoleRules.ignoresAllies(definition.role)) null else state.allies
                .filter { it.health > 0 && it.positionTicks >= enemyState.positionTicks }
                .minWithOrNull(compareBy<ProductAllyState> { abs(it.positionTicks - enemyState.positionTicks) }.thenBy { it.entityId })
            if (target != null && abs(target.positionTicks - enemyState.positionTicks) <= definition.attackRangeTicks) {
                if (enemyState.cooldownRemainingTicks == 0) {
                    target.health -= scaledEnemyDamage(stage, definition.attackDamage)
                    enemyState.cooldownRemainingTicks = definition.cooldownTicks
                }
                return@forEach
            }

            if (ProductBattleRoleRules.attacksBaseAtRange(definition.role)) {
                if (stage.basePositionTicks - enemyState.positionTicks <= definition.attackRangeTicks) {
                    if (enemyState.cooldownRemainingTicks == 0) {
                        damageBase(state, stage, scaledEnemyDamage(stage, definition.attackDamage))
                        enemyState.cooldownRemainingTicks = definition.cooldownTicks
                    }
                    return@forEach
                }
            }

            val speed = max(1, multiplyPermille(definition.speedTicks, 1_000 - enemyState.slowPermille))
            enemyState.positionTicks += speed
            if (ProductBattleRoleRules.disruptsTowers(definition.role)) {
                state.slots.filter { abs(it.positionTicks - enemyState.positionTicks) <= 45 }
                    .forEach {
                        it.cooldownRemainingTicks = min(MAX_SAVED_COOLDOWN_TICKS, it.cooldownRemainingTicks + 1)
                    }
            }
            if (enemyState.positionTicks >= stage.basePositionTicks) {
                damageBase(state, stage, scaledEnemyDamage(stage, definition.baseLeakDamage))
                leaking += enemyState
            }
        }
        state.enemies.removeAll(leaking.toSet())
        state.allies.removeAll { it.health <= 0 }
    }

    private fun tickAllies(state: ProductBattleState) {
        state.allies.sortedBy(ProductAllyState::entityId).forEach { allyState ->
            if (allyState.health <= 0) return@forEach
            val definition = catalog.allies.getValue(allyState.allyId)
            val target = state.enemies.filter { it.health > 0 }
                .minWithOrNull(compareBy<ProductEnemyState> { abs(it.positionTicks - allyState.positionTicks) }.thenBy { it.entityId })
            if (target != null && abs(target.positionTicks - allyState.positionTicks) <= definition.attackRangeTicks) {
                if (allyState.cooldownRemainingTicks == 0) {
                    val raw = scaledAllyDamage(state, definition)
                    val armor = catalog.enemies.getValue(target.enemyId).armor
                    target.health -= max(1, raw - armor)
                    allyState.cooldownRemainingTicks = definition.cooldownTicks
                }
            } else {
                allyState.positionTicks = max(0, allyState.positionTicks - effectiveAllySpeed(state, definition))
            }
        }
    }

    private fun tickTowers(state: ProductBattleState) {
        state.slots.forEach { slot ->
            val tower = slot.towerId?.let(catalog.towers::get) ?: return@forEach
            if (slot.cooldownRemainingTicks > 0) return@forEach
            val range = effectiveTowerRange(state, tower)
            val target = state.enemies.asSequence()
                .filter { it.health > 0 && abs(it.positionTicks - slot.positionTicks) <= range }
                .sortedWith(compareByDescending<ProductEnemyState> { it.positionTicks }.thenBy { it.entityId })
                .firstOrNull() ?: return@forEach
            val damage = effectiveTowerDamage(state, tower, slot.level)
            applyTowerDamage(target, damage)
            val modifiers = modifiers(state)
            if (ProductBattleRoleRules.splashes(tower.role) && tower.splashRadiusTicks > 0) {
                val radius = scale(
                    tower.splashRadiusTicks,
                    1_000 + modifiers.splashRadiusPermille,
                )
                state.enemies.filter {
                    it.entityId != target.entityId && it.health > 0 && abs(it.positionTicks - target.positionTicks) <= radius
                }.forEach { applyTowerDamage(it, max(1, damage / 2)) }
            }
            if (ProductBattleRoleRules.slows(tower.role) && tower.slowPermille > 0) {
                val slow = min(
                    900,
                    tower.slowPermille + modifiers.slowStrengthPermille,
                )
                target.slowPermille = max(target.slowPermille, slow)
                target.slowRemainingTicks = max(target.slowRemainingTicks, tower.slowDurationTicks)
            }
            slot.cooldownRemainingTicks = effectiveTowerCooldown(state, tower, slot.level)
        }
    }

    private fun cleanupAndProgress(state: ProductBattleState, stage: ProductStageDefinition) {
        val defeated = state.enemies.filter { it.health <= 0 }
        if (defeated.isNotEmpty()) {
            val reward = defeated.sumOf { catalog.enemies.getValue(it.enemyId).killReward.toLong() }
            state.resource = min(effectiveResourceCap(state, stage).toLong(), state.resource.toLong() + reward).toInt()
            state.enemies.removeAll(defeated.toSet())
        }
        state.allies.removeAll { it.health <= 0 }
        if (state.baseHealth <= 0) {
            state.baseHealth = 0
            state.phase = MySdBattlePhase.DEFEAT
            state.terminalResult = MySdTerminalResult.DEFEAT
            return
        }

        val wave = stage.waves[state.waveIndex]
        val allSpawned = wave.groups.indices.all { state.spawnedByGroup[it] >= wave.groups[it].count }
        if (!allSpawned || state.enemies.isNotEmpty()) return
        val waveNumber = state.waveIndex + 1
        if (waveNumber == stage.waves.size) {
            state.phase = MySdBattlePhase.VICTORY
            state.terminalResult = MySdTerminalResult.VICTORY
        } else if (waveNumber in stage.enhancementAfterWaves) {
            state.phase = MySdBattlePhase.ENHANCEMENT
            state.rerollsRemaining = 1
            rollEnhancementOffers(state, stage)
        } else {
            advanceToNextWave(state, stage)
        }
    }

    private fun advanceToNextWave(state: ProductBattleState, stage: ProductStageDefinition) {
        state.waveIndex += 1
        state.waveElapsedTicks = 0
        state.spawnedByGroup.clear()
        state.spawnedByGroup += MutableList(stage.waves[state.waveIndex].groups.size) { 0 }
        state.phase = MySdBattlePhase.COMBAT
    }

    private fun rollEnhancementOffers(state: ProductBattleState, stage: ProductStageDefinition) {
        val eligible = stage.enhancementPoolIds.filter { id ->
            val definition = catalog.enhancements.getValue(id)
            state.enhancements.count { it == id } < definition.maxStacks
        }.toMutableList()
        state.enhancementOffers.clear()
        val offerCount = min(3, eligible.size)
        while (state.enhancementOffers.size < offerCount) {
            state.rngState = state.rngState * 6_364_136_223_846_793_005L + 1_442_695_040_888_963_407L
            val index = ((state.rngState ushr 1) % eligible.size.toLong()).toInt()
            state.enhancementOffers += eligible.removeAt(index)
        }
    }

    private fun damageBase(state: ProductBattleState, stage: ProductStageDefinition, rawDamage: Int) {
        val armor = modifiers(state).baseArmorPermille
        val damage = max(1, multiplyPermille(rawDamage, max(100, 1_000 - armor)))
        state.baseHealth = max(0, state.baseHealth - damage)
        if (state.baseHealth == 0) {
            state.phase = MySdBattlePhase.DEFEAT
            state.terminalResult = MySdTerminalResult.DEFEAT
        }
    }

    private fun applyTowerDamage(enemy: ProductEnemyState, rawDamage: Int) {
        val armor = catalog.enemies.getValue(enemy.enemyId).armor
        enemy.health -= max(1, rawDamage - armor)
    }

    private fun stage(state: ProductBattleState): ProductStageDefinition = catalog.stages.getValue(state.stageId)

    private fun effectiveTowerDamage(
        state: ProductBattleState,
        tower: ProductTowerDefinition,
        level: Int,
    ): Int {
        val base = tower.damage + (level - 1) * tower.damagePerLevel
        val roster = 1_000 + (state.launch.rosterLevels[tower.id]?.minus(1) ?: 0) * 100
        val multiplier = roster + state.launch.towerPowerPermille + modifiers(state).towerDamagePermille
        return scale(base, multiplier)
    }

    private fun effectiveTowerCooldown(
        state: ProductBattleState,
        tower: ProductTowerDefinition,
        level: Int,
    ): Int {
        val base = max(tower.minimumCooldownTicks, tower.cooldownTicks - (level - 1) * tower.cooldownReductionPerLevel)
        val reduction = modifiers(state).towerCooldownPermille
        return max(tower.minimumCooldownTicks, multiplyPermille(base, max(100, 1_000 - reduction)))
    }

    private fun effectiveTowerRange(state: ProductBattleState, tower: ProductTowerDefinition): Int =
        scale(tower.rangeTicks, 1_000 + modifiers(state).towerRangePermille)

    private fun scaledAllyHealth(state: ProductBattleState, ally: ProductAllyDefinition): Int {
        val roster = 1_000 + (state.launch.rosterLevels[ally.id]?.minus(1) ?: 0) * 100
        return scale(
            ally.health,
            roster + state.launch.allyPowerPermille + modifiers(state).allyHealthPermille,
        )
    }

    private fun scaledAllyDamage(state: ProductBattleState, ally: ProductAllyDefinition): Int {
        val roster = 1_000 + (state.launch.rosterLevels[ally.id]?.minus(1) ?: 0) * 100
        return scale(
            ally.damage,
            roster + state.launch.allyPowerPermille + modifiers(state).allyDamagePermille,
        )
    }

    private fun effectiveAllySpeed(state: ProductBattleState, ally: ProductAllyDefinition): Int = max(
        1,
        scale(
            ally.speedTicks,
            1_000 + modifiers(state).allySpeedPermille,
        ),
    )

    private fun heroPowerMultiplier(state: ProductBattleState, skillId: ContentId): Int =
        1_000 + state.launch.heroPowerPermille +
            (state.launch.rosterLevels[skillId]?.minus(1) ?: 0) * 100

    private fun effectiveHeroCooldown(state: ProductBattleState, base: Int): Int {
        val reduction = modifiers(state).heroCooldownPermille
        return max(BattleRuntimeAdapter.TICKS_PER_SECOND, multiplyPermille(base, max(100, 1_000 - reduction)))
    }

    private fun scaledEnemyDamage(stage: ProductStageDefinition, damage: Int): Int =
        scale(damage, 1_000 + (stage.ordinal - 1) * 75)

    private fun modifiers(state: ProductBattleState): ProductBattleModifiers =
        ProductBattleModifierResolver.resolve(catalog, state.enhancements)

    private fun effectiveResourceCap(state: ProductBattleState, stage: ProductStageDefinition): Int =
        scale(stage.resourceCap, 1_000 + modifiers(state).resourceCapPermille)

    private fun allocateEntityId(state: ProductBattleState): Long? {
        if (state.nextEntityId >= BattleRuntimeAdapter.MAX_SAFE_ID_CURSOR) {
            exhaustBattleClock(state)
            return null
        }
        return state.nextEntityId++
    }

    private fun exhaustBattleClock(state: ProductBattleState) {
        state.baseHealth = 0
        state.phase = MySdBattlePhase.DEFEAT
        state.terminalResult = MySdTerminalResult.DEFEAT
    }

    private fun scale(value: Int, permille: Int): Int = multiplyPermille(value, permille)

    private fun multiplyPermille(value: Int, permille: Int): Int =
        ((value.toLong() * permille.toLong()) / 1_000L).coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()

    companion object {
        const val MAX_PHASE_TICKS: Int = 1_000_000_000
        const val MAX_SAVED_COOLDOWN_TICKS: Int = 1_000_000
    }
}

private object ProductBattleProjector {
    fun project(state: ProductBattleState, catalog: ProductCatalog): ProductBattleStateView {
        val stage = catalog.stages.getValue(state.stageId)
        val resourceBonus = ProductBattleModifierResolver.resolve(catalog, state.enhancements).resourceCapPermille
        return ProductBattleStateView(
            runId = state.runId,
            stageId = state.stageId,
            loadoutIds = state.launch.loadoutIds.toList(),
            heroSkillIds = state.launch.heroSkillIds.toList(),
            rosterLevels = state.launch.rosterLevels.toMap(),
            towerPowerPermille = state.launch.towerPowerPermille,
            allyPowerPermille = state.launch.allyPowerPermille,
            economyPermille = state.launch.economyPermille,
            heroPowerPermille = state.launch.heroPowerPermille,
            tick = state.tick,
            phase = state.phase,
            paused = state.paused,
            speed = state.speed,
            waveIndex = state.waveIndex,
            waveElapsedTicks = state.waveElapsedTicks,
            spawnedByGroup = state.spawnedByGroup.toList(),
            resource = state.resource,
            resourceCap = ((stage.resourceCap.toLong() * (1_000L + resourceBonus.toLong())) / 1_000L)
                .coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
            baseHealth = state.baseHealth,
            baseMaxHealth = stage.baseHealth,
            slots = state.slots.map(ProductTowerSlotState::copy),
            allies = state.allies.map(ProductAllyState::copy),
            enemies = state.enemies.map(ProductEnemyState::copy),
            skillCooldowns = state.skillCooldowns.toMap(),
            skillCooldownTotals = state.launch.heroSkillIds.associateWith { skillId ->
                val base = catalog.heroSkills.getValue(skillId).cooldownTicks
                val reduction = state.enhancements.sumOf { enhancementId ->
                    catalog.enhancements.getValue(enhancementId)
                        .takeIf { it.kind == ProductEnhancementKind.HERO_COOLDOWN }
                        ?.magnitudePermille ?: 0
                }
                max(BattleRuntimeAdapter.TICKS_PER_SECOND, base.toLong()
                    .times(max(100, 1_000 - reduction).toLong())
                    .div(1_000L)
                    .coerceAtMost(Int.MAX_VALUE.toLong())
                    .toInt())
            },
            enhancements = state.enhancements.toList(),
            enhancementOffers = state.enhancementOffers.toList(),
            rerollsRemaining = state.rerollsRemaining,
            terminalResult = state.terminalResult,
        )
    }
}

private object ProductBattleHasher {
    fun hash(state: ProductBattleState, pending: List<EngineCommand>): String = stableHashOf {
        add(state.seed)
        add(state.runId)
        add(state.launch.stageId.value)
        state.launch.loadoutIds.forEach { add(it.value) }
        state.launch.heroSkillIds.forEach { add(it.value) }
        state.launch.rosterLevels.toSortedMap(compareBy(ContentId::value)).forEach { (id, level) -> add(id.value).add(level) }
        add(state.launch.towerPowerPermille)
        add(state.launch.allyPowerPermille)
        add(state.launch.economyPermille)
        add(state.launch.heroPowerPermille)
        add(state.tick)
        add(state.phase.name)
        add(state.paused)
        add(state.speed.name)
        add(state.waveIndex)
        add(state.waveElapsedTicks)
        state.spawnedByGroup.forEach { add(it) }
        add(state.resource)
        add(state.incomeRemainder)
        add(state.baseHealth)
        state.slots.forEach { slot ->
            add(slot.id.value).add(slot.positionTicks).add(slot.towerId?.value ?: "")
            add(slot.level).add(slot.cooldownRemainingTicks)
        }
        state.allies.sortedBy(ProductAllyState::entityId).forEach { ally ->
            add(ally.entityId).add(ally.allyId.value).add(ally.health).add(ally.maxHealth)
            add(ally.positionTicks).add(ally.cooldownRemainingTicks)
        }
        state.enemies.sortedBy(ProductEnemyState::entityId).forEach { enemy ->
            add(enemy.entityId).add(enemy.enemyId.value).add(enemy.health).add(enemy.maxHealth)
            add(enemy.positionTicks).add(enemy.cooldownRemainingTicks)
            add(enemy.slowRemainingTicks).add(enemy.slowPermille)
        }
        state.skillCooldowns.toSortedMap(compareBy(ContentId::value)).forEach { (id, cooldown) ->
            add(id.value).add(cooldown)
        }
        state.enhancements.forEach { add(it.value) }
        state.enhancementOffers.forEach { add(it.value) }
        add(state.rerollsRemaining).add(state.nextEntityId).add(state.nextCommandId).add(state.rngState)
        add(state.terminalResult?.name ?: "")
        pending.sortedWith(dev.myengine.core.CommandQueue.commandComparator).forEach { command ->
            add(command.id.value).add(command.scheduledTick.value).add(command.type)
            add(command.actorId ?: Long.MIN_VALUE).add(command.stablePayload())
        }
    }
}

private data class DecodedBattlePayload(
    val state: ProductBattleState,
    val pendingCommands: List<EngineCommand>,
)

private object BattlePayloadCodec {
    private const val BOUNDARY = "product-battle-runtime"

    fun encode(state: ProductBattleState, pendingCommands: List<EngineCommand>): String {
        val fields = linkedMapOf(
            "seed" to state.seed.toString(),
            "runId" to PersistenceWire.encodeText(state.runId),
            "stageId" to state.stageId.value,
            "loadout" to state.launch.loadoutIds.joinToString(",") { it.value },
            "heroLoadout" to state.launch.heroSkillIds.joinToString(",") { it.value },
            "roster" to state.launch.rosterLevels.toSortedMap(compareBy(ContentId::value))
                .entries.joinToString(",") { "${it.key.value}:${it.value}" },
            "towerPower" to state.launch.towerPowerPermille.toString(),
            "allyPower" to state.launch.allyPowerPermille.toString(),
            "economy" to state.launch.economyPermille.toString(),
            "heroPower" to state.launch.heroPowerPermille.toString(),
            "tick" to state.tick.toString(),
            "phase" to state.phase.name,
            "paused" to flag(state.paused),
            "speed" to state.speed.name,
            "waveIndex" to state.waveIndex.toString(),
            "waveElapsed" to state.waveElapsedTicks.toString(),
            "spawned" to state.spawnedByGroup.joinToString(","),
            "resource" to state.resource.toString(),
            "incomeRemainder" to state.incomeRemainder.toString(),
            "baseHealth" to state.baseHealth.toString(),
            "slots" to state.slots.joinToString(";") { slot ->
                listOf(
                    slot.id.value,
                    slot.positionTicks,
                    slot.towerId?.value ?: "~",
                    slot.level,
                    slot.cooldownRemainingTicks,
                ).joinToString(",")
            },
            "allies" to state.allies.sortedBy(ProductAllyState::entityId).joinToString(";") { ally ->
                listOf(
                    ally.entityId,
                    ally.allyId.value,
                    ally.health,
                    ally.maxHealth,
                    ally.positionTicks,
                    ally.cooldownRemainingTicks,
                ).joinToString(",")
            },
            "enemies" to state.enemies.sortedBy(ProductEnemyState::entityId).joinToString(";") { enemy ->
                listOf(
                    enemy.entityId,
                    enemy.enemyId.value,
                    enemy.health,
                    enemy.maxHealth,
                    enemy.positionTicks,
                    enemy.cooldownRemainingTicks,
                    enemy.slowRemainingTicks,
                    enemy.slowPermille,
                ).joinToString(",")
            },
            "skills" to state.skillCooldowns.toSortedMap(compareBy(ContentId::value))
                .entries.joinToString(",") { "${it.key.value}:${it.value}" },
            "enhancements" to state.enhancements.joinToString(",") { it.value },
            "offers" to state.enhancementOffers.joinToString(",") { it.value },
            "rerolls" to state.rerollsRemaining.toString(),
            "nextEntityId" to state.nextEntityId.toString(),
            "nextCommandId" to state.nextCommandId.toString(),
            "rngState" to state.rngState.toString(),
            "terminal" to (state.terminalResult?.name ?: ""),
            "pending" to pendingCommands.sortedWith(dev.myengine.core.CommandQueue.commandComparator)
                .joinToString(";") { command ->
                    listOf(
                        command.id.value,
                        command.scheduledTick.value,
                        command.type,
                        PersistenceWire.encodeText(command.stablePayload()),
                        command.actorId?.toString() ?: "~",
                    ).joinToString(",")
                },
        )
        return PersistenceWire.document(BOUNDARY, BattleRuntimeAdapter.CURRENT_SAVE_SCHEMA, fields)
    }

    fun decode(input: String, envelopeVersion: Int, catalog: ProductCatalog): DecodedBattlePayload {
        if (input.length > MAX_BATTLE_SAVE_CHARS) throw MalformedPersistenceException("Battle save exceeds the import limit.")
        var lines = 1
        input.forEach {
            if (it == '\n' && ++lines > 128) throw MalformedPersistenceException("Battle save contains too many fields.")
        }
        val document = PersistenceWire.parse(input, BOUNDARY, BattleRuntimeAdapter.CURRENT_SAVE_SCHEMA)
        if (document.version != envelopeVersion) {
            throw MalformedPersistenceException("Battle payload and envelope schema versions differ.")
        }
        if (document.version !in BattleRuntimeAdapter.OLDEST_SAVE_SCHEMA..BattleRuntimeAdapter.CURRENT_SAVE_SCHEMA) {
            throw MalformedPersistenceException("Unsupported battle payload schema.")
        }
        val common = setOf(
            "seed", "runId", "stageId", "loadout", "roster", "towerPower", "allyPower", "economy",
            "heroPower", "tick", "phase", "paused", "waveIndex", "waveElapsed", "spawned",
            "resource", "incomeRemainder", "baseHealth", "slots", "allies", "enemies", "skills",
            "enhancements", "offers", "rerolls", "nextEntityId", "nextCommandId", "rngState",
            "terminal", "pending",
        )
        PersistenceWire.requireExactKeys(
            document,
            if (document.version >= 5) common + setOf("speed", "heroLoadout") else common,
        )
        val fields = document.fields
        val stageId = contentId(fields, "stageId")
        val stage = catalog.stages[stageId] ?: throw MalformedPersistenceException("Unknown saved stage.")
        val restoredRoster = idIntMap(fields, "roster").toMutableMap().also { levels ->
            if (document.version == 4) catalog.heroSkills.keys.forEach { levels.putIfAbsent(it, 1) }
        }
        val launch = ProductBattleLaunch(
            stageId = stageId,
            loadoutIds = ids(fields, "loadout"),
            heroSkillIds = if (document.version >= 5) ids(fields, "heroLoadout") else stage.heroSkillIds,
            rosterLevels = restoredRoster,
            towerPowerPermille = nonNegative(fields, "towerPower"),
            allyPowerPermille = nonNegative(fields, "allyPower"),
            economyPermille = nonNegative(fields, "economy"),
            heroPowerPermille = nonNegative(fields, "heroPower"),
        )
        if (launch.loadoutIds.any { it !in catalog.towers && it !in catalog.allies } ||
            launch.heroSkillIds.any { it !in stage.heroSkillIds }
        ) {
            throw MalformedPersistenceException("Saved loadout contains unknown content.")
        }
        val slots = records(fields, "slots").map { record ->
            requireSize(record, 5, "slot")
            ProductTowerSlotState(
                id = parseId(record[0], "slot id"),
                positionTicks = parseNonNegativeInt(record[1], "slot position"),
                towerId = record[2].takeUnless { it == "~" }?.let { parseId(it, "tower id") },
                level = parseNonNegativeInt(record[3], "tower level"),
                cooldownRemainingTicks = parseNonNegativeInt(record[4], "tower cooldown"),
            )
        }.toMutableList()
        val allies = records(fields, "allies").map { record ->
            requireSize(record, 6, "ally")
            ProductAllyState(
                entityId = parseNonNegativeLong(record[0], "ally entity"),
                allyId = parseId(record[1], "ally id"),
                health = parseNonNegativeInt(record[2], "ally health"),
                maxHealth = parsePositiveInt(record[3], "ally max health"),
                positionTicks = parseNonNegativeInt(record[4], "ally position"),
                cooldownRemainingTicks = parseNonNegativeInt(record[5], "ally cooldown"),
            )
        }.toMutableList()
        val enemies = records(fields, "enemies").map { record ->
            requireSize(record, 8, "enemy")
            ProductEnemyState(
                entityId = parseNonNegativeLong(record[0], "enemy entity"),
                enemyId = parseId(record[1], "enemy id"),
                health = parseNonNegativeInt(record[2], "enemy health"),
                maxHealth = parsePositiveInt(record[3], "enemy max health"),
                positionTicks = parseNonNegativeInt(record[4], "enemy position"),
                cooldownRemainingTicks = parseNonNegativeInt(record[5], "enemy cooldown"),
                slowRemainingTicks = parseNonNegativeInt(record[6], "enemy slow duration"),
                slowPermille = parseNonNegativeInt(record[7], "enemy slow"),
            )
        }.toMutableList()
        val phase = enumValue<MySdBattlePhase>(fields, "phase")
        val terminal = fields.getValue("terminal").takeIf(String::isNotEmpty)?.let {
            enumValue<MySdTerminalResult>(it, "terminal")
        }
        val state = ProductBattleState(
            seed = fields.getValue("seed").toLongOrNull()
                ?: throw MalformedPersistenceException("Malformed seed."),
            runId = PersistenceWire.decodeText(fields, "runId"),
            launch = launch,
            tick = parseNonNegativeLong(fields.getValue("tick"), "tick"),
            phase = phase,
            paused = savedFlag(fields, "paused"),
            speed = if (document.version >= 5) enumValue(fields, "speed") else MySdBattleSpeed.ONE_X,
            waveIndex = parseNonNegativeInt(fields.getValue("waveIndex"), "wave index"),
            waveElapsedTicks = nonNegative(fields, "waveElapsed"),
            spawnedByGroup = intList(fields, "spawned").toMutableList(),
            resource = nonNegative(fields, "resource"),
            incomeRemainder = nonNegative(fields, "incomeRemainder"),
            baseHealth = nonNegative(fields, "baseHealth"),
            slots = slots,
            allies = allies,
            enemies = enemies,
            skillCooldowns = idIntMap(fields, "skills").toMutableMap(),
            enhancements = ids(fields, "enhancements").toMutableList(),
            enhancementOffers = ids(fields, "offers").toMutableList(),
            rerollsRemaining = nonNegative(fields, "rerolls"),
            nextEntityId = parseNonNegativeLong(fields.getValue("nextEntityId"), "next entity"),
            nextCommandId = parseNonNegativeLong(fields.getValue("nextCommandId"), "next command"),
            rngState = fields.getValue("rngState").toLongOrNull()
                ?: throw MalformedPersistenceException("Malformed RNG state."),
            terminalResult = terminal,
        )
        val pending = records(fields, "pending").map { record ->
            requireSize(record, 5, "pending command")
            TextCommand(
                id = CommandId(parseNonNegativeLong(record[0], "command id")),
                scheduledTick = Tick(parseNonNegativeLong(record[1], "scheduled tick")),
                type = record[2].also { if (it.isBlank()) throw MalformedPersistenceException("Blank command type.") },
                payload = decodeStandaloneText(record[3], "command payload"),
                actorId = record[4].takeUnless { it == "~" }?.toLongOrNull()
                    ?: if (record[4] == "~") null else throw MalformedPersistenceException("Malformed actor id."),
            )
        }
        validate(state, pending, stage, catalog)
        return DecodedBattlePayload(state, pending)
    }

    private fun validate(
        state: ProductBattleState,
        pending: List<EngineCommand>,
        stage: ProductStageDefinition,
        catalog: ProductCatalog,
    ) {
        if (state.runId != "run-${state.seed.toULong().toString(16)}-${stage.ordinal}") {
            throw MalformedPersistenceException("Run identity does not match its seed and stage.")
        }
        val allowedLoadout = stage.towerIds.toSet() + stage.allyIds.toSet()
        if (state.launch.loadoutIds.size !in 2..ProductBattleController.MAX_LOADOUT_SIZE ||
            state.launch.loadoutIds.distinct().size != state.launch.loadoutIds.size ||
            state.launch.loadoutIds.any { it !in allowedLoadout } ||
            state.launch.loadoutIds.none(catalog.towers::containsKey) ||
            state.launch.loadoutIds.none(catalog.allies::containsKey)
        ) throw MalformedPersistenceException("Invalid saved launch loadout.")
        if (state.launch.heroSkillIds.distinct().size != state.launch.heroSkillIds.size ||
            state.launch.heroSkillIds.any { it !in stage.heroSkillIds }
        ) throw MalformedPersistenceException("Invalid saved hero loadout.")
        val fullRoster = catalog.towers.keys + catalog.allies.keys + catalog.heroSkills.keys
        if (state.launch.rosterLevels.keys != fullRoster || state.launch.rosterLevels.values.any { it !in 1..5 }) {
            throw MalformedPersistenceException("Invalid saved roster levels.")
        }
        if (listOf(
                state.launch.towerPowerPermille,
                state.launch.allyPowerPermille,
                state.launch.economyPermille,
                state.launch.heroPowerPermille,
            ).any { it !in 0..5_000 }
        ) throw MalformedPersistenceException("Invalid saved launch bonuses.")
        if (state.tick !in 0..BattleRuntimeAdapter.MAX_SAFE_TICK ||
            state.waveIndex !in stage.waves.indices || state.waveElapsedTicks < 0
        ) {
            throw MalformedPersistenceException("Invalid saved battle coordinates.")
        }
        if (state.spawnedByGroup.size != stage.waves[state.waveIndex].groups.size) {
            throw MalformedPersistenceException("Saved spawn cursors do not match the current wave.")
        }
        if (state.waveElapsedTicks > ProductBattleReducer.MAX_PHASE_TICKS ||
            state.waveElapsedTicks.toLong() > state.tick
        ) throw MalformedPersistenceException("Saved wave clock is outside canonical bounds.")
        state.spawnedByGroup.forEachIndexed { index, count ->
            if (count !in 0..stage.waves[state.waveIndex].groups[index].count) {
                throw MalformedPersistenceException("Invalid saved spawn cursor.")
            }
        }
        val currentWave = stage.waves[state.waveIndex]
        val allSpawned = currentWave.groups.indices.all { index ->
            state.spawnedByGroup[index] == currentWave.groups[index].count
        }
        val waveNumber = state.waveIndex + 1
        val expectedEnhancementChoices = stage.enhancementAfterWaves.count { it < waveNumber }
        if (state.enhancements.size != expectedEnhancementChoices) {
            throw MalformedPersistenceException("Saved enhancement count does not match wave progress.")
        }
        when (state.phase) {
            MySdBattlePhase.PREPARATION -> if (
                state.waveIndex != 0 || state.waveElapsedTicks != 0 ||
                (state.tick > 0 && !state.paused) || state.resource > stage.initialResource || state.incomeRemainder != 0 ||
                state.baseHealth != stage.baseHealth || state.skillCooldowns.values.any { it != 0 } ||
                state.spawnedByGroup.any { it != 0 } || state.enemies.isNotEmpty() ||
                state.enhancements.isNotEmpty() || state.enhancementOffers.isNotEmpty()
            ) throw MalformedPersistenceException("Preparation state contains progressed battle data.")
            MySdBattlePhase.COMBAT -> if (allSpawned && state.enemies.isEmpty()) {
                throw MalformedPersistenceException("Completed combat wave was not advanced.")
            }
            MySdBattlePhase.ENHANCEMENT -> if (
                waveNumber !in stage.enhancementAfterWaves || !allSpawned || state.enemies.isNotEmpty()
            ) throw MalformedPersistenceException("Enhancement phase is not attached to a completed choice wave.")
            MySdBattlePhase.VICTORY -> if (
                state.waveIndex != stage.waves.lastIndex || !allSpawned || state.enemies.isNotEmpty()
            ) throw MalformedPersistenceException("Victory does not describe a cleared final wave.")
            MySdBattlePhase.DEFEAT -> Unit
        }
        val resourceBonus = state.enhancements.sumOf { id ->
            catalog.enhancements[id]?.takeIf { it.kind == ProductEnhancementKind.RESOURCE_CAP }?.magnitudePermille ?: 0
        }
        val resourceCap = ((stage.resourceCap.toLong() * (1_000L + resourceBonus.toLong())) / 1_000L)
            .coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        if (state.resource !in 0..resourceCap || state.incomeRemainder !in 0 until BattleRuntimeAdapter.TICKS_PER_SECOND) {
            throw MalformedPersistenceException("Invalid saved battle economy.")
        }
        if (state.baseHealth !in 0..stage.baseHealth) throw MalformedPersistenceException("Invalid saved base health.")
        val canonicalSlots = stage.buildSlots.associate { it.id to it.positionTicks }
        if (state.slots.size != stage.buildSlots.size ||
            state.slots.map { it.id }.distinct().size != state.slots.size ||
            state.slots.any { canonicalSlots[it.id] != it.positionTicks }
        ) {
            throw MalformedPersistenceException("Saved build slots do not match the stage.")
        }
        state.slots.forEach { slot ->
            if (slot.level !in 0..ProductBattleController.MAX_TOWER_LEVEL ||
                slot.cooldownRemainingTicks !in 0..ProductBattleReducer.MAX_SAVED_COOLDOWN_TICKS
            ) {
                throw MalformedPersistenceException("Invalid saved tower state.")
            }
            if ((slot.towerId == null) != (slot.level == 0)) throw MalformedPersistenceException("Invalid empty tower state.")
            if (slot.towerId != null && (slot.towerId !in stage.towerIds || slot.towerId !in state.launch.loadoutIds)) {
                throw MalformedPersistenceException("Invalid saved built tower.")
            }
        }
        val entityIds = state.allies.map { it.entityId } + state.enemies.map { it.entityId }
        if (entityIds.distinct().size != entityIds.size) throw MalformedPersistenceException("Duplicate battle entity id.")
        val modifiers = ProductBattleModifierResolver.resolve(catalog, state.enhancements)
        fun scaleCanonical(value: Int, permille: Int): Int =
            ((value.toLong() * permille.toLong()) / 1_000L).coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()
        if (state.allies.size > ProductBattleController.MAX_ALLIES || state.allies.any { allyState ->
                val definition = catalog.allies[allyState.allyId]
                val roster = state.launch.rosterLevels[allyState.allyId]
                val expectedMaxHealth = if (definition == null || roster == null) -1 else scaleCanonical(
                    definition.health,
                    1_000 + (roster - 1) * 100 + state.launch.allyPowerPermille + modifiers.allyHealthPermille,
                )
                allyState.entityId <= 0 || allyState.allyId !in stage.allyIds ||
                    allyState.allyId !in state.launch.loadoutIds || allyState.maxHealth != expectedMaxHealth ||
                    allyState.health !in 1..allyState.maxHealth || allyState.positionTicks !in 0..stage.basePositionTicks ||
                    allyState.cooldownRemainingTicks !in 0..(definition?.cooldownTicks ?: -1)
            } || state.enemies.size > currentWave.groups.sumOf(ProductSpawnGroup::count) ||
            state.enemies.any { enemyState ->
                val definition = catalog.enemies[enemyState.enemyId]
                val expectedMaxHealth = definition?.let {
                    scaleCanonical(it.health, 1_000 + (stage.ordinal - 1) * 90 + state.waveIndex * 35)
                } ?: -1
                val maximumSlowDuration = stage.towerIds.mapNotNull(catalog.towers::get)
                    .maxOfOrNull(ProductTowerDefinition::slowDurationTicks) ?: 0
                enemyState.entityId <= 0 || definition == null || enemyState.maxHealth != expectedMaxHealth ||
                    enemyState.health !in 1..enemyState.maxHealth ||
                    enemyState.positionTicks !in 0..stage.basePositionTicks ||
                    enemyState.cooldownRemainingTicks !in 0..definition.cooldownTicks ||
                    enemyState.slowRemainingTicks !in 0..maximumSlowDuration || enemyState.slowPermille !in 0..900 ||
                    ((enemyState.slowRemainingTicks == 0) != (enemyState.slowPermille == 0))
            }
        ) throw MalformedPersistenceException("Invalid saved entity.")
        if (state.nextEntityId <= (entityIds.maxOrNull() ?: 0L) ||
            state.nextEntityId > BattleRuntimeAdapter.MAX_SAFE_ID_CURSOR
        ) {
            throw MalformedPersistenceException("Invalid next entity cursor.")
        }
        val maximumSkillCooldowns = state.launch.heroSkillIds.associateWith { skillId ->
            val base = catalog.heroSkills.getValue(skillId).cooldownTicks
            max(
                BattleRuntimeAdapter.TICKS_PER_SECOND,
                scaleCanonical(base, max(100, 1_000 - modifiers.heroCooldownPermille)),
            )
        }
        if (state.skillCooldowns.keys != state.launch.heroSkillIds.toSet() || state.skillCooldowns.any { (id, value) ->
                value !in 0..maximumSkillCooldowns.getValue(id)
            }
        ) {
            throw MalformedPersistenceException("Invalid saved hero skills.")
        }
        if (state.enhancements.any { it !in stage.enhancementPoolIds } ||
            state.enhancements.groupingBy { it }.eachCount().any { (id, count) ->
                count > catalog.enhancements.getValue(id).maxStacks
            } || state.enhancementOffers.distinct().size != state.enhancementOffers.size ||
            state.enhancementOffers.any { offerId ->
                offerId !in stage.enhancementPoolIds ||
                    state.enhancements.count { it == offerId } >= catalog.enhancements.getValue(offerId).maxStacks
            }
        ) throw MalformedPersistenceException("Invalid saved enhancement state.")
        if (state.rerollsRemaining !in 0..1) throw MalformedPersistenceException("Invalid enhancement reroll state.")
        if (state.phase == MySdBattlePhase.ENHANCEMENT) {
            val eligibleCount = stage.enhancementPoolIds.count { id ->
                state.enhancements.count { it == id } < catalog.enhancements.getValue(id).maxStacks
            }
            if (state.enhancementOffers.size != min(3, eligibleCount)) {
                throw MalformedPersistenceException("Invalid enhancement choice state.")
            }
        } else if (state.enhancementOffers.isNotEmpty()) {
            throw MalformedPersistenceException("Unexpected enhancement offers.")
        }
        if (state.nextEntityId <= 0 || state.nextCommandId !in 1..BattleRuntimeAdapter.MAX_SAFE_ID_CURSOR) {
            throw MalformedPersistenceException("Invalid saved id cursor.")
        }
        if (pending.map { it.id }.distinct().size != pending.size || pending.any {
                it.id.value <= 0 || it.id.value >= state.nextCommandId ||
                    it.scheduledTick.value !in (state.tick + 1)..BattleRuntimeAdapter.MAX_SAFE_TICK ||
                    BattleCommandCodec.decode(it) == null
            }
        ) {
            throw MalformedPersistenceException("Invalid pending command ids.")
        }
        when (state.terminalResult) {
            MySdTerminalResult.VICTORY -> if (state.phase != MySdBattlePhase.VICTORY || state.baseHealth <= 0) {
                throw MalformedPersistenceException("Invalid saved victory.")
            }
            MySdTerminalResult.DEFEAT -> if (state.phase != MySdBattlePhase.DEFEAT || state.baseHealth != 0) {
                throw MalformedPersistenceException("Invalid saved defeat.")
            }
            null -> if (state.phase == MySdBattlePhase.VICTORY || state.phase == MySdBattlePhase.DEFEAT || state.baseHealth == 0) {
                throw MalformedPersistenceException("Invalid non-terminal battle.")
            }
        }
        if (state.tick == BattleRuntimeAdapter.MAX_SAFE_TICK && state.terminalResult == null) {
            throw MalformedPersistenceException("Exhausted battle clock must be terminal.")
        }
    }

    private fun flag(value: Boolean): String = if (value) "1" else "0"

    private fun savedFlag(fields: Map<String, String>, key: String): Boolean = when (fields.getValue(key)) {
        "0" -> false
        "1" -> true
        else -> throw MalformedPersistenceException("Malformed flag: $key")
    }

    private fun contentId(fields: Map<String, String>, key: String): ContentId = parseId(fields.getValue(key), key)

    private fun parseId(raw: String, field: String): ContentId = try {
        ContentId.of(raw)
    } catch (_: IllegalArgumentException) {
        throw MalformedPersistenceException("Malformed content id: $field")
    }

    private fun ids(fields: Map<String, String>, key: String): List<ContentId> =
        boundedBattleSplit(fields.getValue(key), ',', key).map { parseId(it, key) }

    private fun intList(fields: Map<String, String>, key: String): List<Int> =
        boundedBattleSplit(fields.getValue(key), ',', key).map { parseNonNegativeInt(it, key) }

    private fun idIntMap(fields: Map<String, String>, key: String): Map<ContentId, Int> {
        val result = linkedMapOf<ContentId, Int>()
        boundedBattleSplit(fields.getValue(key), ',', key).forEach { entry ->
            val parts = boundedBattleSplit(entry, ':', key, maximum = 2)
            requireSize(parts, 2, key)
            val id = parseId(parts[0], key)
            if (result.put(id, parseNonNegativeInt(parts[1], key)) != null) {
                throw MalformedPersistenceException("Duplicate map entry: $key")
            }
        }
        return result
    }

    private fun records(fields: Map<String, String>, key: String): List<List<String>> =
        boundedBattleSplit(fields.getValue(key), ';', key).map {
            boundedBattleSplit(it, ',', key, maximum = 8)
        }

    private fun nonNegative(fields: Map<String, String>, key: String): Int =
        parseNonNegativeInt(fields.getValue(key), key)

    private fun parseNonNegativeInt(raw: String, field: String): Int = raw.toIntOrNull()?.takeIf { it >= 0 }
        ?: throw MalformedPersistenceException("Malformed non-negative integer: $field")

    private fun parsePositiveInt(raw: String, field: String): Int = raw.toIntOrNull()?.takeIf { it > 0 }
        ?: throw MalformedPersistenceException("Malformed positive integer: $field")

    private fun parseNonNegativeLong(raw: String, field: String): Long = raw.toLongOrNull()?.takeIf { it >= 0 }
        ?: throw MalformedPersistenceException("Malformed non-negative long: $field")

    private inline fun <reified T : Enum<T>> enumValue(fields: Map<String, String>, key: String): T =
        enumValue(fields.getValue(key), key)

    private inline fun <reified T : Enum<T>> enumValue(raw: String, field: String): T = try {
        enumValueOf<T>(raw)
    } catch (_: IllegalArgumentException) {
        throw MalformedPersistenceException("Malformed enum: $field")
    }

    private fun requireSize(parts: List<String>, size: Int, field: String) {
        if (parts.size != size) throw MalformedPersistenceException("Malformed record: $field")
    }

    private fun decodeStandaloneText(raw: String, field: String): String =
        PersistenceWire.decodeText(mapOf(field to raw), field)
}
