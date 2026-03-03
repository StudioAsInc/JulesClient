package dev.therealashik.jules.sdk

import dev.therealashik.jules.sdk.model.CreateSessionRequest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CreateSessionRequestSerializationTest {

    private val json = Json {
        encodeDefaults = true
    }

    @Test
    fun testSourceContextOmittedWhenNull() {
        val request = CreateSessionRequest(
            prompt = "Test prompt",
            sourceContext = null,
            title = "Test title"
        )
        val jsonString = json.encodeToString(CreateSessionRequest.serializer(), request)

        // Ensure sourceContext is NOT in the JSON string
        assertFalse(jsonString.contains("sourceContext"), "JSON should not contain sourceContext when it is null")
        assertTrue(jsonString.contains("prompt"), "JSON should contain prompt")
        assertTrue(jsonString.contains("title"), "JSON should contain title")
    }

    @Test
    fun testSourceContextIncludedWhenNotNull() {
        val request = CreateSessionRequest(
            prompt = "Test prompt",
            sourceContext = dev.therealashik.jules.sdk.model.SourceContext(source = "test-source"),
            title = "Test title"
        )
        val jsonString = json.encodeToString(CreateSessionRequest.serializer(), request)

        // Ensure sourceContext IS in the JSON string
        assertTrue(jsonString.contains("sourceContext"), "JSON should contain sourceContext when it is not null")
        assertTrue(jsonString.contains("test-source"), "JSON should contain the source name")
    }
}
