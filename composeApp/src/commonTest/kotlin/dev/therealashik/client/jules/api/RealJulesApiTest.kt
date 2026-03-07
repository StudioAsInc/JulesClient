package dev.therealashik.client.jules.api

import dev.therealashik.jules.sdk.JulesClient
import dev.therealashik.jules.sdk.model.AutomationMode
import dev.therealashik.jules.sdk.model.JulesSession
import dev.therealashik.jules.sdk.model.JulesSource
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
import kotlin.test.assertEquals

class RealJulesApiTest {

    @Test
    fun testSetApiKeyDelegation() {
        val client = JulesClient()
        val api = RealJulesApi(client)
        val apiKey = "test-api-key"

        api.setApiKey(apiKey)

        assertEquals(apiKey, client.getApiKey())
    }

    @Test
    fun testGetApiKeyDelegation() {
        val apiKey = "test-api-key"
        val client = JulesClient(apiKey = apiKey)
        val api = RealJulesApi(client)

        assertEquals(apiKey, api.getApiKey())
    }

    @Test
    fun testSetBaseUrlDelegation() {
        val client = JulesClient()
        val api = RealJulesApi(client)
        val baseUrl = "https://example.com/api"

        api.setBaseUrl(baseUrl)

        assertEquals(baseUrl, client.getBaseUrl())
    }

    @Test
    fun testGetBaseUrlDelegation() {
        val baseUrl = "https://example.com/api"
        val client = JulesClient(baseUrl = baseUrl)
        val api = RealJulesApi(client)

        assertEquals(baseUrl, api.getBaseUrl())
    }

    @Test
    fun testListSourcesDelegation() = runTest {
        val expectedResponse = ListSourcesResponse(
            sources = listOf(JulesSource(name = "sources/1", id = "1", displayName = "Source 1")),
            nextPageToken = null
        )

        val mockEngine = MockEngine { request ->
            respond(
                content = Json.encodeToString(expectedResponse),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val client = JulesClient(apiKey = "test-key", engine = mockEngine)
        val api = RealJulesApi(client)

        val response = api.listSources(pageSize = 10, pageToken = null)

        assertEquals(expectedResponse.sources.size, response.sources.size)
        assertEquals(expectedResponse.sources[0].name, response.sources[0].name)
    }

    @Test
    fun testCreateSessionDelegation() = runTest {
        val expectedSession = JulesSession(
            name = "sessions/1",
            prompt = "test prompt",
            createTime = "2024-01-01T00:00:00Z"
        )

        val mockEngine = MockEngine { request ->
            respond(
                content = Json.encodeToString(expectedSession),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val client = JulesClient(apiKey = "test-key", engine = mockEngine)
        val api = RealJulesApi(client)

        val session = api.createSession(
            prompt = "test prompt",
            sourceName = "sources/1",
            title = "Test Session",
            requirePlanApproval = true,
            automationMode = AutomationMode.AUTO_CREATE_PR,
            startingBranch = "main"
        )

        assertEquals(expectedSession.name, session.name)
        assertEquals(expectedSession.prompt, session.prompt)
    }
}
