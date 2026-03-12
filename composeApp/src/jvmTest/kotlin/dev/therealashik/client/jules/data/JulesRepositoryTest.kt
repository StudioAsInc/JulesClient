package dev.therealashik.client.jules.data

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import dev.therealashik.client.jules.api.JulesApi
import dev.therealashik.client.jules.cache.CacheManager
import dev.therealashik.client.jules.db.JulesDatabase
import dev.therealashik.client.jules.model.CacheConfig
import dev.therealashik.jules.sdk.model.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class JulesRepositoryTest {
    private lateinit var driver: SqlDriver
    private lateinit var database: JulesDatabase
    private lateinit var repository: JulesRepository
    private lateinit var cacheManager: CacheManager
    private val mockApi = MockJulesApi()
    private val json = Json { ignoreUnknownKeys = true }

    @BeforeTest
    fun setup() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        JulesDatabase.Schema.create(driver)
        database = JulesDatabase(driver)
        cacheManager = CacheManager(database, CacheConfig(), MainScope())
        repository = JulesRepository(database, mockApi, cacheManager)
    }

    @AfterTest
    fun tearDown() {
        driver.close()
    }

    @Test
    fun testRefreshActivitiesRedundantSessionCall() = runTest {
        val sessionId = "sessions/1"
        val session = JulesSession(name = sessionId, prompt = "test", createTime = "2024-01-01T00:00:00Z")

        // Pre-populate cache with session
        cacheManager.set("session_$sessionId", json.encodeToString(session))

        // Reset mock counters
        mockApi.getSessionCalls = 0
        mockApi.listActivitiesCalls = 0

        // Call refreshActivities
        repository.refreshActivities(sessionId, forceNetwork = false)

        // Verify calls
        assertEquals(1, mockApi.listActivitiesCalls, "Should call listActivities once")
        // OPTIMIZED BEHAVIOR: it should NOT call getSession if cached and forceNetwork is false
        assertEquals(0, mockApi.getSessionCalls, "Optimized behavior avoids redundant getSession network call")
    }

    @Test
    fun testRefreshActivitiesForceNetwork() = runTest {
        val sessionId = "sessions/1"
        val session = JulesSession(name = sessionId, prompt = "test", createTime = "2024-01-01T00:00:00Z")

        // Pre-populate cache with session
        cacheManager.set("session_$sessionId", json.encodeToString(session))

        // Reset mock counters
        mockApi.getSessionCalls = 0
        mockApi.listActivitiesCalls = 0

        // Call refreshActivities with forceNetwork = true
        repository.refreshActivities(sessionId, forceNetwork = true)

        // Verify calls
        assertEquals(1, mockApi.listActivitiesCalls, "Should call listActivities once")
        assertEquals(1, mockApi.getSessionCalls, "Should call getSession when forceNetwork is true")
    }

    class MockJulesApi : JulesApi {
        var getSessionCalls = 0
        var listActivitiesCalls = 0

        override fun setApiKey(key: String) {}
        override fun getApiKey(): String = "test-key"
        override fun setBaseUrl(url: String) {}
        override fun getBaseUrl(): String = "https://jules.googleapis.com/v1alpha"
        override suspend fun listSources(pageSize: Int, pageToken: String?): ListSourcesResponse = ListSourcesResponse()
        override suspend fun listAllSources(): List<JulesSource> = emptyList()
        override suspend fun getSource(sourceName: String): JulesSource = JulesSource("test", "test", "test")
        override suspend fun listSessions(pageSize: Int, pageToken: String?): ListSessionsResponse = ListSessionsResponse()
        override suspend fun listAllSessions(): List<JulesSession> = emptyList()

        override suspend fun getSession(sessionName: String): JulesSession {
            getSessionCalls++
            return JulesSession(name = sessionName, prompt = "test", createTime = "2024-01-01T00:00:00Z")
        }

        override suspend fun createSession(prompt: String, sourceName: String, title: String?, requirePlanApproval: Boolean, automationMode: AutomationMode, startingBranch: String): JulesSession = JulesSession(name = "test", prompt = "test", createTime = "2024-01-01T00:00:00Z")
        override suspend fun updateSession(sessionName: String, updates: Map<String, Any?>, updateMask: List<String>): JulesSession = JulesSession(name = "test", prompt = "test", createTime = "2024-01-01T00:00:00Z")
        override suspend fun deleteSession(sessionName: String) {}

        override suspend fun listActivities(sessionName: String, pageSize: Int, pageToken: String?, createTime: String?): ListActivitiesResponse {
            listActivitiesCalls++
            return ListActivitiesResponse(activities = emptyList(), nextPageToken = null)
        }

        override suspend fun sendMessage(sessionName: String, prompt: String) {}
        override suspend fun approvePlan(sessionName: String, planId: String?) {}
    }
}
