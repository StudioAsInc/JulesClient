package dev.therealashik.client.jules.utils

import dev.therealashik.jules.sdk.model.SessionState
import kotlin.test.Test
import kotlin.test.assertEquals

class SessionUtilsTest {
    @Test
    fun testGetSessionDisplayInfo() {
        // IN_PROGRESS
        val inProgress = getSessionDisplayInfo(SessionState.IN_PROGRESS)
        assertEquals("In Progress", inProgress.label)
        assertEquals("🚧", inProgress.emoji)
        assertEquals("Generating solution — hang tight.", inProgress.helperText)
        assertEquals("none", inProgress.cta)
        assertEquals(true, inProgress.shimmer)

        // PLANNING
        val planning = getSessionDisplayInfo(SessionState.PLANNING)
        assertEquals("Planning", planning.label)
        assertEquals("🧠", planning.emoji)
        assertEquals("Analyzing requirements...", planning.helperText)
        assertEquals("none", planning.cta)
        assertEquals(true, planning.shimmer)

        // QUEUED
        val queued = getSessionDisplayInfo(SessionState.QUEUED)
        assertEquals("Queued", queued.label)
        assertEquals("⏳", queued.emoji)
        assertEquals("Waiting for agent availability.", queued.helperText)
        assertEquals("none", queued.cta)
        assertEquals(true, queued.shimmer)

        // AWAITING_PLAN_APPROVAL
        val planApproval = getSessionDisplayInfo(SessionState.AWAITING_PLAN_APPROVAL)
        assertEquals("Plan Ready", planApproval.label)
        assertEquals("📋", planApproval.emoji)
        assertEquals("Review the proposed plan.", planApproval.helperText)
        assertEquals("Approve", planApproval.cta)
        assertEquals(false, planApproval.shimmer)

        // AWAITING_USER_FEEDBACK
        val userFeedback = getSessionDisplayInfo(SessionState.AWAITING_USER_FEEDBACK)
        assertEquals("Feedback Needed", userFeedback.label)
        assertEquals("🗣️", userFeedback.emoji)
        assertEquals("Please provide your input.", userFeedback.helperText)
        assertEquals("Respond", userFeedback.cta)
        assertEquals(false, userFeedback.shimmer)

        // PAUSED
        val paused = getSessionDisplayInfo(SessionState.PAUSED)
        assertEquals("Paused", paused.label)
        assertEquals("⏸️", paused.emoji)
        assertEquals("Session is currently paused.", paused.helperText)
        assertEquals("Resume", paused.cta)
        assertEquals(false, paused.shimmer)

        // COMPLETED
        val completed = getSessionDisplayInfo(SessionState.COMPLETED)
        assertEquals("Completed", completed.label)
        assertEquals("✅", completed.emoji)
        assertEquals("Task finished successfully.", completed.helperText)
        assertEquals("none", completed.cta)
        assertEquals(false, completed.shimmer)

        // FAILED
        val failed = getSessionDisplayInfo(SessionState.FAILED)
        assertEquals("Failed", failed.label)
        assertEquals("❌", failed.emoji)
        assertEquals("Something went wrong.", failed.helperText)
        assertEquals("Retry", failed.cta)
        assertEquals(false, failed.shimmer)
    }
}
