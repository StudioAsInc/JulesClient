package dev.therealashik.client.jules.api

import dev.therealashik.client.jules.model.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import dev.therealashik.client.jules.utils.toJsonObject

interface JulesApi {
    fun setApiKey(key: String)
    fun getApiKey(): String
    suspend fun listSources(pageSize: Int = 50, pageToken: String? = null): ListSourcesResponse
    suspend fun listAllSources(): List<JulesSource>
    suspend fun getSource(sourceName: String): JulesSource
    suspend fun listSessions(pageSize: Int = 20, pageToken: String? = null): ListSessionsResponse
    suspend fun listAllSessions(): List<JulesSession>
    suspend fun getSession(sessionName: String): JulesSession
    suspend fun createSession(
        prompt: String,
        sourceName: String,
        title: String? = null,
        requirePlanApproval: Boolean = true,
        automationMode: AutomationMode = AutomationMode.AUTO_CREATE_PR,
        startingBranch: String = "main"
    ): JulesSession
    suspend fun updateSession(sessionName: String, updates: Map<String, Any?>, updateMask: List<String>): JulesSession
    suspend fun deleteSession(sessionName: String)
    suspend fun listActivities(sessionName: String, pageSize: Int = 50, pageToken: String? = null): ListActivitiesResponse
    suspend fun sendMessage(sessionName: String, prompt: String)
    suspend fun approvePlan(sessionName: String, planId: String? = null)
}

object GeminiService {
    private const val BASE_URL = "https://jules.googleapis.com/v1alpha"
    
    private var apiKey: String? = null

    fun setApiKey(key: String) {
        apiKey = key
    }

    fun getApiKey(): String {
        return apiKey ?: ""
    }

    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
                encodeDefaults = true
            })
        }
    }

    // Helper for authenticated requests
    private suspend inline fun <reified T> authRequest(
        urlString: String,
        method: HttpMethod = HttpMethod.Get,
        body: Any? = null
    ): T {
        val key = apiKey ?: throw IllegalStateException("API Key not set")
        val response = client.request(urlString) {
            this.method = method
            header("X-Goog-Api-Key", key)
            contentType(ContentType.Application.Json)
            if (body != null) {
                setBody(body)
            }
        }

        if (response.status.value !in 200..299) {
             throw Exception("API Error: ${response.status} - ${response.body<String>()}")
        }

        return response.body()
    }

    // ==================== SOURCES ====================

    suspend fun listSources(pageSize: Int = 50, pageToken: String? = null): ListSourcesResponse {
        val params = mutableListOf<String>()
        params.add("pageSize=$pageSize")
        if (pageToken != null) params.add("pageToken=$pageToken")
        val query = if (params.isNotEmpty()) "?${params.joinToString("&")}" else ""

        return authRequest("$BASE_URL/sources$query")
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
        val url = if (sourceName.startsWith("sources/")) "$BASE_URL/$sourceName" else "$BASE_URL/sources/$sourceName"
        return authRequest(url)
    }

    // ==================== SESSIONS ====================

    suspend fun listSessions(pageSize: Int = 20, pageToken: String? = null): ListSessionsResponse {
        val params = mutableListOf<String>()
        params.add("pageSize=$pageSize")
        if (pageToken != null) params.add("pageToken=$pageToken")
        val query = if (params.isNotEmpty()) "?${params.joinToString("&")}" else ""

        return authRequest("$BASE_URL/sessions$query")
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
         val url = if (sessionName.startsWith("sessions/")) "$BASE_URL/$sessionName" else "$BASE_URL/sessions/$sessionName"
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
        val request = CreateSessionRequest(
            prompt = prompt,
            sourceContext = SourceContext(
                source = sourceName,
                githubRepoContext = GitHubRepoContext(startingBranch = startingBranch)
            ),
            title = title,
            requirePlanApproval = requirePlanApproval,
            automationMode = automationMode
        )
        return authRequest("$BASE_URL/sessions", HttpMethod.Post, request)
    }

    suspend fun updateSession(sessionName: String, updates: Map<String, Any?>, updateMask: List<String>): JulesSession {
        val cleanName = if (sessionName.startsWith("sessions/")) sessionName else "sessions/$sessionName"
        val query = if (updateMask.isNotEmpty()) "?updateMask=${updateMask.joinToString(",")}" else ""
        val url = "$BASE_URL/$cleanName$query"

        val jsonBody = updates.toJsonObject()

        return authRequest(url, HttpMethod.Patch, jsonBody)
    }

    suspend fun deleteSession(sessionName: String) {
        val url = if (sessionName.startsWith("sessions/")) "$BASE_URL/$sessionName" else "$BASE_URL/sessions/$sessionName"
        // Delete returns Empty, so we need to handle that.
        val key = apiKey ?: throw IllegalStateException("API Key not set")
        val response = client.delete(url) {
            header("X-Goog-Api-Key", key)
        }
        if (response.status.value !in 200..299) {
             throw Exception("Delete Error: ${response.status}")
        }
    }

    // ==================== ACTIVITIES ====================

    suspend fun listActivities(sessionName: String, pageSize: Int = 50, pageToken: String? = null): ListActivitiesResponse {
         val params = mutableListOf<String>()
        params.add("pageSize=$pageSize")
        if (pageToken != null) params.add("pageToken=$pageToken")
        val query = if (params.isNotEmpty()) "?${params.joinToString("&")}" else ""

        // Ensure sessionName doesn't have double slashes if it already contains sessions/
        val cleanName = if (sessionName.startsWith("sessions/")) sessionName else "sessions/$sessionName"

        return authRequest("$BASE_URL/$cleanName/activities$query")
    }

    suspend fun sendMessage(sessionName: String, prompt: String) {
        val cleanName = if (sessionName.startsWith("sessions/")) sessionName else "sessions/$sessionName"
        // Returns empty or object, we ignore result
        // The response body is strictly required to NOT be empty by Ktor if we expect T.
        // So we use a specific call.

        val key = apiKey ?: throw IllegalStateException("API Key not set")
        val response = client.post("$BASE_URL/$cleanName:sendMessage") {
            header("X-Goog-Api-Key", key)
            contentType(ContentType.Application.Json)
            setBody(SendMessageRequest(prompt))
        }
        if (response.status.value !in 200..299) {
             throw Exception("Send Message Error: ${response.status}")
        }
    }

    suspend fun approvePlan(sessionName: String, planId: String? = null) {
        val cleanName = if (sessionName.startsWith("sessions/")) sessionName else "sessions/$sessionName"

        val key = apiKey ?: throw IllegalStateException("API Key not set")
        val response = client.post("$BASE_URL/$cleanName:approvePlan") {
            header("X-Goog-Api-Key", key)
            contentType(ContentType.Application.Json)
            setBody(ApprovePlanRequest(planId))
        }
        if (response.status.value !in 200..299) {
             throw Exception("Approve Plan Error: ${response.status}")
        }
    }
}

object RealJulesApi : JulesApi {
    override fun setApiKey(key: String) = GeminiService.setApiKey(key)
    override fun getApiKey(): String = GeminiService.getApiKey()
    override suspend fun listSources(pageSize: Int, pageToken: String?): ListSourcesResponse = GeminiService.listSources(pageSize, pageToken)
    override suspend fun listAllSources(): List<JulesSource> = GeminiService.listAllSources()
    override suspend fun getSource(sourceName: String): JulesSource = GeminiService.getSource(sourceName)
    override suspend fun listSessions(pageSize: Int, pageToken: String?): ListSessionsResponse = GeminiService.listSessions(pageSize, pageToken)
    override suspend fun listAllSessions(): List<JulesSession> = GeminiService.listAllSessions()
    override suspend fun getSession(sessionName: String): JulesSession = GeminiService.getSession(sessionName)
    override suspend fun createSession(
        prompt: String,
        sourceName: String,
        title: String?,
        requirePlanApproval: Boolean,
        automationMode: AutomationMode,
        startingBranch: String
    ): JulesSession = GeminiService.createSession(prompt, sourceName, title, requirePlanApproval, automationMode, startingBranch)

    override suspend fun updateSession(sessionName: String, updates: Map<String, Any?>, updateMask: List<String>): JulesSession = GeminiService.updateSession(sessionName, updates, updateMask)
    override suspend fun deleteSession(sessionName: String) = GeminiService.deleteSession(sessionName)
    override suspend fun listActivities(sessionName: String, pageSize: Int, pageToken: String?): ListActivitiesResponse = GeminiService.listActivities(sessionName, pageSize, pageToken)
    override suspend fun sendMessage(sessionName: String, prompt: String) = GeminiService.sendMessage(sessionName, prompt)
    override suspend fun approvePlan(sessionName: String, planId: String?) = GeminiService.approvePlan(sessionName, planId)
}
