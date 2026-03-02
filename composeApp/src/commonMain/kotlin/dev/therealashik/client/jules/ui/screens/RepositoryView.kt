package dev.therealashik.client.jules.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontFamily
import dev.therealashik.jules.sdk.model.JulesSession
import dev.therealashik.jules.sdk.model.JulesSource
import dev.therealashik.jules.sdk.model.SessionState
import dev.therealashik.client.jules.ui.*
import dev.therealashik.client.jules.ui.JulesSpacing
import dev.therealashik.client.jules.ui.JulesShapes
import dev.therealashik.client.jules.ui.JulesSizes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepositoryView(
    source: JulesSource,
    sessions: List<JulesSession>,
    onStartNewSession: () -> Unit,
    onSelectSession: (JulesSession) -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Overview", "Environment", "Knowledge")

    // Filter sessions belonging to this source
    val sourceSessions = remember(sessions, source) {
        sessions.filter { it.sourceContext?.source == source.name }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
        ) {
            Column(modifier = Modifier.padding(horizontal = JulesSpacing.xxl, vertical = JulesSpacing.xxxl)) {
                Text(
                    text = source.displayName ?: source.name.substringAfterLast("/"),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = source.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(top = JulesSpacing.xs)
                )
                
                Spacer(modifier = Modifier.height(JulesSpacing.xxxl))

                // Tabs
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.primary,
                    divider = {},
                    indicator = { tabPositions ->
                        Box(
                            Modifier
                                .tabIndicatorOffset(tabPositions[selectedTab])
                                .height(3.dp)
                                .padding(horizontal = JulesSpacing.l)
                                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                        )
                    }
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { 
                                Text(
                                    title, 
                                    color = if (selectedTab == index) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 14.sp
                                ) 
                            }
                        )
                    }
                }
            }
        }

        when (selectedTab) {
            0 -> OverviewTab(sourceSessions, onStartNewSession, onSelectSession)
            1 -> EnvironmentTab(source)
            2 -> KnowledgeTab()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverviewTab(
    sessions: List<JulesSession>,
    onStartNewSession: () -> Unit,
    onSelectSession: (JulesSession) -> Unit
) {
    val activeCount = sessions.count {
        it.state != SessionState.COMPLETED && it.state != SessionState.FAILED
    }
    val completedCount = sessions.count { it.state == SessionState.COMPLETED }
    val failedCount = sessions.count { it.state == SessionState.FAILED }

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }
    val filters = listOf("All", "Active", "Completed", "Failed", "Scheduled", "Archived")

    val filteredSessions = remember(sessions, searchQuery, selectedFilter) {
        sessions.filter { session ->
            val matchesSearch = (session.title ?: "").contains(searchQuery, ignoreCase = true) ||
                    session.name.contains(searchQuery, ignoreCase = true)

            val matchesFilter = when (selectedFilter) {
                "Active" -> session.state != SessionState.COMPLETED && session.state != SessionState.FAILED
                "Completed" -> session.state == SessionState.COMPLETED
                "Failed" -> session.state == SessionState.FAILED
                "Scheduled" -> session.state == SessionState.QUEUED
                "Archived" -> false // Placeholder for archived logic
                else -> true
            }
            matchesSearch && matchesFilter
        }.sortedByDescending { it.createTime } // Sort by newest
    }

    LazyColumn(
        contentPadding = PaddingValues(JulesSpacing.l),
        verticalArrangement = Arrangement.spacedBy(JulesSpacing.l)
    ) {
        // Stats Cards
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(JulesSpacing.l)) {
                StatsCard("Active", activeCount, MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                StatsCard("Completed", completedCount, JulesGreen, Modifier.weight(1f))
                StatsCard("Failed", failedCount, MaterialTheme.colorScheme.error, Modifier.weight(1f))
            }
        }

        // Search & Filter
        item {
            Column {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search sessions...", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent
                    ),
                    shape = JulesShapes.medium
                )

                Spacer(modifier = Modifier.height(JulesSpacing.m))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(JulesSpacing.s)) {
                    items(filters) { filter ->
                        MyFilterChip(
                            selected = selectedFilter == filter,
                            onClick = { selectedFilter = filter },
                            label = { Text(filter) }
                        )
                    }
                }
            }
        }

        // Start New Session Button
        item {
            Button(
                onClick = onStartNewSession,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = JulesShapes.small
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(JulesSpacing.s))
                Text("Start New Session", color = MaterialTheme.colorScheme.onPrimary)
            }
        }

        // History List
        if (filteredSessions.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(JulesSpacing.xxxl), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No sessions found", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        } else {
            items(filteredSessions) { session ->
                SessionHistoryItem(session, onSelectSession)
            }
        }
    }
}

@Composable
fun MyFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit
) {
    Surface(
        color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
        shape = JulesShapes.small,
        border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else Color.Transparent),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(modifier = Modifier.padding(horizontal = JulesSpacing.m, vertical = 6.dp)) {
            CompositionLocalProvider(LocalContentColor provides if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant) {
                label()
            }
        }
    }
}

@Composable
fun StatsCard(label: String, count: Int, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.height(100.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = JulesShapes.large,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Subtle accent
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(4.dp)
                    .background(color)
                    .align(Alignment.CenterStart)
            )
            
            Column(
                modifier = Modifier
                    .padding(start = JulesSpacing.xl, top = JulesSpacing.l, bottom = JulesSpacing.l, end = JulesSpacing.l),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    label.uppercase(), 
                    style = MaterialTheme.typography.labelSmall, 
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    count.toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

@Composable
fun SessionHistoryItem(session: JulesSession, onSelect: (JulesSession) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onSelect(session) },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = JulesShapes.medium
    ) {
        Row(
            modifier = Modifier.padding(JulesSpacing.l),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status Icon
            val (icon, color) = when (session.state) {
                SessionState.COMPLETED -> "✓" to JulesGreen
                SessionState.FAILED -> "✕" to JulesRed
                else -> "↻" to JulesIndigo
            }

            Box(
                modifier = Modifier
                    .size(JulesSizes.avatar)
                    .background(color.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(icon, color = color, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.width(JulesSpacing.l))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    session.title ?: "Untitled Session",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    session.createTime.take(10), // Simple date format
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(JulesSpacing.s))

            // Status Badge
            Text(
                session.state.name,
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
fun EnvironmentTab(source: JulesSource) {
    Column(modifier = Modifier.padding(JulesSpacing.l)) {
        // Project Structure Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            shape = JulesShapes.medium
        ) {
            Column(modifier = Modifier.padding(JulesSpacing.l)) {
                Text("Project Structure", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(JulesSpacing.l))

                EnvironmentRow("Detected Language", "Kotlin / TypeScript") // Mock
                EnvironmentRow("Package Manager", "Gradle / pnpm") // Mock
                EnvironmentRow("Framework", "Compose Multiplatform / React") // Mock
            }
        }

        Spacer(modifier = Modifier.height(JulesSpacing.l))

        // Capabilities Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            shape = JulesShapes.medium
        ) {
            Column(modifier = Modifier.padding(JulesSpacing.l)) {
                Text("Capabilities", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(JulesSpacing.l))

                CapabilityRow("Read Files", true)
                CapabilityRow("Run Commands", true)
                CapabilityRow("Create Pull Requests", true)
            }
        }
    }
}

@Composable
fun EnvironmentRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = JulesSpacing.xs),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun CapabilityRow(label: String, enabled: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = JulesSpacing.xs),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(if (enabled) "✅" else "❌", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun KnowledgeTab() {
    Box(
        modifier = Modifier.fillMaxSize().padding(JulesSpacing.l),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
             CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(JulesSizes.touchTarget)
             )
             Spacer(modifier = Modifier.height(JulesSpacing.l))
             Text("Indexing Codebase...", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
             Text(
                 "The AI is learning the structure of your repository.",
                 style = MaterialTheme.typography.bodyMedium,
                 color = MaterialTheme.colorScheme.onSurfaceVariant
             )
        }
    }
}
