package dev.mysd.game.persistence

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PersistenceWireTest {
    @Test
    fun `integer decode keeps the exact nested field path`() {
        val error = assertFailsWith<MalformedPersistenceException> {
            PersistenceWire.int(
                fields = mapOf("state.resource" to "not-an-int"),
                key = "state.resource",
            )
        }

        assertEquals("Malformed integer in persistence field: state.resource", error.message)
    }

    @Test
    fun `content id decode rejects malformed identifier at its field path`() {
        val fields = mapOf("state.stageId" to PersistenceWire.encodeText("Not a content id"))

        val error = assertFailsWith<MalformedPersistenceException> {
            PersistenceWire.contentId(fields, "state.stageId")
        }

        assertEquals("Malformed content id in persistence field: state.stageId", error.message)
    }

    @Test
    fun `parse rejects duplicate fields before field validation`() {
        val payload = PersistenceWire.document(
            boundary = "run-save",
            version = 4,
            fields = mapOf("tick" to "12"),
        ) + "\ntick=13"

        val error = assertFailsWith<MalformedPersistenceException> {
            PersistenceWire.parse(payload, expectedBoundary = "run-save", currentVersion = 4)
        }

        assertEquals("Duplicate persistence field: tick", error.message)
    }

    @Test
    fun `parse rejects future version with boundary and versions`() {
        val payload = PersistenceWire.document("run-save", version = 5, fields = emptyMap())

        val error = assertFailsWith<FutureSchemaVersionException> {
            PersistenceWire.parse(payload, expectedBoundary = "run-save", currentVersion = 4)
        }

        assertEquals("run-save", error.boundary)
        assertEquals(5, error.version)
        assertEquals(4, error.currentVersion)
    }

    @Test
    fun `range validators report their field paths`() {
        val belowMinimum = assertFailsWith<MalformedPersistenceException> {
            PersistenceWire.requireAtLeast(0, minimum = 1, field = "state.towerCooldownTicks")
        }
        assertEquals(
            "Value below minimum in persistence field: state.towerCooldownTicks",
            belowMinimum.message,
        )

        val aboveMaximum = assertFailsWith<MalformedPersistenceException> {
            PersistenceWire.requireAtMost(2, maximum = 1, field = "state.waveSpawnCount")
        }
        assertEquals(
            "Value above maximum in persistence field: state.waveSpawnCount",
            aboveMaximum.message,
        )

        val outsideRange = assertFailsWith<MalformedPersistenceException> {
            PersistenceWire.requireInRange(4, range = 0..3, field = "state.resource")
        }
        assertEquals(
            "Value outside range in persistence field: state.resource",
            outsideRange.message,
        )
    }
}
