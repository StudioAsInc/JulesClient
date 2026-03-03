package dev.therealashik.client.jules.api

import dev.therealashik.jules.sdk.model.*

interface JulesApi {
    fun setApiKey(key: String)
    fun getApiKey(): String

    fun setBaseUrl(url: String)
    fun getBaseUrl(): String

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
