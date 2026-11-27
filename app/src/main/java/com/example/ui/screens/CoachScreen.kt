package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.MailOutline
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.mcp.McpWorkspaceState
import com.example.ui.theme.AscendThemeTokens
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.viewmodel.AscendUiState
import com.example.ui.viewmodel.CoachMessage
import com.example.ui.viewmodel.CoachRequestState
import com.example.ui.viewmodel.CoachUiState

@Composable
fun CoachScreen(
    workspaceState: AscendUiState,
    mcpState: McpWorkspaceState,
    coachState: CoachUiState,
    onSendMessage: (String) -> Unit,
    onClearHistory: () -> Unit,
    onDismissError: () -> Unit,
    onOpenMcpConnections: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = AscendThemeTokens.colors
    val listState = rememberLazyListState()
    var inputText by remember { mutableStateOf("") }
    var lastSentPrompt by remember { mutableStateOf("") }

    val quickPrompts = remember {
        listOf(
            "Help me plan my day",
            "What's on my calendar today?",
            "Summarize my recent emails",
            "I keep procrastinating",
            "I feel overwhelmed by tasks",
            "Break a big goal into steps"
        )
    }

    val isBusy = coachState.requestState is CoachRequestState.Loading ||
        coachState.requestState is CoachRequestState.UsingTool ||
        coachState.requestState is CoachRequestState.Streaming

    LaunchedEffect(coachState.messages.size, coachState.requestState) {
        if (coachState.messages.isNotEmpty()) {
            listState.animateScrollToItem(coachState.messages.lastIndex)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
    ) {
        // Compact Context Banner + Live MCP Tool Pills + Clear Action
        Surface(
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, tokens.cardBorder),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = "AI Life Coach",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Context-Aware Gemini Coach",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Lv ${workspaceState.level} · ${workspaceState.totalXp} XP · ${workspaceState.pendingTasksCount} focus · ${workspaceState.ghostTasks.size} ghosts",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = JetBrainsMonoFontFamily
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (coachState.messages.any { it.id > 0L }) {
                        IconButton(
                            onClick = onClearHistory,
                            enabled = !isBusy,
                            modifier = Modifier.testTag("coach_clear_history_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.DeleteOutline,
                                contentDescription = "Clear conversation history",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Live MCP Tool Status Badges (tap to manage in MCP connections)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CoachToolBadge(
                        icon = Icons.Rounded.CalendarMonth,
                        label = if (mcpState.calendarConnected) "Calendar live" else "Calendar off",
                        connected = mcpState.calendarConnected,
                        activeColor = tokens.mint,
                        onClick = onOpenMcpConnections
                    )
                    CoachToolBadge(
                        icon = Icons.Rounded.MailOutline,
                        label = if (mcpState.gmailConnected) "Gmail live" else "Gmail off",
                        connected = mcpState.gmailConnected,
                        activeColor = tokens.mint,
                        onClick = onOpenMcpConnections
                    )
                }
            }
        }

        // Chat Stream
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(coachState.messages, key = { it.id }) { msg ->
                CoachMessageBubble(message = msg)
            }

            if (coachState.requestState is CoachRequestState.Loading ||
                coachState.requestState is CoachRequestState.UsingTool
            ) {
                item(key = "coach_loading") {
                    val statusText = when (val req = coachState.requestState) {
                        is CoachRequestState.UsingTool -> req.statusLabel
                        else -> "Ascend AI Life Coach is thinking..."
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (coachState.requestState is CoachRequestState.Error && lastSentPrompt.isNotBlank()) {
                item(key = "coach_error_retry") {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 4.dp)
                    ) {
                        Surface(
                            onClick = { onSendMessage(lastSentPrompt) },
                            shape = RoundedCornerShape(999.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                            modifier = Modifier.testTag("coach_retry_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Refresh,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "Retry last prompt",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        TextButton(onClick = onDismissError) {
                            Text("Dismiss", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        // Quick-Ask Chips
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(quickPrompts) { suggestion ->
                Surface(
                    onClick = {
                        if (!isBusy) {
                            lastSentPrompt = suggestion
                            onSendMessage(suggestion)
                        }
                    },
                    shape = RoundedCornerShape(999.dp),
                    color = tokens.surfaceElevated,
                    border = BorderStroke(1.dp, tokens.cardBorderStrong),
                    modifier = Modifier.testTag("coach_quick_chip_${suggestion.hashCode()}")
                ) {
                    Text(
                        text = suggestion,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
                    )
                }
            }
        }

        // Message Composer
        Surface(
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, tokens.cardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("Ask about tasks, schedule, or emails...") },
                    maxLines = 3,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("coach_message_input"),
                    shape = RoundedCornerShape(18.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = tokens.cardBorderStrong,
                        focusedContainerColor = MaterialTheme.colorScheme.background,
                        unfocusedContainerColor = MaterialTheme.colorScheme.background
                    )
                )

                IconButton(
                    onClick = {
                        val trimmed = inputText.trim()
                        if (trimmed.isNotEmpty() && !isBusy) {
                            lastSentPrompt = trimmed
                            onSendMessage(trimmed)
                            inputText = ""
                        }
                    },
                    enabled = inputText.isNotBlank() && !isBusy,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            if (inputText.isNotBlank() && !isBusy) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                tokens.surfaceElevated
                            }
                        )
                        .testTag("coach_send_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Send,
                        contentDescription = "Send coaching message",
                        tint = if (inputText.isNotBlank() && !isBusy) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            tokens.textMuted
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun CoachToolBadge(
    icon: ImageVector,
    label: String,
    connected: Boolean,
    activeColor: Color,
    onClick: () -> Unit
) {
    val tokens = AscendThemeTokens.colors
    val tint = if (connected) activeColor else MaterialTheme.colorScheme.onSurfaceVariant
    val bg = if (connected) tokens.mintBg else tokens.surfaceElevated
    val border = if (connected) activeColor.copy(alpha = 0.45f) else tokens.cardBorder

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(999.dp),
        color = bg,
        border = BorderStroke(1.dp, border)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = tint
            )
        }
    }
}

@Composable
private fun CoachMessageBubble(message: CoachMessage) {
    val tokens = AscendThemeTokens.colors
    val bubbleShape = RoundedCornerShape(
        topStart = 20.dp,
        topEnd = 20.dp,
        bottomStart = if (message.isUser) 20.dp else 6.dp,
        bottomEnd = if (message.isUser) 6.dp else 20.dp
    )
    val bgColor = when {
        message.isError -> MaterialTheme.colorScheme.errorContainer
        message.isUser -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surface
    }
    val borderColor = when {
        message.isError -> MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
        message.isUser -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        else -> tokens.cardBorder
    }
    val textColor = when {
        message.isError -> MaterialTheme.colorScheme.onErrorContainer
        message.isUser -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .clip(bubbleShape)
                .background(bgColor)
                .border(width = 1.dp, color = borderColor, shape = bubbleShape)
                .padding(horizontal = 15.dp, vertical = 12.dp)
        ) {
            if (message.isError) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ErrorOutline,
                        contentDescription = "AI Error",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .size(16.dp)
                    )
                    Text(
                        text = message.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor
                    )
                }
            } else {
                Text(
                    text = if (message.isStreaming) "${message.text}▍" else message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor
                )
            }
        }
    }
}
