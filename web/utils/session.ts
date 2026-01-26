import { JulesSession, SessionState } from '../types';

export interface SessionDisplayInfo {
    label: string;
    emoji: string;
    helperText: string;
    cta: string;
    shimmer: boolean;
}

export const getSessionDisplayInfo = (state: SessionState): SessionDisplayInfo => {
    switch (state) {
        case 'IN_PROGRESS':
            return {
                label: 'In Progress',
                emoji: '🚧',
                helperText: 'Generating solution — hang tight.',
                cta: 'none',
                shimmer: true
            };
        case 'PLANNING':
            return {
                label: 'Planning',
                emoji: '🧠',
                helperText: 'Analyzing requirements...',
                cta: 'none',
                shimmer: true
            };
        case 'QUEUED':
            return {
                label: 'Queued',
                emoji: '⏳',
                helperText: 'Waiting for agent availability.',
                cta: 'none',
                shimmer: true
            };
        case 'AWAITING_PLAN_APPROVAL':
            return {
                label: 'Plan Ready',
                emoji: '📋',
                helperText: 'Review the proposed plan.',
                cta: 'Approve',
                shimmer: false
            };
        case 'AWAITING_USER_FEEDBACK':
            return {
                label: 'Feedback Needed',
                emoji: '🗣️',
                helperText: 'Please provide your input.',
                cta: 'Respond',
                shimmer: false
            };
        case 'PAUSED':
            return {
                label: 'Paused',
                emoji: '⏸️',
                helperText: 'Session is currently paused.',
                cta: 'Resume',
                shimmer: false
            };
        case 'COMPLETED':
            return {
                label: 'Completed',
                emoji: '✅',
                helperText: 'Task finished successfully.',
                cta: 'none',
                shimmer: false
            };
        case 'FAILED':
            return {
                label: 'Failed',
                emoji: '❌',
                helperText: 'Something went wrong.',
                cta: 'Retry',
                shimmer: false
            };
        default:
            return {
                label: 'Unknown',
                emoji: '❓',
                helperText: '',
                cta: 'none',
                shimmer: false
            };
    }
};

const getGroupScore = (state: SessionState): number => {
    // Lower score = higher sort order (1st group)
    switch (state) {
        // Group 1: Working
        case 'IN_PROGRESS':
        case 'PLANNING':
        case 'QUEUED':
            return 1;
        // Group 2: User Blocked
        case 'AWAITING_PLAN_APPROVAL':
        case 'AWAITING_USER_FEEDBACK':
            return 2;
        // Group 3: Paused
        case 'PAUSED':
            return 3;
        // Group 4: Terminal
        case 'COMPLETED':
        case 'FAILED':
            return 4;
        default:
            return 5;
    }
};

export const sortSessions = (sessions: JulesSession[]): JulesSession[] => {
    return [...sessions].sort((a, b) => {
        // 1. Group Priority
        const groupA = getGroupScore(a.state);
        const groupB = getGroupScore(b.state);
        if (groupA !== groupB) {
            return groupA - groupB;
        }

        // 2. Priority (Descending)
        // Default to 0 if undefined
        const priorityA = a.priority ?? 0;
        const priorityB = b.priority ?? 0;
        if (priorityA !== priorityB) {
            return priorityB - priorityA;
        }

        // 3. Last Update (Most recent first / Descending)
        const timeA = a.updateTime ? new Date(a.updateTime).getTime() : 0;
        const timeB = b.updateTime ? new Date(b.updateTime).getTime() : 0;
        return timeB - timeA;
    });
};
