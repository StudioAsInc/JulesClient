package dev.therealashik.client.jules.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.therealashik.client.jules.model.AutomationMode
import dev.therealashik.client.jules.model.JulesSource
import dev.therealashik.client.jules.utils.PlatformFile
import dev.therealashik.client.jules.utils.rememberFilePickerLauncher
import dev.therealashik.client.jules.viewmodel.CreateSessionConfig
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val DEFAULT_PLACEHOLDERS = listOf(
    "Refactor this function...",
    "Fix CSS alignment issues...",
    "Add a new API endpoint...",
    "Optimize performance...",
    "Write unit tests...",
    "Explain this code..."
)

private const val PLACEHOLDER_CYCLE_INTERVAL = 3500L

enum class InputAreaVariant {
    DEFAULT, // Hero Card
    CHAT    // Compact Floating Bar
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InputArea(
    onSendMessage: (String, CreateSessionConfig) -> Unit,
    isLoading: Boolean,
    currentSource: JulesSource?,
    modifier: Modifier = Modifier,
    variant: InputAreaVariant = InputAreaVariant.DEFAULT,
    onSendMessageMinimal: ((String) -> Unit)? = null // For simple chat usage
) {
    var input by remember { mutableStateOf("") }
    var isFocused by remember { mutableStateOf(false) }

    // Dynamic Placeholder Logic
    var placeholderIndex by remember { mutableStateOf(0) }
    LaunchedEffect(isFocused, input) {
        if (!isFocused && input.isEmpty()) {
            while (true) {
                delay(PLACEHOLDER_CYCLE_INTERVAL)
                placeholderIndex = (placeholderIndex + 1) % DEFAULT_PLACEHOLDERS.size
            }
        }
    }
    val placeholderText = DEFAULT_PLACEHOLDERS[placeholderIndex]

    // Attachments
    val attachments = remember { mutableStateListOf<PlatformFile>() }
    val filePicker = rememberFilePickerLauncher { file ->
        attachments.add(file)
    }
    val scope = rememberCoroutineScope()

    // Expansion Logic
    val isExpanded = isFocused || input.isNotEmpty() || attachments.isNotEmpty()

    // Branch Selection
    var selectedBranch by remember(currentSource) {
        mutableStateOf(currentSource?.githubRepo?.defaultBranch?.displayName ?: "main")
    }
    var isBranchMenuOpen by remember { mutableStateOf(false) }

    // Settings / Mode Selection
    var isSettingsMenuOpen by remember { mutableStateOf(false) }
    var sessionTitle by remember { mutableStateOf("") }
    var selectedMode by remember { mutableStateOf("START") } // "START", "SCHEDULED", "INTERACTIVE", "REVIEW"

    val focusRequester = remember { FocusRequester() }

    if (variant == InputAreaVariant.CHAT) {
        // --- CHAT VARIANT (Compact floating bar) ---
        Row(
            modifier = modifier
                .fillMaxWidth()
                .background(
                    Color(0xFF1C1C1F).copy(alpha = 0.95f),
                    RoundedCornerShape(26.dp)
                )
                .border(
                    1.dp,
                    if (isFocused) Color.White.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.1f),
                    RoundedCornerShape(26.dp)
                )
                .padding(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Plus Button
            IconButton(
                onClick = { filePicker.launch() },
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Add",
                    tint = Color(0xFFA1A1AA) // Zinc-400
                )
            }

            TextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester)
                    .onFocusChanged { isFocused = it.isFocused },
                placeholder = { Text(placeholderText, color = Color(0xFF71717A)) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = Color.White,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                maxLines = 6
            )

            // Send Button
            val isEnabled = (input.isNotBlank() || attachments.isNotEmpty()) && !isLoading
            IconButton(
                onClick = {
                    if (isEnabled) {
                         scope.launch {
                            var prompt = input
                            if (attachments.isNotEmpty()) {
                                val sb = StringBuilder()
                                attachments.forEach { file ->
                                    val content = try {
                                        file.readText()
                                    } catch (e: Exception) {
                                        "Error reading file: ${e.message}"
                                    }
                                    sb.append("File: ${file.name}\n```\n$content\n```\n\n")
                                }
                                val filesContent = sb.toString().trim()
                                prompt = if (prompt.isNotBlank()) "$prompt\n\n--- Attached Files ---\n$filesContent" else "--- Attached Files ---\n$filesContent"
                            }

                            if (onSendMessageMinimal != null) {
                                onSendMessageMinimal(prompt)
                            } else {
                                onSendMessage(prompt, CreateSessionConfig(startingBranch = selectedBranch))
                            }
                            input = ""
                            attachments.clear()
                        }
                    }
                },
                enabled = isEnabled || isLoading,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        if (isEnabled) Color(0xFF4F46E5) else Color(0xFF27272A)
                    )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                } else {
                    Icon(
                        Icons.Default.ArrowForward,
                        contentDescription = "Send",
                        tint = if (isEnabled) Color.White else Color(0xFF71717A),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    } else {
        // --- DEFAULT VARIANT (Hero Card) ---
        Column(modifier = modifier) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF141417)),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = if (isExpanded) Color(0xFF6366F1).copy(alpha = 0.4f) else Color.White.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .animateContentSize()
                    .clickable(enabled = !isFocused) {
                        focusRequester.requestFocus()
                    }
                    .then(
                        if (isExpanded) Modifier.shadow(
                            elevation = 8.dp,
                            shape = RoundedCornerShape(16.dp),
                            spotColor = Color(0xFF6366F1).copy(alpha = 0.15f)
                        ) else Modifier.shadow(elevation = 2.dp, shape = RoundedCornerShape(16.dp))
                    )
            ) {
                Column(
                    modifier = Modifier.padding(if (isExpanded) PaddingValues(12.dp) else PaddingValues(horizontal = 16.dp, vertical = 12.dp))
                ) {
                    TextField(
                        value = input,
                        onValueChange = { input = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                            .onFocusChanged { isFocused = it.isFocused },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = Color(0xFFE4E4E7),
                            unfocusedTextColor = Color(0xFFE4E4E7),
                            cursorColor = Color(0xFF6366F1)
                        ),
                        placeholder = {
                            Text(
                                placeholderText,
                                color = Color(0xFFA1A1AA),
                                modifier = Modifier.animateContentSize()
                            )
                        },
                        minLines = if (isExpanded) 3 else 1,
                        maxLines = if (isExpanded) 10 else 1
                    )

                    // Attachments Display
                    if (attachments.isNotEmpty()) {
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(attachments) { file ->
                                InputChip(
                                    selected = true,
                                    onClick = { attachments.remove(file) },
                                    label = { Text(file.name, maxLines = 1, fontSize = 12.sp) },
                                    trailingIcon = { Icon(Icons.Default.Close, null, Modifier.size(14.dp)) },
                                    colors = InputChipDefaults.inputChipColors(
                                        containerColor = Color(0xFF27272A),
                                        labelColor = Color(0xFFE4E4E7),
                                        selectedContainerColor = Color(0xFF27272A),
                                        selectedLabelColor = Color(0xFFE4E4E7)
                                    ),
                                    border = InputChipDefaults.inputChipBorder(
                                        enabled = true,
                                        selected = true,
                                        borderColor = Color.White.copy(alpha = 0.1f)
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Footer Controls
            AnimatedVisibility(visible = isExpanded) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Actions
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Plus Button
                        IconButton(
                            onClick = { filePicker.launch() },
                            modifier = Modifier
                                .size(32.dp)
                                .padding(4.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add", tint = Color(0xFFA1A1AA), modifier = Modifier.size(20.dp))
                        }

                        // Branch Selector
                        Box {
                            Row(
                                modifier = Modifier
                                    .height(32.dp)
                                    .clickable { isBranchMenuOpen = true }
                                    .padding(horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.AccountTree, contentDescription = null, tint = Color(0xFF818CF8), modifier = Modifier.size(14.dp))
                                Text(selectedBranch, color = Color(0xFFE4E4E7), fontSize = 13.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color(0xFF71717A), modifier = Modifier.size(14.dp))
                            }

                            DropdownMenu(
                                expanded = isBranchMenuOpen,
                                onDismissRequest = { isBranchMenuOpen = false },
                                modifier = Modifier.background(Color(0xFF121215))
                            ) {
                                currentSource?.githubRepo?.branches?.forEach { branch ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                branch.displayName,
                                                color = if(branch.displayName == selectedBranch) Color(0xFF818CF8) else Color.LightGray
                                            )
                                        },
                                        onClick = {
                                            selectedBranch = branch.displayName
                                            isBranchMenuOpen = false
                                        },
                                        leadingIcon = {
                                            if (branch.displayName == selectedBranch) {
                                                Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF818CF8))
                                            }
                                        }
                                    )
                                }
                            }
                        }

                        // Settings Button
                        Box {
                            IconButton(
                                onClick = { isSettingsMenuOpen = true },
                                modifier = Modifier
                                    .size(32.dp)
                                    .padding(4.dp)
                            ) {
                                Icon(
                                    Icons.Default.Settings,
                                    contentDescription = "Settings",
                                    tint = if(sessionTitle.isNotEmpty()) Color(0xFF818CF8) else Color(0xFFA1A1AA),
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = isSettingsMenuOpen,
                                onDismissRequest = { isSettingsMenuOpen = false },
                                modifier = Modifier.background(Color(0xFF121215)).width(280.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    // Session Title
                                    Text("SESSION TITLE", fontSize = 10.sp, color = Color(0xFFA1A1AA), fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    OutlinedTextField(
                                        value = sessionTitle,
                                        onValueChange = { sessionTitle = it },
                                        placeholder = { Text("Optional title...", fontSize = 12.sp) },
                                        textStyle = LocalTextStyle.current.copy(fontSize = 12.sp, color = Color.White),
                                        modifier = Modifier.fillMaxWidth().height(40.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Color(0xFF818CF8),
                                            unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                                            focusedContainerColor = Color(0xFF18181B),
                                            unfocusedContainerColor = Color(0xFF18181B)
                                        )
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))

                                    // Session Mode
                                    Text("SESSION MODE", fontSize = 10.sp, color = Color(0xFFA1A1AA), fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(6.dp))

                                    val modes = listOf(
                                        Triple("START", "Start immediately", Icons.Default.Send), // Default/Rocket equivalent
                                        Triple("SCHEDULED", "Scheduled task", Icons.Default.Schedule),
                                        Triple("INTERACTIVE", "Interactive plan", Icons.Default.ChatBubble),
                                        Triple("REVIEW", "Review plan", Icons.Default.Search)
                                    )

                                    modes.forEach { (mode, desc, icon) ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    selectedMode = mode
                                                    isSettingsMenuOpen = false
                                                }
                                                .background(if (selectedMode == mode) Color(0xFF818CF8).copy(alpha = 0.1f) else Color.Transparent, RoundedCornerShape(8.dp))
                                                .padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(icon, contentDescription = null, tint = if(selectedMode == mode) Color(0xFF818CF8) else Color.Gray, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        if (mode == "START") "Start immediately" else if (mode == "SCHEDULED") "Scheduled task" else if (mode == "INTERACTIVE") "Interactive plan" else "Review plan",
                                                        color = if(selectedMode == mode) Color.White else Color.LightGray,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                    if (mode == "SCHEDULED") {
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Box(
                                                            modifier = Modifier
                                                                .background(Color(0xFF6366F1).copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                                            ) {
                                                                Text("NEW", color = Color(0xFF818CF8), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                                            }
                                                    }
                                                }
                                            }
                                            if (selectedMode == mode) {
                                                Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF818CF8), modifier = Modifier.size(14.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Send Button
                    IconButton(
                        onClick = {
                            if ((input.isNotBlank() || attachments.isNotEmpty()) && !isLoading) {
                                scope.launch {
                                    var prompt = input
                                    if (attachments.isNotEmpty()) {
                                        val sb = StringBuilder()
                                        attachments.forEach { file ->
                                            val content = try {
                                                file.readText()
                                            } catch (e: Exception) {
                                                "Error reading file: ${e.message}"
                                            }
                                            sb.append("File: ${file.name}\n```\n$content\n```\n\n")
                                        }
                                        val filesContent = sb.toString().trim()
                                        prompt = if (prompt.isNotBlank()) "$prompt\n\n--- Attached Files ---\n$filesContent" else "--- Attached Files ---\n$filesContent"
                                    }

                                    onSendMessage(
                                        prompt,
                                        CreateSessionConfig(
                                            title = sessionTitle.takeIf { it.isNotBlank() },
                                            startingBranch = selectedBranch,
                                            automationMode = when (selectedMode) {
                                                "INTERACTIVE", "REVIEW" -> AutomationMode.NONE
                                                else -> AutomationMode.AUTO_CREATE_PR
                                            }
                                        )
                                    )
                                    input = ""
                                    sessionTitle = ""
                                    attachments.clear()
                                }
                            }
                        },
                        enabled = (input.isNotBlank() || attachments.isNotEmpty()) && !isLoading,
                        modifier = Modifier
                            .size(24.dp)
                            .shadow(
                                elevation = if (input.isNotBlank() || attachments.isNotEmpty()) 4.dp else 0.dp,
                                spotColor = Color(0xFF6366F1).copy(alpha = 0.25f),
                                shape = RoundedCornerShape(6.dp)
                            )
                            .background(
                                if (input.isNotBlank() || attachments.isNotEmpty()) Color(0xFF4F46E5) else Color(0xFF27272A),
                                RoundedCornerShape(6.dp)
                            )
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(12.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.ArrowForward, contentDescription = "Send", tint = if (input.isNotBlank() || attachments.isNotEmpty()) Color.White else Color.Gray, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        }
    }
}
