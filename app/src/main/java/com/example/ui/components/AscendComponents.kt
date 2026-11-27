package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.EventRepeat
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.model.EisenhowerQuadrant
import com.example.data.model.ReminderRecurrence
import com.example.data.model.TaskCategory
import com.example.data.model.TaskEntity
import com.example.data.repository.AscendRepository
import com.example.ui.theme.AscendThemeTokens
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Composable
fun quadrantAccentColor(quadrant: EisenhowerQuadrant): Color {
    val tokens = AscendThemeTokens.colors
    return when (quadrant) {
        EisenhowerQuadrant.DO_FIRST -> tokens.doFirst
        EisenhowerQuadrant.SCHEDULE -> tokens.schedule
        EisenhowerQuadrant.DELEGATE -> tokens.delegate
        EisenhowerQuadrant.ELIMINATE -> tokens.eliminate
    }
}

@Composable
fun quadrantContainerColor(quadrant: EisenhowerQuadrant): Color {
    val tokens = AscendThemeTokens.colors
    return when (quadrant) {
        EisenhowerQuadrant.DO_FIRST -> tokens.doFirstBg
        EisenhowerQuadrant.SCHEDULE -> tokens.scheduleBg
        EisenhowerQuadrant.DELEGATE -> tokens.delegateBg
        EisenhowerQuadrant.ELIMINATE -> tokens.eliminateBg
    }
}

@Composable
fun AscendCard(
    modifier: Modifier = Modifier,
    borderColor: Color = AscendThemeTokens.colors.cardBorder,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    cornerRadius: Dp = 20.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = modifier,
            shape = shape,
            color = containerColor,
            border = BorderStroke(1.dp, borderColor),
            content = content
        )
    } else {
        Surface(
            modifier = modifier,
            shape = shape,
            color = containerColor,
            border = BorderStroke(1.dp, borderColor),
            content = content
        )
    }
}

@Composable
fun QuadrantBadge(
    quadrant: EisenhowerQuadrant,
    modifier: Modifier = Modifier
) {
    val accent = quadrantAccentColor(quadrant)
    val bg = quadrantContainerColor(quadrant)
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .border(1.dp, accent.copy(alpha = 0.4f), RoundedCornerShape(999.dp))
            .padding(horizontal = 9.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(accent)
        )
        Text(
            text = quadrant.shortLabel,
            style = MaterialTheme.typography.labelSmall,
            color = accent,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun CategoryPill(
    category: String,
    modifier: Modifier = Modifier
) {
    val tokens = AscendThemeTokens.colors
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = tokens.surfaceElevated,
        border = BorderStroke(1.dp, tokens.cardBorder)
    ) {
        Text(
            text = category,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun TaskRowCard(
    task: TaskEntity,
    onToggleComplete: () -> Unit,
    onPostpone: () -> Unit,
    onMoveQuadrant: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    showQuadrantBadge: Boolean = true
) {
    val tokens = AscendThemeTokens.colors
    val accent = quadrantAccentColor(task.quadrantEnum)
    val isGhost = task.isGhostTask()
    var menuExpanded by remember { mutableStateOf(false) }

    val checkBg by animateColorAsState(
        targetValue = if (task.isCompleted) tokens.mint else Color.Transparent,
        label = "check_bg"
    )
    val checkBorder by animateColorAsState(
        targetValue = if (task.isCompleted) tokens.mint else accent.copy(alpha = 0.9f),
        label = "check_border"
    )

    AscendCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("task_item_card_${task.id}"),
        borderColor = if (isGhost) tokens.softRed.copy(alpha = 0.45f) else tokens.cardBorder,
        onClick = onEdit
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 6.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.Top
        ) {
            // 48dp accessible circular completion checkbox
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onToggleComplete)
                    .testTag("task_toggle_${task.id}"),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(checkBg)
                        .border(2.dp, checkBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (task.isCompleted) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = "Mark task incomplete",
                            tint = MaterialTheme.colorScheme.background,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 6.dp, end = 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                    ),
                    color = if (task.isCompleted) tokens.textMuted else MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                // Metadata line: category · due · postponed
                val metaParts = buildList {
                    add(task.category)
                    add(AscendRepository.formatDueLabel(task.dueAtMillis))
                    if (task.rescheduleCount > 0) {
                        add("Postponed ${task.rescheduleCount}×")
                    }
                }
                Text(
                    text = metaParts.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isGhost) tokens.softRed else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                val reminderLabel = if (!task.isCompleted) task.reminderBadgeLabel() else null
                if (reminderLabel != null) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f))
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                                RoundedCornerShape(999.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.NotificationsActive,
                            contentDescription = "Reminder scheduled",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = reminderLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (showQuadrantBadge) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        QuadrantBadge(quadrant = task.quadrantEnum)
                        if (!task.isCompleted) {
                            Text(
                                text = "+20 XP",
                                style = MaterialTheme.typography.labelSmall,
                                color = tokens.mint
                            )
                        }
                    }
                }
            }

            // Postpone / more actions (...) button
            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.testTag("task_postpone_menu_${task.id}")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MoreHoriz,
                        contentDescription = "Postpone or edit task",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    containerColor = tokens.surfaceElevated
                ) {
                    if (!task.isCompleted) {
                        DropdownMenuItem(
                            text = { Text("Postpone (+1 day)") },
                            leadingIcon = {
                                Icon(
                                    Icons.Outlined.EventRepeat,
                                    contentDescription = null,
                                    tint = tokens.gold
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onPostpone()
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Move quadrant") },
                        leadingIcon = {
                            Icon(Icons.Outlined.SwapHoriz, contentDescription = null)
                        },
                        onClick = {
                            menuExpanded = false
                            onMoveQuadrant()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Edit task") },
                        leadingIcon = {
                            Icon(Icons.Outlined.Edit, contentDescription = null)
                        },
                        onClick = {
                            menuExpanded = false
                            onEdit()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete task", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = {
                            Icon(
                                Icons.Outlined.DeleteOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TaskEditorSheet(
    initialTask: TaskEntity? = null,
    defaultQuadrant: EisenhowerQuadrant = EisenhowerQuadrant.DO_FIRST,
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        notes: String,
        category: TaskCategory,
        quadrant: EisenhowerQuadrant,
        dueAtMillis: Long,
        reminderEnabled: Boolean,
        reminderTimestamp: Long?,
        reminderRecurrence: ReminderRecurrence,
        reminderCustomDays: Set<Int>
    ) -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val tokens = AscendThemeTokens.colors
    val now = remember { System.currentTimeMillis() }
    val oneDay = 24L * 60L * 60L * 1000L
    val defaultReminderMillis = remember {
        Calendar.getInstance().apply {
            add(Calendar.HOUR_OF_DAY, 1)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    var title by remember(initialTask) { mutableStateOf(initialTask?.title ?: "") }
    var notes by remember(initialTask) { mutableStateOf(initialTask?.notes ?: "") }
    var category by remember(initialTask) {
        mutableStateOf(initialTask?.categoryEnum ?: TaskCategory.PERSONAL)
    }
    var quadrant by remember(initialTask) {
        mutableStateOf(initialTask?.quadrantEnum ?: defaultQuadrant)
    }
    var dueAtMillis by remember(initialTask) {
        mutableLongStateOf(initialTask?.dueAtMillis ?: (now + 4L * 60L * 60L * 1000L))
    }
    var reminderEnabled by remember(initialTask) {
        mutableStateOf(initialTask?.reminderEnabled ?: false)
    }
    var reminderTimestamp by remember(initialTask) {
        mutableLongStateOf(initialTask?.reminderTimestamp ?: defaultReminderMillis)
    }
    var reminderRecurrence by remember(initialTask) {
        mutableStateOf(initialTask?.recurrenceEnum ?: ReminderRecurrence.ONE_TIME)
    }
    var reminderCustomDays by remember(initialTask) {
        val existingDays = initialTask?.customDaySet.orEmpty()
        mutableStateOf(if (existingDays.isNotEmpty()) existingDays else setOf(1, 3, 5))
    }
    var showDatePicker by remember { mutableStateOf(false) }
    var showReminderDatePicker by remember { mutableStateOf(false) }
    var showReminderTimePicker by remember { mutableStateOf(false) }
    var notificationPermissionDenied by remember { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        notificationPermissionDenied = !granted
    }

    val requestNotificationPermissionIfNeeded: () -> Unit = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val isGranted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (isGranted) {
                notificationPermissionDenied = false
            } else {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = tokens.surfaceElevated,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = if (initialTask == null) "New task" else "Edit task",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                placeholder = { Text("What needs your focus?") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("task_title_input"),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = tokens.cardBorderStrong,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes (optional)") },
                placeholder = { Text("Helpful context or first step") },
                minLines = 2,
                maxLines = 3,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("task_notes_input"),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = tokens.cardBorderStrong,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )

            // Category selector: Study / Personal / Growth / Work / Health
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "CATEGORY",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TaskCategory.entries.forEach { cat ->
                        val selected = category == cat
                        Surface(
                            onClick = { category = cat },
                            modifier = Modifier.testTag("category_chip_${cat.label.lowercase()}"),
                            shape = RoundedCornerShape(999.dp),
                            color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(
                                1.dp,
                                if (selected) MaterialTheme.colorScheme.primary else tokens.cardBorderStrong
                            )
                        ) {
                            Text(
                                text = cat.label,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
                            )
                        }
                    }
                }
            }

            // Priority selector: Urgent — do first / Important — schedule / Urgent — delegate / Maybe later
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "PRIORITY",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    EisenhowerQuadrant.entries.forEach { item ->
                        val isSelected = quadrant == item
                        val accent = quadrantAccentColor(item)
                        val bg = quadrantContainerColor(item)
                        Surface(
                            onClick = { quadrant = item },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("quadrant_select_${item.name.lowercase()}"),
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) bg else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) accent else tokens.cardBorder
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(9.dp)
                                            .clip(CircleShape)
                                            .background(accent)
                                    )
                                    Text(
                                        text = item.modalLabel,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isSelected) accent else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = "Selected priority",
                                        tint = accent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Due Date
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "DUE DATE",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val dateOptions = listOf(
                        "Today" to (now + 4L * 60L * 60L * 1000L),
                        "Tomorrow" to (now + oneDay),
                        "+3 Days" to (now + 3L * oneDay)
                    )
                    dateOptions.forEach { (label, targetMillis) ->
                        val selected = AscendRepository.dayKeyFromMillis(dueAtMillis) ==
                            AscendRepository.dayKeyFromMillis(targetMillis)
                        Surface(
                            onClick = { dueAtMillis = targetMillis },
                            shape = RoundedCornerShape(12.dp),
                            color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(
                                1.dp,
                                if (selected) MaterialTheme.colorScheme.primary else tokens.cardBorderStrong
                            )
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                            )
                        }
                    }

                    Surface(
                        onClick = { showDatePicker = true },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, tokens.cardBorderStrong)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CalendarToday,
                                contentDescription = "Pick custom due date",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(dueAtMillis)),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Reminders (One-time & Recurring Notifications)
            AscendCard(
                containerColor = MaterialTheme.colorScheme.surface,
                borderColor = if (reminderEnabled) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
                } else {
                    tokens.cardBorder
                },
                cornerRadius = 16.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
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
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (reminderEnabled) {
                                            MaterialTheme.colorScheme.primaryContainer
                                        } else {
                                            tokens.surfaceElevated
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.NotificationsActive,
                                    contentDescription = null,
                                    tint = if (reminderEnabled) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Enable Reminder",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (reminderEnabled) {
                                        SimpleDateFormat("EEE, MMM d · h:mm a", Locale.getDefault())
                                            .format(Date(reminderTimestamp))
                                    } else {
                                        "Schedule a one-time or recurring alert"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (reminderEnabled) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        }

                        Switch(
                            checked = reminderEnabled,
                            onCheckedChange = { checked ->
                                reminderEnabled = checked
                                if (checked) {
                                    requestNotificationPermissionIfNeeded()
                                }
                            },
                            modifier = Modifier.testTag("task_reminder_switch"),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary,
                                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                uncheckedTrackColor = tokens.surfaceElevated
                            )
                        )
                    }

                    if (notificationPermissionDenied && reminderEnabled) {
                        Text(
                            text = "Notifications are currently blocked for Ascend. Enable notification permission in Android settings to receive alerts.",
                            style = MaterialTheme.typography.bodySmall,
                            color = tokens.gold
                        )
                    }

                    AnimatedVisibility(visible = reminderEnabled) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            // Date & Time pickers
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    onClick = { showReminderDatePicker = true },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("reminder_date_button"),
                                    shape = RoundedCornerShape(12.dp),
                                    color = tokens.surfaceElevated,
                                    border = BorderStroke(1.dp, tokens.cardBorderStrong)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.CalendarToday,
                                            contentDescription = "Pick reminder date",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                                                .format(Date(reminderTimestamp)),
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Surface(
                                    onClick = { showReminderTimePicker = true },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("reminder_time_button"),
                                    shape = RoundedCornerShape(12.dp),
                                    color = tokens.surfaceElevated,
                                    border = BorderStroke(1.dp, tokens.cardBorderStrong)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.AccessTime,
                                            contentDescription = "Pick reminder time",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = SimpleDateFormat("h:mm a", Locale.getDefault())
                                                .format(Date(reminderTimestamp)),
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }

                            // Recurrence selector
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "REPEAT",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    ReminderRecurrence.entries.forEach { rec ->
                                        val isSelected = reminderRecurrence == rec
                                        Surface(
                                            onClick = { reminderRecurrence = rec },
                                            modifier = Modifier.testTag("recurrence_chip_${rec.name.lowercase()}"),
                                            shape = RoundedCornerShape(999.dp),
                                            color = if (isSelected) {
                                                MaterialTheme.colorScheme.primaryContainer
                                            } else {
                                                tokens.surfaceElevated
                                            },
                                            border = BorderStroke(
                                                1.dp,
                                                if (isSelected) {
                                                    MaterialTheme.colorScheme.primary
                                                } else {
                                                    tokens.cardBorderStrong
                                                }
                                            )
                                        ) {
                                            Text(
                                                text = rec.label,
                                                style = MaterialTheme.typography.labelMedium,
                                                color = if (isSelected) {
                                                    MaterialTheme.colorScheme.onPrimaryContainer
                                                } else {
                                                    MaterialTheme.colorScheme.onSurface
                                                },
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // Custom weekday chips (M T W T F S S)
                            AnimatedVisibility(visible = reminderRecurrence == ReminderRecurrence.CUSTOM_DAYS) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = "CUSTOM DAYS",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    val dayLabels = listOf(
                                        1 to "M",
                                        2 to "T",
                                        3 to "W",
                                        4 to "T",
                                        5 to "F",
                                        6 to "S",
                                        7 to "S"
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        dayLabels.forEach { (isoDay, letter) ->
                                            val active = isoDay in reminderCustomDays
                                            Surface(
                                                onClick = {
                                                    reminderCustomDays = if (active) {
                                                        if (reminderCustomDays.size > 1) {
                                                            reminderCustomDays - isoDay
                                                        } else {
                                                            reminderCustomDays
                                                        }
                                                    } else {
                                                        reminderCustomDays + isoDay
                                                    }
                                                },
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .testTag("custom_day_chip_$isoDay"),
                                                shape = CircleShape,
                                                color = if (active) {
                                                    MaterialTheme.colorScheme.primary
                                                } else {
                                                    tokens.surfaceElevated
                                                },
                                                border = BorderStroke(
                                                    1.dp,
                                                    if (active) {
                                                        MaterialTheme.colorScheme.primary
                                                    } else {
                                                        tokens.cardBorderStrong
                                                    }
                                                )
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text(
                                                        text = letter,
                                                        style = MaterialTheme.typography.labelMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (active) {
                                                            MaterialTheme.colorScheme.onPrimary
                                                        } else {
                                                            MaterialTheme.colorScheme.onSurfaceVariant
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, tokens.cardBorderStrong)
                ) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            onSave(
                                title.trim(),
                                notes.trim(),
                                category,
                                quadrant,
                                dueAtMillis,
                                reminderEnabled,
                                if (reminderEnabled) reminderTimestamp else null,
                                if (reminderEnabled) reminderRecurrence else ReminderRecurrence.ONE_TIME,
                                if (reminderEnabled && reminderRecurrence == ReminderRecurrence.CUSTOM_DAYS) {
                                    reminderCustomDays
                                } else {
                                    emptySet()
                                }
                            )
                        }
                    },
                    enabled = title.isNotBlank(),
                    modifier = Modifier
                        .weight(1.4f)
                        .height(50.dp)
                        .testTag("save_task_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(if (initialTask == null) "Add task" else "Update task")
                }
            }
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = dueAtMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { selectedUtc ->
                            dueAtMillis = selectedUtc + 17L * 60L * 60L * 1000L
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("Select")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }

    if (showReminderDatePicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = reminderTimestamp)
        DatePickerDialog(
            onDismissRequest = { showReminderDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { selectedUtc ->
                            val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                                timeInMillis = selectedUtc
                            }
                            val localCal = Calendar.getInstance().apply {
                                timeInMillis = reminderTimestamp
                                set(Calendar.YEAR, utcCal.get(Calendar.YEAR))
                                set(Calendar.MONTH, utcCal.get(Calendar.MONTH))
                                set(Calendar.DAY_OF_MONTH, utcCal.get(Calendar.DAY_OF_MONTH))
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }
                            reminderTimestamp = localCal.timeInMillis
                        }
                        showReminderDatePicker = false
                    }
                ) {
                    Text("Select")
                }
            },
            dismissButton = {
                TextButton(onClick = { showReminderDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }

    if (showReminderTimePicker) {
        val currentCal = remember(reminderTimestamp) {
            Calendar.getInstance().apply { timeInMillis = reminderTimestamp }
        }
        val timePickerState = rememberTimePickerState(
            initialHour = currentCal.get(Calendar.HOUR_OF_DAY),
            initialMinute = currentCal.get(Calendar.MINUTE),
            is24Hour = false
        )
        AlertDialog(
            onDismissRequest = { showReminderTimePicker = false },
            containerColor = tokens.surfaceElevated,
            title = {
                Text(
                    text = "Select reminder time",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    TimePicker(state = timePickerState)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val updated = Calendar.getInstance().apply {
                            timeInMillis = reminderTimestamp
                            set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                            set(Calendar.MINUTE, timePickerState.minute)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        reminderTimestamp = updated.timeInMillis
                        showReminderTimePicker = false
                    }
                ) {
                    Text("Set time")
                }
            },
            dismissButton = {
                TextButton(onClick = { showReminderTimePicker = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoveQuadrantSheet(
    task: TaskEntity,
    onDismiss: () -> Unit,
    onSelectQuadrant: (EisenhowerQuadrant) -> Unit
) {
    val tokens = AscendThemeTokens.colors
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = tokens.surfaceElevated
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Move quadrant",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = task.title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            EisenhowerQuadrant.entries.forEach { quadrant ->
                val isCurrent = task.quadrantEnum == quadrant
                val accent = quadrantAccentColor(quadrant)
                val bg = quadrantContainerColor(quadrant)
                Surface(
                    onClick = { onSelectQuadrant(quadrant) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("move_to_${quadrant.name.lowercase()}"),
                    shape = RoundedCornerShape(16.dp),
                    color = if (isCurrent) bg else MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        if (isCurrent) 1.5.dp else 1.dp,
                        if (isCurrent) accent else tokens.cardBorder
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = quadrant.modalLabel,
                                style = MaterialTheme.typography.titleMedium,
                                color = if (isCurrent) accent else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = quadrant.guidance,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (isCurrent) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = "Current quadrant",
                                tint = accent
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SectionHeaderRow(
    title: String,
    trailingText: String? = null,
    onTrailingClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (trailingText != null) {
            if (onTrailingClick != null) {
                TextButton(
                    onClick = onTrailingClick,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = trailingText,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            } else {
                Text(
                    text = trailingText,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun CoachSparklePill(
    label: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f))
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f), RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Icon(
            imageVector = Icons.Rounded.AutoAwesome,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}
