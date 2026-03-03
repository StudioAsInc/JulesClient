package dev.therealashik.jules.sdk

import io.ktor.client.engine.mock.*
import io.ktor.http.*
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.fail

class JulesClientTest {

    @Test
    fun testAuthRequestErrorJsonParsingFallback() = runTest {
        val mockEngine = MockEngine { request ->
            respond(
                content = "{ invalid json }",
                status = HttpStatusCode.BadRequest,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val client = JulesClient(
            apiKey = "test-api-key",
            engine = mockEngine,
            maxRetries = 1 // No need to retry for 400 Bad Request
        )

        try {
            client.getSource("test-source")
            fail("Expected JulesException.ValidationError to be thrown")
        } catch (e: JulesException.ValidationError) {
            assertTrue(
                e.message!!.contains("400"),
                "Error message should contain status code '400' when JSON parsing fails, but was: ${e.message}"
            )
        }
    }
}
