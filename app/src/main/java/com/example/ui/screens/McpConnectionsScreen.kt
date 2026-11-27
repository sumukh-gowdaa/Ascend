package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Hub
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material.icons.rounded.MailOutline
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.auth.GoogleAccountInfo
import com.example.mcp.CalendarEventItem
import com.example.mcp.GmailMessageItem
import com.example.mcp.McpWorkspaceState
import com.example.ui.components.AscendCard
import com.example.ui.components.SectionHeaderRow
import com.example.ui.theme.AscendThemeTokens
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.viewmodel.AscendUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun McpConnectionsScreen(
    uiState: AscendUiState,
    mcpState: McpWorkspaceState,
    exportedJson: String?,
    googleAccount: GoogleAccountInfo?,
    isAuthLoading: Boolean,
    authStatusMessage: String?,
    onConnectCalendar: () -> Unit,
    onDisconnectCalendar: () -> Unit,
    onRefreshCalendar: () -> Unit,
    onConnectGmail: () -> Unit,
    onDisconnectGmail: () -> Unit,
    onRefreshGmail: () -> Unit,
    onGoogleSignIn: () -> Unit,
    onGoogleSignOut: () -> Unit,
    onSaveDisplayName: (String) -> Unit,
    onClearSampleData: () -> Unit,
    onCreateLocalBackup: () -> Unit,
    onDismissExportedJson: () -> Unit,
    onRestoreLocalBackup: () -> Unit,
    onResetAllData: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = AscendThemeTokens.colors
    val clipboardManager = LocalClipboardManager.current

    var displayNameDraft by remember { mutableStateOf(uiState.preferences.displayName) }
    LaunchedEffect(uiState.preferences.displayName) {
        displayNameDraft = uiState.preferences.displayName
    }

    var showResetDialog by remember { mutableStateOf(false) }

    val initialLetter = uiState.preferences.displayName.firstOrNull()?.uppercase() ?: "A"
    val lastBackupLabel = remember(uiState.preferences.lastBackupTimestamp) {
        if (uiState.preferences.lastBackupTimestamp <= 0L) {
            "No local backup saved yet"
        } else {
            val fmt = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
            "Last backup: ${fmt.format(Date(uiState.preferences.lastBackupTimestamp))}"
        }
    }

    val liveConnectorCount = (if (mcpState.calendarConnected) 1 else 0) +
        (if (mcpState.gmailConnected) 1 else 0)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Header
        item(key = "mcp_header") {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "MODEL CONTEXT PROTOCOL",
                    style = MaterialTheme.typography.labelSmall,
                    color = tokens.skyBlue
                )
                Text(
                    text = "MCP connections",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Authorize read-only Google Calendar and Gmail access natively so your AI Life Coach can call live tools for your schedule and inbox.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // MCP Architecture Overview Card
        item(key = "mcp_architecture_card") {
            AscendCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("mcp_architecture_card"),
                borderColor = tokens.skyBlue.copy(alpha = 0.45f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(tokens.skyBlueBg)
                                .border(1.dp, tokens.skyBlue.copy(alpha = 0.5f), RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Hub,
                                contentDescription = "Model Context Protocol architecture",
                                tint = tokens.skyBlue,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Native OAuth + Gemini Tool Bridge",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Read-only scopes · Tokens held in memory only",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Text(
                        text = "Connect Google Calendar or Gmail below using Google Identity Services. When you ask the AI Life Coach about meetings, deadlines, or emails, Gemini invokes `getCalendarEvents` and `getRecentEmails` with your live data while Room keeps your tasks 100% offline-ready.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        McpMetricPill(
                            label = "LOCAL GRAPH",
                            value = "${uiState.tasks.size} tasks",
                            accent = tokens.mint,
                            modifier = Modifier.weight(1f)
                        )
                        McpMetricPill(
                            label = "LIVE CONNECTORS",
                            value = "$liveConnectorCount / 2 active",
                            accent = if (liveConnectorCount > 0) tokens.mint else tokens.skyBlue,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Interactive Google Calendar & Gmail MCP Cards + Local Task Tools
        item(key = "mcp_connectors_section") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionHeaderRow(title = "Tool Connectors")

                InteractiveCalendarConnectorCard(
                    connected = mcpState.calendarConnected,
                    isLoading = mcpState.calendarLoading,
                    statusMessage = mcpState.calendarStatusMessage,
                    events = mcpState.calendarEvents,
                    onConnect = onConnectCalendar,
                    onDisconnect = onDisconnectCalendar,
                    onRefresh = onRefreshCalendar
                )

                InteractiveGmailConnectorCard(
                    connected = mcpState.gmailConnected,
                    isLoading = mcpState.gmailLoading,
                    statusMessage = mcpState.gmailStatusMessage,
                    emails = mcpState.recentEmails,
                    onConnect = onConnectGmail,
                    onDisconnect = onDisconnectGmail,
                    onRefresh = onRefreshGmail
                )

                McpConnectorCard(
                    icon = Icons.Rounded.Checklist,
                    title = "Task tools",
                    statusLabel = "Active · Local Room",
                    statusAccent = tokens.mint,
                    statusBg = tokens.mintBg,
                    description = "Local Room Eisenhower Matrix, Ghost Task detector, and XP wallet are synced directly into every AI Life Coach turn.",
                    capabilities = listOf("Eisenhower Matrix sync", "Ghost Task breakdown", "XP & Streak telemetry"),
                    tag = "mcp_card_task_tools"
                )
            }
        }

        // Workspace Profile & Optional Google Sign-In
        item(key = "profile_section_header") {
            SectionHeaderRow(title = "Workspace & Account")
        }

        item(key = "profile_identity_card") {
            AscendCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("profile_google_card"),
                borderColor = if (googleAccount != null) tokens.mint.copy(alpha = 0.45f) else tokens.cardBorderStrong
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        if (googleAccount?.photoUrl != null) {
                            AsyncImage(
                                model = googleAccount.photoUrl,
                                contentDescription = "Google profile photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .border(1.5.dp, tokens.mint, CircleShape)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = initialLetter,
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = googleAccount?.displayName ?: uiState.preferences.displayName,
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (googleAccount != null && googleAccount.email.isNotBlank()) {
                                Text(
                                    text = googleAccount.email,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                if (googleAccount != null) {
                                    Icon(
                                        imageVector = Icons.Rounded.VerifiedUser,
                                        contentDescription = "Google account linked",
                                        tint = tokens.mint,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                Text(
                                    text = if (googleAccount != null) {
                                        "Google Linked · Level ${uiState.level} ${uiState.levelTitle}"
                                    } else {
                                        "Offline Local Workspace · Level ${uiState.level} ${uiState.levelTitle}"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = tokens.mint
                                )
                            }
                        }
                    }

                    if (googleAccount == null) {
                        Button(
                            onClick = onGoogleSignIn,
                            enabled = !isAuthLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("profile_google_sign_in_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            if (isAuthLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Signing in with Google...")
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.AccountCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(19.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Sign in with Google")
                            }
                        }
                    } else {
                        OutlinedButton(
                            onClick = onGoogleSignOut,
                            enabled = !isAuthLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("profile_google_sign_out_button"),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, tokens.cardBorderStrong)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.Logout,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sign Out", color = MaterialTheme.colorScheme.onSurface)
                        }
                    }

                    if (!authStatusMessage.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = tokens.surfaceElevated,
                            border = BorderStroke(1.dp, tokens.cardBorderStrong),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("profile_auth_status_banner")
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Info,
                                    contentDescription = "Authentication status",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .padding(top = 1.dp)
                                        .size(16.dp)
                                )
                                Text(
                                    text = authStatusMessage,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = tokens.cardBorder)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = displayNameDraft,
                            onValueChange = { displayNameDraft = it },
                            label = { Text("Display name") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("profile_display_name_input"),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = tokens.cardBorderStrong
                            )
                        )
                        Button(
                            onClick = { onSaveDisplayName(displayNameDraft) },
                            enabled = displayNameDraft.trim().isNotEmpty() &&
                                displayNameDraft.trim() != uiState.preferences.displayName,
                            modifier = Modifier
                                .height(54.dp)
                                .testTag("profile_save_name_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Icon(Icons.Rounded.Check, contentDescription = "Save display name")
                        }
                    }
                }
            }
        }

        // Local Data & Backup
        item(key = "data_management_card") {
            AscendCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Local Data & Backup",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = lastBackupLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedButton(
                        onClick = onClearSampleData,
                        enabled = uiState.sampleItemCount > 0,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("clear_sample_data_button"),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(
                            1.dp,
                            if (uiState.sampleItemCount > 0) MaterialTheme.colorScheme.primary else tokens.cardBorder
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CleaningServices,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (uiState.sampleItemCount > 0) {
                                "Clear Starter Tasks (${uiState.sampleItemCount} items)"
                            } else {
                                "Starter Tasks Cleared"
                            }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onCreateLocalBackup,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("create_local_backup_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Backup,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Backup")
                        }

                        OutlinedButton(
                            onClick = onRestoreLocalBackup,
                            enabled = uiState.hasLocalBackup,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("restore_local_backup_button"),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, tokens.cardBorderStrong)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Restore,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Restore")
                        }
                    }

                    OutlinedButton(
                        onClick = { showResetDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("reset_all_data_button"),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.55f))
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteForever,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Reset All Local Data", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }

        item(key = "mcp_bottom_spacer") {
            Spacer(modifier = Modifier.height(72.dp))
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            containerColor = tokens.surfaceElevated,
            title = { Text("Reset All Local Data?") },
            text = {
                Text(
                    "This permanently deletes all tasks, XP progress, unlocked rewards, streak history, and saved local backups on this device."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showResetDialog = false
                        onResetAllData()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text("Reset Everything")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (exportedJson != null) {
        AlertDialog(
            onDismissRequest = onDismissExportedJson,
            containerColor = tokens.surfaceElevated,
            title = { Text("Local Backup Saved") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Saved locally on-device. You can also copy the JSON snapshot below:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.background,
                        border = BorderStroke(1.dp, tokens.cardBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    ) {
                        Text(
                            text = exportedJson,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .verticalScroll(rememberScrollState())
                                .padding(12.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(exportedJson))
                        onDismissExportedJson()
                    }
                ) {
                    Icon(Icons.Outlined.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy JSON")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissExportedJson) {
                    Text("Done")
                }
            }
        )
    }
}

@Composable
private fun InteractiveCalendarConnectorCard(
    connected: Boolean,
    isLoading: Boolean,
    statusMessage: String?,
    events: List<CalendarEventItem>,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onRefresh: () -> Unit
) {
    val tokens = AscendThemeTokens.colors
    val accent = if (connected) tokens.mint else tokens.skyBlue
    val bg = if (connected) tokens.mintBg else tokens.skyBlueBg
    val statusText = when {
        isLoading -> "Syncing..."
        connected -> "Connected · calendar.readonly"
        else -> "Not connected"
    }

    AscendCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("mcp_card_calendar"),
        borderColor = accent.copy(alpha = 0.42f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(bg)
                        .border(1.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CalendarMonth,
                        contentDescription = "Google Calendar MCP",
                        tint = accent,
                        modifier = Modifier.size(21.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Google Calendar MCP",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = bg,
                        border = BorderStroke(1.dp, accent.copy(alpha = 0.45f))
                    ) {
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = accent,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                        )
                    }
                }

                if (connected) {
                    IconButton(
                        onClick = onRefresh,
                        enabled = !isLoading,
                        modifier = Modifier.testTag("mcp_calendar_refresh_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "Refresh Google Calendar events",
                            tint = tokens.mint
                        )
                    }
                }
            }

            Text(
                text = "Fetches today's and tomorrow's events from your primary Google Calendar and exposes `getCalendarEvents` to your AI Life Coach.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (!connected) {
                    Button(
                        onClick = onConnect,
                        enabled = !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("mcp_calendar_connect_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Authorizing Calendar...")
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.Link,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Connect")
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = onDisconnect,
                        enabled = !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("mcp_calendar_disconnect_button"),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, tokens.softRed.copy(alpha = 0.55f))
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.LinkOff,
                            contentDescription = null,
                            tint = tokens.softRed,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Disconnect", color = tokens.softRed)
                    }
                }
            }

            if (!statusMessage.isNullOrBlank()) {
                ConnectorStatusBanner(message = statusMessage, tag = "mcp_calendar_status")
            }

            if (connected) {
                HorizontalDivider(color = tokens.cardBorder)
                Text(
                    text = "TODAY & TOMORROW (${events.size})",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFontFamily),
                    color = tokens.mint
                )
                if (events.isEmpty()) {
                    Text(
                        text = "No upcoming events found for today or tomorrow.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        events.take(4).forEach { event ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = tokens.surfaceElevated,
                                border = BorderStroke(1.dp, tokens.cardBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.Top,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Schedule,
                                        contentDescription = null,
                                        tint = tokens.skyBlue,
                                        modifier = Modifier
                                            .padding(top = 2.dp)
                                            .size(15.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = event.summary,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = if (event.isAllDay) {
                                                "All day · ${event.startTime}"
                                            } else {
                                                event.startTime
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (event.location.isNotBlank()) {
                                            Text(
                                                text = event.location,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = tokens.textMuted,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
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

@Composable
private fun InteractiveGmailConnectorCard(
    connected: Boolean,
    isLoading: Boolean,
    statusMessage: String?,
    emails: List<GmailMessageItem>,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onRefresh: () -> Unit
) {
    val tokens = AscendThemeTokens.colors
    val accent = if (connected) tokens.mint else tokens.gold
    val bg = if (connected) tokens.mintBg else tokens.goldBg
    val statusText = when {
        isLoading -> "Syncing..."
        connected -> "Connected · gmail.readonly"
        else -> "Not connected"
    }

    AscendCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("mcp_card_gmail"),
        borderColor = accent.copy(alpha = 0.42f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(bg)
                        .border(1.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MailOutline,
                        contentDescription = "Gmail MCP",
                        tint = accent,
                        modifier = Modifier.size(21.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Gmail MCP",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = bg,
                        border = BorderStroke(1.dp, accent.copy(alpha = 0.45f))
                    ) {
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = accent,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                        )
                    }
                }

                if (connected) {
                    IconButton(
                        onClick = onRefresh,
                        enabled = !isLoading,
                        modifier = Modifier.testTag("mcp_gmail_refresh_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "Refresh recent Gmail messages",
                            tint = tokens.mint
                        )
                    }
                }
            }

            Text(
                text = "Fetches your 5 most recent Gmail subjects and snippets and exposes `getRecentEmails` to your AI Life Coach for inbox triage and deadlines.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (!connected) {
                    Button(
                        onClick = onConnect,
                        enabled = !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("mcp_gmail_connect_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Authorizing Gmail...")
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.Link,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Connect")
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = onDisconnect,
                        enabled = !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("mcp_gmail_disconnect_button"),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, tokens.softRed.copy(alpha = 0.55f))
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.LinkOff,
                            contentDescription = null,
                            tint = tokens.softRed,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Disconnect", color = tokens.softRed)
                    }
                }
            }

            if (!statusMessage.isNullOrBlank()) {
                ConnectorStatusBanner(message = statusMessage, tag = "mcp_gmail_status")
            }

            if (connected) {
                HorizontalDivider(color = tokens.cardBorder)
                Text(
                    text = "RECENT MESSAGES (${emails.size})",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFontFamily),
                    color = tokens.mint
                )
                if (emails.isEmpty()) {
                    Text(
                        text = "No recent Gmail messages found.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        emails.take(5).forEach { mail ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = tokens.surfaceElevated,
                                border = BorderStroke(1.dp, tokens.cardBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = mail.subject,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = mail.from,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = mail.snippet,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
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

@Composable
private fun ConnectorStatusBanner(message: String, tag: String) {
    val tokens = AscendThemeTokens.colors
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = tokens.surfaceElevated,
        border = BorderStroke(1.dp, tokens.cardBorderStrong),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(tag)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Info,
                contentDescription = "Connector status",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(top = 1.dp)
                    .size(15.dp)
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun McpMetricPill(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val tokens = AscendThemeTokens.colors
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = tokens.surfaceElevated,
        border = BorderStroke(1.dp, tokens.cardBorder)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.SemiBold
                ),
                color = accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun McpConnectorCard(
    icon: ImageVector,
    title: String,
    statusLabel: String,
    statusAccent: Color,
    statusBg: Color,
    description: String,
    capabilities: List<String>,
    tag: String
) {
    val tokens = AscendThemeTokens.colors
    AscendCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(tag),
        borderColor = statusAccent.copy(alpha = 0.35f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(statusBg)
                        .border(1.dp, statusAccent.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = statusAccent,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = statusBg,
                        border = BorderStroke(1.dp, statusAccent.copy(alpha = 0.45f))
                    ) {
                        Text(
                            text = statusLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = statusAccent,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                capabilities.take(3).forEach { cap ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = tokens.surfaceElevated,
                        border = BorderStroke(1.dp, tokens.cardBorder)
                    ) {
                        Text(
                            text = cap,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}
