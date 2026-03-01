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
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Forum
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
import dev.therealashik.jules.sdk.model.JulesSession
import dev.therealashik.jules.sdk.model.JulesSource
import dev.therealashik.client.jules.utils.getSessionDisplayInfo
import dev.therealashik.client.jules.utils.openUrl
import dev.therealashik.client.jules.ui.*

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
                    .background(JulesDrawerBackdrop)
                    .clickable { onClose() }
            )

            // Drawer Content
            Column(
                modifier = Modifier
                    .width(JulesSizes.drawerWidth)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RectangleShape)
                    .clickable(enabled = false) {}
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .height(JulesSizes.drawerHeaderHeight)
                        .padding(horizontal = JulesSpacing.l),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(JulesSpacing.s)
                    ) {
                        Icon(
                            Icons.Default.SmartToy,
                            contentDescription = "Jules",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(JulesSpacing.xxl)
                        )
                        Text(
                            "jules",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            letterSpacing = (-0.5).sp
                        )
                    }

                    FilledTonalIconButton(
                        onClick = onClose,
                        modifier = Modifier.size(JulesSpacing.xxxl),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Icon(Icons.Default.Close, null, modifier = Modifier.size(JulesSizes.iconMedium))
                    }
                }
                Divider(color = MaterialTheme.colorScheme.outlineVariant)

                // Search Input
                var searchQuery by remember { mutableStateOf("") }
                var isFocused by remember { mutableStateOf(false) }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(JulesSpacing.l)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            JulesShapes.medium
                        )
                        .border(
                            1.dp,
                            if (isFocused) MaterialTheme.colorScheme.primary else Color.Transparent,
                            JulesShapes.medium
                        )
                        .padding(horizontal = JulesSpacing.m, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = if (isFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(JulesSizes.iconSmall)
                    )

                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = JulesSpacing.m)
                            .onFocusChanged { isFocused = it.isFocused },
                        textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp),
                        singleLine = true,
                        decorationBox = { innerTextField ->
                            if (searchQuery.isEmpty()) {
                                Text("Search repositories & sessions...", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
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

                LazyColumn(modifier = Modifier.weight(1f).padding(horizontal = JulesSpacing.s)) {
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

                    item { Spacer(modifier = Modifier.height(JulesSpacing.l)) }

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
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        .padding(JulesSpacing.l)
                ) {
                    // Progress
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = JulesSpacing.m),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            "Daily Usage ($sessionsUsed/$dailyLimit)",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(2.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(sessionsUsed.toFloat() / dailyLimit.coerceAtLeast(1))
                                .height(4.dp)
                                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp))
                        )
                    }

                    Spacer(modifier = Modifier.height(JulesSpacing.m))

                    // Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(JulesSpacing.s)
                    ) {
                        // Settings Button
                        FilledTonalButton(
                            onClick = { onNavigateToSettings?.invoke() },
                            modifier = Modifier.weight(1f).height(40.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            shape = JulesShapes.small
                        ) {
                            Icon(Icons.Default.Settings, null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(JulesSpacing.s))
                            Text("Settings", fontSize = 14.sp)
                        }

                        // Docs
                        FilledTonalIconButton(
                            onClick = { openUrl("https://docs.jules.ai") },
                            modifier = Modifier.size(40.dp),
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            Icon(Icons.Default.MenuBook, null, modifier = Modifier.size(18.dp))
                        }

                         // Discord
                        FilledTonalIconButton(
                            onClick = { openUrl("https://discord.gg/jules") },
                            modifier = Modifier.size(40.dp),
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            Icon(Icons.Default.Forum, null, modifier = Modifier.size(18.dp))
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
            .padding(vertical = JulesSpacing.s, horizontal = JulesSpacing.xs),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(JulesSpacing.s)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFFA1A1AA))
            if (count != null && count > 0) {
                 Text(
                    count.toString(),
                    fontSize = 10.sp,
                    color = Color(0xFFD4D4D8),
                    modifier = Modifier
                        .background(Color.White.copy(alpha = JulesOpacity.normal), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                 )
            }
        }

        Icon(
            if (isExpanded) Icons.Default.ExpandMore else Icons.Default.KeyboardArrowRight,
            contentDescription = null,
            tint = JulesZinc,
            modifier = Modifier.size(JulesSizes.iconSmall)
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

    val backgroundColor = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
    val contentColor = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clip(JulesShapes.small)
            .background(backgroundColor)
            .clickable { onSelect() }
            .padding(horizontal = JulesSpacing.m, vertical = JulesSpacing.s),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(JulesSpacing.m)
    ) {
        // Emoji
        Text(
            text = displayInfo.emoji,
            fontSize = 16.sp,
        )

        Column(modifier = Modifier.weight(1f)) {
            // Title
            Text(
                text = session.title ?: session.prompt,
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Status and Helper (Compact)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = displayInfo.label,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = contentColor.copy(alpha = 0.7f)
                )
                if (displayInfo.cta != "none") {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "• ${displayInfo.cta}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1
                    )
                }
            }
        }

        // Context Menu
        Box {
             IconButton(
                onClick = { menuExpanded = true },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = "Options",
                    modifier = Modifier.size(16.dp),
                    tint = contentColor.copy(alpha = 0.7f)
                )
            }

            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
                modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainer)
            ) {
                DropdownMenuItem(
                    text = { Text("Delete", fontSize = 12.sp, color = MaterialTheme.colorScheme.error) },
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
    val backgroundColor = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
    val contentColor = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface
    val iconBg = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clip(JulesShapes.small)
            .background(backgroundColor)
            .clickable { onSelect() }
            .padding(horizontal = JulesSpacing.m, vertical = JulesSpacing.s),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(iconBg, RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.Center
        ) {
             Text(
                 "G",
                 fontSize = 12.sp,
                 fontWeight = FontWeight.Bold,
                 color = if (isSelected) contentColor else MaterialTheme.colorScheme.onSurfaceVariant
             )
        }
        Spacer(modifier = Modifier.width(JulesSpacing.m))
        Text(
            text = source.displayName ?: source.name.split("/").takeLast(2).joinToString("/"),
            fontSize = 14.sp,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
