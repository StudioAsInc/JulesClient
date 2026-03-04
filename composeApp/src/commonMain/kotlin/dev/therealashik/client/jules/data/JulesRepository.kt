package dev.therealashik.client.jules.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import dev.therealashik.client.jules.api.JulesApi
import dev.therealashik.client.jules.cache.CacheManager
import dev.therealashik.client.jules.db.JulesDatabase
import dev.therealashik.jules.sdk.model.*
import dev.therealashik.client.jules.model.CreateSessionConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class JulesRepository(
    private val db: JulesDatabase,
    private val api: JulesApi,
    private val cache: CacheManager
) {
    private val queries = db.julesDatabaseQueries
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var backgroundRefreshJob: Job? = null

    // SOURCES
    val sources: Flow<List<JulesSource>> = queries.selectAllSources()
        .asFlow()
        .mapToList(Dispatchers.IO)
        .map { entities ->
            entities.map { json.decodeFromString(it.json_blob) }
        }

    suspend fun refreshSources(forceNetwork: Boolean = false) {
        withContext(Dispatchers.IO) {
            val cacheKey = "sources_all"
            
            if (!forceNetwork) {
                cache.get(cacheKey)?.let { cached ->
                    val sources = json.decodeFromString<List<JulesSource>>(cached)
                    val encodedSources = sources.map { it.name to json.encodeToString(it) }
                    queries.transaction {
                        queries.deleteAllSources()
                        encodedSources.forEach { (name, jsonBlob) ->
                            queries.insertSource(name, jsonBlob)
                        }
                    }
                    return@withContext
                }
            }
            
            try {
                val remote = api.listAllSources()
                val encodedRemote = remote.map { it.name to json.encodeToString(it) }
                queries.transaction {
                    queries.deleteAllSources()
                    encodedRemote.forEach { (name, jsonBlob) ->
                        queries.insertSource(name, jsonBlob)
                    }
                }
                cache.set(cacheKey, json.encodeToString(remote))
            } catch (e: Exception) {
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

    suspend fun refreshSessions(forceNetwork: Boolean = false) {
        withContext(Dispatchers.IO) {
            val cacheKey = "sessions_all"
            
            if (!forceNetwork) {
                cache.get(cacheKey)?.let { cached ->
                    val sessions = json.decodeFromString<List<JulesSession>>(cached)
                    val encodedSessions = sessions.map { Triple(it.name, json.encodeToString(it), it.updateTime) }
                    queries.transaction {
                        queries.deleteAllSessions()
                        encodedSessions.forEach { (name, jsonBlob, updateTime) ->
                            queries.insertSession(name, jsonBlob, updateTime)
                        }
                    }
                    return@withContext
                }
            }
            
            try {
                val remote = api.listAllSessions()
                val encodedRemote = remote.map { Triple(it.name, json.encodeToString(it), it.updateTime) }
                queries.transaction {
                    queries.deleteAllSessions()
                    encodedRemote.forEach { (name, jsonBlob, updateTime) ->
                        queries.insertSession(name, jsonBlob, updateTime)
                    }
                }
                cache.set(cacheKey, json.encodeToString(remote))
            } catch (e: Exception) {
                println("Failed to refresh sessions: $e")
                throw e
            }
        }
    }

    suspend fun getSession(sessionId: String, forceNetwork: Boolean = false): JulesSession? {
        return withContext(Dispatchers.IO) {
            val cacheKey = "session_$sessionId"
            
            if (!forceNetwork) {
                cache.get(cacheKey)?.let { cached ->
                    return@withContext json.decodeFromString<JulesSession>(cached)
                }
            }
            
            val local = queries.getSession(sessionId).executeAsOneOrNull()
            if (local != null && !forceNetwork) {
                return@withContext json.decodeFromString<JulesSession>(local.json_blob)
            }
            
            try {
                val remote = api.getSession(sessionId)
                queries.insertSession(remote.name, json.encodeToString(remote), remote.updateTime)
                cache.set(cacheKey, json.encodeToString(remote))
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

    suspend fun refreshActivities(sessionId: String, forceNetwork: Boolean = false) {
        withContext(Dispatchers.IO) {
            val cacheKey = "activities_$sessionId"
            
            if (!forceNetwork) {
                cache.get(cacheKey)?.let { cached ->
                    val activities = json.decodeFromString<List<JulesActivity>>(cached)
                    val encodedActivities = activities.map { Triple(it.name, json.encodeToString(it), it.createTime) }
                    queries.transaction {
                        queries.deleteAllActivitiesForSession(sessionId)
                        encodedActivities.forEach { (name, jsonBlob, createTime) ->
                            queries.insertActivity(name, sessionId, jsonBlob, createTime)
                        }
                    }
                    return@withContext
                }
            }
            
            try {
                val allActivities = mutableListOf<JulesActivity>()
                var pageToken: String? = null
                do {
                    val response = api.listActivities(sessionId, pageSize = 50, pageToken = pageToken)
                    allActivities.addAll(response.activities)
                    pageToken = response.nextPageToken
                } while (pageToken != null)

                val encodedActivities = allActivities.map { Triple(it.name, json.encodeToString(it), it.createTime) }
                queries.transaction {
                    queries.deleteAllActivitiesForSession(sessionId)
                    encodedActivities.forEach { (name, jsonBlob, createTime) ->
                        queries.insertActivity(name, sessionId, jsonBlob, createTime)
                    }
                }
                val activitiesJsonArray = "[" + encodedActivities.joinToString(",") { it.second } + "]"
                cache.set(cacheKey, activitiesJsonArray)

                // Also refresh the session details itself
                // Optimized: Using getSession which has caching logic
                getSession(sessionId, forceNetwork = forceNetwork)
            } catch (e: Exception) {
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
            cache.delete("sessions_all") // Invalidate sessions list
            session
        }
    }

    suspend fun sendMessage(sessionName: String, text: String) {
        withContext(Dispatchers.IO) {
            api.sendMessage(sessionName, text)
            cache.delete("activities_$sessionName") // Invalidate activities cache
            refreshActivities(sessionName, forceNetwork = true)
        }
    }

    suspend fun approvePlan(sessionName: String, planId: String?) {
        withContext(Dispatchers.IO) {
            api.approvePlan(sessionName, planId)
            cache.delete("activities_$sessionName") // Invalidate activities cache
            refreshActivities(sessionName, forceNetwork = true)
        }
    }

    suspend fun deleteSession(sessionName: String) {
        withContext(Dispatchers.IO) {
            api.deleteSession(sessionName)
            queries.deleteSession(sessionName)
            cache.delete("sessions_all") // Invalidate sessions list
            cache.delete("session_$sessionName") // Invalidate session cache
            cache.delete("activities_$sessionName") // Invalidate activities cache
        }
    }
    
    // CACHE WARMING
    suspend fun warmCache() {
        withContext(Dispatchers.IO) {
            try {
                coroutineScope {
                    // Warm sources and sessions concurrently
                    val sourcesDeferred = async { refreshSources(forceNetwork = true) }
                    val sessionsDeferred = async { refreshSessions(forceNetwork = true) }

                    sourcesDeferred.await()
                    sessionsDeferred.await()

                    // Selective cache warming for frequently accessed sessions
                    // Get the 3 most recently updated sessions
                    val recentSessions = queries.selectAllSessions().executeAsList().take(3)

                    recentSessions.forEach { sessionEntity ->
                        launch {
                            try {
                                refreshActivities(sessionEntity.name, forceNetwork = true)
                            } catch (e: Exception) {
                                println("Failed to warm activities for ${sessionEntity.name}: $e")
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                println("Cache warming failed: $e")
            }
        }
    }

    fun startBackgroundRefresh(intervalMs: Long = 15 * 60 * 1000L) {
        if (backgroundRefreshJob?.isActive == true) return

        backgroundRefreshJob = repositoryScope.launch {
            while (true) {
                try {
                    warmCache()
                } catch (e: Exception) {
                    println("Background refresh iteration failed: $e")
                }
                delay(intervalMs)
            }
        }
    }

    fun stopBackgroundRefresh() {
        backgroundRefreshJob?.cancel()
        backgroundRefreshJob = null
    }
}
