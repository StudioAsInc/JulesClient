package dev.therealashik.jules.sdk

import dev.therealashik.jules.sdk.model.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.timeout
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.delay
import kotlinx.serialization.json.*
import kotlin.math.pow

class JulesClient(
    private var apiKey: String = "",
    private var baseUrl: String = "https://jules.googleapis.com/v1alpha",
    private val maxRetries: Int = 3,
    private val timeoutMs: Long = 30000,
    private val debugMode: Boolean = false,
    engine: HttpClientEngine? = null,
    private val logger: JulesLogger = DefaultJulesLogger()
) {
    companion object {
        const val SDK_VERSION = "1.0.0"
    }

    // TODO: Implement rate limiting to prevent API quota exhaustion
    // TODO: Add WebSocket support for real-time activity streaming
    private val client = engine?.let { HttpClient(it) { configureClient() } } ?: HttpClient { configureClient() }

    private fun HttpClientConfig<*>.configureClient() {
        install(HttpTimeout)
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
                encodeDefaults = true
            })
        }
        install(Logging) {
            logger = object : Logger {
                override fun log(message: String) {
                    this@JulesClient.logger.log(message)
                }
            }
            level = if (debugMode) LogLevel.ALL else LogLevel.INFO
            sanitizeHeader { it == "X-Goog-Api-Key" }
        }
    }

    fun setApiKey(key: String) {
        apiKey = key
    }

    fun getApiKey(): String = apiKey

    fun setBaseUrl(url: String) {
        baseUrl = url
    }

    fun getBaseUrl(): String = baseUrl

    private suspend inline fun <reified T> authRequest(
        urlString: String,
        method: HttpMethod = HttpMethod.Get,
        body: Any? = null,
        retries: Int = maxRetries
    ): T {
        if (apiKey.isEmpty()) throw JulesException.AuthError("API Key not set")

        var lastException: Exception? = null
        
        repeat(retries) { attempt ->
            try {
                if (debugMode && attempt > 0) {
                    logger.log("[JulesSDK] Retry attempt $attempt for $urlString")
                }

                val response = client.request(urlString) {
                    this.method = method
                    header("X-Goog-Api-Key", apiKey)
                    header("User-Agent", "JulesSDK/$SDK_VERSION")
                    contentType(ContentType.Application.Json)
                    timeout {
                        requestTimeoutMillis = timeoutMs
                    }
                    if (body != null) {
                        setBody(body)
                    }
                }

                if (response.status.value in 200..299) {
                    return response.body()
                }

                val errorBody = response.body<String>()
                val errorMessage = try {
                    val element = Json { ignoreUnknownKeys = true }.decodeFromString<JsonElement>(errorBody)
                    element.jsonObject["error"]?.jsonObject?.get("message")?.jsonPrimitive?.content
                } catch (e: Exception) {
                    null
                }

                when (response.status.value) {
                    401, 403 -> throw JulesException.AuthError("Authentication failed: ${errorMessage ?: response.status}")
                    400 -> throw JulesException.ValidationError("Invalid request: ${errorMessage ?: response.status}")
                    in 500..599 -> throw JulesException.ServerError(response.status.value, "Server error: ${errorMessage ?: response.status}")
                    else -> throw JulesException.NetworkError("Request failed: ${response.status} - ${errorMessage ?: ""}")
                }
            } catch (e: JulesException) {
                throw e
            } catch (e: Exception) {
                lastException = e
                if (attempt < retries - 1) {
                    val delayMs = (100 * 2.0.pow(attempt)).toLong()
                    if (debugMode) {
                        logger.log("[JulesSDK] Network error, retrying in ${delayMs}ms: ${e.message}")
                    }
                    delay(delayMs)
                } else {
                    throw JulesException.NetworkError("Network request failed after $retries attempts", e)
                }
            }
        }

        throw JulesException.NetworkError("Request failed", lastException)
    }

    suspend fun listSources(pageSize: Int = 50, pageToken: String? = null): ListSourcesResponse {
        val params = buildString {
            append("?pageSize=$pageSize")
            if (pageToken != null) append("&pageToken=$pageToken")
        }
        return authRequest("$baseUrl/sources$params")
    }

    suspend fun listAllSources(): List<JulesSource> {
        val allSources = mutableListOf<JulesSource>()
        var pageToken: String? = null
        do {
            val response = listSources(pageSize = 50, pageToken = pageToken)
            allSources.addAll(response.sources)
            pageToken = response.nextPageToken
        } while (pageToken != null)
        return allSources
    }

    suspend fun getSource(sourceName: String): JulesSource {
        val url = if (sourceName.startsWith("sources/")) "$baseUrl/$sourceName" else "$baseUrl/sources/$sourceName"
        return authRequest(url)
    }

    suspend fun listSessions(pageSize: Int = 20, pageToken: String? = null): ListSessionsResponse {
        val params = buildString {
            append("?pageSize=$pageSize")
            if (pageToken != null) append("&pageToken=$pageToken")
        }
        return authRequest("$baseUrl/sessions$params")
    }

    suspend fun listAllSessions(): List<JulesSession> {
        val allSessions = mutableListOf<JulesSession>()
        var pageToken: String? = null
        do {
            val response = listSessions(pageSize = 50, pageToken = pageToken)
            allSessions.addAll(response.sessions)
            pageToken = response.nextPageToken
        } while (pageToken != null)
        return allSessions
    }

    suspend fun getSession(sessionName: String): JulesSession {
        val url = if (sessionName.startsWith("sessions/")) "$baseUrl/$sessionName" else "$baseUrl/sessions/$sessionName"
        return authRequest(url)
    }

    suspend fun createSession(
        prompt: String,
        sourceName: String,
        title: String? = null,
        requirePlanApproval: Boolean = true,
        automationMode: AutomationMode = AutomationMode.AUTO_CREATE_PR,
        startingBranch: String = "main"
    ): JulesSession {
        val sourceContext = if (sourceName.isNotEmpty()) {
            SourceContext(
                source = sourceName,
                githubRepoContext = GitHubRepoContext(startingBranch = startingBranch)
            )
        } else null

        val request = CreateSessionRequest(
            prompt = prompt,
            sourceContext = sourceContext,
            title = title,
            requirePlanApproval = requirePlanApproval,
            automationMode = automationMode
        )
        return authRequest("$baseUrl/sessions", HttpMethod.Post, request)
    }

    suspend fun updateSession(sessionName: String, updates: Map<String, Any?>, updateMask: List<String>): JulesSession {
        val cleanName = if (sessionName.startsWith("sessions/")) sessionName else "sessions/$sessionName"
        val query = if (updateMask.isNotEmpty()) "?updateMask=${updateMask.joinToString(",")}" else ""
        val jsonBody = buildJsonObject {
            updates.forEach { (key, value) ->
                when (value) {
                    is String -> put(key, value)
                    is Boolean -> put(key, value)
                    is Number -> put(key, JsonPrimitive(value))
                    null -> put(key, JsonNull)
                }
            }
        }
        return authRequest("$baseUrl/$cleanName$query", HttpMethod.Patch, jsonBody)
    }

    suspend fun deleteSession(sessionName: String) {
        val url = if (sessionName.startsWith("sessions/")) "$baseUrl/$sessionName" else "$baseUrl/sessions/$sessionName"
        val response = client.delete(url) {
            header("X-Goog-Api-Key", apiKey)
            header("User-Agent", "JulesSDK/$SDK_VERSION")
        }
        if (response.status.value !in 200..299) {
            throw JulesException.ServerError(response.status.value, "Delete failed: ${response.status}")
        }
    }

    suspend fun listActivities(sessionName: String, pageSize: Int = 50, pageToken: String? = null): ListActivitiesResponse {
        val cleanName = if (sessionName.startsWith("sessions/")) sessionName else "sessions/$sessionName"
        val params = buildString {
            append("?pageSize=$pageSize")
            if (pageToken != null) append("&pageToken=$pageToken")
        }
        return authRequest("$baseUrl/$cleanName/activities$params")
    }

    suspend fun sendMessage(sessionName: String, prompt: String) {
        val cleanName = if (sessionName.startsWith("sessions/")) sessionName else "sessions/$sessionName"
        val response = client.post("$baseUrl/$cleanName:sendMessage") {
            header("X-Goog-Api-Key", apiKey)
            header("User-Agent", "JulesSDK/$SDK_VERSION")
            contentType(ContentType.Application.Json)
            setBody(SendMessageRequest(prompt))
        }
        if (response.status.value !in 200..299) {
            throw JulesException.ServerError(response.status.value, "Send message failed: ${response.status}")
        }
    }

    suspend fun approvePlan(sessionName: String, planId: String? = null) {
        val cleanName = if (sessionName.startsWith("sessions/")) sessionName else "sessions/$sessionName"
        val response = client.post("$baseUrl/$cleanName:approvePlan") {
            header("X-Goog-Api-Key", apiKey)
            header("User-Agent", "JulesSDK/$SDK_VERSION")
            contentType(ContentType.Application.Json)
            setBody(ApprovePlanRequest(planId))
        }
        if (response.status.value !in 200..299) {
            throw JulesException.ServerError(response.status.value, "Approve plan failed: ${response.status}")
        }
    }
}
