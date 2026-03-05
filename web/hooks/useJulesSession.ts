import { useSessionList } from './useSessionList';
import { useActiveSession } from './useActiveSession';
import { JulesSource } from '../types';

import { GeminiService } from '../services/geminiService';

export function useJulesSession(
    service: GeminiService | null,
    currentSource: JulesSource | null,
    navigate: (path: string) => void
) {
    const sessionList = useSessionList(service);

    const activeSession = useActiveSession(
        service,
        currentSource,
        navigate,
        {
            addSession: sessionList.addSession,
            updateSession: sessionList.updateSession,
            removeSession: sessionList.removeSession
        }
    );

    return {
        // Session List
        sessions: sessionList.sessions,
        setSessions: sessionList.setSessions,
        sessionsUsed: sessionList.sessionsUsed,
        dailyLimit: sessionList.dailyLimit,
        fetchSessions: sessionList.fetchSessions,

        // Active Session
        currentSession: activeSession.currentSession,
        setCurrentSession: activeSession.setCurrentSession,
        activities: activeSession.activities,
        isProcessing: activeSession.isProcessing,
        error: activeSession.error,
        setError: activeSession.setError,
        startPolling: activeSession.startPolling,
        handleSendMessage: activeSession.handleSendMessage,
        handleApprovePlan: activeSession.handleApprovePlan,
        handleSelectSession: activeSession.handleSelectSession,
        handleDeleteSession: activeSession.handleDeleteSession,
        handleUpdateSession: activeSession.handleUpdateSession
    };
}
