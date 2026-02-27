package dev.therealashik.client.jules.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.therealashik.client.jules.cache.CacheManager
import dev.therealashik.client.jules.model.*
import dev.therealashik.client.jules.storage.SettingsStorage
import dev.therealashik.client.jules.storage.getCacheConfig
import dev.therealashik.client.jules.storage.saveCacheConfig
import dev.therealashik.client.jules.theme.ThemeManager
import dev.therealashik.client.jules.ui.JulesSpacing
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    themeManager: ThemeManager,
    cacheManager: CacheManager,
    settingsStorage: SettingsStorage,
    apiKey: String,
    onApiKeyChange: (String) -> Unit,
    onNavigateBack: () -> Unit,
    onEditTheme: (CustomTheme?) -> Unit
) {
    val scope = rememberCoroutineScope()
    val activeTheme by themeManager.activeTheme.collectAsState()
    val customThemes by themeManager.customThemes.collectAsState()
    val cacheStats by cacheManager.stats.collectAsState()
    
    var cacheConfig by remember { mutableStateOf(CacheConfig()) }
    var showClearDialog by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        cacheConfig = settingsStorage.getCacheConfig()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(JulesSpacing.l)
        ) {
            // Account Section
            Text("Account", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(JulesSpacing.m))

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(JulesSpacing.l)) {
                    Text("Jules API Key", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(JulesSpacing.s))
                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = onApiKeyChange,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("Enter your API key") },
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Password
                        )
                    )
                    Spacer(Modifier.height(JulesSpacing.s))
                    Text(
                        "Your API key is stored locally on your device.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(JulesSpacing.xl))

            // Appearance Section
            Text("Appearance", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(JulesSpacing.m))
            
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(JulesSpacing.l)) {
                    Text("Theme Preset", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(JulesSpacing.s))
                    
                    ThemePreset.entries.forEach { preset ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    scope.launch {
                                        themeManager.setActivePreset(preset)
                                    }
                                }
                                .padding(vertical = JulesSpacing.s),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = activeTheme == preset.theme,
                                onClick = {
                                    scope.launch {
                                        themeManager.setActivePreset(preset)
                                    }
                                }
                            )
                            Spacer(Modifier.width(JulesSpacing.s))
                            Text(preset.displayName)
                        }
                    }
                }
            }
            
            Spacer(Modifier.height(JulesSpacing.l))
            
            // Custom Themes Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Custom Themes", style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = { onEditTheme(null) }) {
                    Icon(Icons.Default.Add, "Create theme")
                }
            }
            Spacer(Modifier.height(JulesSpacing.m))
            
            if (customThemes.isEmpty()) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(JulesSpacing.xl),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No custom themes", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            } else {
                customThemes.forEach { customTheme ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = JulesSpacing.s)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    scope.launch {
                                        themeManager.setActiveCustomTheme(customTheme.id)
                                    }
                                }
                                .padding(JulesSpacing.l),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(customTheme.name, style = MaterialTheme.typography.bodyLarge)
                                if (customTheme.isActive) {
                                    Text("Active", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            Row {
                                IconButton(onClick = { onEditTheme(customTheme) }) {
                                    Icon(Icons.Default.Edit, "Edit")
                                }
                                IconButton(onClick = {
                                    scope.launch {
                                        themeManager.deleteCustomTheme(customTheme.id)
                                    }
                                }) {
                                    Icon(Icons.Default.Delete, "Delete")
                                }
                            }
                        }
                    }
                }
            }
            
            Spacer(Modifier.height(JulesSpacing.xl))
            
            // Cache Section
            Text("Cache", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(JulesSpacing.m))
            
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(JulesSpacing.l)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Enabled")
                        Switch(
                            checked = cacheConfig.enabled,
                            onCheckedChange = {
                                cacheConfig = cacheConfig.copy(enabled = it)
                                scope.launch {
                                    settingsStorage.saveCacheConfig(cacheConfig)
                                }
                            }
                        )
                    }
                    
                    Spacer(Modifier.height(JulesSpacing.m))
                    Divider()
                    Spacer(Modifier.height(JulesSpacing.m))
                    
                    // Expiration
                    Text("Default Expiration", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(JulesSpacing.s))
                    
                    var expandedExpiration by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expandedExpiration,
                        onExpandedChange = { expandedExpiration = it }
                    ) {
                        OutlinedTextField(
                            value = cacheConfig.defaultExpiration.displayName,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandedExpiration) },
                            modifier = Modifier.fillMaxWidth().menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedExpiration,
                            onDismissRequest = { expandedExpiration = false }
                        ) {
                            CacheExpiration.entries.forEach { exp ->
                                DropdownMenuItem(
                                    text = { Text(exp.displayName) },
                                    onClick = {
                                        cacheConfig = cacheConfig.copy(defaultExpiration = exp)
                                        scope.launch {
                                            settingsStorage.saveCacheConfig(cacheConfig)
                                        }
                                        expandedExpiration = false
                                    }
                                )
                            }
                        }
                    }
                    
                    Spacer(Modifier.height(JulesSpacing.m))
                    
                    // Size Limit
                    Text("Size Limit", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(JulesSpacing.s))
                    
                    var expandedSize by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expandedSize,
                        onExpandedChange = { expandedSize = it }
                    ) {
                        OutlinedTextField(
                            value = cacheConfig.sizeLimit.displayName,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandedSize) },
                            modifier = Modifier.fillMaxWidth().menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedSize,
                            onDismissRequest = { expandedSize = false }
                        ) {
                            CacheSizeLimit.entries.forEach { limit ->
                                DropdownMenuItem(
                                    text = { Text(limit.displayName) },
                                    onClick = {
                                        cacheConfig = cacheConfig.copy(sizeLimit = limit)
                                        scope.launch {
                                            settingsStorage.saveCacheConfig(cacheConfig)
                                        }
                                        expandedSize = false
                                    }
                                )
                            }
                        }
                    }
                    
                    Spacer(Modifier.height(JulesSpacing.m))
                    Divider()
                    Spacer(Modifier.height(JulesSpacing.m))
                    
                    Text("Statistics", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(JulesSpacing.s))
                    Text("Size: ${cacheStats.totalSizeBytes / 1024} KB")
                    Text("Entries: ${cacheStats.entryCount}")
                    Text("Hit rate: ${(cacheStats.hitRate * 100).toInt()}%")
                    Text("Hits: ${cacheStats.hitCount}")
                    Text("Misses: ${cacheStats.missCount}")
                    
                    Spacer(Modifier.height(JulesSpacing.m))
                    
                    Button(
                        onClick = { showClearDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Clear Cache")
                    }
                }
            }
            
            Spacer(Modifier.height(JulesSpacing.xl))
            
            // About Section
            Text("About", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(JulesSpacing.m))
            
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(JulesSpacing.l)) {
                    Text("Jules Client", style = MaterialTheme.typography.bodyLarge)
                    Text("Version 1.0.0", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
    
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear Cache") },
            text = { Text("Are you sure you want to clear all cached data?") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        cacheManager.clear()
                        showClearDialog = false
                    }
                }) {
                    Text("Clear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
