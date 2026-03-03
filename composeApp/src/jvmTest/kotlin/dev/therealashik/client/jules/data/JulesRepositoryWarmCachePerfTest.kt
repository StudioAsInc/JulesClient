package dev.therealashik.client.jules.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import dev.therealashik.client.jules.api.JulesApi
import dev.therealashik.client.jules.cache.CacheManager
import dev.therealashik.client.jules.db.JulesDatabase
import dev.therealashik.client.jules.model.CreateSessionConfig
import dev.therealashik.client.jules.model.CacheConfig
import dev.therealashik.jules.sdk.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.system.measureTimeMillis
import kotlinx.coroutines.delay

class MockJulesApi : JulesApi {
    override suspend fun listAllSources(): List<JulesSource> {
        delay(1000) // Simulate network delay
        return listOf()
    }

    override suspend fun listAllSessions(): List<JulesSession> {
        delay(1000) // Simulate network delay
        return listOf()
    }

    // Other methods...
    override suspend fun listActivities(sessionName: String, pageSize: Int, pageToken: String?): ListActivitiesResponse = throw NotImplementedError()
    override suspend fun createSession(prompt: String, sourceName: String, title: String?, requirePlanApproval: Boolean, automationMode: AutomationMode, startingBranch: String): JulesSession = throw NotImplementedError()
    override fun setApiKey(key: String): Unit = throw NotImplementedError()
    override fun getApiKey(): String = throw NotImplementedError()
    override fun setBaseUrl(url: String): Unit = throw NotImplementedError()
    override fun getBaseUrl(): String = throw NotImplementedError()
    override suspend fun listSources(pageSize: Int, pageToken: String?): ListSourcesResponse = throw NotImplementedError()
    override suspend fun getSource(sourceName: String): JulesSource = throw NotImplementedError()
    override suspend fun listSessions(pageSize: Int, pageToken: String?): ListSessionsResponse = throw NotImplementedError()
    override suspend fun updateSession(sessionName: String, updates: Map<String, Any?>, updateMask: List<String>): JulesSession = throw NotImplementedError()
    override suspend fun getSession(sessionId: String): JulesSession = throw NotImplementedError()
    override suspend fun sendMessage(sessionName: String, text: String): Unit = throw NotImplementedError()
    override suspend fun approvePlan(sessionName: String, planId: String?): Unit = throw NotImplementedError()
    override suspend fun deleteSession(sessionName: String): Unit = throw NotImplementedError()
}

class JulesRepositoryWarmCachePerfTest {
    @Test
    fun benchmarkWarmCache() = runBlocking {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        JulesDatabase.Schema.create(driver)
        val db = JulesDatabase(driver)
        val config = CacheConfig()
        val scope = CoroutineScope(Dispatchers.Unconfined)

        val api = MockJulesApi()

        // Use real CacheManager with in-memory DB
        val cache = CacheManager(db, config, scope)
        val repository = JulesRepository(db, api, cache)

        val time = measureTimeMillis {
            repository.warmCache()
        }

        println("Baseline warmCache took: $time ms")
    }
}
