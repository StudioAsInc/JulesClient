package dev.therealashik.client.jules.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.therealashik.jules.sdk.model.JulesSession
import dev.therealashik.jules.sdk.model.SessionState
import dev.therealashik.client.jules.ui.JulesOpacity
import dev.therealashik.client.jules.ui.JulesShapes
import dev.therealashik.client.jules.ui.JulesSizes
import dev.therealashik.client.jules.ui.JulesSpacing

@Composable
fun ProactiveSection(
    sessions: List<JulesSession>,
    onSelectSession: ((JulesSession) -> Unit)? = null
) {
    var isEnabled by remember { mutableStateOf(false) }
    var activeTab by remember { mutableStateOf("overview") }

    val runningSession = sessions.find {
        it.state in listOf(
            SessionState.QUEUED,
            SessionState.PLANNING,
            SessionState.AWAITING_PLAN_APPROVAL,
            SessionState.AWAITING_USER_FEEDBACK,
            SessionState.IN_PROGRESS,
            SessionState.PAUSED
        )
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(JulesSpacing.m)
    ) {
        // Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(JulesSpacing.s)
        ) {
            TabButton("Repo overview", activeTab == "overview") { activeTab = "overview" }
            TabButton("Suggested", activeTab == "suggested") { activeTab = "suggested" }
            TabButton("Scheduled", activeTab == "scheduled") { activeTab = "scheduled" }
        }

        // Auto Find Issues Toggle Card
        Card(
            shape = JulesShapes.medium,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().border(1.dp, Color.White.copy(alpha = JulesOpacity.subtle), JulesShapes.medium)
        ) {
            Row(
                modifier = Modifier.padding(JulesSpacing.l).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(JulesSpacing.m),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xFF1E1E22), JulesShapes.circle)
                            .border(1.dp, Color.White.copy(alpha = JulesOpacity.subtle), JulesShapes.circle),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Lightbulb, contentDescription = null, tint = Color(0xFF818CF8), modifier = Modifier.size(JulesSizes.iconMedium))
                    }
                    Column(verticalArrangement = Arrangement.Center) {
                        Text("Automatically find issues", color = Color(0xFFF4F4F5), fontSize = 15.sp, fontWeight = FontWeight.Medium)
                        Text("Scan codebase for bugs & improvements", color = Color.Gray, fontSize = 12.sp)
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("0/5", color = Color.Gray, fontSize = 12.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                    Spacer(modifier = Modifier.width(JulesSpacing.xxl))
                    Switch(
                        checked = isEnabled,
                        onCheckedChange = { isEnabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF4F46E5),
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = Color(0xFF3F3F46)
                        ),
                        modifier = Modifier.scale(0.8f)
                    )
                }
            }
        }

        // Active Session
        if (runningSession != null) {
            val infiniteTransition = rememberInfiniteTransition()
            val pulseAlpha by infiniteTransition.animateFloat(
                initialValue = 0.5f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000),
                    repeatMode = RepeatMode.Reverse
                )
            )

            Column(verticalArrangement = Arrangement.spacedBy(JulesSpacing.m)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(JulesSpacing.s)) {
                    Icon(Icons.Default.List, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(JulesSizes.iconSmall))
                    Text("Active Session", color = Color.Gray, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }

                Card(
                    shape = JulesShapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color.White.copy(alpha = JulesOpacity.subtle), JulesShapes.medium)
                        .clickable { onSelectSession?.invoke(runningSession) }
                ) {
                    Column(modifier = Modifier.padding(JulesSpacing.l)) {
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(JulesSpacing.m),
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(JulesSizes.avatar)
                                        .background(Color(0xFF6366F1).copy(alpha = JulesOpacity.normal), JulesShapes.small)
                                        .border(1.dp, Color(0xFF6366F1).copy(alpha = JulesOpacity.focused), JulesShapes.small),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.RocketLaunch, contentDescription = null, tint = Color(0xFF818CF8), modifier = Modifier.size(JulesSizes.iconSmall))
                                }
                                Text(
                                    (runningSession.title ?: "Untitled Session").replace("##", "").trim(),
                                    color = Color(0xFFE4E4E7),
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1
                                )
                            }
                            Icon(Icons.Default.MoreHoriz, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(JulesSizes.iconSmall))
                        }

                        Spacer(modifier = Modifier.height(JulesSpacing.m))

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(JulesSpacing.s)) {
                            Box(modifier = Modifier
                                .size(8.dp)
                                .alpha(pulseAlpha)
                                .background(Color(0xFF818CF8), JulesShapes.circle)
                            )
                            Text(
                                getSessionStatusText(runningSession.state),
                                color = Color.Gray,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // Schedule Section
        Column(verticalArrangement = Arrangement.spacedBy(JulesSpacing.m)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(JulesSpacing.s)) {
                Icon(Icons.Default.Schedule, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(JulesSizes.iconSmall))
                Text("Schedule", color = Color.Gray, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(JulesSpacing.s), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                CategoryPill(Icons.Default.Bolt, "Performance")
                CategoryPill(Icons.Default.Palette, "Design")
                CategoryPill(Icons.Default.Security, "Security")
            }
        }
    }
}

@Composable
fun TabButton(text: String, isActive: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(JulesShapes.circle)
            .background(if (isActive) Color(0xFF27272A) else Color.Transparent)
            .border(1.dp, if (isActive) Color.White.copy(alpha = JulesOpacity.normal) else Color.Transparent, JulesShapes.circle)
            .clickable(onClick = onClick)
            .padding(horizontal = JulesSpacing.l, vertical = 6.dp)
    ) {
        Text(
            text,
            color = if (isActive) Color.White else Color(0xFFA1A1AA),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun CategoryPill(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(JulesSpacing.s),
        modifier = Modifier
            .height(JulesSizes.touchTarget) // Ensuring 48dp minimum touch target
            .clip(JulesShapes.small)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, Color.White.copy(alpha = JulesOpacity.subtle), JulesShapes.small)
            .padding(horizontal = JulesSpacing.m, vertical = JulesSpacing.s)
    ) {
        Icon(icon, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp))
        Text(text, color = Color.Gray, fontSize = 12.sp, lineHeight = 12.sp, fontWeight = FontWeight.Medium)
    }
}

private fun Modifier.scale(scale: Float): Modifier = this.then(
    Modifier.graphicsLayer(scaleX = scale, scaleY = scale)
)

private fun getSessionStatusText(state: SessionState): String {
    return when (state) {
        SessionState.QUEUED -> "Queued"
        SessionState.PLANNING -> "Planning..."
        SessionState.AWAITING_PLAN_APPROVAL -> "Awaiting Approval"
        SessionState.AWAITING_USER_FEEDBACK -> "Needs Feedback"
        SessionState.IN_PROGRESS -> "In Progress..."
        SessionState.PAUSED -> "Paused"
        SessionState.COMPLETED -> "Completed"
        SessionState.FAILED -> "Failed"
    }
}
