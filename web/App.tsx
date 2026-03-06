import React, { useState, useEffect, useCallback, useMemo } from 'react';
import { Routes, Route, useNavigate, useLocation } from 'react-router-dom';
import { Header } from './components/Header';
import { HomeView } from './components/HomeView';
import { SessionView } from './components/SessionView';
import { RepositoryView } from './components/RepositoryView';
import { SettingsView } from './components/SettingsView';
import { Drawer } from './components/Drawer';
import { LoginScreen } from './components/LoginScreen';
import { GeminiService } from './services/geminiService';
import { useTheme } from './contexts/ThemeContext';
import { useSources } from './hooks/useSources';
import { useJulesSession } from './hooks/useJulesSession';
import { Logger } from './utils/logger';

export default function App() {
    const [apiKey, setApiKey] = useState<string>('');
    const [isDrawerOpen, setIsDrawerOpen] = useState(false);

    // Get settings from theme context
    const { defaultCardCollapsed, pagination } = useTheme();

    const navigate = useNavigate();
    const location = useLocation();

    const geminiService = useMemo(() => {
        return apiKey ? new GeminiService(apiKey, pagination) : null;
    }, [apiKey, pagination]);

    // Initialize API key from storage
    useEffect(() => {
        // Use sessionStorage for API keys as it's more secure than localStorage
        let storedKey = sessionStorage.getItem('jules_api_key');

        // Migration: If key is in localStorage, move it to sessionStorage and remove from localStorage
        const legacyKey = localStorage.getItem('jules_api_key');
        if (legacyKey && !storedKey) {
            storedKey = legacyKey;
            sessionStorage.setItem('jules_api_key', legacyKey);
            localStorage.removeItem('jules_api_key');
        }

        if (storedKey) {
            setApiKey(storedKey);
        }
    }, []);

    const handleSetKey = (key: string) => {
        // Store key in sessionStorage instead of localStorage to prevent persistence after session ends
        sessionStorage.setItem('jules_api_key', key);
        setApiKey(key);
    };

    // Custom Hooks
    const {
        sources,
        currentSource,
        setCurrentSource,
        isLoading: isSourcesLoading,
        error: sourceError
    } = useSources(geminiService);

    const {
        currentSession,
        setCurrentSession,
        sessions,
        setSessions, // Needed for delete session optimization in hook? No, hook handles it.
        activities,
        sessionsUsed,
        dailyLimit,
        isProcessing: isSessionProcessing,
        error: sessionError,
        setError: setSessionError,
        startPolling,
        handleSendMessage,
        handleApprovePlan,
        handleSelectSession,
        handleDeleteSession,
        handleUpdateSession
    } = useJulesSession(geminiService, currentSource, navigate);

    // Combined processing state
    const isProcessing = isSessionProcessing || isSourcesLoading;
    const error = sessionError || sourceError;

    // Sync state with URL params
    useEffect(() => {
        const syncWithUrl = async () => {
            const pathParts = location.pathname.split('/');

            // Handle Session route
            if (pathParts[1] === 'session' && pathParts[2]) {
                const sessName = `sessions/${pathParts[2]}`;
                if (!currentSession || currentSession.name !== sessName) {
                    try {
                        const sess = await geminiService.getSession(sessName);
                        setCurrentSession(sess);
                        startPolling(sess.name);
                    } catch (e) {
                        Logger.error("Failed to load session from URL", e);
                    }
                }
            }
            // Handle Repository route
            else if (pathParts[1] === 'repository') {
                const repoName = location.pathname.replace('/repository/', 'sources/');
                if (!currentSource || currentSource.name !== repoName) {
                    // We need sources to be loaded first
                    if (sources.length > 0) {
                        const found = sources.find(s => s.name === repoName);
                        if (found) setCurrentSource(found);
                    }
                }
            }
        };

        if (apiKey) syncWithUrl();
    }, [location.pathname, apiKey, sources.length, geminiService]); // Dependencies adapted from original

    const handleSessionMessage = useCallback((text: string) => {
        handleSendMessage(text, { mode: 'START' });
    }, [handleSendMessage]);

    if (!apiKey) {
        return <LoginScreen onSetKey={handleSetKey} />;
    }

    return (
        <div className="h-[100dvh] w-full bg-background text-textMain font-sans flex flex-col overflow-hidden">
            <Header
                onOpenDrawer={() => setIsDrawerOpen(true)}
                currentSource={currentSource}
                sources={sources}
                onSourceChange={(source) => {
                    setCurrentSource(source);
                    navigate(`/repository/${source.name.replace('sources/', '')}`);
                }}
                isLoading={isProcessing}
            />

            <Drawer
                isOpen={isDrawerOpen}
                onClose={() => setIsDrawerOpen(false)}
                sessions={sessions}
                sources={sources}
                currentSessionId={currentSession?.name}
                currentSourceId={currentSource?.name}
                onSelectSession={(session) => {
                    handleSelectSession(session);
                    setIsDrawerOpen(false);
                }}
                onDeleteSession={handleDeleteSession}
                onUpdateSession={handleUpdateSession}
                onSelectSource={(source) => {
                    setCurrentSource(source);
                    navigate(`/repository/${source.name.replace('sources/', '')}`);
                    setIsDrawerOpen(false);
                }}
                sessionsUsed={sessionsUsed}
                dailyLimit={dailyLimit}
            />

            <Routes>
                <Route path="/" element={
                    <HomeView
                        onSendMessage={handleSendMessage}
                        isProcessing={isProcessing}
                        error={error}
                        currentSource={currentSource}
                        onResetKey={() => {
                            sessionStorage.removeItem('jules_api_key');
                            localStorage.removeItem('jules_api_key'); // Ensure legacy key is also removed
                            setApiKey('');
                        }}
                        sessions={sessions}
                        onSelectSession={handleSelectSession}
                    />
                } />

                <Route path="/settings" element={
                    <SettingsView />
                } />

                <Route path="/session/:sessionId" element={
                    currentSession ? (
                        <SessionView
                            session={currentSession}
                            activities={activities}
                            isProcessing={isProcessing}
                            error={error}
                            onSendMessage={handleSessionMessage}
                            onApprovePlan={handleApprovePlan}
                            defaultCardCollapsed={defaultCardCollapsed}
                        />
                    ) : (
                        <div className="flex-1 flex items-center justify-center text-zinc-500">
                            Loading session...
                        </div>
                    )
                } />

                <Route path="/repository/*" element={
                    currentSource ? (
                        <RepositoryView
                            source={currentSource}
                            sessions={sessions}
                            onSelectSession={handleSelectSession}
                            onCreateNew={() => {
                                setCurrentSession(null);
                                navigate('/');
                            }}
                        />
                    ) : (
                        <div className="flex-1 flex items-center justify-center text-zinc-500">
                            Loading repository...
                        </div>
                    )
                } />
            </Routes>
        </div>
    );
}
