import React from 'react';

// ==================== API ENUMS ====================

export type AutomationMode = 'AUTO_CREATE_PR' | 'NONE' | 'AUTO_MERGE';

export type SessionState =
    | 'QUEUED'
    | 'PLANNING'
    | 'AWAITING_PLAN_APPROVAL'
    | 'AWAITING_USER_FEEDBACK'
    | 'IN_PROGRESS'
    | 'PAUSED'
    | 'COMPLETED'
    | 'FAILED';

// ==================== SOURCE TYPES ====================

export interface GitHubRepoInfo {
    owner: string;
    repo: string;
    isPrivate?: boolean;
    defaultBranch?: { displayName: string };
    branches?: Array<{ displayName: string }>;
}

export interface JulesSource {
    name: string; // "sources/github/owner/repo"
    id?: string; // "github/owner/repo"
    displayName?: string; // helper for UI: "owner/repo"
    githubRepo?: GitHubRepoInfo;
}

// ==================== SESSION TYPES ====================

export interface SourceContext {
    source: string; // "sources/github/owner/repo"
    githubRepoContext?: {
        startingBranch?: string;
    };
}

export interface PullRequestOutput {
    url: string;
    title: string;
    description: string;
    branch?: string;
}

export interface SessionOutput {
    pullRequest?: PullRequestOutput;
}

export interface JulesSession {
    name: string; // "sessions/{id}"
    id?: string;
    title?: string;
    prompt: string;
    state: SessionState;
    priority?: number;
    createTime: string;
    updateTime?: string;
    // Additional API fields
    sourceContext?: SourceContext;
    automationMode?: AutomationMode;
    requirePlanApproval?: boolean;
    outputs?: SessionOutput[];
}

// ==================== PLAN TYPES ====================

export interface Step {
    id?: string;
    index?: number;
    title: string;
    description?: string;
}

export interface Plan {
    id?: string;
    steps: Step[];
    createTime?: string;
}

// ==================== ACTIVITY TYPES ====================

export interface MessageContent {
    prompt?: string;
    text?: string;
    message?: string;
    userMessage?: string;
    user_message?: string;
    agentMessage?: string;
    agent_message?: string;
    content?: string;
    parts?: { text: string }[];
}

export interface ProgressUpdate {
    status?: string;
    status_update?: string;
    title?: string;
    progress_title?: string;
    description?: string;
    progress_description?: string;
    text?: string;
    message?: string;
}

export interface MediaArtifact {
    mimeType: string;
    data: string;
}

export interface BashOutputArtifact {
    command: string;
    output: string;
    exitCode: number;
}

export interface ChangeSetArtifact {
    source?: string;
    gitPatch?: {
        baseCommitId?: string;
        unidiffPatch?: string;
        suggestedCommitMessage?: string;
    };
}

export interface ActivityArtifact {
    media?: MediaArtifact;
    bashOutput?: BashOutputArtifact;
    changeSet?: ChangeSetArtifact;
}

export interface JulesActivity {
    name: string; // "sessions/{id}/activities/{id}"
    id?: string;
    originator?: 'system' | 'agent' | 'user';
    description?: string;
    createTime: string;

    // Activity types (oneof)
    userMessaged?: MessageContent | string;
    userMessage?: MessageContent | string;
    agentMessaged?: MessageContent | string;
    agentMessage?: MessageContent | string;
    planGenerated?: {
        plan: Plan;
    };
    planApproved?: {
        planId?: string;
    };
    sessionCompleted?: {};
    sessionFailed?: {
        reason?: string;
    };
    progressUpdated?: ProgressUpdate;

    // Artifacts
    artifacts?: ActivityArtifact[];
}

// ==================== PAGINATION TYPES ====================

export interface PaginatedResponse<T> {
    items: T[];
    nextPageToken?: string;
    hasMore: boolean;
}

export interface ListSourcesResponse {
    sources: JulesSource[];
    nextPageToken?: string;
}

export interface ListSessionsResponse {
    sessions: JulesSession[];
    nextPageToken?: string;
}

export interface ListActivitiesResponse {
    activities: JulesActivity[];
    nextPageToken?: string;
}

// ==================== UI UTILITY TYPES ====================

export interface SuggestionCategory {
    id: string;
    label: string;
    icon: React.ReactNode;
}

// ==================== HELPER UTILITIES ====================

/**
 * Format relative time from ISO string (e.g., "2 hours ago")
 */
export const formatRelativeTime = (isoString?: string): string => {
    if (!isoString) return '';

    try {
        const date = new Date(isoString);
        const now = new Date();
        const diffMs = now.getTime() - date.getTime();
        const diffSec = Math.floor(diffMs / 1000);
        const diffMin = Math.floor(diffSec / 60);
        const diffHour = Math.floor(diffMin / 60);
        const diffDay = Math.floor(diffHour / 24);
        const diffWeek = Math.floor(diffDay / 7);
        const diffMonth = Math.floor(diffDay / 30);

        if (diffSec < 60) return 'just now';
        if (diffMin < 60) return `${diffMin}m ago`;
        if (diffHour < 24) return `${diffHour}h ago`;
        if (diffDay < 7) return `${diffDay}d ago`;
        if (diffWeek < 4) return `${diffWeek}w ago`;
        if (diffMonth < 12) return `${diffMonth}mo ago`;

        return date.toLocaleDateString();
    } catch {
        return '';
    }
};

/**
 * Get session state display info
 */
export const getSessionStateInfo = (state?: SessionState): { label: string; color: string } => {
    switch (state) {
        case 'QUEUED':
            return { label: 'Queued', color: 'yellow' };
        case 'PLANNING':
            return { label: 'Planning', color: 'blue' };
        case 'AWAITING_PLAN_APPROVAL':
            return { label: 'Awaiting Approval', color: 'amber' };
        case 'AWAITING_USER_FEEDBACK':
            return { label: 'Needs Feedback', color: 'amber' };
        case 'IN_PROGRESS':
            return { label: 'In Progress', color: 'blue' };
        case 'PAUSED':
            return { label: 'Paused', color: 'zinc' };
        case 'COMPLETED':
            return { label: 'Completed', color: 'green' };
        case 'FAILED':
            return { label: 'Failed', color: 'red' };
        default:
            return { label: 'Unknown', color: 'zinc' };
    }
};