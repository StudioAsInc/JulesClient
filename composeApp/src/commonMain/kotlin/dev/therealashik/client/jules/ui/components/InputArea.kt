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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.therealashik.jules.sdk.model.AutomationMode
import dev.therealashik.jules.sdk.model.JulesSource
import dev.therealashik.client.jules.ui.JulesOpacity
import dev.therealashik.client.jules.ui.JulesShapes
import dev.therealashik.client.jules.ui.JulesSizes
import dev.therealashik.client.jules.ui.JulesSpacing
import dev.therealashik.client.jules.utils.PlatformFile
import dev.therealashik.client.jules.utils.rememberFilePickerLauncher
import dev.therealashik.client.jules.model.CreateSessionConfig
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
    sources: List<JulesSource>,
    onSourceChange: (JulesSource) -> Unit,
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

    // Repository Selection
    var isRepoMenuOpen by remember { mutableStateOf(false) }
    var repoSearch by remember { mutableStateOf("") }
    val filteredSources = remember(sources, repoSearch) {
        if (repoSearch.isEmpty()) sources
        else sources.filter {
            (it.displayName ?: it.name).contains(repoSearch, ignoreCase = true)
        }
    }

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
                    JulesShapes.pill
                )
                .border(
                    1.dp,
                    if (isFocused) Color.White.copy(alpha = JulesOpacity.focused) else Color.White.copy(alpha = JulesOpacity.normal),
                    JulesShapes.pill
                )
                .padding(JulesSpacing.s),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Plus Button
            IconButton(
                onClick = { filePicker.launch() },
                modifier = Modifier
                    .size(JulesSizes.touchTarget)
                    .clip(JulesShapes.circle)
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
            FilledIconButton(
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
                modifier = Modifier.size(JulesSizes.touchTarget),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = if (isEnabled) Color(0xFF4F46E5) else Color(0xFF27272A),
                    contentColor = if (isEnabled) Color.White else Color(0xFF71717A),
                    disabledContainerColor = Color(0xFF27272A),
                    disabledContentColor = Color(0xFF71717A)
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
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    } else {
        // --- DEFAULT VARIANT (Hero Card) ---
        Column(modifier = modifier) {
            Card(
                shape = JulesShapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = if (isExpanded) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant,
                        shape = JulesShapes.large
                    )
                    .animateContentSize()
                    .clickable(enabled = !isFocused) {
                        focusRequester.requestFocus()
                    }
                    .then(
                        if (isExpanded) Modifier.shadow(
                            elevation = 8.dp,
                            shape = JulesShapes.large,
                            spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        ) else Modifier.shadow(elevation = 2.dp, shape = JulesShapes.large)
                    )
            ) {
                Column(
                    modifier = Modifier.padding(PaddingValues(horizontal = JulesSpacing.l, vertical = JulesSpacing.m))
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
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            cursorColor = MaterialTheme.colorScheme.primary
                        ),
                        placeholder = {
                            Text(
                                placeholderText,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                                .padding(top = JulesSpacing.s),
                            horizontalArrangement = Arrangement.spacedBy(JulesSpacing.s)
                        ) {
                            items(attachments) { file ->
                                InputChip(
                                    selected = true,
                                    onClick = { attachments.remove(file) },
                                    label = { Text(file.name, maxLines = 1, fontSize = 12.sp) },
                                    trailingIcon = { Icon(Icons.Default.Close, null, Modifier.size(14.dp)) },
                                    colors = InputChipDefaults.inputChipColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        selectedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        selectedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    border = InputChipDefaults.inputChipBorder(
                                        enabled = true,
                                        selected = true,
                                        borderColor = MaterialTheme.colorScheme.outlineVariant
                                    )
                                )
                            }
                        }
                    }

                    // Inlined Actions Row
                    AnimatedVisibility(visible = isExpanded) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = JulesSpacing.m),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(JulesSpacing.s),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Attachment Button (Pill Style)
                                FilledTonalButton(
                                    onClick = { filePicker.launch() },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.height(32.dp),
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                ) {
                                    Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Add", fontSize = 12.sp)
                                }

                                // Repository Selector (Chip Style)
                                Box {
                                    AssistChip(
                                        onClick = { isRepoMenuOpen = true },
                                        label = {
                                            Text(
                                                currentSource?.displayName ?: currentSource?.name ?: "Select Repo",
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.widthIn(max = 100.dp),
                                                fontSize = 12.sp
                                            )
                                        },
                                        leadingIcon = { Icon(Icons.Default.Terminal, null, modifier = Modifier.size(14.dp)) },
                                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, null, modifier = Modifier.size(14.dp)) },
                                        colors = AssistChipDefaults.assistChipColors(
                                            containerColor = Color.Transparent,
                                            labelColor = MaterialTheme.colorScheme.onSurface
                                        ),
                                        border = AssistChipDefaults.assistChipBorder(borderColor = MaterialTheme.colorScheme.outlineVariant)
                                    )

                                    if (isRepoMenuOpen) {
                                        ModalBottomSheet(
                                            onDismissRequest = { isRepoMenuOpen = false },
                                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                            contentColor = MaterialTheme.colorScheme.onSurface
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 16.dp)
                                                    .padding(bottom = 16.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                                                    androidx.compose.foundation.text.BasicTextField(
                                                        value = repoSearch,
                                                        onValueChange = { repoSearch = it },
                                                        modifier = Modifier.weight(1f).padding(start = 8.dp),
                                                        textStyle = androidx.compose.ui.text.TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp),
                                                        singleLine = true,
                                                        decorationBox = { innerTextField ->
                                                            if (repoSearch.isEmpty()) {
                                                                Text("Find a repository...", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                                                            }
                                                            innerTextField()
                                                        }
                                                    )
                                                }

                                                Spacer(modifier = Modifier.height(12.dp))

                                                LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                                                    items(filteredSources) { source ->
                                                        val isSelected = currentSource?.name == source.name
                                                        Row(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .heightIn(min = 44.dp)
                                                                .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                                                .clickable {
                                                                    onSourceChange(source)
                                                                    isRepoMenuOpen = false
                                                                }
                                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Text(
                                                                source.displayName ?: source.name,
                                                                fontSize = 14.sp,
                                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                                                modifier = Modifier.weight(1f),
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                            if (isSelected) {
                                                                Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                // Branch Selector
                                Box {
                                    AssistChip(
                                        onClick = { isBranchMenuOpen = true },
                                        label = {
                                            Text(
                                                selectedBranch,
                                                fontSize = 12.sp,
                                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                            )
                                        },
                                        leadingIcon = { Icon(Icons.Default.AccountTree, null, modifier = Modifier.size(14.dp)) },
                                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, null, modifier = Modifier.size(14.dp)) },
                                        colors = AssistChipDefaults.assistChipColors(
                                            containerColor = Color.Transparent,
                                            labelColor = MaterialTheme.colorScheme.onSurface
                                        ),
                                        border = AssistChipDefaults.assistChipBorder(borderColor = MaterialTheme.colorScheme.outlineVariant)
                                    )

                                    DropdownMenu(
                                        expanded = isBranchMenuOpen,
                                        onDismissRequest = { isBranchMenuOpen = false },
                                        modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainer)
                                    ) {
                                        currentSource?.githubRepo?.branches?.forEach { branch ->
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        branch.displayName,
                                                        color = if(branch.displayName == selectedBranch) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                                    )
                                                },
                                                onClick = {
                                                    selectedBranch = branch.displayName
                                                    isBranchMenuOpen = false
                                                },
                                                leadingIcon = {
                                                    if (branch.displayName == selectedBranch) {
                                                        Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
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
                                        modifier = Modifier.size(32.dp),
                                        colors = IconButtonDefaults.iconButtonColors(
                                            contentColor = if(sessionTitle.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    ) {
                                        Icon(
                                            Icons.Default.Settings,
                                            contentDescription = "Settings",
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    DropdownMenu(
                                        expanded = isSettingsMenuOpen,
                                        onDismissRequest = { isSettingsMenuOpen = false },
                                        modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainer).width(280.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(JulesSpacing.m)) {
                                            // Session Title
                                            Text("SESSION TITLE", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                                            Spacer(modifier = Modifier.height(6.dp))
                                            OutlinedTextField(
                                                value = sessionTitle,
                                                onValueChange = { sessionTitle = it },
                                                placeholder = { Text("Optional title...", fontSize = 12.sp) },
                                                textStyle = LocalTextStyle.current.copy(fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface),
                                                modifier = Modifier.fillMaxWidth().height(40.dp),
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                                )
                                            )
                                        }
                                    }
                                }
                            }

                            // Send Split Button
                            Row(
                                modifier = Modifier
                                    .height(36.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(if ((input.isNotBlank() || attachments.isNotEmpty()) && !isLoading) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                // Main Action
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .clickable(enabled = (input.isNotBlank() || attachments.isNotEmpty()) && !isLoading) {
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
                                        .padding(horizontal = 16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isLoading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                    } else {
                                        Text(
                                            "Send",
                                            color = if ((input.isNotBlank() || attachments.isNotEmpty()) && !isLoading) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=0.38f),
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp
                                        )
                                    }
                                }

                                // Divider
                                Box(
                                    modifier = Modifier
                                        .width(1.dp)
                                        .fillMaxHeight()
                                        .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f))
                                )

                                // Dropdown Trigger
                                var isModeMenuOpen by remember { mutableStateOf(false) }
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .clickable(enabled = !isLoading) { isModeMenuOpen = true }
                                        .padding(horizontal = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.ArrowDropDown,
                                        null,
                                        tint = if ((input.isNotBlank() || attachments.isNotEmpty()) && !isLoading) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=0.38f),
                                        modifier = Modifier.size(18.dp)
                                    )

                                    DropdownMenu(
                                        expanded = isModeMenuOpen,
                                        onDismissRequest = { isModeMenuOpen = false },
                                        modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainer)
                                    ) {
                                        val modes = listOf(
                                            Triple("START", "Start immediately", Icons.Default.Send),
                                            Triple("SCHEDULED", "Scheduled task", Icons.Default.Schedule),
                                            Triple("INTERACTIVE", "Interactive plan", Icons.Default.ChatBubble),
                                            Triple("REVIEW", "Review plan", Icons.Default.Search)
                                        )

                                        modes.forEach { (mode, label, icon) ->
                                            DropdownMenuItem(
                                                text = {
                                                    Column {
                                                        Text(label, fontSize = 14.sp)
                                                        if (mode == selectedMode) {
                                                            Text("Selected", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                                                        }
                                                    }
                                                },
                                                leadingIcon = {
                                                    Icon(icon, null, tint = if (mode == selectedMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                                                },
                                                onClick = {
                                                    selectedMode = mode
                                                    isModeMenuOpen = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
