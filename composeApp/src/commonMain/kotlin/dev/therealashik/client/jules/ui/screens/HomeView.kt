package dev.therealashik.client.jules.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.therealashik.jules.sdk.model.JulesSession
import dev.therealashik.jules.sdk.model.JulesSource
import dev.therealashik.client.jules.ui.JulesBackground
import dev.therealashik.client.jules.ui.JulesOpacity
import dev.therealashik.client.jules.ui.JulesPrimary
import dev.therealashik.client.jules.ui.JulesShapes
import dev.therealashik.client.jules.ui.JulesSizes
import dev.therealashik.client.jules.ui.JulesSpacing
import dev.therealashik.client.jules.ui.components.InputArea
import dev.therealashik.client.jules.ui.components.ProactiveSection
import dev.therealashik.client.jules.model.CreateSessionConfig

@Composable
fun HomeView(
    currentSource: JulesSource?,
    sources: List<JulesSource>,
    onSourceChange: (JulesSource) -> Unit,
    onSendMessage: (String, CreateSessionConfig) -> Unit,
    isProcessing: Boolean,
    sessions: List<JulesSession> = emptyList(),
    onSelectSession: ((JulesSession) -> Unit)? = null,
    onResetKey: (() -> Unit)? = null,
    error: String? = null
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState()),
        contentAlignment = Alignment.TopCenter
    ) {
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(animationSpec = tween(500)),
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 800.dp)
                    .padding(horizontal = JulesSpacing.l, vertical = JulesSpacing.xxl),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                if (currentSource == null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = JulesSpacing.xxl)
                            .background(Color(0xFFF59E0B).copy(alpha = JulesOpacity.normal), JulesShapes.medium)
                            .border(1.dp, Color(0xFFF59E0B).copy(alpha = JulesOpacity.focused), JulesShapes.medium)
                            .padding(JulesSpacing.l),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(JulesSpacing.m)
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(JulesSizes.iconMedium))
                        Text(
                            "No repositories found. Ensure the Jules App is installed on your GitHub.",
                            color = Color(0xFFF59E0B),
                            fontSize = 14.sp,
                            modifier = Modifier.padding(vertical = JulesSpacing.xs)
                        )
                    }
                }

                // Input Area
                InputArea(
                    onSendMessage = onSendMessage,
                    isLoading = isProcessing,
                    currentSource = currentSource,
                    sources = sources,
                    onSourceChange = onSourceChange,
                    modifier = Modifier.padding(bottom = JulesSpacing.m)
                )

                if (error != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = JulesSpacing.xxl)
                            .background(Color(0xFFEF4444).copy(alpha = JulesOpacity.normal), JulesShapes.medium)
                            .border(1.dp, Color(0xFFEF4444).copy(alpha = JulesOpacity.focused), JulesShapes.medium)
                            .padding(JulesSpacing.l),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(JulesSpacing.m)
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(JulesSizes.iconMedium))
                        Text(
                            error,
                            color = Color(0xFFEF4444),
                            fontSize = 14.sp
                        )
                    }
                }

                // Proactive Section
                ProactiveSection(
                    sessions = sessions,
                    onSelectSession = onSelectSession
                )

                // Footer
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = JulesSpacing.xxxl),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(JulesSpacing.s)) {
                        ActionButton(Icons.Default.ViewInAr, "Render")
                        ActionButton(Icons.Default.Terminal, "CLI")
                        ActionButton(Icons.Default.Code, "API")
                    }

                    if (onResetKey != null) {
                        ActionButton(Icons.Default.Refresh, "Reset Key", onClick = onResetKey)
                    }
                }
            
            // Bottom padding for mobile
            Spacer(modifier = Modifier.height(JulesSpacing.xxl))
        }
    }
}
}

@Composable
fun ActionButton(icon: ImageVector, label: String, onClick: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .height(JulesSizes.touchTarget)
            .clip(JulesShapes.small)
            .background(Color(0xFF1E1E22))
            .border(1.dp, Color.White.copy(alpha = JulesOpacity.normal), JulesShapes.small)
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(horizontal = JulesSpacing.m, vertical = JulesSpacing.s),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(JulesSpacing.s)
    ) {
        Icon(icon, contentDescription = null, tint = Color(0xFFA1A1AA), modifier = Modifier.size(14.dp))
        Text(
            label,
            color = Color(0xFFA1A1AA),
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}
