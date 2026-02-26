package dev.therealashik.client.jules.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import dev.therealashik.client.jules.api.JulesApi
import dev.therealashik.client.jules.db.JulesDatabase
import dev.therealashik.client.jules.model.*
import dev.therealashik.client.jules.viewmodel.CreateSessionConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class JulesRepository(
    private val db: JulesDatabase,
    private val api: JulesApi
) {
    private val queries = db.julesDatabaseQueries
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    // SOURCES
    val sources: Flow<List<JulesSource>> = queries.selectAllSources()
        .asFlow()
        .mapToList(Dispatchers.IO)
        .map { entities ->
            entities.map { json.decodeFromString(it.json_blob) }
        }

    suspend fun refreshSources() {
        withContext(Dispatchers.IO) {
            try {
                val remote = api.listAllSources()
                queries.transaction {
                    queries.deleteAllSources()
                    remote.forEach { source ->
                        queries.insertSource(source.name, json.encodeToString(source))
                    }
                }
            } catch (e: Exception) {
                // Log error or rethrow?
                // For now just print
                println("Failed to refresh sources: $e")
                throw e
            }
        }
    }

    // SESSIONS
    val sessions: Flow<List<JulesSession>> = queries.selectAllSessions()
        .asFlow()
        .mapToList(Dispatchers.IO)
        .map { entities ->
            entities.map { json.decodeFromString(it.json_blob) }
        }

    suspend fun refreshSessions() {
        withContext(Dispatchers.IO) {
            try {
                val remote = api.listAllSessions()
                queries.transaction {
                    queries.deleteAllSessions()
                    remote.forEach { session ->
                        queries.insertSession(session.name, json.encodeToString(session), session.updateTime)
                    }
                }
            } catch (e: Exception) {
                println("Failed to refresh sessions: $e")
                throw e
            }
        }
    }

    suspend fun getSession(sessionId: String): JulesSession? {
        return withContext(Dispatchers.IO) {
            val local = queries.getSession(sessionId).executeAsOneOrNull()
            if (local != null) {
                return@withContext json.decodeFromString<JulesSession>(local.json_blob)
            }
            try {
                val remote = api.getSession(sessionId)
                queries.insertSession(remote.name, json.encodeToString(remote), remote.updateTime)
                remote
            } catch (e: Exception) {
                null
            }
        }
    }

    // ACTIVITIES
    fun getActivities(sessionId: String): Flow<List<JulesActivity>> {
        return queries.selectActivitiesForSession(sessionId)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { entities ->
                entities.map { json.decodeFromString(it.json_blob) }
            }
    }

    suspend fun refreshActivities(sessionId: String) {
        withContext(Dispatchers.IO) {
            try {
                val allActivities = mutableListOf<JulesActivity>()
                var pageToken: String? = null
                do {
                    val response = api.listActivities(sessionId, pageSize = 50, pageToken = pageToken)
                    allActivities.addAll(response.activities)
                    pageToken = response.nextPageToken
                } while (pageToken != null)

                queries.transaction {
                    queries.deleteAllActivitiesForSession(sessionId)
                    allActivities.forEach { activity ->
                        queries.insertActivity(activity.name, sessionId, json.encodeToString(activity), activity.createTime)
                    }
                }

                // Also refresh the session details itself
                val session = api.getSession(sessionId)
                queries.insertSession(session.name, json.encodeToString(session), session.updateTime)
            } catch (e: Exception) {
                println("Failed to refresh activities: $e")
                throw e
            }
        }
    }

    // ACTIONS

    suspend fun createSession(
        prompt: String,
        config: CreateSessionConfig,
        source: JulesSource
    ): JulesSession {
        return withContext(Dispatchers.IO) {
            val session = api.createSession(
                prompt = prompt,
                sourceName = source.name,
                title = config.title,
                requirePlanApproval = config.requirePlanApproval,
                automationMode = config.automationMode,
                startingBranch = config.startingBranch
            )
            queries.insertSession(session.name, json.encodeToString(session), session.updateTime)
            session
        }
    }

    suspend fun sendMessage(sessionName: String, text: String) {
        withContext(Dispatchers.IO) {
            api.sendMessage(sessionName, text)
            refreshActivities(sessionName)
        }
    }

    suspend fun approvePlan(sessionName: String, planId: String?) {
        withContext(Dispatchers.IO) {
            api.approvePlan(sessionName, planId)
            refreshActivities(sessionName)
        }
    }

    suspend fun deleteSession(sessionName: String) {
        withContext(Dispatchers.IO) {
            api.deleteSession(sessionName)
            queries.deleteSession(sessionName)
        }
    }
}
