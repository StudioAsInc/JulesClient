package dev.therealashik.client.jules.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import dev.therealashik.client.jules.model.JulesSession
import dev.therealashik.client.jules.model.JulesSource
import dev.therealashik.client.jules.utils.getSessionDisplayInfo

@Composable
fun Drawer(
    isOpen: Boolean,
    onClose: () -> Unit,
    sessions: List<JulesSession>,
    currentSessionId: String?,
    onSelectSession: (JulesSession) -> Unit,
    onDeleteSession: (String) -> Unit,
    sessionsUsed: Int,
    dailyLimit: Int,
    sources: List<JulesSource>,
    currentSource: JulesSource?,
    onSelectSource: (JulesSource) -> Unit,
    onNavigateToSettings: (() -> Unit)? = null
) {
    AnimatedVisibility(
        visible = isOpen,
        enter = slideInHorizontally(
            initialOffsetX = { -it },
            animationSpec = tween(300, easing = FastOutSlowInEasing)
        ) + fadeIn(tween(300)),
        exit = slideOutHorizontally(
            targetOffsetX = { -it },
            animationSpec = tween(300)
        ) + fadeOut(tween(300))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(100f)
        ) {
            // Backdrop
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .clickable { onClose() }
            )

            // Drawer Content
            Column(
                modifier = Modifier
                    .width(320.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.surface)
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.05f),
                        shape = RectangleShape
                    )
                    .clickable(enabled = false) {}
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .padding(bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Logo replacement since image is missing
                        Icon(
                            Icons.Default.SmartToy,
                            contentDescription = "Jules",
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            "jules",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            letterSpacing = (-0.5).sp
                        )
                    }

                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Close, null, tint = Color(0xFF9CA3AF), modifier = Modifier.size(20.dp))
                    }
                }
                Divider(color = Color.White.copy(alpha = 0.05f))

                // Search Input
                var searchQuery by remember { mutableStateOf("") }
                var isFocused by remember { mutableStateOf(false) }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .background(
                            if (isFocused) Color(0xFF1E1E22) else Color(0xFF161619),
                            RoundedCornerShape(12.dp)
                        )
                        .border(
                            1.dp,
                            if (isFocused) Color(0xFF6366F1).copy(alpha = 0.5f) else Color.White.copy(alpha = 0.1f),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = if (isFocused) Color(0xFF6366F1) else Color(0xFF71717A),
                        modifier = Modifier.size(16.dp)
                    )

                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 12.dp)
                            .onFocusChanged { isFocused = it.isFocused },
                        textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                        singleLine = true,
                        decorationBox = { innerTextField ->
                            if (searchQuery.isEmpty()) {
                                Text("Search repositories & sessions...", color = Color(0xFF52525B), fontSize = 14.sp)
                            }
                            innerTextField()
                        }
                    )
                }

                // Filtering logic
                var isSessionsExpanded by remember { mutableStateOf(true) }
                var isCodebasesExpanded by remember { mutableStateOf(true) }

                val filteredSessions = remember(sessions, searchQuery) {
                    if (searchQuery.isBlank()) sessions
                    else sessions.filter {
                        (it.title ?: "").contains(searchQuery, ignoreCase = true) ||
                                it.prompt.contains(searchQuery, ignoreCase = true)
                    }
                }

                val filteredSources = remember(sources, searchQuery) {
                    if (searchQuery.isBlank()) sources
                    else sources.filter {
                        (it.displayName ?: "").contains(searchQuery, ignoreCase = true) ||
                                it.name.contains(searchQuery, ignoreCase = true)
                    }
                }

                LazyColumn(modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
                    // Recent Sessions
                    item {
                         DrawerSectionHeader(
                            title = "Recent sessions",
                            count = if (searchQuery.isNotEmpty()) filteredSessions.size else null,
                            isExpanded = isSessionsExpanded,
                            onToggle = { isSessionsExpanded = !isSessionsExpanded }
                        )
                    }
                    if (isSessionsExpanded) {
                        items(filteredSessions) { session ->
                            SessionItem(
                                session = session,
                                isSelected = session.name == currentSessionId,
                                onSelect = { onSelectSession(session); onClose() }, // Close drawer on selection
                                onDelete = { onDeleteSession(session.name) }
                            )
                        }
                    }

                    item { Spacer(modifier = Modifier.height(16.dp)) }

                    // Codebases
                    item {
                        DrawerSectionHeader(
                            title = "Codebases",
                            count = if (searchQuery.isNotEmpty()) filteredSources.size else null,
                            isExpanded = isCodebasesExpanded,
                            onToggle = { isCodebasesExpanded = !isCodebasesExpanded }
                        )
                    }
                    if (isCodebasesExpanded) {
                        items(filteredSources) { source ->
                            SourceItem(
                                source = source,
                                isSelected = currentSource?.name == source.name,
                                onSelect = { onSelectSource(source); onClose() }
                            )
                        }
                    }
                }

                // Footer
                Column(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, Color.White.copy(alpha = 0.05f)) // Top border effectively
                        .padding(16.dp)
                ) {
                    // Progress
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            "Daily Usage ($sessionsUsed/$dailyLimit)",
                            fontSize = 12.sp,
                            color = Color(0xFF71717A)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .background(Color(0xFF3F3F46), RoundedCornerShape(2.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(sessionsUsed.toFloat() / dailyLimit.coerceAtLeast(1))
                                .height(4.dp)
                                .background(Color(0xFF6366F1), RoundedCornerShape(2.dp))
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Settings Button
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF1E1E22))
                                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                                .clickable { onNavigateToSettings?.invoke() },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.Settings, null, tint = Color(0xFFD4D4D8), modifier = Modifier.size(14.dp))
                                Text("Settings", fontSize = 14.sp, color = Color(0xFFD4D4D8))
                            }
                        }

                        // Docs
                        IconButton(
                            onClick = { /* TODO */ },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF1E1E22))
                                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                        ) {
                            // Using generic icon for Docs
                            Icon(Icons.Default.Description, null, tint = Color(0xFF9CA3AF), modifier = Modifier.size(18.dp))
                        }

                         // Discord
                        IconButton(
                            onClick = { /* TODO */ },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF1E1E22))
                                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                        ) {
                             // Using generic icon for Discord
                            Icon(Icons.Default.Chat, null, tint = Color(0xFF9CA3AF), modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DrawerSectionHeader(
    title: String,
    count: Int?,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFFA1A1AA))
            if (count != null && count > 0) {
                 Text(
                    count.toString(),
                    fontSize = 10.sp,
                    color = Color(0xFFD4D4D8),
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                 )
            }
        }

        Icon(
            if (isExpanded) Icons.Default.ExpandMore else Icons.Default.KeyboardArrowRight,
            contentDescription = null,
            tint = Color(0xFF71717A),
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
fun SessionItem(
    session: JulesSession,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    val displayInfo = remember(session.state) { getSessionDisplayInfo(session.state) }
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) Color.White.copy(alpha = 0.05f) else Color.Transparent)
            .clickable { onSelect() }
            .padding(12.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Emoji
        Text(
            text = displayInfo.emoji,
            fontSize = 16.sp,
            modifier = Modifier.padding(top = 2.dp)
        )

        Column(modifier = Modifier.weight(1f)) {
            // Title
            Text(
                text = session.title ?: session.prompt,
                fontSize = 14.sp,
                fontWeight = FontWeight.Light,
                color = if (isSelected) Color.White else Color(0xFFD4D4D8),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Status and Helper
            Column {
                Text(
                    text = displayInfo.label,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isSelected) Color(0xFFA5B4FC) else Color(0xFFA1A1AA), // Indigo-300 or Zinc-400
                    modifier = Modifier.padding(top = 2.dp)
                )

                Text(
                    text = displayInfo.helperText,
                    fontSize = 10.sp,
                    color = Color(0xFF52525B), // Zinc-600
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (displayInfo.cta != "none") {
                    Text(
                        text = "${displayInfo.cta} →",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF818CF8), // Indigo-400
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }

        // Context Menu
        Box {
             IconButton(
                onClick = { menuExpanded = true },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    Icons.Default.MoreHoriz,
                    contentDescription = "Options",
                    tint = Color(0xFF71717A),
                    modifier = Modifier.size(16.dp)
                )
            }

            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
                modifier = Modifier.background(Color(0xFF18181B))
            ) {
                // We can add Pause/Resume here if we had callbacks, for now just Delete as per previous implementation
                DropdownMenuItem(
                    text = { Text("Delete", fontSize = 12.sp, color = Color(0xFFF87171)) },
                    onClick = {
                        onDelete()
                        menuExpanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun SourceItem(
    source: JulesSource,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) Color.White.copy(alpha = 0.05f) else Color.Transparent)
            .clickable { onSelect() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon (Github or box)
        Box(
            modifier = Modifier.size(16.dp).background(Color(0xFF27272A), RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.Center
        ) {
            // Placeholder for Github icon
             Text("G", fontSize = 10.sp, color = Color.Gray)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = source.displayName ?: source.name.split("/").takeLast(2).joinToString("/"),
                fontSize = 14.sp,
                color = if (isSelected) Color.White else Color(0xFFA1A1AA),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
