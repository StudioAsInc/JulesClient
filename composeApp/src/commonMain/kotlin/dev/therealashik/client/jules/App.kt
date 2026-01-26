package dev.therealashik.client.jules

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.therealashik.client.jules.ui.JulesBackground
import dev.therealashik.client.jules.ui.JulesTheme
import dev.therealashik.client.jules.ui.LoginScreen
import dev.therealashik.client.jules.ui.components.Drawer
import dev.therealashik.client.jules.ui.components.Header
import dev.therealashik.client.jules.ui.screens.HomeView
import dev.therealashik.client.jules.ui.screens.RepositoryView
import dev.therealashik.client.jules.ui.screens.SessionView
import dev.therealashik.client.jules.ui.screens.SettingsView
import androidx.compose.material3.MaterialTheme
import dev.therealashik.client.jules.viewmodel.Screen
import dev.therealashik.client.jules.viewmodel.SharedViewModel

@Composable
fun App() {
    val viewModel = viewModel { SharedViewModel() }
    val state by viewModel.uiState.collectAsState()

    JulesTheme(preset = state.currentTheme) {
        // Handle Back Navigation
        BackHandler(enabled = state.currentScreen !is Screen.Home) {
            viewModel.navigateBack()
        }

        JulesAppContent(viewModel)
    }
}

@Composable
fun JulesAppContent(viewModel: SharedViewModel) {
    val state by viewModel.uiState.collectAsState()
    var isDrawerOpen by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Header(
                onOpenDrawer = { isDrawerOpen = true },
                currentSource = state.currentSource,
                sources = state.sources,
                onSourceChange = { viewModel.selectSource(it) },
                isLoading = state.isProcessing || state.isLoading,
                onOpenSettings = { viewModel.navigateToSettings() }
            )

            Box(modifier = Modifier.weight(1f)) {
                when (val screen = state.currentScreen) {
                    is Screen.Home -> {
                        HomeView(
                            currentSource = state.currentSource,
                            onSendMessage = { text, config ->
                                viewModel.createSession(text, config)
                            },
                            isProcessing = state.isProcessing,
                            sessions = state.sessions,
                            onSelectSession = { viewModel.selectSession(it) },
                            onResetKey = { viewModel.setApiKey("") }, // Assuming setApiKey empty resets it or we need a proper reset
                            error = state.error
                        )
                    }
                    is Screen.Session -> {
                        val session = state.currentSession
                        // Fallback if session is null but screen is Session (shouldn't happen often)
                        if (session != null && session.name == screen.sessionId) {
                            SessionView(
                                session = session,
                                activities = state.activities,
                                isProcessing = state.isProcessing,
                                error = state.error,
                                defaultCardState = state.defaultCardState,
                                onSendMessage = { text ->
                                    viewModel.sendMessage(text)
                                },
                                onApprovePlan = { planId ->
                                    viewModel.approvePlan(session.name, planId)
                                },
                                onNavigateHome = {
                                    viewModel.navigateBack()
                                }
                            )
                        } else {
                            // Loading state or mismatch
                            // Ideally show loading
                        }
                    }
                    is Screen.Repository -> {
                        if (state.currentSource != null) {
                            RepositoryView(
                                source = state.currentSource!!,
                                sessions = state.sessions,
                                onStartNewSession = { viewModel.navigateBack() }, // Going back to Home triggers new session flow UI
                                onSelectSession = { viewModel.selectSession(it) }
                            )
                        } else {
                            // Should not happen, fallback to Home
                            HomeView(
                                currentSource = null,
                                onSendMessage = { text, config -> viewModel.createSession(text, config) },
                                isProcessing = state.isProcessing,
                                sessions = state.sessions,
                                onSelectSession = { viewModel.selectSession(it) },
                                error = state.error
                            )
                        }
                    }
                    is Screen.Settings -> {
                        SettingsView(
                            defaultCardState = state.defaultCardState,
                            currentTheme = state.currentTheme,
                            currentApiKey = state.apiKey ?: "",
                            onUpdateCardState = { viewModel.updateDefaultCardState(it) },
                            onThemeChange = { viewModel.setTheme(it) },
                            onApiKeyChange = { viewModel.setApiKey(it) },
                            onBack = { viewModel.navigateBack() }
                        )
                    }
                }
            }
        }
        
        Drawer(
            isOpen = isDrawerOpen,
            onClose = { isDrawerOpen = false },
            sessions = state.sessions,
            currentSessionId = state.currentSession?.name,
            onSelectSession = { 
                viewModel.selectSession(it)
                isDrawerOpen = false 
            },
            onDeleteSession = { viewModel.deleteSession(it) },
            sessionsUsed = state.sessionsUsed,
            dailyLimit = state.dailyLimit,
            sources = state.sources,
            currentSource = state.currentSource,
            onSelectSource = {
                viewModel.selectSource(it)
                isDrawerOpen = false
            },
            onNavigateToSettings = {
                viewModel.navigateToSettings()
                isDrawerOpen = false
            }
        )
    }
}
