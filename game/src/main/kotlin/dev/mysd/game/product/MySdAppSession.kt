package dev.mysd.game.product

/** Single Android-free product facade. Android renders snapshots and submits only typed intents. */
interface MySdAppSession {
    fun snapshot(): MySdAppSnapshot

    fun submit(intent: MySdAppIntent): MySdAppResult

    /** Advances one 50 ms wall-clock pulse at the selected 1x/2x pacing. */
    fun pulse(): MySdAppResult

    /** Advances an explicit number of authoritative 20 Hz ticks for tests and headless drivers. */
    fun step(ticks: Int): MySdAppResult

    fun saveBundle(): MySdSaveBundle
}

/** Stable construction/restore entry point used by Android and headless verification. */
object MySdAppFactory {
    const val DEFAULT_SEED: Long = 0x4D_79_53_44L

    fun create(seed: Long = DEFAULT_SEED): MySdAppSession =
        DefaultMySdAppSession.create(seed)

    fun restore(bundle: MySdSaveBundle): MySdRestoreResult =
        DefaultMySdAppSession.restore(bundle)
}
