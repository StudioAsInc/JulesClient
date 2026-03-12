import { useState, useRef, useCallback, useEffect } from 'react';
import { GeminiService } from '../services/geminiService';
import { JulesSession, JulesActivity, JulesSource } from '../types';
import { SessionCreateOptions } from '../components/InputArea';
import { Logger } from '../utils/logger';

interface SessionListActions {
    addSession: (session: JulesSession) => void;
    updateSession: (name: string, session: JulesSession) => void;
    removeSession: (name: string) => void;
}

export function useActiveSession(
    service: GeminiService | null,
    currentSource: JulesSource | null,
    navigate: (path: string) => void,
    sessionListActions: SessionListActions
) {
    const [currentSession, setCurrentSession] = useState<JulesSession | null>(null);
    const [activities, setActivities] = useState<JulesActivity[]>([]);
    const [isProcessing, setIsProcessing] = useState(false);
    const [error, setError] = useState<string | null>(null);

    // Polling ref
    const pollTimeout = useRef<number | null>(null);
    const activePollingSession = useRef<string | null>(null);
    const activitiesRef = useRef<JulesActivity[]>([]);

    const startPolling = useCallback((sessionName: string) => {
        if (pollTimeout.current) window.clearTimeout(pollTimeout.current);
        activePollingSession.current = sessionName;

        // Reset activities if switching sessions
        if (activitiesRef.current.length > 0 && !activitiesRef.current[0].name.startsWith(sessionName)) {
            activitiesRef.current = [];
            setActivities([]);
        }

        const poll = async () => {
            if (activePollingSession.current !== sessionName || !service) return;

            try {
                const sessionId = sessionName.split('/').pop()!;
                // Incremental fetch if we have existing activities
                const lastActivity = activitiesRef.current[activitiesRef.current.length - 1];
                const options = (activitiesRef.current.length > 0 && lastActivity?.createTime)
                    ? { createTime: lastActivity.createTime }
                    : undefined;

                const response = await service.listActivities(sessionId, options);
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
                const sess = await service.getSession(sessionName);
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
                Logger.error("Polling error", e);
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

    const handleSendMessage = useCallback(async (text: string, options: SessionCreateOptions) => {
        if (!service) return;
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

                const session = await service.createSession(text, currentSource.name, {
                    title: options.title,
                    requirePlanApproval: requireApproval,
                    startingBranch: options.branch || 'main',
                    automationMode: options.automationMode || 'AUTO_CREATE_PR'
                });

                setCurrentSession(session);
                sessionListActions.addSession(session);
                navigate(`/session/${session.name.replace('sessions/', '')}`);
                startPolling(session.name);
            } else {
                // SEND MESSAGE TO EXISTING SESSION
                await service.sendMessage(currentSession.name, text);
                // Force immediate update - polling will set isProcessing based on API state
                const sessionId = currentSession.name.split('/').pop()!;
                const lastActivity = activitiesRef.current[activitiesRef.current.length - 1];
                const response = await service.listActivities(sessionId, {
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
    }, [currentSession, currentSource, navigate, startPolling, sessionListActions]);

    const handleApprovePlan = useCallback(async (activityName: string) => {
        if (!currentSession || !service) return;
        setIsProcessing(true);
        try {
            await service.approvePlan(currentSession.name);
        } catch (e: any) {
            setError(e.message);
        } finally {
            setIsProcessing(false);
        }
    }, [currentSession, service]);

    const handleSelectSession = useCallback((session: JulesSession) => {
        setCurrentSession(session);
        navigate(`/session/${session.name.replace('sessions/', '')}`);
        startPolling(session.name);
    }, [navigate, startPolling]);

    const handleDeleteSession = useCallback(async (sessionName: string) => {
        if (!service) return;
        try {
            await service.deleteSession(sessionName);
            sessionListActions.removeSession(sessionName);

            // If the deleted session is the current one, redirect to home
            if (currentSession?.name === sessionName) {
                setCurrentSession(null);
                setActivities([]);
                navigate('/');
            }
        } catch (e: any) {
            setError(e.message || "Failed to delete session");
        }
    }, [currentSession, navigate, sessionListActions, service]);

    const handleUpdateSession = useCallback(async (sessionName: string, updates: Partial<JulesSession>, updateMask: string[]) => {
        if (!service) return;
        try {
            const updated = await service.updateSession(sessionName, updates, updateMask);
            sessionListActions.updateSession(sessionName, updated);
            if (currentSession?.name === sessionName) {
                setCurrentSession(updated);
            }
        } catch (e: any) {
            setError(e.message || "Failed to update session");
        }
    }, [currentSession, sessionListActions, service]);

    return {
        currentSession,
        setCurrentSession, // Exposed for URL syncing or manual setting
        activities,
        isProcessing,
        error,
        setError,
        startPolling,
        handleSendMessage,
        handleApprovePlan,
        handleSelectSession,
        handleDeleteSession,
        handleUpdateSession
    };
}
