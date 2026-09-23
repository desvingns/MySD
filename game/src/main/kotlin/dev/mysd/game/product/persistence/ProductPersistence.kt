package dev.mysd.game.product.persistence

import dev.mysd.game.content.ContentId
import dev.mysd.game.persistence.FutureSchemaVersionException
import dev.mysd.game.persistence.MalformedPersistenceException
import dev.mysd.game.persistence.PersistenceException
import dev.mysd.game.persistence.PersistenceWire
import dev.mysd.game.persistence.ProfileStore
import dev.mysd.game.persistence.ProfileStoreCodec
import dev.mysd.game.persistence.UnsupportedSchemaVersionException
import dev.mysd.game.product.MySdRoute
import dev.mysd.game.product.MySdSettingId
import dev.mysd.game.product.MySdTerminalResult
import dev.mysd.game.product.content.ProductCatalog
import dev.mysd.game.product.meta.ProductArenaState
import dev.mysd.game.product.meta.ProductLedgerEntry
import dev.mysd.game.product.meta.ProductLedgerKind
import dev.mysd.game.product.meta.ProductProfileManager
import dev.mysd.game.product.meta.ProductProfileState
import dev.mysd.game.product.meta.ProductStageProgress
import dev.mysd.game.product.runtime.ProductBattleSaveEnvelope

private const val MAX_DOCUMENT_CHARS: Int = 2_000_000
private const val MAX_COLLECTION_ITEMS: Int = 10_000
private const val MAX_FIELD_CHARS: Int = 1_000_000

private fun requireDocumentBound(input: String, label: String, maximumLines: Int = 128) {
    if (input.length > MAX_DOCUMENT_CHARS) {
        throw MalformedPersistenceException("$label exceeds the import limit.")
    }
    var lines = 1
    input.forEach {
        if (it == '\n' && ++lines > maximumLines) {
            throw MalformedPersistenceException("$label contains too many fields.")
        }
    }
}

private fun boundedSplit(raw: String, delimiter: Char, field: String, maximum: Int = MAX_COLLECTION_ITEMS): List<String> {
    if (raw.isEmpty()) return emptyList()
    if (raw.length > MAX_FIELD_CHARS) throw MalformedPersistenceException("Persistence field is too large: $field")
    var count = 1
    raw.forEach { if (it == delimiter && ++count > maximum) throw MalformedPersistenceException("Too many items: $field") }
    return raw.split(delimiter)
}

internal sealed interface ProductRunDecodeResult {
    data class Decoded(val save: ProductBattleSaveEnvelope) : ProductRunDecodeResult
    data class Incompatible(val expected: String, val actual: String) : ProductRunDecodeResult
    data class Invalid(val reason: String) : ProductRunDecodeResult
}

/** Transport codec for the opaque engine-owned run envelope. */
internal object ProductRunSaveCodec {
    const val CURRENT_SCHEMA_VERSION: Int = 1
    private const val BOUNDARY = "product-run-envelope"
    private val KEYS = setOf(
        "runtimeId",
        "contentPackId",
        "contentPackVersion",
        "runtimeSchemaId",
        "runtimeSchemaVersion",
        "payload",
    )

    fun encode(save: ProductBattleSaveEnvelope): String = PersistenceWire.document(
        BOUNDARY,
        CURRENT_SCHEMA_VERSION,
        linkedMapOf(
            "runtimeId" to PersistenceWire.encodeText(save.runtimeId),
            "contentPackId" to PersistenceWire.encodeText(save.contentPackId),
            "contentPackVersion" to PersistenceWire.encodeText(save.contentPackVersion),
            "runtimeSchemaId" to PersistenceWire.encodeText(save.schemaId),
            "runtimeSchemaVersion" to save.schemaVersion.toString(),
            "payload" to PersistenceWire.encodeText(save.payload),
        ),
    )

    fun decode(input: String): ProductRunDecodeResult = try {
        requireDocumentBound(input, "run save")
        val document = PersistenceWire.parse(input, BOUNDARY, CURRENT_SCHEMA_VERSION)
        PersistenceWire.requireExactKeys(document, KEYS)
        val fields = document.fields
        val schemaVersion = PersistenceWire.int(fields, "runtimeSchemaVersion")
        if (schemaVersion <= 0) throw MalformedPersistenceException("Invalid runtime schema version.")
        ProductRunDecodeResult.Decoded(
            ProductBattleSaveEnvelope(
                runtimeId = PersistenceWire.decodeText(fields, "runtimeId"),
                contentPackId = PersistenceWire.decodeText(fields, "contentPackId"),
                contentPackVersion = PersistenceWire.decodeText(fields, "contentPackVersion"),
                schemaId = PersistenceWire.decodeText(fields, "runtimeSchemaId"),
                schemaVersion = schemaVersion,
                payload = PersistenceWire.decodeText(fields, "payload"),
            ),
        )
    } catch (failure: FutureSchemaVersionException) {
        ProductRunDecodeResult.Incompatible(
            expected = CURRENT_SCHEMA_VERSION.toString(),
            actual = failure.version.toString(),
        )
    } catch (failure: PersistenceException) {
        ProductRunDecodeResult.Invalid(failure.message ?: "Invalid run save.")
    } catch (failure: IllegalArgumentException) {
        ProductRunDecodeResult.Invalid(failure.message ?: "Invalid run save.")
    }
}

internal sealed interface ProductProfileDecodeResult {
    data class Decoded(val profile: ProductProfileState, val migrated: Boolean) : ProductProfileDecodeResult
    data class Incompatible(val expected: String, val actual: String) : ProductProfileDecodeResult
    data class Invalid(val reason: String) : ProductProfileDecodeResult
}

/** Explicit hero selection in schema 4; schema 3 and legacy 1/2 stores retain auto-equip on migration. */
internal object ProductProfileCodec {
    const val CURRENT_SCHEMA_VERSION: Int = 4
    private const val BOUNDARY = "product-profile"
    private const val LEGACY_HEADER = "mysd.persistence|profile-store"
    private val KEYS = setOf(
        "profileId",
        "sessionSeed",
        "revision",
        "nextEventId",
        "nextRunOrdinal",
        "route",
        "selectedStage",
        "serviceMessage",
        "softCurrency",
        "premiumShaped",
        "energy",
        "maximumEnergy",
        "energyAnchor",
        "energyObserved",
        "unlockedStages",
        "stageProgress",
        "rosterLevels",
        "unlockedRoster",
        "loadout",
        "selectedHeroSkillIds",
        "unlockedTech",
        "rewardTrackPoints",
        "claimedRewardTiers",
        "claimedBattleRuns",
        "shopPurchases",
        "settings",
        "serviceHistory",
        "ledger",
        "nextLedgerId",
        "arenaFormation",
        "arenaPlayerPower",
        "arenaOpponentPower",
        "arenaResult",
        "arenaRunCount",
    )

    fun encode(value: ProductProfileState, catalog: ProductCatalog): String {
        validate(value, catalog)
        val fields = linkedMapOf(
            "profileId" to PersistenceWire.encodeText(value.profileId),
            "sessionSeed" to value.sessionSeed.toString(),
            "revision" to value.revision.toString(),
            "nextEventId" to value.nextEventId.toString(),
            "nextRunOrdinal" to value.nextRunOrdinal.toString(),
            "route" to value.route.name,
            "selectedStage" to (value.selectedStageId?.value ?: ""),
            "serviceMessage" to (value.serviceMessageCode?.let(PersistenceWire::encodeText) ?: ""),
            "softCurrency" to value.softCurrency.toString(),
            "premiumShaped" to value.premiumShaped.toString(),
            "energy" to value.energy.toString(),
            "maximumEnergy" to value.maximumEnergy.toString(),
            "energyAnchor" to value.lastEnergyEpochSeconds.toString(),
            "energyObserved" to value.lastObservedEpochSeconds.toString(),
            "unlockedStages" to value.unlockedStages.sortedBy(ContentId::value).joinToString(",") { it.value },
            "stageProgress" to value.stageProgress.toSortedMap(compareBy(ContentId::value)).entries
                .joinToString(";") { (id, progress) -> "${id.value},${progress.bestStars},${progress.clearCount}" },
            "rosterLevels" to value.rosterLevels.toSortedMap(compareBy(ContentId::value)).entries
                .joinToString(",") { (id, level) -> "${id.value}:$level" },
            "unlockedRoster" to value.unlockedRoster.sortedBy(ContentId::value).joinToString(",") { it.value },
            "loadout" to value.loadoutIds.joinToString(",") { it.value },
            "selectedHeroSkillIds" to value.selectedHeroSkillIds.joinToString(",") { it.value },
            "unlockedTech" to value.unlockedTech.sortedBy(ContentId::value).joinToString(",") { it.value },
            "rewardTrackPoints" to value.rewardTrackPoints.toString(),
            "claimedRewardTiers" to value.claimedRewardTiers.sortedBy(ContentId::value).joinToString(",") { it.value },
            "claimedBattleRuns" to encodeTexts(value.claimedBattleRuns.sorted()),
            "shopPurchases" to value.shopPurchases.toSortedMap(compareBy(ContentId::value)).entries
                .joinToString(",") { (id, count) -> "${id.value}:$count" },
            "settings" to MySdSettingId.entries.joinToString(",") { id ->
                "${id.name}:${if (value.settings.getValue(id)) 1 else 0}"
            },
            "serviceHistory" to encodeTexts(value.localServiceHistory),
            "ledger" to value.ledger.sortedBy(ProductLedgerEntry::id).joinToString(";") { entry ->
                listOf(
                    entry.id,
                    entry.kind.name,
                    PersistenceWire.encodeText(entry.sourceId),
                    entry.softDelta,
                    entry.premiumDelta,
                    entry.energyDelta,
                    entry.rewardTrackDelta,
                ).joinToString(",")
            },
            "nextLedgerId" to value.nextLedgerId.toString(),
            "arenaFormation" to PersistenceWire.encodeText(value.arena.opponentFormationId),
            "arenaPlayerPower" to value.arena.playerPower.toString(),
            "arenaOpponentPower" to value.arena.opponentPower.toString(),
            "arenaResult" to (value.arena.result?.name ?: ""),
            "arenaRunCount" to value.arena.runCount.toString(),
        )
        return PersistenceWire.document(BOUNDARY, CURRENT_SCHEMA_VERSION, fields)
    }

    fun decode(input: String, catalog: ProductCatalog): ProductProfileDecodeResult {
        try {
            requireDocumentBound(
                input,
                "profile save",
                maximumLines = if (input.startsWith(LEGACY_HEADER)) 20_000 else 128,
            )
        } catch (failure: PersistenceException) {
            return ProductProfileDecodeResult.Invalid(failure.message ?: "Invalid profile save.")
        }
        if (input.startsWith(LEGACY_HEADER)) return decodeLegacy(input, catalog)
        return try {
            val document = PersistenceWire.parse(input, BOUNDARY, CURRENT_SCHEMA_VERSION)
            if (document.version !in 3..CURRENT_SCHEMA_VERSION) {
                return ProductProfileDecodeResult.Incompatible(
                    expected = CURRENT_SCHEMA_VERSION.toString(),
                    actual = document.version.toString(),
                )
            }
            PersistenceWire.requireExactKeys(document, if (document.version == 3) KEYS - "selectedHeroSkillIds" else KEYS)
            val fields = document.fields
            val unlockedRoster = ids(fields, "unlockedRoster").toMutableSet()
            val state = ProductProfileState(
                profileId = PersistenceWire.decodeText(fields, "profileId"),
                sessionSeed = long(fields, "sessionSeed"),
                revision = nonNegativeLong(fields, "revision"),
                nextEventId = positiveLong(fields, "nextEventId"),
                nextRunOrdinal = positiveLong(fields, "nextRunOrdinal"),
                route = enumValue(fields, "route"),
                selectedStageId = fields.getValue("selectedStage").takeIf(String::isNotEmpty)?.let {
                    parseId(it, "selected stage")
                },
                serviceMessageCode = fields.getValue("serviceMessage").takeIf(String::isNotEmpty)?.let {
                    decodeStandaloneText(it, "service message")
                },
                softCurrency = nonNegativeLong(fields, "softCurrency"),
                premiumShaped = nonNegativeLong(fields, "premiumShaped"),
                energy = nonNegativeInt(fields, "energy"),
                maximumEnergy = positiveInt(fields, "maximumEnergy"),
                lastEnergyEpochSeconds = nonNegativeLong(fields, "energyAnchor"),
                lastObservedEpochSeconds = nonNegativeLong(fields, "energyObserved"),
                unlockedStages = ids(fields, "unlockedStages").toMutableSet(),
                stageProgress = progressMap(fields.getValue("stageProgress")),
                rosterLevels = idIntMap(fields, "rosterLevels").toMutableMap(),
                unlockedRoster = unlockedRoster,
                loadoutIds = ids(fields, "loadout").toMutableList(),
                selectedHeroSkillIds = if (document.version == 3) {
                    catalog.heroSkills.keys.filter(unlockedRoster::contains).sortedBy(ContentId::value).toMutableList()
                } else {
                    ids(fields, "selectedHeroSkillIds").toMutableList()
                },
                unlockedTech = ids(fields, "unlockedTech").toMutableSet(),
                rewardTrackPoints = nonNegativeInt(fields, "rewardTrackPoints"),
                claimedRewardTiers = ids(fields, "claimedRewardTiers").toMutableSet(),
                claimedBattleRuns = decodeTexts(fields.getValue("claimedBattleRuns"), "claimed battle runs").toMutableSet(),
                shopPurchases = idIntMap(fields, "shopPurchases").toMutableMap(),
                settings = settings(fields.getValue("settings")),
                localServiceHistory = decodeTexts(fields.getValue("serviceHistory"), "service history").toMutableList(),
                ledger = ledger(fields.getValue("ledger")),
                nextLedgerId = positiveLong(fields, "nextLedgerId"),
                arena = ProductArenaState(
                    opponentFormationId = PersistenceWire.decodeText(fields, "arenaFormation"),
                    playerPower = nonNegativeInt(fields, "arenaPlayerPower"),
                    opponentPower = nonNegativeInt(fields, "arenaOpponentPower"),
                    result = fields.getValue("arenaResult").takeIf(String::isNotEmpty)?.let {
                        enumValue<MySdTerminalResult>(it, "arena result")
                    },
                    runCount = nonNegativeInt(fields, "arenaRunCount"),
                ),
            )
            validate(state, catalog)
            ProductProfileDecodeResult.Decoded(state, migrated = document.version < CURRENT_SCHEMA_VERSION)
        } catch (failure: FutureSchemaVersionException) {
            ProductProfileDecodeResult.Incompatible(
                expected = CURRENT_SCHEMA_VERSION.toString(),
                actual = failure.version.toString(),
            )
        } catch (failure: UnsupportedSchemaVersionException) {
            ProductProfileDecodeResult.Incompatible(
                expected = CURRENT_SCHEMA_VERSION.toString(),
                actual = failure.version.toString(),
            )
        } catch (failure: PersistenceException) {
            ProductProfileDecodeResult.Invalid(failure.message ?: "Invalid product profile.")
        } catch (failure: IllegalArgumentException) {
            ProductProfileDecodeResult.Invalid(failure.message ?: "Invalid product profile.")
        } catch (failure: RuntimeException) {
            ProductProfileDecodeResult.Invalid(failure.message ?: "Invalid product profile.")
        }
    }

    private fun decodeLegacy(input: String, catalog: ProductCatalog): ProductProfileDecodeResult = try {
        ProductProfileDecodeResult.Decoded(migrate(ProfileStoreCodec.decode(input), catalog), migrated = true)
    } catch (failure: FutureSchemaVersionException) {
        ProductProfileDecodeResult.Incompatible(
            expected = ProfileStoreCodec.CURRENT_SCHEMA_VERSION.toString(),
            actual = failure.version.toString(),
        )
    } catch (failure: PersistenceException) {
        ProductProfileDecodeResult.Invalid(failure.message ?: "Invalid legacy profile.")
    } catch (failure: IllegalArgumentException) {
        ProductProfileDecodeResult.Invalid(failure.message ?: "Invalid legacy profile.")
    } catch (failure: RuntimeException) {
        ProductProfileDecodeResult.Invalid(failure.message ?: "Invalid legacy profile.")
    }

    private fun migrate(legacy: ProfileStore, catalog: ProductCatalog): ProductProfileState {
        val seed = legacy.profileId.fold(0x4D795344L) { acc, char -> acc * 31L + char.code }
        val manager = ProductProfileManager.create(seed, catalog)
        val state = manager.state
        state.profileId = legacy.profileId
        val validStages = legacy.unlockedStages.mapNotNull { raw ->
            runCatching { ContentId.of(raw) }.getOrNull()?.takeIf(catalog.stages::containsKey)
        }.toSet()
        catalog.orderedStages.drop(1).forEach { stage ->
            if (stage.id in validStages && catalog.orderedStages[stage.ordinal - 2].id in state.unlockedStages) {
                state.unlockedStages += stage.id
            }
        }
        state.softCurrency = legacy.currencies["soft"] ?: legacy.currencies["gold"] ?: state.softCurrency
        state.premiumShaped = legacy.currencies["premium-shaped"]
            ?: legacy.currencies["gems"]
            ?: state.premiumShaped
        state.energy = legacy.energy.coerceIn(0, state.maximumEnergy)
        val requestedTech = legacy.tech.mapNotNull { raw ->
            runCatching { ContentId.of(raw) }.getOrNull()
        }.toSet()
        catalog.techNodes.values
            .sortedWith(compareBy({ it.prerequisites.size }, { it.id.value }))
            .forEach { node ->
                if (node.id in requestedTech && state.unlockedTech.containsAll(node.prerequisites)) {
                    state.unlockedTech += node.id
                }
            }
        // Legacy claims had no reward-track point coordinate, so they cannot be migrated safely.
        state.claimedRewardTiers.clear()
        state.localServiceHistory += legacy.localServiceHistory.filter(String::isNotBlank)
            .takeLast(ProductProfileManager.MAX_SERVICE_HISTORY)
        state.ledger.clear()
        state.ledger += ProductLedgerEntry(
            id = 1,
            kind = ProductLedgerKind.INITIAL_GRANT,
            sourceId = "legacy-profile-migration",
            softDelta = state.softCurrency,
            premiumDelta = state.premiumShaped,
            energyDelta = state.energy,
        )
        state.nextLedgerId = 2
        validate(state, catalog)
        return state
    }

    private fun validate(value: ProductProfileState, catalog: ProductCatalog) {
        if (value.profileId.isBlank()) throw MalformedPersistenceException("Blank profile id.")
        if (value.revision !in 0..ProductProfileManager.MAX_COUNTER.toLong() ||
            value.nextEventId !in 1..ProductProfileManager.MAX_COUNTER.toLong() ||
            value.nextRunOrdinal !in 1..ProductProfileManager.MAX_COUNTER.toLong()
        ) {
            throw MalformedPersistenceException("Invalid profile cursors.")
        }
        if (value.softCurrency !in 0..ProductProfileManager.MAX_CURRENCY ||
            value.premiumShaped !in 0..ProductProfileManager.MAX_CURRENCY
        ) {
            throw MalformedPersistenceException("Negative profile currency.")
        }
        if (value.maximumEnergy != ProductProfileManager.DEFAULT_MAX_ENERGY || value.energy !in 0..value.maximumEnergy) {
            throw MalformedPersistenceException("Invalid profile energy.")
        }
        if (value.lastEnergyEpochSeconds < 0 || value.lastObservedEpochSeconds < 0 ||
            value.lastObservedEpochSeconds < value.lastEnergyEpochSeconds
        ) throw MalformedPersistenceException("Invalid energy clock.")
        if (value.unlockedStages.isEmpty() || !catalog.stages.keys.containsAll(value.unlockedStages)) {
            throw MalformedPersistenceException("Invalid unlocked stages.")
        }
        val unlockedOrdinals = value.unlockedStages.map { catalog.stages.getValue(it).ordinal }.sorted()
        if (unlockedOrdinals != (1..unlockedOrdinals.last()).toList()) {
            throw MalformedPersistenceException("Unlocked stages must form a contiguous campaign prefix.")
        }
        value.selectedStageId?.let {
            if (it !in catalog.stages) throw MalformedPersistenceException("Unknown selected stage.")
        }
        val selectedStage = value.selectedStageId
        if (value.route == MySdRoute.STAGE_SETUP && (selectedStage == null || selectedStage !in value.unlockedStages)) {
            throw MalformedPersistenceException("Stage setup points to a locked stage.")
        }
        value.stageProgress.forEach { (id, progress) ->
            if (id !in catalog.stages || id !in value.unlockedStages || progress.bestStars !in 0..3 ||
                progress.clearCount !in 0..ProductProfileManager.MAX_COUNTER ||
                (progress.clearCount == 0 && progress.bestStars != 0)
            ) throw MalformedPersistenceException("Invalid stage progress.")
            if (progress.clearCount > 0) {
                catalog.stages.getValue(id).unlocksStageId?.let { unlocked ->
                    if (unlocked !in value.unlockedStages) {
                        throw MalformedPersistenceException("Cleared stage did not unlock its successor.")
                    }
                }
            }
        }
        val rosterIds = catalog.towers.keys + catalog.allies.keys
        val fullRosterIds = rosterIds + catalog.heroSkills.keys
        if (value.rosterLevels.keys != fullRosterIds || value.rosterLevels.values.any { it !in 1..ProductProfileManager.MAX_ROSTER_LEVEL }) {
            throw MalformedPersistenceException("Invalid roster levels.")
        }
        if (value.unlockedRoster.isEmpty() || !fullRosterIds.containsAll(value.unlockedRoster) ||
            value.unlockedRoster.none(catalog.towers::containsKey) ||
            value.unlockedRoster.none(catalog.allies::containsKey) ||
            value.unlockedRoster.none(catalog.heroSkills::containsKey)
        ) throw MalformedPersistenceException("Invalid unlocked roster.")
        val clearedOrdinal = value.stageProgress.entries
            .filter { it.value.clearCount > 0 }
            .maxOfOrNull { catalog.stages.getValue(it.key).ordinal } ?: 0
        val expectedUnlockedRoster = buildSet {
            catalog.towers.values.filter { it.unlockAfterStageOrdinal <= clearedOrdinal }.forEach { add(it.id) }
            catalog.allies.values.filter { it.unlockAfterStageOrdinal <= clearedOrdinal }.forEach { add(it.id) }
            catalog.heroSkills.values.filter { it.unlockAfterStageOrdinal <= clearedOrdinal }.forEach { add(it.id) }
        }
        if (value.unlockedRoster != expectedUnlockedRoster) {
            throw MalformedPersistenceException("Unlocked roster does not match campaign progress.")
        }
        if (value.loadoutIds.size !in 2..ProductProfileManager.MAX_LOADOUT_SIZE ||
            value.loadoutIds.distinct().size != value.loadoutIds.size ||
            !rosterIds.containsAll(value.loadoutIds) ||
            !value.unlockedRoster.containsAll(value.loadoutIds) ||
            value.loadoutIds.none(catalog.towers::containsKey) ||
            value.loadoutIds.none(catalog.allies::containsKey)
        ) throw MalformedPersistenceException("Invalid loadout.")
        if (value.selectedHeroSkillIds.size > ProductProfileManager.MAX_HERO_SKILLS ||
            value.selectedHeroSkillIds.distinct().size != value.selectedHeroSkillIds.size ||
            value.selectedHeroSkillIds != value.selectedHeroSkillIds.sortedBy(ContentId::value) ||
            !catalog.heroSkills.keys.containsAll(value.selectedHeroSkillIds) ||
            !value.unlockedRoster.containsAll(value.selectedHeroSkillIds)
        ) throw MalformedPersistenceException("Invalid selected hero skills.")
        if (!catalog.techNodes.keys.containsAll(value.unlockedTech) || value.unlockedTech.any { nodeId ->
                !value.unlockedTech.containsAll(catalog.techNodes.getValue(nodeId).prerequisites)
            }
        ) throw MalformedPersistenceException("Invalid technology graph state.")
        if (value.rewardTrackPoints !in 0..ProductProfileManager.MAX_REWARD_TRACK_POINTS ||
            value.claimedRewardTiers.size > MAX_COLLECTION_ITEMS || value.claimedRewardTiers.any { claimed ->
                val tier = catalog.rewardTiers.firstOrNull { it.id == claimed }
                tier == null || value.rewardTrackPoints < tier.requiredPoints
            }
        ) throw MalformedPersistenceException("Invalid reward track state.")
        if (value.claimedBattleRuns.size > MAX_COLLECTION_ITEMS || value.claimedBattleRuns.any(String::isBlank)) {
            throw MalformedPersistenceException("Invalid battle claims.")
        }
        if (value.shopPurchases.size > catalog.shopOffers.size || value.shopPurchases.any { (id, count) ->
                id !in catalog.shopOffers || count !in 0..ProductProfileManager.MAX_COUNTER ||
                    (!catalog.shopOffers.getValue(id).repeatable && count > 1)
            }
        ) {
            throw MalformedPersistenceException("Invalid shop purchase state.")
        }
        if (value.settings.keys != MySdSettingId.entries.toSet()) throw MalformedPersistenceException("Invalid settings.")
        if (value.localServiceHistory.size > ProductProfileManager.MAX_SERVICE_HISTORY ||
            value.localServiceHistory.any(String::isBlank)
        ) throw MalformedPersistenceException("Invalid service history.")
        if (value.ledger.size > MAX_COLLECTION_ITEMS ||
            value.ledger.map(ProductLedgerEntry::id).distinct().size != value.ledger.size ||
            value.ledger.any {
                it.id <= 0 || it.sourceId.isBlank() ||
                    it.softDelta !in -ProductProfileManager.MAX_CURRENCY..ProductProfileManager.MAX_CURRENCY ||
                    it.premiumDelta !in -ProductProfileManager.MAX_CURRENCY..ProductProfileManager.MAX_CURRENCY ||
                    it.energyDelta !in -value.maximumEnergy..value.maximumEnergy ||
                    it.rewardTrackDelta !in -ProductProfileManager.MAX_REWARD_TRACK_POINTS..ProductProfileManager.MAX_REWARD_TRACK_POINTS
            } || value.nextLedgerId <= (value.ledger.maxOfOrNull(ProductLedgerEntry::id) ?: 0) ||
            value.nextLedgerId > ProductProfileManager.MAX_COUNTER.toLong()
        ) throw MalformedPersistenceException("Invalid economy ledger.")
        if (exactLongSum(value.ledger.map(ProductLedgerEntry::softDelta)) != value.softCurrency ||
            exactLongSum(value.ledger.map(ProductLedgerEntry::premiumDelta)) != value.premiumShaped ||
            exactLongSum(value.ledger.map { it.energyDelta.toLong() }) != value.energy.toLong() ||
            exactLongSum(value.ledger.map { it.rewardTrackDelta.toLong() }) != value.rewardTrackPoints.toLong()
        ) throw MalformedPersistenceException("Economy ledger does not reconcile.")
        if (value.arena.opponentFormationId.isBlank() || value.arena.playerPower < 0 ||
            value.arena.opponentPower < 0 || value.arena.runCount !in 0..ProductProfileManager.MAX_COUNTER
        ) throw MalformedPersistenceException("Invalid arena state.")
    }

    private fun encodeTexts(values: Collection<String>): String =
        values.joinToString(".") { PersistenceWire.encodeText(it) }

    private fun decodeTexts(raw: String, field: String): List<String> =
        boundedSplit(raw, '.', field).map { decodeStandaloneText(it, field) }

    private fun ids(fields: Map<String, String>, key: String): List<ContentId> =
        boundedSplit(fields.getValue(key), ',', key).map { parseId(it, key) }

    private fun progressMap(raw: String): MutableMap<ContentId, ProductStageProgress> {
        val result = linkedMapOf<ContentId, ProductStageProgress>()
        boundedSplit(raw, ';', "stage progress").forEach { entry ->
            val parts = boundedSplit(entry, ',', "stage progress", maximum = 3)
            requireSize(parts, 3, "stage progress")
            val id = parseId(parts[0], "stage progress")
            if (result.put(
                    id,
                    ProductStageProgress(
                        bestStars = parseNonNegativeInt(parts[1], "stage stars"),
                        clearCount = parseNonNegativeInt(parts[2], "stage clears"),
                    ),
                ) != null
            ) throw MalformedPersistenceException("Duplicate stage progress.")
        }
        return result
    }

    private fun idIntMap(fields: Map<String, String>, key: String): Map<ContentId, Int> {
        val result = linkedMapOf<ContentId, Int>()
        boundedSplit(fields.getValue(key), ',', key).forEach { entry ->
            val parts = boundedSplit(entry, ':', key, maximum = 2)
            requireSize(parts, 2, key)
            val id = parseId(parts[0], key)
            if (result.put(id, parseNonNegativeInt(parts[1], key)) != null) {
                throw MalformedPersistenceException("Duplicate map entry: $key")
            }
        }
        return result
    }

    private fun settings(raw: String): MutableMap<MySdSettingId, Boolean> {
        val result = linkedMapOf<MySdSettingId, Boolean>()
        boundedSplit(raw, ',', "settings", maximum = MySdSettingId.entries.size).forEach { entry ->
            val parts = boundedSplit(entry, ':', "settings", maximum = 2)
            requireSize(parts, 2, "settings")
            val id = enumValue<MySdSettingId>(parts[0], "setting")
            val value = when (parts[1]) {
                "0" -> false
                "1" -> true
                else -> throw MalformedPersistenceException("Malformed setting flag.")
            }
            if (result.put(id, value) != null) throw MalformedPersistenceException("Duplicate setting.")
        }
        return result
    }

    private fun ledger(raw: String): MutableList<ProductLedgerEntry> =
        boundedSplit(raw, ';', "ledger").map { entry ->
            val parts = boundedSplit(entry, ',', "ledger", maximum = 7)
            requireSize(parts, 7, "ledger")
            ProductLedgerEntry(
                id = parsePositiveLong(parts[0], "ledger id"),
                kind = enumValue(parts[1], "ledger kind"),
                sourceId = decodeStandaloneText(parts[2], "ledger source"),
                softDelta = parseLong(parts[3], "ledger soft delta"),
                premiumDelta = parseLong(parts[4], "ledger premium delta"),
                energyDelta = parseInt(parts[5], "ledger energy delta"),
                rewardTrackDelta = parseInt(parts[6], "ledger reward-track delta"),
            )
        }.toMutableList()

    private fun parseId(raw: String, field: String): ContentId = try {
        ContentId.of(raw)
    } catch (_: IllegalArgumentException) {
        throw MalformedPersistenceException("Malformed content id: $field")
    }

    private fun long(fields: Map<String, String>, key: String): Long = parseLong(fields.getValue(key), key)
    private fun nonNegativeLong(fields: Map<String, String>, key: String): Long =
        parseNonNegativeLong(fields.getValue(key), key)
    private fun positiveLong(fields: Map<String, String>, key: String): Long =
        parsePositiveLong(fields.getValue(key), key)
    private fun nonNegativeInt(fields: Map<String, String>, key: String): Int =
        parseNonNegativeInt(fields.getValue(key), key)
    private fun positiveInt(fields: Map<String, String>, key: String): Int =
        parsePositiveInt(fields.getValue(key), key)

    private fun parseLong(raw: String, field: String): Long = raw.toLongOrNull()
        ?: throw MalformedPersistenceException("Malformed long: $field")
    private fun parseNonNegativeLong(raw: String, field: String): Long =
        raw.toLongOrNull()?.takeIf { it >= 0 } ?: throw MalformedPersistenceException("Malformed non-negative long: $field")
    private fun parsePositiveLong(raw: String, field: String): Long =
        raw.toLongOrNull()?.takeIf { it > 0 } ?: throw MalformedPersistenceException("Malformed positive long: $field")
    private fun parseInt(raw: String, field: String): Int = raw.toIntOrNull()
        ?: throw MalformedPersistenceException("Malformed integer: $field")
    private fun parseNonNegativeInt(raw: String, field: String): Int =
        raw.toIntOrNull()?.takeIf { it >= 0 } ?: throw MalformedPersistenceException("Malformed non-negative integer: $field")
    private fun parsePositiveInt(raw: String, field: String): Int =
        raw.toIntOrNull()?.takeIf { it > 0 } ?: throw MalformedPersistenceException("Malformed positive integer: $field")

    private inline fun <reified T : Enum<T>> enumValue(fields: Map<String, String>, key: String): T =
        enumValue(fields.getValue(key), key)
    private inline fun <reified T : Enum<T>> enumValue(raw: String, field: String): T = try {
        enumValueOf<T>(raw)
    } catch (_: IllegalArgumentException) {
        throw MalformedPersistenceException("Malformed enum: $field")
    }

    private fun decodeStandaloneText(raw: String, field: String): String =
        PersistenceWire.decodeText(mapOf(field to raw), field)

    private fun requireSize(parts: List<String>, size: Int, field: String) {
        if (parts.size != size) throw MalformedPersistenceException("Malformed record: $field")
    }

    private fun exactLongSum(values: Iterable<Long>): Long = values.fold(0L) { total, value ->
        Math.addExact(total, value)
    }
}
