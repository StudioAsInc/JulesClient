package dev.therealashik.client.jules.viewmodel

import dev.therealashik.client.jules.api.JulesApi
import dev.therealashik.jules.sdk.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertNotNull
import dev.therealashik.client.jules.model.Account
import dev.therealashik.client.jules.Settings
import dev.therealashik.client.jules.data.JulesRepository

// Avoid DB usage by making a dummy JulesRepository that just passes through methods we care about
// Actually, JulesRepository takes db, api, cache. The flow `sessions` comes from DB.
// Let's create a MockJulesRepository or since we pass it to ViewModel, let's just make sure DB is avoided entirely.
// Wait, SharedViewModel expects JulesRepository which is not an interface.

@OptIn(ExperimentalCoroutinesApi::class)
class SharedViewModelTest {

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())

        // Clear settings before tests
        Settings.saveString("accounts", "[]")
        Settings.saveString("active_account_id", "")
        Settings.saveString("api_key", "")
        Settings.saveString("theme", "MIDNIGHT")
        Settings.saveBoolean("default_card_state", false)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()

        // Clean up settings
        Settings.saveString("accounts", "[]")
        Settings.saveString("active_account_id", "")
        Settings.saveString("api_key", "")
    }

    class MockJulesApi : JulesApi {
        var currentApiKey: String = ""
        var currentBaseUrl: String = ""

        override fun setApiKey(key: String) { currentApiKey = key }
        override fun getApiKey(): String = currentApiKey
        override fun setBaseUrl(url: String) { currentBaseUrl = url }
        override fun getBaseUrl(): String = currentBaseUrl

        override suspend fun listSources(pageSize: Int, pageToken: String?) = ListSourcesResponse()
        override suspend fun listAllSources() = emptyList<JulesSource>()
        override suspend fun getSource(sourceName: String) = JulesSource("", "", "")

        override suspend fun listSessions(pageSize: Int, pageToken: String?) = ListSessionsResponse()
        override suspend fun listAllSessions() = emptyList<JulesSession>()
        override suspend fun getSession(sessionName: String) = JulesSession(name = "", prompt = "", createTime = "")

        override suspend fun createSession(
            prompt: String,
            sourceName: String,
            title: String?,
            requirePlanApproval: Boolean,
            automationMode: AutomationMode,
            startingBranch: String
        ) = JulesSession(name = "", prompt = "", createTime = "")

        override suspend fun updateSession(sessionName: String, updates: Map<String, Any?>, updateMask: List<String>) = JulesSession(name = "", prompt = "", createTime = "")
        override suspend fun deleteSession(sessionName: String) {}

        override suspend fun listActivities(sessionName: String, pageSize: Int, pageToken: String?) = ListActivitiesResponse()
        override suspend fun sendMessage(sessionName: String, prompt: String) {}
        override suspend fun approvePlan(sessionName: String, planId: String?) {}
    }

    @Test
    fun `addAccount sets as active when it is the first account`() = runBlocking {
        val mockApi = MockJulesApi()

        // We initialize ViewModel. It will hit DB. The error in calculateSessionsUsed is because of TimeUtils.
        // Wait, if it fails because of `calculateSessionsUsed` using `TimeUtils` we just need `TimeUtils` to not throw NoClassDefFoundError.
        val viewModel = SharedViewModel(api = mockApi)

        val newAccount = Account(
            id = "acc_1",
            name = "Test Account",
            apiKey = "test_key_1",
            apiUrl = "https://test.api.com"
        )

        viewModel.addAccount(newAccount)

        val state = viewModel.uiState.value
        assertEquals(listOf(newAccount), state.accounts)
        assertEquals("acc_1", state.activeAccountId)
        assertEquals("test_key_1", state.apiKey)

        assertEquals("test_key_1", mockApi.currentApiKey)
        assertEquals("https://test.api.com", mockApi.currentBaseUrl)
    }

    @Test
    fun `addAccount does not change active account when one already exists`() = runBlocking {
        val mockApi = MockJulesApi()
        val viewModel = SharedViewModel(api = mockApi)

        val firstAccount = Account(
            id = "acc_1",
            name = "First Account",
            apiKey = "test_key_1",
            apiUrl = "https://test.api.com"
        )
        val secondAccount = Account(
            id = "acc_2",
            name = "Second Account",
            apiKey = "test_key_2",
            apiUrl = "https://test2.api.com"
        )

        viewModel.addAccount(firstAccount)
        viewModel.addAccount(secondAccount)

        val state = viewModel.uiState.value
        assertEquals(listOf(firstAccount, secondAccount), state.accounts)
        assertEquals("acc_1", state.activeAccountId)
        assertEquals("test_key_1", state.apiKey)

        assertEquals("test_key_1", mockApi.currentApiKey)
        assertEquals("https://test.api.com", mockApi.currentBaseUrl)
    }

    @Test
    fun `addAccount updates existing account`() = runBlocking {
        val mockApi = MockJulesApi()
        val viewModel = SharedViewModel(api = mockApi)

        val originalAccount = Account(
            id = "acc_1",
            name = "Test Account",
            apiKey = "test_key_1",
            apiUrl = "https://test.api.com"
        )
        viewModel.addAccount(originalAccount)

        val updatedAccount = originalAccount.copy(
            name = "Updated Account",
            apiKey = "new_key_1"
        )
        viewModel.addAccount(updatedAccount)

        val state = viewModel.uiState.value
        assertEquals(1, state.accounts.size)
        assertEquals(updatedAccount, state.accounts.first())
        assertEquals("acc_1", state.activeAccountId)
        assertEquals("new_key_1", state.apiKey)

        assertEquals("new_key_1", mockApi.currentApiKey)
        assertEquals("https://test.api.com", mockApi.currentBaseUrl)
    }

    @Test
    fun `addAccount updates existing active account and applies to API`() = runBlocking {
        val mockApi = MockJulesApi()
        val viewModel = SharedViewModel(api = mockApi)

        val firstAccount = Account(id = "acc_1", name = "First", apiKey = "key1", apiUrl = "url1")
        val secondAccount = Account(id = "acc_2", name = "Second", apiKey = "key2", apiUrl = "url2")

        viewModel.addAccount(firstAccount)
        viewModel.addAccount(secondAccount)

        // now acc_1 is active. Update acc_2. It shouldn't change the API
        val updatedSecond = secondAccount.copy(apiKey = "new_key_2")
        viewModel.addAccount(updatedSecond)

        assertEquals("key1", mockApi.currentApiKey)

        // Update acc_1. It SHOULD change the API
        val updatedFirst = firstAccount.copy(apiKey = "new_key_1", apiUrl = "new_url_1")
        viewModel.addAccount(updatedFirst)

        assertEquals("new_key_1", mockApi.currentApiKey)
        assertEquals("new_url_1", mockApi.currentBaseUrl)
    }
}
