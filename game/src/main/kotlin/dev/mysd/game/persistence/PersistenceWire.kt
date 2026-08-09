package dev.mysd.game.persistence

import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import java.util.Base64

internal data class PersistenceDocument(
    val boundary: String,
    val version: Int,
    val fields: Map<String, String>,
)

internal object PersistenceWire {
    private const val MAGIC = "mysd.persistence"
    private const val MAX_ITEMS = 10_000

    fun document(boundary: String, version: Int, fields: Map<String, String>): String {
        val lines = mutableListOf("$MAGIC|$boundary", "schemaVersion=$version")
        fields.forEach { (key, value) ->
            lines += "$key=$value"
        }
        return lines.joinToString("\n")
    }

    fun parse(input: String, expectedBoundary: String, currentVersion: Int): PersistenceDocument {
        if (input.isEmpty() || input.contains('\r')) {
            throw MalformedPersistenceException("Persistence input is empty or has unsupported line endings")
        }

        val lines = input.split('\n')
        if (lines.size < 2 || lines[0] != "$MAGIC|$expectedBoundary") {
            throw MalformedPersistenceException("Invalid $expectedBoundary persistence header")
        }

        val versionText = lines[1].removePrefix("schemaVersion=")
        if (versionText == lines[1] || versionText.isEmpty()) {
            throw MalformedPersistenceException("Missing persistence schema version")
        }
        val version = versionText.toIntOrNull()
            ?: throw MalformedPersistenceException("Malformed persistence schema version")
        if (version > currentVersion) {
            throw FutureSchemaVersionException(expectedBoundary, version, currentVersion)
        }
        if (version < 1) {
            throw UnsupportedSchemaVersionException(expectedBoundary, version)
        }

        val fields = linkedMapOf<String, String>()
        lines.drop(2).forEach { line ->
            val separator = line.indexOf('=')
            if (separator <= 0) {
                throw MalformedPersistenceException("Malformed persistence field")
            }
            val key = line.substring(0, separator)
            val value = line.substring(separator + 1)
            if (fields.put(key, value) != null) {
                throw MalformedPersistenceException("Duplicate persistence field: $key")
            }
        }
        return PersistenceDocument(expectedBoundary, version, fields)
    }

    fun requireExactKeys(document: PersistenceDocument, expected: Set<String>) {
        val actual = document.fields.keys
        if (actual != expected) {
            val unexpected = actual - expected
            val missing = expected - actual
            throw MalformedPersistenceException(
                "Invalid ${document.boundary} fields; missing=$missing unexpected=$unexpected",
            )
        }
    }

    fun required(fields: Map<String, String>, key: String): String =
        fields[key] ?: throw MalformedPersistenceException("Missing persistence field: $key")

    fun encodeText(value: String): String =
        Base64.getEncoder().encodeToString(value.toByteArray(StandardCharsets.UTF_8))

    fun decodeText(fields: Map<String, String>, key: String): String {
        val raw = required(fields, key)
        return try {
            val bytes = Base64.getDecoder().decode(raw)
            val canonical = Base64.getEncoder().encodeToString(bytes)
            if (canonical != raw) {
                throw MalformedPersistenceException("Non-canonical base64 in persistence field: $key")
            }
            StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes))
                .toString()
        } catch (error: PersistenceException) {
            throw error
        } catch (_: Exception) {
            throw MalformedPersistenceException("Malformed text in persistence field: $key")
        }
    }

    fun int(fields: Map<String, String>, key: String): Int =
        required(fields, key).toIntOrNull()
            ?: throw MalformedPersistenceException("Malformed integer in persistence field: $key")

    fun long(fields: Map<String, String>, key: String): Long =
        required(fields, key).toLongOrNull()
            ?: throw MalformedPersistenceException("Malformed long in persistence field: $key")

    fun count(fields: Map<String, String>, key: String): Int {
        val count = int(fields, key)
        if (count !in 0..MAX_ITEMS) {
            throw MalformedPersistenceException("Invalid item count in persistence field: $key")
        }
        return count
    }

    fun flag(fields: Map<String, String>, key: String): Boolean = when (required(fields, key)) {
        "0" -> false
        "1" -> true
        else -> throw MalformedPersistenceException("Malformed flag in persistence field: $key")
    }

    fun requireNonNegative(value: Long, field: String) {
        if (value < 0) throw MalformedPersistenceException("Negative value in persistence field: $field")
    }

    fun requireNonNegative(value: Int, field: String) {
        if (value < 0) throw MalformedPersistenceException("Negative value in persistence field: $field")
    }

    fun requireNonBlank(value: String, field: String) {
        if (value.isBlank()) throw MalformedPersistenceException("Blank value in persistence field: $field")
    }
}
