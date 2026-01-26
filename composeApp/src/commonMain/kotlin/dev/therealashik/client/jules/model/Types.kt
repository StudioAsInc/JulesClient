package dev.therealashik.client.jules.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

// ==================== CORE ENUMS ====================

@Serializable
enum class AutomationMode {
    @SerialName("AUTO_CREATE_PR") AUTO_CREATE_PR,
    @SerialName("NONE") NONE,
    @SerialName("AUTO_MERGE") AUTO_MERGE
}

@Serializable
enum class SessionState {
    @SerialName("QUEUED") QUEUED,
    @SerialName("PLANNING") PLANNING,
    @SerialName("AWAITING_PLAN_APPROVAL") AWAITING_PLAN_APPROVAL,
    @SerialName("AWAITING_USER_FEEDBACK") AWAITING_USER_FEEDBACK,
    @SerialName("IN_PROGRESS") IN_PROGRESS,
    @SerialName("PAUSED") PAUSED,
    @SerialName("COMPLETED") COMPLETED,
    @SerialName("FAILED") FAILED
}

// ==================== SOURCES ====================

@Serializable
data class GitHubRepoInfo(
    val owner: String,
    val repo: String,
    val isPrivate: Boolean? = false,
    val defaultBranch: BranchInfo? = null,
    val branches: List<BranchInfo> = emptyList()
)

@Serializable
data class BranchInfo(val displayName: String)

@Serializable
data class JulesSource(
    val name: String, // "sources/github/owner/repo"
    val id: String? = null,
    val displayName: String? = null,
    val githubRepo: GitHubRepoInfo? = null
)

// ==================== SESSIONS ====================

@Serializable
data class JulesSession(
    val name: String, // "sessions/{id}"
    val id: String? = null,
    val title: String? = null,
    val prompt: String,
    val state: SessionState = SessionState.QUEUED,
    val priority: Int? = null,
    val createTime: String, // ISO String
    val updateTime: String? = null,
    val sourceContext: SourceContext? = null,
    val automationMode: AutomationMode? = null,
    val requirePlanApproval: Boolean? = true,
    val outputs: List<SessionOutput> = emptyList()
)

@Serializable
data class SourceContext(
    val source: String,
    val githubRepoContext: GitHubRepoContext? = null
)

@Serializable
data class GitHubRepoContext(val startingBranch: String? = null)

@Serializable
data class SessionOutput(
    val pullRequest: PullRequestOutput? = null
)

@Serializable
data class PullRequestOutput(
    val url: String,
    val title: String,
    val description: String,
    val branch: String? = null
)

// ==================== ACTIVITIES & ARTIFACTS ====================

@Serializable
data class JulesActivity(
    val name: String,
    val id: String? = null,
    val originator: String? = null, // "system", "agent", "user"
    val description: String? = null,
    val createTime: String,

    // Activity Payloads (OneOf)
    val userMessaged: MessageContent? = null,
    val userMessage: MessageContent? = null, // fallback
    val agentMessaged: MessageContent? = null,
    val agentMessage: MessageContent? = null, // fallback
    val planGenerated: PlanGenerated? = null,
    val planApproved: PlanApproved? = null,
    val progressUpdated: ProgressUpdate? = null,
    val sessionCompleted: SessionCompleted? = null,
    val sessionFailed: SessionFailed? = null,

    val artifacts: List<ActivityArtifact> = emptyList()
)

@Serializable
data class MessageContent(
    val text: String? = null,
    val parts: List<MessagePart>? = null
)

@Serializable
data class MessagePart(val text: String)

@Serializable
data class PlanGenerated(val plan: Plan)

@Serializable
data class PlanApproved(val planId: String? = null)

@Serializable
data class Plan(
    val id: String? = null,
    val steps: List<Step> = emptyList(),
    val createTime: String? = null
)

@Serializable
data class Step(
    val id: String? = null,
    val index: Int? = null,
    val title: String,
    val description: String? = null
)

@Serializable
data class ProgressUpdate(
    val status: String? = null,
    val title: String? = null,
    val description: String? = null
)

@Serializable
data class SessionCompleted(val dummy: Boolean? = null) // Empty object in JSON usually

@Serializable
data class SessionFailed(val reason: String? = null)

// Artifacts

@Serializable
data class ActivityArtifact(
    val media: MediaArtifact? = null,
    val bashOutput: BashOutputArtifact? = null,
    val changeSet: ChangeSetArtifact? = null
)

@Serializable
data class MediaArtifact(
    val mimeType: String,
    val data: String // Base64
)

@Serializable
data class BashOutputArtifact(
    val command: String,
    val output: String,
    val exitCode: Int
)

@Serializable
data class ChangeSetArtifact(
    val source: String? = null,
    val gitPatch: GitPatch? = null
)

@Serializable
data class GitPatch(
    val baseCommitId: String? = null,
    val unidiffPatch: String? = null,
    val suggestedCommitMessage: String? = null
)

// ==================== API RESPONSES ====================

@Serializable
data class ListSourcesResponse(
    val sources: List<JulesSource> = emptyList(),
    val nextPageToken: String? = null
)

@Serializable
data class ListSessionsResponse(
    val sessions: List<JulesSession> = emptyList(),
    val nextPageToken: String? = null
)

@Serializable
data class ListActivitiesResponse(
    val activities: List<JulesActivity> = emptyList(),
    val nextPageToken: String? = null
)

@Serializable
data class CreateSessionRequest(
    val prompt: String,
    val sourceContext: SourceContext,
    val title: String? = null,
    val requirePlanApproval: Boolean = true,
    val automationMode: AutomationMode = AutomationMode.AUTO_CREATE_PR
)

@Serializable
data class SendMessageRequest(
    val prompt: String
)

@Serializable
data class ApprovePlanRequest(
    val planId: String? = null
)
