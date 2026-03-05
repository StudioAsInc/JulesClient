package dev.therealashik.client.jules.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.therealashik.client.jules.Settings
import dev.therealashik.client.jules.api.JulesApi
import dev.therealashik.client.jules.api.RealJulesApi
import dev.therealashik.client.jules.data.JulesData
import dev.therealashik.client.jules.data.JulesRepository
import dev.therealashik.jules.sdk.model.*
import dev.therealashik.client.jules.model.ThemePreset
import dev.therealashik.client.jules.model.CustomTheme
import dev.therealashik.client.jules.model.CreateSessionConfig
import dev.therealashik.client.jules.utils.TimeUtils
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.minus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ==================== UI STATE ====================

sealed class Screen {
    data object Home : Screen()
    data class Session(val sessionId: String) : Screen()
    data class Repository(val sourceId: String) : Screen()
    data object Settings : Screen()
    data class ThemeEditor(val themeId: String? = null) : Screen()
}

data class JulesUiState(
    val apiKey: String? = null,
    val accounts: List<dev.therealashik.client.jules.model.Account> = emptyList(),
    val activeAccountId: String? = null,
    val sources: List<JulesSource> = emptyList(),
    val currentSource: JulesSource? = null,
    val sessions: List<JulesSession> = emptyList(),
    val currentSession: JulesSession? = null,
    val activities: List<JulesActivity> = emptyList(),
    val sessionsUsed: Int = 0,
    val dailyLimit: Int = 100, // Hardcoded for now
    val isProcessing: Boolean = false,
    val currentScreen: Screen = Screen.Home,
    val error: String? = null,
    val isLoading: Boolean = false,
    val defaultCardState: Boolean = false, // false = Collapsed, true = Expanded
    val currentTheme: ThemePreset = ThemePreset.MIDNIGHT
)

// ==================== VIEW MODEL ====================

// TODO: Split this ViewModel into smaller, feature-specific ViewModels
// TODO: Add proper error handling with retry mechanisms
// TODO: Implement offline mode support
class SharedViewModel(
    private val api: JulesApi = RealJulesApi,
    private val repository: JulesRepository = JulesData.repository
) : ViewModel() {

    private val _uiState = MutableStateFlow(JulesUiState())
    val uiState: StateFlow<JulesUiState> = _uiState.asStateFlow()

    private var pollJob: Job? = null
    private var activitiesJob: Job? = null

    init {
        observeDatabase()
    }

    private fun observeDatabase() {
        // Observe sessions and sources from DB
        repository.sessions.onEach { sessions ->
            _uiState.update {
                it.copy(
                    sessions = sessions,
                    sessionsUsed = calculateSessionsUsed(sessions)
                )
            }
        }.launchIn(viewModelScope)

        repository.sources.onEach { sources ->
            _uiState.update {
                val current = it.currentSource ?: sources.firstOrNull()
                it.copy(
                    sources = sources,
                    currentSource = current
                )
            }
        }.launchIn(viewModelScope)
    }

    // --- Initialization ---

    fun setApiKey(key: String) {
        // Backward compatibility: If an API key is set directly,
        // create a default account or update the active one.
        val defaultUrl = "https://jules.googleapis.com/v1alpha"

        api.setApiKey(key)
        api.setBaseUrl(defaultUrl)

        // Save as single default account for now to support existing flows
        val activeId = "default"
        val accounts = if (key.isNotBlank()) {
            listOf(dev.therealashik.client.jules.model.Account(id = activeId, name = "Default", apiKey = key, apiUrl = defaultUrl))
        } else {
            emptyList()
        }

        // Save serialized accounts JSON string
        Settings.saveString("api_key", key)
        if (accounts.isNotEmpty()) {
            saveAccounts(accounts, activeId)
        } else {
            saveAccounts(emptyList(), null)
        }

        _uiState.update { it.copy(apiKey = key, accounts = accounts, activeAccountId = if (key.isNotBlank()) activeId else null) }
        
        if (key.isNotBlank()) {
            loadInitialData()
        }
    }

    private fun saveAccounts(accounts: List<dev.therealashik.client.jules.model.Account>, activeId: String?) {
        val accountsJson = kotlinx.serialization.json.Json.encodeToString(
            kotlinx.serialization.builtins.ListSerializer(dev.therealashik.client.jules.model.Account.serializer()),
            accounts
        )
        Settings.saveString("accounts", accountsJson)
        Settings.saveString("active_account_id", activeId ?: "")
    }

    fun addAccount(account: dev.therealashik.client.jules.model.Account) {
        _uiState.update { state ->
            val updatedAccounts = state.accounts.filter { it.id != account.id } + account
            // If it's the first account, make it active
            val newActiveId = if (state.activeAccountId.isNullOrBlank()) account.id else state.activeAccountId

            saveAccounts(updatedAccounts, newActiveId)

            // Re-apply if active
            if (newActiveId == account.id) {
                api.setApiKey(account.apiKey)
                api.setBaseUrl(account.apiUrl)
                Settings.saveString("api_key", account.apiKey) // For backward compatibility
            }

            state.copy(accounts = updatedAccounts, activeAccountId = newActiveId, apiKey = if (newActiveId == account.id) account.apiKey else state.apiKey)
        }
        if (api.getApiKey().isNotBlank()) {
            refreshAll()
        }
    }

    fun switchAccount(accountId: String) {
        val account = _uiState.value.accounts.find { it.id == accountId } ?: return

        api.setApiKey(account.apiKey)
        api.setBaseUrl(account.apiUrl)
        Settings.saveString("api_key", account.apiKey)
        Settings.saveString("active_account_id", accountId)

        _uiState.update {
            it.copy(
                activeAccountId = accountId,
                apiKey = account.apiKey,
                // Clear state
                sources = emptyList(),
                currentSource = null,
                sessions = emptyList(),
                currentSession = null,
                activities = emptyList(),
                currentScreen = Screen.Home
            )
        }

        // Optionally clear DB/Cache here or scope repository queries by account

        refreshAll()
    }

    fun removeAccount(accountId: String) {
        _uiState.update { state ->
            val updatedAccounts = state.accounts.filter { it.id != accountId }
            val newActiveId = if (state.activeAccountId == accountId) {
                updatedAccounts.firstOrNull()?.id
            } else {
                state.activeAccountId
            }

            saveAccounts(updatedAccounts, newActiveId)

            val newActiveAccount = updatedAccounts.find { it.id == newActiveId }
            if (newActiveAccount != null) {
                api.setApiKey(newActiveAccount.apiKey)
                api.setBaseUrl(newActiveAccount.apiUrl)
                Settings.saveString("api_key", newActiveAccount.apiKey)
            } else {
                api.setApiKey("")
                Settings.saveString("api_key", "")
            }

            state.copy(
                accounts = updatedAccounts,
                activeAccountId = newActiveId,
                apiKey = newActiveAccount?.apiKey ?: ""
            )
        }
        if (api.getApiKey().isNotBlank()) {
            refreshAll()
        } else {
            _uiState.update {
                it.copy(
                    sources = emptyList(),
                    currentSource = null,
                    sessions = emptyList(),
                    currentSession = null,
                    activities = emptyList(),
                    currentScreen = Screen.Home
                )
            }
        }
    }

    private fun loadInitialData() {
        loadSettings()
        loadAccounts()

        if (api.getApiKey().isBlank()) {
             return
        }

        refreshAll()
    }

    private fun loadSettings() {
        val savedCardState = Settings.getBoolean("default_card_state", false)
        val savedThemeStr = Settings.getString("theme", ThemePreset.MIDNIGHT.name)
        val savedTheme = try {
            ThemePreset.valueOf(savedThemeStr)
        } catch (e: Exception) {
            ThemePreset.MIDNIGHT
        }
        _uiState.update { it.copy(defaultCardState = savedCardState, currentTheme = savedTheme) }
    }

    private fun loadAccounts() {
        val accountsJson = Settings.getString("accounts", "[]")
        val savedAccounts = try {
            kotlinx.serialization.json.Json.decodeFromString(
                kotlinx.serialization.builtins.ListSerializer(dev.therealashik.client.jules.model.Account.serializer()),
                accountsJson
            )
        } catch (e: Exception) {
            emptyList()
        }

        val activeAccountId = Settings.getString("active_account_id", "")

        if (savedAccounts.isNotEmpty() && activeAccountId.isNotBlank()) {
            val activeAccount = savedAccounts.find { it.id == activeAccountId } ?: savedAccounts.first()
            api.setApiKey(activeAccount.apiKey)
            api.setBaseUrl(activeAccount.apiUrl)
            _uiState.update { it.copy(apiKey = activeAccount.apiKey, accounts = savedAccounts, activeAccountId = activeAccount.id) }
        } else {
            // Load API Key (backward compat)
            val savedKey = Settings.getString("api_key", "")
            if (savedKey.isNotBlank()) {
                 val defaultUrl = "https://jules.googleapis.com/v1alpha"
                 api.setApiKey(savedKey)
                 api.setBaseUrl(defaultUrl)
                 val accounts = listOf(dev.therealashik.client.jules.model.Account(id = "default", name = "Default", apiKey = savedKey, apiUrl = defaultUrl))
                 _uiState.update { it.copy(apiKey = savedKey, accounts = accounts, activeAccountId = "default") }
            }
        }
    }

    private fun refreshAll() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                // If no key, don't try to fetch data yet
                if (api.getApiKey().isBlank()) {
                     _uiState.update { it.copy(isLoading = false) }
                     return@launch
                }

                // Execute network calls on IO dispatcher
                val (sourcesResp, allSessions) = withContext(Dispatchers.IO) {
                    val srcDeferred = async { api.listSources() }
                    val sessDeferred = async { api.listAllSessions() }
                    srcDeferred.await() to sessDeferred.await()
                }

                // Auto-select first source if none selected
                val firstSource = sourcesResp.sources.firstOrNull()

                _uiState.update {
                    it.copy(
                        sources = sourcesResp.sources,
                        currentSource = it.currentSource ?: firstSource,
                        sessions = allSessions,
                        sessionsUsed = calculateSessionsUsed(allSessions),
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    // --- Navigation & Selection ---

    fun navigateToSettings() {
        _uiState.update { it.copy(currentScreen = Screen.Settings) }
    }

    fun navigateToThemeEditor(themeId: String? = null) {
        _uiState.update { it.copy(currentScreen = Screen.ThemeEditor(themeId)) }
    }

    fun updateDefaultCardState(expanded: Boolean) {
        Settings.saveBoolean("default_card_state", expanded)
        _uiState.update { it.copy(defaultCardState = expanded) }
    }

    fun setTheme(theme: ThemePreset) {
        Settings.saveString("theme", theme.name)
        _uiState.update { it.copy(currentTheme = theme) }
    }

    fun selectSource(source: JulesSource) {
        _uiState.update {
            it.copy(
                currentSource = source,
                currentScreen = Screen.Repository(source.name)
            )
        }
    }

    fun selectSession(session: JulesSession) {
        _uiState.update {
            it.copy(
                currentSession = session,
                currentScreen = Screen.Session(session.name),
                activities = emptyList() // Clear old activities immediately
            )
        }

        // Observe activities for this session
        activitiesJob?.cancel()
        activitiesJob = repository.getActivities(session.name).onEach { activities ->
            _uiState.update { it.copy(activities = activities) }
        }.launchIn(viewModelScope)

        startPolling(session.name)
    }

    fun handleDeepLink(url: String) {
        val prefix = "jules://"
        if (url.startsWith(prefix)) {
            val path = url.removePrefix(prefix)
            val parts = path.split("/")
            if (parts.isNotEmpty()) {
                when (parts[0]) {
                    "session" -> {
                        val sessionId = parts.getOrNull(1)
                        if (sessionId != null) {
                            val session = _uiState.value.sessions.find { it.name == sessionId }
                            if (session != null) {
                                selectSession(session)
                            } else {
                                _uiState.update { it.copy(currentScreen = Screen.Session(sessionId)) }
                                startPolling(sessionId)
                            }
                        }
                    }
                    "repository" -> {
                        val sourceId = parts.getOrNull(1)
                        if (sourceId != null) {
                            val source = _uiState.value.sources.find { it.name == sourceId }
                            if (source != null) {
                                selectSource(source)
                            } else {
                                _uiState.update { it.copy(currentScreen = Screen.Repository(sourceId)) }
                            }
                        }
                    }
                }
            }
        }
    }

    fun navigateBack() {
        _uiState.update { state ->
            when (state.currentScreen) {
                is Screen.Session, is Screen.Repository, is Screen.Settings -> {
                    // Stop polling when leaving session
                    stopPolling()
                    activitiesJob?.cancel()
                    state.copy(currentScreen = Screen.Home, currentSession = null)
                }
                is Screen.ThemeEditor -> {
                    state.copy(currentScreen = Screen.Settings)
                }
                else -> state // Already at home or can't go back
            }
        }
    }

    // --- Actions ---

    fun createSession(prompt: String, config: CreateSessionConfig) {
        val source = _uiState.value.currentSource ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, error = null) }
            try {
                val session = repository.createSession(prompt, config, source)

                // Select the new session
                // Note: sessions list will update via flow automatically
                selectSession(session)

                _uiState.update {
                    it.copy(isProcessing = false)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to create: ${e.message}", isProcessing = false) }
            }
        }
    }

    fun sendMessage(text: String) {
        val session = _uiState.value.currentSession ?: return
        viewModelScope.launch {
             _uiState.update { it.copy(isProcessing = true, error = null) }
             try {
                 repository.sendMessage(session.name, text)
                 // Processing status will be updated via polling/refresh
             } catch (e: Exception) {
                 _uiState.update { it.copy(error = "Failed to send: ${e.message}", isProcessing = false) }
             }
        }
    }

    fun approvePlan(sessionId: String, planId: String? = null) {
         viewModelScope.launch {
             _uiState.update { it.copy(isProcessing = true) }
             try {
                 repository.approvePlan(sessionId, planId)
             } catch (e: Exception) {
                  _uiState.update { it.copy(error = "Failed to approve: ${e.message}", isProcessing = false) }
             }
         }
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            try {
                repository.deleteSession(sessionId)

                // If we are in the deleted session, go home
                if (_uiState.value.currentSession?.name == sessionId) {
                    navigateBack()
                }
            } catch (e: Exception) {
                 _uiState.update { it.copy(error = "Failed to delete: ${e.message}") }
            }
        }
    }

    fun updateSession(sessionId: String) {
        // Placeholder as per plan
        _uiState.update { it.copy(error = "Update Session not supported yet") }
    }

    // --- Internals ---

    private fun calculateSessionsUsed(sessions: List<JulesSession>): Int {
        try {
            val now = TimeUtils.nowInstant()
            val twentyFourHoursAgo = now.minus(24, DateTimeUnit.HOUR)
            return sessions.count {
                try {
                    val instant = Instant.parse(it.createTime)
                    instant > twentyFourHoursAgo
                } catch (e: Exception) {
                    false
                }
            }
        } catch (e: Exception) {
            return sessions.size
        }
    }

    private fun stopPolling() {
        pollJob?.cancel()
        pollJob = null
    }

    private fun startPolling(sessionName: String) {
        stopPolling()
        pollJob = viewModelScope.launch {
            var currentDelay = 2000L
            while (true) {
                val success = refreshSession(sessionName)
                if (success) {
                    currentDelay = 2000L
                } else {
                    currentDelay = (currentDelay * 2).coerceAtMost(30000L)
                }
                delay(currentDelay)
            }
        }
    }

    private suspend fun refreshSession(sessionName: String): Boolean {
        try {
            // Check if we are still looking at this session
            if (_uiState.value.currentSession?.name != sessionName) return true

            // Execute concurrent requests to reduce latency
            val (activitiesResp, session) = withContext(Dispatchers.IO) {
                val actDeferred = async { api.listActivities(sessionName) }
                val sessDeferred = async { api.getSession(sessionName) }
                actDeferred.await() to sessDeferred.await()
            }

            val isProcessing = session.state == SessionState.QUEUED ||
                               session.state == SessionState.PLANNING ||
                               session.state == SessionState.IN_PROGRESS

            if (session.state == SessionState.COMPLETED || session.state == SessionState.FAILED) {
                stopPolling()
            }

            _uiState.update { state ->
                state.copy(
                    currentSession = session,
                    isProcessing = isProcessing
                )
            }
            return true
        } catch (e: Exception) {
            return false
        }
    }
}

// Moved to model package
