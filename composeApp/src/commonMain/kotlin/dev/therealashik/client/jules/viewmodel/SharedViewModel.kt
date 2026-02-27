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
}

data class JulesUiState(
    val apiKey: String? = null,
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
        // Allow setting empty key to "logout" or reset, but always save it
        api.setApiKey(key)
        Settings.saveString("api_key", key)
        _uiState.update { it.copy(apiKey = key) }
        
        if (key.isNotBlank()) {
            loadInitialData()
        }
    }

    private fun loadInitialData() {
        // Load settings
        val savedCardState = Settings.getBoolean("default_card_state", false)
        val savedThemeStr = Settings.getString("theme", ThemePreset.MIDNIGHT.name)
        val savedTheme = try {
            ThemePreset.valueOf(savedThemeStr)
        } catch (e: Exception) {
            ThemePreset.MIDNIGHT
        }
        
        // Load API Key
        val savedKey = Settings.getString("api_key", "")
        if (savedKey.isNotBlank()) {
             api.setApiKey(savedKey)
             _uiState.update { it.copy(apiKey = savedKey) }
        }

        _uiState.update { it.copy(defaultCardState = savedCardState, currentTheme = savedTheme) }

        if (api.getApiKey().isBlank()) {
             return
        }

        refreshAll()
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

    fun navigateBack() {
        _uiState.update { state ->
            when (state.currentScreen) {
                is Screen.Session, is Screen.Repository, is Screen.Settings -> {
                    // Stop polling when leaving session
                    stopPolling()
                    activitiesJob?.cancel()
                    state.copy(currentScreen = Screen.Home, currentSession = null)
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
            println("Polling error: $e")
            return false
        }
    }
}

// Moved to model package
