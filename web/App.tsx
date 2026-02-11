import React, { useState, useEffect, useRef, useCallback } from 'react';
import { Routes, Route, useNavigate, useParams, useLocation } from 'react-router-dom';
import { Header } from './components/Header';
import { HomeView } from './components/HomeView';
import { SessionView } from './components/SessionView';
import { RepositoryView } from './components/RepositoryView';
import { SettingsView } from './components/SettingsView';
import { Drawer } from './components/Drawer';
import * as JulesApi from './services/geminiService';
import { JulesActivity, JulesSource, JulesSession, AutomationMode } from './types';
import { AlertCircle, Key, ChevronRight } from 'lucide-react';
import { SessionMode, SessionCreateOptions } from './components/InputArea';
import { useTheme } from './contexts/ThemeContext';

export default function App() {
    const [apiKey, setApiKey] = useState<string>('');
    const [sources, setSources] = useState<JulesSource[]>([]);
    const [currentSource, setCurrentSource] = useState<JulesSource | null>(null);

    const [currentSession, setCurrentSession] = useState<JulesSession | null>(null);
    const [sessions, setSessions] = useState<JulesSession[]>([]);
    const [tempKey, setTempKey] = useState('');
    const [activities, setActivities] = useState<JulesActivity[]>([]);
    const [sessionsUsed, setSessionsUsed] = useState(0);
    const [dailyLimit] = useState(100); // Default to Pro plan

    // Get settings from theme context
    const { defaultCardCollapsed } = useTheme();

    const navigate = useNavigate();
    const location = useLocation();
    const [isProcessing, setIsProcessing] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [isDrawerOpen, setIsDrawerOpen] = useState(false);

    // Polling ref
    const pollTimeout = useRef<number | null>(null);
    const activePollingSession = useRef<string | null>(null);
    const activitiesRef = useRef<JulesActivity[]>([]);

    useEffect(() => {
        const key = localStorage.getItem('jules_api_key');
        if (key) {
            setApiKey(key);
            JulesApi.setApiKey(key);
            fetchSources();
            fetchSessions();
        }
    }, []);

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
    }, [location.pathname, apiKey, sources.length]);

    const handleSetKey = (key: string) => {
        localStorage.setItem('jules_api_key', key);
        setApiKey(key);
        JulesApi.setApiKey(key);
        fetchSources();
        fetchSessions();
    };

    const fetchSources = async () => {
        try {
            const response = await JulesApi.listSources();
            setSources(response.sources);
            if (response.sources.length > 0 && !currentSource) {
                // If we are on settings page, don't force select source yet, wait for nav
                // But generally we want a source.
                setCurrentSource(response.sources[0]);
            }
        } catch (e) {
            console.error(e);
            // If 401, reset key
            if (e instanceof Error && e.message.includes('Invalid API Key')) {
                setError("Invalid API Key. Please reset.");
            }
        }
    };



    const fetchSessions = async () => {
        const allSessions = await JulesApi.listAllSessions();
        setSessions(allSessions);

        // Calculate sessions in last 24 hours
        const twentyFourHoursAgo = new Date(Date.now() - 24 * 60 * 60 * 1000);
        const usedCount = allSessions.filter(s => {
            const createDate = new Date(s.createTime);
            return createDate > twentyFourHoursAgo;
        }).length;

        setSessionsUsed(usedCount);
    };

    const startPolling = useCallback((sessionName: string) => {
        if (pollTimeout.current) clearTimeout(pollTimeout.current);
        activePollingSession.current = sessionName;

        // Reset activities if switching sessions
        if (activitiesRef.current.length > 0 && !activitiesRef.current[0].name.startsWith(sessionName)) {
            activitiesRef.current = [];
            setActivities([]);
        }

        const poll = async () => {
            if (activePollingSession.current !== sessionName) return;

            try {
                const sessionId = sessionName.split('/').pop()!;
                // Incremental fetch if we have existing activities
                const lastActivity = activitiesRef.current[activitiesRef.current.length - 1];
                const options = (activitiesRef.current.length > 0 && lastActivity?.createTime)
                    ? { createTime: lastActivity.createTime }
                    : undefined;

                const response = await JulesApi.listActivities(sessionId, options);
                if (activePollingSession.current !== sessionName) return;

                const newActivities = response.activities;

                // Merge new activities
                if (newActivities.length > 0) {
                    const existingNames = new Set(activitiesRef.current.map(a => a.name));
                    const uniqueNew = newActivities.filter(a => !existingNames.has(a.name));

                    if (uniqueNew.length > 0) {
                        const updated = [...activitiesRef.current, ...uniqueNew];
                        activitiesRef.current = updated;
                        setActivities(updated);
                    }
                } else if (activitiesRef.current.length === 0 && !options) {
                    // Initial load returned empty
                    setActivities([]);
                }

                // Also check session status for outputs and state
                const sess = await JulesApi.getSession(sessionName);
                if (activePollingSession.current !== sessionName) return;

                setCurrentSession(prev => {
                    // Only update if state has changed to avoid unnecessary re-renders
                    if (!prev || prev.state !== sess.state || prev.outputs?.length !== sess.outputs?.length) {
                        return sess;
                    }
                    return prev;
                });

                // Use API state to determine if processing
                // Active states: QUEUED, PLANNING, IN_PROGRESS
                // Waiting states: AWAITING_PLAN_APPROVAL, AWAITING_USER_FEEDBACK, PAUSED
                // Terminal states: COMPLETED, FAILED
                const isActive = ['QUEUED', 'PLANNING', 'IN_PROGRESS'].includes(sess.state);
                setIsProcessing(isActive);

                const isTerminal = ['COMPLETED', 'FAILED'].includes(sess.state);

                if (activePollingSession.current === sessionName && !isTerminal) {
                    pollTimeout.current = window.setTimeout(poll, 2000);
                }
            } catch (e) {
                console.error("Polling error", e);
                // Retry on error
                if (activePollingSession.current === sessionName) {
                    pollTimeout.current = window.setTimeout(poll, 2000);
                }
            }
        };

        poll();
    }, []);

    // Memoized handler to prevent InputArea re-renders
    const handleSendMessage = useCallback(async (text: string, options: SessionCreateOptions) => {
        setError(null);
        setIsProcessing(true);

        try {
            if (!currentSession) {
                // CREATE NEW SESSION
                if (!currentSource) {
                    throw new Error("Please select a repository first.");
                }

                // Map UI Mode to API Options
                const requireApproval = options.mode === 'REVIEW' || options.mode === 'SCHEDULED' || options.mode === 'INTERACTIVE';

                const session = await JulesApi.createSession(text, currentSource.name, {
                    title: options.title,
                    requirePlanApproval: requireApproval,
                    startingBranch: options.branch || 'main',
                    automationMode: options.automationMode || 'AUTO_CREATE_PR'
                });

                setCurrentSession(session);
                setSessions(prev => [session, ...prev]);
                setSessionsUsed(prev => prev + 1);
                navigate(`/session/${session.name.replace('sessions/', '')}`);
                startPolling(session.name);
            } else {
                // SEND MESSAGE TO EXISTING SESSION
                await JulesApi.sendMessage(currentSession.name, text);
                // Force immediate update - polling will set isProcessing based on API state
                const sessionId = currentSession.name.split('/').pop()!;
                const lastActivity = activitiesRef.current[activitiesRef.current.length - 1];
                const response = await JulesApi.listActivities(sessionId, {
                    createTime: lastActivity?.createTime
                });

                // Merge
                if (response.activities.length > 0) {
                     const existingNames = new Set(activitiesRef.current.map(a => a.name));
                     const uniqueNew = response.activities.filter(a => !existingNames.has(a.name));
                     if (uniqueNew.length > 0) {
                         const updated = [...activitiesRef.current, ...uniqueNew];
                         activitiesRef.current = updated;
                         setActivities(updated);
                     }
                }
                // Polling will handle isProcessing based on session.state from API
            }
        } catch (e: any) {
            setError(e.message || "An error occurred");
            setIsProcessing(false);
        }
    }, [currentSession, currentSource, navigate, startPolling]);

    const handleApprovePlan = useCallback(async (activityName: string) => {
        if (!currentSession) return;
        setIsProcessing(true);
        try {
            await JulesApi.approvePlan(currentSession.name);
        } catch (e: any) {
            setError(e.message);
        } finally {
            setIsProcessing(false);
        }
    }, [currentSession]);

    const handleSessionMessage = useCallback((text: string) => {
        handleSendMessage(text, { mode: 'START' });
    }, [handleSendMessage]);

    const handleSelectSession = (session: JulesSession) => {
        setCurrentSession(session);
        navigate(`/session/${session.name.replace('sessions/', '')}`);
        setIsDrawerOpen(false);
        startPolling(session.name);
    };

    const handleDeleteSession = async (sessionName: string) => {
        try {
            await JulesApi.deleteSession(sessionName);
            setSessions(prev => prev.filter(s => s.name !== sessionName));

            // If the deleted session is the current one, redirect to home
            if (currentSession?.name === sessionName) {
                setCurrentSession(null);
                setActivities([]);
                navigate('/');
            }
        } catch (e: any) {
            setError(e.message || "Failed to delete session");
        }
    };

    const handleUpdateSession = async (sessionName: string, updates: Partial<JulesSession>, updateMask: string[]) => {
        try {
            const updated = await JulesApi.updateSession(sessionName, updates, updateMask);
            setSessions(prev => prev.map(s => s.name === sessionName ? updated : s));
            if (currentSession?.name === sessionName) {
                setCurrentSession(updated);
            }
        } catch (e: any) {
            setError(e.message || "Failed to update session");
        }
    };

    // Cleanup polling on unmount or session switch
    useEffect(() => {
        return () => {
            activePollingSession.current = null;
            if (pollTimeout.current) clearTimeout(pollTimeout.current);
        };
    }, []);

    if (!apiKey) {
        return (
            <div className="min-h-screen bg-background flex items-center justify-center p-4">
                <div className="w-full max-w-md bg-[#161619] border border-white/10 rounded-2xl p-8 shadow-2xl">
                    <div className="flex justify-center mb-6">
                        <img src="https://jules.google/squid.png" alt="Jules" className="w-12 h-12 opacity-80" />
                    </div>
                    <h2 className="text-xl font-medium text-center text-white mb-2">Welcome to Jules Client</h2>
                    <p className="text-zinc-500 text-center text-sm mb-6">Enter your API Key to access your Jules agent.</p>

                    <form onSubmit={(e) => {
                        e.preventDefault();
                        handleSetKey(tempKey);
                    }}>
                        <div className="mb-4">
                            <label htmlFor="apiKey" className="block text-xs font-medium text-zinc-400 mb-1.5 uppercase tracking-wide">
                                API Key <span className="text-indigo-500">*</span>
                            </label>
                            <div className="relative">
                                <Key className="absolute left-3 top-1/2 -translate-y-1/2 text-zinc-500" size={16} />
                                <input
                                    id="apiKey"
                                    name="key"
                                    type="password"
                                    value={tempKey}
                                    onChange={(e) => setTempKey(e.target.value)}
                                    placeholder="sk-..."
                                    className="w-full bg-black/50 border border-white/10 rounded-xl py-3 pl-10 pr-4 text-white focus:outline-none focus:border-indigo-500 transition-colors"
                                    autoFocus
                                    required
                                    aria-required="true"
                                />
                            </div>
                        </div>
                        <button
                            type="submit"
                            disabled={!tempKey.trim()}
                            className={`
                                w-full font-medium py-3 rounded-xl transition-colors flex items-center justify-center gap-2
                                ${!tempKey.trim()
                                    ? 'bg-[#27272a] text-zinc-500 cursor-not-allowed'
                                    : 'bg-indigo-600 hover:bg-indigo-500 text-white'}
                            `}
                        >
                            Enter App <ChevronRight size={16} />
                        </button>
                    </form>
                </div>
            </div>
        );
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
                onSelectSession={handleSelectSession}
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