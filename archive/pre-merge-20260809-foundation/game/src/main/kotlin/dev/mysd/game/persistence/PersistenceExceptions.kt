package dev.mysd.game.persistence

/** Base type for deterministic persistence input failures. */
sealed class PersistenceException(message: String) : IllegalArgumentException(message)

class MalformedPersistenceException(message: String) : PersistenceException(message)

class FutureSchemaVersionException(
    val boundary: String,
    val version: Int,
    val currentVersion: Int,
) : PersistenceException(
    "Unsupported future schema version $version for $boundary; current version is $currentVersion",
)

class UnsupportedSchemaVersionException(
    val boundary: String,
    val version: Int,
) : PersistenceException("Unsupported schema version $version for $boundary")
