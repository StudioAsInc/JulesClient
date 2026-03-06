import { useState, useCallback, useEffect } from 'react';
import { GeminiService } from '../services/geminiService';
import { JulesSession } from '../types';
import { Logger } from '../utils/logger';

export function useSessionList(service: GeminiService | null) {
    const [sessions, setSessions] = useState<JulesSession[]>([]);
    const [sessionsUsed, setSessionsUsed] = useState(0);
    const [dailyLimit] = useState(100); // Default to Pro plan

    const fetchSessions = useCallback(async () => {
        if (!service) return;
        try {
            const allSessions = await service.listAllSessions();
            setSessions(allSessions);

            // Calculate sessions in last 24 hours
            const twentyFourHoursAgo = new Date(Date.now() - 24 * 60 * 60 * 1000);
            const usedCount = allSessions.filter(s => {
                const createDate = new Date(s.createTime);
                return createDate > twentyFourHoursAgo;
            }).length;

            setSessionsUsed(usedCount);
        } catch (e) {
            Logger.error("Failed to fetch sessions", e);
        }
    }, [service]);

    // Initial fetch
    useEffect(() => {
        if (service) {
            fetchSessions();
        }
    }, [service, fetchSessions]);

    // Helper functions to modify local state
    const addSession = useCallback((session: JulesSession) => {
        setSessions(prev => [session, ...prev]);
        setSessionsUsed(prev => prev + 1);
    }, []);

    const updateSession = useCallback((sessionName: string, updatedSession: JulesSession) => {
        setSessions(prev => prev.map(s => s.name === sessionName ? updatedSession : s));
    }, []);

    const removeSession = useCallback((sessionName: string) => {
        setSessions(prev => prev.filter(s => s.name !== sessionName));
    }, []);

    return {
        sessions,
        setSessions,
        sessionsUsed,
        dailyLimit,
        fetchSessions,
        addSession,
        updateSession,
        removeSession
    };
}
