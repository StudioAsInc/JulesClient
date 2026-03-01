package dev.therealashik.client.jules.ui.screens

import dev.therealashik.client.jules.ui.components.DiffView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.mikepenz.markdown.compose.Markdown
import com.mikepenz.markdown.model.DefaultMarkdownColors
import com.mikepenz.markdown.model.DefaultMarkdownTypography
import dev.therealashik.jules.sdk.model.*
import dev.therealashik.client.jules.model.*
import dev.therealashik.client.jules.ui.JulesBackground
import dev.therealashik.client.jules.ui.JulesOpacity
import dev.therealashik.client.jules.ui.JulesShapes
import dev.therealashik.client.jules.ui.JulesSizes
import dev.therealashik.client.jules.ui.JulesSpacing
import dev.therealashik.client.jules.ui.JulesSurface
import dev.therealashik.client.jules.ui.components.InputArea
import dev.therealashik.client.jules.ui.components.InputAreaVariant
import kotlin.io.encoding.ExperimentalEncodingApi

@Composable
fun SessionView(
    session: JulesSession,
    activities: List<JulesActivity>,
    isProcessing: Boolean,
    error: String? = null,
    defaultCardState: Boolean,
    onSendMessage: (String) -> Unit,
    onApprovePlan: (String?) -> Unit,
    onNavigateHome: () -> Unit
) {
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Auto-scroll to bottom
    LaunchedEffect(activities.size, session.outputs.size, error) {
        if (activities.isNotEmpty() || session.outputs.isNotEmpty() || error != null) {
            // Wait a frame for layout
            kotlinx.coroutines.delay(100)
            val count = activities.size + session.outputs.filter { it.pullRequest != null }.size
            if (count > 0 || error != null) {
                 listState.animateScrollToItem(listState.layoutInfo.totalItemsCount - 1)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceContainer,
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = JulesSpacing.l, vertical = JulesSpacing.m),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Chat",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp,
                        modifier = Modifier.clickable { onNavigateHome() }
                    )
                    Text(" / ", color = MaterialTheme.colorScheme.outline, fontSize = 14.sp)
                    Text(
                        session.title ?: session.name.removePrefix("sessions/"),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Chat History
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = JulesSpacing.l),
                contentPadding = PaddingValues(bottom = 160.dp, top = JulesSpacing.l)
            ) {
                items(activities) { activity ->
                    ActivityItem(activity, defaultCardState, onApprovePlan)
                }

                // Session Outputs (PR Cards)
                session.outputs.forEach { output ->
                    val pr = output.pullRequest
                    if (pr != null) {
                        item {
                            Spacer(modifier = Modifier.height(JulesSpacing.l))
                            PullRequestCard(pr)
                        }
                    }
                }

                // Shimmer Loading State - Appended so previous content isn't hidden
                if (isProcessing) {
                    item {
                        ShimmerMessage()
                    }
                }

                // Plan Approval Card (Pinned to bottom of list if waiting)
                if (session.state == SessionState.AWAITING_PLAN_APPROVAL) {
                    item {
                        PlanApprovalCard(onApprove = { onApprovePlan(null) })
                    }
                }

                // Completion / Failure Status (if no specific activity rendered it)
                if (session.state == SessionState.COMPLETED) {
                     item {
                         Spacer(modifier = Modifier.height(JulesSpacing.xxl))
                         StatusBanner(true, "Session Completed Successfully")
                     }
                } else if (session.state == SessionState.FAILED) {
                     item {
                         Spacer(modifier = Modifier.height(JulesSpacing.xxl))
                         StatusBanner(false, "Session Failed")
                     }
                }

                // Error Display
                if (!error.isNullOrBlank()) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = JulesSpacing.l)
                                .background(Color(0xFFEF4444).copy(alpha = JulesOpacity.normal), JulesShapes.medium)
                                .border(1.dp, Color(0xFFEF4444).copy(alpha = JulesOpacity.focused), JulesShapes.medium)
                                .padding(JulesSpacing.m),
                            horizontalArrangement = Arrangement.spacedBy(JulesSpacing.s),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Warning, null, tint = Color(0xFFEF4444), modifier = Modifier.size(JulesSizes.iconSmall))
                            Text(error, color = Color(0xFFEF4444), fontSize = 14.sp)
                        }
                    }
                }
            }
        }

        // Input Area (Floating & Gradient)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            // Gradient Overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color(0xFF0C0C0C).copy(alpha = 0.8f),
                                Color(0xFF0C0C0C)
                            )
                        )
                    )
            )

            // Input Pill
            InputArea(
                variant = InputAreaVariant.CHAT,
                onSendMessage = { text, _ -> onSendMessage(text) },
                onSendMessageMinimal = onSendMessage,
                isLoading = isProcessing,
                currentSource = null,
                sources = emptyList(),
                onSourceChange = {},
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = JulesSpacing.l)
                    .padding(bottom = JulesSpacing.l)
            )
        }
    }
}

private fun getTextContent(content: MessageContent?): String? {
    if (content == null) return null
    val text = content.text
    if (!text.isNullOrBlank()) return text
    val parts = content.parts
    if (parts != null && parts.isNotEmpty()) {
        return parts.joinToString("") { it.text }
    }
    return null
}

@Composable
fun ActivityItem(activity: JulesActivity, defaultCardState: Boolean, onApprovePlan: (String?) -> Unit) {
    val isUser = activity.userMessaged != null || activity.userMessage != null
    val isPlan = activity.planGenerated != null
    val isProgress = activity.progressUpdated != null
    val isCompleted = activity.sessionCompleted != null
    val isFailed = activity.sessionFailed != null

    // Determine content text
    val text = when {
        isUser -> getTextContent(activity.userMessaged) ?: getTextContent(activity.userMessage)
        activity.agentMessaged != null -> getTextContent(activity.agentMessaged)
        activity.agentMessage != null -> getTextContent(activity.agentMessage)
        isPlan -> null // Rendered separately
        else -> activity.description // System message or fallback
    }

    // Relaxed check: Render if there is text, OR it's a special type, OR it has artifacts.
    // Even if text is null, we might want to show "System Event" or artifacts.
    val hasArtifacts = activity.artifacts.isNotEmpty()
    val shouldRender = text != null || isPlan || isProgress || hasArtifacts || isCompleted || isFailed || activity.originator == "system"

    if (!shouldRender) {
        return
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = JulesSpacing.m) // Increased vertical padding from 4.dp to 12.dp
    ) {
        // System Message
        if (activity.originator == "system" && !isPlan && !isProgress && !isCompleted && !isFailed) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = JulesSpacing.s),
                contentAlignment = Alignment.Center
            ) {
                 Text(
                    text = text ?: "System Event",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF71717A), // Zinc-500
                    modifier = Modifier
                        .background(Color.White.copy(alpha = JulesOpacity.subtle), JulesShapes.circle)
                        .border(1.dp, Color.White.copy(alpha = JulesOpacity.subtle), JulesShapes.circle)
                        .padding(horizontal = JulesSpacing.m, vertical = JulesSpacing.xs)
                )
            }
            return
        }

        // User/Agent Message Header & Bubble
        if (text != null) {
            var isTimestampVisible by remember { mutableStateOf(false) }

            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !isUser) { isTimestampVisible = !isTimestampVisible },
                horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
            ) {
                if (!isUser) {
                     Box(
                        modifier = Modifier
                            .size(JulesSizes.avatar)
                            .background(Color(0xFF18181B), JulesShapes.circle)
                            .border(1.dp, Color.White.copy(alpha = JulesOpacity.normal), JulesShapes.circle),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.SmartToy,
                            contentDescription = "Jules",
                            tint = Color(0xFF818CF8), // Indigo-400
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(JulesSpacing.l))
                }

                val bubbleShape = JulesShapes.large

                Column(
                    horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .then(if (isUser) Modifier.fillMaxWidth(0.85f) else Modifier)
                ) {
                    // Bubble
                    Box(
                        modifier = Modifier
                            .clip(bubbleShape)
                            .background(if (isUser) Color(0xFF27272A) else Color.Transparent)
                            .border(
                                width = if (isUser) 1.dp else 0.dp,
                                color = if (isUser) Color.White.copy(alpha = JulesOpacity.subtle) else Color.Transparent,
                                shape = bubbleShape
                            )
                            .padding(if (isUser) JulesSpacing.l else 0.dp)
                            .animateContentSize()
                    ) {
                        // Expansion Logic for User Messages
                        if (isUser) {
                            val isLong = text.length > 500 || text.count { it == '\n' } > 8
                            var isExpanded by remember { mutableStateOf(false) }

                            Column {
                                Box(
                                    modifier = Modifier.heightIn(max = if (isLong && !isExpanded) 200.dp else 5000.dp)
                                ) {
                                    SelectionContainer {
                                        Markdown(
                                            content = text,
                                            colors = DefaultMarkdownColors(
                                                 text = Color.White.copy(alpha = 0.9f),
                                                 codeText = Color(0xFFCE9178),
                                                 codeBackground = Color(0xFF1E1E1E),
                                                 inlineCodeText = Color(0xFFCE9178),
                                                 inlineCodeBackground = Color(0xFF1E1E1E),
                                                 dividerColor = Color.Gray,
                                                 linkText = Color(0xFF818CF8)
                                            ),
                                            typography = DefaultMarkdownTypography(
                                                 h1 = MaterialTheme.typography.headlineLarge,
                                                 h2 = MaterialTheme.typography.headlineMedium,
                                                 h3 = MaterialTheme.typography.headlineSmall,
                                                 h4 = MaterialTheme.typography.titleLarge,
                                                 h5 = MaterialTheme.typography.titleMedium,
                                                 h6 = MaterialTheme.typography.titleSmall,
                                                 text = MaterialTheme.typography.bodyMedium.copy(lineHeight = 24.sp),
                                                 code = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                                                 inlineCode = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                                                 quote = MaterialTheme.typography.bodyMedium,
                                                 paragraph = MaterialTheme.typography.bodyMedium,
                                                 ordered = MaterialTheme.typography.bodyMedium,
                                                 bullet = MaterialTheme.typography.bodyMedium,
                                                 list = MaterialTheme.typography.bodyMedium,
                                                 link = MaterialTheme.typography.bodyMedium
                                            )
                                        )
                                    }

                                    // Gradient Overlay
                                    if (isLong && !isExpanded) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.BottomCenter)
                                                .fillMaxWidth()
                                                .height(JulesSizes.touchTarget)
                                                .background(
                                                    Brush.verticalGradient(
                                                        listOf(Color.Transparent, Color(0xFF27272A))
                                                    )
                                                )
                                        )
                                    }
                                }

                                if (isLong) {
                                    TextButton(
                                        onClick = { isExpanded = !isExpanded },
                                        modifier = Modifier.align(Alignment.CenterHorizontally).height(32.dp),
                                        contentPadding = PaddingValues(JulesSpacing.xs)
                                    ) {
                                        Text(
                                            if (isExpanded) "Show less" else "Show more",
                                            fontSize = 12.sp,
                                            color = Color(0xFFA1A1AA)
                                        )
                                        Spacer(Modifier.width(JulesSpacing.xs))
                                        Icon(
                                            if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                            null,
                                            tint = Color(0xFFA1A1AA),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        } else {
                            // Agent Message (Standard Render)
                            SelectionContainer {
                                Markdown(
                                    content = text,
                                    colors = DefaultMarkdownColors(
                                         text = Color(0xFFE4E4E7), // Zinc-200
                                         codeText = Color(0xFFCE9178),
                                         codeBackground = Color(0xFF1E1E1E),
                                         inlineCodeText = Color(0xFFCE9178),
                                         inlineCodeBackground = Color(0xFF1E1E1E),
                                         dividerColor = Color.Gray,
                                         linkText = Color(0xFF818CF8)
                                    ),
                                    typography = DefaultMarkdownTypography(
                                         h1 = MaterialTheme.typography.headlineLarge,
                                         h2 = MaterialTheme.typography.headlineMedium,
                                         h3 = MaterialTheme.typography.headlineSmall,
                                         h4 = MaterialTheme.typography.titleLarge,
                                         h5 = MaterialTheme.typography.titleMedium,
                                         h6 = MaterialTheme.typography.titleSmall,
                                         text = MaterialTheme.typography.bodyMedium.copy(
                                             lineHeight = 24.sp,
                                             fontWeight = FontWeight.Light
                                         ),
                                         code = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                                         inlineCode = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                                         quote = MaterialTheme.typography.bodyMedium,
                                         paragraph = MaterialTheme.typography.bodyMedium,
                                         ordered = MaterialTheme.typography.bodyMedium,
                                         bullet = MaterialTheme.typography.bodyMedium,
                                         list = MaterialTheme.typography.bodyMedium,
                                         link = MaterialTheme.typography.bodyMedium
                                    )
                                )
                            }
                        }
                    }

                    // Timestamp (Agent only)
                    AnimatedVisibility(visible = isTimestampVisible && !isUser) {
                        Text(
                            "Jules • ${activity.createTime.take(16).replace("T", " ")}", // Simple formatting
                            fontSize = 10.sp,
                            color = Color(0xFF52525B),
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(top = JulesSpacing.xs)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(JulesSpacing.s))
        }

        // Indented Content (Plans, Artifacts, Progress)
        // If it was a user message, we don't usually have these attached, but if we do, show them.
        // For agent, we indent.
        Column(modifier = Modifier.padding(start = 40.dp)) {

            // Progress Updates
            val progressUpdate = activity.progressUpdated
            if (isProgress && progressUpdate != null) {
                ProgressItem(progressUpdate)
                Spacer(modifier = Modifier.height(JulesSpacing.s))
            }

            // Plan
            val planGenerated = activity.planGenerated
            if (isPlan && planGenerated != null) {
                val plan = planGenerated.plan
                // val isApproved = activity.planApproved != null // Simplification
                PlanCard(plan, defaultCardState, onApprove = { onApprovePlan(activity.name) })
                Spacer(modifier = Modifier.height(JulesSpacing.s))
            }

            // Artifacts
            activity.artifacts.forEach { artifact ->
                ArtifactView(artifact, defaultCardState)
                Spacer(modifier = Modifier.height(JulesSpacing.s))
            }

            // Completion / Failure
            if (isCompleted) {
                StatusBanner(true, "Session Completed Successfully")
            }
            if (isFailed) {
                StatusBanner(false, "Session Failed: ${activity.sessionFailed?.reason ?: "Unknown"}")
            }
        }
    }
}

@Composable
fun ProgressItem(progress: ProgressUpdate) {
    val title = progress.title ?: progress.status ?: "Processing"
    val description = progress.description

    val icon = when {
        title.contains("read", true) || title.contains("analyz", true) -> Icons.Default.Description
        title.contains("run", true) || title.contains("exec", true) -> Icons.Default.Code
        else -> Icons.Default.CheckCircle
    }

    Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(vertical = JulesSpacing.xs)) {
        Box(
            modifier = Modifier
                .size(JulesSizes.avatar)
                .background(Color(0xFF18181B), JulesShapes.circle)
                .border(1.dp, Color.White.copy(alpha = JulesOpacity.normal), JulesShapes.circle),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.SmartToy, null, tint = Color(0xFF818CF8).copy(0.7f), modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(JulesSpacing.l))
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = Color(0xFFA1A1AA), modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(JulesSpacing.s))
                Text(title, color = Color(0xFFE4E4E7), fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
            if (!description.isNullOrBlank() && description != title) {
                Text(description, color = Color(0xFFA1A1AA), fontSize = 13.sp, lineHeight = 18.sp, modifier = Modifier.padding(start = 22.dp))
            }
        }
    }
}

@Composable
fun StatusBanner(success: Boolean, message: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(
                if (success) Color(0xFF064E3B) else Color(0xFF7F1D1D),
                JulesShapes.small
            )
            .padding(horizontal = JulesSpacing.m, vertical = JulesSpacing.s)
    ) {
        Text(if (success) "✓" else "⚠", color = Color.White, fontSize = 16.sp)
        Spacer(modifier = Modifier.width(JulesSpacing.s))
        Text(message, color = Color.White, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun PlanCard(plan: Plan, defaultExpanded: Boolean, onApprove: () -> Unit) {
    var isExpanded by remember { mutableStateOf(defaultExpanded) }

    Card(
        modifier = Modifier.fillMaxWidth().border(1.dp, Color.White.copy(alpha = JulesOpacity.normal), JulesShapes.medium),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF121215))
    ) {
        Column {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(horizontal = JulesSpacing.xl, vertical = JulesSpacing.m),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.List, null, tint = Color(0xFF818CF8), modifier = Modifier.size(JulesSizes.iconSmall))
                Spacer(modifier = Modifier.width(JulesSpacing.s))
                Text("Execution Plan", style = MaterialTheme.typography.titleMedium, fontSize = 14.sp, color = Color.White)

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    "${plan.steps.size} steps",
                    fontSize = 12.sp,
                    color = Color(0xFF71717A),
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier
                        .background(Color.White.copy(alpha = JulesOpacity.subtle), JulesShapes.small) // Was 4.dp
                        .border(1.dp, Color.White.copy(alpha = JulesOpacity.subtle), JulesShapes.small) // Was 4.dp
                        .padding(horizontal = JulesSpacing.s, vertical = 2.dp)
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column {
                    Column(Modifier.padding(JulesSpacing.s)) {
                        plan.steps.forEachIndexed { index, step ->
                            PlanStepItem(step, index)
                        }
                    }

                    // Footer
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black.copy(0.2f))
                            .padding(JulesSpacing.l)
                    ) {
                        Button(
                            onClick = onApprove,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Start Coding")
                            Spacer(modifier = Modifier.width(JulesSpacing.s))
                            Icon(Icons.Default.ChevronRight, null, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlanStepItem(step: Step, index: Int) {
    var isExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(JulesShapes.medium)
            .clickable { isExpanded = !isExpanded }
            .padding(JulesSpacing.m)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(Color(0xFF18181B), JulesShapes.circle)
                    .border(1.dp, Color.White.copy(alpha = JulesOpacity.normal), JulesShapes.circle),
                contentAlignment = Alignment.Center
            ) {
                Text("${index + 1}", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF71717A))
            }
            Spacer(Modifier.width(JulesSpacing.m))
            Text(step.title, modifier = Modifier.weight(1f), fontSize = 14.sp, color = Color(0xFFE4E4E7), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Icon(
                Icons.Default.ExpandMore,
                null,
                modifier = Modifier.size(14.dp).rotate(if (isExpanded) 180f else 0f),
                tint = Color(0xFF71717A)
            )
        }

        AnimatedVisibility(visible = isExpanded) {
            Text(
                step.description ?: "",
                modifier = Modifier.padding(start = 36.dp, top = JulesSpacing.s),
                fontSize = 14.sp,
                color = Color(0xFF9CA3AF),
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
fun PlanApprovalCard(onApprove: () -> Unit) {
    // Similar to PlanCard but as a standalone sticky prompt
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = JulesSpacing.l)
            .border(1.dp, MaterialTheme.colorScheme.primary, JulesShapes.medium),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(JulesSpacing.l),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Plan Proposed",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(JulesSpacing.s))
            Text(
                "The agent has proposed a plan. Please review the steps above and approve to proceed.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(JulesSpacing.l))
            Button(
                onClick = onApprove,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Approve Plan")
            }
        }
    }
}

@OptIn(ExperimentalEncodingApi::class)
@Composable
fun ArtifactView(artifact: ActivityArtifact, defaultExpanded: Boolean) {
    val bashOutput = artifact.bashOutput
    val changeSet = artifact.changeSet
    val media = artifact.media

    if (bashOutput != null) {
        CodeBlock(
            title = bashOutput.command,
            content = bashOutput.output,
            language = "bash",
            exitCode = bashOutput.exitCode,
            defaultExpanded = defaultExpanded
        )
    } else if (changeSet != null) {
        val patch = changeSet.gitPatch?.unidiffPatch ?: ""

        // Extract filename
        val match = Regex("^\\+\\+\\+\\s+(?:b/)?(.+)$", RegexOption.MULTILINE).find(patch)
        val fileName = match?.groupValues?.get(1)?.trim()?.let { fullPath ->
           val parts = fullPath.split('/')
           if (parts.size > 2) ".../${parts.takeLast(2).joinToString("/")}" else fullPath
        }

        CodeBlock(
            title = changeSet.gitPatch?.suggestedCommitMessage ?: "Code Changes",
            subtitle = fileName,
            content = patch,
            language = "diff",
            defaultExpanded = defaultExpanded
        )
    } else if (media != null) {
        val base64Data = media.data
        val mime = media.mimeType
        val model = "data:$mime;base64,$base64Data"

        Card(
            shape = JulesShapes.small,
            modifier = Modifier.fillMaxWidth().border(1.dp, Color.White.copy(alpha = JulesOpacity.normal), JulesShapes.small)
        ) {
            Column {
                Box(modifier = Modifier.fillMaxWidth().background(Color(0xFF1E1E1E)).padding(JulesSpacing.s)) {
                    Text("Generated Artifact", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                }
                AsyncImage(
                    model = model,
                    contentDescription = "Generated Artifact",
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = ContentScale.FillWidth
                )
            }
        }
    }
}

@Composable
fun CodeBlock(title: String, subtitle: String? = null, content: String, language: String, exitCode: Int? = null, defaultExpanded: Boolean = false) {
    var isExpanded by remember { mutableStateOf(defaultExpanded) }
    val isError = exitCode != null && exitCode != 0
    val borderColor = if (isError) Color(0xFFEF4444).copy(alpha = 0.3f) else Color.White.copy(alpha = JulesOpacity.normal)
    val headerBg = if (isError) Color(0xFFEF4444).copy(alpha = JulesOpacity.subtle) else Color.White.copy(alpha = 0.02f)

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF09090B)),
        shape = JulesShapes.medium,
        modifier = Modifier.fillMaxWidth().border(1.dp, borderColor, JulesShapes.medium)
    ) {
        Column {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(headerBg)
                    .clickable { isExpanded = !isExpanded }
                    .padding(JulesSpacing.m),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        if (isError) Icons.Default.Warning else Icons.Default.Code,
                        null,
                        tint = if (isError) Color(0xFFF87171) else Color(0xFF71717A),
                        modifier = Modifier.size(JulesSizes.iconSmall)
                    )
                    Spacer(modifier = Modifier.width(JulesSpacing.m))
                    Column(modifier = Modifier.weight(1f)) {
                         Text(
                             title,
                             color = if (isError) Color(0xFFF87171) else Color(0xFFD4D4D8),
                             style = MaterialTheme.typography.bodyMedium,
                             fontFamily = FontFamily.Monospace,
                             maxLines = 1,
                             overflow = TextOverflow.Ellipsis
                         )
                         if (subtitle != null) {
                             Text(
                                 subtitle,
                                 color = Color(0xFF71717A),
                                 fontSize = 10.sp,
                                 fontFamily = FontFamily.Monospace
                             )
                         }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (exitCode != null && isError) {
                        Text(
                            "Failed",
                            color = Color(0xFFF87171),
                            fontSize = 10.sp,
                            modifier = Modifier
                                .background(Color(0xFFEF4444).copy(0.1f), RoundedCornerShape(4.dp))
                                .border(1.dp, Color(0xFFEF4444).copy(0.2f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                        Spacer(Modifier.width(JulesSpacing.s))
                    }

                    Icon(
                        if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        null,
                        tint = Color(0xFF71717A),
                        modifier = Modifier.size(JulesSizes.iconSmall)
                    )
                }
            }

            // Content
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (isError) Color(0xFFEF4444).copy(0.02f) else Color.Black.copy(0.2f))
                        .border(
                             width = 1.dp,
                             color = if (isError) Color(0xFFEF4444).copy(0.1f) else Color.White.copy(JulesOpacity.subtle)
                        )
                ) {
                    if (language == "diff") {
                        DiffView(content)
                    } else {
                        SelectionContainer {
                            Text(
                                text = content,
                                color = if (isError) Color(0xFFFCA5A5) else Color(0xFFA1A1AA),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(JulesSpacing.m)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PullRequestCard(pr: PullRequestOutput) {
    val uriHandler = LocalUriHandler.current
    val clipboardManager = LocalClipboardManager.current

    // Pulse Animation
    val infiniteTransition = rememberInfiniteTransition()
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        )
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.White.copy(alpha = JulesOpacity.normal), JulesShapes.medium),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .background(Brush.verticalGradient(listOf(Color(0xFF18181B), Color(0xFF0F0F12))))
                .padding(JulesSpacing.l)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.background(Color(0xFF064E3B).copy(alpha=0.2f), RoundedCornerShape(6.dp)).padding(6.dp)) {
                         // Fallback icon for GitPullRequest since standard Icons might not have it
                         Icon(Icons.Default.Check, null, tint = Color(0xFF4ADE80), modifier = Modifier.size(14.dp))
                    }
                    Spacer(modifier = Modifier.width(JulesSpacing.m))
                    Column(Modifier.weight(1f)) {
                        Text("Pull Request Ready", style = MaterialTheme.typography.labelSmall, color = Color(0xFFE4E4E7))
                        Text("Click to review", style = MaterialTheme.typography.bodySmall, fontSize = 10.sp, color = Color(0xFF71717A))
                    }

                    // Pulse Dot
                    Box(modifier = Modifier.size(8.dp)) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .scale(scale)
                                .background(Color(0xFF22C55E).copy(alpha = 0.5f), CircleShape)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFF22C55E), CircleShape)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(JulesSpacing.l))

                Text(pr.title, style = MaterialTheme.typography.titleMedium, fontSize = 16.sp, color = Color.White)
                if (pr.description.isNotEmpty()) {
                    Text(
                        pr.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFA1A1AA),
                        maxLines = 2,
                        modifier = Modifier.padding(top = 4.dp),
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(JulesSpacing.xl))

                Row(modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = { uriHandler.openUri(pr.url) },
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = JulesShapes.small,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.OpenInNew, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(JulesSpacing.s))
                        Text("View PR")
                    }

                    Spacer(Modifier.width(JulesSpacing.s))

                    IconButton(
                        onClick = { clipboardManager.setText(AnnotatedString(pr.url)) },
                        modifier = Modifier
                            .size(44.dp)
                            .background(Color.White.copy(alpha = JulesOpacity.subtle), JulesShapes.small)
                            .border(1.dp, Color.White.copy(alpha = JulesOpacity.subtle), JulesShapes.small)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy URL", tint = Color(0xFFA1A1AA))
                    }
                }

                Text(
                    pr.url,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 10.sp,
                    color = Color(0xFF52525B),
                    modifier = Modifier.padding(top=JulesSpacing.m).fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
