package dev.therealashik.jules.sdk

import dev.therealashik.jules.sdk.model.ListSourcesResponse
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertTrue

class LoggingInterceptorTest {

    class MockJulesLogger : JulesLogger {
        val logs = mutableListOf<String>()
        override fun log(message: String) {
            logs.add(message)
        }
    }

    @Test
    fun testLoggingInterceptor() = runTest {
        val mockLogger = MockJulesLogger()
        val mockEngine = MockEngine { request ->
            val response = ListSourcesResponse(sources = emptyList(), nextPageToken = null)
            respond(
                content = Json.encodeToString(response),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val client = JulesClient(
            apiKey = "test_key",
            engine = mockEngine,
            logger = mockLogger,
            debugMode = true
        )

        client.listSources()

        // Verify that Ktor logging intercepted the request/response
        // Ktor logging typically produces multiple log lines for a single request
        assertTrue(mockLogger.logs.isNotEmpty(), "Logs should not be empty")

        // Check for some common Ktor log patterns
        val hasRequestLog = mockLogger.logs.any { it.contains("REQUEST") || it.contains("http://localhost/v1alpha/sources") }
        val hasResponseLog = mockLogger.logs.any { it.contains("RESPONSE") || it.contains("200 OK") }

        assertTrue(hasRequestLog, "Logs should contain request information. Actual logs: ${mockLogger.logs}")
        assertTrue(hasResponseLog, "Logs should contain response information. Actual logs: ${mockLogger.logs}")
    }
}
