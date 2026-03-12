package dev.therealashik.client.jules.api

import dev.therealashik.jules.sdk.JulesClient
import dev.therealashik.jules.sdk.model.*

class RealJulesApi(private val client: JulesClient = JulesClient()) : JulesApi {

    override fun setApiKey(key: String) {
        client.setApiKey(key)
    }

    override fun getApiKey(): String {
        return client.getApiKey()
    }

    override fun setBaseUrl(url: String) {
        client.setBaseUrl(url)
    }

    override fun getBaseUrl(): String {
        return client.getBaseUrl()
    }

    override suspend fun listSources(pageSize: Int, pageToken: String?): ListSourcesResponse {
        return client.listSources(pageSize, pageToken)
    }

    override suspend fun listAllSources(): List<JulesSource> {
        return client.listAllSources()
    }

    override suspend fun getSource(sourceName: String): JulesSource {
        return client.getSource(sourceName)
    }

    override suspend fun listSessions(pageSize: Int, pageToken: String?): ListSessionsResponse {
        return client.listSessions(pageSize, pageToken)
    }

    override suspend fun listAllSessions(): List<JulesSession> {
        return client.listAllSessions()
    }

    override suspend fun getSession(sessionName: String): JulesSession {
        return client.getSession(sessionName)
    }

    override suspend fun createSession(
        prompt: String,
        sourceName: String,
        title: String?,
        requirePlanApproval: Boolean,
        automationMode: AutomationMode,
        startingBranch: String
    ): JulesSession {
        return client.createSession(prompt, sourceName, title, requirePlanApproval, automationMode, startingBranch)
    }

    override suspend fun updateSession(
        sessionName: String,
        updates: Map<String, Any?>,
        updateMask: List<String>
    ): JulesSession {
        return client.updateSession(sessionName, updates, updateMask)
    }

    override suspend fun deleteSession(sessionName: String) {
        client.deleteSession(sessionName)
    }

    override suspend fun listActivities(
        sessionName: String,
        pageSize: Int,
        pageToken: String?,
        createTime: String?
    ): ListActivitiesResponse {
        return client.listActivities(sessionName, pageSize, pageToken, createTime)
    }

    override suspend fun sendMessage(sessionName: String, prompt: String) {
        client.sendMessage(sessionName, prompt)
    }

    override suspend fun approvePlan(sessionName: String, planId: String?) {
        client.approvePlan(sessionName, planId)
    }
}
