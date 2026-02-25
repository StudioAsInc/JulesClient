import { useState, useRef, useCallback, useEffect } from 'react';
import * as JulesApi from '../services/geminiService';
import { JulesSession, JulesActivity, JulesSource } from '../types';
import { SessionCreateOptions } from '../components/InputArea';

export function useJulesSession(
    apiKey: string | null,
    currentSource: JulesSource | null,
    navigate: (path: string) => void
) {
    const [currentSession, setCurrentSession] = useState<JulesSession | null>(null);
    const [sessions, setSessions] = useState<JulesSession[]>([]);
    const [activities, setActivities] = useState<JulesActivity[]>([]);
    const [sessionsUsed, setSessionsUsed] = useState(0);
    const [dailyLimit] = useState(100); // Default to Pro plan
    const [isProcessing, setIsProcessing] = useState(false);
    const [error, setError] = useState<string | null>(null);

    // Polling ref
    const pollTimeout = useRef<number | null>(null);
    const activePollingSession = useRef<string | null>(null);
    const activitiesRef = useRef<JulesActivity[]>([]);

    const fetchSessions = useCallback(async () => {
        if (!apiKey) return;
        try {
            const allSessions = await JulesApi.listAllSessions();
            setSessions(allSessions);

            // Calculate sessions in last 24 hours
            const twentyFourHoursAgo = new Date(Date.now() - 24 * 60 * 60 * 1000);
            const usedCount = allSessions.filter(s => {
                const createDate = new Date(s.createTime);
                return createDate > twentyFourHoursAgo;
            }).length;

            setSessionsUsed(usedCount);
        } catch (e) {
            console.error("Failed to fetch sessions", e);
        }
    }, [apiKey]);

    const startPolling = useCallback((sessionName: string) => {
        if (pollTimeout.current) window.clearTimeout(pollTimeout.current);
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

    // Cleanup polling on unmount
    useEffect(() => {
        return () => {
            activePollingSession.current = null;
            if (pollTimeout.current) window.clearTimeout(pollTimeout.current);
        };
    }, []);

    // Initial fetch
    useEffect(() => {
        if (apiKey) {
            fetchSessions();
        }
    }, [apiKey, fetchSessions]);

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

    const handleSelectSession = useCallback((session: JulesSession) => {
        setCurrentSession(session);
        navigate(`/session/${session.name.replace('sessions/', '')}`);
        startPolling(session.name);
    }, [navigate, startPolling]);

    const handleDeleteSession = useCallback(async (sessionName: string) => {
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
    }, [currentSession, navigate]);

    const handleUpdateSession = useCallback(async (sessionName: string, updates: Partial<JulesSession>, updateMask: string[]) => {
        try {
            const updated = await JulesApi.updateSession(sessionName, updates, updateMask);
            setSessions(prev => prev.map(s => s.name === sessionName ? updated : s));
            if (currentSession?.name === sessionName) {
                setCurrentSession(updated);
            }
        } catch (e: any) {
            setError(e.message || "Failed to update session");
        }
    }, [currentSession]);

    return {
        currentSession,
        setCurrentSession, // Exposed for URL syncing or manual setting
        sessions,
        setSessions, // Exposed if needed
        activities,
        sessionsUsed,
        dailyLimit,
        isProcessing,
        error,
        setError,
        fetchSessions,
        startPolling,
        handleSendMessage,
        handleApprovePlan,
        handleSelectSession,
        handleDeleteSession,
        handleUpdateSession
    };
}
