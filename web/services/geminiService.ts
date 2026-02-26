import {
  JulesActivity,
  JulesSession,
  JulesSource,
  AutomationMode,
  ListSourcesResponse,
  ListSessionsResponse,
  ListActivitiesResponse,
} from "../types";
import { PaginationSettings } from "../types/themeTypes";

export interface ListSourcesOptions {
  pageSize?: number;
  pageToken?: string;
}

export interface ListSessionsOptions {
  pageSize?: number;
  pageToken?: string;
}

export interface CreateSessionOptions {
  title?: string;
  requirePlanApproval?: boolean;
  startingBranch?: string;
  automationMode?: AutomationMode;
}

export interface ListActivitiesOptions {
  pageSize?: number;
  pageToken?: string;
  createTime?: string;
}

export class GeminiService {
  private apiKey: string;
  private paginationSettings: PaginationSettings;
  private readonly baseUrl = "https://jules.googleapis.com/v1alpha";

  constructor(apiKey: string, paginationSettings?: PaginationSettings) {
    this.apiKey = apiKey;
    this.paginationSettings = paginationSettings || {
      autoPaginate: true,
      pageSize: 20
    };
  }

  private getHeaders() {
    return {
      "Content-Type": "application/json",
      "x-goog-api-key": this.apiKey,
    };
  }

  // ==================== SOURCES ====================

  async listSources(options?: ListSourcesOptions): Promise<ListSourcesResponse> {
    try {
      const params = new URLSearchParams();
      params.append('pageSize', String(options?.pageSize ?? this.paginationSettings.pageSize));
      if (options?.pageToken) params.append('pageToken', options.pageToken);

      const queryString = params.toString();
      const url = `${this.baseUrl}/sources${queryString ? `?${queryString}` : ''}`;

      const res = await fetch(url, { headers: this.getHeaders() });

      if (res.status === 401) throw new Error("Invalid API Key");
      if (!res.ok) throw new Error("Failed to fetch sources");

      const data = await res.json();

      const sources = (data.sources || []).map((s: any) => {
        const repo = s.githubRepo || s.github_repo;
        return {
          ...s,
          id: s.id,
          githubRepo: repo ? {
            owner: repo.owner,
            repo: repo.repo,
            isPrivate: repo.isPrivate ?? repo.is_private ?? false,
            defaultBranch: repo.defaultBranch || repo.default_branch,
            branches: repo.branches || []
          } : undefined,
          displayName: repo ? `${repo.owner}/${repo.repo}` : s.name.split('/').slice(-2).join('/')
        };
      });

      return {
        sources,
        nextPageToken: data.nextPageToken || data.next_page_token
      };
    } catch (error) {
      console.error("listSources error:", error);
      throw error;
    }
  }

  /**
   * Fetch all sources with automatic pagination
   */
  async listAllSources(): Promise<JulesSource[]> {
    const allSources: JulesSource[] = [];
    let pageToken: string | undefined;

    do {
      const response = await this.listSources({ pageSize: this.paginationSettings.pageSize, pageToken });
      allSources.push(...response.sources);
      pageToken = this.paginationSettings.autoPaginate ? response.nextPageToken : undefined;
    } while (pageToken);

    return allSources;
  }

  async getSource(sourceName: string): Promise<JulesSource> {
    try {
      const url = sourceName.startsWith('sources/')
        ? `${this.baseUrl}/${sourceName}`
        : `${this.baseUrl}/sources/${sourceName}`;

      const res = await fetch(url, { headers: this.getHeaders() });

      if (res.status === 401) throw new Error("Invalid API Key");
      if (!res.ok) throw new Error("Failed to fetch source");

      const data = await res.json();
      const repo = data.githubRepo || data.github_repo;

      return {
        ...data,
        id: data.id,
        githubRepo: repo ? {
          owner: repo.owner,
          repo: repo.repo,
          isPrivate: repo.isPrivate ?? repo.is_private ?? false,
          defaultBranch: repo.defaultBranch || repo.default_branch,
          branches: repo.branches || []
        } : undefined,
        displayName: repo ? `${repo.owner}/${repo.repo}` : data.name.split('/').slice(-2).join('/')
      };
    } catch (error) {
      console.error("getSource error:", error);
      throw error;
    }
  }

  // ==================== SESSIONS ====================

  async listSessions(options?: ListSessionsOptions): Promise<ListSessionsResponse> {
    try {
      const params = new URLSearchParams();
      params.append('pageSize', String(options?.pageSize ?? this.paginationSettings.pageSize));
      if (options?.pageToken) params.append('pageToken', options.pageToken);

      const res = await fetch(`${this.baseUrl}/sessions?${params.toString()}`, { headers: this.getHeaders() });

      if (!res.ok) {
        console.warn("listSessions failed with status:", res.status);
        return { sessions: [], nextPageToken: undefined };
      }

      const data = await res.json();
      const sessions = (data.sessions || []).map(mapSession);

      return {
        sessions,
        nextPageToken: data.nextPageToken || data.next_page_token
      };
    } catch (error) {
      console.error("listSessions network error:", error);
      return { sessions: [], nextPageToken: undefined };
    }
  }

  /**
   * Fetch all sessions with automatic pagination
   */
  async listAllSessions(): Promise<JulesSession[]> {
    const allSessions: JulesSession[] = [];
    let pageToken: string | undefined;

    do {
      const response = await this.listSessions({ pageSize: this.paginationSettings.pageSize, pageToken });
      allSessions.push(...response.sessions);
      pageToken = this.paginationSettings.autoPaginate ? response.nextPageToken : undefined;
    } while (pageToken);

    return allSessions;
  }

  async createSession(
    prompt: string,
    sourceName?: string,
    options?: CreateSessionOptions
  ): Promise<JulesSession> {
    const payload: Record<string, any> = {
      prompt
    };

    if (sourceName) {
      payload.requirePlanApproval = options?.requirePlanApproval ?? true;
      payload.automationMode = options?.automationMode ?? "AUTO_CREATE_PR";
      payload.sourceContext = {
        source: sourceName,
        githubRepoContext: {
          startingBranch: options?.startingBranch || "main"
        }
      };
    }

    // Add optional title if provided
    if (options?.title) {
      payload.title = options.title;
    }

    const res = await fetch(`${this.baseUrl}/sessions`, {
      method: "POST",
      headers: this.getHeaders(),
      body: JSON.stringify(payload),
    });

    if (!res.ok) {
      const err = await res.text();
      throw new Error(`Failed to create session: ${err}`);
    }

    const data = await res.json();
    return mapSession(data, prompt);
  }

  async getSession(sessionId: string): Promise<JulesSession> {
    const url = sessionId.startsWith('sessions/')
      ? `${this.baseUrl}/${sessionId}`
      : `${this.baseUrl}/sessions/${sessionId}`;

    const res = await fetch(url, { headers: this.getHeaders() });
    if (!res.ok) throw new Error("Failed to get session");

    const data = await res.json();
    return mapSession(data);
  }

  // ==================== ACTIVITIES ====================

  async listActivities(
    sessionId: string,
    options?: ListActivitiesOptions
  ): Promise<ListActivitiesResponse> {
    try {
      const params = new URLSearchParams();
      params.append('pageSize', String(options?.pageSize ?? this.paginationSettings.pageSize));
      if (options?.pageToken) params.append('pageToken', options.pageToken);
      if (options?.createTime) params.append('createTime', options.createTime);

      const res = await fetch(
        `${this.baseUrl}/sessions/${sessionId}/activities?${params.toString()}`,
        { headers: this.getHeaders() }
      );

      if (!res.ok) {
        return { activities: [], nextPageToken: undefined };
      }

      const data = await res.json();
      const activities = (data.activities || []).map(mapActivity);

      return {
        activities,
        nextPageToken: data.nextPageToken || data.next_page_token
      };
    } catch (error) {
      console.warn("listActivities network error:", error);
      return { activities: [], nextPageToken: undefined };
    }
  }

  /**
   * Fetch all activities for a session with automatic pagination
   */
  async listAllActivities(sessionId: string): Promise<JulesActivity[]> {
    const allActivities: JulesActivity[] = [];
    let pageToken: string | undefined;

    do {
      const response = await this.listActivities(sessionId, { pageSize: this.paginationSettings.pageSize, pageToken });
      allActivities.push(...response.activities);
      pageToken = this.paginationSettings.autoPaginate ? response.nextPageToken : undefined;
    } while (pageToken);

    return allActivities;
  }

  async getActivity(activityName: string): Promise<JulesActivity> {
    try {
      const url = activityName.startsWith('sessions/')
        ? `${this.baseUrl}/${activityName}`
        : activityName;

      const res = await fetch(url, { headers: this.getHeaders() });
      if (!res.ok) throw new Error("Failed to get activity");

      const data = await res.json();
      return mapActivity(data);
    } catch (error) {
      console.error("getActivity error:", error);
      throw error;
    }
  }

  // ==================== SESSION ACTIONS ====================

  async sendMessage(sessionName: string, prompt: string): Promise<boolean> {
    const res = await fetch(`${this.baseUrl}/${sessionName}:sendMessage`, {
      method: "POST",
      headers: this.getHeaders(),
      body: JSON.stringify({ prompt }),
    });

    if (!res.ok) throw new Error("Failed to send message");
    return true;
  }

  async approvePlan(sessionName: string, planId?: string): Promise<boolean> {
    const body: Record<string, any> = {};
    if (planId) {
      body.planId = planId;
    }

    const res = await fetch(`${this.baseUrl}/${sessionName}:approvePlan`, {
      method: "POST",
      headers: this.getHeaders(),
      body: JSON.stringify(body),
    });

    if (!res.ok) throw new Error("Failed to approve plan");
    return true;
  }

  async deleteSession(sessionName: string): Promise<boolean> {
    const url = sessionName.startsWith('sessions/')
      ? `${this.baseUrl}/${sessionName}`
      : `${this.baseUrl}/sessions/${sessionName}`;

    const res = await fetch(url, {
      method: "DELETE",
      headers: this.getHeaders(),
    });

    if (!res.ok) {
      const err = await res.text();
      throw new Error(`Failed to delete session: ${err}`);
    }

    return true;
  }

  async updateSession(sessionName: string, updates: Partial<JulesSession>, updateMask?: string[]): Promise<JulesSession> {
    const url = sessionName.startsWith('sessions/')
      ? `${this.baseUrl}/${sessionName}`
      : `${this.baseUrl}/sessions/${sessionName}`;

    const params = new URLSearchParams();
    if (updateMask) {
      params.append('updateMask', updateMask.join(','));
    }

    const res = await fetch(`${url}?${params.toString()}`, {
      method: "PATCH",
      headers: this.getHeaders(),
      body: JSON.stringify(updates),
    });

    if (!res.ok) {
      const err = await res.text();
      throw new Error(`Failed to update session: ${err}`);
    }

    const data = await res.json();
    return mapSession(data);
  }
}

// Helper to map API session response to our interface
const mapSession = (data: any, fallbackPrompt?: string): JulesSession => {
  return {
    ...data,
    state: data.state || 'QUEUED',
    prompt: data.prompt || fallbackPrompt || '',
    title: data.title,
    createTime: data.createTime || data.create_time,
    updateTime: data.updateTime || data.update_time,
    sourceContext: data.sourceContext || data.source_context,
    automationMode: data.automationMode || data.automation_mode,
    requirePlanApproval: data.requirePlanApproval ?? data.require_plan_approval,
    outputs: (data.outputs || []).map((o: any) => ({
      ...o,
      pullRequest: o.pullRequest || o.pull_request ? {
        url: (o.pullRequest || o.pull_request)?.url,
        title: (o.pullRequest || o.pull_request)?.title,
        description: (o.pullRequest || o.pull_request)?.description,
        branch: (o.pullRequest || o.pull_request)?.branch
      } : undefined
    }))
  };
};

// Helper to map API activity response to our interface
const mapActivity = (a: any): JulesActivity => {
  return {
    ...a,
    id: a.id,
    originator: a.originator,
    description: a.description,
    createTime: a.createTime || a.create_time,
    userMessaged: a.userMessaged || a.user_messaged,
    userMessage: a.userMessage || a.user_message,
    agentMessaged: a.agentMessaged || a.agent_messaged,
    agentMessage: a.agentMessage || a.agent_message,
    planGenerated: a.planGenerated || a.plan_generated,
    planApproved: a.planApproved || a.plan_approved ? {
      planId: (a.planApproved || a.plan_approved)?.planId ||
        (a.planApproved || a.plan_approved)?.plan_id
    } : undefined,
    progressUpdated: a.progressUpdated || a.progress_updated,
    sessionCompleted: a.sessionCompleted || a.session_completed,
    sessionFailed: a.sessionFailed || a.session_failed ? {
      reason: (a.sessionFailed || a.session_failed)?.reason
    } : undefined,
    artifacts: a.artifacts
  };
};
