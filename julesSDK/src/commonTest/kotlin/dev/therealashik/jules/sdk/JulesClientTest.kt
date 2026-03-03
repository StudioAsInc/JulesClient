package dev.therealashik.jules.sdk

import dev.therealashik.jules.sdk.model.*
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlin.test.Test
import kotlin.test.assertEquals

class JulesClientTest {

    private fun createMockClient(handler: MockRequestHandler): JulesClient {
        val mockHttpClient = HttpClient(MockEngine) {
            engine {
                addHandler(handler)
            }
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    encodeDefaults = true
                })
            }
        }
        return JulesClient(
            apiKey = "test-key",
            baseUrl = "https://api.example.com",
            maxRetries = 1,
            httpClient = mockHttpClient,
            debugMode = true
        )
    }

    @Test
    fun testGetSession_WithSessionsPrefix() = runTest {
        var requestedUrl = ""
        val mockSession = JulesSession(
            name = "sessions/123",
            prompt = "Test prompt",
            createTime = "2023-01-01T00:00:00Z"
        )

        val client = createMockClient { request ->
            requestedUrl = request.url.toString()
            respond(
                content = Json.encodeToString(mockSession),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val session = client.getSession("sessions/123")
        assertEquals("https://api.example.com/sessions/123", requestedUrl)
        assertEquals("sessions/123", session.name)
    }

    @Test
    fun testGetSession_WithoutSessionsPrefix() = runTest {
        var requestedUrl = ""
        val mockSession = JulesSession(
            name = "sessions/456",
            prompt = "Another test prompt",
            createTime = "2023-01-01T00:00:00Z"
        )

        val client = createMockClient { request ->
            requestedUrl = request.url.toString()
            respond(
                content = Json.encodeToString(mockSession),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val session = client.getSession("456")
        assertEquals("https://api.example.com/sessions/456", requestedUrl)
        assertEquals("sessions/456", session.name)
    }

    @Test
    fun testGetSource_WithSourcesPrefix() = runTest {
        var requestedUrl = ""
        val mockSource = JulesSource(
            name = "sources/abc"
        )

        val client = createMockClient { request ->
            requestedUrl = request.url.toString()
            respond(
                content = Json.encodeToString(mockSource),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val source = client.getSource("sources/abc")
        assertEquals("https://api.example.com/sources/abc", requestedUrl)
        assertEquals("sources/abc", source.name)
    }

    @Test
    fun testGetSource_WithoutSourcesPrefix() = runTest {
        var requestedUrl = ""
        val mockSource = JulesSource(
            name = "sources/xyz"
        )

        val client = createMockClient { request ->
            requestedUrl = request.url.toString()
            respond(
                content = Json.encodeToString(mockSource),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val source = client.getSource("xyz")
        assertEquals("https://api.example.com/sources/xyz", requestedUrl)
        assertEquals("sources/xyz", source.name)
    }

    @Test
    fun testUpdateSession_WithSessionsPrefix() = runTest {
        var requestedUrl = ""
        val mockSession = JulesSession(
            name = "sessions/123",
            prompt = "Updated prompt",
            createTime = "2023-01-01T00:00:00Z"
        )

        val client = createMockClient { request ->
            requestedUrl = request.url.toString()
            respond(
                content = Json.encodeToString(mockSession),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val session = client.updateSession("sessions/123", mapOf("prompt" to "Updated prompt"), listOf("prompt"))
        assertEquals("https://api.example.com/sessions/123?updateMask=prompt", requestedUrl)
        assertEquals("sessions/123", session.name)
    }

    @Test
    fun testUpdateSession_WithoutSessionsPrefix() = runTest {
        var requestedUrl = ""
        val mockSession = JulesSession(
            name = "sessions/456",
            prompt = "Another updated prompt",
            createTime = "2023-01-01T00:00:00Z"
        )

        val client = createMockClient { request ->
            requestedUrl = request.url.toString()
            respond(
                content = Json.encodeToString(mockSession),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val session = client.updateSession("456", mapOf("prompt" to "Another updated prompt"), emptyList())
        assertEquals("https://api.example.com/sessions/456", requestedUrl)
        assertEquals("sessions/456", session.name)
    }
}
