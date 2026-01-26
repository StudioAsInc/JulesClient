package dev.therealashik.client.jules.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Monitor
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import dev.therealashik.client.jules.ui.* // Import all UI constants
import androidx.compose.material.icons.filled.Storage
import dev.therealashik.client.jules.ui.ThemePreset
import dev.therealashik.client.jules.ui.getThemeConfig
import androidx.compose.material3.ColorScheme
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.*

@Composable
fun SettingsView(
    defaultCardState: Boolean,
    currentTheme: ThemePreset,
    currentApiKey: String,
    onUpdateCardState: (Boolean) -> Unit,
    onThemeChange: (ThemePreset) -> Unit,
    onApiKeyChange: (String) -> Unit,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Sticky Header
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .zIndex(10f),
            color = Color(0xFF0E0E11).copy(alpha = 0.8f),
            tonalElevation = 2.dp
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Transparent)
                    ) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF9CA3AF),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Text(
                        "Settings",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
                HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
            }
        }

        // Scrollable Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = 80.dp) // Offset for header + status bar approx
                .padding(horizontal = 16.dp, vertical = 24.dp)
        ) {


            // Account Section
            Row(
                modifier = Modifier.padding(bottom = 8.dp, start = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                 Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint = Color(0xFF71717A),
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    "ACCOUNT",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF71717A),
                    letterSpacing = 1.5.sp
                )
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF161619)),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "API Key",
                        fontSize = 14.sp,
                        color = Color(0xFFD4D4D8),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    var apiKey by remember { mutableStateOf(currentApiKey) }
                    
                    // Update local state if prop changes
                    LaunchedEffect(currentApiKey) {
                        apiKey = currentApiKey
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        BasicTextField(
                            value = apiKey,
                            onValueChange = { 
                                apiKey = it 
                                onApiKeyChange(it) 
                            },
                            visualTransformation = PasswordVisualTransformation(),
                            textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                            cursorBrush = SolidColor(Color.White),
                            modifier = Modifier.fillMaxWidth(),
                            decorationBox = { innerTextField ->
                                if (apiKey.isEmpty()) {
                                    Text("sk-...", color = Color.Gray, fontSize = 14.sp)
                                }
                                innerTextField()
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Your key is stored locally on this device.",
                        fontSize = 12.sp,
                        color = Color(0xFF71717A)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))

            // Appearance Section
            Row(
                modifier = Modifier.padding(bottom = 8.dp, start = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Monitor,
                    contentDescription = null,
                    tint = Color(0xFF71717A),
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    "APPEARANCE",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF71717A),
                    letterSpacing = 1.5.sp
                )
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF161619)),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                modifier = Modifier.fillMaxWidth()
            ) {

                    // Theme Selection
                    Column(
                         modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            "Theme",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            ThemePreset.values().forEach { preset ->
                                val config = getThemeConfig(preset)
                                val isSelected = currentTheme == preset
                                
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(config.background)
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp, 
                                            color = if (isSelected) config.primary else Color.White.copy(alpha = 0.2f), 
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable { onThemeChange(preset) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .background(config.primary, RoundedCornerShape(8.dp))
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.05f)) // Separator
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                            Text(
                                "Default Card State",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Choose whether large items (commands, code changes) appear expanded or collapsed by default.",
                            fontSize = 14.sp,
                            color = Color(0xFF9CA3AF), // Zinc-400
                            lineHeight = 20.sp
                        )
                    }

                    // Toggle Button Group
                    Row(
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        // Expanded Button (defaultCardState == true means Expanded)

                        val isExpanded = defaultCardState
                        val isCollapsed = !defaultCardState

                        // Expanded Option
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (isExpanded) Color(0xFF4F46E5) else Color.Transparent
                                )
                                .clickable { onUpdateCardState(true) } // Set Expanded
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Expanded",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isExpanded) Color.White else Color(0xFF71717A)
                            )
                        }

                        // Collapsed Option
                         Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (isCollapsed) Color(0xFF4F46E5) else Color.Transparent
                                )
                                .clickable { onUpdateCardState(false) } // Set Collapsed
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Collapsed",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isCollapsed) Color.White else Color(0xFF71717A)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // About Section
             Row(
                modifier = Modifier.padding(bottom = 8.dp, start = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Tune, // Using Tune as Sliders
                    contentDescription = null,
                    tint = Color(0xFF71717A),
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    "ABOUT",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF71717A),
                    letterSpacing = 1.5.sp
                )
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF161619)),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    SettingsRow("Client Version", "v0.1.0-alpha")
                    Divider(color = Color.White.copy(alpha = 0.05f))
                    SettingsRow("Theme", "Dark (System)")
                }
            }
            Spacer(modifier = Modifier.height(32.dp))

            // Data Section
            Row(
                modifier = Modifier.padding(bottom = 8.dp, start = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Storage, // Need to make sure Storage is available or use generic
                    contentDescription = null,
                    tint = Color(0xFF71717A),
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    "DATA & STORAGE",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF71717A),
                    letterSpacing = 1.5.sp
                )
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF161619)),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    SettingsActionRow("Export Settings", "Copy to Clipboard", onClick = {})
                    HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                    SettingsActionRow("Clear All Data", "Reset App", isDestructive = true, onClick = {})
                }
            }
        }
    }
}

@Composable
fun SettingsActionRow(label: String, actionLabel: String, isDestructive: Boolean = false, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 14.sp, color = Color(0xFFD4D4D8))
        Text(
            actionLabel, 
            fontSize = 14.sp, 
            color = if (isDestructive) Color(0xFFEF4444) else JulesPrimary,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun SettingsRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 14.sp, color = Color(0xFFD4D4D8)) // Zinc-300
        Text(value, fontSize = 14.sp, color = Color(0xFF71717A)) // Zinc-500
    }
}
