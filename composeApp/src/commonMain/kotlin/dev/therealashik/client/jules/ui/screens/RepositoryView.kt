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
import dev.therealashik.client.jules.model.JulesSession
import dev.therealashik.client.jules.model.JulesSource
import dev.therealashik.client.jules.model.SessionState
import dev.therealashik.client.jules.ui.*

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
                            MaterialTheme.colorScheme.surface, // Use surface instead of hardcoded
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
        ) {
            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 32.dp)) {
                Text(
                    text = source.displayName ?: source.name.substringAfterLast("/"),
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = source.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(top = 4.dp)
                )
                
                Spacer(modifier = Modifier.height(32.dp))

                // Tabs
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = JulesPrimary,
                    divider = {},
                    indicator = { tabPositions ->
                        Box(
                            Modifier
                                .tabIndicatorOffset(tabPositions[selectedTab])
                                .height(3.dp)
                                .padding(horizontal = 16.dp)
                                .background(JulesPrimary, RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
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
                                    color = if (selectedTab == index) Color.White else Color.Gray,
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
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Stats Cards
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                StatsCard("Active", activeCount, JulesIndigo, Modifier.weight(1f))
                StatsCard("Completed", completedCount, JulesGreen, Modifier.weight(1f))
                StatsCard("Failed", failedCount, JulesRed, Modifier.weight(1f))
            }
        }

        // Search & Filter
        item {
            Column {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search sessions...", color = Color.Gray) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = JulesSurface,
                        unfocusedContainerColor = JulesSurface,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                colors = ButtonDefaults.buttonColors(containerColor = JulesPrimary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Start New Session")
            }
        }

        // History List
        if (filteredSessions.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No sessions found", color = Color.Gray, style = MaterialTheme.typography.bodyLarge)
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
        color = if (selected) JulesPrimary.copy(alpha = 0.2f) else JulesSurface,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, if (selected) JulesPrimary else Color.Transparent),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
            CompositionLocalProvider(LocalContentColor provides if (selected) JulesPrimary else Color.Gray) {
                label()
            }
        }
    }
}

@Composable
fun StatsCard(label: String, count: Int, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.height(100.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF18181B)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
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
                    .padding(start = 20.dp, top = 16.dp, bottom = 16.dp, end = 16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    label.uppercase(), 
                    style = MaterialTheme.typography.labelSmall, 
                    color = Color.Gray,
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    count.toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
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
        colors = CardDefaults.cardColors(containerColor = JulesSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
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
                    .size(32.dp)
                    .background(color.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(icon, color = color, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    session.title ?: "Untitled Session",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    session.createTime.take(10), // Simple date format
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

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
    Column(modifier = Modifier.padding(16.dp)) {
        // Project Structure Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = JulesSurface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Project Structure", style = MaterialTheme.typography.titleMedium, color = Color.White)
                Spacer(modifier = Modifier.height(16.dp))

                EnvironmentRow("Detected Language", "Kotlin / TypeScript") // Mock
                EnvironmentRow("Package Manager", "Gradle / pnpm") // Mock
                EnvironmentRow("Framework", "Compose Multiplatform / React") // Mock
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Capabilities Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = JulesSurface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Capabilities", style = MaterialTheme.typography.titleMedium, color = Color.White)
                Spacer(modifier = Modifier.height(16.dp))

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
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = Color.White)
    }
}

@Composable
fun CapabilityRow(label: String, enabled: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
        Text(if (enabled) "✅" else "❌", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun KnowledgeTab() {
    Box(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
             CircularProgressIndicator(
                color = JulesPrimary,
                modifier = Modifier.size(48.dp)
             )
             Spacer(modifier = Modifier.height(16.dp))
             Text("Indexing Codebase...", style = MaterialTheme.typography.titleMedium, color = Color.White)
             Text(
                 "The AI is learning the structure of your repository.",
                 style = MaterialTheme.typography.bodyMedium,
                 color = Color.Gray
             )
        }
    }
}
