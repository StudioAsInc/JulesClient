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
import dev.therealashik.client.jules.model.JulesSession
import dev.therealashik.client.jules.model.SessionState

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
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TabButton("Repo overview", activeTab == "overview") { activeTab = "overview" }
            TabButton("Suggested", activeTab == "suggested") { activeTab = "suggested" }
            TabButton("Scheduled", activeTab == "scheduled") { activeTab = "scheduled" }
        }

        // Auto Find Issues Toggle Card
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
        ) {
            Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xFF1E1E22), CircleShape)
                            .border(1.dp, Color.White.copy(alpha = 0.05f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Lightbulb, contentDescription = null, tint = Color(0xFF818CF8), modifier = Modifier.size(20.dp))
                    }
                    Column {
                        Text("Automatically find issues", color = Color(0xFFF4F4F5), fontSize = 15.sp, fontWeight = FontWeight.Medium)
                        Text("Scan codebase for bugs & improvements", color = Color.Gray, fontSize = 12.sp)
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("0/5", color = Color.Gray, fontSize = 12.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
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

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.List, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                    Text("Active Session", color = Color.Gray, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }

                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                        .clickable { onSelectSession?.invoke(runningSession) }
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(Color(0xFF6366F1).copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                        .border(1.dp, Color(0xFF6366F1).copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.RocketLaunch, contentDescription = null, tint = Color(0xFF818CF8), modifier = Modifier.size(16.dp))
                                }
                                Text(
                                    runningSession.title ?: "Untitled Session",
                                    color = Color(0xFFE4E4E7),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                            Icon(Icons.Default.MoreHoriz, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(modifier = Modifier
                                .size(8.dp)
                                .alpha(pulseAlpha)
                                .background(Color(0xFF818CF8), CircleShape)
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
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Schedule, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                Text("Schedule", color = Color.Gray, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
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
            .clip(CircleShape)
            .background(if (isActive) Color(0xFF27272A) else Color.Transparent)
            .border(1.dp, if (isActive) Color.White.copy(alpha = 0.1f) else Color.Transparent, CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Text(
            text,
            color = if (isActive) Color.White else Color.Gray,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun CategoryPill(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Icon(icon, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp))
        Text(text, color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Medium)
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
