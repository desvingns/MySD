package dev.mysd.game.content

sealed class ContentValidationException(message: String) : IllegalArgumentException(message)

class InvalidContentIdException(
    val rawValue: String,
) : ContentValidationException("Invalid content id: '$rawValue'")

class MalformedContentFixtureException(
    message: String,
    val fieldPath: String? = null,
) : ContentValidationException(
    if (fieldPath == null) message else "$fieldPath: $message",
)

class FutureContentSchemaVersionException(
    val version: Int,
    val currentVersion: Int,
) : ContentValidationException(
    "Unsupported future content fixture schema version $version; current version is $currentVersion",
)

class UnsupportedContentSchemaVersionException(
    val version: Int,
) : ContentValidationException("Unsupported content fixture schema version: $version")

class FutureContentVersionException(
    val version: Int,
    val currentVersion: Int,
) : ContentValidationException(
    "Unsupported future content version $version; current version is $currentVersion",
)

class UnsupportedContentVersionException(
    val version: Int,
) : ContentValidationException("Unsupported content version: $version")
