package dev.therealashik.client.jules.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import dev.therealashik.client.jules.model.JulesSource

@Composable
fun Header(
    onOpenDrawer: () -> Unit,
    currentSource: JulesSource?,
    sources: List<JulesSource>,
    onSourceChange: (JulesSource) -> Unit,
    isLoading: Boolean,
    onOpenSettings: () -> Unit = {}
) {
    var expanded by remember { mutableStateOf(false) }
    var search by remember { mutableStateOf("") }

    val filteredSources = remember(sources, search) {
        if (search.isEmpty()) sources
        else sources.filter {
            (it.displayName ?: it.name).contains(search, ignoreCase = true)
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF0C0C0C).copy(alpha = 0.9f),
        tonalElevation = 0.dp
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .height(48.dp)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left Section
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Sidebar Trigger
                    IconButton(
                        onClick = onOpenDrawer,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Menu,
                            contentDescription = "Toggle sidebar",
                            tint = Color(0xFF71717A),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Repo Pill
                    Box {
                        Row(
                            modifier = Modifier
                                .heightIn(min = 32.dp)
                                .widthIn(max = 200.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF1A1A1D))
                                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                                .clickable { expanded = true }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Gradient icon box
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(Color(0xFF6366F1), Color(0xFF4F46E5))
                                        ),
                                        RoundedCornerShape(4.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Terminal,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(11.dp)
                                )
                            }

                            // Repo name
                            Text(
                                text = currentSource?.displayName ?: currentSource?.name?.split("/")?.takeLast(2)?.joinToString("/") ?: "Select Repository",
                                fontSize = 12.sp,
                                color = Color(0xFFD4D4D8),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )

                            Icon(
                                Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = Color(0xFF71717A),
                                modifier = Modifier.size(11.dp)
                            )
                        }

                        if (expanded) {
                            Popup(
                                alignment = Alignment.TopStart,
                                onDismissRequest = { expanded = false },
                                properties = PopupProperties(focusable = true)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .padding(top = 4.dp)
                                        .width(320.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFF121215))
                                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                        .shadow(16.dp, RoundedCornerShape(12.dp))
                                ) {
                                    // Search input
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp)
                                            .background(Color(0xFF09090B), RoundedCornerShape(8.dp))
                                            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Search, null, tint = Color(0xFF71717A), modifier = Modifier.size(14.dp))
                                        BasicTextField(
                                            value = search,
                                            onValueChange = { search = it },
                                            modifier = Modifier.weight(1f).padding(start = 8.dp),
                                            textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                                            singleLine = true,
                                            decorationBox = { innerTextField ->
                                                if (search.isEmpty()) {
                                                    Text("Find a repository...", color = Color(0xFF52525B), fontSize = 14.sp)
                                                }
                                                innerTextField()
                                            }
                                        )
                                    }

                                    Divider(color = Color.White.copy(alpha = 0.05f), thickness = 1.dp)

                                    // Source list
                                    LazyColumn(modifier = Modifier.heightIn(max = 250.dp)) {
                                        items(filteredSources) { source ->
                                            val isSelected = currentSource?.name == source.name
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .heightIn(min = 44.dp)
                                                    .background(if (isSelected) Color.White.copy(0.05f) else Color.Transparent)
                                                    .clickable {
                                                        onSourceChange(source)
                                                        expanded = false
                                                    }
                                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    source.displayName ?: source.name,
                                                    fontSize = 14.sp,
                                                    color = if (isSelected) Color.White else Color(0xFF9CA3AF),
                                                    fontFamily = FontFamily.Monospace,
                                                    modifier = Modifier.weight(1f),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                if (isSelected) {
                                                    Icon(Icons.Default.Check, null, tint = Color(0xFF818CF8), modifier = Modifier.size(14.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Right Section: Settings
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "Configure",
                        tint = Color(0xFF71717A),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Bottom border
            Divider(
                modifier = Modifier.align(Alignment.BottomCenter),
                color = Color.White.copy(alpha = 0.05f),
                thickness = 1.dp
            )

            // Loading Indicator
            if (isLoading) {
                 LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .align(Alignment.BottomCenter),
                    color = Color(0xFF818CF8),
                    trackColor = Color.Transparent
                )
            }
        }
    }
}
