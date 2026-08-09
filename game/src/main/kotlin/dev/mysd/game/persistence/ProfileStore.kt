package dev.mysd.game.persistence

data class ProfileStore(
    val profileId: String,
    val unlockedStages: Set<String>,
    val currencies: Map<String, Long>,
    val energy: Int,
    val roster: Set<String>,
    val loadout: List<String>,
    val tech: Set<String>,
    val claims: Set<String>,
    val localServiceHistory: List<String>,
)

object ProfileStoreCodec {
    const val CURRENT_SCHEMA_VERSION: Int = 2
    private const val BOUNDARY = "profile-store"

    fun encode(value: ProfileStore): String {
        validate(value)
        val fields = linkedMapOf(
            "profileId" to PersistenceWire.encodeText(value.profileId),
            "energy" to value.energy.toString(),
            "unlockedCount" to value.unlockedStages.size.toString(),
        )
        value.unlockedStages.sorted().forEachIndexed { index, stage ->
            fields["unlocked.$index"] = PersistenceWire.encodeText(stage)
        }
        fields["currencyCount"] = value.currencies.size.toString()
        value.currencies.toSortedMap().entries.forEachIndexed { index, (currency, amount) ->
            fields["currency.$index.name"] = PersistenceWire.encodeText(currency)
            fields["currency.$index.amount"] = amount.toString()
        }
        fields["rosterCount"] = value.roster.size.toString()
        value.roster.sorted().forEachIndexed { index, troop ->
            fields["roster.$index"] = PersistenceWire.encodeText(troop)
        }
        fields["loadoutCount"] = value.loadout.size.toString()
        value.loadout.forEachIndexed { index, troop ->
            fields["loadout.$index"] = PersistenceWire.encodeText(troop)
        }
        fields["techCount"] = value.tech.size.toString()
        value.tech.sorted().forEachIndexed { index, tech ->
            fields["tech.$index"] = PersistenceWire.encodeText(tech)
        }
        fields["claimCount"] = value.claims.size.toString()
        value.claims.sorted().forEachIndexed { index, claim ->
            fields["claim.$index"] = PersistenceWire.encodeText(claim)
        }
        fields["serviceHistoryCount"] = value.localServiceHistory.size.toString()
        value.localServiceHistory.forEachIndexed { index, event ->
            fields["serviceHistory.$index"] = PersistenceWire.encodeText(event)
        }
        return PersistenceWire.document(BOUNDARY, CURRENT_SCHEMA_VERSION, fields)
    }

    fun decode(input: String): ProfileStore {
        val document = PersistenceWire.parse(input, BOUNDARY, CURRENT_SCHEMA_VERSION)
        val fields = document.fields
        val unlockedCount = PersistenceWire.count(fields, "unlockedCount")
        val currencyCount = PersistenceWire.count(fields, "currencyCount")
        val rosterCount = PersistenceWire.count(fields, "rosterCount")
        val loadoutCount = PersistenceWire.count(fields, "loadoutCount")
        val claimCount = PersistenceWire.count(fields, "claimCount")
        val techCount = if (document.version == 1) 0 else PersistenceWire.count(fields, "techCount")
        val serviceHistoryCount = if (document.version == 1) 0 else PersistenceWire.count(fields, "serviceHistoryCount")
        val expected = buildSet {
            addAll(setOf("profileId", "energy", "unlockedCount", "currencyCount", "rosterCount", "loadoutCount", "claimCount"))
            repeat(unlockedCount) { add("unlocked.$it") }
            repeat(currencyCount) {
                add("currency.$it.name")
                add("currency.$it.amount")
            }
            repeat(rosterCount) { add("roster.$it") }
            repeat(loadoutCount) { add("loadout.$it") }
            if (document.version >= CURRENT_SCHEMA_VERSION) {
                add("techCount")
                add("serviceHistoryCount")
                repeat(techCount) { add("tech.$it") }
                repeat(serviceHistoryCount) { add("serviceHistory.$it") }
            }
            repeat(claimCount) { add("claim.$it") }
        }
        PersistenceWire.requireExactKeys(document, expected)

        val unlockedEntries = (0 until unlockedCount).map { index ->
            PersistenceWire.decodeText(fields, "unlocked.$index")
        }
        val unlocked = rejectDuplicates(unlockedEntries, "unlocked stages")
        val currencies = linkedMapOf<String, Long>()
        repeat(currencyCount) { index ->
            val name = PersistenceWire.decodeText(fields, "currency.$index.name")
            val amount = PersistenceWire.long(fields, "currency.$index.amount")
            if (currencies.put(name, amount) != null) {
                throw MalformedPersistenceException("Duplicate profile currency")
            }
        }
        val rosterEntries = (0 until rosterCount).map { index ->
            PersistenceWire.decodeText(fields, "roster.$index")
        }
        val roster = rejectDuplicates(rosterEntries, "roster")
        val loadout = (0 until loadoutCount).map { index ->
            PersistenceWire.decodeText(fields, "loadout.$index")
        }
        val techEntries = (0 until techCount).map { index ->
            PersistenceWire.decodeText(fields, "tech.$index")
        }
        val tech = rejectDuplicates(techEntries, "tech")
        val claimEntries = (0 until claimCount).map { index ->
            PersistenceWire.decodeText(fields, "claim.$index")
        }
        val claims = rejectDuplicates(claimEntries, "claims")
        val serviceHistory = (0 until serviceHistoryCount).map { index ->
            PersistenceWire.decodeText(fields, "serviceHistory.$index")
        }
        val result = ProfileStore(
            profileId = PersistenceWire.decodeText(fields, "profileId"),
            unlockedStages = unlocked,
            currencies = currencies,
            energy = PersistenceWire.int(fields, "energy"),
            roster = roster,
            loadout = loadout,
            tech = tech,
            claims = claims,
            localServiceHistory = serviceHistory,
        )
        validate(result)
        return result
    }

    private fun validate(value: ProfileStore) {
        PersistenceWire.requireNonBlank(value.profileId, "profileId")
        PersistenceWire.requireNonNegative(value.energy, "energy")
        value.unlockedStages.forEach { PersistenceWire.requireNonBlank(it, "unlocked stage") }
        value.currencies.forEach { (currency, amount) ->
            PersistenceWire.requireNonBlank(currency, "currency")
            PersistenceWire.requireNonNegative(amount, "currency amount")
        }
        value.roster.forEach { PersistenceWire.requireNonBlank(it, "roster item") }
        value.loadout.forEach { PersistenceWire.requireNonBlank(it, "loadout item") }
        if (value.loadout.size != value.loadout.distinct().size) {
            throw MalformedPersistenceException("Duplicate profile loadout entry")
        }
        value.tech.forEach { PersistenceWire.requireNonBlank(it, "tech item") }
        value.claims.forEach { PersistenceWire.requireNonBlank(it, "claim") }
        value.localServiceHistory.forEach { PersistenceWire.requireNonBlank(it, "service history") }
        if (!value.roster.containsAll(value.loadout)) {
            throw MalformedPersistenceException("Profile loadout contains an item outside the roster")
        }
    }

    private fun <T> rejectDuplicates(values: List<T>, field: String): Set<T> {
        if (values.size != values.distinct().size) {
            throw MalformedPersistenceException("Duplicate profile $field entry")
        }
        return values.toCollection(linkedSetOf())
    }
}
