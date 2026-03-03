package dev.therealashik.jules.sdk

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

class JulesClientTest {

    @Test
    fun testListAllSourcesPagination() = runTest {
        val mockEngine = MockEngine { request ->
            when (request.url.encodedPath) {
                "/v1alpha/sources" -> {
                    val pageToken = request.url.parameters["pageToken"]
                    if (pageToken == null) {
                        // First page
                        val response = ListSourcesResponse(
                            sources = listOf(
                                JulesSource(name = "sources/1", id = "1", displayName = "Source 1"),
                                JulesSource(name = "sources/2", id = "2", displayName = "Source 2")
                            ),
                            nextPageToken = "page2_token"
                        )
                        respond(
                            content = Json.encodeToString(response),
                            status = HttpStatusCode.OK,
                            headers = headersOf(HttpHeaders.ContentType, "application/json")
                        )
                    } else if (pageToken == "page2_token") {
                        // Second page
                        val response = ListSourcesResponse(
                            sources = listOf(
                                JulesSource(name = "sources/3", id = "3", displayName = "Source 3")
                            ),
                            nextPageToken = null
                        )
                        respond(
                            content = Json.encodeToString(response),
                            status = HttpStatusCode.OK,
                            headers = headersOf(HttpHeaders.ContentType, "application/json")
                        )
                    } else {
                        respond(
                            content = """{"error": "Invalid token"}""",
                            status = HttpStatusCode.BadRequest,
                            headers = headersOf(HttpHeaders.ContentType, "application/json")
                        )
                    }
                }
                else -> respond(
                    content = "Not found",
                    status = HttpStatusCode.NotFound
                )
            }
        }

        val client = JulesClient(apiKey = "test_key", engine = mockEngine)
        val allSources = client.listAllSources()

        assertEquals(3, allSources.size)
        assertEquals("sources/1", allSources[0].name)
        assertEquals("sources/2", allSources[1].name)
        assertEquals("sources/3", allSources[2].name)
    }
}
