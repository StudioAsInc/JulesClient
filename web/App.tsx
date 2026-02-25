import React, { useState, useEffect, useCallback } from 'react';
import { Routes, Route, useNavigate, useLocation } from 'react-router-dom';
import { Header } from './components/Header';
import { HomeView } from './components/HomeView';
import { SessionView } from './components/SessionView';
import { RepositoryView } from './components/RepositoryView';
import { SettingsView } from './components/SettingsView';
import { Drawer } from './components/Drawer';
import { LoginScreen } from './components/LoginScreen';
import * as JulesApi from './services/geminiService';
import { useTheme } from './contexts/ThemeContext';
import { useSources } from './hooks/useSources';
import { useJulesSession } from './hooks/useJulesSession';

export default function App() {
    const [apiKey, setApiKey] = useState<string>('');
    const [isDrawerOpen, setIsDrawerOpen] = useState(false);

    // Get settings from theme context
    const { defaultCardCollapsed } = useTheme();

    const navigate = useNavigate();
    const location = useLocation();

    // Initialize API key from local storage or environment
    useEffect(() => {
        // Check environment variables first (injected by Vite)
        const envKey = process.env.GEMINI_API_KEY || process.env.API_KEY;
        const storedKey = localStorage.getItem('jules_api_key');
        const key = envKey || storedKey;

        if (key) {
            setApiKey(key);
            JulesApi.setApiKey(key);
        }
    }, []);

    const handleSetKey = (key: string) => {
        localStorage.setItem('jules_api_key', key);
        setApiKey(key);
        JulesApi.setApiKey(key);
    };

    // Custom Hooks
    const {
        sources,
        currentSource,
        setCurrentSource,
        isLoading: isSourcesLoading,
        error: sourceError
    } = useSources(apiKey);

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
    } = useJulesSession(apiKey, currentSource, navigate);

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
                        const sess = await JulesApi.getSession(sessName);
                        setCurrentSession(sess);
                        startPolling(sess.name);
                    } catch (e) {
                        console.error("Failed to load session from URL", e);
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
    }, [location.pathname, apiKey, sources.length]); // Dependencies adapted from original

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
                            localStorage.removeItem('jules_api_key');
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
