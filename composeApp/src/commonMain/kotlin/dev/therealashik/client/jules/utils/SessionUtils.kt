package dev.therealashik.client.jules.utils

import dev.therealashik.client.jules.model.SessionState

data class SessionDisplayInfo(
    val label: String,
    val emoji: String,
    val helperText: String,
    val cta: String,
    val shimmer: Boolean
)

fun getSessionDisplayInfo(state: SessionState): SessionDisplayInfo {
    return when (state) {
        SessionState.IN_PROGRESS -> SessionDisplayInfo(
            label = "In Progress",
            emoji = "🚧",
            helperText = "Generating solution — hang tight.",
            cta = "none",
            shimmer = true
        )
        SessionState.PLANNING -> SessionDisplayInfo(
            label = "Planning",
            emoji = "🧠",
            helperText = "Analyzing requirements...",
            cta = "none",
            shimmer = true
        )
        SessionState.QUEUED -> SessionDisplayInfo(
            label = "Queued",
            emoji = "⏳",
            helperText = "Waiting for agent availability.",
            cta = "none",
            shimmer = true
        )
        SessionState.AWAITING_PLAN_APPROVAL -> SessionDisplayInfo(
            label = "Plan Ready",
            emoji = "📋",
            helperText = "Review the proposed plan.",
            cta = "Approve",
            shimmer = false
        )
        SessionState.AWAITING_USER_FEEDBACK -> SessionDisplayInfo(
            label = "Feedback Needed",
            emoji = "🗣️",
            helperText = "Please provide your input.",
            cta = "Respond",
            shimmer = false
        )
        SessionState.PAUSED -> SessionDisplayInfo(
            label = "Paused",
            emoji = "⏸️",
            helperText = "Session is currently paused.",
            cta = "Resume",
            shimmer = false
        )
        SessionState.COMPLETED -> SessionDisplayInfo(
            label = "Completed",
            emoji = "✅",
            helperText = "Task finished successfully.",
            cta = "none",
            shimmer = false
        )
        SessionState.FAILED -> SessionDisplayInfo(
            label = "Failed",
            emoji = "❌",
            helperText = "Something went wrong.",
            cta = "Retry",
            shimmer = false
        )
    }
}
