package dev.therealashik.jules.sdk

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class JulesClientNetworkRetryTest {

    @Test
    fun testNetworkRetrySuccess() = runTest {
        var attempts = 0
        val mockEngine = MockEngine { request ->
            attempts++
            if (attempts == 1) {
                // First attempt fails with network error (timeout/disconnect)
                throw RuntimeException("Connection reset by peer")
            } else {
                // Second attempt succeeds
                respond(
                    content = """{"sources": []}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
            }
        }

        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    encodeDefaults = true
                })
            }
        }

        val client = JulesClient(
            apiKey = "test-key",
            baseUrl = "https://test.com",
            maxRetries = 3,
            httpClient = httpClient
        )

        val response = client.listSources()
        assertEquals(2, attempts)
        assertEquals(0, response.sources.size)
    }

    @Test
    fun testNetworkRetryFailure() = runTest {
        var attempts = 0
        val mockEngine = MockEngine { request ->
            attempts++
            // Always fails with network error
            throw RuntimeException("Connection reset by peer")
        }

        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    encodeDefaults = true
                })
            }
        }

        val client = JulesClient(
            apiKey = "test-key",
            baseUrl = "https://test.com",
            maxRetries = 3,
            httpClient = httpClient
        )

        val exception = assertFailsWith<JulesException.NetworkError> {
            client.listSources()
        }

        assertEquals(3, attempts)
        assertEquals("Network request failed after 3 attempts", exception.message)
    }
}
