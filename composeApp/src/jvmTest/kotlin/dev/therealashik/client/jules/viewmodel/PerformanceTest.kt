package dev.therealashik.client.jules.viewmodel

import dev.therealashik.client.jules.api.JulesApi
import dev.therealashik.client.jules.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.fail
import kotlin.math.max
import kotlin.math.min

@OptIn(ExperimentalCoroutinesApi::class)
class PerformanceTest {

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    class MockJulesApi(
        private val delayMillis: Long = 200
    ) : JulesApi {

        var listActivitiesStart = 0L
        var listActivitiesEnd = 0L
        var getSessionStart = 0L
        var getSessionEnd = 0L

        val completion = CompletableDeferred<Unit>()
        var callsCompleted = 0

        private fun checkCompletion() {
            if (callsCompleted >= 2) {
                completion.complete(Unit)
            }
        }

        override fun setApiKey(key: String) {}
        override fun getApiKey(): String = "test-key"

        override suspend fun listSources(pageSize: Int, pageToken: String?): ListSourcesResponse = ListSourcesResponse()
        override suspend fun listAllSources(): List<JulesSource> = emptyList()
        override suspend fun getSource(sourceName: String): JulesSource = JulesSource("test-source")

        override suspend fun listSessions(pageSize: Int, pageToken: String?): ListSessionsResponse = ListSessionsResponse()
        override suspend fun listAllSessions(): List<JulesSession> = emptyList()

        override suspend fun getSession(sessionName: String): JulesSession {
            getSessionStart = System.currentTimeMillis()
            delay(delayMillis)
            getSessionEnd = System.currentTimeMillis()
            callsCompleted++
            checkCompletion()
            return JulesSession(
                name = sessionName,
                prompt = "test",
                createTime = "2024-01-01T00:00:00Z"
            )
        }

        override suspend fun createSession(
            prompt: String,
            sourceName: String,
            title: String?,
            requirePlanApproval: Boolean,
            automationMode: AutomationMode,
            startingBranch: String
        ): JulesSession = JulesSession(name = "sessions/new", prompt = prompt, createTime = "2024-01-01T00:00:00Z")

        override suspend fun updateSession(
            sessionName: String,
            updates: Map<String, Any?>,
            updateMask: List<String>
        ): JulesSession = throw NotImplementedError()

        override suspend fun deleteSession(sessionName: String) {}

        override suspend fun listActivities(
            sessionName: String,
            pageSize: Int,
            pageToken: String?
        ): ListActivitiesResponse {
            listActivitiesStart = System.currentTimeMillis()
            delay(delayMillis)
            listActivitiesEnd = System.currentTimeMillis()
            callsCompleted++
            checkCompletion()
            return ListActivitiesResponse()
        }

        override suspend fun sendMessage(sessionName: String, prompt: String) {}
        override suspend fun approvePlan(sessionName: String, planId: String?) {}
    }

    @Test
    fun testRefreshSessionParallelism() = runBlocking {
        val mockApi = MockJulesApi(delayMillis = 200)
        val viewModel = SharedViewModel(api = mockApi)

        viewModel.setApiKey("test-key")

        val session = JulesSession(name = "sessions/1", prompt = "test", createTime = "2024-01-01T00:00:00Z")
        viewModel.selectSession(session)

        try {
            withTimeout(5000) {
                mockApi.completion.await()
            }
        } catch (e: TimeoutCancellationException) {
            fail("Timed out waiting for API calls")
        }

        val listDuration = mockApi.listActivitiesEnd - mockApi.listActivitiesStart
        val sessionDuration = mockApi.getSessionEnd - mockApi.getSessionStart

        println("ListActivities: ${mockApi.listActivitiesStart} -> ${mockApi.listActivitiesEnd} ($listDuration ms)")
        println("GetSession: ${mockApi.getSessionStart} -> ${mockApi.getSessionEnd} ($sessionDuration ms)")

        val start = max(mockApi.listActivitiesStart, mockApi.getSessionStart)
        val end = min(mockApi.listActivitiesEnd, mockApi.getSessionEnd)
        val overlap = max(0L, end - start)

        println("Overlap: $overlap ms")

        if (overlap < 50) {
             println("Execution appears SEQUENTIAL")
        } else {
             println("Execution appears PARALLEL")
        }

        // Cleanup to allow runTest to finish (stop infinite polling loop)
        viewModel.navigateBack()

        // Uncomment after verifying baseline
        assertTrue(overlap > 100, "Expected parallel execution (overlap > 100ms), but got $overlap ms")
    }
}
