package io.appstarterpack.util

import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTime::class)
class DateSerializerTest {

    @Serializable
    private data class Wrapper(
        @kotlinx.serialization.Serializable(with = DateSerializer::class)
        val timestamp: Instant
    )

    private val json = Json

    @Test
    fun `deserializes ISO-8601 string to Instant`() {
        val result = json.decodeFromString<Wrapper>("""{"timestamp":"2024-03-15T10:30:00Z"}""")
        assertEquals(Instant.parse("2024-03-15T10:30:00Z"), result.timestamp)
    }

    @Test
    fun `serializes Instant to ISO-8601 string`() {
        val wrapper = Wrapper(timestamp = Instant.parse("2024-03-15T10:30:00Z"))
        val result = json.encodeToString(wrapper)
        assertEquals("""{"timestamp":"2024-03-15T10:30:00Z"}""", result)
    }

    @Test
    fun `round-trips correctly`() {
        val original = """{"timestamp":"2024-03-15T10:30:00Z"}"""
        val decoded = json.decodeFromString<Wrapper>(original)
        val reEncoded = json.encodeToString(decoded)
        assertEquals(original, reEncoded)
    }

    @Test
    fun `handles millisecond precision`() {
        val input = """{"timestamp":"2024-03-15T10:30:00.123Z"}"""
        val decoded = json.decodeFromString<Wrapper>(input)
        assertEquals(Instant.parse("2024-03-15T10:30:00.123Z"), decoded.timestamp)
    }

    @Test
    fun `handles epoch`() {
        val input = """{"timestamp":"1970-01-01T00:00:00Z"}"""
        val decoded = json.decodeFromString<Wrapper>(input)
        assertEquals(Instant.fromEpochMilliseconds(0), decoded.timestamp)
    }
}
